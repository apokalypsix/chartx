package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.data.model.FootprintBar;
import com.apokalypsix.chartx.core.data.model.FootprintLevel;
import com.apokalypsix.chartx.core.data.model.FootprintSeries;
import com.apokalypsix.chartx.core.data.model.Imbalance;
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
 * V2 render layer for footprint (order flow) charts using the abstracted rendering API.
 *
 * <p>This layer displays bid/ask volume at each price level within each bar:
 * <ul>
 *   <li>Bid x Ask format showing volume on each side</li>
 *   <li>Delta mode showing buy-sell difference</li>
 *   <li>Volume bars mode</li>
 *   <li>Volume profile mode per bar</li>
 *   <li>Imbalance highlighting for supply/demand zones</li>
 * </ul>
 *
 * <p>This is the V2 version that uses backend-agnostic rendering interfaces
 * instead of direct GL calls.
 */
public class FootprintLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(FootprintLayerV2.class);

    /** Z-order for footprint layer (renders after candles to overlay) */
    public static final int Z_ORDER = 155;

    /**
     * Display mode for footprint rendering.
     */
    public enum DisplayMode {
        /** Show bid on left, ask on right as volume bars */
        BID_ASK,
        /** Show delta value (ask - bid) as single bar */
        DELTA,
        /** Show total volume as bars */
        VOLUME,
        /** Show mini volume profile per bar */
        PROFILE
    }

    // Series to render
    private FootprintSeries series;

    // V2 API resources
    private Buffer volumeBuffer;      // For volume bars (position + color)
    private Buffer imbalanceBuffer;   // For imbalance highlights (position + color)
    private Shader defaultShader;     // Position + per-vertex color
    private boolean v2Initialized = false;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;

    // Reusable vertex arrays (avoid allocation in render loop)
    private float[] volumeVertices;
    private float[] imbalanceVertices;
    private int volumeCapacity;
    private int imbalanceCapacity;

    // Configuration
    private DisplayMode displayMode = DisplayMode.BID_ASK;
    private boolean highlightImbalances = true;
    private float imbalanceThreshold = 3.0f;
    private boolean showPOC = true;

    // Colors
    private Color bidColor = new Color(214, 69, 65);           // Red for sellers
    private Color askColor = new Color(38, 166, 91);           // Green for buyers
    private Color neutralColor = new Color(100, 100, 100);
    private Color pocColor = new Color(255, 193, 7);           // Yellow highlight
    private Color buyImbalanceColor = new Color(0, 200, 83, 100);
    private Color sellImbalanceColor = new Color(255, 82, 82, 100);

    /**
     * Creates a footprint layer with default z-order.
     */
    public FootprintLayerV2() {
        super(Z_ORDER);
        initializeVertexArrays(512, 256);
    }

    /**
     * Creates a footprint layer with a custom z-order.
     *
     * @param zOrder the z-order for this layer
     */
    public FootprintLayerV2(int zOrder) {
        super(zOrder);
        initializeVertexArrays(512, 256);
    }

    private void initializeVertexArrays(int volumeLevelCapacity, int imbalanceCapacity) {
        this.volumeCapacity = volumeLevelCapacity;
        this.imbalanceCapacity = imbalanceCapacity;
        // 6 vertices per quad (2 triangles), FLOATS_PER_VERTEX floats per vertex
        // *2 for potential bid+ask bars per level
        this.volumeVertices = new float[volumeLevelCapacity * 6 * FLOATS_PER_VERTEX * 2];
        this.imbalanceVertices = new float[imbalanceCapacity * 6 * FLOATS_PER_VERTEX];
    }

    /**
     * Sets the footprint series to render.
     * Repaints automatically.
     *
     * @param series the footprint series
     */
    public void setSeries(FootprintSeries series) {
        this.series = series;
        markDirty();
        requestRepaint();
    }

    /**
     * Returns the current series.
     */
    public FootprintSeries getSeries() {
        return series;
    }

    // ========== Configuration methods ==========

    /**
     * Sets the display mode for footprint rendering.
     *
     * @param mode the display mode (BID_ASK, DELTA, VOLUME, PROFILE)
     */
    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        markDirty();
    }

    /**
     * Returns the current display mode.
     */
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Sets whether to highlight volume imbalances.
     *
     * @param highlight true to highlight imbalances
     */
    public void setHighlightImbalances(boolean highlight) {
        this.highlightImbalances = highlight;
        markDirty();
    }

    /**
     * Returns whether imbalance highlighting is enabled.
     */
    public boolean isHighlightImbalances() {
        return highlightImbalances;
    }

    /**
     * Sets the imbalance detection threshold.
     *
     * @param threshold ratio threshold (e.g., 3.0 for 300%)
     */
    public void setImbalanceThreshold(float threshold) {
        this.imbalanceThreshold = threshold;
        markDirty();
    }

    /**
     * Returns the imbalance threshold.
     */
    public float getImbalanceThreshold() {
        return imbalanceThreshold;
    }

    /**
     * Sets whether to show Point of Control (POC) highlighting.
     *
     * @param show true to highlight POC
     */
    public void setShowPOC(boolean show) {
        this.showPOC = show;
        markDirty();
    }

    /**
     * Returns whether POC highlighting is enabled.
     */
    public boolean isShowPOC() {
        return showPOC;
    }

    // ========== Color configuration ==========

    public void setBidColor(Color color) {
        this.bidColor = color;
        markDirty();
    }

    public Color getBidColor() {
        return bidColor;
    }

    public void setAskColor(Color color) {
        this.askColor = color;
        markDirty();
    }

    public Color getAskColor() {
        return askColor;
    }

    public void setNeutralColor(Color color) {
        this.neutralColor = color;
        markDirty();
    }

    public Color getNeutralColor() {
        return neutralColor;
    }

    public void setPocColor(Color color) {
        this.pocColor = color;
        markDirty();
    }

    public Color getPocColor() {
        return pocColor;
    }

    public void setBuyImbalanceColor(Color color) {
        this.buyImbalanceColor = color;
        markDirty();
    }

    public Color getBuyImbalanceColor() {
        return buyImbalanceColor;
    }

    public void setSellImbalanceColor(Color color) {
        this.sellImbalanceColor = color;
        markDirty();
    }

    public Color getSellImbalanceColor() {
        return sellImbalanceColor;
    }

    // ========== RenderLayer implementation ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("FootprintLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for volume bars (position + color)
        // Estimate: 100 bars * 50 levels * 2 sides * 6 vertices * 6 floats
        volumeBuffer = resources.getOrCreateBuffer("footprint.volume",
                BufferDescriptor.positionColor2D(100 * 50 * 2 * 6 * FLOATS_PER_VERTEX));

        // Create buffer for imbalance highlights (position + color)
        imbalanceBuffer = resources.getOrCreateBuffer("footprint.imbalance",
                BufferDescriptor.positionColor2D(100 * 10 * 6 * FLOATS_PER_VERTEX));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("FootprintLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("FootprintLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Skip if no series
        if (series == null || series.isEmpty()) {
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        // Check shader validity
        if (defaultShader == null || !defaultShader.isValid()) {
            log.warn("FootprintLayerV2 default shader not available");
            return;
        }

        // Get coordinate system for this series
        CoordinateSystem coords = ctx.getCoordinatesForData(series);

        // Get visible range
        int firstIdx = findFirstVisibleIndex(series, ctx);
        int lastIdx = findLastVisibleIndex(series, ctx);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        RenderDevice device = ctx.getDevice();

        // Render imbalance backgrounds first (with alpha blending)
        if (highlightImbalances) {
            renderImbalanceHighlights(ctx, coords, firstIdx, lastIdx, device);
        }

        // Render volume bars
        renderVolumeBars(ctx, coords, firstIdx, lastIdx);
    }

    private void renderVolumeBars(RenderContext ctx, CoordinateSystem coords,
                                   int firstIdx, int lastIdx) {
        // Estimate capacity needed
        int estimatedLevels = 0;
        FootprintBar[] bars = series.getBarsArray();
        for (int i = firstIdx; i <= lastIdx; i++) {
            if (bars[i] != null) {
                estimatedLevels += bars[i].getLevelCount();
            }
        }
        ensureVolumeCapacity(estimatedLevels);

        long[] timestamps = series.getTimestampsArray();
        float tickSize = series.getTickSize();

        int floatIdx = 0;

        // Find max volume for scaling
        float maxVolume = findMaxVolume(firstIdx, lastIdx);
        if (maxVolume <= 0) return;

        for (int i = firstIdx; i <= lastIdx; i++) {
            FootprintBar bar = bars[i];
            if (bar == null || bar.getLevelCount() == 0) continue;

            float barCenterX = (float) coords.xValueToScreenX(timestamps[i]);
            float barWidth = (float) ctx.getBarWidth() * 0.9f;
            float halfBar = barWidth / 2;

            FootprintLevel[] levels = bar.getLevelsArray();
            int pocIdx = bar.getPOCIndex();

            for (int lvl = 0; lvl < levels.length; lvl++) {
                FootprintLevel level = levels[lvl];

                float priceY = (float) coords.yValueToScreenY(level.getPrice());
                float tickHeight = (float) Math.abs(
                        coords.yValueToScreenY(level.getPrice() + tickSize) -
                        coords.yValueToScreenY(level.getPrice()));

                // Ensure minimum visible height
                tickHeight = Math.max(tickHeight, 2.0f);

                switch (displayMode) {
                    case DELTA -> {
                        // Single bar showing delta direction
                        float delta = level.getDelta();
                        float width = Math.abs(delta) / maxVolume * halfBar;
                        Color color = delta > 0 ? askColor : bidColor;

                        if (delta > 0) {
                            // Ask (buy) - right side
                            floatIdx = addQuad(volumeVertices, floatIdx,
                                    barCenterX, priceY - tickHeight / 2,
                                    barCenterX + width, priceY + tickHeight / 2,
                                    color);
                        } else if (delta < 0) {
                            // Bid (sell) - left side
                            floatIdx = addQuad(volumeVertices, floatIdx,
                                    barCenterX - width, priceY - tickHeight / 2,
                                    barCenterX, priceY + tickHeight / 2,
                                    color);
                        }
                    }
                    case VOLUME -> {
                        // Total volume bar centered
                        float totalVol = level.getTotalVolume();
                        float width = totalVol / maxVolume * halfBar;
                        float delta = level.getDelta();
                        Color color = delta > 0 ? askColor : (delta < 0 ? bidColor : neutralColor);

                        floatIdx = addQuad(volumeVertices, floatIdx,
                                barCenterX - width / 2, priceY - tickHeight / 2,
                                barCenterX + width / 2, priceY + tickHeight / 2,
                                color);
                    }
                    case PROFILE -> {
                        // Mini profile - single bar showing total volume
                        float totalVol = level.getTotalVolume();
                        float width = totalVol / maxVolume * barWidth;

                        // Color gradient based on position in bar
                        float ratio = (float) lvl / Math.max(1, levels.length - 1);
                        Color color = blendColors(bidColor, askColor, ratio);

                        floatIdx = addQuad(volumeVertices, floatIdx,
                                barCenterX - halfBar, priceY - tickHeight / 2,
                                barCenterX - halfBar + width, priceY + tickHeight / 2,
                                color);
                    }
                    case BID_ASK -> {
                        // Bid bar (left side)
                        float bidWidth = level.getBidVolume() / maxVolume * halfBar;
                        if (bidWidth > 0) {
                            floatIdx = addQuad(volumeVertices, floatIdx,
                                    barCenterX - bidWidth, priceY - tickHeight / 2,
                                    barCenterX, priceY + tickHeight / 2,
                                    bidColor);
                        }

                        // Ask bar (right side)
                        float askWidth = level.getAskVolume() / maxVolume * halfBar;
                        if (askWidth > 0) {
                            floatIdx = addQuad(volumeVertices, floatIdx,
                                    barCenterX, priceY - tickHeight / 2,
                                    barCenterX + askWidth, priceY + tickHeight / 2,
                                    askColor);
                        }
                    }
                }

                // POC highlight
                if (showPOC && lvl == pocIdx) {
                    floatIdx = addQuad(volumeVertices, floatIdx,
                            barCenterX - halfBar, priceY - tickHeight / 2,
                            barCenterX + halfBar, priceY + tickHeight / 2,
                            pocColor, 0.3f);
                }
            }
        }

        // Upload and draw
        if (floatIdx > 0) {
            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            volumeBuffer.upload(volumeVertices, 0, floatIdx);
            volumeBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
        }
    }

    private void renderImbalanceHighlights(RenderContext ctx, CoordinateSystem coords,
                                            int firstIdx, int lastIdx, RenderDevice device) {
        int estimatedImbalances = (lastIdx - firstIdx + 1) * 5;
        ensureImbalanceCapacity(estimatedImbalances);

        FootprintBar[] bars = series.getBarsArray();
        long[] timestamps = series.getTimestampsArray();
        float tickSize = series.getTickSize();

        int floatIdx = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            FootprintBar bar = bars[i];
            if (bar == null) continue;

            List<Imbalance> imbalances = bar.getImbalances(imbalanceThreshold);
            if (imbalances.isEmpty()) continue;

            float barCenterX = (float) coords.xValueToScreenX(timestamps[i]);
            float barWidth = (float) ctx.getBarWidth() * 0.9f;
            float halfBar = barWidth / 2;

            for (Imbalance imbalance : imbalances) {
                float priceY = (float) coords.yValueToScreenY(imbalance.getPrice());
                float tickHeight = (float) Math.abs(
                        coords.yValueToScreenY(imbalance.getPrice() + tickSize) -
                        coords.yValueToScreenY(imbalance.getPrice()));

                // Ensure minimum visible height
                tickHeight = Math.max(tickHeight, 2.0f);

                Color color = imbalance.isBuyImbalance() ? buyImbalanceColor : sellImbalanceColor;

                floatIdx = addQuad(imbalanceVertices, floatIdx,
                        barCenterX - halfBar, priceY - tickHeight / 2,
                        barCenterX + halfBar, priceY + tickHeight / 2,
                        color);
            }
        }

        if (floatIdx > 0) {
            // Enable alpha blending for semi-transparent highlights
            device.setBlendMode(BlendMode.ALPHA);

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            imbalanceBuffer.upload(imbalanceVertices, 0, floatIdx);
            imbalanceBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();

            // Restore default blend mode
            device.setBlendMode(BlendMode.NONE);
        }
    }

    // ========== Helper methods ==========

    private float findMaxVolume(int firstIdx, int lastIdx) {
        float max = 0;
        FootprintBar[] bars = series.getBarsArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            FootprintBar bar = bars[i];
            if (bar == null) continue;

            for (FootprintLevel level : bar.getLevelsArray()) {
                float vol = level.getTotalVolume();
                if (vol > max) max = vol;
            }
        }
        return max;
    }

    private int findFirstVisibleIndex(FootprintSeries series, RenderContext ctx) {
        long startTime = ctx.getViewport().getStartTime();
        return series.indexAtOrAfter(startTime);
    }

    private int findLastVisibleIndex(FootprintSeries series, RenderContext ctx) {
        long endTime = ctx.getViewport().getEndTime();
        return series.indexAtOrBefore(endTime);
    }

    private int addQuad(float[] vertices, int index,
                        float x1, float y1, float x2, float y2, Color color) {
        return addQuad(vertices, index, x1, y1, x2, y2, color, color.getAlpha() / 255f);
    }

    private int addQuad(float[] vertices, int index,
                        float x1, float y1, float x2, float y2, Color color, float alpha) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        // Triangle 1: top-left, top-right, bottom-right
        index = addVertex(vertices, index, x1, y1, r, g, b, alpha);
        index = addVertex(vertices, index, x2, y1, r, g, b, alpha);
        index = addVertex(vertices, index, x2, y2, r, g, b, alpha);

        // Triangle 2: top-left, bottom-right, bottom-left
        index = addVertex(vertices, index, x1, y1, r, g, b, alpha);
        index = addVertex(vertices, index, x2, y2, r, g, b, alpha);
        index = addVertex(vertices, index, x1, y2, r, g, b, alpha);

        return index;
    }

    private int addVertex(float[] vertices, int index,
                          float x, float y, float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }

    private void ensureVolumeCapacity(int levelCount) {
        // Each level may have up to 2 quads (bid + ask) + 1 POC quad = 3 quads
        // Each quad = 6 vertices * FLOATS_PER_VERTEX floats
        int required = levelCount * 3 * 6 * FLOATS_PER_VERTEX;
        if (required > volumeVertices.length) {
            volumeCapacity = levelCount + levelCount / 2;
            volumeVertices = new float[volumeCapacity * 3 * 6 * FLOATS_PER_VERTEX];
        }
    }

    private void ensureImbalanceCapacity(int imbalanceCount) {
        int required = imbalanceCount * 6 * FLOATS_PER_VERTEX;
        if (required > imbalanceVertices.length) {
            imbalanceCapacity = imbalanceCount + imbalanceCount / 2;
            imbalanceVertices = new float[imbalanceCapacity * 6 * FLOATS_PER_VERTEX];
        }
    }

    private Color blendColors(Color c1, Color c2, float ratio) {
        float inv = 1.0f - ratio;
        int r = (int) (c1.getRed() * inv + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * inv + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * inv + c2.getBlue() * ratio);
        int a = (int) (c1.getAlpha() * inv + c2.getAlpha() * ratio);
        return new Color(r, g, b, a);
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
                resources.disposeBuffer("footprint.volume");
                resources.disposeBuffer("footprint.imbalance");
            }
            volumeBuffer = null;
            imbalanceBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }
}
