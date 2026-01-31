package com.apokalypsix.chartx.chart.finance;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.ChartLayout;
import com.apokalypsix.chartx.chart.series.CandlestickSeries;
import com.apokalypsix.chartx.core.render.model.FootprintLayerV2;
import com.apokalypsix.chartx.core.render.model.HistogramLayerV2;
import com.apokalypsix.chartx.core.render.model.TPOLayerV2;
import com.apokalypsix.chartx.core.render.model.VolumeProfileLayerV2;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.core.data.model.FootprintSeries;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.core.data.model.VolumeProfileSeries;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.ui.overlay.IndicatorOverlay;
import com.apokalypsix.chartx.core.ui.overlay.InfoOverlayConfig;
import com.apokalypsix.chartx.core.ui.overlay.OHLCDataOverlay;
import com.apokalypsix.chartx.core.ui.overlay.PriceBarOverlay;
import com.apokalypsix.chartx.core.ui.overlay.SymbolInfoOverlay;

/**
 * Financial chart component extending Chart with OHLC-specific features.
 *
 * <p>FinanceChart adds financial market-specific functionality:
 * <ul>
 *   <li>OHLC (candlestick) data support via {@link #addCandlestickSeries(OhlcData, OhlcSeriesOptions)}</li>
 *   <li>Financial info overlays (symbol, OHLC values, indicators)</li>
 *   <li>Technical indicators via the indicator manager</li>
 *   <li>Advanced financial chart types (Volume Profile, TPO, Footprint)</li>
 * </ul>
 *
 * <p>For generic charting without financial features, use {@link Chart} directly.
 *
 * <p>Basic usage:
 * <pre>{@code
 * FinanceChart chart = new FinanceChart("main");
 * chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions());
 * chart.setSymbolInfo("AAPL", "1D", "NASDAQ");
 * chart.addIndicator("SMA", Map.of("period", 20));
 * frame.add(chart);
 * }</pre>
 *
 * @see Chart for generic chart functionality
 * @see ChartLayout for multi-pane financial chart layouts
 */
public class FinanceChart extends Chart {

    // ========== Primary OHLC Data ==========
    private OhlcData primaryOhlcData;
    private CandlestickSeries primaryCandlestickSeries;

    // ========== Advanced Financial Chart Layers (lazy init for performance) ==========
    private VolumeProfileLayerV2 volumeProfileLayer;
    private TPOLayerV2 tpoLayer;
    private FootprintLayerV2 footprintLayer;
    private HistogramLayerV2 histogramLayer;
    private boolean financialLayersInitialized = false;

    // ========== Financial Data ==========
    private HistogramData histogramData;

    // ========== Constructors ==========

    /**
     * Creates a financial chart with auto-generated ID using default OpenGL backend.
     */
    public FinanceChart() {
        super();
    }

    /**
     * Creates a financial chart with the specified ID using default OpenGL backend.
     *
     * @param id unique identifier for this chart
     */
    public FinanceChart(String id) {
        super(id);
    }

    /**
     * Creates a financial chart with the specified ID and rendering backend.
     *
     * @param id      unique identifier for this chart
     * @param backend the rendering backend to use
     */
    public FinanceChart(String id, RenderBackend backend) {
        super(id, backend);
    }

    // ========== Financial Overlays Setup ==========

    @Override
    protected void setupDefaultOverlays() {
        // Create combined price bar overlay (replaces symbol + OHLC overlays)
        PriceBarOverlay priceBarOverlay = new PriceBarOverlay();

        // Create indicator overlay with new bottom collapse icon behavior
        IndicatorOverlay indicatorOverlay = new IndicatorOverlay(
                new InfoOverlayConfig()
                        .transparent(true)
                        .textShadow(true)
                        .collapseIconPosition(InfoOverlayConfig.CollapseIconPosition.BOTTOM_ROW));
        indicatorOverlay.setBottomCollapseIcon(true);

        // Bind indicator manager
        indicatorOverlay.setIndicatorManager(indicatorManager);

        // Bind series provider so overlay displays values from chart series
        indicatorOverlay.setSeriesProvider(this::getAllSeries);

        // Register overlays
        registerOverlay(OVERLAY_PRICEBAR, priceBarOverlay);
        registerOverlay(OVERLAY_INDICATOR, indicatorOverlay);

        // Wire up settings callbacks
        wireOverlaySettingsCallbacks();
    }

    // ========== OHLC Data Management ==========

    /**
     * Creates and adds a candlestick series, setting it as the primary OHLC data source.
     *
     * <p>The first candlestick series added becomes the primary data source for:
     * <ul>
     *   <li>Auto-scaling calculations</li>
     *   <li>Indicator calculations</li>
     *   <li>OHLC data overlay display</li>
     * </ul>
     *
     * @param data the OHLC data
     * @param options the rendering options
     * @return the created candlestick series
     */
    @Override
    public CandlestickSeries addCandlestickSeries(OhlcData data, OhlcSeriesOptions options) {
        // If this is the first candlestick series, set it as primary
        boolean isFirstCandlestick = (primaryCandlestickSeries == null);

        // Unsubscribe from old primary data if replacing
        if (isFirstCandlestick && primaryOhlcData != null) {
            primaryOhlcData.removeListener(getDataListener());
        }

        // Remove previous primary series if replacing
        if (isFirstCandlestick && primaryCandlestickSeries != null) {
            removeSeries(primaryCandlestickSeries);
        }

        // Create the series using parent implementation
        CandlestickSeries series = new CandlestickSeries(data.getId(), data, options);
        addSeries(series);

        // Set as primary if first candlestick series
        if (isFirstCandlestick && data != null) {
            this.primaryOhlcData = data;
            this.primaryCandlestickSeries = series;

            // Don't set data on dataLayer - CandlestickSeries handles rendering
            dataLayer.setData(null);
            pipeline.setPrimaryData(data);
            indicatorManager.setSourceData(data);

            // Wire up price bar overlay with OHLC data
            PriceBarOverlay priceBarOverlay = getPriceBarOverlay();
            if (priceBarOverlay != null) {
                priceBarOverlay.setData(data);
            }

            // Subscribe to data changes
            data.addListener(getDataListener());

            // Fit viewport to data
            if (!data.isEmpty()) {
                viewport.fitToData(data);
                syncAxisWithViewport();
            }
        }

        repaint();
        return series;
    }

    /**
     * Returns the primary OHLC data.
     *
     * @return the primary OHLC data, or null if not set
     */
    public OhlcData getPrimaryData() {
        return primaryOhlcData;
    }

    /**
     * Returns the primary candlestick series if one was created.
     *
     * @return the primary candlestick series, or null if not created
     */
    public CandlestickSeries getPrimaryCandlestickSeries() {
        return primaryCandlestickSeries;
    }

    // ========== Auto-scaling Overrides ==========

    @Override
    protected void performAutoScale() {
        // Auto-scaling is now handled exclusively by RenderPipeline.autoScaleAxes()
        // to avoid double auto-scaling that causes flickering during zoom operations.
        // The RenderPipeline auto-scaling also includes overlay data in the range calculation.
    }

    @Override
    protected void onRangeChanged() {
        if (viewport.isAutoScaleY() && primaryOhlcData != null) {
            viewport.autoScalePriceToData(primaryOhlcData);
            syncAxisWithViewport();
        }
    }

    @Override
    protected void onDataChanged(Data<?> data) {
        if (viewport.isAutoScaleY() && primaryOhlcData != null) {
            viewport.autoScalePriceToData(primaryOhlcData);
            syncAxisWithViewport();
        }
    }

    @Override
    public void zoomToFit() {
        if (primaryOhlcData != null && !primaryOhlcData.isEmpty()) {
            viewport.fitToData(primaryOhlcData);
            syncAxisWithViewport();
            repaint();
        }
    }

    private void syncDefaultAxisWithViewport() {
        if (axisManager == null || viewport == null) {
            return;
        }
        var defaultAxis = axisManager.getDefaultAxis();
        if (defaultAxis != null && defaultAxis.isAutoScale()) {
            double oldMin = defaultAxis.getMinValue();
            double oldMax = defaultAxis.getMaxValue();
            double newMin = viewport.getMinPrice();
            double newMax = viewport.getMaxPrice();

            if (oldMin != newMin || oldMax != newMax) {
                defaultAxis.setValueRange(newMin, newMax);
                pipeline.getCoordinates().invalidateCache();
            }
        }
    }

    // ========== Symbol Info ==========

    /**
     * Sets the symbol information displayed in the price bar overlay.
     *
     * @param symbol    the symbol name (e.g., "AAPL", "EUR/USD")
     * @param timeframe the timeframe (e.g., "1D", "4H") - currently not displayed in compact mode
     * @param exchange  the exchange (e.g., "NASDAQ", "NYSE")
     */
    @Override
    public void setSymbolInfo(String symbol, String timeframe, String exchange) {
        PriceBarOverlay overlay = getPriceBarOverlay();
        if (overlay != null) {
            overlay.setSymbolInfo(exchange, symbol);
            repaint();
        }
    }

    /**
     * Returns the price bar overlay for the combined symbol + OHLC display.
     *
     * @return the price bar overlay, or null if not registered
     */
    public PriceBarOverlay getPriceBarOverlay() {
        return getOverlay(OVERLAY_PRICEBAR, PriceBarOverlay.class);
    }

    // ========== Advanced Financial Layers ==========

    /**
     * Lazily initializes advanced financial chart layers.
     */
    private void ensureFinancialLayersInitialized() {
        if (financialLayersInitialized) {
            return;
        }

        volumeProfileLayer = new VolumeProfileLayerV2();
        tpoLayer = new TPOLayerV2();
        footprintLayer = new FootprintLayerV2();
        histogramLayer = new HistogramLayerV2();

        // Wire up repaint callbacks
        volumeProfileLayer.setRepaintCallback(this::repaint);
        tpoLayer.setRepaintCallback(this::repaint);
        footprintLayer.setRepaintCallback(this::repaint);
        histogramLayer.setRepaintCallback(this::repaint);

        // Add to pipeline
        pipeline.addLayer(volumeProfileLayer);
        pipeline.addLayer(tpoLayer);
        pipeline.addLayer(footprintLayer);
        pipeline.addLayer(histogramLayer);
        pipeline.sortLayers();

        financialLayersInitialized = true;
    }

    // ========== Histogram Data ==========

    /**
     * Sets histogram data for this chart (e.g., volume bars).
     *
     * @param data the histogram data
     */
    public void setHistogramData(HistogramData data) {
        ensureFinancialLayersInitialized();

        if (this.histogramData != null) {
            this.histogramData.removeListener(getDataListener());
        }

        this.histogramData = data;
        histogramLayer.setData(data);
        pipeline.setHistogramData(data);

        if (data != null) {
            data.addListener(getDataListener());
            if (getPrimaryData() == null && data.size() > 0) {
                fitToHistogram();
            }
        }

        repaint();
    }

    /**
     * Returns the histogram data.
     *
     * @return the histogram data, or null if not set
     */
    public HistogramData getHistogramData() {
        return histogramData;
    }

    private void fitToHistogram() {
        if (histogramData == null || histogramData.size() == 0) {
            return;
        }

        long startTime = histogramData.getXValue(0);
        long endTime = histogramData.getXValue(histogramData.size() - 1);
        long timePadding = (long) ((endTime - startTime) * 0.02);
        viewport.setTimeRange(startTime - timePadding, endTime + timePadding);
        autoScaleToHistogram();
    }

    /**
     * Auto-scales the Y-axis to fit the histogram data.
     */
    public void autoScaleToHistogram() {
        if (histogramData == null || histogramData.size() == 0) {
            return;
        }

        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();

        int firstIdx = histogramData.indexAtOrAfter(startTime);
        int lastIdx = histogramData.indexAtOrBefore(endTime);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        float min = histogramData.findMinValue(firstIdx, lastIdx);
        float max = histogramData.findMaxValue(firstIdx, lastIdx);

        min = Math.min(min, 0);
        max = Math.max(max, 0);

        double padding = (max - min) * 0.05;
        viewport.setPriceRange(min - padding, max + padding);
        syncAxisWithViewport();
    }

    // ========== Volume Profile ==========

    /**
     * Sets the volume profile data to display.
     *
     * @param series the volume profile series
     */
    public void setVolumeProfile(VolumeProfileSeries series) {
        ensureFinancialLayersInitialized();
        volumeProfileLayer.setSeries(series);
        repaint();
    }

    /**
     * Returns the current volume profile series.
     *
     * @return the volume profile series, or null if not set
     */
    public VolumeProfileSeries getVolumeProfile() {
        return volumeProfileLayer != null ? volumeProfileLayer.getSeries() : null;
    }

    /**
     * Builds and sets a volume profile from the current OHLC data.
     *
     * @param tickSize the price tick size for aggregation
     * @return the built volume profile series, or null if no data
     */
    public VolumeProfileSeries buildVolumeProfile(float tickSize) {
        OhlcData data = getPrimaryData();
        if (data == null || data.isEmpty()) {
            return null;
        }

        VolumeProfileSeries profile = new VolumeProfileSeries(
                "vp_" + getId(), "Volume Profile", tickSize);
        profile.buildFromOHLC(data, 0, data.size() - 1);
        setVolumeProfile(profile);
        return profile;
    }

    /**
     * Sets volume profile overlay mode.
     *
     * @param overlay true to overlay on price chart, false for separate
     */
    public void setVolumeProfileOverlayMode(boolean overlay) {
        ensureFinancialLayersInitialized();
        volumeProfileLayer.setOverlayMode(overlay);
        pipeline.sortLayers();
        repaint();
    }

    // ========== TPO / Market Profile ==========

    /**
     * Sets the TPO (Time Price Opportunity) data to display.
     *
     * <p>If no primary OHLC data is set, the viewport will be automatically
     * fitted to the TPO data bounds.
     *
     * @param series the TPO series
     */
    public void setTPOData(TPOSeries series) {
        ensureFinancialLayersInitialized();
        tpoLayer.setSeries(series);

        // Also update the indicator overlay to show TPO values
        IndicatorOverlay indicatorOverlay = getOverlay(OVERLAY_INDICATOR, IndicatorOverlay.class);
        if (indicatorOverlay != null) {
            indicatorOverlay.setTPOSeries(series);
        }

        // Fit viewport to TPO data if no primary OHLC data and series is not empty
        if (series != null && !series.isEmpty() && primaryOhlcData == null) {
            fitToTPOData(series);
        }

        repaint();
    }

    /**
     * Fits the viewport to the given TPO series bounds.
     */
    private void fitToTPOData(TPOSeries series) {
        if (series == null || series.isEmpty()) {
            return;
        }

        // Set time range with padding
        long startTime = series.getMinX();
        long endTime = series.getMaxX();
        long timePadding = (long) ((endTime - startTime) * 0.05);
        viewport.setTimeRange(startTime - timePadding, endTime + timePadding);

        // Set price range with padding
        float lowestPrice = series.findLowestPrice(0, series.size() - 1);
        float highestPrice = series.findHighestPrice(0, series.size() - 1);
        double pricePadding = (highestPrice - lowestPrice) * 0.05;
        viewport.setPriceRange(lowestPrice - pricePadding, highestPrice + pricePadding);

        syncAxisWithViewport();
    }

    /**
     * Returns the current TPO series.
     *
     * @return the TPO series, or null if not set
     */
    public TPOSeries getTPOData() {
        return tpoLayer != null ? tpoLayer.getSeries() : null;
    }

    /**
     * Sets whether to show the Point of Control line on TPO charts.
     *
     * @param show true to show POC
     * @deprecated Use {@link TPOSeries#setShowPOC(boolean)} instead
     */
    @Deprecated
    public void setTPOShowPOC(boolean show) {
        TPOSeries series = getTPOData();
        if (series != null) {
            series.setShowPOC(show);
            repaint();
        }
    }

    /**
     * Sets whether to show the value area on TPO charts.
     *
     * @param show true to show value area
     * @deprecated Use {@link TPOSeries#setShowValueArea(boolean)} instead
     */
    @Deprecated
    public void setTPOShowValueArea(boolean show) {
        TPOSeries series = getTPOData();
        if (series != null) {
            series.setShowValueArea(show);
            repaint();
        }
    }

    /**
     * Sets TPO overlay mode.
     *
     * @param overlay true to overlay on price chart, false for separate
     * @deprecated Use {@link TPOSeries#setOverlayMode(boolean)} instead
     */
    @Deprecated
    public void setTPOOverlayMode(boolean overlay) {
        TPOSeries series = getTPOData();
        if (series != null) {
            series.setOverlayMode(overlay);
            ensureFinancialLayersInitialized();
            pipeline.sortLayers();
            repaint();
        }
    }

    /**
     * Sets whether to show the initial balance range on TPO charts.
     *
     * @param show true to show initial balance
     * @deprecated Use {@link TPOSeries#setShowInitialBalance(boolean)} instead
     */
    @Deprecated
    public void setTPOShowInitialBalance(boolean show) {
        TPOSeries series = getTPOData();
        if (series != null) {
            series.setShowInitialBalance(show);
            repaint();
        }
    }

    /**
     * Sets whether to highlight single prints on TPO charts.
     *
     * @param highlight true to highlight single prints
     * @deprecated Use {@link TPOSeries#setHighlightSinglePrints(boolean)} instead
     */
    @Deprecated
    public void setTPOHighlightSinglePrints(boolean highlight) {
        TPOSeries series = getTPOData();
        if (series != null) {
            series.setHighlightSinglePrints(highlight);
            repaint();
        }
    }

    /**
     * Sets the opacity for TPO rendering.
     *
     * @param opacity opacity value (0.0 = transparent, 1.0 = opaque)
     * @deprecated Use {@link TPOSeries#setOpacity(float)} instead
     */
    @Deprecated
    public void setTPOOpacity(float opacity) {
        TPOSeries series = getTPOData();
        if (series != null) {
            series.setOpacity(opacity);
            repaint();
        }
    }

    // ========== Footprint Charts ==========

    /**
     * Sets the footprint data to display.
     *
     * @param series the footprint series
     */
    public void setFootprintData(FootprintSeries series) {
        ensureFinancialLayersInitialized();
        footprintLayer.setSeries(series);
        repaint();
    }

    /**
     * Returns the current footprint series.
     *
     * @return the footprint series, or null if not set
     */
    public FootprintSeries getFootprintData() {
        return footprintLayer != null ? footprintLayer.getSeries() : null;
    }

    /**
     * Sets the footprint display mode.
     *
     * @param mode the display mode
     */
    public void setFootprintDisplayMode(FootprintLayerV2.DisplayMode mode) {
        if (footprintLayer != null) {
            footprintLayer.setDisplayMode(mode);
            repaint();
        }
    }

    /**
     * Sets the imbalance threshold for footprint highlighting.
     *
     * @param threshold the threshold value (e.g., 3.0 for 300% imbalance)
     */
    public void setFootprintImbalanceThreshold(float threshold) {
        if (footprintLayer != null) {
            footprintLayer.setImbalanceThreshold(threshold);
            repaint();
        }
    }
}
