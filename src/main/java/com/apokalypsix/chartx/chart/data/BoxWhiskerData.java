package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Category-based box-and-whisker data for box plot charts.
 *
 * <p>Stores statistical quartile data for each category:
 * minimum, first quartile (Q1), median, third quartile (Q3), and maximum.
 * Data is stored in parallel primitive arrays for cache-friendly access.
 *
 * <p>X-values are sequential indices (0, 1, 2...) representing categories.
 * Use labels to provide category names for display (e.g., "Group A", "Group B").
 *
 * <p>Box plots are category-based (e.g., comparing distributions across
 * different groups) rather than time-based.
 */
public class BoxWhiskerData extends AbstractData<float[]> {

    // Value arrays for the five key statistics
    private float[] min;
    private float[] q1;      // First quartile (25th percentile)
    private float[] median;  // Second quartile (50th percentile)
    private float[] q3;      // Third quartile (75th percentile)
    private float[] max;

    /**
     * Creates empty BoxWhiskerData with the specified ID and name.
     */
    public BoxWhiskerData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty BoxWhiskerData with the specified initial capacity.
     */
    public BoxWhiskerData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.min = new float[capacity];
        this.q1 = new float[capacity];
        this.median = new float[capacity];
        this.q3 = new float[capacity];
        this.max = new float[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        min = Arrays.copyOf(min, newCapacity);
        q1 = Arrays.copyOf(q1, newCapacity);
        median = Arrays.copyOf(median, newCapacity);
        q3 = Arrays.copyOf(q3, newCapacity);
        max = Arrays.copyOf(max, newCapacity);
    }

    @Override
    protected void shiftValueArrays(int index, int count) {
        System.arraycopy(min, index + 1, min, index, count);
        System.arraycopy(q1, index + 1, q1, index, count);
        System.arraycopy(median, index + 1, median, index, count);
        System.arraycopy(q3, index + 1, q3, index, count);
        System.arraycopy(max, index + 1, max, index, count);
    }

    // ========== X-value override for category data ==========

    /**
     * Returns the X-axis value at the specified index.
     * For category data, this is simply the index itself.
     */
    @Override
    public long getXValue(int index) {
        checkIndex(index);
        return index;
    }

    /**
     * Returns the minimum X value (always 0 for category data).
     */
    @Override
    public long getMinX() {
        return isEmpty() ? -1 : 0;
    }

    /**
     * Returns the maximum X value (size - 1 for category data).
     */
    @Override
    public long getMaxX() {
        return isEmpty() ? -1 : size - 1;
    }

    /**
     * Finds the index at or before the given X value.
     * For category data, this clamps to valid range.
     */
    @Override
    public int indexAtOrBefore(long xValue) {
        if (size == 0 || xValue < 0) {
            return -1;
        }
        return (int) Math.min(xValue, size - 1);
    }

    /**
     * Finds the index at or after the given X value.
     * For category data, this clamps to valid range.
     */
    @Override
    public int indexAtOrAfter(long xValue) {
        if (size == 0 || xValue >= size) {
            return -1;
        }
        return (int) Math.max(xValue, 0);
    }

    /**
     * Returns the first index visible in the given X range.
     * For category data, this always returns 0 if data exists.
     */
    @Override
    public int getFirstVisibleIndex(long minX) {
        return size > 0 ? 0 : -1;
    }

    /**
     * Returns the last index visible in the given X range.
     * For category data, this always returns size - 1 if data exists.
     */
    @Override
    public int getLastVisibleIndex(long maxX) {
        return size > 0 ? size - 1 : -1;
    }

    // ========== Value accessors ==========

    public float getMin(int index) {
        checkIndex(index);
        return min[index];
    }

    public float getQ1(int index) {
        checkIndex(index);
        return q1[index];
    }

    public float getMedian(int index) {
        checkIndex(index);
        return median[index];
    }

    public float getQ3(int index) {
        checkIndex(index);
        return q3[index];
    }

    public float getMax(int index) {
        checkIndex(index);
        return max[index];
    }

    /**
     * Returns the interquartile range (IQR = Q3 - Q1) at the specified index.
     */
    public float getIQR(int index) {
        checkIndex(index);
        return q3[index] - q1[index];
    }

    /**
     * Returns true if the data at the specified index is valid.
     */
    public boolean hasValue(int index) {
        checkIndex(index);
        return !Float.isNaN(median[index]);
    }

    // ========== Range queries ==========

    /**
     * Finds the overall minimum value in the specified index range.
     */
    public float findMinValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float minVal = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(min[i]) && min[i] < minVal) {
                minVal = min[i];
            }
        }
        return minVal;
    }

    /**
     * Finds the overall maximum value in the specified index range.
     */
    public float findMaxValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float maxVal = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(max[i]) && max[i] > maxVal) {
                maxVal = max[i];
            }
        }
        return maxVal;
    }

    // ========== Mutation ==========

    /**
     * Appends a new box-whisker data point.
     *
     * @param min minimum value
     * @param q1 first quartile
     * @param median median value
     * @param q3 third quartile
     * @param max maximum value
     * @throws IllegalArgumentException if values are out of order
     */
    public void append(float min, float q1, float median, float q3, float max) {
        append(min, q1, median, q3, max, null);
    }

    /**
     * Appends a new box-whisker data point with a category label.
     *
     * @param min minimum value
     * @param q1 first quartile
     * @param median median value
     * @param q3 third quartile
     * @param max maximum value
     * @param label the category label
     * @throws IllegalArgumentException if values are out of order
     */
    public void append(float min, float q1, float median, float q3, float max, String label) {
        validateOrder(min, q1, median, q3, max);
        ensureCapacity(size + 1);

        this.min[size] = min;
        this.q1[size] = q1;
        this.median[size] = median;
        this.q3[size] = q3;
        this.max[size] = max;
        this.labels[size] = label;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Updates the last data point.
     */
    public void updateLast(float min, float q1, float median, float q3, float max) {
        checkNotEmpty();
        validateOrder(min, q1, median, q3, max);

        this.min[size - 1] = min;
        this.q1[size - 1] = q1;
        this.median[size - 1] = median;
        this.q3[size - 1] = q3;
        this.max[size - 1] = max;

        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     */
    public void loadFromArrays(float[] min, float[] q1, float[] median, float[] q3, float[] max) {
        loadFromArrays(min, q1, median, q3, max, null);
    }

    /**
     * Loads data from arrays with optional category labels. Replaces any existing data.
     */
    public void loadFromArrays(float[] min, float[] q1, float[] median, float[] q3, float[] max, String[] labels) {
        int length = min.length;
        if (q1.length != length || median.length != length || q3.length != length || max.length != length) {
            throw new IllegalArgumentException("All arrays must have same length");
        }
        if (labels != null && labels.length != length) {
            throw new IllegalArgumentException("Labels array must have same length if provided");
        }

        ensureCapacity(length);
        System.arraycopy(min, 0, this.min, 0, length);
        System.arraycopy(q1, 0, this.q1, 0, length);
        System.arraycopy(median, 0, this.median, 0, length);
        System.arraycopy(q3, 0, this.q3, 0, length);
        System.arraycopy(max, 0, this.max, 0, length);
        if (labels != null) {
            System.arraycopy(labels, 0, this.labels, 0, length);
        }
        this.size = length;
    }

    private void validateOrder(float min, float q1, float median, float q3, float max) {
        if (!Float.isNaN(min) && !Float.isNaN(q1) && min > q1) {
            throw new IllegalArgumentException("min must be <= q1");
        }
        if (!Float.isNaN(q1) && !Float.isNaN(median) && q1 > median) {
            throw new IllegalArgumentException("q1 must be <= median");
        }
        if (!Float.isNaN(median) && !Float.isNaN(q3) && median > q3) {
            throw new IllegalArgumentException("median must be <= q3");
        }
        if (!Float.isNaN(q3) && !Float.isNaN(max) && q3 > max) {
            throw new IllegalArgumentException("q3 must be <= max");
        }
    }

    // ========== Raw array access ==========

    public float[] getMinArray() {
        return min;
    }

    public float[] getQ1Array() {
        return q1;
    }

    public float[] getMedianArray() {
        return median;
    }

    public float[] getQ3Array() {
        return q3;
    }

    public float[] getMaxArray() {
        return max;
    }
}
