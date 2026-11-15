package network.akash.sdk.sdl;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ResourceUnits(String raw) {

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(Ki|Mi|Gi|Ti|K|M|G|T|k|m|)?");

    public long toBytes() {
        Matcher m = SIZE_PATTERN.matcher(raw);
        if (!m.matches()) {
            return new BigDecimal(raw).longValue();
        }
        BigDecimal value = new BigDecimal(m.group(1));
        String suffix = m.group(2);
        if (suffix == null || suffix.isEmpty()) return value.longValue();
        return switch (suffix) {
            case "k", "K" -> value.multiply(BigDecimal.valueOf(1000)).longValue();
            case "Ki" -> value.multiply(BigDecimal.valueOf(1024)).longValue();
            case "m", "M" -> value.multiply(BigDecimal.valueOf(1_000_000)).longValue();
            case "Mi" -> value.multiply(BigDecimal.valueOf(1024 * 1024)).longValue();
            case "G" -> value.multiply(BigDecimal.valueOf(1_000_000_000L)).longValue();
            case "Gi" -> value.multiply(BigDecimal.valueOf(1024L * 1024 * 1024)).longValue();
            case "T" -> value.multiply(BigDecimal.valueOf(1_000_000_000_000L)).longValue();
            case "Ti" -> value.multiply(BigDecimal.valueOf(1024L * 1024 * 1024 * 1024)).longValue();
            default -> value.longValue();
        };
    }

    public double toMilliCpu() {
        try {
            return new BigDecimal(raw).multiply(BigDecimal.valueOf(1000)).doubleValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
