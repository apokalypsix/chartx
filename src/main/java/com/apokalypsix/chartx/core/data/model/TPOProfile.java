package com.apokalypsix.chartx.core.data.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Represents a single Time Price Opportunity (TPO) profile for a trading session.
 *
 * <p>A TPO profile shows which prices were touched during each time period
 * of a session, typically represented as letters (A, B, C, etc.) or blocks.
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li>POC (Point of Control): Price with the most TPOs</li>
 *   <li>Value Area: Price range containing ~70% of TPOs</li>
 *   <li>Initial Balance: First hour's range (periods A + B typically)</li>
 *   <li>Single Prints: Prices touched by only one TPO period</li>
 * </ul>
 */
public class TPOProfile {

    private final long sessionStart;
    private final long sessionEnd;
    private final float tickSize;
    private final long tpoPeriodMillis;

    // Price levels sorted by price
    // Each level has a bit mask indicating which periods touched it
    // For unlimited periods, use List<Set<Integer>>
    private final TreeMap<Float, Long> levelTpoMask = new TreeMap<>();

    // Period tracking
    private int periodCount = 0;
    private static final int MAX_PERIODS = 64; // Limited by long bitmask

    // Session OHLC
    private float openPrice = Float.NaN;
    private float highOfDay = Float.NEGATIVE_INFINITY;
    private float lowOfDay = Float.POSITIVE_INFINITY;
    private float closePrice = Float.NaN;

    // Initial Balance (first hour typically)
    private float ibHigh = Float.NaN;
    private float ibLow = Float.NaN;
    private int ibPeriods = 2; // Number of periods in initial balance

    // Cached computed values
    private float poc = Float.NaN;
    private float vah = Float.NaN;
    private float val = Float.NaN;
    private int pocTpoCount = 0;
    private boolean cacheValid = false;

    /**
     * Creates a new TPO profile for a session.
     *
     * @param sessionStart session start timestamp
     * @param sessionEnd session end timestamp
     * @param tickSize price tick size
     * @param tpoPeriodMillis TPO period duration (e.g., 30 minutes)
     */
    public TPOProfile(long sessionStart, long sessionEnd, float tickSize, long tpoPeriodMillis) {
        this.sessionStart = sessionStart;
        this.sessionEnd = sessionEnd;
        this.tickSize = tickSize;
        this.tpoPeriodMillis = tpoPeriodMillis;
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public long getSessionEnd() {
        return sessionEnd;
    }

    public float getTickSize() {
        return tickSize;
    }

    public long getTpoPeriodMillis() {
        return tpoPeriodMillis;
    }

    // ========== Building the profile ==========

    /**
     * Adds a price touch for a specific period.
     *
     * @param price the price touched
     * @param periodIndex the period index (0 = A, 1 = B, etc.)
     */
    public void addTPO(float price, int periodIndex) {
        if (periodIndex >= MAX_PERIODS) {
            throw new IllegalArgumentException("Period index exceeds maximum: " + periodIndex);
        }

        float alignedPrice = alignPrice(price);
        long mask = levelTpoMask.getOrDefault(alignedPrice, 0L);
        mask |= (1L << periodIndex);
        levelTpoMask.put(alignedPrice, mask);

        // Update period count
        if (periodIndex >= periodCount) {
            periodCount = periodIndex + 1;
        }

        // Update day's range
        if (alignedPrice > highOfDay) {
            highOfDay = alignedPrice;
        }
        if (alignedPrice < lowOfDay) {
            lowOfDay = alignedPrice;
        }

        invalidateCache();
    }

    /**
     * Adds multiple TPOs for a price range touched during a period.
     *
     * @param highPrice the high of the period
     * @param lowPrice the low of the period
     * @param periodIndex the period index
     */
    public void addTPORange(float highPrice, float lowPrice, int periodIndex) {
        float high = alignPrice(highPrice);
        float low = alignPrice(lowPrice);

        for (float price = low; price <= high; price += tickSize) {
            addTPO(price, periodIndex);
        }
    }

    /**
     * Sets the opening price.
     */
    public void setOpenPrice(float price) {
        this.openPrice = price;
    }

    /**
     * Sets the closing price.
     */
    public void setClosePrice(float price) {
        this.closePrice = price;
    }

    /**
     * Sets the initial balance (IB) range.
     *
     * @param ibHigh IB high price
     * @param ibLow IB low price
     */
    public void setInitialBalance(float ibHigh, float ibLow) {
        this.ibHigh = ibHigh;
        this.ibLow = ibLow;
    }

    /**
     * Sets the number of periods that make up the initial balance.
     */
    public void setIBPeriods(int periods) {
        this.ibPeriods = periods;
    }

    /**
     * Returns the number of periods that make up the initial balance.
     */
    public int getIBPeriods() {
        return ibPeriods;
    }

    private float alignPrice(float price) {
        return Math.round(price / tickSize) * tickSize;
    }

    private void invalidateCache() {
        cacheValid = false;
    }

    // ========== Computed values ==========

    private void ensureCacheValid() {
        if (cacheValid) return;

        // Find POC (price with most TPOs)
        poc = Float.NaN;
        pocTpoCount = 0;

        for (var entry : levelTpoMask.entrySet()) {
            int count = Long.bitCount(entry.getValue());
            if (count > pocTpoCount) {
                pocTpoCount = count;
                poc = entry.getKey();
            }
        }

        // Calculate Value Area (70% of TPOs)
        calculateValueArea();

        cacheValid = true;
    }

    private void calculateValueArea() {
        if (levelTpoMask.isEmpty() || Float.isNaN(poc)) {
            vah = Float.NaN;
            val = Float.NaN;
            return;
        }

        // Total TPO count
        int totalTpos = 0;
        for (long mask : levelTpoMask.values()) {
            totalTpos += Long.bitCount(mask);
        }

        // Target is 70% of total
        int targetTpos = (int) (totalTpos * 0.7);

        // Start from POC and expand outward
        float currentHigh = poc;
        float currentLow = poc;
        int currentTpos = Long.bitCount(levelTpoMask.get(poc));

        while (currentTpos < targetTpos) {
            // Get TPO counts at next levels up and down
            Float nextUp = levelTpoMask.higherKey(currentHigh);
            Float nextDown = levelTpoMask.lowerKey(currentLow);

            int upCount = (nextUp != null) ? Long.bitCount(levelTpoMask.get(nextUp)) : 0;
            int downCount = (nextDown != null) ? Long.bitCount(levelTpoMask.get(nextDown)) : 0;

            if (upCount == 0 && downCount == 0) {
                break;
            }

            // Expand toward higher TPO count
            if (upCount >= downCount && nextUp != null) {
                currentHigh = nextUp;
                currentTpos += upCount;
            } else if (nextDown != null) {
                currentLow = nextDown;
                currentTpos += downCount;
            } else if (nextUp != null) {
                currentHigh = nextUp;
                currentTpos += upCount;
            }
        }

        vah = currentHigh;
        val = currentLow;
    }

    // ========== Access methods ==========

    /**
     * Returns the number of price levels in the profile.
     */
    public int getLevelCount() {
        return levelTpoMask.size();
    }

    /**
     * Returns all price levels (sorted ascending).
     */
    public List<Float> getPriceLevels() {
        return new ArrayList<>(levelTpoMask.keySet());
    }

    /**
     * Returns the TPO count at a price level.
     */
    public int getTPOCountAt(float price) {
        Long mask = levelTpoMask.get(alignPrice(price));
        return mask != null ? Long.bitCount(mask) : 0;
    }

    /**
     * Returns the TPO mask at a price level.
     */
    public long getTPOMaskAt(float price) {
        return levelTpoMask.getOrDefault(alignPrice(price), 0L);
    }

    /**
     * Returns the TPO letter for a period index (A=0, B=1, etc.).
     */
    public static char getTPOLetter(int periodIndex) {
        if (periodIndex < 26) {
            return (char) ('A' + periodIndex);
        } else if (periodIndex < 52) {
            return (char) ('a' + (periodIndex - 26));
        } else {
            return '?';
        }
    }

    /**
     * Returns which periods touched a price level.
     */
    public List<Integer> getPeriodsAt(float price) {
        List<Integer> periods = new ArrayList<>();
        long mask = getTPOMaskAt(price);
        for (int i = 0; i < periodCount; i++) {
            if ((mask & (1L << i)) != 0) {
                periods.add(i);
            }
        }
        return periods;
    }

    /**
     * Returns the TPO letters at a price level.
     */
    public String getTPOLettersAt(float price) {
        StringBuilder sb = new StringBuilder();
        long mask = getTPOMaskAt(price);
        for (int i = 0; i < periodCount; i++) {
            if ((mask & (1L << i)) != 0) {
                sb.append(getTPOLetter(i));
            }
        }
        return sb.toString();
    }

    /**
     * Returns the Point of Control price.
     */
    public float getPOC() {
        ensureCacheValid();
        return poc;
    }

    /**
     * Returns the POC TPO count.
     */
    public int getPOCCount() {
        ensureCacheValid();
        return pocTpoCount;
    }

    /**
     * Returns the Value Area High.
     */
    public float getValueAreaHigh() {
        ensureCacheValid();
        return vah;
    }

    /**
     * Returns the Value Area Low.
     */
    public float getValueAreaLow() {
        ensureCacheValid();
        return val;
    }

    public float getOpenPrice() {
        return openPrice;
    }

    public float getClosePrice() {
        return closePrice;
    }

    public float getHighOfDay() {
        return highOfDay;
    }

    public float getLowOfDay() {
        return lowOfDay;
    }

    public float getIBHigh() {
        return ibHigh;
    }

    public float getIBLow() {
        return ibLow;
    }

    public int getPeriodCount() {
        return periodCount;
    }

    /**
     * Returns prices that have only one TPO (single prints).
     */
    public List<Float> getSinglePrints() {
        List<Float> singles = new ArrayList<>();
        for (var entry : levelTpoMask.entrySet()) {
            if (Long.bitCount(entry.getValue()) == 1) {
                singles.add(entry.getKey());
            }
        }
        return singles;
    }

    /**
     * Returns true if the price is within the value area.
     */
    public boolean isInValueArea(float price) {
        ensureCacheValid();
        float aligned = alignPrice(price);
        return aligned >= val && aligned <= vah;
    }

    /**
     * Returns true if the price is within the initial balance.
     */
    public boolean isInInitialBalance(float price) {
        if (Float.isNaN(ibHigh) || Float.isNaN(ibLow)) {
            return false;
        }
        float aligned = alignPrice(price);
        return aligned >= ibLow && aligned <= ibHigh;
    }

    @Override
    public String toString() {
        return String.format("TPOProfile[start=%d, levels=%d, POC=%.2f, VA=%.2f-%.2f]",
                sessionStart, getLevelCount(), poc, val, vah);
    }
}
