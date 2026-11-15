package network.akash.sdk.keyring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileKeyringTest {

    @TempDir
    Path tempDir;

    private FileKeyring keyring;

    @BeforeEach
    void setUp() throws Exception {
        keyring = new FileKeyring(tempDir.resolve("keys"), "test-passphrase");
    }

    @Test
    void saveAndLoad() throws Exception {
        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8};
        keyring.save("mykey", key);
        Optional<byte[]> loaded = keyring.load("mykey");
        assertTrue(loaded.isPresent());
        assertArrayEquals(key, loaded.get());
    }

    @Test
    void loadNonExistent() throws Exception {
        Optional<byte[]> result = keyring.load("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void delete() throws Exception {
        keyring.save("toDelete", new byte[]{1, 2, 3});
        assertTrue(keyring.exists("toDelete"));
        keyring.delete("toDelete");
        assertFalse(keyring.exists("toDelete"));
    }

    @Test
    void listKeys() throws Exception {
        keyring.save("key1", new byte[]{1});
        keyring.save("key2", new byte[]{2});
        keyring.save("key3", new byte[]{3});
        List<String> names = keyring.list();
        assertEquals(3, names.size());
        assertTrue(names.containsAll(List.of("key1", "key2", "key3")));
    }

    @Test
    void exists() throws Exception {
        assertFalse(keyring.exists("nope"));
        keyring.save("yep", new byte[]{42});
        assertTrue(keyring.exists("yep"));
    }

    @Test
    void wrongPassphraseFailsDecrypt() throws Exception {
        keyring.save("secret", new byte[]{10, 20, 30});
        FileKeyring wrongKeyring = new FileKeyring(tempDir.resolve("keys"), "wrong-passphrase");
        assertThrows(Exception.class, () -> wrongKeyring.load("secret"));
    }

    @Test
    void overwriteKey() throws Exception {
        keyring.save("key", new byte[]{1, 2, 3});
        keyring.save("key", new byte[]{4, 5, 6});
        assertArrayEquals(new byte[]{4, 5, 6}, keyring.load("key").orElseThrow());
    }

    @Test
    void largeKey() throws Exception {
        byte[] largeKey = new byte[1024];
        for (int i = 0; i < largeKey.length; i++) largeKey[i] = (byte) (i % 256);
        keyring.save("large", largeKey);
        assertArrayEquals(largeKey, keyring.load("large").orElseThrow());
    }
}
