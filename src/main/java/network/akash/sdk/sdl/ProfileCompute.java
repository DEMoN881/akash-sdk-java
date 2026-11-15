package network.akash.sdk.sdl;

public record ProfileCompute(
        ResourceUnits cpu,
        ResourceUnits memory,
        ResourceUnits storage,
        ResourceUnits gpu
) {}
