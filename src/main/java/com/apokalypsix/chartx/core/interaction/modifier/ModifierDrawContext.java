package com.apokalypsix.chartx.core.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ModifierSurface;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Graphics2D;

/**
 * Rendering context for modifier drawing operations.
 *
 * <p>Provides modifiers with access to the graphics context and coordinate
 * system for rendering visual feedback like crosshairs, selection rectangles,
 * and tooltips.
 *
 * <p>The context is passed to {@link ChartModifier#onDraw(ModifierDrawContext)}
 * after the main chart layers have rendered.
 */
public class ModifierDrawContext {

    /** The graphics context for drawing */
    private final Graphics2D graphics;

    /** The coordinate system for data-to-screen conversion */
    private final CoordinateSystem coordinates;

    /** The modifier surface for additional chart access */
    private final ModifierSurface surface;

    /** The chart area left boundary */
    private final int chartLeft;

    /** The chart area right boundary */
    private final int chartRight;

    /** The chart area top boundary */
    private final int chartTop;

    /** The chart area bottom boundary */
    private final int chartBottom;

    /**
     * Creates a new modifier draw context.
     *
     * @param graphics the graphics context
     * @param coordinates the coordinate system
     * @param surface the modifier surface
     */
    public ModifierDrawContext(Graphics2D graphics, CoordinateSystem coordinates, ModifierSurface surface) {
        this.graphics = graphics;
        this.coordinates = coordinates;
        this.surface = surface;

        // Cache chart area bounds
        this.chartLeft = surface.getChartLeft();
        this.chartRight = surface.getChartRight();
        this.chartTop = surface.getChartTop();
        this.chartBottom = surface.getChartBottom();
    }

    /**
     * Returns the graphics context for drawing.
     *
     * @return the graphics context
     */
    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system
     */
    public CoordinateSystem getCoordinates() {
        return coordinates;
    }

    /**
     * Returns the modifier surface.
     *
     * @return the modifier surface
     */
    public ModifierSurface getSurface() {
        return surface;
    }

    /**
     * Returns the chart area left boundary.
     *
     * @return left X coordinate
     */
    public int getChartLeft() {
        return chartLeft;
    }

    /**
     * Returns the chart area right boundary.
     *
     * @return right X coordinate
     */
    public int getChartRight() {
        return chartRight;
    }

    /**
     * Returns the chart area top boundary.
     *
     * @return top Y coordinate
     */
    public int getChartTop() {
        return chartTop;
    }

    /**
     * Returns the chart area bottom boundary.
     *
     * @return bottom Y coordinate
     */
    public int getChartBottom() {
        return chartBottom;
    }

    /**
     * Returns the width of the chart area.
     *
     * @return width in pixels
     */
    public int getChartWidth() {
        return chartRight - chartLeft;
    }

    /**
     * Returns the height of the chart area.
     *
     * @return height in pixels
     */
    public int getChartHeight() {
        return chartBottom - chartTop;
    }

    /**
     * Converts a timestamp to X screen coordinate.
     *
     * @param timestamp time in epoch milliseconds
     * @return X coordinate in pixels
     */
    public double timeToX(long timestamp) {
        return coordinates.xValueToScreenX(timestamp);
    }

    /**
     * Converts a price to Y screen coordinate.
     *
     * @param price the price value
     * @return Y coordinate in pixels
     */
    public double priceToY(double price) {
        return coordinates.yValueToScreenY(price);
    }

    /**
     * Returns true if the given X coordinate is within the chart area.
     *
     * @param x the X coordinate
     * @return true if in chart area
     */
    public boolean isXInChartArea(double x) {
        return x >= chartLeft && x <= chartRight;
    }

    /**
     * Returns true if the given Y coordinate is within the chart area.
     *
     * @param y the Y coordinate
     * @return true if in chart area
     */
    public boolean isYInChartArea(double y) {
        return y >= chartTop && y <= chartBottom;
    }

    /**
     * Clips the given X coordinate to the chart area.
     *
     * @param x the X coordinate
     * @return the clipped X coordinate
     */
    public double clipX(double x) {
        return Math.max(chartLeft, Math.min(chartRight, x));
    }

    /**
     * Clips the given Y coordinate to the chart area.
     *
     * @param y the Y coordinate
     * @return the clipped Y coordinate
     */
    public double clipY(double y) {
        return Math.max(chartTop, Math.min(chartBottom, y));
    }
}
