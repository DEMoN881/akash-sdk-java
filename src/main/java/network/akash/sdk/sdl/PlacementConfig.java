package network.akash.sdk.sdl;

import java.util.List;
import java.util.Map;

public record PlacementConfig(
        Map<String, String> attributes,
        List<String> signedBy,
        Map<String, PricingConfig> pricing
) {}
