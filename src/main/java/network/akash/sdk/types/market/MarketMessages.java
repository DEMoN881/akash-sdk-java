package network.akash.sdk.types.market;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.DecCoin;
import network.akash.sdk.cosmos.Msg;
import java.util.List;

/**
 * Market transaction messages.
 * Port of akash-api/go/node/market/v1beta4 messages.
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
            return new byte[0];
        }
    }

    public record MsgCloseBid(BidID bidId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-bid"; }

        @Override
        public void validateBasic() {
            bidId.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(bidId.provider()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgCreateLease(BidID bidId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "create-lease"; }

        @Override
        public void validateBasic() {
            bidId.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(bidId.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgWithdrawLease(LeaseID leaseId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "withdraw-lease"; }

        @Override
        public void validateBasic() {
            leaseId.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(leaseId.provider()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgCloseLease(LeaseID leaseId) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-lease"; }

        @Override
        public void validateBasic() {
            leaseId.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(leaseId.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }
}
