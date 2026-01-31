package com.apokalypsix.chartx.core.ui.sidebar;

import java.awt.Color;
import java.awt.Font;

/**
 * Configuration options for the chart sidebar.
 *
 * <p>The sidebar displays tool buttons for drawing, indicators, and other
 * chart interactions. Uses a fluent builder pattern for easy configuration.
 *
 * <p>Example usage:
 * <pre>{@code
 * SidebarConfig config = new SidebarConfig()
 *     .position(SidebarPosition.LEFT)
 *     .width(48)
 *     .collapsible(true)
 *     .visible(true);
 * }</pre>
 */
public class SidebarConfig {

    // Position
    private SidebarPosition position = SidebarPosition.LEFT;
    private int width = 48;
    private int collapsedWidth = 8;

    // Appearance
    private Color backgroundColor = new Color(25, 27, 31);
    private Color borderColor = new Color(60, 62, 66);
    private Color buttonColor = new Color(40, 42, 46);
    private Color buttonHoverColor = new Color(55, 57, 61);
    private Color buttonSelectedColor = new Color(42, 130, 255);
    private Color textColor = new Color(180, 182, 186);
    private Color textSelectedColor = new Color(255, 255, 255);
    private Font buttonFont = new Font("SansSerif", Font.BOLD, 10);
    private int buttonSize = 36;
    private int buttonPadding = 6;
    private int groupSpacing = 8;
    private int cornerRadius = 4;

    // Behavior
    private boolean visible = true;
    private boolean collapsible = true;
    private boolean showTooltips = true;
    private boolean showGroupHeaders = false;

    /**
     * Creates default sidebar configuration.
     */
    public SidebarConfig() {
    }

    /**
     * Creates a copy of the given configuration.
     */
    public SidebarConfig(SidebarConfig other) {
        this.position = other.position;
        this.width = other.width;
        this.collapsedWidth = other.collapsedWidth;
        this.backgroundColor = other.backgroundColor;
        this.borderColor = other.borderColor;
        this.buttonColor = other.buttonColor;
        this.buttonHoverColor = other.buttonHoverColor;
        this.buttonSelectedColor = other.buttonSelectedColor;
        this.textColor = other.textColor;
        this.textSelectedColor = other.textSelectedColor;
        this.buttonFont = other.buttonFont;
        this.buttonSize = other.buttonSize;
        this.buttonPadding = other.buttonPadding;
        this.groupSpacing = other.groupSpacing;
        this.cornerRadius = other.cornerRadius;
        this.visible = other.visible;
        this.collapsible = other.collapsible;
        this.showTooltips = other.showTooltips;
        this.showGroupHeaders = other.showGroupHeaders;
    }

    // ========== Getters ==========

    public SidebarPosition getPosition() {
        return position;
    }

    public int getWidth() {
        return width;
    }

    public int getCollapsedWidth() {
        return collapsedWidth;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public Color getButtonColor() {
        return buttonColor;
    }

    public Color getButtonHoverColor() {
        return buttonHoverColor;
    }

    public Color getButtonSelectedColor() {
        return buttonSelectedColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getTextSelectedColor() {
        return textSelectedColor;
    }

    public Font getButtonFont() {
        return buttonFont;
    }

    public int getButtonSize() {
        return buttonSize;
    }

    public int getButtonPadding() {
        return buttonPadding;
    }

    public int getGroupSpacing() {
        return groupSpacing;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public boolean isShowTooltips() {
        return showTooltips;
    }

    public boolean isShowGroupHeaders() {
        return showGroupHeaders;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the sidebar position.
     */
    public SidebarConfig position(SidebarPosition position) {
        this.position = position != null ? position : SidebarPosition.LEFT;
        return this;
    }

    /**
     * Sets the sidebar width in pixels.
     */
    public SidebarConfig width(int width) {
        this.width = Math.max(32, width);
        return this;
    }

    /**
     * Sets the width when collapsed.
     */
    public SidebarConfig collapsedWidth(int width) {
        this.collapsedWidth = Math.max(4, width);
        return this;
    }

    /**
     * Sets the background color.
     */
    public SidebarConfig backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the border color.
     */
    public SidebarConfig borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the button background color.
     */
    public SidebarConfig buttonColor(Color color) {
        this.buttonColor = color;
        return this;
    }

    /**
     * Sets the button hover color.
     */
    public SidebarConfig buttonHoverColor(Color color) {
        this.buttonHoverColor = color;
        return this;
    }

    /**
     * Sets the button selected color.
     */
    public SidebarConfig buttonSelectedColor(Color color) {
        this.buttonSelectedColor = color;
        return this;
    }

    /**
     * Sets the text color for buttons.
     */
    public SidebarConfig textColor(Color color) {
        this.textColor = color;
        return this;
    }

    /**
     * Sets the text color when button is selected.
     */
    public SidebarConfig textSelectedColor(Color color) {
        this.textSelectedColor = color;
        return this;
    }

    /**
     * Sets the button font.
     */
    public SidebarConfig buttonFont(Font font) {
        this.buttonFont = font != null ? font : new Font("SansSerif", Font.BOLD, 10);
        return this;
    }

    /**
     * Sets the button size in pixels.
     */
    public SidebarConfig buttonSize(int size) {
        this.buttonSize = Math.max(24, size);
        return this;
    }

    /**
     * Sets the padding between buttons.
     */
    public SidebarConfig buttonPadding(int padding) {
        this.buttonPadding = Math.max(0, padding);
        return this;
    }

    /**
     * Sets the spacing between tool groups.
     */
    public SidebarConfig groupSpacing(int spacing) {
        this.groupSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the corner radius for buttons.
     */
    public SidebarConfig cornerRadius(int radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    /**
     * Sets whether the sidebar is visible.
     */
    public SidebarConfig visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets whether the sidebar can be collapsed.
     */
    public SidebarConfig collapsible(boolean collapsible) {
        this.collapsible = collapsible;
        return this;
    }

    /**
     * Sets whether to show tooltips on hover.
     */
    public SidebarConfig showTooltips(boolean show) {
        this.showTooltips = show;
        return this;
    }

    /**
     * Sets whether to show group headers.
     */
    public SidebarConfig showGroupHeaders(boolean show) {
        this.showGroupHeaders = show;
        return this;
    }
}
