package network.akash.sdk.types.audit;

import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.types.base.Attribute;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditMessagesTest {

    private static final String OWNER = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();
    private static final String AUDITOR = AkashKeyPair.fromHex(
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    ).getAddress().toBech32();

    @Test
    void signProviderAttributesValid() {
        var msg = new AuditMessages.MsgSignProviderAttributes(
                OWNER, AUDITOR, List.of(new Attribute("verified", "true")));
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("audit", msg.getRoute());
        assertEquals(AUDITOR, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void signProviderAttributesEmptyAttrsThrows() {
        var msg = new AuditMessages.MsgSignProviderAttributes(OWNER, AUDITOR, List.of());
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void deleteProviderAttributesValid() {
        var msg = new AuditMessages.MsgDeleteProviderAttributes(OWNER, AUDITOR, List.of("key1"));
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("delete-provider-attributes", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void deleteProviderAttributesEmptyKeysThrows() {
        var msg = new AuditMessages.MsgDeleteProviderAttributes(OWNER, AUDITOR, List.of());
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void signBytesAreDeterministic() {
        var msg = new AuditMessages.MsgSignProviderAttributes(
                OWNER, AUDITOR, List.of(new Attribute("k", "v")));
        assertArrayEquals(msg.getSignBytes(), msg.getSignBytes());
    }
}
