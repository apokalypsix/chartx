package com.apokalypsix.chartx.chart.data;

/**
 * Value object representing a single point in a band series.
 *
 * <p>Contains upper, middle, and lower band values at a timestamp.
 * Designed for reuse in rendering loops to avoid allocation.
 */
public class BandPoint {

    private long timestamp;
    private float upper;
    private float middle;
    private float lower;

    /**
     * Creates an empty band point.
     */
    public BandPoint() {
    }

    /**
     * Creates a band point with the specified values.
     */
    public BandPoint(long timestamp, float upper, float middle, float lower) {
        this.timestamp = timestamp;
        this.upper = upper;
        this.middle = middle;
        this.lower = lower;
    }

    /**
     * Sets all values. For reuse in rendering loops.
     */
    public void set(long timestamp, float upper, float middle, float lower) {
        this.timestamp = timestamp;
        this.upper = upper;
        this.middle = middle;
        this.lower = lower;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getUpper() {
        return upper;
    }

    public float getMiddle() {
        return middle;
    }

    public float getLower() {
        return lower;
    }

    /**
     * Returns the band width (upper - lower).
     */
    public float getBandWidth() {
        return upper - lower;
    }

    @Override
    public String toString() {
        return String.format("BandPoint[time=%d, upper=%.2f, middle=%.2f, lower=%.2f]",
                timestamp, upper, middle, lower);
    }
}
