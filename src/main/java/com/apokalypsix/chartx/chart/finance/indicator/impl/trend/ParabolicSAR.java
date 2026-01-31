package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOhlcIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Parabolic SAR (Stop and Reverse) indicator.
 *
 * <p>The Parabolic SAR is a trend-following indicator that provides potential
 * entry and exit points. It appears as dots above or below the price:
 * <ul>
 *   <li>Dots below price = uptrend (bullish)</li>
 *   <li>Dots above price = downtrend (bearish)</li>
 * </ul>
 *
 * <p>The indicator uses an Acceleration Factor (AF) that starts small and
 * increases each time a new extreme point is made, up to a maximum value.
 *
 * <p>Standard parameters: AF start = 0.02, AF step = 0.02, AF max = 0.20
 */
public class ParabolicSAR extends AbstractOhlcIndicator {

    private final float afStart;
    private final float afStep;
    private final float afMax;

    /**
     * Creates Parabolic SAR with default parameters (0.02, 0.02, 0.20).
     */
    public ParabolicSAR() {
        this(0.02f, 0.02f, 0.20f);
    }

    /**
     * Creates Parabolic SAR with custom parameters.
     *
     * @param afStart starting acceleration factor
     * @param afStep acceleration factor increment
     * @param afMax maximum acceleration factor
     */
    public ParabolicSAR(float afStart, float afStep, float afMax) {
        super(String.format("psar_%.2f_%.2f_%.2f", afStart, afStep, afMax),
              String.format("PSAR(%.2f,%.2f,%.2f)", afStart, afStep, afMax),
              2);

        if (afStart <= 0 || afStep <= 0 || afMax <= 0) {
            throw new IllegalArgumentException("AF values must be positive");
        }
        if (afStart > afMax) {
            throw new IllegalArgumentException("AF start must be <= AF max");
        }

        this.afStart = afStart;
        this.afStep = afStep;
        this.afMax = afMax;
    }

    /**
     * Returns the starting AF.
     */
    public float getAfStart() {
        return afStart;
    }

    /**
     * Returns the AF step.
     */
    public float getAfStep() {
        return afStep;
    }

    /**
     * Returns the maximum AF.
     */
    public float getAfMax() {
        return afMax;
    }

    @Override
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        int size = source.size();

        if (size < 2) {
            for (int i = 0; i < size; i++) {
                outValues[i] = Float.NaN;
            }
            return;
        }

        // Initialize - first bar has no SAR
        outValues[0] = Float.NaN;

        // Determine initial trend direction (look at first two bars)
        boolean isUptrend = highs[1] > highs[0] || lows[1] > lows[0];

        float sar;
        float ep; // Extreme Point
        float af = afStart;

        if (isUptrend) {
            sar = lows[0]; // Start SAR at the low
            ep = highs[0];  // Extreme point is the high
        } else {
            sar = highs[0]; // Start SAR at the high
            ep = lows[0];   // Extreme point is the low
        }

        for (int i = 1; i < size; i++) {
            float high = highs[i];
            float low = lows[i];

            // Calculate new SAR
            float newSar = sar + af * (ep - sar);

            if (isUptrend) {
                // In uptrend, SAR cannot be above prior two lows
                if (i >= 2) {
                    newSar = Math.min(newSar, Math.min(lows[i - 1], lows[i - 2]));
                } else {
                    newSar = Math.min(newSar, lows[i - 1]);
                }

                // Check for reversal
                if (low < newSar) {
                    // Reverse to downtrend
                    isUptrend = false;
                    sar = ep; // New SAR is the old extreme point
                    ep = low;  // New extreme point is current low
                    af = afStart;
                    outValues[i] = sar;
                } else {
                    sar = newSar;
                    outValues[i] = sar;

                    // Update extreme point if we made a new high
                    if (high > ep) {
                        ep = high;
                        af = Math.min(af + afStep, afMax);
                    }
                }
            } else {
                // In downtrend, SAR cannot be below prior two highs
                if (i >= 2) {
                    newSar = Math.max(newSar, Math.max(highs[i - 1], highs[i - 2]));
                } else {
                    newSar = Math.max(newSar, highs[i - 1]);
                }

                // Check for reversal
                if (high > newSar) {
                    // Reverse to uptrend
                    isUptrend = true;
                    sar = ep; // New SAR is the old extreme point
                    ep = high; // New extreme point is current high
                    af = afStart;
                    outValues[i] = sar;
                } else {
                    sar = newSar;
                    outValues[i] = sar;

                    // Update extreme point if we made a new low
                    if (low < ep) {
                        ep = low;
                        af = Math.min(af + afStep, afMax);
                    }
                }
            }
        }
    }
}
