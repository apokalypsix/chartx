package com.apokalypsix.chartx.core.ui.overlay;

import com.apokalypsix.chartx.chart.data.OhlcData;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.DecimalFormat;

/**
 * Overlay displaying symbol and OHLC data in a single horizontal line.
 *
 * <p>Format: {@code EXCHANGE:SYMBOL  O:xxx  H:xxx  L:xxx  C:xxx  V:xxx}
 *
 * <p>Features:
 * <ul>
 *   <li>Compact single-line display</li>
 *   <li>Symbol in white/bold, labels in gray, close colored by direction</li>
 *   <li>Transparent mode (no background) by default</li>
 *   <li>Text shadow for readability on chart</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * PriceBarOverlay overlay = new PriceBarOverlay(config);
 * overlay.setSymbolInfo("BINANCE", "BTC");
 * overlay.setData(ohlcData);
 * }</pre>
 */
public class PriceBarOverlay extends InfoOverlay {

    private String exchange = "";
    private String symbol = "";
    private OhlcData data;
    private int displayIndex = -1; // -1 means latest bar

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat volumeFormat = new DecimalFormat("#,##0");

    // Layout mode: true for horizontal single-line, false for standard vertical
    private boolean horizontalLayout = true;

    /**
     * Creates a price bar overlay with default configuration.
     */
    public PriceBarOverlay() {
        this(new InfoOverlayConfig()
                .transparent(true)
                .textShadow(true)
                .collapsible(false)
                .showExpandButton(false));
    }

    /**
     * Creates a price bar overlay with the given configuration.
     *
     * @param config the appearance configuration
     */
    public PriceBarOverlay(InfoOverlayConfig config) {
        super("", config);
    }

    /**
     * Returns the exchange name.
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Returns the symbol name.
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Sets the symbol information.
     *
     * @param exchange the exchange (e.g., "BINANCE", "NASDAQ")
     * @param symbol   the symbol (e.g., "BTC", "AAPL")
     */
    public void setSymbolInfo(String exchange, String symbol) {
        this.exchange = exchange != null ? exchange : "";
        this.symbol = symbol != null ? symbol : "";
    }

    /**
     * Returns the bound OHLC data.
     */
    public OhlcData getData() {
        return data;
    }

    /**
     * Sets the OHLC data to display.
     *
     * @param data the OHLC data
     */
    public void setData(OhlcData data) {
        this.data = data;
    }

    /**
     * Returns the index of the bar to display.
     *
     * @return the bar index, or -1 for latest bar
     */
    public int getDisplayIndex() {
        return displayIndex;
    }

    /**
     * Sets the index of the bar to display.
     *
     * @param index the bar index, or -1 for latest bar
     */
    public void setDisplayIndex(int index) {
        this.displayIndex = index;
    }

    /**
     * Resets to display the latest bar.
     */
    public void showLatestBar() {
        this.displayIndex = -1;
    }

    /**
     * Returns whether horizontal layout mode is enabled.
     */
    public boolean isHorizontalLayout() {
        return horizontalLayout;
    }

    /**
     * Sets whether to use horizontal single-line layout.
     *
     * @param horizontalLayout true for single-line, false for standard vertical
     */
    public void setHorizontalLayout(boolean horizontalLayout) {
        this.horizontalLayout = horizontalLayout;
    }

    /**
     * Sets the price format pattern.
     *
     * @param pattern the DecimalFormat pattern
     */
    public void setPriceFormat(String pattern) {
        priceFormat.applyPattern(pattern);
    }

    @Override
    public void updateContent() {
        clearRows();

        if (!horizontalLayout) {
            // Standard vertical layout - populate rows like OHLCDataOverlay
            updateVerticalContent();
        }
        // Horizontal layout doesn't use rows - rendered directly in render()
    }

    /**
     * Updates content for standard vertical layout.
     */
    private void updateVerticalContent() {
        if (data == null || data.size() == 0) {
            addRow("O", "—");
            addRow("H", "—");
            addRow("L", "—");
            addRow("C", "—");
            return;
        }

        int index = displayIndex >= 0 && displayIndex < data.size()
                ? displayIndex
                : data.size() - 1;

        float open = data.getOpen(index);
        float high = data.getHigh(index);
        float low = data.getLow(index);
        float close = data.getClose(index);
        float volume = data.getVolume(index);

        ColorType priceColor = close >= open ? ColorType.POSITIVE : ColorType.NEGATIVE;

        addRow("O", priceFormat.format(open));
        addRow("H", priceFormat.format(high));
        addRow("L", priceFormat.format(low));
        addRow("C", priceFormat.format(close), priceColor);
        addRow("Vol", formatVolume(volume));
    }

    @Override
    public void render(Graphics2D g2, int width, int height) {
        if (!config.isVisible()) {
            return;
        }

        if (!horizontalLayout) {
            // Use standard vertical rendering from parent
            super.render(g2, width, height);
            return;
        }

        // Horizontal single-line rendering
        renderHorizontalLayout(g2, width, height);
    }

    /**
     * Renders the horizontal single-line layout.
     */
    private void renderHorizontalLayout(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int padding = config.getPadding();
        int marginX = config.getMarginX();
        int marginY = config.getMarginY();

        // Build the display string and calculate positions
        FontMetrics headerFm = g2.getFontMetrics(config.getHeaderFont());
        FontMetrics labelFm = g2.getFontMetrics(config.getLabelFont());
        FontMetrics valueFm = g2.getFontMetrics(config.getValueFont());

        int lineHeight = Math.max(headerFm.getHeight(), Math.max(labelFm.getHeight(), valueFm.getHeight()));

        // Calculate position based on overlay position
        int x, y;
        OverlayPosition pos = config.getPosition();

        switch (pos) {
            case TOP_RIGHT:
                x = width - marginX; // Will adjust for content width
                y = marginY + stackOffset;
                break;
            case BOTTOM_LEFT:
                x = marginX;
                y = height - lineHeight - marginY - stackOffset;
                break;
            case BOTTOM_RIGHT:
                x = width - marginX;
                y = height - lineHeight - marginY - stackOffset;
                break;
            case TOP_LEFT:
            default:
                x = marginX;
                y = marginY + stackOffset;
                break;
        }

        // Get OHLC values
        float open = 0, high = 0, low = 0, close = 0, volume = 0;
        float prevClose = 0;
        boolean hasData = data != null && data.size() > 0;

        if (hasData) {
            int index = displayIndex >= 0 && displayIndex < data.size()
                    ? displayIndex
                    : data.size() - 1;

            open = data.getOpen(index);
            high = data.getHigh(index);
            low = data.getLow(index);
            close = data.getClose(index);
            volume = data.getVolume(index);

            if (index > 0) {
                prevClose = data.getClose(index - 1);
            }
        }

        // Determine close color
        Color closeColor = close >= open ? config.getPositiveColor() : config.getNegativeColor();

        // Build segments for rendering
        // Format: EXCHANGE:SYMBOL  O:xxx  H:xxx  L:xxx  C:xxx  V:xxx
        int currentX = x;
        int textY = y + headerFm.getAscent();

        Color shadowColor = new Color(0, 0, 0, 180);
        boolean useShadow = config.isTextShadow();

        // Draw symbol (EXCHANGE:SYMBOL)
        String symbolText = buildSymbolText();
        if (!symbolText.isEmpty()) {
            g2.setFont(config.getHeaderFont());
            if (useShadow) {
                g2.setColor(shadowColor);
                g2.drawString(symbolText, currentX + 1, textY + 1);
            }
            g2.setColor(config.getHeaderColor());
            g2.drawString(symbolText, currentX, textY);
            currentX += headerFm.stringWidth(symbolText) + padding * 2;
        }

        if (hasData) {
            // Draw O:xxx
            currentX = drawLabelValue(g2, "O", priceFormat.format(open), config.getLabelColor(),
                    currentX, textY, labelFm, valueFm, useShadow, shadowColor);

            // Draw H:xxx
            currentX = drawLabelValue(g2, "H", priceFormat.format(high), config.getLabelColor(),
                    currentX, textY, labelFm, valueFm, useShadow, shadowColor);

            // Draw L:xxx
            currentX = drawLabelValue(g2, "L", priceFormat.format(low), config.getLabelColor(),
                    currentX, textY, labelFm, valueFm, useShadow, shadowColor);

            // Draw C:xxx (colored)
            currentX = drawLabelValue(g2, "C", priceFormat.format(close), closeColor,
                    currentX, textY, labelFm, valueFm, useShadow, shadowColor);

            // Draw V:xxx
            drawLabelValue(g2, "V", formatVolume(volume), config.getLabelColor(),
                    currentX, textY, labelFm, valueFm, useShadow, shadowColor);
        } else {
            // No data - show placeholders
            g2.setFont(config.getLabelFont());
            String placeholder = "O:—  H:—  L:—  C:—  V:—";
            if (useShadow) {
                g2.setColor(shadowColor);
                g2.drawString(placeholder, currentX + 1, textY + 1);
            }
            g2.setColor(config.getLabelColor());
            g2.drawString(placeholder, currentX, textY);
        }

        // Update bounds for hit testing
        int totalWidth = currentX - x + padding;
        bounds.setBounds(x, y, totalWidth, lineHeight);
        headerBounds.setBounds(x, y, totalWidth, lineHeight);
    }

    /**
     * Draws a label:value pair and returns the new X position.
     */
    private int drawLabelValue(Graphics2D g2, String label, String value, Color valueColor,
                               int x, int y, FontMetrics labelFm, FontMetrics valueFm,
                               boolean useShadow, Color shadowColor) {
        int spacing = config.getPadding();

        // Draw label
        g2.setFont(config.getLabelFont());
        String labelWithColon = label + ":";
        if (useShadow) {
            g2.setColor(shadowColor);
            g2.drawString(labelWithColon, x + 1, y + 1);
        }
        g2.setColor(config.getLabelColor());
        g2.drawString(labelWithColon, x, y);
        x += labelFm.stringWidth(labelWithColon);

        // Draw value
        g2.setFont(config.getValueFont());
        if (useShadow) {
            g2.setColor(shadowColor);
            g2.drawString(value, x + 1, y + 1);
        }
        g2.setColor(valueColor);
        g2.drawString(value, x, y);
        x += valueFm.stringWidth(value) + spacing;

        return x;
    }

    /**
     * Builds the symbol text (EXCHANGE:SYMBOL or just SYMBOL).
     */
    private String buildSymbolText() {
        if (exchange.isEmpty() && symbol.isEmpty()) {
            return "";
        }
        if (exchange.isEmpty()) {
            return symbol;
        }
        if (symbol.isEmpty()) {
            return exchange;
        }
        return exchange + ":" + symbol;
    }

    /**
     * Formats volume with K/M/B suffixes.
     */
    private String formatVolume(float volume) {
        if (volume >= 1_000_000_000) {
            return String.format("%.2fB", volume / 1_000_000_000);
        } else if (volume >= 1_000_000) {
            return String.format("%.2fM", volume / 1_000_000);
        } else if (volume >= 1_000) {
            return String.format("%.2fK", volume / 1_000);
        } else {
            return volumeFormat.format(volume);
        }
    }
}
