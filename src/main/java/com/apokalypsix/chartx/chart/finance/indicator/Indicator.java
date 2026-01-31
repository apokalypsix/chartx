package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.Data;

/**
 * Interface for technical indicators.
 *
 * <p>Indicators transform source data into output data that can be
 * displayed on charts. This interface supports both full calculation and
 * incremental updates for streaming data.
 *
 * @param <S> the source data type
 * @param <R> the result data type
 */
public interface Indicator<S extends Data<?>, R extends Data<?>> {

    /**
     * Returns the name of this indicator.
     *
     * @return indicator name (e.g., "SMA(20)", "RSI(14)")
     */
    String getName();

    /**
     * Returns the minimum number of bars required before this indicator
     * can produce valid values.
     *
     * @return minimum bars needed for calculation
     */
    int getMinimumBars();

    /**
     * Calculates the indicator from scratch.
     *
     * @param source the source data
     * @return the calculated result data
     */
    R calculate(S source);

    /**
     * Updates an existing result with new data from the source.
     *
     * <p>This method is used for incremental updates when new bars are added
     * to the source data. It's more efficient than recalculating from scratch.
     *
     * @param result the existing result data to update
     * @param source the source data
     * @param fromIndex the index to start updating from
     */
    void update(R result, S source, int fromIndex);
}
