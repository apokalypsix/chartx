package com.apokalypsix.chartx.chart.finance.indicator.base;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyyData;

/**
 * Base class for indicators that produce band output (upper, middle, lower).
 *
 * <p>Used for Bollinger Bands, Keltner Channels, Donchian Channels, and similar
 * indicators that display a center line with upper and lower boundaries.
 *
 * <p>Subclasses implement {@link #computeBands(OhlcData, float[], float[], float[], long[])}
 * to fill the output arrays with calculated band values.
 */
public abstract class AbstractBandIndicator extends AbstractIndicator<OhlcData, XyyData> {

    protected final String id;

    /**
     * Creates an OHLC to XyyData (band) indicator.
     *
     * @param id unique identifier for the output series
     * @param name display name for the indicator
     * @param minimumBars minimum bars before valid output
     */
    protected AbstractBandIndicator(String id, String name, int minimumBars) {
        super(name, minimumBars);
        this.id = id;
    }

    @Override
    public XyyData calculate(OhlcData source) {
        int size = source.size();
        XyyData result = createEmptyResult(id, name, size);

        if (size == 0) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] upper = new float[size];
        float[] middle = new float[size];
        float[] lower = new float[size];

        // Let subclass fill in the band values
        computeBands(source, upper, middle, lower, timestamps);

        // Append all values to result
        for (int i = 0; i < size; i++) {
            result.append(timestamps[i], upper[i], middle[i], lower[i]);
        }

        return result;
    }

    @Override
    public void update(XyyData result, OhlcData source, int fromIndex) {
        int resultSize = result.size();
        int sourceSize = source.size();

        if (sourceSize <= resultSize) {
            return;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] upper = new float[sourceSize];
        float[] middle = new float[sourceSize];
        float[] lower = new float[sourceSize];

        // Compute all band values
        computeBands(source, upper, middle, lower, timestamps);

        // Append only the new ones
        for (int i = resultSize; i < sourceSize; i++) {
            result.append(timestamps[i], upper[i], middle[i], lower[i]);
        }
    }

    /**
     * Computes band values and fills the output arrays.
     * Values before the indicator has enough data should be set to Float.NaN.
     *
     * @param source the source OHLC data
     * @param outUpper output array for upper band values
     * @param outMiddle output array for middle band values
     * @param outLower output array for lower band values
     * @param timestamps the timestamps array (for reference if needed)
     */
    protected abstract void computeBands(OhlcData source, float[] outUpper,
                                         float[] outMiddle, float[] outLower, long[] timestamps);

    @Override
    protected void copyNewValues(XyyData dest, XyyData src, int fromIndex) {
        long[] timestamps = src.getTimestampsArray();
        float[] upper = src.getUpperArray();
        float[] middle = src.getMiddleArray();
        float[] lower = src.getLowerArray();
        for (int i = fromIndex; i < src.size(); i++) {
            dest.append(timestamps[i], upper[i], middle[i], lower[i]);
        }
    }

    @Override
    protected XyyData createEmptyResult(String id, String name, int capacity) {
        return new XyyData(id, name, capacity);
    }
}
