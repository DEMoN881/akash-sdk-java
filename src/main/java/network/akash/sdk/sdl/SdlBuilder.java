package network.akash.sdk.sdl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an SDL from Java, from existing YAML, or from both.
 * {@link #yaml()} writes the document. {@link #build()} parses that document,
 * so the result is the same object {@link SdlParser} would produce.
 * Calling {@link #service(String)} or {@link #group(String)} again with the same
 * name edits that entry instead of adding a second one.
 */
public final class SdlBuilder {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Map<String, Object> root;

    private SdlBuilder(Map<String, Object> root) {
        this.root = root;
    }

    public static SdlBuilder create() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "2.0");
        root.put("services", new LinkedHashMap<String, Object>());
        Map<String, Object> profiles = new LinkedHashMap<>();
        profiles.put("compute", new LinkedHashMap<String, Object>());
        profiles.put("placement", new LinkedHashMap<String, Object>());
        root.put("profiles", profiles);
        root.put("deployment", new LinkedHashMap<String, Object>());
        return new SdlBuilder(root);
    }

    public static SdlBuilder from(String yaml) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = YAML.readValue(yaml, Map.class);
        return new SdlBuilder(mutable(parsed));
    }

    public static SdlBuilder from(Sdl sdl) throws Exception {
        return from(fromSdl(sdl).yaml());
    }

    public SdlBuilder version(String version) {
        root.put("version", version);
        return this;
    }

    public Service service(String name) {
        return new Service(name);
    }

    public Group group(String name) {
        return new Group(name);
    }

    public String yaml() {
        try {
            return YAML.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("cannot write SDL", e);
        }
    }

    public Sdl build() throws Exception {
        Sdl sdl = SdlParser.parse(yaml());
        sdl.validate();
        return sdl;
    }

    public final class Service {
        private final String name;
        private final Map<String, Object> body;

        private Service(String name) {
            this.name = name;
            this.body = map(services(), name);
        }

        public Service image(String image) {
            body.put("image", image);
            return this;
        }

        public Service count(int count) {
            body.put("count", count);
            for (Object placement : deploymentOf(name).values()) {
                if (placement instanceof Map<?, ?> place) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) place;
                    body.put("count", count);
                }
            }
            return this;
        }

        public Service command(String... command) {
            body.put("command", List.of(command));
            return this;
        }

        public Service args(String... args) {
            body.put("args", List.of(args));
            return this;
        }

        public Service env(String key, String value) {
            Object current = body.get("env");
            if (current instanceof List<?> list) {
                Map<String, Object> converted = new LinkedHashMap<>();
                for (Object item : list) {
                    String line = String.valueOf(item);
                    int eq = line.indexOf('=');
                    if (eq > 0) converted.put(line.substring(0, eq), line.substring(eq + 1));
                }
                body.put("env", converted);
            }
            map(body, "env").put(key, value);
            return this;
        }

        public Service expose(int port) {
            return expose(port, port, "tcp", true);
        }

        public Service expose(int port, int externalPort, String proto, boolean global) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("port", port);
            entry.put("as", externalPort);
            entry.put("proto", proto);
            entry.put("to", List.of(Map.of("global", global)));
            list(body, "expose").add(entry);
            return this;
        }

        public Service compute(String cpu, String memory, String storage) {
            return compute(cpu, memory, storage, null);
        }

        public Service compute(String cpu, String memory, String storage, String gpu) {
            Map<String, Object> resources = map(map(computeProfiles(), name), "resources");
            resources.put("cpu", Map.of("units", cpu));
            resources.put("memory", Map.of("size", memory));
            resources.put("storage", Map.of("size", storage));
            if (gpu != null && !gpu.isBlank()) {
                resources.put("gpu", Map.of("units", gpu));
            }
            return this;
        }

        public SdlBuilder end() {
            return SdlBuilder.this;
        }
    }

    public final class Group {
        private final String name;
        private final Map<String, Object> body;

        private Group(String name) {
            this.name = name;
            this.body = map(placements(), name);
        }

        public Group attribute(String key, String value) {
            map(body, "attributes").put(key, value);
            return this;
        }

        public Group signedBy(String auditor) {
            list(map(body, "signedBy"), "anyOf").add(auditor);
            return this;
        }

        public Group price(String service, String denom, String amount) {
            Map<String, Object> price = new LinkedHashMap<>();
            price.put("denom", denom);
            price.put("amount", amount);
            map(body, "pricing").put(service, price);
            Map<String, Object> link = map(map(deployment(), service), name);
            link.put("profile", service);
            link.put("count", serviceCount(service));
            return this;
        }

        public SdlBuilder end() {
            return SdlBuilder.this;
        }
    }

    private Map<String, Object> services() {
        return map(root, "services");
    }

    private Map<String, Object> profiles() {
        return map(root, "profiles");
    }

    private Map<String, Object> computeProfiles() {
        return map(profiles(), "compute");
    }

    private Map<String, Object> placements() {
        return map(profiles(), "placement");
    }

    private Map<String, Object> deployment() {
        return map(root, "deployment");
    }

    private Map<String, Object> deploymentOf(String service) {
        return map(deployment(), service);
    }

    private int serviceCount(String service) {
        Object count = services().containsKey(service) ? map(services(), service).get("count") : null;
        if (count instanceof Number number) {
            return number.intValue();
        }
        if (count != null) {
            return Integer.parseInt(String.valueOf(count));
        }
        return 1;
    }

    private static SdlBuilder fromSdl(Sdl sdl) {
        SdlBuilder builder = create().version(sdl.version());
        for (var service : sdl.services().entrySet()) {
            Service current = builder.service(service.getKey()).image(service.getValue().image()).count(service.getValue().count());
            if (!service.getValue().command().isEmpty()) {
                current.command(service.getValue().command().toArray(String[]::new));
            }
            if (!service.getValue().args().isEmpty()) {
                current.args(service.getValue().args().toArray(String[]::new));
            }
            service.getValue().env().forEach(current::env);
            for (ServiceExpose expose : service.getValue().expose()) {
                current.expose(expose.port(), expose.externalPort(), expose.proto(), expose.global());
            }
            ProfileCompute compute = sdl.computeProfiles().get(service.getKey());
            if (compute != null) {
                current.compute(
                        compute.cpu().raw(),
                        compute.memory().raw(),
                        compute.storage().raw(),
                        compute.gpu() == null ? null : compute.gpu().raw());
            }
        }
        for (var placement : sdl.placements().entrySet()) {
            Group group = builder.group(placement.getKey());
            placement.getValue().attributes().forEach(group::attribute);
            placement.getValue().signedBy().forEach(group::signedBy);
            placement.getValue().pricing().forEach((service, price) ->
                    group.price(service, price.denom(), price.amount()));
        }
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map<?, ?> existing) {
            return (Map<String, Object>) existing;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        parent.put(key, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof List<?> existing) {
            return (List<Object>) existing;
        }
        List<Object> created = new ArrayList<>();
        parent.put(key, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutable(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                copy.put(entry.getKey(), mutable((Map<String, Object>) map));
            } else if (value instanceof List<?> list) {
                copy.put(entry.getKey(), mutableList(list));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> mutableList(List<?> source) {
        List<Object> copy = new ArrayList<>();
        for (Object value : source) {
            if (value instanceof Map<?, ?> map) {
                copy.add(mutable((Map<String, Object>) map));
            } else if (value instanceof List<?> list) {
                copy.add(mutableList(list));
            } else {
                copy.add(value);
            }
        }
        return copy;
    }
}
