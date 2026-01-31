package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOhlcIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Hull Moving Average (HMA) indicator.
 *
 * <p>The Hull Moving Average reduces lag while improving smoothness.
 * Developed by Alan Hull, it uses weighted moving averages of different periods.
 *
 * <p>Formula:
 * <ol>
 *   <li>Calculate WMA(n/2) of the price</li>
 *   <li>Calculate WMA(n) of the price</li>
 *   <li>Calculate raw: 2 × WMA(n/2) - WMA(n)</li>
 *   <li>Calculate HMA: WMA(√n) of the raw values</li>
 * </ol>
 *
 * <p>This results in a moving average that is both responsive and smooth.
 */
public class HMAIndicator extends AbstractOhlcIndicator {

    private final int period;
    private final int halfPeriod;
    private final int sqrtPeriod;

    /**
     * Creates an HMA indicator with the specified period.
     *
     * @param period the number of periods (bars)
     */
    public HMAIndicator(int period) {
        super("hma_" + period, "HMA(" + period + ")",
                // Minimum bars: period + sqrtPeriod - 1
                period + (int) Math.sqrt(period) - 1);
        if (period < 4) {
            throw new IllegalArgumentException("Period must be at least 4");
        }
        this.period = period;
        this.halfPeriod = period / 2;
        this.sqrtPeriod = (int) Math.sqrt(period);
    }

    /**
     * Returns the HMA period.
     */
    public int getPeriod() {
        return period;
    }

    @Override
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] closes = source.getCloseArray();
        int size = source.size();

        // Pre-calculate weight sums
        float weightSumHalf = halfPeriod * (halfPeriod + 1) / 2.0f;
        float weightSumFull = period * (period + 1) / 2.0f;
        float weightSumSqrt = sqrtPeriod * (sqrtPeriod + 1) / 2.0f;

        // Step 1 & 2: Calculate WMA(n/2) and WMA(n) for each bar
        float[] wmaHalf = new float[size];
        float[] wmaFull = new float[size];

        for (int i = 0; i < size; i++) {
            // WMA(n/2)
            if (i < halfPeriod - 1) {
                wmaHalf[i] = Float.NaN;
            } else {
                float sum = 0;
                int weight = 1;
                for (int j = i - halfPeriod + 1; j <= i; j++) {
                    sum += closes[j] * weight;
                    weight++;
                }
                wmaHalf[i] = sum / weightSumHalf;
            }

            // WMA(n)
            if (i < period - 1) {
                wmaFull[i] = Float.NaN;
            } else {
                float sum = 0;
                int weight = 1;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += closes[j] * weight;
                    weight++;
                }
                wmaFull[i] = sum / weightSumFull;
            }
        }

        // Step 3: Calculate raw HMA: 2 * WMA(n/2) - WMA(n)
        float[] rawHma = new float[size];
        for (int i = 0; i < size; i++) {
            if (Float.isNaN(wmaHalf[i]) || Float.isNaN(wmaFull[i])) {
                rawHma[i] = Float.NaN;
            } else {
                rawHma[i] = 2 * wmaHalf[i] - wmaFull[i];
            }
        }

        // Step 4: Calculate final HMA: WMA(sqrt(n)) of raw values
        int minBars = getMinimumBars();
        for (int i = 0; i < size; i++) {
            if (i < minBars - 1) {
                outValues[i] = Float.NaN;
            } else {
                // Check if we have enough valid raw values
                boolean hasEnough = true;
                for (int j = i - sqrtPeriod + 1; j <= i; j++) {
                    if (Float.isNaN(rawHma[j])) {
                        hasEnough = false;
                        break;
                    }
                }

                if (!hasEnough) {
                    outValues[i] = Float.NaN;
                } else {
                    float sum = 0;
                    int weight = 1;
                    for (int j = i - sqrtPeriod + 1; j <= i; j++) {
                        sum += rawHma[j] * weight;
                        weight++;
                    }
                    outValues[i] = sum / weightSumSqrt;
                }
            }
        }
    }
}
