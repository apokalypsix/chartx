package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractBandIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Keltner Channels indicator.
 *
 * <p>Keltner Channels are volatility-based bands set above and below an EMA.
 * Unlike Bollinger Bands (which use standard deviation), Keltner Channels use ATR.
 *
 * <p>Formula:
 * <ul>
 *   <li>Middle Band = EMA(close, emaPeriod)</li>
 *   <li>Upper Band = EMA + multiplier × ATR(atrPeriod)</li>
 *   <li>Lower Band = EMA - multiplier × ATR(atrPeriod)</li>
 * </ul>
 *
 * <p>Standard parameters: EMA(20), ATR(10), multiplier = 2.0
 */
public class KeltnerChannels extends AbstractBandIndicator {

    private final int emaPeriod;
    private final int atrPeriod;
    private final float multiplier;

    /**
     * Creates Keltner Channels with default parameters (20, 10, 2.0).
     */
    public KeltnerChannels() {
        this(20, 10, 2.0f);
    }

    /**
     * Creates Keltner Channels with custom parameters.
     *
     * @param emaPeriod period for the EMA (middle line)
     * @param atrPeriod period for the ATR calculation
     * @param multiplier multiplier for the ATR bands
     */
    public KeltnerChannels(int emaPeriod, int atrPeriod, float multiplier) {
        super(String.format("keltner_%d_%d_%.1f", emaPeriod, atrPeriod, multiplier),
              String.format("Keltner(%d,%d,%.1f)", emaPeriod, atrPeriod, multiplier),
              Math.max(emaPeriod, atrPeriod));

        if (emaPeriod < 1 || atrPeriod < 1) {
            throw new IllegalArgumentException("Periods must be at least 1");
        }
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive");
        }

        this.emaPeriod = emaPeriod;
        this.atrPeriod = atrPeriod;
        this.multiplier = multiplier;
    }

    /**
     * Returns the EMA period.
     */
    public int getEmaPeriod() {
        return emaPeriod;
    }

    /**
     * Returns the ATR period.
     */
    public int getAtrPeriod() {
        return atrPeriod;
    }

    /**
     * Returns the multiplier.
     */
    public float getMultiplier() {
        return multiplier;
    }

    @Override
    protected void computeBands(OhlcData source, float[] outUpper,
                                float[] outMiddle, float[] outLower, long[] timestamps) {
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        // Calculate EMA
        float[] ema = calculateEMA(closes, size, emaPeriod);

        // Calculate ATR
        float[] atr = calculateATR(highs, lows, closes, size, atrPeriod);

        // Calculate bands
        int minBars = getMinimumBars();
        for (int i = 0; i < size; i++) {
            if (i < minBars - 1 || Float.isNaN(ema[i]) || Float.isNaN(atr[i])) {
                outUpper[i] = Float.NaN;
                outMiddle[i] = Float.NaN;
                outLower[i] = Float.NaN;
            } else {
                float offset = multiplier * atr[i];
                outMiddle[i] = ema[i];
                outUpper[i] = ema[i] + offset;
                outLower[i] = ema[i] - offset;
            }
        }
    }

    private float[] calculateEMA(float[] closes, int size, int period) {
        float[] ema = new float[size];
        double mult = 2.0 / (period + 1);
        double value = 0;

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                ema[i] = Float.NaN;
                value += closes[i];
            } else if (i == period - 1) {
                value = (value + closes[i]) / period;
                ema[i] = (float) value;
            } else {
                value = (closes[i] - value) * mult + value;
                ema[i] = (float) value;
            }
        }
        return ema;
    }

    private float[] calculateATR(float[] highs, float[] lows, float[] closes,
                                  int size, int period) {
        float[] atr = new float[size];
        if (size == 0) return atr;

        // First TR (just H-L for first bar)
        float prevClose = closes[0];
        double atrValue = 0;

        for (int i = 0; i < size; i++) {
            // Calculate True Range
            float tr;
            if (i == 0) {
                tr = highs[i] - lows[i];
            } else {
                float hl = highs[i] - lows[i];
                float hpc = Math.abs(highs[i] - prevClose);
                float lpc = Math.abs(lows[i] - prevClose);
                tr = Math.max(hl, Math.max(hpc, lpc));
                prevClose = closes[i];
            }

            // Calculate ATR using Wilder's smoothing
            if (i < period) {
                atrValue += tr;
                atr[i] = Float.NaN;
            } else if (i == period) {
                atrValue = (atrValue + tr) / period;
                atr[i] = (float) atrValue;
            } else {
                atrValue = (atrValue * (period - 1) + tr) / period;
                atr[i] = (float) atrValue;
            }
        }
        return atr;
    }
}
