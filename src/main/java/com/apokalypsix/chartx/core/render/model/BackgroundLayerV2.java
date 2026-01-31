package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for background, grid lines, and chart border using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link BackgroundLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Supports solid color or gradient backgrounds. Use {@link #setGradient(GradientType, Color, Color)}
 * to enable gradient backgrounds, or {@link #setChartAreaColor(Color)} for solid backgrounds.
 */
public class BackgroundLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(BackgroundLayerV2.class);

    /** Z-order for the background layer (renders first) */
    public static final int Z_ORDER = 0;

    /** Types of background gradients */
    public enum GradientType {
        /** No gradient - solid color background */
        NONE,
        /** Vertical gradient from top to bottom */
        VERTICAL,
        /** Horizontal gradient from left to right */
        HORIZONTAL,
        /** Diagonal gradient from top-left to bottom-right */
        DIAGONAL
    }

    private Color chartAreaColor = new Color(25, 27, 30);
    private Color gridColor = new Color(50, 52, 55);
    private Color borderColor = new Color(60, 62, 65);

    // Gradient support
    private GradientType gradientType = GradientType.NONE;
    private Color gradientStartColor = new Color(30, 35, 45);
    private Color gradientEndColor = new Color(20, 22, 25);

    private boolean showGrid = false;
    private boolean showBorder = true;

    private int horizontalGridLines = 8;
    private int verticalGridLines = 10;

    // V2 API resources
    private Buffer solidBuffer;      // For solid backgrounds, grid, and border (position only)
    private Buffer gradientBuffer;   // For gradient backgrounds (position + color)
    private Shader simpleShader;     // Position only, uniform color
    private Shader defaultShader;    // Position + per-vertex color
    private boolean v2Initialized = false;

    // Floats per vertex for gradient: x, y, r, g, b, a
    private static final int GRADIENT_FLOATS_PER_VERTEX = 6;

    public BackgroundLayerV2() {
        super(Z_ORDER);
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("BackgroundLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for solid background, grid, and border (position only)
        solidBuffer = resources.getOrCreateBuffer("background.solid",
                BufferDescriptor.positionOnly2D(256));

        // Create buffer for gradient background (position + color)
        gradientBuffer = resources.getOrCreateBuffer("background.gradient",
                BufferDescriptor.positionColor2D(36 * GRADIENT_FLOATS_PER_VERTEX));

        // Get shaders
        simpleShader = resources.getShader(ResourceManager.SHADER_SIMPLE);
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("BackgroundLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("BackgroundLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();

        // Draw chart area background
        drawChartAreaBackground(ctx, viewport);

        // Draw grid
        if (showGrid) {
            drawGrid(ctx, viewport);
        }

        // Draw border
        if (showBorder) {
            drawBorder(ctx, viewport);
        }
    }

    private void drawChartAreaBackground(RenderContext ctx, Viewport viewport) {
        float left = viewport.getLeftInset();
        float right = viewport.getWidth() - viewport.getRightInset();
        float top = viewport.getTopInset();
        float bottom = viewport.getHeight() - viewport.getBottomInset();

        if (gradientType == GradientType.NONE) {
            // Solid color background using simple shader
            if (simpleShader == null || !simpleShader.isValid()) {
                return;
            }

            float[] vertices = {
                    left, top,
                    left, bottom,
                    right, bottom,
                    left, top,
                    right, bottom,
                    right, top
            };

            simpleShader.bind();
            simpleShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
            simpleShader.setUniform("uColor",
                    chartAreaColor.getRed() / 255f,
                    chartAreaColor.getGreen() / 255f,
                    chartAreaColor.getBlue() / 255f,
                    1.0f);

            solidBuffer.upload(vertices, 0, vertices.length);
            solidBuffer.draw(DrawMode.TRIANGLES);
            simpleShader.unbind();
        } else {
            // Gradient background using default shader with per-vertex colors
            if (defaultShader == null || !defaultShader.isValid()) {
                return;
            }

            float[] vertices = buildGradientVertices(left, top, right, bottom);

            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            gradientBuffer.upload(vertices, 0, vertices.length);
            gradientBuffer.draw(DrawMode.TRIANGLES);
            defaultShader.unbind();
        }
    }

    private float[] buildGradientVertices(float left, float top, float right, float bottom) {
        float sr = gradientStartColor.getRed() / 255f;
        float sg = gradientStartColor.getGreen() / 255f;
        float sb = gradientStartColor.getBlue() / 255f;
        float sa = gradientStartColor.getAlpha() / 255f;

        float er = gradientEndColor.getRed() / 255f;
        float eg = gradientEndColor.getGreen() / 255f;
        float eb = gradientEndColor.getBlue() / 255f;
        float ea = gradientEndColor.getAlpha() / 255f;

        return switch (gradientType) {
            case VERTICAL -> new float[] {
                    // Triangle 1: top-left (start), bottom-left (end), bottom-right (end)
                    left, top, sr, sg, sb, sa,
                    left, bottom, er, eg, eb, ea,
                    right, bottom, er, eg, eb, ea,
                    // Triangle 2: top-left (start), bottom-right (end), top-right (start)
                    left, top, sr, sg, sb, sa,
                    right, bottom, er, eg, eb, ea,
                    right, top, sr, sg, sb, sa
            };
            case HORIZONTAL -> new float[] {
                    // Triangle 1: top-left (start), bottom-left (start), bottom-right (end)
                    left, top, sr, sg, sb, sa,
                    left, bottom, sr, sg, sb, sa,
                    right, bottom, er, eg, eb, ea,
                    // Triangle 2: top-left (start), bottom-right (end), top-right (end)
                    left, top, sr, sg, sb, sa,
                    right, bottom, er, eg, eb, ea,
                    right, top, er, eg, eb, ea
            };
            case DIAGONAL -> new float[] {
                    // Triangle 1: top-left (start), bottom-left (mid), bottom-right (end)
                    left, top, sr, sg, sb, sa,
                    left, bottom, (sr + er) / 2, (sg + eg) / 2, (sb + eb) / 2, (sa + ea) / 2,
                    right, bottom, er, eg, eb, ea,
                    // Triangle 2: top-left (start), bottom-right (end), top-right (mid)
                    left, top, sr, sg, sb, sa,
                    right, bottom, er, eg, eb, ea,
                    right, top, (sr + er) / 2, (sg + eg) / 2, (sb + eb) / 2, (sa + ea) / 2
            };
            default -> new float[0];
        };
    }

    private void drawGrid(RenderContext ctx, Viewport viewport) {
        if (simpleShader == null || !simpleShader.isValid()) {
            return;
        }

        float left = viewport.getLeftInset();
        float right = viewport.getWidth() - viewport.getRightInset();
        float top = viewport.getTopInset();
        float bottom = viewport.getHeight() - viewport.getBottomInset();

        int totalLines = horizontalGridLines + verticalGridLines;
        float[] vertices = new float[totalLines * 4]; // 2 points per line, 2 floats per point
        int idx = 0;

        // Horizontal grid lines
        float height = bottom - top;
        for (int i = 1; i < horizontalGridLines; i++) {
            float y = top + (height * i) / horizontalGridLines;
            vertices[idx++] = left;
            vertices[idx++] = y;
            vertices[idx++] = right;
            vertices[idx++] = y;
        }

        // Vertical grid lines
        float width = right - left;
        for (int i = 1; i < verticalGridLines; i++) {
            float x = left + (width * i) / verticalGridLines;
            vertices[idx++] = x;
            vertices[idx++] = top;
            vertices[idx++] = x;
            vertices[idx++] = bottom;
        }

        simpleShader.bind();
        simpleShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        simpleShader.setUniform("uColor",
                gridColor.getRed() / 255f,
                gridColor.getGreen() / 255f,
                gridColor.getBlue() / 255f,
                0.5f);

        solidBuffer.upload(vertices, 0, idx);
        solidBuffer.draw(DrawMode.LINES);

        simpleShader.unbind();
    }

    private void drawBorder(RenderContext ctx, Viewport viewport) {
        if (simpleShader == null || !simpleShader.isValid()) {
            return;
        }

        float left = viewport.getLeftInset();
        float right = viewport.getWidth() - viewport.getRightInset();
        float top = viewport.getTopInset();
        float bottom = viewport.getHeight() - viewport.getBottomInset();

        // Border as a line loop
        float[] vertices = {
                left, top,
                right, top,
                right, top,
                right, bottom,
                right, bottom,
                left, bottom,
                left, bottom,
                left, top
        };

        simpleShader.bind();
        simpleShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        simpleShader.setUniform("uColor",
                borderColor.getRed() / 255f,
                borderColor.getGreen() / 255f,
                borderColor.getBlue() / 255f,
                1.0f);

        solidBuffer.upload(vertices, 0, vertices.length);
        solidBuffer.draw(DrawMode.LINES);

        simpleShader.unbind();
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
                resources.disposeBuffer("background.solid");
                resources.disposeBuffer("background.gradient");
            }
            solidBuffer = null;
            gradientBuffer = null;
            simpleShader = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setChartAreaColor(Color color) {
        this.chartAreaColor = color;
        markDirty();
    }

    public Color getChartAreaColor() {
        return chartAreaColor;
    }

    public void setGridColor(Color color) {
        this.gridColor = color;
        markDirty();
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        markDirty();
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        markDirty();
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowBorder(boolean show) {
        this.showBorder = show;
        markDirty();
    }

    public boolean isShowBorder() {
        return showBorder;
    }

    public void setGridLines(int horizontal, int vertical) {
        this.horizontalGridLines = horizontal;
        this.verticalGridLines = vertical;
        markDirty();
    }

    // ========== Gradient Configuration ==========

    /**
     * Returns the current gradient type.
     */
    public GradientType getGradientType() {
        return gradientType;
    }

    /**
     * Sets a gradient background.
     *
     * @param type gradient direction (NONE for solid color)
     * @param startColor color at gradient start
     * @param endColor color at gradient end
     */
    public void setGradient(GradientType type, Color startColor, Color endColor) {
        this.gradientType = type;
        this.gradientStartColor = startColor;
        this.gradientEndColor = endColor;
        markDirty();
    }

    /**
     * Sets a vertical gradient (top to bottom).
     *
     * @param topColor color at the top of the chart
     * @param bottomColor color at the bottom of the chart
     */
    public void setVerticalGradient(Color topColor, Color bottomColor) {
        setGradient(GradientType.VERTICAL, topColor, bottomColor);
    }

    /**
     * Sets a horizontal gradient (left to right).
     *
     * @param leftColor color at the left of the chart
     * @param rightColor color at the right of the chart
     */
    public void setHorizontalGradient(Color leftColor, Color rightColor) {
        setGradient(GradientType.HORIZONTAL, leftColor, rightColor);
    }

    /**
     * Disables gradient and returns to solid color background.
     */
    public void clearGradient() {
        this.gradientType = GradientType.NONE;
        markDirty();
    }

    /**
     * Returns the gradient start color.
     */
    public Color getGradientStartColor() {
        return gradientStartColor;
    }

    /**
     * Returns the gradient end color.
     */
    public Color getGradientEndColor() {
        return gradientEndColor;
    }
}
