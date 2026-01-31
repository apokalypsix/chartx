package com.apokalypsix.chartx.core.data.model;

/**
 * Represents a volume imbalance at a specific price level.
 *
 * <p>An imbalance occurs when the ratio between ask and bid volume
 * (or vice versa) exceeds a threshold, indicating strong buying
 * or selling pressure.
 */
public class Imbalance {

    private final float price;
    private final boolean buyImbalance;
    private final float ratio;
    private final float bidVolume;
    private final float askVolume;

    /**
     * Creates a new imbalance.
     *
     * @param price the price level
     * @param buyImbalance true if this is a buy imbalance (ask >> bid)
     * @param ratio the imbalance ratio
     * @param bidVolume the bid volume
     * @param askVolume the ask volume
     */
    public Imbalance(float price, boolean buyImbalance, float ratio,
                     float bidVolume, float askVolume) {
        this.price = price;
        this.buyImbalance = buyImbalance;
        this.ratio = ratio;
        this.bidVolume = bidVolume;
        this.askVolume = askVolume;
    }

    /**
     * Creates an imbalance from a footprint level.
     */
    public static Imbalance fromLevel(FootprintLevel level, float threshold) {
        if (level.isBuyImbalance(threshold)) {
            return new Imbalance(
                    level.getPrice(),
                    true,
                    level.getAskVolume() / level.getBidVolume(),
                    level.getBidVolume(),
                    level.getAskVolume()
            );
        } else if (level.isSellImbalance(threshold)) {
            return new Imbalance(
                    level.getPrice(),
                    false,
                    level.getBidVolume() / level.getAskVolume(),
                    level.getBidVolume(),
                    level.getAskVolume()
            );
        }
        return null;
    }

    public float getPrice() {
        return price;
    }

    /**
     * Returns true if this is a buy imbalance (ask >> bid).
     * Returns false if this is a sell imbalance (bid >> ask).
     */
    public boolean isBuyImbalance() {
        return buyImbalance;
    }

    /**
     * Returns true if this is a sell imbalance (bid >> ask).
     */
    public boolean isSellImbalance() {
        return !buyImbalance;
    }

    /**
     * Returns the imbalance ratio.
     */
    public float getRatio() {
        return ratio;
    }

    public float getBidVolume() {
        return bidVolume;
    }

    public float getAskVolume() {
        return askVolume;
    }

    @Override
    public String toString() {
        return String.format("Imbalance[%s, %.2f @ %.2f, ratio=%.1f]",
                buyImbalance ? "BUY" : "SELL", bidVolume, askVolume, ratio);
    }
}
