package com.apokalypsix.chartx.chart.style;

/**
 * Defines the visual style for rendering OHLC data.
 *
 * <p>Each style provides a different way to visualize price action:
 * <ul>
 *   <li>{@link #CANDLESTICK} - Traditional filled candlesticks (default)</li>
 *   <li>{@link #OHLC_BAR} - Vertical line with horizontal ticks for open/close</li>
 *   <li>{@link #HOLLOW_CANDLE} - Outline for bullish, filled for bearish</li>
 *   <li>{@link #COLORED_CANDLE} - Custom color rules (e.g., close vs previous close)</li>
 *   <li>{@link #HEIKIN_ASHI} - Smoothed candles using calculated OHLC values</li>
 * </ul>
 */
public enum ChartStyle {

    /**
     * Traditional filled candlesticks.
     * Green/bullish when close >= open, red/bearish when close < open.
     */
    CANDLESTICK,

    /**
     * OHLC bar style.
     * Vertical line from high to low, with horizontal ticks for open (left) and close (right).
     */
    OHLC_BAR,

    /**
     * Hollow candle style.
     * Bullish candles (close >= open) are drawn as outlines.
     * Bearish candles (close < open) are filled.
     */
    HOLLOW_CANDLE,

    /**
     * Custom color rules for candles.
     * Uses an {@link OHLCColorRule} to determine colors based on arbitrary criteria
     * (e.g., comparing close to previous bar's close).
     */
    COLORED_CANDLE,

    /**
     * Heikin-Ashi candles.
     * Uses transformed OHLC values that smooth price action:
     * <ul>
     *   <li>HA Close = (Open + High + Low + Close) / 4</li>
     *   <li>HA Open = (prev HA Open + prev HA Close) / 2</li>
     *   <li>HA High = max(High, HA Open, HA Close)</li>
     *   <li>HA Low = min(Low, HA Open, HA Close)</li>
     * </ul>
     */
    HEIKIN_ASHI
}
