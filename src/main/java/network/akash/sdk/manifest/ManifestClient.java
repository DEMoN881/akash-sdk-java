package network.akash.sdk.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import network.akash.sdk.crypto.CertificateGenerator.CertificateResult;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public final class ManifestClient {

    private final ObjectMapper mapper = new ObjectMapper();

    public void submitManifest(String providerUrl, long dseq, Manifest manifest,
                               CertificateResult clientCert) throws Exception {
        String url = providerUrl.replaceAll("/+$", "")
                + "/deployment/" + dseq + "/manifest";

        String json = mapper.writeValueAsString(manifest.toSubmitFormat());

        SSLContext sslContext = buildMtlsContext(clientCert);

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        if (conn instanceof HttpsURLConnection httpsConn) {
            httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
            httpsConn.setHostnameVerifier((hostname, session) -> true);
        }

        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new ManifestSubmitException(status,
                    new String(conn.getErrorStream() != null
                            ? conn.getErrorStream().readAllBytes()
                            : new byte[0]));
        }
    }

    private SSLContext buildMtlsContext(CertificateResult clientCert) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("client", clientCert.keyPair().getPrivate(), new char[0],
                new java.security.cert.Certificate[]{clientCert.certificate()});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, new char[0]);

        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
        }};

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), trustAll, new java.security.SecureRandom());
        return ctx;
    }

    public static class ManifestSubmitException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        public ManifestSubmitException(int statusCode, String responseBody) {
            super("Manifest submit failed: HTTP " + statusCode + " — " + responseBody);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int statusCode() { return statusCode; }
        public String responseBody() { return responseBody; }
    }
}
