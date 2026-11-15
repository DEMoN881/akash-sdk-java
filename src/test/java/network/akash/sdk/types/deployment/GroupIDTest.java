package network.akash.sdk.types.deployment;

import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupIDTest {

    private static final String ADDR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();

    @Test
    void makeFromDeploymentID() {
        DeploymentID did = new DeploymentID(ADDR, 10);
        GroupID gid = GroupID.make(did, 1);
        assertEquals(ADDR, gid.owner());
        assertEquals(10, gid.dseq());
        assertEquals(1, gid.gseq());
    }

    @Test
    void deploymentIDExtraction() {
        GroupID gid = new GroupID(ADDR, 10, 2);
        DeploymentID did = gid.deploymentID();
        assertEquals(ADDR, did.owner());
        assertEquals(10, did.dseq());
    }

    @Test
    void validateValid() {
        assertDoesNotThrow(() -> new GroupID(ADDR, 1, 1).validate());
    }

    @Test
    void validateZeroGseqThrows() {
        assertThrows(IllegalArgumentException.class, () -> new GroupID(ADDR, 1, 0).validate());
    }

    @Test
    void toStringFormat() {
        GroupID gid = new GroupID(ADDR, 10, 3);
        assertEquals(ADDR + "/10/3", gid.toString());
    }

    @Test
    void nullOwnerThrows() {
        assertThrows(NullPointerException.class, () -> new GroupID(null, 1, 1));
    }
}
