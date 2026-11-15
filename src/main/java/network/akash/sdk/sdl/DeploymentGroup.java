package network.akash.sdk.sdl;

import java.util.List;
import java.util.Map;

public record DeploymentGroup(
        String name,
        int gseq,
        Map<String, String> attributes,
        List<String> signedBy,
        List<GroupResource> resources
) {}
