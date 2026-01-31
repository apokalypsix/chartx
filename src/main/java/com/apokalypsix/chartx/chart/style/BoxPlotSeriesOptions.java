package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for box plot series.
 *
 * <p>Box plots display statistical quartile data with a box (Q1-Q3),
 * median line, and whiskers (min-max).
 */
public class BoxPlotSeriesOptions extends SeriesOptions {

    /** Box fill color */
    private Color boxColor = new Color(65, 131, 196, 180);

    /** Box border and whisker color */
    private Color borderColor = new Color(40, 40, 40);

    /** Median line color */
    private Color medianColor = new Color(220, 100, 50);

    /** Border/whisker line width */
    private float lineWidth = 1.5f;

    /** Median line width */
    private float medianLineWidth = 2.0f;

    /** Box width ratio (0.0 - 1.0 of available space) */
    private float boxWidthRatio = 0.6f;

    /** Whisker cap width ratio relative to box width */
    private float whiskerCapRatio = 0.5f;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether to show notches at median */
    private boolean showNotches = false;

    /** Whether to show mean marker */
    private boolean showMean = false;

    /** Mean marker color */
    private Color meanColor = new Color(0, 150, 0);

    /**
     * Creates default box plot options.
     */
    public BoxPlotSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public BoxPlotSeriesOptions(BoxPlotSeriesOptions other) {
        super(other);
        this.boxColor = other.boxColor;
        this.borderColor = other.borderColor;
        this.medianColor = other.medianColor;
        this.lineWidth = other.lineWidth;
        this.medianLineWidth = other.medianLineWidth;
        this.boxWidthRatio = other.boxWidthRatio;
        this.whiskerCapRatio = other.whiskerCapRatio;
        this.opacity = other.opacity;
        this.showNotches = other.showNotches;
        this.showMean = other.showMean;
        this.meanColor = other.meanColor;
    }

    // ========== Getters ==========

    public Color getBoxColor() {
        return boxColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public Color getMedianColor() {
        return medianColor;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public float getMedianLineWidth() {
        return medianLineWidth;
    }

    public float getBoxWidthRatio() {
        return boxWidthRatio;
    }

    public float getWhiskerCapRatio() {
        return whiskerCapRatio;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isShowNotches() {
        return showNotches;
    }

    public boolean isShowMean() {
        return showMean;
    }

    public Color getMeanColor() {
        return meanColor;
    }

    // ========== Fluent setters ==========

    public BoxPlotSeriesOptions boxColor(Color color) {
        this.boxColor = color;
        return this;
    }

    public BoxPlotSeriesOptions boxColor(int r, int g, int b, int a) {
        this.boxColor = new Color(r, g, b, a);
        return this;
    }

    public BoxPlotSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public BoxPlotSeriesOptions medianColor(Color color) {
        this.medianColor = color;
        return this;
    }

    public BoxPlotSeriesOptions lineWidth(float width) {
        this.lineWidth = Math.max(0.5f, width);
        return this;
    }

    public BoxPlotSeriesOptions medianLineWidth(float width) {
        this.medianLineWidth = Math.max(0.5f, width);
        return this;
    }

    public BoxPlotSeriesOptions boxWidthRatio(float ratio) {
        this.boxWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        return this;
    }

    public BoxPlotSeriesOptions whiskerCapRatio(float ratio) {
        this.whiskerCapRatio = Math.max(0.0f, Math.min(1.0f, ratio));
        return this;
    }

    public BoxPlotSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    public BoxPlotSeriesOptions showNotches(boolean show) {
        this.showNotches = show;
        return this;
    }

    public BoxPlotSeriesOptions showMean(boolean show) {
        this.showMean = show;
        return this;
    }

    public BoxPlotSeriesOptions meanColor(Color color) {
        this.meanColor = color;
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public BoxPlotSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public BoxPlotSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public BoxPlotSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public BoxPlotSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
