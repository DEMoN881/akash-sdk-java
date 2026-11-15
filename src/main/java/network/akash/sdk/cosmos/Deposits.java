package network.akash.sdk.cosmos;

/**
 * Builds the deposit message Akash node 2.2 accepts.
 * The amount is taken from the signing account balance.
 */
public final class Deposits {

    private Deposits() {}

    public static akash.base.deposit.v1.Deposit fromBalance(Coin coin) {
        return akash.base.deposit.v1.Deposit.newBuilder()
                .setAmount(cosmos.base.v1beta1.CoinOuterClass.Coin.newBuilder()
                        .setDenom(coin.denom())
                        .setAmount(coin.amount().toString())
                        .build())
                .addSources(akash.base.deposit.v1.Source.balance)
                .build();
    }
}
