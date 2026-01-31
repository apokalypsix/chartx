package com.apokalypsix.chartx.core.coordinate;

/**
 * Interface for coordinate transformation between data space and screen space.
 *
 * <p>Implementations handle the mapping from data coordinates (X-value, Y-value)
 * to screen coordinates (pixels) and vice versa. X-values are axis-agnostic:
 * <ul>
 *   <li>For time-series data: X-values are timestamps in epoch milliseconds</li>
 *   <li>For category-based data: X-values are indices (0, 1, 2...)</li>
 * </ul>
 */
public interface CoordinateSystem {

    /**
     * Converts an X-value to an X screen coordinate.
     * For time-series data, xValue is a timestamp in epoch milliseconds.
     * For category data, xValue is an index.
     *
     * @param xValue the X-axis data value
     * @return X coordinate in pixels
     */
    double xValueToScreenX(long xValue);

    /**
     * Converts a Y-value (price/value) to a Y screen coordinate.
     *
     * @param yValue the Y-axis value
     * @return Y coordinate in pixels
     */
    double yValueToScreenY(double yValue);

    /**
     * Converts an X screen coordinate to an X-value.
     *
     * @param screenX X coordinate in pixels
     * @return X-axis data value
     */
    long screenXToXValue(double screenX);

    /**
     * Converts a Y screen coordinate to a Y-value.
     *
     * @param screenY Y coordinate in pixels
     * @return the Y-axis value
     */
    double screenYToYValue(double screenY);

    /**
     * Batch conversion of X-values to screen X coordinates.
     * This method is optimized for rendering large datasets.
     *
     * @param xValues array of X-values
     * @param screenX output array for X coordinates (must be same length or larger)
     * @param offset starting index in xValues array
     * @param count number of elements to convert
     */
    default void xValueToScreenX(long[] xValues, float[] screenX, int offset, int count) {
        for (int i = 0; i < count; i++) {
            screenX[i] = (float) xValueToScreenX(xValues[offset + i]);
        }
    }

    /**
     * Batch conversion of Y-values to screen Y coordinates.
     * This method is optimized for rendering large datasets.
     *
     * @param yValues array of Y-values
     * @param screenY output array for Y coordinates (must be same length or larger)
     * @param offset starting index in yValues array
     * @param count number of elements to convert
     */
    default void yValueToScreenY(float[] yValues, float[] screenY, int offset, int count) {
        for (int i = 0; i < count; i++) {
            screenY[i] = (float) yValueToScreenY(yValues[offset + i]);
        }
    }

    /**
     * Returns the width in pixels for a given X-value span.
     * For time-series: xSpan is duration in milliseconds.
     * For category: xSpan is the number of categories.
     *
     * @param xSpan the span of X-values
     * @return width in pixels
     */
    double getPixelWidth(long xSpan);

    /**
     * Returns the height of a Y-value span in pixels.
     *
     * @param ySpan the span of Y-values
     * @return height in pixels
     */
    double getPixelHeight(double ySpan);
}
