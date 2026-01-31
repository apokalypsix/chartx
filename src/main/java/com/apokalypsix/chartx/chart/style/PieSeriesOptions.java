package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rendering options for pie and donut chart series.
 *
 * <p>Controls appearance of pie/donut slices including colors, borders,
 * labels, and explode effects.
 */
public class PieSeriesOptions {

    /** Label display mode */
    public enum LabelMode {
        NONE,           // No labels
        INSIDE,         // Labels inside slices
        OUTSIDE,        // Labels outside with leader lines
        PERCENT,        // Show percentages inside
        VALUE,          // Show values inside
        LABEL_PERCENT   // Show label and percent
    }

    /** Default color palette for slices */
    private static final Color[] DEFAULT_COLORS = {
        new Color(65, 131, 196),   // Blue
        new Color(220, 100, 50),   // Orange
        new Color(50, 170, 80),    // Green
        new Color(200, 60, 60),    // Red
        new Color(155, 89, 182),   // Purple
        new Color(52, 152, 219),   // Light blue
        new Color(241, 196, 15),   // Yellow
        new Color(46, 204, 113),   // Emerald
        new Color(231, 76, 60),    // Coral
        new Color(149, 165, 166)   // Gray
    };

    /** Colors for each slice (cycles if fewer colors than slices) */
    private List<Color> sliceColors;

    /** Border color for slices */
    private Color borderColor = new Color(255, 255, 255, 200);

    /** Border width */
    private float borderWidth = 1.5f;

    /** Inner radius ratio (0 = pie, > 0 = donut) */
    private float innerRadiusRatio = 0.0f;

    /** Padding ratio around the chart */
    private float paddingRatio = 0.1f;

    /** Default explode offset in pixels */
    private float defaultExplodeOffset = 0;

    /** Whether to show labels */
    private LabelMode labelMode = LabelMode.NONE;

    /** Label color */
    private Color labelColor = new Color(40, 40, 40);

    /** Label font size */
    private float labelFontSize = 12f;

    /** Minimum slice percentage to show label (avoid clutter) */
    private float minLabelPercent = 3f;

    /** Start angle in degrees (0 = 3 o'clock, 90 = 12 o'clock) */
    private float startAngleDegrees = -90f;

    /** Whether slices are ordered clockwise */
    private boolean clockwise = true;

    /** Number of segments per slice (more = smoother) */
    private int segmentsPerSlice = 32;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether chart is visible */
    private boolean visible = true;

    /**
     * Creates default pie series options.
     */
    public PieSeriesOptions() {
        this.sliceColors = new ArrayList<>(Arrays.asList(DEFAULT_COLORS));
    }

    /**
     * Creates a copy of the given options.
     */
    public PieSeriesOptions(PieSeriesOptions other) {
        this.sliceColors = new ArrayList<>(other.sliceColors);
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.innerRadiusRatio = other.innerRadiusRatio;
        this.paddingRatio = other.paddingRatio;
        this.defaultExplodeOffset = other.defaultExplodeOffset;
        this.labelMode = other.labelMode;
        this.labelColor = other.labelColor;
        this.labelFontSize = other.labelFontSize;
        this.minLabelPercent = other.minLabelPercent;
        this.startAngleDegrees = other.startAngleDegrees;
        this.clockwise = other.clockwise;
        this.segmentsPerSlice = other.segmentsPerSlice;
        this.opacity = other.opacity;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    /**
     * Returns the color for the slice at the given index.
     * Colors cycle if there are more slices than colors.
     */
    public Color getSliceColor(int index) {
        if (sliceColors.isEmpty()) {
            return DEFAULT_COLORS[index % DEFAULT_COLORS.length];
        }
        return sliceColors.get(index % sliceColors.size());
    }

    public List<Color> getSliceColors() {
        return sliceColors;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getInnerRadiusRatio() {
        return innerRadiusRatio;
    }

    public float getPaddingRatio() {
        return paddingRatio;
    }

    public float getDefaultExplodeOffset() {
        return defaultExplodeOffset;
    }

    public LabelMode getLabelMode() {
        return labelMode;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public float getLabelFontSize() {
        return labelFontSize;
    }

    public float getMinLabelPercent() {
        return minLabelPercent;
    }

    public float getStartAngleDegrees() {
        return startAngleDegrees;
    }

    public double getStartAngleRadians() {
        return Math.toRadians(startAngleDegrees);
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public int getSegmentsPerSlice() {
        return segmentsPerSlice;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isDonut() {
        return innerRadiusRatio > 0;
    }

    // ========== Fluent Setters ==========

    public PieSeriesOptions sliceColors(Color... colors) {
        this.sliceColors = new ArrayList<>(Arrays.asList(colors));
        return this;
    }

    public PieSeriesOptions sliceColors(List<Color> colors) {
        this.sliceColors = new ArrayList<>(colors);
        return this;
    }

    public PieSeriesOptions addSliceColor(Color color) {
        this.sliceColors.add(color);
        return this;
    }

    public PieSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public PieSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0, width);
        return this;
    }

    public PieSeriesOptions noBorder() {
        this.borderWidth = 0;
        return this;
    }

    public PieSeriesOptions innerRadiusRatio(float ratio) {
        this.innerRadiusRatio = Math.max(0, Math.min(0.95f, ratio));
        return this;
    }

    /**
     * Convenience method to create a donut chart with the given hole size.
     */
    public PieSeriesOptions donut(float holeRatio) {
        return innerRadiusRatio(holeRatio);
    }

    public PieSeriesOptions paddingRatio(float ratio) {
        this.paddingRatio = Math.max(0, Math.min(0.5f, ratio));
        return this;
    }

    public PieSeriesOptions defaultExplodeOffset(float offset) {
        this.defaultExplodeOffset = Math.max(0, offset);
        return this;
    }

    public PieSeriesOptions labelMode(LabelMode mode) {
        this.labelMode = mode;
        return this;
    }

    public PieSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public PieSeriesOptions labelFontSize(float size) {
        this.labelFontSize = Math.max(6, size);
        return this;
    }

    public PieSeriesOptions minLabelPercent(float percent) {
        this.minLabelPercent = Math.max(0, percent);
        return this;
    }

    public PieSeriesOptions startAngleDegrees(float degrees) {
        this.startAngleDegrees = degrees;
        return this;
    }

    public PieSeriesOptions clockwise(boolean clockwise) {
        this.clockwise = clockwise;
        return this;
    }

    public PieSeriesOptions segmentsPerSlice(int segments) {
        this.segmentsPerSlice = Math.max(3, Math.min(64, segments));
        return this;
    }

    public PieSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public PieSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
