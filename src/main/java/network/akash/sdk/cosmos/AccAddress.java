package network.akash.sdk.cosmos;

import network.akash.sdk.crypto.Bech32;
import java.util.Arrays;
import java.util.Objects;

/**
 * Cosmos SDK account address (bech32-encoded).
 * Port of cosmos-sdk/types.AccAddress
 */
public final class AccAddress {

    private final byte[] bytes;
    private final String bech32;

    private AccAddress(byte[] bytes, String bech32) {
        this.bytes = bytes;
        this.bech32 = bech32;
    }

    public static AccAddress fromBech32(String addr) {
        if (addr == null || addr.isEmpty()) {
            throw new IllegalArgumentException("empty address string");
        }
        Bech32.Decoded decoded = Bech32.decode(addr);
        if (!"akash".equals(decoded.hrp())) {
            throw new IllegalArgumentException("invalid bech32 prefix: expected 'akash', got '" + decoded.hrp() + "'");
        }
        return new AccAddress(decoded.data(), addr);
    }

    public static AccAddress fromBytes(byte[] raw, String hrp) {
        String encoded = Bech32.encode(hrp, raw);
        return new AccAddress(raw, encoded);
    }

    public static AccAddress fromBytes(byte[] raw) {
        return fromBytes(raw, "akash");
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String toBech32() {
        return bech32;
    }

    @Override
    public String toString() {
        return bech32;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccAddress that)) return false;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
