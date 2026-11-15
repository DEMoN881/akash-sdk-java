package network.akash.sdk.types.market;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.GroupID;
import java.util.Objects;

/**
 * Bid identifier (order + provider).
 * Port of akash-api/go/node/market/v1beta4.BidID
 */
public record BidID(String owner, long dseq, int gseq, int oseq, String provider) {

    public BidID {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
    }

    public static BidID make(OrderID orderId, AccAddress provider) {
        return new BidID(orderId.owner(), orderId.dseq(), orderId.gseq(), orderId.oseq(), provider.toString());
    }

    public OrderID orderID() {
        return new OrderID(owner, dseq, gseq, oseq);
    }

    public GroupID groupID() {
        return orderID().groupID();
    }

    public DeploymentID deploymentID() {
        return groupID().deploymentID();
    }

    public LeaseID leaseID() {
        return new LeaseID(owner, dseq, gseq, oseq, provider);
    }

    public boolean equals(BidID other) {
        return orderID().equals(other.orderID()) && provider.equals(other.provider);
    }

    public void validate() {
        orderID().validate();
        AccAddress.fromBech32(provider);
        if (owner.equals(provider)) {
            throw new IllegalArgumentException("BidID: self-bid not allowed");
        }
    }

    @Override
    public String toString() {
        return orderID() + "/" + provider;
    }
}
