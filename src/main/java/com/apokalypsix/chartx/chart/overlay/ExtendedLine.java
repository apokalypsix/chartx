package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * An extended line drawing that passes through two points and extends infinitely in both directions.
 *
 * <p>Unlike a trend line which has a defined start and end, an extended line
 * uses two points to define its direction but extends to the edges of the chart.
 */
public class ExtendedLine extends Drawing {

    private AnchorPoint point1;
    private AnchorPoint point2;

    /**
     * Creates an extended line with only the first point.
     * Used during interactive creation.
     *
     * @param id unique identifier
     * @param timestamp1 first point time
     * @param price1 first point price
     */
    public ExtendedLine(String id, long timestamp1, double price1) {
        super(id);
        this.point1 = new AnchorPoint(timestamp1, price1);
        this.point2 = null;
    }

    /**
     * Creates a complete extended line.
     *
     * @param id unique identifier
     * @param timestamp1 first point time
     * @param price1 first point price
     * @param timestamp2 second point time
     * @param price2 second point price
     */
    public ExtendedLine(String id, long timestamp1, double price1,
                        long timestamp2, double price2) {
        super(id);
        this.point1 = new AnchorPoint(timestamp1, price1);
        this.point2 = new AnchorPoint(timestamp2, price2);
    }

    @Override
    public Type getType() {
        return Type.EXTENDED_LINE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (point1 != null) {
            points.add(point1);
        }
        if (point2 != null) {
            points.add(point2);
        }
        return points;
    }

    @Override
    public boolean isComplete() {
        return point1 != null && point2 != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> point1 = point;
            case 1 -> point2 = point;
            default -> throw new IndexOutOfBoundsException("ExtendedLine has only 2 anchor points, index: " + index);
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) {
            return false;
        }

        double x1 = coords.xValueToScreenX(point1.timestamp());
        double y1 = coords.yValueToScreenY(point1.price());
        double x2 = coords.xValueToScreenX(point2.timestamp());
        double y2 = coords.yValueToScreenY(point2.price());

        // Use infinite line distance
        double distance = distanceToLine(screenX, screenY, x1, y1, x2, y2);
        return distance <= hitDistance;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double x1 = coords.xValueToScreenX(point1.timestamp());
        double y1 = coords.yValueToScreenY(point1.price());
        double x2 = coords.xValueToScreenX(point2.timestamp());
        double y2 = coords.yValueToScreenY(point2.price());

        // Check point1 handle
        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) {
            return HandleType.ANCHOR_0;
        }

        // Check point2 handle
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) {
            return HandleType.ANCHOR_1;
        }

        // Check if on the line (for body dragging)
        if (containsPoint(screenX, screenY, coords, handleRadius)) {
            return HandleType.BODY;
        }

        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        double dx = px - x;
        double dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    // ========== Getters and Setters ==========

    /**
     * Returns the first anchor point.
     */
    public AnchorPoint getPoint1() {
        return point1;
    }

    /**
     * Returns the second anchor point.
     */
    public AnchorPoint getPoint2() {
        return point2;
    }

    /**
     * Sets the second anchor point.
     */
    public void setPoint2(AnchorPoint point2) {
        this.point2 = point2;
    }

    /**
     * Returns the slope of the line in price per millisecond.
     */
    public double getSlope() {
        if (!isComplete()) {
            return 0;
        }
        long deltaTime = point2.timestamp() - point1.timestamp();
        if (deltaTime == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (point2.price() - point1.price()) / deltaTime;
    }

    /**
     * Returns the price at a given timestamp along this line.
     */
    public double getPriceAt(long timestamp) {
        if (!isComplete()) {
            return Double.NaN;
        }
        double slope = getSlope();
        return point1.price() + slope * (timestamp - point1.timestamp());
    }

    /**
     * Moves the entire line by the specified delta.
     *
     * @param deltaTime time offset in milliseconds
     * @param deltaPrice price offset
     */
    public void move(long deltaTime, double deltaPrice) {
        if (point1 != null) {
            point1 = new AnchorPoint(point1.timestamp() + deltaTime, point1.price() + deltaPrice);
        }
        if (point2 != null) {
            point2 = new AnchorPoint(point2.timestamp() + deltaTime, point2.price() + deltaPrice);
        }
    }
}
