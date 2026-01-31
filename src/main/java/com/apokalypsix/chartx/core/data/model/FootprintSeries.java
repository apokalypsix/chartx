package com.apokalypsix.chartx.core.data.model;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * High-performance footprint chart data.
 *
 * <p>A footprint chart shows bid/ask volume at each price level within
 * each time period (bar). This is useful for:
 * <ul>
 *   <li>Identifying supply/demand imbalances</li>
 *   <li>Seeing where large orders were executed</li>
 *   <li>Understanding order flow within each candle</li>
 * </ul>
 *
 * <p>The data can be linked to OhlcData for bar boundary alignment.
 *
 * <h2>Display Modes</h2>
 * <ul>
 *   <li>BID_ASK: Shows "bid x ask" format (e.g., "100 x 150")</li>
 *   <li>DELTA: Shows delta value (ask - bid)</li>
 *   <li>VOLUME: Shows total volume as bars</li>
 *   <li>PROFILE: Mini volume profile per bar</li>
 * </ul>
 */
public class FootprintSeries implements Data<FootprintBar> {

    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    private static final float GROWTH_FACTOR = 1.5f;

    private final String id;
    private final String name;
    private final float tickSize;

    // Timestamps array for fast lookup
    private long[] timestamps;

    // Bars array
    private FootprintBar[] bars;

    private int size;

    // Linked OHLC data for bar boundaries
    private OhlcData linkedOHLC;

    // Configuration
    private float imbalanceThreshold = 3.0f;

    // Listener support
    private final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates a new footprint series.
     *
     * @param id unique identifier
     * @param name display name
     * @param tickSize price tick size for level bucketing
     */
    public FootprintSeries(String id, String name, float tickSize) {
        this(id, name, tickSize, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new footprint series with specified capacity.
     */
    public FootprintSeries(String id, String name, float tickSize, int initialCapacity) {
        this.id = id;
        this.name = name;
        this.tickSize = tickSize;
        this.timestamps = new long[initialCapacity];
        this.bars = new FootprintBar[initialCapacity];
        this.size = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getXValue(int index) {
        checkIndex(index);
        return timestamps[index];
    }

    /**
     * @deprecated Use {@link #getXValue(int)} instead
     */
    @Deprecated
    public long getTimestamp(int index) {
        return getXValue(index);
    }

    @Override
    public long getMinX() {
        return size > 0 ? timestamps[0] : -1;
    }

    /**
     * @deprecated Use {@link #getMinX()} instead
     */
    @Deprecated
    public long getStartTime() {
        return getMinX();
    }

    @Override
    public long getMaxX() {
        return size > 0 ? timestamps[size - 1] : -1;
    }

    /**
     * @deprecated Use {@link #getMaxX()} instead
     */
    @Deprecated
    public long getEndTime() {
        return getMaxX();
    }

    public float getTickSize() {
        return tickSize;
    }

    // ========== Data access ==========

    /**
     * Returns the footprint bar at the given index.
     */
    public FootprintBar getBar(int index) {
        checkIndex(index);
        return bars[index];
    }

    /**
     * Returns the price levels for a bar.
     */
    public FootprintLevel[] getLevels(int barIndex) {
        checkIndex(barIndex);
        return bars[barIndex].getLevelsArray();
    }

    /**
     * Returns the delta for a bar.
     */
    public float getDelta(int barIndex) {
        checkIndex(barIndex);
        return bars[barIndex].getDelta();
    }

    /**
     * Returns the POC price index for a bar.
     */
    public int getPOCIndex(int barIndex) {
        checkIndex(barIndex);
        return bars[barIndex].getPOCIndex();
    }

    /**
     * Returns imbalances for a bar.
     */
    public List<Imbalance> getImbalances(int barIndex) {
        checkIndex(barIndex);
        return bars[barIndex].getImbalances(imbalanceThreshold);
    }

    /**
     * Returns imbalances for a bar with custom threshold.
     */
    public List<Imbalance> getImbalances(int barIndex, float threshold) {
        checkIndex(barIndex);
        return bars[barIndex].getImbalances(threshold);
    }

    // ========== Range queries ==========

    @Override
    public int indexAtOrBefore(long timestamp) {
        if (size == 0 || timestamp < timestamps[0]) {
            return -1;
        }
        if (timestamp >= timestamps[size - 1]) {
            return size - 1;
        }
        return binarySearchAtOrBefore(timestamp);
    }

    @Override
    public int indexAtOrAfter(long timestamp) {
        if (size == 0 || timestamp > timestamps[size - 1]) {
            return -1;
        }
        if (timestamp <= timestamps[0]) {
            return 0;
        }
        return binarySearchAtOrAfter(timestamp);
    }

    private int binarySearchAtOrBefore(long timestamp) {
        int low = 0;
        int high = size - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = timestamps[mid];

            if (midVal <= timestamp) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    private int binarySearchAtOrAfter(long timestamp) {
        int low = 0;
        int high = size - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = timestamps[mid];

            if (midVal >= timestamp) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    /**
     * Finds the highest price across visible bars.
     */
    public float findHighestPrice(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float highest = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            float high = bars[i].getHighPrice();
            if (!Float.isNaN(high) && high > highest) {
                highest = high;
            }
        }
        return highest;
    }

    /**
     * Finds the lowest price across visible bars.
     */
    public float findLowestPrice(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float lowest = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            float low = bars[i].getLowPrice();
            if (!Float.isNaN(low) && low < lowest) {
                lowest = low;
            }
        }
        return lowest;
    }

    // ========== Mutation ==========

    /**
     * Appends a new bar to the series.
     *
     * @param bar the footprint bar to append
     */
    public void append(FootprintBar bar) {
        if (size > 0 && bar.getTimestamp() <= timestamps[size - 1]) {
            throw new IllegalArgumentException(
                    "Timestamp must be ascending. Last: " + timestamps[size - 1] +
                            ", given: " + bar.getTimestamp());
        }

        ensureCapacity(size + 1);
        timestamps[size] = bar.getTimestamp();
        bars[size] = bar;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Creates and appends a new empty bar.
     *
     * @param timestamp the bar timestamp
     * @return the created bar
     */
    public FootprintBar appendBar(long timestamp) {
        FootprintBar bar = new FootprintBar(timestamp, tickSize);
        append(bar);
        return bar;
    }

    /**
     * Gets or creates a bar at the given timestamp.
     * If the timestamp matches the last bar, returns it.
     * Otherwise creates a new bar.
     */
    public FootprintBar getOrCreateBar(long timestamp) {
        if (size > 0 && timestamps[size - 1] == timestamp) {
            return bars[size - 1];
        }
        return appendBar(timestamp);
    }

    /**
     * Updates the last bar in the series.
     */
    public void updateLast(FootprintBar bar) {
        if (size == 0) {
            throw new IllegalStateException("Cannot update: series is empty");
        }
        bars[size - 1] = bar;
        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Adds volume to the current (last) bar.
     * Creates a new bar if the timestamp doesn't match.
     *
     * @param timestamp the trade timestamp
     * @param price the trade price
     * @param bidVolume volume at bid
     * @param askVolume volume at ask
     */
    public void addVolume(long timestamp, float price, float bidVolume, float askVolume) {
        // Align timestamp to bar boundary if linked OHLC exists
        long barTimestamp = alignTimestamp(timestamp);

        FootprintBar bar = getOrCreateBar(barTimestamp);
        bar.addVolume(price, bidVolume, askVolume);

        // Notify of update
        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Aligns a timestamp to a bar boundary.
     */
    private long alignTimestamp(long timestamp) {
        if (linkedOHLC != null && !linkedOHLC.isEmpty()) {
            int idx = linkedOHLC.indexAtOrBefore(timestamp);
            if (idx >= 0) {
                return linkedOHLC.getXValue(idx);
            }
        }
        return timestamp;
    }

    /**
     * Clears all data.
     */
    public void clear() {
        Arrays.fill(bars, 0, size, null);
        size = 0;
        listenerSupport.fireDataCleared(this);
    }

    // ========== Configuration ==========

    /**
     * Links this footprint data to OHLC data for bar boundary alignment.
     */
    public void setLinkedOHLC(OhlcData ohlc) {
        this.linkedOHLC = ohlc;
    }

    public OhlcData getLinkedOHLC() {
        return linkedOHLC;
    }

    /**
     * Sets the imbalance detection threshold.
     *
     * @param threshold ratio threshold (e.g., 3.0 for 300%)
     */
    public void setImbalanceThreshold(float threshold) {
        this.imbalanceThreshold = threshold;
    }

    public float getImbalanceThreshold() {
        return imbalanceThreshold;
    }

    // ========== Raw array access ==========

    /**
     * Returns the raw timestamps array for rendering.
     */
    public long[] getTimestampsArray() {
        return timestamps;
    }

    /**
     * Returns the raw bars array for rendering.
     */
    public FootprintBar[] getBarsArray() {
        return bars;
    }

    // ========== Internal ==========

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > timestamps.length) {
            int newCapacity = Math.max(minCapacity, (int) (timestamps.length * GROWTH_FACTOR));
            timestamps = Arrays.copyOf(timestamps, newCapacity);
            bars = Arrays.copyOf(bars, newCapacity);
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex >= size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                    "Invalid range [" + fromIndex + ", " + toIndex + "], Size: " + size);
        }
    }

    // ========== Listeners ==========

    @Override
    public void addListener(DataListener listener) {
        listenerSupport.addListener(listener);
    }

    @Override
    public void removeListener(DataListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public String toString() {
        return String.format("FootprintSeries[id=%s, name=%s, size=%d, tickSize=%.4f]",
                id, name, size, tickSize);
    }
}
