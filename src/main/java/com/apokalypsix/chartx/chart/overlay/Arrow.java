package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * An arrow drawing from a start point to an end point with an arrowhead.
 *
 * <p>Arrows are useful for indicating direction of price movements or
 * pointing to specific chart features.
 */
public class Arrow extends Drawing {

    private AnchorPoint start;
    private AnchorPoint end;
    private float headSize = 12.0f;

    public Arrow(String id, long startTimestamp, double startPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
    }

    public Arrow(String id, long startTimestamp, double startPrice, long endTimestamp, double endPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.end = new AnchorPoint(endTimestamp, endPrice);
    }

    @Override
    public Type getType() {
        return Type.TREND_LINE; // Reuse or add ARROW to enum
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (start != null) points.add(start);
        if (end != null) points.add(end);
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
            default -> throw new IndexOutOfBoundsException("Arrow has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        return distanceToLineSegment(screenX, screenY, x1, y1, x2, y2) <= hitDistance;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getStart() { return start; }
    public AnchorPoint getEnd() { return end; }
    public void setEnd(AnchorPoint end) { this.end = end; }

    public float getHeadSize() { return headSize; }
    public void setHeadSize(float headSize) { this.headSize = headSize; }

    public void move(long deltaTime, double deltaPrice) {
        if (start != null) start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        if (end != null) end = new AnchorPoint(end.timestamp() + deltaTime, end.price() + deltaPrice);
    }
}
