package com.apokalypsix.chartx.core.data.model;

/**
 * Represents volume data at a single price level within a footprint bar.
 *
 * <p>Each level stores:
 * <ul>
 *   <li>Price - the price level</li>
 *   <li>Bid volume - volume traded at bid (sellers hitting bids)</li>
 *   <li>Ask volume - volume traded at ask (buyers lifting offers)</li>
 * </ul>
 *
 * <p>The delta (ask - bid) indicates buying/selling pressure at this level.
 */
public class FootprintLevel {

    private final float price;
    private float bidVolume;
    private float askVolume;

    /**
     * Creates a new footprint level.
     *
     * @param price the price level
     */
    public FootprintLevel(float price) {
        this.price = price;
        this.bidVolume = 0;
        this.askVolume = 0;
    }

    /**
     * Creates a new footprint level with initial volumes.
     *
     * @param price the price level
     * @param bidVolume initial bid volume
     * @param askVolume initial ask volume
     */
    public FootprintLevel(float price, float bidVolume, float askVolume) {
        this.price = price;
        this.bidVolume = bidVolume;
        this.askVolume = askVolume;
    }

    public float getPrice() {
        return price;
    }

    public float getBidVolume() {
        return bidVolume;
    }

    public void setBidVolume(float bidVolume) {
        this.bidVolume = bidVolume;
    }

    public void addBidVolume(float volume) {
        this.bidVolume += volume;
    }

    public float getAskVolume() {
        return askVolume;
    }

    public void setAskVolume(float askVolume) {
        this.askVolume = askVolume;
    }

    public void addAskVolume(float volume) {
        this.askVolume += volume;
    }

    /**
     * Returns the delta (ask - bid) at this level.
     * Positive = more buying, Negative = more selling.
     */
    public float getDelta() {
        return askVolume - bidVolume;
    }

    /**
     * Returns the total volume at this level (bid + ask).
     */
    public float getTotalVolume() {
        return bidVolume + askVolume;
    }

    /**
     * Returns true if there's a buy imbalance (ask >> bid).
     *
     * @param threshold the ratio threshold (e.g., 3.0 for 300%)
     */
    public boolean isBuyImbalance(float threshold) {
        return bidVolume > 0 && askVolume / bidVolume >= threshold;
    }

    /**
     * Returns true if there's a sell imbalance (bid >> ask).
     *
     * @param threshold the ratio threshold (e.g., 3.0 for 300%)
     */
    public boolean isSellImbalance(float threshold) {
        return askVolume > 0 && bidVolume / askVolume >= threshold;
    }

    /**
     * Returns the imbalance ratio (ask/bid or bid/ask, whichever is larger).
     * Returns 1.0 if either side is zero.
     */
    public float getImbalanceRatio() {
        if (bidVolume == 0 || askVolume == 0) {
            return 1.0f;
        }
        return Math.max(askVolume / bidVolume, bidVolume / askVolume);
    }

    /**
     * Creates a copy of this level.
     */
    public FootprintLevel copy() {
        return new FootprintLevel(price, bidVolume, askVolume);
    }

    @Override
    public String toString() {
        return String.format("%.0f x %.0f @ %.2f", bidVolume, askVolume, price);
    }
}
