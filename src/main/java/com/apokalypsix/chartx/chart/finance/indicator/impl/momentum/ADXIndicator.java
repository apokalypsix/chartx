package com.apokalypsix.chartx.chart.finance.indicator.impl.momentum;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.finance.indicator.result.MultiLineResult;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Average Directional Index (ADX) with Directional Movement Indicators (+DI/-DI).
 *
 * <p>ADX measures trend strength regardless of direction. +DI and -DI indicate
 * trend direction.
 *
 * <p>Components:
 * <ul>
 *   <li><b>+DI</b> (Plus Directional Indicator): Measures upward movement</li>
 *   <li><b>-DI</b> (Minus Directional Indicator): Measures downward movement</li>
 *   <li><b>ADX</b>: Smoothed average of the difference between +DI and -DI</li>
 * </ul>
 *
 * <p>Interpretation:
 * <ul>
 *   <li>ADX > 25: Strong trend</li>
 *   <li>ADX < 20: Weak trend or ranging market</li>
 *   <li>+DI > -DI: Uptrend</li>
 *   <li>-DI > +DI: Downtrend</li>
 * </ul>
 *
 * <p>Standard period: 14
 */
public class ADXIndicator implements Indicator<OhlcData, MultiLineResult> {

    private static final String[] LINE_NAMES = {"ADX", "+DI", "-DI"};

    private final int period;
    private final String name;

    /**
     * Creates ADX with default period (14).
     */
    public ADXIndicator() {
        this(14);
    }

    /**
     * Creates ADX with custom period.
     *
     * @param period the lookback period
     */
    public ADXIndicator(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        this.period = period;
        this.name = "ADX(" + period + ")";
    }

    /**
     * Returns the period.
     */
    public int getPeriod() {
        return period;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return 2 * period; // Need enough data for smoothing
    }

    @Override
    public MultiLineResult calculate(OhlcData source) {
        int size = source.size();
        MultiLineResult result = new MultiLineResult(
                "adx_" + period, name, LINE_NAMES
        );

        if (size < 2) {
            return result;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        long[] timestamps = source.getTimestampsArray();

        // Arrays for intermediate calculations
        float[] tr = new float[size];     // True Range
        float[] plusDM = new float[size]; // Plus Directional Movement
        float[] minusDM = new float[size]; // Minus Directional Movement

        // First bar - no previous data
        tr[0] = highs[0] - lows[0];
        plusDM[0] = 0;
        minusDM[0] = 0;

        // Calculate TR, +DM, -DM for each bar
        for (int i = 1; i < size; i++) {
            float high = highs[i];
            float low = lows[i];
            float prevClose = closes[i - 1];
            float prevHigh = highs[i - 1];
            float prevLow = lows[i - 1];

            // True Range
            float hl = high - low;
            float hpc = Math.abs(high - prevClose);
            float lpc = Math.abs(low - prevClose);
            tr[i] = Math.max(hl, Math.max(hpc, lpc));

            // Directional Movement
            float upMove = high - prevHigh;
            float downMove = prevLow - low;

            if (upMove > downMove && upMove > 0) {
                plusDM[i] = upMove;
            } else {
                plusDM[i] = 0;
            }

            if (downMove > upMove && downMove > 0) {
                minusDM[i] = downMove;
            } else {
                minusDM[i] = 0;
            }
        }

        // Calculate smoothed values using Wilder's smoothing
        float[] smoothTR = new float[size];
        float[] smoothPlusDM = new float[size];
        float[] smoothMinusDM = new float[size];
        float[] dx = new float[size];

        double sumTR = 0, sumPlusDM = 0, sumMinusDM = 0;

        for (int i = 0; i < size; i++) {
            if (i < period) {
                sumTR += tr[i];
                sumPlusDM += plusDM[i];
                sumMinusDM += minusDM[i];
                smoothTR[i] = Float.NaN;
                smoothPlusDM[i] = Float.NaN;
                smoothMinusDM[i] = Float.NaN;
                dx[i] = Float.NaN;
            } else if (i == period) {
                smoothTR[i] = (float) (sumTR + tr[i]);
                smoothPlusDM[i] = (float) (sumPlusDM + plusDM[i]);
                smoothMinusDM[i] = (float) (sumMinusDM + minusDM[i]);

                // Calculate DX
                float plusDI = smoothTR[i] == 0 ? 0 : (smoothPlusDM[i] / smoothTR[i]) * 100;
                float minusDI = smoothTR[i] == 0 ? 0 : (smoothMinusDM[i] / smoothTR[i]) * 100;
                float diSum = plusDI + minusDI;
                dx[i] = diSum == 0 ? 0 : (Math.abs(plusDI - minusDI) / diSum) * 100;
            } else {
                // Wilder's smoothing: New = Prev - (Prev/period) + Current
                smoothTR[i] = smoothTR[i - 1] - (smoothTR[i - 1] / period) + tr[i];
                smoothPlusDM[i] = smoothPlusDM[i - 1] - (smoothPlusDM[i - 1] / period) + plusDM[i];
                smoothMinusDM[i] = smoothMinusDM[i - 1] - (smoothMinusDM[i - 1] / period) + minusDM[i];

                float plusDI = smoothTR[i] == 0 ? 0 : (smoothPlusDM[i] / smoothTR[i]) * 100;
                float minusDI = smoothTR[i] == 0 ? 0 : (smoothMinusDM[i] / smoothTR[i]) * 100;
                float diSum = plusDI + minusDI;
                dx[i] = diSum == 0 ? 0 : (Math.abs(plusDI - minusDI) / diSum) * 100;
            }
        }

        // Calculate ADX (smoothed DX)
        float[] adx = new float[size];
        double dxSum = 0;

        for (int i = 0; i < size; i++) {
            if (Float.isNaN(dx[i])) {
                adx[i] = Float.NaN;
            } else if (i < 2 * period) {
                dxSum += dx[i];
                adx[i] = Float.NaN;
            } else if (i == 2 * period) {
                adx[i] = (float) (dxSum / period);
            } else {
                // Wilder's smoothing for ADX
                adx[i] = ((adx[i - 1] * (period - 1)) + dx[i]) / period;
            }
        }

        // Build result
        for (int i = 0; i < size; i++) {
            float plusDI = Float.NaN;
            float minusDI = Float.NaN;

            if (!Float.isNaN(smoothTR[i]) && smoothTR[i] != 0) {
                plusDI = (smoothPlusDM[i] / smoothTR[i]) * 100;
                minusDI = (smoothMinusDM[i] / smoothTR[i]) * 100;
            }

            result.append(timestamps[i], adx[i], plusDI, minusDI);
        }

        return result;
    }

    @Override
    public void update(MultiLineResult result, OhlcData source, int fromIndex) {
        // Recalculate for simplicity
        MultiLineResult recalculated = calculate(source);
        int resultSize = result.size();
        int newSize = recalculated.size();

        for (int i = resultSize; i < newSize; i++) {
            result.append(recalculated.getXValue(i),
                    recalculated.getLine("ADX").getValue(i),
                    recalculated.getLine("+DI").getValue(i),
                    recalculated.getLine("-DI").getValue(i));
        }
    }
}
