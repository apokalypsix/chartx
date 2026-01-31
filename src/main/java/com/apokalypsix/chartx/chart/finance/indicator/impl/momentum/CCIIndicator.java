package com.apokalypsix.chartx.chart.finance.indicator.impl.momentum;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOscillator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Commodity Channel Index (CCI) indicator.
 *
 * <p>CCI measures the current price level relative to an average price level
 * over a given period. It is typically used to identify cyclical trends.
 *
 * <p>Formula:
 * CCI = (Typical Price - SMA of TP) / (0.015 × Mean Deviation)
 * where Typical Price = (High + Low + Close) / 3
 *
 * <p>Interpretation:
 * <ul>
 *   <li>CCI > +100: Overbought (potential reversal down)</li>
 *   <li>CCI < -100: Oversold (potential reversal up)</li>
 *   <li>Zero line crossovers signal momentum shifts</li>
 * </ul>
 *
 * <p>Standard period: 20
 */
public class CCIIndicator extends AbstractOscillator {

    private final int period;
    private static final float CONSTANT = 0.015f;

    /**
     * Creates CCI with default period (20).
     */
    public CCIIndicator() {
        this(20);
    }

    /**
     * Creates CCI with custom period.
     *
     * @param period the lookback period
     */
    public CCIIndicator(int period) {
        // CCI is unbounded (can go beyond ±100), center at 0
        super("cci_" + period, "CCI(" + period + ")", period, 0);
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        this.period = period;
    }

    /**
     * Returns the period.
     */
    public int getPeriod() {
        return period;
    }

    @Override
    public float getOverboughtLevel() {
        return 100;
    }

    @Override
    public float getOversoldLevel() {
        return -100;
    }

    @Override
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // Calculate typical price
        float[] tp = new float[size];
        for (int i = 0; i < size; i++) {
            tp[i] = (highs[i] + lows[i] + closes[i]) / 3;
        }

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                // Calculate SMA of typical price
                float tpSum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    tpSum += tp[j];
                }
                float smaTP = tpSum / period;

                // Calculate Mean Deviation
                float mdSum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    mdSum += Math.abs(tp[j] - smaTP);
                }
                float meanDev = mdSum / period;

                // Calculate CCI
                if (meanDev == 0) {
                    outValues[i] = 0;
                } else {
                    outValues[i] = (tp[i] - smaTP) / (CONSTANT * meanDev);
                }
            }
        }
    }
}
