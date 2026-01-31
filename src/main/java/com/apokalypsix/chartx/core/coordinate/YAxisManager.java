package com.apokalypsix.chartx.core.coordinate;

import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.util.*;

/**
 * Manages multiple Y-axes for a chart pane.
 *
 * <p>Handles axis creation, series-to-axis associations, inset calculations
 * based on visible axes, and per-axis auto-scaling.
 */
public class YAxisManager {

    private final Map<String, YAxis> axes = new LinkedHashMap<>();
    private final Map<String, String> seriesToAxis = new HashMap<>();

    // Cached insets based on visible axes
    private int leftInset = 0;
    private int rightInset = 60; // Default right inset for single axis
    private boolean insetsDirty = true;

    /**
     * Creates a YAxisManager with a default axis.
     */
    public YAxisManager() {
        // Create default axis on the right side
        YAxis defaultAxis = new YAxis(YAxis.DEFAULT_AXIS_ID, YAxis.Position.RIGHT);
        axes.put(defaultAxis.getId(), defaultAxis);
    }

    // ========== Axis management ==========

    /**
     * Creates a new Y-axis with the given ID and position.
     *
     * @param id unique axis ID
     * @param position axis position (LEFT or RIGHT)
     * @return the created axis
     * @throws IllegalArgumentException if an axis with this ID already exists
     */
    public YAxis createAxis(String id, YAxis.Position position) {
        if (axes.containsKey(id)) {
            throw new IllegalArgumentException("Axis with ID '" + id + "' already exists");
        }
        YAxis axis = new YAxis(id, position);
        axes.put(id, axis);
        insetsDirty = true;
        return axis;
    }

    /**
     * Returns the axis with the given ID, or null if not found.
     */
    public YAxis getAxis(String id) {
        return axes.get(id);
    }

    /**
     * Returns the default axis.
     */
    public YAxis getDefaultAxis() {
        return axes.get(YAxis.DEFAULT_AXIS_ID);
    }

    /**
     * Returns all axes in creation order.
     */
    public Collection<YAxis> getAllAxes() {
        return Collections.unmodifiableCollection(axes.values());
    }

    /**
     * Returns all visible axes.
     */
    public List<YAxis> getVisibleAxes() {
        List<YAxis> visible = new ArrayList<>();
        for (YAxis axis : axes.values()) {
            if (axis.isVisible()) {
                visible.add(axis);
            }
        }
        return visible;
    }

    /**
     * Returns all axes at the specified position.
     */
    public List<YAxis> getAxesAtPosition(YAxis.Position position) {
        List<YAxis> result = new ArrayList<>();
        for (YAxis axis : axes.values()) {
            if (axis.getPosition() == position) {
                result.add(axis);
            }
        }
        return result;
    }

    /**
     * Returns visible axes at the specified position in display order.
     */
    public List<YAxis> getVisibleAxesAtPosition(YAxis.Position position) {
        List<YAxis> result = new ArrayList<>();
        for (YAxis axis : axes.values()) {
            if (axis.isVisible() && axis.getPosition() == position) {
                result.add(axis);
            }
        }
        return result;
    }

    /**
     * Removes an axis. Cannot remove the default axis.
     *
     * @param id axis ID to remove
     * @return true if removed, false if not found or is default
     */
    public boolean removeAxis(String id) {
        if (YAxis.DEFAULT_AXIS_ID.equals(id)) {
            return false; // Cannot remove default axis
        }
        boolean removed = axes.remove(id) != null;
        if (removed) {
            // Remove any series associations
            seriesToAxis.values().removeIf(axisId -> axisId.equals(id));
            insetsDirty = true;
        }
        return removed;
    }

    // ========== Series-to-axis associations ==========

    /**
     * Associates a series with an axis.
     *
     * @param seriesId the series ID
     * @param axisId the axis ID
     * @throws IllegalArgumentException if axis does not exist
     */
    public void setSeriesAxis(String seriesId, String axisId) {
        if (!axes.containsKey(axisId)) {
            throw new IllegalArgumentException("Axis '" + axisId + "' does not exist");
        }
        seriesToAxis.put(seriesId, axisId);
    }

    /**
     * Returns the axis ID for a series, or the default axis ID if not explicitly set.
     */
    public String getAxisIdForSeries(String seriesId) {
        return seriesToAxis.getOrDefault(seriesId, YAxis.DEFAULT_AXIS_ID);
    }

    /**
     * Returns the axis for a series.
     */
    public YAxis getAxisForSeries(String seriesId) {
        String axisId = getAxisIdForSeries(seriesId);
        return axes.get(axisId);
    }

    /**
     * Returns all series IDs associated with the given axis.
     */
    public List<String> getSeriesForAxis(String axisId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : seriesToAxis.entrySet()) {
            if (entry.getValue().equals(axisId)) {
                result.add(entry.getKey());
            }
        }
        // Include series using default axis implicitly
        if (YAxis.DEFAULT_AXIS_ID.equals(axisId)) {
            // Default axis gets any series not explicitly assigned
            // This is handled by getAxisIdForSeries returning default
        }
        return result;
    }

    // ========== Inset calculations ==========

    /**
     * Recalculates left and right insets based on visible axes and their widths.
     */
    public void updateInsets() {
        leftInset = 0;
        rightInset = 0;

        for (YAxis axis : axes.values()) {
            if (axis.isVisible()) {
                if (axis.getPosition() == YAxis.Position.LEFT) {
                    leftInset += axis.getWidth();
                } else {
                    rightInset += axis.getWidth();
                }
            }
        }

        // Ensure minimum right inset even if no visible axes
        if (rightInset == 0 && leftInset == 0) {
            rightInset = 60; // Default
        }

        insetsDirty = false;
    }

    /**
     * Returns the left inset (total width of visible left axes).
     */
    public int getLeftInset() {
        if (insetsDirty) {
            updateInsets();
        }
        return leftInset;
    }

    /**
     * Returns the right inset (total width of visible right axes).
     */
    public int getRightInset() {
        if (insetsDirty) {
            updateInsets();
        }
        return rightInset;
    }

    /**
     * Marks the insets as needing recalculation.
     */
    public void invalidateInsets() {
        insetsDirty = true;
    }

    // ========== Auto-scaling ==========

    /**
     * Auto-scales the specified axis to fit OHLC data within the visible time range.
     *
     * @param axisId the axis to scale
     * @param data the OHLC data
     * @param startTime visible start time
     * @param endTime visible end time
     */
    public void autoScaleToOHLC(String axisId, OhlcData data, long startTime, long endTime) {
        YAxis axis = axes.get(axisId);
        if (axis == null || !axis.isAutoScale() || data == null || data.isEmpty()) {
            return;
        }

        int firstIdx = data.indexAtOrAfter(startTime);
        int lastIdx = data.indexAtOrBefore(endTime);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        double high = data.findHighestHigh(firstIdx, lastIdx);
        double low = data.findLowestLow(firstIdx, lastIdx);

        double padding = (high - low) * axis.getAutoScalePadding();
        axis.setValueRange(low - padding, high + padding);
    }

    /**
     * Auto-scales the specified axis to fit histogram data within the visible time range.
     *
     * @param axisId the axis to scale
     * @param data the histogram data
     * @param startTime visible start time
     * @param endTime visible end time
     */
    public void autoScaleToHistogram(String axisId, HistogramData data, long startTime, long endTime) {
        YAxis axis = axes.get(axisId);
        if (axis == null || !axis.isAutoScale() || data == null || data.isEmpty()) {
            return;
        }

        int firstIdx = data.indexAtOrAfter(startTime);
        int lastIdx = data.indexAtOrBefore(endTime);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        double maxVal = data.findMaxValue(firstIdx, lastIdx);
        double minVal = data.findMinValue(firstIdx, lastIdx);

        // For histograms, ensure baseline (0) is visible
        double low = Math.min(0, minVal);
        double high = Math.max(0, maxVal);

        double span = high - low;
        double padding = span * axis.getAutoScalePadding();
        axis.setValueRange(low - padding, high + padding);
    }

    /**
     * Auto-scales the specified axis to fit XY data within the visible time range.
     *
     * @param axisId the axis to scale
     * @param data the XY data
     * @param startTime visible start time
     * @param endTime visible end time
     */
    public void autoScaleToXyData(String axisId, XyData data, long startTime, long endTime) {
        YAxis axis = axes.get(axisId);
        if (axis == null || !axis.isAutoScale() || data == null || data.isEmpty()) {
            return;
        }

        int firstIdx = data.indexAtOrAfter(startTime);
        int lastIdx = data.indexAtOrBefore(endTime);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        double maxVal = data.findMaxValue(firstIdx, lastIdx);
        double minVal = data.findMinValue(firstIdx, lastIdx);

        if (Double.isNaN(maxVal) || Double.isNaN(minVal)) {
            return;
        }

        double span = maxVal - minVal;
        double padding = span * axis.getAutoScalePadding();
        axis.setValueRange(minVal - padding, maxVal + padding);
    }

    /**
     * Auto-scales an axis based on generic data.
     * Delegates to the appropriate type-specific method.
     */
    public void autoScaleToData(String axisId, Data<?> data, long startTime, long endTime) {
        if (data instanceof OhlcData) {
            autoScaleToOHLC(axisId, (OhlcData) data, startTime, endTime);
        } else if (data instanceof HistogramData) {
            autoScaleToHistogram(axisId, (HistogramData) data, startTime, endTime);
        } else if (data instanceof XyData) {
            autoScaleToXyData(axisId, (XyData) data, startTime, endTime);
        }
    }
}
