package com.apokalypsix.chartx.chart.axis.scale;

import java.text.DecimalFormat;

/**
 * Linear axis scale (default).
 *
 * <p>Provides uniform distribution of values where equal pixel distances
 * represent equal value differences. This is the default scale type for
 * most charts and maintains backward compatibility with existing behavior.
 *
 * <p>Grid levels are calculated using "nice" intervals that are multiples
 * of 1, 2, or 5 times a power of 10 (e.g., 0.1, 0.2, 0.5, 1, 2, 5, 10...).
 *
 * <p>This class is a stateless singleton - use {@link #INSTANCE}.
 */
public final class LinearScale implements AxisScale {

    /** Singleton instance for linear scale. */
    public static final LinearScale INSTANCE = new LinearScale();

    private LinearScale() {
        // Singleton
    }

    @Override
    public double normalize(double value, double min, double max) {
        double span = max - min;
        if (span == 0) {
            return 0.5;
        }
        return (value - min) / span;
    }

    @Override
    public double interpolate(double normalized, double min, double max) {
        return min + normalized * (max - min);
    }

    @Override
    public double[] calculateGridLevels(double min, double max, int targetCount) {
        double range = max - min;
        if (range <= 0 || targetCount <= 0) {
            return new double[0];
        }

        // Calculate nice grid interval
        double interval = calculateNiceInterval(range, targetCount);

        // Find first grid line at or above min
        double firstLevel = Math.ceil(min / interval) * interval;

        // Count levels
        int count = 0;
        for (double p = firstLevel; p <= max; p += interval) {
            count++;
            if (count > 100) break; // Safety cap
        }

        double[] levels = new double[count];
        int i = 0;
        for (double p = firstLevel; p <= max && i < count; p += interval) {
            levels[i++] = p;
        }

        return levels;
    }

    /**
     * Calculates a "nice" interval for grid lines.
     *
     * <p>Nice intervals are multiples of 1, 2, or 5 times a power of 10.
     * This produces visually pleasing grid lines at values like
     * 0, 10, 20, 30... or 0, 0.5, 1.0, 1.5...
     *
     * @param range       the value range (max - min)
     * @param targetCount desired number of grid lines
     * @return nice interval value
     */
    private double calculateNiceInterval(double range, int targetCount) {
        double roughInterval = range / targetCount;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughInterval)));

        double normalized = roughInterval / magnitude;

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
        return defaultFormat.format(value);
    }

    @Override
    public boolean isValidRange(double min, double max) {
        return max >= min && Double.isFinite(min) && Double.isFinite(max);
    }

    @Override
    public String getDefaultFormatPattern() {
        return "#,##0.00";
    }

    @Override
    public String toString() {
        return "LinearScale";
    }
}
