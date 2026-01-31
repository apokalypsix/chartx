package com.apokalypsix.chartx.chart.overlay;

/**
 * Immutable anchor point for drawings, stored in data coordinates.
 *
 * <p>Using data coordinates (time/price) ensures drawings move correctly
 * during pan/zoom operations.
 *
 * @param timestamp time in epoch milliseconds
 * @param price the price/value at this anchor
 */
public record AnchorPoint(long timestamp, double price) {

    /**
     * Creates an anchor point with the specified coordinates.
     */
    public AnchorPoint {
        // Validation if needed
    }

    /**
     * Returns a new anchor point with a different timestamp.
     */
    public AnchorPoint withTimestamp(long newTimestamp) {
        return new AnchorPoint(newTimestamp, price);
    }

    /**
     * Returns a new anchor point with a different price.
     */
    public AnchorPoint withPrice(double newPrice) {
        return new AnchorPoint(timestamp, newPrice);
    }

    @Override
    public String toString() {
        return String.format("AnchorPoint[time=%d, price=%.4f]", timestamp, price);
    }
}
