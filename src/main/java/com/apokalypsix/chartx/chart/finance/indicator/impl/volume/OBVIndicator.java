package com.apokalypsix.chartx.chart.finance.indicator.impl.volume;

import com.apokalypsix.chartx.chart.finance.indicator.base.AbstractOhlcIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * On-Balance Volume (OBV) indicator.
 *
 * <p>OBV is a cumulative indicator that relates volume to price change.
 * It was developed by Joseph Granville and is used to predict price movements.
 *
 * <p>Formula:
 * <ul>
 *   <li>If close > previous close: OBV = Previous OBV + Current Volume</li>
 *   <li>If close < previous close: OBV = Previous OBV - Current Volume</li>
 *   <li>If close = previous close: OBV = Previous OBV</li>
 * </ul>
 *
 * <p>Interpretation:
 * <ul>
 *   <li>Rising OBV indicates buying pressure</li>
 *   <li>Falling OBV indicates selling pressure</li>
 *   <li>Divergence between OBV and price can signal reversals</li>
 * </ul>
 */
public class OBVIndicator extends AbstractOhlcIndicator {

    /**
     * Creates an OBV indicator.
     */
    public OBVIndicator() {
        super("obv", "OBV", 1);
    }

    @Override
    protected void computeValues(OhlcData source, float[] outValues, long[] timestamps) {
        float[] closes = source.getCloseArray();
        float[] volumes = source.getVolumeArray();
        int size = source.size();

        if (size == 0) {
            return;
        }

        // First bar: OBV starts at 0 (or could start at first volume)
        double obv = 0;
        outValues[0] = 0;

        for (int i = 1; i < size; i++) {
            float close = closes[i];
            float prevClose = closes[i - 1];
            float volume = volumes[i];

            if (close > prevClose) {
                obv += volume;
            } else if (close < prevClose) {
                obv -= volume;
            }
            // If close == prevClose, OBV stays the same

            outValues[i] = (float) obv;
        }
    }
}
