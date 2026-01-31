package com.apokalypsix.chartx.chart.finance.indicator.base;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.data.Data;

/**
 * Base class for indicator implementations providing common functionality.
 *
 * <p>Provides a template for indicator development with sensible defaults
 * and helper methods for common patterns.
 *
 * @param <S> the source data type
 * @param <R> the result data type
 */
public abstract class AbstractIndicator<S extends Data<?>, R extends Data<?>>
        implements Indicator<S, R> {

    protected final String name;
    protected final int minimumBars;

    /**
     * Creates an indicator with specified name and minimum bars.
     *
     * @param name the indicator name (e.g., "SMA(20)")
     * @param minimumBars minimum bars needed before producing valid output
     */
    protected AbstractIndicator(String name, int minimumBars) {
        this.name = name;
        this.minimumBars = Math.max(1, minimumBars);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return minimumBars;
    }

    /**
     * Template method for incremental update.
     * Default implementation recalculates from scratch.
     * Override for better performance when incremental calculation is possible.
     */
    @Override
    public void update(R result, S source, int fromIndex) {
        // Default: recalculate entirely (subclasses should override for efficiency)
        R recalculated = calculate(source);
        copyNewValues(result, recalculated, result.size());
    }

    /**
     * Copies new values from source to destination starting at the given index.
     * Subclasses should override to provide type-specific copying.
     *
     * @param dest the destination data
     * @param src the source data
     * @param fromIndex the index to start copying from
     */
    protected abstract void copyNewValues(R dest, R src, int fromIndex);

    /**
     * Creates an empty result data structure.
     * Subclasses should override to create the appropriate result type.
     *
     * @param id the data series ID
     * @param name the data series name
     * @param capacity initial capacity
     * @return new empty result instance
     */
    protected abstract R createEmptyResult(String id, String name, int capacity);
}
