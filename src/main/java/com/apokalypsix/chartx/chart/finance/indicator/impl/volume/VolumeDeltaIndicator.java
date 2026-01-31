package com.apokalypsix.chartx.chart.finance.indicator.impl.volume;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Volume Delta indicator.
 *
 * <p>Volume Delta estimates the difference between buying and selling volume
 * for each bar. This is an approximation since actual trade-by-trade data
 * is not available in OHLC data.
 *
 * <p>Estimation method:
 * <ul>
 *   <li>Buy Volume = Volume × (Close - Low) / (High - Low)</li>
 *   <li>Sell Volume = Volume × (High - Close) / (High - Low)</li>
 *   <li>Delta = Buy Volume - Sell Volume</li>
 * </ul>
 *
 * <p>This approximation works well for most cases but may be less accurate
 * for bars with unusual price action.
 *
 * <p>Interpretation:
 * <ul>
 *   <li>Positive delta: More buying pressure</li>
 *   <li>Negative delta: More selling pressure</li>
 *   <li>Large delta on small price move: Absorption</li>
 * </ul>
 */
public class VolumeDeltaIndicator implements Indicator<OhlcData, HistogramData> {

    private final String name = "Volume Delta";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return 1;
    }

    @Override
    public HistogramData calculate(OhlcData source) {
        int size = source.size();
        HistogramData result = new HistogramData("volume_delta", name, size);

        if (size == 0) {
            return result;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        float[] volumes = source.getVolumeArray();
        long[] timestamps = source.getTimestampsArray();

        for (int i = 0; i < size; i++) {
            float high = highs[i];
            float low = lows[i];
            float close = closes[i];
            float volume = volumes[i];

            float range = high - low;
            float delta;

            if (range == 0) {
                // No price movement - assign based on comparison with previous close
                if (i > 0) {
                    delta = close >= closes[i - 1] ? volume : -volume;
                } else {
                    delta = 0;
                }
            } else {
                // Calculate buy/sell ratio based on close position within range
                float buyRatio = (close - low) / range;
                float sellRatio = (high - close) / range;

                float buyVolume = volume * buyRatio;
                float sellVolume = volume * sellRatio;
                delta = buyVolume - sellVolume;
            }

            result.append(timestamps[i], delta);
        }

        return result;
    }

    @Override
    public void update(HistogramData result, OhlcData source, int fromIndex) {
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

            result.append(timestamps[i], delta);
        }
    }
}
