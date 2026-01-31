package com.apokalypsix.chartx.core.coordinate;

import com.apokalypsix.chartx.chart.axis.Viewport;

/**
 * Standard Cartesian coordinate system implementation.
 *
 * <p>Transforms between data coordinates (X-value, Y-value) and screen coordinates (pixels)
 * using linear scaling based on the current viewport. X-values are axis-agnostic:
 * timestamps for time-series data, indices for category data.
 */
public class CartesianCoordinateSystem implements CoordinateSystem {

    private final Viewport viewport;

    // Cached transformation parameters for performance
    private double xScale;
    private double xOffset;
    private double yScale;
    private double yOffset;
    private boolean cacheValid = false;

    /**
     * Creates a coordinate system using the given viewport.
     */
    public CartesianCoordinateSystem(Viewport viewport) {
        this.viewport = viewport;
    }

    /**
     * Updates the cached transformation parameters.
     * Call this after viewport changes.
     */
    public void updateCache() {
        long visibleXSpan = viewport.getVisibleDuration();
        double visibleYSpan = viewport.getVisiblePriceSpan();
        int chartWidth = viewport.getChartWidth();
        int chartHeight = viewport.getChartHeight();

        // X transformation: xValue -> screenX
        // screenX = (xValue - minX) * scale + leftInset
        if (visibleXSpan > 0) {
            xScale = (double) chartWidth / visibleXSpan;
        } else {
            xScale = 0;
        }
        xOffset = viewport.getLeftInset() - viewport.getStartTime() * xScale;

        // Y transformation: yValue -> screenY
        // Note: Y is inverted (value increases upward, screenY increases downward)
        // screenY = topInset + chartHeight - (yValue - minY) * scale
        if (visibleYSpan > 0) {
            yScale = chartHeight / visibleYSpan;
        } else {
            yScale = 0;
        }
        yOffset = viewport.getTopInset() + chartHeight + viewport.getMinPrice() * yScale;

        cacheValid = true;
    }

    /**
     * Invalidates the cache. Call when viewport parameters change externally.
     */
    public void invalidateCache() {
        cacheValid = false;
    }

    private void ensureCacheValid() {
        if (!cacheValid) {
            updateCache();
        }
    }

    @Override
    public double xValueToScreenX(long xValue) {
        ensureCacheValid();
        return xValue * xScale + xOffset;
    }

    @Override
    public double yValueToScreenY(double yValue) {
        ensureCacheValid();
        return yOffset - yValue * yScale;
    }

    @Override
    public long screenXToXValue(double screenX) {
        ensureCacheValid();
        if (xScale == 0) {
            return viewport.getStartTime();
        }
        return (long) ((screenX - xOffset) / xScale);
    }

    @Override
    public double screenYToYValue(double screenY) {
        ensureCacheValid();
        if (yScale == 0) {
            return viewport.getMinPrice();
        }
        return (yOffset - screenY) / yScale;
    }

    @Override
    public void xValueToScreenX(long[] xValues, float[] screenX, int offset, int count) {
        ensureCacheValid();
        // Inline the calculation to avoid method call overhead
        for (int i = 0; i < count; i++) {
            screenX[i] = (float) (xValues[offset + i] * xScale + xOffset);
        }
    }

    @Override
    public void yValueToScreenY(float[] yValues, float[] screenY, int offset, int count) {
        ensureCacheValid();
        // Inline the calculation to avoid method call overhead
        for (int i = 0; i < count; i++) {
            screenY[i] = (float) (yOffset - yValues[offset + i] * yScale);
        }
    }

    @Override
    public double getPixelWidth(long xSpan) {
        ensureCacheValid();
        return xSpan * xScale;
    }

    @Override
    public double getPixelHeight(double ySpan) {
        ensureCacheValid();
        return ySpan * yScale;
    }

    /**
     * Returns the underlying viewport.
     */
    public Viewport getViewport() {
        return viewport;
    }
}
