package network.akash.sdk.keyring;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoryKeyring implements Keyring {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void save(String name, byte[] privateKey) {
        store.put(name, privateKey.clone());
    }

    @Override
    public Optional<byte[]> load(String name) {
        byte[] data = store.get(name);
        return data != null ? Optional.of(data.clone()) : Optional.empty();
    }

    @Override
    public void delete(String name) {
        store.remove(name);
    }

    @Override
    public List<String> list() {
        return new ArrayList<>(store.keySet());
    }

    @Override
    public boolean exists(String name) {
        return store.containsKey(name);
    }
}
