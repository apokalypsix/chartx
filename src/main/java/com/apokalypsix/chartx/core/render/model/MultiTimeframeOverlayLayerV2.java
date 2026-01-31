package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.chart.data.OHLCBar;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.Timeframe;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.data.TimeframeAggregator;
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
 * V2 render layer for displaying higher timeframe (HTF) candles overlaid on
 * the lower timeframe (LTF) chart using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link MultiTimeframeOverlayLayer} that uses
 * backend-agnostic rendering interfaces instead of direct GL calls.
 *
 * <p>MultiTimeframeOverlayLayerV2 provides:
 * <ul>
 *   <li>Automatic aggregation of LTF data to HTF</li>
 *   <li>Semi-transparent HTF candle rendering</li>
 *   <li>Proper alignment of HTF bars on the LTF time axis</li>
 *   <li>Different visual styles (hollow, outline, filled, range shading)</li>
 * </ul>
 *
 * <p>HTF candles are typically rendered with reduced opacity or as
 * hollow/outline candles to avoid obscuring the LTF candles.
 */
public class MultiTimeframeOverlayLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(MultiTimeframeOverlayLayerV2.class);

    /** Z-order for HTF overlay (just above data layer) */
    public static final int Z_ORDER = 210;

    /**
     * Visual style for HTF candles.
     */
    public enum HTFStyle {
        /** Filled candles with reduced opacity */
        TRANSPARENT_FILLED,
        /** Outline only (hollow) candles */
        HOLLOW_OUTLINE,
        /** Filled body with prominent outline */
        FILLED_WITH_OUTLINE,
        /** Only the body (no wicks) */
        BODY_ONLY,
        /** Shaded area showing HTF range */
        RANGE_SHADING
    }

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;

    // Source and aggregated data
    private OhlcData sourceData;
    private OhlcData htfData;
    private Timeframe htfTimeframe = Timeframe.M5;
    private boolean htfDirty = true;

    // Visual configuration
    private HTFStyle style = HTFStyle.HOLLOW_OUTLINE;
    private Color bullColor = new Color(0, 255, 0, 100);
    private Color bearColor = new Color(255, 0, 0, 100);
    private Color neutralColor = new Color(128, 128, 128, 100);
    private float opacity = 0.5f;
    private float lineWidth = 2.0f;
    private float bodyWidthRatio = 0.8f;  // HTF body width as ratio of available space

    // V2 API resources
    private Buffer bodyBuffer;      // For filled candle bodies (triangles)
    private Buffer wickBuffer;      // For candle wicks (lines)
    private Buffer outlineBuffer;   // For body outlines (lines)
    private Shader defaultShader;   // Position + per-vertex color
    private boolean v2Initialized = false;

    // Reusable arrays for rendering
    private float[] bodyVertices;
    private float[] wickVertices;
    private float[] outlineVertices;
    private int bodyVertexCount;
    private int wickVertexCount;
    private int outlineVertexCount;

    // Data listener for auto-update
    private final DataListener aggregationListener;

    // Reusable bar instance
    private final OHLCBar reusableBar = new OHLCBar();

    /**
     * Creates a multi-timeframe overlay layer using V2 rendering.
     */
    public MultiTimeframeOverlayLayerV2() {
        super(Z_ORDER);

        // Pre-allocate vertex arrays
        bodyVertices = new float[4096];
        wickVertices = new float[2048];
        outlineVertices = new float[4096];

        this.aggregationListener = new DataListener() {
            @Override
            public void onDataAppended(Data<?> data, int newIndex) {
                htfDirty = true;
                markDirty();
            }

            @Override
            public void onDataUpdated(Data<?> data, int index) {
                htfDirty = true;
                markDirty();
            }

            @Override
            public void onDataCleared(Data<?> data) {
                htfData = null;
                htfDirty = true;
                markDirty();
            }
        };
    }

    // ========== Configuration ==========

    /**
     * Sets the source OHLC data (lower timeframe).
     */
    public void setSourceData(OhlcData data) {
        if (this.sourceData != null) {
            this.sourceData.removeListener(aggregationListener);
        }

        this.sourceData = data;
        this.htfDirty = true;

        if (data != null) {
            data.addListener(aggregationListener);
        }

        markDirty();
    }

    /**
     * Returns the source data.
     */
    public OhlcData getSourceData() {
        return sourceData;
    }

    /**
     * Sets the higher timeframe for aggregation.
     */
    public void setHTFTimeframe(Timeframe timeframe) {
        if (this.htfTimeframe != timeframe) {
            this.htfTimeframe = timeframe;
            this.htfDirty = true;
            markDirty();
        }
    }

    /**
     * Returns the HTF timeframe.
     */
    public Timeframe getHTFTimeframe() {
        return htfTimeframe;
    }

    /**
     * Returns the aggregated HTF data.
     */
    public OhlcData getHTFData() {
        ensureHTFData();
        return htfData;
    }

    /**
     * Sets the visual style for HTF candles.
     */
    public void setStyle(HTFStyle style) {
        this.style = style;
        markDirty();
    }

    /**
     * Returns the current visual style.
     */
    public HTFStyle getStyle() {
        return style;
    }

    /**
     * Sets the bullish candle color.
     */
    public void setBullColor(Color color) {
        this.bullColor = color;
        markDirty();
    }

    /**
     * Returns the bullish candle color.
     */
    public Color getBullColor() {
        return bullColor;
    }

    /**
     * Sets the bearish candle color.
     */
    public void setBearColor(Color color) {
        this.bearColor = color;
        markDirty();
    }

    /**
     * Returns the bearish candle color.
     */
    public Color getBearColor() {
        return bearColor;
    }

    /**
     * Sets the neutral (doji) candle color.
     */
    public void setNeutralColor(Color color) {
        this.neutralColor = color;
        markDirty();
    }

    /**
     * Returns the neutral candle color.
     */
    public Color getNeutralColor() {
        return neutralColor;
    }

    /**
     * Sets the overall opacity (0.0 - 1.0).
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
        markDirty();
    }

    /**
     * Returns the current opacity.
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Sets the line width for outline styles.
     */
    public void setLineWidth(float width) {
        this.lineWidth = width;
        markDirty();
    }

    /**
     * Returns the line width.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the body width ratio (0.0 - 1.0).
     */
    public void setBodyWidthRatio(float ratio) {
        this.bodyWidthRatio = Math.max(0.1f, Math.min(1f, ratio));
        markDirty();
    }

    /**
     * Returns the body width ratio.
     */
    public float getBodyWidthRatio() {
        return bodyWidthRatio;
    }

    // ========== Rendering ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("MultiTimeframeOverlayLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create separate buffers for bodies (triangles), wicks (lines), and outlines (lines)
        bodyBuffer = resources.getOrCreateBuffer("htf.body",
                BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX));

        wickBuffer = resources.getOrCreateBuffer("htf.wick",
                BufferDescriptor.positionColor2D(2048 * FLOATS_PER_VERTEX));

        outlineBuffer = resources.getOrCreateBuffer("htf.outline",
                BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX));

        // Get the default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("MultiTimeframeOverlayLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("MultiTimeframeOverlayLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        ensureHTFData();

        if (htfData == null || htfData.isEmpty()) {
            return;
        }

        if (defaultShader == null || !defaultShader.isValid()) {
            log.warn("Default shader not available for HTF overlay rendering");
            return;
        }

        CoordinateSystem coords = ctx.getCoordinates();
        buildVertices(ctx, coords);

        RenderDevice device = ctx.getDevice();

        // Enable alpha blending for transparency
        device.setBlendMode(BlendMode.ALPHA);

        // Render based on style
        switch (style) {
            case TRANSPARENT_FILLED:
            case BODY_ONLY:
                renderBodies(ctx);
                if (style != HTFStyle.BODY_ONLY) {
                    renderWicks(ctx, device);
                }
                break;

            case HOLLOW_OUTLINE:
                renderOutlines(ctx, device);
                renderWicks(ctx, device);
                break;

            case FILLED_WITH_OUTLINE:
                renderBodies(ctx);
                renderOutlines(ctx, device);
                renderWicks(ctx, device);
                break;

            case RANGE_SHADING:
                renderRangeShading(ctx);
                break;
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        // V2 resources are managed by ResourceManager
        v2Initialized = false;

        // Clean up listener
        if (sourceData != null) {
            sourceData.removeListener(aggregationListener);
        }
    }

    /**
     * Disposes V2 resources.
     * Call this when the RenderContext is available during cleanup.
     */
    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            ResourceManager resources = ctx.getResourceManager();
            if (resources != null) {
                resources.disposeBuffer("htf.body");
                resources.disposeBuffer("htf.wick");
                resources.disposeBuffer("htf.outline");
            }
            bodyBuffer = null;
            wickBuffer = null;
            outlineBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }

    // ========== Internal ==========

    private void ensureHTFData() {
        if (!htfDirty && htfData != null) {
            return;
        }

        if (sourceData == null || sourceData.isEmpty()) {
            htfData = null;
            htfDirty = false;
            return;
        }

        htfData = TimeframeAggregator.aggregate(sourceData, htfTimeframe);
        htfDirty = false;
    }

    private void buildVertices(RenderContext ctx, CoordinateSystem coords) {
        bodyVertexCount = 0;
        wickVertexCount = 0;
        outlineVertexCount = 0;

        int firstVisible = findFirstVisibleHTFBar(ctx);
        int lastVisible = findLastVisibleHTFBar(ctx);

        if (firstVisible < 0 || lastVisible < 0) {
            return;
        }

        // Calculate bar width in pixels
        long htfDuration = htfTimeframe.millis;
        double htfBarWidth = coords.getPixelWidth(htfDuration);
        float bodyWidth = (float) (htfBarWidth * bodyWidthRatio);

        for (int i = firstVisible; i <= lastVisible && i < htfData.size(); i++) {
            htfData.getBar(i, reusableBar);

            // Calculate center X position (middle of HTF bar's time range)
            float centerX = (float) coords.xValueToScreenX(reusableBar.getTimestamp() + htfDuration / 2);

            // Calculate Y positions
            float openY = (float) coords.yValueToScreenY(reusableBar.getOpen());
            float highY = (float) coords.yValueToScreenY(reusableBar.getHigh());
            float lowY = (float) coords.yValueToScreenY(reusableBar.getLow());
            float closeY = (float) coords.yValueToScreenY(reusableBar.getClose());

            // Determine color
            Color color;
            if (reusableBar.getClose() > reusableBar.getOpen()) {
                color = bullColor;
            } else if (reusableBar.getClose() < reusableBar.getOpen()) {
                color = bearColor;
            } else {
                color = neutralColor;
            }

            // Apply opacity
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = (color.getAlpha() / 255f) * opacity;

            // Add body vertices (quad as two triangles)
            float halfWidth = bodyWidth / 2;
            addBodyQuad(centerX - halfWidth, openY, centerX + halfWidth, closeY, r, g, b, a);

            // Add wick vertices (vertical line)
            addWickLine(centerX, highY, lowY, r, g, b, a);

            // Add outline vertices (if needed)
            if (style == HTFStyle.HOLLOW_OUTLINE || style == HTFStyle.FILLED_WITH_OUTLINE) {
                addOutlineQuad(centerX - halfWidth, openY, centerX + halfWidth, closeY, r, g, b, a);
            }
        }
    }

    private int findFirstVisibleHTFBar(RenderContext ctx) {
        if (htfData == null || htfData.isEmpty()) {
            return -1;
        }

        long visibleStart = ctx.getCoordinates().screenXToXValue(0);
        return htfData.indexAtOrBefore(visibleStart);
    }

    private int findLastVisibleHTFBar(RenderContext ctx) {
        if (htfData == null || htfData.isEmpty()) {
            return -1;
        }

        long visibleEnd = ctx.getCoordinates().screenXToXValue(ctx.getViewport().getWidth());
        return htfData.indexAtOrBefore(visibleEnd);
    }

    private void addBodyQuad(float x1, float y1, float x2, float y2,
                             float r, float g, float b, float a) {
        ensureBodyCapacity(6 * FLOATS_PER_VERTEX);  // 6 vertices, 6 floats each

        int idx = bodyVertexCount * FLOATS_PER_VERTEX;

        // Triangle 1
        bodyVertices[idx++] = x1; bodyVertices[idx++] = y1;
        bodyVertices[idx++] = r;  bodyVertices[idx++] = g;
        bodyVertices[idx++] = b;  bodyVertices[idx++] = a;

        bodyVertices[idx++] = x2; bodyVertices[idx++] = y1;
        bodyVertices[idx++] = r;  bodyVertices[idx++] = g;
        bodyVertices[idx++] = b;  bodyVertices[idx++] = a;

        bodyVertices[idx++] = x1; bodyVertices[idx++] = y2;
        bodyVertices[idx++] = r;  bodyVertices[idx++] = g;
        bodyVertices[idx++] = b;  bodyVertices[idx++] = a;

        // Triangle 2
        bodyVertices[idx++] = x2; bodyVertices[idx++] = y1;
        bodyVertices[idx++] = r;  bodyVertices[idx++] = g;
        bodyVertices[idx++] = b;  bodyVertices[idx++] = a;

        bodyVertices[idx++] = x2; bodyVertices[idx++] = y2;
        bodyVertices[idx++] = r;  bodyVertices[idx++] = g;
        bodyVertices[idx++] = b;  bodyVertices[idx++] = a;

        bodyVertices[idx++] = x1; bodyVertices[idx++] = y2;
        bodyVertices[idx++] = r;  bodyVertices[idx++] = g;
        bodyVertices[idx++] = b;  bodyVertices[idx] = a;

        bodyVertexCount += 6;
    }

    private void addWickLine(float x, float highY, float lowY,
                             float r, float g, float b, float a) {
        ensureWickCapacity(2 * FLOATS_PER_VERTEX);

        int idx = wickVertexCount * FLOATS_PER_VERTEX;

        wickVertices[idx++] = x; wickVertices[idx++] = highY;
        wickVertices[idx++] = r; wickVertices[idx++] = g;
        wickVertices[idx++] = b; wickVertices[idx++] = a;

        wickVertices[idx++] = x; wickVertices[idx++] = lowY;
        wickVertices[idx++] = r; wickVertices[idx++] = g;
        wickVertices[idx++] = b; wickVertices[idx] = a;

        wickVertexCount += 2;
    }

    private void addOutlineQuad(float x1, float y1, float x2, float y2,
                                float r, float g, float b, float a) {
        ensureOutlineCapacity(8 * FLOATS_PER_VERTEX);  // 8 vertices for 4 lines

        int idx = outlineVertexCount * FLOATS_PER_VERTEX;

        // Top line
        outlineVertices[idx++] = x1; outlineVertices[idx++] = y1;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;
        outlineVertices[idx++] = x2; outlineVertices[idx++] = y1;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;

        // Right line
        outlineVertices[idx++] = x2; outlineVertices[idx++] = y1;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;
        outlineVertices[idx++] = x2; outlineVertices[idx++] = y2;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;

        // Bottom line
        outlineVertices[idx++] = x2; outlineVertices[idx++] = y2;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;
        outlineVertices[idx++] = x1; outlineVertices[idx++] = y2;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;

        // Left line
        outlineVertices[idx++] = x1; outlineVertices[idx++] = y2;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx++] = a;
        outlineVertices[idx++] = x1; outlineVertices[idx++] = y1;
        outlineVertices[idx++] = r;  outlineVertices[idx++] = g;
        outlineVertices[idx++] = b;  outlineVertices[idx] = a;

        outlineVertexCount += 8;
    }

    private void ensureBodyCapacity(int additional) {
        int required = (bodyVertexCount * FLOATS_PER_VERTEX) + additional;
        if (required > bodyVertices.length) {
            float[] newArray = new float[Math.max(required, bodyVertices.length * 2)];
            System.arraycopy(bodyVertices, 0, newArray, 0, bodyVertexCount * FLOATS_PER_VERTEX);
            bodyVertices = newArray;
        }
    }

    private void ensureWickCapacity(int additional) {
        int required = (wickVertexCount * FLOATS_PER_VERTEX) + additional;
        if (required > wickVertices.length) {
            float[] newArray = new float[Math.max(required, wickVertices.length * 2)];
            System.arraycopy(wickVertices, 0, newArray, 0, wickVertexCount * FLOATS_PER_VERTEX);
            wickVertices = newArray;
        }
    }

    private void ensureOutlineCapacity(int additional) {
        int required = (outlineVertexCount * FLOATS_PER_VERTEX) + additional;
        if (required > outlineVertices.length) {
            float[] newArray = new float[Math.max(required, outlineVertices.length * 2)];
            System.arraycopy(outlineVertices, 0, newArray, 0, outlineVertexCount * FLOATS_PER_VERTEX);
            outlineVertices = newArray;
        }
    }

    private void renderBodies(RenderContext ctx) {
        if (bodyVertexCount == 0) {
            return;
        }

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        bodyBuffer.upload(bodyVertices, 0, bodyVertexCount * FLOATS_PER_VERTEX);
        bodyBuffer.draw(DrawMode.TRIANGLES);

        defaultShader.unbind();
    }

    private void renderWicks(RenderContext ctx, RenderDevice device) {
        if (wickVertexCount == 0) {
            return;
        }

        device.setLineWidth(lineWidth);

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        wickBuffer.upload(wickVertices, 0, wickVertexCount * FLOATS_PER_VERTEX);
        wickBuffer.draw(DrawMode.LINES);

        defaultShader.unbind();
    }

    private void renderOutlines(RenderContext ctx, RenderDevice device) {
        if (outlineVertexCount == 0) {
            return;
        }

        device.setLineWidth(lineWidth);

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        outlineBuffer.upload(outlineVertices, 0, outlineVertexCount * FLOATS_PER_VERTEX);
        outlineBuffer.draw(DrawMode.LINES);

        defaultShader.unbind();
    }

    private void renderRangeShading(RenderContext ctx) {
        // Range shading uses filled rectangles spanning the full HTF high-low range
        // Reuse the body rendering path - vertices are already built to show the range
        if (bodyVertexCount == 0) {
            return;
        }

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        bodyBuffer.upload(bodyVertices, 0, bodyVertexCount * FLOATS_PER_VERTEX);
        bodyBuffer.draw(DrawMode.TRIANGLES);

        defaultShader.unbind();
    }
}
