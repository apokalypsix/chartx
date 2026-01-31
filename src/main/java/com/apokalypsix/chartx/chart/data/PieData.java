package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Data for pie and donut charts.
 *
 * <p>Stores labeled values for pie/donut segments. Each segment has:
 * <ul>
 *   <li>A value (the segment size)</li>
 *   <li>An optional label (for display)</li>
 *   <li>An optional explode offset (for emphasizing segments)</li>
 * </ul>
 *
 * <p>X-values are sequential indices (0, 1, 2...) representing each segment.
 * Use labels to provide segment names for display.
 *
 * <p>The data automatically computes percentages and cumulative angles
 * for efficient rendering.
 */
public class PieData extends AbstractData<float[]> {

    /** Segment values */
    private float[] values;

    /** Explode offset for each segment (0 = not exploded) */
    private float[] explodeOffsets;

    /** Cached total value for percentage calculations */
    private float cachedTotal = Float.NaN;

    /** Cached start angles for each segment */
    private double[] cachedStartAngles;

    /** Cached sweep angles for each segment */
    private double[] cachedSweepAngles;

    /**
     * Creates empty pie data with the specified ID and name.
     */
    public PieData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty pie data with the specified initial capacity.
     */
    public PieData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.values = new float[capacity];
        this.explodeOffsets = new float[capacity];
        this.cachedStartAngles = new double[capacity];
        this.cachedSweepAngles = new double[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        values = Arrays.copyOf(values, newCapacity);
        explodeOffsets = Arrays.copyOf(explodeOffsets, newCapacity);
        cachedStartAngles = Arrays.copyOf(cachedStartAngles, newCapacity);
        cachedSweepAngles = Arrays.copyOf(cachedSweepAngles, newCapacity);
    }

    @Override
    protected void shiftValueArrays(int index, int count) {
        System.arraycopy(values, index + 1, values, index, count);
        System.arraycopy(explodeOffsets, index + 1, explodeOffsets, index, count);
        invalidateCache();
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

    // ========== Value Accessors ==========

    /**
     * Returns the value at the specified index.
     */
    public float getValue(int index) {
        checkIndex(index);
        return values[index];
    }

    /**
     * Returns the explode offset at the specified index.
     */
    public float getExplodeOffset(int index) {
        checkIndex(index);
        return explodeOffsets[index];
    }

    /**
     * Returns the percentage this segment represents of the total.
     */
    public float getPercentage(int index) {
        checkIndex(index);
        float total = getTotal();
        if (total <= 0) {
            return 0;
        }
        return (values[index] / total) * 100f;
    }

    /**
     * Returns the raw values array. For rendering use only.
     */
    public float[] getValuesArray() {
        return values;
    }

    /**
     * Returns the raw explode offsets array. For rendering use only.
     */
    public float[] getExplodeOffsetsArray() {
        return explodeOffsets;
    }

    // ========== Angle Calculations ==========

    /**
     * Returns the start angle for the segment at the specified index.
     *
     * @param index segment index
     * @return start angle in radians
     */
    public double getStartAngle(int index) {
        checkIndex(index);
        ensureAnglesComputed();
        return cachedStartAngles[index];
    }

    /**
     * Returns the sweep angle (arc length) for the segment at the specified index.
     *
     * @param index segment index
     * @return sweep angle in radians
     */
    public double getSweepAngle(int index) {
        checkIndex(index);
        ensureAnglesComputed();
        return cachedSweepAngles[index];
    }

    /**
     * Returns the center angle for the segment at the specified index.
     *
     * @param index segment index
     * @return center angle in radians
     */
    public double getCenterAngle(int index) {
        checkIndex(index);
        ensureAnglesComputed();
        return cachedStartAngles[index] + cachedSweepAngles[index] / 2;
    }

    /**
     * Returns the end angle for the segment at the specified index.
     *
     * @param index segment index
     * @return end angle in radians
     */
    public double getEndAngle(int index) {
        checkIndex(index);
        ensureAnglesComputed();
        return cachedStartAngles[index] + cachedSweepAngles[index];
    }

    private void ensureAnglesComputed() {
        if (!Float.isNaN(cachedTotal)) {
            return;
        }

        float total = 0;
        for (int i = 0; i < size; i++) {
            if (values[i] > 0) {
                total += values[i];
            }
        }
        cachedTotal = total;

        if (total <= 0) {
            Arrays.fill(cachedStartAngles, 0, size, 0);
            Arrays.fill(cachedSweepAngles, 0, size, 0);
            return;
        }

        double startAngle = -Math.PI / 2; // Start at 12 o'clock
        for (int i = 0; i < size; i++) {
            cachedStartAngles[i] = startAngle;
            double sweep = (values[i] > 0) ? (values[i] / total) * 2 * Math.PI : 0;
            cachedSweepAngles[i] = sweep;
            startAngle += sweep;
        }
    }

    private void invalidateCache() {
        cachedTotal = Float.NaN;
    }

    // ========== Aggregate Values ==========

    /**
     * Returns the sum of all values.
     */
    public float getTotal() {
        ensureAnglesComputed();
        return cachedTotal;
    }

    /**
     * Returns the minimum value.
     */
    public float getMinValue() {
        if (size == 0) {
            return Float.NaN;
        }
        float min = Float.POSITIVE_INFINITY;
        for (int i = 0; i < size; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    /**
     * Returns the maximum value.
     */
    public float getMaxValue() {
        if (size == 0) {
            return Float.NaN;
        }
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < size; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * Returns the index of the segment containing the given angle.
     *
     * @param angle angle in radians
     * @return segment index, or -1 if not found
     */
    public int indexAtAngle(double angle) {
        ensureAnglesComputed();

        // Normalize angle to [-PI, PI]
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }

        for (int i = 0; i < size; i++) {
            double start = cachedStartAngles[i];
            double end = start + cachedSweepAngles[i];

            // Handle wrap-around
            if (end > Math.PI) {
                if (angle >= start || angle <= end - 2 * Math.PI) {
                    return i;
                }
            } else if (angle >= start && angle <= end) {
                return i;
            }
        }
        return -1;
    }

    // ========== Mutation ==========

    /**
     * Appends a new segment.
     *
     * @param label segment label
     * @param value segment value
     * @return the index of the added segment
     */
    public int append(String label, float value) {
        return append(label, value, 0);
    }

    /**
     * Appends a new segment with an explode offset.
     *
     * @param label segment label
     * @param value segment value
     * @param explodeOffset offset for exploding the segment
     * @return the index of the added segment
     */
    public int append(String label, float value, float explodeOffset) {
        ensureCapacity(size + 1);

        labels[size] = label;
        values[size] = Math.max(0, value);
        explodeOffsets[size] = Math.max(0, explodeOffset);

        invalidateCache();
        int addedIndex = size;
        size++;

        listenerSupport.fireDataAppended(null, addedIndex);
        return addedIndex;
    }

    /**
     * Updates the value at the specified index.
     *
     * @param index segment index
     * @param value new value
     */
    public void setValue(int index, float value) {
        checkIndex(index);
        values[index] = Math.max(0, value);
        invalidateCache();
        listenerSupport.fireDataUpdated(null, index);
    }

    /**
     * Updates the segment at the specified index.
     *
     * @param index segment index
     * @param label new label
     * @param value new value
     */
    public void update(int index, String label, float value) {
        checkIndex(index);
        labels[index] = label;
        values[index] = Math.max(0, value);
        invalidateCache();
        listenerSupport.fireDataUpdated(null, index);
    }

    /**
     * Sets the explode offset for a segment.
     *
     * @param index segment index
     * @param offset explode offset (0 = not exploded)
     */
    public void setExplodeOffset(int index, float offset) {
        checkIndex(index);
        explodeOffsets[index] = Math.max(0, offset);
        listenerSupport.fireDataUpdated(null, index);
    }

    /**
     * Explodes a segment by the specified amount.
     *
     * @param index segment index
     * @param offset explode offset
     */
    public void explode(int index, float offset) {
        setExplodeOffset(index, offset);
    }

    /**
     * Resets all explode offsets to 0.
     */
    public void resetExplode() {
        Arrays.fill(explodeOffsets, 0, size, 0);
        listenerSupport.fireDataUpdated(null, 0);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     *
     * @param labels array of labels
     * @param values array of values
     */
    public void loadFromArrays(String[] labels, float[] values) {
        loadFromArrays(labels, values, null);
    }

    /**
     * Loads data from arrays with explode offsets. Replaces any existing data.
     *
     * @param labels array of labels
     * @param values array of values
     * @param explodeOffsets array of explode offsets (null = all zeros)
     */
    public void loadFromArrays(String[] labels, float[] values, float[] explodeOffsets) {
        int length = values.length;
        if (labels.length != length) {
            throw new IllegalArgumentException("Labels and values arrays must have same length");
        }
        if (explodeOffsets != null && explodeOffsets.length != length) {
            throw new IllegalArgumentException("Explode offsets array must have same length");
        }

        ensureCapacity(length);
        System.arraycopy(labels, 0, this.labels, 0, length);
        System.arraycopy(values, 0, this.values, 0, length);

        if (explodeOffsets != null) {
            System.arraycopy(explodeOffsets, 0, this.explodeOffsets, 0, length);
        } else {
            Arrays.fill(this.explodeOffsets, 0, length, 0);
        }

        this.size = length;
        invalidateCache();
    }

    @Override
    public void clear() {
        super.clear();
        invalidateCache();
    }
}
