package com.apokalypsix.chartx.core.ui.overlay;

import com.apokalypsix.chartx.chart.finance.indicator.IndicatorInstance;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorManager;
import com.apokalypsix.chartx.chart.finance.indicator.result.IndicatorResult;
import com.apokalypsix.chartx.chart.series.RenderableSeries;
import com.apokalypsix.chartx.chart.series.SeriesType;
import com.apokalypsix.chartx.core.data.model.TPOProfile;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Overlay displaying active indicator values.
 *
 * <p>Shows the current values of all enabled indicators. Each indicator
 * displays its name and current value(s) at the displayed bar index.
 *
 * <p>For multi-line indicators (like MACD), all lines are shown.
 * For band indicators (like Bollinger Bands), upper/lower bands are shown.
 *
 * <p>Example usage:
 * <pre>{@code
 * IndicatorOverlay overlay = new IndicatorOverlay(config);
 * overlay.setIndicatorManager(indicatorManager);
 * overlay.setDisplayIndex(currentBarIndex);
 * }</pre>
 */
public class IndicatorOverlay extends InfoOverlay {

    private IndicatorManager indicatorManager;
    private Supplier<List<RenderableSeries<?, ?>>> seriesProvider;
    private TPOSeries tpoSeries;
    private int displayIndex = -1; // -1 means latest bar
    private final DecimalFormat valueFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");

    // New collapse behavior: icon at bottom when expanded
    private boolean bottomCollapseIcon = false;

    /**
     * Creates an indicator overlay with default configuration.
     */
    public IndicatorOverlay() {
        this(new InfoOverlayConfig());
    }

    /**
     * Creates an indicator overlay with the given configuration.
     *
     * @param config the appearance configuration
     */
    public IndicatorOverlay(InfoOverlayConfig config) {
        super("Indicators", config);
    }

    /**
     * Returns the indicator manager.
     */
    public IndicatorManager getIndicatorManager() {
        return indicatorManager;
    }

    /**
     * Sets the indicator manager to display values from.
     *
     * @param indicatorManager the indicator manager
     */
    public void setIndicatorManager(IndicatorManager indicatorManager) {
        this.indicatorManager = indicatorManager;
    }

    /**
     * Sets the series provider to display values from chart series.
     *
     * <p>The provider should return the list of renderable series from the chart.
     * This allows the overlay to display values for series that are added directly
     * to the chart (e.g., via addLineSeries) rather than through the indicator system.
     *
     * @param provider supplier that returns the list of series
     */
    public void setSeriesProvider(Supplier<List<RenderableSeries<?, ?>>> provider) {
        this.seriesProvider = provider;
    }

    /**
     * Returns the TPO series displayed in the overlay.
     *
     * @return the TPO series, or null if not set
     */
    public TPOSeries getTPOSeries() {
        return tpoSeries;
    }

    /**
     * Sets the TPO series to display in the overlay.
     *
     * <p>When set, the overlay will show TPO information (POC, VAH, VAL values)
     * with a settings icon to open the TPO property dialog.
     *
     * @param tpoSeries the TPO series to display, or null to remove
     */
    public void setTPOSeries(TPOSeries tpoSeries) {
        this.tpoSeries = tpoSeries;
    }

    /**
     * Returns the index of the bar to display values for.
     *
     * @return the bar index, or -1 for latest bar
     */
    public int getDisplayIndex() {
        return displayIndex;
    }

    /**
     * Sets the index of the bar to display values for.
     *
     * @param index the bar index, or -1 for latest bar
     */
    public void setDisplayIndex(int index) {
        this.displayIndex = index;
    }

    /**
     * Resets to display values at the latest bar.
     */
    public void showLatestBar() {
        this.displayIndex = -1;
    }

    /**
     * Sets the value format pattern.
     *
     * @param pattern the DecimalFormat pattern
     */
    public void setValueFormat(String pattern) {
        valueFormat.applyPattern(pattern);
    }

    /**
     * Returns whether the collapse icon is shown at the bottom when expanded.
     */
    public boolean isBottomCollapseIcon() {
        return bottomCollapseIcon;
    }

    /**
     * Sets whether to show the collapse icon at the bottom when expanded.
     * When true:
     * - Expanded: no title, just indicator rows + collapse icon row with triangle pointing up
     * - Collapsed: single row with "Indicators" text and expand icon (triangle pointing down)
     *
     * @param bottomCollapseIcon true to use bottom collapse icon mode
     */
    public void setBottomCollapseIcon(boolean bottomCollapseIcon) {
        this.bottomCollapseIcon = bottomCollapseIcon;
        if (bottomCollapseIcon) {
            // Configure for the new behavior
            config.collapseIconPosition(InfoOverlayConfig.CollapseIconPosition.BOTTOM_ROW);
        } else {
            config.collapseIconPosition(InfoOverlayConfig.CollapseIconPosition.HEADER);
        }
    }

    @Override
    public String getTitle() {
        // When using bottom collapse icon mode:
        // - Expanded: no title (return empty)
        // - Collapsed: "Indicators"
        if (bottomCollapseIcon && !isCollapsed()) {
            return "";
        }
        return super.getTitle();
    }

    @Override
    public void updateContent() {
        clearRows();

        // Display indicator values from IndicatorManager
        if (indicatorManager != null) {
            Collection<IndicatorInstance<?, ?>> indicators = indicatorManager.getActiveIndicators();
            for (IndicatorInstance<?, ?> instance : indicators) {
                if (!instance.isEnabled()) {
                    continue;
                }

                Data<?> output = instance.getOutputData();
                if (output == null || output.isEmpty()) {
                    continue;
                }

                String name = instance.getDescriptor().getName();
                addIndicatorValues(name, output, instance);
            }
        }

        // Display values from chart series (added via addLineSeries, addBandSeries, etc.)
        if (seriesProvider != null) {
            List<RenderableSeries<?, ?>> series = seriesProvider.get();
            if (series != null) {
                for (RenderableSeries<?, ?> s : series) {
                    // Skip primary candlestick series (displayed in OHLC overlay)
                    if (s.getType() == SeriesType.CANDLESTICK) {
                        continue;
                    }

                    if (!s.isVisible()) {
                        continue;
                    }

                    Data<?> data = s.getData();
                    if (data == null || data.isEmpty()) {
                        continue;
                    }

                    // Use data name or fallback to series ID
                    String name = data.getName();
                    if (name == null || name.isEmpty()) {
                        name = s.getId();
                    }

                    addSeriesValues(name, data, s);
                }
            }
        }

        // Display TPO values if available
        if (tpoSeries != null && !tpoSeries.isEmpty()) {
            addTPOValues();
        }
    }

    /**
     * Adds TPO (Market Profile) values to the overlay.
     */
    private void addTPOValues() {
        // Get latest profile or the one at display index
        int profileIndex = tpoSeries.size() - 1;
        if (displayIndex >= 0) {
            // Find profile that contains the display timestamp
            // For now, just use the latest profile
            profileIndex = tpoSeries.size() - 1;
        }

        if (profileIndex < 0) {
            return;
        }

        TPOProfile profile = tpoSeries.getProfile(profileIndex);
        if (profile == null) {
            return;
        }

        // Add TPO header row with settings icon
        String name = tpoSeries.getName() != null ? tpoSeries.getName() : "TPO";
        addRow(name, "", ColorType.NEUTRAL, tpoSeries, true);

        // Add POC value
        float poc = profile.getPOC();
        if (!Float.isNaN(poc) && tpoSeries.isShowPOC()) {
            addRow("  POC", priceFormat.format(poc), ColorType.NEUTRAL, null, false);
        }

        // Add Value Area values
        if (tpoSeries.isShowValueArea()) {
            float vah = profile.getValueAreaHigh();
            float val = profile.getValueAreaLow();
            if (!Float.isNaN(vah)) {
                addRow("  VAH", priceFormat.format(vah), ColorType.NEUTRAL, null, false);
            }
            if (!Float.isNaN(val)) {
                addRow("  VAL", priceFormat.format(val), ColorType.NEUTRAL, null, false);
            }
        }
    }

    /**
     * Adds value rows for an indicator.
     */
    private void addIndicatorValues(String name, Data<?> output, IndicatorInstance<?, ?> instance) {
        // Handle different output types
        if (output instanceof IndicatorResult) {
            addIndicatorResultValues(name, (IndicatorResult) output, instance);
        } else if (output instanceof XyyData) {
            addXyyValues(name, (XyyData) output, instance);
        } else if (output instanceof XyData) {
            addXyValues(name, (XyData) output, instance);
        }
    }

    /**
     * Adds values for an IndicatorResult (multi-line).
     */
    private void addIndicatorResultValues(String name, IndicatorResult result, IndicatorInstance<?, ?> instance) {
        int lineCount = result.getLineCount();
        List<String> lineNames = result.getLineNames();
        boolean firstLine = true;

        for (int i = 0; i < lineCount; i++) {
            XyData line = result.getLine(i);
            if (line == null || line.isEmpty()) {
                continue;
            }

            int index = displayIndex >= 0 && displayIndex < line.size()
                    ? displayIndex
                    : line.size() - 1;

            float value = line.getValue(index);

            String label;
            if (lineCount == 1) {
                // Single line - just show indicator name
                label = name;
            } else {
                // Multi-line - show indicator name + line name
                String lineName = (lineNames != null && i < lineNames.size())
                        ? lineNames.get(i)
                        : "Line " + (i + 1);
                label = name + " " + lineName;
            }

            // Show settings icon only on first line of each indicator
            addRow(label, valueFormat.format(value), ColorType.NEUTRAL, instance, firstLine);
            firstLine = false;
        }
    }

    /**
     * Adds values for XyyData (band data with upper, middle, lower).
     */
    private void addXyyValues(String name, XyyData data, IndicatorInstance<?, ?> instance) {
        int index = displayIndex >= 0 && displayIndex < data.size()
                ? displayIndex
                : data.size() - 1;

        float upper = data.getUpper(index);
        float middle = data.getMiddle(index);
        float lower = data.getLower(index);

        // Show settings icon only on first line (Upper)
        addRow(name + " Upper", valueFormat.format(upper), ColorType.NEUTRAL, instance, true);
        addRow(name + " Mid", valueFormat.format(middle), ColorType.NEUTRAL, instance, false);
        addRow(name + " Lower", valueFormat.format(lower), ColorType.NEUTRAL, instance, false);
    }

    /**
     * Adds values for basic XyData.
     */
    private void addXyValues(String name, XyData data, IndicatorInstance<?, ?> instance) {
        int index = displayIndex >= 0 && displayIndex < data.size()
                ? displayIndex
                : data.size() - 1;

        float value = data.getValue(index);
        addRow(name, valueFormat.format(value), ColorType.NEUTRAL, instance, true);
    }

    // ========== Series Display Methods ==========

    /**
     * Adds values for a chart series.
     */
    private void addSeriesValues(String name, Data<?> data, RenderableSeries<?, ?> series) {
        if (data instanceof XyyData xyyData) {
            addXyySeriesValues(name, xyyData, series);
        } else if (data instanceof XyData xyData) {
            addXySeriesValues(name, xyData, series);
        }
        // Note: OhlcData is handled by OHLCDataOverlay, not here
    }

    /**
     * Adds values for XyyData series (band data with upper, middle, lower).
     */
    private void addXyySeriesValues(String name, XyyData data, RenderableSeries<?, ?> series) {
        int index = displayIndex >= 0 && displayIndex < data.size()
                ? displayIndex
                : data.size() - 1;

        float upper = data.getUpper(index);
        float middle = data.getMiddle(index);
        float lower = data.getLower(index);

        // Show settings icon only on first line
        addRow(name + " Upper", valueFormat.format(upper), ColorType.NEUTRAL, series, true);
        addRow(name + " Mid", valueFormat.format(middle), ColorType.NEUTRAL, series, false);
        addRow(name + " Lower", valueFormat.format(lower), ColorType.NEUTRAL, series, false);
    }

    /**
     * Adds values for XyData series.
     */
    private void addXySeriesValues(String name, XyData data, RenderableSeries<?, ?> series) {
        int index = displayIndex >= 0 && displayIndex < data.size()
                ? displayIndex
                : data.size() - 1;

        float value = data.getValue(index);
        addRow(name, valueFormat.format(value), ColorType.NEUTRAL, series, true);
    }
}
