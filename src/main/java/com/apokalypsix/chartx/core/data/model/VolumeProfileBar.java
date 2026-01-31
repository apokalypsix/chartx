package com.apokalypsix.chartx.core.data.model;

/**
 * Value object representing a single price level in a volume profile.
 *
 * <p>This class is designed for reuse in rendering loops to avoid
 * object allocation. Use the {@link #set} method to update values.
 */
public class VolumeProfileBar {

    private float price;
    private float buyVolume;
    private float sellVolume;

    /**
     * Creates an empty volume profile bar.
     */
    public VolumeProfileBar() {
    }

    /**
     * Creates a volume profile bar with the specified values.
     */
    public VolumeProfileBar(float price, float buyVolume, float sellVolume) {
        this.price = price;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
    }

    /**
     * Sets all values. For reuse in rendering loops.
     */
    public void set(float price, float buyVolume, float sellVolume) {
        this.price = price;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
    }

    /**
     * Returns the price level.
     */
    public float getPrice() {
        return price;
    }

    /**
     * Returns the buy volume at this level.
     */
    public float getBuyVolume() {
        return buyVolume;
    }

    /**
     * Returns the sell volume at this level.
     */
    public float getSellVolume() {
        return sellVolume;
    }

    /**
     * Returns the total volume (buy + sell).
     */
    public float getTotalVolume() {
        return buyVolume + sellVolume;
    }

    /**
     * Returns the delta (buy - sell).
     */
    public float getDelta() {
        return buyVolume - sellVolume;
    }

    /**
     * Returns the percentage of buy volume (0.0 to 1.0).
     */
    public float getBuyPercentage() {
        float total = getTotalVolume();
        return total > 0 ? buyVolume / total : 0.5f;
    }

    @Override
    public String toString() {
        return String.format("VolumeProfileBar[price=%.2f, buy=%.0f, sell=%.0f, total=%.0f]",
                price, buyVolume, sellVolume, getTotalVolume());
    }
}
