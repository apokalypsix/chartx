package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.series.SeriesType;
import com.apokalypsix.chartx.chart.style.LegendConfig;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.util.ShapeUtils;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.TextRenderer;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for chart legend using GPU text rendering.
 *
 * <p>Renders the legend with color swatches and series names directly on the GPU
 * using the TextRenderer API, replacing Java2D rendering in TextOverlay.
 */
public class LegendLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(LegendLayerV2.class);

    /** Z-order for legend (renders after axis labels) */
    public static final int Z_ORDER = 920;

    // Configuration
    private LegendConfig config;
    private List<LegendItem> items = new ArrayList<>();

    // V2 API resources
    private Buffer backgroundBuffer;
    private Buffer swatchBuffer;
    private Shader colorShader;
    private boolean v2Initialized = false;

    // Vertex data
    private static final int MAX_ITEMS = 20;
    private final float[] bgVertices = new float[ShapeUtils.roundedRectFloatCount(4)];
    private final float[] swatchVertices = new float[MAX_ITEMS * ShapeUtils.RECT_VERTICES * ShapeUtils.FLOATS_PER_VERTEX];

    // Cached layout
    private float legendX, legendY;
    private float legendWidth, legendHeight;
    private boolean layoutDirty = true;

    public LegendLayerV2() {
        super(Z_ORDER);
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        log.debug("LegendLayerV2 GL initialized");
    }

    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        backgroundBuffer = resources.getOrCreateBuffer("legend.background",
                BufferDescriptor.positionColor2D(bgVertices.length));

        swatchBuffer = resources.getOrCreateBuffer("legend.swatches",
                BufferDescriptor.positionColor2D(swatchVertices.length));

        colorShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("LegendLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        if (config == null || !config.isVisible() || items.isEmpty()) {
            return;
        }

        // Filter visible items
        List<LegendItem> visibleItems = new ArrayList<>();
        for (LegendItem item : items) {
            if (item.isVisible()) {
                visibleItems.add(item);
            }
        }
        if (visibleItems.isEmpty()) {
            return;
        }

        if (!v2Initialized) {
            initializeV2(ctx);
        }

        ResourceManager resources = ctx.getResourceManager();
        TextRenderer textRenderer = resources.getTextRenderer();
        if (textRenderer == null) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        int width = viewport.getWidth();
        int height = viewport.getHeight();

        // Set scale factor for HiDPI displays
        textRenderer.setScaleFactor(ctx.getScaleFactor());

        // Calculate layout if needed
        if (layoutDirty) {
            calculateLayout(textRenderer, viewport, visibleItems);
            layoutDirty = false;
        }

        // Draw background
        if (config.isShowBackground()) {
            drawBackground(ctx);
        }

        // Draw swatches
        drawSwatches(ctx, textRenderer, visibleItems);

        // Draw text
        drawText(ctx, textRenderer, viewport, visibleItems);
    }

    private void calculateLayout(TextRenderer textRenderer, Viewport viewport, List<LegendItem> items) {
        textRenderer.setFontSize(config.getFont().getSize());

        int swatchSize = config.getColorSwatchSize();
        int swatchSpacing = config.getSwatchSpacing();
        int padding = config.getPadding();
        int itemSpacing = config.getItemSpacing();
        // Use actual rendered text height for layout
        float fontSize = textRenderer.getTextHeight();

        float totalWidth = padding * 2;
        float totalHeight = padding * 2;

        if (config.getOrientation() == LegendConfig.Orientation.HORIZONTAL) {
            for (int i = 0; i < items.size(); i++) {
                LegendItem item = items.get(i);
                float itemWidth = swatchSize + swatchSpacing + textRenderer.getTextWidth(item.getDisplayName());
                totalWidth += itemWidth;
                if (i > 0) {
                    totalWidth += itemSpacing;
                }
            }
            totalHeight += fontSize + 4;
        } else {
            float maxWidth = 0;
            for (LegendItem item : items) {
                float itemWidth = swatchSize + swatchSpacing + textRenderer.getTextWidth(item.getDisplayName());
                maxWidth = Math.max(maxWidth, itemWidth);
            }
            totalWidth += maxWidth;
            totalHeight += items.size() * (fontSize + 6) - 4;
        }

        legendWidth = totalWidth;
        legendHeight = totalHeight;

        // Calculate position
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();
        int marginX = config.getMarginX();
        int marginY = config.getMarginY();

        switch (config.getPosition()) {
            case TOP_LEFT:
                legendX = chartLeft + marginX;
                legendY = chartTop + marginY;
                break;
            case TOP_RIGHT:
                legendX = chartRight - legendWidth - marginX;
                legendY = chartTop + marginY;
                break;
            case BOTTOM_LEFT:
                legendX = chartLeft + marginX;
                legendY = chartBottom - legendHeight - marginY;
                break;
            case BOTTOM_RIGHT:
                legendX = chartRight - legendWidth - marginX;
                legendY = chartBottom - legendHeight - marginY;
                break;
            case TOP_CENTER:
                legendX = (chartLeft + chartRight - legendWidth) / 2;
                legendY = chartTop + marginY;
                break;
            case BOTTOM_CENTER:
                legendX = (chartLeft + chartRight - legendWidth) / 2;
                legendY = chartBottom - legendHeight - marginY;
                break;
            default:
                legendX = chartLeft + marginX;
                legendY = chartTop + marginY;
        }
    }

    private void drawBackground(RenderContext ctx) {
        if (colorShader == null || !colorShader.isValid()) {
            return;
        }

        Color bg = config.getBackgroundColor();
        float r = bg.getRed() / 255f;
        float g = bg.getGreen() / 255f;
        float b = bg.getBlue() / 255f;
        float a = bg.getAlpha() / 255f;

        int floatCount = ShapeUtils.tessellateRoundedRect(bgVertices, 0,
                legendX, legendY, legendWidth, legendHeight,
                config.getCornerRadius(), r, g, b, a);

        colorShader.bind();
        colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        backgroundBuffer.upload(bgVertices, 0, floatCount);
        backgroundBuffer.draw(DrawMode.TRIANGLES);

        colorShader.unbind();
    }

    private void drawSwatches(RenderContext ctx, TextRenderer textRenderer, List<LegendItem> items) {
        if (colorShader == null || !colorShader.isValid()) {
            return;
        }

        int swatchSize = config.getColorSwatchSize();
        int swatchSpacing = config.getSwatchSpacing();
        int padding = config.getPadding();
        int itemSpacing = config.getItemSpacing();
        // Use actual rendered text height for layout
        float fontSize = textRenderer.getTextHeight();

        float x = legendX + padding;
        float y = legendY + padding;

        int swatchIdx = 0;

        for (LegendItem item : items) {
            Color c = item.getColor();
            float r = c.getRed() / 255f;
            float g = c.getGreen() / 255f;
            float b = c.getBlue() / 255f;

            // Calculate swatch Y position (vertically centered with text)
            float swatchY = y + (fontSize - swatchSize) / 2 + 2;

            // Tessellate swatch based on series type
            if (swatchIdx + ShapeUtils.rectFloatCount() <= swatchVertices.length) {
                if (item.getType() == SeriesType.LINE) {
                    // Line: draw horizontal line
                    float lineY = swatchY + swatchSize / 2;
                    swatchIdx += ShapeUtils.tessellateRect(swatchVertices, swatchIdx,
                            x, lineY - 1, swatchSize, 2, r, g, b, 1f);
                } else {
                    // Bar/Candle: draw filled rectangle
                    swatchIdx += ShapeUtils.tessellateRect(swatchVertices, swatchIdx,
                            x, swatchY, swatchSize, swatchSize, r, g, b, 1f);
                }
            }

            // Move to next item position
            if (config.getOrientation() == LegendConfig.Orientation.HORIZONTAL) {
                float itemWidth = swatchSize + swatchSpacing + textRenderer.getTextWidth(item.getDisplayName());
                x += itemWidth + itemSpacing;
            } else {
                y += fontSize + 6;
            }
        }

        if (swatchIdx > 0) {
            colorShader.bind();
            colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            swatchBuffer.upload(swatchVertices, 0, swatchIdx);
            swatchBuffer.draw(DrawMode.TRIANGLES);

            colorShader.unbind();
        }
    }

    private void drawText(RenderContext ctx, TextRenderer textRenderer, Viewport viewport, List<LegendItem> items) {
        int width = viewport.getWidth();
        int height = viewport.getHeight();

        if (!textRenderer.beginBatch(width, height)) {
            return;
        }

        try {
            textRenderer.setFontSize(config.getFont().getSize());
            Color textColor = config.getTextColor();

            int swatchSize = config.getColorSwatchSize();
            int swatchSpacing = config.getSwatchSpacing();
            int padding = config.getPadding();
            int itemSpacing = config.getItemSpacing();
            // Use actual rendered text height for layout
            float fontSize = textRenderer.getTextHeight();

            float x = legendX + padding;
            float y = legendY + padding;

            for (LegendItem item : items) {
                float textX = x + swatchSize + swatchSpacing;
                float textY = y + fontSize;

                textRenderer.drawText(item.getDisplayName(), textX, textY, textColor);

                // Move to next item position
                if (config.getOrientation() == LegendConfig.Orientation.HORIZONTAL) {
                    float itemWidth = swatchSize + swatchSpacing + textRenderer.getTextWidth(item.getDisplayName());
                    x += itemWidth + itemSpacing;
                } else {
                    y += fontSize + 6;
                }
            }
        } finally {
            textRenderer.endBatch();
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        v2Initialized = false;
    }

    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            ResourceManager resources = ctx.getResourceManager();
            if (resources != null) {
                resources.disposeBuffer("legend.background");
                resources.disposeBuffer("legend.swatches");
            }
            backgroundBuffer = null;
            swatchBuffer = null;
            colorShader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setConfig(LegendConfig config) {
        this.config = config;
        layoutDirty = true;
        markDirty();
    }

    public LegendConfig getConfig() {
        return config;
    }

    public void setItems(List<LegendItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        layoutDirty = true;
        markDirty();
    }

    public List<LegendItem> getItems() {
        return items;
    }

    public void markLayoutDirty() {
        layoutDirty = true;
        markDirty();
    }
}
