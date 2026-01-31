package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.style.TooltipConfig;
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
 * Render layer for tooltips using GPU text rendering.
 *
 * <p>Renders tooltip with data values at cursor position directly on the GPU
 * using the TextRenderer API, replacing Java2D rendering in TextOverlay.
 */
public class TooltipLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(TooltipLayerV2.class);

    /** Z-order for tooltip (highest, always on top) */
    public static final int Z_ORDER = 950;

    // Configuration
    private TooltipConfig config;
    private TooltipData data;
    private int tooltipX, tooltipY;
    private boolean visible = false;

    // V2 API resources
    private Buffer backgroundBuffer;
    private Buffer swatchBuffer;
    private Shader colorShader;
    private boolean v2Initialized = false;

    // Vertex data
    private final float[] bgVertices = new float[ShapeUtils.roundedRectFloatCount(6)];
    private static final int MAX_ROWS = 20;
    private final float[] swatchVertices = new float[MAX_ROWS * ShapeUtils.RECT_VERTICES * ShapeUtils.FLOATS_PER_VERTEX];

    // Time formatter
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final Date reusableDate = new Date();

    public TooltipLayerV2() {
        super(Z_ORDER);
        timeFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        log.debug("TooltipLayerV2 GL initialized");
    }

    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        backgroundBuffer = resources.getOrCreateBuffer("tooltip.background",
                BufferDescriptor.positionColor2D(bgVertices.length));

        swatchBuffer = resources.getOrCreateBuffer("tooltip.swatches",
                BufferDescriptor.positionColor2D(swatchVertices.length));

        colorShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("TooltipLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        if (!visible || config == null || !config.isEnabled() || data == null || data.isEmpty()) {
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

        // Set scale factor for HiDPI displays and calculate tooltip dimensions
        float scaleFactor = ctx.getScaleFactor();
        textRenderer.setScaleFactor(scaleFactor);
        textRenderer.setFontSize(config.getFont().getSize());
        float[] dims = calculateTooltipDimensions(textRenderer, scaleFactor);
        float tooltipWidth = dims[0];
        float tooltipHeight = dims[1];

        // Calculate position with bounds checking
        float posX = calculateTooltipX(tooltipWidth, width, viewport, scaleFactor);
        float posY = calculateTooltipY(tooltipHeight, height, viewport, scaleFactor);

        // Draw background
        drawBackground(ctx, posX, posY, tooltipWidth, tooltipHeight, scaleFactor);

        // Draw content
        drawContent(ctx, textRenderer, viewport, posX, posY, scaleFactor);
    }

    private float[] calculateTooltipDimensions(TextRenderer textRenderer, float scaleFactor) {
        // Scale config values for HiDPI
        int padding = (int)(config.getPadding() * scaleFactor);
        int rowSpacing = (int)(config.getRowSpacing() * scaleFactor);
        // Use actual rendered text height for layout
        float fontSize = textRenderer.getTextHeight();
        textRenderer.setFontSize(config.getLabelFont().getSize());
        float labelFontSize = textRenderer.getTextHeight();
        // Restore main font size
        textRenderer.setFontSize(config.getFont().getSize());

        float maxWidth = 0;
        float totalHeight = padding * 2;

        // Scale swatch and spacing dimensions
        float swatchSize = 12 * scaleFactor;
        float swatchSpacing = 5 * scaleFactor;
        float labelValueSpacing = 3 * scaleFactor;
        float headerExtra = 2 * scaleFactor;

        // Add timestamp row if enabled
        if (config.isShowTimestamp() && data.getTimestamp() > 0) {
            reusableDate.setTime(data.getTimestamp());
            String timeStr = timeFormat.format(reusableDate);
            maxWidth = Math.max(maxWidth, textRenderer.getTextWidth(timeStr));
            totalHeight += fontSize + rowSpacing;
        }

        // Calculate dimensions for each row
        for (TooltipData.TooltipRow row : data.getRows()) {
            float rowWidth = 0;

            // Swatch space
            if (row.getColor() != null) {
                rowWidth += swatchSize + swatchSpacing;
            }

            // Label
            if (row.getLabel() != null) {
                rowWidth += textRenderer.getTextWidth(row.getLabel()) + labelValueSpacing;
            }

            // Value
            if (row.getValue() != null) {
                rowWidth += textRenderer.getTextWidth(row.getValue());
            }

            maxWidth = Math.max(maxWidth, rowWidth);
            totalHeight += (row.isHeader() ? fontSize + headerExtra : fontSize) + rowSpacing;
        }

        totalHeight -= rowSpacing; // Remove last spacing
        return new float[]{maxWidth + padding * 2, totalHeight};
    }

    private float calculateTooltipX(float tooltipWidth, int viewportWidth, Viewport viewport, float scaleFactor) {
        // Scale offset for HiDPI
        float offsetX = config.getOffsetX() * scaleFactor;
        float x = tooltipX + offsetX;

        // Insets are already in physical pixels (scaled when set)
        int leftInset = viewport.getLeftInset();
        int chartRight = viewportWidth - viewport.getRightInset();

        // Avoid going off-screen
        if (x + tooltipWidth > chartRight) {
            x = tooltipX - tooltipWidth - offsetX;
        }
        if (x < leftInset) {
            x = leftInset;
        }

        return x;
    }

    private float calculateTooltipY(float tooltipHeight, int viewportHeight, Viewport viewport, float scaleFactor) {
        // Scale offset for HiDPI
        float offsetY = config.getOffsetY() * scaleFactor;
        float y = tooltipY + offsetY;

        // Insets are already in physical pixels (scaled when set)
        int topInset = viewport.getTopInset();
        int chartBottom = viewportHeight - viewport.getBottomInset();

        // Avoid going off-screen
        if (y + tooltipHeight > chartBottom) {
            y = tooltipY - tooltipHeight - offsetY;
        }
        if (y < topInset) {
            y = topInset;
        }

        return y;
    }

    private void drawBackground(RenderContext ctx, float x, float y, float w, float h, float scaleFactor) {
        if (colorShader == null || !colorShader.isValid()) {
            return;
        }

        Color bg = config.getBackgroundColor();
        float r = bg.getRed() / 255f;
        float g = bg.getGreen() / 255f;
        float b = bg.getBlue() / 255f;
        float a = bg.getAlpha() / 255f;

        // Scale corner radius for HiDPI
        int cornerRadius = (int)(config.getCornerRadius() * scaleFactor);
        int floatCount = ShapeUtils.tessellateRoundedRect(bgVertices, 0,
                x, y, w, h, cornerRadius, r, g, b, a);

        colorShader.bind();
        colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        backgroundBuffer.upload(bgVertices, 0, floatCount);
        backgroundBuffer.draw(DrawMode.TRIANGLES);

        colorShader.unbind();
    }

    private void drawContent(RenderContext ctx, TextRenderer textRenderer, Viewport viewport, float posX, float posY, float scaleFactor) {
        int width = viewport.getWidth();
        int height = viewport.getHeight();

        // Scale config values for HiDPI
        int padding = (int)(config.getPadding() * scaleFactor);
        int rowSpacing = (int)(config.getRowSpacing() * scaleFactor);
        float swatchSize = 8 * scaleFactor;
        float swatchOffset = 4 * scaleFactor;
        float swatchSpacing = 12 * scaleFactor + 5 * scaleFactor; // swatch width + gap
        float labelValueSpacing = 3 * scaleFactor;
        float headerExtra = 2 * scaleFactor;

        // Use actual rendered text height for layout
        textRenderer.setFontSize(config.getFont().getSize());
        float fontSize = textRenderer.getTextHeight();

        float x = posX + padding;
        float y = posY + padding;

        // Collect swatches
        int swatchIdx = 0;
        List<TooltipData.TooltipRow> rows = data.getRows();

        // Draw timestamp first if enabled
        float textStartY = y;
        if (config.isShowTimestamp() && data.getTimestamp() > 0) {
            textStartY += fontSize + rowSpacing;
        }

        // Collect swatch positions
        float swatchY = textStartY;
        for (TooltipData.TooltipRow row : rows) {
            if (row.getColor() != null) {
                Color c = row.getColor();
                float r = c.getRed() / 255f;
                float g = c.getGreen() / 255f;
                float b = c.getBlue() / 255f;

                float swatchCenterY = swatchY + fontSize / 2 - swatchOffset;
                if (swatchIdx + ShapeUtils.rectFloatCount() <= swatchVertices.length) {
                    swatchIdx += ShapeUtils.tessellateRect(swatchVertices, swatchIdx,
                            x, swatchCenterY, swatchSize, swatchSize, r, g, b, 1f);
                }
            }
            swatchY += (row.isHeader() ? fontSize + headerExtra : fontSize) + rowSpacing;
        }

        // Draw swatches
        if (swatchIdx > 0 && colorShader != null && colorShader.isValid()) {
            colorShader.bind();
            colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            swatchBuffer.upload(swatchVertices, 0, swatchIdx);
            swatchBuffer.draw(DrawMode.TRIANGLES);

            colorShader.unbind();
        }

        // Draw text
        if (!textRenderer.beginBatch(width, height)) {
            return;
        }

        try {
            textRenderer.setFontSize(config.getFont().getSize());
            Color textColor = config.getTextColor();
            Color labelColor = config.getLabelColor();

            float textY = y + fontSize;

            // Draw timestamp
            if (config.isShowTimestamp() && data.getTimestamp() > 0) {
                reusableDate.setTime(data.getTimestamp());
                String timeStr = timeFormat.format(reusableDate);
                textRenderer.drawText(timeStr, x, textY, labelColor);
                textY += fontSize + rowSpacing;
            }

            // Draw rows
            for (TooltipData.TooltipRow row : rows) {
                float textX = x;

                // Skip space for swatch
                if (row.getColor() != null) {
                    textX += swatchSpacing;
                }

                // Draw label
                if (row.getLabel() != null) {
                    textRenderer.drawText(row.getLabel(), textX, textY, labelColor);
                    textX += textRenderer.getTextWidth(row.getLabel()) + labelValueSpacing;
                }

                // Draw value
                if (row.getValue() != null) {
                    textRenderer.drawText(row.getValue(), textX, textY,
                            row.isHeader() ? textColor : textColor);
                }

                textY += (row.isHeader() ? fontSize + headerExtra : fontSize) + rowSpacing;
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
                resources.disposeBuffer("tooltip.background");
                resources.disposeBuffer("tooltip.swatches");
            }
            backgroundBuffer = null;
            swatchBuffer = null;
            colorShader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setConfig(TooltipConfig config) {
        this.config = config;
        if (config != null) {
            timeFormat.applyPattern(config.getTimestampFormat());
        }
        markDirty();
    }

    public TooltipConfig getConfig() {
        return config;
    }

    public void setData(TooltipData data) {
        this.data = data;
        markDirty();
    }

    public TooltipData getData() {
        return data;
    }

    public void setPosition(int x, int y) {
        this.tooltipX = x;
        this.tooltipY = y;
        markDirty();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        markDirty();
    }

    public boolean isVisible() {
        return visible;
    }
}
