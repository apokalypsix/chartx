package com.apokalypsix.chartx.chart.axis.scale;

import java.text.DecimalFormat;

/**
 * Strategy interface for axis scaling transformations.
 *
 * <p>This interface enables different scale types (linear, logarithmic, percentage, etc.)
 * to be used interchangeably with Y-axes. Each scale type defines how values are:
 * <ul>
 *   <li>Normalized to the 0-1 range for coordinate transformations</li>
 *   <li>Interpolated back from normalized to actual values</li>
 *   <li>Distributed for grid lines and labels</li>
 *   <li>Formatted for display</li>
 * </ul>
 *
 * <p>Scale implementations should be stateless when possible (use singletons)
 * to avoid object allocation. State that varies (like reference values for
 * percentage scales) should be stored in the implementation.
 *
 * <p>Usage:
 * <pre>{@code
 * // Set logarithmic scale on Y-axis
 * chart.getYAxis().setScale(LogarithmicScale.BASE_10);
 *
 * // Set percentage scale with reference value
 * chart.getYAxis().setScale(new PercentageScale(firstPrice));
 * }</pre>
 */
public interface AxisScale {

    /**
     * Normalizes a value to the 0-1 range based on the axis min/max.
     *
     * <p>For linear scales: {@code (value - min) / (max - min)}
     * <p>For log scales: {@code (log(value) - log(min)) / (log(max) - log(min))}
     *
     * @param value the value to normalize
     * @param min   the axis minimum value
     * @param max   the axis maximum value
     * @return normalized value where 0 = min and 1 = max
     */
    double normalize(double value, double min, double max);

    /**
     * Interpolates from a normalized value (0-1) to an actual value.
     *
     * <p>This is the inverse of {@link #normalize(double, double, double)}.
     *
     * @param normalized normalized position (0-1)
     * @param min        the axis minimum value
     * @param max        the axis maximum value
     * @return actual value
     */
    double interpolate(double normalized, double min, double max);

    /**
     * Calculates grid/label levels for the given range.
     *
     * <p>The returned levels should be "nice" values appropriate for the scale type:
     * <ul>
     *   <li>Linear: multiples of 1, 2, 5 × 10^n</li>
     *   <li>Logarithmic: powers of the base (1, 10, 100, 1000...)</li>
     *   <li>Percentage: clean percentage intervals (-10%, 0%, +10%...)</li>
     * </ul>
     *
     * @param min         the axis minimum value
     * @param max         the axis maximum value
     * @param targetCount desired number of grid lines (hint, not exact)
     * @return array of values where grid lines/labels should appear
     */
    double[] calculateGridLevels(double min, double max, int targetCount);

    /**
     * Formats a value for display on axis labels.
     *
     * <p>Most scales delegate to the provided DecimalFormat, but some scales
     * (like PercentageScale) may override to show custom formatting like "+10.5%".
     *
     * @param value         the value to format
     * @param defaultFormat the axis's default DecimalFormat
     * @return formatted string for display
     */
    String formatValue(double value, DecimalFormat defaultFormat);

    /**
     * Validates that the given range is valid for this scale type.
     *
     * <p>For example, logarithmic scales require positive values.
     *
     * @param min the proposed minimum value
     * @param max the proposed maximum value
     * @return true if the range is valid for this scale
     */
    boolean isValidRange(double min, double max);

    /**
     * Returns the default number format pattern for this scale type.
     *
     * <p>Used when an axis doesn't have a custom format set.
     *
     * @return DecimalFormat pattern string
     */
    String getDefaultFormatPattern();
}
