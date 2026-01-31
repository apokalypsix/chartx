package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for OHLC/candlestick series.
 *
 * <p>Supports multiple chart styles including candlesticks, OHLC bars,
 * hollow candles, and Heikin-Ashi. Uses a fluent builder pattern.
 */
public class OhlcSeriesOptions extends SeriesOptions {

    /**
     * Style for rendering OHLC data.
     */
    public enum OhlcStyle {
        /** Standard filled candlesticks */
        CANDLESTICK,
        /** Traditional OHLC bars (no body, just lines) */
        OHLC_BAR,
        /** Hollow candles (bullish = hollow, bearish = filled) */
        HOLLOW_CANDLE,
        /** Heikin-Ashi candles (requires HeikinAshiTransform on data) */
        HEIKIN_ASHI,
        /** Simple line connecting close prices */
        LINE
    }

    /** Color for bullish (up) candles */
    private Color upColor = new Color(38, 166, 91);  // Green

    /** Color for bearish (down) candles */
    private Color downColor = new Color(214, 69, 65); // Red

    /** Color for wicks (can be null to use candle body color) */
    private Color wickColor = null;

    /** Border color (can be null for no border) */
    private Color borderColor = null;

    /** Chart style */
    private OhlcStyle style = OhlcStyle.CANDLESTICK;

    /** Width of candle body as ratio of available space (0.0 - 1.0) */
    private float barWidthRatio = 0.8f;

    /** Wick width in pixels */
    private float wickWidth = 1.0f;

    /**
     * Creates default OHLC series options.
     */
    public OhlcSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public OhlcSeriesOptions(OhlcSeriesOptions other) {
        super(other);
        this.upColor = other.upColor;
        this.downColor = other.downColor;
        this.wickColor = other.wickColor;
        this.borderColor = other.borderColor;
        this.style = other.style;
        this.barWidthRatio = other.barWidthRatio;
        this.wickWidth = other.wickWidth;
    }

    // ========== Getters ==========

    public Color getUpColor() {
        return upColor;
    }

    public Color getDownColor() {
        return downColor;
    }

    public Color getWickColor() {
        return wickColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public OhlcStyle getStyle() {
        return style;
    }

    public float getBarWidthRatio() {
        return barWidthRatio;
    }

    public float getWickWidth() {
        return wickWidth;
    }

    /**
     * Returns the appropriate color for the given candle.
     *
     * @param isBullish true if the candle is bullish (close >= open)
     * @return the appropriate color
     */
    public Color getColorForCandle(boolean isBullish) {
        return isBullish ? upColor : downColor;
    }

    /**
     * Returns the wick color, falling back to candle color if not set.
     *
     * @param isBullish true if the candle is bullish
     * @return the wick color
     */
    public Color getWickColorForCandle(boolean isBullish) {
        return wickColor != null ? wickColor : getColorForCandle(isBullish);
    }

    // ========== Fluent setters ==========

    /**
     * Sets the color for bullish (up) candles.
     *
     * @param upColor the color
     * @return this for chaining
     */
    public OhlcSeriesOptions upColor(Color upColor) {
        this.upColor = upColor;
        return this;
    }

    /**
     * Sets the color for bullish candles using RGB values.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public OhlcSeriesOptions upColor(int r, int g, int b) {
        this.upColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the color for bearish (down) candles.
     *
     * @param downColor the color
     * @return this for chaining
     */
    public OhlcSeriesOptions downColor(Color downColor) {
        this.downColor = downColor;
        return this;
    }

    /**
     * Sets the color for bearish candles using RGB values.
     *
     * @param r red (0-255)
     * @param g green (0-255)
     * @param b blue (0-255)
     * @return this for chaining
     */
    public OhlcSeriesOptions downColor(int r, int g, int b) {
        this.downColor = new Color(r, g, b);
        return this;
    }

    /**
     * Sets the wick color. Pass null to use candle body color.
     *
     * @param wickColor the wick color
     * @return this for chaining
     */
    public OhlcSeriesOptions wickColor(Color wickColor) {
        this.wickColor = wickColor;
        return this;
    }

    /**
     * Sets the border color. Pass null for no border.
     *
     * @param borderColor the border color
     * @return this for chaining
     */
    public OhlcSeriesOptions borderColor(Color borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    /**
     * Sets the chart style.
     *
     * @param style the style
     * @return this for chaining
     */
    public OhlcSeriesOptions style(OhlcStyle style) {
        this.style = style != null ? style : OhlcStyle.CANDLESTICK;
        return this;
    }

    /**
     * Sets the candle body width as a ratio of available space.
     *
     * @param barWidthRatio the ratio (0.0 - 1.0)
     * @return this for chaining
     */
    public OhlcSeriesOptions barWidthRatio(float barWidthRatio) {
        this.barWidthRatio = Math.max(0.1f, Math.min(1.0f, barWidthRatio));
        return this;
    }

    /**
     * Sets the wick width in pixels.
     *
     * @param wickWidth the width
     * @return this for chaining
     */
    public OhlcSeriesOptions wickWidth(float wickWidth) {
        this.wickWidth = Math.max(0.5f, wickWidth);
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public OhlcSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public OhlcSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public OhlcSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public OhlcSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
