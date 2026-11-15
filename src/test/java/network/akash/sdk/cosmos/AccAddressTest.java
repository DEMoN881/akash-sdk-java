package network.akash.sdk.cosmos;

import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccAddressTest {

    private static AccAddress validAddress() {
        return AkashKeyPair.fromHex(
                "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
        ).getAddress();
    }

    @Test
    void fromBech32Valid() {
        AccAddress addr = validAddress();
        String bech = addr.toBech32();
        AccAddress parsed = AccAddress.fromBech32(bech);
        assertEquals(addr, parsed);
    }

    @Test
    void fromBech32InvalidPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> AccAddress.fromBech32("cosmos1qypqxpq9qcrsszg2pvxq6rs0zqg3yyc5lzv7xu"));
    }

    @Test
    void fromBech32Empty() {
        assertThrows(IllegalArgumentException.class, () -> AccAddress.fromBech32(""));
    }

    @Test
    void fromBech32Null() {
        assertThrows(IllegalArgumentException.class, () -> AccAddress.fromBech32(null));
    }

    @Test
    void fromBytesDefaultPrefix() {
        byte[] raw = new byte[20];
        raw[0] = 1;
        AccAddress addr = AccAddress.fromBytes(raw);
        assertTrue(addr.toBech32().startsWith("akash1"));
    }

    @Test
    void fromBytesWithHrp() {
        byte[] raw = new byte[20];
        AccAddress addr = AccAddress.fromBytes(raw, "akash");
        assertTrue(addr.toBech32().startsWith("akash1"));
    }

    @Test
    void getBytesRoundtrip() {
        AccAddress addr = validAddress();
        byte[] bytes = addr.getBytes();
        AccAddress reconstructed = AccAddress.fromBytes(bytes, "akash");
        assertEquals(addr, reconstructed);
    }

    @Test
    void toStringReturnsBech32() {
        AccAddress addr = validAddress();
        assertEquals(addr.toBech32(), addr.toString());
    }

    @Test
    void equalsSameBytes() {
        byte[] raw = new byte[20];
        raw[19] = 42;
        AccAddress a = AccAddress.fromBytes(raw, "akash");
        AccAddress b = AccAddress.fromBytes(raw, "akash");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualDifferentBytes() {
        byte[] raw1 = new byte[20];
        raw1[0] = 1;
        byte[] raw2 = new byte[20];
        raw2[0] = 2;
        assertNotEquals(AccAddress.fromBytes(raw1), AccAddress.fromBytes(raw2));
    }
}
