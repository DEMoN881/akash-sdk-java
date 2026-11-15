package network.akash.sdk.types.market;

import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.GroupID;
import java.util.Objects;

/**
 * Order identifier (owner + dseq + gseq + oseq).
 * Port of akash-api/go/node/market/v1beta4.OrderID
 */
public record OrderID(String owner, long dseq, int gseq, int oseq) {

    public OrderID {
        Objects.requireNonNull(owner, "owner must not be null");
    }

    public static OrderID make(GroupID groupId, int oseq) {
        return new OrderID(groupId.owner(), groupId.dseq(), groupId.gseq(), oseq);
    }

    public GroupID groupID() {
        return new GroupID(owner, dseq, gseq);
    }

    public boolean equals(OrderID other) {
        return groupID().equals(other.groupID()) && oseq == other.oseq;
    }

    public void validate() {
        groupID().validate();
        if (oseq == 0) {
            throw new IllegalArgumentException("OrderID: invalid order sequence");
        }
    }

    @Override
    public String toString() {
        return groupID() + "/" + oseq;
    }
}
