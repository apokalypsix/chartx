package com.apokalypsix.chartx.core.ui.overlay;

import java.awt.Color;
import java.awt.Font;

/**
 * Configuration options for foldable info overlays.
 *
 * <p>Info overlays display information like symbol details, OHLC values,
 * and indicator values in collapsible panels at chart corners.
 *
 * <p>Example usage:
 * <pre>{@code
 * InfoOverlayConfig config = new InfoOverlayConfig()
 *     .position(OverlayPosition.TOP_LEFT)
 *     .visible(true)
 *     .collapsible(true)
 *     .showBackground(true);
 * }</pre>
 */
public class InfoOverlayConfig {

    // Position
    private OverlayPosition position = OverlayPosition.TOP_LEFT;
    private int marginX = 10;
    private int marginY = 10;
    private int stackSpacing = 5;

    // Appearance
    private Color backgroundColor = new Color(30, 32, 36, 220);
    private Color borderColor = new Color(60, 62, 66);
    private Color headerColor = new Color(220, 222, 226);
    private Color labelColor = new Color(140, 142, 146);
    private Color valueColor = new Color(220, 222, 226);
    private Color positiveColor = new Color(38, 166, 154);
    private Color negativeColor = new Color(239, 83, 80);
    private Color expandButtonColor = new Color(100, 102, 106);
    private Font headerFont = new Font("SansSerif", Font.BOLD, 12);
    private Font labelFont = new Font("SansSerif", Font.PLAIN, 10);
    private Font valueFont = new Font("SansSerif", Font.PLAIN, 11);
    private int padding = 8;
    private int rowSpacing = 4;
    private int columnSpacing = 12;
    private int cornerRadius = 4;
    private int minWidth = 120;

    // Behavior
    private boolean visible = true;
    private boolean collapsible = true;
    private boolean collapsed = false;
    private boolean showBackground = true;
    private boolean showBorder = true;
    private boolean showExpandButton = true;

    // Transparent mode (for minimal overlays)
    private boolean transparent = false;
    private boolean textShadow = false;
    private CollapseIconPosition collapseIconPosition = CollapseIconPosition.HEADER;

    /**
     * Position of the collapse/expand icon.
     */
    public enum CollapseIconPosition {
        /** Icon in header row (default behavior) */
        HEADER,
        /** Icon on its own row at bottom when expanded */
        BOTTOM_ROW
    }

    /**
     * Creates default info overlay configuration.
     */
    public InfoOverlayConfig() {
    }

    /**
     * Creates a copy of the given configuration.
     */
    public InfoOverlayConfig(InfoOverlayConfig other) {
        this.position = other.position;
        this.marginX = other.marginX;
        this.marginY = other.marginY;
        this.stackSpacing = other.stackSpacing;
        this.backgroundColor = other.backgroundColor;
        this.borderColor = other.borderColor;
        this.headerColor = other.headerColor;
        this.labelColor = other.labelColor;
        this.valueColor = other.valueColor;
        this.positiveColor = other.positiveColor;
        this.negativeColor = other.negativeColor;
        this.expandButtonColor = other.expandButtonColor;
        this.headerFont = other.headerFont;
        this.labelFont = other.labelFont;
        this.valueFont = other.valueFont;
        this.padding = other.padding;
        this.rowSpacing = other.rowSpacing;
        this.columnSpacing = other.columnSpacing;
        this.cornerRadius = other.cornerRadius;
        this.minWidth = other.minWidth;
        this.visible = other.visible;
        this.collapsible = other.collapsible;
        this.collapsed = other.collapsed;
        this.showBackground = other.showBackground;
        this.showBorder = other.showBorder;
        this.showExpandButton = other.showExpandButton;
        this.transparent = other.transparent;
        this.textShadow = other.textShadow;
        this.collapseIconPosition = other.collapseIconPosition;
    }

    // ========== Getters ==========

    public OverlayPosition getPosition() {
        return position;
    }

    public int getMarginX() {
        return marginX;
    }

    public int getMarginY() {
        return marginY;
    }

    public int getStackSpacing() {
        return stackSpacing;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public Color getHeaderColor() {
        return headerColor;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public Color getValueColor() {
        return valueColor;
    }

    public Color getPositiveColor() {
        return positiveColor;
    }

    public Color getNegativeColor() {
        return negativeColor;
    }

    public Color getExpandButtonColor() {
        return expandButtonColor;
    }

    public Font getHeaderFont() {
        return headerFont;
    }

    public Font getLabelFont() {
        return labelFont;
    }

    public Font getValueFont() {
        return valueFont;
    }

    public int getPadding() {
        return padding;
    }

    public int getRowSpacing() {
        return rowSpacing;
    }

    public int getColumnSpacing() {
        return columnSpacing;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public boolean isShowBackground() {
        return showBackground;
    }

    public boolean isShowBorder() {
        return showBorder;
    }

    public boolean isShowExpandButton() {
        return showExpandButton;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isTextShadow() {
        return textShadow;
    }

    public CollapseIconPosition getCollapseIconPosition() {
        return collapseIconPosition;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the overlay position.
     */
    public InfoOverlayConfig position(OverlayPosition position) {
        this.position = position != null ? position : OverlayPosition.TOP_LEFT;
        return this;
    }

    /**
     * Sets the margin from chart edges.
     */
    public InfoOverlayConfig margin(int x, int y) {
        this.marginX = Math.max(0, x);
        this.marginY = Math.max(0, y);
        return this;
    }

    /**
     * Sets the spacing between stacked overlays.
     */
    public InfoOverlayConfig stackSpacing(int spacing) {
        this.stackSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the background color.
     */
    public InfoOverlayConfig backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the border color.
     */
    public InfoOverlayConfig borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the header text color.
     */
    public InfoOverlayConfig headerColor(Color color) {
        this.headerColor = color;
        return this;
    }

    /**
     * Sets the label text color.
     */
    public InfoOverlayConfig labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    /**
     * Sets the value text color.
     */
    public InfoOverlayConfig valueColor(Color color) {
        this.valueColor = color;
        return this;
    }

    /**
     * Sets the color for positive values/changes.
     */
    public InfoOverlayConfig positiveColor(Color color) {
        this.positiveColor = color;
        return this;
    }

    /**
     * Sets the color for negative values/changes.
     */
    public InfoOverlayConfig negativeColor(Color color) {
        this.negativeColor = color;
        return this;
    }

    /**
     * Sets the expand button color.
     */
    public InfoOverlayConfig expandButtonColor(Color color) {
        this.expandButtonColor = color;
        return this;
    }

    /**
     * Sets the header font.
     */
    public InfoOverlayConfig headerFont(Font font) {
        this.headerFont = font != null ? font : new Font("SansSerif", Font.BOLD, 12);
        return this;
    }

    /**
     * Sets the label font.
     */
    public InfoOverlayConfig labelFont(Font font) {
        this.labelFont = font != null ? font : new Font("SansSerif", Font.PLAIN, 10);
        return this;
    }

    /**
     * Sets the value font.
     */
    public InfoOverlayConfig valueFont(Font font) {
        this.valueFont = font != null ? font : new Font("SansSerif", Font.PLAIN, 11);
        return this;
    }

    /**
     * Sets the internal padding.
     */
    public InfoOverlayConfig padding(int padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    /**
     * Sets the spacing between rows.
     */
    public InfoOverlayConfig rowSpacing(int spacing) {
        this.rowSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the spacing between columns.
     */
    public InfoOverlayConfig columnSpacing(int spacing) {
        this.columnSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the corner radius.
     */
    public InfoOverlayConfig cornerRadius(int radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    /**
     * Sets the minimum width.
     */
    public InfoOverlayConfig minWidth(int width) {
        this.minWidth = Math.max(60, width);
        return this;
    }

    /**
     * Sets whether the overlay is visible.
     */
    public InfoOverlayConfig visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets whether the overlay can be collapsed.
     */
    public InfoOverlayConfig collapsible(boolean collapsible) {
        this.collapsible = collapsible;
        return this;
    }

    /**
     * Sets whether the overlay starts collapsed.
     */
    public InfoOverlayConfig collapsed(boolean collapsed) {
        this.collapsed = collapsed;
        return this;
    }

    /**
     * Sets whether to show the background.
     */
    public InfoOverlayConfig showBackground(boolean show) {
        this.showBackground = show;
        return this;
    }

    /**
     * Sets whether to show the border.
     */
    public InfoOverlayConfig showBorder(boolean show) {
        this.showBorder = show;
        return this;
    }

    /**
     * Sets whether to show the expand/collapse button.
     */
    public InfoOverlayConfig showExpandButton(boolean show) {
        this.showExpandButton = show;
        return this;
    }

    /**
     * Sets whether the overlay is transparent (no background or border).
     * When transparent, text shadow is recommended for readability.
     */
    public InfoOverlayConfig transparent(boolean transparent) {
        this.transparent = transparent;
        if (transparent) {
            this.showBackground = false;
            this.showBorder = false;
        }
        return this;
    }

    /**
     * Sets whether to render text shadow for readability on transparent overlays.
     */
    public InfoOverlayConfig textShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    /**
     * Sets the position of the collapse/expand icon.
     */
    public InfoOverlayConfig collapseIconPosition(CollapseIconPosition position) {
        this.collapseIconPosition = position != null ? position : CollapseIconPosition.HEADER;
        return this;
    }
}
