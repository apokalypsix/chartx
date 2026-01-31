package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A filled polygon drawing consisting of multiple connected points.
 *
 * <p>Polygons are always closed shapes and can be filled with a color.
 * Useful for marking zones or areas on the chart.
 */
public class Polygon extends Drawing {

    private final List<AnchorPoint> points = new ArrayList<>();

    private boolean filled = true;
    private Color fillColor = new Color(100, 149, 237, 50);
    private Color borderColor = null;  // If null, uses drawing color

    public Polygon(String id, long timestamp, double price) {
        super(id);
        points.add(new AnchorPoint(timestamp, price));
    }

    public Polygon(String id, List<AnchorPoint> initialPoints) {
        super(id);
        if (initialPoints != null) {
            points.addAll(initialPoints);
        }
    }

    @Override
    public Type getType() {
        return Type.POLYGON;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        return new ArrayList<>(points);
    }

    @Override
    public boolean isComplete() {
        return points.size() >= 3;  // Minimum 3 points for a polygon
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        if (index < 0 || index >= points.size()) {
            throw new IndexOutOfBoundsException("Invalid anchor index: " + index);
        }
        points.set(index, point);
    }

    @Override
    public int getRequiredAnchorCount() {
        return 3;  // Minimum required
    }

    /**
     * Adds a new point to the polygon.
     */
    public void addPoint(AnchorPoint point) {
        points.add(point);
    }

    /**
     * Adds a new point at the specified index.
     */
    public void addPoint(int index, AnchorPoint point) {
        points.add(index, point);
    }

    /**
     * Removes the point at the specified index.
     */
    public void removePoint(int index) {
        if (points.size() > 3) {  // Keep minimum 3 points
            points.remove(index);
        }
    }

    /**
     * Returns the number of points in this polygon.
     */
    public int getPointCount() {
        return points.size();
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (points.size() < 3) return false;

        // Check if point is inside the polygon using ray casting
        if (filled && isPointInsidePolygon(screenX, screenY, coords)) {
            return true;
        }

        // Check distance to edges
        for (int i = 0; i < points.size(); i++) {
            AnchorPoint p1 = points.get(i);
            AnchorPoint p2 = points.get((i + 1) % points.size());

            double x1 = coords.xValueToScreenX(p1.timestamp());
            double y1 = coords.yValueToScreenY(p1.price());
            double x2 = coords.xValueToScreenX(p2.timestamp());
            double y2 = coords.yValueToScreenY(p2.price());

            if (distanceToLineSegment(screenX, screenY, x1, y1, x2, y2) <= hitDistance) {
                return true;
            }
        }

        return false;
    }

    private boolean isPointInsidePolygon(int px, int py, CoordinateSystem coords) {
        int count = 0;
        int n = points.size();

        for (int i = 0; i < n; i++) {
            AnchorPoint p1 = points.get(i);
            AnchorPoint p2 = points.get((i + 1) % n);

            double x1 = coords.xValueToScreenX(p1.timestamp());
            double y1 = coords.yValueToScreenY(p1.price());
            double x2 = coords.xValueToScreenX(p2.timestamp());
            double y2 = coords.yValueToScreenY(p2.price());

            if ((y1 <= py && py < y2) || (y2 <= py && py < y1)) {
                double xIntersect = x1 + (py - y1) / (y2 - y1) * (x2 - x1);
                if (px < xIntersect) {
                    count++;
                }
            }
        }

        return count % 2 == 1;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (points.isEmpty()) return HandleType.NONE;

        // Check each anchor point
        for (int i = 0; i < points.size(); i++) {
            AnchorPoint p = points.get(i);
            double x = coords.xValueToScreenX(p.timestamp());
            double y = coords.yValueToScreenY(p.price());

            if (isNearPoint(screenX, screenY, x, y, handleRadius)) {
                return switch (i) {
                    case 0 -> HandleType.ANCHOR_0;
                    case 1 -> HandleType.ANCHOR_1;
                    case 2 -> HandleType.ANCHOR_2;
                    case 3 -> HandleType.ANCHOR_3;
                    default -> HandleType.BODY;
                };
            }
        }

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }

    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }

    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color borderColor) { this.borderColor = borderColor; }

    public void move(long deltaTime, double deltaPrice) {
        for (int i = 0; i < points.size(); i++) {
            AnchorPoint p = points.get(i);
            points.set(i, new AnchorPoint(p.timestamp() + deltaTime, p.price() + deltaPrice));
        }
    }
}
