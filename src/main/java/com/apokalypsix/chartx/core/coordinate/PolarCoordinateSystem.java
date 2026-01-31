package com.apokalypsix.chartx.core.coordinate;

/**
 * Coordinate system for polar/radial charts.
 *
 * <p>Provides transformations between data values and polar coordinates,
 * and from polar coordinates to screen (Cartesian) coordinates.
 * Used for pie charts, donut charts, radar charts, and other polar visualizations.
 *
 * <p>Angles are measured in radians, with 0 at the top (12 o'clock position)
 * and increasing clockwise by default.
 */
public class PolarCoordinateSystem {

    /** Center X coordinate in screen space */
    private float centerX;

    /** Center Y coordinate in screen space */
    private float centerY;

    /** Outer radius in pixels */
    private float outerRadius;

    /** Inner radius in pixels (for donut charts, 0 for pie charts) */
    private float innerRadius;

    /** Start angle in radians (default: -PI/2 for 12 o'clock) */
    private double startAngle = -Math.PI / 2;

    /** Whether angles increase clockwise (default: true) */
    private boolean clockwise = true;

    /** Minimum value for radial scaling */
    private double minValue = 0;

    /** Maximum value for radial scaling */
    private double maxValue = 1;

    /** Total angle span in radians (default: 2*PI for full circle) */
    private double angleSpan = 2 * Math.PI;

    /**
     * Creates a polar coordinate system with the given center and radius.
     *
     * @param centerX center X coordinate in pixels
     * @param centerY center Y coordinate in pixels
     * @param outerRadius outer radius in pixels
     */
    public PolarCoordinateSystem(float centerX, float centerY, float outerRadius) {
        this(centerX, centerY, outerRadius, 0);
    }

    /**
     * Creates a polar coordinate system with inner and outer radius (for donuts).
     *
     * @param centerX center X coordinate in pixels
     * @param centerY center Y coordinate in pixels
     * @param outerRadius outer radius in pixels
     * @param innerRadius inner radius in pixels
     */
    public PolarCoordinateSystem(float centerX, float centerY, float outerRadius, float innerRadius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.outerRadius = outerRadius;
        this.innerRadius = Math.max(0, Math.min(innerRadius, outerRadius - 1));
    }

    // ========== Configuration ==========

    /**
     * Sets the center point.
     */
    public PolarCoordinateSystem center(float x, float y) {
        this.centerX = x;
        this.centerY = y;
        return this;
    }

    /**
     * Sets the outer radius.
     */
    public PolarCoordinateSystem outerRadius(float radius) {
        this.outerRadius = radius;
        return this;
    }

    /**
     * Sets the inner radius (for donut charts).
     */
    public PolarCoordinateSystem innerRadius(float radius) {
        this.innerRadius = Math.max(0, Math.min(radius, outerRadius - 1));
        return this;
    }

    /**
     * Sets the start angle (where the first segment begins).
     *
     * @param radians angle in radians (default: -PI/2 for 12 o'clock)
     */
    public PolarCoordinateSystem startAngle(double radians) {
        this.startAngle = radians;
        return this;
    }

    /**
     * Sets whether angles increase clockwise.
     */
    public PolarCoordinateSystem clockwise(boolean clockwise) {
        this.clockwise = clockwise;
        return this;
    }

    /**
     * Sets the value range for radial scaling.
     */
    public PolarCoordinateSystem valueRange(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    /**
     * Sets the total angle span (for partial circles like gauges).
     *
     * @param radians total angle span (default: 2*PI for full circle)
     */
    public PolarCoordinateSystem angleSpan(double radians) {
        this.angleSpan = Math.max(0, Math.min(2 * Math.PI, radians));
        return this;
    }

    // ========== Getters ==========

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getAngleSpan() {
        return angleSpan;
    }

    /**
     * Returns the usable radius range (outer - inner).
     */
    public float getRadiusRange() {
        return outerRadius - innerRadius;
    }

    // ========== Angle Transformations ==========

    /**
     * Converts an index to an angle for category-based layouts.
     *
     * @param index the category index
     * @param totalCount total number of categories
     * @return angle in radians
     */
    public double indexToAngle(int index, int totalCount) {
        if (totalCount <= 0) {
            return startAngle;
        }
        double fraction = (double) index / totalCount;
        double angle = fraction * angleSpan;
        return clockwise ? startAngle + angle : startAngle - angle;
    }

    /**
     * Converts an index to an angle, centered on the category.
     *
     * @param index the category index
     * @param totalCount total number of categories
     * @return angle in radians at the center of the category
     */
    public double indexToCenterAngle(int index, int totalCount) {
        if (totalCount <= 0) {
            return startAngle;
        }
        double fraction = (index + 0.5) / totalCount;
        double angle = fraction * angleSpan;
        return clockwise ? startAngle + angle : startAngle - angle;
    }

    /**
     * Converts a normalized fraction [0, 1] to an angle.
     *
     * @param fraction normalized value from 0 to 1
     * @return angle in radians
     */
    public double fractionToAngle(double fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        double angle = fraction * angleSpan;
        return clockwise ? startAngle + angle : startAngle - angle;
    }

    /**
     * Returns the angular width for each category.
     *
     * @param totalCount total number of categories
     * @return angular width in radians
     */
    public double getCategoryAngleWidth(int totalCount) {
        if (totalCount <= 0) {
            return angleSpan;
        }
        return angleSpan / totalCount;
    }

    // ========== Radius Transformations ==========

    /**
     * Converts a data value to a radius.
     *
     * @param value the data value
     * @return radius in pixels
     */
    public float valueToRadius(double value) {
        if (Double.isNaN(value)) {
            return innerRadius;
        }
        double range = maxValue - minValue;
        if (range <= 0) {
            return (innerRadius + outerRadius) / 2;
        }
        double normalized = (value - minValue) / range;
        normalized = Math.max(0, Math.min(1, normalized));
        return innerRadius + (float) (normalized * getRadiusRange());
    }

    /**
     * Converts a normalized fraction [0, 1] to a radius.
     *
     * @param fraction normalized value from 0 to 1
     * @return radius in pixels
     */
    public float fractionToRadius(double fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        return innerRadius + (float) (fraction * getRadiusRange());
    }

    /**
     * Converts a radius to a data value.
     *
     * @param radius radius in pixels
     * @return the data value
     */
    public double radiusToValue(float radius) {
        float range = getRadiusRange();
        if (range <= 0) {
            return minValue;
        }
        double normalized = (radius - innerRadius) / range;
        normalized = Math.max(0, Math.min(1, normalized));
        return minValue + normalized * (maxValue - minValue);
    }

    // ========== Polar to Cartesian Transformations ==========

    /**
     * Converts polar coordinates to screen X coordinate.
     *
     * @param angle angle in radians
     * @param radius radius in pixels
     * @return X coordinate in screen space
     */
    public float polarToScreenX(double angle, float radius) {
        return centerX + radius * (float) Math.cos(angle);
    }

    /**
     * Converts polar coordinates to screen Y coordinate.
     *
     * @param angle angle in radians
     * @param radius radius in pixels
     * @return Y coordinate in screen space
     */
    public float polarToScreenY(double angle, float radius) {
        return centerY + radius * (float) Math.sin(angle);
    }

    /**
     * Converts polar coordinates to screen coordinates, storing in output array.
     *
     * @param angle angle in radians
     * @param radius radius in pixels
     * @param out output array [x, y] (must have length >= 2)
     */
    public void polarToScreen(double angle, float radius, float[] out) {
        out[0] = polarToScreenX(angle, radius);
        out[1] = polarToScreenY(angle, radius);
    }

    /**
     * Batch conversion of polar coordinates to screen coordinates.
     *
     * @param angles array of angles in radians
     * @param radii array of radii in pixels
     * @param outX output array for X coordinates
     * @param outY output array for Y coordinates
     * @param count number of points to convert
     */
    public void polarToScreen(double[] angles, float[] radii, float[] outX, float[] outY, int count) {
        for (int i = 0; i < count; i++) {
            outX[i] = polarToScreenX(angles[i], radii[i]);
            outY[i] = polarToScreenY(angles[i], radii[i]);
        }
    }

    // ========== Screen to Polar Transformations ==========

    /**
     * Converts screen coordinates to an angle.
     *
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @return angle in radians
     */
    public double screenToAngle(float screenX, float screenY) {
        return Math.atan2(screenY - centerY, screenX - centerX);
    }

    /**
     * Converts screen coordinates to a radius.
     *
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @return radius in pixels
     */
    public float screenToRadius(float screenX, float screenY) {
        float dx = screenX - centerX;
        float dy = screenY - centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Checks if a screen point is within the polar area (between inner and outer radius).
     *
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @return true if the point is within the polar area
     */
    public boolean containsPoint(float screenX, float screenY) {
        float radius = screenToRadius(screenX, screenY);
        return radius >= innerRadius && radius <= outerRadius;
    }

    /**
     * Converts a screen angle to a normalized fraction [0, 1] within the angle span.
     *
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @return normalized fraction, or NaN if outside angle span
     */
    public double screenToFraction(float screenX, float screenY) {
        double angle = screenToAngle(screenX, screenY);

        // Normalize angle relative to start
        double relativeAngle = clockwise ? angle - startAngle : startAngle - angle;

        // Normalize to [0, 2*PI]
        while (relativeAngle < 0) {
            relativeAngle += 2 * Math.PI;
        }
        while (relativeAngle > 2 * Math.PI) {
            relativeAngle -= 2 * Math.PI;
        }

        // Check if within angle span
        if (relativeAngle > angleSpan) {
            return Double.NaN;
        }

        return relativeAngle / angleSpan;
    }

    // ========== Utility Methods ==========

    /**
     * Recalculates center and radius to fit within the given bounds.
     *
     * @param x left edge
     * @param y top edge
     * @param width available width
     * @param height available height
     * @param padding padding around the polar area
     */
    public void fitToBounds(float x, float y, float width, float height, float padding) {
        this.centerX = x + width / 2;
        this.centerY = y + height / 2;
        float maxRadius = Math.min(width, height) / 2 - padding;
        float innerRatio = outerRadius > 0 ? innerRadius / outerRadius : 0;
        this.outerRadius = Math.max(1, maxRadius);
        this.innerRadius = outerRadius * innerRatio;
    }

    /**
     * Creates a copy of this coordinate system.
     */
    public PolarCoordinateSystem copy() {
        PolarCoordinateSystem copy = new PolarCoordinateSystem(centerX, centerY, outerRadius, innerRadius);
        copy.startAngle = this.startAngle;
        copy.clockwise = this.clockwise;
        copy.minValue = this.minValue;
        copy.maxValue = this.maxValue;
        copy.angleSpan = this.angleSpan;
        return copy;
    }
}
