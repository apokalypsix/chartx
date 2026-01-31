package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.text.DecimalFormat;

/**
 * Configuration options for data labels on chart series.
 *
 * <p>Data labels display values directly on data points. This configuration
 * controls positioning, density (to avoid overlapping), and appearance.
 * Uses a fluent builder pattern for easy configuration.
 */
public class DataLabelConfig {

    /**
     * Position of the label relative to the data point.
     */
    public enum LabelPosition {
        ABOVE,
        BELOW,
        LEFT,
        RIGHT,
        CENTER
    }

    /**
     * Density control mode to avoid overlapping labels.
     */
    public enum DensityMode {
        /** Show all labels (may overlap) */
        ALL,
        /** Show every Nth label */
        EVERY_N,
        /** Show labels with minimum pixel spacing */
        MIN_SPACING,
        /** Only show at local minima and maxima */
        EXTREMA_ONLY,
        /** Show at significant values only */
        SIGNIFICANT
    }

    /**
     * Functional interface for custom value formatting.
     */
    @FunctionalInterface
    public interface ValueFormatter {
        String format(double value, int index);
    }

    // Display
    private boolean enabled = true;
    private LabelPosition position = LabelPosition.ABOVE;
    private int offsetX = 0;
    private int offsetY = -5;

    // Density control
    private DensityMode densityMode = DensityMode.MIN_SPACING;
    private int everyN = 5;
    private int minSpacing = 50;

    // LOD - hide when zoomed out
    private double minBarWidth = 10.0;

    // Appearance
    private Color textColor = Color.WHITE;
    private Color backgroundColor = null;
    private float fontSize = 10f;
    private int padding = 2;
    private int cornerRadius = 2;

    // Value formatting
    private String formatPattern = "#,##0.00";
    private ValueFormatter customFormatter = null;

    /**
     * Creates default data label configuration.
     */
    public DataLabelConfig() {
    }

    /**
     * Creates a copy of the given configuration.
     */
    public DataLabelConfig(DataLabelConfig other) {
        this.enabled = other.enabled;
        this.position = other.position;
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.densityMode = other.densityMode;
        this.everyN = other.everyN;
        this.minSpacing = other.minSpacing;
        this.minBarWidth = other.minBarWidth;
        this.textColor = other.textColor;
        this.backgroundColor = other.backgroundColor;
        this.fontSize = other.fontSize;
        this.padding = other.padding;
        this.cornerRadius = other.cornerRadius;
        this.formatPattern = other.formatPattern;
        this.customFormatter = other.customFormatter;
    }

    // ========== Getters ==========

    public boolean isEnabled() {
        return enabled;
    }

    public LabelPosition getPosition() {
        return position;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public DensityMode getDensityMode() {
        return densityMode;
    }

    public int getEveryN() {
        return everyN;
    }

    public int getMinSpacing() {
        return minSpacing;
    }

    public double getMinBarWidth() {
        return minBarWidth;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public float getFontSize() {
        return fontSize;
    }

    public int getPadding() {
        return padding;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public String getFormatPattern() {
        return formatPattern;
    }

    public ValueFormatter getCustomFormatter() {
        return customFormatter;
    }

    // ========== Fluent setters ==========

    /**
     * Sets whether data labels are enabled.
     *
     * @param enabled true to show labels
     * @return this for chaining
     */
    public DataLabelConfig enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the label position relative to data point.
     *
     * @param position the position
     * @return this for chaining
     */
    public DataLabelConfig position(LabelPosition position) {
        this.position = position != null ? position : LabelPosition.ABOVE;
        return this;
    }

    /**
     * Sets the offset from the calculated position.
     *
     * @param x horizontal offset in pixels
     * @param y vertical offset in pixels
     * @return this for chaining
     */
    public DataLabelConfig offset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }

    /**
     * Sets the density mode for label filtering.
     *
     * @param mode the density mode
     * @return this for chaining
     */
    public DataLabelConfig densityMode(DensityMode mode) {
        this.densityMode = mode != null ? mode : DensityMode.MIN_SPACING;
        return this;
    }

    /**
     * Sets the N for EVERY_N density mode.
     *
     * @param n show every Nth label
     * @return this for chaining
     */
    public DataLabelConfig everyN(int n) {
        this.everyN = Math.max(1, n);
        return this;
    }

    /**
     * Sets the minimum spacing between labels for MIN_SPACING mode.
     *
     * @param spacing minimum spacing in pixels
     * @return this for chaining
     */
    public DataLabelConfig minSpacing(int spacing) {
        this.minSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the minimum bar width (in pixels) below which labels are hidden.
     * This provides LOD (level of detail) support.
     *
     * @param width minimum bar width
     * @return this for chaining
     */
    public DataLabelConfig minBarWidth(double width) {
        this.minBarWidth = Math.max(0, width);
        return this;
    }

    /**
     * Sets the text color.
     *
     * @param color the text color
     * @return this for chaining
     */
    public DataLabelConfig textColor(Color color) {
        this.textColor = color != null ? color : Color.WHITE;
        return this;
    }

    /**
     * Sets the background color. Set to null for no background.
     *
     * @param color the background color (can be null)
     * @return this for chaining
     */
    public DataLabelConfig backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the font size.
     *
     * @param size the font size in points
     * @return this for chaining
     */
    public DataLabelConfig fontSize(float size) {
        this.fontSize = Math.max(6, size);
        return this;
    }

    /**
     * Sets the padding around the label text.
     *
     * @param padding the padding in pixels
     * @return this for chaining
     */
    public DataLabelConfig padding(int padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    /**
     * Sets the corner radius for label background.
     *
     * @param radius the radius in pixels
     * @return this for chaining
     */
    public DataLabelConfig cornerRadius(int radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    /**
     * Sets the DecimalFormat pattern for value formatting.
     *
     * @param pattern the format pattern
     * @return this for chaining
     */
    public DataLabelConfig formatPattern(String pattern) {
        this.formatPattern = pattern != null ? pattern : "#,##0.00";
        return this;
    }

    /**
     * Sets a custom value formatter. If set, overrides formatPattern.
     *
     * @param formatter the custom formatter
     * @return this for chaining
     */
    public DataLabelConfig customFormatter(ValueFormatter formatter) {
        this.customFormatter = formatter;
        return this;
    }

    /**
     * Formats a value according to the configuration.
     *
     * @param value the value to format
     * @param index the data index
     * @return the formatted string
     */
    public String formatValue(double value, int index) {
        if (customFormatter != null) {
            return customFormatter.format(value, index);
        }
        return new DecimalFormat(formatPattern).format(value);
    }
}
