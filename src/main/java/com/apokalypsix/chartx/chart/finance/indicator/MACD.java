package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.awt.Color;

/**
 * Moving Average Convergence Divergence (MACD) indicator.
 *
 * <p>MACD is a trend-following momentum indicator that shows the relationship
 * between two moving averages. It consists of:
 * <ul>
 *   <li>MACD Line = Fast EMA - Slow EMA</li>
 *   <li>Signal Line = EMA of MACD Line</li>
 *   <li>Histogram = MACD Line - Signal Line</li>
 * </ul>
 *
 * <p>Standard parameters: 12, 26, 9
 */
public class MACD {

    /** Default MACD line color (blue) */
    public static final Color MACD_LINE_COLOR = new Color(33, 150, 243);
    /** Default signal line color (orange) */
    public static final Color SIGNAL_LINE_COLOR = new Color(255, 152, 0);
    /** Default positive histogram color (green) */
    public static final Color POSITIVE_COLOR = new Color(38, 166, 91);
    /** Default negative histogram color (red) */
    public static final Color NEGATIVE_COLOR = new Color(214, 69, 65);

    /**
     * Result containing all MACD components.
     */
    public static class Result {
        public final XyData macdLine;
        public final XyData signalLine;
        public final HistogramData histogram;

        public Result(XyData macdLine, XyData signalLine, HistogramData histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
    }

    /**
     * Calculates MACD with standard parameters (12, 26, 9).
     */
    public static Result calculate(OhlcData source) {
        return calculate(source, 12, 26, 9);
    }

    /**
     * Calculates MACD from OHLC data.
     *
     * @param source the source OHLC data
     * @param fastPeriod the fast EMA period (typically 12)
     * @param slowPeriod the slow EMA period (typically 26)
     * @param signalPeriod the signal line period (typically 9)
     * @return a Result containing MACD line, signal line, and histogram
     */
    public static Result calculate(OhlcData source, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (fastPeriod < 1 || slowPeriod < 1 || signalPeriod < 1) {
            throw new IllegalArgumentException("Periods must be at least 1");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("Fast period must be less than slow period");
        }

        String suffix = String.format("(%d,%d,%d)", fastPeriod, slowPeriod, signalPeriod);

        // Create result data
        XyData macdLine = new XyData("macd" + suffix, "MACD" + suffix, source.size());
        XyData signalLine = new XyData("signal" + suffix, "Signal" + suffix, source.size());
        HistogramData histogram = new HistogramData("macd_hist" + suffix, "MACD Histogram", source.size());

        if (source.isEmpty()) {
            return new Result(macdLine, signalLine, histogram);
        }

        long[] timestamps = source.getTimestampsArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // Calculate fast and slow EMAs
        double fastMultiplier = 2.0 / (fastPeriod + 1);
        double slowMultiplier = 2.0 / (slowPeriod + 1);
        double signalMultiplier = 2.0 / (signalPeriod + 1);

        double fastEMA = 0;
        double slowEMA = 0;
        double signalEMA = 0;
        int validMacdCount = 0;

        for (int i = 0; i < size; i++) {
            float close = closes[i];

            // Calculate fast EMA
            if (i < fastPeriod - 1) {
                fastEMA += close;
            } else if (i == fastPeriod - 1) {
                fastEMA = (fastEMA + close) / fastPeriod;
            } else {
                fastEMA = (close - fastEMA) * fastMultiplier + fastEMA;
            }

            // Calculate slow EMA
            if (i < slowPeriod - 1) {
                slowEMA += close;
            } else if (i == slowPeriod - 1) {
                slowEMA = (slowEMA + close) / slowPeriod;
            } else {
                slowEMA = (close - slowEMA) * slowMultiplier + slowEMA;
            }

            // Calculate MACD
            if (i < slowPeriod - 1) {
                macdLine.append(timestamps[i], Float.NaN);
                signalLine.append(timestamps[i], Float.NaN);
                histogram.append(timestamps[i], 0);
            } else {
                float macd = (float) (fastEMA - slowEMA);
                macdLine.append(timestamps[i], macd);

                // Calculate signal line (EMA of MACD)
                if (validMacdCount < signalPeriod - 1) {
                    signalEMA += macd;
                    signalLine.append(timestamps[i], Float.NaN);
                    histogram.append(timestamps[i], 0);
                } else if (validMacdCount == signalPeriod - 1) {
                    signalEMA = (signalEMA + macd) / signalPeriod;
                    signalLine.append(timestamps[i], (float) signalEMA);
                    histogram.append(timestamps[i], (float) (macd - signalEMA));
                } else {
                    signalEMA = (macd - signalEMA) * signalMultiplier + signalEMA;
                    signalLine.append(timestamps[i], (float) signalEMA);
                    histogram.append(timestamps[i], (float) (macd - signalEMA));
                }

                validMacdCount++;
            }
        }

        return new Result(macdLine, signalLine, histogram);
    }
}
