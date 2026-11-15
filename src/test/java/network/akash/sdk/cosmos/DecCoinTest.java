package network.akash.sdk.cosmos;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class DecCoinTest {

    @Test
    void ofString() {
        DecCoin c = DecCoin.of("uakt", "1.5");
        assertEquals("uakt", c.denom());
        assertEquals(new BigDecimal("1.5"), c.amount());
    }

    @Test
    void ofDouble() {
        DecCoin c = DecCoin.of("uakt", 2.5);
        assertEquals("uakt", c.denom());
    }

    @Test
    void isZero() {
        assertTrue(DecCoin.of("uakt", "0").isZero());
        assertFalse(DecCoin.of("uakt", "0.001").isZero());
    }

    @Test
    void protoAmountIsScaledInteger() {
        assertEquals("1500000000000000000", DecCoin.of("uakt", "1.5").protoAmount());
        assertEquals("50000000000000000000000", DecCoin.of("uakt", "50000").protoAmount());
    }

    @Test
    void toStringFormat() {
        DecCoin c = DecCoin.of("uakt", "1.5");
        assertEquals("1.5uakt", c.toString());
    }

    @Test
    void nullDenomThrows() {
        assertThrows(NullPointerException.class, () -> new DecCoin(null, BigDecimal.ONE));
    }

    @Test
    void nullAmountThrows() {
        assertThrows(NullPointerException.class, () -> new DecCoin("uakt", null));
    }
}
