package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.ImpulseSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable impulse (stem) series using XyData.
 *
 * <p>Renders vertical lines from baseline to each data value, with optional
 * markers at the top. Also known as a lollipop chart or stem plot.
 */
public class ImpulseSeries extends AbstractRenderableSeries<XyData, ImpulseSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int SEGMENTS_PER_CIRCLE = 12;

    // Rendering resources
    private Buffer stemBuffer;
    private Buffer markerBuffer;
    private ResourceManager resourceManager;

    // Reusable arrays
    private float[] stemVertices;
    private float[] markerVertices;
    private int vertexCapacity;

    /**
     * Creates an impulse series with default options.
     */
    public ImpulseSeries(XyData data) {
        this(data, new ImpulseSeriesOptions());
    }

    /**
     * Creates an impulse series with the given options.
     */
    public ImpulseSeries(XyData data, ImpulseSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates an impulse series with a custom ID.
     */
    public ImpulseSeries(String id, XyData data, ImpulseSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.IMPULSE;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor stemDesc = BufferDescriptor.positionColor2D(2048);
        stemBuffer = resources.getOrCreateBuffer(id + "_stem", stemDesc);

        BufferDescriptor markerDesc = BufferDescriptor.positionColor2D(4096);
        markerBuffer = resources.getOrCreateBuffer(id + "_marker", markerDesc);

        vertexCapacity = 256;
        stemVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX]; // 2 vertices per stem
        markerVertices = new float[vertexCapacity * SEGMENTS_PER_CIRCLE * 3 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_stem");
            resourceManager.disposeBuffer(id + "_marker");
        }
        stemBuffer = null;
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

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw stems
        ctx.getDevice().setLineWidth(options.getLineWidth());
        int stemFloatCount = buildStemVertices(coords, firstIdx, lastIdx);
        if (stemFloatCount > 0) {
            stemBuffer.upload(stemVertices, 0, stemFloatCount);
            stemBuffer.draw(DrawMode.LINES);
        }

        // Draw markers if enabled
        if (options.getMarkerShape() != ImpulseSeriesOptions.MarkerShape.NONE) {
            int markerFloatCount = buildMarkerVertices(coords, firstIdx, lastIdx);
            if (markerFloatCount > 0) {
                markerBuffer.upload(markerVertices, 0, markerFloatCount);
                markerBuffer.draw(DrawMode.TRIANGLES);
            }
        }

        shader.unbind();
    }

    private int buildStemVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        Color color = options.getColor();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = options.getOpacity();

        float baselineY = (float) coords.yValueToScreenY(options.getBaseline());

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];
            if (Float.isNaN(value)) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float valueY = (float) coords.yValueToScreenY(value);

            // Baseline point
            floatIndex = addVertex(stemVertices, floatIndex, x, baselineY, r, g, b, a);
            // Value point
            floatIndex = addVertex(stemVertices, floatIndex, x, valueY, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildMarkerVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        Color markerColor = options.getMarkerColor();
        float r = markerColor.getRed() / 255f;
        float g = markerColor.getGreen() / 255f;
        float b = markerColor.getBlue() / 255f;
        float a = options.getOpacity();

        float radius = options.getMarkerSize() / 2.0f;
        ImpulseSeriesOptions.MarkerShape shape = options.getMarkerShape();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];
            if (Float.isNaN(value)) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(value);

            switch (shape) {
                case CIRCLE:
                    floatIndex = buildCircleMarker(floatIndex, cx, cy, radius, r, g, b, a);
                    break;
                case SQUARE:
                    floatIndex = buildSquareMarker(floatIndex, cx, cy, radius, r, g, b, a);
                    break;
                case DIAMOND:
                    floatIndex = buildDiamondMarker(floatIndex, cx, cy, radius, r, g, b, a);
                    break;
                default:
                    break;
            }
        }

        return floatIndex;
    }

    private int buildCircleMarker(int floatIndex, float cx, float cy, float radius,
                                   float r, float g, float b, float a) {
        for (int j = 0; j < SEGMENTS_PER_CIRCLE; j++) {
            double angle1 = 2 * Math.PI * j / SEGMENTS_PER_CIRCLE;
            double angle2 = 2 * Math.PI * (j + 1) / SEGMENTS_PER_CIRCLE;

            floatIndex = addVertex(markerVertices, floatIndex, cx, cy, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex,
                    cx + radius * (float) Math.cos(angle1),
                    cy + radius * (float) Math.sin(angle1), r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex,
                    cx + radius * (float) Math.cos(angle2),
                    cy + radius * (float) Math.sin(angle2), r, g, b, a);
        }
        return floatIndex;
    }

    private int buildSquareMarker(int floatIndex, float cx, float cy, float halfSize,
                                   float r, float g, float b, float a) {
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

        return floatIndex;
    }

    private int buildDiamondMarker(int floatIndex, float cx, float cy, float halfSize,
                                    float r, float g, float b, float a) {
        // Top triangle
        floatIndex = addVertex(markerVertices, floatIndex, cx, cy - halfSize, r, g, b, a);
        floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy, r, g, b, a);
        floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy, r, g, b, a);

        // Bottom triangle
        floatIndex = addVertex(markerVertices, floatIndex, cx, cy + halfSize, r, g, b, a);
        floatIndex = addVertex(markerVertices, floatIndex, cx + halfSize, cy, r, g, b, a);
        floatIndex = addVertex(markerVertices, floatIndex, cx - halfSize, cy, r, g, b, a);

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
        if (pointCount > vertexCapacity) {
            vertexCapacity = pointCount + pointCount / 2;
            stemVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
            markerVertices = new float[vertexCapacity * SEGMENTS_PER_CIRCLE * 3 * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        double dataMin = data.findMinValue(startIdx, endIdx);
        return Math.min(dataMin, options.getBaseline());
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        double dataMax = data.findMaxValue(startIdx, endIdx);
        return Math.max(dataMax, options.getBaseline());
    }
}
