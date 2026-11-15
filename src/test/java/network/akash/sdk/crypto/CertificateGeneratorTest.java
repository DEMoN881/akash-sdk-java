package network.akash.sdk.crypto;

import network.akash.sdk.crypto.CertificateGenerator.CertificateResult;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class CertificateGeneratorTest {

    @Test
    void generateCertificate() throws Exception {
        CertificateResult result = CertificateGenerator.generate("CN=test,O=Test");
        assertNotNull(result.certificate());
        assertNotNull(result.keyPair());
        assertNotNull(result.certPem());
        assertNotNull(result.privateKeyPem());
    }

    @Test
    void certPemContainsMarkers() throws Exception {
        CertificateResult result = CertificateGenerator.generate("CN=test");
        assertTrue(result.certPem().contains("BEGIN CERTIFICATE"));
        assertTrue(result.certPem().contains("END CERTIFICATE"));
    }

    @Test
    void privateKeyPemContainsMarkers() throws Exception {
        CertificateResult result = CertificateGenerator.generate("CN=test");
        assertTrue(result.privateKeyPem().contains("BEGIN EC PRIVATE KEY")
                || result.privateKeyPem().contains("BEGIN PRIVATE KEY"));
    }

    @Test
    void generateForAkashAddress() throws Exception {
        CertificateResult result = CertificateGenerator.generateForAkash("akash1abc123");
        X509Certificate cert = result.certificate();
        String dn = cert.getSubjectX500Principal().getName();
        assertTrue(dn.contains("akash1abc123"));
    }

    @Test
    void certBytesNotEmpty() throws Exception {
        CertificateResult result = CertificateGenerator.generate("CN=test");
        assertTrue(result.certBytes().length > 0);
        assertTrue(result.publicKeyBytes().length > 0);
    }

    @Test
    void uniqueSerialNumbers() throws Exception {
        CertificateResult r1 = CertificateGenerator.generate("CN=test1");
        CertificateResult r2 = CertificateGenerator.generate("CN=test2");
        assertNotEquals(r1.certificate().getSerialNumber(), r2.certificate().getSerialNumber());
    }

    @Test
    void ecKeyPairGenerated() throws Exception {
        CertificateResult result = CertificateGenerator.generate("CN=test");
        assertEquals("EC", result.keyPair().getPublic().getAlgorithm());
    }

    @Test
    void toPemStaticMethod() throws Exception {
        CertificateResult result = CertificateGenerator.generate("CN=test");
        String pem = CertificateGenerator.toPem(result.certificate());
        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----"));
    }
}
