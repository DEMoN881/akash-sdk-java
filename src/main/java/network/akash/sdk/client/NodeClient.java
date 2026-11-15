package network.akash.sdk.client;

/**
 * Node client interface — provides sync info from the connected Akash node.
 * Port of akash-api/go/node/client/v1beta2.NodeClient
 */
public interface NodeClient {

    SyncInfo syncInfo() throws Exception;

    record SyncInfo(
            String latestBlockHash,
            long latestBlockHeight,
            String latestBlockTime,
            boolean catchingUp
    ) {}
}
