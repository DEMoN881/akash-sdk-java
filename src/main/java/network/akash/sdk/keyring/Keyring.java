package network.akash.sdk.keyring;

import java.util.List;
import java.util.Optional;

public interface Keyring {

    void save(String name, byte[] privateKey) throws Exception;

    Optional<byte[]> load(String name) throws Exception;

    void delete(String name) throws Exception;

    List<String> list() throws Exception;

    boolean exists(String name) throws Exception;
}
