package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Built-in color rules for OHLC bar coloring.
 *
 * <p>These rules determine whether a bar is colored as bullish or bearish
 * based on different comparison criteria.
 */
public enum ColoredCandleRule implements OHLCColorRule {

    /**
     * Traditional candlestick coloring: compare close to open.
     * Bullish (green) when close >= open, bearish (red) when close < open.
     */
    CLOSE_VS_OPEN {
        @Override
        public Color getColor(OhlcData data, int index, Color bullishColor, Color bearishColor) {
            float close = data.getCloseArray()[index];
            float open = data.getOpenArray()[index];
            return close >= open ? bullishColor : bearishColor;
        }
    },

    /**
     * Momentum coloring: compare close to previous bar's close.
     * Bullish when current close >= previous close, bearish otherwise.
     * The first bar uses CLOSE_VS_OPEN logic.
     */
    CLOSE_VS_PREV_CLOSE {
        @Override
        public Color getColor(OhlcData data, int index, Color bullishColor, Color bearishColor) {
            if (index == 0) {
                // No previous bar, fall back to close vs open
                return CLOSE_VS_OPEN.getColor(data, index, bullishColor, bearishColor);
            }
            float close = data.getCloseArray()[index];
            float prevClose = data.getCloseArray()[index - 1];
            return close >= prevClose ? bullishColor : bearishColor;
        }
    },

    /**
     * Breakout high coloring: compare close to previous bar's high.
     * Bullish when current close >= previous high, bearish otherwise.
     * The first bar uses CLOSE_VS_OPEN logic.
     */
    CLOSE_VS_PREV_HIGH {
        @Override
        public Color getColor(OhlcData data, int index, Color bullishColor, Color bearishColor) {
            if (index == 0) {
                return CLOSE_VS_OPEN.getColor(data, index, bullishColor, bearishColor);
            }
            float close = data.getCloseArray()[index];
            float prevHigh = data.getHighArray()[index - 1];
            return close >= prevHigh ? bullishColor : bearishColor;
        }
    },

    /**
     * Breakout low coloring: compare close to previous bar's low.
     * Bullish when current close >= previous low, bearish otherwise.
     * The first bar uses CLOSE_VS_OPEN logic.
     */
    CLOSE_VS_PREV_LOW {
        @Override
        public Color getColor(OhlcData data, int index, Color bullishColor, Color bearishColor) {
            if (index == 0) {
                return CLOSE_VS_OPEN.getColor(data, index, bullishColor, bearishColor);
            }
            float close = data.getCloseArray()[index];
            float prevLow = data.getLowArray()[index - 1];
            return close >= prevLow ? bullishColor : bearishColor;
        }
    }
}
