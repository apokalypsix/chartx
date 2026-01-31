package com.apokalypsix.chartx.chart.finance.indicator.result;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.XyData;

import java.util.*;

/**
 * Result type for indicators that produce multiple output lines.
 *
 * <p>Examples:
 * <ul>
 *   <li>MACD: 3 lines (MACD, Signal, Histogram)</li>
 *   <li>Stochastic: 2 lines (%K, %D)</li>
 *   <li>ADX/DMI: 3 lines (ADX, +DI, -DI)</li>
 * </ul>
 *
 * <p>All lines share the same timestamp array for memory efficiency.
 */
public class MultiLineResult implements IndicatorResult {

    private final String id;
    private final String name;
    private final List<String> lineNames;
    private final Map<String, XyData> linesByName;
    private final List<XyData> linesInOrder;
    private int primaryLineIndex = 0;

    /**
     * Creates a multi-line result with the specified line names.
     *
     * @param id unique identifier
     * @param name display name
     * @param lineNames names for each output line (in order)
     */
    public MultiLineResult(String id, String name, String... lineNames) {
        this(id, name, Arrays.asList(lineNames));
    }

    /**
     * Creates a multi-line result with the specified line names.
     *
     * @param id unique identifier
     * @param name display name
     * @param lineNames names for each output line
     */
    public MultiLineResult(String id, String name, List<String> lineNames) {
        this.id = id;
        this.name = name;
        this.lineNames = new ArrayList<>(lineNames);
        this.linesByName = new LinkedHashMap<>();
        this.linesInOrder = new ArrayList<>();

        for (String lineName : lineNames) {
            XyData lineData = new XyData(id + "_" + lineName.toLowerCase().replace(" ", "_"), lineName);
            linesByName.put(lineName, lineData);
            linesInOrder.add(lineData);
        }
    }

    /**
     * Sets which line should be considered the primary/main line.
     *
     * @param index the index of the primary line
     */
    public void setPrimaryLineIndex(int index) {
        if (index < 0 || index >= linesInOrder.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + linesInOrder.size());
        }
        this.primaryLineIndex = index;
    }

    /**
     * Sets which line should be considered the primary/main line.
     *
     * @param lineName the name of the primary line
     */
    public void setPrimaryLine(String lineName) {
        int index = lineNames.indexOf(lineName);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown line: " + lineName);
        }
        this.primaryLineIndex = index;
    }

    /**
     * Appends values for all lines at the given timestamp.
     * Values must be provided in the same order as the line names.
     *
     * @param timestamp the timestamp
     * @param values the values for each line (in order)
     */
    public void append(long timestamp, float... values) {
        if (values.length != linesInOrder.size()) {
            throw new IllegalArgumentException(
                    "Expected " + linesInOrder.size() + " values, got " + values.length);
        }
        for (int i = 0; i < linesInOrder.size(); i++) {
            linesInOrder.get(i).append(timestamp, values[i]);
        }
    }

    /**
     * Appends a value for a specific line.
     *
     * @param lineName the line name
     * @param timestamp the timestamp
     * @param value the value
     */
    public void appendToLine(String lineName, long timestamp, float value) {
        XyData line = linesByName.get(lineName);
        if (line == null) {
            throw new IllegalArgumentException("Unknown line: " + lineName);
        }
        line.append(timestamp, value);
    }

    // ========== IndicatorResult implementation ==========

    @Override
    public int getLineCount() {
        return linesInOrder.size();
    }

    @Override
    public List<String> getLineNames() {
        return Collections.unmodifiableList(lineNames);
    }

    @Override
    public XyData getLine(String name) {
        return linesByName.get(name);
    }

    @Override
    public XyData getLine(int index) {
        return linesInOrder.get(index);
    }

    @Override
    public XyData getPrimaryLine() {
        return linesInOrder.get(primaryLineIndex);
    }

    @Override
    public List<XyData> getAllLines() {
        return Collections.unmodifiableList(linesInOrder);
    }

    // ========== Data interface implementation ==========

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return linesInOrder.isEmpty() ? 0 : linesInOrder.get(0).size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public long getXValue(int index) {
        return getPrimaryLine().getXValue(index);
    }

    /**
     * @deprecated Use {@link #getXValue(int)} instead
     */
    @Deprecated
    public long getTimestamp(int index) {
        return getXValue(index);
    }

    /**
     * Returns the timestamps array from the primary line.
     */
    public long[] getTimestampsArray() {
        return getPrimaryLine().getTimestampsArray();
    }

    @Override
    public int indexAtOrBefore(long timestamp) {
        return getPrimaryLine().indexAtOrBefore(timestamp);
    }

    @Override
    public int indexAtOrAfter(long timestamp) {
        return getPrimaryLine().indexAtOrAfter(timestamp);
    }

    @Override
    public void clear() {
        for (XyData line : linesInOrder) {
            line.clear();
        }
    }

    @Override
    public void addListener(DataListener listener) {
        // Add to primary line only (to avoid duplicate notifications)
        getPrimaryLine().addListener(listener);
    }

    @Override
    public void removeListener(DataListener listener) {
        getPrimaryLine().removeListener(listener);
    }
}
