package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for sunburst series.
 *
 * <p>Controls appearance of hierarchical donut charts
 * with multiple concentric rings.
 */
public class SunburstSeriesOptions {

    /** Inner radius ratio (0-1, relative to outer radius) */
    private float innerRadiusRatio = 0.2f;

    /** Ring width ratio (how much of remaining space each ring takes) */
    private float ringWidthRatio = 0.8f;

    /** Gap between rings in pixels */
    private float ringGap = 2f;

    /** Gap between segments in degrees */
    private float segmentGap = 0.5f;

    /** Default segment color */
    private Color defaultColor = new Color(65, 131, 196);

    /** Border color */
    private Color borderColor = new Color(30, 30, 30);

    /** Border width */
    private float borderWidth = 1.0f;

    /** Whether to show labels */
    private boolean showLabels = true;

    /** Label color */
    private Color labelColor = new Color(240, 240, 240);

    /** Label font size */
    private float labelFontSize = 11f;

    /** Minimum arc angle (degrees) to show label */
    private float minLabelAngle = 10f;

    /** Whether to show center label */
    private boolean showCenterLabel = true;

    /** Center label text */
    private String centerLabel = "";

    /** Center label font size */
    private float centerLabelFontSize = 14f;

    /** Maximum visible depth (0 = all levels) */
    private int maxVisibleDepth = 0;

    /** Arc tessellation segments per degree */
    private int segmentsPerDegree = 2;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether visible */
    private boolean visible = true;

    /**
     * Creates default sunburst options.
     */
    public SunburstSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public SunburstSeriesOptions(SunburstSeriesOptions other) {
        this.innerRadiusRatio = other.innerRadiusRatio;
        this.ringWidthRatio = other.ringWidthRatio;
        this.ringGap = other.ringGap;
        this.segmentGap = other.segmentGap;
        this.defaultColor = other.defaultColor;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.showLabels = other.showLabels;
        this.labelColor = other.labelColor;
        this.labelFontSize = other.labelFontSize;
        this.minLabelAngle = other.minLabelAngle;
        this.showCenterLabel = other.showCenterLabel;
        this.centerLabel = other.centerLabel;
        this.centerLabelFontSize = other.centerLabelFontSize;
        this.maxVisibleDepth = other.maxVisibleDepth;
        this.segmentsPerDegree = other.segmentsPerDegree;
        this.opacity = other.opacity;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    public float getInnerRadiusRatio() {
        return innerRadiusRatio;
    }

    public float getRingWidthRatio() {
        return ringWidthRatio;
    }

    public float getRingGap() {
        return ringGap;
    }

    public float getSegmentGap() {
        return segmentGap;
    }

    public Color getDefaultColor() {
        return defaultColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public float getLabelFontSize() {
        return labelFontSize;
    }

    public float getMinLabelAngle() {
        return minLabelAngle;
    }

    public boolean isShowCenterLabel() {
        return showCenterLabel;
    }

    public String getCenterLabel() {
        return centerLabel;
    }

    public float getCenterLabelFontSize() {
        return centerLabelFontSize;
    }

    public int getMaxVisibleDepth() {
        return maxVisibleDepth;
    }

    public int getSegmentsPerDegree() {
        return segmentsPerDegree;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Calculates the inner and outer radius for a given depth level.
     *
     * @param depth node depth (0 = root)
     * @param outerRadius total outer radius of the chart
     * @return array of [innerRadius, outerRadius] for this ring
     */
    public float[] getRingRadii(int depth, float outerRadius) {
        if (depth == 0) {
            // Root - center area
            return new float[]{0, outerRadius * innerRadiusRatio};
        }

        float availableRadius = outerRadius * (1 - innerRadiusRatio);
        int maxDepth = maxVisibleDepth > 0 ? maxVisibleDepth : 10;
        float ringWidth = availableRadius / maxDepth;

        float inner = outerRadius * innerRadiusRatio + (depth - 1) * ringWidth + ringGap / 2;
        float outer = outerRadius * innerRadiusRatio + depth * ringWidth - ringGap / 2;

        return new float[]{inner, Math.min(outer, outerRadius)};
    }

    // ========== Fluent Setters ==========

    public SunburstSeriesOptions innerRadiusRatio(float ratio) {
        this.innerRadiusRatio = Math.max(0, Math.min(0.8f, ratio));
        return this;
    }

    public SunburstSeriesOptions ringWidthRatio(float ratio) {
        this.ringWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        return this;
    }

    public SunburstSeriesOptions ringGap(float gap) {
        this.ringGap = Math.max(0, gap);
        return this;
    }

    public SunburstSeriesOptions segmentGap(float gap) {
        this.segmentGap = Math.max(0, gap);
        return this;
    }

    public SunburstSeriesOptions defaultColor(Color color) {
        this.defaultColor = color;
        return this;
    }

    public SunburstSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public SunburstSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0, width);
        return this;
    }

    public SunburstSeriesOptions showLabels(boolean show) {
        this.showLabels = show;
        return this;
    }

    public SunburstSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public SunburstSeriesOptions minLabelAngle(float angle) {
        this.minLabelAngle = Math.max(1, angle);
        return this;
    }

    public SunburstSeriesOptions showCenterLabel(boolean show) {
        this.showCenterLabel = show;
        return this;
    }

    public SunburstSeriesOptions centerLabel(String label) {
        this.centerLabel = label;
        return this;
    }

    public SunburstSeriesOptions maxVisibleDepth(int depth) {
        this.maxVisibleDepth = Math.max(0, depth);
        return this;
    }

    public SunburstSeriesOptions segmentsPerDegree(int segments) {
        this.segmentsPerDegree = Math.max(1, Math.min(10, segments));
        return this;
    }

    public SunburstSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public SunburstSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
