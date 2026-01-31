package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * An ellipse drawing defined by two corner anchor points (bounding box).
 *
 * <p>Ellipses are useful for highlighting circular or oval price zones.
 */
public class Ellipse extends Drawing {

    private AnchorPoint corner1;
    private AnchorPoint corner2;

    private boolean filled = true;
    private Color fillColor = new Color(100, 149, 237, 50);

    public Ellipse(String id, long timestamp1, double price1) {
        super(id);
        this.corner1 = new AnchorPoint(timestamp1, price1);
        this.corner2 = null;
    }

    public Ellipse(String id, long timestamp1, double price1, long timestamp2, double price2) {
        super(id);
        this.corner1 = new AnchorPoint(timestamp1, price1);
        this.corner2 = new AnchorPoint(timestamp2, price2);
    }

    @Override
    public Type getType() {
        return Type.ELLIPSE;
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
            default -> throw new IndexOutOfBoundsException("Ellipse has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(corner1.timestamp());
        double y1 = coords.yValueToScreenY(corner1.price());
        double x2 = coords.xValueToScreenX(corner2.timestamp());
        double y2 = coords.yValueToScreenY(corner2.price());

        double cx = (x1 + x2) / 2;
        double cy = (y1 + y2) / 2;
        double rx = Math.abs(x2 - x1) / 2;
        double ry = Math.abs(y2 - y1) / 2;

        if (rx == 0 || ry == 0) return false;

        double dx = (screenX - cx) / rx;
        double dy = (screenY - cy) / ry;
        double dist = dx * dx + dy * dy;

        if (filled) {
            return dist <= 1.0;
        }
        return Math.abs(Math.sqrt(dist) - 1.0) * Math.min(rx, ry) <= hitDistance;
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

    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }

    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }

    public void move(long deltaTime, double deltaPrice) {
        if (corner1 != null) corner1 = new AnchorPoint(corner1.timestamp() + deltaTime, corner1.price() + deltaPrice);
        if (corner2 != null) corner2 = new AnchorPoint(corner2.timestamp() + deltaTime, corner2.price() + deltaPrice);
    }
}
