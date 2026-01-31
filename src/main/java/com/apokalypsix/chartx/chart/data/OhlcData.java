package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.OHLCBar;

import java.util.Arrays;

/**
 * High-performance OHLC (candlestick) data storage using primitive arrays.
 *
 * <p>Data is stored in parallel primitive arrays for cache-friendly access
 * and minimal memory overhead. This design avoids object allocation per bar
 * and enables efficient bulk operations.
 *
 * <p>The data supports both batch loading and streaming (append) updates.
 * All timestamps must be in ascending order.
 */
public class OhlcData extends AbstractData<OHLCBar> {

    // Parallel arrays for OHLCV data
    private float[] open;
    private float[] high;
    private float[] low;
    private float[] close;
    private float[] volume;

    /**
     * Creates empty OHLC data with the specified ID and name.
     */
    public OhlcData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty OHLC data with the specified initial capacity.
     */
    public OhlcData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.open = new float[capacity];
        this.high = new float[capacity];
        this.low = new float[capacity];
        this.close = new float[capacity];
        this.volume = new float[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        open = Arrays.copyOf(open, newCapacity);
        high = Arrays.copyOf(high, newCapacity);
        low = Arrays.copyOf(low, newCapacity);
        close = Arrays.copyOf(close, newCapacity);
        volume = Arrays.copyOf(volume, newCapacity);
    }

    // ========== OHLC accessors ==========

    public float getOpen(int index) {
        checkIndex(index);
        return open[index];
    }

    public float getHigh(int index) {
        checkIndex(index);
        return high[index];
    }

    public float getLow(int index) {
        checkIndex(index);
        return low[index];
    }

    public float getClose(int index) {
        checkIndex(index);
        return close[index];
    }

    public float getVolume(int index) {
        checkIndex(index);
        return volume[index];
    }

    /**
     * Fills the provided bar with data at the specified index.
     * This method avoids allocation and is preferred for rendering loops.
     *
     * @param index the index
     * @param out the bar to fill
     */
    public void getBar(int index, OHLCBar out) {
        checkIndex(index);
        out.set(xValues[index], open[index], high[index], low[index], close[index], volume[index]);
    }

    /**
     * Returns true if the bar at the given index is bullish (close >= open).
     */
    public boolean isBullish(int index) {
        checkIndex(index);
        return close[index] >= open[index];
    }

    // ========== Range queries ==========

    /**
     * Finds the highest high value in the specified index range.
     */
    public float findHighestHigh(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float highest = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (high[i] > highest) {
                highest = high[i];
            }
        }
        return highest;
    }

    /**
     * Finds the lowest low value in the specified index range.
     */
    public float findLowestLow(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float lowest = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (low[i] < lowest) {
                lowest = low[i];
            }
        }
        return lowest;
    }

    // ========== Mutation ==========

    /**
     * Appends a new bar to the data.
     * Timestamp must be greater than the last bar's timestamp.
     *
     * @throws IllegalArgumentException if timestamp is not in ascending order
     */
    public void append(long timestamp, float open, float high, float low, float close, float volume) {
        validateAscendingTimestamp(timestamp);
        ensureCapacity(size + 1);

        xValues[size] = timestamp;
        this.open[size] = open;
        this.high[size] = high;
        this.low[size] = low;
        this.close[size] = close;
        this.volume[size] = volume;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Appends a bar to the data.
     */
    public void append(OHLCBar bar) {
        append(bar.getTimestamp(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
    }

    /**
     * Updates the last bar in the data.
     * Useful for real-time updates where the current bar is still forming.
     *
     * @throws IllegalStateException if the data is empty
     */
    public void updateLast(float open, float high, float low, float close, float volume) {
        checkNotEmpty();
        int lastIndex = size - 1;
        this.open[lastIndex] = open;
        this.high[lastIndex] = high;
        this.low[lastIndex] = low;
        this.close[lastIndex] = close;
        this.volume[lastIndex] = volume;

        listenerSupport.fireDataUpdated(this, lastIndex);
    }

    /**
     * Loads data from parallel arrays. Replaces any existing data.
     * Arrays are copied, not referenced.
     *
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public void loadFromArrays(long[] timestamps, float[] open, float[] high,
                               float[] low, float[] close, float[] volume) {
        int length = timestamps.length;
        if (open.length != length || high.length != length || low.length != length
                || close.length != length || volume.length != length) {
            throw new IllegalArgumentException("All arrays must have the same length");
        }

        ensureCapacity(length);
        System.arraycopy(timestamps, 0, this.xValues, 0, length);
        System.arraycopy(open, 0, this.open, 0, length);
        System.arraycopy(high, 0, this.high, 0, length);
        System.arraycopy(low, 0, this.low, 0, length);
        System.arraycopy(close, 0, this.close, 0, length);
        System.arraycopy(volume, 0, this.volume, 0, length);
        this.size = length;
    }

    // ========== View creation ==========

    /**
     * Creates an XyData view of the close prices.
     * The returned XyData shares timestamp/close arrays - modifications to this
     * OhlcData will be reflected in the returned view.
     *
     * @return XyData containing close prices
     */
    public XyData asClosePriceData() {
        XyData xyData = new XyData(id + "_close", name + " Close");
        if (size > 0) {
            xyData.loadFromArrays(xValues, close);
        }
        return xyData;
    }

    /**
     * Creates an XyData view of the open prices.
     */
    public XyData asOpenPriceData() {
        XyData xyData = new XyData(id + "_open", name + " Open");
        if (size > 0) {
            xyData.loadFromArrays(xValues, open);
        }
        return xyData;
    }

    /**
     * Creates an XyData view of the high prices.
     */
    public XyData asHighPriceData() {
        XyData xyData = new XyData(id + "_high", name + " High");
        if (size > 0) {
            xyData.loadFromArrays(xValues, high);
        }
        return xyData;
    }

    /**
     * Creates an XyData view of the low prices.
     */
    public XyData asLowPriceData() {
        XyData xyData = new XyData(id + "_low", name + " Low");
        if (size > 0) {
            xyData.loadFromArrays(xValues, low);
        }
        return xyData;
    }

    // ========== Raw array access ==========

    /**
     * Returns the raw open array. For rendering use only.
     */
    public float[] getOpenArray() {
        return open;
    }

    /**
     * Returns the raw high array. For rendering use only.
     */
    public float[] getHighArray() {
        return high;
    }

    /**
     * Returns the raw low array. For rendering use only.
     */
    public float[] getLowArray() {
        return low;
    }

    /**
     * Returns the raw close array. For rendering use only.
     */
    public float[] getCloseArray() {
        return close;
    }

    /**
     * Returns the raw volume array. For rendering use only.
     */
    public float[] getVolumeArray() {
        return volume;
    }
}
