package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.List;

/**
 * A horizontal line drawing at a specific price level.
 *
 * <p>Horizontal lines span the full chart width and are commonly used
 * to mark support/resistance levels.
 */
public class HorizontalLine extends Drawing {

    private AnchorPoint anchor;

    /**
     * Creates a horizontal line at the specified price level.
     *
     * @param id unique identifier
     * @param price the price level
     */
    public HorizontalLine(String id, double price) {
        super(id);
        // Use 0 for timestamp since horizontal lines don't depend on time
        this.anchor = new AnchorPoint(0, price);
    }

    /**
     * Creates a horizontal line at the specified price level with a reference timestamp.
     * The timestamp can be used for label positioning but doesn't affect the line itself.
     *
     * @param id unique identifier
     * @param timestamp reference timestamp (for label positioning)
     * @param price the price level
     */
    public HorizontalLine(String id, long timestamp, double price) {
        super(id);
        this.anchor = new AnchorPoint(timestamp, price);
    }

    @Override
    public Type getType() {
        return Type.HORIZONTAL_LINE;
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
            throw new IndexOutOfBoundsException("HorizontalLine has only 1 anchor point, index: " + index);
        }
        // Preserve the new price but keep the horizontal nature
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

        double lineY = coords.yValueToScreenY(anchor.price());
        return Math.abs(screenY - lineY) <= hitDistance;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double lineY = coords.yValueToScreenY(anchor.price());

        // For horizontal lines, the handle is anywhere along the line
        if (Math.abs(screenY - lineY) <= handleRadius) {
            return HandleType.BODY;
        }

        return HandleType.NONE;
    }

    // ========== Getters and Setters ==========

    /**
     * Returns the price level of this horizontal line.
     */
    public double getPrice() {
        return anchor.price();
    }

    /**
     * Sets the price level of this horizontal line.
     */
    public void setPrice(double price) {
        this.anchor = new AnchorPoint(anchor.timestamp(), price);
    }

    /**
     * Returns the anchor point.
     */
    public AnchorPoint getAnchor() {
        return anchor;
    }

    /**
     * Moves the line by the specified price delta.
     *
     * @param deltaPrice price offset
     */
    public void move(double deltaPrice) {
        anchor = new AnchorPoint(anchor.timestamp(), anchor.price() + deltaPrice);
    }
}
