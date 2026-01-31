package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.DataListener;

/**
 * Base interface for all data storage classes in ChartX.
 *
 * <p>Data classes handle pure storage and CRUD operations without any rendering concerns.
 * The same Data instance can be rendered by multiple Series with different visual styles.
 *
 * <p>Implementations should prioritize memory efficiency using primitive arrays
 * and cache-friendly access patterns.
 *
 * @param <T> the type of value stored (e.g., Float for XyData, OhlcBar for OhlcData)
 */
public interface Data<T> {

    /**
     * Returns the unique identifier for this data.
     */
    String getId();

    /**
     * Returns the display name for this data.
     */
    String getName();

    /**
     * Returns the number of data points.
     */
    int size();

    /**
     * Returns true if this data contains no points.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    // ========== X-axis value access ==========

    /**
     * Returns the X-axis value at the specified index.
     * For time-series data, this is a timestamp in epoch milliseconds.
     * For category data, this is typically the index itself.
     *
     * @param index the index (0-based)
     * @return the X-axis value
     * @throws IndexOutOfBoundsException if index is out of range
     */
    long getXValue(int index);

    /**
     * Returns the minimum X-axis value (first data point), or -1 if empty.
     */
    default long getMinX() {
        return isEmpty() ? -1 : getXValue(0);
    }

    /**
     * Returns the maximum X-axis value (last data point), or -1 if empty.
     */
    default long getMaxX() {
        return isEmpty() ? -1 : getXValue(size() - 1);
    }

    // ========== Index search ==========

    /**
     * Finds the index of the data point at or before the given X value.
     * For time-series data, uses binary search for efficiency.
     *
     * @param xValue the X-axis value to search for
     * @return the index, or -1 if no data point exists at or before the value
     */
    int indexAtOrBefore(long xValue);

    /**
     * Finds the index of the data point at or after the given X value.
     * For time-series data, uses binary search for efficiency.
     *
     * @param xValue the X-axis value to search for
     * @return the index, or -1 if no data point exists at or after the value
     */
    int indexAtOrAfter(long xValue);

    /**
     * Returns the first index visible in the given X range.
     *
     * @param minX start of visible range
     * @return index of first visible point, or -1 if none visible
     */
    default int getFirstVisibleIndex(long minX) {
        return indexAtOrAfter(minX);
    }

    /**
     * Returns the last index visible in the given X range.
     *
     * @param maxX end of visible range
     * @return index of last visible point, or -1 if none visible
     */
    default int getLastVisibleIndex(long maxX) {
        return indexAtOrBefore(maxX);
    }

    // ========== CRUD operations ==========

    /**
     * Clears all data points.
     */
    void clear();

    // ========== Listeners ==========

    /**
     * Adds a listener to receive notifications when data changes.
     *
     * @param listener the listener to add
     */
    void addListener(DataListener listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeListener(DataListener listener);
}
