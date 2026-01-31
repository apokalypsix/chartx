package com.apokalypsix.chartx.chart;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.apokalypsix.chartx.ChartX;
import com.apokalypsix.chartx.chart.axis.TimeAxis;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorInstance;
import com.apokalypsix.chartx.chart.interaction.DrawingTool;
import com.apokalypsix.chartx.chart.interaction.modifier.DrawingModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.MouseWheelZoomModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.RolloverModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.ZoomPanModifier;
import com.apokalypsix.chartx.chart.overlay.Annotation;
import com.apokalypsix.chartx.chart.overlay.Drawing;
import com.apokalypsix.chartx.chart.overlay.region.PriceRangeRegion;
import com.apokalypsix.chartx.chart.overlay.region.TimeRangeRegion;
import com.apokalypsix.chartx.chart.series.BandSeries;
import com.apokalypsix.chartx.chart.series.CandlestickSeries;
import com.apokalypsix.chartx.chart.series.HistogramSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.series.RenderableSeries;
import com.apokalypsix.chartx.chart.series.ScatterSeries;
import com.apokalypsix.chartx.chart.style.BandSeriesOptions;
import com.apokalypsix.chartx.chart.style.HistogramSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.chart.style.ScatterSeriesOptions;
import com.apokalypsix.chartx.core.interaction.DrawingInteractionHandler;
import com.apokalypsix.chartx.core.render.model.AnnotationLayerV2;
import com.apokalypsix.chartx.core.render.model.DrawingLayerV2;
import com.apokalypsix.chartx.core.render.model.MultiSeriesLayerV2;
import com.apokalypsix.chartx.core.render.model.OverlayLayerV2;
import com.apokalypsix.chartx.core.render.model.RegionLayerV2;
import com.apokalypsix.chartx.core.render.api.RenderBackend;

/**
 * Generic chart component supporting various chart types and features.
 *
 * <p>Chart provides core charting functionality:
 * <ul>
 *   <li>Series management (candlesticks, lines, bands, scatter, histograms)</li>
 *   <li>Drawing tools and annotations</li>
 *   <li>Regions (time and price range highlighting)</li>
 *   <li>Multi-pane synchronization support</li>
 * </ul>
 *
 * <p>For financial charting with indicators, symbol info, and advanced chart types
 * (Volume Profile, TPO, Footprint), use {@link FinanceChart} instead.
 *
 * <p>Basic usage:
 * <pre>{@code
 * Chart chart = new Chart("main");
 * chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions());
 * chart.addLineSeries(xyData, new LineSeriesOptions().color(Color.BLUE));
 * frame.add(chart);
 * }</pre>
 *
 * <p>For financial charts with indicators:
 * <pre>{@code
 * FinanceChart chart = new FinanceChart("main");
 * chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions());
 * chart.setSymbolInfo("AAPL", "1D", "NASDAQ");
 * chart.addIndicator("SMA", Map.of("period", 20));
 * frame.add(chart);
 * }</pre>
 *
 * @see FinanceChart for financial-specific features
 * @see ChartLayout for multi-pane layouts
 */
public class Chart extends AbstractChartComponent {

    // ========== Identity ==========
    private final String id;
    private ChartType chartType = ChartType.PRICE;

    // ========== Series Layer (from ChartPanel) ==========
    private final MultiSeriesLayerV2 multiSeriesLayer;

    // ========== Common Layers ==========
    private final RegionLayerV2 regionLayer;
    private final DrawingLayerV2 drawingLayer;
    private final AnnotationLayerV2 annotationLayer;

    // ========== Overlay Layer ==========
    private OverlayLayerV2 overlayLayer;
    private boolean overlayLayerInitialized = false;

    // ========== Drawing Interaction ==========
    private final DrawingInteractionHandler drawingHandler;

    // ========== Synchronization ==========
    private RangeChangeListener rangeChangeListener;
    private boolean followLatestData = false;

    // ========== Data Listener ==========
    private final DataListener dataListener = new DataListener() {
        @Override
        public void onDataAppended(Data<?> data, int newIndex) {
            handleDataChanged(data, true);
        }

        @Override
        public void onDataUpdated(Data<?> data, int index) {
            handleDataChanged(data, false);
        }

        @Override
        public void onDataCleared(Data<?> data) {
            SwingUtilities.invokeLater(() -> repaint());
        }
    };

    /**
     * Returns the data listener for subscribing to data changes.
     * Subclasses can use this to subscribe additional data sources.
     *
     * @return the data listener
     */
    protected DataListener getDataListener() {
        return dataListener;
    }

    // ========== Constructors ==========

    /**
     * Creates a chart with auto-generated ID using default OpenGL backend.
     */
    public Chart() {
        this("chart-" + UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Creates a chart with the specified ID using the default backend.
     *
     * <p>The default backend is determined by {@link ChartX#getDefaultBackend()}.
     * To change the default, call {@link ChartX#setDefaultBackend(RenderBackend)}
     * at application startup.
     *
     * @param id unique identifier for this chart
     * @see ChartX#setDefaultBackend(RenderBackend)
     */
    public Chart(String id) {
        this(id, ChartX.getDefaultBackend());
    }

    /**
     * Creates a chart with the specified ID and rendering backend.
     *
     * @param id unique identifier for this chart
     * @param backend the rendering backend to use
     */
    public Chart(String id, RenderBackend backend) {
        super(backend);
        this.id = id;

        // Initialize core layers
        this.regionLayer = new RegionLayerV2();
        this.multiSeriesLayer = new MultiSeriesLayerV2();
        this.annotationLayer = new AnnotationLayerV2();
        this.drawingLayer = new DrawingLayerV2();
        this.drawingHandler = new DrawingInteractionHandler(drawingLayer);

        // Add core layers to pipeline
        pipeline.addLayer(regionLayer);
        pipeline.addLayer(multiSeriesLayer);
        pipeline.addLayer(annotationLayer);
        pipeline.addLayer(drawingLayer);

        // Wire up repaint callbacks
        regionLayer.setRepaintCallback(this::repaint);
        multiSeriesLayer.setRepaintCallback(this::repaint);
        annotationLayer.setRepaintCallback(this::repaint);
        drawingLayer.setRepaintCallback(this::repaint);

        // Set annotation layer on text overlay
        textOverlay.setAnnotationLayer(annotationLayer);
    }

    /**
     * Lazily initializes the overlay layer for line overlays.
     */
    private void ensureOverlayLayerInitialized() {
        if (overlayLayerInitialized) {
            return;
        }

        overlayLayer = new OverlayLayerV2();
        overlayLayer.setRepaintCallback(this::repaint);
        pipeline.addLayer(overlayLayer);
        pipeline.sortLayers();

        overlayLayerInitialized = true;
    }

    @Override
    protected void addAdditionalLayers() {
        // Layers are added in constructor, no additional layers needed here
    }

    @Override
    protected void addIndicatorResultToLayer(IndicatorInstance<?, ?> instance, Object result) {
        if (!instance.isEnabled()) {
            return;
        }

        // Get color from indicator parameters
        Color color = instance.getParameterValue("color");
        if (color == null) {
            color = Color.BLUE;
        }

        if (result instanceof XyData xyData) {
            // Single line indicator (EMA, SMA, etc.)
            LineSeriesOptions options = new LineSeriesOptions()
                    .color(color)
                    .lineWidth(1.5f);
            indicatorLayer.addLineOverlay(xyData, options);
            pipeline.addOverlayData(xyData);
        } else if (result instanceof XyyData xyyData) {
            // Band indicator (Bollinger, Keltner, etc.)
            // Add upper band
            XyData upperData = xyyData.asUpperData();
            LineSeriesOptions upperOptions = new LineSeriesOptions()
                    .color(color)
                    .lineWidth(1.0f)
                    .opacity(0.8f);
            indicatorLayer.addLineOverlay(upperData, upperOptions);
            pipeline.addOverlayData(upperData);

            // Add middle band
            XyData middleData = xyyData.asMiddleData();
            LineSeriesOptions middleOptions = new LineSeriesOptions()
                    .color(color)
                    .lineWidth(1.5f);
            indicatorLayer.addLineOverlay(middleData, middleOptions);
            pipeline.addOverlayData(middleData);

            // Add lower band
            XyData lowerData = xyyData.asLowerData();
            LineSeriesOptions lowerOptions = new LineSeriesOptions()
                    .color(color)
                    .lineWidth(1.0f)
                    .opacity(0.8f);
            indicatorLayer.addLineOverlay(lowerData, lowerOptions);
            pipeline.addOverlayData(lowerData);
        }
    }

    @Override
    protected void setupDefaultModifiers() {
        modifierGroup
            .with(new DrawingModifier())
            .with(new ZoomPanModifier())
            .with(new MouseWheelZoomModifier())
            .with(new RolloverModifier());
    }

    @Override
    protected void onViewportChanged() {
        if (rangeChangeListener != null) {
            rangeChangeListener.onRangeChanged(
                    viewport.getStartTime(), viewport.getEndTime());
        }
    }

    // ========== Identity ==========

    /**
     * Returns the chart ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the chart type.
     */
    public ChartType getChartType() {
        return chartType;
    }

    /**
     * Sets the chart type (affects axis label display and auto-scaling behavior).
     */
    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
    }

    // ========== Series Management ==========

    /**
     * Adds a series to the chart.
     */
    public void addSeries(RenderableSeries<?, ?> series) {
        multiSeriesLayer.addSeries(series);
    }

    /**
     * Batch add multiple series (single repaint).
     */
    public void addAllSeries(List<? extends RenderableSeries<?, ?>> seriesList) {
        multiSeriesLayer.addAllSeries(seriesList);
    }

    /**
     * Removes a series by ID.
     */
    public boolean removeSeries(String seriesId) {
        return multiSeriesLayer.removeSeries(seriesId);
    }

    /**
     * Removes a series.
     */
    public boolean removeSeries(RenderableSeries<?, ?> series) {
        return multiSeriesLayer.removeSeries(series);
    }

    /**
     * Returns a series by ID.
     */
    public RenderableSeries<?, ?> getSeries(String seriesId) {
        return multiSeriesLayer.getSeries(seriesId);
    }

    /**
     * Returns all series.
     */
    public List<RenderableSeries<?, ?>> getAllSeries() {
        return multiSeriesLayer.getAllSeries();
    }

    /**
     * Adds a custom render layer to the chart pipeline.
     * Use this for specialized visualizations like heatmaps, contours, etc.
     *
     * @param layer the render layer to add
     */
    public void addRenderLayer(com.apokalypsix.chartx.core.render.model.RenderLayer layer) {
        pipeline.addLayer(layer);
        pipeline.sortLayers();
    }

    /**
     * Clears all series.
     */
    public void clearSeries() {
        multiSeriesLayer.clearSeries();
    }

    // ========== Series Factory Methods ==========

    /**
     * Creates and adds a line series.
     */
    public LineSeries addLineSeries(XyData data, LineSeriesOptions options) {
        LineSeries series = new LineSeries(data, options);
        addSeries(series);
        return series;
    }

    /**
     * Creates and adds a candlestick series.
     */
    public CandlestickSeries addCandlestickSeries(OhlcData data, OhlcSeriesOptions options) {
        CandlestickSeries series = new CandlestickSeries(data, options);
        addSeries(series);
        return series;
    }

    /**
     * Creates and adds a histogram series.
     */
    public HistogramSeries addHistogramSeries(HistogramData data, HistogramSeriesOptions options) {
        HistogramSeries series = new HistogramSeries(data, options);
        addSeries(series);
        return series;
    }

    /**
     * Creates and adds a band series.
     */
    public BandSeries addBandSeries(XyyData data, BandSeriesOptions options) {
        BandSeries series = new BandSeries(data, options);
        addSeries(series);
        return series;
    }

    /**
     * Creates and adds a scatter series.
     */
    public ScatterSeries addScatterSeries(XyData data, ScatterSeriesOptions options) {
        ScatterSeries series = new ScatterSeries(data, options);
        addSeries(series);
        return series;
    }

    // ========== Overlay Line Series (alternative API) ==========

    /**
     * Adds a line overlay using the overlay layer (useful for indicator overlays).
     */
    public void addLineOverlay(XyData data, LineSeriesOptions options) {
        ensureOverlayLayerInitialized();
        overlayLayer.addLineOverlay(data, options);
        pipeline.addOverlayData(data);
    }

    /**
     * Removes a line overlay.
     */
    public void removeLineOverlay(XyData data) {
        if (overlayLayer != null) {
            overlayLayer.removeLineOverlay(data);
            pipeline.removeOverlayData(data);
        }
    }

    /**
     * Clears all overlays.
     */
    public void clearOverlays() {
        if (overlayLayer != null) {
            overlayLayer.clearOverlays();
        }
    }

    // ========== Drawing Tools ==========

    /**
     * Sets the active drawing tool.
     */
    public void setDrawingTool(DrawingTool tool) {
        drawingHandler.setActiveTool(tool);
        repaint();
    }

    /**
     * Returns the active drawing tool.
     */
    public DrawingTool getDrawingTool() {
        return drawingHandler.getActiveTool();
    }

    /**
     * Adds a drawing.
     */
    public void addDrawing(Drawing drawing) {
        drawingLayer.addDrawing(drawing);
    }

    /**
     * Batch add multiple drawings (single repaint).
     */
    public void addAllDrawings(List<Drawing> drawings) {
        drawingLayer.addAllDrawings(drawings);
    }

    /**
     * Removes a drawing.
     */
    public void removeDrawing(Drawing drawing) {
        drawingLayer.removeDrawing(drawing);
    }

    /**
     * Removes a drawing by ID.
     */
    public void removeDrawing(String drawingId) {
        drawingLayer.removeDrawing(drawingId);
    }

    /**
     * Clears all drawings.
     */
    public void clearDrawings() {
        drawingLayer.clearDrawings();
    }

    @Override
    public DrawingLayerV2 getDrawingLayer() {
        return drawingLayer;
    }

    @Override
    public DrawingInteractionHandler getDrawingHandler() {
        return drawingHandler;
    }

    /**
     * Adds a drawing listener.
     */
    public void addDrawingListener(DrawingInteractionHandler.DrawingListener listener) {
        drawingHandler.addDrawingListener(listener);
    }

    /**
     * Removes a drawing listener.
     */
    public void removeDrawingListener(DrawingInteractionHandler.DrawingListener listener) {
        drawingHandler.removeDrawingListener(listener);
    }

    // ========== Annotations ==========

    /**
     * Adds an annotation.
     */
    public void addAnnotation(Annotation annotation) {
        annotationLayer.addAnnotation(annotation);
    }

    /**
     * Batch add multiple annotations (single repaint).
     */
    public void addAllAnnotations(List<? extends Annotation> annotations) {
        annotationLayer.addAllAnnotations(annotations);
    }

    /**
     * Removes an annotation.
     */
    public void removeAnnotation(Annotation annotation) {
        annotationLayer.removeAnnotation(annotation);
    }

    /**
     * Removes an annotation by ID.
     */
    public void removeAnnotation(String annotationId) {
        annotationLayer.removeAnnotation(annotationId);
    }

    /**
     * Clears all annotations.
     */
    public void clearAnnotations() {
        annotationLayer.clearAnnotations();
    }

    /**
     * Returns all annotations.
     */
    public List<Annotation> getAnnotations() {
        return annotationLayer.getAnnotations();
    }

    // ========== Regions ==========

    /**
     * Adds a time range region.
     */
    public void addRegion(TimeRangeRegion region) {
        regionLayer.addRegion(region);
    }

    /**
     * Batch add multiple time range regions (single repaint).
     */
    public void addAllRegions(List<TimeRangeRegion> regions) {
        regionLayer.addAllRegions(regions);
    }

    /**
     * Removes a time range region.
     */
    public void removeRegion(TimeRangeRegion region) {
        regionLayer.removeRegion(region);
    }

    /**
     * Removes a time range region by ID.
     */
    public void removeTimeRegion(String regionId) {
        regionLayer.removeTimeRegion(regionId);
    }

    /**
     * Clears all time range regions.
     */
    public void clearTimeRegions() {
        regionLayer.clearTimeRegions();
    }

    /**
     * Returns all time range regions.
     */
    public List<TimeRangeRegion> getTimeRegions() {
        return regionLayer.getTimeRegions();
    }

    /**
     * Adds a price range region.
     */
    public void addPriceRegion(PriceRangeRegion region) {
        regionLayer.addPriceRegion(region);
    }

    /**
     * Batch add multiple price range regions (single repaint).
     */
    public void addAllPriceRegions(List<PriceRangeRegion> regions) {
        regionLayer.addAllPriceRegions(regions);
    }

    /**
     * Removes a price range region.
     */
    public void removePriceRegion(PriceRangeRegion region) {
        regionLayer.removePriceRegion(region);
    }

    /**
     * Removes a price range region by ID.
     */
    public void removePriceRegion(String regionId) {
        regionLayer.removePriceRegion(regionId);
    }

    /**
     * Clears all price range regions.
     */
    public void clearPriceRegions() {
        regionLayer.clearPriceRegions();
    }

    /**
     * Returns all price range regions.
     */
    public List<PriceRangeRegion> getPriceRegions() {
        return regionLayer.getPriceRegions();
    }

    /**
     * Clears all regions (both time and price).
     */
    public void clearAllRegions() {
        regionLayer.clearAllRegions();
    }

    // ========== Navigation ==========

    /**
     * Fits the viewport to show all data from all series.
     * Subclasses can override to provide data-specific fitting.
     */
    public void zoomToFit() {
        List<RenderableSeries<?, ?>> allSeries = multiSeriesLayer.getAllSeries();
        if (allSeries.isEmpty()) {
            repaint();
            return;
        }

        // Find the time range across all series
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        boolean hasData = false;

        for (RenderableSeries<?, ?> series : allSeries) {
            Data<?> data = series.getData();
            if (data != null && !data.isEmpty()) {
                long start = data.getMinX();
                long end = data.getMaxX();
                if (start >= 0 && end >= 0) {
                    minTime = Math.min(minTime, start);
                    maxTime = Math.max(maxTime, end);
                    hasData = true;
                }
            }
        }

        if (!hasData || minTime >= maxTime) {
            repaint();
            return;
        }

        // Add 5% padding on each side
        long span = maxTime - minTime;
        long padding = Math.max(span / 20, 1);
        minTime -= padding;
        maxTime += padding;

        // Set the viewport time range
        viewport.setTimeRange(minTime, maxTime);

        // Keep horizontal axis in sync with viewport
        if (horizontalAxis instanceof TimeAxis ta) {
            ta.setVisibleRange(minTime, maxTime);
        }

        // Trigger auto-scaling for Y-axis
        onRangeChanged();
        repaint();
    }

    /**
     * Scrolls the viewport to show the specified timestamp.
     */
    public void scrollTo(long timestamp) {
        long halfDuration = viewport.getVisibleDuration() / 2;
        viewport.setTimeRange(timestamp - halfDuration, timestamp + halfDuration);
        repaint();
    }

    // ========== Visible Range Synchronization ==========

    /**
     * Sets the visible range (for synchronization from parent layout).
     *
     * @param start X-axis start value (timestamp for time-based, index for categorical)
     * @param end X-axis end value (timestamp for time-based, index for categorical)
     */
    public void setVisibleRange(long start, long end) {
        viewport.setTimeRange(start, end);

        // Keep horizontal axis in sync with viewport
        if (horizontalAxis instanceof TimeAxis ta) {
            ta.setVisibleRange(start, end);
        }

        onRangeChanged();
        repaint();
    }

    /**
     * Called when the visible range changes. Subclasses can override to perform
     * auto-scaling or other updates.
     */
    protected void onRangeChanged() {
        // Subclasses can override for data-specific auto-scaling
    }

    /**
     * Sets the listener for visible range changes.
     *
     * @param listener the listener to set
     */
    public void setRangeChangeListener(RangeChangeListener listener) {
        this.rangeChangeListener = listener;
    }

    // ========== Real-time Data Support ==========

    /**
     * Enables or disables viewport following mode.
     */
    public void setFollowLatestData(boolean follow) {
        this.followLatestData = follow;
    }

    /**
     * Returns true if viewport following mode is enabled.
     */
    public boolean isFollowLatestData() {
        return followLatestData;
    }

    private void handleDataChanged(Data<?> data, boolean isAppend) {
        SwingUtilities.invokeLater(() -> {
            if (followLatestData && isAppend) {
                adjustViewportToShowLatest(data);
            }

            onDataChanged(data);
            repaint();
        });
    }

    /**
     * Called when data changes. Subclasses can override for auto-scaling.
     *
     * @param data the data that changed
     */
    protected void onDataChanged(Data<?> data) {
        // Subclasses can override for data-specific auto-scaling
    }

    private void adjustViewportToShowLatest(Data<?> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        long latestTime = data.getMaxX();
        long viewEnd = viewport.getEndTime();
        long padding = barDuration * 2;

        if (latestTime + padding > viewEnd) {
            long shift = (latestTime + padding) - viewEnd;
            long newStart = viewport.getStartTime() + shift;
            long newEnd = viewEnd + shift;

            viewport.setTimeRange(newStart, newEnd);
            onViewportChanged();
        }
    }

    // ========== Y-Axis Management ==========

    /**
     * Creates a new Y-axis with the given ID and position.
     *
     * @param axisId   unique identifier for the axis
     * @param position LEFT or RIGHT
     * @return the created Y-axis
     */
    public YAxis createYAxis(String axisId, YAxis.Position position) {
        return axisManager.createAxis(axisId, position);
    }

    /**
     * Returns the default Y-axis.
     *
     * @return the default Y-axis
     * @deprecated Use {@link #getYAxis()} instead (inherited from AbstractChartComponent)
     */
    @Deprecated
    public YAxis getDefaultYAxis() {
        return axisManager.getDefaultAxis();
    }

    /**
     * Associates data with a specific Y-axis.
     */
    public void setDataAxis(Data<?> data, String axisId) {
        axisManager.setSeriesAxis(data.getId(), axisId);
        repaint();
    }

    // ========== Crosshair ==========

    /**
     * Sets the crosshair position (for synchronization from parent).
     */
    public void setCrosshairPosition(int x, int y) {
        updateCrosshairPosition(x, y);
        repaint();
    }

    /**
     * Sets crosshair visibility.
     */
    public void setCrosshairVisible(boolean visible) {
        crosshairLayer.setCursorVisible(visible);
        textOverlay.setCrosshairVisible(visible);
        repaint();
    }

    // ========== Auto-scaling ==========

    @Override
    public void repaint() {
        performAutoScale();
        super.repaint();
    }

    /**
     * Performs auto-scaling before repaint. Subclasses override to implement
     * data-specific auto-scaling.
     */
    protected void performAutoScale() {
        // Guard against calls during construction
        if (axisManager == null || multiSeriesLayer == null || pipeline == null) {
            return;
        }

        YAxis defaultAxis = axisManager.getDefaultAxis();
        if (defaultAxis == null || !defaultAxis.isAutoScale()) {
            return;
        }

        // Get all series from the multi-series layer
        if (multiSeriesLayer.getAllSeries().isEmpty()) {
            return;
        }

        // Determine visible index range
        // For category-based charts, use the full data range (0 to size-1)
        // For time-based charts, convert viewport time range to indices
        int startIdx = 0;
        int endIdx = Integer.MAX_VALUE;

        // Find the largest series size to use as a reasonable end index
        for (RenderableSeries<?, ?> series : multiSeriesLayer.getAllSeries()) {
            if (series.getData() != null) {
                int size = series.getData().size();
                if (size > 0 && size - 1 < endIdx) {
                    endIdx = size - 1;
                }
            }
        }

        if (endIdx == Integer.MAX_VALUE || endIdx < startIdx) {
            return;
        }

        // Get min/max values from all series
        double min = multiSeriesLayer.getMinValue(startIdx, endIdx);
        double max = multiSeriesLayer.getMaxValue(startIdx, endIdx);

        if (Double.isNaN(min) || Double.isNaN(max) || min >= max) {
            return;
        }

        // Add some padding (5% on each side)
        double range = max - min;
        double padding = range * 0.05;
        min -= padding;
        max += padding;

        // Update the Y-axis range
        defaultAxis.setValueRange(min, max);
        viewport.setPriceRange(min, max);
        pipeline.getCoordinates().invalidateCache();
    }

    /**
     * Syncs the default Y-axis with the current viewport price range.
     */
    protected void syncAxisWithViewport() {
        YAxis defaultAxis = axisManager.getDefaultAxis();
        if (defaultAxis != null) {
            defaultAxis.setValueRange(viewport.getMinPrice(), viewport.getMaxPrice());
            pipeline.getCoordinates().invalidateCache();
        }
    }

    private void syncDefaultAxisWithViewport() {
        if (axisManager == null || viewport == null) {
            return;
        }
        YAxis defaultAxis = axisManager.getDefaultAxis();
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

    // ========== Render Component ==========

    /**
     * Returns the render component for direct access.
     */
    public JComponent getRenderComponent() {
        return renderComponent;
    }

    // ========== Listener Interface ==========

    /**
     * Listener interface for visible range changes.
     */
    public interface RangeChangeListener {
        /**
         * Called when the visible range changes.
         *
         * @param start X-axis start value (timestamp for time-based, index for categorical)
         * @param end X-axis end value (timestamp for time-based, index for categorical)
         */
        void onRangeChanged(long start, long end);
    }
}
