package network.akash.sdk.client;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Msg;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeUrlTest {

    @Test
    void typeUrlsMatchNode22() {
        assertEquals("/akash.cert.v1.MsgCreateCertificate", SerialBroadcaster.typeUrl(msg("cert", "create-certificate")));
        assertEquals("/akash.deployment.v1beta4.MsgCreateDeployment", SerialBroadcaster.typeUrl(msg("deployment", "create-deployment")));
        assertEquals("/akash.market.v1beta5.MsgCreateLease", SerialBroadcaster.typeUrl(msg("market", "create-lease")));
        assertEquals("/akash.provider.v1beta4.MsgCreateProvider", SerialBroadcaster.typeUrl(msg("provider", "create-provider")));
        assertEquals("/akash.audit.v1.MsgSignProviderAttributes", SerialBroadcaster.typeUrl(msg("audit", "sign-provider-attributes")));
    }

    private static Msg msg(String route, String type) {
        return new Msg() {
            @Override public String getRoute() { return route; }
            @Override public String getType() { return type; }
            @Override public void validateBasic() {}
            @Override public List<AccAddress> getSigners() { return List.of(); }
            @Override public byte[] getSignBytes() { return new byte[0]; }
        };
    }
}
