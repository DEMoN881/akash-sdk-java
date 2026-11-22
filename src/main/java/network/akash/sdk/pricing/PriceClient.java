package network.akash.sdk.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads deployment price estimates from the Akash Console API.
 * This is not a chain endpoint and does not need a key or a wallet.
 */
public final class PriceClient implements AutoCloseable {

    public static final String CONSOLE_API = "https://console-api.akash.network";

    private static final MediaType JSON = MediaType.get("application/json");

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final boolean ownsClient;

    public PriceClient() {
        this(CONSOLE_API);
    }

    public PriceClient(String baseUrl) {
        this(baseUrl, new OkHttpClient(), true);
    }

    public PriceClient(String baseUrl, OkHttpClient httpClient) {
        this(baseUrl, httpClient, false);
    }

    private PriceClient(String baseUrl, OkHttpClient httpClient, boolean ownsClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
        this.ownsClient = ownsClient;
    }

    /** CPU is in milli ({@code 1000} = 1 core). Memory and storage are bytes. */
    public PriceEstimate estimate(long cpuMillis, long memoryBytes, long storageBytes) throws IOException {
        ObjectNode body = spec(cpuMillis, memoryBytes, storageBytes);
        JsonNode json = post("/v1/pricing", body);
        return estimate(json);
    }

    /** At most 10 specs. The result is in the same order. */
    public List<PriceEstimate> estimate(List<Resources> specs) throws IOException {
        if (specs == null || specs.isEmpty() || specs.size() > 10) {
            throw new IllegalArgumentException("pricing accepts 1 to 10 resource specs");
        }
        ArrayNode body = mapper.createArrayNode();
        for (Resources spec : specs) {
            body.add(spec(spec.cpuMillis(), spec.memoryBytes(), spec.storageBytes()));
        }
        JsonNode json = post("/v1/pricing", body);
        if (!json.isArray()) {
            throw new IOException("expected a pricing array");
        }
        List<PriceEstimate> out = new ArrayList<>();
        for (JsonNode item : json) {
            out.add(estimate(item));
        }
        return out;
    }

    /** GPU models whose price the API actually filled in. */
    public List<GpuPrice> gpuPrices() throws IOException {
        JsonNode json = get("/v1/gpu-prices");
        JsonNode models = json.get("models");
        if (models == null || !models.isArray()) {
            throw new IOException("gpu price list has no models");
        }
        List<GpuPrice> out = new ArrayList<>();
        for (JsonNode model : models) {
            JsonNode price = model.get("price");
            if (price == null || price.isNull()) {
                continue;
            }
            out.add(new GpuPrice(
                    text(model, "vendor"),
                    text(model, "model"),
                    text(model, "ram"),
                    text(model, "interface"),
                    text(price, "currency"),
                    price.path("min").asDouble(),
                    price.path("max").asDouble(),
                    price.path("avg").asDouble(),
                    price.path("weightedAverage").asDouble(),
                    price.path("med").asDouble()
            ));
        }
        return out;
    }

    @Override
    public void close() {
        if (ownsClient) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    private ObjectNode spec(long cpuMillis, long memoryBytes, long storageBytes) {
        ObjectNode node = mapper.createObjectNode();
        node.put("cpu", cpuMillis);
        node.put("memory", memoryBytes);
        node.put("storage", storageBytes);
        return node;
    }

    private PriceEstimate estimate(JsonNode json) {
        JsonNode spec = json.path("spec");
        return new PriceEstimate(
                spec.path("cpu").asLong(),
                spec.path("memory").asLong(),
                spec.path("storage").asLong(),
                json.path("akash").asDouble(),
                json.path("aws").asDouble(),
                json.path("gcp").asDouble(),
                json.path("azure").asDouble()
        );
    }

    private JsonNode post(String path, JsonNode body) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(mapper.writeValueAsBytes(body), JSON))
                .build();
        return send(request);
    }

    private JsonNode get(String path) throws IOException {
        Request request = new Request.Builder().url(baseUrl + path).get().build();
        return send(request);
    }

    private JsonNode send(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            String raw = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("pricing failed: HTTP " + response.code() + " — " + raw);
            }
            return mapper.readTree(raw);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    /** One CPU / memory / storage shape sent to {@link #estimate(List)}. */
    public record Resources(long cpuMillis, long memoryBytes, long storageBytes) {
    }
}
