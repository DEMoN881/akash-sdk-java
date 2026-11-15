package network.akash.sdk.client;

import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.GroupID;
import network.akash.sdk.types.market.BidID;
import network.akash.sdk.types.market.LeaseID;
import network.akash.sdk.types.market.OrderID;
import java.util.List;
import java.util.Map;

/**
 * Query client interface — wraps all Akash and Cosmos SDK query modules.
 * Port of akash-api/go/node/client/v1beta2.QueryClient
 *
 * Each method mirrors the corresponding gRPC query endpoint.
 * Responses are returned as generic Map (JSON-decoded from protobuf)
 * until protobuf-generated Java stubs are available.
 */
public interface QueryClient {

    // --- Deployment queries ---

    Map<String, Object> deployments(Map<String, String> filters) throws Exception;

    Map<String, Object> deployment(DeploymentID id) throws Exception;

    Map<String, Object> group(GroupID id) throws Exception;

    // --- Market queries ---

    Map<String, Object> orders(Map<String, String> filters) throws Exception;

    Map<String, Object> order(OrderID id) throws Exception;

    Map<String, Object> bids(Map<String, String> filters) throws Exception;

    Map<String, Object> bid(BidID id) throws Exception;

    Map<String, Object> leases(Map<String, String> filters) throws Exception;

    Map<String, Object> lease(LeaseID id) throws Exception;

    // --- Provider queries ---

    Map<String, Object> providers(Map<String, String> filters) throws Exception;

    Map<String, Object> provider(String providerAddress) throws Exception;

    // --- Audit queries ---

    Map<String, Object> allProvidersAttributes() throws Exception;

    Map<String, Object> providerAttributes(String providerAddress) throws Exception;

    Map<String, Object> providerAuditorAttributes(String providerAddress, String auditorAddress) throws Exception;

    Map<String, Object> auditorAttributes(String auditorAddress) throws Exception;

    // --- Certificate queries ---

    Map<String, Object> certificates(Map<String, String> filters) throws Exception;

    // --- Cosmos SDK module accessors ---

    CosmosQueryModules cosmos();

    interface CosmosQueryModules {
        Map<String, Object> authAccount(String address) throws Exception;
        Map<String, Object> bankBalance(String address, String denom) throws Exception;
        Map<String, Object> bankAllBalances(String address) throws Exception;
        Map<String, Object> stakingDelegations(String delegator) throws Exception;
        Map<String, Object> govProposal(long proposalId) throws Exception;
    }
}
