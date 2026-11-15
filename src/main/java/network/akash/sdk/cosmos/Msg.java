package network.akash.sdk.cosmos;

import java.util.List;

/**
 * Cosmos SDK Msg interface.
 * Port of cosmos-sdk/types.Msg
 */
public interface Msg {

    String getRoute();

    String getType();

    void validateBasic() throws Exception;

    List<AccAddress> getSigners();

    byte[] getSignBytes();
}
