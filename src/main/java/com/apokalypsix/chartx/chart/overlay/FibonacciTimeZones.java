package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Fibonacci Time Zones drawing that displays vertical lines at Fibonacci intervals.
 *
 * <p>Time zones are placed at Fibonacci number intervals (1, 1, 2, 3, 5, 8, 13, 21, ...)
 * from the starting point, helping identify potential trend reversal times.
 */
public class FibonacciTimeZones extends Drawing {

    public static final int[] DEFAULT_INTERVALS = {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89};

    private AnchorPoint start;
    private AnchorPoint unitEnd;  // Defines the unit interval

    private int[] intervals = DEFAULT_INTERVALS;
    private boolean showLabels = true;
    private Color[] lineColors;

    public FibonacciTimeZones(String id, long timestamp, double price) {
        super(id);
        this.start = new AnchorPoint(timestamp, price);
        initDefaultColors();
    }

    public FibonacciTimeZones(String id, long startTimestamp, double startPrice,
                               long unitEndTimestamp, double unitEndPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.unitEnd = new AnchorPoint(unitEndTimestamp, unitEndPrice);
        initDefaultColors();
    }

    private void initDefaultColors() {
        lineColors = new Color[DEFAULT_INTERVALS.length];
        for (int i = 0; i < lineColors.length; i++) {
            lineColors[i] = new Color(156, 39, 176, 180);  // Purple with alpha
        }
    }

    @Override
    public Type getType() {
        return Type.FIBONACCI_TIME_ZONES;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (start != null) points.add(start);
        if (unitEnd != null) points.add(unitEnd);
        return points;
    }

    @Override
    public boolean isComplete() {
        return start != null && unitEnd != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> start = point;
            case 1 -> unitEnd = point;
            default -> throw new IndexOutOfBoundsException("FibonacciTimeZones has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    /**
     * Returns the unit interval duration in milliseconds.
     */
    public long getUnitInterval() {
        if (!isComplete()) return 0;
        return unitEnd.timestamp() - start.timestamp();
    }

    /**
     * Returns the timestamp at a given Fibonacci interval index.
     */
    public long getTimestampAtInterval(int intervalIndex) {
        if (!isComplete() || intervalIndex >= intervals.length) return 0;
        long unit = getUnitInterval();
        int cumulativeFib = 0;
        for (int i = 0; i <= intervalIndex; i++) {
            cumulativeFib += intervals[i];
        }
        return start.timestamp() + unit * cumulativeFib;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        long unit = getUnitInterval();
        int cumulative = 0;
        for (int interval : intervals) {
            cumulative += interval;
            long timestamp = start.timestamp() + unit * cumulative;
            double x = coords.xValueToScreenX(timestamp);
            if (Math.abs(screenX - x) <= hitDistance) return true;
        }
        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(unitEnd.timestamp());
        double y2 = coords.yValueToScreenY(unitEnd.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getStart() { return start; }
    public AnchorPoint getUnitEnd() { return unitEnd; }
    public void setUnitEnd(AnchorPoint unitEnd) { this.unitEnd = unitEnd; }

    public int[] getIntervals() { return intervals; }
    public void setIntervals(int[] intervals) { this.intervals = intervals; }

    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }

    public Color getLineColor(int index) {
        if (lineColors != null && index < lineColors.length) return lineColors[index];
        return getColor();
    }

    public void setLineColor(int index, Color color) {
        if (lineColors == null) lineColors = new Color[intervals.length];
        if (index < lineColors.length) lineColors[index] = color;
    }

    public void move(long deltaTime, double deltaPrice) {
        if (start != null) start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        if (unitEnd != null) unitEnd = new AnchorPoint(unitEnd.timestamp() + deltaTime, unitEnd.price() + deltaPrice);
    }
}
