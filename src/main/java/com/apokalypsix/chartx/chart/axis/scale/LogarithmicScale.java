package com.apokalypsix.chartx.chart.axis.scale;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Logarithmic axis scale for displaying data with exponential ranges.
 *
 * <p>Logarithmic scales are useful for:
 * <ul>
 *   <li>Financial data with large percentage moves (crypto, long-term stock charts)</li>
 *   <li>Scientific data spanning multiple orders of magnitude</li>
 *   <li>Data where percentage changes are more meaningful than absolute changes</li>
 * </ul>
 *
 * <p>In a logarithmic scale, equal distances on the axis represent equal
 * percentage changes (ratios), not equal absolute differences.
 *
 * <p><strong>Important:</strong> Logarithmic scales require strictly positive values.
 * Values <= 0 cannot be represented. The axis range validation will reject
 * ranges that include zero or negative values.
 *
 * <p>Usage:
 * <pre>{@code
 * // Use base-10 logarithmic scale (most common)
 * chart.getYAxis().setScale(LogarithmicScale.BASE_10);
 *
 * // Use base-2 scale (useful for computing/doubling data)
 * chart.getYAxis().setScale(LogarithmicScale.BASE_2);
 *
 * // Custom base with scientific notation
 * chart.getYAxis().setScale(new LogarithmicScale(10, true));
 * }</pre>
 */
public final class LogarithmicScale implements AxisScale {

    /** Common logarithmic scale (base 10). */
    public static final LogarithmicScale BASE_10 = new LogarithmicScale(10);

    /** Binary logarithmic scale (base 2). */
    public static final LogarithmicScale BASE_2 = new LogarithmicScale(2);

    /** Natural logarithmic scale (base e). */
    public static final LogarithmicScale BASE_E = new LogarithmicScale(Math.E);

    private final double base;
    private final double logBase;  // Cached log(base) for efficiency
    private final boolean useScientificNotation;

    /**
     * Creates a logarithmic scale with the given base.
     *
     * @param base the logarithm base (must be > 1)
     */
    public LogarithmicScale(double base) {
        this(base, false);
    }

    /**
     * Creates a logarithmic scale with the given base and notation option.
     *
     * @param base                   the logarithm base (must be > 1)
     * @param useScientificNotation true to format large values in scientific notation
     */
    public LogarithmicScale(double base, boolean useScientificNotation) {
        if (base <= 1) {
            throw new IllegalArgumentException("Logarithm base must be > 1, got: " + base);
        }
        this.base = base;
        this.logBase = Math.log(base);
        this.useScientificNotation = useScientificNotation;
    }

    /**
     * Returns the logarithm base.
     */
    public double getBase() {
        return base;
    }

    /**
     * Returns whether scientific notation is used for formatting.
     */
    public boolean isUseScientificNotation() {
        return useScientificNotation;
    }

    @Override
    public double normalize(double value, double min, double max) {
        // Handle edge cases
        if (value <= 0 || min <= 0 || max <= 0 || max <= min) {
            return 0.5;
        }

        double logMin = Math.log(min) / logBase;
        double logMax = Math.log(max) / logBase;
        double logValue = Math.log(value) / logBase;

        double logSpan = logMax - logMin;
        if (logSpan == 0) {
            return 0.5;
        }

        return (logValue - logMin) / logSpan;
    }

    @Override
    public double interpolate(double normalized, double min, double max) {
        // Handle edge cases
        if (min <= 0 || max <= 0 || max <= min) {
            return min;
        }

        double logMin = Math.log(min) / logBase;
        double logMax = Math.log(max) / logBase;

        double logValue = logMin + normalized * (logMax - logMin);
        return Math.pow(base, logValue);
    }

    @Override
    public double[] calculateGridLevels(double min, double max, int targetCount) {
        // Handle edge cases
        if (min <= 0 || max <= 0 || max <= min || targetCount <= 0) {
            return new double[0];
        }

        List<Double> levels = new ArrayList<>();

        // Find the range of exponents
        double logMin = Math.log(min) / logBase;
        double logMax = Math.log(max) / logBase;

        int expMin = (int) Math.floor(logMin);
        int expMax = (int) Math.ceil(logMax);

        // Determine subdivision based on range and target count
        int expRange = expMax - expMin;
        boolean includeSubdivisions = (expRange <= targetCount / 2);

        // Subdivisions at 2x and 5x for base-10, or 1.5x for base-2
        double[] subdivisions;
        if (base == 10) {
            subdivisions = includeSubdivisions ? new double[]{1, 2, 5} : new double[]{1};
        } else if (base == 2) {
            subdivisions = includeSubdivisions ? new double[]{1, 1.5} : new double[]{1};
        } else {
            subdivisions = new double[]{1};
        }

        // Generate grid levels at powers and optional subdivisions
        for (int exp = expMin; exp <= expMax; exp++) {
            double powerValue = Math.pow(base, exp);

            for (double mult : subdivisions) {
                double level = powerValue * mult;
                if (level >= min && level <= max) {
                    levels.add(level);
                }
            }
        }

        // Safety check - limit number of levels
        if (levels.size() > 100) {
            // Reduce by skipping subdivisions or every other power
            List<Double> reduced = new ArrayList<>();
            int step = Math.max(1, levels.size() / targetCount);
            for (int i = 0; i < levels.size(); i += step) {
                reduced.add(levels.get(i));
            }
            levels = reduced;
        }

        // Convert to primitive array
        double[] result = new double[levels.size()];
        for (int i = 0; i < levels.size(); i++) {
            result[i] = levels.get(i);
        }
        return result;
    }

    @Override
    public String formatValue(double value, DecimalFormat defaultFormat) {
        if (useScientificNotation && (value >= 1e6 || value <= 1e-4)) {
            // Use scientific notation for very large or small values
            return String.format("%.2e", value);
        }

        // For values that are clean powers of the base, show without decimals
        if (base == 10 && isPowerOfTen(value)) {
            if (value >= 1) {
                return String.format("%.0f", value);
            }
        }

        return defaultFormat.format(value);
    }

    /**
     * Checks if value is approximately a power of 10.
     */
    private boolean isPowerOfTen(double value) {
        if (value <= 0) return false;
        double log10 = Math.log10(value);
        return Math.abs(log10 - Math.round(log10)) < 1e-9;
    }

    @Override
    public boolean isValidRange(double min, double max) {
        // Logarithmic scale requires strictly positive values
        return min > 0 && max > min && Double.isFinite(min) && Double.isFinite(max);
    }

    @Override
    public String getDefaultFormatPattern() {
        return "#,##0.##";  // Fewer decimal places for log scale
    }

    @Override
    public String toString() {
        if (base == 10) {
            return "LogarithmicScale(base=10)";
        } else if (base == 2) {
            return "LogarithmicScale(base=2)";
        } else if (Math.abs(base - Math.E) < 1e-9) {
            return "LogarithmicScale(base=e)";
        } else {
            return String.format("LogarithmicScale(base=%.2f)", base);
        }
    }
}
