package network.akash.sdk.cosmos;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;

class CoinTest {

    @Test
    void ofLong() {
        Coin c = Coin.of("uakt", 1000);
        assertEquals("uakt", c.denom());
        assertEquals(BigInteger.valueOf(1000), c.amount());
    }

    @Test
    void ofString() {
        Coin c = Coin.of("uakt", "999999999999999999");
        assertEquals(new BigInteger("999999999999999999"), c.amount());
    }

    @Test
    void isZero() {
        assertTrue(Coin.of("uakt", 0).isZero());
        assertFalse(Coin.of("uakt", 1).isZero());
    }

    @Test
    void isPositive() {
        assertTrue(Coin.of("uakt", 100).isPositive());
        assertFalse(Coin.of("uakt", 0).isPositive());
    }

    @Test
    void isNegative() {
        assertTrue(new Coin("uakt", BigInteger.valueOf(-1)).isNegative());
        assertFalse(Coin.of("uakt", 0).isNegative());
    }

    @Test
    void add() {
        Coin a = Coin.of("uakt", 100);
        Coin b = Coin.of("uakt", 200);
        Coin result = a.add(b);
        assertEquals(BigInteger.valueOf(300), result.amount());
        assertEquals("uakt", result.denom());
    }

    @Test
    void addDifferentDenomThrows() {
        Coin a = Coin.of("uakt", 100);
        Coin b = Coin.of("atom", 200);
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void sub() {
        Coin a = Coin.of("uakt", 300);
        Coin b = Coin.of("uakt", 100);
        assertEquals(BigInteger.valueOf(200), a.sub(b).amount());
    }

    @Test
    void subDifferentDenomThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Coin.of("uakt", 100).sub(Coin.of("atom", 50)));
    }

    @Test
    void validate() {
        assertDoesNotThrow(() -> Coin.of("uakt", 100).validate());
    }

    @Test
    void validateNegativeThrows() {
        Coin c = new Coin("uakt", BigInteger.valueOf(-1));
        assertThrows(IllegalArgumentException.class, c::validate);
    }

    @Test
    void validateEmptyDenomThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Coin("", BigInteger.ONE));
    }

    @Test
    void nullDenomThrows() {
        assertThrows(NullPointerException.class, () -> new Coin(null, BigInteger.ONE));
    }

    @Test
    void nullAmountThrows() {
        assertThrows(NullPointerException.class, () -> new Coin("uakt", null));
    }

    @Test
    void toStringFormat() {
        assertEquals("1000uakt", Coin.of("uakt", 1000).toString());
    }
}
