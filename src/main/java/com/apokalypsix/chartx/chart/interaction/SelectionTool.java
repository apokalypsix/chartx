package com.apokalypsix.chartx.chart.interaction;

/**
 * Enum defining selection tool modes.
 *
 * <p>Selection tools allow users to select regions on the chart
 * for various purposes (zoom, analysis, measurement, etc.).
 */
public enum SelectionTool {

    /**
     * No selection tool active.
     */
    NONE,

    /**
     * Rectangular selection - drag to create a rectangular region.
     * Returns both time range and price range.
     */
    RECTANGLE,

    /**
     * Horizontal time range selection - drag horizontally.
     * Returns only time range (full price range).
     */
    TIME_RANGE,

    /**
     * Vertical price range selection - drag vertically.
     * Returns only price range (full time range).
     */
    PRICE_RANGE,

    /**
     * Crosshair selection - click to select single point.
     * Returns time and price at click location.
     */
    CROSSHAIR,

    /**
     * Zoom to selection - same as RECTANGLE but zooms after selection.
     */
    ZOOM_TO_SELECTION;

    /**
     * Returns true if this tool allows horizontal selection.
     */
    public boolean allowsHorizontalSelection() {
        return this == RECTANGLE || this == TIME_RANGE || this == ZOOM_TO_SELECTION;
    }

    /**
     * Returns true if this tool allows vertical selection.
     */
    public boolean allowsVerticalSelection() {
        return this == RECTANGLE || this == PRICE_RANGE || this == ZOOM_TO_SELECTION;
    }

    /**
     * Returns true if this is a drag-based selection.
     */
    public boolean isDragSelection() {
        return this == RECTANGLE || this == TIME_RANGE ||
               this == PRICE_RANGE || this == ZOOM_TO_SELECTION;
    }

    /**
     * Returns true if this is a click-based selection.
     */
    public boolean isClickSelection() {
        return this == CROSSHAIR;
    }
}
