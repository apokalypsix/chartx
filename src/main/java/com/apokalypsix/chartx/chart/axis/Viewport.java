package com.apokalypsix.chartx.chart.axis;

import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Manages the visible data range and coordinate transformations for a chart.
 *
 * <p>The viewport defines what portion of the data is currently visible and handles
 * transformations between data coordinates (time, price) and screen coordinates (pixels).
 *
 * <p>This class is mutable and designed for efficient updates during pan/zoom operations.
 */
public class Viewport {

    // Visible data range
    private long startTime;
    private long endTime;
    private double minPrice;
    private double maxPrice;

    // Screen dimensions (in pixels)
    private int width;
    private int height;

    // Chart area insets (for axes, labels)
    private int leftInset;
    private int rightInset;
    private int topInset;
    private int bottomInset;

    // Auto-scaling
    private boolean autoScaleY = true;
    private double autoScalePadding = 0.05; // 5% padding

    /**
     * Creates a viewport with default values.
     */
    public Viewport() {
        this.startTime = 0;
        this.endTime = 1;
        this.minPrice = 0;
        this.maxPrice = 1;
        this.width = 800;
        this.height = 600;
        this.leftInset = 0;
        this.rightInset = 60;  // Space for price axis
        this.topInset = 10;
        this.bottomInset = 30; // Space for time axis
    }

    // ========== Getters ==========

    /**
     * Returns the minimum X-value (start of visible range).
     * For time-series: timestamp in epoch milliseconds.
     * For category: starting index.
     */
    public long getMinX() {
        return startTime;
    }

    /**
     * Returns the maximum X-value (end of visible range).
     * For time-series: timestamp in epoch milliseconds.
     * For category: ending index.
     */
    public long getMaxX() {
        return endTime;
    }

    /**
     * Returns the minimum Y-value (bottom of visible range).
     */
    public double getMinY() {
        return minPrice;
    }

    /**
     * Returns the maximum Y-value (top of visible range).
     */
    public double getMaxY() {
        return maxPrice;
    }

    /**
     * Returns the start time of the visible range.
     * Convenience alias for getMinX() for time-series data.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time of the visible range.
     * Convenience alias for getMaxX() for time-series data.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Returns the minimum price of the visible range.
     * Convenience alias for getMinY() for financial data.
     */
    public double getMinPrice() {
        return minPrice;
    }

    /**
     * Returns the maximum price of the visible range.
     * Convenience alias for getMaxY() for financial data.
     */
    public double getMaxPrice() {
        return maxPrice;
    }

    public TimeRange getTimeRange() {
        return new TimeRange(startTime, endTime);
    }

    public PriceRange getPriceRange() {
        return new PriceRange(minPrice, maxPrice);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Returns the width of the chart area (excluding insets).
     */
    public int getChartWidth() {
        return Math.max(1, width - leftInset - rightInset);
    }

    /**
     * Returns the height of the chart area (excluding insets).
     */
    public int getChartHeight() {
        return Math.max(1, height - topInset - bottomInset);
    }

    public int getLeftInset() {
        return leftInset;
    }

    public int getRightInset() {
        return rightInset;
    }

    public int getTopInset() {
        return topInset;
    }

    public int getBottomInset() {
        return bottomInset;
    }

    /**
     * Returns whether automatic Y-axis scaling is enabled.
     *
     * @return true if auto-scaling is enabled
     * @deprecated Use {@code chart.getYAxis().isAutoScale()} instead
     */
    @Deprecated
    public boolean isAutoScaleY() {
        return autoScaleY;
    }

    // ========== Computed properties ==========

    /**
     * Returns the duration of the visible time range in milliseconds.
     */
    public long getVisibleDuration() {
        return endTime - startTime;
    }

    /**
     * Returns the visible price span (max - min).
     */
    public double getVisiblePriceSpan() {
        return maxPrice - minPrice;
    }

    /**
     * Returns the number of pixels per millisecond.
     */
    public double getPixelsPerMillis() {
        long duration = getVisibleDuration();
        return duration > 0 ? (double) getChartWidth() / duration : 0;
    }

    /**
     * Returns the number of pixels per price unit.
     */
    public double getPixelsPerPriceUnit() {
        double span = getVisiblePriceSpan();
        return span > 0 ? getChartHeight() / span : 0;
    }

    /**
     * Estimates the number of visible bars given a bar duration.
     */
    public int estimateVisibleBarCount(long barDurationMillis) {
        if (barDurationMillis <= 0) return 0;
        return (int) (getVisibleDuration() / barDurationMillis);
    }

    /**
     * Returns the pixels available per bar, given a bar duration.
     * This is useful for LOD calculations.
     */
    public double getPixelsPerBar(long barDurationMillis) {
        int barCount = estimateVisibleBarCount(barDurationMillis);
        return barCount > 0 ? (double) getChartWidth() / barCount : getChartWidth();
    }

    // ========== Coordinate conversion ==========

    /**
     * Converts a timestamp to an X screen coordinate.
     */
    public double timeToX(long timestamp) {
        if (startTime == endTime) {
            return leftInset + getChartWidth() / 2.0;
        }
        double normalized = (double) (timestamp - startTime) / (endTime - startTime);
        return leftInset + normalized * getChartWidth();
    }

    /**
     * Converts an X screen coordinate to a timestamp.
     */
    public long xToTime(double x) {
        double normalized = (x - leftInset) / getChartWidth();
        return startTime + (long) (normalized * (endTime - startTime));
    }

    /**
     * Converts a price to a Y screen coordinate.
     * Note: Y is inverted (0 at top, increases downward).
     */
    public double priceToY(double price) {
        if (minPrice == maxPrice) {
            return topInset + getChartHeight() / 2.0;
        }
        double normalized = (price - minPrice) / (maxPrice - minPrice);
        // Invert Y: high prices at top
        return topInset + (1.0 - normalized) * getChartHeight();
    }

    /**
     * Converts a Y screen coordinate to a price.
     */
    public double yToPrice(double y) {
        double normalized = 1.0 - (y - topInset) / getChartHeight();
        return minPrice + normalized * (maxPrice - minPrice);
    }

    // ========== Setters / Mutation ==========

    /**
     * Sets the visible time range.
     *
     * @param startTime start timestamp
     * @param endTime   end timestamp
     * @deprecated Use {@code chart.getTimeAxis().setVisibleRange(startTime, endTime)} instead
     */
    @Deprecated
    public void setTimeRange(long startTime, long endTime) {
        if (endTime < startTime) {
            throw new IllegalArgumentException("End time must be >= start time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Sets the visible price range.
     *
     * @param minPrice minimum price
     * @param maxPrice maximum price
     * @deprecated Use {@code chart.getYAxis().setVisibleRange(minPrice, maxPrice)} instead
     */
    @Deprecated
    public void setPriceRange(double minPrice, double maxPrice) {
        if (maxPrice < minPrice) {
            throw new IllegalArgumentException("Max price must be >= min price");
        }
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    /**
     * Sets the screen dimensions.
     */
    public void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    /**
     * Sets the chart area insets.
     */
    public void setInsets(int left, int right, int top, int bottom) {
        this.leftInset = left;
        this.rightInset = right;
        this.topInset = top;
        this.bottomInset = bottom;
    }

    /**
     * Enables or disables automatic Y-axis scaling.
     *
     * @param autoScaleY true to enable auto-scaling
     * @deprecated Use {@code chart.getYAxis().setAutoRange(AutoRangeMode.ALWAYS)} or
     *             {@code chart.getYAxis().setAutoRange(AutoRangeMode.NEVER)} instead
     */
    @Deprecated
    public void setAutoScaleY(boolean autoScaleY) {
        this.autoScaleY = autoScaleY;
    }

    /**
     * Sets the padding percentage for auto-scaling (default 0.05 = 5%).
     *
     * @param padding padding as a fraction (e.g., 0.05 = 5%)
     * @deprecated Use {@code chart.getYAxis().setGrowBy(padding)} instead
     */
    @Deprecated
    public void setAutoScalePadding(double padding) {
        this.autoScalePadding = padding;
    }

    // ========== Navigation ==========

    // Accumulated sub-unit pan amounts (for categorical charts where small deltas round to 0)
    private double accumulatedPanX = 0;

    /**
     * Pans the viewport by the given pixel deltas.
     */
    public void pan(double deltaX, double deltaY) {
        // Calculate time delta using floating point to avoid truncation
        double timePerPixel = (double) getVisibleDuration() / getChartWidth();
        double exactTimeDelta = deltaX * timePerPixel;

        // Accumulate sub-unit movements (important for categorical charts with small ranges)
        accumulatedPanX += exactTimeDelta;

        // Only apply whole-unit changes to the time range
        long timeDelta = (long) accumulatedPanX;
        if (timeDelta != 0) {
            startTime -= timeDelta;
            endTime -= timeDelta;
            accumulatedPanX -= timeDelta;  // Keep the remainder for next pan
        }

        // Calculate price delta (inverted because Y grows downward)
        if (!autoScaleY) {
            double pricePerPixel = getVisiblePriceSpan() / getChartHeight();
            double priceDelta = deltaY * pricePerPixel;

            minPrice += priceDelta;
            maxPrice += priceDelta;
        }
    }

    /**
     * Zooms the viewport by the given factor, centered at the specified screen point.
     *
     * @param factor zoom factor (>1 zooms in, <1 zooms out)
     * @param anchorX X coordinate of zoom center
     * @param anchorY Y coordinate of zoom center
     */
    public void zoom(double factor, double anchorX, double anchorY) {
        // Clamp factor to reasonable bounds
        factor = Math.max(0.1, Math.min(10.0, factor));

        // Zoom time axis
        long anchorTime = xToTime(anchorX);
        long leftDuration = anchorTime - startTime;
        long rightDuration = endTime - anchorTime;

        startTime = anchorTime - (long) (leftDuration / factor);
        endTime = anchorTime + (long) (rightDuration / factor);

        // Ensure minimum duration (1 unit - works for both milliseconds and category indices)
        if (endTime - startTime < 1) {
            endTime = startTime + 1;
        }

        // Zoom price axis (only if not auto-scaling)
        if (!autoScaleY) {
            double anchorPrice = yToPrice(anchorY);
            double bottomSpan = anchorPrice - minPrice;
            double topSpan = maxPrice - anchorPrice;

            minPrice = anchorPrice - bottomSpan / factor;
            maxPrice = anchorPrice + topSpan / factor;
        }
    }

    /**
     * Zooms the time axis only (horizontal zoom).
     */
    public void zoomTime(double factor, double anchorX) {
        long anchorTime = xToTime(anchorX);
        long leftDuration = anchorTime - startTime;
        long rightDuration = endTime - anchorTime;

        startTime = anchorTime - (long) (leftDuration / factor);
        endTime = anchorTime + (long) (rightDuration / factor);

        // Ensure minimum duration (1 unit - works for both milliseconds and category indices)
        if (endTime - startTime < 1) {
            endTime = startTime + 1;
        }
    }

    /**
     * Zooms the price axis only (vertical zoom).
     *
     * @param factor zoom factor (>1 zooms in, <1 zooms out)
     * @param anchorY Y coordinate of zoom center
     */
    public void zoomPrice(double factor, double anchorY) {
        factor = Math.max(0.1, Math.min(10.0, factor));

        double anchorPrice = yToPrice(anchorY);
        double bottomSpan = anchorPrice - minPrice;
        double topSpan = maxPrice - anchorPrice;

        minPrice = anchorPrice - bottomSpan / factor;
        maxPrice = anchorPrice + topSpan / factor;

        // Ensure minimum price range
        double minRange = 0.01;
        if (maxPrice - minPrice < minRange) {
            double center = (minPrice + maxPrice) / 2;
            minPrice = center - minRange / 2;
            maxPrice = center + minRange / 2;
        }
    }

    /**
     * Fits the viewport to the given OHLC data.
     *
     * @param data the OHLC data to fit to
     */
    public void fitToData(OhlcData data) {
        if (data.isEmpty()) {
            return;
        }

        // Fit time range
        startTime = data.getMinX();
        endTime = data.getMaxX();

        // Add some padding to time range
        long timePadding = (long) ((endTime - startTime) * 0.02);
        startTime -= timePadding;
        endTime += timePadding;

        // Fit price range
        autoScalePriceToData(data);
    }

    /**
     * Auto-scales the price range to fit the visible OHLC data.
     *
     * @param data the OHLC data to scale to
     */
    public void autoScalePriceToData(OhlcData data) {
        if (data.isEmpty()) {
            return;
        }

        int firstVisible = data.indexAtOrAfter(startTime);
        int lastVisible = data.indexAtOrBefore(endTime);

        if (firstVisible < 0 || lastVisible < 0 || firstVisible > lastVisible) {
            return;
        }

        double high = data.findHighestHigh(firstVisible, lastVisible);
        double low = data.findLowestLow(firstVisible, lastVisible);

        double padding = (high - low) * autoScalePadding;
        minPrice = low - padding;
        maxPrice = high + padding;
    }

    /**
     * Creates a copy of this viewport.
     */
    public Viewport copy() {
        Viewport copy = new Viewport();
        copy.startTime = this.startTime;
        copy.endTime = this.endTime;
        copy.minPrice = this.minPrice;
        copy.maxPrice = this.maxPrice;
        copy.width = this.width;
        copy.height = this.height;
        copy.leftInset = this.leftInset;
        copy.rightInset = this.rightInset;
        copy.topInset = this.topInset;
        copy.bottomInset = this.bottomInset;
        copy.autoScaleY = this.autoScaleY;
        copy.autoScalePadding = this.autoScalePadding;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("Viewport[time=%d-%d, price=%.2f-%.2f, size=%dx%d]",
                startTime, endTime, minPrice, maxPrice, width, height);
    }
}
