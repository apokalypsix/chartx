package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for band series (Bollinger Bands, Keltner Channels, etc.).
 *
 * <p>Supports styling for upper, middle, and lower bands, as well as
 * fill between bands. Uses a fluent builder pattern.
 */
public class BandSeriesOptions extends SeriesOptions {

    /** Color for the upper band line */
    private Color upperColor = new Color(100, 149, 237);   // Cornflower blue

    /** Color for the middle band line */
    private Color middleColor = new Color(65, 131, 196);   // Blue

    /** Color for the lower band line */
    private Color lowerColor = new Color(100, 149, 237);   // Cornflower blue

    /** Fill color between upper and lower bands */
    private Color fillColor = new Color(100, 149, 237, 30); // Semi-transparent

    /** Line width for all bands */
    private float lineWidth = 1.0f;

    /** Whether to show the fill between bands */
    private boolean showFill = true;

    /** Whether to show the middle line */
    private boolean showMiddle = true;

    /**
     * Creates default band series options.
     */
    public BandSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public BandSeriesOptions(BandSeriesOptions other) {
        super(other);
        this.upperColor = other.upperColor;
        this.middleColor = other.middleColor;
        this.lowerColor = other.lowerColor;
        this.fillColor = other.fillColor;
        this.lineWidth = other.lineWidth;
        this.showFill = other.showFill;
        this.showMiddle = other.showMiddle;
    }

    // ========== Getters ==========

    public Color getUpperColor() {
        return upperColor;
    }

    public Color getMiddleColor() {
        return middleColor;
    }

    public Color getLowerColor() {
        return lowerColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public boolean isShowFill() {
        return showFill;
    }

    public boolean isShowMiddle() {
        return showMiddle;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the upper band line color.
     *
     * @param upperColor the color
     * @return this for chaining
     */
    public BandSeriesOptions upperColor(Color upperColor) {
        this.upperColor = upperColor;
        return this;
    }

    /**
     * Sets the upper band line color using RGB.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public BandSeriesOptions upperColor(int r, int g, int b) {
        this.upperColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the middle band line color.
     *
     * @param middleColor the color
     * @return this for chaining
     */
    public BandSeriesOptions middleColor(Color middleColor) {
        this.middleColor = middleColor;
        return this;
    }

    /**
     * Sets the middle band line color using RGB.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public BandSeriesOptions middleColor(int r, int g, int b) {
        this.middleColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the lower band line color.
     *
     * @param lowerColor the color
     * @return this for chaining
     */
    public BandSeriesOptions lowerColor(Color lowerColor) {
        this.lowerColor = lowerColor;
        return this;
    }

    /**
     * Sets the lower band line color using RGB.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public BandSeriesOptions lowerColor(int r, int g, int b) {
        this.lowerColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets all band colors to the same value.
     *
     * @param color the color for all bands
     * @return this for chaining
     */
    public BandSeriesOptions bandColor(Color color) {
        this.upperColor = color;
        this.middleColor = color;
        this.lowerColor = color;
        return this;
    }

    /**
     * Sets the fill color between bands.
     *
     * @param fillColor the fill color
     * @return this for chaining
     */
    public BandSeriesOptions fillColor(Color fillColor) {
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
    public BandSeriesOptions fillColor(int r, int g, int b, int a) {
        this.fillColor = new Color(r, g, b, a);
        return this;
    }

    /**
     * Sets the line width for all bands.
     *
     * @param lineWidth the width in pixels
     * @return this for chaining
     */
    public BandSeriesOptions lineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.5f, lineWidth);
        return this;
    }

    /**
     * Sets whether to show the fill between bands.
     *
     * @param showFill true to show fill
     * @return this for chaining
     */
    public BandSeriesOptions showFill(boolean showFill) {
        this.showFill = showFill;
        return this;
    }

    /**
     * Sets whether to show the middle line.
     *
     * @param showMiddle true to show middle line
     * @return this for chaining
     */
    public BandSeriesOptions showMiddle(boolean showMiddle) {
        this.showMiddle = showMiddle;
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public BandSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public BandSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public BandSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public BandSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
