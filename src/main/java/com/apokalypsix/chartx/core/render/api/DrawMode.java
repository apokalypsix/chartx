package com.apokalypsix.chartx.core.render.api;

/**
 * Primitive drawing modes.
 */
public enum DrawMode {
    /**
     * Draw triangles (3 vertices per triangle).
     */
    TRIANGLES,

    /**
     * Draw triangle strip (shared vertices).
     */
    TRIANGLE_STRIP,

    /**
     * Draw triangle fan (shared center vertex).
     */
    TRIANGLE_FAN,

    /**
     * Draw lines (2 vertices per line).
     */
    LINES,

    /**
     * Draw connected line strip.
     */
    LINE_STRIP,

    /**
     * Draw closed line loop.
     */
    LINE_LOOP,

    /**
     * Draw points.
     */
    POINTS
}
