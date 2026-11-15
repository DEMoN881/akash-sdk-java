package network.akash.sdk.keyring;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileKeyring implements Keyring {

    private static final String EXTENSION = ".key";
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LEN = 256;

    private final Path directory;
    private final char[] passphrase;

    public FileKeyring(Path directory, String passphrase) throws IOException {
        this.directory = directory;
        this.passphrase = passphrase.toCharArray();
        Files.createDirectories(directory);
    }

    @Override
    public void save(String name, byte[] privateKey) throws Exception {
        SecureRandom rng = new SecureRandom();
        byte[] salt = new byte[SALT_LEN];
        rng.nextBytes(salt);
        byte[] iv = new byte[IV_LEN];
        rng.nextBytes(iv);

        SecretKeySpec aesKey = deriveKey(salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));

        byte[] encrypted = cipher.doFinal(privateKey);

        byte[] output = new byte[SALT_LEN + IV_LEN + encrypted.length];
        System.arraycopy(salt, 0, output, 0, SALT_LEN);
        System.arraycopy(iv, 0, output, SALT_LEN, IV_LEN);
        System.arraycopy(encrypted, 0, output, SALT_LEN + IV_LEN, encrypted.length);

        Files.write(keyPath(name), output);
    }

    @Override
    public Optional<byte[]> load(String name) throws Exception {
        Path path = keyPath(name);
        if (!Files.exists(path)) return Optional.empty();

        byte[] data = Files.readAllBytes(path);
        if (data.length < SALT_LEN + IV_LEN + TAG_BITS / 8) {
            throw new IllegalStateException("Corrupt key file: " + name);
        }

        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        System.arraycopy(data, 0, salt, 0, SALT_LEN);
        System.arraycopy(data, SALT_LEN, iv, 0, IV_LEN);

        byte[] encrypted = new byte[data.length - SALT_LEN - IV_LEN];
        System.arraycopy(data, SALT_LEN + IV_LEN, encrypted, 0, encrypted.length);

        SecretKeySpec aesKey = deriveKey(salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));

        return Optional.of(cipher.doFinal(encrypted));
    }

    @Override
    public void delete(String name) throws Exception {
        Files.deleteIfExists(keyPath(name));
    }

    @Override
    public List<String> list() throws Exception {
        if (!Files.isDirectory(directory)) return List.of();
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                    .filter(p -> p.toString().endsWith(EXTENSION))
                    .map(p -> {
                        String fn = p.getFileName().toString();
                        return fn.substring(0, fn.length() - EXTENSION.length());
                    })
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean exists(String name) throws Exception {
        return Files.exists(keyPath(name));
    }

    private Path keyPath(String name) {
        return directory.resolve(name + EXTENSION);
    }

    private SecretKeySpec deriveKey(byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LEN);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }
}
