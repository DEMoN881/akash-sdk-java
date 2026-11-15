package network.akash.sdk.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class Bip32 {

    private static final BigInteger SECP256K1_N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    private static final long HARDENED = 0x80000000L;

    public static final int[] COSMOS_PATH = {44, 118, 0, 0, 0};
    public static final int COSMOS_HARDENED_DEPTH = 3;

    private Bip32() {}

    public static byte[] deriveCosmosPrivateKey(byte[] seed) {
        return derive(seed, COSMOS_PATH, COSMOS_HARDENED_DEPTH);
    }

    public static byte[] derive(byte[] seed, int[] path, int hardenedDepth) {
        byte[] hmac = hmacSha512("Bitcoin seed".getBytes(), seed);
        byte[] key = Arrays.copyOfRange(hmac, 0, 32);
        byte[] chainCode = Arrays.copyOfRange(hmac, 32, 64);

        for (int i = 0; i < path.length; i++) {
            long index = Integer.toUnsignedLong(path[i]);
            if (i < hardenedDepth) {
                index |= HARDENED;
            }
            byte[] data;
            if ((index & HARDENED) != 0) {
                data = new byte[37];
                data[0] = 0x00;
                System.arraycopy(padTo32(key), 0, data, 1, 32);
                ByteBuffer.wrap(data, 33, 4).putInt((int) index);
            } else {
                byte[] pubKey = publicKeyFromPrivate(key);
                data = new byte[37];
                System.arraycopy(pubKey, 0, data, 0, 33);
                ByteBuffer.wrap(data, 33, 4).putInt((int) index);
            }
            hmac = hmacSha512(chainCode, data);
            BigInteger ki = new BigInteger(1, Arrays.copyOfRange(hmac, 0, 32));
            ki = ki.add(new BigInteger(1, key)).mod(SECP256K1_N);
            key = padTo32(ki.toByteArray());
            chainCode = Arrays.copyOfRange(hmac, 32, 64);
        }
        return key;
    }

    private static byte[] publicKeyFromPrivate(byte[] privKey) {
        var spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
        var point = spec.getG().multiply(new BigInteger(1, privKey)).normalize();
        return point.getEncoded(true);
    }

    private static byte[] hmacSha512(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key, "HmacSHA512"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA512 failed", e);
        }
    }

    private static byte[] padTo32(byte[] src) {
        if (src.length == 32) return src;
        byte[] out = new byte[32];
        if (src.length > 32) {
            System.arraycopy(src, src.length - 32, out, 0, 32);
        } else {
            System.arraycopy(src, 0, out, 32 - src.length, src.length);
        }
        return out;
    }
}
