package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for linear gauge series.
 *
 * <p>Controls appearance of horizontal or vertical gauge bars
 * with optional tick marks, labels, and color zones.
 */
public class LinearGaugeSeriesOptions {

    /** Gauge orientation */
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    /** How the value indicator is displayed */
    public enum IndicatorStyle {
        FILL,       // Fill from start to value
        MARKER,     // Single marker at value position
        BOTH        // Fill with marker overlay
    }

    /** Gauge orientation */
    private Orientation orientation = Orientation.HORIZONTAL;

    /** Indicator style */
    private IndicatorStyle indicatorStyle = IndicatorStyle.FILL;

    /** Background/track color */
    private Color trackColor = new Color(60, 60, 60);

    /** Value fill color (used when no zones) */
    private Color fillColor = new Color(65, 131, 196);

    /** Marker/indicator color */
    private Color markerColor = new Color(255, 255, 255);

    /** Border color */
    private Color borderColor = new Color(40, 40, 40);

    /** Track height/width ratio (relative to available space) */
    private float trackThickness = 0.3f;

    /** Border width */
    private float borderWidth = 1.0f;

    /** Marker width in pixels */
    private float markerWidth = 4.0f;

    /** Whether to use zone colors for fill */
    private boolean useZoneColors = true;

    /** Whether to show tick marks */
    private boolean showTicks = true;

    /** Number of major tick marks */
    private int majorTickCount = 5;

    /** Number of minor ticks between major ticks */
    private int minorTicksPerMajor = 4;

    /** Major tick length in pixels */
    private float majorTickLength = 10f;

    /** Minor tick length in pixels */
    private float minorTickLength = 5f;

    /** Tick color */
    private Color tickColor = new Color(150, 150, 150);

    /** Whether to show labels at major ticks */
    private boolean showLabels = true;

    /** Label color */
    private Color labelColor = new Color(180, 180, 180);

    /** Label font size */
    private float labelFontSize = 10f;

    /** Whether to show current value label */
    private boolean showValueLabel = true;

    /** Value label font size */
    private float valueLabelFontSize = 14f;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Padding around the gauge */
    private float padding = 10f;

    /** Whether visible */
    private boolean visible = true;

    /**
     * Creates default linear gauge options.
     */
    public LinearGaugeSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public LinearGaugeSeriesOptions(LinearGaugeSeriesOptions other) {
        this.orientation = other.orientation;
        this.indicatorStyle = other.indicatorStyle;
        this.trackColor = other.trackColor;
        this.fillColor = other.fillColor;
        this.markerColor = other.markerColor;
        this.borderColor = other.borderColor;
        this.trackThickness = other.trackThickness;
        this.borderWidth = other.borderWidth;
        this.markerWidth = other.markerWidth;
        this.useZoneColors = other.useZoneColors;
        this.showTicks = other.showTicks;
        this.majorTickCount = other.majorTickCount;
        this.minorTicksPerMajor = other.minorTicksPerMajor;
        this.majorTickLength = other.majorTickLength;
        this.minorTickLength = other.minorTickLength;
        this.tickColor = other.tickColor;
        this.showLabels = other.showLabels;
        this.labelColor = other.labelColor;
        this.labelFontSize = other.labelFontSize;
        this.showValueLabel = other.showValueLabel;
        this.valueLabelFontSize = other.valueLabelFontSize;
        this.opacity = other.opacity;
        this.padding = other.padding;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    public Orientation getOrientation() {
        return orientation;
    }

    public IndicatorStyle getIndicatorStyle() {
        return indicatorStyle;
    }

    public Color getTrackColor() {
        return trackColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getMarkerColor() {
        return markerColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getTrackThickness() {
        return trackThickness;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getMarkerWidth() {
        return markerWidth;
    }

    public boolean isUseZoneColors() {
        return useZoneColors;
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

    public float getMajorTickLength() {
        return majorTickLength;
    }

    public float getMinorTickLength() {
        return minorTickLength;
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

    public boolean isShowValueLabel() {
        return showValueLabel;
    }

    public float getValueLabelFontSize() {
        return valueLabelFontSize;
    }

    public float getOpacity() {
        return opacity;
    }

    public float getPadding() {
        return padding;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isHorizontal() {
        return orientation == Orientation.HORIZONTAL;
    }

    // ========== Fluent Setters ==========

    public LinearGaugeSeriesOptions orientation(Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public LinearGaugeSeriesOptions horizontal() {
        this.orientation = Orientation.HORIZONTAL;
        return this;
    }

    public LinearGaugeSeriesOptions vertical() {
        this.orientation = Orientation.VERTICAL;
        return this;
    }

    public LinearGaugeSeriesOptions indicatorStyle(IndicatorStyle style) {
        this.indicatorStyle = style;
        return this;
    }

    public LinearGaugeSeriesOptions trackColor(Color color) {
        this.trackColor = color;
        return this;
    }

    public LinearGaugeSeriesOptions fillColor(Color color) {
        this.fillColor = color;
        return this;
    }

    public LinearGaugeSeriesOptions markerColor(Color color) {
        this.markerColor = color;
        return this;
    }

    public LinearGaugeSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public LinearGaugeSeriesOptions trackThickness(float thickness) {
        this.trackThickness = Math.max(0.1f, Math.min(1.0f, thickness));
        return this;
    }

    public LinearGaugeSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0, width);
        return this;
    }

    public LinearGaugeSeriesOptions markerWidth(float width) {
        this.markerWidth = Math.max(1, width);
        return this;
    }

    public LinearGaugeSeriesOptions useZoneColors(boolean use) {
        this.useZoneColors = use;
        return this;
    }

    public LinearGaugeSeriesOptions showTicks(boolean show) {
        this.showTicks = show;
        return this;
    }

    public LinearGaugeSeriesOptions majorTickCount(int count) {
        this.majorTickCount = Math.max(2, count);
        return this;
    }

    public LinearGaugeSeriesOptions minorTicksPerMajor(int count) {
        this.minorTicksPerMajor = Math.max(0, count);
        return this;
    }

    public LinearGaugeSeriesOptions tickColor(Color color) {
        this.tickColor = color;
        return this;
    }

    public LinearGaugeSeriesOptions showLabels(boolean show) {
        this.showLabels = show;
        return this;
    }

    public LinearGaugeSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public LinearGaugeSeriesOptions showValueLabel(boolean show) {
        this.showValueLabel = show;
        return this;
    }

    public LinearGaugeSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public LinearGaugeSeriesOptions padding(float padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    public LinearGaugeSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
