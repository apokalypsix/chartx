package com.apokalypsix.chartx.chart.finance.indicator.impl.momentum;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOscillator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Williams %R indicator.
 *
 * <p>Williams %R is a momentum indicator that measures overbought and oversold
 * levels, similar to Stochastic oscillator but inverted. It shows where the
 * current close is relative to the highest high over the lookback period.
 *
 * <p>Formula:
 * %R = (Highest High - Close) / (Highest High - Lowest Low) × -100
 *
 * <p>Range: -100 to 0
 * <ul>
 *   <li>%R between -20 and 0: Overbought</li>
 *   <li>%R between -80 and -100: Oversold</li>
 * </ul>
 *
 * <p>Standard period: 14
 */
public class WilliamsRIndicator extends AbstractOscillator {

    private final int period;

    /**
     * Creates Williams %R with default period (14).
     */
    public WilliamsRIndicator() {
        this(14);
    }

    /**
     * Creates Williams %R with custom period.
     *
     * @param period the lookback period
     */
    public WilliamsRIndicator(int period) {
        // Bounded: -100 to 0, overbought at -20, oversold at -80, center at -50
        super("williams_r_" + period, "Williams %R(" + period + ")", period,
                -20, -80, -50);
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
    public float getSuggestedMinY() {
        return -100;
    }

    @Override
    public float getSuggestedMaxY() {
        return 0;
    }

    @Override
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                // Find highest high and lowest low in period
                float hh = Float.NEGATIVE_INFINITY;
                float ll = Float.POSITIVE_INFINITY;

                for (int j = i - period + 1; j <= i; j++) {
                    hh = Math.max(hh, highs[j]);
                    ll = Math.min(ll, lows[j]);
                }

                // Calculate Williams %R
                float range = hh - ll;
                if (range == 0) {
                    outValues[i] = -50; // Midpoint when range is zero
                } else {
                    outValues[i] = ((hh - closes[i]) / range) * -100;
                }
            }
        }
    }
}
