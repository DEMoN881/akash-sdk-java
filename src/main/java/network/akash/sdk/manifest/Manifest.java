package network.akash.sdk.manifest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record Manifest(List<ManifestGroup> groups) {

    public List<Map<String, Object>> toSubmitFormat() {
        return groups.stream().map(ManifestGroup::toMap).collect(Collectors.toList());
    }

    public record ManifestGroup(String name, List<ManifestService> services) {
        Map<String, Object> toMap() {
            return Map.of(
                    "name", name,
                    "services", services.stream().map(ManifestService::toMap).collect(Collectors.toList())
            );
        }
    }

    public record ManifestService(
            String name,
            String image,
            List<String> command,
            List<String> args,
            Map<String, String> env,
            int count,
            List<ServiceExpose> expose,
            Resources resources
    ) {
        Map<String, Object> toMap() {
            return Map.of(
                    "name", name,
                    "image", image,
                    "command", command != null ? command : List.of(),
                    "args", args != null ? args : List.of(),
                    "env", env != null ? env.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.toList()) : List.of(),
                    "count", count,
                    "expose", expose.stream().map(ServiceExpose::toMap).collect(Collectors.toList()),
                    "resources", resources.toMap()
            );
        }
    }

    public record ServiceExpose(int port, int externalPort, String proto, boolean global) {
        Map<String, Object> toMap() {
            return Map.of(
                    "port", port,
                    "external_port", externalPort,
                    "proto", proto,
                    "global", global
            );
        }
    }

    public record Resources(long cpuMillis, long memoryBytes, long storageBytes) {
        Map<String, Object> toMap() {
            return Map.of(
                    "cpu", Map.of("units", Map.of("val", String.valueOf(cpuMillis))),
                    "memory", Map.of("quantity", Map.of("val", String.valueOf(memoryBytes))),
                    "storage", List.of(Map.of("name", "default",
                            "quantity", Map.of("val", String.valueOf(storageBytes))))
            );
        }
    }
}
