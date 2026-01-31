package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOhlcIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Weighted Moving Average (WMA) indicator.
 *
 * <p>WMA gives more weight to recent prices. Each price is multiplied by a weight,
 * with the most recent price having the highest weight.
 *
 * <p>Formula: WMA = Σ(Price × Weight) / Σ(Weights)
 * where Weight[i] = i (oldest = 1, newest = period)
 *
 * <p>Example for period = 5:
 * WMA = (P1×1 + P2×2 + P3×3 + P4×4 + P5×5) / (1+2+3+4+5)
 */
public class WMAIndicator extends AbstractOhlcIndicator {

    private final int period;
    private final float weightSum;

    /**
     * Creates a WMA indicator with the specified period.
     *
     * @param period the number of periods (bars) to average
     */
    public WMAIndicator(int period) {
        super("wma_" + period, "WMA(" + period + ")", period);
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        this.period = period;
        // Sum of weights: 1 + 2 + 3 + ... + period = period * (period + 1) / 2
        this.weightSum = period * (period + 1) / 2.0f;
    }

    /**
     * Returns the WMA period.
     */
    public int getPeriod() {
        return period;
    }

    @Override
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] closes = source.getCloseArray();
        int size = source.size();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                // Not enough data yet
                outValues[i] = Float.NaN;
            } else {
                // Calculate weighted sum
                float weightedSum = 0;
                int weight = 1;
                for (int j = i - period + 1; j <= i; j++) {
                    weightedSum += closes[j] * weight;
                    weight++;
                }
                outValues[i] = weightedSum / weightSum;
            }
        }
    }
}
