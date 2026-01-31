package com.apokalypsix.chartx.chart.finance.indicator.impl.trend;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.finance.indicator.result.MultiLineResult;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Pivot Points indicator.
 *
 * <p>Pivot Points are support and resistance levels calculated from the previous
 * period's high, low, and close. They are commonly used to identify potential
 * turning points in the market.
 *
 * <p>This implementation supports three calculation methods:
 * <ul>
 *   <li>Standard (Classic)</li>
 *   <li>Fibonacci</li>
 *   <li>Woodie</li>
 * </ul>
 *
 * <p>Output lines: Pivot (P), R1, R2, R3, S1, S2, S3
 */
public class PivotPoints implements Indicator<OhlcData, MultiLineResult> {

    /**
     * Pivot point calculation methods.
     */
    public enum PivotType {
        /** Standard/Classic: P = (H+L+C)/3 */
        STANDARD,
        /** Fibonacci: Uses Fibonacci retracement levels */
        FIBONACCI,
        /** Woodie: Gives more weight to close: P = (H+L+2C)/4 */
        WOODIE
    }

    private static final String[] LINE_NAMES = {"Pivot", "R1", "R2", "R3", "S1", "S2", "S3"};

    private final PivotType type;
    private final String name;

    /**
     * Creates Pivot Points with Standard calculation.
     */
    public PivotPoints() {
        this(PivotType.STANDARD);
    }

    /**
     * Creates Pivot Points with specified calculation method.
     *
     * @param type the pivot calculation method
     */
    public PivotPoints(PivotType type) {
        this.type = type;
        this.name = "Pivot Points (" + type.name() + ")";
    }

    /**
     * Returns the pivot type.
     */
    public PivotType getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return 2; // Need at least one previous bar
    }

    @Override
    public MultiLineResult calculate(OhlcData source) {
        int size = source.size();
        MultiLineResult result = new MultiLineResult(
                "pivot_" + type.name().toLowerCase(),
                name,
                LINE_NAMES
        );

        if (size < 2) {
            return result;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        long[] timestamps = source.getTimestampsArray();

        // First bar has no pivot (need previous bar data)
        result.append(timestamps[0], Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                Float.NaN, Float.NaN, Float.NaN);

        for (int i = 1; i < size; i++) {
            // Use previous bar's H, L, C
            float prevHigh = highs[i - 1];
            float prevLow = lows[i - 1];
            float prevClose = closes[i - 1];

            float[] levels = calculateLevels(prevHigh, prevLow, prevClose);
            result.append(timestamps[i], levels[0], levels[1], levels[2], levels[3],
                    levels[4], levels[5], levels[6]);
        }

        return result;
    }

    @Override
    public void update(MultiLineResult result, OhlcData source, int fromIndex) {
        int resultSize = result.size();
        int sourceSize = source.size();

        if (sourceSize <= resultSize) {
            return;
        }

        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        long[] timestamps = source.getTimestampsArray();

        for (int i = resultSize; i < sourceSize; i++) {
            if (i == 0) {
                result.append(timestamps[i], Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                        Float.NaN, Float.NaN, Float.NaN);
            } else {
                float prevHigh = highs[i - 1];
                float prevLow = lows[i - 1];
                float prevClose = closes[i - 1];

                float[] levels = calculateLevels(prevHigh, prevLow, prevClose);
                result.append(timestamps[i], levels[0], levels[1], levels[2], levels[3],
                        levels[4], levels[5], levels[6]);
            }
        }
    }

    /**
     * Calculates pivot levels: [Pivot, R1, R2, R3, S1, S2, S3]
     */
    private float[] calculateLevels(float high, float low, float close) {
        float pivot, r1, r2, r3, s1, s2, s3;
        float range = high - low;

        switch (type) {
            case FIBONACCI:
                pivot = (high + low + close) / 3;
                r1 = pivot + 0.382f * range;
                r2 = pivot + 0.618f * range;
                r3 = pivot + 1.0f * range;
                s1 = pivot - 0.382f * range;
                s2 = pivot - 0.618f * range;
                s3 = pivot - 1.0f * range;
                break;

            case WOODIE:
                pivot = (high + low + 2 * close) / 4;
                r1 = 2 * pivot - low;
                s1 = 2 * pivot - high;
                r2 = pivot + range;
                s2 = pivot - range;
                r3 = r1 + range;
                s3 = s1 - range;
                break;

            default: // STANDARD
                pivot = (high + low + close) / 3;
                r1 = 2 * pivot - low;
                s1 = 2 * pivot - high;
                r2 = pivot + range;
                s2 = pivot - range;
                r3 = high + 2 * (pivot - low);
                s3 = low - 2 * (high - pivot);
                break;
        }

        return new float[]{pivot, r1, r2, r3, s1, s2, s3};
    }
}
