package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractBandIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Donchian Channels indicator.
 *
 * <p>Donchian Channels display the highest high and lowest low over a specified period.
 * They are used to identify breakout levels and trend direction.
 *
 * <p>Formula:
 * <ul>
 *   <li>Upper Band = Highest High over N periods</li>
 *   <li>Lower Band = Lowest Low over N periods</li>
 *   <li>Middle Band = (Upper + Lower) / 2</li>
 * </ul>
 *
 * <p>Standard parameter: 20 periods (popularized by the "Turtle Traders")
 */
public class DonchianChannels extends AbstractBandIndicator {

    private final int period;

    /**
     * Creates Donchian Channels with default period (20).
     */
    public DonchianChannels() {
        this(20);
    }

    /**
     * Creates Donchian Channels with custom period.
     *
     * @param period the lookback period for high/low
     */
    public DonchianChannels(int period) {
        super("donchian_" + period, "Donchian(" + period + ")", period);
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
    protected void computeBands(OhlcData source, float[] outUpper,
                                float[] outMiddle, float[] outLower, long[] timestamps) {
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        int size = source.size();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outUpper[i] = Float.NaN;
                outMiddle[i] = Float.NaN;
                outLower[i] = Float.NaN;
            } else {
                // Find highest high and lowest low in the period
                float highestHigh = Float.NEGATIVE_INFINITY;
                float lowestLow = Float.POSITIVE_INFINITY;

                for (int j = i - period + 1; j <= i; j++) {
                    if (highs[j] > highestHigh) {
                        highestHigh = highs[j];
                    }
                    if (lows[j] < lowestLow) {
                        lowestLow = lows[j];
                    }
                }

                outUpper[i] = highestHigh;
                outLower[i] = lowestLow;
                outMiddle[i] = (highestHigh + lowestLow) / 2;
            }
        }
    }
}
