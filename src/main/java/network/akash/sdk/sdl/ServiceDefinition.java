package network.akash.sdk.sdl;

import java.util.List;
import java.util.Map;

public record ServiceDefinition(
        String image,
        List<String> command,
        List<String> args,
        Map<String, String> env,
        List<ServiceExpose> expose,
        int count
) {}
