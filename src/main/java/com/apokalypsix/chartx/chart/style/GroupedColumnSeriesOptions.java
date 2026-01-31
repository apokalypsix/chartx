package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.util.Arrays;

/**
 * Rendering options for grouped column/bar series.
 *
 * <p>Provides configuration for group spacing, per-group colors,
 * and overall bar appearance. Uses a fluent builder pattern.
 */
public class GroupedColumnSeriesOptions extends SeriesOptions {

    /** Default color palette for groups */
    private static final Color[] DEFAULT_PALETTE = {
        new Color(65, 131, 196),   // Blue
        new Color(196, 98, 65),    // Orange
        new Color(98, 196, 65),    // Green
        new Color(196, 65, 131),   // Pink
        new Color(131, 65, 196),   // Purple
        new Color(65, 196, 163),   // Teal
        new Color(196, 163, 65),   // Yellow
        new Color(131, 196, 65),   // Lime
    };

    /** Colors for each group */
    private Color[] groupColors;

    /** Ratio of total group width to available bar slot width (0.0 - 1.0) */
    private float groupWidthRatio = 0.8f;

    /** Spacing between bars within a group (pixels) */
    private float barSpacing = 1.0f;

    /** Baseline value for bars */
    private double baseline = 0.0;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 1.0f;

    /** Whether to show bar borders */
    private boolean showBorders = false;

    /** Border color */
    private Color borderColor = new Color(40, 40, 40);

    /** Border width */
    private float borderWidth = 1.0f;

    /**
     * Creates default grouped column options.
     */
    public GroupedColumnSeriesOptions() {
        this.groupColors = Arrays.copyOf(DEFAULT_PALETTE, DEFAULT_PALETTE.length);
    }

    /**
     * Creates grouped column options with specific group count.
     */
    public GroupedColumnSeriesOptions(int groupCount) {
        this.groupColors = new Color[groupCount];
        for (int i = 0; i < groupCount; i++) {
            this.groupColors[i] = DEFAULT_PALETTE[i % DEFAULT_PALETTE.length];
        }
    }

    /**
     * Creates a copy of the given options.
     */
    public GroupedColumnSeriesOptions(GroupedColumnSeriesOptions other) {
        super(other);
        this.groupColors = Arrays.copyOf(other.groupColors, other.groupColors.length);
        this.groupWidthRatio = other.groupWidthRatio;
        this.barSpacing = other.barSpacing;
        this.baseline = other.baseline;
        this.opacity = other.opacity;
        this.showBorders = other.showBorders;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
    }

    // ========== Getters ==========

    /**
     * Returns the color for the specified group.
     */
    public Color getGroupColor(int groupIndex) {
        if (groupIndex < 0 || groupIndex >= groupColors.length) {
            return DEFAULT_PALETTE[groupIndex % DEFAULT_PALETTE.length];
        }
        return groupColors[groupIndex];
    }

    /**
     * Returns all group colors.
     */
    public Color[] getGroupColors() {
        return Arrays.copyOf(groupColors, groupColors.length);
    }

    public float getGroupWidthRatio() {
        return groupWidthRatio;
    }

    public float getBarSpacing() {
        return barSpacing;
    }

    public double getBaseline() {
        return baseline;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isShowBorders() {
        return showBorders;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the color for a specific group.
     *
     * @param groupIndex the group index
     * @param color the color
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions groupColor(int groupIndex, Color color) {
        ensureGroupColorCapacity(groupIndex + 1);
        groupColors[groupIndex] = color;
        return this;
    }

    /**
     * Sets colors for all groups.
     *
     * @param colors array of colors
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions groupColors(Color... colors) {
        this.groupColors = Arrays.copyOf(colors, colors.length);
        return this;
    }

    /**
     * Sets the ratio of total group width to available slot width.
     *
     * @param ratio the ratio (0.0 - 1.0)
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions groupWidthRatio(float ratio) {
        this.groupWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        return this;
    }

    /**
     * Sets the spacing between bars within a group.
     *
     * @param spacing the spacing in pixels
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions barSpacing(float spacing) {
        this.barSpacing = Math.max(0.0f, spacing);
        return this;
    }

    /**
     * Sets the baseline value for bars.
     *
     * @param baseline the baseline value
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions baseline(double baseline) {
        this.baseline = baseline;
        return this;
    }

    /**
     * Sets the overall opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    /**
     * Sets whether to show bar borders.
     *
     * @param showBorders true to show borders
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions showBorders(boolean showBorders) {
        this.showBorders = showBorders;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border width.
     *
     * @param width the border width
     * @return this for chaining
     */
    public GroupedColumnSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0.0f, width);
        return this;
    }

    // ========== Internal ==========

    private void ensureGroupColorCapacity(int minCapacity) {
        if (groupColors.length < minCapacity) {
            Color[] newColors = new Color[minCapacity];
            System.arraycopy(groupColors, 0, newColors, 0, groupColors.length);
            for (int i = groupColors.length; i < minCapacity; i++) {
                newColors[i] = DEFAULT_PALETTE[i % DEFAULT_PALETTE.length];
            }
            groupColors = newColors;
        }
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public GroupedColumnSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public GroupedColumnSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public GroupedColumnSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public GroupedColumnSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
