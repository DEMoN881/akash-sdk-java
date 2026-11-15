package network.akash.sdk.types.provider;

import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.types.base.Attribute;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProviderMessagesTest {

    private static final String ADDR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();

    @Test
    void createProviderValid() {
        var msg = new ProviderMessages.MsgCreateProvider(
                ADDR, "https://provider.test", List.of(new Attribute("region", "us-west")), "test@test.com", "https://test.com");
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("provider", msg.getRoute());
        assertEquals("create-provider", msg.getType());
        assertEquals(ADDR, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void createProviderEmptyUriThrows() {
        var msg = new ProviderMessages.MsgCreateProvider(ADDR, "", List.of(), null, null);
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void updateProviderValid() {
        var msg = new ProviderMessages.MsgUpdateProvider(
                ADDR, "https://updated.test", List.of(), null, null);
        assertDoesNotThrow(msg::validateBasic);
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void deleteProviderValid() {
        var msg = new ProviderMessages.MsgDeleteProvider(ADDR);
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("delete-provider", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void signBytesWithAttributes() {
        var msg = new ProviderMessages.MsgCreateProvider(
                ADDR, "https://test.com",
                List.of(new Attribute("arch", "amd64"), new Attribute("region", "eu")),
                "a@b.com", "https://b.com");
        byte[] bytes = msg.getSignBytes();
        assertTrue(bytes.length > 0);
        assertArrayEquals(bytes, msg.getSignBytes());
    }
}
