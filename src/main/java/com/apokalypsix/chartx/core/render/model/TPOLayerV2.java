package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.data.model.TPOProfile;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for TPO (Time Price Opportunity) / Market Profile charts
 * using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link TPOLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>This layer displays market profile data showing price distribution
 * over trading sessions:
 * <ul>
 *   <li>TPO blocks for each time period at each price level</li>
 *   <li>Point of Control (POC) - highest activity price level</li>
 *   <li>Value Area - range containing 70% of trading activity</li>
 *   <li>Initial Balance - first hour's trading range</li>
 *   <li>Single prints - isolated price levels</li>
 * </ul>
 *
 * <h2>Display Modes</h2>
 * <p>Currently renders BLOCKS mode. Text letters (A, B, C...) are rendered
 * separately and not included in this V2 layer.
 *
 * <h2>Overlay Mode</h2>
 * <p>When overlay mode is enabled, TPO renders on top of candlesticks with
 * semi-transparency, allowing both chart types to be visible simultaneously.
 */
public class TPOLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(TPOLayerV2.class);

    /** Z-order for TPO layer (renders before candles, after volume profile) */
    public static final int Z_ORDER = 145;

    /** Z-order for TPO overlay mode (renders after candles) */
    public static final int Z_ORDER_OVERLAY = 250;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    // Vertices per block (2 triangles = 6 vertices)
    private static final int VERTICES_PER_BLOCK = 6;

    // V2 API resources
    private Buffer blockBuffer;      // For TPO blocks
    private Buffer highlightBuffer;  // For POC, VA, IB, single print highlights
    private Shader defaultShader;
    private boolean v2Initialized = false;

    // Reusable arrays for vertex data (avoid allocation during render)
    private float[] blockVertices;
    private float[] highlightVertices;
    private int blockCapacity;
    private int highlightCapacity;

    // Data
    private TPOSeries series;

    // Colors - period colors cycle through these
    private Color[] periodColors = {
            new Color(100, 149, 237),  // Cornflower blue
            new Color(144, 238, 144),  // Light green
            new Color(255, 182, 193),  // Light pink
            new Color(255, 218, 185),  // Peach
            new Color(221, 160, 221),  // Plum
            new Color(176, 224, 230),  // Powder blue
            new Color(255, 255, 224),  // Light yellow
            new Color(230, 230, 250),  // Lavender
    };

    // Note: Colors are now stored on TPOSeries and read at render time

    // Block sizing
    private float blockWidth = 8.0f;
    private float blockGap = 1.0f;

    /**
     * Creates a TPO layer using V2 renderers.
     */
    public TPOLayerV2() {
        super(Z_ORDER);
        // Pre-allocate vertex arrays
        blockCapacity = 1024;
        highlightCapacity = 256;
        blockVertices = new float[blockCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX];
        highlightVertices = new float[highlightCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX];
    }

    /**
     * Creates a TPO layer with a custom z-order.
     *
     * @param zOrder the z-order for this layer
     */
    public TPOLayerV2(int zOrder) {
        super(zOrder);
        blockCapacity = 1024;
        highlightCapacity = 256;
        blockVertices = new float[blockCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX];
        highlightVertices = new float[highlightCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX];
    }

    /**
     * Sets the TPO series to render.
     * Repaints automatically.
     *
     * @param series the TPO series
     */
    public void setSeries(TPOSeries series) {
        this.series = series;
        updateZOrderFromSeries();
        markDirty();
        requestRepaint();
    }

    /**
     * Returns the current series.
     */
    public TPOSeries getSeries() {
        return series;
    }

    // ========== Display settings (delegated to series) ==========

    /**
     * Sets whether to highlight the Point of Control level.
     * @deprecated Use {@link TPOSeries#setShowPOC(boolean)} instead
     */
    @Deprecated
    public void setShowPOC(boolean show) {
        if (series != null) {
            series.setShowPOC(show);
        }
        markDirty();
    }

    /**
     * Sets whether to show the value area shading.
     * @deprecated Use {@link TPOSeries#setShowValueArea(boolean)} instead
     */
    @Deprecated
    public void setShowValueArea(boolean show) {
        if (series != null) {
            series.setShowValueArea(show);
        }
        markDirty();
    }

    /**
     * Sets whether to show the initial balance range.
     * @deprecated Use {@link TPOSeries#setShowInitialBalance(boolean)} instead
     */
    @Deprecated
    public void setShowInitialBalance(boolean show) {
        if (series != null) {
            series.setShowInitialBalance(show);
        }
        markDirty();
    }

    /**
     * Sets whether to highlight single prints.
     * @deprecated Use {@link TPOSeries#setHighlightSinglePrints(boolean)} instead
     */
    @Deprecated
    public void setHighlightSinglePrints(boolean highlight) {
        if (series != null) {
            series.setHighlightSinglePrints(highlight);
        }
        markDirty();
    }

    /**
     * Sets overlay mode - renders TPO on top of candlesticks with semi-transparency.
     * @deprecated Use {@link TPOSeries#setOverlayMode(boolean)} instead
     */
    @Deprecated
    public void setOverlayMode(boolean overlay) {
        if (series != null) {
            series.setOverlayMode(overlay);
        }
        updateZOrderFromSeries();
        markDirty();
    }

    /**
     * Sets the opacity for TPO rendering.
     * @deprecated Use {@link TPOSeries#setOpacity(float)} instead
     */
    @Deprecated
    public void setOpacity(float opacity) {
        if (series != null) {
            series.setOpacity(opacity);
        }
        markDirty();
    }

    /**
     * Updates z-order based on series overlay mode.
     */
    private void updateZOrderFromSeries() {
        if (series != null && series.isOverlayMode()) {
            setZOrder(Z_ORDER_OVERLAY);
        } else {
            setZOrder(Z_ORDER);
        }
    }

    /**
     * Sets the colors used for TPO periods.
     *
     * @param colors array of colors that cycle for each period
     */
    public void setPeriodColors(Color[] colors) {
        this.periodColors = colors;
        markDirty();
    }

    /**
     * Sets the POC highlight color.
     * @deprecated Use {@link TPOSeries#setPocColor(Color)} instead
     */
    @Deprecated
    public void setPOCColor(Color color) {
        if (series != null) {
            series.setPocColor(color);
        }
        markDirty();
    }

    /**
     * Sets the value area background color.
     * @deprecated Use {@link TPOSeries#setValueAreaColor(Color)} instead
     */
    @Deprecated
    public void setValueAreaColor(Color color) {
        if (series != null) {
            series.setValueAreaColor(color);
        }
        markDirty();
    }

    /**
     * Sets the initial balance block color.
     * @deprecated Use {@link TPOSeries#setIBColor(Color)} instead
     */
    @Deprecated
    public void setIBBlockColor(Color color) {
        if (series != null) {
            series.setIBColor(color);
        }
        markDirty();
    }

    /**
     * Sets the single print highlight color.
     * @deprecated Use {@link TPOSeries#setSinglePrintColor(Color)} instead
     */
    @Deprecated
    public void setSinglePrintColor(Color color) {
        if (series != null) {
            series.setSinglePrintColor(color);
        }
        markDirty();
    }

    /**
     * Sets the block width in pixels.
     *
     * @param width the block width
     */
    public void setBlockWidth(float width) {
        this.blockWidth = width;
        markDirty();
    }

    /**
     * Sets the gap between blocks in pixels.
     *
     * @param gap the gap between blocks
     */
    public void setBlockGap(float gap) {
        this.blockGap = gap;
        markDirty();
    }

    // ========== RenderLayer implementation ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("TPOLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for TPO blocks (position + color)
        int blockBufferCapacity = blockCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX;
        blockBuffer = resources.getOrCreateBuffer("tpo.blocks",
                BufferDescriptor.positionColor2D(blockBufferCapacity));

        // Create buffer for highlights (position + color)
        int highlightBufferCapacity = highlightCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX;
        highlightBuffer = resources.getOrCreateBuffer("tpo.highlights",
                BufferDescriptor.positionColor2D(highlightBufferCapacity));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("TPOLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("TPOLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        if (series == null || series.isEmpty()) {
            return;
        }

        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinatesForData(series);

        // Get visible range
        int firstIdx = findFirstVisibleIndex(viewport);
        int lastIdx = findLastVisibleIndex(viewport);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        RenderDevice device = ctx.getDevice();

        // Render backgrounds and highlights first (behind blocks)
        if (series.isShowValueArea()) {
            renderValueAreaBackgrounds(ctx, coords, firstIdx, lastIdx, device);
        }

        // Note: Initial Balance is now shown as white-colored blocks, not as a background

        if (series.isHighlightSinglePrints()) {
            renderSinglePrints(ctx, coords, firstIdx, lastIdx, device);
        }

        if (series.isShowPOC()) {
            renderPOCHighlights(ctx, coords, firstIdx, lastIdx, device);
        }

        // Render VAH/VAL lines
        if (series.isShowVAH() || series.isShowVAL()) {
            renderVAHVALLines(ctx, coords, firstIdx, lastIdx, device);
        }

        // Render TPO blocks
        renderBlocks(ctx, coords, firstIdx, lastIdx, device);
    }

    private void renderBlocks(RenderContext ctx, CoordinateSystem coords,
                               int firstIdx, int lastIdx, RenderDevice device) {
        // Estimate capacity needed
        int estimatedBlocks = 0;
        TPOProfile[] profiles = series.getProfilesArray();
        for (int i = firstIdx; i <= lastIdx; i++) {
            if (profiles[i] != null) {
                estimatedBlocks += profiles[i].getLevelCount() * profiles[i].getPeriodCount();
            }
        }
        ensureBlockCapacity(estimatedBlocks);

        long[] sessionStarts = series.getSessionStartsArray();
        float tickSize = series.getTickSize();

        int floatIdx = 0;

        for (int profileIdx = firstIdx; profileIdx <= lastIdx; profileIdx++) {
            TPOProfile profile = profiles[profileIdx];
            if (profile == null) continue;

            // Calculate profile X position
            float profileStartX = (float) coords.xValueToScreenX(sessionStarts[profileIdx]);
            float profileWidth = (float) coords.xValueToScreenX(profile.getSessionEnd()) - profileStartX;

            // Distribute blocks across profile width
            int periodCount = profile.getPeriodCount();
            float effectiveBlockWidth = (profileWidth - blockGap * periodCount) / periodCount;

            List<Float> priceLevels = profile.getPriceLevels();

            for (float price : priceLevels) {
                float priceY = (float) coords.yValueToScreenY(price);
                float tickHeight = (float) Math.abs(coords.yValueToScreenY(price + tickSize) -
                        coords.yValueToScreenY(price));

                long tpoMask = profile.getTPOMaskAt(price);

                // Draw block for each period that touched this price
                float blockX = profileStartX + blockGap;
                int ibPeriodCount = profile.getIBPeriods();
                boolean showIB = series.isShowInitialBalance();
                boolean showPOC = series.isShowPOC();
                float opacity = series.getOpacity();
                Color ibColor = series.getIBColor();
                Color pocColor = series.getPocColor();
                for (int period = 0; period < periodCount; period++) {
                    if ((tpoMask & (1L << period)) != 0) {
                        Color color = periodColors[period % periodColors.length];

                        // Initial Balance periods get IB color
                        if (showIB && period < ibPeriodCount) {
                            color = ibColor;
                        }
                        // POC overrides IB color
                        else if (showPOC && Math.abs(price - profile.getPOC()) < tickSize / 2) {
                            color = pocColor;
                        }

                        float x1 = blockX;
                        float x2 = blockX + effectiveBlockWidth;
                        float y1 = priceY - tickHeight / 2 + 0.5f;
                        float y2 = priceY + tickHeight / 2 - 0.5f;

                        floatIdx = addQuad(blockVertices, floatIdx, x1, y1, x2, y2,
                                color.getRed() / 255f, color.getGreen() / 255f,
                                color.getBlue() / 255f, opacity);
                    }
                    blockX += effectiveBlockWidth + blockGap;
                }
            }
        }

        float opacity = series.getOpacity();
        if (floatIdx > 0) {
            // Enable blending if opacity < 1
            if (opacity < 1.0f) {
                device.setBlendMode(BlendMode.ALPHA);
            }

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            blockBuffer.upload(blockVertices, 0, floatIdx);
            blockBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();

            if (opacity < 1.0f) {
                device.setBlendMode(BlendMode.NONE);
            }
        }
    }

    private void renderValueAreaBackgrounds(RenderContext ctx, CoordinateSystem coords,
                                             int firstIdx, int lastIdx, RenderDevice device) {
        ensureHighlightCapacity(lastIdx - firstIdx + 1);

        TPOProfile[] profiles = series.getProfilesArray();
        long[] sessionStarts = series.getSessionStartsArray();

        int floatIdx = 0;

        for (int profileIdx = firstIdx; profileIdx <= lastIdx; profileIdx++) {
            TPOProfile profile = profiles[profileIdx];
            if (profile == null) continue;

            float vah = profile.getValueAreaHigh();
            float val = profile.getValueAreaLow();
            if (Float.isNaN(vah) || Float.isNaN(val)) continue;

            float x1 = (float) coords.xValueToScreenX(sessionStarts[profileIdx]);
            float x2 = (float) coords.xValueToScreenX(profile.getSessionEnd());
            float y1 = (float) coords.yValueToScreenY(vah);
            float y2 = (float) coords.yValueToScreenY(val);

            Color vaColor = series.getValueAreaColor();
            floatIdx = addQuad(highlightVertices, floatIdx, x1, y1, x2, y2,
                    vaColor.getRed() / 255f, vaColor.getGreen() / 255f,
                    vaColor.getBlue() / 255f, vaColor.getAlpha() / 255f);
        }

        if (floatIdx > 0) {
            device.setBlendMode(BlendMode.ALPHA);

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            highlightBuffer.upload(highlightVertices, 0, floatIdx);
            highlightBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
            device.setBlendMode(BlendMode.NONE);
        }
    }

    private void renderPOCHighlights(RenderContext ctx, CoordinateSystem coords,
                                      int firstIdx, int lastIdx, RenderDevice device) {
        TPOProfile[] profiles = series.getProfilesArray();
        long[] sessionStarts = series.getSessionStartsArray();
        float tickSize = series.getTickSize();

        int floatIdx = 0;

        for (int profileIdx = firstIdx; profileIdx <= lastIdx; profileIdx++) {
            TPOProfile profile = profiles[profileIdx];
            if (profile == null) continue;

            float poc = profile.getPOC();
            if (Float.isNaN(poc)) continue;

            float x1 = (float) coords.xValueToScreenX(sessionStarts[profileIdx]);
            float x2 = (float) coords.xValueToScreenX(profile.getSessionEnd());
            float priceY = (float) coords.yValueToScreenY(poc);
            float tickHeight = (float) Math.abs(coords.yValueToScreenY(poc + tickSize) -
                    coords.yValueToScreenY(poc));

            Color pocColor = series.getPocColor();
            floatIdx = addQuad(highlightVertices, floatIdx,
                    x1, priceY - tickHeight / 2, x2, priceY + tickHeight / 2,
                    pocColor.getRed() / 255f, pocColor.getGreen() / 255f,
                    pocColor.getBlue() / 255f, pocColor.getAlpha() / 255f * 0.3f);
        }

        if (floatIdx > 0) {
            device.setBlendMode(BlendMode.ALPHA);

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            highlightBuffer.upload(highlightVertices, 0, floatIdx);
            highlightBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
            device.setBlendMode(BlendMode.NONE);
        }
    }

    private void renderSinglePrints(RenderContext ctx, CoordinateSystem coords,
                                     int firstIdx, int lastIdx, RenderDevice device) {
        TPOProfile[] profiles = series.getProfilesArray();
        long[] sessionStarts = series.getSessionStartsArray();
        float tickSize = series.getTickSize();

        // Estimate single prints count for capacity
        int estimatedSingles = 0;
        for (int i = firstIdx; i <= lastIdx; i++) {
            if (profiles[i] != null) {
                estimatedSingles += profiles[i].getSinglePrints().size();
            }
        }
        ensureHighlightCapacity(estimatedSingles);

        int floatIdx = 0;

        for (int profileIdx = firstIdx; profileIdx <= lastIdx; profileIdx++) {
            TPOProfile profile = profiles[profileIdx];
            if (profile == null) continue;

            List<Float> singles = profile.getSinglePrints();
            if (singles.isEmpty()) continue;

            float x1 = (float) coords.xValueToScreenX(sessionStarts[profileIdx]);
            float x2 = (float) coords.xValueToScreenX(profile.getSessionEnd());

            Color spColor = series.getSinglePrintColor();
            for (float price : singles) {
                float priceY = (float) coords.yValueToScreenY(price);
                float tickHeight = (float) Math.abs(coords.yValueToScreenY(price + tickSize) -
                        coords.yValueToScreenY(price));

                floatIdx = addQuad(highlightVertices, floatIdx,
                        x1, priceY - tickHeight / 2, x2, priceY + tickHeight / 2,
                        spColor.getRed() / 255f, spColor.getGreen() / 255f,
                        spColor.getBlue() / 255f, spColor.getAlpha() / 255f);
            }
        }

        if (floatIdx > 0) {
            device.setBlendMode(BlendMode.ALPHA);

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            highlightBuffer.upload(highlightVertices, 0, floatIdx);
            highlightBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
            device.setBlendMode(BlendMode.NONE);
        }
    }

    private void renderVAHVALLines(RenderContext ctx, CoordinateSystem coords,
                                    int firstIdx, int lastIdx, RenderDevice device) {
        TPOProfile[] profiles = series.getProfilesArray();
        long[] sessionStarts = series.getSessionStartsArray();

        boolean showVAH = series.isShowVAH();
        boolean showVAL = series.isShowVAL();

        // Estimate capacity: 2 lines per profile, potentially many segments for dashed lines
        int estimatedSegments = (lastIdx - firstIdx + 1) * 50; // Allow for dashed lines
        ensureHighlightCapacity(estimatedSegments);

        int floatIdx = 0;

        for (int profileIdx = firstIdx; profileIdx <= lastIdx; profileIdx++) {
            TPOProfile profile = profiles[profileIdx];
            if (profile == null) continue;

            float vah = profile.getValueAreaHigh();
            float val = profile.getValueAreaLow();

            float x1 = (float) coords.xValueToScreenX(sessionStarts[profileIdx]);
            float x2 = (float) coords.xValueToScreenX(profile.getSessionEnd());

            // Render VAH line
            if (showVAH && !Float.isNaN(vah)) {
                float y = (float) coords.yValueToScreenY(vah);
                Color color = series.getVahColor();
                TPOSeries.LineType lineType = series.getVahLineType();
                float thickness = series.getVahLineThickness().getWidth();

                floatIdx = addLine(highlightVertices, floatIdx, x1, y, x2, y, thickness,
                        color.getRed() / 255f, color.getGreen() / 255f,
                        color.getBlue() / 255f, color.getAlpha() / 255f,
                        lineType.getDashPattern());
            }

            // Render VAL line
            if (showVAL && !Float.isNaN(val)) {
                float y = (float) coords.yValueToScreenY(val);
                Color color = series.getValColor();
                TPOSeries.LineType lineType = series.getValLineType();
                float thickness = series.getValLineThickness().getWidth();

                floatIdx = addLine(highlightVertices, floatIdx, x1, y, x2, y, thickness,
                        color.getRed() / 255f, color.getGreen() / 255f,
                        color.getBlue() / 255f, color.getAlpha() / 255f,
                        lineType.getDashPattern());
            }
        }

        if (floatIdx > 0) {
            device.setBlendMode(BlendMode.ALPHA);

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            highlightBuffer.upload(highlightVertices, 0, floatIdx);
            highlightBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
            device.setBlendMode(BlendMode.NONE);
        }
    }

    /**
     * Adds a horizontal line as a thin quad, with optional dash pattern.
     */
    private int addLine(float[] vertices, int index,
                        float x1, float y, float x2, float y2Ignored,
                        float thickness, float r, float g, float b, float a,
                        float[] dashPattern) {
        float halfThickness = thickness / 2;

        if (dashPattern == null) {
            // Solid line - single quad
            return addQuad(vertices, index, x1, y - halfThickness, x2, y + halfThickness, r, g, b, a);
        }

        // Dashed/dotted line - multiple segments
        float totalLength = x2 - x1;
        float patternLength = 0;
        for (float d : dashPattern) {
            patternLength += d;
        }

        float x = x1;
        int patternIdx = 0;
        boolean drawing = true; // Start by drawing

        while (x < x2) {
            float segmentLength = dashPattern[patternIdx % dashPattern.length];
            float segmentEnd = Math.min(x + segmentLength, x2);

            if (drawing) {
                index = addQuad(vertices, index, x, y - halfThickness, segmentEnd, y + halfThickness, r, g, b, a);
            }

            x = segmentEnd;
            patternIdx++;
            drawing = !drawing;
        }

        return index;
    }

    private int findFirstVisibleIndex(Viewport viewport) {
        long startTime = viewport.getStartTime();
        int idx = series.indexAtOrBefore(startTime);
        // If a profile contains the start time, use it
        if (idx >= 0) {
            TPOProfile profile = series.getProfile(idx);
            if (profile.getSessionEnd() >= startTime) {
                return idx;
            }
        }
        return series.indexAtOrAfter(startTime);
    }

    private int findLastVisibleIndex(Viewport viewport) {
        long endTime = viewport.getEndTime();
        return series.indexAtOrBefore(endTime);
    }

    private int addQuad(float[] vertices, int index,
                        float x1, float y1, float x2, float y2,
                        float r, float g, float b, float a) {
        // Triangle 1
        index = addVertex(vertices, index, x1, y1, r, g, b, a);
        index = addVertex(vertices, index, x2, y1, r, g, b, a);
        index = addVertex(vertices, index, x2, y2, r, g, b, a);

        // Triangle 2
        index = addVertex(vertices, index, x1, y1, r, g, b, a);
        index = addVertex(vertices, index, x2, y2, r, g, b, a);
        index = addVertex(vertices, index, x1, y2, r, g, b, a);

        return index;
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

    private void ensureBlockCapacity(int blockCount) {
        int required = blockCount * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX;
        if (required > blockVertices.length) {
            blockCapacity = blockCount + blockCount / 2;
            blockVertices = new float[blockCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX];
        }
    }

    private void ensureHighlightCapacity(int highlightCount) {
        int required = highlightCount * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX * 3; // Multiple highlight types
        if (required > highlightVertices.length) {
            highlightCapacity = highlightCount * 3 + highlightCount;
            highlightVertices = new float[highlightCapacity * VERTICES_PER_BLOCK * FLOATS_PER_VERTEX];
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
                resources.disposeBuffer("tpo.blocks");
                resources.disposeBuffer("tpo.highlights");
            }
            blockBuffer = null;
            highlightBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }
}
