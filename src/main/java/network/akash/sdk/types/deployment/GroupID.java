package network.akash.sdk.types.deployment;

import java.util.Objects;

/**
 * Group identifier (owner + dseq + gseq).
 * Port of chain-sdk go/node/deployment/v1.GroupID
 */
public record GroupID(String owner, long dseq, int gseq) {

    public GroupID {
        Objects.requireNonNull(owner, "owner must not be null");
    }

    public static GroupID make(DeploymentID deploymentId, int gseq) {
        return new GroupID(deploymentId.owner(), deploymentId.dseq(), gseq);
    }

    public DeploymentID deploymentID() {
        return new DeploymentID(owner, dseq);
    }

    public boolean equals(GroupID other) {
        return deploymentID().equals(other.deploymentID()) && gseq == other.gseq;
    }

    public void validate() {
        deploymentID().validate();
        if (gseq == 0) {
            throw new IllegalArgumentException("GroupID: invalid group sequence");
        }
    }

    @Override
    public String toString() {
        return deploymentID() + "/" + gseq;
    }
}
