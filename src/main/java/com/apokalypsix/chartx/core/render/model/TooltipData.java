package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Data container for tooltip content.
 *
 * <p>This class is designed to be reused each frame to avoid allocations.
 * Call {@link #clear()} to reset for the next frame.
 */
public class TooltipData {

    private long timestamp;
    private final List<TooltipRow> rows = new ArrayList<>();

    // Reusable formatters (not thread-safe, but tooltip is single-threaded)
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat volumeFormat = new DecimalFormat("#,##0");

    /**
     * Represents a single row in the tooltip.
     */
    public static class TooltipRow {
        private final String seriesName;
        private final Color color;
        private final String label;
        private final String value;
        private final boolean isHeader;

        /**
         * Creates a tooltip row.
         *
         * @param seriesName series name (for color swatch, can be null)
         * @param color color for swatch (can be null)
         * @param label field label like "O:", "H:", "L:", "C:" (can be null)
         * @param value formatted value
         */
        public TooltipRow(String seriesName, Color color, String label, String value) {
            this(seriesName, color, label, value, false);
        }

        /**
         * Creates a tooltip row.
         *
         * @param seriesName series name (for color swatch, can be null)
         * @param color color for swatch (can be null)
         * @param label field label (can be null)
         * @param value formatted value
         * @param isHeader true if this is a header row (bold)
         */
        public TooltipRow(String seriesName, Color color, String label, String value, boolean isHeader) {
            this.seriesName = seriesName;
            this.color = color;
            this.label = label;
            this.value = value;
            this.isHeader = isHeader;
        }

        public String getSeriesName() {
            return seriesName;
        }

        public Color getColor() {
            return color;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean isHeader() {
            return isHeader;
        }
    }

    /**
     * Creates an empty tooltip data container.
     */
    public TooltipData() {
    }

    /**
     * Sets the price format pattern.
     *
     * @param pattern DecimalFormat pattern
     */
    public void setPriceFormat(String pattern) {
        priceFormat.applyPattern(pattern);
    }

    /**
     * Sets the number of decimal places for price.
     *
     * @param decimals number of decimal places
     */
    public void setPriceDecimals(int decimals) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append(".");
            for (int i = 0; i < decimals; i++) {
                pattern.append("0");
            }
        }
        priceFormat.applyPattern(pattern.toString());
    }

    // ========== Getters ==========

    public long getTimestamp() {
        return timestamp;
    }

    public List<TooltipRow> getRows() {
        return rows;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    // ========== Setters ==========

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // ========== Add Methods ==========

    /**
     * Adds an OHLCV row for candlestick data.
     *
     * @param seriesName name of the series
     * @param color series color
     * @param open open price
     * @param high high price
     * @param low low price
     * @param close close price
     * @param volume volume
     */
    public void addOHLCRow(String seriesName, Color color, float open, float high,
                           float low, float close, float volume) {
        // Header row with series name
        if (seriesName != null) {
            rows.add(new TooltipRow(seriesName, color, null, seriesName, true));
        }

        // OHLCV values
        rows.add(new TooltipRow(null, null, "O:", formatPrice(open)));
        rows.add(new TooltipRow(null, null, "H:", formatPrice(high)));
        rows.add(new TooltipRow(null, null, "L:", formatPrice(low)));
        rows.add(new TooltipRow(null, null, "C:", formatPrice(close)));
        if (volume > 0) {
            rows.add(new TooltipRow(null, null, "V:", formatVolume(volume)));
        }
    }

    /**
     * Adds a simple value row.
     *
     * @param seriesName name of the series
     * @param color series color
     * @param value the value
     */
    public void addValueRow(String seriesName, Color color, float value) {
        if (Float.isNaN(value)) return;
        rows.add(new TooltipRow(seriesName, color, null, formatPrice(value)));
    }

    /**
     * Adds a labeled value row.
     *
     * @param seriesName name of the series
     * @param color series color
     * @param label the label
     * @param value the value
     */
    public void addLabeledRow(String seriesName, Color color, String label, float value) {
        if (Float.isNaN(value)) return;
        rows.add(new TooltipRow(seriesName, color, label, formatPrice(value)));
    }

    /**
     * Adds a header row.
     *
     * @param text the header text
     * @param color optional color
     */
    public void addHeader(String text, Color color) {
        rows.add(new TooltipRow(text, color, null, text, true));
    }

    /**
     * Adds a custom row with pre-formatted value.
     *
     * @param seriesName series name (can be null)
     * @param color color (can be null)
     * @param label label (can be null)
     * @param formattedValue the formatted value string
     */
    public void addCustomRow(String seriesName, Color color, String label, String formattedValue) {
        rows.add(new TooltipRow(seriesName, color, label, formattedValue));
    }

    // ========== Utility ==========

    /**
     * Clears all data for reuse.
     */
    public void clear() {
        timestamp = 0;
        rows.clear();
    }

    /**
     * Formats a price value.
     */
    public String formatPrice(float value) {
        return priceFormat.format(value);
    }

    /**
     * Formats a price value.
     */
    public String formatPrice(double value) {
        return priceFormat.format(value);
    }

    /**
     * Formats a volume value.
     */
    public String formatVolume(float value) {
        if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000);
        }
        return volumeFormat.format(value);
    }
}
