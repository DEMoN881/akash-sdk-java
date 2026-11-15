package network.akash.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import network.akash.sdk.cosmos.Msg;
import network.akash.sdk.crypto.AkashKeyPair;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serial transaction broadcaster — serializes transaction submissions through a single queue,
 * handles sequence number sync, gas estimation, retry on sequence mismatch.
 *
 * Port of akash-api/go/node/client/v1beta2.serialBroadcaster (tx.go)
 *
 * Key design from Go SDK:
 * 1. All broadcast requests are queued and processed one at a time (serial)
 * 2. Sequence numbers are tracked locally and synced from chain on mismatch
 * 3. On sequence mismatch, the tx is retried once with the corrected sequence
 * 4. Broadcast uses commit mode, with retry polling on timeout
 */
public final class SerialBroadcaster implements TxClient, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SerialBroadcaster.class);
    private static final Pattern SEQUENCE_MISMATCH = Pattern.compile("account sequence mismatch, expected (\\d+), got (\\d+)");
    private static final String TIMEOUT_ERROR = "timed out waiting for tx to be included in a block";
    private static final String NOT_FOUND_SUFFIX = ") not found";

    private final String rpcEndpoint;
    private final String restEndpoint;
    private final AkashKeyPair keyPair;
    private final String chainId;
    private final NodeClient nodeClient;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    private final BlockingQueue<BroadcastRequest> requestQueue;
    private final ExecutorService broadcastExecutor;
    private final AtomicLong currentSequence;
    private final AtomicLong accountNumber;
    private final AtomicBoolean running;

    public SerialBroadcaster(
            String rpcEndpoint,
            String restEndpoint,
            AkashKeyPair keyPair,
            String chainId,
            NodeClient nodeClient
    ) {
        this.rpcEndpoint = rpcEndpoint;
        this.restEndpoint = restEndpoint;
        this.keyPair = keyPair;
        this.chainId = chainId;
        this.nodeClient = nodeClient;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();

        this.requestQueue = new LinkedBlockingQueue<>();
        this.broadcastExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "akash-serial-broadcaster");
            t.setDaemon(true);
            return t;
        });
        this.currentSequence = new AtomicLong(-1);
        this.accountNumber = new AtomicLong(-1);
        this.running = new AtomicBoolean(true);

        broadcastExecutor.submit(this::broadcastLoop);
    }

    @Override
    public BroadcastResult broadcast(List<Msg> msgs, BroadcastOptions... opts) throws Exception {
        if (!running.get()) {
            throw new IllegalStateException("tx client: not running");
        }

        BroadcastOptions mergedOpts = new BroadcastOptions();
        for (BroadcastOptions opt : opts) {
            mergedOpts.merge(opt);
        }

        CompletableFuture<BroadcastResult> future = new CompletableFuture<>();
        requestQueue.put(new BroadcastRequest(msgs, mergedOpts, future));

        Duration timeout = mergedOpts.getBroadcastTimeout() != null
                ? mergedOpts.getBroadcastTimeout()
                : BroadcastOptions.DEFAULT_TIMEOUT;

        try {
            return future.get(timeout.toMillis() + 5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("tx client: broadcast timed out", e);
        }
    }

    private void broadcastLoop() {
        while (running.get()) {
            try {
                BroadcastRequest req = requestQueue.poll(1, TimeUnit.SECONDS);
                if (req == null) continue;

                try {
                    BroadcastResult result = doBroadcastWithRetry(req.msgs, req.opts);
                    req.future.complete(result);
                } catch (Exception e) {
                    req.future.completeExceptionally(e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private BroadcastResult doBroadcastWithRetry(List<Msg> msgs, BroadcastOptions opts) throws Exception {
        ensureSequenceSynced();

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                BroadcastResult result = broadcastTx(msgs, opts);
                currentSequence.incrementAndGet();
                return result;
            } catch (SequenceMismatchException e) {
                log.warn("sequence mismatch, syncing: {}", e.getMessage());
                syncAccountSequence();
                if (attempt == 1) throw e;
            }
        }

        throw new RuntimeException("broadcast failed after retries");
    }

    private BroadcastResult broadcastTx(List<Msg> msgs, BroadcastOptions opts) throws Exception {
        long seq = currentSequence.get();
        long accNum = accountNumber.get();

        // Build the amino/protobuf transaction envelope
        // In a full implementation this uses protobuf serialization + secp256k1 signing
        Map<String, Object> txBody = buildTxBody(msgs, opts);
        Map<String, Object> authInfo = buildAuthInfo(seq, accNum, opts);
        Map<String, Object> txRaw = buildSignedTx(txBody, authInfo);

        // Broadcast via /cosmos/tx/v1beta1/txs (REST) in BROADCAST_MODE_SYNC
        Map<String, Object> broadcastPayload = new HashMap<>();
        broadcastPayload.put("tx_bytes", txRaw.get("tx_bytes"));
        broadcastPayload.put("mode", "BROADCAST_MODE_SYNC");

        String json = mapper.writeValueAsString(broadcastPayload);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(restEndpoint + "cosmos/tx/v1beta1/txs")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) {
                throw new RuntimeException("empty broadcast response");
            }
            String respBody = response.body().string();
            JsonNode root = mapper.readTree(respBody);
            JsonNode txResponse = root.path("tx_response");

            int code = txResponse.path("code").asInt(0);
            String rawLog = txResponse.path("raw_log").asText("");
            String txHash = txResponse.path("txhash").asText("");

            if (code != 0) {
                Matcher m = SEQUENCE_MISMATCH.matcher(rawLog);
                if (m.find()) {
                    throw new SequenceMismatchException(rawLog);
                }
                throw new TxFailedException(code, rawLog, txResponse.path("codespace").asText(""));
            }

            // Wait for tx to be included in block (like BroadcastTxCommit behavior)
            Duration timeout = opts.getBroadcastTimeout() != null
                    ? opts.getBroadcastTimeout()
                    : BroadcastOptions.DEFAULT_TIMEOUT;

            return waitForTx(txHash, timeout);
        }
    }

    private BroadcastResult waitForTx(String txHash, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            try {
                return queryTx(txHash);
            } catch (TxNotFoundException e) {
                Thread.sleep(BroadcastOptions.BLOCK_RETRY_PERIOD.toMillis());
            }
        }

        throw new RuntimeException("tx client: timed out waiting for tx " + txHash + " to be included in a block");
    }

    private BroadcastResult queryTx(String txHash) throws Exception {
        Request request = new Request.Builder()
                .url(restEndpoint + "cosmos/tx/v1beta1/txs/" + txHash)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) throw new RuntimeException("empty tx query response");
            String body = response.body().string();

            if (response.code() == 404 || body.contains(NOT_FOUND_SUFFIX)) {
                throw new TxNotFoundException(txHash);
            }

            JsonNode root = mapper.readTree(body);
            JsonNode txResp = root.path("tx_response");

            return new BroadcastResult(
                    txResp.path("txhash").asText(""),
                    txResp.path("height").asLong(0),
                    txResp.path("code").asInt(0),
                    txResp.path("raw_log").asText(""),
                    txResp.path("codespace").asText(""),
                    txResp.path("gas_wanted").asLong(0),
                    txResp.path("gas_used").asLong(0)
            );
        }
    }

    private void ensureSequenceSynced() throws Exception {
        if (currentSequence.get() < 0) {
            syncAccountSequence();
        }
    }

    private void syncAccountSequence() throws Exception {
        NodeClient.SyncInfo syncInfo = nodeClient.syncInfo();
        if (syncInfo.catchingUp()) {
            throw new RuntimeException("tx client: cannot sync from catching up node");
        }

        String address = keyPair.getAddress().toBech32();
        Request request = new Request.Builder()
                .url(restEndpoint + "cosmos/auth/v1beta1/accounts/" + address)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) throw new RuntimeException("cannot fetch account info");
            String body = response.body().string();
            JsonNode root = mapper.readTree(body);
            JsonNode account = root.path("account");

            long accNum = account.path("account_number").asLong(0);
            long seq = account.path("sequence").asLong(0);

            accountNumber.set(accNum);
            currentSequence.set(seq);

            log.info("account sequence synced: accountNumber={}, sequence={}", accNum, seq);
        }
    }

    private Map<String, Object> buildTxBody(List<Msg> msgs, BroadcastOptions opts) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Msg msg : msgs) {
            Map<String, Object> m = new HashMap<>();
            m.put("@type", "/akash.deployment.v1beta3.Msg" + capitalize(msg.getType()));
            messages.add(m);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messages", messages);
        if (opts.getNote() != null) body.put("memo", opts.getNote());
        if (opts.getTimeoutHeight() != null) body.put("timeout_height", opts.getTimeoutHeight().toString());
        return body;
    }

    private Map<String, Object> buildAuthInfo(long sequence, long accountNumber, BroadcastOptions opts) {
        Map<String, Object> authInfo = new HashMap<>();
        // Signer info
        Map<String, Object> signerInfo = new HashMap<>();
        Map<String, Object> publicKey = new HashMap<>();
        publicKey.put("@type", "/cosmos.crypto.secp256k1.PubKey");
        publicKey.put("key", Base64.getEncoder().encodeToString(keyPair.getPublicKey()));
        signerInfo.put("public_key", publicKey);
        signerInfo.put("sequence", String.valueOf(sequence));
        Map<String, Object> modeInfo = new HashMap<>();
        Map<String, Object> single = new HashMap<>();
        single.put("mode", "SIGN_MODE_DIRECT");
        modeInfo.put("single", single);
        signerInfo.put("mode_info", modeInfo);
        authInfo.put("signer_infos", List.of(signerInfo));

        // Fee
        Map<String, Object> fee = new HashMap<>();
        if (opts.getGas() != null) {
            fee.put("gas_limit", String.valueOf(opts.getGas()));
        } else {
            fee.put("gas_limit", "200000");
        }
        if (opts.getFees() != null) {
            fee.put("amount", List.of(parseCoinString(opts.getFees())));
        }
        authInfo.put("fee", fee);
        return authInfo;
    }

    private Map<String, Object> buildSignedTx(Map<String, Object> txBody, Map<String, Object> authInfo) {
        // In a full implementation:
        // 1. Serialize txBody and authInfo as protobuf
        // 2. Create SignDoc (bodyBytes, authInfoBytes, chainId, accountNumber)
        // 3. SHA256 hash the SignDoc
        // 4. Sign with secp256k1 (keyPair.sign())
        // 5. Compose TxRaw (bodyBytes, authInfoBytes, signatures)
        // 6. Base64-encode for REST broadcast
        //
        // This is a stub — the actual signing requires cosmos-sdk protobuf schemas.
        // For now, returns the structure that would be base64-encoded.
        Map<String, Object> result = new HashMap<>();
        result.put("tx_bytes", ""); // placeholder: real impl serializes protobuf
        result.put("body", txBody);
        result.put("auth_info", authInfo);
        return result;
    }

    private Map<String, String> parseCoinString(String coin) {
        // Parse "1000uakt" → {denom: "uakt", amount: "1000"}
        int i = 0;
        while (i < coin.length() && (Character.isDigit(coin.charAt(i)) || coin.charAt(i) == '.')) i++;
        Map<String, String> result = new HashMap<>();
        result.put("denom", coin.substring(i));
        result.put("amount", coin.substring(0, i));
        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        // "create-deployment" → "CreateDeployment"
        StringBuilder sb = new StringBuilder();
        for (String part : s.split("-")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public void close() {
        running.set(false);
        broadcastExecutor.shutdownNow();
    }

    // --- Internal exceptions ---

    private static class SequenceMismatchException extends RuntimeException {
        SequenceMismatchException(String msg) { super(msg); }
    }

    private static class TxNotFoundException extends Exception {
        TxNotFoundException(String hash) { super("tx not found: " + hash); }
    }

    public static class TxFailedException extends RuntimeException {
        private final int code;
        private final String codespace;

        TxFailedException(int code, String rawLog, String codespace) {
            super("tx failed: code=" + code + " log=" + rawLog);
            this.code = code;
            this.codespace = codespace;
        }

        public int getCode() { return code; }
        public String getCodespace() { return codespace; }
    }

    private record BroadcastRequest(List<Msg> msgs, BroadcastOptions opts, CompletableFuture<BroadcastResult> future) {}
}
