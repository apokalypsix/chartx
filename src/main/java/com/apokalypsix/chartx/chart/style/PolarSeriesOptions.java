package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for polar line and mountain series.
 *
 * <p>Controls appearance of polar lines and filled areas.
 */
public class PolarSeriesOptions {

    /** Line color */
    private Color lineColor = new Color(65, 131, 196);

    /** Fill color (for mountain series) */
    private Color fillColor = new Color(65, 131, 196, 100);

    /** Line width */
    private float lineWidth = 2.0f;

    /** Whether to show the line */
    private boolean showLine = true;

    /** Whether to show the fill */
    private boolean showFill = false;

    /** Whether to close the polygon (connect last point to first) */
    private boolean closePath = true;

    /** Whether to use spline interpolation */
    private boolean useSpline = false;

    /** Spline tension (0.0 = loose, 1.0 = tight) */
    private float splineTension = 0.5f;

    /** Segments per curve section for splines */
    private int splineSegments = 8;

    /** Whether to show data point markers */
    private boolean showMarkers = true;

    /** Marker size in pixels */
    private float markerSize = 4.0f;

    /** Marker color (null = same as line) */
    private Color markerColor = null;

    /** Padding ratio around the chart */
    private float paddingRatio = 0.1f;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether chart is visible */
    private boolean visible = true;

    /**
     * Creates default polar series options.
     */
    public PolarSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public PolarSeriesOptions(PolarSeriesOptions other) {
        this.lineColor = other.lineColor;
        this.fillColor = other.fillColor;
        this.lineWidth = other.lineWidth;
        this.showLine = other.showLine;
        this.showFill = other.showFill;
        this.closePath = other.closePath;
        this.useSpline = other.useSpline;
        this.splineTension = other.splineTension;
        this.splineSegments = other.splineSegments;
        this.showMarkers = other.showMarkers;
        this.markerSize = other.markerSize;
        this.markerColor = other.markerColor;
        this.paddingRatio = other.paddingRatio;
        this.opacity = other.opacity;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    public Color getLineColor() {
        return lineColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public boolean isShowLine() {
        return showLine;
    }

    public boolean isShowFill() {
        return showFill;
    }

    public boolean isClosePath() {
        return closePath;
    }

    public boolean isUseSpline() {
        return useSpline;
    }

    public float getSplineTension() {
        return splineTension;
    }

    public int getSplineSegments() {
        return splineSegments;
    }

    public boolean isShowMarkers() {
        return showMarkers;
    }

    public float getMarkerSize() {
        return markerSize;
    }

    public Color getMarkerColor() {
        return markerColor != null ? markerColor : lineColor;
    }

    public float getPaddingRatio() {
        return paddingRatio;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    // ========== Fluent Setters ==========

    public PolarSeriesOptions lineColor(Color color) {
        this.lineColor = color;
        return this;
    }

    public PolarSeriesOptions lineColor(int r, int g, int b) {
        this.lineColor = new Color(r, g, b);
        return this;
    }

    public PolarSeriesOptions fillColor(Color color) {
        this.fillColor = color;
        return this;
    }

    public PolarSeriesOptions fillColor(int r, int g, int b, int a) {
        this.fillColor = new Color(r, g, b, a);
        return this;
    }

    public PolarSeriesOptions lineWidth(float width) {
        this.lineWidth = Math.max(0.5f, width);
        return this;
    }

    public PolarSeriesOptions showLine(boolean show) {
        this.showLine = show;
        return this;
    }

    public PolarSeriesOptions showFill(boolean show) {
        this.showFill = show;
        return this;
    }

    public PolarSeriesOptions closePath(boolean close) {
        this.closePath = close;
        return this;
    }

    public PolarSeriesOptions useSpline(boolean use) {
        this.useSpline = use;
        return this;
    }

    public PolarSeriesOptions splineTension(float tension) {
        this.splineTension = Math.max(0, Math.min(1, tension));
        return this;
    }

    public PolarSeriesOptions splineSegments(int segments) {
        this.splineSegments = Math.max(2, Math.min(16, segments));
        return this;
    }

    public PolarSeriesOptions showMarkers(boolean show) {
        this.showMarkers = show;
        return this;
    }

    public PolarSeriesOptions markerSize(float size) {
        this.markerSize = Math.max(1, size);
        return this;
    }

    public PolarSeriesOptions markerColor(Color color) {
        this.markerColor = color;
        return this;
    }

    public PolarSeriesOptions paddingRatio(float ratio) {
        this.paddingRatio = Math.max(0, Math.min(0.4f, ratio));
        return this;
    }

    public PolarSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public PolarSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Convenience: configure as a mountain (area) chart.
     */
    public PolarSeriesOptions asMountain() {
        this.showFill = true;
        return this;
    }

    /**
     * Convenience: configure as a line-only chart.
     */
    public PolarSeriesOptions asLine() {
        this.showFill = false;
        this.showLine = true;
        return this;
    }
}
