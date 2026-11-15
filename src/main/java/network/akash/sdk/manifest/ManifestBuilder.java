package network.akash.sdk.manifest;

import network.akash.sdk.sdl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ManifestBuilder {

    private ManifestBuilder() {}

    public static Manifest fromSdl(Sdl sdl) {
        List<Manifest.ManifestGroup> groups = new ArrayList<>();

        for (DeploymentGroup dg : sdl.groups()) {
            List<Manifest.ManifestService> services = new ArrayList<>();

            for (GroupResource gr : dg.resources()) {
                ServiceDefinition svcDef = sdl.services().get(gr.serviceName());
                if (svcDef == null) continue;

                ProfileCompute compute = gr.compute();

                long cpuMillis = (long) compute.cpu().toMilliCpu();
                long memBytes = compute.memory().toBytes();
                long storageBytes = compute.storage().toBytes();

                List<Manifest.ServiceExpose> exposes = svcDef.expose().stream()
                        .map(e -> new Manifest.ServiceExpose(
                                e.port(), e.externalPort(), e.proto(), e.global()))
                        .collect(Collectors.toList());

                services.add(new Manifest.ManifestService(
                        gr.serviceName(),
                        svcDef.image(),
                        svcDef.command(),
                        svcDef.args(),
                        svcDef.env(),
                        gr.count(),
                        exposes,
                        new Manifest.Resources(cpuMillis, memBytes, storageBytes)
                ));
            }

            groups.add(new Manifest.ManifestGroup(dg.name(), services));
        }

        return new Manifest(groups);
    }
}
