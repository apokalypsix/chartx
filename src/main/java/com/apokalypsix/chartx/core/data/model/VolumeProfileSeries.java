package com.apokalypsix.chartx.core.data.model;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;

import java.awt.Color;
import java.util.Arrays;

/**
 * Volume Profile data series representing volume distribution across price levels.
 *
 * <p>A Volume Profile displays the amount of trading activity at each price level
 * over a specified time range. It shows where the market spent the most time
 * and can help identify support/resistance levels.
 *
 * <p>Key concepts:
 * <ul>
 *   <li><b>POC (Point of Control)</b> - The price level with the highest volume</li>
 *   <li><b>Value Area</b> - The price range where 70% of trading occurred</li>
 *   <li><b>VAH (Value Area High)</b> - Upper bound of the value area</li>
 *   <li><b>VAL (Value Area Low)</b> - Lower bound of the value area</li>
 * </ul>
 *
 * <p>Data is stored in parallel primitive arrays for cache-friendly access.
 * Price levels are sorted in ascending order.
 */
public class VolumeProfileSeries implements Data<VolumeProfileBar> {

    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    private static final float GROWTH_FACTOR = 1.5f;
    private static final float VALUE_AREA_PERCENTAGE = 0.70f;

    private final String id;
    private final String name;

    // Price levels (sorted ascending)
    private float[] priceLevels;
    private float[] buyVolume;      // Volume from upticks/market buys
    private float[] sellVolume;     // Volume from downticks/market sells

    private int levelCount;

    // Profile configuration
    private float tickSize;         // Price increment for bucketing
    private long startTime;         // Profile time range start
    private long endTime;           // Profile time range end

    // Computed values (cached, lazily recalculated)
    private int pocIndex = -1;      // Point of Control index
    private int vahIndex = -1;      // Value Area High index
    private int valIndex = -1;      // Value Area Low index
    private float totalVolume = 0;
    private boolean dirty = true;   // True if cached values need recalculation

    // Visual style
    private Color buyColor = new Color(38, 166, 91);    // Green for buys
    private Color sellColor = new Color(214, 69, 65);   // Red for sells
    private Color pocColor = new Color(255, 193, 7);    // Yellow for POC
    private Color valueAreaColor = new Color(100, 149, 237, 50); // Semi-transparent blue
    private float opacity = 0.8f;

    // Listener support for real-time updates
    private final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates an empty volume profile with the specified tick size.
     *
     * @param id unique identifier
     * @param name display name
     * @param tickSize price increment for bucketing (e.g., 0.25 for ES futures)
     */
    public VolumeProfileSeries(String id, String name, float tickSize) {
        this(id, name, tickSize, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates an empty volume profile with the specified capacity.
     *
     * @param id unique identifier
     * @param name display name
     * @param tickSize price increment for bucketing
     * @param initialCapacity initial number of price levels
     */
    public VolumeProfileSeries(String id, String name, float tickSize, int initialCapacity) {
        if (tickSize <= 0) {
            throw new IllegalArgumentException("Tick size must be positive: " + tickSize);
        }

        this.id = id;
        this.name = name;
        this.tickSize = tickSize;
        this.priceLevels = new float[initialCapacity];
        this.buyVolume = new float[initialCapacity];
        this.sellVolume = new float[initialCapacity];
        this.levelCount = 0;
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
        return levelCount;
    }

    @Override
    public long getXValue(int index) {
        // Volume profiles span a time range rather than being point-in-time
        // Return the start time for any index
        return startTime;
    }

    /**
     * @deprecated Use {@link #getXValue(int)} instead
     */
    @Deprecated
    public long getTimestamp(int index) {
        return getXValue(index);
    }

    // ========== Profile configuration ==========

    /**
     * Returns the tick size (price increment) for this profile.
     */
    public float getTickSize() {
        return tickSize;
    }

    /**
     * Returns the start time of the profile's time range.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of the profile's time range.
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the end time of the profile's time range.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time of the profile's time range.
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the time range for this profile.
     */
    public void setTimeRange(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // ========== Level accessors ==========

    /**
     * Returns the number of price levels in the profile.
     */
    public int getLevelCount() {
        return levelCount;
    }

    /**
     * Returns the price at the specified level index.
     */
    public float getPriceAt(int levelIndex) {
        checkLevelIndex(levelIndex);
        return priceLevels[levelIndex];
    }

    /**
     * Returns the buy volume at the specified level index.
     */
    public float getBuyVolumeAt(int levelIndex) {
        checkLevelIndex(levelIndex);
        return buyVolume[levelIndex];
    }

    /**
     * Returns the sell volume at the specified level index.
     */
    public float getSellVolumeAt(int levelIndex) {
        checkLevelIndex(levelIndex);
        return sellVolume[levelIndex];
    }

    /**
     * Returns the total volume (buy + sell) at the specified level index.
     */
    public float getTotalVolumeAt(int levelIndex) {
        checkLevelIndex(levelIndex);
        return buyVolume[levelIndex] + sellVolume[levelIndex];
    }

    /**
     * Returns the delta (buy - sell) at the specified level index.
     */
    public float getDeltaAt(int levelIndex) {
        checkLevelIndex(levelIndex);
        return buyVolume[levelIndex] - sellVolume[levelIndex];
    }

    // ========== Profile statistics ==========

    /**
     * Returns the Point of Control (POC) price - the level with highest volume.
     */
    public float getPOCPrice() {
        ensureComputed();
        return pocIndex >= 0 ? priceLevels[pocIndex] : Float.NaN;
    }

    /**
     * Returns the index of the POC level.
     */
    public int getPOCIndex() {
        ensureComputed();
        return pocIndex;
    }

    /**
     * Returns the Value Area High (VAH) price.
     */
    public float getValueAreaHigh() {
        ensureComputed();
        return vahIndex >= 0 ? priceLevels[vahIndex] : Float.NaN;
    }

    /**
     * Returns the index of the VAH level.
     */
    public int getVAHIndex() {
        ensureComputed();
        return vahIndex;
    }

    /**
     * Returns the Value Area Low (VAL) price.
     */
    public float getValueAreaLow() {
        ensureComputed();
        return valIndex >= 0 ? priceLevels[valIndex] : Float.NaN;
    }

    /**
     * Returns the index of the VAL level.
     */
    public int getVALIndex() {
        ensureComputed();
        return valIndex;
    }

    /**
     * Returns the total volume across all price levels.
     */
    public float getTotalVolume() {
        ensureComputed();
        return totalVolume;
    }

    /**
     * Returns the lowest price level in the profile.
     */
    public float getLowestPrice() {
        return levelCount > 0 ? priceLevels[0] : Float.NaN;
    }

    /**
     * Returns the highest price level in the profile.
     */
    public float getHighestPrice() {
        return levelCount > 0 ? priceLevels[levelCount - 1] : Float.NaN;
    }

    /**
     * Returns true if the given level index is within the value area.
     */
    public boolean isInValueArea(int levelIndex) {
        ensureComputed();
        return levelIndex >= valIndex && levelIndex <= vahIndex;
    }

    // ========== Building the profile ==========

    /**
     * Adds volume at a specific price level.
     *
     * <p>The price is quantized to the nearest tick size. If the level already
     * exists, the volume is added to it. Otherwise, a new level is inserted
     * in sorted order.
     *
     * @param price the price level
     * @param buyVol buy volume to add
     * @param sellVol sell volume to add
     */
    public void addVolume(float price, float buyVol, float sellVol) {
        // Quantize price to tick size
        float quantizedPrice = Math.round(price / tickSize) * tickSize;

        // Find or create the level
        int index = findOrInsertLevel(quantizedPrice);

        // Add volume
        buyVolume[index] += buyVol;
        sellVolume[index] += sellVol;

        dirty = true;
        listenerSupport.fireDataUpdated(this, index);
    }

    /**
     * Adds total volume at a price level (split evenly between buy/sell).
     */
    public void addVolume(float price, float volume) {
        addVolume(price, volume / 2, volume / 2);
    }

    /**
     * Builds a volume profile from OHLC data.
     *
     * <p>Volume is distributed across the bar's price range, with more weight
     * given to the body of the candle.
     *
     * @param ohlc the source OHLC data
     * @param fromIndex start index (inclusive)
     * @param toIndex end index (inclusive)
     */
    public void buildFromOHLC(OhlcData ohlc, int fromIndex, int toIndex) {
        if (ohlc == null || ohlc.isEmpty()) {
            return;
        }

        if (fromIndex < 0) fromIndex = 0;
        if (toIndex >= ohlc.size()) toIndex = ohlc.size() - 1;

        // Set time range
        setTimeRange(ohlc.getXValue(fromIndex), ohlc.getXValue(toIndex));

        // Process each bar
        for (int i = fromIndex; i <= toIndex; i++) {
            float low = ohlc.getLow(i);
            float high = ohlc.getHigh(i);
            float open = ohlc.getOpen(i);
            float close = ohlc.getClose(i);
            float volume = ohlc.getVolume(i);

            if (volume <= 0) continue;

            // Distribute volume across the bar's range
            distributeBarVolume(low, high, open, close, volume);
        }

        recalculateStatistics();
    }

    /**
     * Distributes a bar's volume across its price range.
     */
    private void distributeBarVolume(float low, float high, float open, float close, float volume) {
        // Calculate number of ticks in the bar
        int tickCount = Math.max(1, Math.round((high - low) / tickSize) + 1);

        // Simple distribution: give more weight to body region
        float bodyLow = Math.min(open, close);
        float bodyHigh = Math.max(open, close);

        boolean isBullish = close >= open;

        // Distribute volume to each tick
        float volumePerTick = volume / tickCount;
        for (float price = low; price <= high + tickSize / 2; price += tickSize) {
            float tickVol = volumePerTick;

            // Give extra weight to body
            if (price >= bodyLow && price <= bodyHigh) {
                tickVol *= 1.5f;
            }

            // Assign as buy or sell based on candle direction
            if (isBullish) {
                addVolume(price, tickVol * 0.6f, tickVol * 0.4f);
            } else {
                addVolume(price, tickVol * 0.4f, tickVol * 0.6f);
            }
        }
    }

    /**
     * Clears all volume data.
     */
    public void clear() {
        levelCount = 0;
        pocIndex = -1;
        vahIndex = -1;
        valIndex = -1;
        totalVolume = 0;
        dirty = false;
        listenerSupport.fireDataCleared(this);
    }

    // ========== Statistics calculation ==========

    /**
     * Recalculates POC and Value Area.
     * Call this after adding all volume data.
     */
    public void recalculateStatistics() {
        if (levelCount == 0) {
            pocIndex = -1;
            vahIndex = -1;
            valIndex = -1;
            totalVolume = 0;
            dirty = false;
            return;
        }

        // Calculate total volume and find POC
        totalVolume = 0;
        float maxVolume = 0;
        pocIndex = 0;

        for (int i = 0; i < levelCount; i++) {
            float vol = buyVolume[i] + sellVolume[i];
            totalVolume += vol;
            if (vol > maxVolume) {
                maxVolume = vol;
                pocIndex = i;
            }
        }

        // Calculate value area (70% of volume centered around POC)
        calculateValueArea();

        dirty = false;
    }

    /**
     * Calculates the value area using the TPO counting method.
     * Starts from POC and expands outward until 70% of volume is included.
     */
    private void calculateValueArea() {
        if (levelCount == 0 || totalVolume <= 0) {
            vahIndex = pocIndex;
            valIndex = pocIndex;
            return;
        }

        float targetVolume = totalVolume * VALUE_AREA_PERCENTAGE;
        float accumulatedVolume = buyVolume[pocIndex] + sellVolume[pocIndex];

        int lowIdx = pocIndex;
        int highIdx = pocIndex;

        // Expand from POC until we have 70% of volume
        while (accumulatedVolume < targetVolume && (lowIdx > 0 || highIdx < levelCount - 1)) {
            float volBelow = (lowIdx > 0) ? buyVolume[lowIdx - 1] + sellVolume[lowIdx - 1] : 0;
            float volAbove = (highIdx < levelCount - 1) ? buyVolume[highIdx + 1] + sellVolume[highIdx + 1] : 0;

            // Add the side with more volume (or only side available)
            if (lowIdx <= 0) {
                highIdx++;
                accumulatedVolume += volAbove;
            } else if (highIdx >= levelCount - 1) {
                lowIdx--;
                accumulatedVolume += volBelow;
            } else if (volBelow >= volAbove) {
                lowIdx--;
                accumulatedVolume += volBelow;
            } else {
                highIdx++;
                accumulatedVolume += volAbove;
            }
        }

        valIndex = lowIdx;
        vahIndex = highIdx;
    }

    private void ensureComputed() {
        if (dirty) {
            recalculateStatistics();
        }
    }

    // ========== Internal helpers ==========

    /**
     * Finds the index of a price level, or inserts a new one in sorted order.
     */
    private int findOrInsertLevel(float price) {
        // Binary search for the price
        int low = 0;
        int high = levelCount - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            float midPrice = priceLevels[mid];

            if (Math.abs(midPrice - price) < tickSize / 2) {
                // Found existing level
                return mid;
            } else if (midPrice < price) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        // Not found - insert at position 'low'
        ensureCapacity(levelCount + 1);

        // Shift elements to make room
        if (low < levelCount) {
            System.arraycopy(priceLevels, low, priceLevels, low + 1, levelCount - low);
            System.arraycopy(buyVolume, low, buyVolume, low + 1, levelCount - low);
            System.arraycopy(sellVolume, low, sellVolume, low + 1, levelCount - low);
        }

        // Insert new level
        priceLevels[low] = price;
        buyVolume[low] = 0;
        sellVolume[low] = 0;
        levelCount++;

        return low;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > priceLevels.length) {
            int newCapacity = Math.max(minCapacity, (int) (priceLevels.length * GROWTH_FACTOR));
            priceLevels = Arrays.copyOf(priceLevels, newCapacity);
            buyVolume = Arrays.copyOf(buyVolume, newCapacity);
            sellVolume = Arrays.copyOf(sellVolume, newCapacity);
        }
    }

    private void checkLevelIndex(int index) {
        if (index < 0 || index >= levelCount) {
            throw new IndexOutOfBoundsException("Level index: " + index + ", Count: " + levelCount);
        }
    }

    // ========== Raw array access for rendering ==========

    /**
     * Returns the raw price levels array. For rendering use only.
     */
    public float[] getPriceLevelsArray() {
        return priceLevels;
    }

    /**
     * Returns the raw buy volume array. For rendering use only.
     */
    public float[] getBuyVolumeArray() {
        return buyVolume;
    }

    /**
     * Returns the raw sell volume array. For rendering use only.
     */
    public float[] getSellVolumeArray() {
        return sellVolume;
    }

    // ========== Binary search for Series interface ==========

    @Override
    public int indexAtOrBefore(long timestamp) {
        // Volume profiles don't use timestamp-based indexing
        // Return last index if within time range
        if (timestamp >= startTime && timestamp <= endTime) {
            return levelCount > 0 ? levelCount - 1 : -1;
        }
        return -1;
    }

    @Override
    public int indexAtOrAfter(long timestamp) {
        // Volume profiles don't use timestamp-based indexing
        // Return first index if within time range
        if (timestamp >= startTime && timestamp <= endTime) {
            return levelCount > 0 ? 0 : -1;
        }
        return -1;
    }

    // ========== Visual style ==========

    public Color getBuyColor() {
        return buyColor;
    }

    public void setBuyColor(Color color) {
        this.buyColor = color;
    }

    public Color getSellColor() {
        return sellColor;
    }

    public void setSellColor(Color color) {
        this.sellColor = color;
    }

    public Color getPocColor() {
        return pocColor;
    }

    public void setPocColor(Color color) {
        this.pocColor = color;
    }

    public Color getValueAreaColor() {
        return valueAreaColor;
    }

    public void setValueAreaColor(Color color) {
        this.valueAreaColor = color;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    // ========== Listener management ==========

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
        return String.format("VolumeProfileSeries[id=%s, name=%s, levels=%d, tickSize=%.4f]",
                id, name, levelCount, tickSize);
    }
}
