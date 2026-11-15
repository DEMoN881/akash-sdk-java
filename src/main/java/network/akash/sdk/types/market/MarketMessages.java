package network.akash.sdk.types.market;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.DecCoin;
import network.akash.sdk.cosmos.Deposits;
import network.akash.sdk.cosmos.Msg;
import java.util.List;

/**
 * Market transaction messages.
 * Port of chain-sdk go/node/market/v1beta5, registered by Akash node 2.2.
 */
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
            return akash.market.v1beta5.MsgCreateBid.newBuilder()
                    .setId(akash.market.v1beta5.BidID.newBuilder()
                            .setOwner(order.owner()).setDseq(order.dseq())
                            .setGseq(order.gseq()).setOseq(order.oseq())
                            .setProvider(provider).build())
                    .setPrice(cosmos.base.v1beta1.CoinOuterClass.DecCoin.newBuilder()
                            .setDenom(price.denom()).setAmount(price.protoAmount()).build())
                    .setDeposit(Deposits.fromBalance(deposit))
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
            return akash.market.v1beta5.MsgCloseBid.newBuilder()
                    .setId(bid(bidId))
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
            return akash.market.v1beta5.MsgCreateLease.newBuilder()
                    .setBidId(bid(bidId))
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
            return akash.market.v1beta5.MsgWithdrawLease.newBuilder()
                    .setId(lease(leaseId))
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
            return akash.market.v1beta5.MsgCloseLease.newBuilder()
                    .setId(lease(leaseId))
                    .build().toByteArray();
        }
    }

    private static akash.market.v1beta5.BidID bid(BidID id) {
        var builder = akash.market.v1beta5.BidID.newBuilder()
                .setOwner(id.owner()).setDseq(id.dseq())
                .setGseq(id.gseq()).setOseq(id.oseq())
                .setProvider(id.provider());
        if (id.bseq() != 0) {
            builder.setBseq(id.bseq());
        }
        return builder.build();
    }

    private static akash.market.v1beta5.LeaseID lease(LeaseID id) {
        return akash.market.v1beta5.LeaseID.newBuilder()
                .setOwner(id.owner()).setDseq(id.dseq())
                .setGseq(id.gseq()).setOseq(id.oseq())
                .setProvider(id.provider())
                .build();
    }
}
