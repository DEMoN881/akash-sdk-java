package network.akash.sdk.sdl;

public record ServiceExpose(int port, int externalPort, String proto, boolean global) {}
