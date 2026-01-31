package com.apokalypsix.chartx.chart.interaction;

/**
 * Drawing tool types for chart interaction.
 *
 * <p>When a drawing tool is active, mouse events are routed to the drawing
 * interaction handler instead of the default pan/zoom behavior.
 */
public enum DrawingTool {

    /** No drawing tool active - default pan/zoom behavior */
    NONE,

    /** Selection tool for selecting and editing existing drawings */
    SELECT,

    // ========== Line Tools ==========

    /** Trend line tool - creates a line between two points */
    TREND_LINE,

    /** Horizontal line tool - creates a line at a price level */
    HORIZONTAL_LINE,

    /** Vertical line tool - creates a line at a timestamp */
    VERTICAL_LINE,

    /** Ray tool - creates a line from a point extending in one direction */
    RAY,

    /** Extended line tool - creates an infinite line through two points */
    EXTENDED_LINE,

    /** Parallel channel tool - creates a channel with parallel lines */
    PARALLEL_CHANNEL,

    /** Regression channel tool - creates a linear regression channel */
    REGRESSION_CHANNEL,

    // ========== Shape Tools ==========

    /** Rectangle tool - creates a rectangle defined by two corners */
    RECTANGLE,

    /** Ellipse tool - creates an ellipse defined by bounding box */
    ELLIPSE,

    /** Triangle tool - creates a triangle with 3 anchor points */
    TRIANGLE,

    /** Polyline tool - creates connected line segments */
    POLYLINE,

    /** Polygon tool - creates a filled polygon */
    POLYGON,

    /** Arrow tool - creates an arrow from start to end */
    ARROW,

    /** Arc tool - creates an arc segment */
    ARC,

    // ========== Fibonacci Tools ==========

    /** Fibonacci retracement tool - creates Fibonacci levels between two points */
    FIBONACCI_RETRACEMENT,

    /** Fibonacci extension tool - creates Fibonacci extension levels */
    FIBONACCI_EXTENSION,

    /** Fibonacci time zones tool - creates vertical lines at Fibonacci intervals */
    FIBONACCI_TIME_ZONES,

    /** Fibonacci fan tool - creates rays at Fibonacci angles */
    FIBONACCI_FAN,

    /** Fibonacci arc tool - creates arcs at Fibonacci distances */
    FIBONACCI_ARC,

    // ========== Gann Tools ==========

    /** Gann fan tool - creates rays at Gann angles */
    GANN_FAN,

    /** Gann box tool - creates a grid with Gann proportions */
    GANN_BOX,

    // ========== Analysis Tools ==========

    /** Pitchfork (Andrew's Fork) tool - creates a pitchfork pattern */
    PITCHFORK,

    /** Measure tool - measures price and time distance */
    MEASURE_TOOL,

    /** Price range tool - creates a horizontal price zone */
    PRICE_RANGE,

    // ========== Annotation Tools ==========

    /** Callout tool - creates a text annotation with pointer */
    CALLOUT,

    /** Price label tool - creates a price label at a location */
    PRICE_LABEL,

    /** Note tool - creates an expandable note */
    NOTE;

    /**
     * Returns true if this tool creates a new drawing.
     */
    public boolean isCreationTool() {
        return switch (this) {
            case NONE, SELECT -> false;
            default -> true;
        };
    }

    /**
     * Returns the number of clicks required to complete a drawing with this tool.
     * Returns -1 for tools that accept variable number of points (like Polyline/Polygon).
     */
    public int getRequiredClicks() {
        return switch (this) {
            case HORIZONTAL_LINE, VERTICAL_LINE, PRICE_LABEL, NOTE -> 1;
            case TREND_LINE, RAY, EXTENDED_LINE, RECTANGLE, ELLIPSE,
                 FIBONACCI_RETRACEMENT, FIBONACCI_EXTENSION, FIBONACCI_TIME_ZONES,
                 FIBONACCI_FAN, FIBONACCI_ARC, GANN_FAN, GANN_BOX,
                 REGRESSION_CHANNEL, MEASURE_TOOL, ARROW, PRICE_RANGE, CALLOUT, ARC -> 2;
            case PARALLEL_CHANNEL, PITCHFORK, TRIANGLE -> 3;
            case POLYLINE, POLYGON -> -1;  // Variable number of points
            default -> 0;
        };
    }

    /**
     * Returns true if this tool accepts a variable number of points.
     */
    public boolean isVariablePointTool() {
        return this == POLYLINE || this == POLYGON;
    }
}
