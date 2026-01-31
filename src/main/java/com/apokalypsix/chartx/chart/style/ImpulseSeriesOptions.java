package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for impulse (stem) series.
 *
 * <p>Impulse series draws vertical lines from baseline to data values,
 * optionally with markers at the top of each stem.
 */
public class ImpulseSeriesOptions extends SeriesOptions {

    /**
     * Marker shape for the top of stems.
     */
    public enum MarkerShape {
        NONE,
        CIRCLE,
        SQUARE,
        DIAMOND
    }

    /** Stem line color */
    private Color color = new Color(65, 131, 196);

    /** Stem line width */
    private float lineWidth = 1.5f;

    /** Marker shape at top of stems */
    private MarkerShape markerShape = MarkerShape.CIRCLE;

    /** Marker size (diameter) */
    private float markerSize = 6.0f;

    /** Marker color (null = use stem color) */
    private Color markerColor = null;

    /** Baseline value */
    private double baseline = 0.0;

    /** Overall opacity */
    private float opacity = 1.0f;

    /**
     * Creates default impulse series options.
     */
    public ImpulseSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public ImpulseSeriesOptions(ImpulseSeriesOptions other) {
        super(other);
        this.color = other.color;
        this.lineWidth = other.lineWidth;
        this.markerShape = other.markerShape;
        this.markerSize = other.markerSize;
        this.markerColor = other.markerColor;
        this.baseline = other.baseline;
        this.opacity = other.opacity;
    }

    // ========== Getters ==========

    public Color getColor() {
        return color;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public MarkerShape getMarkerShape() {
        return markerShape;
    }

    public float getMarkerSize() {
        return markerSize;
    }

    public Color getMarkerColor() {
        return markerColor != null ? markerColor : color;
    }

    public double getBaseline() {
        return baseline;
    }

    public float getOpacity() {
        return opacity;
    }

    // ========== Fluent setters ==========

    public ImpulseSeriesOptions color(Color color) {
        this.color = color;
        return this;
    }

    public ImpulseSeriesOptions color(int r, int g, int b) {
        this.color = new Color(r, g, b);
        return this;
    }

    public ImpulseSeriesOptions lineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.5f, lineWidth);
        return this;
    }

    public ImpulseSeriesOptions markerShape(MarkerShape shape) {
        this.markerShape = shape != null ? shape : MarkerShape.NONE;
        return this;
    }

    public ImpulseSeriesOptions markerSize(float size) {
        this.markerSize = Math.max(1.0f, size);
        return this;
    }

    public ImpulseSeriesOptions markerColor(Color color) {
        this.markerColor = color;
        return this;
    }

    public ImpulseSeriesOptions baseline(double baseline) {
        this.baseline = baseline;
        return this;
    }

    public ImpulseSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public ImpulseSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public ImpulseSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public ImpulseSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public ImpulseSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
