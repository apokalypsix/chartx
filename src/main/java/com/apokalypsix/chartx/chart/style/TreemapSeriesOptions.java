package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for treemap series.
 *
 * <p>Controls appearance of treemap cells including borders,
 * labels, and color schemes.
 */
public class TreemapSeriesOptions {

    /** Default cell color (used when node has no color) */
    private Color defaultColor = new Color(65, 131, 196);

    /** Border color */
    private Color borderColor = new Color(30, 30, 30);

    /** Border width */
    private float borderWidth = 1.0f;

    /** Padding between cells */
    private float padding = 2f;

    /** Minimum cell size for rendering */
    private float minCellSize = 4f;

    /** Whether to show cell labels */
    private boolean showLabels = true;

    /** Label color */
    private Color labelColor = new Color(240, 240, 240);

    /** Label font size */
    private float labelFontSize = 12f;

    /** Minimum cell size to show label */
    private float minLabelCellSize = 40f;

    /** Whether to show values in labels */
    private boolean showValues = false;

    /** Value format string */
    private String valueFormat = "%.1f";

    /** Maximum visible depth (0 = all levels) */
    private int maxVisibleDepth = 0;

    /** Whether to show group headers */
    private boolean showHeaders = false;

    /** Group header height */
    private float headerHeight = 20f;

    /** Header background color */
    private Color headerColor = new Color(50, 50, 50);

    /** Header text color */
    private Color headerTextColor = new Color(200, 200, 200);

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether visible */
    private boolean visible = true;

    /**
     * Creates default treemap options.
     */
    public TreemapSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public TreemapSeriesOptions(TreemapSeriesOptions other) {
        this.defaultColor = other.defaultColor;
        this.borderColor = other.borderColor;
        this.borderWidth = other.borderWidth;
        this.padding = other.padding;
        this.minCellSize = other.minCellSize;
        this.showLabels = other.showLabels;
        this.labelColor = other.labelColor;
        this.labelFontSize = other.labelFontSize;
        this.minLabelCellSize = other.minLabelCellSize;
        this.showValues = other.showValues;
        this.valueFormat = other.valueFormat;
        this.maxVisibleDepth = other.maxVisibleDepth;
        this.showHeaders = other.showHeaders;
        this.headerHeight = other.headerHeight;
        this.headerColor = other.headerColor;
        this.headerTextColor = other.headerTextColor;
        this.opacity = other.opacity;
        this.visible = other.visible;
    }

    // ========== Getters ==========

    public Color getDefaultColor() {
        return defaultColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getPadding() {
        return padding;
    }

    public float getMinCellSize() {
        return minCellSize;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public float getLabelFontSize() {
        return labelFontSize;
    }

    public float getMinLabelCellSize() {
        return minLabelCellSize;
    }

    public boolean isShowValues() {
        return showValues;
    }

    public String getValueFormat() {
        return valueFormat;
    }

    public int getMaxVisibleDepth() {
        return maxVisibleDepth;
    }

    public boolean isShowHeaders() {
        return showHeaders;
    }

    public float getHeaderHeight() {
        return headerHeight;
    }

    public Color getHeaderColor() {
        return headerColor;
    }

    public Color getHeaderTextColor() {
        return headerTextColor;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    // ========== Fluent Setters ==========

    public TreemapSeriesOptions defaultColor(Color color) {
        this.defaultColor = color;
        return this;
    }

    public TreemapSeriesOptions borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    public TreemapSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0, width);
        return this;
    }

    public TreemapSeriesOptions padding(float padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    public TreemapSeriesOptions minCellSize(float size) {
        this.minCellSize = Math.max(1, size);
        return this;
    }

    public TreemapSeriesOptions showLabels(boolean show) {
        this.showLabels = show;
        return this;
    }

    public TreemapSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public TreemapSeriesOptions labelFontSize(float size) {
        this.labelFontSize = Math.max(6, size);
        return this;
    }

    public TreemapSeriesOptions minLabelCellSize(float size) {
        this.minLabelCellSize = Math.max(10, size);
        return this;
    }

    public TreemapSeriesOptions showValues(boolean show) {
        this.showValues = show;
        return this;
    }

    public TreemapSeriesOptions valueFormat(String format) {
        this.valueFormat = format;
        return this;
    }

    public TreemapSeriesOptions maxVisibleDepth(int depth) {
        this.maxVisibleDepth = Math.max(0, depth);
        return this;
    }

    public TreemapSeriesOptions showHeaders(boolean show) {
        this.showHeaders = show;
        return this;
    }

    public TreemapSeriesOptions headerHeight(float height) {
        this.headerHeight = Math.max(0, height);
        return this;
    }

    public TreemapSeriesOptions headerColor(Color color) {
        this.headerColor = color;
        return this;
    }

    public TreemapSeriesOptions headerTextColor(Color color) {
        this.headerTextColor = color;
        return this;
    }

    public TreemapSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public TreemapSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
