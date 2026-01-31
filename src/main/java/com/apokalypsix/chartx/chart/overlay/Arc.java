package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * An arc drawing defined by center point, radius point, and arc extent.
 *
 * <p>The arc is defined by a center point and a point on the arc.
 * By default it draws a semicircle, but can be configured for different arc angles.
 */
public class Arc extends Drawing {

    private AnchorPoint center;
    private AnchorPoint radiusPoint;  // Point on the arc that defines radius

    private double startAngle = 0;    // In radians
    private double arcExtent = Math.PI;  // In radians (default: semicircle)

    public Arc(String id, long centerTimestamp, double centerPrice) {
        super(id);
        this.center = new AnchorPoint(centerTimestamp, centerPrice);
    }

    public Arc(String id, long centerTimestamp, double centerPrice,
               long radiusTimestamp, double radiusPrice) {
        super(id);
        this.center = new AnchorPoint(centerTimestamp, centerPrice);
        this.radiusPoint = new AnchorPoint(radiusTimestamp, radiusPrice);
    }

    @Override
    public Type getType() {
        return Type.ARC;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (center != null) points.add(center);
        if (radiusPoint != null) points.add(radiusPoint);
        return points;
    }

    @Override
    public boolean isComplete() {
        return center != null && radiusPoint != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> center = point;
            case 1 -> radiusPoint = point;
            default -> throw new IndexOutOfBoundsException("Arc has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    /**
     * Returns the radius in screen coordinates.
     */
    public double getRadius(CoordinateSystem coords) {
        if (!isComplete()) return 0;
        double cx = coords.xValueToScreenX(center.timestamp());
        double cy = coords.yValueToScreenY(center.price());
        double rx = coords.xValueToScreenX(radiusPoint.timestamp());
        double ry = coords.yValueToScreenY(radiusPoint.price());
        return Math.sqrt((rx - cx) * (rx - cx) + (ry - cy) * (ry - cy));
    }

    /**
     * Returns the angle from center to radius point.
     */
    public double getAngleToRadiusPoint(CoordinateSystem coords) {
        if (!isComplete()) return 0;
        double cx = coords.xValueToScreenX(center.timestamp());
        double cy = coords.yValueToScreenY(center.price());
        double rx = coords.xValueToScreenX(radiusPoint.timestamp());
        double ry = coords.yValueToScreenY(radiusPoint.price());
        return Math.atan2(ry - cy, rx - cx);
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double cx = coords.xValueToScreenX(center.timestamp());
        double cy = coords.yValueToScreenY(center.price());
        double radius = getRadius(coords);

        double dist = Math.sqrt((screenX - cx) * (screenX - cx) + (screenY - cy) * (screenY - cy));

        // Check if point is on the arc (within hit distance of the circle radius)
        if (Math.abs(dist - radius) > hitDistance) return false;

        // Check if point is within the arc angle range
        double angle = Math.atan2(screenY - cy, screenX - cx);
        double baseAngle = getAngleToRadiusPoint(coords);
        double actualStart = baseAngle + startAngle;

        // Normalize angles to [0, 2*PI)
        angle = normalizeAngle(angle);
        actualStart = normalizeAngle(actualStart);
        double actualEnd = normalizeAngle(actualStart + arcExtent);

        return isAngleInRange(angle, actualStart, arcExtent);
    }

    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    private boolean isAngleInRange(double angle, double start, double extent) {
        double end = start + extent;
        if (extent >= 0) {
            if (end > 2 * Math.PI) {
                return angle >= start || angle <= normalizeAngle(end);
            }
            return angle >= start && angle <= end;
        } else {
            if (end < 0) {
                return angle <= start || angle >= normalizeAngle(end);
            }
            return angle <= start && angle >= end;
        }
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double cx = coords.xValueToScreenX(center.timestamp());
        double cy = coords.yValueToScreenY(center.price());
        double rx = coords.xValueToScreenX(radiusPoint.timestamp());
        double ry = coords.yValueToScreenY(radiusPoint.price());

        if (isNearPoint(screenX, screenY, cx, cy, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, rx, ry, handleRadius)) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getCenter() { return center; }
    public AnchorPoint getRadiusPoint() { return radiusPoint; }
    public void setRadiusPoint(AnchorPoint radiusPoint) { this.radiusPoint = radiusPoint; }

    public double getStartAngle() { return startAngle; }
    public void setStartAngle(double startAngle) { this.startAngle = startAngle; }

    public double getArcExtent() { return arcExtent; }
    public void setArcExtent(double arcExtent) { this.arcExtent = arcExtent; }

    public void move(long deltaTime, double deltaPrice) {
        if (center != null) center = new AnchorPoint(center.timestamp() + deltaTime, center.price() + deltaPrice);
        if (radiusPoint != null) radiusPoint = new AnchorPoint(radiusPoint.timestamp() + deltaTime, radiusPoint.price() + deltaPrice);
    }
}
