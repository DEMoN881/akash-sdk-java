package network.akash.sdk.types.market;

import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeaseIDTest {

    private static final String OWNER = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();
    private static final String PROVIDER = AkashKeyPair.fromHex(
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    ).getAddress().toBech32();

    @Test
    void makeFromBidID() {
        BidID bid = new BidID(OWNER, 10, 2, 1, PROVIDER);
        LeaseID lease = LeaseID.make(bid);
        assertEquals(OWNER, lease.owner());
        assertEquals(10, lease.dseq());
        assertEquals(PROVIDER, lease.provider());
    }

    @Test
    void bidIDRoundtrip() {
        BidID original = new BidID(OWNER, 10, 2, 1, PROVIDER);
        LeaseID lease = LeaseID.make(original);
        BidID back = lease.bidID();
        assertEquals(original.owner(), back.owner());
        assertEquals(original.dseq(), back.dseq());
        assertEquals(original.provider(), back.provider());
    }

    @Test
    void deploymentIDChain() {
        LeaseID lease = new LeaseID(OWNER, 10, 2, 1, PROVIDER);
        assertEquals(OWNER, lease.deploymentID().owner());
        assertEquals(10, lease.deploymentID().dseq());
    }

    @Test
    void validate() {
        assertDoesNotThrow(() -> new LeaseID(OWNER, 1, 1, 1, PROVIDER).validate());
    }

    @Test
    void validateSelfBidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new LeaseID(OWNER, 1, 1, 1, OWNER).validate());
    }
}
