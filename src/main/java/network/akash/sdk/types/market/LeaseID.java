package network.akash.sdk.types.market;

import network.akash.sdk.types.deployment.DeploymentID;
import network.akash.sdk.types.deployment.GroupID;
import java.util.Objects;

/**
 * Lease identifier (same structure as BidID).
 * Port of akash-api/go/node/market/v1beta4.LeaseID
 */
public record LeaseID(String owner, long dseq, int gseq, int oseq, String provider) {

    public LeaseID {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
    }

    public static LeaseID make(BidID bidId) {
        return new LeaseID(bidId.owner(), bidId.dseq(), bidId.gseq(), bidId.oseq(), bidId.provider());
    }

    public BidID bidID() {
        return new BidID(owner, dseq, gseq, oseq, provider);
    }

    public OrderID orderID() {
        return bidID().orderID();
    }

    public GroupID groupID() {
        return orderID().groupID();
    }

    public DeploymentID deploymentID() {
        return groupID().deploymentID();
    }

    public boolean equals(LeaseID other) {
        return bidID().equals(other.bidID());
    }

    public void validate() {
        bidID().validate();
    }

    @Override
    public String toString() {
        return bidID().toString();
    }
}
