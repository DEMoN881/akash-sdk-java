package network.akash.sdk.types.market;

import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.types.deployment.GroupID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderIDTest {

    private static final String ADDR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();

    @Test
    void makeFromGroupID() {
        GroupID gid = new GroupID(ADDR, 10, 2);
        OrderID oid = OrderID.make(gid, 1);
        assertEquals(ADDR, oid.owner());
        assertEquals(10, oid.dseq());
        assertEquals(2, oid.gseq());
        assertEquals(1, oid.oseq());
    }

    @Test
    void groupIDExtraction() {
        OrderID oid = new OrderID(ADDR, 10, 2, 1);
        GroupID gid = oid.groupID();
        assertEquals(2, gid.gseq());
    }

    @Test
    void validateValid() {
        assertDoesNotThrow(() -> new OrderID(ADDR, 1, 1, 1).validate());
    }

    @Test
    void validateZeroOseqThrows() {
        assertThrows(IllegalArgumentException.class, () -> new OrderID(ADDR, 1, 1, 0).validate());
    }

    @Test
    void toStringFormat() {
        OrderID oid = new OrderID(ADDR, 10, 2, 3);
        assertEquals(ADDR + "/10/2/3", oid.toString());
    }
}
