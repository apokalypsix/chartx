package com.apokalypsix.chartx.chart.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group of XyData series to be rendered as stacked areas or columns.
 *
 * <p>Each series in the group is stacked on top of the previous one,
 * with the first series at the bottom. Supports both standard stacking
 * (cumulative sum) and 100% stacking (normalized to percentages).
 *
 * <p>This class manages the series list and per-series colors, but
 * does not compute the stacked values itself. Use
 * {@link com.apokalypsix.chartx.data.service.StackingCalculator} for
 * the actual stacking computation.
 */
public class StackedSeriesGroup {

    private final String id;
    private final String name;
    private final List<XyData> seriesList;
    private final List<Color> colors;

    /**
     * Creates a new stacked series group.
     *
     * @param id unique identifier for this group
     * @param name display name for the group
     */
    public StackedSeriesGroup(String id, String name) {
        this.id = id;
        this.name = name;
        this.seriesList = new ArrayList<>();
        this.colors = new ArrayList<>();
    }

    /**
     * Returns the group ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the group name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of series in the group.
     */
    public int size() {
        return seriesList.size();
    }

    /**
     * Returns true if the group has no series.
     */
    public boolean isEmpty() {
        return seriesList.isEmpty();
    }

    /**
     * Adds a series to the group with a default color.
     *
     * @param series the series to add
     * @return this for chaining
     */
    public StackedSeriesGroup addSeries(XyData series) {
        return addSeries(series, getDefaultColor(seriesList.size()));
    }

    /**
     * Adds a series to the group with a specified color.
     *
     * @param series the series to add
     * @param color the color for this series
     * @return this for chaining
     */
    public StackedSeriesGroup addSeries(XyData series, Color color) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        seriesList.add(series);
        colors.add(color != null ? color : getDefaultColor(seriesList.size() - 1));
        return this;
    }

    /**
     * Removes a series from the group.
     *
     * @param series the series to remove
     * @return true if the series was found and removed
     */
    public boolean removeSeries(XyData series) {
        int idx = seriesList.indexOf(series);
        if (idx >= 0) {
            seriesList.remove(idx);
            colors.remove(idx);
            return true;
        }
        return false;
    }

    /**
     * Returns the series at the specified index.
     *
     * @param index the index (0 = bottom of stack)
     * @return the series
     */
    public XyData getSeries(int index) {
        return seriesList.get(index);
    }

    /**
     * Returns the color for the series at the specified index.
     *
     * @param index the series index
     * @return the color
     */
    public Color getColor(int index) {
        return colors.get(index);
    }

    /**
     * Sets the color for the series at the specified index.
     *
     * @param index the series index
     * @param color the new color
     */
    public void setColor(int index, Color color) {
        if (index >= 0 && index < colors.size()) {
            colors.set(index, color != null ? color : getDefaultColor(index));
        }
    }

    /**
     * Returns an unmodifiable view of the series list.
     */
    public List<XyData> getSeriesList() {
        return Collections.unmodifiableList(seriesList);
    }

    /**
     * Returns an unmodifiable view of the colors list.
     */
    public List<Color> getColors() {
        return Collections.unmodifiableList(colors);
    }

    /**
     * Finds the first visible index across all series for the given start time.
     *
     * @param startTime the start time
     * @return the first visible index, or -1 if none found
     */
    public int getFirstVisibleIndex(long startTime) {
        int firstIdx = Integer.MAX_VALUE;
        for (XyData series : seriesList) {
            int idx = series.getFirstVisibleIndex(startTime);
            if (idx >= 0 && idx < firstIdx) {
                firstIdx = idx;
            }
        }
        return firstIdx == Integer.MAX_VALUE ? -1 : firstIdx;
    }

    /**
     * Finds the last visible index across all series for the given end time.
     *
     * @param endTime the end time
     * @return the last visible index, or -1 if none found
     */
    public int getLastVisibleIndex(long endTime) {
        int lastIdx = -1;
        for (XyData series : seriesList) {
            int idx = series.getLastVisibleIndex(endTime);
            if (idx > lastIdx) {
                lastIdx = idx;
            }
        }
        return lastIdx;
    }

    /**
     * Returns the maximum size across all series.
     */
    public int getMaxSize() {
        int maxSize = 0;
        for (XyData series : seriesList) {
            if (series.size() > maxSize) {
                maxSize = series.size();
            }
        }
        return maxSize;
    }

    /**
     * Returns the minimum size across all series.
     * Useful for determining the common data range.
     */
    public int getMinSize() {
        if (seriesList.isEmpty()) {
            return 0;
        }
        int minSize = Integer.MAX_VALUE;
        for (XyData series : seriesList) {
            if (series.size() < minSize) {
                minSize = series.size();
            }
        }
        return minSize;
    }

    private static Color getDefaultColor(int index) {
        // Default color palette for stacked series
        Color[] palette = {
            new Color(65, 131, 196),   // Blue
            new Color(196, 98, 65),    // Orange
            new Color(98, 196, 65),    // Green
            new Color(196, 65, 131),   // Pink
            new Color(131, 65, 196),   // Purple
            new Color(65, 196, 163),   // Teal
            new Color(196, 163, 65),   // Yellow
            new Color(131, 196, 65),   // Lime
        };
        return palette[index % palette.length];
    }

    @Override
    public String toString() {
        return String.format("StackedSeriesGroup[id=%s, name=%s, count=%d]",
                id, name, seriesList.size());
    }
}
