package com.apokalypsix.chartx.chart.finance.indicator.impl.momentum;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOscillator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Rate of Change (ROC) indicator.
 *
 * <p>ROC measures the percentage change in price over a specified period.
 * It oscillates around zero, with positive values indicating price increase
 * and negative values indicating price decrease.
 *
 * <p>Formula: ROC = ((Close - Close[n]) / Close[n]) × 100
 *
 * <p>Uses:
 * <ul>
 *   <li>Identify overbought/oversold conditions</li>
 *   <li>Spot divergences with price</li>
 *   <li>Confirm trend direction and strength</li>
 * </ul>
 *
 * <p>Standard period: 12
 */
public class ROCIndicator extends AbstractOscillator {

    private final int period;

    /**
     * Creates ROC with default period (12).
     */
    public ROCIndicator() {
        this(12);
    }

    /**
     * Creates ROC with custom period.
     *
     * @param period the lookback period
     */
    public ROCIndicator(int period) {
        // Unbounded oscillator centered at 0
        super("roc_" + period, "ROC(" + period + ")", period + 1, 0);
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
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] closes = source.getCloseArray();
        int size = source.size();

        for (int i = 0; i < size; i++) {
            if (i < period) {
                outValues[i] = Float.NaN;
            } else {
                float previousClose = closes[i - period];
                if (previousClose == 0) {
                    outValues[i] = 0;
                } else {
                    outValues[i] = ((closes[i] - previousClose) / previousClose) * 100;
                }
            }
        }
    }
}
