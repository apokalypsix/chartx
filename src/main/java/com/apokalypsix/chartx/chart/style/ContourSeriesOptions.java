package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

import com.apokalypsix.chartx.core.render.util.ColorMap;

/**
 * Rendering options for contour series.
 *
 * <p>Controls contour line appearance, level generation, and fill options.
 */
public class ContourSeriesOptions extends SeriesOptions {

    /** Color map for contour lines/fills */
    private ColorMap colorMap = ColorMap.viridis();

    /** Whether to auto-generate contour levels */
    private boolean autoLevels = true;

    /** Number of contour levels (when auto) */
    private int levelCount = 10;

    /** Manual contour levels (when not auto) */
    private float[] levels = null;

    /** Whether to show contour lines */
    private boolean showLines = true;

    /** Contour line width */
    private float lineWidth = 1.5f;

    /** Line color (null = use color map) */
    private Color lineColor = null;

    /** Whether to show filled contours */
    private boolean showFill = false;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether to show labels on contour lines */
    private boolean showLabels = false;

    /** Label font size */
    private float labelFontSize = 10f;

    /** Label color */
    private Color labelColor = new Color(40, 40, 40);

    /**
     * Creates default contour series options.
     */
    public ContourSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public ContourSeriesOptions(ContourSeriesOptions other) {
        super(other);
        this.colorMap = other.colorMap;
        this.autoLevels = other.autoLevels;
        this.levelCount = other.levelCount;
        this.levels = other.levels != null ? other.levels.clone() : null;
        this.showLines = other.showLines;
        this.lineWidth = other.lineWidth;
        this.lineColor = other.lineColor;
        this.showFill = other.showFill;
        this.opacity = other.opacity;
        this.showLabels = other.showLabels;
        this.labelFontSize = other.labelFontSize;
        this.labelColor = other.labelColor;
    }

    // ========== Getters ==========

    public ColorMap getColorMap() {
        return colorMap;
    }

    public boolean isAutoLevels() {
        return autoLevels;
    }

    public int getLevelCount() {
        return levelCount;
    }

    public float[] getLevels() {
        return levels;
    }

    public boolean isShowLines() {
        return showLines;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public boolean isShowFill() {
        return showFill;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public float getLabelFontSize() {
        return labelFontSize;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    /**
     * Generates contour levels based on data range.
     *
     * @param minValue minimum data value
     * @param maxValue maximum data value
     * @return array of contour threshold values
     */
    public float[] generateLevels(float minValue, float maxValue) {
        if (!autoLevels && levels != null) {
            return levels;
        }

        float range = maxValue - minValue;
        float step = range / (levelCount + 1);

        float[] generated = new float[levelCount];
        for (int i = 0; i < levelCount; i++) {
            generated[i] = minValue + step * (i + 1);
        }
        return generated;
    }

    // ========== Fluent Setters ==========

    public ContourSeriesOptions colorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
        return this;
    }

    public ContourSeriesOptions autoLevels(boolean auto) {
        this.autoLevels = auto;
        return this;
    }

    public ContourSeriesOptions levelCount(int count) {
        this.levelCount = Math.max(1, Math.min(50, count));
        this.autoLevels = true;
        return this;
    }

    public ContourSeriesOptions levels(float... levels) {
        this.levels = levels;
        this.autoLevels = false;
        return this;
    }

    public ContourSeriesOptions showLines(boolean show) {
        this.showLines = show;
        return this;
    }

    public ContourSeriesOptions lineWidth(float width) {
        this.lineWidth = Math.max(0.5f, width);
        return this;
    }

    public ContourSeriesOptions lineColor(Color color) {
        this.lineColor = color;
        return this;
    }

    public ContourSeriesOptions showFill(boolean show) {
        this.showFill = show;
        return this;
    }

    public ContourSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public ContourSeriesOptions showLabels(boolean show) {
        this.showLabels = show;
        return this;
    }

    public ContourSeriesOptions labelFontSize(float size) {
        this.labelFontSize = Math.max(6, size);
        return this;
    }

    public ContourSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    // ========== Override parent methods ==========

    @Override
    public ContourSeriesOptions xAxisId(String xAxisId) {
        super.xAxisId(xAxisId);
        return this;
    }

    @Override
    public ContourSeriesOptions yAxisId(String yAxisId) {
        super.yAxisId(yAxisId);
        return this;
    }

    @Override
    public ContourSeriesOptions visible(boolean visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public ContourSeriesOptions zOrder(int zOrder) {
        super.zOrder(zOrder);
        return this;
    }
}
