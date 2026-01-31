package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Fibonacci Fan drawing that displays rays at Fibonacci ratio angles.
 *
 * <p>Fan lines extend from the start point through Fibonacci ratio points
 * along the vertical range defined by start and end points.
 */
public class FibonacciFan extends Drawing {

    public static final float[] DEFAULT_LEVELS = {0f, 0.236f, 0.382f, 0.5f, 0.618f, 0.786f, 1.0f};

    private AnchorPoint start;
    private AnchorPoint end;

    private float[] levels = DEFAULT_LEVELS;
    private boolean showLabels = true;
    private Color[] levelColors;

    public FibonacciFan(String id, long timestamp, double price) {
        super(id);
        this.start = new AnchorPoint(timestamp, price);
        initDefaultColors();
    }

    public FibonacciFan(String id, long startTimestamp, double startPrice,
                         long endTimestamp, double endPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.end = new AnchorPoint(endTimestamp, endPrice);
        initDefaultColors();
    }

    private void initDefaultColors() {
        levelColors = new Color[DEFAULT_LEVELS.length];
        levelColors[0] = new Color(128, 128, 128);
        levelColors[1] = new Color(255, 82, 82);
        levelColors[2] = new Color(76, 175, 80);
        levelColors[3] = new Color(33, 150, 243);
        levelColors[4] = new Color(255, 152, 0);
        levelColors[5] = new Color(156, 39, 176);
        levelColors[6] = new Color(128, 128, 128);
    }

    @Override
    public Type getType() {
        return Type.FIBONACCI_FAN;
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
            default -> throw new IndexOutOfBoundsException("FibonacciFan has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    /**
     * Returns the price at a given level (0.0 to 1.0).
     */
    public double getPriceAtLevel(float level) {
        if (!isComplete()) return Double.NaN;
        double priceRange = end.price() - start.price();
        return start.price() + priceRange * level;
    }

    /**
     * Returns the endpoint for a fan line at the given level.
     */
    public AnchorPoint getFanEndpoint(float level) {
        if (!isComplete()) return null;
        double price = getPriceAtLevel(level);
        return new AnchorPoint(end.timestamp(), price);
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());

        for (float level : levels) {
            double price = getPriceAtLevel(level);
            double x2 = coords.xValueToScreenX(end.timestamp());
            double y2 = coords.yValueToScreenY(price);

            // Check distance to ray from start through (x2, y2)
            if (distanceToRay(screenX, screenY, x1, y1, x2, y2) <= hitDistance) return true;
        }
        return false;
    }

    private double distanceToRay(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));

        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0, t);  // Ray extends from start

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
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
