package com.apokalypsix.chartx.chart.finance.indicator;

/**
 * Categories for organizing technical indicators.
 */
public enum IndicatorCategory {

    /**
     * Trend-following indicators (e.g., SMA, EMA, MACD).
     */
    TREND("Trend"),

    /**
     * Momentum/oscillator indicators (e.g., RSI, Stochastic).
     */
    MOMENTUM("Momentum"),

    /**
     * Volatility indicators (e.g., ATR, Bollinger Bands).
     */
    VOLATILITY("Volatility"),

    /**
     * Volume-based indicators (e.g., VWAP, OBV).
     */
    VOLUME("Volume"),

    /**
     * Support/resistance and price level indicators.
     */
    SUPPORT_RESISTANCE("Support/Resistance"),

    /**
     * Custom or user-defined indicators.
     */
    CUSTOM("Custom");

    private final String displayName;

    IndicatorCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name for this category.
     */
    public String getDisplayName() {
        return displayName;
    }
}
