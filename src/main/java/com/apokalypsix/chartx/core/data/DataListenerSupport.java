package com.apokalypsix.chartx.core.data;

import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.DataListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reusable helper class for managing data listeners.
 *
 * <p>Provides thread-safe listener registration and event firing.
 * Uses CopyOnWriteArrayList for safe iteration during event dispatch.
 */
public class DataListenerSupport {

    private final List<DataListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a listener to receive data events.
     *
     * @param listener the listener to add
     */
    public void addListener(DataListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(DataListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fires a data appended event to all registered listeners.
     *
     * @param data the data that changed
     * @param newIndex the index of the newly appended data point
     */
    public void fireDataAppended(Data<?> data, int newIndex) {
        for (DataListener listener : listeners) {
            listener.onDataAppended(data, newIndex);
        }
    }

    /**
     * Fires a data updated event to all registered listeners.
     *
     * @param data the data that changed
     * @param index the index of the updated data point
     */
    public void fireDataUpdated(Data<?> data, int index) {
        for (DataListener listener : listeners) {
            listener.onDataUpdated(data, index);
        }
    }

    /**
     * Fires a data cleared event to all registered listeners.
     *
     * @param data the data that was cleared
     */
    public void fireDataCleared(Data<?> data) {
        for (DataListener listener : listeners) {
            listener.onDataCleared(data);
        }
    }

    /**
     * Returns true if there are any registered listeners.
     */
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }
}
