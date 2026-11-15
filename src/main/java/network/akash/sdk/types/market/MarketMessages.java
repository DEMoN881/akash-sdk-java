package network.akash.sdk.types.market;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.DecCoin;
import network.akash.sdk.cosmos.Msg;
import java.util.List;

public final class MarketMessages {

    public static final String ROUTE = "market";

    private MarketMessages() {}

    public record MsgCreateBid(
            OrderID order,
            String provider,
            DecCoin price,
            Coin deposit
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "create-bid"; }

        @Override
        public void validateBasic() {
            order.validate();
            AccAddress provAddr = AccAddress.fromBech32(provider);
            AccAddress ownerAddr = AccAddress.fromBech32(order.owner());
            if (provAddr.equals(ownerAddr)) {
                throw new IllegalArgumentException("provider cannot bid on own order");
            }
            if (price.isZero()) {
                throw new IllegalArgumentException("bid price must not be zero");
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(provider));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.market.v1beta4.MsgCreateBid.newBuilder()
                    .setOrder(akash.market.v1beta4.OrderID.newBuilder()
                            .setOwner(order.owner()).setDseq(order.dseq())
                            .setGseq(order.gseq()).setOseq(order.oseq()).build())
                    .setProvider(provider)
                    .setPrice(cosmos.base.v1beta1.CoinOuterClass.DecCoin.newBuilder()
                            .setDenom(price.denom()).setAmount(price.amount().toPlainString()).build())
                    .setDeposit(cosmos.base.v1beta1.CoinOuterClass.Coin.newBuilder()
                            .setDenom(deposit.denom()).setAmount(deposit.amount().toString()).build())
                    .build().toByteArray();
        }
    }

    public record MsgCloseBid(BidID bidId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-bid"; }

        @Override public void validateBasic() { bidId.validate(); }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(bidId.provider()));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.market.v1beta4.MsgCloseBid.newBuilder()
                    .setBidId(akash.market.v1beta4.BidID.newBuilder()
                            .setOwner(bidId.owner()).setDseq(bidId.dseq())
                            .setGseq(bidId.gseq()).setOseq(bidId.oseq())
                            .setProvider(bidId.provider()).build())
                    .build().toByteArray();
        }
    }

    public record MsgCreateLease(BidID bidId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "create-lease"; }

        @Override public void validateBasic() { bidId.validate(); }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(bidId.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.market.v1beta4.MsgCreateLease.newBuilder()
                    .setBidId(akash.market.v1beta4.BidID.newBuilder()
                            .setOwner(bidId.owner()).setDseq(bidId.dseq())
                            .setGseq(bidId.gseq()).setOseq(bidId.oseq())
                            .setProvider(bidId.provider()).build())
                    .build().toByteArray();
        }
    }

    public record MsgWithdrawLease(LeaseID leaseId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "withdraw-lease"; }

        @Override public void validateBasic() { leaseId.validate(); }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(leaseId.provider()));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.market.v1beta4.MsgWithdrawLease.newBuilder()
                    .setLeaseId(akash.market.v1beta4.LeaseID.newBuilder()
                            .setOwner(leaseId.owner()).setDseq(leaseId.dseq())
                            .setGseq(leaseId.gseq()).setOseq(leaseId.oseq())
                            .setProvider(leaseId.provider()).build())
                    .build().toByteArray();
        }
    }

    public record MsgCloseLease(LeaseID leaseId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-lease"; }

        @Override public void validateBasic() { leaseId.validate(); }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(leaseId.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.market.v1beta4.MsgCloseLease.newBuilder()
                    .setLeaseId(akash.market.v1beta4.LeaseID.newBuilder()
                            .setOwner(leaseId.owner()).setDseq(leaseId.dseq())
                            .setGseq(leaseId.gseq()).setOseq(leaseId.oseq())
                            .setProvider(leaseId.provider()).build())
                    .build().toByteArray();
        }
    }
}
