package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Andrews' Pitchfork drawing defined by three points.
 *
 * <p>The first point is the pivot, and the second and third points define
 * a trend that the pitchfork forks from. Lines extend from the pivot through
 * the midpoint of points 2-3, and parallel lines through points 2 and 3.
 */
public class Pitchfork extends Drawing {

    private AnchorPoint point1; // Pivot point
    private AnchorPoint point2; // First swing point
    private AnchorPoint point3; // Second swing point

    public Pitchfork(String id, long timestamp1, double price1) {
        super(id);
        this.point1 = new AnchorPoint(timestamp1, price1);
    }

    public Pitchfork(String id, long t1, double p1, long t2, double p2, long t3, double p3) {
        super(id);
        this.point1 = new AnchorPoint(t1, p1);
        this.point2 = new AnchorPoint(t2, p2);
        this.point3 = new AnchorPoint(t3, p3);
    }

    @Override
    public Type getType() {
        return Type.PITCHFORK;
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
            default -> throw new IndexOutOfBoundsException("Pitchfork has only 3 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 3;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(point1.timestamp());
        double y1 = coords.yValueToScreenY(point1.price());
        double x2 = coords.xValueToScreenX(point2.timestamp());
        double y2 = coords.yValueToScreenY(point2.price());
        double x3 = coords.xValueToScreenX(point3.timestamp());
        double y3 = coords.yValueToScreenY(point3.price());

        // Midpoint of p2-p3
        double mx = (x2 + x3) / 2;
        double my = (y2 + y3) / 2;

        // Check distance to median line (p1 to midpoint, extended)
        if (distanceToLine(screenX, screenY, x1, y1, mx, my) <= hitDistance) return true;

        // Check distance to upper and lower fork lines
        double dx = mx - x1;
        double dy = my - y1;
        if (distanceToLine(screenX, screenY, x2, y2, x2 + dx, y2 + dy) <= hitDistance) return true;
        if (distanceToLine(screenX, screenY, x3, y3, x3 + dx, y3 + dy) <= hitDistance) return true;

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

    public void move(long deltaTime, double deltaPrice) {
        if (point1 != null) point1 = new AnchorPoint(point1.timestamp() + deltaTime, point1.price() + deltaPrice);
        if (point2 != null) point2 = new AnchorPoint(point2.timestamp() + deltaTime, point2.price() + deltaPrice);
        if (point3 != null) point3 = new AnchorPoint(point3.timestamp() + deltaTime, point3.price() + deltaPrice);
    }
}
