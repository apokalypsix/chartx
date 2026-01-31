package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A regression channel drawing based on linear regression of price data.
 *
 * <p>The channel consists of a linear regression line through two points
 * with parallel upper and lower bands at a configurable standard deviation distance.
 */
public class RegressionChannel extends Drawing {

    private AnchorPoint start;
    private AnchorPoint end;

    private double standardDeviations = 2.0;
    private double channelWidth = 0;  // Calculated or manually set
    private boolean filled = true;
    private Color fillColor = new Color(100, 149, 237, 30);

    public RegressionChannel(String id, long startTimestamp, double startPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
    }

    public RegressionChannel(String id, long startTimestamp, double startPrice,
                              long endTimestamp, double endPrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.end = new AnchorPoint(endTimestamp, endPrice);
    }

    @Override
    public Type getType() {
        return Type.REGRESSION_CHANNEL;
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
            default -> throw new IndexOutOfBoundsException("RegressionChannel has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    /**
     * Returns the slope of the regression line in price per millisecond.
     */
    public double getSlope() {
        if (!isComplete()) return 0;
        long deltaTime = end.timestamp() - start.timestamp();
        if (deltaTime == 0) return 0;
        return (end.price() - start.price()) / deltaTime;
    }

    /**
     * Returns the price on the center line at a given timestamp.
     */
    public double getPriceAt(long timestamp) {
        if (!isComplete()) return Double.NaN;
        return start.price() + getSlope() * (timestamp - start.timestamp());
    }

    /**
     * Returns the upper channel price at a given timestamp.
     */
    public double getUpperPriceAt(long timestamp) {
        return getPriceAt(timestamp) + channelWidth;
    }

    /**
     * Returns the lower channel price at a given timestamp.
     */
    public double getLowerPriceAt(long timestamp) {
        return getPriceAt(timestamp) - channelWidth;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        // Check center line
        if (distanceToLineSegment(screenX, screenY, x1, y1, x2, y2) <= hitDistance) return true;

        // Check upper line
        double upperY1 = coords.yValueToScreenY(start.price() + channelWidth);
        double upperY2 = coords.yValueToScreenY(end.price() + channelWidth);
        if (distanceToLineSegment(screenX, screenY, x1, upperY1, x2, upperY2) <= hitDistance) return true;

        // Check lower line
        double lowerY1 = coords.yValueToScreenY(start.price() - channelWidth);
        double lowerY2 = coords.yValueToScreenY(end.price() - channelWidth);
        if (distanceToLineSegment(screenX, screenY, x1, lowerY1, x2, lowerY2) <= hitDistance) return true;

        return false;
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

    public double getStandardDeviations() { return standardDeviations; }
    public void setStandardDeviations(double standardDeviations) { this.standardDeviations = standardDeviations; }

    public double getChannelWidth() { return channelWidth; }
    public void setChannelWidth(double channelWidth) { this.channelWidth = channelWidth; }

    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }

    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }

    public void move(long deltaTime, double deltaPrice) {
        if (start != null) start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        if (end != null) end = new AnchorPoint(end.timestamp() + deltaTime, end.price() + deltaPrice);
    }
}
