package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;

/**
 * Average True Range (ATR) indicator.
 *
 * <p>ATR measures market volatility by decomposing the entire range of an
 * asset price for that period.
 *
 * <p>True Range is the greatest of:
 * <ul>
 *   <li>Current High - Current Low</li>
 *   <li>|Current High - Previous Close|</li>
 *   <li>|Current Low - Previous Close|</li>
 * </ul>
 *
 * <p>ATR is the moving average of the True Range.
 */
public class ATR {

    /** Default ATR line color */
    public static final Color DEFAULT_COLOR = new Color(0, 150, 136);

    /**
     * Calculates ATR from OHLC data.
     *
     * @param source the source OHLC data
     * @param period the ATR period (typically 14)
     * @return XyData containing ATR values
     */
    public static XyData calculate(OhlcData source, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }

        String id = "atr_" + period;
        String name = "ATR(" + period + ")";
        XyData result = new XyData(id, name, source.size());

        if (source.isEmpty()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // First point: TR = High - Low (no previous close)
        result.append(timestamps[0], Float.NaN);

        double atr = 0;

        for (int i = 1; i < size; i++) {
            float tr = calculateTrueRange(highs[i], lows[i], closes[i - 1]);

            if (i < period) {
                // Accumulating for initial ATR
                atr += tr;
                result.append(timestamps[i], Float.NaN);
            } else if (i == period) {
                // First ATR value is SMA of first 'period' true ranges
                atr = (atr + tr) / period;
                result.append(timestamps[i], (float) atr);
            } else {
                // Subsequent ATR values using Wilder's smoothing
                atr = (atr * (period - 1) + tr) / period;
                result.append(timestamps[i], (float) atr);
            }
        }

        return result;
    }

    /**
     * Calculates the True Range for a single bar.
     */
    public static float calculateTrueRange(float high, float low, float prevClose) {
        float hl = high - low;
        float hpc = Math.abs(high - prevClose);
        float lpc = Math.abs(low - prevClose);
        return Math.max(hl, Math.max(hpc, lpc));
    }

    /**
     * Updates an existing ATR data with new data from the source.
     *
     * @param atr the existing ATR data
     * @param source the source OHLC data
     * @param period the ATR period (must match original calculation)
     */
    public static void update(XyData atr, OhlcData source, int period) {
        if (source.size() < 2 || atr.isEmpty()) {
            return;
        }

        int atrSize = atr.size();
        int sourceSize = source.size();

        if (sourceSize <= atrSize) {
            return;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        long[] timestamps = source.getTimestampsArray();

        // Get last valid ATR value
        double lastATR = 0;
        for (int i = atrSize - 1; i >= 0; i--) {
            if (atr.hasValue(i)) {
                lastATR = atr.getValue(i);
                break;
            }
        }

        // Calculate new ATR values
        for (int i = atrSize; i < sourceSize; i++) {
            float tr = calculateTrueRange(highs[i], lows[i], closes[i - 1]);
            lastATR = (lastATR * (period - 1) + tr) / period;
            atr.append(timestamps[i], (float) lastATR);
        }
    }
}
