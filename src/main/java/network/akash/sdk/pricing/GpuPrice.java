package network.akash.sdk.pricing;

/**
 * One GPU model from the Akash Console price list.
 * The amounts are USD. The API does not label them as hourly or monthly.
 */
public record GpuPrice(
        String vendor,
        String model,
        String ram,
        String interconnect,
        String currency,
        double min,
        double max,
        double avg,
        double weightedAverage,
        double median
) {
}
