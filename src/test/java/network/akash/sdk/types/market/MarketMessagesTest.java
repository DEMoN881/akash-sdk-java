package network.akash.sdk.types.market;

import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.DecCoin;
import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarketMessagesTest {

    private static final String OWNER = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();
    private static final String PROVIDER = AkashKeyPair.fromHex(
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    ).getAddress().toBech32();
    private static final OrderID ORDER = new OrderID(OWNER, 1, 1, 1);
    private static final BidID BID = new BidID(OWNER, 1, 1, 1, PROVIDER);
    private static final LeaseID LEASE = new LeaseID(OWNER, 1, 1, 1, PROVIDER);

    @Test
    void createBidValid() {
        var msg = new MarketMessages.MsgCreateBid(
                ORDER, PROVIDER, DecCoin.of("uakt", "1.0"), Coin.of("uakt", 5000000));
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("market", msg.getRoute());
        assertEquals("create-bid", msg.getType());
        assertEquals(PROVIDER, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void createBidSelfBidThrows() {
        var msg = new MarketMessages.MsgCreateBid(
                ORDER, OWNER, DecCoin.of("uakt", "1.0"), Coin.of("uakt", 5000000));
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void createBidZeroPriceThrows() {
        var msg = new MarketMessages.MsgCreateBid(
                ORDER, PROVIDER, DecCoin.of("uakt", "0"), Coin.of("uakt", 5000000));
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void closeBidValid() {
        var msg = new MarketMessages.MsgCloseBid(BID);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals(PROVIDER, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void createLeaseValid() {
        var msg = new MarketMessages.MsgCreateLease(BID);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals(OWNER, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void withdrawLeaseValid() {
        var msg = new MarketMessages.MsgWithdrawLease(LEASE);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals(PROVIDER, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void closeLeaseValid() {
        var msg = new MarketMessages.MsgCloseLease(LEASE);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals(OWNER, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void signBytesAreDeterministic() {
        var msg = new MarketMessages.MsgCloseBid(BID);
        assertArrayEquals(msg.getSignBytes(), msg.getSignBytes());
    }
}
