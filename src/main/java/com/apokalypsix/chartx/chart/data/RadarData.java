package com.apokalypsix.chartx.chart.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data for radar/spider charts.
 *
 * <p>Stores multiple data series that share a common set of axes (spokes).
 * Each series is a set of values, one for each axis. The data can be used
 * to render overlapping polygons on a polar coordinate system.
 *
 * <p>X-values are sequential indices (0, 1, 2...) representing each series.
 * Use labels to provide series names for display.
 *
 * <p>Example: A radar chart comparing products on dimensions like
 * "Price", "Quality", "Features", "Support", "Reliability".
 */
public class RadarData extends AbstractData<float[][]> {

    /** Axis labels (spoke names) */
    private final List<String> axisLabels;

    /** Axis min values */
    private float[] axisMin;

    /** Axis max values */
    private float[] axisMax;

    /** Values for each series and each axis: values[seriesIndex][axisIndex] */
    private float[][] seriesValues;

    /** Series names */
    private final List<String> seriesNames;

    /** Number of axes */
    private int axisCount;

    /**
     * Creates radar data with the specified axes.
     *
     * @param id unique identifier
     * @param name display name
     * @param axisLabels labels for each axis
     */
    public RadarData(String id, String name, String... axisLabels) {
        super(id, name);
        this.axisLabels = new ArrayList<>(Arrays.asList(axisLabels));
        this.axisCount = axisLabels.length;
        this.seriesNames = new ArrayList<>();

        // Initialize axis ranges to auto-scale
        this.axisMin = new float[axisCount];
        this.axisMax = new float[axisCount];
        Arrays.fill(axisMin, 0);
        Arrays.fill(axisMax, 1);

        // Re-initialize seriesValues with correct axisCount
        // (initializeValueArrays was called from super() when axisCount was still 0)
        reinitializeSeriesValues();
    }

    /**
     * Reinitializes the series values array after axisCount is set.
     */
    private void reinitializeSeriesValues() {
        int capacity = labels.length;
        this.seriesValues = new float[capacity][axisCount];
    }

    @Override
    protected void initializeValueArrays(int capacity) {
        this.seriesValues = new float[capacity][axisCount];
    }

    @Override
    protected void growValueArrays(int newCapacity) {
        float[][] newValues = new float[newCapacity][];
        for (int i = 0; i < size; i++) {
            newValues[i] = seriesValues[i];
        }
        for (int i = size; i < newCapacity; i++) {
            newValues[i] = new float[axisCount];
        }
        seriesValues = newValues;
    }

    @Override
    protected void shiftValueArrays(int index, int count) {
        System.arraycopy(seriesValues, index + 1, seriesValues, index, count);
        seriesNames.remove(index);
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

    // ========== Axis Management ==========

    /**
     * Returns the number of axes (spokes).
     */
    public int getAxisCount() {
        return axisCount;
    }

    /**
     * Returns the label for the axis at the given index.
     */
    public String getAxisLabel(int axisIndex) {
        if (axisIndex < 0 || axisIndex >= axisCount) {
            return "";
        }
        return axisLabels.get(axisIndex);
    }

    /**
     * Returns all axis labels.
     */
    public List<String> getAxisLabels() {
        return new ArrayList<>(axisLabels);
    }

    /**
     * Sets the range for an axis.
     *
     * @param axisIndex axis index
     * @param min minimum value
     * @param max maximum value
     */
    public void setAxisRange(int axisIndex, float min, float max) {
        if (axisIndex >= 0 && axisIndex < axisCount) {
            axisMin[axisIndex] = min;
            axisMax[axisIndex] = max;
        }
    }

    /**
     * Sets the same range for all axes.
     */
    public void setAllAxisRange(float min, float max) {
        Arrays.fill(axisMin, min);
        Arrays.fill(axisMax, max);
    }

    /**
     * Returns the minimum value for an axis.
     */
    public float getAxisMin(int axisIndex) {
        if (axisIndex < 0 || axisIndex >= axisCount) {
            return 0;
        }
        return axisMin[axisIndex];
    }

    /**
     * Returns the maximum value for an axis.
     */
    public float getAxisMax(int axisIndex) {
        if (axisIndex < 0 || axisIndex >= axisCount) {
            return 1;
        }
        return axisMax[axisIndex];
    }

    /**
     * Returns the angle for an axis (in radians).
     *
     * @param axisIndex axis index
     * @return angle in radians, starting at -PI/2 (12 o'clock)
     */
    public double getAxisAngle(int axisIndex) {
        return -Math.PI / 2 + (2 * Math.PI * axisIndex / axisCount);
    }

    // ========== Series Management ==========

    /**
     * Returns the number of data series.
     */
    public int getSeriesCount() {
        return size;
    }

    /**
     * Returns the name of a series.
     */
    public String getSeriesName(int seriesIndex) {
        if (seriesIndex < 0 || seriesIndex >= size) {
            return "";
        }
        return seriesNames.get(seriesIndex);
    }

    /**
     * Returns all series names.
     */
    public List<String> getSeriesNames() {
        return new ArrayList<>(seriesNames);
    }

    // ========== Value Accessors ==========

    /**
     * Returns the value for a specific series and axis.
     */
    public float getValue(int seriesIndex, int axisIndex) {
        checkIndex(seriesIndex);
        if (axisIndex < 0 || axisIndex >= axisCount) {
            return Float.NaN;
        }
        return seriesValues[seriesIndex][axisIndex];
    }

    /**
     * Returns all values for a series.
     */
    public float[] getSeriesValues(int seriesIndex) {
        checkIndex(seriesIndex);
        return Arrays.copyOf(seriesValues[seriesIndex], axisCount);
    }

    /**
     * Returns the normalized value (0-1) for a specific series and axis.
     */
    public float getNormalizedValue(int seriesIndex, int axisIndex) {
        float value = getValue(seriesIndex, axisIndex);
        float min = axisMin[axisIndex];
        float max = axisMax[axisIndex];
        float range = max - min;
        if (range <= 0) {
            return 0.5f;
        }
        return Math.max(0, Math.min(1, (value - min) / range));
    }

    /**
     * Returns the raw values array. For rendering use only.
     */
    public float[][] getSeriesValuesArray() {
        return seriesValues;
    }

    // ========== Mutation ==========

    /**
     * Adds a new data series.
     *
     * @param name series name
     * @param values values for each axis (must match axis count)
     * @return the index of the added series
     */
    public int addSeries(String name, float... values) {
        if (values.length != axisCount) {
            throw new IllegalArgumentException(
                    "Values length (" + values.length + ") must match axis count (" + axisCount + ")");
        }

        ensureCapacity(size + 1);

        seriesNames.add(name);
        labels[size] = name;
        System.arraycopy(values, 0, seriesValues[size], 0, axisCount);

        int addedIndex = size;
        size++;

        listenerSupport.fireDataAppended(null, addedIndex);
        return addedIndex;
    }

    /**
     * Updates the values for an existing series.
     *
     * @param seriesIndex series index
     * @param values new values for each axis
     */
    public void updateSeries(int seriesIndex, float... values) {
        checkIndex(seriesIndex);
        if (values.length != axisCount) {
            throw new IllegalArgumentException(
                    "Values length (" + values.length + ") must match axis count (" + axisCount + ")");
        }

        System.arraycopy(values, 0, seriesValues[seriesIndex], 0, axisCount);
        listenerSupport.fireDataUpdated(null, seriesIndex);
    }

    /**
     * Updates a single value.
     *
     * @param seriesIndex series index
     * @param axisIndex axis index
     * @param value new value
     */
    public void setValue(int seriesIndex, int axisIndex, float value) {
        checkIndex(seriesIndex);
        if (axisIndex < 0 || axisIndex >= axisCount) {
            throw new IndexOutOfBoundsException("Axis index: " + axisIndex + ", Count: " + axisCount);
        }

        seriesValues[seriesIndex][axisIndex] = value;
        listenerSupport.fireDataUpdated(null, seriesIndex);
    }

    /**
     * Auto-scales axis ranges based on all series values.
     */
    public void autoScaleAxes() {
        if (size == 0) {
            return;
        }

        for (int a = 0; a < axisCount; a++) {
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;

            for (int s = 0; s < size; s++) {
                float value = seriesValues[s][a];
                if (!Float.isNaN(value)) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }

            if (Float.isFinite(min) && Float.isFinite(max)) {
                // Add 10% padding
                float range = max - min;
                float padding = range * 0.1f;
                axisMin[a] = min - padding;
                axisMax[a] = max + padding;
            }
        }
    }

    /**
     * Auto-scales all axes to a common range.
     */
    public void autoScaleUniform() {
        if (size == 0) {
            return;
        }

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int s = 0; s < size; s++) {
            for (int a = 0; a < axisCount; a++) {
                float value = seriesValues[s][a];
                if (!Float.isNaN(value)) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }

        if (Float.isFinite(min) && Float.isFinite(max)) {
            float range = max - min;
            float padding = range * 0.1f;
            setAllAxisRange(min - padding, max + padding);
        }
    }

    @Override
    public void clear() {
        super.clear();
        seriesNames.clear();
    }
}
