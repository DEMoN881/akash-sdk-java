package network.akash.sdk.types.deployment;

import network.akash.sdk.cosmos.AccAddress;
import java.util.Objects;

/**
 * Deployment identifier (owner + dseq).
 * Port of akash-api/go/node/deployment/v1beta3.DeploymentID
 */
public record DeploymentID(String owner, long dseq) {

    public DeploymentID {
        Objects.requireNonNull(owner, "owner must not be null");
    }

    public boolean equals(DeploymentID other) {
        return owner.equals(other.owner) && dseq == other.dseq;
    }

    public void validate() {
        AccAddress.fromBech32(owner);
        if (dseq == 0) {
            throw new IllegalArgumentException("DeploymentID: invalid deployment sequence");
        }
    }

    public AccAddress getOwnerAddress() {
        return AccAddress.fromBech32(owner);
    }

    public static DeploymentID parse(String val) {
        String[] parts = val.split("/");
        return parsePath(parts);
    }

    public static DeploymentID parsePath(String[] parts) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid deployment ID path");
        }
        AccAddress.fromBech32(parts[0]);
        long dseq = Long.parseUnsignedLong(parts[1]);
        return new DeploymentID(parts[0], dseq);
    }

    @Override
    public String toString() {
        return owner + "/" + dseq;
    }
}
