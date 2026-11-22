package network.akash.sdk.types.bank;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.Msg;

import java.util.List;

/**
 * Cosmos SDK bank transaction messages.
 * Supports MsgSend for token transfers between accounts.
 */
public final class BankMessages {

    public static final String ROUTE = "bank";

    private BankMessages() {}

    public record MsgSend(
            String fromAddress,
            String toAddress,
            List<Coin> amount
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "send"; }

        @Override
        public void validateBasic() {
            if (fromAddress == null || fromAddress.isEmpty()) {
                throw new IllegalArgumentException("invalid sender address: empty");
            }
            if (toAddress == null || toAddress.isEmpty()) {
                throw new IllegalArgumentException("invalid recipient address: empty");
            }
            if (amount == null || amount.isEmpty()) {
                throw new IllegalArgumentException("invalid coins: empty");
            }
            for (Coin coin : amount) {
                coin.validate();
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(fromAddress));
        }

        @Override
        public byte[] getSignBytes() {
            cosmos.bank.v1beta1.MsgSend.Builder builder = cosmos.bank.v1beta1.MsgSend.newBuilder()
                    .setFromAddress(fromAddress)
                    .setToAddress(toAddress);
            for (Coin coin : amount) {
                builder.addAmount(cosmos.base.v1beta1.CoinOuterClass.Coin.newBuilder()
                        .setDenom(coin.denom())
                        .setAmount(String.valueOf(coin.amount()))
                        .build());
            }
            return builder.build().toByteArray();
        }
    }
}
