package network.akash.sdk.types.cert;

import network.akash.sdk.crypto.AkashKeyPair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CertMessagesTest {

    private static final String ADDR = AkashKeyPair.fromHex(
            "e856db0e3c5e0404c0bc7a84e0a3ab483e5e0fb49dfc0c4a7c3b8f1b2f5a0d1e"
    ).getAddress().toBech32();

    @Test
    void createCertificateValid() {
        var msg = new CertMessages.MsgCreateCertificate(ADDR, new byte[]{1, 2, 3}, new byte[]{4, 5, 6});
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("cert", msg.getRoute());
        assertEquals("create-certificate", msg.getType());
        assertEquals(ADDR, msg.getSigners().get(0).toBech32());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void createCertificateEmptyCertThrows() {
        var msg = new CertMessages.MsgCreateCertificate(ADDR, new byte[0], new byte[]{1});
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void createCertificateEmptyPubkeyThrows() {
        var msg = new CertMessages.MsgCreateCertificate(ADDR, new byte[]{1}, new byte[0]);
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void revokeCertificateValid() {
        var msg = new CertMessages.MsgRevokeCertificate(ADDR, "serial-123");
        assertDoesNotThrow(msg::validateBasic);
        assertEquals("revoke-certificate", msg.getType());
        assertTrue(msg.getSignBytes().length > 0);
    }

    @Test
    void revokeCertificateEmptySerialThrows() {
        var msg = new CertMessages.MsgRevokeCertificate(ADDR, "");
        assertThrows(IllegalArgumentException.class, msg::validateBasic);
    }

    @Test
    void signBytesAreDeterministic() {
        var msg = new CertMessages.MsgCreateCertificate(ADDR, new byte[]{1}, new byte[]{2});
        assertArrayEquals(msg.getSignBytes(), msg.getSignBytes());
    }
}
