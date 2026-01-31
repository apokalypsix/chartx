package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

import com.apokalypsix.chartx.core.render.util.ColorMap;

/**
 * Rendering options for vector field series.
 *
 * <p>Controls appearance of vectors including arrows,
 * colors, and scaling.
 */
public class VectorFieldSeriesOptions {

    /** Arrow style */
    public enum ArrowStyle {
        LINE,           // Simple line
        ARROW,          // Line with arrowhead
        TRIANGLE        // Filled triangle
    }

    /** Arrow style */
    private ArrowStyle arrowStyle = ArrowStyle.ARROW;

    /** Fixed arrow color (null = use color map based on magnitude) */
    private Color arrowColor = null;

    /** Color map for magnitude-based coloring */
    private ColorMap colorMap = ColorMap.viridis();

    /** Arrow length scale factor */
    private float lengthScale = 1.0f;

    /** Minimum arrow length in pixels */
    private float minLength = 5f;

    /** Maximum arrow length in pixels */
    private float maxLength = 50f;

    /** Arrow line width */
    private float lineWidth = 1.5f;

    /** Arrowhead size ratio (relative to arrow length) */
    private float arrowheadRatio = 0.3f;

    /** Arrowhead angle in degrees */
    private float arrowheadAngle = 25f;

    /** Whether to scale arrow size by magnitude */
    private boolean scaleByMagnitude = true;

    /** Whether to normalize arrows to same length */
    private boolean normalizeLength = false;

    /** Skip factor for sparse rendering (1 = all, 2 = every other, etc.) */
    private int skipFactor = 1;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether visible */
    private boolean visible = true;

    /** Y-axis ID for coordinate system */
    private String yAxisId = "main";

    /**
     * Creates default vector field options.
     */
    public VectorFieldSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public VectorFieldSeriesOptions(VectorFieldSeriesOptions other) {
        this.arrowStyle = other.arrowStyle;
        this.arrowColor = other.arrowColor;
        this.colorMap = other.colorMap;
        this.lengthScale = other.lengthScale;
        this.minLength = other.minLength;
        this.maxLength = other.maxLength;
        this.lineWidth = other.lineWidth;
        this.arrowheadRatio = other.arrowheadRatio;
        this.arrowheadAngle = other.arrowheadAngle;
        this.scaleByMagnitude = other.scaleByMagnitude;
        this.normalizeLength = other.normalizeLength;
        this.skipFactor = other.skipFactor;
        this.opacity = other.opacity;
        this.visible = other.visible;
        this.yAxisId = other.yAxisId;
    }

    // ========== Getters ==========

    public ArrowStyle getArrowStyle() {
        return arrowStyle;
    }

    public Color getArrowColor() {
        return arrowColor;
    }

    public ColorMap getColorMap() {
        return colorMap;
    }

    public float getLengthScale() {
        return lengthScale;
    }

    public float getMinLength() {
        return minLength;
    }

    public float getMaxLength() {
        return maxLength;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public float getArrowheadRatio() {
        return arrowheadRatio;
    }

    public float getArrowheadAngle() {
        return arrowheadAngle;
    }

    public boolean isScaleByMagnitude() {
        return scaleByMagnitude;
    }

    public boolean isNormalizeLength() {
        return normalizeLength;
    }

    public int getSkipFactor() {
        return skipFactor;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getYAxisId() {
        return yAxisId;
    }

    // ========== Fluent Setters ==========

    public VectorFieldSeriesOptions arrowStyle(ArrowStyle style) {
        this.arrowStyle = style;
        return this;
    }

    public VectorFieldSeriesOptions arrowColor(Color color) {
        this.arrowColor = color;
        return this;
    }

    public VectorFieldSeriesOptions colorMap(ColorMap map) {
        this.colorMap = map;
        return this;
    }

    public VectorFieldSeriesOptions lengthScale(float scale) {
        this.lengthScale = Math.max(0.01f, scale);
        return this;
    }

    public VectorFieldSeriesOptions minLength(float length) {
        this.minLength = Math.max(1, length);
        return this;
    }

    public VectorFieldSeriesOptions maxLength(float length) {
        this.maxLength = Math.max(minLength, length);
        return this;
    }

    public VectorFieldSeriesOptions lineWidth(float width) {
        this.lineWidth = Math.max(0.5f, width);
        return this;
    }

    public VectorFieldSeriesOptions arrowheadRatio(float ratio) {
        this.arrowheadRatio = Math.max(0.1f, Math.min(0.5f, ratio));
        return this;
    }

    public VectorFieldSeriesOptions arrowheadAngle(float degrees) {
        this.arrowheadAngle = Math.max(10, Math.min(60, degrees));
        return this;
    }

    public VectorFieldSeriesOptions scaleByMagnitude(boolean scale) {
        this.scaleByMagnitude = scale;
        return this;
    }

    public VectorFieldSeriesOptions normalizeLength(boolean normalize) {
        this.normalizeLength = normalize;
        return this;
    }

    public VectorFieldSeriesOptions skipFactor(int factor) {
        this.skipFactor = Math.max(1, factor);
        return this;
    }

    public VectorFieldSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public VectorFieldSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public VectorFieldSeriesOptions yAxisId(String id) {
        this.yAxisId = id;
        return this;
    }
}
