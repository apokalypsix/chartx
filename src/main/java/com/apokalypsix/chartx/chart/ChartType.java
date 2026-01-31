package com.apokalypsix.chartx.chart;

/**
 * Defines the type/purpose of a chart, affecting default axis labels
 * and auto-scaling behavior.
 */
public enum ChartType {
    /**
     * Main price chart with candlesticks.
     * Y-axis shows price values with appropriate precision.
     */
    PRICE,

    /**
     * Volume histogram pane.
     * Y-axis shows volume values, typically with K/M/B suffixes.
     */
    VOLUME,

    /**
     * Technical indicator pane (RSI, MACD, etc.).
     * Y-axis scaling depends on indicator type.
     */
    INDICATOR,

    /**
     * TPO/Market Profile chart.
     * Shows time at price distribution.
     */
    TPO,

    /**
     * Footprint/Cluster chart.
     * Shows bid/ask volume at each price level.
     */
    FOOTPRINT,

    /**
     * Volume Profile chart.
     * Shows volume distribution across price levels.
     */
    VOLUME_PROFILE
}
