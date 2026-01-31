package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.finance.indicator.result.IchimokuResult;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Ichimoku Cloud (Ichimoku Kinko Hyo) indicator.
 *
 * <p>The Ichimoku Cloud is a comprehensive indicator that defines support/resistance,
 * identifies trend direction, gauges momentum, and provides trading signals.
 *
 * <p>Components:
 * <ul>
 *   <li><b>Tenkan-sen</b> (Conversion Line): (9-period high + 9-period low) / 2</li>
 *   <li><b>Kijun-sen</b> (Base Line): (26-period high + 26-period low) / 2</li>
 *   <li><b>Senkou Span A</b>: (Tenkan + Kijun) / 2, plotted 26 periods ahead</li>
 *   <li><b>Senkou Span B</b>: (52-period high + 52-period low) / 2, plotted 26 periods ahead</li>
 *   <li><b>Chikou Span</b>: Close price, plotted 26 periods behind</li>
 * </ul>
 *
 * <p>Standard parameters: Tenkan=9, Kijun=26, Senkou=52, Displacement=26
 */
public class IchimokuCloud implements Indicator<OhlcData, IchimokuResult> {

    private final int tenkanPeriod;
    private final int kijunPeriod;
    private final int senkouPeriod;
    private final int displacement;
    private final String name;

    /**
     * Creates Ichimoku Cloud with default parameters (9, 26, 52, 26).
     */
    public IchimokuCloud() {
        this(9, 26, 52, 26);
    }

    /**
     * Creates Ichimoku Cloud with custom parameters.
     *
     * @param tenkanPeriod period for Tenkan-sen (conversion line)
     * @param kijunPeriod period for Kijun-sen (base line)
     * @param senkouPeriod period for Senkou Span B
     * @param displacement periods to shift Senkou spans forward and Chikou back
     */
    public IchimokuCloud(int tenkanPeriod, int kijunPeriod, int senkouPeriod, int displacement) {
        if (tenkanPeriod < 1 || kijunPeriod < 1 || senkouPeriod < 1 || displacement < 1) {
            throw new IllegalArgumentException("All periods must be at least 1");
        }
        this.tenkanPeriod = tenkanPeriod;
        this.kijunPeriod = kijunPeriod;
        this.senkouPeriod = senkouPeriod;
        this.displacement = displacement;
        this.name = String.format("Ichimoku(%d,%d,%d,%d)",
                tenkanPeriod, kijunPeriod, senkouPeriod, displacement);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return Math.max(senkouPeriod, kijunPeriod);
    }

    @Override
    public IchimokuResult calculate(OhlcData source) {
        int size = source.size();
        IchimokuResult result = new IchimokuResult(
                String.format("ichimoku_%d_%d_%d_%d", tenkanPeriod, kijunPeriod, senkouPeriod, displacement)
        );

        if (size == 0) {
            return result;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        long[] timestamps = source.getTimestampsArray();

        // Pre-calculate Tenkan and Kijun for each bar
        float[] tenkan = new float[size];
        float[] kijun = new float[size];

        for (int i = 0; i < size; i++) {
            // Tenkan-sen: (High + Low) / 2 for tenkanPeriod
            if (i < tenkanPeriod - 1) {
                tenkan[i] = Float.NaN;
            } else {
                float hh = Float.NEGATIVE_INFINITY;
                float ll = Float.POSITIVE_INFINITY;
                for (int j = i - tenkanPeriod + 1; j <= i; j++) {
                    hh = Math.max(hh, highs[j]);
                    ll = Math.min(ll, lows[j]);
                }
                tenkan[i] = (hh + ll) / 2;
            }

            // Kijun-sen: (High + Low) / 2 for kijunPeriod
            if (i < kijunPeriod - 1) {
                kijun[i] = Float.NaN;
            } else {
                float hh = Float.NEGATIVE_INFINITY;
                float ll = Float.POSITIVE_INFINITY;
                for (int j = i - kijunPeriod + 1; j <= i; j++) {
                    hh = Math.max(hh, highs[j]);
                    ll = Math.min(ll, lows[j]);
                }
                kijun[i] = (hh + ll) / 2;
            }
        }

        // Calculate Senkou Span B values (needs to be pre-calculated for displacement)
        float[] senkouB = new float[size];
        for (int i = 0; i < size; i++) {
            if (i < senkouPeriod - 1) {
                senkouB[i] = Float.NaN;
            } else {
                float hh = Float.NEGATIVE_INFINITY;
                float ll = Float.POSITIVE_INFINITY;
                for (int j = i - senkouPeriod + 1; j <= i; j++) {
                    hh = Math.max(hh, highs[j]);
                    ll = Math.min(ll, lows[j]);
                }
                senkouB[i] = (hh + ll) / 2;
            }
        }

        // Build result with all 5 lines
        for (int i = 0; i < size; i++) {
            float tenkanVal = tenkan[i];
            float kijunVal = kijun[i];

            // Senkou Span A at this point was calculated 'displacement' bars ago
            // (plotted 'displacement' periods ahead)
            float senkouAVal = Float.NaN;
            int senkouASource = i - displacement;
            if (senkouASource >= 0 && senkouASource < size) {
                if (!Float.isNaN(tenkan[senkouASource]) && !Float.isNaN(kijun[senkouASource])) {
                    senkouAVal = (tenkan[senkouASource] + kijun[senkouASource]) / 2;
                }
            }

            // Senkou Span B at this point was calculated 'displacement' bars ago
            float senkouBVal = Float.NaN;
            if (senkouASource >= 0 && senkouASource < size) {
                senkouBVal = senkouB[senkouASource];
            }

            // Chikou Span: close price from 'displacement' periods in the future
            // (plotted 'displacement' periods behind)
            float chikouVal = Float.NaN;
            int chikouSource = i + displacement;
            if (chikouSource < size) {
                chikouVal = closes[chikouSource];
            }

            result.appendIchimoku(timestamps[i], tenkanVal, kijunVal,
                    senkouAVal, senkouBVal, chikouVal);
        }

        return result;
    }

    @Override
    public void update(IchimokuResult result, OhlcData source, int fromIndex) {
        // For simplicity, recalculate (Ichimoku displacement makes incremental complex)
        IchimokuResult recalculated = calculate(source);
        int resultSize = result.size();
        int newSize = recalculated.size();

        for (int i = resultSize; i < newSize; i++) {
            result.appendIchimoku(
                    recalculated.getXValue(i),
                    recalculated.getTenkanSen().getValue(i),
                    recalculated.getKijunSen().getValue(i),
                    recalculated.getSenkouSpanA().getValue(i),
                    recalculated.getSenkouSpanB().getValue(i),
                    recalculated.getChikouSpan().getValue(i)
            );
        }
    }

    // Getters
    public int getTenkanPeriod() { return tenkanPeriod; }
    public int getKijunPeriod() { return kijunPeriod; }
    public int getSenkouPeriod() { return senkouPeriod; }
    public int getDisplacement() { return displacement; }
}
