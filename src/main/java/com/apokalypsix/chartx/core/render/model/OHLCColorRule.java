package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Functional interface for determining the color of OHLC bars based on custom criteria.
 *
 * <p>Color rules allow flexible coloring based on any data-driven logic, such as:
 * <ul>
 *   <li>Traditional: close vs open (bullish/bearish)</li>
 *   <li>Momentum: close vs previous close</li>
 *   <li>Breakout: close vs previous high/low</li>
 * </ul>
 *
 * <p>Implementations receive the full data and current index to enable lookback
 * comparisons to previous bars.
 *
 * @see ColoredCandleRule for built-in implementations
 */
@FunctionalInterface
public interface OHLCColorRule {

    /**
     * Determines the color for the bar at the given index.
     *
     * @param data the OHLC data containing the values
     * @param index the index of the bar to color
     * @param bullishColor the default bullish (positive) color
     * @param bearishColor the default bearish (negative) color
     * @return the color to use for rendering this bar
     */
    Color getColor(OhlcData data, int index, Color bullishColor, Color bearishColor);
}
