package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Fibonacci Arc drawing that displays arcs at Fibonacci ratio distances.
 *
 * <p>Arcs are centered at the end point with radii based on Fibonacci ratios
 * of the distance between start and end points.
 */
public class FibonacciArc extends Drawing {

    public static final float[] DEFAULT_LEVELS = {0.236f, 0.382f, 0.5f, 0.618f, 0.786f, 1.0f};

    private AnchorPoint start;
    private AnchorPoint end;

    private float[] levels = DEFAULT_LEVELS;
    private boolean showLabels = true;
    private Color[] levelColors;

    public FibonacciArc(String id, long timestamp, double price) {
        super(id);
        this.start = new AnchorPoint(timestamp, price);
        initDefaultColors();
    }

    public FibonacciArc(String id, long startTimestamp, double startPrice,
                         long endTimestamp, double endPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.end = new AnchorPoint(endTimestamp, endPrice);
        initDefaultColors();
    }

    private void initDefaultColors() {
        levelColors = new Color[DEFAULT_LEVELS.length];
        levelColors[0] = new Color(255, 82, 82);
        levelColors[1] = new Color(76, 175, 80);
        levelColors[2] = new Color(33, 150, 243);
        levelColors[3] = new Color(255, 152, 0);
        levelColors[4] = new Color(156, 39, 176);
        levelColors[5] = new Color(128, 128, 128);
    }

    @Override
    public Type getType() {
        return Type.FIBONACCI_ARC;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (start != null) points.add(start);
        if (end != null) points.add(end);
        return points;
    }

    @Override
    public boolean isComplete() {
        return start != null && end != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> start = point;
            case 1 -> end = point;
            default -> throw new IndexOutOfBoundsException("FibonacciArc has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    /**
     * Returns the base radius (distance from start to end in screen coords).
     */
    public double getBaseRadius(CoordinateSystem coords) {
        if (!isComplete()) return 0;
        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    /**
     * Returns the radius for a given Fibonacci level.
     */
    public double getRadiusAtLevel(float level, CoordinateSystem coords) {
        return getBaseRadius(coords) * level;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double cx = coords.xValueToScreenX(end.timestamp());
        double cy = coords.yValueToScreenY(end.price());
        double baseRadius = getBaseRadius(coords);

        double distFromCenter = Math.sqrt((screenX - cx) * (screenX - cx) + (screenY - cy) * (screenY - cy));

        for (float level : levels) {
            double arcRadius = baseRadius * level;
            if (Math.abs(distFromCenter - arcRadius) <= hitDistance) return true;
        }
        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getStart() { return start; }
    public AnchorPoint getEnd() { return end; }
    public void setEnd(AnchorPoint end) { this.end = end; }

    public float[] getLevels() { return levels; }
    public void setLevels(float[] levels) { this.levels = levels; }

    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }

    public Color getLevelColor(int index) {
        if (levelColors != null && index < levelColors.length) return levelColors[index];
        return getColor();
    }

    public void setLevelColor(int index, Color color) {
        if (levelColors == null) levelColors = new Color[levels.length];
        if (index < levelColors.length) levelColors[index] = color;
    }

    public void move(long deltaTime, double deltaPrice) {
        if (start != null) start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        if (end != null) end = new AnchorPoint(end.timestamp() + deltaTime, end.price() + deltaPrice);
    }
}
