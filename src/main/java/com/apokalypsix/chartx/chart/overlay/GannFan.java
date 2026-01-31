package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A Gann Fan drawing displaying rays at specific Gann angle ratios.
 *
 * <p>Gann angles represent time/price relationships:
 * 1x1 = 45 degrees (1 unit time, 1 unit price)
 * 2x1 = steeper (2 units price per 1 unit time)
 * 1x2 = shallower (1 unit price per 2 units time)
 */
public class GannFan extends Drawing {

    /**
     * Gann angle ratios as [time, price] pairs.
     * The ratio represents "price units per time unit".
     */
    public static final float[][] DEFAULT_ANGLES = {
        {1, 8},  // 1x8
        {1, 4},  // 1x4
        {1, 3},  // 1x3
        {1, 2},  // 1x2
        {1, 1},  // 1x1 (45 degrees)
        {2, 1},  // 2x1
        {3, 1},  // 3x1
        {4, 1},  // 4x1
        {8, 1},  // 8x1
    };

    private AnchorPoint start;
    private AnchorPoint scalePoint;  // Defines the unit scale

    private float[][] angles = DEFAULT_ANGLES;
    private boolean showLabels = true;
    private boolean upward = true;  // Fan direction
    private Color[] lineColors;

    public GannFan(String id, long timestamp, double price) {
        super(id);
        this.start = new AnchorPoint(timestamp, price);
        initDefaultColors();
    }

    public GannFan(String id, long startTimestamp, double startPrice,
                    long scaleTimestamp, double scalePrice) {
        super(id);
        this.start = new AnchorPoint(startTimestamp, startPrice);
        this.scalePoint = new AnchorPoint(scaleTimestamp, scalePrice);
        initDefaultColors();
    }

    private void initDefaultColors() {
        lineColors = new Color[DEFAULT_ANGLES.length];
        lineColors[0] = new Color(255, 82, 82);    // 1x8
        lineColors[1] = new Color(255, 152, 0);    // 1x4
        lineColors[2] = new Color(255, 193, 7);    // 1x3
        lineColors[3] = new Color(76, 175, 80);    // 1x2
        lineColors[4] = new Color(33, 150, 243);   // 1x1
        lineColors[5] = new Color(76, 175, 80);    // 2x1
        lineColors[6] = new Color(255, 193, 7);    // 3x1
        lineColors[7] = new Color(255, 152, 0);    // 4x1
        lineColors[8] = new Color(255, 82, 82);    // 8x1
    }

    @Override
    public Type getType() {
        return Type.GANN_FAN;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (start != null) points.add(start);
        if (scalePoint != null) points.add(scalePoint);
        return points;
    }

    @Override
    public boolean isComplete() {
        return start != null && scalePoint != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> start = point;
            case 1 -> scalePoint = point;
            default -> throw new IndexOutOfBoundsException("GannFan has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    /**
     * Returns the time unit (milliseconds per unit).
     */
    public long getTimeUnit() {
        if (!isComplete()) return 0;
        return Math.abs(scalePoint.timestamp() - start.timestamp());
    }

    /**
     * Returns the price unit.
     */
    public double getPriceUnit() {
        if (!isComplete()) return 0;
        return Math.abs(scalePoint.price() - start.price());
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());

        long timeUnit = getTimeUnit();
        double priceUnit = getPriceUnit();

        for (float[] angle : angles) {
            float timeRatio = angle[0];
            float priceRatio = angle[1];

            // Calculate endpoint
            long deltaTime = (long) (timeUnit * timeRatio * 10);  // Extend ray
            double deltaPrice = priceUnit * priceRatio * 10;
            if (!upward) deltaPrice = -deltaPrice;

            double x2 = coords.xValueToScreenX(start.timestamp() + deltaTime);
            double y2 = coords.yValueToScreenY(start.price() + deltaPrice);

            if (distanceToRay(screenX, screenY, x1, y1, x2, y2) <= hitDistance) return true;
        }
        return false;
    }

    private double distanceToRay(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));

        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0, t);

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(scalePoint.timestamp());
        double y2 = coords.yValueToScreenY(scalePoint.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getStart() { return start; }
    public AnchorPoint getScalePoint() { return scalePoint; }
    public void setScalePoint(AnchorPoint scalePoint) { this.scalePoint = scalePoint; }

    public float[][] getAngles() { return angles; }
    public void setAngles(float[][] angles) { this.angles = angles; }

    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }

    public boolean isUpward() { return upward; }
    public void setUpward(boolean upward) { this.upward = upward; }

    public Color getLineColor(int index) {
        if (lineColors != null && index < lineColors.length) return lineColors[index];
        return getColor();
    }

    public void setLineColor(int index, Color color) {
        if (lineColors == null) lineColors = new Color[angles.length];
        if (index < lineColors.length) lineColors[index] = color;
    }

    public void move(long deltaTime, double deltaPrice) {
        if (start != null) start = new AnchorPoint(start.timestamp() + deltaTime, start.price() + deltaPrice);
        if (scalePoint != null) scalePoint = new AnchorPoint(scalePoint.timestamp() + deltaTime, scalePoint.price() + deltaPrice);
    }
}
