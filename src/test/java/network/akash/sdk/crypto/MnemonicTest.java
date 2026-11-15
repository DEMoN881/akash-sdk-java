package network.akash.sdk.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MnemonicTest {

    @Test
    void bip39SeedDerivation() {
        byte[] seed = Bip39.mnemonicToSeed("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about");
        assertEquals(64, seed.length);
        assertNotNull(seed);
    }

    @Test
    void bip39SeedDeterministic() {
        String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        byte[] seed1 = Bip39.mnemonicToSeed(mnemonic);
        byte[] seed2 = Bip39.mnemonicToSeed(mnemonic);
        assertArrayEquals(seed1, seed2);
    }

    @Test
    void bip39WithPassphrase() {
        String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        byte[] seed1 = Bip39.mnemonicToSeed(mnemonic, "");
        byte[] seed2 = Bip39.mnemonicToSeed(mnemonic, "mypassphrase");
        assertNotEquals(bytesToHex(seed1), bytesToHex(seed2));
    }

    @Test
    void bip32CosmosDeriveKey() {
        String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        byte[] seed = Bip39.mnemonicToSeed(mnemonic);
        byte[] privKey = Bip32.deriveCosmosPrivateKey(seed);
        assertEquals(32, privKey.length);
    }

    @Test
    void fromMnemonicProducesValidAddress() {
        AkashKeyPair kp = AkashKeyPair.fromMnemonic(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about");
        String addr = kp.getAddress().toBech32();
        assertTrue(addr.startsWith("akash1"));
        assertEquals(44, addr.length());
    }

    @Test
    void fromMnemonicDeterministic() {
        String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        AkashKeyPair kp1 = AkashKeyPair.fromMnemonic(mnemonic);
        AkashKeyPair kp2 = AkashKeyPair.fromMnemonic(mnemonic);
        assertEquals(kp1.getAddress().toBech32(), kp2.getAddress().toBech32());
        assertArrayEquals(kp1.getPublicKey(), kp2.getPublicKey());
    }

    @Test
    void differentMnemonicsDifferentKeys() {
        AkashKeyPair kp1 = AkashKeyPair.fromMnemonic(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about");
        AkashKeyPair kp2 = AkashKeyPair.fromMnemonic(
                "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong");
        assertNotEquals(kp1.getAddress().toBech32(), kp2.getAddress().toBech32());
    }

    @Test
    void signWithMnemonicDerivedKey() {
        AkashKeyPair kp = AkashKeyPair.fromMnemonic(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about");
        byte[] sig = kp.sign("test message".getBytes());
        assertEquals(64, sig.length);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
