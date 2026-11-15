package network.akash.sdk.types.base;

import java.util.Objects;

/**
 * Key-value attribute used across Akash modules (providers, deployments).
 * Port of akash-api/go/node/base/v1beta1.Attribute
 */
public record Attribute(String key, String value) {

    public Attribute {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    public void validate() {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("attribute key cannot be empty");
        }
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
