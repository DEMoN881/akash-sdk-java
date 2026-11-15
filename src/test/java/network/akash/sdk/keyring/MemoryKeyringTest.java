package network.akash.sdk.keyring;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryKeyringTest {

    @Test
    void saveAndLoad() throws Exception {
        MemoryKeyring kr = new MemoryKeyring();
        kr.save("key1", new byte[]{1, 2, 3});
        Optional<byte[]> loaded = kr.load("key1");
        assertTrue(loaded.isPresent());
        assertArrayEquals(new byte[]{1, 2, 3}, loaded.get());
    }

    @Test
    void loadReturnsEmptyForMissing() throws Exception {
        MemoryKeyring kr = new MemoryKeyring();
        assertTrue(kr.load("missing").isEmpty());
    }

    @Test
    void deleteRemovesKey() throws Exception {
        MemoryKeyring kr = new MemoryKeyring();
        kr.save("k", new byte[]{1});
        kr.delete("k");
        assertFalse(kr.exists("k"));
    }

    @Test
    void listKeys() throws Exception {
        MemoryKeyring kr = new MemoryKeyring();
        kr.save("a", new byte[]{1});
        kr.save("b", new byte[]{2});
        assertEquals(2, kr.list().size());
    }

    @Test
    void dataIsolated() throws Exception {
        MemoryKeyring kr = new MemoryKeyring();
        byte[] original = {10, 20, 30};
        kr.save("iso", original);
        original[0] = 99;
        assertArrayEquals(new byte[]{10, 20, 30}, kr.load("iso").orElseThrow());
    }
}
