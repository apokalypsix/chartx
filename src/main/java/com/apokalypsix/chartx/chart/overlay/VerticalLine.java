package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.List;

/**
 * A vertical line drawing at a specific timestamp.
 *
 * <p>Vertical lines span the full chart height and are commonly used
 * to mark important times or events.
 */
public class VerticalLine extends Drawing {

    private AnchorPoint anchor;

    /**
     * Creates a vertical line at the specified timestamp.
     *
     * @param id unique identifier
     * @param timestamp the time position
     */
    public VerticalLine(String id, long timestamp) {
        super(id);
        // Use 0 for price since vertical lines don't depend on price
        this.anchor = new AnchorPoint(timestamp, 0);
    }

    /**
     * Creates a vertical line at the specified position.
     * The price is stored for reference but doesn't affect the line itself.
     *
     * @param id unique identifier
     * @param timestamp the time position
     * @param price reference price (for positioning during creation)
     */
    public VerticalLine(String id, long timestamp, double price) {
        super(id);
        this.anchor = new AnchorPoint(timestamp, price);
    }

    @Override
    public Type getType() {
        return Type.VERTICAL_LINE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        return List.of(anchor);
    }

    @Override
    public boolean isComplete() {
        return anchor != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("VerticalLine has only 1 anchor point, index: " + index);
        }
        this.anchor = new AnchorPoint(point.timestamp(), point.price());
    }

    @Override
    public int getRequiredAnchorCount() {
        return 1;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) {
            return false;
        }

        double lineX = coords.xValueToScreenX(anchor.timestamp());
        return Math.abs(screenX - lineX) <= hitDistance;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double lineX = coords.xValueToScreenX(anchor.timestamp());

        // For vertical lines, the handle is anywhere along the line
        if (Math.abs(screenX - lineX) <= handleRadius) {
            return HandleType.BODY;
        }

        return HandleType.NONE;
    }

    // ========== Getters and Setters ==========

    /**
     * Returns the timestamp of this vertical line.
     */
    public long getTimestamp() {
        return anchor.timestamp();
    }

    /**
     * Sets the timestamp of this vertical line.
     */
    public void setTimestamp(long timestamp) {
        this.anchor = new AnchorPoint(timestamp, anchor.price());
    }

    /**
     * Returns the anchor point.
     */
    public AnchorPoint getAnchor() {
        return anchor;
    }

    /**
     * Moves the line by the specified time delta.
     *
     * @param deltaTime time offset in milliseconds
     */
    public void move(long deltaTime) {
        anchor = new AnchorPoint(anchor.timestamp() + deltaTime, anchor.price());
    }
}
