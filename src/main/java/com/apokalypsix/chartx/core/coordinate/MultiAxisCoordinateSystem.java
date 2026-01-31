package com.apokalypsix.chartx.core.coordinate;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.axis.scale.LinearScale;

import java.util.HashMap;
import java.util.Map;

/**
 * Coordinate system that supports multiple Y-axes with independent value transformations.
 *
 * <p>The X transformation is shared across all axes (timestamps for time-series,
 * indices for category data), while each Y-axis has its own value transformation
 * based on its individual value range.
 *
 * <p>This class extends the single-axis CartesianCoordinateSystem pattern to support
 * arbitrary numbers of Y-axes, each with cached transformation parameters for performance.
 */
public class MultiAxisCoordinateSystem implements CoordinateSystem {

    private final Viewport viewport;
    private final YAxisManager axisManager;

    // Shared X transformation parameters
    private double xScale;
    private double xOffset;

    // Per-axis Y transformation caches
    private final Map<String, AxisTransform> axisTransforms = new HashMap<>();

    private boolean xCacheValid = false;

    /**
     * Creates a multi-axis coordinate system.
     *
     * @param viewport the viewport (provides screen dimensions and time range)
     * @param axisManager the axis manager (provides axis configurations)
     */
    public MultiAxisCoordinateSystem(Viewport viewport, YAxisManager axisManager) {
        this.viewport = viewport;
        this.axisManager = axisManager;
    }

    /**
     * Updates all cached transformation parameters.
     * Call this after viewport or axis changes.
     */
    public void updateCache() {
        updateXCache();
        updateAxisCaches();
    }

    /**
     * Updates the shared X transformation cache.
     */
    private void updateXCache() {
        long visibleXSpan = viewport.getVisibleDuration();
        int chartWidth = viewport.getChartWidth();

        if (visibleXSpan > 0) {
            xScale = (double) chartWidth / visibleXSpan;
        } else {
            xScale = 0;
        }
        xOffset = viewport.getLeftInset() - viewport.getStartTime() * xScale;

        xCacheValid = true;
    }

    /**
     * Updates the per-axis Y transformation caches.
     */
    private void updateAxisCaches() {
        int chartHeight = viewport.getChartHeight();
        int topInset = viewport.getTopInset();

        for (YAxis axis : axisManager.getAllAxes()) {
            AxisTransform transform = axisTransforms.computeIfAbsent(
                    axis.getId(), id -> new AxisTransform());

            // Calculate effective height and top offset based on heightRatio and anchor
            int effectiveHeight;
            int effectiveTop;
            switch (axis.getAnchor()) {
                case TOP:
                    effectiveHeight = (int) (chartHeight * axis.getHeightRatio());
                    effectiveTop = topInset;
                    break;
                case BOTTOM:
                    effectiveHeight = (int) (chartHeight * axis.getHeightRatio());
                    effectiveTop = topInset + chartHeight - effectiveHeight;
                    break;
                default: // FULL
                    effectiveHeight = chartHeight;
                    effectiveTop = topInset;
                    break;
            }

            transform.effectiveHeight = effectiveHeight;
            transform.effectiveTop = effectiveTop;
            transform.axis = axis;
            transform.isLinear = (axis.getScale() instanceof LinearScale);

            double valueSpan = axis.getValueSpan();

            if (valueSpan > 0) {
                transform.yScale = effectiveHeight / valueSpan;
            } else {
                transform.yScale = 0;
            }

            // Y is inverted: screenY = effectiveTop + effectiveHeight - (value - minValue) * scale
            // Simplify: screenY = (effectiveTop + effectiveHeight + minValue * scale) - value * scale
            transform.yOffset = effectiveTop + effectiveHeight + axis.getMinValue() * transform.yScale;
            transform.valid = true;
        }
    }

    /**
     * Invalidates all caches. Call when viewport or axis parameters change externally.
     */
    public void invalidateCache() {
        xCacheValid = false;
        for (AxisTransform transform : axisTransforms.values()) {
            transform.valid = false;
        }
    }

    /**
     * Invalidates cache for a specific axis.
     */
    public void invalidateAxisCache(String axisId) {
        AxisTransform transform = axisTransforms.get(axisId);
        if (transform != null) {
            transform.valid = false;
        }
    }

    private void ensureXCacheValid() {
        if (!xCacheValid) {
            updateXCache();
        }
    }

    private AxisTransform ensureAxisCacheValid(String axisId) {
        AxisTransform transform = axisTransforms.get(axisId);
        if (transform == null || !transform.valid) {
            YAxis axis = axisManager.getAxis(axisId);
            if (axis == null) {
                axis = axisManager.getDefaultAxis();
                axisId = YAxis.DEFAULT_AXIS_ID;
            }
            transform = axisTransforms.computeIfAbsent(axisId, id -> new AxisTransform());

            int chartHeight = viewport.getChartHeight();
            int topInset = viewport.getTopInset();

            // Calculate effective height and top offset based on heightRatio and anchor
            int effectiveHeight;
            int effectiveTop;
            switch (axis.getAnchor()) {
                case TOP:
                    effectiveHeight = (int) (chartHeight * axis.getHeightRatio());
                    effectiveTop = topInset;
                    break;
                case BOTTOM:
                    effectiveHeight = (int) (chartHeight * axis.getHeightRatio());
                    effectiveTop = topInset + chartHeight - effectiveHeight;
                    break;
                default: // FULL
                    effectiveHeight = chartHeight;
                    effectiveTop = topInset;
                    break;
            }

            transform.effectiveHeight = effectiveHeight;
            transform.effectiveTop = effectiveTop;
            transform.axis = axis;
            transform.isLinear = (axis.getScale() instanceof LinearScale);

            double valueSpan = axis.getValueSpan();

            if (valueSpan > 0) {
                transform.yScale = effectiveHeight / valueSpan;
            } else {
                transform.yScale = 0;
            }
            transform.yOffset = effectiveTop + effectiveHeight + axis.getMinValue() * transform.yScale;
            transform.valid = true;
        }
        return transform;
    }

    // ========== X transformations - shared across all axes ==========

    @Override
    public double xValueToScreenX(long xValue) {
        ensureXCacheValid();
        return xValue * xScale + xOffset;
    }

    @Override
    public long screenXToXValue(double screenX) {
        ensureXCacheValid();
        if (xScale == 0) {
            return viewport.getStartTime();
        }
        return (long) ((screenX - xOffset) / xScale);
    }

    @Override
    public void xValueToScreenX(long[] xValues, float[] screenX, int offset, int count) {
        ensureXCacheValid();
        for (int i = 0; i < count; i++) {
            screenX[i] = (float) (xValues[offset + i] * xScale + xOffset);
        }
    }

    @Override
    public double getPixelWidth(long xSpan) {
        ensureXCacheValid();
        return xSpan * xScale;
    }

    // ========== Y transformations - uses default axis ==========

    @Override
    public double yValueToScreenY(double yValue) {
        return yValueToScreenY(yValue, YAxis.DEFAULT_AXIS_ID);
    }

    @Override
    public double screenYToYValue(double screenY) {
        return screenYToYValue(screenY, YAxis.DEFAULT_AXIS_ID);
    }

    @Override
    public void yValueToScreenY(float[] yValues, float[] screenY, int offset, int count) {
        yValueToScreenY(yValues, screenY, offset, count, YAxis.DEFAULT_AXIS_ID);
    }

    @Override
    public double getPixelHeight(double ySpan) {
        return getPixelHeight(ySpan, YAxis.DEFAULT_AXIS_ID);
    }

    // ========== Y transformations - axis-specific ==========

    /**
     * Converts a Y-value to a Y screen coordinate using the specified axis.
     *
     * @param yValue the Y-value
     * @param axisId the axis ID
     * @return Y coordinate in pixels
     */
    public double yValueToScreenY(double yValue, String axisId) {
        AxisTransform transform = ensureAxisCacheValid(axisId);

        if (transform.isLinear) {
            // Fast path for linear scale - use precomputed scale and offset
            return transform.yOffset - yValue * transform.yScale;
        } else {
            // Non-linear scale - use axis normalize
            double normalized = transform.axis.normalize(yValue);
            // Y is inverted: normalized 0 (min) at bottom, 1 (max) at top
            return transform.effectiveTop + transform.effectiveHeight * (1.0 - normalized);
        }
    }

    /**
     * Converts a Y screen coordinate to a Y-value using the specified axis.
     *
     * @param screenY Y coordinate in pixels
     * @param axisId the axis ID
     * @return the Y-value
     */
    public double screenYToYValue(double screenY, String axisId) {
        AxisTransform transform = ensureAxisCacheValid(axisId);

        if (transform.isLinear) {
            // Fast path for linear scale
            if (transform.yScale == 0) {
                return transform.axis != null ? transform.axis.getMinValue() : 0;
            }
            return (transform.yOffset - screenY) / transform.yScale;
        } else {
            // Non-linear scale - use axis interpolate
            if (transform.effectiveHeight == 0) {
                return transform.axis != null ? transform.axis.getMinValue() : 0;
            }
            // Y is inverted: calculate normalized position
            double normalized = 1.0 - (screenY - transform.effectiveTop) / transform.effectiveHeight;
            return transform.axis.interpolate(normalized);
        }
    }

    /**
     * Batch conversion of Y-values to screen Y coordinates using the specified axis.
     *
     * @param yValues array of Y-values
     * @param screenY output array for Y coordinates
     * @param offset starting index in yValues array
     * @param count number of elements to convert
     * @param axisId the axis ID
     */
    public void yValueToScreenY(float[] yValues, float[] screenY, int offset, int count, String axisId) {
        AxisTransform transform = ensureAxisCacheValid(axisId);

        if (transform.isLinear) {
            // Fast path for linear scale
            for (int i = 0; i < count; i++) {
                screenY[i] = (float) (transform.yOffset - yValues[offset + i] * transform.yScale);
            }
        } else {
            // Non-linear scale - use axis normalize
            YAxis axis = transform.axis;
            double min = axis.getMinValue();
            double max = axis.getMaxValue();
            int top = transform.effectiveTop;
            int height = transform.effectiveHeight;

            for (int i = 0; i < count; i++) {
                double normalized = axis.getScale().normalize(yValues[offset + i], min, max);
                screenY[i] = (float) (top + height * (1.0 - normalized));
            }
        }
    }

    /**
     * Returns the height of a Y-value span in pixels using the specified axis.
     *
     * <p>Note: For non-linear scales, this returns an approximation based on
     * the span as a fraction of the total visible range.
     *
     * @param ySpan the Y-value span
     * @param axisId the axis ID
     * @return height in pixels
     */
    public double getPixelHeight(double ySpan, String axisId) {
        AxisTransform transform = ensureAxisCacheValid(axisId);

        if (transform.isLinear) {
            return ySpan * transform.yScale;
        } else {
            // For non-linear scales, approximate using fraction of total range
            double valueSpan = transform.axis.getValueSpan();
            if (valueSpan == 0) {
                return 0;
            }
            return (ySpan / valueSpan) * transform.effectiveHeight;
        }
    }

    // ========== Convenience methods ==========

    /**
     * Returns a wrapper coordinate system for a specific axis.
     * This allows passing axis-specific coordinates to components that expect
     * a standard CoordinateSystem interface.
     *
     * @param axisId the axis ID
     * @return axis-specific coordinate system wrapper
     */
    public CoordinateSystem forAxis(String axisId) {
        return new AxisSpecificCoordinateSystem(this, axisId);
    }

    /**
     * Returns the underlying viewport.
     */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Returns the axis manager.
     */
    public YAxisManager getAxisManager() {
        return axisManager;
    }

    /**
     * Cached transformation parameters for a single axis.
     */
    private static class AxisTransform {
        double yScale;
        double yOffset;
        int effectiveHeight;
        int effectiveTop;
        YAxis axis;
        boolean isLinear;
        boolean valid = false;
    }
}
