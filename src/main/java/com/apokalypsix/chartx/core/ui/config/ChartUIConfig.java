package com.apokalypsix.chartx.core.ui.config;

import com.apokalypsix.chartx.core.ui.overlay.InfoOverlayConfig;
import com.apokalypsix.chartx.core.ui.sidebar.SidebarConfig;

/**
 * Master configuration for all chart UI components.
 *
 * <p>This class aggregates configuration for:
 * <ul>
 *   <li>Sidebar - tool buttons for drawing, indicators</li>
 *   <li>Symbol Info Overlay - symbol name, timeframe, exchange</li>
 *   <li>OHLC Data Overlay - current bar OHLC values</li>
 *   <li>Indicator Overlay - active indicator values</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ChartUIConfig config = new ChartUIConfig()
 *     .enableSidebar(true)
 *     .sidebarConfig(new SidebarConfig()
 *         .position(SidebarPosition.LEFT)
 *         .width(48))
 *     .enableInfoOverlays(true)
 *     .symbolOverlayConfig(new InfoOverlayConfig()
 *         .visible(true)
 *         .collapsible(true));
 *
 * chart.setChartUIConfig(config);
 * }</pre>
 */
public class ChartUIConfig {

    // Sidebar
    private boolean sidebarEnabled = true;
    private SidebarConfig sidebarConfig = new SidebarConfig();

    // Info overlays
    private boolean infoOverlaysEnabled = true;
    private InfoOverlayConfig symbolOverlayConfig = new InfoOverlayConfig();
    private InfoOverlayConfig ohlcOverlayConfig = new InfoOverlayConfig();
    private InfoOverlayConfig indicatorOverlayConfig = new InfoOverlayConfig();

    /**
     * Creates default UI configuration.
     */
    public ChartUIConfig() {
    }

    /**
     * Creates a copy of the given configuration.
     */
    public ChartUIConfig(ChartUIConfig other) {
        this.sidebarEnabled = other.sidebarEnabled;
        this.sidebarConfig = new SidebarConfig(other.sidebarConfig);
        this.infoOverlaysEnabled = other.infoOverlaysEnabled;
        this.symbolOverlayConfig = new InfoOverlayConfig(other.symbolOverlayConfig);
        this.ohlcOverlayConfig = new InfoOverlayConfig(other.ohlcOverlayConfig);
        this.indicatorOverlayConfig = new InfoOverlayConfig(other.indicatorOverlayConfig);
    }

    // ========== Getters ==========

    public boolean isSidebarEnabled() {
        return sidebarEnabled;
    }

    public SidebarConfig getSidebarConfig() {
        return sidebarConfig;
    }

    public boolean isInfoOverlaysEnabled() {
        return infoOverlaysEnabled;
    }

    public InfoOverlayConfig getSymbolOverlayConfig() {
        return symbolOverlayConfig;
    }

    public InfoOverlayConfig getOhlcOverlayConfig() {
        return ohlcOverlayConfig;
    }

    public InfoOverlayConfig getIndicatorOverlayConfig() {
        return indicatorOverlayConfig;
    }

    // ========== Fluent setters ==========

    /**
     * Enables or disables the sidebar.
     *
     * @param enabled true to enable sidebar
     * @return this for chaining
     */
    public ChartUIConfig enableSidebar(boolean enabled) {
        this.sidebarEnabled = enabled;
        return this;
    }

    /**
     * Sets the sidebar configuration.
     *
     * @param config the sidebar config
     * @return this for chaining
     */
    public ChartUIConfig sidebarConfig(SidebarConfig config) {
        this.sidebarConfig = config != null ? config : new SidebarConfig();
        return this;
    }

    /**
     * Enables or disables all info overlays.
     *
     * @param enabled true to enable info overlays
     * @return this for chaining
     */
    public ChartUIConfig enableInfoOverlays(boolean enabled) {
        this.infoOverlaysEnabled = enabled;
        return this;
    }

    /**
     * Sets the symbol info overlay configuration.
     *
     * @param config the overlay config
     * @return this for chaining
     */
    public ChartUIConfig symbolOverlayConfig(InfoOverlayConfig config) {
        this.symbolOverlayConfig = config != null ? config : new InfoOverlayConfig();
        return this;
    }

    /**
     * Sets the OHLC data overlay configuration.
     *
     * @param config the overlay config
     * @return this for chaining
     */
    public ChartUIConfig ohlcOverlayConfig(InfoOverlayConfig config) {
        this.ohlcOverlayConfig = config != null ? config : new InfoOverlayConfig();
        return this;
    }

    /**
     * Sets the indicator values overlay configuration.
     *
     * @param config the overlay config
     * @return this for chaining
     */
    public ChartUIConfig indicatorOverlayConfig(InfoOverlayConfig config) {
        this.indicatorOverlayConfig = config != null ? config : new InfoOverlayConfig();
        return this;
    }

    // ========== Convenience methods ==========

    /**
     * Enables sidebar and all info overlays with default settings.
     *
     * @return this for chaining
     */
    public ChartUIConfig enableAll() {
        this.sidebarEnabled = true;
        this.infoOverlaysEnabled = true;
        return this;
    }

    /**
     * Disables sidebar and all info overlays.
     *
     * @return this for chaining
     */
    public ChartUIConfig disableAll() {
        this.sidebarEnabled = false;
        this.infoOverlaysEnabled = false;
        return this;
    }

    /**
     * Creates a minimal configuration with only essential features.
     *
     * @return minimal UI config
     */
    public static ChartUIConfig minimal() {
        return new ChartUIConfig()
                .enableSidebar(false)
                .enableInfoOverlays(true)
                .symbolOverlayConfig(new InfoOverlayConfig().visible(true))
                .ohlcOverlayConfig(new InfoOverlayConfig().visible(true))
                .indicatorOverlayConfig(new InfoOverlayConfig().visible(false));
    }

    /**
     * Creates a full TradingView-style configuration.
     *
     * @return full UI config
     */
    public static ChartUIConfig tradingViewStyle() {
        return new ChartUIConfig()
                .enableSidebar(true)
                .sidebarConfig(new SidebarConfig()
                        .visible(true)
                        .collapsible(true))
                .enableInfoOverlays(true)
                .symbolOverlayConfig(new InfoOverlayConfig()
                        .visible(true)
                        .collapsible(true))
                .ohlcOverlayConfig(new InfoOverlayConfig()
                        .visible(true)
                        .collapsible(true))
                .indicatorOverlayConfig(new InfoOverlayConfig()
                        .visible(true)
                        .collapsible(true)
                        .collapsed(true));
    }
}
