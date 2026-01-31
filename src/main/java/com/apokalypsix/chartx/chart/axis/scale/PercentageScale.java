package com.apokalypsix.chartx.chart.axis.scale;

import java.text.DecimalFormat;

/**
 * Percentage axis scale for displaying relative changes from a reference value.
 *
 * <p>This scale displays values as percentage changes from a reference point
 * (typically the first price or a specific baseline). The scale transformation
 * is linear, but labels show percentage changes instead of absolute values.
 *
 * <p>This is useful for:
 * <ul>
 *   <li>Comparing performance of multiple securities on the same chart</li>
 *   <li>Showing return on investment from a specific date</li>
 *   <li>Displaying normalized data for comparison</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * // Set percentage scale based on first close price
 * double firstPrice = data.getClose(0);
 * chart.getYAxis().setScale(new PercentageScale(firstPrice));
 *
 * // Labels will show: -5.25%, 0.00%, +10.50%, etc.
 * }</pre>
 */
public final class PercentageScale implements AxisScale {

    private final double referenceValue;
    private final boolean showPlusSign;

    /**
     * Creates a percentage scale with the given reference value.
     *
     * <p>The reference value represents 0% on the scale.
     *
     * @param referenceValue the value that represents 0% (e.g., opening price)
     * @throws IllegalArgumentException if referenceValue is zero
     */
    public PercentageScale(double referenceValue) {
        this(referenceValue, true);
    }

    /**
     * Creates a percentage scale with the given reference value and sign option.
     *
     * @param referenceValue the value that represents 0% (e.g., opening price)
     * @param showPlusSign   true to show + sign for positive values
     * @throws IllegalArgumentException if referenceValue is zero
     */
    public PercentageScale(double referenceValue, boolean showPlusSign) {
        if (referenceValue == 0) {
            throw new IllegalArgumentException("Reference value cannot be zero for percentage scale");
        }
        this.referenceValue = referenceValue;
        this.showPlusSign = showPlusSign;
    }

    /**
     * Returns the reference value (the 0% baseline).
     */
    public double getReferenceValue() {
        return referenceValue;
    }

    /**
     * Returns whether positive values show a + sign.
     */
    public boolean isShowPlusSign() {
        return showPlusSign;
    }

    @Override
    public double normalize(double value, double min, double max) {
        // Linear transformation - same as LinearScale
        double span = max - min;
        if (span == 0) {
            return 0.5;
        }
        return (value - min) / span;
    }

    @Override
    public double interpolate(double normalized, double min, double max) {
        // Linear transformation - same as LinearScale
        return min + normalized * (max - min);
    }

    @Override
    public double[] calculateGridLevels(double min, double max, int targetCount) {
        // Calculate percentage range
        double minPct = ((min - referenceValue) / referenceValue) * 100;
        double maxPct = ((max - referenceValue) / referenceValue) * 100;
        double pctRange = maxPct - minPct;

        if (pctRange <= 0 || targetCount <= 0) {
            return new double[0];
        }

        // Calculate nice percentage interval
        double roughInterval = pctRange / targetCount;
        double interval = calculateNicePercentageInterval(roughInterval);

        // Find first grid line at a nice percentage value
        double firstPct = Math.ceil(minPct / interval) * interval;

        // Count levels
        int count = 0;
        for (double pct = firstPct; pct <= maxPct; pct += interval) {
            count++;
            if (count > 100) break; // Safety cap
        }

        // Convert percentage levels back to absolute values
        double[] levels = new double[count];
        int i = 0;
        for (double pct = firstPct; pct <= maxPct && i < count; pct += interval) {
            levels[i++] = referenceValue * (1 + pct / 100);
        }

        return levels;
    }

    /**
     * Calculates nice percentage intervals (1%, 2%, 5%, 10%, etc.)
     */
    private double calculateNicePercentageInterval(double rough) {
        double absRough = Math.abs(rough);
        if (absRough <= 0) {
            return 1.0;
        }

        double magnitude = Math.pow(10, Math.floor(Math.log10(absRough)));
        double normalized = absRough / magnitude;

        double niceNormalized;
        if (normalized <= 1.0) {
            niceNormalized = 1.0;
        } else if (normalized <= 2.0) {
            niceNormalized = 2.0;
        } else if (normalized <= 5.0) {
            niceNormalized = 5.0;
        } else {
            niceNormalized = 10.0;
        }

        return niceNormalized * magnitude;
    }

    @Override
    public String formatValue(double value, DecimalFormat defaultFormat) {
        double pctChange = ((value - referenceValue) / referenceValue) * 100;

        // Format with appropriate precision
        String formatted;
        if (Math.abs(pctChange) >= 100) {
            formatted = String.format("%.1f%%", pctChange);
        } else if (Math.abs(pctChange) >= 10) {
            formatted = String.format("%.2f%%", pctChange);
        } else {
            formatted = String.format("%.2f%%", pctChange);
        }

        // Add plus sign for positive values
        if (showPlusSign && pctChange > 0 && !formatted.startsWith("+")) {
            formatted = "+" + formatted;
        }

        return formatted;
    }

    @Override
    public boolean isValidRange(double min, double max) {
        return max >= min && Double.isFinite(min) && Double.isFinite(max);
    }

    @Override
    public String getDefaultFormatPattern() {
        return "+0.00%;-0.00%";  // Pattern for DecimalFormat (not actually used)
    }

    @Override
    public String toString() {
        return String.format("PercentageScale(ref=%.4f)", referenceValue);
    }
}
