package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

import com.apokalypsix.chartx.core.render.util.ColorMap;

/**
 * Rendering options for heatmap series.
 *
 * <p>Controls color mapping, cell appearance, and value range.
 */
public class HeatmapSeriesOptions extends SeriesOptions {

    /** Color map for value-to-color mapping */
    private ColorMap colorMap = ColorMap.viridis();

    /** Whether to auto-scale color range to data */
    private boolean autoScale = true;

    /** Manual min value for color scale (when autoScale is false) */
    private float minValue = 0;

    /** Manual max value for color scale */
    private float maxValue = 1;

    /** Whether to show cell borders */
    private boolean showCellBorders = false;

    /** Cell border color */
    private Color cellBorderColor = new Color(40, 40, 40, 100);

    /** Cell border width */
    private float cellBorderWidth = 0.5f;

    /** Color for NaN/missing values (null = transparent) */
    private Color nanColor = null;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether to interpolate between cells (smooth) */
    private boolean interpolate = false;

    /**
     * Creates default heatmap series options.
     */
    public HeatmapSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public HeatmapSeriesOptions(HeatmapSeriesOptions other) {
        super(other);
        this.colorMap = other.colorMap;
        this.autoScale = other.autoScale;
        this.minValue = other.minValue;
        this.maxValue = other.maxValue;
        this.showCellBorders = other.showCellBorders;
        this.cellBorderColor = other.cellBorderColor;
        this.cellBorderWidth = other.cellBorderWidth;
        this.nanColor = other.nanColor;
        this.opacity = other.opacity;
        this.interpolate = other.interpolate;
    }

    // ========== Getters ==========

    public ColorMap getColorMap() {
        return colorMap;
    }

    public boolean isAutoScale() {
        return autoScale;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public boolean isShowCellBorders() {
        return showCellBorders;
    }

    public Color getCellBorderColor() {
        return cellBorderColor;
    }

    public float getCellBorderWidth() {
        return cellBorderWidth;
    }

    public Color getNanColor() {
        return nanColor;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    // ========== Fluent Setters ==========

    public HeatmapSeriesOptions colorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
        return this;
    }

    public HeatmapSeriesOptions viridis() {
        this.colorMap = ColorMap.viridis();
        return this;
    }

    public HeatmapSeriesOptions plasma() {
        this.colorMap = ColorMap.plasma();
        return this;
    }

    public HeatmapSeriesOptions jet() {
        this.colorMap = ColorMap.jet();
        return this;
    }

    public HeatmapSeriesOptions grayscale() {
        this.colorMap = ColorMap.grayscale();
        return this;
    }

    public HeatmapSeriesOptions thermal() {
        this.colorMap = ColorMap.thermal();
        return this;
    }

    public HeatmapSeriesOptions autoScale(boolean autoScale) {
        this.autoScale = autoScale;
        return this;
    }

    public HeatmapSeriesOptions valueRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        this.autoScale = false;
        return this;
    }

    public HeatmapSeriesOptions showCellBorders(boolean show) {
        this.showCellBorders = show;
        return this;
    }

    public HeatmapSeriesOptions cellBorderColor(Color color) {
        this.cellBorderColor = color;
        return this;
    }

    public HeatmapSeriesOptions cellBorderWidth(float width) {
        this.cellBorderWidth = Math.max(0, width);
        return this;
    }

    public HeatmapSeriesOptions nanColor(Color color) {
        this.nanColor = color;
        return this;
    }

    public HeatmapSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public HeatmapSeriesOptions interpolate(boolean interpolate) {
        this.interpolate = interpolate;
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public HeatmapSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public HeatmapSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public HeatmapSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public HeatmapSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
