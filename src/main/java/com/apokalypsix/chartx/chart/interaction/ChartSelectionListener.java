package com.apokalypsix.chartx.chart.interaction;

import com.apokalypsix.chartx.chart.data.OHLCBar;
import com.apokalypsix.chartx.core.interaction.DataPointHitTester;

/**
 * Listener interface for chart data point selection and interaction events.
 *
 * <p>Implementations receive callbacks for:
 * <ul>
 *   <li>Click events on data points (bars/candles)</li>
 *   <li>Hover events when mouse moves over data points</li>
 *   <li>Selection changes</li>
 *   <li>Region selection (drag-select)</li>
 * </ul>
 */
public interface ChartSelectionListener {

    /**
     * Called when a data point (bar/candle) is clicked.
     *
     * @param event the click event details
     */
    default void onDataPointClicked(DataPointClickEvent event) {}

    /**
     * Called when a data point (bar/candle) is double-clicked.
     *
     * @param event the click event details
     */
    default void onDataPointDoubleClicked(DataPointClickEvent event) {}

    /**
     * Called when the mouse hovers over a data point.
     *
     * @param event the hover event details
     */
    default void onDataPointHovered(DataPointHoverEvent event) {}

    /**
     * Called when the mouse leaves the chart or no longer hovers over any data point.
     */
    default void onHoverEnded() {}

    /**
     * Called when a data point becomes selected.
     *
     * @param event the selection event details
     */
    default void onDataPointSelected(DataPointSelectionEvent event) {}

    /**
     * Called when selection is cleared.
     */
    default void onSelectionCleared() {}

    /**
     * Called when a region is selected (drag-select).
     *
     * @param event the region selection event
     */
    default void onRegionSelected(RegionSelectionEvent event) {}

    // ========== Event Classes ==========

    /**
     * Event data for data point click.
     */
    record DataPointClickEvent(
            int barIndex,
            OHLCBar bar,
            double screenX,
            double screenY,
            DataPointHitTester.HitRegion hitRegion,
            int clickCount,
            boolean isShiftDown,
            boolean isControlDown,
            boolean isAltDown
    ) {
        /**
         * Returns true if this is a multi-select click (Ctrl/Cmd held).
         */
        public boolean isMultiSelect() {
            return isControlDown;
        }

        /**
         * Returns true if this is a range-select click (Shift held).
         */
        public boolean isRangeSelect() {
            return isShiftDown;
        }
    }

    /**
     * Event data for data point hover.
     */
    record DataPointHoverEvent(
            int barIndex,
            OHLCBar bar,
            double screenX,
            double screenY,
            DataPointHitTester.HitRegion hitRegion
    ) {}

    /**
     * Event data for data point selection.
     */
    record DataPointSelectionEvent(
            int barIndex,
            OHLCBar bar,
            boolean addedToSelection
    ) {}

    /**
     * Event data for region selection.
     */
    record RegionSelectionEvent(
            long startTime,
            long endTime,
            double topPrice,
            double bottomPrice,
            int startIndex,
            int endIndex,
            int barCount
    ) {
        /**
         * Returns the time span of the selection in milliseconds.
         */
        public long getTimeSpan() {
            return endTime - startTime;
        }

        /**
         * Returns the price range of the selection.
         */
        public double getPriceRange() {
            return topPrice - bottomPrice;
        }
    }
}
