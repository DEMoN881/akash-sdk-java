package network.akash.sdk.cosmos;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Cosmos SDK Coin type.
 * Port of cosmos-sdk/types.Coin
 */
public record Coin(String denom, BigInteger amount) {

    public Coin {
        Objects.requireNonNull(denom, "denom must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (denom.isEmpty()) throw new IllegalArgumentException("empty denom");
    }

    public static Coin of(String denom, long amount) {
        return new Coin(denom, BigInteger.valueOf(amount));
    }

    public static Coin of(String denom, String amount) {
        return new Coin(denom, new BigInteger(amount));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Coin add(Coin other) {
        if (!denom.equals(other.denom)) {
            throw new IllegalArgumentException("cannot add coins of different denoms: " + denom + " vs " + other.denom);
        }
        return new Coin(denom, amount.add(other.amount));
    }

    public Coin sub(Coin other) {
        if (!denom.equals(other.denom)) {
            throw new IllegalArgumentException("cannot sub coins of different denoms: " + denom + " vs " + other.denom);
        }
        return new Coin(denom, amount.subtract(other.amount));
    }

    public void validate() {
        if (denom.isEmpty()) throw new IllegalArgumentException("invalid denom");
        if (isNegative()) throw new IllegalArgumentException("negative coin amount");
    }

    @Override
    public String toString() {
        return amount + denom;
    }
}
