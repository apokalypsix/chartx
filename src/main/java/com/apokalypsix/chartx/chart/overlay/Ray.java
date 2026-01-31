package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * A ray drawing that starts at one point and extends infinitely in one direction.
 *
 * <p>A ray is defined by a start point and a direction point. The line
 * extends from the start point through the direction point to infinity.
 */
public class Ray extends Drawing {

    private AnchorPoint start;
    private AnchorPoint direction;  // Point defining direction (ray extends through this)

    /**
     * Creates a ray with only the start point.
     * Used during interactive creation.
     *
     * @param id unique identifier
     * @param startTimestamp start time
     * @param startPrice start price
     */
    public Ray(String id, long startTimestamp, double startPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.direction = null;
    }

    /**
     * Creates a complete ray.
     *
     * @param id unique identifier
     * @param startTimestamp start time
     * @param startPrice start price
     * @param directionTimestamp direction point time
     * @param directionPrice direction point price
     */
    public Ray(String id, long startTimestamp, double startPrice,
               long directionTimestamp, double directionPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.direction = new AnchorPoint(directionTimestamp, directionPrice);
    }

    @Override
    public Type getType() {
        return Type.RAY;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (start != null) {
            points.add(start);
        }
        if (direction != null) {
            points.add(direction);
        }
        return points;
    }

    @Override
    public boolean isComplete() {
        return start != null && direction != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> start = point;
            case 1 -> direction = point;
            default -> throw new IndexOutOfBoundsException("Ray has only 2 anchor points, index: " + index);
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

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(direction.timestamp());
        double y2 = coords.yValueToScreenY(direction.price());

        // Calculate distance to infinite ray
        double distance = distanceToRay(screenX, screenY, x1, y1, x2, y2);
        return distance <= hitDistance;
    }

    /**
     * Calculates distance from a point to a ray.
     * The ray starts at (x1, y1) and extends through (x2, y2) to infinity.
     */
    private double distanceToRay(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0) {
            return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // Project point onto ray, but only consider t >= 0 (ray starts at start point)
        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0, t);  // Clamp to ray (starts at 0, extends to infinity)

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(direction.timestamp());
        double y2 = coords.yValueToScreenY(direction.price());

        // Check start handle
        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) {
            return HandleType.ANCHOR_0;
        }

        // Check direction handle
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) {
            return HandleType.ANCHOR_1;
        }

        // Check if on the ray (for body dragging)
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
     * Returns the start anchor point.
     */
    public AnchorPoint getStart() {
        return start;
    }

    /**
     * Returns the direction anchor point.
     */
    public AnchorPoint getDirection() {
        return direction;
    }

    /**
     * Sets the direction anchor point.
     */
    public void setDirection(AnchorPoint direction) {
        this.direction = direction;
    }

    /**
     * Moves the entire ray by the specified delta.
     *
     * @param deltaTime time offset in milliseconds
     * @param deltaPrice price offset
     */
    public void move(long deltaTime, double deltaPrice) {
        if (start != null) {
            start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        }
        if (direction != null) {
            direction = new AnchorPoint(direction.timestamp() + deltaTime, direction.price() + deltaPrice);
        }
    }
}
