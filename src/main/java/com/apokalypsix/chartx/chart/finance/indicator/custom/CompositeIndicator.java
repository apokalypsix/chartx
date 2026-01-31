package com.apokalypsix.chartx.chart.finance.indicator.custom;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.finance.indicator.result.MultiLineResult;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Combines multiple indicators into a single output.
 *
 * <p>CompositeIndicator allows creating complex indicators by combining
 * the outputs of simpler ones. Supports various combination operations
 * like averaging, differencing, and custom formulas.
 *
 * <p>Example usage:
 * <pre>
 * // Create a MACD-style indicator: EMA(12) - EMA(26)
 * CompositeIndicator composite = CompositeIndicator.builder("macd_line")
 *     .addIndicator("fast", new EMAIndicator(12))
 *     .addIndicator("slow", new EMAIndicator(26))
 *     .combine((fast, slow) -> fast - slow)
 *     .build();
 *
 * // Create average of two SMAs
 * CompositeIndicator avg = CompositeIndicator.builder("sma_avg")
 *     .addIndicator("sma20", new SMAIndicator(20))
 *     .addIndicator("sma50", new SMAIndicator(50))
 *     .average()
 *     .build();
 * </pre>
 */
public class CompositeIndicator implements Indicator<OhlcData, XyData> {

    /**
     * Builder for creating composite indicators.
     */
    public static class Builder {
        private final String id;
        private String name;
        private final List<String> indicatorNames = new ArrayList<>();
        private final List<Indicator<OhlcData, XyData>> indicators = new ArrayList<>();
        private CombineFunction combiner;

        Builder(String id) {
            this.id = id;
            this.name = id;
        }

        /**
         * Sets the display name.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds an indicator to the composite.
         *
         * @param name local name for referencing in the combiner
         * @param indicator the indicator to add
         */
        public Builder addIndicator(String name, Indicator<OhlcData, XyData> indicator) {
            indicatorNames.add(name);
            indicators.add(indicator);
            return this;
        }

        /**
         * Sets a custom combine function for two indicators.
         * Use when exactly 2 indicators are added.
         */
        public Builder combine(BiFunction<Float, Float, Float> function) {
            this.combiner = values -> {
                if (values.length < 2) return Float.NaN;
                Float a = values[0];
                Float b = values[1];
                if (a == null || b == null || Float.isNaN(a) || Float.isNaN(b)) {
                    return Float.NaN;
                }
                return function.apply(a, b);
            };
            return this;
        }

        /**
         * Combines indicators by computing their average.
         */
        public Builder average() {
            this.combiner = values -> {
                float sum = 0;
                int count = 0;
                for (Float v : values) {
                    if (v != null && !Float.isNaN(v)) {
                        sum += v;
                        count++;
                    }
                }
                return count > 0 ? sum / count : Float.NaN;
            };
            return this;
        }

        /**
         * Combines indicators by computing their sum.
         */
        public Builder sum() {
            this.combiner = values -> {
                float sum = 0;
                boolean hasValue = false;
                for (Float v : values) {
                    if (v != null && !Float.isNaN(v)) {
                        sum += v;
                        hasValue = true;
                    }
                }
                return hasValue ? sum : Float.NaN;
            };
            return this;
        }

        /**
         * Combines indicators by computing their difference (first - second - ...).
         */
        public Builder difference() {
            this.combiner = values -> {
                if (values.length == 0 || values[0] == null || Float.isNaN(values[0])) {
                    return Float.NaN;
                }
                float result = values[0];
                for (int i = 1; i < values.length; i++) {
                    if (values[i] == null || Float.isNaN(values[i])) {
                        return Float.NaN;
                    }
                    result -= values[i];
                }
                return result;
            };
            return this;
        }

        /**
         * Sets a custom combine function for any number of indicators.
         */
        public Builder combineAll(CombineFunction function) {
            this.combiner = function;
            return this;
        }

        /**
         * Builds the composite indicator.
         */
        public CompositeIndicator build() {
            if (indicators.isEmpty()) {
                throw new IllegalStateException("At least one indicator is required");
            }
            if (combiner == null) {
                throw new IllegalStateException("A combine function is required");
            }
            return new CompositeIndicator(id, name, new ArrayList<>(indicators), combiner);
        }
    }

    /**
     * Functional interface for combining indicator values.
     */
    @FunctionalInterface
    public interface CombineFunction {
        /**
         * Combines values from multiple indicators at a single point in time.
         *
         * @param values array of values from each indicator (may contain NaN)
         * @return the combined value
         */
        float combine(Float[] values);
    }

    private final String id;
    private final String name;
    private final List<Indicator<OhlcData, XyData>> indicators;
    private final CombineFunction combiner;

    private CompositeIndicator(String id, String name,
                               List<Indicator<OhlcData, XyData>> indicators,
                               CombineFunction combiner) {
        this.id = id;
        this.name = name;
        this.indicators = indicators;
        this.combiner = combiner;
    }

    /**
     * Creates a new builder for a composite indicator.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        int max = 1;
        for (Indicator<OhlcData, XyData> ind : indicators) {
            max = Math.max(max, ind.getMinimumBars());
        }
        return max;
    }

    @Override
    public XyData calculate(OhlcData source) {
        int size = source.size();
        XyData result = new XyData(id, name, size);

        if (size == 0) {
            return result;
        }

        // Calculate all sub-indicators
        List<XyData> outputs = new ArrayList<>();
        for (Indicator<OhlcData, XyData> ind : indicators) {
            outputs.add(ind.calculate(source));
        }

        // Combine values at each point
        long[] timestamps = source.getTimestampsArray();
        Float[] values = new Float[indicators.size()];

        for (int i = 0; i < size; i++) {
            // Collect values from each indicator
            for (int j = 0; j < outputs.size(); j++) {
                XyData output = outputs.get(j);
                values[j] = i < output.size() ? output.getValue(i) : Float.NaN;
            }

            float combined = combiner.combine(values);
            result.append(timestamps[i], combined);
        }

        return result;
    }

    @Override
    public void update(XyData result, OhlcData source, int fromIndex) {
        // Recalculate for simplicity (composite updates are complex)
        XyData recalculated = calculate(source);
        long[] timestamps = recalculated.getTimestampsArray();
        float[] values = recalculated.getValuesArray();

        for (int i = result.size(); i < recalculated.size(); i++) {
            result.append(timestamps[i], values[i]);
        }
    }
}
