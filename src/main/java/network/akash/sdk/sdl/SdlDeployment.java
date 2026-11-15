package network.akash.sdk.sdl;

import akash.base.v1beta1.Attribute;
import akash.deployment.v1beta4.CPU;
import akash.deployment.v1beta4.Endpoint;
import akash.deployment.v1beta4.GPU;
import akash.deployment.v1beta4.GroupRequirements;
import akash.deployment.v1beta4.GroupSignedBy;
import akash.deployment.v1beta4.GroupSpec;
import akash.deployment.v1beta4.Memory;
import akash.deployment.v1beta4.ResourceUnit;
import akash.deployment.v1beta4.ResourceValue;
import akash.deployment.v1beta4.Resources;
import akash.deployment.v1beta4.Storage;
import com.google.protobuf.ByteString;
import cosmos.base.v1beta1.CoinOuterClass;
import network.akash.sdk.cosmos.DecCoin;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns an {@link Sdl} into the two values {@code MsgCreateDeployment} needs:
 * one GroupSpec per group, and the 32-byte manifest version the provider checks.
 * {@link #manifestJson()} is the body of the provider PUT. Its hash is {@link #version()}.
 */
public final class SdlDeployment {

    private final List<byte[]> groups;
    private final byte[] version;
    private final String manifestJson;

    private SdlDeployment(List<byte[]> groups, byte[] version, String manifestJson) {
        this.groups = groups;
        this.version = version;
        this.manifestJson = manifestJson;
    }

    public static SdlDeployment from(Sdl sdl) {
        if (sdl.groups() == null || sdl.groups().isEmpty()) {
            throw new IllegalArgumentException("SDL has no groups");
        }
        List<byte[]> specs = new ArrayList<>();
        List<Object> manifest = new ArrayList<>();
        for (DeploymentGroup group : sdl.groups()) {
            if (group.resources() == null || group.resources().isEmpty()) {
                throw new IllegalArgumentException(
                        "group " + group.name() + " has no services; price() must use the service name");
            }
            specs.add(groupSpec(group, sdl).toByteArray());
            manifest.add(manifestGroup(group, sdl));
        }
        String json = canonicalJson(manifest);
        return new SdlDeployment(List.copyOf(specs), sha256(json), json);
    }

    public List<byte[]> groups() {
        return groups;
    }

    public byte[] version() {
        return version.clone();
    }

    public String manifestJson() {
        return manifestJson;
    }

    private static GroupSpec groupSpec(DeploymentGroup group, Sdl sdl) {
        GroupSignedBy.Builder signed = GroupSignedBy.newBuilder();
        if (group.signedBy() != null) {
            group.signedBy().forEach(signed::addAnyOf);
        }
        GroupRequirements.Builder requirements = GroupRequirements.newBuilder().setSignedBy(signed);
        if (group.attributes() != null) {
            List<String> keys = new ArrayList<>(group.attributes().keySet());
            Collections.sort(keys);
            for (String key : keys) {
                requirements.addAttributes(Attribute.newBuilder()
                        .setKey(key)
                        .setValue(group.attributes().get(key)));
            }
        }
        GroupSpec.Builder spec = GroupSpec.newBuilder()
                .setName(group.name())
                .setRequirements(requirements);
        int id = 1;
        for (GroupResource resource : group.resources()) {
            spec.addResources(resourceUnit(id++, resource, sdl));
        }
        return spec.build();
    }

    private static ResourceUnit resourceUnit(int id, GroupResource resource, Sdl sdl) {
        ServiceDefinition service = sdl.services().get(resource.serviceName());
        ProfileCompute compute = resource.compute();
        Resources.Builder resources = Resources.newBuilder()
                .setId(id)
                .setCpu(CPU.newBuilder().setUnits(value(milliCpu(compute.cpu().raw()))))
                .setMemory(Memory.newBuilder().setQuantity(value(Long.toString(compute.memory().toBytes()))))
                .addStorage(Storage.newBuilder()
                        .setName("default")
                        .setQuantity(value(Long.toString(compute.storage().toBytes()))))
                .setGpu(GPU.newBuilder().setUnits(value(gpuUnits(compute))));
        int sequence = 0;
        for (ServiceExpose expose : service.expose()) {
            if (!expose.global()) {
                continue;
            }
            Endpoint.Builder endpoint = Endpoint.newBuilder();
            if (!sharedHttp(expose)) {
                endpoint.setKind(Endpoint.Kind.RANDOM_PORT);
            }
            if (sequence != 0) {
                endpoint.setSequenceNumber(sequence);
            }
            resources.addEndpoints(endpoint);
            sequence++;
        }
        DecCoin price = DecCoin.of(resource.price().denom(), resource.price().amount());
        return ResourceUnit.newBuilder()
                .setResource(resources)
                .setCount(resource.count())
                .setPrice(CoinOuterClass.DecCoin.newBuilder()
                        .setDenom(price.denom())
                        .setAmount(price.protoAmount()))
                .build();
    }

    private static Map<String, Object> manifestGroup(DeploymentGroup group, Sdl sdl) {
        List<Object> services = new ArrayList<>();
        int id = 1;
        for (GroupResource resource : group.resources()) {
            services.add(manifestService(id++, resource, sdl));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", group.name());
        body.put("services", services);
        return body;
    }

    private static Map<String, Object> manifestService(int id, GroupResource resource, Sdl sdl) {
        ServiceDefinition service = sdl.services().get(resource.serviceName());
        ProfileCompute compute = resource.compute();
        List<Object> endpoints = new ArrayList<>();
        List<Object> expose = new ArrayList<>();
        int sequence = 0;
        for (ServiceExpose port : service.expose()) {
            int assigned = port.global() ? sequence++ : 0;
            if (port.global()) {
                Map<String, Object> endpoint = new LinkedHashMap<>();
                if (!sharedHttp(port)) {
                    endpoint.put("kind", Endpoint.Kind.RANDOM_PORT.getNumber());
                }
                endpoint.put("sequence_number", assigned);
                endpoints.add(endpoint);
            }
            expose.add(manifestExpose(port, assigned));
        }

        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("id", id);
        resources.put("cpu", Map.of("units", Map.of("val", milliCpu(compute.cpu().raw()))));
        resources.put("memory", Map.of("size", Map.of("val", Long.toString(compute.memory().toBytes()))));
        resources.put("storage", List.of(storage(compute)));
        resources.put("gpu", Map.of("units", Map.of("val", gpuUnits(compute))));
        resources.put("endpoints", endpoints);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", resource.serviceName());
        body.put("image", service.image());
        body.put("command", stringsOrNull(service.command()));
        body.put("args", stringsOrNull(service.args()));
        body.put("env", envOrNull(service.env()));
        body.put("resources", resources);
        body.put("count", resource.count());
        body.put("expose", expose);
        body.put("credentials", null);
        return body;
    }

    private static Map<String, Object> storage(ProfileCompute compute) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "default");
        body.put("size", Map.of("val", Long.toString(compute.storage().toBytes())));
        return body;
    }

    private static Map<String, Object> manifestExpose(ServiceExpose port, int sequence) {
        Map<String, Object> http = new LinkedHashMap<>();
        http.put("maxBodySize", 0);
        http.put("readTimeout", 0);
        http.put("sendTimeout", 0);
        http.put("nextTries", 0);
        http.put("nextTimeout", 0);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("port", port.port());
        body.put("externalPort", port.externalPort());
        body.put("proto", port.proto() == null ? "TCP" : port.proto().toUpperCase(Locale.ROOT));
        body.put("service", "");
        body.put("global", port.global());
        body.put("hosts", null);
        body.put("httpOptions", http);
        body.put("ip", "");
        body.put("endpointSequenceNumber", sequence);
        return body;
    }

    private static boolean sharedHttp(ServiceExpose port) {
        return port.global()
                && port.externalPort() == 80
                && "tcp".equalsIgnoreCase(port.proto());
    }

    private static String milliCpu(String raw) {
        String text = raw.trim();
        if (text.endsWith("m") || text.endsWith("M")) {
            text = text.substring(0, text.length() - 1);
            return integer(new BigDecimal(text));
        }
        return integer(new BigDecimal(text).movePointRight(3));
    }

    private static String gpuUnits(ProfileCompute compute) {
        if (compute.gpu() == null || compute.gpu().raw() == null || compute.gpu().raw().isBlank()) {
            return "0";
        }
        return integer(new BigDecimal(compute.gpu().raw().trim()));
    }

    private static String integer(BigDecimal value) {
        return value.stripTrailingZeros().toBigIntegerExact().toString();
    }

    private static ResourceValue value(String text) {
        return ResourceValue.newBuilder().setVal(ByteString.copyFromUtf8(text)).build();
    }

    private static Object stringsOrNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values;
    }

    private static Object envOrNull(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        env.forEach((key, val) -> lines.add(key + "=" + val));
        return lines;
    }

    private static byte[] sha256(String json) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String canonicalJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return quote(text);
        }
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(canonicalJson(list.get(i)));
            }
            builder.append(']');
            return builder.toString();
        }
        if (value instanceof Map<?, ?> map) {
            List<String> keys = new ArrayList<>();
            for (Object key : map.keySet()) {
                keys.add(String.valueOf(key));
            }
            Collections.sort(keys);
            StringBuilder builder = new StringBuilder("{");
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                String key = keys.get(i);
                builder.append(quote(key)).append(':').append(canonicalJson(map.get(key)));
            }
            builder.append('}');
            return builder.toString();
        }
        throw new IllegalArgumentException("unsupported json value " + value.getClass().getName());
    }

    private static String quote(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 2);
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                case '<' -> builder.append("\\u003c");
                case '>' -> builder.append("\\u003e");
                case '&' -> builder.append("\\u0026");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        builder.append('"');
        return builder.toString();
    }
}
