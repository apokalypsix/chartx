package com.apokalypsix.chartx.chart.data;

import java.util.Arrays;

/**
 * Category-based grouped column data for grouped bar/column charts.
 *
 * <p>Stores multiple value arrays per category, where each array represents
 * a group (e.g., different product lines). Data is stored in parallel primitive
 * arrays for cache-friendly access.
 *
 * <p>X-values are sequential indices (0, 1, 2...) representing categories.
 * Use labels to provide category names for display.
 *
 * <p>Example: monthly sales data with 3 product categories would have
 * indices for each month (Jan=0, Feb=1, ...) and 3 value arrays (one per product).
 */
public class GroupedColumnData extends AbstractData<float[]> {

    // Value arrays per group
    private float[][] groupValues;
    private String[] groupNames;
    private int groupCount;

    /**
     * Creates grouped column data with the specified number of groups.
     *
     * @param id unique identifier
     * @param name display name
     * @param groupCount number of value groups per category
     */
    public GroupedColumnData(String id, String name, int groupCount) {
        super(id, name);
        if (groupCount < 1) {
            throw new IllegalArgumentException("Group count must be at least 1");
        }
        this.groupCount = groupCount;
        this.groupNames = new String[groupCount];
        for (int i = 0; i < groupCount; i++) {
            groupNames[i] = "Group " + (i + 1);
        }
        // Reinitialize value arrays now that groupCount is set
        initializeValueArrays(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates grouped column data with named groups.
     *
     * @param id unique identifier
     * @param name display name
     * @param groupNames names for each group
     */
    public GroupedColumnData(String id, String name, String[] groupNames) {
        super(id, name);
        if (groupNames == null || groupNames.length < 1) {
            throw new IllegalArgumentException("At least one group name required");
        }
        this.groupCount = groupNames.length;
        this.groupNames = Arrays.copyOf(groupNames, groupNames.length);
        // Reinitialize value arrays now that groupCount is set
        initializeValueArrays(DEFAULT_INITIAL_CAPACITY);
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        groupValues = new float[groupCount][];
        for (int g = 0; g < groupCount; g++) {
            groupValues[g] = new float[capacity];
        }
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        for (int g = 0; g < groupCount; g++) {
            groupValues[g] = Arrays.copyOf(groupValues[g], newCapacity);
        }
    }

    @Override
    protected void shiftValueArrays(int index, int count) {
        for (int g = 0; g < groupCount; g++) {
            System.arraycopy(groupValues[g], index + 1, groupValues[g], index, count);
        }
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

    // ========== Group accessors ==========

    /**
     * Returns the number of value groups.
     */
    public int getGroupCount() {
        return groupCount;
    }

    /**
     * Returns the name of the specified group.
     */
    public String getGroupName(int groupIndex) {
        return groupNames[groupIndex];
    }

    /**
     * Sets the name of the specified group.
     */
    public void setGroupName(int groupIndex, String name) {
        groupNames[groupIndex] = name;
    }

    /**
     * Returns all group names.
     */
    public String[] getGroupNames() {
        return Arrays.copyOf(groupNames, groupNames.length);
    }

    // ========== Value accessors ==========

    /**
     * Returns the value for a specific group at the given data index.
     *
     * @param index the data index
     * @param groupIndex the group index
     * @return the value
     */
    public float getValue(int index, int groupIndex) {
        checkIndex(index);
        if (groupIndex < 0 || groupIndex >= groupCount) {
            throw new IndexOutOfBoundsException("Group index: " + groupIndex);
        }
        return groupValues[groupIndex][index];
    }

    /**
     * Returns all values at the given data index as an array.
     *
     * @param index the data index
     * @param output array to receive values (must be at least groupCount length)
     */
    public void getValues(int index, float[] output) {
        checkIndex(index);
        for (int g = 0; g < groupCount; g++) {
            output[g] = groupValues[g][index];
        }
    }

    /**
     * Returns the raw value array for a specific group.
     * For rendering use only - do not modify.
     */
    public float[] getGroupValuesArray(int groupIndex) {
        if (groupIndex < 0 || groupIndex >= groupCount) {
            throw new IndexOutOfBoundsException("Group index: " + groupIndex);
        }
        return groupValues[groupIndex];
    }

    // ========== Range queries ==========

    /**
     * Finds the minimum value across all groups in the specified range.
     */
    public float findMinValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float min = Float.POSITIVE_INFINITY;
        for (int g = 0; g < groupCount; g++) {
            for (int i = fromIndex; i <= toIndex; i++) {
                float value = groupValues[g][i];
                if (!Float.isNaN(value) && value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    /**
     * Finds the maximum value across all groups in the specified range.
     */
    public float findMaxValue(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float max = Float.NEGATIVE_INFINITY;
        for (int g = 0; g < groupCount; g++) {
            for (int i = fromIndex; i <= toIndex; i++) {
                float value = groupValues[g][i];
                if (!Float.isNaN(value) && value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    /**
     * Finds the minimum value for a specific group in the specified range.
     */
    public float findMinValue(int fromIndex, int toIndex, int groupIndex) {
        checkRange(fromIndex, toIndex);
        float min = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            float value = groupValues[groupIndex][i];
            if (!Float.isNaN(value) && value < min) {
                min = value;
            }
        }
        return min;
    }

    /**
     * Finds the maximum value for a specific group in the specified range.
     */
    public float findMaxValue(int fromIndex, int toIndex, int groupIndex) {
        checkRange(fromIndex, toIndex);
        float max = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            float value = groupValues[groupIndex][i];
            if (!Float.isNaN(value) && value > max) {
                max = value;
            }
        }
        return max;
    }

    // ========== Mutation ==========

    /**
     * Appends a new data point with values for all groups.
     *
     * @param values values for each group (must have length == groupCount)
     * @throws IllegalArgumentException if values array has wrong length
     */
    public void append(float[] values) {
        append(values, null);
    }

    /**
     * Appends a new data point with values for all groups and a category label.
     *
     * @param values values for each group (must have length == groupCount)
     * @param label the category label
     * @throws IllegalArgumentException if values array has wrong length
     */
    public void append(float[] values, String label) {
        if (values == null || values.length != groupCount) {
            throw new IllegalArgumentException(
                    "Values array must have exactly " + groupCount + " elements");
        }

        ensureCapacity(size + 1);

        for (int g = 0; g < groupCount; g++) {
            groupValues[g][size] = values[g];
        }
        labels[size] = label;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Updates the values at the last index.
     *
     * @param values new values for each group
     * @throws IllegalStateException if data is empty
     */
    public void updateLast(float[] values) {
        checkNotEmpty();
        if (values == null || values.length != groupCount) {
            throw new IllegalArgumentException(
                    "Values array must have exactly " + groupCount + " elements");
        }

        for (int g = 0; g < groupCount; g++) {
            groupValues[g][size - 1] = values[g];
        }
        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Loads data from arrays. Replaces any existing data.
     *
     * @param groupedValues 2D array [groupIndex][dataIndex]
     */
    public void loadFromArrays(float[][] groupedValues) {
        loadFromArrays(groupedValues, null);
    }

    /**
     * Loads data from arrays with optional category labels. Replaces any existing data.
     *
     * @param groupedValues 2D array [groupIndex][dataIndex]
     * @param labels array of category labels (may be null)
     */
    public void loadFromArrays(float[][] groupedValues, String[] labels) {
        if (groupedValues == null || groupedValues.length != groupCount) {
            throw new IllegalArgumentException(
                    "Grouped values must have exactly " + groupCount + " arrays");
        }

        int length = groupedValues[0].length;
        for (float[] values : groupedValues) {
            if (values.length != length) {
                throw new IllegalArgumentException(
                        "All value arrays must have same length");
            }
        }
        if (labels != null && labels.length != length) {
            throw new IllegalArgumentException(
                    "Labels array must have same length as value arrays");
        }

        ensureCapacity(length);
        for (int g = 0; g < groupCount; g++) {
            System.arraycopy(groupedValues[g], 0, this.groupValues[g], 0, length);
        }
        if (labels != null) {
            System.arraycopy(labels, 0, this.labels, 0, length);
        }
        this.size = length;
    }
}
