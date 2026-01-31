package com.apokalypsix.chartx.chart.finance.indicator.result;

import com.apokalypsix.chartx.chart.data.XyData;

/**
 * Specialized result for Ichimoku Cloud indicator.
 *
 * <p>The Ichimoku Cloud consists of 5 lines:
 * <ul>
 *   <li><b>Tenkan-sen</b> (Conversion Line): (9-period high + 9-period low) / 2</li>
 *   <li><b>Kijun-sen</b> (Base Line): (26-period high + 26-period low) / 2</li>
 *   <li><b>Senkou Span A</b> (Leading Span A): (Tenkan + Kijun) / 2, plotted 26 periods ahead</li>
 *   <li><b>Senkou Span B</b> (Leading Span B): (52-period high + 52-period low) / 2, plotted 26 periods ahead</li>
 *   <li><b>Chikou Span</b> (Lagging Span): Close price, plotted 26 periods behind</li>
 * </ul>
 *
 * <p>The area between Senkou Span A and B forms the "cloud" (kumo).
 */
public class IchimokuResult extends MultiLineResult {

    // Standard Ichimoku line names
    public static final String TENKAN_SEN = "Tenkan-sen";
    public static final String KIJUN_SEN = "Kijun-sen";
    public static final String SENKOU_SPAN_A = "Senkou Span A";
    public static final String SENKOU_SPAN_B = "Senkou Span B";
    public static final String CHIKOU_SPAN = "Chikou Span";

    // Standard line order
    private static final String[] LINE_NAMES = {
            TENKAN_SEN, KIJUN_SEN, SENKOU_SPAN_A, SENKOU_SPAN_B, CHIKOU_SPAN
    };

    /**
     * Creates an Ichimoku result with standard line names.
     *
     * @param id unique identifier
     */
    public IchimokuResult(String id) {
        super(id, "Ichimoku Cloud", LINE_NAMES);
        // Primary line is Tenkan-sen
        setPrimaryLineIndex(0);
    }

    /**
     * Returns the Tenkan-sen (Conversion Line).
     */
    public XyData getTenkanSen() {
        return getLine(TENKAN_SEN);
    }

    /**
     * Returns the Kijun-sen (Base Line).
     */
    public XyData getKijunSen() {
        return getLine(KIJUN_SEN);
    }

    /**
     * Returns the Senkou Span A (Leading Span A).
     */
    public XyData getSenkouSpanA() {
        return getLine(SENKOU_SPAN_A);
    }

    /**
     * Returns the Senkou Span B (Leading Span B).
     */
    public XyData getSenkouSpanB() {
        return getLine(SENKOU_SPAN_B);
    }

    /**
     * Returns the Chikou Span (Lagging Span).
     */
    public XyData getChikouSpan() {
        return getLine(CHIKOU_SPAN);
    }

    /**
     * Convenience method to append all 5 values at once.
     *
     * @param timestamp the timestamp
     * @param tenkan Tenkan-sen value
     * @param kijun Kijun-sen value
     * @param senkouA Senkou Span A value
     * @param senkouB Senkou Span B value
     * @param chikou Chikou Span value
     */
    public void appendIchimoku(long timestamp, float tenkan, float kijun,
                               float senkouA, float senkouB, float chikou) {
        append(timestamp, tenkan, kijun, senkouA, senkouB, chikou);
    }

    /**
     * Checks if the price is above the cloud at the given index.
     *
     * @param price the price to check
     * @param index the data index
     * @return true if price is above both Senkou Spans
     */
    public boolean isAboveCloud(float price, int index) {
        float spanA = getSenkouSpanA().getValue(index);
        float spanB = getSenkouSpanB().getValue(index);
        if (Float.isNaN(spanA) || Float.isNaN(spanB)) {
            return false;
        }
        float cloudTop = Math.max(spanA, spanB);
        return price > cloudTop;
    }

    /**
     * Checks if the price is below the cloud at the given index.
     *
     * @param price the price to check
     * @param index the data index
     * @return true if price is below both Senkou Spans
     */
    public boolean isBelowCloud(float price, int index) {
        float spanA = getSenkouSpanA().getValue(index);
        float spanB = getSenkouSpanB().getValue(index);
        if (Float.isNaN(spanA) || Float.isNaN(spanB)) {
            return false;
        }
        float cloudBottom = Math.min(spanA, spanB);
        return price < cloudBottom;
    }

    /**
     * Checks if the price is inside the cloud at the given index.
     *
     * @param price the price to check
     * @param index the data index
     * @return true if price is between Senkou Spans
     */
    public boolean isInsideCloud(float price, int index) {
        return !isAboveCloud(price, index) && !isBelowCloud(price, index);
    }

    /**
     * Returns whether the cloud is bullish (Senkou A above Senkou B) at the given index.
     *
     * @param index the data index
     * @return true if bullish cloud
     */
    public boolean isBullishCloud(int index) {
        float spanA = getSenkouSpanA().getValue(index);
        float spanB = getSenkouSpanB().getValue(index);
        return !Float.isNaN(spanA) && !Float.isNaN(spanB) && spanA > spanB;
    }

    /**
     * Returns the cloud thickness at the given index.
     *
     * @param index the data index
     * @return the absolute difference between Senkou Spans
     */
    public float getCloudThickness(int index) {
        float spanA = getSenkouSpanA().getValue(index);
        float spanB = getSenkouSpanB().getValue(index);
        if (Float.isNaN(spanA) || Float.isNaN(spanB)) {
            return Float.NaN;
        }
        return Math.abs(spanA - spanB);
    }
}
