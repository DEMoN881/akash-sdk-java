package network.akash.sdk.types.deployment;

import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentIDTest {

    private static final String ADDR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();

    @Test
    void validateValid() {
        assertDoesNotThrow(() -> new DeploymentID(ADDR, 1).validate());
    }

    @Test
    void validateZeroDseqThrows() {
        assertThrows(IllegalArgumentException.class, () -> new DeploymentID(ADDR, 0).validate());
    }

    @Test
    void validateInvalidOwnerThrows() {
        assertThrows(IllegalArgumentException.class, () -> new DeploymentID("bad", 1).validate());
    }

    @Test
    void parseRoundtrip() {
        DeploymentID id = new DeploymentID(ADDR, 42);
        DeploymentID parsed = DeploymentID.parse(id.toString());
        assertEquals(id.owner(), parsed.owner());
        assertEquals(id.dseq(), parsed.dseq());
    }

    @Test
    void parseInvalidPathThrows() {
        assertThrows(IllegalArgumentException.class, () -> DeploymentID.parse("only-one-part"));
    }

    @Test
    void toStringFormat() {
        DeploymentID id = new DeploymentID(ADDR, 100);
        assertEquals(ADDR + "/100", id.toString());
    }

    @Test
    void getOwnerAddress() {
        DeploymentID id = new DeploymentID(ADDR, 1);
        assertEquals(ADDR, id.getOwnerAddress().toBech32());
    }

    @Test
    void nullOwnerThrows() {
        assertThrows(NullPointerException.class, () -> new DeploymentID(null, 1));
    }
}
