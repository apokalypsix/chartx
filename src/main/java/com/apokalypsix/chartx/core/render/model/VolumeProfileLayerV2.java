package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.data.model.VolumeProfileSeries;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for volume profile data using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link VolumeProfileLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>This layer displays volume distribution across price levels as horizontal bars.
 * It shows:
 * <ul>
 *   <li>Volume bars proportional to trading activity at each price level</li>
 *   <li>Point of Control (POC) - the highest volume price level</li>
 *   <li>Value Area - the range containing 70% of volume</li>
 * </ul>
 *
 * <p>The profile can be positioned on either side of the chart and supports
 * multiple display modes (total volume, delta, buy/sell split).
 */
public class VolumeProfileLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(VolumeProfileLayerV2.class);

    /** Z-order for volume profile layer (renders before candles, after grid) */
    public static final int Z_ORDER = 140;

    /** Z-order for overlay mode (renders after candles) */
    public static final int Z_ORDER_OVERLAY = 245;

    /**
     * Display mode for volume profile bars.
     */
    public enum DisplayMode {
        /** Show total volume as single color */
        TOTAL,
        /** Show delta (buy - sell) with color based on sign */
        DELTA,
        /** Show buy and sell portions in different colors */
        BUY_SELL
    }

    /**
     * Position of the profile on the chart.
     */
    public enum Position {
        /** Profile on left side of chart */
        LEFT,
        /** Profile on right side of chart */
        RIGHT
    }

    // Configuration
    private VolumeProfileSeries series;
    private DisplayMode displayMode = DisplayMode.BUY_SELL;
    private Position position = Position.RIGHT;
    private int widthPixels = 100;
    private float widthPercentage = 0.15f;
    private boolean usePercentageWidth = false;
    private boolean showPOC = true;
    private boolean showValueArea = true;
    private boolean overlayMode = false;

    // V2 API resources
    private Buffer barBuffer;
    private Buffer valueAreaBuffer;
    private Shader defaultShader;
    private boolean v2Initialized = false;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    // Vertices per bar (2 triangles = 6 vertices)
    private static final int VERTICES_PER_BAR = 6;

    // Reusable vertex arrays
    private float[] barVertices;
    private float[] valueAreaVertices;
    private int barVertexCapacity;

    /**
     * Creates a volume profile layer.
     */
    public VolumeProfileLayerV2() {
        super(Z_ORDER);
        // Pre-allocate vertex arrays
        barVertexCapacity = 256;
        barVertices = new float[barVertexCapacity * 2 * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
        valueAreaVertices = new float[VERTICES_PER_BAR * FLOATS_PER_VERTEX];
    }

    /**
     * Creates a volume profile layer with a custom z-order.
     *
     * @param zOrder the z-order for this layer
     */
    public VolumeProfileLayerV2(int zOrder) {
        super(zOrder);
        barVertexCapacity = 256;
        barVertices = new float[barVertexCapacity * 2 * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
        valueAreaVertices = new float[VERTICES_PER_BAR * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("VolumeProfileLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for volume bars (position + color)
        // Initial capacity for 512 levels (each may have 2 bars in BUY_SELL mode)
        barBuffer = resources.getOrCreateBuffer("volumeprofile.bars",
                BufferDescriptor.positionColor2D(512 * 2 * VERTICES_PER_BAR * FLOATS_PER_VERTEX));

        // Create buffer for value area background (single quad)
        valueAreaBuffer = resources.getOrCreateBuffer("volumeprofile.valuearea",
                BufferDescriptor.positionColor2D(VERTICES_PER_BAR * FLOATS_PER_VERTEX));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("VolumeProfileLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("VolumeProfileLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        if (series == null || series.getLevelCount() == 0) {
            return;
        }

        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinatesForData(series);

        // Render value area background first
        if (showValueArea) {
            renderValueArea(ctx, coords);
        }

        // Render volume bars
        renderVolumeBars(ctx, coords);
    }

    private void renderVolumeBars(RenderContext ctx, CoordinateSystem coords) {
        int levelCount = series.getLevelCount();
        if (levelCount == 0) {
            return;
        }

        // Ensure capacity
        ensureCapacity(levelCount);

        // Calculate profile dimensions
        Viewport viewport = ctx.getViewport();
        int chartWidth = ctx.getChartWidth();
        int chartLeft = viewport.getLeftInset();
        int chartRight = chartLeft + chartWidth;

        float profileWidth = usePercentageWidth
                ? chartWidth * widthPercentage
                : widthPixels;

        // Determine the maximum volume for scaling
        float maxVolume = 0;
        for (int i = 0; i < levelCount; i++) {
            float vol = series.getTotalVolumeAt(i);
            if (vol > maxVolume) {
                maxVolume = vol;
            }
        }

        if (maxVolume <= 0) {
            return;
        }

        // Calculate bar height from tick size
        float tickSize = series.getTickSize();
        float priceTop = series.getPriceAt(0);
        float priceBottom = series.getPriceAt(0) + tickSize;
        double screenTop = coords.yValueToScreenY(priceTop);
        double screenBottom = coords.yValueToScreenY(priceBottom);
        float barHeight = (float) Math.abs(screenBottom - screenTop) * 0.9f;
        float halfHeight = barHeight / 2;

        int pocIndex = series.getPOCIndex();
        float opacity = series.getOpacity();

        int floatIndex = 0;
        float[] priceLevels = series.getPriceLevelsArray();
        float[] buyVolumes = series.getBuyVolumeArray();
        float[] sellVolumes = series.getSellVolumeArray();

        for (int i = 0; i < levelCount; i++) {
            float price = priceLevels[i];
            float buyVol = buyVolumes[i];
            float sellVol = sellVolumes[i];
            float totalVol = buyVol + sellVol;

            if (totalVol <= 0) {
                continue;
            }

            // Calculate screen Y position
            float centerY = (float) coords.yValueToScreenY(price);

            // Calculate bar width proportional to volume
            float normalizedVol = totalVol / maxVolume;
            float barWidth = normalizedVol * profileWidth;

            // Calculate X position based on side
            float barLeft, barRight;
            if (position == Position.RIGHT) {
                barLeft = chartRight - barWidth;
                barRight = chartRight;
            } else {
                barLeft = chartLeft;
                barRight = chartLeft + barWidth;
            }

            float top = centerY - halfHeight;
            float bottom = centerY + halfHeight;

            // Skip if outside visible area
            if (bottom < 0 || top > ctx.getHeight()) {
                continue;
            }

            // Determine colors based on display mode
            if (displayMode == DisplayMode.BUY_SELL && buyVol > 0 && sellVol > 0) {
                // Draw buy and sell portions separately
                float buyRatio = buyVol / totalVol;

                if (position == Position.RIGHT) {
                    float splitX = barLeft + barWidth * buyRatio;

                    // Draw sell portion (left)
                    floatIndex = addBar(barVertices, floatIndex, barLeft, splitX, top, bottom,
                            series.getSellColor(), opacity);

                    // Draw buy portion (right)
                    floatIndex = addBar(barVertices, floatIndex, splitX, barRight, top, bottom,
                            series.getBuyColor(), opacity);
                } else {
                    float splitX = barLeft + barWidth * buyRatio;

                    // Draw buy portion (left)
                    floatIndex = addBar(barVertices, floatIndex, barLeft, splitX, top, bottom,
                            series.getBuyColor(), opacity);

                    // Draw sell portion (right)
                    floatIndex = addBar(barVertices, floatIndex, splitX, barRight, top, bottom,
                            series.getSellColor(), opacity);
                }
            } else {
                // Single color based on mode
                Color color = getBarColor(i);
                if (showPOC && i == pocIndex) {
                    color = series.getPocColor();
                }
                floatIndex = addBar(barVertices, floatIndex, barLeft, barRight, top, bottom,
                        color, opacity);
            }
        }

        if (floatIndex > 0) {
            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            barBuffer.upload(barVertices, 0, floatIndex);
            barBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
        }
    }

    private void renderValueArea(RenderContext ctx, CoordinateSystem coords) {
        int valIndex = series.getVALIndex();
        int vahIndex = series.getVAHIndex();

        if (valIndex < 0 || vahIndex < 0) {
            return;
        }

        float valPrice = series.getValueAreaLow();
        float vahPrice = series.getValueAreaHigh();

        double valY = coords.yValueToScreenY(valPrice);
        double vahY = coords.yValueToScreenY(vahPrice);

        // Ensure proper order (in screen coords, lower price = higher Y)
        float top = (float) Math.min(valY, vahY);
        float bottom = (float) Math.max(valY, vahY);

        // Extend to profile area
        Viewport viewport = ctx.getViewport();
        int chartWidth = ctx.getChartWidth();
        int chartLeft = viewport.getLeftInset();
        int chartRight = chartLeft + chartWidth;

        float profileWidth = usePercentageWidth
                ? chartWidth * widthPercentage
                : widthPixels;

        float left, right;
        if (position == Position.RIGHT) {
            left = chartRight - profileWidth;
            right = chartRight;
        } else {
            left = chartLeft;
            right = chartLeft + profileWidth;
        }

        Color vaColor = series.getValueAreaColor();
        float r = vaColor.getRed() / 255f;
        float g = vaColor.getGreen() / 255f;
        float b = vaColor.getBlue() / 255f;
        float a = vaColor.getAlpha() / 255f;

        // Build value area quad (2 triangles)
        int idx = 0;
        idx = addVertex(valueAreaVertices, idx, left, top, r, g, b, a);
        idx = addVertex(valueAreaVertices, idx, left, bottom, r, g, b, a);
        idx = addVertex(valueAreaVertices, idx, right, bottom, r, g, b, a);
        idx = addVertex(valueAreaVertices, idx, left, top, r, g, b, a);
        idx = addVertex(valueAreaVertices, idx, right, bottom, r, g, b, a);
        idx = addVertex(valueAreaVertices, idx, right, top, r, g, b, a);

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        valueAreaBuffer.upload(valueAreaVertices, 0, idx);
        valueAreaBuffer.draw(DrawMode.TRIANGLES);

        defaultShader.unbind();
    }

    private Color getBarColor(int levelIndex) {
        return switch (displayMode) {
            case TOTAL -> series.getBuyColor();
            case DELTA -> {
                float delta = series.getDeltaAt(levelIndex);
                yield delta >= 0 ? series.getBuyColor() : series.getSellColor();
            }
            case BUY_SELL -> {
                float buyVol = series.getBuyVolumeAt(levelIndex);
                float sellVol = series.getSellVolumeAt(levelIndex);
                yield buyVol >= sellVol ? series.getBuyColor() : series.getSellColor();
            }
        };
    }

    private int addBar(float[] vertices, int index, float left, float right,
                       float top, float bottom, Color color, float opacity) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = opacity;

        // Triangle 1: top-left, bottom-left, bottom-right
        index = addVertex(vertices, index, left, top, r, g, b, a);
        index = addVertex(vertices, index, left, bottom, r, g, b, a);
        index = addVertex(vertices, index, right, bottom, r, g, b, a);

        // Triangle 2: top-left, bottom-right, top-right
        index = addVertex(vertices, index, left, top, r, g, b, a);
        index = addVertex(vertices, index, right, bottom, r, g, b, a);
        index = addVertex(vertices, index, right, top, r, g, b, a);

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

    private void ensureCapacity(int levelCount) {
        // Each level may need up to 2 bars (buy/sell) in BUY_SELL mode
        int requiredFloats = levelCount * 2 * VERTICES_PER_BAR * FLOATS_PER_VERTEX;

        if (requiredFloats > barVertices.length) {
            barVertexCapacity = levelCount * 2 + levelCount / 2;
            barVertices = new float[barVertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
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
                resources.disposeBuffer("volumeprofile.bars");
                resources.disposeBuffer("volumeprofile.valuearea");
            }
            barBuffer = null;
            valueAreaBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }

    // ========== Series Management ==========

    /**
     * Sets the volume profile series to render.
     * Repaints automatically.
     *
     * @param series the volume profile series
     */
    public void setSeries(VolumeProfileSeries series) {
        this.series = series;
        markDirty();
        requestRepaint();
    }

    /**
     * Returns the current series.
     */
    public VolumeProfileSeries getSeries() {
        return series;
    }

    // ========== Configuration ==========

    /**
     * Sets the display mode for volume bars.
     *
     * @param mode the display mode
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
     * Sets the position of the profile (LEFT or RIGHT side of chart).
     *
     * @param position the position
     */
    public void setPosition(Position position) {
        this.position = position;
        markDirty();
    }

    /**
     * Returns the current position.
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Sets the width of the profile area in pixels.
     *
     * @param width width in pixels
     */
    public void setWidthPixels(int width) {
        this.widthPixels = width;
        this.usePercentageWidth = false;
        markDirty();
    }

    /**
     * Returns the configured width in pixels.
     */
    public int getWidthPixels() {
        return widthPixels;
    }

    /**
     * Sets the width as a percentage of chart width.
     *
     * @param percentage width percentage (0.0 to 1.0)
     */
    public void setWidthPercentage(float percentage) {
        this.widthPercentage = Math.max(0.05f, Math.min(0.5f, percentage));
        this.usePercentageWidth = true;
        markDirty();
    }

    /**
     * Returns the configured width percentage.
     */
    public float getWidthPercentage() {
        return widthPercentage;
    }

    /**
     * Sets whether to highlight the Point of Control level.
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

    /**
     * Sets whether to show the value area background.
     *
     * @param show true to show value area
     */
    public void setShowValueArea(boolean show) {
        this.showValueArea = show;
        markDirty();
    }

    /**
     * Returns whether value area display is enabled.
     */
    public boolean isShowValueArea() {
        return showValueArea;
    }

    /**
     * Sets overlay mode - renders volume profile on top of candlesticks.
     *
     * @param overlay true to enable overlay mode
     */
    public void setOverlayMode(boolean overlay) {
        this.overlayMode = overlay;
        if (overlay) {
            setZOrder(Z_ORDER_OVERLAY);
            if (series != null) {
                series.setOpacity(0.6f);
            }
        } else {
            setZOrder(Z_ORDER);
            if (series != null) {
                series.setOpacity(0.8f);
            }
        }
        markDirty();
    }

    /**
     * Returns whether overlay mode is enabled.
     */
    public boolean isOverlayMode() {
        return overlayMode;
    }
}
