package com.apokalypsix.chartx.chart.finance.indicator.impl.momentum;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOscillator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Momentum indicator.
 *
 * <p>Momentum measures the rate of change in prices, showing the speed of
 * price movement. Positive values indicate upward momentum; negative values
 * indicate downward momentum.
 *
 * <p>Formula: Momentum = Close - Close[n periods ago]
 *
 * <p>This is an unbounded oscillator centered at zero. Rising momentum suggests
 * strengthening trend; falling momentum suggests weakening trend.
 *
 * <p>Standard period: 10
 */
public class MomentumIndicator extends AbstractOscillator {

    private final int period;

    /**
     * Creates Momentum with default period (10).
     */
    public MomentumIndicator() {
        this(10);
    }

    /**
     * Creates Momentum with custom period.
     *
     * @param period the lookback period
     */
    public MomentumIndicator(int period) {
        // Unbounded oscillator centered at 0
        super("momentum_" + period, "Momentum(" + period + ")", period + 1, 0);
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
                outValues[i] = closes[i] - closes[i - period];
            }
        }
    }
}
