package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Time-indexed single-value data for line charts, indicators, and scatter plots.
 *
 * <p>Data is stored in parallel primitive arrays for cache-friendly access.
 * Supports NaN values to represent gaps (e.g., periods before indicator has enough data).
 */
public class XyData extends AbstractData<Float> {

    // Value array
    private float[] values;

    /**
     * Creates empty XyData with the specified ID and name.
     */
    public XyData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty XyData with the specified initial capacity.
     */
    public XyData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.values = new float[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        values = Arrays.copyOf(values, newCapacity);
    }

    // ========== Value accessors ==========

    /**
     * Returns the value at the specified index.
     * May return Float.NaN if no valid value at that index.
     */
    public float getValue(int index) {
        checkIndex(index);
        return values[index];
    }

    /**
     * Returns true if the value at the specified index is valid (not NaN).
     */
    public boolean hasValue(int index) {
        checkIndex(index);
        return !Float.isNaN(values[index]);
    }

    // ========== Range queries ==========

    /**
     * Finds the minimum value in the specified index range.
     * Ignores NaN values.
     */
    public float findMinValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float min = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(values[i]) && values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    /**
     * Finds the maximum value in the specified index range.
     * Ignores NaN values.
     */
    public float findMaxValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float max = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(values[i]) && values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    // ========== Mutation ==========

    /**
     * Appends a new data point.
     * Timestamp must be greater than the last point's timestamp.
     *
     * @throws IllegalArgumentException if timestamp is not in ascending order
     */
    public void append(long timestamp, float value) {
        validateAscendingTimestamp(timestamp);
        ensureCapacity(size + 1);

        xValues[size] = timestamp;
        values[size] = value;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Updates the value at the last timestamp.
     *
     * @throws IllegalStateException if the data is empty
     */
    public void updateLast(float value) {
        checkNotEmpty();
        values[size - 1] = value;
        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     *
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public void loadFromArrays(long[] timestamps, float[] values) {
        if (timestamps.length != values.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }

        int length = timestamps.length;
        ensureCapacity(length);
        System.arraycopy(timestamps, 0, this.xValues, 0, length);
        System.arraycopy(values, 0, this.values, 0, length);
        this.size = length;
    }

    // ========== Raw array access ==========

    /**
     * Returns the raw values array. For rendering use only.
     * Do not modify the returned array.
     */
    public float[] getValuesArray() {
        return values;
    }
}
