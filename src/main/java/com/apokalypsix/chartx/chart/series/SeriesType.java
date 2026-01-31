package com.apokalypsix.chartx.chart.series;

/**
 * Type enumeration for renderable series.
 */
public enum SeriesType {
    /** Line series (XyData) */
    LINE,
    /** Candlestick/OHLC series (OhlcData) */
    CANDLESTICK,
    /** Histogram/bar series (HistogramData) */
    HISTOGRAM,
    /** Band series with upper/middle/lower (XyyData) */
    BAND,
    /** Scatter plot series (XyData) */
    SCATTER,
    /** Spline line series (XyData) */
    SPLINE_LINE,
    /** Spline mountain/area series (XyData) */
    SPLINE_MOUNTAIN,
    /** Stacked mountain/area series (XyData groups) */
    STACKED_MOUNTAIN,
    /** Grouped column series (grouped values per timestamp) */
    GROUPED_COLUMN,
    /** Stacked column series (XyData groups) */
    STACKED_COLUMN,
    /** Horizontal bar series (HistogramData) */
    HORIZONTAL_BAR,
    /** Bubble series (BubbleData) */
    BUBBLE,
    /** Box plot series (BoxWhiskerData) */
    BOX_PLOT,
    /** Error bar series (XyyData) */
    ERROR_BAR,
    /** Waterfall series (WaterfallData) */
    WATERFALL,
    /** Impulse/stem series (XyData) */
    IMPULSE,
    /** Pie chart series (PieData) */
    PIE,
    /** Donut chart series (PieData with inner radius) */
    DONUT,
    /** Polar line series (XyData in polar coordinates) */
    POLAR_LINE,
    /** Polar mountain/area series (XyData in polar coordinates) */
    POLAR_MOUNTAIN,
    /** Polar column series (XyData as radial bars) */
    POLAR_COLUMN,
    /** Radar/spider chart series (multi-axis polar) */
    RADAR,
    /** Heatmap series (2D grid) */
    HEATMAP,
    /** Contour series (iso-lines from 2D data) */
    CONTOUR,
    /** Linear gauge series (horizontal/vertical bar gauge) */
    LINEAR_GAUGE,
    /** Radial gauge series (arc-based with needle) */
    RADIAL_GAUGE,
    /** Gantt chart series (project tasks and dependencies) */
    GANTT,
    /** Treemap series (hierarchical rectangles) */
    TREEMAP,
    /** Sunburst series (hierarchical donut rings) */
    SUNBURST,
    /** Vector field series (directional arrows) */
    VECTOR_FIELD
}
