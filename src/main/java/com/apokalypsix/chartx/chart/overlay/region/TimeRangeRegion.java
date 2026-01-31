package com.apokalypsix.chartx.chart.overlay.region;

import java.awt.Color;

/**
 * Represents a time range region with background coloring.
 *
 * <p>Regions are used to highlight specific time periods on the chart,
 * such as trading sessions, market hours, or significant events.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Highlight pre-market hours
 * TimeRangeRegion preMarket = new TimeRangeRegion(
 *     startTime, endTime,
 *     new Color(50, 50, 70, 80),  // Semi-transparent blue
 *     "Pre-market"
 * );
 * regionLayer.addRegion(preMarket);
 * }</pre>
 */
public class TimeRangeRegion {

    private final String id;
    private long startTime;
    private long endTime;
    private Color fillColor;
    private Color borderColor;
    private String label;
    private boolean visible = true;

    /**
     * Creates a region with the specified time range and fill color.
     *
     * @param startTime start timestamp in milliseconds
     * @param endTime end timestamp in milliseconds
     * @param fillColor background fill color (use alpha for transparency)
     */
    public TimeRangeRegion(long startTime, long endTime, Color fillColor) {
        this(generateId(), startTime, endTime, fillColor, null, null);
    }

    /**
     * Creates a region with the specified time range, fill color, and label.
     *
     * @param startTime start timestamp in milliseconds
     * @param endTime end timestamp in milliseconds
     * @param fillColor background fill color
     * @param label optional label to display
     */
    public TimeRangeRegion(long startTime, long endTime, Color fillColor, String label) {
        this(generateId(), startTime, endTime, fillColor, null, label);
    }

    /**
     * Creates a fully configured region.
     *
     * @param id unique identifier
     * @param startTime start timestamp
     * @param endTime end timestamp
     * @param fillColor background fill color
     * @param borderColor optional border color
     * @param label optional label
     */
    public TimeRangeRegion(String id, long startTime, long endTime,
                           Color fillColor, Color borderColor, String label) {
        if (endTime < startTime) {
            throw new IllegalArgumentException("End time must be >= start time");
        }
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.label = label;
    }

    private static String generateId() {
        return "region_" + System.nanoTime();
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDuration() {
        return endTime - startTime;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public String getLabel() {
        return label;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean hasBorder() {
        return borderColor != null;
    }

    public boolean hasLabel() {
        return label != null && !label.isEmpty();
    }

    // ========== Setters ==========

    public void setStartTime(long startTime) {
        if (startTime > this.endTime) {
            throw new IllegalArgumentException("Start time must be <= end time");
        }
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        if (endTime < this.startTime) {
            throw new IllegalArgumentException("End time must be >= start time");
        }
        this.endTime = endTime;
    }

    public void setTimeRange(long startTime, long endTime) {
        if (endTime < startTime) {
            throw new IllegalArgumentException("End time must be >= start time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // ========== Utility ==========

    /**
     * Returns true if this region overlaps with the given time range.
     */
    public boolean overlaps(long rangeStart, long rangeEnd) {
        return startTime < rangeEnd && endTime > rangeStart;
    }

    /**
     * Returns true if the given timestamp is within this region.
     */
    public boolean contains(long timestamp) {
        return timestamp >= startTime && timestamp <= endTime;
    }

    @Override
    public String toString() {
        return String.format("TimeRangeRegion[id=%s, time=%d-%d, label=%s]",
                id, startTime, endTime, label);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TimeRangeRegion other = (TimeRangeRegion) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
