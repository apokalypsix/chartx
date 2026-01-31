package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.overlay.region.PriceRangeRegion;
import com.apokalypsix.chartx.chart.overlay.region.TimeRangeRegion;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for time and price range background regions using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link RegionLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Time range regions are rendered as filled rectangles spanning the full chart height.
 * Price range regions are rendered as filled rectangles spanning the full chart width.
 * Both support optional borders and are rendered behind the main chart data.
 */
public class RegionLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(RegionLayerV2.class);

    /** Z-order for region layer (renders early, behind most content) */
    public static final int Z_ORDER = 50;

    private final List<TimeRangeRegion> timeRegions = new ArrayList<>();
    private final List<PriceRangeRegion> priceRegions = new ArrayList<>();

    // V2 API resources
    private Buffer fillBuffer;
    private Buffer borderBuffer;
    private Shader defaultShader;
    private boolean v2Initialized = false;

    // Reusable arrays
    private float[] fillVertices;
    private float[] borderVertices;
    private int fillCapacity;
    private int borderCapacity;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    // Vertices per region fill (2 triangles = 6 vertices)
    private static final int FILL_VERTICES_PER_REGION = 6;
    // Vertices per region border (4 lines = 8 vertices)
    private static final int BORDER_VERTICES_PER_REGION = 8;

    public RegionLayerV2() {
        super(Z_ORDER);
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("RegionLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for fills (position + color)
        fillBuffer = resources.getOrCreateBuffer("region.fill",
                BufferDescriptor.positionColor2D(64 * FILL_VERTICES_PER_REGION * FLOATS_PER_VERTEX));

        // Create buffer for borders (position + color)
        borderBuffer = resources.getOrCreateBuffer("region.border",
                BufferDescriptor.positionColor2D(64 * BORDER_VERTICES_PER_REGION * FLOATS_PER_VERTEX));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        // Pre-allocate arrays
        fillCapacity = 16;
        borderCapacity = 16;
        fillVertices = new float[fillCapacity * FILL_VERTICES_PER_REGION * FLOATS_PER_VERTEX];
        borderVertices = new float[borderCapacity * BORDER_VERTICES_PER_REGION * FLOATS_PER_VERTEX];

        v2Initialized = true;
        log.debug("RegionLayerV2 V2 resources initialized");
    }

    // ========== Time Region management ==========

    /**
     * Adds a time range region to this layer.
     */
    public void addRegion(TimeRangeRegion region) {
        if (!timeRegions.contains(region)) {
            timeRegions.add(region);
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Batch add multiple time range regions (single repaint at end).
     */
    public void addAllRegions(List<TimeRangeRegion> regions) {
        boolean added = false;
        for (TimeRangeRegion region : regions) {
            if (!timeRegions.contains(region)) {
                timeRegions.add(region);
                added = true;
            }
        }
        if (added) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a time range region from this layer.
     */
    public void removeRegion(TimeRangeRegion region) {
        if (timeRegions.remove(region)) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a time range region by ID.
     */
    public void removeTimeRegion(String id) {
        if (timeRegions.removeIf(r -> r.getId().equals(id))) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Clears all time range regions.
     */
    public void clearTimeRegions() {
        if (!timeRegions.isEmpty()) {
            timeRegions.clear();
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Returns all time range regions.
     */
    public List<TimeRangeRegion> getTimeRegions() {
        return new ArrayList<>(timeRegions);
    }

    /**
     * Returns the time region at the specified timestamp, if any.
     */
    public TimeRangeRegion getTimeRegionAt(long timestamp) {
        for (TimeRangeRegion region : timeRegions) {
            if (region.isVisible() && region.contains(timestamp)) {
                return region;
            }
        }
        return null;
    }

    // ========== Price Region management ==========

    /**
     * Adds a price range region to this layer.
     */
    public void addPriceRegion(PriceRangeRegion region) {
        if (!priceRegions.contains(region)) {
            priceRegions.add(region);
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Batch add multiple price range regions (single repaint at end).
     */
    public void addAllPriceRegions(List<PriceRangeRegion> regions) {
        boolean added = false;
        for (PriceRangeRegion region : regions) {
            if (!priceRegions.contains(region)) {
                priceRegions.add(region);
                added = true;
            }
        }
        if (added) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a price range region from this layer.
     */
    public void removePriceRegion(PriceRangeRegion region) {
        if (priceRegions.remove(region)) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a price range region by ID.
     */
    public void removePriceRegion(String id) {
        if (priceRegions.removeIf(r -> r.getId().equals(id))) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Clears all price range regions.
     */
    public void clearPriceRegions() {
        if (!priceRegions.isEmpty()) {
            priceRegions.clear();
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Returns all price range regions.
     */
    public List<PriceRangeRegion> getPriceRegions() {
        return new ArrayList<>(priceRegions);
    }

    /**
     * Returns the price region at the specified price, if any.
     */
    public PriceRangeRegion getPriceRegionAt(double price) {
        for (PriceRangeRegion region : priceRegions) {
            if (region.isVisible() && region.contains(price)) {
                return region;
            }
        }
        return null;
    }

    // ========== Combined operations ==========

    /**
     * Clears all regions (both time and price).
     */
    public void clearAllRegions() {
        if (!timeRegions.isEmpty() || !priceRegions.isEmpty()) {
            timeRegions.clear();
            priceRegions.clear();
            markDirty();
            requestRepaint();
        }
    }

    /**
     * @deprecated Use {@link #getTimeRegions()} instead
     */
    @Deprecated
    public List<TimeRangeRegion> getRegions() {
        return getTimeRegions();
    }

    /**
     * @deprecated Use {@link #removeTimeRegion(String)} instead
     */
    @Deprecated
    public void removeRegion(String id) {
        removeTimeRegion(id);
    }

    /**
     * @deprecated Use {@link #clearTimeRegions()} instead
     */
    @Deprecated
    public void clearRegions() {
        clearTimeRegions();
    }

    /**
     * @deprecated Use {@link #getTimeRegionAt(long)} instead
     */
    @Deprecated
    public TimeRangeRegion getRegionAt(long timestamp) {
        return getTimeRegionAt(timestamp);
    }

    // ========== Rendering ==========

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("RegionLayerV2 requires abstracted API - skipping render");
            return;
        }

        if (timeRegions.isEmpty() && priceRegions.isEmpty()) {
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinates();

        // Filter visible regions
        List<TimeRangeRegion> visibleTimeRegions = getVisibleTimeRegions(viewport);
        List<PriceRangeRegion> visiblePriceRegions = getVisiblePriceRegions(viewport);

        if (visibleTimeRegions.isEmpty() && visiblePriceRegions.isEmpty()) {
            return;
        }

        // Ensure capacity
        int totalRegions = visibleTimeRegions.size() + visiblePriceRegions.size();
        ensureCapacity(totalRegions);

        // Build vertex data for both region types
        int fillFloatCount = 0;
        int borderFloatCount = 0;

        fillFloatCount = buildTimeRegionFillVertices(visibleTimeRegions, coords, viewport, fillFloatCount);
        fillFloatCount = buildPriceRegionFillVertices(visiblePriceRegions, coords, viewport, fillFloatCount);

        borderFloatCount = buildTimeRegionBorderVertices(visibleTimeRegions, coords, viewport, borderFloatCount);
        borderFloatCount = buildPriceRegionBorderVertices(visiblePriceRegions, coords, viewport, borderFloatCount);

        // Get shader
        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw fills
        if (fillFloatCount > 0) {
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders
        if (borderFloatCount > 0) {
            borderBuffer.upload(borderVertices, 0, borderFloatCount);
            borderBuffer.draw(DrawMode.LINES);
        }

        defaultShader.unbind();
    }

    private List<TimeRangeRegion> getVisibleTimeRegions(Viewport viewport) {
        List<TimeRangeRegion> visible = new ArrayList<>();
        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();

        for (TimeRangeRegion region : timeRegions) {
            if (region.isVisible() && region.overlaps(startTime, endTime)) {
                visible.add(region);
            }
        }
        return visible;
    }

    private List<PriceRangeRegion> getVisiblePriceRegions(Viewport viewport) {
        List<PriceRangeRegion> visible = new ArrayList<>();
        double minPrice = viewport.getMinPrice();
        double maxPrice = viewport.getMaxPrice();

        for (PriceRangeRegion region : priceRegions) {
            if (region.isVisible() && region.overlaps(minPrice, maxPrice)) {
                visible.add(region);
            }
        }
        return visible;
    }

    private int buildTimeRegionFillVertices(List<TimeRangeRegion> visibleRegions,
                                             CoordinateSystem coords, Viewport viewport, int floatIndex) {
        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();

        for (TimeRangeRegion region : visibleRegions) {
            Color fillColor = region.getFillColor();
            if (fillColor == null) {
                continue;
            }

            float left = (float) coords.xValueToScreenX(region.getStartTime());
            float right = (float) coords.xValueToScreenX(region.getEndTime());

            // Clamp to visible area
            left = Math.max(left, viewport.getLeftInset());
            right = Math.min(right, viewport.getWidth() - viewport.getRightInset());

            if (left >= right) {
                continue;
            }

            float r = fillColor.getRed() / 255f;
            float g = fillColor.getGreen() / 255f;
            float b = fillColor.getBlue() / 255f;
            float a = fillColor.getAlpha() / 255f;

            // Triangle 1: top-left, bottom-left, bottom-right
            floatIndex = addVertex(fillVertices, floatIndex, left, chartTop, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, left, chartBottom, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, right, chartBottom, r, g, b, a);

            // Triangle 2: top-left, bottom-right, top-right
            floatIndex = addVertex(fillVertices, floatIndex, left, chartTop, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, right, chartBottom, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, right, chartTop, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildPriceRegionFillVertices(List<PriceRangeRegion> visibleRegions,
                                              CoordinateSystem coords, Viewport viewport, int floatIndex) {
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();

        for (PriceRangeRegion region : visibleRegions) {
            Color fillColor = region.getFillColor();
            if (fillColor == null) {
                continue;
            }

            // Note: Y axis is inverted (higher price = lower screen Y)
            float top = (float) coords.yValueToScreenY(region.getMaxPrice());
            float bottom = (float) coords.yValueToScreenY(region.getMinPrice());

            // Clamp to visible area
            top = Math.max(top, viewport.getTopInset());
            bottom = Math.min(bottom, viewport.getHeight() - viewport.getBottomInset());

            if (top >= bottom) {
                continue;
            }

            float r = fillColor.getRed() / 255f;
            float g = fillColor.getGreen() / 255f;
            float b = fillColor.getBlue() / 255f;
            float a = fillColor.getAlpha() / 255f;

            // Triangle 1: top-left, bottom-left, bottom-right
            floatIndex = addVertex(fillVertices, floatIndex, chartLeft, top, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, chartLeft, bottom, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, chartRight, bottom, r, g, b, a);

            // Triangle 2: top-left, bottom-right, top-right
            floatIndex = addVertex(fillVertices, floatIndex, chartLeft, top, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, chartRight, bottom, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, chartRight, top, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildTimeRegionBorderVertices(List<TimeRangeRegion> visibleRegions,
                                               CoordinateSystem coords, Viewport viewport, int floatIndex) {
        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();

        for (TimeRangeRegion region : visibleRegions) {
            Color borderColor = region.getBorderColor();
            if (borderColor == null) {
                continue;
            }

            float left = (float) coords.xValueToScreenX(region.getStartTime());
            float right = (float) coords.xValueToScreenX(region.getEndTime());

            float r = borderColor.getRed() / 255f;
            float g = borderColor.getGreen() / 255f;
            float b = borderColor.getBlue() / 255f;
            float a = borderColor.getAlpha() / 255f;

            // Left vertical line (if visible)
            if (left >= viewport.getLeftInset() && left <= viewport.getWidth() - viewport.getRightInset()) {
                floatIndex = addVertex(borderVertices, floatIndex, left, chartTop, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, left, chartBottom, r, g, b, a);
            }

            // Right vertical line (if visible)
            if (right >= viewport.getLeftInset() && right <= viewport.getWidth() - viewport.getRightInset()) {
                floatIndex = addVertex(borderVertices, floatIndex, right, chartTop, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, chartBottom, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildPriceRegionBorderVertices(List<PriceRangeRegion> visibleRegions,
                                                CoordinateSystem coords, Viewport viewport, int floatIndex) {
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();

        for (PriceRangeRegion region : visibleRegions) {
            Color borderColor = region.getBorderColor();
            if (borderColor == null) {
                continue;
            }

            // Note: Y axis is inverted (higher price = lower screen Y)
            float top = (float) coords.yValueToScreenY(region.getMaxPrice());
            float bottom = (float) coords.yValueToScreenY(region.getMinPrice());

            float r = borderColor.getRed() / 255f;
            float g = borderColor.getGreen() / 255f;
            float b = borderColor.getBlue() / 255f;
            float a = borderColor.getAlpha() / 255f;

            // Top horizontal line (if visible)
            if (top >= viewport.getTopInset() && top <= viewport.getHeight() - viewport.getBottomInset()) {
                floatIndex = addVertex(borderVertices, floatIndex, chartLeft, top, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, chartRight, top, r, g, b, a);
            }

            // Bottom horizontal line (if visible)
            if (bottom >= viewport.getTopInset() && bottom <= viewport.getHeight() - viewport.getBottomInset()) {
                floatIndex = addVertex(borderVertices, floatIndex, chartLeft, bottom, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, chartRight, bottom, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int addVertex(float[] vertices, int index, float x, float y,
                          float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }

    private void ensureCapacity(int regionCount) {
        int requiredFillFloats = regionCount * FILL_VERTICES_PER_REGION * FLOATS_PER_VERTEX;
        int requiredBorderFloats = regionCount * BORDER_VERTICES_PER_REGION * FLOATS_PER_VERTEX;

        if (fillVertices == null || requiredFillFloats > fillVertices.length) {
            fillCapacity = regionCount + regionCount / 2;
            fillVertices = new float[fillCapacity * FILL_VERTICES_PER_REGION * FLOATS_PER_VERTEX];
        }

        if (borderVertices == null || requiredBorderFloats > borderVertices.length) {
            borderCapacity = regionCount + regionCount / 2;
            borderVertices = new float[borderCapacity * BORDER_VERTICES_PER_REGION * FLOATS_PER_VERTEX];
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        // V2 resources are managed by ResourceManager
        v2Initialized = false;
    }

    /**
     * Disposes V2 resources.
     * Call this when the RenderContext is available during cleanup.
     */
    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            ResourceManager resources = ctx.getResourceManager();
            if (resources != null) {
                resources.disposeBuffer("region.fill");
                resources.disposeBuffer("region.border");
            }
            fillBuffer = null;
            borderBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }
}
