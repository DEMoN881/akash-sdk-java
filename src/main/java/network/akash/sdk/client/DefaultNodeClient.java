package network.akash.sdk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default NodeClient implementation — queries Tendermint RPC /status endpoint.
 * Port of akash-api/go/node/client/v1beta2 node.go
 */
public final class DefaultNodeClient implements NodeClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultNodeClient.class);

    private final String rpcEndpoint;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public DefaultNodeClient(String rpcEndpoint) {
        this.rpcEndpoint = rpcEndpoint.endsWith("/") ? rpcEndpoint : rpcEndpoint + "/";
        this.httpClient = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public SyncInfo syncInfo() throws Exception {
        String url = rpcEndpoint + "status";
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("failed to query node status: HTTP " + response.code());
            }

            JsonNode root = mapper.readTree(response.body().string());
            JsonNode syncInfoNode = root.path("result").path("sync_info");

            return new SyncInfo(
                    syncInfoNode.path("latest_block_hash").asText(),
                    syncInfoNode.path("latest_block_height").asLong(),
                    syncInfoNode.path("latest_block_time").asText(),
                    syncInfoNode.path("catching_up").asBoolean()
            );
        }
    }
}
