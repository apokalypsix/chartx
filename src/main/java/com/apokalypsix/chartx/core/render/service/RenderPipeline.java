package com.apokalypsix.chartx.core.render.service;

import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.CartesianCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.backend.opengl.GLBackendResourceManager;
import com.apokalypsix.chartx.core.render.backend.opengl.GLRenderDevice;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.core.render.model.RenderLayer;
import com.apokalypsix.chartx.core.data.model.FootprintSeries;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Main rendering pipeline that orchestrates layer-based rendering.
 *
 * <p>Implements GLEventListener to integrate with JOGL's rendering loop.
 * Layers are rendered in z-order from back to front.
 */
public class RenderPipeline implements GLEventListener {

    private static final Logger log = LoggerFactory.getLogger(RenderPipeline.class);

    private final List<RenderLayer> layers = new ArrayList<>();
    private final GLResourceManager resources = new GLResourceManager();
    private final Viewport viewport;
    private final YAxisManager axisManager;
    private final MultiAxisCoordinateSystem coordinates;

    // Backward-compatible single-axis coordinate system
    private final CartesianCoordinateSystem legacyCoordinates;

    // Primary data (for auto-scaling and visible range calculation)
    private OhlcData primaryData;
    private HistogramData histogramData;
    private final List<XyData> overlayData = new ArrayList<>();
    private long barDuration = 60000; // Default 1 minute
    private float scaleFactor = 1.0f; // Display scale factor for HiDPI

    // Category axis for categorical charts (population pyramids, bar charts)
    private CategoryAxis categoryAxis;

    // Advanced series (Footprint and TPO)
    private FootprintSeries footprintSeries;
    private float footprintImbalanceThreshold = 3.0f;
    private boolean footprintHighlightImbalances = true;

    private TPOSeries tpoSeries;
    private boolean tpoShowPOC = true;
    private boolean tpoShowValueArea = true;
    private boolean tpoShowInitialBalance = true;
    private boolean tpoHighlightSinglePrints = true;

    // Background color
    private Color backgroundColor = new Color(20, 22, 25); // Dark background

    private boolean initialized = false;

    // Abstracted rendering API (optional, for v2 renderers)
    private boolean useAbstractedAPI = false;
    private RenderDevice renderDevice;
    private ResourceManager abstractResourceManager;

    /**
     * Creates a render pipeline with the given viewport.
     */
    public RenderPipeline(Viewport viewport) {
        this(viewport, new YAxisManager());
    }

    /**
     * Creates a render pipeline with the given viewport and axis manager.
     */
    public RenderPipeline(Viewport viewport, YAxisManager axisManager) {
        this.viewport = viewport;
        this.axisManager = axisManager;
        this.coordinates = new MultiAxisCoordinateSystem(viewport, axisManager);
        this.legacyCoordinates = new CartesianCoordinateSystem(viewport);
    }

    /**
     * Adds a layer to the pipeline.
     */
    public void addLayer(RenderLayer layer) {
        layers.add(layer);
        layers.sort(Comparator.comparingInt(RenderLayer::getZOrder));

        if (initialized) {
            // Initialize immediately if pipeline is already running
            resources.runOnGLThread(() -> {
                // Will be processed on next frame
            });
        }
    }

    /**
     * Removes a layer from the pipeline.
     */
    public void removeLayer(RenderLayer layer) {
        layers.remove(layer);
    }

    /**
     * Re-sorts layers by z-order. Call this after changing a layer's z-order.
     */
    public void sortLayers() {
        layers.sort(Comparator.comparingInt(RenderLayer::getZOrder));
    }

    /**
     * Sets the primary OHLC data.
     */
    public void setPrimaryData(OhlcData data) {
        this.primaryData = data;
    }

    /**
     * Sets the histogram data.
     */
    public void setHistogramData(HistogramData data) {
        this.histogramData = data;
    }

    /**
     * Sets the category axis for categorical charts.
     *
     * @param axis the category axis
     */
    public void setCategoryAxis(CategoryAxis axis) {
        this.categoryAxis = axis;
    }

    /**
     * Returns the category axis.
     *
     * @return the category axis, or null if not set
     */
    public CategoryAxis getCategoryAxis() {
        return categoryAxis;
    }

    /**
     * Adds overlay data.
     */
    public void addOverlayData(XyData data) {
        overlayData.add(data);
    }

    /**
     * Removes overlay data.
     */
    public void removeOverlayData(XyData data) {
        overlayData.remove(data);
    }

    /**
     * Clears all overlay data.
     */
    public void clearOverlayData() {
        overlayData.clear();
    }

    // ========== Footprint Series ==========

    /**
     * Sets the footprint data series.
     */
    public void setFootprintSeries(FootprintSeries series) {
        this.footprintSeries = series;
    }

    /**
     * Returns the footprint series.
     */
    public FootprintSeries getFootprintSeries() {
        return footprintSeries;
    }

    /**
     * Sets the footprint imbalance threshold.
     */
    public void setFootprintImbalanceThreshold(float threshold) {
        this.footprintImbalanceThreshold = threshold;
    }

    /**
     * Returns the footprint imbalance threshold.
     */
    public float getFootprintImbalanceThreshold() {
        return footprintImbalanceThreshold;
    }

    /**
     * Sets whether to highlight footprint imbalances.
     */
    public void setFootprintHighlightImbalances(boolean highlight) {
        this.footprintHighlightImbalances = highlight;
    }

    /**
     * Returns true if footprint imbalances are highlighted.
     */
    public boolean isFootprintHighlightImbalances() {
        return footprintHighlightImbalances;
    }

    // ========== TPO Series ==========

    /**
     * Sets the TPO data series.
     */
    public void setTPOSeries(TPOSeries series) {
        this.tpoSeries = series;
    }

    /**
     * Returns the TPO series.
     */
    public TPOSeries getTPOSeries() {
        return tpoSeries;
    }

    /**
     * Sets whether to show POC in TPO charts.
     */
    public void setTPOShowPOC(boolean show) {
        this.tpoShowPOC = show;
    }

    /**
     * Returns true if POC is shown in TPO charts.
     */
    public boolean isTPOShowPOC() {
        return tpoShowPOC;
    }

    /**
     * Sets whether to show Value Area in TPO charts.
     */
    public void setTPOShowValueArea(boolean show) {
        this.tpoShowValueArea = show;
    }

    /**
     * Returns true if Value Area is shown in TPO charts.
     */
    public boolean isTPOShowValueArea() {
        return tpoShowValueArea;
    }

    /**
     * Sets whether to show Initial Balance in TPO charts.
     */
    public void setTPOShowInitialBalance(boolean show) {
        this.tpoShowInitialBalance = show;
    }

    /**
     * Returns true if Initial Balance is shown in TPO charts.
     */
    public boolean isTPOShowInitialBalance() {
        return tpoShowInitialBalance;
    }

    /**
     * Sets whether to highlight single prints in TPO charts.
     */
    public void setTPOHighlightSinglePrints(boolean highlight) {
        this.tpoHighlightSinglePrints = highlight;
    }

    /**
     * Returns true if single prints are highlighted in TPO charts.
     */
    public boolean isTPOHighlightSinglePrints() {
        return tpoHighlightSinglePrints;
    }

    /**
     * Sets the bar duration for the primary series.
     */
    public void setBarDuration(long barDuration) {
        this.barDuration = barDuration;
    }

    /**
     * Sets the display scale factor for HiDPI displays.
     *
     * @param scale the scale factor (1.0 for standard, 2.0 for Retina)
     */
    public void setScaleFactor(float scale) {
        this.scaleFactor = Math.max(1.0f, scale);
    }

    /**
     * Gets the display scale factor.
     */
    public float getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Sets the background color.
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    /**
     * Returns the viewport.
     */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Returns the multi-axis coordinate system.
     */
    public MultiAxisCoordinateSystem getCoordinates() {
        return coordinates;
    }

    /**
     * Returns the legacy single-axis coordinate system for backward compatibility.
     */
    public CoordinateSystem getLegacyCoordinates() {
        return legacyCoordinates;
    }

    /**
     * Returns the axis manager.
     */
    public YAxisManager getAxisManager() {
        return axisManager;
    }

    /**
     * Returns the legacy resource manager.
     */
    public GLResourceManager getResources() {
        return resources;
    }

    /**
     * Enables the abstracted rendering API for v2 renderers.
     *
     * <p>When enabled, the pipeline will initialize {@link RenderDevice} and
     * {@link ResourceManager} instances that can be used by renderers implementing
     * {@link com.apokalypsix.chartx.core.render.api.AbstractRenderer}.
     *
     * <p>This must be called before the pipeline is initialized (before the first display).
     *
     * @param enabled true to enable the abstracted API
     */
    public void setUseAbstractedAPI(boolean enabled) {
        if (initialized) {
            throw new IllegalStateException("Cannot change API mode after initialization");
        }
        this.useAbstractedAPI = enabled;
    }

    /**
     * Returns true if the abstracted rendering API is enabled.
     */
    public boolean isUsingAbstractedAPI() {
        return useAbstractedAPI;
    }

    /**
     * Returns the abstracted render device.
     *
     * @return the render device, or null if abstracted API is not enabled
     */
    public RenderDevice getRenderDevice() {
        return renderDevice;
    }

    /**
     * Returns the abstracted resource manager.
     *
     * @return the resource manager, or null if abstracted API is not enabled
     */
    public ResourceManager getAbstractResourceManager() {
        return abstractResourceManager;
    }

    /**
     * Marks all layers as dirty, forcing a full redraw.
     */
    public void markAllDirty() {
        for (RenderLayer layer : layers) {
            layer.markDirty();
        }
    }

    // ========== GLEventListener implementation ==========

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        log.debug("Initializing render pipeline (OpenGL {})", gl.glGetString(GL.GL_VERSION));

        // Enable blending for transparency
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // Enable line smoothing
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);

        // Initialize legacy resource manager
        resources.initialize(gl);

        // Initialize abstracted API if enabled
        if (useAbstractedAPI) {
            log.debug("Initializing abstracted rendering API");
            GLRenderDevice glDevice = new GLRenderDevice(null);
            glDevice.setGL(gl);  // Set the GL context directly
            glDevice.initialize();
            renderDevice = glDevice;

            abstractResourceManager = new GLBackendResourceManager();
            abstractResourceManager.initialize(renderDevice);
        }

        // Initialize all layers
        for (RenderLayer layer : layers) {
            layer.initialize(gl, resources);
        }

        initialized = true;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        log.debug("Disposing render pipeline");

        // Dispose all layers
        for (RenderLayer layer : layers) {
            layer.dispose(gl);
        }

        // Dispose abstracted API if enabled
        if (useAbstractedAPI) {
            if (abstractResourceManager != null) {
                abstractResourceManager.dispose();
                abstractResourceManager = null;
            }
            if (renderDevice != null) {
                renderDevice.dispose();
                renderDevice = null;
            }
        }

        // Dispose legacy resource manager
        resources.dispose(gl);

        initialized = false;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        // Process any pending GL operations
        resources.processPendingOperations(gl);

        // Update GL context and process pending operations for abstracted API
        if (useAbstractedAPI && renderDevice != null) {
            ((GLRenderDevice) renderDevice).setGL(gl);
            if (abstractResourceManager != null) {
                abstractResourceManager.processPendingOperations();
            }
        }

        // Update viewport insets based on visible axes
        updateViewportInsets();

        // Update coordinate system caches
        coordinates.updateCache();
        legacyCoordinates.updateCache();

        // Clear background
        float r = backgroundColor.getRed() / 255f;
        float g = backgroundColor.getGreen() / 255f;
        float b = backgroundColor.getBlue() / 255f;
        gl.glClearColor(r, g, b, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // Create render context for this frame
        RenderContext ctx = createRenderContext(gl);

        // Auto-scale each axis to its associated series
        autoScaleAxes();

        // Also update legacy coordinates after auto-scaling
        coordinates.invalidateCache();
        coordinates.updateCache();
        legacyCoordinates.invalidateCache();
        legacyCoordinates.updateCache();

        // Render each visible layer
        for (RenderLayer layer : layers) {
            if (layer.isVisible()) {
                layer.render(ctx);
                layer.markClean();
            }
        }
    }

    /**
     * Updates viewport insets based on visible axes.
     * Insets are scaled for HiDPI since viewport dimensions are in physical pixels.
     */
    private void updateViewportInsets() {
        axisManager.updateInsets();
        // Use effective scale: if scaleFactor is 1.0 but viewport is large (>1500 pixels),
        // assume HiDPI and use scale 2.0 to ensure proper inset sizing
        float effectiveScale = scaleFactor;
        if (scaleFactor == 1.0f && viewport.getHeight() > 1500) {
            effectiveScale = 2.0f;
        }

        // Start with Y-axis insets
        int leftInset = (int)(axisManager.getLeftInset() * effectiveScale);
        int rightInset = (int)(axisManager.getRightInset() * effectiveScale);
        int topInset = (int)(10 * effectiveScale);
        int bottomInset = (int)(40 * effectiveScale);

        // Add category axis inset if present
        if (categoryAxis != null && categoryAxis.isVisible() && categoryAxis.getCategoryCount() > 0) {
            int categoryAxisWidth = (int)(categoryAxis.getHeight() * effectiveScale);
            switch (categoryAxis.getInternalPosition()) {
                case LEFT:
                    leftInset += categoryAxisWidth;
                    break;
                case RIGHT:
                    rightInset += categoryAxisWidth;
                    break;
                case TOP:
                    topInset += categoryAxisWidth;
                    break;
                case BOTTOM:
                    bottomInset += categoryAxisWidth;
                    break;
            }
        }

        viewport.setInsets(leftInset, rightInset, topInset, bottomInset);
    }

    /**
     * Auto-scales each axis that has auto-scaling enabled.
     */
    private void autoScaleAxes() {
        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();

        for (YAxis axis : axisManager.getAllAxes()) {
            if (!axis.isAutoScale()) {
                continue;
            }

            String axisId = axis.getId();

            // Scale to primary OHLC data if on default axis
            if (YAxis.DEFAULT_AXIS_ID.equals(axisId) && primaryData != null && !primaryData.isEmpty()) {
                axisManager.autoScaleToOHLC(axisId, primaryData, startTime, endTime);
                // Also update legacy viewport price range for backward compatibility
                viewport.setPriceRange(axis.getMinValue(), axis.getMaxValue());
            }

            // Scale to histogram data if assigned to this axis
            if (histogramData != null && !histogramData.isEmpty()) {
                axisManager.autoScaleToHistogram(axisId, histogramData, startTime, endTime);
            }

            // Scale to overlay data - assume all on default axis for simplicity
            for (XyData overlay : overlayData) {
                if (!overlay.isEmpty()) {
                    axisManager.autoScaleToXyData(axisId, overlay, startTime, endTime);
                }
            }
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        // Update viewport dimensions
        viewport.setSize(width, height);
        coordinates.invalidateCache();

        // Update GL viewport
        gl.glViewport(0, 0, width, height);

        // Mark all layers dirty
        markAllDirty();

        log.debug("Viewport resized to {}x{}", width, height);
    }

    /**
     * Creates the render context for the current frame.
     */
    private RenderContext createRenderContext(GL2ES2 gl) {
        RenderContext ctx = new RenderContext(gl, viewport, coordinates, axisManager, resources);
        ctx.setBarDuration(barDuration);
        ctx.setScaleFactor(scaleFactor);
        ctx.setCategoryAxis(categoryAxis);

        // Add abstracted API components if enabled
        if (useAbstractedAPI) {
            ctx.setDevice(renderDevice);
            ctx.setResourceManager(abstractResourceManager);
        }

        // Calculate visible range from primary data
        if (primaryData != null && !primaryData.isEmpty()) {
            int firstVisible = primaryData.indexAtOrAfter(viewport.getStartTime());
            int lastVisible = primaryData.indexAtOrBefore(viewport.getEndTime());
            ctx.setVisibleRange(firstVisible, lastVisible);
        }

        return ctx;
    }

    // ========== Non-GL Rendering (for offscreen backends) ==========

    /**
     * Renders using the abstracted RenderDevice API without requiring GL.
     *
     * <p>This method enables rendering with Vulkan, Metal, DX12, and other
     * non-OpenGL backends. The device must be initialized before calling this.
     *
     * <p>Note: Layers must support the abstracted API (check hasAbstractedAPI()
     * in RenderContext) for full functionality. Layers that only use the legacy
     * GL API will not render correctly with this method.
     *
     * @param device the render device to use
     * @param resourceManager the resource manager for the device
     * @param width the viewport width
     * @param height the viewport height
     */
    public void renderWithDevice(RenderDevice device, ResourceManager resourceManager,
                                  int width, int height) {
        if (device == null || !device.isInitialized()) {
            log.warn("Cannot render: device is null or not initialized");
            return;
        }

        // Store device references for layers
        this.renderDevice = device;
        this.abstractResourceManager = resourceManager;

        // Update viewport size
        viewport.setSize(width, height);
        device.setViewport(0, 0, width, height);

        // Update viewport insets based on visible axes
        updateViewportInsets();

        // Update coordinate system caches
        coordinates.updateCache();
        legacyCoordinates.updateCache();

        // Clear background using abstracted API
        float r = backgroundColor.getRed() / 255f;
        float g = backgroundColor.getGreen() / 255f;
        float b = backgroundColor.getBlue() / 255f;
        device.clearScreen(r, g, b, 1.0f);

        // Create render context for this frame (without GL)
        RenderContext ctx = createRenderContextForDevice(device, resourceManager);

        // Auto-scale each axis to its associated series
        autoScaleAxes();

        // Also update coordinates after auto-scaling
        coordinates.invalidateCache();
        coordinates.updateCache();
        legacyCoordinates.invalidateCache();
        legacyCoordinates.updateCache();

        // Render each visible layer
        for (RenderLayer layer : layers) {
            if (layer.isVisible()) {
                layer.render(ctx);
                layer.markClean();
            }
        }
    }

    /**
     * Creates a render context for non-GL rendering.
     */
    private RenderContext createRenderContextForDevice(RenderDevice device, ResourceManager resourceManager) {
        // Create context with null GL - layers should use hasAbstractedAPI()
        RenderContext ctx = new RenderContext(null, viewport, coordinates, axisManager, resources);
        ctx.setBarDuration(barDuration);
        ctx.setScaleFactor(scaleFactor);
        ctx.setCategoryAxis(categoryAxis);
        ctx.setDevice(device);
        ctx.setResourceManager(resourceManager);

        // Calculate visible range from primary data
        if (primaryData != null && !primaryData.isEmpty()) {
            int firstVisible = primaryData.indexAtOrAfter(viewport.getStartTime());
            int lastVisible = primaryData.indexAtOrBefore(viewport.getEndTime());
            ctx.setVisibleRange(firstVisible, lastVisible);
        }

        return ctx;
    }

    /**
     * Initializes layers for non-GL rendering.
     *
     * <p>This initializes layers using the abstracted API instead of GL.
     * Should be called once before renderWithDevice().
     *
     * @param device the render device
     * @param resourceManager the resource manager
     */
    public void initializeForDevice(RenderDevice device, ResourceManager resourceManager) {
        if (initialized) {
            return;
        }

        this.renderDevice = device;
        this.abstractResourceManager = resourceManager;
        this.useAbstractedAPI = true;

        log.debug("Initializing render pipeline for {} backend", device.getBackendType());

        // Note: Layers still expect GL for initialize() - this is a limitation
        // that will require layer-by-layer migration to fully support non-GL.
        // For now, layers should gracefully handle null GL if they support
        // the abstracted API.

        initialized = true;
    }

    /**
     * Returns true if the pipeline is configured to use a specific RenderDevice.
     */
    public boolean hasConfiguredDevice() {
        return renderDevice != null;
    }
}
