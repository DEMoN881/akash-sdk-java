package network.akash.sdk.pricing;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceClientTest {

    private HttpServer server;
    private PriceClient client;
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/pricing", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = lastBody.get().startsWith("[")
                    ? """
                    [{"spec":{"cpu":100,"memory":128,"storage":128},"akash":0.63,"aws":3.39,"gcp":3.74,"azure":4.07},
                     {"spec":{"cpu":1000,"memory":256,"storage":256},"akash":4.10,"aws":20.0,"gcp":21.0,"azure":22.0}]
                    """.getBytes(StandardCharsets.UTF_8)
                    : """
                    {"spec":{"cpu":100,"memory":134217728,"storage":134217728},"akash":0.63,"aws":3.39,"gcp":3.74,"azure":4.07}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/v1/gpu-prices", exchange -> {
            byte[] body = """
                    {"models":[
                      {"vendor":"nvidia","model":"a100","ram":"40Gi","interface":"SXM4",
                       "price":{"currency":"USD","min":0.84,"max":1.10,"avg":0.90,"weightedAverage":0.88,"med":0.86}},
                      {"vendor":"nvidia","model":"t4","ram":"16Gi","interface":"PCIe","price":null}
                    ]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        client = new PriceClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop(0);
    }

    @Test
    void estimatesOneShapeInUsdPerMonth() throws Exception {
        PriceEstimate quote = client.estimate(100, 134217728, 134217728);

        assertTrue(lastBody.get().contains("\"cpu\":100"));
        assertEquals(100, quote.cpuMillis());
        assertEquals(134217728, quote.memoryBytes());
        assertEquals(0.63, quote.akashUsdPerMonth());
        assertEquals(3.39, quote.awsUsdPerMonth());
        assertEquals(3.74, quote.gcpUsdPerMonth());
        assertEquals(4.07, quote.azureUsdPerMonth());
    }

    @Test
    void estimatesSeveralShapesInOrder() throws Exception {
        List<PriceEstimate> quotes = client.estimate(List.of(
                new PriceClient.Resources(100, 128, 128),
                new PriceClient.Resources(1000, 256, 256)
        ));

        assertEquals(2, quotes.size());
        assertEquals(0.63, quotes.get(0).akashUsdPerMonth());
        assertEquals(4.10, quotes.get(1).akashUsdPerMonth());
        assertEquals(1000, quotes.get(1).cpuMillis());
    }

    @Test
    void gpuPricesSkipModelsWithoutAPrice() throws Exception {
        List<GpuPrice> prices = client.gpuPrices();

        assertEquals(1, prices.size());
        GpuPrice a100 = prices.get(0);
        assertEquals("nvidia", a100.vendor());
        assertEquals("a100", a100.model());
        assertEquals("40Gi", a100.ram());
        assertEquals("SXM4", a100.interconnect());
        assertEquals("USD", a100.currency());
        assertEquals(0.86, a100.median());
    }
}
