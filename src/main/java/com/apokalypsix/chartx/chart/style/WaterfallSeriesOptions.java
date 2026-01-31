package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for waterfall chart series.
 *
 * <p>Waterfall charts display running totals with color-coded bars
 * showing increases, decreases, and totals.
 */
public class WaterfallSeriesOptions extends SeriesOptions {

    /** Color for positive (increase) bars */
    private Color positiveColor = new Color(34, 139, 34);

    /** Color for negative (decrease) bars */
    private Color negativeColor = new Color(220, 20, 60);

    /** Color for total bars */
    private Color totalColor = new Color(65, 105, 225);

    /** Connector line color (line connecting bar tops) */
    private Color connectorColor = new Color(100, 100, 100);

    /** Border color (null = no border) */
    private Color borderColor = new Color(40, 40, 40);

    /** Border width */
    private float borderWidth = 1.0f;

    /** Bar width ratio (0.0 - 1.0 of available space) */
    private float barWidthRatio = 0.7f;

    /** Whether to show connector lines between bars */
    private boolean showConnectors = true;

    /** Connector line width */
    private float connectorWidth = 1.0f;

    /** Whether connector lines are dashed */
    private boolean connectorDashed = true;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 1.0f;

    /**
     * Creates default waterfall series options.
     */
    public WaterfallSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public WaterfallSeriesOptions(WaterfallSeriesOptions other) {
        super(other);
        this.positiveColor = other.positiveColor;
        this.negativeColor = other.negativeColor;
        this.totalColor = other.totalColor;
        this.connectorColor = other.connectorColor;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.barWidthRatio = other.barWidthRatio;
        this.showConnectors = other.showConnectors;
        this.connectorWidth = other.connectorWidth;
        this.connectorDashed = other.connectorDashed;
        this.opacity = other.opacity;
    }

    // ========== Getters ==========

    public Color getPositiveColor() {
        return positiveColor;
    }

    public Color getNegativeColor() {
        return negativeColor;
    }

    public Color getTotalColor() {
        return totalColor;
    }

    public Color getConnectorColor() {
        return connectorColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getBarWidthRatio() {
        return barWidthRatio;
    }

    public boolean isShowConnectors() {
        return showConnectors;
    }

    public float getConnectorWidth() {
        return connectorWidth;
    }

    public boolean isConnectorDashed() {
        return connectorDashed;
    }

    public float getOpacity() {
        return opacity;
    }

    // ========== Fluent setters ==========

    public WaterfallSeriesOptions positiveColor(Color color) {
        this.positiveColor = color;
        return this;
    }

    public WaterfallSeriesOptions positiveColor(int r, int g, int b) {
        this.positiveColor = new Color(r, g, b);
        return this;
    }

    public WaterfallSeriesOptions negativeColor(Color color) {
        this.negativeColor = color;
        return this;
    }

    public WaterfallSeriesOptions negativeColor(int r, int g, int b) {
        this.negativeColor = new Color(r, g, b);
        return this;
    }

    public WaterfallSeriesOptions totalColor(Color color) {
        this.totalColor = color;
        return this;
    }

    public WaterfallSeriesOptions totalColor(int r, int g, int b) {
        this.totalColor = new Color(r, g, b);
        return this;
    }

    public WaterfallSeriesOptions connectorColor(Color color) {
        this.connectorColor = color;
        return this;
    }

    public WaterfallSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public WaterfallSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0.0f, width);
        return this;
    }

    public WaterfallSeriesOptions barWidthRatio(float ratio) {
        this.barWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        return this;
    }

    public WaterfallSeriesOptions showConnectors(boolean show) {
        this.showConnectors = show;
        return this;
    }

    public WaterfallSeriesOptions connectorWidth(float width) {
        this.connectorWidth = Math.max(0.5f, width);
        return this;
    }

    public WaterfallSeriesOptions connectorDashed(boolean dashed) {
        this.connectorDashed = dashed;
        return this;
    }

    public WaterfallSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public WaterfallSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public WaterfallSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public WaterfallSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public WaterfallSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
