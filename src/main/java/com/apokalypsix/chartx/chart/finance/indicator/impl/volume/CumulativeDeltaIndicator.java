package com.apokalypsix.chartx.chart.finance.indicator.impl.volume;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

/**
 * Cumulative Volume Delta indicator.
 *
 * <p>Cumulative Delta is the running sum of volume delta. It shows the
 * cumulative balance between buying and selling pressure over time.
 *
 * <p>Estimation method (same as Volume Delta):
 * <ul>
 *   <li>Buy Volume = Volume × (Close - Low) / (High - Low)</li>
 *   <li>Sell Volume = Volume × (High - Close) / (High - Low)</li>
 *   <li>Delta = Buy Volume - Sell Volume</li>
 *   <li>Cumulative Delta = Running sum of Delta</li>
 * </ul>
 *
 * <p>Interpretation:
 * <ul>
 *   <li>Rising cumulative delta with rising price: Healthy uptrend</li>
 *   <li>Falling cumulative delta with falling price: Healthy downtrend</li>
 *   <li>Divergence between cumulative delta and price: Potential reversal</li>
 * </ul>
 */
public class CumulativeDeltaIndicator implements Indicator<OhlcData, XyData> {

    private final String name = "Cumulative Delta";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return 1;
    }

    @Override
    public XyData calculate(OhlcData source) {
        int size = source.size();
        XyData result = new XyData("cumulative_delta", name, size);

        if (size == 0) {
            return result;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        float[] volumes = source.getVolumeArray();
        long[] timestamps = source.getTimestampsArray();

        double cumulativeDelta = 0;

        for (int i = 0; i < size; i++) {
            float high = highs[i];
            float low = lows[i];
            float close = closes[i];
            float volume = volumes[i];

            float range = high - low;
            float delta;

            if (range == 0) {
                if (i > 0) {
                    delta = close >= closes[i - 1] ? volume : -volume;
                } else {
                    delta = 0;
                }
            } else {
                float buyRatio = (close - low) / range;
                float sellRatio = (high - close) / range;
                delta = volume * buyRatio - volume * sellRatio;
            }

            cumulativeDelta += delta;
            result.append(timestamps[i], (float) cumulativeDelta);
        }

        return result;
    }

    @Override
    public void update(XyData result, OhlcData source, int fromIndex) {
        int resultSize = result.size();
        int sourceSize = source.size();

        if (sourceSize <= resultSize) {
            return;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        float[] volumes = source.getVolumeArray();
        long[] timestamps = source.getTimestampsArray();

        // Get the last cumulative delta value
        double cumulativeDelta = resultSize > 0 ? result.getValue(resultSize - 1) : 0;

        for (int i = resultSize; i < sourceSize; i++) {
            float high = highs[i];
            float low = lows[i];
            float close = closes[i];
            float volume = volumes[i];

            float range = high - low;
            float delta;

            if (range == 0) {
                if (i > 0) {
                    delta = close >= closes[i - 1] ? volume : -volume;
                } else {
                    delta = 0;
                }
            } else {
                float buyRatio = (close - low) / range;
                float sellRatio = (high - close) / range;
                delta = volume * buyRatio - volume * sellRatio;
            }

            cumulativeDelta += delta;
            result.append(timestamps[i], (float) cumulativeDelta);
        }
    }
}
