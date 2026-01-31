package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A price range drawing that highlights a horizontal price zone.
 *
 * <p>Price ranges span the full chart width and are commonly used
 * to mark support/resistance zones or price targets.
 */
public class PriceRange extends Drawing {

    private AnchorPoint top;
    private AnchorPoint bottom;

    private boolean filled = true;
    private Color fillColor = new Color(100, 149, 237, 50);

    public PriceRange(String id, double topPrice) {
        super(id);
        this.top = new AnchorPoint(0, topPrice);
    }

    public PriceRange(String id, double topPrice, double bottomPrice) {
        super(id);
        this.top = new AnchorPoint(0, topPrice);
        this.bottom = new AnchorPoint(0, bottomPrice);
    }

    @Override
    public Type getType() {
        return Type.PRICE_RANGE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (top != null) points.add(top);
        if (bottom != null) points.add(bottom);
        return points;
    }

    @Override
    public boolean isComplete() {
        return top != null && bottom != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> top = new AnchorPoint(0, point.price());
            case 1 -> bottom = new AnchorPoint(0, point.price());
            default -> throw new IndexOutOfBoundsException("PriceRange has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double y1 = coords.yValueToScreenY(top.price());
        double y2 = coords.yValueToScreenY(bottom.price());

        double topY = Math.min(y1, y2);
        double bottomY = Math.max(y1, y2);

        // Check if inside the range
        if (filled && screenY >= topY && screenY <= bottomY) {
            return true;
        }

        // Check near top or bottom lines
        if (Math.abs(screenY - topY) <= hitDistance) return true;
        if (Math.abs(screenY - bottomY) <= hitDistance) return true;

        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double y1 = coords.yValueToScreenY(top.price());
        double y2 = coords.yValueToScreenY(bottom.price());

        if (Math.abs(screenY - y1) <= handleRadius) return HandleType.ANCHOR_0;
        if (Math.abs(screenY - y2) <= handleRadius) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    public double getTopPrice() { return top.price(); }
    public double getBottomPrice() { return bottom != null ? bottom.price() : top.price(); }

    public void setTopPrice(double price) { this.top = new AnchorPoint(0, price); }
    public void setBottomPrice(double price) { this.bottom = new AnchorPoint(0, price); }

    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }

    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }

    public void move(double deltaPrice) {
        if (top != null) top = new AnchorPoint(0, top.price() + deltaPrice);
        if (bottom != null) bottom = new AnchorPoint(0, bottom.price() + deltaPrice);
    }
}
