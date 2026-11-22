package network.akash.sdk.types.deployment;

import akash.deployment.v1beta4.*;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the Group proto message correctly carries group_spec (field 3)
 * and created_at (field 4), which were added to enable dynamic pricing
 * calculations from on-chain deployment data.
 */
class GroupProtoTest {

    private static final String OWNER = "akash1qqqsyqcyq5rqwzqfpg9scrgwpugqzlneg4jx5";

    // ── round-trip: build → serialize → parse ────────────────────────

    @Test
    void groupSpecRoundTrip() throws Exception {
        var cpu = CPU.newBuilder()
                .setUnits(ResourceValue.newBuilder()
                        .setVal(ByteString.copyFromUtf8("1000"))
                        .build())
                .build();
        var mem = Memory.newBuilder()
                .setQuantity(ResourceValue.newBuilder()
                        .setVal(ByteString.copyFromUtf8("536870912"))
                        .build())
                .build();
        var stor = Storage.newBuilder()
                .setName("default")
                .setQuantity(ResourceValue.newBuilder()
                        .setVal(ByteString.copyFromUtf8("536870912"))
                        .build())
                .build();

        var resources = Resources.newBuilder()
                .setId(1)
                .setCpu(cpu)
                .setMemory(mem)
                .addStorage(stor)
                .build();

        var resourceUnit = ResourceUnit.newBuilder()
                .setResource(resources)
                .setCount(1)
                .build();

        var groupSpec = GroupSpec.newBuilder()
                .setName("dcloud")
                .addResources(resourceUnit)
                .build();

        var groupId = akash.deployment.v1beta4.GroupID.newBuilder()
                .setOwner(OWNER)
                .setDseq(42)
                .setGseq(1)
                .build();

        var group = Group.newBuilder()
                .setGroupId(groupId)
                .setState("open")
                .setGroupSpec(groupSpec)
                .setCreatedAt(123456789L)
                .build();

        // serialize → deserialize
        byte[] bytes = group.toByteArray();
        var parsed = Group.parseFrom(bytes);

        assertEquals("open", parsed.getState());
        assertEquals(123456789L, parsed.getCreatedAt());
        assertTrue(parsed.hasGroupSpec());
        assertEquals("dcloud", parsed.getGroupSpec().getName());
        assertEquals(1, parsed.getGroupSpec().getResourcesCount());

        var parsedRes = parsed.getGroupSpec().getResources(0).getResource();
        assertEquals(1, parsedRes.getId());
        assertTrue(parsedRes.hasCpu());
        assertTrue(parsedRes.hasMemory());
        assertEquals(1, parsedRes.getStorageCount());
        assertEquals("default", parsedRes.getStorage(0).getName());
    }

    // ── default values when group_spec is absent ─────────────────────

    @Test
    void groupWithoutGroupSpecHasDefaults() {
        var group = Group.newBuilder()
                .setGroupId(akash.deployment.v1beta4.GroupID.newBuilder()
                        .setOwner(OWNER)
                        .setDseq(1)
                        .setGseq(1)
                        .build())
                .setState("open")
                .build();

        assertFalse(group.hasGroupSpec());
        assertEquals(0, group.getCreatedAt());
        assertEquals(0, group.getGroupSpec().getResourcesCount());
    }

    // ── QueryDeploymentResponse carries groups with spec ─────────────

    @Test
    void deploymentResponseContainsGroupSpec() throws Exception {
        var groupSpec = GroupSpec.newBuilder()
                .setName("web")
                .addResources(ResourceUnit.newBuilder()
                        .setResource(Resources.newBuilder()
                                .setId(1)
                                .setCpu(CPU.newBuilder()
                                        .setUnits(ResourceValue.newBuilder()
                                                .setVal(ByteString.copyFromUtf8("500"))
                                                .build())
                                        .build())
                                .build())
                        .setCount(2)
                        .build())
                .build();

        var group = Group.newBuilder()
                .setGroupId(akash.deployment.v1beta4.GroupID.newBuilder()
                        .setOwner(OWNER).setDseq(10).setGseq(1).build())
                .setState("open")
                .setGroupSpec(groupSpec)
                .setCreatedAt(999L)
                .build();

        var deployment = Deployment.newBuilder()
                .setDeploymentId(akash.deployment.v1beta4.DeploymentID.newBuilder()
                        .setOwner(OWNER).setDseq(10).build())
                .setState("active")
                .build();

        var resp = QueryDeploymentResponse.newBuilder()
                .setDeployment(deployment)
                .addGroups(group)
                .build();

        // round-trip
        var parsed = QueryDeploymentResponse.parseFrom(resp.toByteArray());
        assertEquals(1, parsed.getGroupsCount());
        assertTrue(parsed.getGroups(0).hasGroupSpec());
        assertEquals("web", parsed.getGroups(0).getGroupSpec().getName());
        assertEquals(2, parsed.getGroups(0).getGroupSpec().getResources(0).getCount());
        assertEquals(999L, parsed.getGroups(0).getCreatedAt());
    }

    // ── GPU resources in group spec ──────────────────────────────────

    @Test
    void groupSpecWithGpuResource() {
        var gpu = GPU.newBuilder()
                .setUnits(ResourceValue.newBuilder()
                        .setVal(ByteString.copyFromUtf8("1"))
                        .build())
                .build();

        var resources = Resources.newBuilder()
                .setId(1)
                .setGpu(gpu)
                .build();

        var groupSpec = GroupSpec.newBuilder()
                .setName("gpu-workload")
                .addResources(ResourceUnit.newBuilder()
                        .setResource(resources)
                        .setCount(1)
                        .build())
                .build();

        var group = Group.newBuilder()
                .setGroupId(akash.deployment.v1beta4.GroupID.newBuilder()
                        .setOwner(OWNER).setDseq(5).setGseq(1).build())
                .setState("open")
                .setGroupSpec(groupSpec)
                .build();

        assertTrue(group.getGroupSpec().getResources(0).getResource().hasGpu());
        assertEquals("1", group.getGroupSpec().getResources(0)
                .getResource().getGpu().getUnits().getVal().toStringUtf8());
    }

    // ── multiple resource units ──────────────────────────────────────

    @Test
    void groupSpecMultipleResourceUnits() {
        var webRes = ResourceUnit.newBuilder()
                .setResource(Resources.newBuilder().setId(1)
                        .setCpu(CPU.newBuilder()
                                .setUnits(ResourceValue.newBuilder()
                                        .setVal(ByteString.copyFromUtf8("100")).build())
                                .build())
                        .build())
                .setCount(3)
                .build();

        var workerRes = ResourceUnit.newBuilder()
                .setResource(Resources.newBuilder().setId(2)
                        .setCpu(CPU.newBuilder()
                                .setUnits(ResourceValue.newBuilder()
                                        .setVal(ByteString.copyFromUtf8("2000")).build())
                                .build())
                        .build())
                .setCount(1)
                .build();

        var groupSpec = GroupSpec.newBuilder()
                .setName("multi")
                .addResources(webRes)
                .addResources(workerRes)
                .build();

        assertEquals(2, groupSpec.getResourcesCount());
        assertEquals(3, groupSpec.getResources(0).getCount());
        assertEquals(1, groupSpec.getResources(1).getCount());
        assertEquals("100", groupSpec.getResources(0).getResource()
                .getCpu().getUnits().getVal().toStringUtf8());
        assertEquals("2000", groupSpec.getResources(1).getResource()
                .getCpu().getUnits().getVal().toStringUtf8());
    }

    // ── field numbers match the proto definition ─────────────────────

    @Test
    void groupFieldNumbers() {
        // group_spec = field 3, created_at = field 4
        assertEquals(3, Group.GROUP_SPEC_FIELD_NUMBER);
        assertEquals(4, Group.CREATED_AT_FIELD_NUMBER);
    }

    // ── endpoints in resources ────────────────────────────────────────

    @Test
    void groupSpecWithEndpoints() {
        var endpoint = Endpoint.newBuilder()
                .setKind(Endpoint.Kind.SHARED_HTTP)
                .setSequenceNumber(0)
                .build();

        var resources = Resources.newBuilder()
                .setId(1)
                .addEndpoints(endpoint)
                .build();

        var groupSpec = GroupSpec.newBuilder()
                .setName("with-endpoint")
                .addResources(ResourceUnit.newBuilder()
                        .setResource(resources)
                        .setCount(1)
                        .build())
                .build();

        assertEquals(1, groupSpec.getResources(0).getResource().getEndpointsCount());
        assertEquals(Endpoint.Kind.SHARED_HTTP,
                groupSpec.getResources(0).getResource().getEndpoints(0).getKind());
    }
}
