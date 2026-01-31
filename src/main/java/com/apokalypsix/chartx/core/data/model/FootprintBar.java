package com.apokalypsix.chartx.core.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * Represents a single footprint bar containing volume data at each price level.
 *
 * <p>A footprint bar shows the bid/ask volume distribution for a specific
 * time period (e.g., one candle). Price levels are stored sorted by price.
 */
public class FootprintBar {

    private final long timestamp;
    private final float tickSize;

    // Price levels sorted by price
    private final TreeMap<Float, FootprintLevel> levels = new TreeMap<>();

    // Cached aggregates
    private float totalVolume = 0;
    private float totalDelta = 0;
    private int pocLevelIndex = -1;
    private boolean cacheValid = false;

    /**
     * Creates a new footprint bar.
     *
     * @param timestamp the bar timestamp
     * @param tickSize the price tick size for bucketing
     */
    public FootprintBar(long timestamp, float tickSize) {
        this.timestamp = timestamp;
        this.tickSize = tickSize;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getTickSize() {
        return tickSize;
    }

    /**
     * Adds volume at a price level.
     *
     * @param price the price level
     * @param bidVolume volume at bid
     * @param askVolume volume at ask
     */
    public void addVolume(float price, float bidVolume, float askVolume) {
        float alignedPrice = alignPrice(price);
        FootprintLevel level = levels.computeIfAbsent(alignedPrice, FootprintLevel::new);
        level.addBidVolume(bidVolume);
        level.addAskVolume(askVolume);
        invalidateCache();
    }

    /**
     * Adds bid volume at a price level.
     */
    public void addBidVolume(float price, float volume) {
        float alignedPrice = alignPrice(price);
        FootprintLevel level = levels.computeIfAbsent(alignedPrice, FootprintLevel::new);
        level.addBidVolume(volume);
        invalidateCache();
    }

    /**
     * Adds ask volume at a price level.
     */
    public void addAskVolume(float price, float volume) {
        float alignedPrice = alignPrice(price);
        FootprintLevel level = levels.computeIfAbsent(alignedPrice, FootprintLevel::new);
        level.addAskVolume(volume);
        invalidateCache();
    }

    /**
     * Aligns a price to the tick size grid.
     */
    private float alignPrice(float price) {
        return Math.round(price / tickSize) * tickSize;
    }

    private void invalidateCache() {
        cacheValid = false;
    }

    private void ensureCacheValid() {
        if (cacheValid) return;

        totalVolume = 0;
        totalDelta = 0;
        float maxVolume = Float.NEGATIVE_INFINITY;
        int index = 0;
        pocLevelIndex = -1;

        for (FootprintLevel level : levels.values()) {
            float vol = level.getTotalVolume();
            totalVolume += vol;
            totalDelta += level.getDelta();

            if (vol > maxVolume) {
                maxVolume = vol;
                pocLevelIndex = index;
            }
            index++;
        }

        cacheValid = true;
    }

    /**
     * Returns the number of price levels in this bar.
     */
    public int getLevelCount() {
        return levels.size();
    }

    /**
     * Returns the level at the given index (sorted by price ascending).
     */
    public FootprintLevel getLevel(int index) {
        if (index < 0 || index >= levels.size()) {
            return null;
        }
        int i = 0;
        for (FootprintLevel level : levels.values()) {
            if (i == index) return level;
            i++;
        }
        return null;
    }

    /**
     * Returns the level at the given price, or null if not present.
     */
    public FootprintLevel getLevelAtPrice(float price) {
        return levels.get(alignPrice(price));
    }

    /**
     * Returns all levels as a list (sorted by price ascending).
     */
    public List<FootprintLevel> getLevels() {
        return new ArrayList<>(levels.values());
    }

    /**
     * Returns all levels as an array (sorted by price ascending).
     */
    public FootprintLevel[] getLevelsArray() {
        return levels.values().toArray(new FootprintLevel[0]);
    }

    /**
     * Returns the total volume across all price levels.
     */
    public float getTotalVolume() {
        ensureCacheValid();
        return totalVolume;
    }

    /**
     * Returns the total delta (ask - bid) across all price levels.
     */
    public float getDelta() {
        ensureCacheValid();
        return totalDelta;
    }

    /**
     * Returns the Point of Control (price with highest volume).
     */
    public float getPOCPrice() {
        ensureCacheValid();
        if (pocLevelIndex < 0 || levels.isEmpty()) {
            return Float.NaN;
        }
        FootprintLevel level = getLevel(pocLevelIndex);
        return level != null ? level.getPrice() : Float.NaN;
    }

    /**
     * Returns the index of the POC level.
     */
    public int getPOCIndex() {
        ensureCacheValid();
        return pocLevelIndex;
    }

    /**
     * Returns the highest price level.
     */
    public float getHighPrice() {
        return levels.isEmpty() ? Float.NaN : levels.lastKey();
    }

    /**
     * Returns the lowest price level.
     */
    public float getLowPrice() {
        return levels.isEmpty() ? Float.NaN : levels.firstKey();
    }

    /**
     * Returns all imbalances in this bar.
     *
     * @param threshold the imbalance ratio threshold (e.g., 3.0 for 300%)
     */
    public List<Imbalance> getImbalances(float threshold) {
        List<Imbalance> result = new ArrayList<>();
        for (FootprintLevel level : levels.values()) {
            Imbalance imbalance = Imbalance.fromLevel(level, threshold);
            if (imbalance != null) {
                result.add(imbalance);
            }
        }
        return result;
    }

    /**
     * Returns stacked imbalances (consecutive price levels with same direction).
     *
     * @param threshold the imbalance ratio threshold
     * @param minStackSize minimum consecutive levels to qualify as stacked
     */
    public List<List<Imbalance>> getStackedImbalances(float threshold, int minStackSize) {
        List<List<Imbalance>> result = new ArrayList<>();
        List<Imbalance> currentStack = new ArrayList<>();
        Boolean lastDirection = null;

        for (FootprintLevel level : levels.values()) {
            Imbalance imbalance = Imbalance.fromLevel(level, threshold);

            if (imbalance != null) {
                boolean isBuy = imbalance.isBuyImbalance();

                if (lastDirection == null || lastDirection == isBuy) {
                    currentStack.add(imbalance);
                } else {
                    // Direction changed - check if previous stack qualifies
                    if (currentStack.size() >= minStackSize) {
                        result.add(new ArrayList<>(currentStack));
                    }
                    currentStack.clear();
                    currentStack.add(imbalance);
                }
                lastDirection = isBuy;
            } else {
                // No imbalance - check if current stack qualifies
                if (currentStack.size() >= minStackSize) {
                    result.add(new ArrayList<>(currentStack));
                }
                currentStack.clear();
                lastDirection = null;
            }
        }

        // Check final stack
        if (currentStack.size() >= minStackSize) {
            result.add(currentStack);
        }

        return result;
    }

    /**
     * Clears all volume data.
     */
    public void clear() {
        levels.clear();
        invalidateCache();
    }

    /**
     * Creates a copy of this bar.
     */
    public FootprintBar copy() {
        FootprintBar copy = new FootprintBar(timestamp, tickSize);
        for (FootprintLevel level : levels.values()) {
            copy.levels.put(level.getPrice(), level.copy());
        }
        return copy;
    }

    @Override
    public String toString() {
        return String.format("FootprintBar[ts=%d, levels=%d, vol=%.0f, delta=%.0f]",
                timestamp, levels.size(), getTotalVolume(), getDelta());
    }
}
