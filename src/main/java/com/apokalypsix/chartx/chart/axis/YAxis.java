package com.apokalypsix.chartx.chart.axis;

import com.apokalypsix.chartx.chart.axis.scale.AxisScale;
import com.apokalypsix.chartx.chart.axis.scale.LinearScale;

import java.awt.Color;
import java.text.DecimalFormat;

/**
 * Represents a Y-axis configuration for multi-axis support.
 *
 * <p>Each Y-axis has its own price range, position (left or right), visibility settings,
 * and formatting options. Multiple series can share the same axis, and each pane can
 * have multiple axes.
 */
public class YAxis {

    /** Default axis ID for backward compatibility. */
    public static final String DEFAULT_AXIS_ID = "default";

    /** Position of the Y-axis on the chart. */
    public enum Position {
        LEFT,
        RIGHT
    }

    /** Auto-range behavior modes. */
    public enum AutoRangeMode {
        /** Never auto-range - manual control only */
        NEVER,
        /** Auto-range once when data is set, then manual */
        ONCE,
        /** Always auto-range to fit visible data */
        ALWAYS
    }

    /** Vertical anchor for positioning the axis area within the chart. */
    public enum VerticalAnchor {
        FULL,    // Use full chart height (default, backward compatible)
        TOP,     // Anchor to top of chart area
        BOTTOM   // Anchor to bottom of chart area
    }

    private final String id;
    private Position position;
    private double minValue;
    private double maxValue;
    private AutoRangeMode autoRangeMode;
    private boolean autoRangeApplied;  // For ONCE mode tracking
    private boolean visible;
    private int width;
    private Color labelColor;
    private DecimalFormat valueFormat;
    private double growBy;  // Padding fraction for auto-range (e.g., 0.05 = 5%)
    private double heightRatio = 1.0;        // Portion of chart height (0.0 to 1.0)
    private VerticalAnchor anchor = VerticalAnchor.FULL;  // Where to anchor the axis area
    private AxisScale scale = LinearScale.INSTANCE;  // Scale transformation strategy

    /**
     * Creates a Y-axis with the given ID and position.
     *
     * @param id unique identifier for this axis
     * @param position where to display the axis (LEFT or RIGHT)
     */
    public YAxis(String id, Position position) {
        this.id = id;
        this.position = position;
        this.minValue = 0;
        this.maxValue = 1;
        this.autoRangeMode = AutoRangeMode.ALWAYS;  // Default to always auto-range
        this.autoRangeApplied = false;
        this.visible = true;
        this.width = 60;
        this.labelColor = new Color(180, 180, 180);
        this.valueFormat = new DecimalFormat("#,##0.00");
        this.growBy = 0.05; // 5% padding
    }

    /**
     * Creates a copy of this Y-axis with a new ID.
     */
    public YAxis copy(String newId) {
        YAxis copy = new YAxis(newId, this.position);
        copy.minValue = this.minValue;
        copy.maxValue = this.maxValue;
        copy.autoRangeMode = this.autoRangeMode;
        copy.autoRangeApplied = this.autoRangeApplied;
        copy.visible = this.visible;
        copy.width = this.width;
        copy.labelColor = this.labelColor;
        copy.valueFormat = this.valueFormat;
        copy.growBy = this.growBy;
        copy.heightRatio = this.heightRatio;
        copy.anchor = this.anchor;
        copy.scale = this.scale;
        return copy;
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getValueSpan() {
        return maxValue - minValue;
    }

    /**
     * Returns the visible range as a PriceRange object.
     *
     * @return the visible price range
     */
    public PriceRange getVisibleRange() {
        return new PriceRange(minValue, maxValue);
    }

    /**
     * Returns the auto-range mode.
     *
     * @return the auto-range behavior mode
     */
    public AutoRangeMode getAutoRange() {
        return autoRangeMode;
    }

    /**
     * Returns true if auto-ranging is enabled (ONCE or ALWAYS mode).
     * This method is for backward compatibility with code using isAutoScale().
     *
     * @return true if auto-ranging is enabled
     */
    public boolean isAutoScale() {
        return autoRangeMode != AutoRangeMode.NEVER;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getWidth() {
        return width;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public DecimalFormat getValueFormat() {
        return valueFormat;
    }

    /**
     * Returns the padding fraction added during auto-ranging.
     *
     * @return padding as a fraction (e.g., 0.05 = 5%)
     */
    public double getGrowBy() {
        return growBy;
    }

    /**
     * Returns the padding fraction (alias for getGrowBy for backward compatibility).
     *
     * @return padding as a fraction
     * @deprecated Use {@link #getGrowBy()} instead
     */
    @Deprecated
    public double getAutoScalePadding() {
        return growBy;
    }

    public double getHeightRatio() {
        return heightRatio;
    }

    public VerticalAnchor getAnchor() {
        return anchor;
    }

    /**
     * Returns the scale transformation strategy for this axis.
     *
     * @return the axis scale (never null)
     */
    public AxisScale getScale() {
        return scale;
    }

    // ========== Setters ==========

    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Sets the visible range for this axis.
     *
     * @param minValue minimum value
     * @param maxValue maximum value
     * @throws IllegalArgumentException if maxValue &lt; minValue or range is invalid for scale
     */
    public void setVisibleRange(double minValue, double maxValue) {
        if (maxValue < minValue) {
            throw new IllegalArgumentException("Max value must be >= min value");
        }
        if (!scale.isValidRange(minValue, maxValue)) {
            throw new IllegalArgumentException(
                    "Invalid range for " + scale + ": min=" + minValue + ", max=" + maxValue);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Sets the visible range from a PriceRange object.
     *
     * @param range the price range
     */
    public void setVisibleRange(PriceRange range) {
        setVisibleRange(range.getMin(), range.getMax());
    }

    /**
     * Sets the value range for this axis (alias for setVisibleRange).
     *
     * @param minValue minimum value
     * @param maxValue maximum value
     * @throws IllegalArgumentException if maxValue &lt; minValue
     * @deprecated Use {@link #setVisibleRange(double, double)} instead
     */
    @Deprecated
    public void setValueRange(double minValue, double maxValue) {
        setVisibleRange(minValue, maxValue);
    }

    /**
     * Sets the auto-range mode.
     *
     * @param mode the auto-range behavior (NEVER, ONCE, or ALWAYS)
     */
    public void setAutoRange(AutoRangeMode mode) {
        this.autoRangeMode = mode;
        if (mode == AutoRangeMode.ONCE) {
            this.autoRangeApplied = false;  // Reset for new ONCE cycle
        }
    }

    /**
     * Enables or disables automatic Y-axis scaling.
     * This method is for backward compatibility.
     *
     * @param autoScale true to enable (ALWAYS mode), false to disable (NEVER mode)
     * @deprecated Use {@link #setAutoRange(AutoRangeMode)} instead
     */
    @Deprecated
    public void setAutoScale(boolean autoScale) {
        this.autoRangeMode = autoScale ? AutoRangeMode.ALWAYS : AutoRangeMode.NEVER;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setWidth(int width) {
        this.width = Math.max(0, width);
    }

    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    public void setValueFormat(DecimalFormat valueFormat) {
        this.valueFormat = valueFormat;
    }

    /**
     * Sets the padding fraction added during auto-ranging.
     *
     * @param fraction padding as a fraction (e.g., 0.05 = 5% padding on each end)
     */
    public void setGrowBy(double fraction) {
        this.growBy = Math.max(0, fraction);
    }

    /**
     * Sets the auto-scale padding (alias for setGrowBy for backward compatibility).
     *
     * @param padding padding as a fraction
     * @deprecated Use {@link #setGrowBy(double)} instead
     */
    @Deprecated
    public void setAutoScalePadding(double padding) {
        setGrowBy(padding);
    }

    /**
     * Sets the height ratio for this axis (portion of chart height to use).
     *
     * @param ratio value between 0.0 and 1.0
     */
    public void setHeightRatio(double ratio) {
        this.heightRatio = Math.max(0.0, Math.min(1.0, ratio));
    }

    /**
     * Sets the vertical anchor for this axis area.
     *
     * @param anchor where to anchor the axis area (TOP, BOTTOM, or FULL)
     */
    public void setAnchor(VerticalAnchor anchor) {
        this.anchor = anchor;
    }

    /**
     * Sets the scale transformation strategy for this axis.
     *
     * <p>The scale determines how values are mapped to screen coordinates.
     * Default is {@link LinearScale#INSTANCE}.
     *
     * <p>Example:
     * <pre>{@code
     * // Use logarithmic scale for crypto data
     * axis.setScale(LogarithmicScale.BASE_10);
     *
     * // Use percentage scale relative to first price
     * axis.setScale(new PercentageScale(firstPrice));
     * }</pre>
     *
     * @param scale the axis scale (null will reset to LinearScale)
     * @return this axis for method chaining
     */
    public YAxis setScale(AxisScale scale) {
        this.scale = (scale != null) ? scale : LinearScale.INSTANCE;
        return this;
    }

    // ========== Utility methods ==========

    /**
     * Formats a value using this axis's scale and value format.
     *
     * <p>Delegates to the scale's formatValue method, which may override
     * default formatting for special scale types (e.g., percentage).
     *
     * @param value the value to format
     * @return formatted string
     */
    public String formatValue(double value) {
        return scale.formatValue(value, valueFormat);
    }

    /**
     * Normalizes a value to the 0-1 range based on this axis's min/max and scale.
     *
     * <p>For linear scales: {@code (value - min) / (max - min)}
     * <p>For log scales: uses logarithmic transformation
     *
     * @param value the value to normalize
     * @return normalized value (0 = minValue, 1 = maxValue)
     */
    public double normalize(double value) {
        return scale.normalize(value, minValue, maxValue);
    }

    /**
     * Interpolates from a normalized value (0-1) to an actual value using this axis's scale.
     *
     * @param normalized normalized position (0-1)
     * @return actual value
     */
    public double interpolate(double normalized) {
        return scale.interpolate(normalized, minValue, maxValue);
    }

    /**
     * Calculates grid/label levels appropriate for this axis's scale and range.
     *
     * @param targetCount desired number of grid lines (hint, not exact)
     * @return array of values where grid lines/labels should appear
     */
    public double[] calculateGridLevels(int targetCount) {
        return scale.calculateGridLevels(minValue, maxValue, targetCount);
    }

    /**
     * Checks if the given range is valid for this axis's scale.
     *
     * <p>For example, logarithmic scales require positive values.
     *
     * @param min proposed minimum value
     * @param max proposed maximum value
     * @return true if the range is valid
     */
    public boolean isValidRange(double min, double max) {
        return scale.isValidRange(min, max);
    }

    /**
     * Expands the value range by the specified percentage on each end.
     *
     * @param percentage expansion percentage (e.g., 0.05 for 5%)
     */
    public void expandByPercent(double percentage) {
        double span = getValueSpan();
        double expansion = span * percentage;
        this.minValue -= expansion;
        this.maxValue += expansion;
    }

    // ========== Auto-range Support ==========

    /**
     * Checks if auto-range should be applied based on current mode.
     *
     * @return true if auto-ranging should occur
     */
    public boolean shouldAutoRange() {
        return switch (autoRangeMode) {
            case NEVER -> false;
            case ONCE -> !autoRangeApplied;
            case ALWAYS -> true;
        };
    }

    /**
     * Marks auto-range as applied (for ONCE mode).
     */
    public void markAutoRangeApplied() {
        this.autoRangeApplied = true;
    }

    /**
     * Resets the auto-range applied flag (for ONCE mode).
     */
    public void resetAutoRange() {
        this.autoRangeApplied = false;
    }

    @Override
    public String toString() {
        return String.format("YAxis[id=%s, pos=%s, range=%.2f-%.2f, visible=%s, autoRange=%s, heightRatio=%.2f, anchor=%s, scale=%s]",
                id, position, minValue, maxValue, visible, autoRangeMode, heightRatio, anchor, scale);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        YAxis other = (YAxis) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
