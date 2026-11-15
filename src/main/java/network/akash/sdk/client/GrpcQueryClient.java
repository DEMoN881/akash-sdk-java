package network.akash.sdk.client;

import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.GroupID;
import network.akash.sdk.types.market.BidID;
import network.akash.sdk.types.market.LeaseID;
import network.akash.sdk.types.market.OrderID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class GrpcQueryClient implements QueryClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcQueryClient.class);
    private final ManagedChannel channel;
    private final JsonFormat.Printer jsonPrinter = JsonFormat.printer().includingDefaultValueFields();
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private final akash.deployment.v1beta4.QueryGrpc.QueryBlockingStub deploymentStub;
    private final akash.market.v1beta5.QueryGrpc.QueryBlockingStub marketStub;
    private final akash.provider.v1beta4.QueryGrpc.QueryBlockingStub providerStub;
    private final akash.audit.v1.QueryGrpc.QueryBlockingStub auditStub;
    private final akash.cert.v1.QueryGrpc.QueryBlockingStub certStub;
    private final cosmos.auth.v1beta1.QueryGrpc.QueryBlockingStub authStub;
    private final cosmos.bank.v1beta1.QueryGrpc.QueryBlockingStub bankStub;

    public GrpcQueryClient(ManagedChannel channel) {
        this.channel = channel;
        this.deploymentStub = akash.deployment.v1beta4.QueryGrpc.newBlockingStub(channel);
        this.marketStub = akash.market.v1beta5.QueryGrpc.newBlockingStub(channel);
        this.providerStub = akash.provider.v1beta4.QueryGrpc.newBlockingStub(channel);
        this.auditStub = akash.audit.v1.QueryGrpc.newBlockingStub(channel);
        this.certStub = akash.cert.v1.QueryGrpc.newBlockingStub(channel);
        this.authStub = cosmos.auth.v1beta1.QueryGrpc.newBlockingStub(channel);
        this.bankStub = cosmos.bank.v1beta1.QueryGrpc.newBlockingStub(channel);
    }

    @Override
    public Map<String, Object> deployments(Map<String, String> filters) throws Exception {
        var reqBuilder = akash.deployment.v1beta4.QueryDeploymentsRequest.newBuilder();
        if (filters != null) {
            var fb = akash.deployment.v1beta4.DeploymentFilters.newBuilder();
            if (filters.containsKey("owner")) fb.setOwner(filters.get("owner"));
            if (filters.containsKey("dseq")) fb.setDseq(Long.parseLong(filters.get("dseq")));
            if (filters.containsKey("state")) fb.setState(filters.get("state"));
            reqBuilder.setFilters(fb);
        }
        var resp = deploymentStub.deployments(reqBuilder.build());
        return toMap(resp);
    }

    @Override
    public Map<String, Object> deployment(DeploymentID id) throws Exception {
        var req = akash.deployment.v1beta4.QueryDeploymentRequest.newBuilder()
                .setId(akash.deployment.v1beta4.DeploymentID.newBuilder()
                        .setOwner(id.owner()).setDseq(id.dseq()).build())
                .build();
        return toMap(deploymentStub.deployment(req));
    }

    @Override
    public Map<String, Object> group(GroupID id) throws Exception {
        var req = akash.deployment.v1beta4.QueryGroupRequest.newBuilder()
                .setId(akash.deployment.v1beta4.GroupID.newBuilder()
                        .setOwner(id.owner()).setDseq(id.dseq()).setGseq(id.gseq()).build())
                .build();
        return toMap(deploymentStub.group(req));
    }

    @Override
    public Map<String, Object> orders(Map<String, String> filters) throws Exception {
        var reqBuilder = akash.market.v1beta5.QueryOrdersRequest.newBuilder();
        if (filters != null) {
            var fb = akash.market.v1beta5.OrderFilters.newBuilder();
            if (filters.containsKey("owner")) fb.setOwner(filters.get("owner"));
            if (filters.containsKey("dseq")) fb.setDseq(Long.parseLong(filters.get("dseq")));
            if (filters.containsKey("gseq")) fb.setGseq(Integer.parseInt(filters.get("gseq")));
            if (filters.containsKey("oseq")) fb.setOseq(Integer.parseInt(filters.get("oseq")));
            if (filters.containsKey("state")) fb.setState(filters.get("state"));
            reqBuilder.setFilters(fb);
        }
        return toMap(marketStub.orders(reqBuilder.build()));
    }

    @Override
    public Map<String, Object> order(OrderID id) throws Exception {
        var req = akash.market.v1beta5.QueryOrderRequest.newBuilder()
                .setId(akash.market.v1beta5.OrderID.newBuilder()
                        .setOwner(id.owner()).setDseq(id.dseq())
                        .setGseq(id.gseq()).setOseq(id.oseq()).build())
                .build();
        return toMap(marketStub.order(req));
    }

    @Override
    public Map<String, Object> bids(Map<String, String> filters) throws Exception {
        var reqBuilder = akash.market.v1beta5.QueryBidsRequest.newBuilder();
        if (filters != null) {
            var fb = akash.market.v1beta5.BidFilters.newBuilder();
            if (filters.containsKey("owner")) fb.setOwner(filters.get("owner"));
            if (filters.containsKey("dseq")) fb.setDseq(Long.parseLong(filters.get("dseq")));
            if (filters.containsKey("gseq")) fb.setGseq(Integer.parseInt(filters.get("gseq")));
            if (filters.containsKey("oseq")) fb.setOseq(Integer.parseInt(filters.get("oseq")));
            if (filters.containsKey("provider")) fb.setProvider(filters.get("provider"));
            if (filters.containsKey("state")) fb.setState(filters.get("state"));
            if (filters.containsKey("bseq")) fb.setBseq(Integer.parseInt(filters.get("bseq")));
            reqBuilder.setFilters(fb);
        }
        return toMap(marketStub.bids(reqBuilder.build()));
    }

    @Override
    public Map<String, Object> bid(BidID id) throws Exception {
        var req = akash.market.v1beta5.QueryBidRequest.newBuilder()
                .setId(akash.market.v1beta5.BidID.newBuilder()
                        .setOwner(id.owner()).setDseq(id.dseq())
                        .setGseq(id.gseq()).setOseq(id.oseq())
                        .setProvider(id.provider())
                        .setBseq(id.bseq()).build())
                .build();
        return toMap(marketStub.bid(req));
    }

    @Override
    public Map<String, Object> leases(Map<String, String> filters) throws Exception {
        var reqBuilder = akash.market.v1beta5.QueryLeasesRequest.newBuilder();
        if (filters != null) {
            var fb = akash.market.v1beta5.LeaseFilters.newBuilder();
            if (filters.containsKey("owner")) fb.setOwner(filters.get("owner"));
            if (filters.containsKey("dseq")) fb.setDseq(Long.parseLong(filters.get("dseq")));
            if (filters.containsKey("gseq")) fb.setGseq(Integer.parseInt(filters.get("gseq")));
            if (filters.containsKey("oseq")) fb.setOseq(Integer.parseInt(filters.get("oseq")));
            if (filters.containsKey("provider")) fb.setProvider(filters.get("provider"));
            if (filters.containsKey("state")) fb.setState(filters.get("state"));
            reqBuilder.setFilters(fb);
        }
        return toMap(marketStub.leases(reqBuilder.build()));
    }

    @Override
    public Map<String, Object> lease(LeaseID id) throws Exception {
        var req = akash.market.v1beta5.QueryLeaseRequest.newBuilder()
                .setId(akash.market.v1beta5.LeaseID.newBuilder()
                        .setOwner(id.owner()).setDseq(id.dseq())
                        .setGseq(id.gseq()).setOseq(id.oseq())
                        .setProvider(id.provider()).build())
                .build();
        return toMap(marketStub.lease(req));
    }

    @Override
    public Map<String, Object> providers(Map<String, String> filters) throws Exception {
        return toMap(providerStub.providers(
                akash.provider.v1beta4.QueryProvidersRequest.newBuilder().build()));
    }

    @Override
    public Map<String, Object> provider(String providerAddress) throws Exception {
        return toMap(providerStub.provider(
                akash.provider.v1beta4.QueryProviderRequest.newBuilder()
                        .setOwner(providerAddress).build()));
    }

    @Override
    public Map<String, Object> allProvidersAttributes() throws Exception {
        return toMap(auditStub.allProvidersAttributes(
                akash.audit.v1.QueryAllProvidersAttributesRequest.newBuilder().build()));
    }

    @Override
    public Map<String, Object> providerAttributes(String providerAddress) throws Exception {
        return toMap(auditStub.providerAttributes(
                akash.audit.v1.QueryProviderAttributesRequest.newBuilder()
                        .setOwner(providerAddress).build()));
    }

    @Override
    public Map<String, Object> providerAuditorAttributes(String providerAddress, String auditorAddress) throws Exception {
        return toMap(auditStub.providerAuditorAttributes(
                akash.audit.v1.QueryProviderAuditorRequest.newBuilder()
                        .setOwner(providerAddress).setAuditor(auditorAddress).build()));
    }

    @Override
    public Map<String, Object> auditorAttributes(String auditorAddress) throws Exception {
        return toMap(auditStub.auditorAttributes(
                akash.audit.v1.QueryAuditorAttributesRequest.newBuilder()
                        .setAuditor(auditorAddress).build()));
    }

    @Override
    public Map<String, Object> certificates(Map<String, String> filters) throws Exception {
        var reqBuilder = akash.cert.v1.QueryCertificatesRequest.newBuilder();
        if (filters != null) {
            var fb = akash.cert.v1.CertificateFilter.newBuilder();
            if (filters.containsKey("owner")) fb.setOwner(filters.get("owner"));
            if (filters.containsKey("state")) fb.setState(filters.get("state"));
            reqBuilder.setFilter(fb);
        }
        return toMap(certStub.certificates(reqBuilder.build()));
    }

    @Override
    public CosmosQueryModules cosmos() {
        return new GrpcCosmosModules();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(com.google.protobuf.MessageOrBuilder msg) throws Exception {
        String json = jsonPrinter.print(msg);
        return mapper.readValue(json, Map.class);
    }

    private class GrpcCosmosModules implements CosmosQueryModules {
        @Override
        public Map<String, Object> authAccount(String address) throws Exception {
            return toMap(authStub.account(
                    cosmos.auth.v1beta1.QueryAccountRequest.newBuilder()
                            .setAddress(address).build()));
        }

        @Override
        public Map<String, Object> bankBalance(String address, String denom) throws Exception {
            return toMap(bankStub.balance(
                    cosmos.bank.v1beta1.QueryBalanceRequest.newBuilder()
                            .setAddress(address).setDenom(denom).build()));
        }

        @Override
        public Map<String, Object> bankAllBalances(String address) throws Exception {
            return toMap(bankStub.allBalances(
                    cosmos.bank.v1beta1.QueryAllBalancesRequest.newBuilder()
                            .setAddress(address).build()));
        }

        @Override
        public Map<String, Object> stakingDelegations(String delegator) throws Exception {
            throw new UnsupportedOperationException("staking queries require cosmos/staking proto definitions");
        }

        @Override
        public Map<String, Object> govProposal(long proposalId) throws Exception {
            throw new UnsupportedOperationException("gov queries require cosmos/gov proto definitions");
        }
    }
}
