package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Time-indexed bubble chart data with X, Y, and size values.
 *
 * <p>Extends XY data with a third dimension (size) for bubble charts.
 * Data is stored in parallel primitive arrays for cache-friendly access.
 */
public class BubbleData extends AbstractData<float[]> {

    // Value arrays
    private float[] values;
    private float[] sizes;

    /**
     * Creates empty BubbleData with the specified ID and name.
     */
    public BubbleData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty BubbleData with the specified initial capacity.
     */
    public BubbleData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.values = new float[capacity];
        this.sizes = new float[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        values = Arrays.copyOf(values, newCapacity);
        sizes = Arrays.copyOf(sizes, newCapacity);
    }

    // ========== Value accessors ==========

    /**
     * Returns the Y value at the specified index.
     */
    public float getValue(int index) {
        checkIndex(index);
        return values[index];
    }

    /**
     * Returns the size value at the specified index.
     */
    public float getSize(int index) {
        checkIndex(index);
        return sizes[index];
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
     * Finds the minimum Y value in the specified index range.
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
     * Finds the maximum Y value in the specified index range.
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

    /**
     * Finds the minimum size in the specified index range.
     */
    public float findMinSize(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float min = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(sizes[i]) && sizes[i] < min) {
                min = sizes[i];
            }
        }
        return min;
    }

    /**
     * Finds the maximum size in the specified index range.
     */
    public float findMaxSize(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float max = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(sizes[i]) && sizes[i] > max) {
                max = sizes[i];
            }
        }
        return max;
    }

    // ========== Mutation ==========

    /**
     * Appends a new data point.
     *
     * @param timestamp the timestamp
     * @param value the Y value
     * @param bubbleSize the size value for the bubble
     * @throws IllegalArgumentException if timestamp is not in ascending order
     */
    public void append(long timestamp, float value, float bubbleSize) {
        validateAscendingTimestamp(timestamp);
        ensureCapacity(size + 1);

        xValues[size] = timestamp;
        values[size] = value;
        sizes[size] = bubbleSize;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Updates the last data point.
     *
     * @param value the new Y value
     * @param bubbleSize the new size value
     * @throws IllegalStateException if data is empty
     */
    public void updateLast(float value, float bubbleSize) {
        checkNotEmpty();
        values[size - 1] = value;
        sizes[size - 1] = bubbleSize;
        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     *
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public void loadFromArrays(long[] timestamps, float[] values, float[] sizes) {
        if (timestamps.length != values.length || timestamps.length != sizes.length) {
            throw new IllegalArgumentException("All arrays must have same length");
        }

        int length = timestamps.length;
        ensureCapacity(length);
        System.arraycopy(timestamps, 0, this.xValues, 0, length);
        System.arraycopy(values, 0, this.values, 0, length);
        System.arraycopy(sizes, 0, this.sizes, 0, length);
        this.size = length;
    }

    // ========== Raw array access ==========

    /**
     * Returns the raw values array. For rendering use only.
     */
    public float[] getValuesArray() {
        return values;
    }

    /**
     * Returns the raw sizes array. For rendering use only.
     */
    public float[] getSizesArray() {
        return sizes;
    }
}
