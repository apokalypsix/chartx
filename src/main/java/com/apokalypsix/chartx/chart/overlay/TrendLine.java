package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * A trend line drawing between two anchor points.
 *
 * <p>Trend lines can optionally be extended beyond their anchor points
 * in either direction (to the left, to the right, or both).
 */
public class TrendLine extends Drawing {

    private AnchorPoint start;
    private AnchorPoint end;

    // Extension options
    private boolean extendLeft = false;
    private boolean extendRight = false;

    /**
     * Creates a new trend line with only the starting anchor point.
     * Used during interactive creation.
     *
     * @param id unique identifier
     * @param startTimestamp start time in epoch milliseconds
     * @param startPrice start price
     */
    public TrendLine(String id, long startTimestamp, double startPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.end = null;
    }

    /**
     * Creates a complete trend line with both anchor points.
     *
     * @param id unique identifier
     * @param startTimestamp start time in epoch milliseconds
     * @param startPrice start price
     * @param endTimestamp end time in epoch milliseconds
     * @param endPrice end price
     */
    public TrendLine(String id, long startTimestamp, double startPrice,
                     long endTimestamp, double endPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.end = new AnchorPoint(endTimestamp, endPrice);
    }

    @Override
    public Type getType() {
        return Type.TREND_LINE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (start != null) {
            points.add(start);
        }
        if (end != null) {
            points.add(end);
        }
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
            default -> throw new IndexOutOfBoundsException("TrendLine has only 2 anchor points, index: " + index);
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
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        double distance;
        if (extendLeft || extendRight) {
            // Use infinite line distance
            distance = distanceToLine(screenX, screenY, x1, y1, x2, y2);
        } else {
            // Use segment distance
            distance = distanceToLineSegment(screenX, screenY, x1, y1, x2, y2);
        }

        return distance <= hitDistance;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        // Check start handle
        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) {
            return HandleType.ANCHOR_0;
        }

        // Check end handle
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
     * Returns the start anchor point.
     */
    public AnchorPoint getStart() {
        return start;
    }

    /**
     * Returns the end anchor point.
     */
    public AnchorPoint getEnd() {
        return end;
    }

    /**
     * Sets the end anchor point.
     */
    public void setEnd(AnchorPoint end) {
        this.end = end;
    }

    /**
     * Returns true if the line extends to the left.
     */
    public boolean isExtendLeft() {
        return extendLeft;
    }

    /**
     * Sets whether the line extends to the left.
     */
    public void setExtendLeft(boolean extendLeft) {
        this.extendLeft = extendLeft;
    }

    /**
     * Returns true if the line extends to the right.
     */
    public boolean isExtendRight() {
        return extendRight;
    }

    /**
     * Sets whether the line extends to the right.
     */
    public void setExtendRight(boolean extendRight) {
        this.extendRight = extendRight;
    }

    /**
     * Moves the entire line by the specified delta.
     *
     * @param deltaTime time offset in milliseconds
     * @param deltaPrice price offset
     */
    public void move(long deltaTime, double deltaPrice) {
        if (start != null) {
            start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        }
        if (end != null) {
            end = new AnchorPoint(end.timestamp() + deltaTime, end.price() + deltaPrice);
        }
    }
}
