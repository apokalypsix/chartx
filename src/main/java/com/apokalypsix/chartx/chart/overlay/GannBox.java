package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A Gann Box drawing that divides price/time into proportional segments.
 *
 * <p>The Gann Box creates a grid of horizontal and vertical lines
 * at specific ratios (typically Gann or Fibonacci ratios) within a bounding box.
 */
public class GannBox extends Drawing {

    public static final float[] DEFAULT_LEVELS = {0f, 0.25f, 0.333f, 0.5f, 0.667f, 0.75f, 1.0f};

    private AnchorPoint corner1;
    private AnchorPoint corner2;

    private float[] priceLevels = DEFAULT_LEVELS;
    private float[] timeLevels = DEFAULT_LEVELS;
    private boolean showDiagonals = true;
    private boolean filled = false;
    private Color fillColor = new Color(100, 149, 237, 20);
    private Color gridColor = new Color(128, 128, 128, 128);

    public GannBox(String id, long timestamp, double price) {
        super(id);
        this.corner1 = new AnchorPoint(timestamp, price);
    }

    public GannBox(String id, long t1, double p1, long t2, double p2) {
        super(id);
        this.corner1 = new AnchorPoint(t1, p1);
        this.corner2 = new AnchorPoint(t2, p2);
    }

    @Override
    public Type getType() {
        return Type.GANN_BOX;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (corner1 != null) points.add(corner1);
        if (corner2 != null) points.add(corner2);
        return points;
    }

    @Override
    public boolean isComplete() {
        return corner1 != null && corner2 != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> corner1 = point;
            case 1 -> corner2 = point;
            default -> throw new IndexOutOfBoundsException("GannBox has only 2 anchor points");
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
        double minPrice = Math.min(corner1.price(), corner2.price());
        double maxPrice = Math.max(corner1.price(), corner2.price());
        return minPrice + (maxPrice - minPrice) * level;
    }

    /**
     * Returns the timestamp at a given level (0.0 to 1.0).
     */
    public long getTimeAtLevel(float level) {
        if (!isComplete()) return 0;
        long minTime = Math.min(corner1.timestamp(), corner2.timestamp());
        long maxTime = Math.max(corner1.timestamp(), corner2.timestamp());
        return minTime + (long) ((maxTime - minTime) * level);
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(corner1.timestamp());
        double y1 = coords.yValueToScreenY(corner1.price());
        double x2 = coords.xValueToScreenX(corner2.timestamp());
        double y2 = coords.yValueToScreenY(corner2.price());

        double left = Math.min(x1, x2);
        double right = Math.max(x1, x2);
        double top = Math.min(y1, y2);
        double bottom = Math.max(y1, y2);

        // Check if inside the box
        if (filled && screenX >= left && screenX <= right && screenY >= top && screenY <= bottom) {
            return true;
        }

        // Check border lines
        if (distanceToLineSegment(screenX, screenY, left, top, right, top) <= hitDistance) return true;
        if (distanceToLineSegment(screenX, screenY, right, top, right, bottom) <= hitDistance) return true;
        if (distanceToLineSegment(screenX, screenY, right, bottom, left, bottom) <= hitDistance) return true;
        if (distanceToLineSegment(screenX, screenY, left, bottom, left, top) <= hitDistance) return true;

        // Check grid lines
        for (float level : priceLevels) {
            double py = top + (bottom - top) * level;
            if (distanceToLineSegment(screenX, screenY, left, py, right, py) <= hitDistance) return true;
        }
        for (float level : timeLevels) {
            double px = left + (right - left) * level;
            if (distanceToLineSegment(screenX, screenY, px, top, px, bottom) <= hitDistance) return true;
        }

        // Check diagonals
        if (showDiagonals) {
            if (distanceToLineSegment(screenX, screenY, left, top, right, bottom) <= hitDistance) return true;
            if (distanceToLineSegment(screenX, screenY, left, bottom, right, top) <= hitDistance) return true;
        }

        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(corner1.timestamp());
        double y1 = coords.yValueToScreenY(corner1.price());
        double x2 = coords.xValueToScreenX(corner2.timestamp());
        double y2 = coords.yValueToScreenY(corner2.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;

        // Check opposite corners for resizing
        if (isNearPoint(screenX, screenY, x1, y2, handleRadius)) return HandleType.ANCHOR_2;
        if (isNearPoint(screenX, screenY, x2, y1, handleRadius)) return HandleType.ANCHOR_3;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getCorner1() { return corner1; }
    public AnchorPoint getCorner2() { return corner2; }
    public void setCorner2(AnchorPoint corner2) { this.corner2 = corner2; }

    public float[] getPriceLevels() { return priceLevels; }
    public void setPriceLevels(float[] priceLevels) { this.priceLevels = priceLevels; }

    public float[] getTimeLevels() { return timeLevels; }
    public void setTimeLevels(float[] timeLevels) { this.timeLevels = timeLevels; }

    public boolean isShowDiagonals() { return showDiagonals; }
    public void setShowDiagonals(boolean showDiagonals) { this.showDiagonals = showDiagonals; }

    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }

    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }

    public Color getGridColor() { return gridColor; }
    public void setGridColor(Color gridColor) { this.gridColor = gridColor; }

    public void move(long deltaTime, double deltaPrice) {
        if (corner1 != null) corner1 = new AnchorPoint(corner1.timestamp() + deltaTime, corner1.price() + deltaPrice);
        if (corner2 != null) corner2 = new AnchorPoint(corner2.timestamp() + deltaTime, corner2.price() + deltaPrice);
    }
}
