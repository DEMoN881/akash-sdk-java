package network.akash.sdk.crypto;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;

/**
 * Bech32 encoding/decoding.
 * Port of btcutil bech32 (used by Cosmos SDK for address encoding).
 */
public final class Bech32 {

    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final int[] GENERATOR = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};

    private Bech32() {}

    public record Decoded(String hrp, byte[] data) {}

    public static String encode(String hrp, byte[] data) {
        byte[] converted = convertBits(data, 8, 5, true);
        return bech32Encode(hrp, converted);
    }

    public static Decoded decode(String bech) {
        if (bech.length() < 8) throw new IllegalArgumentException("too short");
        if (bech.length() > 90) throw new IllegalArgumentException("too long");

        String lower = bech.toLowerCase(Locale.ROOT);
        if (!lower.equals(bech) && !bech.toUpperCase(Locale.ROOT).equals(bech)) {
            throw new IllegalArgumentException("mixed case");
        }
        bech = lower;

        int pos = bech.lastIndexOf('1');
        if (pos < 1 || pos + 7 > bech.length()) {
            throw new IllegalArgumentException("invalid separator position");
        }

        String hrp = bech.substring(0, pos);
        byte[] dataAndChecksum = new byte[bech.length() - pos - 1];
        for (int i = 0; i < dataAndChecksum.length; i++) {
            int idx = CHARSET.indexOf(bech.charAt(pos + 1 + i));
            if (idx < 0) throw new IllegalArgumentException("invalid character: " + bech.charAt(pos + 1 + i));
            dataAndChecksum[i] = (byte) idx;
        }

        if (!verifyChecksum(hrp, dataAndChecksum)) {
            throw new IllegalArgumentException("invalid checksum");
        }

        byte[] data5bit = Arrays.copyOfRange(dataAndChecksum, 0, dataAndChecksum.length - 6);
        byte[] data = convertBits(data5bit, 5, 8, false);
        return new Decoded(hrp, data);
    }

    private static String bech32Encode(String hrp, byte[] data5bit) {
        byte[] checksum = createChecksum(hrp, data5bit);
        StringBuilder sb = new StringBuilder(hrp.length() + 1 + data5bit.length + checksum.length);
        sb.append(hrp).append('1');
        for (byte b : data5bit) sb.append(CHARSET.charAt(b));
        for (byte b : checksum) sb.append(CHARSET.charAt(b));
        return sb.toString();
    }

    private static int polymod(byte[] values) {
        int chk = 1;
        for (byte v : values) {
            int top = chk >>> 25;
            chk = ((chk & 0x1ffffff) << 5) ^ (v & 0xff);
            for (int i = 0; i < 5; i++) {
                if (((top >>> i) & 1) != 0) chk ^= GENERATOR[i];
            }
        }
        return chk;
    }

    private static byte[] hrpExpand(String hrp) {
        byte[] ret = new byte[hrp.length() * 2 + 1];
        for (int i = 0; i < hrp.length(); i++) {
            ret[i] = (byte) (hrp.charAt(i) >>> 5);
            ret[i + hrp.length() + 1] = (byte) (hrp.charAt(i) & 0x1f);
        }
        return ret;
    }

    private static boolean verifyChecksum(String hrp, byte[] data) {
        byte[] expanded = hrpExpand(hrp);
        byte[] combined = new byte[expanded.length + data.length];
        System.arraycopy(expanded, 0, combined, 0, expanded.length);
        System.arraycopy(data, 0, combined, expanded.length, data.length);
        return polymod(combined) == 1;
    }

    private static byte[] createChecksum(String hrp, byte[] data) {
        byte[] expanded = hrpExpand(hrp);
        byte[] values = new byte[expanded.length + data.length + 6];
        System.arraycopy(expanded, 0, values, 0, expanded.length);
        System.arraycopy(data, 0, values, expanded.length, data.length);
        int pm = polymod(values) ^ 1;
        byte[] ret = new byte[6];
        for (int i = 0; i < 6; i++) {
            ret[i] = (byte) ((pm >>> (5 * (5 - i))) & 31);
        }
        return ret;
    }

    static byte[] convertBits(byte[] data, int fromBits, int toBits, boolean pad) {
        int acc = 0;
        int bits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int maxv = (1 << toBits) - 1;
        for (byte b : data) {
            int value = b & 0xff;
            if (value >>> fromBits != 0) throw new IllegalArgumentException("invalid data range");
            acc = (acc << fromBits) | value;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                out.write((acc >>> bits) & maxv);
            }
        }
        if (pad) {
            if (bits > 0) out.write((acc << (toBits - bits)) & maxv);
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new IllegalArgumentException("invalid padding");
        }
        return out.toByteArray();
    }
}
