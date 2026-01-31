package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for error bar series.
 *
 * <p>Error bars display uncertainty or variation in data values,
 * showing upper and lower bounds around a central value.
 */
public class ErrorBarSeriesOptions extends SeriesOptions {

    /** Error bar line color */
    private Color color = new Color(40, 40, 40);

    /** Error bar line width */
    private float lineWidth = 1.5f;

    /** Cap width in pixels */
    private float capWidth = 8.0f;

    /** Whether to show the center value marker */
    private boolean showCenterMarker = true;

    /** Center marker size in pixels */
    private float centerMarkerSize = 4.0f;

    /** Center marker color (null = same as line color) */
    private Color centerMarkerColor = null;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 1.0f;

    /** Whether error bars are horizontal (false = vertical) */
    private boolean horizontal = false;

    /**
     * Creates default error bar options.
     */
    public ErrorBarSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public ErrorBarSeriesOptions(ErrorBarSeriesOptions other) {
        super(other);
        this.color = other.color;
        this.lineWidth = other.lineWidth;
        this.capWidth = other.capWidth;
        this.showCenterMarker = other.showCenterMarker;
        this.centerMarkerSize = other.centerMarkerSize;
        this.centerMarkerColor = other.centerMarkerColor;
        this.opacity = other.opacity;
        this.horizontal = other.horizontal;
    }

    // ========== Getters ==========

    public Color getColor() {
        return color;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public float getCapWidth() {
        return capWidth;
    }

    public boolean isShowCenterMarker() {
        return showCenterMarker;
    }

    public float getCenterMarkerSize() {
        return centerMarkerSize;
    }

    public Color getCenterMarkerColor() {
        return centerMarkerColor != null ? centerMarkerColor : color;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    // ========== Fluent setters ==========

    public ErrorBarSeriesOptions color(Color color) {
        this.color = color;
        return this;
    }

    public ErrorBarSeriesOptions color(int r, int g, int b) {
        this.color = new Color(r, g, b);
        return this;
    }

    public ErrorBarSeriesOptions lineWidth(float width) {
        this.lineWidth = Math.max(0.5f, width);
        return this;
    }

    public ErrorBarSeriesOptions capWidth(float width) {
        this.capWidth = Math.max(0.0f, width);
        return this;
    }

    public ErrorBarSeriesOptions showCenterMarker(boolean show) {
        this.showCenterMarker = show;
        return this;
    }

    public ErrorBarSeriesOptions centerMarkerSize(float size) {
        this.centerMarkerSize = Math.max(1.0f, size);
        return this;
    }

    public ErrorBarSeriesOptions centerMarkerColor(Color color) {
        this.centerMarkerColor = color;
        return this;
    }

    public ErrorBarSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    public ErrorBarSeriesOptions horizontal(boolean horizontal) {
        this.horizontal = horizontal;
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public ErrorBarSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public ErrorBarSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public ErrorBarSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public ErrorBarSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
