package com.apokalypsix.chartx.core.data;

import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Transforms OHLC data to Heikin-Ashi values.
 *
 * <p>Heikin-Ashi candles use modified OHLC calculations to smooth price action:
 * <ul>
 *   <li>HA Close = (Open + High + Low + Close) / 4</li>
 *   <li>HA Open = (prev HA Open + prev HA Close) / 2</li>
 *   <li>HA High = max(High, HA Open, HA Close)</li>
 *   <li>HA Low = min(Low, HA Open, HA Close)</li>
 * </ul>
 *
 * <p>The resulting smoothed candles help identify trends more easily:
 * <ul>
 *   <li>Strong uptrend: No lower wick, large body</li>
 *   <li>Strong downtrend: No upper wick, large body</li>
 *   <li>Consolidation: Small body with wicks on both sides</li>
 * </ul>
 *
 * <p>This transform creates a new series rather than modifying in place,
 * allowing the original data to be preserved for indicator calculations.
 */
public class HeikinAshiTransform {

    /**
     * Transforms an OHLC series to Heikin-Ashi values.
     *
     * @param source the source OHLC series
     * @return a new series with Heikin-Ashi values
     */
    public static OhlcData transform(OhlcData source) {
        if (source == null || source.isEmpty()) {
            return new OhlcData(
                source != null ? source.getId() + "_HA" : "empty_HA",
                source != null ? source.getName() + " (HA)" : "Empty (HA)",
                0
            );
        }

        int size = source.size();
        String newId = source.getId() + "_HA";
        String newName = source.getName() + " (HA)";
        OhlcData result = new OhlcData(newId, newName, size);

        // Get raw arrays for performance
        long[] srcTimestamps = source.getTimestampsArray();
        float[] srcOpen = source.getOpenArray();
        float[] srcHigh = source.getHighArray();
        float[] srcLow = source.getLowArray();
        float[] srcClose = source.getCloseArray();
        float[] srcVolume = source.getVolumeArray();

        // First bar: use actual values for HA open
        float haClose = (srcOpen[0] + srcHigh[0] + srcLow[0] + srcClose[0]) / 4.0f;
        float haOpen = (srcOpen[0] + srcClose[0]) / 2.0f;  // First bar approximation
        float haHigh = Math.max(srcHigh[0], Math.max(haOpen, haClose));
        float haLow = Math.min(srcLow[0], Math.min(haOpen, haClose));

        result.append(srcTimestamps[0], haOpen, haHigh, haLow, haClose, srcVolume[0]);

        // Track previous HA values
        float prevHaOpen = haOpen;
        float prevHaClose = haClose;

        // Process remaining bars
        for (int i = 1; i < size; i++) {
            // HA Close = (O + H + L + C) / 4
            haClose = (srcOpen[i] + srcHigh[i] + srcLow[i] + srcClose[i]) / 4.0f;

            // HA Open = (prev HA Open + prev HA Close) / 2
            haOpen = (prevHaOpen + prevHaClose) / 2.0f;

            // HA High = max(High, HA Open, HA Close)
            haHigh = Math.max(srcHigh[i], Math.max(haOpen, haClose));

            // HA Low = min(Low, HA Open, HA Close)
            haLow = Math.min(srcLow[i], Math.min(haOpen, haClose));

            result.append(srcTimestamps[i], haOpen, haHigh, haLow, haClose, srcVolume[i]);

            // Update previous values
            prevHaOpen = haOpen;
            prevHaClose = haClose;
        }

        return result;
    }

    /**
     * Updates an existing Heikin-Ashi series with new data from the source.
     *
     * <p>This method is useful for real-time updates where the source series
     * has received new bars and the HA series needs to be updated efficiently.
     *
     * @param haSeries the existing Heikin-Ashi series
     * @param source the source series with new data
     * @param fromIndex the index in the source series from which to process new data
     */
    public static void updateTransformed(OhlcData haSeries, OhlcData source, int fromIndex) {
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

        // Get previous HA values
        float prevHaOpen;
        float prevHaClose;

        if (haSeries.isEmpty()) {
            // Start fresh with first bar
            float haClose = (srcOpen[fromIndex] + srcHigh[fromIndex] + srcLow[fromIndex] + srcClose[fromIndex]) / 4.0f;
            float haOpen = (srcOpen[fromIndex] + srcClose[fromIndex]) / 2.0f;
            float haHigh = Math.max(srcHigh[fromIndex], Math.max(haOpen, haClose));
            float haLow = Math.min(srcLow[fromIndex], Math.min(haOpen, haClose));

            haSeries.append(srcTimestamps[fromIndex], haOpen, haHigh, haLow, haClose, srcVolume[fromIndex]);

            prevHaOpen = haOpen;
            prevHaClose = haClose;
            fromIndex++;
        } else {
            // Get last HA bar's values
            int lastHaIdx = haSeries.size() - 1;
            prevHaOpen = haSeries.getOpen(lastHaIdx);
            prevHaClose = haSeries.getClose(lastHaIdx);

            // Check if we need to update the last bar (same timestamp)
            if (fromIndex < srcSize && srcTimestamps[fromIndex] == haSeries.getXValue(lastHaIdx)) {
                // Update the last HA bar
                float haClose = (srcOpen[fromIndex] + srcHigh[fromIndex] + srcLow[fromIndex] + srcClose[fromIndex]) / 4.0f;

                // For updating, we need the previous bar's HA values
                if (lastHaIdx > 0) {
                    prevHaOpen = haSeries.getOpen(lastHaIdx - 1);
                    prevHaClose = haSeries.getClose(lastHaIdx - 1);
                } else {
                    prevHaOpen = (srcOpen[fromIndex] + srcClose[fromIndex]) / 2.0f;
                    prevHaClose = haClose;
                }

                float haOpen = (prevHaOpen + prevHaClose) / 2.0f;
                float haHigh = Math.max(srcHigh[fromIndex], Math.max(haOpen, haClose));
                float haLow = Math.min(srcLow[fromIndex], Math.min(haOpen, haClose));

                haSeries.updateLast(haOpen, haHigh, haLow, haClose, srcVolume[fromIndex]);

                prevHaOpen = haOpen;
                prevHaClose = haClose;
                fromIndex++;
            }
        }

        // Process any remaining new bars
        for (int i = fromIndex; i < srcSize; i++) {
            float haClose = (srcOpen[i] + srcHigh[i] + srcLow[i] + srcClose[i]) / 4.0f;
            float haOpen = (prevHaOpen + prevHaClose) / 2.0f;
            float haHigh = Math.max(srcHigh[i], Math.max(haOpen, haClose));
            float haLow = Math.min(srcLow[i], Math.min(haOpen, haClose));

            haSeries.append(srcTimestamps[i], haOpen, haHigh, haLow, haClose, srcVolume[i]);

            prevHaOpen = haOpen;
            prevHaClose = haClose;
        }
    }
}
