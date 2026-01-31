package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Volume Weighted Average Price (VWAP) indicator.
 *
 * <p>VWAP is the ratio of the value traded to the total volume traded over a
 * particular time horizon. It represents the average price a security has
 * traded at throughout the day, based on both volume and price.
 *
 * <p>VWAP = Cumulative(Typical Price * Volume) / Cumulative(Volume)
 * where Typical Price = (High + Low + Close) / 3
 *
 * <p>VWAP resets at the start of each trading session.
 */
public class VWAP {

    /** Default VWAP line color */
    public static final Color DEFAULT_COLOR = new Color(156, 39, 176);

    /**
     * Calculates VWAP from OHLC data.
     * Uses daily reset (resets at midnight by default).
     *
     * @param source the source OHLC data (must include volume)
     * @return XyData containing VWAP values
     */
    public static XyData calculate(OhlcData source) {
        return calculate(source, TimeZone.getDefault());
    }

    /**
     * Calculates VWAP with timezone-aware session reset.
     *
     * @param source the source OHLC data
     * @param timezone the timezone for session detection
     * @return XyData containing VWAP values
     */
    public static XyData calculate(OhlcData source, TimeZone timezone) {
        XyData result = new XyData("vwap", "VWAP", source.size());

        if (source.isEmpty()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        float[] volumes = source.getVolumeArray();
        int size = source.size();

        Calendar cal = Calendar.getInstance(timezone);
        int lastDay = -1;
        double cumulativePV = 0;  // Cumulative (Price * Volume)
        double cumulativeV = 0;   // Cumulative Volume

        for (int i = 0; i < size; i++) {
            // Check if this is a new trading session (new day)
            cal.setTimeInMillis(timestamps[i]);
            int currentDay = cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR) * 1000;

            if (currentDay != lastDay) {
                // Reset for new session
                cumulativePV = 0;
                cumulativeV = 0;
                lastDay = currentDay;
            }

            // Calculate typical price
            float typicalPrice = (highs[i] + lows[i] + closes[i]) / 3f;

            // Accumulate
            cumulativePV += typicalPrice * volumes[i];
            cumulativeV += volumes[i];

            // Calculate VWAP
            float vwap;
            if (cumulativeV > 0) {
                vwap = (float) (cumulativePV / cumulativeV);
            } else {
                vwap = typicalPrice;
            }

            result.append(timestamps[i], vwap);
        }

        return result;
    }

    /**
     * Calculates anchored VWAP from a specific start index.
     *
     * <p>Anchored VWAP calculates VWAP from a user-specified starting point
     * rather than resetting each session.
     *
     * @param source the source OHLC data
     * @param startIndex the index to start calculation from
     * @return XyData containing anchored VWAP values
     */
    public static XyData calculateAnchored(OhlcData source, int startIndex) {
        XyData result = new XyData("avwap", "Anchored VWAP", source.size());

        if (source.isEmpty() || startIndex < 0 || startIndex >= source.size()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        float[] volumes = source.getVolumeArray();
        int size = source.size();

        // Fill NaN before start index
        for (int i = 0; i < startIndex; i++) {
            result.append(timestamps[i], Float.NaN);
        }

        double cumulativePV = 0;
        double cumulativeV = 0;

        for (int i = startIndex; i < size; i++) {
            float typicalPrice = (highs[i] + lows[i] + closes[i]) / 3f;
            cumulativePV += typicalPrice * volumes[i];
            cumulativeV += volumes[i];

            float vwap = cumulativeV > 0 ? (float) (cumulativePV / cumulativeV) : typicalPrice;
            result.append(timestamps[i], vwap);
        }

        return result;
    }
}
