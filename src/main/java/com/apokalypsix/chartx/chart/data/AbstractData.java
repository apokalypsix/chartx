package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;

import java.util.Arrays;

/**
 * Abstract base class for Data implementations.
 *
 * <p>Provides common functionality for X-value management, binary search,
 * array growth, and listener support. The X-values are axis-agnostic:
 * <ul>
 *   <li>For time-series data: X-values are timestamps in epoch milliseconds</li>
 *   <li>For category-based data: X-values are indices (0, 1, 2...)</li>
 * </ul>
 *
 * <p>The interpretation of X-values is determined by the axis type
 * (TimeAxis, CategoryAxis, NumericAxis), not by this data class.
 *
 * <p>Subclasses should add their specific value arrays and implement
 * append/update methods.
 */
public abstract class AbstractData<T> implements Data<T> {

    protected static final int DEFAULT_INITIAL_CAPACITY = 1024;
    protected static final float GROWTH_FACTOR = 1.5f;

    protected final String id;
    protected final String name;

    // X-value array (timestamps for time-series, indices for categorical)
    protected long[] xValues;
    protected int size;

    // Optional labels for category data (e.g., "Q1", "Q2", "Product A")
    protected String[] labels;

    // Listener support for real-time updates
    protected final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates data with the specified ID and name.
     */
    protected AbstractData(String id, String name) {
        this(id, name, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates data with the specified initial capacity.
     */
    protected AbstractData(String id, String name, int initialCapacity) {
        this.id = id;
        this.name = name;
        this.xValues = new long[initialCapacity];
        this.labels = new String[initialCapacity];
        this.size = 0;
        initializeValueArrays(initialCapacity);
    }

    /**
     * Subclasses must initialize their value arrays with the given capacity.
     */
    protected abstract void initializeValueArrays(int capacity);

    /**
     * Subclasses must grow their value arrays to the new capacity.
     */
    protected abstract void growValueArrays(int newCapacity);

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public long getXValue(int index) {
        checkIndex(index);
        return xValues[index];
    }

    /**
     * Returns the timestamp at the specified index.
     * Convenience alias for getXValue() for time-series data.
     */
    public long getTimestamp(int index) {
        return getXValue(index);
    }

    // ========== Label support (for category data) ==========

    /**
     * Returns the label at the specified index, or null if not set.
     *
     * @param index the index
     * @return the label, or null if not set
     */
    public String getLabel(int index) {
        checkIndex(index);
        return labels[index];
    }

    /**
     * Sets the label at the specified index.
     *
     * @param index the index
     * @param label the label to set
     */
    public void setLabel(int index, String label) {
        checkIndex(index);
        labels[index] = label;
    }

    /**
     * Returns the raw labels array. For rendering use only.
     * Do not modify the returned array.
     */
    public String[] getLabelsArray() {
        return labels;
    }

    /**
     * Returns the index of the first label matching the given string.
     *
     * @param label the label to find
     * @return the index, or -1 if not found
     */
    public int indexOfLabel(String label) {
        if (label == null) {
            for (int i = 0; i < size; i++) {
                if (labels[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (label.equals(labels[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ========== Index search ==========

    @Override
    public int indexAtOrBefore(long xValue) {
        if (size == 0 || xValue < xValues[0]) {
            return -1;
        }
        if (xValue >= xValues[size - 1]) {
            return size - 1;
        }
        return binarySearchAtOrBefore(xValue);
    }

    @Override
    public int indexAtOrAfter(long xValue) {
        if (size == 0 || xValue > xValues[size - 1]) {
            return -1;
        }
        if (xValue <= xValues[0]) {
            return 0;
        }
        return binarySearchAtOrAfter(xValue);
    }

    private int binarySearchAtOrBefore(long xValue) {
        int low = 0;
        int high = size - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = xValues[mid];

            if (midVal <= xValue) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    private int binarySearchAtOrAfter(long xValue) {
        int low = 0;
        int high = size - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = xValues[mid];

            if (midVal >= xValue) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ========== Clear ==========

    @Override
    public void clear() {
        size = 0;
        listenerSupport.fireDataCleared(this);
    }

    // ========== Raw array access ==========

    /**
     * Returns the raw X-values array. For rendering use only.
     * Do not modify the returned array.
     */
    public long[] getXValuesArray() {
        return xValues;
    }

    /**
     * Returns the raw timestamp array. Convenience alias for getXValuesArray()
     * for time-series data. Do not modify the returned array.
     */
    public long[] getTimestampsArray() {
        return xValues;
    }

    // ========== Capacity management ==========

    /**
     * Ensures the arrays can hold the specified number of elements.
     */
    protected void ensureCapacity(int minCapacity) {
        if (minCapacity > xValues.length) {
            int newCapacity = Math.max(minCapacity, (int) (xValues.length * GROWTH_FACTOR));
            xValues = Arrays.copyOf(xValues, newCapacity);
            labels = Arrays.copyOf(labels, newCapacity);
            growValueArrays(newCapacity);
        }
    }

    /**
     * Validates that the given X-value is in ascending order.
     * Used by time-series data to ensure chronological order.
     */
    protected void validateAscendingXValue(long xValue) {
        if (size > 0 && xValue <= xValues[size - 1]) {
            throw new IllegalArgumentException(
                    "X-value must be ascending. Last: " + xValues[size - 1] + ", given: " + xValue);
        }
    }

    /**
     * Validates that the given timestamp is in ascending order.
     * Convenience alias for validateAscendingXValue() for time-series data.
     */
    protected void validateAscendingTimestamp(long timestamp) {
        validateAscendingXValue(timestamp);
    }

    // ========== Element removal (for category data) ==========

    /**
     * Removes the data point at the specified index.
     * Primarily used for category-based data.
     *
     * @param index the index to remove
     */
    public void remove(int index) {
        checkIndex(index);

        int numToMove = size - index - 1;
        if (numToMove > 0) {
            System.arraycopy(xValues, index + 1, xValues, index, numToMove);
            System.arraycopy(labels, index + 1, labels, index, numToMove);
            shiftValueArrays(index, numToMove);
        }

        xValues[size - 1] = 0;
        labels[size - 1] = null;
        size--;

        listenerSupport.fireDataUpdated(this, index);
    }

    /**
     * Subclasses may override to shift their value arrays when an element is removed.
     * Default implementation does nothing (for time-series data that doesn't support removal).
     *
     * @param index the index where shifting starts
     * @param count the number of elements to shift
     */
    protected void shiftValueArrays(int index, int count) {
        // Default: no-op. Subclasses override if they support element removal.
    }

    // ========== Validation ==========

    protected void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    protected void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex >= size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                    "Invalid range [" + fromIndex + ", " + toIndex + "], Size: " + size);
        }
    }

    protected void checkNotEmpty() {
        if (size == 0) {
            throw new IllegalStateException("Cannot update: data is empty");
        }
    }

    // ========== Listener management ==========

    @Override
    public void addListener(DataListener listener) {
        listenerSupport.addListener(listener);
    }

    @Override
    public void removeListener(DataListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, name=%s, size=%d]", getClass().getSimpleName(), id, name, size);
    }
}
