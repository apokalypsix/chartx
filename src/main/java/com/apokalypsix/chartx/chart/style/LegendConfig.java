package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.awt.Font;

/**
 * Configuration options for the chart legend.
 *
 * <p>The legend displays series names with their associated colors inside
 * the chart area. Uses a fluent builder pattern for easy configuration.
 */
public class LegendConfig {

    /**
     * Position of the legend within the chart area.
     */
    public enum Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_CENTER,
        BOTTOM_CENTER
    }

    /**
     * Orientation of legend items.
     */
    public enum Orientation {
        /** Items arranged horizontally */
        HORIZONTAL,
        /** Items arranged vertically */
        VERTICAL
    }

    // Position
    private Position position = Position.TOP_LEFT;
    private Orientation orientation = Orientation.HORIZONTAL;
    private int marginX = 10;
    private int marginY = 10;
    private int itemSpacing = 15;

    // Appearance
    private Color backgroundColor = new Color(30, 32, 36, 200);
    private Color borderColor = new Color(60, 62, 66);
    private Color textColor = new Color(220, 222, 226);
    private Font font = new Font("SansSerif", Font.PLAIN, 11);
    private int padding = 8;
    private int colorSwatchSize = 12;
    private int swatchSpacing = 5;
    private int cornerRadius = 4;

    // Visibility
    private boolean visible = true;
    private boolean showBackground = true;
    private boolean showBorder = true;

    /**
     * Creates default legend configuration.
     */
    public LegendConfig() {
    }

    /**
     * Creates a copy of the given configuration.
     */
    public LegendConfig(LegendConfig other) {
        this.position = other.position;
        this.orientation = other.orientation;
        this.marginX = other.marginX;
        this.marginY = other.marginY;
        this.itemSpacing = other.itemSpacing;
        this.backgroundColor = other.backgroundColor;
        this.borderColor = other.borderColor;
        this.textColor = other.textColor;
        this.font = other.font;
        this.padding = other.padding;
        this.colorSwatchSize = other.colorSwatchSize;
        this.swatchSpacing = other.swatchSpacing;
        this.cornerRadius = other.cornerRadius;
        this.visible = other.visible;
        this.showBackground = other.showBackground;
        this.showBorder = other.showBorder;
    }

    // ========== Getters ==========

    public Position getPosition() {
        return position;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getMarginX() {
        return marginX;
    }

    public int getMarginY() {
        return marginY;
    }

    public int getItemSpacing() {
        return itemSpacing;
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

    public Font getFont() {
        return font;
    }

    public int getPadding() {
        return padding;
    }

    public int getColorSwatchSize() {
        return colorSwatchSize;
    }

    public int getSwatchSpacing() {
        return swatchSpacing;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isShowBackground() {
        return showBackground;
    }

    public boolean isShowBorder() {
        return showBorder;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the legend position.
     *
     * @param position the position
     * @return this for chaining
     */
    public LegendConfig position(Position position) {
        this.position = position != null ? position : Position.TOP_LEFT;
        return this;
    }

    /**
     * Sets the item orientation.
     *
     * @param orientation the orientation
     * @return this for chaining
     */
    public LegendConfig orientation(Orientation orientation) {
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;
        return this;
    }

    /**
     * Sets the margin from chart edges.
     *
     * @param x horizontal margin in pixels
     * @param y vertical margin in pixels
     * @return this for chaining
     */
    public LegendConfig margin(int x, int y) {
        this.marginX = Math.max(0, x);
        this.marginY = Math.max(0, y);
        return this;
    }

    /**
     * Sets the spacing between legend items.
     *
     * @param spacing the spacing in pixels
     * @return this for chaining
     */
    public LegendConfig itemSpacing(int spacing) {
        this.itemSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the background color.
     *
     * @param color the background color (can have alpha for transparency)
     * @return this for chaining
     */
    public LegendConfig backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this for chaining
     */
    public LegendConfig borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the text color.
     *
     * @param color the text color
     * @return this for chaining
     */
    public LegendConfig textColor(Color color) {
        this.textColor = color;
        return this;
    }

    /**
     * Sets the font for legend text.
     *
     * @param font the font
     * @return this for chaining
     */
    public LegendConfig font(Font font) {
        this.font = font != null ? font : new Font("SansSerif", Font.PLAIN, 11);
        return this;
    }

    /**
     * Sets the internal padding.
     *
     * @param padding the padding in pixels
     * @return this for chaining
     */
    public LegendConfig padding(int padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    /**
     * Sets the color swatch size.
     *
     * @param size the size in pixels
     * @return this for chaining
     */
    public LegendConfig colorSwatchSize(int size) {
        this.colorSwatchSize = Math.max(4, size);
        return this;
    }

    /**
     * Sets the spacing between swatch and text.
     *
     * @param spacing the spacing in pixels
     * @return this for chaining
     */
    public LegendConfig swatchSpacing(int spacing) {
        this.swatchSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the corner radius for rounded rectangle background.
     *
     * @param radius the radius in pixels
     * @return this for chaining
     */
    public LegendConfig cornerRadius(int radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    /**
     * Sets whether the legend is visible.
     *
     * @param visible true to show legend
     * @return this for chaining
     */
    public LegendConfig visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets whether to show the background.
     *
     * @param show true to show background
     * @return this for chaining
     */
    public LegendConfig showBackground(boolean show) {
        this.showBackground = show;
        return this;
    }

    /**
     * Sets whether to show the border.
     *
     * @param show true to show border
     * @return this for chaining
     */
    public LegendConfig showBorder(boolean show) {
        this.showBorder = show;
        return this;
    }
}
