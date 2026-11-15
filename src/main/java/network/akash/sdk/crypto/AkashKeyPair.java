package network.akash.sdk.crypto;

import network.akash.sdk.cosmos.AccAddress;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Secp256k1 key pair for Cosmos/Akash transaction signing.
 * Port of cosmos-sdk/crypto/keyring key management.
 */
public final class AkashKeyPair {

    private static final ECNamedCurveParameterSpec CURVE_SPEC = ECNamedCurveTable.getParameterSpec("secp256k1");
    private static final ECDomainParameters DOMAIN = new ECDomainParameters(
            CURVE_SPEC.getCurve(), CURVE_SPEC.getG(), CURVE_SPEC.getN(), CURVE_SPEC.getH());

    private final BigInteger privateKey;
    private final byte[] publicKey;
    private final AccAddress address;

    public AkashKeyPair(byte[] privKeyBytes) {
        this.privateKey = new BigInteger(1, privKeyBytes);
        ECPoint pubPoint = CURVE_SPEC.getG().multiply(this.privateKey).normalize();
        this.publicKey = pubPoint.getEncoded(true);
        this.address = deriveAddress(this.publicKey, "akash");
    }

    public static AkashKeyPair fromPrivateKey(byte[] privKey) {
        return new AkashKeyPair(privKey);
    }

    public static AkashKeyPair fromHex(String hex) {
        byte[] bytes = hexToBytes(hex);
        return new AkashKeyPair(bytes);
    }

    public byte[] getPublicKey() {
        return Arrays.copyOf(publicKey, publicKey.length);
    }

    public AccAddress getAddress() {
        return address;
    }

    public byte[] sign(byte[] message) {
        byte[] hash = sha256(message);
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(privateKey, DOMAIN));
        BigInteger[] sig = signer.generateSignature(hash);
        BigInteger r = sig[0];
        BigInteger s = sig[1];
        BigInteger halfN = DOMAIN.getN().shiftRight(1);
        if (s.compareTo(halfN) > 0) {
            s = DOMAIN.getN().subtract(s);
        }
        byte[] sigBytes = new byte[64];
        byte[] rBytes = toFixedLength(r.toByteArray(), 32);
        byte[] sBytes = toFixedLength(s.toByteArray(), 32);
        System.arraycopy(rBytes, 0, sigBytes, 0, 32);
        System.arraycopy(sBytes, 0, sigBytes, 32, 32);
        return sigBytes;
    }

    private static AccAddress deriveAddress(byte[] pubKey, String hrp) {
        byte[] sha = sha256(pubKey);
        byte[] ripemd = ripemd160(sha);
        return AccAddress.fromBytes(ripemd, hrp);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] ripemd160(byte[] data) {
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(data, 0, data.length);
        byte[] out = new byte[20];
        digest.doFinal(out, 0);
        return out;
    }

    private static byte[] toFixedLength(byte[] src, int len) {
        if (src.length == len) return src;
        byte[] dst = new byte[len];
        if (src.length > len) {
            System.arraycopy(src, src.length - len, dst, 0, len);
        } else {
            System.arraycopy(src, 0, dst, len - src.length, src.length);
        }
        return dst;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
