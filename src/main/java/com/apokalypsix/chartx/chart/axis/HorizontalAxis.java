package com.apokalypsix.chartx.chart.axis;

/**
 * Generic interface for horizontal axes that map domain values to normalized [0, 1) coordinates.
 *
 * <p>This interface provides a unified abstraction over time-based axes ({@link TimeAxis}) and
 * categorical axes ({@link CategoryAxis}), allowing chart components to work with either
 * axis type through a common contract.
 *
 * <p>Usage:
 * <pre>{@code
 * // Time-based chart
 * chart.setXAxis(new TimeAxis());
 *
 * // Categorical chart
 * CategoryAxis categoryAxis = new CategoryAxis();
 * categoryAxis.setCategories("A", "B", "C");
 * chart.setXAxis(categoryAxis);
 * }</pre>
 *
 * @param <T> the domain value type (Long for time-based, Integer for categories)
 */
public interface HorizontalAxis<T> {

    /** Position constants for horizontal axes. */
    enum Position {
        TOP,
        BOTTOM
    }

    /**
     * Returns the axis identifier.
     *
     * @return the unique axis ID
     */
    String getId();

    /**
     * Returns the axis position (TOP or BOTTOM).
     *
     * @return the axis position
     */
    Position getPosition();

    /**
     * Returns whether the axis is visible.
     *
     * @return true if visible
     */
    boolean isVisible();

    /**
     * Sets axis visibility.
     *
     * @param visible true to show the axis
     */
    void setVisible(boolean visible);

    /**
     * Converts a domain value to a normalized position in [0, 1) range.
     *
     * <p>For time-based axes, this normalizes the timestamp relative to the visible range.
     * For categorical axes, this returns the position of the category index.
     *
     * @param value the domain value (timestamp or category index)
     * @return normalized position where 0 = start, 1 = end
     */
    double toNormalized(T value);

    /**
     * Returns true if this is a time-based axis, false for categorical.
     *
     * <p>Used by rendering layers to choose appropriate rendering logic.
     *
     * @return true for {@link TimeAxis}, false for {@link CategoryAxis}
     */
    boolean isTimeBased();
}
