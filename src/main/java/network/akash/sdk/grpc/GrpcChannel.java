package network.akash.sdk.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Managed gRPC channel for connecting to an Akash node.
 * Wraps io.grpc.ManagedChannel with connection lifecycle.
 */
public final class GrpcChannel implements AutoCloseable {

    private final ManagedChannel channel;
    private final String target;

    private GrpcChannel(ManagedChannel channel, String target) {
        this.channel = channel;
        this.target = target;
    }

    public static GrpcChannel connect(String host, int port) {
        return connect(host, port, false);
    }

    public static GrpcChannel connect(String host, int port, boolean useTls) {
        String target = host + ":" + port;
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
        if (!useTls) {
            builder.usePlaintext();
        }
        ManagedChannel channel = builder.build();
        return new GrpcChannel(channel, target);
    }

    public static GrpcChannel connect(String target) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        return new GrpcChannel(channel, target);
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public void close() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
