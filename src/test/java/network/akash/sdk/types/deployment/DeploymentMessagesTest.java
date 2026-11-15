package network.akash.sdk.types.deployment;

import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentMessagesTest {

    private static final String ADDR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();
    private static final DeploymentID DID = new DeploymentID(ADDR, 1);
    private static final GroupID GID = new GroupID(ADDR, 1, 1);
    private static final byte[] VERSION = new byte[32];

    static {
        VERSION[0] = 1;
    }

    private static byte[] group() {
        return akash.deployment.v1beta4.GroupSpec.newBuilder().setName("dcloud").build().toByteArray();
    }

    @Test
    void createDeploymentValidation() {
        var msg = new DeploymentMessages.MsgCreateDeployment(
                DID, List.of(group()), VERSION, Coin.of("uakt", 5000000));
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("deployment", msg.getRoute());
        assertEquals("create-deployment", msg.getType());
        assertEquals(1, msg.getSigners().size());
        assertEquals(ADDR, msg.getSigners().get(0).toBech32());
    }

    @Test
    void createDeploymentSignBytesNonEmpty() {
        var msg = new DeploymentMessages.MsgCreateDeployment(
                DID, List.of(group()), VERSION, Coin.of("uakt", 5000000));
        byte[] signBytes = msg.getSignBytes();
        assertTrue(signBytes.length > 0);
    }

    @Test
    void createDeploymentEmptyGroupsThrows() {
        var msg = new DeploymentMessages.MsgCreateDeployment(
                DID, List.of(), VERSION, Coin.of("uakt", 5000000));
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void createDeploymentBadVersionLengthThrows() {
        var msg = new DeploymentMessages.MsgCreateDeployment(
                DID, List.of(group()), new byte[16], Coin.of("uakt", 5000000));
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void createDeploymentEmptyVersionThrows() {
        var msg = new DeploymentMessages.MsgCreateDeployment(
                DID, List.of(group()), new byte[0], Coin.of("uakt", 5000000));
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void updateDeploymentValid() {
        var msg = new DeploymentMessages.MsgUpdateDeployment(DID, VERSION);
        assertDoesNotThrow(msg::validateBasic);
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void closeDeploymentValid() {
        var msg = new DeploymentMessages.MsgCloseDeployment(DID);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("close-deployment", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void createDeploymentEncodesGroupsHashAndBalanceDeposit() throws Exception {
        var msg = new DeploymentMessages.MsgCreateDeployment(
                DID, List.of(group()), VERSION, Coin.of("uakt", 5000000));
        var parsed = akash.deployment.v1beta4.MsgCreateDeployment.parseFrom(msg.getSignBytes());
        assertEquals("dcloud", parsed.getGroups(0).getName());
        assertEquals(1, parsed.getHash().byteAt(0));
        assertEquals("uakt", parsed.getDeposit().getAmount().getDenom());
        assertEquals("5000000", parsed.getDeposit().getAmount().getAmount());
        assertEquals(akash.base.deposit.v1.Source.balance, parsed.getDeposit().getSources(0));
    }

    @Test
    void closeGroupValid() {
        var msg = new DeploymentMessages.MsgCloseGroup(GID);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("close-group", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void pauseGroupValid() {
        var msg = new DeploymentMessages.MsgPauseGroup(GID);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("pause-group", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void startGroupValid() {
        var msg = new DeploymentMessages.MsgStartGroup(GID);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("start-group", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void signBytesAreDeterministic() {
        var msg = new DeploymentMessages.MsgCloseDeployment(DID);
        assertArrayEquals(msg.getSignBytes(), msg.getSignBytes());
    }
}
