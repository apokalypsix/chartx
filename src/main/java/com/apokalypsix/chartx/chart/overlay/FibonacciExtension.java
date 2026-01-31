package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A Fibonacci extension drawing tool for projecting price targets.
 *
 * <p>Fibonacci extensions use three points: the trend start, trend end,
 * and retracement point to project extension levels beyond the original move.
 */
public class FibonacciExtension extends Drawing {

    public static final float[] DEFAULT_LEVELS = {0f, 0.618f, 1.0f, 1.272f, 1.618f, 2.0f, 2.618f};

    private AnchorPoint point1; // Trend start
    private AnchorPoint point2; // Trend end
    private AnchorPoint point3; // Retracement point

    private float[] levels = DEFAULT_LEVELS;
    private boolean showLabels = true;
    private boolean extendLines = false;
    private Color[] levelColors;

    public FibonacciExtension(String id, long timestamp, double price) {
        super(id);
        this.point1 = new AnchorPoint(timestamp, price);
        initDefaultColors();
    }

    public FibonacciExtension(String id, long t1, double p1, long t2, double p2, long t3, double p3) {
        super(id);
        this.point1 = new AnchorPoint(t1, p1);
        this.point2 = new AnchorPoint(t2, p2);
        this.point3 = new AnchorPoint(t3, p3);
        initDefaultColors();
    }

    private void initDefaultColors() {
        levelColors = new Color[DEFAULT_LEVELS.length];
        levelColors[0] = new Color(128, 128, 128);
        levelColors[1] = new Color(76, 175, 80);
        levelColors[2] = new Color(33, 150, 243);
        levelColors[3] = new Color(156, 39, 176);
        levelColors[4] = new Color(255, 152, 0);
        levelColors[5] = new Color(244, 67, 54);
        levelColors[6] = new Color(255, 235, 59);
    }

    @Override
    public Type getType() {
        return Type.FIBONACCI_EXTENSION;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(3);
        if (point1 != null) points.add(point1);
        if (point2 != null) points.add(point2);
        if (point3 != null) points.add(point3);
        return points;
    }

    @Override
    public boolean isComplete() {
        return point1 != null && point2 != null && point3 != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> point1 = point;
            case 1 -> point2 = point;
            case 2 -> point3 = point;
            default -> throw new IndexOutOfBoundsException("FibonacciExtension has only 3 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 3;
    }

    public double getPriceAtLevel(double level) {
        if (!isComplete()) return Double.NaN;
        double trendRange = point2.price() - point1.price();
        return point3.price() + trendRange * level;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        for (float level : levels) {
            double price = getPriceAtLevel(level);
            double levelY = coords.yValueToScreenY(price);
            if (Math.abs(screenY - levelY) <= hitDistance) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(point1.timestamp());
        double y1 = coords.yValueToScreenY(point1.price());
        double x2 = coords.xValueToScreenX(point2.timestamp());
        double y2 = coords.yValueToScreenY(point2.price());
        double x3 = coords.xValueToScreenX(point3.timestamp());
        double y3 = coords.yValueToScreenY(point3.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;
        if (isNearPoint(screenX, screenY, x3, y3, handleRadius)) return HandleType.ANCHOR_2;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getPoint1() { return point1; }
    public AnchorPoint getPoint2() { return point2; }
    public AnchorPoint getPoint3() { return point3; }

    public void setPoint2(AnchorPoint point2) { this.point2 = point2; }
    public void setPoint3(AnchorPoint point3) { this.point3 = point3; }

    public float[] getLevels() { return levels; }
    public void setLevels(float[] levels) { this.levels = levels; }

    public Color getLevelColor(int index) {
        if (levelColors != null && index < levelColors.length) return levelColors[index];
        return getColor();
    }

    public void setLevelColor(int index, Color color) {
        if (levelColors == null) levelColors = new Color[levels.length];
        if (index < levelColors.length) levelColors[index] = color;
    }

    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }

    public boolean isExtendLines() { return extendLines; }
    public void setExtendLines(boolean extendLines) { this.extendLines = extendLines; }

    public void move(long deltaTime, double deltaPrice) {
        if (point1 != null) point1 = new AnchorPoint(point1.timestamp() + deltaTime, point1.price() + deltaPrice);
        if (point2 != null) point2 = new AnchorPoint(point2.timestamp() + deltaTime, point2.price() + deltaPrice);
        if (point3 != null) point3 = new AnchorPoint(point3.timestamp() + deltaTime, point3.price() + deltaPrice);
    }
}
