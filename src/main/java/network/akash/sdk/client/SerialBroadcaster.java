package network.akash.sdk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import cosmos.base.v1beta1.CoinOuterClass;
import cosmos.tx.v1beta1.TxOuterClass;
import network.akash.sdk.cosmos.Msg;
import network.akash.sdk.cosmos.TxSigner;
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

        List<Any> anyMsgs = new ArrayList<>();
        for (Msg msg : msgs) {
            byte[] signBytes = msg.getSignBytes();
            String typeUrl = resolveTypeUrl(msg);
            anyMsgs.add(Any.newBuilder()
                    .setTypeUrl(typeUrl)
                    .setValue(ByteString.copyFrom(signBytes))
                    .build());
        }

        long gasLimit = opts.getGas() != null ? opts.getGas() : 200000L;
        List<CoinOuterClass.Coin> feeCoins = new ArrayList<>();
        if (opts.getFees() != null) {
            String[] parsed = parseCoinString(opts.getFees());
            feeCoins.add(CoinOuterClass.Coin.newBuilder()
                    .setDenom(parsed[1])
                    .setAmount(parsed[0])
                    .build());
        }

        byte[] txRawBytes = TxSigner.buildAndSign(
                keyPair, chainId, accNum, seq,
                anyMsgs, gasLimit, feeCoins,
                opts.getNote(), opts.getTimeoutHeight()
        );

        String txBytesBase64 = Base64.getEncoder().encodeToString(txRawBytes);

        TxOuterClass.BroadcastTxRequest broadcastReq = TxOuterClass.BroadcastTxRequest.newBuilder()
                .setTxBytes(ByteString.copyFrom(txRawBytes))
                .setMode(TxOuterClass.BroadcastMode.BROADCAST_MODE_SYNC)
                .build();

        Map<String, Object> broadcastPayload = new HashMap<>();
        broadcastPayload.put("tx_bytes", txBytesBase64);
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

    private String resolveTypeUrl(Msg msg) {
        String route = msg.getRoute();
        String type = msg.getType();
        String msgName = "Msg" + toPascalCase(type);
        return switch (route) {
            case "deployment" -> "/akash.deployment.v1beta3." + msgName;
            case "market" -> "/akash.market.v1beta4." + msgName;
            case "provider" -> "/akash.provider.v1beta3." + msgName;
            case "audit" -> "/akash.audit.v1beta3." + msgName;
            case "cert" -> "/akash.cert.v1beta3." + msgName;
            default -> "/" + route + "." + msgName;
        };
    }

    private String[] parseCoinString(String coin) {
        int i = 0;
        while (i < coin.length() && (Character.isDigit(coin.charAt(i)) || coin.charAt(i) == '.')) i++;
        return new String[]{coin.substring(0, i), coin.substring(i)};
    }

    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) return s;
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
