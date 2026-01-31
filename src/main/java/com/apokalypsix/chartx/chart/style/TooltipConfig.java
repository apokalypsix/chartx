package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.awt.Font;

/**
 * Configuration options for the chart tooltip.
 *
 * <p>The tooltip displays data values at the cursor position,
 * following the cursor as it moves across the chart.
 * Uses a fluent builder pattern for easy configuration.
 */
public class TooltipConfig {

    /**
     * Tooltip display mode for series values.
     */
    public enum TooltipMode {
        /** Show values for all visible series at crosshair position */
        MULTI_SERIES,
        /** Show only primary series (OHLC) values */
        PRIMARY_ONLY,
        /** Show only the nearest series to cursor */
        NEAREST_SERIES
    }

    // Behavior
    private boolean enabled = true;
    private TooltipMode mode = TooltipMode.MULTI_SERIES;
    private boolean snapToDataPoint = true;
    private boolean showTimestamp = true;
    private boolean showOnlyVisibleSeries = true;

    // Position
    private int offsetX = 15;
    private int offsetY = 15;
    private boolean avoidCursor = true;

    // Appearance
    private Color backgroundColor = new Color(30, 32, 36, 230);
    private Color borderColor = new Color(70, 72, 76);
    private Color textColor = new Color(220, 222, 226);
    private Color labelColor = new Color(140, 142, 146);
    private Font font = new Font("SansSerif", Font.PLAIN, 11);
    private Font labelFont = new Font("SansSerif", Font.BOLD, 10);
    private int padding = 8;
    private int rowSpacing = 4;
    private int cornerRadius = 6;

    // Formatting
    private int priceDecimals = 2;
    private String timestampFormat = "HH:mm:ss";

    /**
     * Creates default tooltip configuration.
     */
    public TooltipConfig() {
    }

    /**
     * Creates a copy of the given configuration.
     */
    public TooltipConfig(TooltipConfig other) {
        this.enabled = other.enabled;
        this.mode = other.mode;
        this.snapToDataPoint = other.snapToDataPoint;
        this.showTimestamp = other.showTimestamp;
        this.showOnlyVisibleSeries = other.showOnlyVisibleSeries;
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.avoidCursor = other.avoidCursor;
        this.backgroundColor = other.backgroundColor;
        this.borderColor = other.borderColor;
        this.textColor = other.textColor;
        this.labelColor = other.labelColor;
        this.font = other.font;
        this.labelFont = other.labelFont;
        this.padding = other.padding;
        this.rowSpacing = other.rowSpacing;
        this.cornerRadius = other.cornerRadius;
        this.priceDecimals = other.priceDecimals;
        this.timestampFormat = other.timestampFormat;
    }

    // ========== Getters ==========

    public boolean isEnabled() {
        return enabled;
    }

    public TooltipMode getMode() {
        return mode;
    }

    public boolean isSnapToDataPoint() {
        return snapToDataPoint;
    }

    public boolean isShowTimestamp() {
        return showTimestamp;
    }

    public boolean isShowOnlyVisibleSeries() {
        return showOnlyVisibleSeries;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public boolean isAvoidCursor() {
        return avoidCursor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public Font getFont() {
        return font;
    }

    public Font getLabelFont() {
        return labelFont;
    }

    public int getPadding() {
        return padding;
    }

    public int getRowSpacing() {
        return rowSpacing;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public int getPriceDecimals() {
        return priceDecimals;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    // ========== Fluent setters ==========

    /**
     * Sets whether the tooltip is enabled.
     *
     * @param enabled true to enable tooltip
     * @return this for chaining
     */
    public TooltipConfig enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the tooltip display mode.
     *
     * @param mode the mode
     * @return this for chaining
     */
    public TooltipConfig mode(TooltipMode mode) {
        this.mode = mode != null ? mode : TooltipMode.MULTI_SERIES;
        return this;
    }

    /**
     * Sets whether to snap to the nearest data point.
     *
     * @param snap true to snap to data points
     * @return this for chaining
     */
    public TooltipConfig snapToDataPoint(boolean snap) {
        this.snapToDataPoint = snap;
        return this;
    }

    /**
     * Sets whether to show the timestamp in the tooltip.
     *
     * @param show true to show timestamp
     * @return this for chaining
     */
    public TooltipConfig showTimestamp(boolean show) {
        this.showTimestamp = show;
        return this;
    }

    /**
     * Sets whether to only show visible series in the tooltip.
     *
     * @param show true to filter to visible series
     * @return this for chaining
     */
    public TooltipConfig showOnlyVisibleSeries(boolean show) {
        this.showOnlyVisibleSeries = show;
        return this;
    }

    /**
     * Sets the offset from the cursor position.
     *
     * @param x horizontal offset in pixels
     * @param y vertical offset in pixels
     * @return this for chaining
     */
    public TooltipConfig offset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }

    /**
     * Sets whether to automatically reposition to avoid obscuring the cursor.
     *
     * @param avoid true to avoid cursor
     * @return this for chaining
     */
    public TooltipConfig avoidCursor(boolean avoid) {
        this.avoidCursor = avoid;
        return this;
    }

    /**
     * Sets the background color.
     *
     * @param color the background color
     * @return this for chaining
     */
    public TooltipConfig backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this for chaining
     */
    public TooltipConfig borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the text color.
     *
     * @param color the text color
     * @return this for chaining
     */
    public TooltipConfig textColor(Color color) {
        this.textColor = color;
        return this;
    }

    /**
     * Sets the label color (for field names like "O:", "H:", etc.).
     *
     * @param color the label color
     * @return this for chaining
     */
    public TooltipConfig labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    /**
     * Sets the font for tooltip text.
     *
     * @param font the font
     * @return this for chaining
     */
    public TooltipConfig font(Font font) {
        this.font = font != null ? font : new Font("SansSerif", Font.PLAIN, 11);
        return this;
    }

    /**
     * Sets the font for labels.
     *
     * @param font the label font
     * @return this for chaining
     */
    public TooltipConfig labelFont(Font font) {
        this.labelFont = font != null ? font : new Font("SansSerif", Font.BOLD, 10);
        return this;
    }

    /**
     * Sets the internal padding.
     *
     * @param padding the padding in pixels
     * @return this for chaining
     */
    public TooltipConfig padding(int padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    /**
     * Sets the spacing between rows.
     *
     * @param spacing the spacing in pixels
     * @return this for chaining
     */
    public TooltipConfig rowSpacing(int spacing) {
        this.rowSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the corner radius for rounded rectangle.
     *
     * @param radius the radius in pixels
     * @return this for chaining
     */
    public TooltipConfig cornerRadius(int radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    /**
     * Sets the number of decimal places for price values.
     *
     * @param decimals the number of decimals
     * @return this for chaining
     */
    public TooltipConfig priceDecimals(int decimals) {
        this.priceDecimals = Math.max(0, Math.min(10, decimals));
        return this;
    }

    /**
     * Sets the timestamp format pattern.
     *
     * @param format the SimpleDateFormat pattern
     * @return this for chaining
     */
    public TooltipConfig timestampFormat(String format) {
        this.timestampFormat = format != null ? format : "HH:mm:ss";
        return this;
    }
}
