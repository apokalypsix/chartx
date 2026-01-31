package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;

/**
 * Relative Strength Index (RSI) indicator.
 *
 * <p>RSI measures the magnitude of recent price changes to evaluate
 * overbought or oversold conditions. Values range from 0 to 100.
 *
 * <p>Formula:
 * RSI = 100 - (100 / (1 + RS))
 * RS = Average Gain / Average Loss
 *
 * <p>Common interpretation:
 * <ul>
 *   <li>RSI > 70: Overbought</li>
 *   <li>RSI < 30: Oversold</li>
 * </ul>
 */
public class RSI {

    /** Default RSI line color */
    public static final Color DEFAULT_COLOR = new Color(156, 39, 176);

    /**
     * Calculates RSI from OHLC data using close prices.
     *
     * @param source the source OHLC data
     * @param period the RSI period (typically 14)
     * @return XyData containing RSI values (0-100)
     */
    public static XyData calculate(OhlcData source, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }

        String id = "rsi_" + period;
        String name = "RSI(" + period + ")";
        XyData result = new XyData(id, name, source.size());

        if (source.size() < 2) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // First point has no RSI
        result.append(timestamps[0], Float.NaN);

        // Track average gain and loss
        double avgGain = 0;
        double avgLoss = 0;

        for (int i = 1; i < size; i++) {
            double change = closes[i] - closes[i - 1];
            double gain = Math.max(0, change);
            double loss = Math.max(0, -change);

            if (i < period) {
                // Accumulating initial values
                avgGain += gain;
                avgLoss += loss;
                result.append(timestamps[i], Float.NaN);
            } else if (i == period) {
                // First RSI value
                avgGain = (avgGain + gain) / period;
                avgLoss = (avgLoss + loss) / period;
                float rsi = calculateRSI(avgGain, avgLoss);
                result.append(timestamps[i], rsi);
            } else {
                // Subsequent RSI values using Wilder's smoothing
                avgGain = (avgGain * (period - 1) + gain) / period;
                avgLoss = (avgLoss * (period - 1) + loss) / period;
                float rsi = calculateRSI(avgGain, avgLoss);
                result.append(timestamps[i], rsi);
            }
        }

        return result;
    }

    private static float calculateRSI(double avgGain, double avgLoss) {
        if (avgLoss == 0) {
            return 100f;
        }
        double rs = avgGain / avgLoss;
        return (float) (100 - (100 / (1 + rs)));
    }

    /**
     * Updates an existing RSI data with new data from the source.
     *
     * @param rsi the existing RSI data
     * @param source the source OHLC data
     * @param period the RSI period (must match original calculation)
     */
    public static void update(XyData rsi, OhlcData source, int period) {
        if (source.size() < 2 || rsi.isEmpty()) {
            return;
        }

        int rsiSize = rsi.size();
        int sourceSize = source.size();

        if (sourceSize <= rsiSize) {
            return;
        }

        // Need to recalculate average gain/loss from existing data
        float[] closes = source.getCloseArray();
        long[] timestamps = source.getTimestampsArray();

        // Find last valid RSI and reconstruct averages
        double avgGain = 0;
        double avgLoss = 0;

        // Calculate initial averages
        for (int i = 1; i <= period && i < sourceSize; i++) {
            double change = closes[i] - closes[i - 1];
            avgGain += Math.max(0, change);
            avgLoss += Math.max(0, -change);
        }
        avgGain /= period;
        avgLoss /= period;

        // Apply Wilder's smoothing up to rsiSize
        for (int i = period + 1; i < rsiSize && i < sourceSize; i++) {
            double change = closes[i] - closes[i - 1];
            double gain = Math.max(0, change);
            double loss = Math.max(0, -change);
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        // Now calculate new RSI values
        for (int i = rsiSize; i < sourceSize; i++) {
            double change = closes[i] - closes[i - 1];
            double gain = Math.max(0, change);
            double loss = Math.max(0, -change);
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            float rsiValue = calculateRSI(avgGain, avgLoss);
            rsi.append(timestamps[i], rsiValue);
        }
    }
}
