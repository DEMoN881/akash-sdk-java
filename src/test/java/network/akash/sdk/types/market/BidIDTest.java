package network.akash.sdk.types.market;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BidIDTest {

    private static final String OWNER = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();
    private static final String PROVIDER = AkashKeyPair.fromHex(
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    ).getAddress().toBech32();

    @Test
    void makeFromOrderID() {
        OrderID oid = new OrderID(OWNER, 10, 2, 1);
        BidID bid = BidID.make(oid, AccAddress.fromBech32(PROVIDER));
        assertEquals(PROVIDER, bid.provider());
    }

    @Test
    void orderIDExtraction() {
        BidID bid = new BidID(OWNER, 10, 2, 1, PROVIDER);
        OrderID oid = bid.orderID();
        assertEquals(1, oid.oseq());
    }

    @Test
    void leaseIDConversion() {
        BidID bid = new BidID(OWNER, 10, 2, 1, PROVIDER);
        LeaseID lease = bid.leaseID();
        assertEquals(bid.owner(), lease.owner());
        assertEquals(bid.provider(), lease.provider());
    }

    @Test
    void validateValid() {
        assertDoesNotThrow(() -> new BidID(OWNER, 1, 1, 1, PROVIDER).validate());
    }

    @Test
    void validateSelfBidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new BidID(OWNER, 1, 1, 1, OWNER).validate());
    }

    @Test
    void toStringFormat() {
        BidID bid = new BidID(OWNER, 10, 2, 1, PROVIDER);
        String s = bid.toString();
        assertTrue(s.contains(OWNER));
        assertTrue(s.contains(PROVIDER));
    }
}
