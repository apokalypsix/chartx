package com.apokalypsix.chartx.chart.style;

import com.apokalypsix.chartx.core.data.StackingCalculator;

/**
 * Rendering options for stacked series (mountain, column).
 *
 * <p>Provides configuration for stacking mode (normal or 100%),
 * opacity, and line rendering. Colors are managed per-series
 * in the StackedSeriesGroup.
 */
public class StackedSeriesOptions extends SeriesOptions {

    /** Stacking mode */
    private StackingCalculator.StackMode stackMode = StackingCalculator.StackMode.NORMAL;

    /** Overall opacity for fill (0.0 - 1.0) */
    private float fillOpacity = 0.7f;

    /** Line width for borders (0 = no border) */
    private float lineWidth = 1.0f;

    /** Line opacity (0.0 - 1.0) */
    private float lineOpacity = 1.0f;

    /** Whether to show lines between stacked areas */
    private boolean showLines = true;

    /**
     * Creates default stacked series options.
     */
    public StackedSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public StackedSeriesOptions(StackedSeriesOptions other) {
        super(other);
        this.stackMode = other.stackMode;
        this.fillOpacity = other.fillOpacity;
        this.lineWidth = other.lineWidth;
        this.lineOpacity = other.lineOpacity;
        this.showLines = other.showLines;
    }

    // ========== Getters ==========

    public StackingCalculator.StackMode getStackMode() {
        return stackMode;
    }

    public float getFillOpacity() {
        return fillOpacity;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public float getLineOpacity() {
        return lineOpacity;
    }

    public boolean isShowLines() {
        return showLines;
    }

    // ========== Fluent setters ==========

    /**
     * Sets the stacking mode.
     *
     * @param mode the stacking mode (NORMAL or PERCENT_100)
     * @return this for chaining
     */
    public StackedSeriesOptions stackMode(StackingCalculator.StackMode mode) {
        this.stackMode = mode != null ? mode : StackingCalculator.StackMode.NORMAL;
        return this;
    }

    /**
     * Enables 100% stacking mode.
     *
     * @return this for chaining
     */
    public StackedSeriesOptions percent100() {
        this.stackMode = StackingCalculator.StackMode.PERCENT_100;
        return this;
    }

    /**
     * Sets the fill opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public StackedSeriesOptions fillOpacity(float opacity) {
        this.fillOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    /**
     * Sets the line width for borders.
     *
     * @param lineWidth the width (0 = no border)
     * @return this for chaining
     */
    public StackedSeriesOptions lineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.0f, lineWidth);
        return this;
    }

    /**
     * Sets the line opacity.
     *
     * @param opacity the opacity (0.0 - 1.0)
     * @return this for chaining
     */
    public StackedSeriesOptions lineOpacity(float opacity) {
        this.lineOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
        return this;
    }

    /**
     * Sets whether to show lines between stacked areas.
     *
     * @param showLines true to show lines
     * @return this for chaining
     */
    public StackedSeriesOptions showLines(boolean showLines) {
        this.showLines = showLines;
        return this;
    }

    // ========== Override parent methods for proper return type ==========

    @Override
    public StackedSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public StackedSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public StackedSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public StackedSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
