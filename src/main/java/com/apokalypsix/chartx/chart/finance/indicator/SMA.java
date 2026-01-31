package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;

/**
 * Simple Moving Average (SMA) indicator.
 *
 * <p>Calculates the arithmetic mean of prices over a specified period.
 * SMA = (Sum of prices over N periods) / N
 */
public class SMA {

    /** Default SMA line color */
    public static final Color DEFAULT_COLOR = new Color(255, 152, 0);

    /**
     * Calculates SMA from OHLC data using close prices.
     *
     * @param source the source OHLC data
     * @param period the SMA period (e.g., 20, 50, 200)
     * @return XyData containing the SMA values
     */
    public static XyData calculate(OhlcData source, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }

        String id = "sma_" + period;
        String name = "SMA(" + period + ")";
        XyData result = new XyData(id, name, source.size());

        if (source.isEmpty()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // Use running sum for efficiency
        double runningSum = 0;

        for (int i = 0; i < size; i++) {
            runningSum += closes[i];

            if (i < period - 1) {
                // Not enough data yet
                result.append(timestamps[i], Float.NaN);
            } else {
                if (i >= period) {
                    // Subtract the value leaving the window
                    runningSum -= closes[i - period];
                }
                float sma = (float) (runningSum / period);
                result.append(timestamps[i], sma);
            }
        }

        return result;
    }

    /**
     * Updates an existing SMA data with new data from the source.
     *
     * @param sma the existing SMA data
     * @param source the source OHLC data
     * @param period the SMA period (must match original calculation)
     */
    public static void update(XyData sma, OhlcData source, int period) {
        if (source.isEmpty() || sma.isEmpty()) {
            return;
        }

        int smaSize = sma.size();
        int sourceSize = source.size();

        if (sourceSize <= smaSize) {
            return;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();

        for (int i = smaSize; i < sourceSize; i++) {
            if (i < period - 1) {
                sma.append(timestamps[i], Float.NaN);
            } else {
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += closes[j];
                }
                sma.append(timestamps[i], (float) (sum / period));
            }
        }
    }
}
