package network.akash.sdk.pricing;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live probe. Stays out of {@code mvn test} unless {@code AKASH_RATE_LIMIT_PROBE=1}.
 * Stops at the first HTTP 429.
 */
@EnabledIfEnvironmentVariable(named = "AKASH_RATE_LIMIT_PROBE", matches = "1")
class PriceClientRateLimitTest {

    private static final int CAP = 120;
    private static final MediaType JSON = MediaType.get("application/json");
    private static final byte[] BODY = "{\"cpu\":100,\"memory\":134217728,\"storage\":134217728}"
            .getBytes();

    @Test
    void pricingRateLimit() throws Exception {
        OkHttpClient http = new OkHttpClient.Builder()
                .callTimeout(15, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(PriceClient.CONSOLE_API + "/v1/pricing")
                .post(RequestBody.create(BODY, JSON))
                .build();

        long started = System.nanoTime();
        int sent = 0;
        int lastStatus = 0;
        String limitHeaders = "";
        String limitBody = "";
        try {
            for (int i = 1; i <= CAP; i++) {
                try (Response response = http.newCall(request).execute()) {
                    sent = i;
                    lastStatus = response.code();
                    if (lastStatus == 429) {
                        limitHeaders = rateHeaders(response);
                        limitBody = response.body() == null ? "" : response.body().string();
                        break;
                    }
                    if (response.body() != null) {
                        response.body().close();
                    }
                    assertTrue(lastStatus == 200, "unexpected HTTP " + lastStatus + " on request " + i);
                }
            }
        } finally {
            http.dispatcher().executorService().shutdown();
            http.connectionPool().evictAll();
        }

        double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        System.out.printf(Locale.US,
                "pricing probe: %d requests in %.2fs, last status %d%n",
                sent, seconds, lastStatus);
        if (lastStatus == 429) {
            System.out.println("rate headers: " + limitHeaders);
            System.out.println("rate body: " + limitBody);
        } else {
            System.out.println("no 429 within " + CAP + " requests");
        }
    }

    private static String rateHeaders(Response response) {
        StringBuilder out = new StringBuilder();
        for (String name : List.of(
                "retry-after",
                "x-ratelimit-limit",
                "x-ratelimit-remaining",
                "x-ratelimit-reset",
                "ratelimit-limit",
                "ratelimit-remaining",
                "ratelimit-reset")) {
            String value = response.header(name);
            if (value != null) {
                out.append(name).append('=').append(value).append(' ');
            }
        }
        if (out.isEmpty()) {
            response.headers().forEach(header -> out.append(header.getFirst())
                    .append('=')
                    .append(header.getSecond())
                    .append("; "));
        }
        return out.toString().trim();
    }
}
