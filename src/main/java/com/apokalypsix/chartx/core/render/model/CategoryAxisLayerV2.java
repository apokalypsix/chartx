package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for category axis using the abstracted rendering API.
 *
 * <p>Renders the category axis background, line, and tick marks for categorical
 * charts like population pyramids and bar charts. Text labels are rendered
 * separately by AxisLabelLayerV2 using GPU text rendering.
 */
public class CategoryAxisLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(CategoryAxisLayerV2.class);

    /** Z-order for category axis layer (renders after Y-axis, before axis labels) */
    public static final int Z_ORDER = 860;

    // Colors
    private Color axisColor = new Color(80, 82, 86);
    private Color backgroundColor = new Color(20, 22, 25);

    // V2 API resources
    private Buffer backgroundBuffer;
    private Buffer lineBuffer;
    private Shader shader;
    private boolean v2Initialized = false;

    // Vertex data
    private float[] lineVertices;
    private int vertexCapacity;

    // Tick mark size
    private static final int TICK_SIZE = 4;

    // Floats per vertex: x, y
    private static final int FLOATS_PER_VERTEX = 2;

    public CategoryAxisLayerV2() {
        super(Z_ORDER);
        vertexCapacity = 128;
        lineVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        log.debug("CategoryAxisLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for axis backgrounds (triangles)
        backgroundBuffer = resources.getOrCreateBuffer("categoryAxis.background",
                BufferDescriptor.positionOnly2D(24 * FLOATS_PER_VERTEX));

        // Create buffer for lines and ticks
        lineBuffer = resources.getOrCreateBuffer("categoryAxis.lines",
                BufferDescriptor.positionOnly2D(256 * FLOATS_PER_VERTEX));

        // Get simple shader (position only, uniform color)
        shader = resources.getShader(ResourceManager.SHADER_SIMPLE);

        v2Initialized = true;
        log.debug("CategoryAxisLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if there's a category axis to render
        CategoryAxis categoryAxis = ctx.getCategoryAxis();
        if (categoryAxis == null || !categoryAxis.isVisible() || categoryAxis.getCategoryCount() == 0) {
            return;
        }

        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("CategoryAxisLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();
        float scaleFactor = ctx.getScaleFactor();

        // Draw axis background
        drawAxisBackground(ctx, viewport, categoryAxis, scaleFactor);

        // Draw axis line and tick marks
        drawAxisLines(ctx, viewport, categoryAxis, scaleFactor);
    }

    private void drawAxisBackground(RenderContext ctx, Viewport viewport, CategoryAxis categoryAxis, float scaleFactor) {
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        int chartTop = viewport.getTopInset();
        int chartBottom = height - viewport.getBottomInset();

        if (shader == null || !shader.isValid()) {
            return;
        }

        // Scale axis width for HiDPI
        int axisWidth = (int) (categoryAxis.getHeight() * scaleFactor);

        float[] bgVertices = new float[12]; // 1 rectangle = 6 vertices * 2 floats
        int idx = 0;

        // Draw background based on position
        switch (categoryAxis.getInternalPosition()) {
            case LEFT:
                // Left axis background - positioned to the left of chart area
                idx = addRect(bgVertices, idx, 0, chartTop, axisWidth, chartBottom);
                break;
            case RIGHT:
                // Right axis background
                int chartRight = width - viewport.getRightInset();
                idx = addRect(bgVertices, idx, chartRight, chartTop, chartRight + axisWidth, chartBottom);
                break;
            case TOP:
                // Top axis background
                int chartLeft = viewport.getLeftInset();
                int chartRightH = width - viewport.getRightInset();
                idx = addRect(bgVertices, idx, chartLeft, 0, chartRightH, axisWidth);
                break;
            case BOTTOM:
                // Bottom axis background
                int chartLeftB = viewport.getLeftInset();
                int chartRightB = width - viewport.getRightInset();
                idx = addRect(bgVertices, idx, chartLeftB, chartBottom, chartRightB, chartBottom + axisWidth);
                break;
        }

        if (idx == 0) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        shader.setUniform("uColor",
                backgroundColor.getRed() / 255f,
                backgroundColor.getGreen() / 255f,
                backgroundColor.getBlue() / 255f,
                1.0f);

        backgroundBuffer.upload(bgVertices, 0, idx);
        backgroundBuffer.draw(DrawMode.TRIANGLES);

        shader.unbind();
    }

    private void drawAxisLines(RenderContext ctx, Viewport viewport, CategoryAxis categoryAxis, float scaleFactor) {
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        int chartTop = viewport.getTopInset();
        int chartBottom = height - viewport.getBottomInset();
        int chartLeft = viewport.getLeftInset();
        int chartRight = width - viewport.getRightInset();

        // Scale axis width for HiDPI
        int axisWidth = (int) (categoryAxis.getHeight() * scaleFactor);

        // Calculate how many ticks we need (one per category)
        int categoryCount = categoryAxis.getCategoryCount();
        ensureCapacity(categoryCount + 4);

        int floatIndex = 0;

        // Use the coordinate system for viewport-aware positioning
        var coords = ctx.getCoordinates();

        switch (categoryAxis.getInternalPosition()) {
            case LEFT:
                // Axis line at the right edge of the category axis area
                floatIndex = addLine(lineVertices, floatIndex, axisWidth, chartTop, axisWidth, chartBottom);

                // Tick marks for each category
                // Use timeToScreenX since categories are indexed via time/X dimension
                for (int i = 0; i < categoryCount; i++) {
                    float y = (float) coords.xValueToScreenX((long) i);
                    if (y >= chartTop && y <= chartBottom) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                axisWidth - TICK_SIZE, y, axisWidth, y);
                    }
                }
                break;

            case RIGHT:
                // Axis line at the left edge of the right axis area
                floatIndex = addLine(lineVertices, floatIndex, chartRight, chartTop, chartRight, chartBottom);

                // Tick marks for each category
                // Use timeToScreenX since categories are indexed via time/X dimension
                for (int i = 0; i < categoryCount; i++) {
                    float y = (float) coords.xValueToScreenX((long) i);
                    if (y >= chartTop && y <= chartBottom) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                chartRight, y, chartRight + TICK_SIZE, y);
                    }
                }
                break;

            case TOP:
                // Axis line at the bottom edge of the top axis area
                floatIndex = addLine(lineVertices, floatIndex, chartLeft, axisWidth, chartRight, axisWidth);

                // Tick marks for each category (use timeToScreenX for horizontal axis)
                for (int i = 0; i < categoryCount; i++) {
                    float x = (float) coords.xValueToScreenX((long) i);
                    // Add 0.5 bar width offset for centering
                    x += (float) (ctx.getBarWidth() / 2.0);
                    if (x >= chartLeft && x <= chartRight) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                x, axisWidth - TICK_SIZE, x, axisWidth);
                    }
                }
                break;

            case BOTTOM:
                // Axis line at the top edge of the bottom axis area
                floatIndex = addLine(lineVertices, floatIndex, chartLeft, chartBottom, chartRight, chartBottom);

                // Tick marks for each category (use timeToScreenX for horizontal axis)
                for (int i = 0; i < categoryCount; i++) {
                    float x = (float) coords.xValueToScreenX((long) i);
                    // Add 0.5 bar width offset for centering
                    x += (float) (ctx.getBarWidth() / 2.0);
                    if (x >= chartLeft && x <= chartRight) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                x, chartBottom, x, chartBottom + TICK_SIZE);
                    }
                }
                break;
        }

        if (floatIndex == 0) {
            return;
        }

        if (shader == null || !shader.isValid()) {
            return;
        }

        Color lineColor = categoryAxis.getLineColor();
        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        shader.setUniform("uColor",
                lineColor.getRed() / 255f,
                lineColor.getGreen() / 255f,
                lineColor.getBlue() / 255f,
                1.0f);

        lineBuffer.upload(lineVertices, 0, floatIndex);
        lineBuffer.draw(DrawMode.LINES);

        shader.unbind();
    }

    private int addLine(float[] vertices, int index, float x1, float y1, float x2, float y2) {
        vertices[index++] = x1;
        vertices[index++] = y1;
        vertices[index++] = x2;
        vertices[index++] = y2;
        return index;
    }

    private int addRect(float[] vertices, int index, float x1, float y1, float x2, float y2) {
        // Triangle 1
        vertices[index++] = x1; vertices[index++] = y1;
        vertices[index++] = x1; vertices[index++] = y2;
        vertices[index++] = x2; vertices[index++] = y2;
        // Triangle 2
        vertices[index++] = x1; vertices[index++] = y1;
        vertices[index++] = x2; vertices[index++] = y2;
        vertices[index++] = x2; vertices[index++] = y1;
        return index;
    }

    private void ensureCapacity(int lineCount) {
        int requiredFloats = lineCount * 2 * FLOATS_PER_VERTEX;
        if (requiredFloats > lineVertices.length) {
            vertexCapacity = lineCount + lineCount / 2;
            lineVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        v2Initialized = false;
    }

    /**
     * Disposes V2 resources.
     */
    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            ResourceManager resources = ctx.getResourceManager();
            if (resources != null) {
                resources.disposeBuffer("categoryAxis.background");
                resources.disposeBuffer("categoryAxis.lines");
            }
            backgroundBuffer = null;
            lineBuffer = null;
            shader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setAxisColor(Color color) {
        this.axisColor = color;
    }

    public Color getAxisColor() {
        return axisColor;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }
}
