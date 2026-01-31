package com.apokalypsix.chartx.core.ui.overlay;

import com.apokalypsix.chartx.chart.style.SeriesOptions;
import com.apokalypsix.chartx.chart.data.OhlcData;

import java.text.DecimalFormat;

/**
 * Overlay displaying OHLC (Open, High, Low, Close) data values.
 *
 * <p>Shows:
 * <ul>
 *   <li>Open price</li>
 *   <li>High price</li>
 *   <li>Low price</li>
 *   <li>Close price</li>
 *   <li>Volume (optional)</li>
 *   <li>Change percentage (optional)</li>
 * </ul>
 *
 * <p>The overlay can display values for:
 * <ul>
 *   <li>The latest bar (default)</li>
 *   <li>The bar at crosshair position</li>
 *   <li>A specific bar index</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * OHLCDataOverlay overlay = new OHLCDataOverlay(config);
 * overlay.setData(ohlcData);
 * overlay.setShowVolume(true);
 * overlay.setShowChange(true);
 * }</pre>
 */
public class OHLCDataOverlay extends InfoOverlay {

    private OhlcData data;
    private SeriesOptions seriesOptions;
    private int displayIndex = -1; // -1 means latest bar
    private boolean showVolume = true;
    private boolean showChange = true;
    private boolean showSettingsIcon = false;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat volumeFormat = new DecimalFormat("#,##0");
    private final DecimalFormat changeFormat = new DecimalFormat("+0.00%;-0.00%");

    /**
     * Creates an OHLC data overlay with default configuration.
     */
    public OHLCDataOverlay() {
        this(new InfoOverlayConfig());
    }

    /**
     * Creates an OHLC data overlay with the given configuration.
     *
     * @param config the appearance configuration
     */
    public OHLCDataOverlay(InfoOverlayConfig config) {
        super("OHLC", config);
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
     * Returns whether volume is shown.
     */
    public boolean isShowVolume() {
        return showVolume;
    }

    /**
     * Sets whether to show volume.
     */
    public void setShowVolume(boolean showVolume) {
        this.showVolume = showVolume;
    }

    /**
     * Returns whether change percentage is shown.
     */
    public boolean isShowChange() {
        return showChange;
    }

    /**
     * Sets whether to show change percentage.
     */
    public void setShowChange(boolean showChange) {
        this.showChange = showChange;
    }

    /**
     * Returns the series options (for settings icon callback).
     */
    public SeriesOptions getSeriesOptions() {
        return seriesOptions;
    }

    /**
     * Sets the series options for editing via settings icon.
     *
     * @param seriesOptions the series options
     */
    public void setSeriesOptions(SeriesOptions seriesOptions) {
        this.seriesOptions = seriesOptions;
    }

    /**
     * Returns whether settings icon is shown.
     */
    public boolean isShowSettingsIcon() {
        return showSettingsIcon;
    }

    /**
     * Sets whether to show a settings icon for series configuration.
     * Requires seriesOptions to be set and a settings callback on the overlay.
     *
     * @param showSettingsIcon true to show settings icon
     */
    public void setShowSettingsIcon(boolean showSettingsIcon) {
        this.showSettingsIcon = showSettingsIcon;
    }

    /**
     * Sets the price format pattern.
     *
     * @param pattern the DecimalFormat pattern
     */
    public void setPriceFormat(String pattern) {
        priceFormat.applyPattern(pattern);
    }

    /**
     * Sets the volume format pattern.
     *
     * @param pattern the DecimalFormat pattern
     */
    public void setVolumeFormat(String pattern) {
        volumeFormat.applyPattern(pattern);
    }

    @Override
    public void updateContent() {
        clearRows();

        if (data == null || data.size() == 0) {
            addRow("O", "—");
            addRow("H", "—");
            addRow("L", "—");
            addRow("C", "—");
            return;
        }

        // Determine which bar to display
        int index = displayIndex >= 0 && displayIndex < data.size()
                ? displayIndex
                : data.size() - 1;

        float open = data.getOpen(index);
        float high = data.getHigh(index);
        float low = data.getLow(index);
        float close = data.getClose(index);
        float volume = data.getVolume(index);

        // Determine color based on close vs open
        ColorType priceColor = close >= open ? ColorType.POSITIVE : ColorType.NEGATIVE;

        // Show settings icon on Open row if enabled and series options available
        boolean hasSettings = showSettingsIcon && seriesOptions != null;
        addRow("O", priceFormat.format(open), ColorType.NEUTRAL, seriesOptions, hasSettings);
        addRow("H", priceFormat.format(high));
        addRow("L", priceFormat.format(low));
        addRow("C", priceFormat.format(close), priceColor);

        if (showVolume) {
            addRow("Vol", formatVolume(volume));
        }

        if (showChange && index > 0) {
            float prevClose = data.getClose(index - 1);
            if (prevClose != 0) {
                float change = (close - prevClose) / prevClose;
                ColorType changeColor = change >= 0 ? ColorType.POSITIVE : ColorType.NEGATIVE;
                addRow("Chg", changeFormat.format(change), changeColor);
            }
        }
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
