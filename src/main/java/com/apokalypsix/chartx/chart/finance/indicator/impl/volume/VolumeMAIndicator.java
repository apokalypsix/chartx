package com.apokalypsix.chartx.chart.finance.indicator.impl.volume;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOhlcIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Volume Moving Average indicator.
 *
 * <p>Calculates a Simple Moving Average of volume, useful for identifying
 * above-average or below-average volume bars.
 *
 * <p>Uses:
 * <ul>
 *   <li>Volume above MA suggests strong interest/conviction</li>
 *   <li>Volume below MA suggests weak interest</li>
 *   <li>Volume spikes can indicate reversals or breakouts</li>
 * </ul>
 *
 * <p>Standard period: 20
 */
public class VolumeMAIndicator extends AbstractOhlcIndicator {

    private final int period;

    /**
     * Creates Volume MA with default period (20).
     */
    public VolumeMAIndicator() {
        this(20);
    }

    /**
     * Creates Volume MA with custom period.
     *
     * @param period the moving average period
     */
    public VolumeMAIndicator(int period) {
        super("volume_ma_" + period, "Volume MA(" + period + ")", period);
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
        float[] volumes = source.getVolumeArray();
        int size = source.size();

        // Use running sum for efficiency
        double runningSum = 0;

        for (int i = 0; i < size; i++) {
            runningSum += volumes[i];

            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                if (i >= period) {
                    runningSum -= volumes[i - period];
                }
                outValues[i] = (float) (runningSum / period);
            }
        }
    }
}
