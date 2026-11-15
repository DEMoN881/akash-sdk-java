package network.akash.sdk.cosmos;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import cosmos.base.v1beta1.CoinOuterClass;
import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.DeploymentMessages;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TxSignerTest {

    private static final AkashKeyPair KEY_PAIR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e");

    @Test
    void buildAndSignProducesTxRaw() {
        DeploymentID did = new DeploymentID(KEY_PAIR.getAddress().toBech32(), 1);
        var closeMsg = new DeploymentMessages.MsgCloseDeployment(did);

        Any anyMsg = Any.newBuilder()
                .setTypeUrl("/akash.deployment.v1beta3.MsgCloseDeployment")
                .setValue(ByteString.copyFrom(closeMsg.getSignBytes()))
                .build();

        byte[] txRaw = TxSigner.buildAndSign(
                KEY_PAIR,
                "akashnet-2",
                0, 0,
                List.of(anyMsg),
                200000,
                List.of(CoinOuterClass.Coin.newBuilder().setDenom("uakt").setAmount("5000").build()),
                "",
                null
        );

        assertNotNull(txRaw);
        assertTrue(txRaw.length > 0);
    }

    @Test
    void signIsDeterministic() {
        DeploymentID did = new DeploymentID(KEY_PAIR.getAddress().toBech32(), 1);
        var closeMsg = new DeploymentMessages.MsgCloseDeployment(did);

        Any anyMsg = Any.newBuilder()
                .setTypeUrl("/akash.deployment.v1beta3.MsgCloseDeployment")
                .setValue(ByteString.copyFrom(closeMsg.getSignBytes()))
                .build();

        byte[] tx1 = TxSigner.buildAndSign(KEY_PAIR, "akashnet-2", 0, 0,
                List.of(anyMsg), 200000, List.of(), "", null);
        byte[] tx2 = TxSigner.buildAndSign(KEY_PAIR, "akashnet-2", 0, 0,
                List.of(anyMsg), 200000, List.of(), "", null);

        assertArrayEquals(tx1, tx2);
    }

    @Test
    void txRawIsValidBase64() {
        DeploymentID did = new DeploymentID(KEY_PAIR.getAddress().toBech32(), 1);
        var closeMsg = new DeploymentMessages.MsgCloseDeployment(did);

        Any anyMsg = Any.newBuilder()
                .setTypeUrl("/akash.deployment.v1beta3.MsgCloseDeployment")
                .setValue(ByteString.copyFrom(closeMsg.getSignBytes()))
                .build();

        byte[] txRaw = TxSigner.buildAndSign(KEY_PAIR, "akashnet-2", 5, 10,
                List.of(anyMsg), 100000, List.of(), "test memo", 50000L);

        String b64 = Base64.getEncoder().encodeToString(txRaw);
        assertNotNull(b64);
        assertFalse(b64.isEmpty());
        assertArrayEquals(txRaw, Base64.getDecoder().decode(b64));
    }

    @Test
    void differentSequenceProducesDifferentTx() {
        DeploymentID did = new DeploymentID(KEY_PAIR.getAddress().toBech32(), 1);
        var closeMsg = new DeploymentMessages.MsgCloseDeployment(did);

        Any anyMsg = Any.newBuilder()
                .setTypeUrl("/akash.deployment.v1beta3.MsgCloseDeployment")
                .setValue(ByteString.copyFrom(closeMsg.getSignBytes()))
                .build();

        byte[] tx1 = TxSigner.buildAndSign(KEY_PAIR, "akashnet-2", 0, 0,
                List.of(anyMsg), 200000, List.of(), "", null);
        byte[] tx2 = TxSigner.buildAndSign(KEY_PAIR, "akashnet-2", 0, 1,
                List.of(anyMsg), 200000, List.of(), "", null);

        assertFalse(java.util.Arrays.equals(tx1, tx2));
    }

    @Test
    void multipleMessages() {
        String addr = KEY_PAIR.getAddress().toBech32();
        DeploymentID did = new DeploymentID(addr, 1);
        var close1 = new DeploymentMessages.MsgCloseDeployment(did);
        var close2 = new DeploymentMessages.MsgCloseDeployment(new DeploymentID(addr, 2));

        List<Any> msgs = List.of(
                Any.newBuilder()
                        .setTypeUrl("/akash.deployment.v1beta3.MsgCloseDeployment")
                        .setValue(ByteString.copyFrom(close1.getSignBytes())).build(),
                Any.newBuilder()
                        .setTypeUrl("/akash.deployment.v1beta3.MsgCloseDeployment")
                        .setValue(ByteString.copyFrom(close2.getSignBytes())).build()
        );

        byte[] txRaw = TxSigner.buildAndSign(KEY_PAIR, "akashnet-2", 0, 0,
                msgs, 400000, List.of(), "", null);
        assertTrue(txRaw.length > 0);
    }
}
