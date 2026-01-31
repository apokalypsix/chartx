package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.ScatterSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable scatter series using XyData.
 *
 * <p>Renders individual data points as markers with configurable shapes and sizes.
 */
public class ScatterSeries extends AbstractRenderableSeries<XyData, ScatterSeriesOptions> {

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int SEGMENTS_PER_CIRCLE = 12; // Vertices to approximate circle
    private static final int VERTICES_PER_SQUARE = 6;  // 2 triangles

    // Rendering resources
    private Buffer markerBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex array
    private float[] markerVertices;
    private int vertexCapacity;

    /**
     * Creates a scatter series with the given data and default options.
     */
    public ScatterSeries(XyData data) {
        this(data, new ScatterSeriesOptions());
    }

    /**
     * Creates a scatter series with the given data and options.
     */
    public ScatterSeries(XyData data, ScatterSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a scatter series with a custom ID.
     */
    public ScatterSeries(String id, XyData data, ScatterSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.SCATTER;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor desc = BufferDescriptor.positionColor2D(4096);
        markerBuffer = resources.getOrCreateBuffer(id + "_marker", desc);

        vertexCapacity = 256;
        // Circles need more vertices
        markerVertices = new float[vertexCapacity * (SEGMENTS_PER_CIRCLE + 2) * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_marker");
        }
        markerBuffer = null;
    }

    @Override
    public void render(RenderContext ctx) {
        if (!initialized || data.isEmpty() || !options.isVisible()) {
            return;
        }

        int firstIdx = data.getFirstVisibleIndex(ctx.getViewport().getStartTime());
        int lastIdx = data.getLastVisibleIndex(ctx.getViewport().getEndTime());

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        int visibleCount = lastIdx - firstIdx + 1;
        ensureCapacity(visibleCount);

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        float markerSize = options.getMarkerSize();
        float halfSize = markerSize / 2.0f;

        Color color = options.getColor();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = options.getOpacity();

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        ScatterSeriesOptions.MarkerShape shape = options.getMarkerShape();

        // Build vertices based on marker shape
        int floatCount;
        DrawMode drawMode;

        switch (shape) {
            case SQUARE:
            case DIAMOND:
                floatCount = buildSquareVertices(coords, firstIdx, lastIdx, halfSize, r, g, b, a, shape == ScatterSeriesOptions.MarkerShape.DIAMOND);
                drawMode = DrawMode.TRIANGLES;
                break;
            case TRIANGLE_UP:
            case TRIANGLE_DOWN:
                floatCount = buildTriangleVertices(coords, firstIdx, lastIdx, halfSize, r, g, b, a, shape == ScatterSeriesOptions.MarkerShape.TRIANGLE_DOWN);
                drawMode = DrawMode.TRIANGLES;
                break;
            case PLUS:
            case CROSS:
                floatCount = buildCrossVertices(coords, firstIdx, lastIdx, halfSize, r, g, b, a, shape == ScatterSeriesOptions.MarkerShape.CROSS);
                ctx.getDevice().setLineWidth(2.0f);
                drawMode = DrawMode.LINES;
                break;
            case CIRCLE:
            default:
                floatCount = buildCircleVertices(coords, firstIdx, lastIdx, halfSize, r, g, b, a);
                drawMode = DrawMode.TRIANGLES;
                break;
        }

        if (floatCount > 0) {
            markerBuffer.upload(markerVertices, 0, floatCount);
            markerBuffer.draw(drawMode);
        }

        shader.unbind();
    }

    private int buildCircleVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                     float radius, float r, float g, float b, float a) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (Float.isNaN(values[i])) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(values[i]);

            // Triangle fan for circle
            for (int j = 0; j < SEGMENTS_PER_CIRCLE; j++) {
                double angle1 = 2 * Math.PI * j / SEGMENTS_PER_CIRCLE;
                double angle2 = 2 * Math.PI * (j + 1) / SEGMENTS_PER_CIRCLE;

                // Center
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy, r, g, b, a);
                // Edge 1
                floatIndex = addVertex(markerVertices, floatIndex,
                        cx + radius * (float) Math.cos(angle1),
                        cy + radius * (float) Math.sin(angle1), r, g, b, a);
                // Edge 2
                floatIndex = addVertex(markerVertices, floatIndex,
                        cx + radius * (float) Math.cos(angle2),
                        cy + radius * (float) Math.sin(angle2), r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildSquareVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                     float halfSize, float r, float g, float b, float a, boolean isDiamond) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (Float.isNaN(values[i])) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(values[i]);

            if (isDiamond) {
                // Diamond: rotated square
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy, r, g, b, a);

                floatIndex = addVertex(markerVertices, floatIndex, cx, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy + halfSize, r, g, b, a);

                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy + halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy, r, g, b, a);

                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy + halfSize, r, g, b, a);
            } else {
                // Square
                float left = cx - halfSize;
                float right = cx + halfSize;
                float top = cy - halfSize;
                float bottom = cy + halfSize;

                floatIndex = addVertex(markerVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, left, bottom, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, right, bottom, r, g, b, a);

                floatIndex = addVertex(markerVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, right, bottom, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, right, top, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildTriangleVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                       float halfSize, float r, float g, float b, float a, boolean pointDown) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (Float.isNaN(values[i])) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(values[i]);

            if (pointDown) {
                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy + halfSize, r, g, b, a);
            } else {
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy + halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy + halfSize, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildCrossVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                    float halfSize, float r, float g, float b, float a, boolean isX) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (Float.isNaN(values[i])) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(values[i]);

            if (isX) {
                // X cross
                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy + halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy + halfSize, r, g, b, a);
            } else {
                // Plus
                floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy - halfSize, r, g, b, a);
                floatIndex = addVertex(markerVertices, floatIndex, cx, cy + halfSize, r, g, b, a);
            }
        }

        return floatIndex;
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

    private void ensureCapacity(int pointCount) {
        // Each point can need up to SEGMENTS_PER_CIRCLE * 3 vertices for circles
        int requiredFloats = pointCount * (SEGMENTS_PER_CIRCLE * 3) * FLOATS_PER_VERTEX;
        if (requiredFloats > markerVertices.length) {
            vertexCapacity = pointCount + pointCount / 2;
            markerVertices = new float[vertexCapacity * (SEGMENTS_PER_CIRCLE * 3) * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        return data.findMinValue(startIdx, endIdx);
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        return data.findMaxValue(startIdx, endIdx);
    }
}
