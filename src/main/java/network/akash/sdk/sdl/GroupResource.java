package network.akash.sdk.sdl;

public record GroupResource(
        String serviceName,
        ProfileCompute compute,
        int count,
        PricingConfig price
) {}
