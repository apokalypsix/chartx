package com.apokalypsix.chartx.chart.data;

/**
 * Mutable value object representing a single OHLC (Open-High-Low-Close) bar.
 *
 * <p>This class is designed for reuse to avoid allocation during rendering loops.
 * Use {@link #set} to populate an existing instance rather than creating new objects.
 *
 * <p>Example usage:
 * <pre>{@code
 * OHLCBar bar = new OHLCBar();
 * for (int i = 0; i < series.size(); i++) {
 *     series.getBar(i, bar);  // Reuses the same instance
 *     renderCandle(bar);
 * }
 * }</pre>
 */
public final class OHLCBar {

    private long timestamp;
    private float open;
    private float high;
    private float low;
    private float close;
    private float volume;

    /**
     * Creates an uninitialized bar. Call {@link #set} to populate.
     */
    public OHLCBar() {
    }

    /**
     * Creates a bar with the specified values.
     */
    public OHLCBar(long timestamp, float open, float high, float low, float close, float volume) {
        set(timestamp, open, high, low, close, volume);
    }

    /**
     * Sets all values of this bar. Intended for object reuse.
     *
     * @return this instance for method chaining
     */
    public OHLCBar set(long timestamp, float open, float high, float low, float close, float volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        return this;
    }

    /**
     * Copies values from another bar.
     *
     * @return this instance for method chaining
     */
    public OHLCBar copyFrom(OHLCBar other) {
        return set(other.timestamp, other.open, other.high, other.low, other.close, other.volume);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getOpen() {
        return open;
    }

    public float getHigh() {
        return high;
    }

    public float getLow() {
        return low;
    }

    public float getClose() {
        return close;
    }

    public float getVolume() {
        return volume;
    }

    /**
     * Returns true if this is a bullish (green) bar (close >= open).
     */
    public boolean isBullish() {
        return close >= open;
    }

    /**
     * Returns true if this is a bearish (red) bar (close < open).
     */
    public boolean isBearish() {
        return close < open;
    }

    /**
     * Returns the body size (absolute difference between open and close).
     */
    public float getBodySize() {
        return Math.abs(close - open);
    }

    /**
     * Returns the full range (high - low).
     */
    public float getRange() {
        return high - low;
    }

    /**
     * Returns the upper wick size.
     */
    public float getUpperWickSize() {
        return high - Math.max(open, close);
    }

    /**
     * Returns the lower wick size.
     */
    public float getLowerWickSize() {
        return Math.min(open, close) - low;
    }

    /**
     * Returns the top of the body (max of open and close).
     */
    public float getBodyTop() {
        return Math.max(open, close);
    }

    /**
     * Returns the bottom of the body (min of open and close).
     */
    public float getBodyBottom() {
        return Math.min(open, close);
    }

    @Override
    public String toString() {
        return String.format("OHLCBar[t=%d, O=%.2f, H=%.2f, L=%.2f, C=%.2f, V=%.0f]",
                timestamp, open, high, low, close, volume);
    }
}
