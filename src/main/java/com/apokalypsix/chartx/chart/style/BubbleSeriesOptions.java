package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for bubble series.
 *
 * <p>Bubble series renders circles at data points with sizes proportional
 * to a third dimension in the data.
 */
public class BubbleSeriesOptions extends SeriesOptions {

    /** Bubble fill color */
    private Color color = new Color(65, 131, 196, 180);

    /** Bubble border color (null = no border) */
    private Color borderColor = new Color(40, 80, 140);

    /** Border width */
    private float borderWidth = 1.0f;

    /** Minimum bubble radius in pixels */
    private float minRadius = 4.0f;

    /** Maximum bubble radius in pixels */
    private float maxRadius = 40.0f;

    /** Whether to scale sizes by area (true) or radius (false) */
    private boolean scaleByArea = true;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 0.7f;

    /**
     * Creates default bubble series options.
     */
    public BubbleSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public BubbleSeriesOptions(BubbleSeriesOptions other) {
        super(other);
        this.color = other.color;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.minRadius = other.minRadius;
        this.maxRadius = other.maxRadius;
        this.scaleByArea = other.scaleByArea;
        this.opacity = other.opacity;
    }

    // ========== Getters ==========

    public Color getColor() {
        return color;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getMinRadius() {
        return minRadius;
    }

    public float getMaxRadius() {
        return maxRadius;
    }

    public boolean isScaleByArea() {
        return scaleByArea;
    }

    public float getOpacity() {
        return opacity;
    }

    // ========== Fluent setters ==========

    public BubbleSeriesOptions color(Color color) {
        this.color = color;
        return this;
    }

    public BubbleSeriesOptions color(int r, int g, int b, int a) {
        this.color = new Color(r, g, b, a);
        return this;
    }

    public BubbleSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public BubbleSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0.0f, width);
        return this;
    }

    public BubbleSeriesOptions minRadius(float radius) {
        this.minRadius = Math.max(1.0f, radius);
        return this;
    }

    public BubbleSeriesOptions maxRadius(float radius) {
        this.maxRadius = Math.max(this.minRadius, radius);
        return this;
    }

    public BubbleSeriesOptions scaleByArea(boolean scaleByArea) {
        this.scaleByArea = scaleByArea;
        return this;
    }

    public BubbleSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public BubbleSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public BubbleSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public BubbleSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public BubbleSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
