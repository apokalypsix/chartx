package com.apokalypsix.chartx.core.data;

import com.apokalypsix.chartx.chart.data.XyData;

import java.util.List;

/**
 * Service for computing cumulative (stacked) values from multiple series.
 *
 * <p>Supports both standard stacking (cumulative sum) and 100% stacking
 * (normalized to percentages). Handles NaN gaps and positive/negative
 * value separation.
 *
 * <p>This class caches computed values and tracks the visible range to
 * avoid recomputation when the data hasn't changed.
 */
public class StackingCalculator {

    /** Stacking mode enumeration */
    public enum StackMode {
        /** Standard stacking: cumulative sum */
        NORMAL,
        /** 100% stacking: values normalized to percentages */
        PERCENT_100
    }

    // Cached computation results
    private float[][] stackedBaselines;  // Bottom of each series [seriesIdx][dataIdx]
    private float[][] stackedTops;       // Top of each series [seriesIdx][dataIdx]
    private float[][] percentValues;     // Percentage values for 100% mode

    // Cache invalidation tracking
    private List<XyData> lastSeriesList;
    private int lastStartIdx = -1;
    private int lastEndIdx = -1;
    private StackMode lastMode = null;
    private long lastComputeVersion;

    // Reusable arrays for computation
    private float[] positiveStack;
    private float[] negativeStack;
    private float[] positiveSum;
    private float[] negativeSum;

    /**
     * Creates a new stacking calculator.
     */
    public StackingCalculator() {
    }

    /**
     * Computes stacked values for a list of series.
     *
     * <p>After calling this method, use {@link #getStackedBaseline(int, int)}
     * and {@link #getStackedTop(int, int)} to retrieve the computed values.
     *
     * @param seriesList list of XyData series to stack (bottom to top order)
     * @param startIdx starting data index
     * @param endIdx ending data index (inclusive)
     * @param mode stacking mode (NORMAL or PERCENT_100)
     */
    public void compute(List<XyData> seriesList, int startIdx, int endIdx, StackMode mode) {
        if (seriesList == null || seriesList.isEmpty() || startIdx > endIdx) {
            return;
        }

        // Check if we can reuse cached results
        if (isCacheValid(seriesList, startIdx, endIdx, mode)) {
            return;
        }

        int seriesCount = seriesList.size();
        int dataCount = endIdx - startIdx + 1;

        ensureCapacity(seriesCount, dataCount);

        if (mode == StackMode.PERCENT_100) {
            computePercentStacking(seriesList, startIdx, endIdx, dataCount);
        } else {
            computeNormalStacking(seriesList, startIdx, endIdx, dataCount);
        }

        // Update cache tracking
        lastSeriesList = seriesList;
        lastStartIdx = startIdx;
        lastEndIdx = endIdx;
        lastMode = mode;
        lastComputeVersion = computeDataVersion(seriesList);
    }

    /**
     * Returns the baseline (bottom) value for a stacked series at a data index.
     *
     * @param seriesIndex index in the series list (0 = bottom series)
     * @param dataIndex index in the data array
     * @return baseline value in data units
     */
    public float getStackedBaseline(int seriesIndex, int dataIndex) {
        if (stackedBaselines == null || seriesIndex < 0 ||
            seriesIndex >= stackedBaselines.length) {
            return 0;
        }
        int adjustedIdx = dataIndex - lastStartIdx;
        if (adjustedIdx < 0 || adjustedIdx >= stackedBaselines[seriesIndex].length) {
            return 0;
        }
        return stackedBaselines[seriesIndex][adjustedIdx];
    }

    /**
     * Returns the top value for a stacked series at a data index.
     *
     * @param seriesIndex index in the series list (0 = bottom series)
     * @param dataIndex index in the data array
     * @return top value in data units
     */
    public float getStackedTop(int seriesIndex, int dataIndex) {
        if (stackedTops == null || seriesIndex < 0 ||
            seriesIndex >= stackedTops.length) {
            return 0;
        }
        int adjustedIdx = dataIndex - lastStartIdx;
        if (adjustedIdx < 0 || adjustedIdx >= stackedTops[seriesIndex].length) {
            return 0;
        }
        return stackedTops[seriesIndex][adjustedIdx];
    }

    /**
     * Returns the percentage value for 100% stacking mode.
     *
     * @param seriesIndex index in the series list
     * @param dataIndex index in the data array
     * @return percentage value (0-100), or NaN if not valid
     */
    public float getPercentValue(int seriesIndex, int dataIndex) {
        if (percentValues == null || seriesIndex < 0 ||
            seriesIndex >= percentValues.length) {
            return Float.NaN;
        }
        int adjustedIdx = dataIndex - lastStartIdx;
        if (adjustedIdx < 0 || adjustedIdx >= percentValues[seriesIndex].length) {
            return Float.NaN;
        }
        return percentValues[seriesIndex][adjustedIdx];
    }

    /**
     * Finds the minimum stacked value across all series in the visible range.
     */
    public float findMinStackedValue() {
        if (stackedBaselines == null) {
            return 0;
        }
        float min = Float.POSITIVE_INFINITY;
        for (float[] baselines : stackedBaselines) {
            for (float baseline : baselines) {
                if (!Float.isNaN(baseline) && baseline < min) {
                    min = baseline;
                }
            }
        }
        return Float.isInfinite(min) ? 0 : min;
    }

    /**
     * Finds the maximum stacked value across all series in the visible range.
     */
    public float findMaxStackedValue() {
        if (stackedTops == null) {
            return 0;
        }
        float max = Float.NEGATIVE_INFINITY;
        for (float[] tops : stackedTops) {
            for (float top : tops) {
                if (!Float.isNaN(top) && top > max) {
                    max = top;
                }
            }
        }
        return Float.isInfinite(max) ? 0 : max;
    }

    /**
     * Clears the cache, forcing recomputation on next call.
     */
    public void invalidateCache() {
        lastSeriesList = null;
        lastStartIdx = -1;
        lastEndIdx = -1;
        lastMode = null;
    }

    // ========== Internal computation ==========

    private void computeNormalStacking(List<XyData> seriesList, int startIdx,
                                       int endIdx, int dataCount) {
        int seriesCount = seriesList.size();

        // Initialize running stacks
        java.util.Arrays.fill(positiveStack, 0, dataCount, 0);
        java.util.Arrays.fill(negativeStack, 0, dataCount, 0);

        for (int s = 0; s < seriesCount; s++) {
            XyData series = seriesList.get(s);
            float[] values = series.getValuesArray();

            for (int d = 0; d < dataCount; d++) {
                int dataIdx = startIdx + d;

                if (dataIdx >= series.size()) {
                    stackedBaselines[s][d] = Float.NaN;
                    stackedTops[s][d] = Float.NaN;
                    continue;
                }

                float value = values[dataIdx];
                if (Float.isNaN(value)) {
                    stackedBaselines[s][d] = Float.NaN;
                    stackedTops[s][d] = Float.NaN;
                    continue;
                }

                // Stack positive and negative values separately
                if (value >= 0) {
                    stackedBaselines[s][d] = positiveStack[d];
                    positiveStack[d] += value;
                    stackedTops[s][d] = positiveStack[d];
                } else {
                    stackedBaselines[s][d] = negativeStack[d];
                    negativeStack[d] += value;
                    stackedTops[s][d] = negativeStack[d];
                }
            }
        }
    }

    private void computePercentStacking(List<XyData> seriesList, int startIdx,
                                        int endIdx, int dataCount) {
        int seriesCount = seriesList.size();

        // First pass: compute totals for each data point
        java.util.Arrays.fill(positiveSum, 0, dataCount, 0);
        java.util.Arrays.fill(negativeSum, 0, dataCount, 0);

        for (XyData series : seriesList) {
            float[] values = series.getValuesArray();
            for (int d = 0; d < dataCount; d++) {
                int dataIdx = startIdx + d;
                if (dataIdx >= series.size()) continue;

                float value = values[dataIdx];
                if (Float.isNaN(value)) continue;

                if (value >= 0) {
                    positiveSum[d] += value;
                } else {
                    negativeSum[d] += Math.abs(value);
                }
            }
        }

        // Second pass: compute stacked percentages
        java.util.Arrays.fill(positiveStack, 0, dataCount, 0);
        java.util.Arrays.fill(negativeStack, 0, dataCount, 0);

        for (int s = 0; s < seriesCount; s++) {
            XyData series = seriesList.get(s);
            float[] values = series.getValuesArray();

            for (int d = 0; d < dataCount; d++) {
                int dataIdx = startIdx + d;

                if (dataIdx >= series.size()) {
                    stackedBaselines[s][d] = Float.NaN;
                    stackedTops[s][d] = Float.NaN;
                    percentValues[s][d] = Float.NaN;
                    continue;
                }

                float value = values[dataIdx];
                if (Float.isNaN(value)) {
                    stackedBaselines[s][d] = Float.NaN;
                    stackedTops[s][d] = Float.NaN;
                    percentValues[s][d] = Float.NaN;
                    continue;
                }

                // Compute percentage
                float total = (value >= 0) ? positiveSum[d] : negativeSum[d];
                float percent;
                if (total > 0) {
                    percent = (Math.abs(value) / total) * 100;
                } else {
                    percent = 0;
                }
                percentValues[s][d] = percent;

                // Stack the percentage values
                if (value >= 0) {
                    stackedBaselines[s][d] = positiveStack[d];
                    positiveStack[d] += percent;
                    stackedTops[s][d] = positiveStack[d];
                } else {
                    stackedBaselines[s][d] = -negativeStack[d];
                    negativeStack[d] += percent;
                    stackedTops[s][d] = -negativeStack[d];
                }
            }
        }
    }

    private void ensureCapacity(int seriesCount, int dataCount) {
        // Ensure output arrays
        if (stackedBaselines == null || stackedBaselines.length < seriesCount) {
            stackedBaselines = new float[seriesCount][];
            stackedTops = new float[seriesCount][];
            percentValues = new float[seriesCount][];
        }

        for (int s = 0; s < seriesCount; s++) {
            if (stackedBaselines[s] == null || stackedBaselines[s].length < dataCount) {
                stackedBaselines[s] = new float[dataCount];
                stackedTops[s] = new float[dataCount];
                percentValues[s] = new float[dataCount];
            }
        }

        // Ensure working arrays
        if (positiveStack == null || positiveStack.length < dataCount) {
            positiveStack = new float[dataCount];
            negativeStack = new float[dataCount];
            positiveSum = new float[dataCount];
            negativeSum = new float[dataCount];
        }
    }

    private boolean isCacheValid(List<XyData> seriesList, int startIdx,
                                 int endIdx, StackMode mode) {
        if (lastSeriesList != seriesList || lastMode != mode ||
            lastStartIdx != startIdx || lastEndIdx != endIdx) {
            return false;
        }

        // Check if any series data has changed
        return computeDataVersion(seriesList) == lastComputeVersion;
    }

    private long computeDataVersion(List<XyData> seriesList) {
        // Simple version based on series sizes
        long version = 0;
        for (XyData series : seriesList) {
            version = version * 31 + series.size();
        }
        return version;
    }
}
