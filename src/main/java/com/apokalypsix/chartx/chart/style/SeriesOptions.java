package com.apokalypsix.chartx.chart.style;

/**
 * Base class for series rendering options.
 *
 * <p>Contains common styling properties shared by all series types:
 * axis binding, visibility, and z-order.
 *
 * <p>Subclasses add type-specific styling (colors, line widths, etc.).
 * All options classes use a fluent builder pattern for easy configuration.
 */
public class SeriesOptions {

    /** ID of the X-axis this series is bound to */
    private String xAxisId = "default";

    /** ID of the Y-axis this series is bound to */
    private String yAxisId = "default";

    /** Whether the series is visible */
    private boolean visible = true;

    /** Render order within the layer (higher = rendered on top) */
    private int zOrder = 0;

    /**
     * Creates default series options.
     */
    public SeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    protected SeriesOptions(SeriesOptions other) {
        this.xAxisId = other.xAxisId;
        this.yAxisId = other.yAxisId;
        this.visible = other.visible;
        this.zOrder = other.zOrder;
    }

    // ========== Getters ==========

    public String getXAxisId() {
        return xAxisId;
    }

    public String getYAxisId() {
        return yAxisId;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getZOrder() {
        return zOrder;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the X-axis ID this series is bound to.
     *
     * @param xAxisId the axis ID
     * @return this for chaining
     */
    public SeriesOptions xAxisId(String xAxisId) {
        this.xAxisId = xAxisId != null ? xAxisId : "default";
        return this;
    }

    /**
     * Sets the Y-axis ID this series is bound to.
     *
     * @param yAxisId the axis ID
     * @return this for chaining
     */
    public SeriesOptions yAxisId(String yAxisId) {
        this.yAxisId = yAxisId != null ? yAxisId : "default";
        return this;
    }

    /**
     * Sets whether the series is visible.
     *
     * @param visible true to show, false to hide
     * @return this for chaining
     */
    public SeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets the render order within the layer.
     * Higher values are rendered on top of lower values.
     *
     * @param zOrder the z-order value
     * @return this for chaining
     */
    public SeriesOptions zOrder(int zOrder) {
        this.zOrder = zOrder;
        return this;
    }

    // ========== Standard setters for frameworks that need them ==========

    public void setXAxisId(String xAxisId) {
        xAxisId(xAxisId);
    }

    public void setYAxisId(String yAxisId) {
        yAxisId(yAxisId);
    }

    public void setVisible(boolean visible) {
        visible(visible);
    }

    public void setZOrder(int zOrder) {
        zOrder(zOrder);
    }
}
