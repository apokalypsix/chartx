package com.apokalypsix.chartx.core.data;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.Timeframe;

/**
 * Aggregates OHLC data from lower to higher timeframes.
 *
 * <p>This class provides methods to:
 * <ul>
 *   <li>Convert entire series to a higher timeframe (e.g., 1m → 5m)</li>
 *   <li>Incrementally update an aggregated series with new data</li>
 *   <li>Handle partial/incomplete periods at the end of the series</li>
 * </ul>
 *
 * <p>The aggregation follows standard OHLC rules:
 * <ul>
 *   <li>Open = first bar's open in period</li>
 *   <li>High = highest high in period</li>
 *   <li>Low = lowest low in period</li>
 *   <li>Close = last bar's close in period</li>
 *   <li>Volume = sum of all volumes in period</li>
 * </ul>
 */
public class TimeframeAggregator {

    /**
     * Aggregates an entire OHLC series to a higher timeframe.
     *
     * @param source the source series to aggregate
     * @param targetTimeframe the target timeframe
     * @return a new series with aggregated data
     * @throws IllegalArgumentException if the target timeframe cannot aggregate from source
     */
    public static OhlcData aggregate(OhlcData source, Timeframe targetTimeframe) {
        if (source == null || source.isEmpty()) {
            return new OhlcData(
                source != null ? source.getId() + "_" + targetTimeframe.label : "empty",
                source != null ? source.getName() + " " + targetTimeframe.displayName : "Empty",
                0
            );
        }

        // Estimate resulting size (source bars / bars per period, rounded up)
        long[] srcTimestamps = source.getTimestampsArray();
        int srcSize = source.size();

        // Calculate approximate number of resulting bars
        long timeRange = srcTimestamps[srcSize - 1] - srcTimestamps[0];
        int estimatedBars = Math.max(1, (int) (timeRange / targetTimeframe.millis) + 2);

        String newId = source.getId() + "_" + targetTimeframe.label;
        String newName = source.getName() + " " + targetTimeframe.displayName;
        OhlcData result = new OhlcData(newId, newName, estimatedBars);

        // Get raw arrays for performance
        float[] srcOpen = source.getOpenArray();
        float[] srcHigh = source.getHighArray();
        float[] srcLow = source.getLowArray();
        float[] srcClose = source.getCloseArray();
        float[] srcVolume = source.getVolumeArray();

        // Aggregation state
        long periodStart = targetTimeframe.alignTimestamp(srcTimestamps[0]);
        long periodEnd = periodStart + targetTimeframe.millis;

        float periodOpen = srcOpen[0];
        float periodHigh = srcHigh[0];
        float periodLow = srcLow[0];
        float periodClose = srcClose[0];
        float periodVolume = srcVolume[0];

        for (int i = 1; i < srcSize; i++) {
            long timestamp = srcTimestamps[i];

            if (timestamp >= periodEnd) {
                // Emit completed period
                result.append(periodStart, periodOpen, periodHigh, periodLow, periodClose, periodVolume);

                // Start new period
                periodStart = targetTimeframe.alignTimestamp(timestamp);
                periodEnd = periodStart + targetTimeframe.millis;
                periodOpen = srcOpen[i];
                periodHigh = srcHigh[i];
                periodLow = srcLow[i];
                periodClose = srcClose[i];
                periodVolume = srcVolume[i];
            } else {
                // Update current period
                if (srcHigh[i] > periodHigh) {
                    periodHigh = srcHigh[i];
                }
                if (srcLow[i] < periodLow) {
                    periodLow = srcLow[i];
                }
                periodClose = srcClose[i];
                periodVolume += srcVolume[i];
            }
        }

        // Emit last period (may be incomplete)
        result.append(periodStart, periodOpen, periodHigh, periodLow, periodClose, periodVolume);

        return result;
    }

    /**
     * Incrementally updates an aggregated series with new source data.
     *
     * <p>This method is useful for real-time updates where the source series
     * has received new bars and the aggregated series needs to be updated
     * efficiently without recalculating the entire series.
     *
     * @param aggregated the existing aggregated series
     * @param source the source series with new data
     * @param targetTimeframe the target timeframe
     * @param fromIndex the index in the source series from which to process new data
     */
    public static void updateAggregated(OhlcData aggregated, OhlcData source,
                                        Timeframe targetTimeframe, int fromIndex) {
        if (source == null || source.isEmpty() || fromIndex >= source.size()) {
            return;
        }

        // Get raw arrays
        long[] srcTimestamps = source.getTimestampsArray();
        float[] srcOpen = source.getOpenArray();
        float[] srcHigh = source.getHighArray();
        float[] srcLow = source.getLowArray();
        float[] srcClose = source.getCloseArray();
        float[] srcVolume = source.getVolumeArray();
        int srcSize = source.size();

        // Determine current state of aggregation
        long currentPeriodStart;
        float periodOpen, periodHigh, periodLow, periodClose, periodVolume;

        if (aggregated.isEmpty()) {
            // Start fresh
            currentPeriodStart = targetTimeframe.alignTimestamp(srcTimestamps[fromIndex]);
            periodOpen = srcOpen[fromIndex];
            periodHigh = srcHigh[fromIndex];
            periodLow = srcLow[fromIndex];
            periodClose = srcClose[fromIndex];
            periodVolume = srcVolume[fromIndex];
            fromIndex++;
        } else {
            // Get the last aggregated bar's period
            int lastAggIdx = aggregated.size() - 1;
            currentPeriodStart = aggregated.getXValue(lastAggIdx);

            // Check if fromIndex falls within the current period
            long periodEnd = currentPeriodStart + targetTimeframe.millis;
            long srcTimestamp = srcTimestamps[fromIndex];

            if (srcTimestamp < periodEnd) {
                // Need to update the last aggregated bar
                // Scan back in source to find start of this period
                int periodStartIdx = findPeriodStart(source, currentPeriodStart, fromIndex);

                // Recalculate the last period
                periodOpen = srcOpen[periodStartIdx];
                periodHigh = srcHigh[periodStartIdx];
                periodLow = srcLow[periodStartIdx];
                periodClose = srcClose[periodStartIdx];
                periodVolume = srcVolume[periodStartIdx];

                for (int i = periodStartIdx + 1; i < srcSize && srcTimestamps[i] < periodEnd; i++) {
                    if (srcHigh[i] > periodHigh) periodHigh = srcHigh[i];
                    if (srcLow[i] < periodLow) periodLow = srcLow[i];
                    periodClose = srcClose[i];
                    periodVolume += srcVolume[i];
                }

                // Update the last bar
                aggregated.updateLast(periodOpen, periodHigh, periodLow, periodClose, periodVolume);

                // Find where to continue from
                fromIndex = findNextPeriodIndex(source, periodEnd, fromIndex);
                if (fromIndex >= srcSize) {
                    return;
                }

                // Start new period
                currentPeriodStart = targetTimeframe.alignTimestamp(srcTimestamps[fromIndex]);
            } else {
                // New data starts after current period
                currentPeriodStart = targetTimeframe.alignTimestamp(srcTimestamp);
            }

            periodOpen = srcOpen[fromIndex];
            periodHigh = srcHigh[fromIndex];
            periodLow = srcLow[fromIndex];
            periodClose = srcClose[fromIndex];
            periodVolume = srcVolume[fromIndex];
            fromIndex++;
        }

        // Process remaining source bars
        long periodEnd = currentPeriodStart + targetTimeframe.millis;

        for (int i = fromIndex; i < srcSize; i++) {
            long timestamp = srcTimestamps[i];

            if (timestamp >= periodEnd) {
                // Emit completed period
                aggregated.append(currentPeriodStart, periodOpen, periodHigh, periodLow, periodClose, periodVolume);

                // Start new period
                currentPeriodStart = targetTimeframe.alignTimestamp(timestamp);
                periodEnd = currentPeriodStart + targetTimeframe.millis;
                periodOpen = srcOpen[i];
                periodHigh = srcHigh[i];
                periodLow = srcLow[i];
                periodClose = srcClose[i];
                periodVolume = srcVolume[i];
            } else {
                // Update current period
                if (srcHigh[i] > periodHigh) periodHigh = srcHigh[i];
                if (srcLow[i] < periodLow) periodLow = srcLow[i];
                periodClose = srcClose[i];
                periodVolume += srcVolume[i];
            }
        }

        // Emit/update final period
        if (aggregated.isEmpty() || aggregated.getXValue(aggregated.size() - 1) != currentPeriodStart) {
            aggregated.append(currentPeriodStart, periodOpen, periodHigh, periodLow, periodClose, periodVolume);
        } else {
            aggregated.updateLast(periodOpen, periodHigh, periodLow, periodClose, periodVolume);
        }
    }

    /**
     * Finds the index in the source series where the given period starts.
     */
    private static int findPeriodStart(OhlcData source, long periodStart, int searchFromIndex) {
        long[] timestamps = source.getTimestampsArray();

        // Search backwards to find the first bar in this period
        int idx = searchFromIndex;
        while (idx > 0 && timestamps[idx - 1] >= periodStart) {
            idx--;
        }
        return idx;
    }

    /**
     * Finds the first index in the source series at or after the given timestamp.
     */
    private static int findNextPeriodIndex(OhlcData source, long periodEnd, int searchFromIndex) {
        long[] timestamps = source.getTimestampsArray();
        int size = source.size();

        for (int i = searchFromIndex; i < size; i++) {
            if (timestamps[i] >= periodEnd) {
                return i;
            }
        }
        return size;
    }

    /**
     * Validates that aggregation from one timeframe to another is possible.
     *
     * @param source the source timeframe
     * @param target the target timeframe
     * @return true if aggregation is valid
     */
    public static boolean canAggregate(Timeframe source, Timeframe target) {
        return target.canAggregateFrom(source);
    }

    /**
     * Estimates the number of bars that will result from aggregation.
     *
     * @param sourceBars number of bars in the source series
     * @param sourceTimeframe the source timeframe
     * @param targetTimeframe the target timeframe
     * @return estimated number of bars in the aggregated series
     */
    public static int estimateResultSize(int sourceBars, Timeframe sourceTimeframe, Timeframe targetTimeframe) {
        if (sourceBars == 0) {
            return 0;
        }
        int ratio = targetTimeframe.getBarsPerPeriod(sourceTimeframe);
        if (ratio <= 0) {
            return sourceBars; // Can't aggregate, return original size
        }
        return (sourceBars + ratio - 1) / ratio;
    }
}
