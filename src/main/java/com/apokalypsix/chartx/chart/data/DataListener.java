package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.Data;

/**
 * Listener interface for data change events.
 *
 * <p>Implementations can register with a Data instance to receive notifications when
 * data is appended, updated, or cleared. This enables real-time chart updates
 * as streaming data arrives.
 */
public interface DataListener {

    /**
     * Called when a new data point is appended.
     *
     * @param data the data that changed
     * @param newIndex the index of the newly appended data point
     */
    void onDataAppended(Data<?> data, int newIndex);

    /**
     * Called when an existing data point is updated.
     *
     * @param data the data that changed
     * @param index the index of the updated data point
     */
    void onDataUpdated(Data<?> data, int index);

    /**
     * Called when the data is cleared.
     *
     * @param data the data that was cleared
     */
    void onDataCleared(Data<?> data);
}
