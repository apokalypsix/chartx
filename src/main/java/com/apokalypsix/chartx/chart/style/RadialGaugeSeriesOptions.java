package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for radial gauge series.
 *
 * <p>Controls appearance of arc-based gauges with needles,
 * color zones, and tick marks.
 */
public class RadialGaugeSeriesOptions {

    /** Needle style options */
    public enum NeedleStyle {
        LINE,       // Simple line from center
        ARROW,      // Arrow-shaped needle
        TRIANGLE    // Triangular needle
    }

    /** Arc span in degrees (full circle = 360) */
    private float arcSpan = 270f;

    /** Start angle in degrees (0 = right, 90 = top) */
    private float startAngle = 135f;

    /** Inner radius ratio (0 = center, 1 = outer edge) */
    private float innerRadiusRatio = 0.7f;

    /** Needle style */
    private NeedleStyle needleStyle = NeedleStyle.ARROW;

    /** Needle color */
    private Color needleColor = new Color(220, 50, 50);

    /** Needle width at base */
    private float needleWidth = 8f;

    /** Needle length ratio relative to gauge radius */
    private float needleLengthRatio = 0.85f;

    /** Center cap radius ratio */
    private float centerCapRatio = 0.08f;

    /** Center cap color */
    private Color centerCapColor = new Color(60, 60, 60);

    /** Background/track color */
    private Color trackColor = new Color(45, 45, 45);

    /** Value arc color (used when no zones) */
    private Color valueColor = new Color(65, 131, 196);

    /** Border color */
    private Color borderColor = new Color(80, 80, 80);

    /** Border width */
    private float borderWidth = 2.0f;

    /** Whether to use zone colors for value arc */
    private boolean useZoneColors = true;

    /** Whether to show the value arc fill */
    private boolean showValueArc = true;

    /** Whether to show tick marks */
    private boolean showTicks = true;

    /** Number of major tick marks */
    private int majorTickCount = 5;

    /** Number of minor ticks between major ticks */
    private int minorTicksPerMajor = 4;

    /** Major tick length ratio (relative to gauge radius) */
    private float majorTickLengthRatio = 0.12f;

    /** Minor tick length ratio */
    private float minorTickLengthRatio = 0.06f;

    /** Major tick width */
    private float majorTickWidth = 2.0f;

    /** Minor tick width */
    private float minorTickWidth = 1.0f;

    /** Tick color */
    private Color tickColor = new Color(150, 150, 150);

    /** Whether to show labels at major ticks */
    private boolean showLabels = true;

    /** Label color */
    private Color labelColor = new Color(180, 180, 180);

    /** Label font size */
    private float labelFontSize = 12f;

    /** Label distance from arc (pixels) */
    private float labelOffset = 15f;

    /** Whether to show current value label */
    private boolean showValueLabel = true;

    /** Value label font size */
    private float valueLabelFontSize = 24f;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Arc tessellation segments per degree */
    private int segmentsPerDegree = 2;

    /** Whether visible */
    private boolean visible = true;

    /**
     * Creates default radial gauge options.
     */
    public RadialGaugeSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public RadialGaugeSeriesOptions(RadialGaugeSeriesOptions other) {
        this.arcSpan = other.arcSpan;
        this.startAngle = other.startAngle;
        this.innerRadiusRatio = other.innerRadiusRatio;
        this.needleStyle = other.needleStyle;
        this.needleColor = other.needleColor;
        this.needleWidth = other.needleWidth;
        this.needleLengthRatio = other.needleLengthRatio;
        this.centerCapRatio = other.centerCapRatio;
        this.centerCapColor = other.centerCapColor;
        this.trackColor = other.trackColor;
        this.valueColor = other.valueColor;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.useZoneColors = other.useZoneColors;
        this.showValueArc = other.showValueArc;
        this.showTicks = other.showTicks;
        this.majorTickCount = other.majorTickCount;
        this.minorTicksPerMajor = other.minorTicksPerMajor;
        this.majorTickLengthRatio = other.majorTickLengthRatio;
        this.minorTickLengthRatio = other.minorTickLengthRatio;
        this.majorTickWidth = other.majorTickWidth;
        this.minorTickWidth = other.minorTickWidth;
        this.tickColor = other.tickColor;
        this.showLabels = other.showLabels;
        this.labelColor = other.labelColor;
        this.labelFontSize = other.labelFontSize;
        this.labelOffset = other.labelOffset;
        this.showValueLabel = other.showValueLabel;
        this.valueLabelFontSize = other.valueLabelFontSize;
        this.opacity = other.opacity;
        this.segmentsPerDegree = other.segmentsPerDegree;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    public float getArcSpan() {
        return arcSpan;
    }

    public float getStartAngle() {
        return startAngle;
    }

    public float getInnerRadiusRatio() {
        return innerRadiusRatio;
    }

    public NeedleStyle getNeedleStyle() {
        return needleStyle;
    }

    public Color getNeedleColor() {
        return needleColor;
    }

    public float getNeedleWidth() {
        return needleWidth;
    }

    public float getNeedleLengthRatio() {
        return needleLengthRatio;
    }

    public float getCenterCapRatio() {
        return centerCapRatio;
    }

    public Color getCenterCapColor() {
        return centerCapColor;
    }

    public Color getTrackColor() {
        return trackColor;
    }

    public Color getValueColor() {
        return valueColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public boolean isUseZoneColors() {
        return useZoneColors;
    }

    public boolean isShowValueArc() {
        return showValueArc;
    }

    public boolean isShowTicks() {
        return showTicks;
    }

    public int getMajorTickCount() {
        return majorTickCount;
    }

    public int getMinorTicksPerMajor() {
        return minorTicksPerMajor;
    }

    public float getMajorTickLengthRatio() {
        return majorTickLengthRatio;
    }

    public float getMinorTickLengthRatio() {
        return minorTickLengthRatio;
    }

    public float getMajorTickWidth() {
        return majorTickWidth;
    }

    public float getMinorTickWidth() {
        return minorTickWidth;
    }

    public Color getTickColor() {
        return tickColor;
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

    public float getLabelOffset() {
        return labelOffset;
    }

    public boolean isShowValueLabel() {
        return showValueLabel;
    }

    public float getValueLabelFontSize() {
        return valueLabelFontSize;
    }

    public float getOpacity() {
        return opacity;
    }

    public int getSegmentsPerDegree() {
        return segmentsPerDegree;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Converts a normalized value (0-1) to an angle in radians.
     */
    public double valueToAngle(float normalized) {
        double startRad = Math.toRadians(startAngle);
        double spanRad = Math.toRadians(arcSpan);
        return startRad + normalized * spanRad;
    }

    /**
     * Returns the end angle in degrees.
     */
    public float getEndAngle() {
        return startAngle + arcSpan;
    }

    // ========== Fluent Setters ==========

    public RadialGaugeSeriesOptions arcSpan(float degrees) {
        this.arcSpan = Math.max(10, Math.min(360, degrees));
        return this;
    }

    public RadialGaugeSeriesOptions startAngle(float degrees) {
        this.startAngle = degrees;
        return this;
    }

    public RadialGaugeSeriesOptions innerRadiusRatio(float ratio) {
        this.innerRadiusRatio = Math.max(0, Math.min(0.95f, ratio));
        return this;
    }

    public RadialGaugeSeriesOptions needleStyle(NeedleStyle style) {
        this.needleStyle = style;
        return this;
    }

    public RadialGaugeSeriesOptions needleColor(Color color) {
        this.needleColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions needleWidth(float width) {
        this.needleWidth = Math.max(1, width);
        return this;
    }

    public RadialGaugeSeriesOptions needleLengthRatio(float ratio) {
        this.needleLengthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        return this;
    }

    public RadialGaugeSeriesOptions centerCapRatio(float ratio) {
        this.centerCapRatio = Math.max(0, Math.min(0.3f, ratio));
        return this;
    }

    public RadialGaugeSeriesOptions centerCapColor(Color color) {
        this.centerCapColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions trackColor(Color color) {
        this.trackColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions valueColor(Color color) {
        this.valueColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0, width);
        return this;
    }

    public RadialGaugeSeriesOptions useZoneColors(boolean use) {
        this.useZoneColors = use;
        return this;
    }

    public RadialGaugeSeriesOptions showValueArc(boolean show) {
        this.showValueArc = show;
        return this;
    }

    public RadialGaugeSeriesOptions showTicks(boolean show) {
        this.showTicks = show;
        return this;
    }

    public RadialGaugeSeriesOptions majorTickCount(int count) {
        this.majorTickCount = Math.max(2, count);
        return this;
    }

    public RadialGaugeSeriesOptions minorTicksPerMajor(int count) {
        this.minorTicksPerMajor = Math.max(0, count);
        return this;
    }

    public RadialGaugeSeriesOptions majorTickLengthRatio(float ratio) {
        this.majorTickLengthRatio = Math.max(0.01f, Math.min(0.3f, ratio));
        return this;
    }

    public RadialGaugeSeriesOptions minorTickLengthRatio(float ratio) {
        this.minorTickLengthRatio = Math.max(0.01f, Math.min(0.2f, ratio));
        return this;
    }

    public RadialGaugeSeriesOptions tickColor(Color color) {
        this.tickColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions showLabels(boolean show) {
        this.showLabels = show;
        return this;
    }

    public RadialGaugeSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public RadialGaugeSeriesOptions showValueLabel(boolean show) {
        this.showValueLabel = show;
        return this;
    }

    public RadialGaugeSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public RadialGaugeSeriesOptions segmentsPerDegree(int segments) {
        this.segmentsPerDegree = Math.max(1, Math.min(10, segments));
        return this;
    }

    public RadialGaugeSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Configures for a half-circle gauge (180 degrees).
     */
    public RadialGaugeSeriesOptions halfCircle() {
        this.arcSpan = 180f;
        this.startAngle = 180f;
        return this;
    }

    /**
     * Configures for a three-quarter gauge (270 degrees).
     */
    public RadialGaugeSeriesOptions threeQuarter() {
        this.arcSpan = 270f;
        this.startAngle = 135f;
        return this;
    }

    /**
     * Configures for a full circle gauge (360 degrees).
     */
    public RadialGaugeSeriesOptions fullCircle() {
        this.arcSpan = 360f;
        this.startAngle = -90f;
        return this;
    }
}
