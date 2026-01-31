package com.apokalypsix.chartx.core.coordinate;

/**
 * Lightweight wrapper that implements CoordinateSystem for a specific axis.
 *
 * <p>Delegates all operations to a MultiAxisCoordinateSystem with a fixed axis ID.
 * This allows passing axis-specific coordinates to components that expect a
 * standard CoordinateSystem interface.
 */
public class AxisSpecificCoordinateSystem implements CoordinateSystem {

    private final MultiAxisCoordinateSystem multiAxis;
    private final String axisId;

    /**
     * Creates a coordinate system wrapper for the specified axis.
     *
     * @param multiAxis the underlying multi-axis coordinate system
     * @param axisId the axis ID to use for all Y transformations
     */
    public AxisSpecificCoordinateSystem(MultiAxisCoordinateSystem multiAxis, String axisId) {
        this.multiAxis = multiAxis;
        this.axisId = axisId;
    }

    /**
     * Returns the axis ID this wrapper is bound to.
     */
    public String getAxisId() {
        return axisId;
    }

    /**
     * Returns the underlying multi-axis coordinate system.
     */
    public MultiAxisCoordinateSystem getMultiAxisCoordinateSystem() {
        return multiAxis;
    }

    // ========== X transformations - delegated directly ==========

    @Override
    public double xValueToScreenX(long xValue) {
        return multiAxis.xValueToScreenX(xValue);
    }

    @Override
    public long screenXToXValue(double screenX) {
        return multiAxis.screenXToXValue(screenX);
    }

    @Override
    public void xValueToScreenX(long[] xValues, float[] screenX, int offset, int count) {
        multiAxis.xValueToScreenX(xValues, screenX, offset, count);
    }

    @Override
    public double getPixelWidth(long xSpan) {
        return multiAxis.getPixelWidth(xSpan);
    }

    // ========== Y transformations - delegated with axis ID ==========

    @Override
    public double yValueToScreenY(double yValue) {
        return multiAxis.yValueToScreenY(yValue, axisId);
    }

    @Override
    public double screenYToYValue(double screenY) {
        return multiAxis.screenYToYValue(screenY, axisId);
    }

    @Override
    public void yValueToScreenY(float[] yValues, float[] screenY, int offset, int count) {
        multiAxis.yValueToScreenY(yValues, screenY, offset, count, axisId);
    }

    @Override
    public double getPixelHeight(double ySpan) {
        return multiAxis.getPixelHeight(ySpan, axisId);
    }
}
