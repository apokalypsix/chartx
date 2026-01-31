package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Time-indexed band data with upper, middle, and lower values.
 *
 * <p>Used for Bollinger Bands, Keltner Channels, Donchian Channels, and similar
 * indicators that display a center line with upper and lower boundaries.
 *
 * <p>Data is stored in parallel primitive arrays for cache-friendly access.
 */
public class XyyData extends AbstractData<float[]> {

    // Parallel arrays for band values
    private float[] upper;
    private float[] middle;
    private float[] lower;

    /**
     * Creates empty band data with the specified ID and name.
     */
    public XyyData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty band data with the specified initial capacity.
     */
    public XyyData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.upper = new float[capacity];
        this.middle = new float[capacity];
        this.lower = new float[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        upper = Arrays.copyOf(upper, newCapacity);
        middle = Arrays.copyOf(middle, newCapacity);
        lower = Arrays.copyOf(lower, newCapacity);
    }

    // ========== Value accessors ==========

    /**
     * Returns the upper band value at the specified index.
     */
    public float getUpper(int index) {
        checkIndex(index);
        return upper[index];
    }

    /**
     * Returns the middle band value at the specified index.
     */
    public float getMiddle(int index) {
        checkIndex(index);
        return middle[index];
    }

    /**
     * Returns the lower band value at the specified index.
     */
    public float getLower(int index) {
        checkIndex(index);
        return lower[index];
    }

    /**
     * Returns true if the values at the index are valid (not NaN).
     */
    public boolean hasValue(int index) {
        checkIndex(index);
        return !Float.isNaN(middle[index]);
    }

    /**
     * Returns the band width at the specified index.
     */
    public float getBandWidth(int index) {
        checkIndex(index);
        return upper[index] - lower[index];
    }

    // ========== Range queries ==========

    /**
     * Finds the minimum lower band value in the range.
     * Ignores NaN values.
     */
    public float findMinLower(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float min = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(lower[i]) && lower[i] < min) {
                min = lower[i];
            }
        }
        return min;
    }

    /**
     * Finds the maximum upper band value in the range.
     * Ignores NaN values.
     */
    public float findMaxUpper(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float max = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(upper[i]) && upper[i] > max) {
                max = upper[i];
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
    public void append(long timestamp, float upper, float middle, float lower) {
        validateAscendingTimestamp(timestamp);
        ensureCapacity(size + 1);

        xValues[size] = timestamp;
        this.upper[size] = upper;
        this.middle[size] = middle;
        this.lower[size] = lower;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Updates the last data point.
     *
     * @throws IllegalStateException if the data is empty
     */
    public void updateLast(float upper, float middle, float lower) {
        checkNotEmpty();
        int lastIndex = size - 1;
        this.upper[lastIndex] = upper;
        this.middle[lastIndex] = middle;
        this.lower[lastIndex] = lower;

        listenerSupport.fireDataUpdated(this, lastIndex);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     *
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public void loadFromArrays(long[] timestamps, float[] upper, float[] middle, float[] lower) {
        int length = timestamps.length;
        if (upper.length != length || middle.length != length || lower.length != length) {
            throw new IllegalArgumentException("All arrays must have the same length");
        }

        ensureCapacity(length);
        System.arraycopy(timestamps, 0, this.xValues, 0, length);
        System.arraycopy(upper, 0, this.upper, 0, length);
        System.arraycopy(middle, 0, this.middle, 0, length);
        System.arraycopy(lower, 0, this.lower, 0, length);
        this.size = length;
    }

    // ========== View creation ==========

    /**
     * Creates an XyData view of the upper band.
     */
    public XyData asUpperData() {
        XyData xyData = new XyData(id + "_upper", name + " Upper");
        if (size > 0) {
            xyData.loadFromArrays(xValues, upper);
        }
        return xyData;
    }

    /**
     * Creates an XyData view of the middle band.
     */
    public XyData asMiddleData() {
        XyData xyData = new XyData(id + "_middle", name + " Middle");
        if (size > 0) {
            xyData.loadFromArrays(xValues, middle);
        }
        return xyData;
    }

    /**
     * Creates an XyData view of the lower band.
     */
    public XyData asLowerData() {
        XyData xyData = new XyData(id + "_lower", name + " Lower");
        if (size > 0) {
            xyData.loadFromArrays(xValues, lower);
        }
        return xyData;
    }

    // ========== Raw array access ==========

    /**
     * Returns the raw upper array. For rendering use only.
     */
    public float[] getUpperArray() {
        return upper;
    }

    /**
     * Returns the raw middle array. For rendering use only.
     */
    public float[] getMiddleArray() {
        return middle;
    }

    /**
     * Returns the raw lower array. For rendering use only.
     */
    public float[] getLowerArray() {
        return lower;
    }
}
