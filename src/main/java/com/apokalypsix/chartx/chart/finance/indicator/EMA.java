package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;

/**
 * Exponential Moving Average (EMA) indicator.
 *
 * <p>Calculates EMA using the formula:
 * EMA = (Price * multiplier) + (Previous EMA * (1 - multiplier))
 * where multiplier = 2 / (period + 1)
 */
public class EMA {

    /** Default EMA line color */
    public static final Color DEFAULT_COLOR = new Color(65, 131, 196);

    /**
     * Calculates EMA from OHLC data using close prices.
     *
     * @param source the source OHLC data
     * @param period the EMA period (e.g., 9, 20, 50)
     * @return XyData containing the EMA values
     */
    public static XyData calculate(OhlcData source, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }

        String id = "ema_" + period;
        String name = "EMA(" + period + ")";
        XyData result = new XyData(id, name, source.size());

        if (source.isEmpty()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        double multiplier = 2.0 / (period + 1);
        double ema = 0;

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                // Not enough data yet - use NaN
                result.append(timestamps[i], Float.NaN);

                // Accumulate for initial SMA
                ema += closes[i];
            } else if (i == period - 1) {
                // First EMA value is the SMA of first 'period' values
                ema = (ema + closes[i]) / period;
                result.append(timestamps[i], (float) ema);
            } else {
                // Standard EMA calculation
                ema = (closes[i] - ema) * multiplier + ema;
                result.append(timestamps[i], (float) ema);
            }
        }

        return result;
    }

    /**
     * Updates an existing EMA data with new data from the source.
     * Call this when new bars are added to the source data.
     *
     * @param ema the existing EMA data
     * @param source the source OHLC data
     * @param period the EMA period (must match the original calculation)
     */
    public static void update(XyData ema, OhlcData source, int period) {
        if (source.isEmpty() || ema.isEmpty()) {
            return;
        }

        int emaSize = ema.size();
        int sourceSize = source.size();

        if (sourceSize <= emaSize) {
            // No new data
            return;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();
        double multiplier = 2.0 / (period + 1);

        // Get the last valid EMA value
        double lastEma = 0;
        for (int i = emaSize - 1; i >= 0; i--) {
            if (ema.hasValue(i)) {
                lastEma = ema.getValue(i);
                break;
            }
        }

        // Calculate EMA for new bars
        for (int i = emaSize; i < sourceSize; i++) {
            if (i < period - 1) {
                ema.append(timestamps[i], Float.NaN);
            } else {
                lastEma = (closes[i] - lastEma) * multiplier + lastEma;
                ema.append(timestamps[i], (float) lastEma);
            }
        }
    }
}
