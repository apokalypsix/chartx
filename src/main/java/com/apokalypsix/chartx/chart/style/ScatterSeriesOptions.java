package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for scatter series.
 *
 * <p>Supports various marker shapes, sizes, and colors.
 * Uses a fluent builder pattern.
 */
public class ScatterSeriesOptions extends SeriesOptions {

    /**
     * Shape of the scatter markers.
     */
    public enum MarkerShape {
        /** Filled circle */
        CIRCLE,
        /** Filled square */
        SQUARE,
        /** Filled diamond (rotated square) */
        DIAMOND,
        /** Filled triangle pointing up */
        TRIANGLE_UP,
        /** Filled triangle pointing down */
        TRIANGLE_DOWN,
        /** Plus sign (+) */
        PLUS,
        /** Cross sign (X) */
        CROSS
    }

    /** Marker color */
    private Color color = new Color(65, 131, 196); // Blue

    /** Marker size in pixels (diameter for circles) */
    private float markerSize = 8.0f;

    /** Marker shape */
    private MarkerShape markerShape = MarkerShape.CIRCLE;

    /** Border color (null for no border) */
    private Color borderColor = null;

    /** Border width (only applicable if borderColor is set) */
    private float borderWidth = 1.0f;

    /** Opacity (0.0 - 1.0) */
    private float opacity = 1.0f;

    /**
     * Creates default scatter series options.
     */
    public ScatterSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public ScatterSeriesOptions(ScatterSeriesOptions other) {
        super(other);
        this.color = other.color;
        this.markerSize = other.markerSize;
        this.markerShape = other.markerShape;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.opacity = other.opacity;
    }

    // ========== Getters ==========

    public Color getColor() {
        return color;
    }

    public float getMarkerSize() {
        return markerSize;
    }

    public MarkerShape getMarkerShape() {
        return markerShape;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getOpacity() {
        return opacity;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the marker color.
     *
     * @param color the color
     * @return this for chaining
     */
    public ScatterSeriesOptions color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the marker color using RGB values.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public ScatterSeriesOptions color(int r, int g, int b) {
        this.color = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the marker size in pixels.
     *
     * @param markerSize the size
     * @return this for chaining
     */
    public ScatterSeriesOptions markerSize(float markerSize) {
        this.markerSize = Math.max(1.0f, markerSize);
        return this;
    }

    /**
     * Sets the marker shape.
     *
     * @param markerShape the shape
     * @return this for chaining
     */
    public ScatterSeriesOptions markerShape(MarkerShape markerShape) {
        this.markerShape = markerShape != null ? markerShape : MarkerShape.CIRCLE;
        return this;
    }

    /**
     * Sets the border color. Pass null for no border.
     *
     * @param borderColor the border color
     * @return this for chaining
     */
    public ScatterSeriesOptions borderColor(Color borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    /**
     * Sets the border width in pixels.
     *
     * @param borderWidth the width
     * @return this for chaining
     */
    public ScatterSeriesOptions borderWidth(float borderWidth) {
        this.borderWidth = Math.max(0.5f, borderWidth);
        return this;
    }

    /**
     * Sets the opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public ScatterSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public ScatterSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public ScatterSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public ScatterSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public ScatterSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
