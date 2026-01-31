package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
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
 * Render layer for crosshair cursor lines and labels using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link CrosshairLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Draws vertical and horizontal lines at the cursor position, plus
 * price labels on Y-axes and time labels on the X-axis.
 */
public class CrosshairLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(CrosshairLayerV2.class);

    /** Z-order for the crosshair layer (renders on top of data, below axes) */
    public static final int Z_ORDER = 800;

    // Cursor position (in screen coordinates)
    private int cursorX = -1;
    private int cursorY = -1;
    private boolean cursorVisible = false;

    // Colors
    private Color lineColor = new Color(100, 102, 106);
    private Color labelTextColor = new Color(220, 222, 226);
    private Color labelBackgroundColor = new Color(60, 62, 66);

    // Label configuration
    private static final int LABEL_PADDING = 4;
    private float fontSize = 11f;

    // Time formatter
    private final SimpleDateFormat timeFormatSeconds = new SimpleDateFormat("HH:mm:ss");
    private final Date reusableDate = new Date();

    // V2 API resources
    private Buffer lineBuffer;
    private Buffer labelBgBuffer;
    private Shader shader;
    private Shader colorShader;
    private boolean v2Initialized = false;


    // Vertex data - reused each frame
    private final float[] lineVertices = new float[8]; // 2 lines * 2 vertices * 2 floats

    // Label background vertices (max 10 labels * 6 vertices * 6 floats)
    private static final int MAX_LABELS = 10;
    private final float[] labelBgVertices = new float[MAX_LABELS * ShapeUtils.RECT_VERTICES * ShapeUtils.FLOATS_PER_VERTEX];

    // Floats per vertex: x, y
    private static final int FLOATS_PER_VERTEX = 2;

    public CrosshairLayerV2() {
        super(Z_ORDER);
        timeFormatSeconds.setTimeZone(TimeZone.getDefault());
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("CrosshairLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create line buffer for crosshair (4 vertices * 2 floats per vertex)
        lineBuffer = resources.getOrCreateBuffer("crosshair.lines",
                BufferDescriptor.positionOnly2D(16 * FLOATS_PER_VERTEX));

        // Create buffer for label backgrounds (position + color)
        labelBgBuffer = resources.getOrCreateBuffer("crosshair.labelBg",
                BufferDescriptor.positionColor2D(labelBgVertices.length));

        // Get simple shader (position only, uniform color) for lines
        shader = resources.getShader(ResourceManager.SHADER_SIMPLE);

        // Get position-color shader for label backgrounds
        colorShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("CrosshairLayerV2 V2 resources initialized");
    }

    /**
     * Updates the cursor position.
     *
     * @param x X coordinate in screen pixels
     * @param y Y coordinate in screen pixels
     */
    public void setCursorPosition(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
        markDirty();
    }

    /**
     * Returns the current cursor X position.
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Returns the current cursor Y position.
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Shows or hides the crosshair.
     */
    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
        markDirty();
    }

    /**
     * Returns true if the crosshair is visible.
     */
    public boolean isCursorVisible() {
        return cursorVisible;
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("CrosshairLayerV2 requires abstracted API - skipping render");
            return;
        }

        if (!cursorVisible || cursorX < 0 || cursorY < 0) {
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();
        float scaleFactor = ctx.getScaleFactor();

        int width = viewport.getWidth();
        int height = viewport.getHeight();
        // Insets are already in physical pixels (scaled when set)
        int chartLeft = viewport.getLeftInset();
        int chartRight = width - viewport.getRightInset();
        int chartTop = viewport.getTopInset();
        int chartBottom = height - viewport.getBottomInset();

        // Only draw if cursor is in chart area
        if (cursorX < chartLeft || cursorX > chartRight ||
            cursorY < chartTop || cursorY > chartBottom) {
            return;
        }

        // Draw crosshair lines
        drawCrosshairLines(ctx, chartLeft, chartRight, chartTop, chartBottom);

        // Draw crosshair labels
        drawCrosshairLabels(ctx, chartLeft, chartRight, chartTop, chartBottom);
    }

    private void drawCrosshairLines(RenderContext ctx,
                                     int chartLeft, int chartRight,
                                     int chartTop, int chartBottom) {
        if (shader == null || !shader.isValid()) {
            return;
        }

        int idx = 0;

        // Vertical line
        lineVertices[idx++] = cursorX;
        lineVertices[idx++] = chartTop;
        lineVertices[idx++] = cursorX;
        lineVertices[idx++] = chartBottom;

        // Horizontal line
        lineVertices[idx++] = chartLeft;
        lineVertices[idx++] = cursorY;
        lineVertices[idx++] = chartRight;
        lineVertices[idx++] = cursorY;

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        shader.setUniform("uColor",
                lineColor.getRed() / 255f,
                lineColor.getGreen() / 255f,
                lineColor.getBlue() / 255f,
                0.8f);

        lineBuffer.upload(lineVertices, 0, idx);
        lineBuffer.draw(DrawMode.LINES);

        shader.unbind();
    }

    // Reusable arrays for label data (avoid allocations)
    private static final int MAX_LABEL_DATA = 10;
    private final String[] labelTexts = new String[MAX_LABEL_DATA];
    private final float[] labelTextX = new float[MAX_LABEL_DATA];
    private final float[] labelTextY = new float[MAX_LABEL_DATA];
    private final boolean[] labelCentered = new boolean[MAX_LABEL_DATA];

    private void drawCrosshairLabels(RenderContext ctx,
                                      int chartLeft, int chartRight,
                                      int chartTop, int chartBottom) {
        ResourceManager resources = ctx.getResourceManager();
        TextRenderer textRenderer = resources.getTextRenderer();
        if (textRenderer == null) {
            return;
        }

        YAxisManager axisManager = ctx.getAxisManager();
        if (axisManager == null) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinates();

        // Get time at cursor
        long time = coords.screenXToXValue(cursorX);
        reusableDate.setTime(time);
        String timeLabel = timeFormatSeconds.format(reusableDate);

        // Background color
        float bgR = labelBackgroundColor.getRed() / 255f;
        float bgG = labelBackgroundColor.getGreen() / 255f;
        float bgB = labelBackgroundColor.getBlue() / 255f;
        float bgA = 1.0f;

        // =================================================================
        // PASS 1: Calculate all label positions and collect background rects
        // =================================================================
        int bgIdx = 0;
        int labelCount = 0;

        // Set scale factor for HiDPI displays and font size
        float scaleFactor = ctx.getScaleFactor();
        textRenderer.setScaleFactor(scaleFactor);
        textRenderer.setFontSize(fontSize);

        // Use actual rendered text height for layout
        float textHeight = textRenderer.getTextHeight();

        // Scale padding for HiDPI
        float padding = LABEL_PADDING * scaleFactor;

        // Get visible axes
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);
        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);

        // Scale edge offset for HiDPI
        float edgeOffset = 2 * scaleFactor;

        // Process right axis labels
        int rightX = chartRight;
        for (YAxis axis : rightAxes) {
            // Scale axis width from logical to physical pixels
            int axisWidth = (int)(axis.getWidth() * scaleFactor);
            CoordinateSystem axisCoords = ctx.getCoordinatesForAxis(axis.getId());
            double price = axisCoords.screenYToYValue(cursorY);
            String priceLabel = axis.formatValue(price);

            float labelWidth = textRenderer.getTextWidth(priceLabel);
            float labelHeight = textHeight + padding;
            float labelX = rightX + edgeOffset;
            float labelY = cursorY - labelHeight / 2;
            float labelW = labelWidth + padding * 2;
            float labelH = labelHeight + padding;

            // Add background rect
            if (bgIdx + ShapeUtils.rectFloatCount() <= labelBgVertices.length) {
                bgIdx += ShapeUtils.tessellateRect(labelBgVertices, bgIdx,
                        labelX, labelY, labelW, labelH, bgR, bgG, bgB, bgA);
            }

            // Store text info for pass 2
            if (labelCount < MAX_LABEL_DATA) {
                labelTexts[labelCount] = priceLabel;
                labelTextX[labelCount] = labelX + padding;
                labelTextY[labelCount] = cursorY + textHeight / 3;
                labelCentered[labelCount] = false;
                labelCount++;
            }

            rightX += axisWidth;
        }

        // Process left axis labels
        int leftX = chartLeft;
        for (int i = leftAxes.size() - 1; i >= 0; i--) {
            YAxis axis = leftAxes.get(i);
            // Scale axis width from logical to physical pixels
            int axisWidth = (int)(axis.getWidth() * scaleFactor);
            leftX -= axisWidth;

            CoordinateSystem axisCoords = ctx.getCoordinatesForAxis(axis.getId());
            double price = axisCoords.screenYToYValue(cursorY);
            String priceLabel = axis.formatValue(price);

            float labelWidth = textRenderer.getTextWidth(priceLabel);
            float labelHeight = textHeight + padding;
            float labelW = labelWidth + padding * 2;
            float labelH = labelHeight + padding;
            float labelX = leftX + axisWidth - labelW - edgeOffset;
            float labelY = cursorY - labelHeight / 2;

            // Add background rect
            if (bgIdx + ShapeUtils.rectFloatCount() <= labelBgVertices.length) {
                bgIdx += ShapeUtils.tessellateRect(labelBgVertices, bgIdx,
                        labelX, labelY, labelW, labelH, bgR, bgG, bgB, bgA);
            }

            // Store text info for pass 2
            if (labelCount < MAX_LABEL_DATA) {
                labelTexts[labelCount] = priceLabel;
                labelTextX[labelCount] = labelX + padding;
                labelTextY[labelCount] = cursorY + textHeight / 3;
                labelCentered[labelCount] = false;
                labelCount++;
            }
        }

        // Process time label (bottom axis)
        float timeLabelWidth = textRenderer.getTextWidth(timeLabel);
        float timeLabelHeight = textHeight + padding;
        float timeLabelW = timeLabelWidth + padding * 2;
        float timeLabelH = timeLabelHeight + padding;
        float timeLabelX = cursorX - timeLabelW / 2;
        float timeLabelY = chartBottom + edgeOffset;

        // Add time label background
        if (bgIdx + ShapeUtils.rectFloatCount() <= labelBgVertices.length) {
            bgIdx += ShapeUtils.tessellateRect(labelBgVertices, bgIdx,
                    timeLabelX, timeLabelY, timeLabelW, timeLabelH, bgR, bgG, bgB, bgA);
        }

        // Store time label text info for pass 2
        if (labelCount < MAX_LABEL_DATA) {
            labelTexts[labelCount] = timeLabel;
            labelTextX[labelCount] = cursorX;
            labelTextY[labelCount] = timeLabelY + textHeight + padding / 2;
            labelCentered[labelCount] = true;
            labelCount++;
        }

        // =================================================================
        // PASS 2: Draw backgrounds FIRST
        // =================================================================
        if (bgIdx > 0 && colorShader != null && colorShader.isValid()) {
            colorShader.bind();
            colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            labelBgBuffer.upload(labelBgVertices, 0, bgIdx);
            labelBgBuffer.draw(DrawMode.TRIANGLES);

            colorShader.unbind();
        }

        // =================================================================
        // PASS 3: Draw text AFTER backgrounds
        // =================================================================
        if (labelCount > 0 && textRenderer.beginBatch(viewport.getWidth(), viewport.getHeight())) {
            try {
                textRenderer.setFontSize(fontSize);

                for (int i = 0; i < labelCount; i++) {
                    if (labelCentered[i]) {
                        textRenderer.drawTextCentered(labelTexts[i], labelTextX[i], labelTextY[i], labelTextColor);
                    } else {
                        textRenderer.drawText(labelTexts[i], labelTextX[i], labelTextY[i], labelTextColor);
                    }
                }
            } finally {
                textRenderer.endBatch();
            }
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
                resources.disposeBuffer("crosshair.lines");
                resources.disposeBuffer("crosshair.labelBg");
            }
            lineBuffer = null;
            labelBgBuffer = null;
            shader = null;
            colorShader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setLineColor(Color color) {
        this.lineColor = color;
    }

    public Color getLineColor() {
        return lineColor;
    }
}
