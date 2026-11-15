package network.akash.sdk.sdl;

import com.google.protobuf.ByteString;
import network.akash.sdk.cosmos.DecCoin;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class SdlDeploymentTest {

    @Test
    void nginxHashMatchesTheProviderVersion() throws Exception {
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

        SdlDeployment deployment = SdlDeployment.from(sdl);

        assertEquals(
                "d134dc32c3c1c819acfde623ff4d10ec53cc35a52f015c7514e157f328a1c0b0",
                HexFormat.of().formatHex(deployment.version()));
        assertEquals(1, deployment.groups().size());
        assertArrayEquals(helloGroup(), deployment.groups().get(0));
    }

    @Test
    void priceNameMustMatchTheService() throws Exception {
        Sdl sdl = SdlBuilder.create()
                .service("nginx")
                    .image("nginx:1.27-alpine")
                    .expose(80)
                    .compute("0.1", "128Mi", "128Mi")
                    .end()
                .group("app")
                    .price("web", "uact", "500")
                    .end()
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> SdlDeployment.from(sdl));
        assertTrue(error.getMessage().contains("app"));
    }

    private static byte[] helloGroup() {
        ByteString memory = ByteString.copyFromUtf8("134217728");
        var resources = akash.deployment.v1beta4.Resources.newBuilder()
                .setId(1)
                .setCpu(akash.deployment.v1beta4.CPU.newBuilder()
                        .setUnits(akash.deployment.v1beta4.ResourceValue.newBuilder()
                                .setVal(ByteString.copyFromUtf8("100"))))
                .setMemory(akash.deployment.v1beta4.Memory.newBuilder()
                        .setQuantity(akash.deployment.v1beta4.ResourceValue.newBuilder().setVal(memory)))
                .addStorage(akash.deployment.v1beta4.Storage.newBuilder()
                        .setName("default")
                        .setQuantity(akash.deployment.v1beta4.ResourceValue.newBuilder().setVal(memory)))
                .setGpu(akash.deployment.v1beta4.GPU.newBuilder()
                        .setUnits(akash.deployment.v1beta4.ResourceValue.newBuilder()
                                .setVal(ByteString.copyFromUtf8("0"))))
                .addEndpoints(akash.deployment.v1beta4.Endpoint.newBuilder())
                .build();
        return akash.deployment.v1beta4.GroupSpec.newBuilder()
                .setName("test")
                .setRequirements(akash.deployment.v1beta4.GroupRequirements.newBuilder()
                        .setSignedBy(akash.deployment.v1beta4.GroupSignedBy.newBuilder()))
                .addResources(akash.deployment.v1beta4.ResourceUnit.newBuilder()
                        .setResource(resources)
                        .setCount(1)
                        .setPrice(cosmos.base.v1beta1.CoinOuterClass.DecCoin.newBuilder()
                                .setDenom("uact")
                                .setAmount(DecCoin.of("uact", "50000").protoAmount())))
                .build()
                .toByteArray();
    }
}
