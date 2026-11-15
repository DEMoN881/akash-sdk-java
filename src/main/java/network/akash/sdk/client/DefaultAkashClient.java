package network.akash.sdk.client;

import io.grpc.ManagedChannel;
import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.grpc.GrpcChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default AkashClient implementation — wires together Query, Tx, and Node sub-clients.
 * Port of akash-api/go/node/client/v1beta2.client
 *
 * Usage:
 * <pre>
 * AkashClient client = DefaultAkashClient.builder()
 *     .rpcEndpoint("http://rpc.akash.network:26657")
 *     .restEndpoint("http://api.akash.network:1317/")
 *     .chainId("akashnet-2")
 *     .keyPair(AkashKeyPair.fromHex("..."))
 *     .build();
 *
 * // Query deployments
 * var deployments = client.query().deployments(Map.of("filters.owner", ownerAddr));
 *
 * // Broadcast a close-deployment transaction
 * var result = client.tx().broadcast(
 *     List.of(new DeploymentMessages.MsgCloseDeployment(deploymentId)),
 *     BroadcastOptions.withGas(200000, false),
 *     BroadcastOptions.withFees("5000uakt")
 * );
 *
 * // Check node status
 * var syncInfo = client.node().syncInfo();
 * </pre>
 */
public final class DefaultAkashClient implements AkashClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultAkashClient.class);

    private final QueryClient queryClient;
    private final TxClient txClient;
    private final NodeClient nodeClient;
    private final String chainId;

    private DefaultAkashClient(QueryClient queryClient, TxClient txClient, NodeClient nodeClient, String chainId) {
        this.queryClient = queryClient;
        this.txClient = txClient;
        this.nodeClient = nodeClient;
        this.chainId = chainId;
    }

    @Override
    public QueryClient query() {
        return queryClient;
    }

    @Override
    public TxClient tx() {
        return txClient;
    }

    @Override
    public NodeClient node() {
        return nodeClient;
    }

    @Override
    public String getChainId() {
        return chainId;
    }

    @Override
    public void close() throws Exception {
        if (txClient instanceof AutoCloseable ac) {
            ac.close();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String rpcEndpoint;
        private String restEndpoint;
        private String grpcHost;
        private int grpcPort = 9090;
        private boolean grpcTls = false;
        private String chainId = "akashnet-2";
        private AkashKeyPair keyPair;

        private Builder() {}

        public Builder rpcEndpoint(String rpcEndpoint) {
            this.rpcEndpoint = rpcEndpoint;
            return this;
        }

        public Builder restEndpoint(String restEndpoint) {
            this.restEndpoint = restEndpoint;
            return this;
        }

        public Builder grpcEndpoint(String host, int port) {
            this.grpcHost = host;
            this.grpcPort = port;
            return this;
        }

        public Builder grpcEndpoint(String host, int port, boolean useTls) {
            this.grpcHost = host;
            this.grpcPort = port;
            this.grpcTls = useTls;
            return this;
        }

        public Builder chainId(String chainId) {
            this.chainId = chainId;
            return this;
        }

        public Builder keyPair(AkashKeyPair keyPair) {
            this.keyPair = keyPair;
            return this;
        }

        public DefaultAkashClient build() {
            if (rpcEndpoint == null) throw new IllegalStateException("rpcEndpoint is required");
            if (restEndpoint == null) throw new IllegalStateException("restEndpoint is required");

            String rpc = rpcEndpoint.endsWith("/") ? rpcEndpoint : rpcEndpoint + "/";
            String rest = restEndpoint.endsWith("/") ? restEndpoint : restEndpoint + "/";

            NodeClient nodeClient = new DefaultNodeClient(rpc);

            QueryClient queryClient;
            if (grpcHost != null) {
                GrpcChannel grpcChannel = GrpcChannel.connect(grpcHost, grpcPort, grpcTls);
                queryClient = new GrpcQueryClient(grpcChannel.getChannel());
                log.info("Using gRPC query client: {}:{}", grpcHost, grpcPort);
            } else {
                queryClient = new DefaultQueryClient(rest);
                log.info("Using REST/LCD query client: {}", rest);
            }

            TxClient txClient;
            if (keyPair != null) {
                txClient = new SerialBroadcaster(rpc, rest, keyPair, chainId, nodeClient);
            } else {
                txClient = (msgs, opts) -> {
                    throw new UnsupportedOperationException("tx client requires a key pair");
                };
            }

            log.info("Akash client initialized: rpc={}, chainId={}", rpc, chainId);
            return new DefaultAkashClient(queryClient, txClient, nodeClient, chainId);
        }
    }
}
