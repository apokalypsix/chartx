package com.apokalypsix.chartx.chart.finance.indicator.dsl;

import com.apokalypsix.chartx.chart.data.OhlcData;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for evaluating expressions.
 *
 * <p>Provides access to price data and caches intermediate calculations
 * for efficiency.
 */
public class EvaluationContext {

    private final OhlcData source;
    private final float[] opens;
    private final float[] highs;
    private final float[] lows;
    private final float[] closes;
    private final float[] volumes;
    private final long[] timestamps;
    private final int size;

    // Cache for computed indicator values
    private final Map<String, float[]> indicatorCache = new HashMap<>();

    /**
     * Creates an evaluation context for the given source data.
     */
    public EvaluationContext(OhlcData source) {
        this.source = source;
        this.opens = source.getOpenArray();
        this.highs = source.getHighArray();
        this.lows = source.getLowArray();
        this.closes = source.getCloseArray();
        this.volumes = source.getVolumeArray();
        this.timestamps = source.getTimestampsArray();
        this.size = source.size();
    }

    /**
     * Returns the source OHLC data.
     */
    public OhlcData getSource() {
        return source;
    }

    /**
     * Returns the number of bars.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns open price at index.
     */
    public float getOpen(int index) {
        return (index >= 0 && index < size) ? opens[index] : Float.NaN;
    }

    /**
     * Returns high price at index.
     */
    public float getHigh(int index) {
        return (index >= 0 && index < size) ? highs[index] : Float.NaN;
    }

    /**
     * Returns low price at index.
     */
    public float getLow(int index) {
        return (index >= 0 && index < size) ? lows[index] : Float.NaN;
    }

    /**
     * Returns close price at index.
     */
    public float getClose(int index) {
        return (index >= 0 && index < size) ? closes[index] : Float.NaN;
    }

    /**
     * Returns volume at index.
     */
    public float getVolume(int index) {
        return (index >= 0 && index < size) ? volumes[index] : Float.NaN;
    }

    /**
     * Returns timestamp at index.
     */
    public long getTimestamp(int index) {
        return (index >= 0 && index < size) ? timestamps[index] : 0;
    }

    /**
     * Returns the full array of timestamps.
     */
    public long[] getTimestamps() {
        return timestamps;
    }

    // ========== Derived price fields ==========

    /**
     * Returns HL2 (High+Low)/2 at index.
     */
    public float getHl2(int index) {
        if (index < 0 || index >= size) return Float.NaN;
        return (highs[index] + lows[index]) / 2;
    }

    /**
     * Returns HLC3 (High+Low+Close)/3 at index.
     */
    public float getHlc3(int index) {
        if (index < 0 || index >= size) return Float.NaN;
        return (highs[index] + lows[index] + closes[index]) / 3;
    }

    /**
     * Returns OHLC4 (Open+High+Low+Close)/4 at index.
     */
    public float getOhlc4(int index) {
        if (index < 0 || index >= size) return Float.NaN;
        return (opens[index] + highs[index] + lows[index] + closes[index]) / 4;
    }

    // ========== Indicator cache ==========

    /**
     * Gets cached indicator values, or null if not cached.
     */
    public float[] getCachedIndicator(String key) {
        return indicatorCache.get(key);
    }

    /**
     * Caches indicator values for reuse.
     */
    public void cacheIndicator(String key, float[] values) {
        indicatorCache.put(key, values);
    }

    /**
     * Checks if an indicator is cached.
     */
    public boolean hasCachedIndicator(String key) {
        return indicatorCache.containsKey(key);
    }

    /**
     * Clears the indicator cache.
     */
    public void clearCache() {
        indicatorCache.clear();
    }
}
