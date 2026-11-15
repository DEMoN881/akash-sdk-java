package network.akash.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.GroupID;
import network.akash.sdk.types.market.BidID;
import network.akash.sdk.types.market.LeaseID;
import network.akash.sdk.types.market.OrderID;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Default QueryClient implementation — uses Akash node's REST (LCD) API.
 * Port of akash-api/go/node/client/v1beta2.queryClient
 *
 * In the Go SDK, queries go over gRPC via generated stubs.
 * Here we use the REST gateway (cosmos LCD) which mirrors the gRPC API as HTTP/JSON.
 * When protobuf-generated Java gRPC stubs are available, this can be replaced with
 * direct gRPC calls.
 */
public final class DefaultQueryClient implements QueryClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultQueryClient.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final String restEndpoint;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final CosmosQueryModules cosmosModules;

    public DefaultQueryClient(String restEndpoint) {
        this.restEndpoint = restEndpoint.endsWith("/") ? restEndpoint : restEndpoint + "/";
        this.httpClient = new OkHttpClient();
        this.mapper = new ObjectMapper();
        this.cosmosModules = new DefaultCosmosQueryModules();
    }

    // --- Deployment ---

    @Override
    public Map<String, Object> deployments(Map<String, String> filters) throws Exception {
        HttpUrl.Builder url = baseUrl("akash/deployment/v1beta4/deployments/list");
        if (filters != null) filters.forEach(url::addQueryParameter);
        return get(url.build());
    }

    @Override
    public Map<String, Object> deployment(DeploymentID id) throws Exception {
        return get(baseUrl("akash/deployment/v1beta4/deployments/info")
                .addQueryParameter("id.owner", id.owner())
                .addQueryParameter("id.dseq", String.valueOf(id.dseq()))
                .build());
    }

    @Override
    public Map<String, Object> group(GroupID id) throws Exception {
        return get(baseUrl("akash/deployment/v1beta4/groups/info")
                .addQueryParameter("id.owner", id.owner())
                .addQueryParameter("id.dseq", String.valueOf(id.dseq()))
                .addQueryParameter("id.gseq", String.valueOf(id.gseq()))
                .build());
    }

    // --- Market ---

    @Override
    public Map<String, Object> orders(Map<String, String> filters) throws Exception {
        HttpUrl.Builder url = baseUrl("akash/market/v1beta5/orders/list");
        if (filters != null) filters.forEach(url::addQueryParameter);
        return get(url.build());
    }

    @Override
    public Map<String, Object> order(OrderID id) throws Exception {
        return get(baseUrl("akash/market/v1beta5/orders/info")
                .addQueryParameter("id.owner", id.owner())
                .addQueryParameter("id.dseq", String.valueOf(id.dseq()))
                .addQueryParameter("id.gseq", String.valueOf(id.gseq()))
                .addQueryParameter("id.oseq", String.valueOf(id.oseq()))
                .build());
    }

    @Override
    public Map<String, Object> bids(Map<String, String> filters) throws Exception {
        HttpUrl.Builder url = baseUrl("akash/market/v1beta5/bids/list");
        if (filters != null) filters.forEach(url::addQueryParameter);
        return get(url.build());
    }

    @Override
    public Map<String, Object> bid(BidID id) throws Exception {
        return get(baseUrl("akash/market/v1beta5/bids/info")
                .addQueryParameter("id.owner", id.owner())
                .addQueryParameter("id.dseq", String.valueOf(id.dseq()))
                .addQueryParameter("id.gseq", String.valueOf(id.gseq()))
                .addQueryParameter("id.oseq", String.valueOf(id.oseq()))
                .addQueryParameter("id.provider", id.provider())
                .build());
    }

    @Override
    public Map<String, Object> leases(Map<String, String> filters) throws Exception {
        HttpUrl.Builder url = baseUrl("akash/market/v1beta5/leases/list");
        if (filters != null) filters.forEach(url::addQueryParameter);
        return get(url.build());
    }

    @Override
    public Map<String, Object> lease(LeaseID id) throws Exception {
        return get(baseUrl("akash/market/v1beta5/leases/info")
                .addQueryParameter("id.owner", id.owner())
                .addQueryParameter("id.dseq", String.valueOf(id.dseq()))
                .addQueryParameter("id.gseq", String.valueOf(id.gseq()))
                .addQueryParameter("id.oseq", String.valueOf(id.oseq()))
                .addQueryParameter("id.provider", id.provider())
                .build());
    }

    // --- Provider ---

    @Override
    public Map<String, Object> providers(Map<String, String> filters) throws Exception {
        HttpUrl.Builder url = baseUrl("akash/provider/v1beta4/providers");
        if (filters != null) filters.forEach(url::addQueryParameter);
        return get(url.build());
    }

    @Override
    public Map<String, Object> provider(String providerAddress) throws Exception {
        return get(baseUrl("akash/provider/v1beta4/providers/" + providerAddress).build());
    }

    // --- Audit ---

    @Override
    public Map<String, Object> allProvidersAttributes() throws Exception {
        return get(baseUrl("akash/audit/v1/audit/attributes/list").build());
    }

    @Override
    public Map<String, Object> providerAttributes(String providerAddress) throws Exception {
        return get(baseUrl("akash/audit/v1/audit/attributes/" + providerAddress + "/list").build());
    }

    @Override
    public Map<String, Object> providerAuditorAttributes(String providerAddress, String auditorAddress) throws Exception {
        return get(baseUrl("akash/audit/v1/audit/attributes/" + auditorAddress + "/" + providerAddress).build());
    }

    @Override
    public Map<String, Object> auditorAttributes(String auditorAddress) throws Exception {
        return get(baseUrl("akash/provider/v1/auditor/" + auditorAddress + "/list").build());
    }

    // --- Cert ---

    @Override
    public Map<String, Object> certificates(Map<String, String> filters) throws Exception {
        HttpUrl.Builder url = baseUrl("akash/cert/v1/certificates/list");
        if (filters != null) filters.forEach(url::addQueryParameter);
        return get(url.build());
    }

    // --- Cosmos SDK ---

    @Override
    public CosmosQueryModules cosmos() {
        return cosmosModules;
    }

    // --- Internal ---

    private HttpUrl.Builder baseUrl(String path) {
        return HttpUrl.parse(restEndpoint + path).newBuilder();
    }

    private Map<String, Object> get(HttpUrl url) throws Exception {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) {
                throw new RuntimeException("empty response body from " + url);
            }
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new RuntimeException("query failed: HTTP " + response.code() + " — " + body);
            }
            return mapper.readValue(body, MAP_TYPE);
        }
    }

    private class DefaultCosmosQueryModules implements CosmosQueryModules {
        @Override
        public Map<String, Object> authAccount(String address) throws Exception {
            return get(baseUrl("cosmos/auth/v1beta1/accounts/" + address).build());
        }

        @Override
        public Map<String, Object> bankBalance(String address, String denom) throws Exception {
            return get(baseUrl("cosmos/bank/v1beta1/balances/" + address + "/by_denom")
                    .addQueryParameter("denom", denom).build());
        }

        @Override
        public Map<String, Object> bankAllBalances(String address) throws Exception {
            return get(baseUrl("cosmos/bank/v1beta1/balances/" + address).build());
        }

        @Override
        public Map<String, Object> stakingDelegations(String delegator) throws Exception {
            return get(baseUrl("cosmos/staking/v1beta1/delegations/" + delegator).build());
        }

        @Override
        public Map<String, Object> govProposal(long proposalId) throws Exception {
            return get(baseUrl("cosmos/gov/v1beta1/proposals/" + proposalId).build());
        }
    }
}
