package com.apokalypsix.chartx.core.interaction;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.chart.data.OHLCBar;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Hit tester for detecting clicks/hovers on data points (candles/bars).
 *
 * <p>Given screen coordinates, this class determines which bar (if any) is
 * at that position and provides the bar's data values.
 *
 * <p>Hit testing considers:
 * <ul>
 *   <li>Horizontal position: within the bar's time range</li>
 *   <li>Vertical position: optionally within the bar's price range</li>
 *   <li>Configurable hit distance tolerance</li>
 * </ul>
 */
public class DataPointHitTester {

    /**
     * Result of a hit test operation.
     */
    public record HitResult(
            boolean hit,
            int barIndex,
            long timestamp,
            float open,
            float high,
            float low,
            float close,
            float volume,
            double screenX,
            double screenY,
            HitRegion region
    ) {
        /** Empty result indicating no hit */
        public static final HitResult MISS = new HitResult(
                false, -1, 0, 0, 0, 0, 0, 0, 0, 0, HitRegion.NONE
        );

        /**
         * Creates a hit result.
         */
        public static HitResult of(int barIndex, OhlcData series, double screenX, double screenY,
                                   HitRegion region) {
            OHLCBar bar = new OHLCBar();
            series.getBar(barIndex, bar);
            return new HitResult(
                    true, barIndex,
                    bar.getTimestamp(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume(),
                    screenX, screenY, region
            );
        }

        /**
         * Returns true if a bar was hit.
         */
        public boolean isHit() {
            return hit;
        }

        /**
         * Returns the bar as an OHLCBar instance.
         */
        public OHLCBar toBar() {
            if (!hit) return null;
            return new OHLCBar(timestamp, open, high, low, close, volume);
        }
    }

    /**
     * Region of the bar that was hit.
     */
    public enum HitRegion {
        NONE,
        BODY,           // The candle body (open-close range)
        UPPER_WICK,     // Upper wick (high to max of open/close)
        LOWER_WICK,     // Lower wick (low to min of open/close)
        BAR_AREA        // Within bar time range but not specifically on candle
    }

    private OhlcData series;
    private long barDuration;  // Duration of one bar in milliseconds
    private boolean strictVertical = false;  // If true, require vertical hit on candle
    private int horizontalTolerance = 0;  // Extra pixels for horizontal hit detection

    /**
     * Creates a hit tester.
     */
    public DataPointHitTester() {
    }

    /**
     * Sets the series to test against.
     *
     * @param series the OHLC series
     */
    public void setSeries(OhlcData series) {
        this.series = series;
    }

    /**
     * Sets the bar duration for hit testing.
     *
     * @param barDuration duration of one bar in milliseconds
     */
    public void setBarDuration(long barDuration) {
        this.barDuration = barDuration;
    }

    /**
     * Sets whether vertical position must be within the candle's price range.
     * If false (default), any vertical position within the bar's time range counts.
     *
     * @param strictVertical true for strict vertical hit testing
     */
    public void setStrictVertical(boolean strictVertical) {
        this.strictVertical = strictVertical;
    }

    /**
     * Sets extra horizontal tolerance in pixels.
     *
     * @param tolerance extra pixels on each side of the bar
     */
    public void setHorizontalTolerance(int tolerance) {
        this.horizontalTolerance = tolerance;
    }

    /**
     * Tests if a screen position hits a data point.
     *
     * @param screenX screen X coordinate
     * @param screenY screen Y coordinate
     * @param coords coordinate system for transformations
     * @return the hit result
     */
    public HitResult hitTest(double screenX, double screenY, CoordinateSystem coords) {
        if (series == null || series.isEmpty() || coords == null) {
            return HitResult.MISS;
        }

        // Convert screen position to data coordinates
        long timestamp = coords.screenXToXValue(screenX);
        double price = coords.screenYToYValue(screenY);

        // Find the bar index at this timestamp
        int index = findBarIndex(timestamp);
        if (index < 0) {
            return HitResult.MISS;
        }

        // Check horizontal bounds with tolerance
        long barStart = series.getXValue(index);
        long barEnd = barStart + barDuration;

        double barStartX = coords.xValueToScreenX(barStart);
        double barEndX = coords.xValueToScreenX(barEnd);

        if (screenX < barStartX - horizontalTolerance || screenX > barEndX + horizontalTolerance) {
            return HitResult.MISS;
        }

        // Get bar data
        OHLCBar bar = new OHLCBar();
        series.getBar(index, bar);

        // Determine hit region
        HitRegion region;
        if (strictVertical) {
            region = getHitRegion(bar, price);
            if (region == HitRegion.NONE) {
                return HitResult.MISS;
            }
        } else {
            region = getHitRegion(bar, price);
            if (region == HitRegion.NONE) {
                region = HitRegion.BAR_AREA;
            }
        }

        return new HitResult(
                true, index,
                bar.getTimestamp(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume(),
                screenX, screenY, region
        );
    }

    /**
     * Finds the bar index at or before the given timestamp.
     */
    private int findBarIndex(long timestamp) {
        if (series == null || series.isEmpty()) {
            return -1;
        }

        int index = series.indexAtOrBefore(timestamp);
        if (index < 0) {
            return -1;
        }

        // Verify the timestamp is within this bar's range
        long barStart = series.getXValue(index);
        if (barDuration > 0 && timestamp >= barStart + barDuration) {
            // Timestamp is after this bar ends, check next bar
            if (index + 1 < series.size()) {
                long nextBarStart = series.getXValue(index + 1);
                if (timestamp >= nextBarStart && timestamp < nextBarStart + barDuration) {
                    return index + 1;
                }
            }
            return -1;  // In a gap between bars
        }

        return index;
    }

    /**
     * Determines which region of the bar the price is in.
     */
    private HitRegion getHitRegion(OHLCBar bar, double price) {
        if (price < bar.getLow() || price > bar.getHigh()) {
            return HitRegion.NONE;
        }

        float bodyTop = Math.max(bar.getOpen(), bar.getClose());
        float bodyBottom = Math.min(bar.getOpen(), bar.getClose());

        if (price >= bodyBottom && price <= bodyTop) {
            return HitRegion.BODY;
        } else if (price > bodyTop) {
            return HitRegion.UPPER_WICK;
        } else {
            return HitRegion.LOWER_WICK;
        }
    }

    /**
     * Tests if a screen position is near any bar within the horizontal range.
     * Returns the nearest bar index even if not directly on a bar.
     *
     * @param screenX screen X coordinate
     * @param coords coordinate system for transformations
     * @return the nearest bar index, or -1 if no bars
     */
    public int getNearestBarIndex(double screenX, CoordinateSystem coords) {
        if (series == null || series.isEmpty()) {
            return -1;
        }

        long timestamp = coords.screenXToXValue(screenX);
        int index = series.indexAtOrBefore(timestamp);

        if (index < 0) {
            return 0;  // Before all bars, return first
        }

        if (index >= series.size() - 1) {
            return series.size() - 1;  // After all bars, return last
        }

        // Check if closer to current or next bar
        long currentBarTime = series.getXValue(index);
        long nextBarTime = series.getXValue(index + 1);

        if (Math.abs(timestamp - currentBarTime) <= Math.abs(timestamp - nextBarTime)) {
            return index;
        } else {
            return index + 1;
        }
    }

    /**
     * Gets the bar at the given index.
     *
     * @param index the bar index
     * @return the bar data, or null if index is invalid
     */
    public OHLCBar getBar(int index) {
        if (series == null || index < 0 || index >= series.size()) {
            return null;
        }
        OHLCBar bar = new OHLCBar();
        series.getBar(index, bar);
        return bar;
    }
}
