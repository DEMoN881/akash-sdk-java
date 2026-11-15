package network.akash.sdk.sdl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SdlParserTest {

    private static final String SIMPLE_SDL = """
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
                dcloud:
                  attributes:
                    region: us-west
                  signedBy:
                    anyOf:
                      - akash1abc
                  pricing:
                    web:
                      denom: uakt
                      amount: 1000
            """;

    @Test
    void parseSimpleSdl() throws Exception {
        Sdl sdl = SdlParser.parse(SIMPLE_SDL);
        assertEquals("2.0", sdl.version());
        assertEquals(1, sdl.services().size());
        assertTrue(sdl.services().containsKey("web"));
    }

    @Test
    void serviceDefinitionParsed() throws Exception {
        Sdl sdl = SdlParser.parse(SIMPLE_SDL);
        ServiceDefinition web = sdl.services().get("web");
        assertEquals("nginx:latest", web.image());
        assertEquals(1, web.count());
        assertEquals(1, web.expose().size());
        assertEquals(80, web.expose().get(0).port());
        assertTrue(web.expose().get(0).global());
    }

    @Test
    void computeProfileParsed() throws Exception {
        Sdl sdl = SdlParser.parse(SIMPLE_SDL);
        ProfileCompute compute = sdl.computeProfiles().get("web");
        assertNotNull(compute);
        assertEquals(500.0, compute.cpu().toMilliCpu(), 0.01);
        assertEquals(512 * 1024 * 1024L, compute.memory().toBytes());
        assertEquals(1024L * 1024 * 1024, compute.storage().toBytes());
    }

    @Test
    void placementParsed() throws Exception {
        Sdl sdl = SdlParser.parse(SIMPLE_SDL);
        PlacementConfig placement = sdl.placements().get("dcloud");
        assertNotNull(placement);
        assertEquals("us-west", placement.attributes().get("region"));
        assertTrue(placement.signedBy().contains("akash1abc"));
        assertEquals("uakt", placement.pricing().get("web").denom());
        assertEquals("1000", placement.pricing().get("web").amount());
    }

    @Test
    void deploymentGroupsBuilt() throws Exception {
        Sdl sdl = SdlParser.parse(SIMPLE_SDL);
        assertEquals(1, sdl.groups().size());
        DeploymentGroup group = sdl.groups().get(0);
        assertEquals("dcloud", group.name());
        assertEquals(1, group.gseq());
        assertEquals(1, group.resources().size());
        assertEquals("web", group.resources().get(0).serviceName());
    }

    @Test
    void validateRejectsEmptyServices() {
        Sdl empty = new Sdl("2.0", java.util.Map.of(),
                java.util.Map.of(), java.util.Map.of(), java.util.List.of());
        assertThrows(IllegalArgumentException.class, empty::validate);
    }

    @Test
    void validateRejectsNoImage() {
        ServiceDefinition noImg = new ServiceDefinition("", java.util.List.of(),
                java.util.List.of(), java.util.Map.of(), java.util.List.of(), 1);
        Sdl sdl = new Sdl("2.0",
                java.util.Map.of("web", noImg),
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.List.of(new DeploymentGroup("g", 1,
                        java.util.Map.of(), java.util.List.of(), java.util.List.of())));
        assertThrows(IllegalArgumentException.class, sdl::validate);
    }

    @Test
    void resourceUnitsSizeConversion() {
        assertEquals(1024, new ResourceUnits("1Ki").toBytes());
        assertEquals(1024 * 1024, new ResourceUnits("1Mi").toBytes());
        assertEquals(1024L * 1024 * 1024, new ResourceUnits("1Gi").toBytes());
        assertEquals(1000, new ResourceUnits("1K").toBytes());
        assertEquals(1_000_000, new ResourceUnits("1M").toBytes());
    }

    @Test
    void resourceUnitsCpuConversion() {
        assertEquals(1000.0, new ResourceUnits("1").toMilliCpu(), 0.01);
        assertEquals(500.0, new ResourceUnits("0.5").toMilliCpu(), 0.01);
        assertEquals(100.0, new ResourceUnits("0.1").toMilliCpu(), 0.01);
    }

    @Test
    void multiServiceSdl() throws Exception {
        String multiSdl = """
                version: "2.0"
                services:
                  web:
                    image: nginx:latest
                    expose:
                      - port: 80
                        to:
                          - global: true
                    count: 2
                  api:
                    image: myapi:latest
                    env:
                      - PORT=3000
                    expose:
                      - port: 3000
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
                          size: 256Mi
                        storage:
                          size: 512Mi
                    api:
                      resources:
                        cpu:
                          units: 1
                        memory:
                          size: 512Mi
                        storage:
                          size: 1Gi
                  placement:
                    dcloud:
                      pricing:
                        web:
                          denom: uakt
                          amount: 500
                        api:
                          denom: uakt
                          amount: 1000
                """;
        Sdl sdl = SdlParser.parse(multiSdl);
        assertEquals(2, sdl.services().size());
        assertEquals(2, sdl.services().get("web").count());
        assertEquals(1, sdl.services().get("api").count());
        assertEquals(2, sdl.groups().get(0).resources().size());
    }
}
