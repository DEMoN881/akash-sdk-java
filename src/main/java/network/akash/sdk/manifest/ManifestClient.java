package network.akash.sdk.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import network.akash.sdk.crypto.CertificateGenerator.CertificateResult;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public final class ManifestClient {

    private final ObjectMapper mapper = new ObjectMapper();

    public void submitManifest(String providerUrl, long dseq, Manifest manifest,
                               CertificateResult clientCert) throws Exception {
        submitManifest(providerUrl, dseq, mapper.writeValueAsString(manifest.toSubmitFormat()), clientCert);
    }

    /**
     * PUT the JSON from {@code SdlDeployment.manifestJson()}.
     * The provider hashes this body and compares it with the on-chain version.
     */
    public void submitManifest(String providerUrl, long dseq, String manifestJson,
                               CertificateResult clientCert) throws Exception {
        String url = providerUrl.replaceAll("/+$", "")
                + "/deployment/" + dseq + "/manifest";

        SSLContext sslContext = buildMtlsContext(clientCert);

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        if (conn instanceof HttpsURLConnection httpsConn) {
            httpsConn.setSSLSocketFactory(withoutServerName(sslContext.getSocketFactory()));
            httpsConn.setHostnameVerifier((hostname, session) -> true);
        }

        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(manifestJson.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new ManifestSubmitException(status,
                    new String(conn.getErrorStream() != null
                            ? conn.getErrorStream().readAllBytes()
                            : new byte[0], StandardCharsets.UTF_8));
        }
    }

    private SSLContext buildMtlsContext(CertificateResult clientCert) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("client", clientCert.keyPair().getPrivate(), new char[0],
                new java.security.cert.Certificate[]{clientCert.certificate()});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, new char[0]);
        KeyManager[] managers = kmf.getKeyManagers();
        for (int i = 0; i < managers.length; i++) {
            if (managers[i] instanceof X509ExtendedKeyManager extended) {
                managers[i] = new AlwaysClient(extended);
            }
        }

        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
        }};

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(managers, trustAll, new java.security.SecureRandom());
        return ctx;
    }

    /** The public listener is selected by SNI and rejects the client certificate. */
    private static SSLSocketFactory withoutServerName(SSLSocketFactory delegate) {
        return new SSLSocketFactory() {
            @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
            @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }

            @Override
            public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                return clearServerName((SSLSocket) delegate.createSocket(socket, host, port, autoClose));
            }

            @Override
            public Socket createSocket(String host, int port) throws IOException {
                return clearServerName((SSLSocket) delegate.createSocket(host, port));
            }

            @Override
            public Socket createSocket(String host, int port, java.net.InetAddress local, int localPort) throws IOException {
                return clearServerName((SSLSocket) delegate.createSocket(host, port, local, localPort));
            }

            @Override
            public Socket createSocket(java.net.InetAddress host, int port) throws IOException {
                return clearServerName((SSLSocket) delegate.createSocket(host, port));
            }

            @Override
            public Socket createSocket(java.net.InetAddress host, int port, java.net.InetAddress local, int localPort) throws IOException {
                return clearServerName((SSLSocket) delegate.createSocket(host, port, local, localPort));
            }

            private Socket clearServerName(SSLSocket socket) {
                SSLParameters parameters = socket.getSSLParameters();
                parameters.setServerNames(List.of());
                socket.setSSLParameters(parameters);
                return socket;
            }
        };
    }

    private static final class AlwaysClient extends X509ExtendedKeyManager {
        private final X509ExtendedKeyManager delegate;

        private AlwaysClient(X509ExtendedKeyManager delegate) {
            this.delegate = delegate;
        }

        @Override public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) { return "client"; }
        @Override public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, javax.net.ssl.SSLEngine engine) { return "client"; }
        @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) { return delegate.chooseServerAlias(keyType, issuers, socket); }
        @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, javax.net.ssl.SSLEngine engine) { return delegate.chooseEngineServerAlias(keyType, issuers, engine); }
        @Override public X509Certificate[] getCertificateChain(String alias) { return delegate.getCertificateChain(alias); }
        @Override public String[] getClientAliases(String keyType, Principal[] issuers) { return new String[]{"client"}; }
        @Override public String[] getServerAliases(String keyType, Principal[] issuers) { return delegate.getServerAliases(keyType, issuers); }
        @Override public PrivateKey getPrivateKey(String alias) { return delegate.getPrivateKey(alias); }
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
