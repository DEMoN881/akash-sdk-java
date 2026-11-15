package network.akash.sdk.cosmos;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cosmos SDK DecCoin type (decimal coin).
 * Port of cosmos-sdk/types.DecCoin
 */
public record DecCoin(String denom, BigDecimal amount) {

    public DecCoin {
        Objects.requireNonNull(denom, "denom must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }

    public static DecCoin of(String denom, String amount) {
        return new DecCoin(denom, new BigDecimal(amount));
    }

    public static DecCoin of(String denom, double amount) {
        return new DecCoin(denom, BigDecimal.valueOf(amount));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + denom;
    }
}
