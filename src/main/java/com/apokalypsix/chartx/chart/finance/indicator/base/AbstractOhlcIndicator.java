package com.apokalypsix.chartx.chart.finance.indicator.base;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

/**
 * Base class for indicators that take OHLC data and produce single-line (XyData) output.
 *
 * <p>This covers most moving averages and simple oscillators like RSI.
 * Subclasses implement {@link #computeValues(OhlcData, float[], long[])} to fill
 * the output arrays with calculated values.
 */
public abstract class AbstractOhlcIndicator extends AbstractIndicator<OhlcData, XyData> {

    protected final String id;

    /**
     * Creates an OHLC to XyData indicator.
     *
     * @param id unique identifier for the output series
     * @param name display name for the indicator
     * @param minimumBars minimum bars before valid output
     */
    protected AbstractOhlcIndicator(String id, String name, int minimumBars) {
        super(name, minimumBars);
        this.id = id;
    }

    @Override
    public XyData calculate(OhlcData source) {
        int size = source.size();
        XyData result = createEmptyResult(id, name, size);

        if (size == 0) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] values = new float[size];

        // Let subclass fill in the values
        computeValues(source, values, timestamps);

        // Append all values to result
        for (int i = 0; i < size; i++) {
            result.append(timestamps[i], values[i]);
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

        // Recalculate only new values
        long[] timestamps = source.getTimestampsArray();
        float[] allValues = new float[sourceSize];

        // Compute all values (some indicators need full history for context)
        computeValues(source, allValues, timestamps);

        // Append only the new ones
        for (int i = resultSize; i < sourceSize; i++) {
            result.append(timestamps[i], allValues[i]);
        }
    }

    /**
     * Computes indicator values and fills the output array.
     * Values before the indicator has enough data should be set to Float.NaN.
     *
     * @param source the source OHLC data
     * @param outValues output array to fill with calculated values
     * @param timestamps the timestamps array (for reference if needed)
     */
    protected abstract void computeValues(OhlcData source, float[] outValues, long[] timestamps);

    @Override
    protected void copyNewValues(XyData dest, XyData src, int fromIndex) {
        long[] timestamps = src.getTimestampsArray();
        float[] values = src.getValuesArray();
        for (int i = fromIndex; i < src.size(); i++) {
            dest.append(timestamps[i], values[i]);
        }
    }

    @Override
    protected XyData createEmptyResult(String id, String name, int capacity) {
        return new XyData(id, name, capacity);
    }
}
