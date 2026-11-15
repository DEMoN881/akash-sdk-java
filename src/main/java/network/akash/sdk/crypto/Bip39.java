package network.akash.sdk.crypto;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Arrays;

public final class Bip39 {

    private Bip39() {}

    public static byte[] mnemonicToSeed(String mnemonic) {
        return mnemonicToSeed(mnemonic, "");
    }

    public static byte[] mnemonicToSeed(String mnemonic, String passphrase) {
        try {
            String salt = "mnemonic" + passphrase;
            KeySpec spec = new PBEKeySpec(
                    mnemonic.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    2048, 512);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("BIP39 seed derivation failed", e);
        }
    }
}
