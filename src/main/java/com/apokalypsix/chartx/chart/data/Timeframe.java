package com.apokalypsix.chartx.chart.data;

/**
 * Standard trading timeframes for chart aggregation.
 *
 * <p>Each timeframe represents a specific time interval for grouping OHLC bars.
 * The library supports aggregating from lower timeframes to higher timeframes
 * (e.g., 1-minute bars to 5-minute bars).
 */
public enum Timeframe {

    /** 1-minute bars */
    M1(60_000L, "1m", "1 Minute"),

    /** 5-minute bars */
    M5(5 * 60_000L, "5m", "5 Minutes"),

    /** 15-minute bars */
    M15(15 * 60_000L, "15m", "15 Minutes"),

    /** 30-minute bars */
    M30(30 * 60_000L, "30m", "30 Minutes"),

    /** 1-hour bars */
    H1(60 * 60_000L, "1H", "1 Hour"),

    /** 4-hour bars */
    H4(4 * 60 * 60_000L, "4H", "4 Hours"),

    /** Daily bars */
    D1(24 * 60 * 60_000L, "D", "Daily"),

    /** Weekly bars */
    W1(7 * 24 * 60 * 60_000L, "W", "Weekly");

    /** Duration in milliseconds */
    public final long millis;

    /** Short display label (e.g., "1m", "5m", "1H") */
    public final String label;

    /** Full display name */
    public final String displayName;

    Timeframe(long millis, String label, String displayName) {
        this.millis = millis;
        this.label = label;
        this.displayName = displayName;
    }

    /**
     * Returns the timeframe duration in seconds.
     */
    public long getSeconds() {
        return millis / 1000L;
    }

    /**
     * Returns the timeframe duration in minutes.
     */
    public double getMinutes() {
        return millis / 60_000.0;
    }

    /**
     * Checks if this timeframe can be aggregated from the given source timeframe.
     *
     * @param source the source timeframe
     * @return true if aggregation is possible (target >= source and evenly divisible)
     */
    public boolean canAggregateFrom(Timeframe source) {
        if (this.millis < source.millis) {
            return false;
        }
        // Check if this timeframe is evenly divisible by source
        return this.millis % source.millis == 0;
    }

    /**
     * Returns the number of source bars that make up one bar of this timeframe.
     *
     * @param source the source timeframe
     * @return the ratio, or -1 if aggregation is not possible
     */
    public int getBarsPerPeriod(Timeframe source) {
        if (!canAggregateFrom(source)) {
            return -1;
        }
        return (int) (this.millis / source.millis);
    }

    /**
     * Aligns a timestamp to the start of a period for this timeframe.
     *
     * @param timestamp the timestamp to align
     * @return the aligned timestamp (start of period)
     */
    public long alignTimestamp(long timestamp) {
        return (timestamp / millis) * millis;
    }

    /**
     * Returns the next period start after the given timestamp.
     *
     * @param timestamp the current timestamp
     * @return the start of the next period
     */
    public long nextPeriodStart(long timestamp) {
        return alignTimestamp(timestamp) + millis;
    }

    /**
     * Returns true if the given timestamp is at the start of a period.
     *
     * @param timestamp the timestamp to check
     * @return true if at period boundary
     */
    public boolean isAtPeriodStart(long timestamp) {
        return timestamp % millis == 0;
    }

    /**
     * Finds the timeframe that best matches the given duration in milliseconds.
     *
     * @param millis the duration to match
     * @return the closest matching timeframe
     */
    public static Timeframe fromMillis(long millis) {
        Timeframe best = M1;
        long bestDiff = Math.abs(best.millis - millis);

        for (Timeframe tf : values()) {
            long diff = Math.abs(tf.millis - millis);
            if (diff < bestDiff) {
                best = tf;
                bestDiff = diff;
            }
        }
        return best;
    }

    /**
     * Parses a timeframe from a string label (e.g., "1m", "5m", "1H", "D").
     *
     * @param label the label to parse
     * @return the matching timeframe, or null if not found
     */
    public static Timeframe fromLabel(String label) {
        if (label == null) {
            return null;
        }
        String normalized = label.trim().toUpperCase();
        for (Timeframe tf : values()) {
            if (tf.label.equalsIgnoreCase(normalized) ||
                tf.name().equalsIgnoreCase(normalized)) {
                return tf;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
