package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Category-based data for waterfall charts.
 *
 * <p>Stores values and computes running totals for waterfall visualization.
 * Each bar can be marked as a "total" bar which resets or shows cumulative value.
 * Data is stored in parallel primitive arrays for cache-friendly access.
 *
 * <p>X-values are sequential indices (0, 1, 2...) representing categories.
 * Use labels to provide category names for display (e.g., "Revenue", "Costs", "Profit").
 *
 * <p>Waterfall charts display a sequence of positive and negative values
 * showing how an initial value is affected by intermediate changes.
 */
public class WaterfallData extends AbstractData<float[]> {

    /** Individual values (positive or negative) */
    private float[] values;

    /** Computed running totals (baseline for each bar) */
    private float[] runningTotals;

    /** Whether each bar is a "total" bar (shows cumulative value, doesn't add) */
    private boolean[] isTotalBar;

    /**
     * Creates empty WaterfallData with the specified ID and name.
     */
    public WaterfallData(String id, String name) {
        super(id, name);
    }

    /**
     * Creates empty WaterfallData with the specified initial capacity.
     */
    public WaterfallData(String id, String name, int initialCapacity) {
        super(id, name, initialCapacity);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.values = new float[capacity];
        this.runningTotals = new float[capacity];
        this.isTotalBar = new boolean[capacity];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        values = Arrays.copyOf(values, newCapacity);
        runningTotals = Arrays.copyOf(runningTotals, newCapacity);
        isTotalBar = Arrays.copyOf(isTotalBar, newCapacity);
    }

    @Override
    protected void shiftValueArrays(int index, int count) {
        System.arraycopy(values, index + 1, values, index, count);
        System.arraycopy(runningTotals, index + 1, runningTotals, index, count);
        System.arraycopy(isTotalBar, index + 1, isTotalBar, index, count);
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

    /**
     * Returns the value (delta) at the specified index.
     */
    public float getValue(int index) {
        checkIndex(index);
        return values[index];
    }

    /**
     * Returns the running total at the specified index.
     * This is the value that the bar extends to from its baseline.
     */
    public float getRunningTotal(int index) {
        checkIndex(index);
        return runningTotals[index];
    }

    /**
     * Returns the baseline (starting point) for the bar at the specified index.
     * For regular bars, this is the previous running total.
     * For total bars, this is 0.
     */
    public float getBaseline(int index) {
        checkIndex(index);
        if (isTotalBar[index]) {
            return 0;
        }
        return index > 0 ? runningTotals[index - 1] : 0;
    }

    /**
     * Returns whether the bar at the specified index is a total bar.
     */
    public boolean isTotalBar(int index) {
        checkIndex(index);
        return isTotalBar[index];
    }

    /**
     * Returns whether the value at the specified index is positive (increase).
     */
    public boolean isPositive(int index) {
        checkIndex(index);
        return values[index] >= 0;
    }

    /**
     * Returns true if the data at the specified index is valid.
     */
    public boolean hasValue(int index) {
        checkIndex(index);
        return !Float.isNaN(values[index]);
    }

    // ========== Range queries ==========

    /**
     * Finds the minimum value (lowest running total) in the specified index range.
     */
    public float findMinValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float minVal = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(runningTotals[i])) {
                float baseline = getBaseline(i);
                minVal = Math.min(minVal, Math.min(baseline, runningTotals[i]));
            }
        }
        // Also consider 0 as potential minimum for total bars
        if (minVal > 0) {
            for (int i = fromIndex; i <= toIndex; i++) {
                if (isTotalBar[i]) {
                    minVal = Math.min(minVal, 0);
                    break;
                }
            }
        }
        return minVal;
    }

    /**
     * Finds the maximum value (highest running total) in the specified index range.
     */
    public float findMaxValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float maxVal = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!Float.isNaN(runningTotals[i])) {
                float baseline = getBaseline(i);
                maxVal = Math.max(maxVal, Math.max(baseline, runningTotals[i]));
            }
        }
        return maxVal;
    }

    // ========== Mutation ==========

    /**
     * Appends a new data point (non-total bar).
     *
     * @param value the value (positive for increase, negative for decrease)
     */
    public void append(float value) {
        append(value, false, null);
    }

    /**
     * Appends a new data point with an optional label.
     *
     * @param value the value
     * @param label optional label for the bar
     */
    public void append(float value, String label) {
        append(value, false, label);
    }

    /**
     * Appends a new data point.
     *
     * @param value the value (for total bars, this is typically ignored and calculated)
     * @param isTotal whether this is a total bar
     * @param label optional label for the bar
     */
    public void append(float value, boolean isTotal, String label) {
        ensureCapacity(size + 1);

        this.isTotalBar[size] = isTotal;
        this.labels[size] = label;

        if (isTotal) {
            // Total bar shows the current running total
            float prevTotal = size > 0 ? runningTotals[size - 1] : 0;
            this.values[size] = prevTotal; // Store the total value
            this.runningTotals[size] = prevTotal;
        } else {
            this.values[size] = value;
            float prevTotal = size > 0 ? runningTotals[size - 1] : 0;
            this.runningTotals[size] = prevTotal + value;
        }
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Appends a total bar that displays the current cumulative value.
     *
     * @param label optional label for the total bar
     */
    public void appendTotal(String label) {
        append(0, true, label);
    }

    /**
     * Updates the last data point.
     */
    public void updateLast(float value) {
        checkNotEmpty();

        if (isTotalBar[size - 1]) {
            // Don't change total bars via updateLast
            return;
        }

        float prevTotal = size > 1 ? runningTotals[size - 2] : 0;
        this.values[size - 1] = value;
        this.runningTotals[size - 1] = prevTotal + value;

        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     * Running totals are computed automatically.
     *
     * @param values array of values
     * @param isTotalBar array indicating which bars are totals
     */
    public void loadFromArrays(float[] values, boolean[] isTotalBar) {
        loadFromArrays(values, isTotalBar, null);
    }

    /**
     * Loads data from arrays with optional labels. Replaces any existing data.
     * Running totals are computed automatically.
     */
    public void loadFromArrays(float[] values, boolean[] isTotalBar, String[] labels) {
        int length = values.length;
        if (isTotalBar.length != length) {
            throw new IllegalArgumentException("isTotalBar array must have same length as values");
        }
        if (labels != null && labels.length != length) {
            throw new IllegalArgumentException("Labels array must have same length if provided");
        }

        ensureCapacity(length);
        System.arraycopy(values, 0, this.values, 0, length);
        System.arraycopy(isTotalBar, 0, this.isTotalBar, 0, length);

        if (labels != null) {
            System.arraycopy(labels, 0, this.labels, 0, length);
        } else {
            Arrays.fill(this.labels, 0, length, null);
        }

        // Compute running totals
        float runningTotal = 0;
        for (int i = 0; i < length; i++) {
            if (isTotalBar[i]) {
                // Total bar shows current cumulative
                this.runningTotals[i] = runningTotal;
            } else {
                runningTotal += values[i];
                this.runningTotals[i] = runningTotal;
            }
        }

        this.size = length;
    }

    // ========== Raw array access ==========

    public float[] getValuesArray() {
        return values;
    }

    public float[] getRunningTotalsArray() {
        return runningTotals;
    }

    public boolean[] getIsTotalBarArray() {
        return isTotalBar;
    }
}
