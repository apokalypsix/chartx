package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyyData;

import java.awt.Color;

/**
 * Bollinger Bands indicator.
 *
 * <p>Bollinger Bands consist of:
 * <ul>
 *   <li>Middle Band = SMA of close price</li>
 *   <li>Upper Band = Middle + (k * Standard Deviation)</li>
 *   <li>Lower Band = Middle - (k * Standard Deviation)</li>
 * </ul>
 *
 * <p>Standard parameters: 20-period SMA with 2 standard deviations
 */
public class BollingerBands {

    /** Default upper/lower band color */
    public static final Color BAND_COLOR = new Color(100, 149, 237);
    /** Default middle band color */
    public static final Color MIDDLE_COLOR = new Color(65, 131, 196);
    /** Default fill color */
    public static final Color FILL_COLOR = new Color(100, 149, 237, 30);

    /**
     * Calculates Bollinger Bands with default parameters (20, 2.0).
     */
    public static XyyData calculate(OhlcData source) {
        return calculate(source, 20, 2.0f);
    }

    /**
     * Calculates Bollinger Bands from OHLC data.
     *
     * @param source the source OHLC data
     * @param period the SMA period (typically 20)
     * @param stdDevMultiplier the standard deviation multiplier (typically 2.0)
     * @return XyyData with upper, middle, and lower bands
     */
    public static XyyData calculate(OhlcData source, int period, float stdDevMultiplier) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }

        String id = String.format("bb_%d_%.1f", period, stdDevMultiplier);
        String name = String.format("BB(%d,%.1f)", period, stdDevMultiplier);
        XyyData result = new XyyData(id, name, source.size());

        if (source.isEmpty()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                result.append(timestamps[i], Float.NaN, Float.NaN, Float.NaN);
            } else {
                // Calculate SMA
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += closes[j];
                }
                double sma = sum / period;

                // Calculate standard deviation
                double sumSqDiff = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    double diff = closes[j] - sma;
                    sumSqDiff += diff * diff;
                }
                double stdDev = Math.sqrt(sumSqDiff / period);

                // Calculate bands
                float middle = (float) sma;
                float upper = (float) (sma + stdDevMultiplier * stdDev);
                float lower = (float) (sma - stdDevMultiplier * stdDev);

                result.append(timestamps[i], upper, middle, lower);
            }
        }

        return result;
    }

    /**
     * Updates an existing Bollinger Bands data with new data from the source.
     *
     * @param bands the existing band data
     * @param source the source OHLC data
     * @param period the SMA period
     * @param stdDevMultiplier the standard deviation multiplier
     */
    public static void update(XyyData bands, OhlcData source, int period, float stdDevMultiplier) {
        if (source.isEmpty() || bands.isEmpty()) {
            return;
        }

        int bandSize = bands.size();
        int sourceSize = source.size();

        if (sourceSize <= bandSize) {
            return;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();

        for (int i = bandSize; i < sourceSize; i++) {
            if (i < period - 1) {
                bands.append(timestamps[i], Float.NaN, Float.NaN, Float.NaN);
            } else {
                // Calculate SMA
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += closes[j];
                }
                double sma = sum / period;

                // Calculate standard deviation
                double sumSqDiff = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    double diff = closes[j] - sma;
                    sumSqDiff += diff * diff;
                }
                double stdDev = Math.sqrt(sumSqDiff / period);

                float middle = (float) sma;
                float upper = (float) (sma + stdDevMultiplier * stdDev);
                float lower = (float) (sma - stdDevMultiplier * stdDev);

                bands.append(timestamps[i], upper, middle, lower);
            }
        }
    }
}
