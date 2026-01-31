package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * A polyline drawing consisting of multiple connected line segments.
 *
 * <p>Polylines can have any number of points (minimum 2) and are useful
 * for drawing complex paths or marking multiple price levels.
 */
public class Polyline extends Drawing {

    private final List<AnchorPoint> points = new ArrayList<>();
    private boolean closed = false;  // If true, connects last point to first

    public Polyline(String id, long timestamp, double price) {
        super(id);
        points.add(new AnchorPoint(timestamp, price));
    }

    public Polyline(String id, List<AnchorPoint> initialPoints) {
        super(id);
        if (initialPoints != null) {
            points.addAll(initialPoints);
        }
    }

    @Override
    public Type getType() {
        return Type.POLYLINE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        return new ArrayList<>(points);
    }

    @Override
    public boolean isComplete() {
        return points.size() >= 2;
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
        return 2;  // Minimum required
    }

    /**
     * Adds a new point to the polyline.
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
        if (points.size() > 2) {  // Keep minimum 2 points
            points.remove(index);
        }
    }

    /**
     * Returns the number of points in this polyline.
     */
    public int getPointCount() {
        return points.size();
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (points.size() < 2) return false;

        for (int i = 0; i < points.size() - 1; i++) {
            AnchorPoint p1 = points.get(i);
            AnchorPoint p2 = points.get(i + 1);

            double x1 = coords.xValueToScreenX(p1.timestamp());
            double y1 = coords.yValueToScreenY(p1.price());
            double x2 = coords.xValueToScreenX(p2.timestamp());
            double y2 = coords.yValueToScreenY(p2.price());

            if (distanceToLineSegment(screenX, screenY, x1, y1, x2, y2) <= hitDistance) {
                return true;
            }
        }

        // Check closing segment if closed
        if (closed && points.size() > 2) {
            AnchorPoint first = points.get(0);
            AnchorPoint last = points.get(points.size() - 1);

            double x1 = coords.xValueToScreenX(last.timestamp());
            double y1 = coords.yValueToScreenY(last.price());
            double x2 = coords.xValueToScreenX(first.timestamp());
            double y2 = coords.yValueToScreenY(first.price());

            if (distanceToLineSegment(screenX, screenY, x1, y1, x2, y2) <= hitDistance) {
                return true;
            }
        }

        return false;
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
                // Return appropriate handle type based on index
                return switch (i) {
                    case 0 -> HandleType.ANCHOR_0;
                    case 1 -> HandleType.ANCHOR_1;
                    case 2 -> HandleType.ANCHOR_2;
                    case 3 -> HandleType.ANCHOR_3;
                    default -> HandleType.BODY;  // For points beyond 3, use BODY
                };
            }
        }

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public void move(long deltaTime, double deltaPrice) {
        for (int i = 0; i < points.size(); i++) {
            AnchorPoint p = points.get(i);
            points.set(i, new AnchorPoint(p.timestamp() + deltaTime, p.price() + deltaPrice));
        }
    }
}
