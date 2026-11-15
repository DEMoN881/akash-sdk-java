package network.akash.sdk.client;

import java.time.Duration;

/**
 * Broadcast options for configuring transaction broadcasts.
 * Port of akash-api/go/node/client/v1beta2.BroadcastOptions
 */
public final class BroadcastOptions {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration BLOCK_RETRY_TIMEOUT = Duration.ofSeconds(300);
    public static final Duration BLOCK_RETRY_PERIOD = Duration.ofSeconds(1);

    private Long timeoutHeight;
    private Double gasAdjustment;
    private Long gas;
    private boolean simulateAndExecute;
    private String gasPrices;
    private String fees;
    private String note;
    private Duration broadcastTimeout;
    private boolean resultAsError;
    private Boolean skipConfirm;

    public BroadcastOptions() {
        this.broadcastTimeout = DEFAULT_TIMEOUT;
    }

    public static BroadcastOptions withGasAdjustment(double val) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.gasAdjustment = val;
        return opts;
    }

    public static BroadcastOptions withNote(String val) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.note = val;
        return opts;
    }

    public static BroadcastOptions withGas(long gas, boolean simulate) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.gas = gas;
        opts.simulateAndExecute = simulate;
        return opts;
    }

    public static BroadcastOptions withGasPrices(String val) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.gasPrices = val;
        return opts;
    }

    public static BroadcastOptions withFees(String val) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.fees = val;
        return opts;
    }

    public static BroadcastOptions withTimeoutHeight(long val) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.timeoutHeight = val;
        return opts;
    }

    public static BroadcastOptions withResultCodeAsError() {
        BroadcastOptions opts = new BroadcastOptions();
        opts.resultAsError = true;
        return opts;
    }

    public static BroadcastOptions withSkipConfirm(boolean val) {
        BroadcastOptions opts = new BroadcastOptions();
        opts.skipConfirm = val;
        return opts;
    }

    // Merge another options set into this one (non-null fields override)
    public BroadcastOptions merge(BroadcastOptions other) {
        if (other.timeoutHeight != null) this.timeoutHeight = other.timeoutHeight;
        if (other.gasAdjustment != null) this.gasAdjustment = other.gasAdjustment;
        if (other.gas != null) this.gas = other.gas;
        if (other.gasPrices != null) this.gasPrices = other.gasPrices;
        if (other.fees != null) this.fees = other.fees;
        if (other.note != null) this.note = other.note;
        if (other.broadcastTimeout != null) this.broadcastTimeout = other.broadcastTimeout;
        if (other.resultAsError) this.resultAsError = true;
        if (other.skipConfirm != null) this.skipConfirm = other.skipConfirm;
        return this;
    }

    public Long getTimeoutHeight() { return timeoutHeight; }
    public Double getGasAdjustment() { return gasAdjustment; }
    public Long getGas() { return gas; }
    public boolean isSimulateAndExecute() { return simulateAndExecute; }
    public String getGasPrices() { return gasPrices; }
    public String getFees() { return fees; }
    public String getNote() { return note; }
    public Duration getBroadcastTimeout() { return broadcastTimeout; }
    public boolean isResultAsError() { return resultAsError; }
    public Boolean getSkipConfirm() { return skipConfirm; }
}
