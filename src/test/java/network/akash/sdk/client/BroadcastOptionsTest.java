package network.akash.sdk.client;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class BroadcastOptionsTest {

    @Test
    void defaultTimeout() {
        BroadcastOptions opts = new BroadcastOptions();
        assertEquals(Duration.ofSeconds(30), opts.getBroadcastTimeout());
    }

    @Test
    void withGas() {
        BroadcastOptions opts = BroadcastOptions.withGas(300000, true);
        assertEquals(300000L, opts.getGas());
        assertTrue(opts.isSimulateAndExecute());
    }

    @Test
    void withFees() {
        BroadcastOptions opts = BroadcastOptions.withFees("5000uakt");
        assertEquals("5000uakt", opts.getFees());
    }

    @Test
    void withGasPrices() {
        BroadcastOptions opts = BroadcastOptions.withGasPrices("0.025uakt");
        assertEquals("0.025uakt", opts.getGasPrices());
    }

    @Test
    void withNote() {
        BroadcastOptions opts = BroadcastOptions.withNote("test note");
        assertEquals("test note", opts.getNote());
    }

    @Test
    void withGasAdjustment() {
        BroadcastOptions opts = BroadcastOptions.withGasAdjustment(1.3);
        assertEquals(1.3, opts.getGasAdjustment());
    }

    @Test
    void withTimeoutHeight() {
        BroadcastOptions opts = BroadcastOptions.withTimeoutHeight(50000);
        assertEquals(50000L, opts.getTimeoutHeight());
    }

    @Test
    void withResultCodeAsError() {
        BroadcastOptions opts = BroadcastOptions.withResultCodeAsError();
        assertTrue(opts.isResultAsError());
    }

    @Test
    void mergeOverrides() {
        BroadcastOptions base = new BroadcastOptions();
        BroadcastOptions overlay = BroadcastOptions.withGas(500000, false);
        overlay = new BroadcastOptions().merge(BroadcastOptions.withFees("10000uakt"))
                .merge(BroadcastOptions.withGas(500000, false));
        assertEquals(500000L, overlay.getGas());
        assertEquals("10000uakt", overlay.getFees());
    }

    @Test
    void mergeDoesNotOverrideWithNull() {
        BroadcastOptions base = BroadcastOptions.withFees("5000uakt");
        BroadcastOptions empty = new BroadcastOptions();
        base.merge(empty);
        assertEquals("5000uakt", base.getFees());
    }

    @Test
    void withSkipConfirm() {
        BroadcastOptions opts = BroadcastOptions.withSkipConfirm(true);
        assertTrue(opts.getSkipConfirm());
    }
}
