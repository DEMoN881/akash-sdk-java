package network.akash.sdk.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Bech32Test {

    @Test
    void encodeDecodeRoundtrip() {
        byte[] data = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13};
        String encoded = Bech32.encode("akash", data);
        assertTrue(encoded.startsWith("akash1"));

        Bech32.Decoded decoded = Bech32.decode(encoded);
        assertEquals("akash", decoded.hrp());
        assertArrayEquals(data, decoded.data());
    }

    @Test
    void decodeKnownBech32() {
        String encoded = Bech32.encode("cosmos", new byte[20]);
        Bech32.Decoded decoded = Bech32.decode(encoded);
        assertEquals("cosmos", decoded.hrp());
        assertArrayEquals(new byte[20], decoded.data());
    }

    @Test
    void decodeTooShort() {
        assertThrows(IllegalArgumentException.class, () -> Bech32.decode("a1b"));
    }

    @Test
    void decodeMixedCase() {
        byte[] data = new byte[20];
        String encoded = Bech32.encode("test", data);
        String mixed = encoded.substring(0, 5) + encoded.substring(5).toUpperCase();
        if (!mixed.equals(encoded) && !mixed.equals(encoded.toUpperCase())) {
            assertThrows(IllegalArgumentException.class, () -> Bech32.decode(mixed));
        }
    }

    @Test
    void decodeInvalidChecksum() {
        byte[] data = new byte[20];
        String encoded = Bech32.encode("akash", data);
        char last = encoded.charAt(encoded.length() - 1);
        char replaced = last == 'q' ? 'p' : 'q';
        String corrupted = encoded.substring(0, encoded.length() - 1) + replaced;
        assertThrows(IllegalArgumentException.class, () -> Bech32.decode(corrupted));
    }

    @Test
    void decodeInvalidCharacter() {
        assertThrows(IllegalArgumentException.class, () -> Bech32.decode("akash1invalid!char"));
    }

    @Test
    void convertBitsPadding() {
        byte[] data = {(byte) 0xff};
        byte[] bits5 = Bech32.convertBits(data, 8, 5, true);
        assertTrue(bits5.length > 0);
        byte[] back = Bech32.convertBits(bits5, 5, 8, false);
        assertArrayEquals(data, back);
    }

    @Test
    void differentHrpsDifferentEncodings() {
        byte[] data = new byte[20];
        String akash = Bech32.encode("akash", data);
        String cosmos = Bech32.encode("cosmos", data);
        assertNotEquals(akash, cosmos);
        assertTrue(akash.startsWith("akash1"));
        assertTrue(cosmos.startsWith("cosmos1"));
    }
}
