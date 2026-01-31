package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A Fibonacci retracement drawing tool.
 *
 * <p>Fibonacci retracements are horizontal lines at key Fibonacci levels
 * between a high and low price point. Common levels include:
 * <ul>
 *   <li>0% - Start of move</li>
 *   <li>23.6%</li>
 *   <li>38.2%</li>
 *   <li>50%</li>
 *   <li>61.8%</li>
 *   <li>78.6%</li>
 *   <li>100% - End of move</li>
 * </ul>
 */
public class FibonacciRetracement extends Drawing {

    /** Default Fibonacci levels */
    public static final float[] DEFAULT_LEVELS = {0f, 0.236f, 0.382f, 0.5f, 0.618f, 0.786f, 1f};

    private AnchorPoint high;   // The 0% level
    private AnchorPoint low;    // The 100% level

    private float[] levels = DEFAULT_LEVELS;
    private boolean showLabels = true;
    private boolean extendLines = false;

    // Level colors (can be customized per level)
    private Color[] levelColors;

    /**
     * Creates a Fibonacci retracement with only the first anchor point.
     * Used during interactive creation.
     *
     * @param id unique identifier
     * @param timestamp anchor timestamp
     * @param price anchor price
     */
    public FibonacciRetracement(String id, long timestamp, double price) {
        super(id);
        this.high = new AnchorPoint(timestamp, price);
        this.low = null;
        initDefaultColors();
    }

    /**
     * Creates a complete Fibonacci retracement.
     *
     * @param id unique identifier
     * @param highTimestamp high point timestamp
     * @param highPrice high point price
     * @param lowTimestamp low point timestamp
     * @param lowPrice low point price
     */
    public FibonacciRetracement(String id, long highTimestamp, double highPrice,
                                 long lowTimestamp, double lowPrice) {
        super(id);
        this.high = new AnchorPoint(highTimestamp, highPrice);
        this.low = new AnchorPoint(lowTimestamp, lowPrice);
        initDefaultColors();
    }

    private void initDefaultColors() {
        // Default colors for each level
        levelColors = new Color[DEFAULT_LEVELS.length];
        levelColors[0] = new Color(128, 128, 128);   // 0%
        levelColors[1] = new Color(76, 175, 80);     // 23.6%
        levelColors[2] = new Color(33, 150, 243);    // 38.2%
        levelColors[3] = new Color(156, 39, 176);    // 50%
        levelColors[4] = new Color(255, 152, 0);     // 61.8%
        levelColors[5] = new Color(244, 67, 54);     // 78.6%
        levelColors[6] = new Color(128, 128, 128);   // 100%
    }

    @Override
    public Type getType() {
        return Type.FIBONACCI_RETRACEMENT;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (high != null) {
            points.add(high);
        }
        if (low != null) {
            points.add(low);
        }
        return points;
    }

    @Override
    public boolean isComplete() {
        return high != null && low != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> high = point;
            case 1 -> low = point;
            default -> throw new IndexOutOfBoundsException(
                    "FibonacciRetracement has only 2 anchor points, index: " + index);
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

        // Check if near any of the Fibonacci levels
        for (float level : levels) {
            double price = getPriceAtLevel(level);
            double levelY = coords.yValueToScreenY(price);

            if (Math.abs(screenY - levelY) <= hitDistance) {
                // Check if within time bounds (or extended)
                if (extendLines) {
                    return true;
                }

                long leftTime = Math.min(high.timestamp(), low.timestamp());
                long rightTime = Math.max(high.timestamp(), low.timestamp());
                double leftX = coords.xValueToScreenX(leftTime);
                double rightX = coords.xValueToScreenX(rightTime);

                if (screenX >= leftX && screenX <= rightX) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) {
            return HandleType.NONE;
        }

        double x1 = coords.xValueToScreenX(high.timestamp());
        double y1 = coords.yValueToScreenY(high.price());
        double x2 = coords.xValueToScreenX(low.timestamp());
        double y2 = coords.yValueToScreenY(low.price());

        // Check high anchor
        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) {
            return HandleType.ANCHOR_0;
        }

        // Check low anchor
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) {
            return HandleType.ANCHOR_1;
        }

        // Check if on any level line (for body drag)
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

    // ========== Fibonacci Level Calculations ==========

    /**
     * Returns the price at a given Fibonacci level.
     *
     * @param level the level (0.0 to 1.0)
     * @return the price at that level
     */
    public double getPriceAtLevel(double level) {
        if (!isComplete()) {
            return Double.NaN;
        }

        double range = high.price() - low.price();
        return low.price() + range * (1 - level);  // Inverted: 0% at high, 100% at low
    }

    /**
     * Returns all configured Fibonacci levels.
     */
    public float[] getLevels() {
        return levels;
    }

    /**
     * Sets custom Fibonacci levels.
     *
     * @param levels array of levels (each 0.0 to 1.0)
     */
    public void setLevels(float[] levels) {
        this.levels = levels;
    }

    /**
     * Returns the color for a specific level index.
     */
    public Color getLevelColor(int index) {
        if (levelColors != null && index < levelColors.length) {
            return levelColors[index];
        }
        return getColor();
    }

    /**
     * Sets the color for a specific level.
     */
    public void setLevelColor(int index, Color color) {
        if (levelColors == null) {
            levelColors = new Color[levels.length];
        }
        if (index < levelColors.length) {
            levelColors[index] = color;
        }
    }

    // ========== Getters and Setters ==========

    /**
     * Returns the high anchor point (0% level).
     */
    public AnchorPoint getHigh() {
        return high;
    }

    /**
     * Returns the low anchor point (100% level).
     */
    public AnchorPoint getLow() {
        return low;
    }

    /**
     * Sets the low anchor point.
     */
    public void setLow(AnchorPoint low) {
        this.low = low;
    }

    /**
     * Returns true if labels are shown.
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Sets whether to show level labels.
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Returns true if lines extend beyond anchor points.
     */
    public boolean isExtendLines() {
        return extendLines;
    }

    /**
     * Sets whether lines extend beyond anchor points.
     */
    public void setExtendLines(boolean extendLines) {
        this.extendLines = extendLines;
    }

    /**
     * Moves the entire drawing by the specified delta.
     *
     * @param deltaTime time offset in milliseconds
     * @param deltaPrice price offset
     */
    public void move(long deltaTime, double deltaPrice) {
        if (high != null) {
            high = new AnchorPoint(high.timestamp() + deltaTime, high.price() + deltaPrice);
        }
        if (low != null) {
            low = new AnchorPoint(low.timestamp() + deltaTime, low.price() + deltaPrice);
        }
    }
}
