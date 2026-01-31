package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rendering options for radar/spider chart series.
 *
 * <p>Controls appearance of radar polygons, grid, and axis labels.
 */
public class RadarSeriesOptions {

    /** Default color palette for series */
    private static final Color[] DEFAULT_COLORS = {
        new Color(65, 131, 196, 180),   // Blue
        new Color(220, 100, 50, 180),   // Orange
        new Color(50, 170, 80, 180),    // Green
        new Color(200, 60, 60, 180),    // Red
        new Color(155, 89, 182, 180),   // Purple
    };

    /** Colors for each series (cycles if fewer colors than series) */
    private List<Color> seriesColors;

    /** Line width for polygon edges */
    private float lineWidth = 2.0f;

    /** Fill opacity for polygons (0-1) */
    private float fillOpacity = 0.3f;

    /** Whether to fill the polygons */
    private boolean showFill = true;

    /** Whether to show dots at data points */
    private boolean showPoints = true;

    /** Point size in pixels */
    private float pointSize = 4.0f;

    // Grid options
    /** Whether to show the grid */
    private boolean showGrid = true;

    /** Grid line color */
    private Color gridColor = new Color(100, 100, 100, 150);

    /** Grid line width */
    private float gridWidth = 1.0f;

    /** Number of concentric grid circles */
    private int gridLevels = 5;

    /** Whether to show the spoke lines (axis lines) */
    private boolean showSpokes = true;

    /** Spoke line color */
    private Color spokeColor = new Color(120, 120, 120);

    /** Spoke line width */
    private float spokeWidth = 1.0f;

    // Axis labels
    /** Whether to show axis labels */
    private boolean showAxisLabels = true;

    /** Axis label color */
    private Color axisLabelColor = new Color(180, 180, 180);

    /** Axis label font size */
    private float axisLabelFontSize = 12f;

    /** Padding ratio around the chart */
    private float paddingRatio = 0.15f;

    /** Whether chart is visible */
    private boolean visible = true;

    /**
     * Creates default radar series options.
     */
    public RadarSeriesOptions() {
        this.seriesColors = new ArrayList<>(Arrays.asList(DEFAULT_COLORS));
    }

    /**
     * Creates a copy of the given options.
     */
    public RadarSeriesOptions(RadarSeriesOptions other) {
        this.seriesColors = new ArrayList<>(other.seriesColors);
        this.lineWidth = other.lineWidth;
        this.fillOpacity = other.fillOpacity;
        this.showFill = other.showFill;
        this.showPoints = other.showPoints;
        this.pointSize = other.pointSize;
        this.showGrid = other.showGrid;
        this.gridColor = other.gridColor;
        this.gridWidth = other.gridWidth;
        this.gridLevels = other.gridLevels;
        this.showSpokes = other.showSpokes;
        this.spokeColor = other.spokeColor;
        this.spokeWidth = other.spokeWidth;
        this.showAxisLabels = other.showAxisLabels;
        this.axisLabelColor = other.axisLabelColor;
        this.axisLabelFontSize = other.axisLabelFontSize;
        this.paddingRatio = other.paddingRatio;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    /**
     * Returns the color for the series at the given index.
     */
    public Color getSeriesColor(int index) {
        if (seriesColors.isEmpty()) {
            return DEFAULT_COLORS[index % DEFAULT_COLORS.length];
        }
        return seriesColors.get(index % seriesColors.size());
    }

    /**
     * Returns the fill color for the series (with fillOpacity applied).
     */
    public Color getSeriesFillColor(int index) {
        Color base = getSeriesColor(index);
        int alpha = (int) (fillOpacity * 255);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    public List<Color> getSeriesColors() {
        return seriesColors;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public float getFillOpacity() {
        return fillOpacity;
    }

    public boolean isShowFill() {
        return showFill;
    }

    public boolean isShowPoints() {
        return showPoints;
    }

    public float getPointSize() {
        return pointSize;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public float getGridWidth() {
        return gridWidth;
    }

    public int getGridLevels() {
        return gridLevels;
    }

    public boolean isShowSpokes() {
        return showSpokes;
    }

    public Color getSpokeColor() {
        return spokeColor;
    }

    public float getSpokeWidth() {
        return spokeWidth;
    }

    public boolean isShowAxisLabels() {
        return showAxisLabels;
    }

    public Color getAxisLabelColor() {
        return axisLabelColor;
    }

    public float getAxisLabelFontSize() {
        return axisLabelFontSize;
    }

    public float getPaddingRatio() {
        return paddingRatio;
    }

    public boolean isVisible() {
        return visible;
    }

    // ========== Fluent Setters ==========

    public RadarSeriesOptions seriesColors(Color... colors) {
        this.seriesColors = new ArrayList<>(Arrays.asList(colors));
        return this;
    }

    public RadarSeriesOptions seriesColors(List<Color> colors) {
        this.seriesColors = new ArrayList<>(colors);
        return this;
    }

    public RadarSeriesOptions lineWidth(float width) {
        this.lineWidth = Math.max(0.5f, width);
        return this;
    }

    public RadarSeriesOptions fillOpacity(float opacity) {
        this.fillOpacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public RadarSeriesOptions showFill(boolean show) {
        this.showFill = show;
        return this;
    }

    public RadarSeriesOptions showPoints(boolean show) {
        this.showPoints = show;
        return this;
    }

    public RadarSeriesOptions pointSize(float size) {
        this.pointSize = Math.max(1, size);
        return this;
    }

    public RadarSeriesOptions showGrid(boolean show) {
        this.showGrid = show;
        return this;
    }

    public RadarSeriesOptions gridColor(Color color) {
        this.gridColor = color;
        return this;
    }

    public RadarSeriesOptions gridWidth(float width) {
        this.gridWidth = Math.max(0.5f, width);
        return this;
    }

    public RadarSeriesOptions gridLevels(int levels) {
        this.gridLevels = Math.max(1, Math.min(10, levels));
        return this;
    }

    public RadarSeriesOptions showSpokes(boolean show) {
        this.showSpokes = show;
        return this;
    }

    public RadarSeriesOptions spokeColor(Color color) {
        this.spokeColor = color;
        return this;
    }

    public RadarSeriesOptions spokeWidth(float width) {
        this.spokeWidth = Math.max(0.5f, width);
        return this;
    }

    public RadarSeriesOptions showAxisLabels(boolean show) {
        this.showAxisLabels = show;
        return this;
    }

    public RadarSeriesOptions axisLabelColor(Color color) {
        this.axisLabelColor = color;
        return this;
    }

    public RadarSeriesOptions axisLabelFontSize(float size) {
        this.axisLabelFontSize = Math.max(6, size);
        return this;
    }

    public RadarSeriesOptions paddingRatio(float ratio) {
        this.paddingRatio = Math.max(0, Math.min(0.4f, ratio));
        return this;
    }

    public RadarSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
