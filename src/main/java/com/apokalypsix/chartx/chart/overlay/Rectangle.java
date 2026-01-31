package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A rectangle drawing defined by two corner anchor points.
 *
 * <p>Rectangles are commonly used to highlight price zones, trading ranges,
 * or areas of consolidation.
 */
public class Rectangle extends Drawing {

    private AnchorPoint corner1;  // First corner (typically top-left or bottom-left)
    private AnchorPoint corner2;  // Opposite corner

    // Visual properties
    private boolean filled = true;
    private Color fillColor = new Color(100, 149, 237, 50);  // Semi-transparent blue
    private Color borderColor = null;  // Use parent getColor() if null

    /**
     * Creates a rectangle with only the first corner defined.
     * Used during interactive creation.
     *
     * @param id unique identifier
     * @param timestamp1 first corner time
     * @param price1 first corner price
     */
    public Rectangle(String id, long timestamp1, double price1) {
        super(id);
        this.corner1 = new AnchorPoint(timestamp1, price1);
        this.corner2 = null;
    }

    /**
     * Creates a complete rectangle with both corners.
     *
     * @param id unique identifier
     * @param timestamp1 first corner time
     * @param price1 first corner price
     * @param timestamp2 second corner time
     * @param price2 second corner price
     */
    public Rectangle(String id, long timestamp1, double price1,
                     long timestamp2, double price2) {
        super(id);
        this.corner1 = new AnchorPoint(timestamp1, price1);
        this.corner2 = new AnchorPoint(timestamp2, price2);
    }

    @Override
    public Type getType() {
        return Type.RECTANGLE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (corner1 != null) {
            points.add(corner1);
        }
        if (corner2 != null) {
            points.add(corner2);
        }
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
            default -> throw new IndexOutOfBoundsException("Rectangle has only 2 anchor points, index: " + index);
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

        double x1 = coords.xValueToScreenX(corner1.timestamp());
        double y1 = coords.yValueToScreenY(corner1.price());
        double x2 = coords.xValueToScreenX(corner2.timestamp());
        double y2 = coords.yValueToScreenY(corner2.price());

        // Normalize coordinates
        double left = Math.min(x1, x2);
        double right = Math.max(x1, x2);
        double top = Math.min(y1, y2);
        double bottom = Math.max(y1, y2);

        // Check if point is inside the rectangle
        if (filled) {
            return screenX >= left && screenX <= right &&
                   screenY >= top && screenY <= bottom;
        }

        // For non-filled, check if near any edge
        boolean nearTop = screenY >= top - hitDistance && screenY <= top + hitDistance &&
                          screenX >= left && screenX <= right;
        boolean nearBottom = screenY >= bottom - hitDistance && screenY <= bottom + hitDistance &&
                             screenX >= left && screenX <= right;
        boolean nearLeft = screenX >= left - hitDistance && screenX <= left + hitDistance &&
                           screenY >= top && screenY <= bottom;
        boolean nearRight = screenX >= right - hitDistance && screenX <= right + hitDistance &&
                            screenY >= top && screenY <= bottom;

        return nearTop || nearBottom || nearLeft || nearRight;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double x1 = coords.xValueToScreenX(corner1.timestamp());
        double y1 = coords.yValueToScreenY(corner1.price());
        double x2 = coords.xValueToScreenX(corner2.timestamp());
        double y2 = coords.yValueToScreenY(corner2.price());

        // Check corner handles
        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) {
            return HandleType.ANCHOR_0;
        }
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) {
            return HandleType.ANCHOR_1;
        }

        // Check opposite corners (for 4-corner editing)
        if (isNearPoint(screenX, screenY, x1, y2, handleRadius)) {
            return HandleType.ANCHOR_2;
        }
        if (isNearPoint(screenX, screenY, x2, y1, handleRadius)) {
            return HandleType.ANCHOR_3;
        }

        // Check if inside for body drag
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
     * Returns the first corner anchor point.
     */
    public AnchorPoint getCorner1() {
        return corner1;
    }

    /**
     * Returns the second corner anchor point.
     */
    public AnchorPoint getCorner2() {
        return corner2;
    }

    /**
     * Sets the second corner anchor point.
     */
    public void setCorner2(AnchorPoint corner2) {
        this.corner2 = corner2;
    }

    /**
     * Returns the left time boundary.
     */
    public long getLeftTime() {
        if (!isComplete()) return 0;
        return Math.min(corner1.timestamp(), corner2.timestamp());
    }

    /**
     * Returns the right time boundary.
     */
    public long getRightTime() {
        if (!isComplete()) return 0;
        return Math.max(corner1.timestamp(), corner2.timestamp());
    }

    /**
     * Returns the top price boundary.
     */
    public double getTopPrice() {
        if (!isComplete()) return 0;
        return Math.max(corner1.price(), corner2.price());
    }

    /**
     * Returns the bottom price boundary.
     */
    public double getBottomPrice() {
        if (!isComplete()) return 0;
        return Math.min(corner1.price(), corner2.price());
    }

    /**
     * Returns true if the rectangle is filled.
     */
    public boolean isFilled() {
        return filled;
    }

    /**
     * Sets whether the rectangle is filled.
     */
    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    /**
     * Returns the fill color.
     */
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * Sets the fill color.
     */
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    /**
     * Returns the border color (null means use parent color).
     */
    public Color getBorderColor() {
        return borderColor;
    }

    /**
     * Sets the border color (null means use parent color).
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * Moves the entire rectangle by the specified delta.
     *
     * @param deltaTime time offset in milliseconds
     * @param deltaPrice price offset
     */
    public void move(long deltaTime, double deltaPrice) {
        if (corner1 != null) {
            corner1 = new AnchorPoint(corner1.timestamp() + deltaTime, corner1.price() + deltaPrice);
        }
        if (corner2 != null) {
            corner2 = new AnchorPoint(corner2.timestamp() + deltaTime, corner2.price() + deltaPrice);
        }
    }
}
