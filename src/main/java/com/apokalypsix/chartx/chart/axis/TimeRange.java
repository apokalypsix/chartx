package com.apokalypsix.chartx.chart.axis;

/**
 * Immutable value object representing a time interval.
 * All times are in epoch milliseconds.
 */
public final class TimeRange {

    private final long start;
    private final long end;

    /**
     * Creates a time range.
     *
     * @param start start time (inclusive, epoch milliseconds)
     * @param end end time (inclusive, epoch milliseconds)
     * @throws IllegalArgumentException if end is before start
     */
    public TimeRange(long start, long end) {
        if (end < start) {
            throw new IllegalArgumentException("End time must be >= start time: start=" + start + ", end=" + end);
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a time range of the specified duration starting at the given time.
     */
    public static TimeRange ofDuration(long start, long durationMillis) {
        return new TimeRange(start, start + durationMillis);
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    /**
     * Returns the duration of this range in milliseconds.
     */
    public long getDuration() {
        return end - start;
    }

    /**
     * Returns true if the given timestamp falls within this range (inclusive).
     */
    public boolean contains(long timestamp) {
        return timestamp >= start && timestamp <= end;
    }

    /**
     * Returns true if this range overlaps with another range.
     */
    public boolean overlaps(TimeRange other) {
        return this.start <= other.end && this.end >= other.start;
    }

    /**
     * Returns a new range expanded by the given amount on both sides.
     */
    public TimeRange expand(long amount) {
        return new TimeRange(start - amount, end + amount);
    }

    /**
     * Returns a new range shifted by the given offset.
     */
    public TimeRange shift(long offset) {
        return new TimeRange(start + offset, end + offset);
    }

    /**
     * Returns the midpoint of this range.
     */
    public long getMidpoint() {
        return start + (end - start) / 2;
    }

    /**
     * Returns a normalized position (0.0 to 1.0) for the given timestamp within this range.
     */
    public double normalize(long timestamp) {
        if (start == end) {
            return 0.5;
        }
        return (double) (timestamp - start) / (end - start);
    }

    /**
     * Interpolates a timestamp from a normalized position (0.0 to 1.0).
     */
    public long interpolate(double normalizedPosition) {
        return start + (long) (normalizedPosition * (end - start));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeRange)) return false;
        TimeRange that = (TimeRange) o;
        return start == that.start && end == that.end;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(start) * 31 + Long.hashCode(end);
    }

    @Override
    public String toString() {
        return String.format("TimeRange[%d, %d]", start, end);
    }
}
