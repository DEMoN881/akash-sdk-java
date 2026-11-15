package network.akash.sdk.sdl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SdlBuilderTest {

    @Test
    void buildsAGroupFromJava() throws Exception {
        Sdl sdl = SdlBuilder.create()
                .service("web")
                    .image("nginx:1.27-alpine")
                    .expose(80)
                    .compute("0.1", "128Mi", "128Mi")
                    .end()
                .group("test")
                    .price("web", "uact", "50000")
                    .end()
                .build();

        assertEquals("nginx:1.27-alpine", sdl.services().get("web").image());
        assertTrue(sdl.services().get("web").expose().get(0).global());
        assertEquals("test", sdl.groups().get(0).name());
        assertEquals("uact", sdl.placements().get("test").pricing().get("web").denom());
        assertEquals("50000", sdl.placements().get("test").pricing().get("web").amount());
    }

    @Test
    void sameGroupNameEditsTheExistingGroup() throws Exception {
        Sdl sdl = SdlBuilder.create()
                .service("web").image("nginx:1.27-alpine").expose(80).compute("0.1", "128Mi", "128Mi").end()
                .service("api").image("api:1").expose(8080).compute("0.1", "128Mi", "128Mi").end()
                .group("app").price("web", "uact", "10").end()
                .group("app").price("api", "uact", "20").end()
                .build();

        assertEquals(1, sdl.groups().size());
        assertEquals(2, sdl.groups().get(0).resources().size());
    }

    @Test
    void patchesExistingYaml() throws Exception {
        String source = """
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
                    dcloud:
                      attributes:
                        region: us-west
                      pricing:
                        web:
                          denom: uakt
                          amount: 1000
                """;

        Sdl sdl = SdlBuilder.from(source)
                .service("web").image("nginx:1.27-alpine").end()
                .build();

        assertEquals("nginx:1.27-alpine", sdl.services().get("web").image());
        assertEquals("us-west", sdl.placements().get("dcloud").attributes().get("region"));
        assertEquals("1000", sdl.placements().get("dcloud").pricing().get("web").amount());
    }

    @Test
    void keepsListStorageAndListEnv() throws Exception {
        String source = """
                version: "2.0"
                services:
                  web:
                    image: nginx:latest
                    env:
                      - MODE=prod
                    expose:
                      - port: 80
                        to:
                          - global: true
                profiles:
                  compute:
                    web:
                      resources:
                        cpu:
                          units: 0.1
                        memory:
                          size: 128Mi
                        storage:
                          - size: 1Gi
                  placement:
                    app:
                      pricing:
                        web:
                          denom: uact
                          amount: 10
                """;

        Sdl sdl = SdlBuilder.from(source)
                .service("web").env("REGION", "eu").end()
                .build();

        assertEquals("prod", sdl.services().get("web").env().get("MODE"));
        assertEquals("eu", sdl.services().get("web").env().get("REGION"));
        assertEquals(1024L * 1024 * 1024, sdl.computeProfiles().get("web").storage().toBytes());
    }
}
