package network.akash.sdk.crypto;

import network.akash.sdk.cosmos.AccAddress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AkashKeyPairTest {

    private static final String TEST_PRIVKEY_HEX =
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e";

    @Test
    void fromHexProducesValidKeyPair() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        assertNotNull(kp);
        assertNotNull(kp.getPublicKey());
        assertNotNull(kp.getAddress());
    }

    @Test
    void publicKeyIsCompressed33Bytes() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        byte[] pubKey = kp.getPublicKey();
        assertEquals(33, pubKey.length);
        assertTrue(pubKey[0] == 0x02 || pubKey[0] == 0x03);
    }

    @Test
    void addressHasAkashPrefix() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        String addr = kp.getAddress().toBech32();
        assertTrue(addr.startsWith("akash1"));
    }

    @Test
    void sameKeyProducesSameAddress() {
        AkashKeyPair kp1 = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        AkashKeyPair kp2 = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        assertEquals(kp1.getAddress(), kp2.getAddress());
        assertArrayEquals(kp1.getPublicKey(), kp2.getPublicKey());
    }

    @Test
    void signProduces64ByteSignature() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        byte[] msg = "hello akash".getBytes();
        byte[] sig = kp.sign(msg);
        assertEquals(64, sig.length);
    }

    @Test
    void signIsDeterministic() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        byte[] msg = "deterministic test".getBytes();
        byte[] sig1 = kp.sign(msg);
        byte[] sig2 = kp.sign(msg);
        assertArrayEquals(sig1, sig2);
    }

    @Test
    void differentMessagesDifferentSignatures() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        byte[] sig1 = kp.sign("message1".getBytes());
        byte[] sig2 = kp.sign("message2".getBytes());
        assertFalse(java.util.Arrays.equals(sig1, sig2));
    }

    @Test
    void differentKeysProduceDifferentAddresses() {
        AkashKeyPair kp1 = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        AkashKeyPair kp2 = AkashKeyPair.fromHex(
                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertNotEquals(kp1.getAddress(), kp2.getAddress());
    }

    @Test
    void fromPrivateKeyBytes() {
        byte[] privKey = new byte[32];
        privKey[31] = 1;
        AkashKeyPair kp = AkashKeyPair.fromPrivateKey(privKey);
        assertNotNull(kp.getAddress());
        assertTrue(kp.getAddress().toBech32().startsWith("akash1"));
    }

    @Test
    void publicKeyIsCopy() {
        AkashKeyPair kp = AkashKeyPair.fromHex(TEST_PRIVKEY_HEX);
        byte[] pk1 = kp.getPublicKey();
        pk1[0] = 0;
        byte[] pk2 = kp.getPublicKey();
        assertNotEquals(pk1[0], pk2[0]);
    }
}
