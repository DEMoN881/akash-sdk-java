package network.akash.sdk.client;

/**
 * Top-level Akash client interface — the umbrella that exposes Query, Tx, and Node sub-clients.
 * Port of akash-api/go/node/client/v1beta2.Client
 */
public interface AkashClient extends AutoCloseable {

    QueryClient query();

    TxClient tx();

    NodeClient node();

    String getChainId();
}
