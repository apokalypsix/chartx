package com.apokalypsix.chartx.chart.finance.indicator.result;

import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.XyData;

import java.util.List;

/**
 * Interface for indicator results that may contain multiple output lines.
 *
 * <p>While simple indicators produce a single output (e.g., SMA produces XyData),
 * complex indicators may produce multiple related outputs:
 * <ul>
 *   <li>MACD: MACD line, Signal line, Histogram</li>
 *   <li>Stochastic: %K line, %D line</li>
 *   <li>Ichimoku: 5 different lines</li>
 *   <li>ADX/DMI: ADX, +DI, -DI</li>
 * </ul>
 *
 * <p>This interface provides a uniform way to access all outputs from any indicator.
 */
public interface IndicatorResult extends Data<float[]> {

    /**
     * Returns the number of output lines in this result.
     */
    int getLineCount();

    /**
     * Returns the names of all output lines.
     */
    List<String> getLineNames();

    /**
     * Returns a specific output line by name.
     *
     * @param name the line name
     * @return the XyData for that line, or null if not found
     */
    XyData getLine(String name);

    /**
     * Returns a specific output line by index.
     *
     * @param index the line index (0-based)
     * @return the XyData for that line
     * @throws IndexOutOfBoundsException if index is out of range
     */
    XyData getLine(int index);

    /**
     * Returns the primary/main output line.
     * For single-line results, this is the only line.
     * For multi-line results, this is typically the most important line.
     */
    XyData getPrimaryLine();

    /**
     * Returns all output lines.
     */
    List<XyData> getAllLines();
}
