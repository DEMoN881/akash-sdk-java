package network.akash.sdk.sdl;

import java.util.List;
import java.util.Map;

public record Sdl(
        String version,
        Map<String, ServiceDefinition> services,
        Map<String, ProfileCompute> computeProfiles,
        Map<String, PlacementConfig> placements,
        List<DeploymentGroup> groups
) {
    public void validate() {
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("SDL must define at least one service");
        }
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("SDL must define at least one deployment group");
        }
        for (var svc : services.values()) {
            if (svc.image() == null || svc.image().isEmpty()) {
                throw new IllegalArgumentException("service must have an image");
            }
        }
    }
}
