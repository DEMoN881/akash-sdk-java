package network.akash.sdk.manifest;

import network.akash.sdk.sdl.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManifestBuilderTest {

    @Test
    void buildFromSdl() throws Exception {
        Sdl sdl = SdlParser.parse("""
                version: "2.0"
                services:
                  web:
                    image: nginx:latest
                    expose:
                      - port: 80
                        as: 80
                        proto: tcp
                        to:
                          - global: true
                    count: 1
                profiles:
                  compute:
                    web:
                      resources:
                        cpu:
                          units: 0.5
                        memory:
                          size: 512Mi
                        storage:
                          size: 1Gi
                  placement:
                    dc1:
                      pricing:
                        web:
                          denom: uakt
                          amount: 1000
                """);

        Manifest manifest = ManifestBuilder.fromSdl(sdl);
        assertEquals(1, manifest.groups().size());
        assertEquals("dc1", manifest.groups().get(0).name());
        assertEquals(1, manifest.groups().get(0).services().size());
        assertEquals("nginx:latest", manifest.groups().get(0).services().get(0).image());
    }

    @Test
    void manifestServiceResources() throws Exception {
        Sdl sdl = SdlParser.parse("""
                version: "2.0"
                services:
                  app:
                    image: myapp:1.0
                    expose:
                      - port: 8080
                        to:
                          - global: true
                    count: 3
                profiles:
                  compute:
                    app:
                      resources:
                        cpu:
                          units: 2
                        memory:
                          size: 1Gi
                        storage:
                          size: 5Gi
                  placement:
                    dc:
                      pricing:
                        app:
                          denom: uakt
                          amount: 5000
                """);

        Manifest manifest = ManifestBuilder.fromSdl(sdl);
        Manifest.ManifestService svc = manifest.groups().get(0).services().get(0);
        assertEquals(3, svc.count());
        assertEquals(2000, svc.resources().cpuMillis());
        assertEquals(1024L * 1024 * 1024, svc.resources().memoryBytes());
    }

    @Test
    void toSubmitFormat() {
        Manifest.ManifestService svc = new Manifest.ManifestService(
                "web", "nginx", List.of(), List.of(), Map.of(), 1,
                List.of(new Manifest.ServiceExpose(80, 80, "tcp", true)),
                new Manifest.Resources(500, 512_000_000L, 1_000_000_000L));
        Manifest manifest = new Manifest(
                List.of(new Manifest.ManifestGroup("grp", List.of(svc))));

        List<Map<String, Object>> json = manifest.toSubmitFormat();
        assertEquals(1, json.size());
        assertEquals("grp", json.get(0).get("name"));
    }

    @Test
    void serviceExposeMapping() throws Exception {
        Sdl sdl = SdlParser.parse("""
                version: "2.0"
                services:
                  web:
                    image: nginx
                    expose:
                      - port: 80
                        as: 8080
                        proto: tcp
                        to:
                          - global: true
                    count: 1
                profiles:
                  compute:
                    web:
                      resources:
                        cpu:
                          units: 0.1
                        memory:
                          size: 128Mi
                        storage:
                          size: 256Mi
                  placement:
                    dc:
                      pricing:
                        web:
                          denom: uakt
                          amount: 100
                """);

        Manifest manifest = ManifestBuilder.fromSdl(sdl);
        Manifest.ServiceExpose expose = manifest.groups().get(0).services().get(0).expose().get(0);
        assertEquals(80, expose.port());
        assertEquals(8080, expose.externalPort());
        assertTrue(expose.global());
    }

    @Test
    void envPassedThrough() {
        Manifest.ManifestService svc = new Manifest.ManifestService(
                "api", "myapi", List.of(), List.of(),
                Map.of("PORT", "3000", "DB_HOST", "localhost"), 1,
                List.of(), new Manifest.Resources(100, 256_000_000, 512_000_000));

        Map<String, Object> map = svc.toMap();
        @SuppressWarnings("unchecked")
        List<String> env = (List<String>) map.get("env");
        assertEquals(2, env.size());
    }
}
