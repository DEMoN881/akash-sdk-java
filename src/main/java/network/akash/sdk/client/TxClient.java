package network.akash.sdk.client;

import network.akash.sdk.cosmos.Msg;
import java.util.List;

/**
 * Transaction client interface — wraps transaction broadcasting.
 * Port of akash-api/go/node/client/v1beta2.TxClient
 */
public interface TxClient {

    BroadcastResult broadcast(List<Msg> msgs, BroadcastOptions... opts) throws Exception;

    record BroadcastResult(
            String txHash,
            long height,
            int code,
            String rawLog,
            String codespace,
            long gasWanted,
            long gasUsed
    ) {
        public boolean isSuccess() {
            return code == 0;
        }
    }
}
