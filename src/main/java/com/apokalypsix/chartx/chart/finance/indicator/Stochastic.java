package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;

/**
 * Stochastic Oscillator indicator.
 *
 * <p>The Stochastic Oscillator is a momentum indicator comparing a particular
 * closing price to a range of prices over a period of time. It consists of:
 * <ul>
 *   <li>%K = Fast line (current close relative to high/low range)</li>
 *   <li>%D = Slow line (SMA of %K)</li>
 * </ul>
 *
 * <p>Standard parameters: 14, 3, 3 (K period, D period, slowing)
 *
 * <p>Common interpretation:
 * <ul>
 *   <li>Above 80: Overbought</li>
 *   <li>Below 20: Oversold</li>
 * </ul>
 */
public class Stochastic {

    /** Default %K line color (blue) */
    public static final Color K_LINE_COLOR = new Color(33, 150, 243);
    /** Default %D line color (orange) */
    public static final Color D_LINE_COLOR = new Color(255, 152, 0);

    /**
     * Result containing both stochastic lines.
     */
    public static class Result {
        public final XyData kLine;
        public final XyData dLine;

        public Result(XyData kLine, XyData dLine) {
            this.kLine = kLine;
            this.dLine = dLine;
        }
    }

    /**
     * Calculates Stochastic with standard parameters (14, 3, 3).
     */
    public static Result calculate(OhlcData source) {
        return calculate(source, 14, 3, 3);
    }

    /**
     * Calculates Stochastic from OHLC data.
     *
     * @param source the source OHLC data
     * @param kPeriod the %K lookback period (typically 14)
     * @param dPeriod the %D smoothing period (typically 3)
     * @param slowing additional smoothing for %K (typically 3)
     * @return a Result containing %K and %D lines
     */
    public static Result calculate(OhlcData source, int kPeriod, int dPeriod, int slowing) {
        if (kPeriod < 1 || dPeriod < 1 || slowing < 1) {
            throw new IllegalArgumentException("Periods must be at least 1");
        }

        String suffix = String.format("(%d,%d,%d)", kPeriod, dPeriod, slowing);

        XyData kLine = new XyData("stoch_k" + suffix, "%K" + suffix, source.size());
        XyData dLine = new XyData("stoch_d" + suffix, "%D" + suffix, source.size());

        if (source.isEmpty()) {
            return new Result(kLine, dLine);
        }

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // We need enough data for the lookback period
        int requiredBars = kPeriod + slowing - 2;

        // Store raw %K values for slowing
        float[] rawK = new float[size];
        float[] slowedK = new float[size];

        // Calculate raw %K (fast stochastic)
        for (int i = 0; i < size; i++) {
            if (i < kPeriod - 1) {
                rawK[i] = Float.NaN;
            } else {
                // Find highest high and lowest low in period
                float highestHigh = Float.NEGATIVE_INFINITY;
                float lowestLow = Float.POSITIVE_INFINITY;
                for (int j = i - kPeriod + 1; j <= i; j++) {
                    if (highs[j] > highestHigh) highestHigh = highs[j];
                    if (lows[j] < lowestLow) lowestLow = lows[j];
                }

                // Calculate %K
                float range = highestHigh - lowestLow;
                if (range > 0) {
                    rawK[i] = 100f * (closes[i] - lowestLow) / range;
                } else {
                    rawK[i] = 50f; // Neutral if no range
                }
            }
        }

        // Apply slowing to %K (SMA of raw %K)
        for (int i = 0; i < size; i++) {
            if (i < requiredBars) {
                slowedK[i] = Float.NaN;
            } else {
                float sum = 0;
                boolean valid = true;
                for (int j = i - slowing + 1; j <= i; j++) {
                    if (Float.isNaN(rawK[j])) {
                        valid = false;
                        break;
                    }
                    sum += rawK[j];
                }
                slowedK[i] = valid ? sum / slowing : Float.NaN;
            }
        }

        // Calculate %D (SMA of slowed %K) and build result series
        int dRequiredBars = requiredBars + dPeriod - 1;

        for (int i = 0; i < size; i++) {
            kLine.append(timestamps[i], slowedK[i]);

            if (i < dRequiredBars) {
                dLine.append(timestamps[i], Float.NaN);
            } else {
                float sum = 0;
                boolean valid = true;
                for (int j = i - dPeriod + 1; j <= i; j++) {
                    if (Float.isNaN(slowedK[j])) {
                        valid = false;
                        break;
                    }
                    sum += slowedK[j];
                }
                dLine.append(timestamps[i], valid ? sum / dPeriod : Float.NaN);
            }
        }

        return new Result(kLine, dLine);
    }
}
