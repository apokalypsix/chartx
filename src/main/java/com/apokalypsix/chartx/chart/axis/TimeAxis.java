package com.apokalypsix.chartx.chart.axis;

import java.time.format.DateTimeFormatter;

/**
 * Represents a time-based horizontal axis for charts.
 *
 * <p>The TimeAxis manages time range, auto-ranging behavior, and display options.
 * It provides a clean API for controlling the horizontal axis similar to how
 * {@link YAxis} controls the vertical axis.
 *
 * <p>Usage:
 * <pre>{@code
 * chart.getTimeAxis().setVisibleRange(startTime, endTime);
 * chart.getTimeAxis().setAutoRange(AutoRangeMode.ALWAYS);
 * chart.getTimeAxis().setGrowBy(0.02);  // 2% padding
 * }</pre>
 */
public class TimeAxis implements HorizontalAxis<Long> {

    /** Default axis ID for backward compatibility. */
    public static final String DEFAULT_AXIS_ID = "default";

    /** Position of the axis on the chart. */
    public enum Position {
        TOP,
        BOTTOM
    }

    /** Auto-range behavior modes. */
    public enum AutoRangeMode {
        /** Never auto-range - manual control only */
        NEVER,
        /** Auto-range once when data is set, then manual */
        ONCE,
        /** Always auto-range to fit visible data */
        ALWAYS
    }

    private final String id;
    private Position position;
    private long minTime;
    private long maxTime;
    private AutoRangeMode autoRangeMode;
    private boolean autoRangeApplied;  // For ONCE mode tracking
    private double growBy;  // Padding fraction (e.g., 0.02 = 2%)
    private boolean visible;
    private String title;
    private DateTimeFormatter labelFormat;

    /**
     * Creates a TimeAxis with the given ID and position.
     *
     * @param id       unique identifier for this axis
     * @param position where to display the axis (TOP or BOTTOM)
     */
    public TimeAxis(String id, Position position) {
        this.id = id;
        this.position = position;
        this.minTime = 0;
        this.maxTime = 1;
        this.autoRangeMode = AutoRangeMode.NEVER;
        this.autoRangeApplied = false;
        this.growBy = 0.0;
        this.visible = true;
        this.title = null;
        this.labelFormat = null;
    }

    /**
     * Creates a default TimeAxis at the bottom position.
     */
    public TimeAxis() {
        this(DEFAULT_AXIS_ID, Position.BOTTOM);
    }

    /**
     * Creates a copy of this TimeAxis with a new ID.
     *
     * @param newId the ID for the copy
     * @return a new TimeAxis with copied settings
     */
    public TimeAxis copy(String newId) {
        TimeAxis copy = new TimeAxis(newId, this.position);
        copy.minTime = this.minTime;
        copy.maxTime = this.maxTime;
        copy.autoRangeMode = this.autoRangeMode;
        copy.autoRangeApplied = this.autoRangeApplied;
        copy.growBy = this.growBy;
        copy.visible = this.visible;
        copy.title = this.title;
        copy.labelFormat = this.labelFormat;
        return copy;
    }

    // ========== Getters ==========

    /**
     * Returns the axis ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the internal axis position enum.
     *
     * @return the position as TimeAxis.Position
     */
    public Position getInternalPosition() {
        return position;
    }

    /**
     * Returns the minimum visible time.
     */
    public long getMinTime() {
        return minTime;
    }

    /**
     * Returns the maximum visible time.
     */
    public long getMaxTime() {
        return maxTime;
    }

    /**
     * Returns the visible time span (max - min).
     */
    public long getTimeSpan() {
        return maxTime - minTime;
    }

    /**
     * Returns the visible time range as a TimeRange object.
     */
    public TimeRange getVisibleRange() {
        return new TimeRange(minTime, maxTime);
    }

    /**
     * Returns the auto-range mode.
     */
    public AutoRangeMode getAutoRange() {
        return autoRangeMode;
    }

    /**
     * Returns true if auto-ranging is enabled (ONCE or ALWAYS mode).
     */
    public boolean isAutoRange() {
        return autoRangeMode != AutoRangeMode.NEVER;
    }

    /**
     * Returns the padding fraction added during auto-ranging.
     */
    public double getGrowBy() {
        return growBy;
    }

    /**
     * Returns whether the axis is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns the axis title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the label format for time display.
     */
    public DateTimeFormatter getLabelFormat() {
        return labelFormat;
    }

    // ========== Setters ==========

    /**
     * Sets the axis position.
     *
     * @param position TOP or BOTTOM
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Sets the visible time range.
     *
     * @param minTime minimum time (start)
     * @param maxTime maximum time (end)
     * @throws IllegalArgumentException if maxTime &lt; minTime
     */
    public void setVisibleRange(long minTime, long maxTime) {
        if (maxTime < minTime) {
            throw new IllegalArgumentException("Max time must be >= min time");
        }
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    /**
     * Sets the visible time range from a TimeRange object.
     *
     * @param range the time range
     */
    public void setVisibleRange(TimeRange range) {
        setVisibleRange(range.getStart(), range.getEnd());
    }

    /**
     * Sets the auto-range mode.
     *
     * @param mode the auto-range behavior (NEVER, ONCE, or ALWAYS)
     */
    public void setAutoRange(AutoRangeMode mode) {
        this.autoRangeMode = mode;
        if (mode == AutoRangeMode.ONCE) {
            this.autoRangeApplied = false;  // Reset for new ONCE cycle
        }
    }

    /**
     * Sets the padding fraction added during auto-ranging.
     *
     * @param fraction padding as a fraction (e.g., 0.02 = 2% padding on each end)
     */
    public void setGrowBy(double fraction) {
        this.growBy = Math.max(0, fraction);
    }

    /**
     * Sets axis visibility.
     *
     * @param visible true to show the axis
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Sets the axis title.
     *
     * @param title the title text, or null for no title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the label format for time display.
     *
     * @param format the DateTimeFormatter to use
     */
    public void setLabelFormat(DateTimeFormatter format) {
        this.labelFormat = format;
    }

    // ========== Auto-range Support ==========

    /**
     * Checks if auto-range should be applied based on current mode.
     *
     * @return true if auto-ranging should occur
     */
    public boolean shouldAutoRange() {
        return switch (autoRangeMode) {
            case NEVER -> false;
            case ONCE -> !autoRangeApplied;
            case ALWAYS -> true;
        };
    }

    /**
     * Marks auto-range as applied (for ONCE mode).
     */
    public void markAutoRangeApplied() {
        this.autoRangeApplied = true;
    }

    /**
     * Resets the auto-range applied flag (for ONCE mode).
     */
    public void resetAutoRange() {
        this.autoRangeApplied = false;
    }

    // ========== HorizontalAxis Implementation ==========

    /**
     * Returns the axis position as a HorizontalAxis.Position.
     *
     * @return the position (TOP or BOTTOM)
     */
    @Override
    public HorizontalAxis.Position getPosition() {
        return position == Position.TOP ? HorizontalAxis.Position.TOP : HorizontalAxis.Position.BOTTOM;
    }

    /**
     * Converts a timestamp to a normalized position in [0, 1) range.
     *
     * @param timestamp the timestamp to normalize
     * @return normalized position where 0 = minTime, 1 = maxTime
     */
    @Override
    public double toNormalized(Long timestamp) {
        return normalize(timestamp);
    }

    /**
     * Returns true since this is a time-based axis.
     *
     * @return true
     */
    @Override
    public boolean isTimeBased() {
        return true;
    }

    // ========== Utility Methods ==========

    /**
     * Normalizes a timestamp to the 0-1 range based on this axis's min/max.
     *
     * @param timestamp the timestamp to normalize
     * @return normalized value (0 = minTime, 1 = maxTime)
     */
    public double normalize(long timestamp) {
        long span = getTimeSpan();
        if (span == 0) {
            return 0.5;
        }
        return (double) (timestamp - minTime) / span;
    }

    /**
     * Interpolates from a normalized value (0-1) to a timestamp.
     *
     * @param normalized normalized position (0-1)
     * @return the corresponding timestamp
     */
    public long interpolate(double normalized) {
        return minTime + (long) (normalized * getTimeSpan());
    }

    /**
     * Expands the time range by the specified fraction on each end.
     *
     * @param fraction expansion fraction (e.g., 0.02 for 2%)
     */
    public void expandByFraction(double fraction) {
        long span = getTimeSpan();
        long expansion = (long) (span * fraction);
        this.minTime -= expansion;
        this.maxTime += expansion;
    }

    @Override
    public String toString() {
        return String.format("TimeAxis[id=%s, pos=%s, range=%d-%d, autoRange=%s, visible=%s]",
                id, position, minTime, maxTime, autoRangeMode, visible);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TimeAxis other = (TimeAxis) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
