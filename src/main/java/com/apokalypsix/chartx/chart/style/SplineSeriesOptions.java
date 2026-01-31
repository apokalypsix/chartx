package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for spline line and mountain series.
 *
 * <p>Extends line series styling with spline-specific parameters like
 * tension and segments per curve. Uses a fluent builder pattern for
 * easy configuration.
 */
public class SplineSeriesOptions extends SeriesOptions {

    /** Line color */
    private Color color = new Color(65, 131, 196); // Default blue

    /** Line width in pixels */
    private float lineWidth = 1.5f;

    /** Spline tension (0.0 = tight corners, 0.5 = standard, 1.0 = loose) */
    private float tension = 0.5f;

    /** Number of tessellation segments per curve segment */
    private int segmentsPerCurve = 8;

    /** Fill color for mountain/area mode (null = no fill) */
    private Color fillColor = null;

    /** Baseline value for filled areas */
    private double baseline = 0.0;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 1.0f;

    /** Whether to show the line (for mountain mode) */
    private boolean showLine = true;

    /**
     * Creates default spline series options.
     */
    public SplineSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public SplineSeriesOptions(SplineSeriesOptions other) {
        super(other);
        this.color = other.color;
        this.lineWidth = other.lineWidth;
        this.tension = other.tension;
        this.segmentsPerCurve = other.segmentsPerCurve;
        this.fillColor = other.fillColor;
        this.baseline = other.baseline;
        this.opacity = other.opacity;
        this.showLine = other.showLine;
    }

    // ========== Getters ==========

    public Color getColor() {
        return color;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public float getTension() {
        return tension;
    }

    public int getSegmentsPerCurve() {
        return segmentsPerCurve;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public double getBaseline() {
        return baseline;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isShowLine() {
        return showLine;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the line color.
     *
     * @param color the color
     * @return this for chaining
     */
    public SplineSeriesOptions color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the line color using RGB values.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public SplineSeriesOptions color(int r, int g, int b) {
        this.color = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the line width in pixels.
     *
     * @param lineWidth the width
     * @return this for chaining
     */
    public SplineSeriesOptions lineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.5f, lineWidth);
        return this;
    }

    /**
     * Sets the spline tension.
     *
     * <p>Tension controls how tightly the curve passes through points:
     * <ul>
     *   <li>0.0 = very tight, sharper corners</li>
     *   <li>0.5 = standard Catmull-Rom (default)</li>
     *   <li>1.0 = very loose, smoother curve</li>
     * </ul>
     *
     * @param tension the tension value (clamped to 0.0 - 1.0)
     * @return this for chaining
     */
    public SplineSeriesOptions tension(float tension) {
        this.tension = Math.max(0.0f, Math.min(1.0f, tension));
        return this;
    }

    /**
     * Sets the number of tessellation segments per curve segment.
     *
     * <p>Higher values produce smoother curves but use more vertices.
     * Typical values are 4-16.
     *
     * @param segments number of segments (clamped to 2-32)
     * @return this for chaining
     */
    public SplineSeriesOptions segmentsPerCurve(int segments) {
        this.segmentsPerCurve = Math.max(2, Math.min(32, segments));
        return this;
    }

    /**
     * Sets the fill color for mountain/area mode.
     *
     * @param fillColor the fill color (null = no fill)
     * @return this for chaining
     */
    public SplineSeriesOptions fillColor(Color fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets the fill color using RGBA values.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @param a alpha (0-255)
     * @return this for chaining
     */
    public SplineSeriesOptions fillColor(int r, int g, int b, int a) {
        this.fillColor = new Color(r, g, b, a);
        return this;
    }

    /**
     * Sets the baseline value for filled areas.
     *
     * @param baseline the baseline value
     * @return this for chaining
     */
    public SplineSeriesOptions baseline(double baseline) {
        this.baseline = baseline;
        return this;
    }

    /**
     * Sets the overall opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public SplineSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    /**
     * Sets whether to show the line on top of filled area.
     *
     * @param showLine true to show line
     * @return this for chaining
     */
    public SplineSeriesOptions showLine(boolean showLine) {
        this.showLine = showLine;
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public SplineSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public SplineSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public SplineSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public SplineSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
