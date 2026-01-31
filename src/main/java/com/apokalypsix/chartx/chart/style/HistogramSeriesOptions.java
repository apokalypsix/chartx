package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for histogram series.
 *
 * <p>Supports positive/negative coloring for delta histograms,
 * bar width configuration, and opacity. Uses a fluent builder pattern.
 */
public class HistogramSeriesOptions extends SeriesOptions {

    /** Color for positive values */
    private Color positiveColor = new Color(38, 166, 91);  // Green

    /** Color for negative values */
    private Color negativeColor = new Color(214, 69, 65);  // Red

    /** Color for zero/neutral values */
    private Color neutralColor = new Color(100, 102, 106); // Gray

    /** Width of bars as ratio of available space (0.0 - 1.0) */
    private float barWidthRatio = 0.8f;

    /** Overall opacity (0.0 - 1.0) */
    private float opacity = 0.7f;

    /** Baseline value (bars drawn from baseline to value) */
    private double baseline = 0.0;

    /**
     * Creates default histogram series options.
     */
    public HistogramSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public HistogramSeriesOptions(HistogramSeriesOptions other) {
        super(other);
        this.positiveColor = other.positiveColor;
        this.negativeColor = other.negativeColor;
        this.neutralColor = other.neutralColor;
        this.barWidthRatio = other.barWidthRatio;
        this.opacity = other.opacity;
        this.baseline = other.baseline;
    }

    // ========== Getters ==========

    public Color getPositiveColor() {
        return positiveColor;
    }

    public Color getNegativeColor() {
        return negativeColor;
    }

    public Color getNeutralColor() {
        return neutralColor;
    }

    public float getBarWidthRatio() {
        return barWidthRatio;
    }

    public float getOpacity() {
        return opacity;
    }

    public double getBaseline() {
        return baseline;
    }

    /**
     * Returns the appropriate color for the given value.
     *
     * @param value the histogram value
     * @return the appropriate color
     */
    public Color getColorForValue(float value) {
        if (value > baseline) {
            return positiveColor;
        } else if (value < baseline) {
            return negativeColor;
        } else {
            return neutralColor;
        }
    }

    // ========== Fluent setters ==========

    /**
     * Sets the color for positive values.
     *
     * @param positiveColor the color
     * @return this for chaining
     */
    public HistogramSeriesOptions positiveColor(Color positiveColor) {
        this.positiveColor = positiveColor;
        return this;
    }

    /**
     * Sets the color for positive values using RGB.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public HistogramSeriesOptions positiveColor(int r, int g, int b) {
        this.positiveColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the color for negative values.
     *
     * @param negativeColor the color
     * @return this for chaining
     */
    public HistogramSeriesOptions negativeColor(Color negativeColor) {
        this.negativeColor = negativeColor;
        return this;
    }

    /**
     * Sets the color for negative values using RGB.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public HistogramSeriesOptions negativeColor(int r, int g, int b) {
        this.negativeColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the color for zero/neutral values.
     *
     * @param neutralColor the color
     * @return this for chaining
     */
    public HistogramSeriesOptions neutralColor(Color neutralColor) {
        this.neutralColor = neutralColor;
        return this;
    }

    /**
     * Sets the bar width as a ratio of available space.
     *
     * @param barWidthRatio the ratio (0.0 - 1.0)
     * @return this for chaining
     */
    public HistogramSeriesOptions barWidthRatio(float barWidthRatio) {
        this.barWidthRatio = Math.max(0.1f, Math.min(1.0f, barWidthRatio));
        return this;
    }

    /**
     * Sets the overall opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public HistogramSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    /**
     * Sets the baseline value.
     *
     * @param baseline the baseline
     * @return this for chaining
     */
    public HistogramSeriesOptions baseline(double baseline) {
        this.baseline = baseline;
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public HistogramSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public HistogramSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public HistogramSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public HistogramSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
