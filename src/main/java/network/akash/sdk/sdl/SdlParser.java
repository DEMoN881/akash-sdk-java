package network.akash.sdk.sdl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public final class SdlParser {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private SdlParser() {}

    public static Sdl parse(String yaml) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = YAML.readValue(yaml, Map.class);
        return parseRoot(root);
    }

    public static Sdl parseFile(File file) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = YAML.readValue(file, Map.class);
        return parseRoot(root);
    }

    public static Sdl parseStream(InputStream stream) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = YAML.readValue(stream, Map.class);
        return parseRoot(root);
    }

    @SuppressWarnings("unchecked")
    private static Sdl parseRoot(Map<String, Object> root) {
        String version = String.valueOf(root.getOrDefault("version", "2.0"));

        Map<String, ServiceDefinition> services = new LinkedHashMap<>();
        Map<String, Object> servicesRaw = (Map<String, Object>) root.getOrDefault("services", Map.of());
        for (var entry : servicesRaw.entrySet()) {
            services.put(entry.getKey(), parseService((Map<String, Object>) entry.getValue()));
        }

        Map<String, ProfileCompute> computeProfiles = new LinkedHashMap<>();
        Map<String, Object> profiles = (Map<String, Object>) root.getOrDefault("profiles", Map.of());
        Map<String, Object> computeRaw = (Map<String, Object>) profiles.getOrDefault("compute", Map.of());
        for (var entry : computeRaw.entrySet()) {
            computeProfiles.put(entry.getKey(), parseCompute((Map<String, Object>) entry.getValue()));
        }

        Map<String, PlacementConfig> placements = new LinkedHashMap<>();
        Map<String, Object> placementRaw = (Map<String, Object>) profiles.getOrDefault("placement", Map.of());
        for (var entry : placementRaw.entrySet()) {
            placements.put(entry.getKey(), parsePlacement((Map<String, Object>) entry.getValue()));
        }

        List<DeploymentGroup> groups = buildGroups(services, computeProfiles, placements);

        return new Sdl(version, services, computeProfiles, placements, groups);
    }

    @SuppressWarnings("unchecked")
    private static ServiceDefinition parseService(Map<String, Object> svc) {
        String image = (String) svc.getOrDefault("image", "");
        List<String> command = svc.containsKey("command")
                ? (List<String>) svc.get("command")
                : List.of();
        List<String> args = svc.containsKey("args")
                ? (List<String>) svc.get("args")
                : List.of();
        Map<String, String> env = svc.containsKey("env")
                ? toStringMap(svc.get("env"))
                : Map.of();

        List<ServiceExpose> exposes = new ArrayList<>();
        List<Object> exposeRaw = (List<Object>) svc.getOrDefault("expose", List.of());
        for (Object e : exposeRaw) {
            Map<String, Object> em = (Map<String, Object>) e;
            int port = toInt(em.get("port"));
            int as = toInt(em.getOrDefault("as", port));
            String proto = (String) em.getOrDefault("proto", "tcp");
            boolean global = false;
            List<Object> to = (List<Object>) em.getOrDefault("to", List.of());
            for (Object t : to) {
                Map<String, Object> tm = (Map<String, Object>) t;
                if (Boolean.TRUE.equals(tm.get("global"))) global = true;
            }
            exposes.add(new ServiceExpose(port, as, proto, global));
        }

        int count = toInt(svc.getOrDefault("count", 1));

        return new ServiceDefinition(image, command, args, env, exposes, count);
    }

    @SuppressWarnings("unchecked")
    private static ProfileCompute parseCompute(Map<String, Object> compute) {
        Map<String, Object> res = (Map<String, Object>) compute.getOrDefault("resources", Map.of());
        Map<String, Object> cpuRaw = (Map<String, Object>) res.getOrDefault("cpu", Map.of());
        Map<String, Object> memRaw = (Map<String, Object>) res.getOrDefault("memory", Map.of());
        Map<String, Object> storageRaw = storageNode(res.get("storage"));
        Map<String, Object> gpuRaw = (Map<String, Object>) res.getOrDefault("gpu", Map.of());

        ResourceUnits cpu = new ResourceUnits(
                String.valueOf(cpuRaw.getOrDefault("units", "0.1")));
        ResourceUnits memory = new ResourceUnits(
                String.valueOf(memRaw.getOrDefault("size", "256Mi")));
        ResourceUnits storage = new ResourceUnits(
                String.valueOf(storageRaw.getOrDefault("size", "512Mi")));
        ResourceUnits gpu = gpuRaw.isEmpty() ? null :
                new ResourceUnits(String.valueOf(gpuRaw.getOrDefault("units", "0")));

        return new ProfileCompute(cpu, memory, storage, gpu);
    }

    @SuppressWarnings("unchecked")
    private static PlacementConfig parsePlacement(Map<String, Object> placement) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Map<String, Object> attrsRaw = (Map<String, Object>) placement.getOrDefault("attributes", Map.of());
        attrsRaw.forEach((k, v) -> attributes.put(k, String.valueOf(v)));

        List<String> signedBy = new ArrayList<>();
        Map<String, Object> signedByRaw = (Map<String, Object>) placement.getOrDefault("signedBy", Map.of());
        if (signedByRaw.containsKey("anyOf")) {
            ((List<String>) signedByRaw.get("anyOf")).forEach(signedBy::add);
        }
        if (signedByRaw.containsKey("allOf")) {
            ((List<String>) signedByRaw.get("allOf")).forEach(signedBy::add);
        }

        Map<String, PricingConfig> pricing = new LinkedHashMap<>();
        Map<String, Object> pricingRaw = (Map<String, Object>) placement.getOrDefault("pricing", Map.of());
        for (var entry : pricingRaw.entrySet()) {
            Map<String, Object> priceMap = (Map<String, Object>) entry.getValue();
            String denom = (String) priceMap.getOrDefault("denom", "uakt");
            String amount = String.valueOf(priceMap.getOrDefault("amount", "0"));
            pricing.put(entry.getKey(), new PricingConfig(denom, amount));
        }

        return new PlacementConfig(attributes, signedBy, pricing);
    }

    private static List<DeploymentGroup> buildGroups(
            Map<String, ServiceDefinition> services,
            Map<String, ProfileCompute> computes,
            Map<String, PlacementConfig> placements) {
        List<DeploymentGroup> groups = new ArrayList<>();
        int gseq = 1;
        for (var placementEntry : placements.entrySet()) {
            List<GroupResource> resources = new ArrayList<>();
            PlacementConfig placement = placementEntry.getValue();
            for (var pricingEntry : placement.pricing().entrySet()) {
                String serviceName = pricingEntry.getKey();
                ServiceDefinition svc = services.get(serviceName);
                ProfileCompute compute = computes.get(serviceName);
                if (svc != null && compute != null) {
                    resources.add(new GroupResource(
                            serviceName, compute, svc.count(), pricingEntry.getValue()));
                }
            }
            groups.add(new DeploymentGroup(placementEntry.getKey(), gseq++,
                    placement.attributes(), placement.signedBy(), resources));
        }
        return groups;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> storageNode(Object node) {
        if (node instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (node instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return (Map<String, Object>) first;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> toStringMap(Object obj) {
        if (obj instanceof Map) {
            Map<String, String> result = new LinkedHashMap<>();
            ((Map<?, ?>) obj).forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
            return result;
        }
        if (obj instanceof List) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Object item : (List<?>) obj) {
                String s = String.valueOf(item);
                int eq = s.indexOf('=');
                if (eq > 0) result.put(s.substring(0, eq), s.substring(eq + 1));
            }
            return result;
        }
        return Map.of();
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(obj));
    }
}
