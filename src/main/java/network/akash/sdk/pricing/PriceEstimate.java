package network.akash.sdk.pricing;

/**
 * Monthly USD estimate from the Akash Console pricing API.
 * {@code akashUsdPerMonth} is the network estimate. The other three are list prices
 * for the same CPU, memory, and storage on AWS, GCP, and Azure.
 */
public record PriceEstimate(
        long cpuMillis,
        long memoryBytes,
        long storageBytes,
        double akashUsdPerMonth,
        double awsUsdPerMonth,
        double gcpUsdPerMonth,
        double azureUsdPerMonth
) {
}
