package com.apokalypsix.chartx.core.render.service.v2;

import com.apokalypsix.chartx.chart.style.LineStyle;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.core.render.api.*;

import java.awt.Color;

/**
 * Renderer for drawing lines with LineStyle support, including dashed lines.
 *
 * <p>Dashed lines are rendered using geometry-based dashing, which generates
 * line segments according to the dash pattern. This approach works reliably
 * across all graphics backends.
 *
 * <p>For solid lines, this renderer provides efficient batch rendering using VBOs.
 *
 * <p>Note: This is a utility class, not a series renderer. It can be used by
 * other renderers to draw styled lines.
 */
public class DashedLineRendererV2 {

    private static final int INITIAL_CAPACITY = 1024;
    private static final int VERTICES_PER_LINE = 2;
    private static final int FLOATS_PER_VERTEX = 6;  // x, y, r, g, b, a

    private Buffer vertexBuffer;
    private Shader shader;

    private float[] vertices;
    private int vertexCount;

    private boolean initialized = false;

    /**
     * Initializes resources using the abstracted rendering API.
     */
    public void initialize(RenderContext ctx) {
        if (initialized) {
            return;
        }

        ResourceManager resources = ctx.getResourceManager();

        // Create vertex buffer (6 floats per vertex: x, y, r, g, b, a)
        BufferDescriptor desc = BufferDescriptor.builder()
                .floatsPerVertex(FLOATS_PER_VERTEX)
                .initialCapacity(INITIAL_CAPACITY * VERTICES_PER_LINE * FLOATS_PER_VERTEX)
                .dynamic(true)
                .attributes(
                        VertexAttribute.floatAttr("a_position", 2, 0),
                        VertexAttribute.floatAttr("a_color", 4, 2 * Float.BYTES)
                )
                .build();

        vertexBuffer = resources.getOrCreateBuffer("dashedline.vertices", desc);
        shader = resources.getShader("default");

        vertices = new float[INITIAL_CAPACITY * VERTICES_PER_LINE * FLOATS_PER_VERTEX];
        vertexCount = 0;
        initialized = true;
    }

    /**
     * Begins a new batch of lines.
     */
    public void begin() {
        vertexCount = 0;
    }

    /**
     * Adds a line segment to the batch.
     *
     * @param x1 start X coordinate (screen space)
     * @param y1 start Y coordinate (screen space)
     * @param x2 end X coordinate (screen space)
     * @param y2 end Y coordinate (screen space)
     * @param style the line style
     */
    public void addLine(float x1, float y1, float x2, float y2, LineStyle style) {
        if (style.isSolid()) {
            addSolidLine(x1, y1, x2, y2, style.color(), style.opacity());
        } else {
            addDashedLine(x1, y1, x2, y2, style);
        }
    }

    /**
     * Adds a solid line to the batch.
     */
    private void addSolidLine(float x1, float y1, float x2, float y2, Color color, float opacity) {
        ensureCapacity(2);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = (color.getAlpha() / 255f) * opacity;

        int idx = vertexCount * FLOATS_PER_VERTEX;
        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = r;
        vertices[idx++] = g;
        vertices[idx++] = b;
        vertices[idx++] = a;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = r;
        vertices[idx++] = g;
        vertices[idx++] = b;
        vertices[idx] = a;

        vertexCount += 2;
    }

    /**
     * Adds a dashed line to the batch by generating dash segments.
     */
    private void addDashedLine(float x1, float y1, float x2, float y2, LineStyle style) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lineLength = (float) Math.sqrt(dx * dx + dy * dy);

        if (lineLength < 0.5f) {
            return;  // Too short to render
        }

        // Normalize direction
        float dirX = dx / lineLength;
        float dirY = dy / lineLength;

        float[] dashPattern = style.dashPattern();
        float dashPhase = style.dashPhase();
        Color color = style.color();
        float opacity = style.opacity();

        // Generate dash segments
        float distance = -dashPhase;  // Start with phase offset
        int patternIndex = 0;
        boolean drawing = true;  // Start by drawing

        // Advance through phase if negative
        while (distance < 0) {
            float dashLength = dashPattern[patternIndex];
            if (distance + dashLength > 0) {
                break;
            }
            distance += dashLength;
            patternIndex = (patternIndex + 1) % dashPattern.length;
            drawing = !drawing;
        }

        while (distance < lineLength) {
            float dashLength = dashPattern[patternIndex];
            float segmentStart = Math.max(0, distance);
            float segmentEnd = Math.min(lineLength, distance + dashLength);

            if (drawing && segmentEnd > segmentStart) {
                float sx1 = x1 + dirX * segmentStart;
                float sy1 = y1 + dirY * segmentStart;
                float sx2 = x1 + dirX * segmentEnd;
                float sy2 = y1 + dirY * segmentEnd;
                addSolidLine(sx1, sy1, sx2, sy2, color, opacity);
            }

            distance += dashLength;
            patternIndex = (patternIndex + 1) % dashPattern.length;
            drawing = !drawing;
        }
    }

    /**
     * Adds a polyline (connected line segments) with the given style.
     *
     * @param xCoords array of X coordinates
     * @param yCoords array of Y coordinates
     * @param count number of points
     * @param style the line style
     */
    public void addPolyline(float[] xCoords, float[] yCoords, int count, LineStyle style) {
        for (int i = 0; i < count - 1; i++) {
            addLine(xCoords[i], yCoords[i], xCoords[i + 1], yCoords[i + 1], style);
        }
    }

    /**
     * Renders all lines in the batch.
     *
     * @param ctx the render context
     * @param lineWidth the line width
     */
    public void end(RenderContext ctx, float lineWidth) {
        if (vertexCount < 2 || !initialized) {
            return;
        }

        RenderDevice device = ctx.getDevice();

        // Upload vertex data
        vertexBuffer.upload(vertices, 0, vertexCount * FLOATS_PER_VERTEX);

        // Set up shader
        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Set line width
        device.setLineWidth(lineWidth);

        // Enable blending for transparency
        device.setBlendMode(BlendMode.ALPHA);

        // Draw lines
        vertexBuffer.draw(DrawMode.LINES);

        // Cleanup
        shader.unbind();
    }

    /**
     * Convenience method to render a single line immediately.
     */
    public void renderLine(RenderContext ctx, float x1, float y1, float x2, float y2, LineStyle style) {
        begin();
        addLine(x1, y1, x2, y2, style);
        end(ctx, style.width());
    }

    /**
     * Convenience method to render a polyline immediately.
     */
    public void renderPolyline(RenderContext ctx, float[] xCoords, float[] yCoords, int count, LineStyle style) {
        begin();
        addPolyline(xCoords, yCoords, count, style);
        end(ctx, style.width());
    }

    /**
     * Releases resources.
     */
    public void dispose(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();
        if (resources != null) {
            resources.disposeBuffer("dashedline.vertices");
        }
        vertexBuffer = null;
        shader = null;
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void ensureCapacity(int additionalVertices) {
        int required = (vertexCount + additionalVertices) * FLOATS_PER_VERTEX;
        if (required > vertices.length) {
            int newCapacity = Math.max(required, vertices.length * 2);
            float[] newVertices = new float[newCapacity];
            System.arraycopy(vertices, 0, newVertices, 0, vertexCount * FLOATS_PER_VERTEX);
            vertices = newVertices;
        }
    }
}
