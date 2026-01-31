package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Time-indexed histogram/bar data for volume, delta, or other bar-based displays.
 *
 * <p>Data is stored in parallel primitive arrays for cache-friendly access.
 * Supports positive and negative values (for delta histograms).
 *
 * <p>This class is functionally identical to XyData but exists as a separate
 * type to enable type-safe series creation and clear semantic intent.
 */
public class HistogramData extends AbstractData<Float> {

    // Value array
    private float[] values;

    /**
     * Creates empty histogram data with the specified ID and name.
     */
    public HistogramData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty histogram data with the specified initial capacity.
     */
    public HistogramData(String id, String name, int initialCapacity) {
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
     */
    public float getValue(int index) {
        checkIndex(index);
        return values[index];
    }

    /**
     * Returns true if the value is positive.
     */
    public boolean isPositive(int index) {
        checkIndex(index);
        return values[index] > 0;
    }

    /**
     * Returns true if the value is negative.
     */
    public boolean isNegative(int index) {
        checkIndex(index);
        return values[index] < 0;
    }

    // ========== Range queries ==========

    /**
     * Finds the minimum value in the specified range.
     */
    public float findMinValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float min = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    /**
     * Finds the maximum value in the specified range.
     */
    public float findMaxValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float max = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * Returns the maximum absolute value in the range.
     * Useful for symmetric scaling of positive/negative histograms.
     */
    public float findMaxAbsValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float maxAbs = 0;
        for (int i = fromIndex; i <= toIndex; i++) {
            float absValue = Math.abs(values[i]);
            if (absValue > maxAbs) {
                maxAbs = absValue;
            }
        }
        return maxAbs;
    }

    // ========== Mutation ==========

    /**
     * Appends a new bar to the data.
     * Timestamp must be greater than the last bar's timestamp.
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
     * Updates the last bar's value.
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
     */
    public float[] getValuesArray() {
        return values;
    }
}
