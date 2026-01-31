package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for line series.
 *
 * <p>Supports various display modes including standard lines, filled areas,
 * and step functions. Uses a fluent builder pattern for easy configuration.
 */
public class LineSeriesOptions extends SeriesOptions {

    /**
     * Display mode for rendering the line series.
     */
    public enum DisplayMode {
        /** Standard connected line (default) */
        LINE,
        /** Filled area from values to baseline */
        AREA,
        /** Step with vertical-then-horizontal transitions */
        STEP_BEFORE,
        /** Step with horizontal-then-vertical transitions */
        STEP_AFTER,
        /** Step meeting at midpoint between values */
        STEP_MIDDLE
    }

    /** Line color */
    private Color color = new Color(65, 131, 196); // Default blue

    /** Line width in pixels */
    private float lineWidth = 1.5f;

    /** Display mode */
    private DisplayMode displayMode = DisplayMode.LINE;

    /** Fill color for area mode */
    private Color fillColor = new Color(65, 131, 196, 77); // 30% alpha

    /** Baseline value for area mode */
    private double baseline = 0.0;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 1.0f;

    /** Whether to show the line on top of filled area */
    private boolean showLine = true;

    /**
     * Creates default line series options.
     */
    public LineSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public LineSeriesOptions(LineSeriesOptions other) {
        super(other);
        this.color = other.color;
        this.lineWidth = other.lineWidth;
        this.displayMode = other.displayMode;
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

    public DisplayMode getDisplayMode() {
        return displayMode;
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
    public LineSeriesOptions color(Color color) {
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
    public LineSeriesOptions color(int r, int g, int b) {
        this.color = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the line width in pixels.
     *
     * @param lineWidth the width
     * @return this for chaining
     */
    public LineSeriesOptions lineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.5f, lineWidth);
        return this;
    }

    /**
     * Sets the display mode.
     *
     * @param displayMode the mode
     * @return this for chaining
     */
    public LineSeriesOptions displayMode(DisplayMode displayMode) {
        this.displayMode = displayMode != null ? displayMode : DisplayMode.LINE;
        return this;
    }

    /**
     * Sets the fill color for area mode.
     *
     * @param fillColor the fill color
     * @return this for chaining
     */
    public LineSeriesOptions fillColor(Color fillColor) {
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
    public LineSeriesOptions fillColor(int r, int g, int b, int a) {
        this.fillColor = new Color(r, g, b, a);
        return this;
    }

    /**
     * Sets the baseline value for area mode.
     *
     * @param baseline the baseline value
     * @return this for chaining
     */
    public LineSeriesOptions baseline(double baseline) {
        this.baseline = baseline;
        return this;
    }

    /**
     * Sets the overall opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public LineSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    /**
     * Sets whether to show the line on top of filled area.
     *
     * @param showLine true to show line
     * @return this for chaining
     */
    public LineSeriesOptions showLine(boolean showLine) {
        this.showLine = showLine;
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public LineSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public LineSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public LineSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public LineSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
