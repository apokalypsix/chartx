package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable line series using XyData.
 *
 * <p>Supports various display modes including standard lines, filled areas,
 * and step functions. Handles gaps in data (NaN values) by breaking the line.
 */
public class LineSeries extends AbstractRenderableSeries<XyData, LineSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 2;

    // Rendering resources
    private Buffer lineBuffer;
    private Buffer fillBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex arrays
    private float[] lineVertices;
    private float[] fillVertices;
    private int vertexCapacity;

    /**
     * Creates a line series with the given data and default options.
     */
    public LineSeries(XyData data) {
        this(data, new LineSeriesOptions());
    }

    /**
     * Creates a line series with the given data and options.
     */
    public LineSeries(XyData data, LineSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a line series with a custom ID.
     */
    public LineSeries(String id, XyData data, LineSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.LINE;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor lineDesc = BufferDescriptor.positionOnly2D(2048);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        BufferDescriptor fillDesc = BufferDescriptor.positionOnly2D(4096);
        fillBuffer = resources.getOrCreateBuffer(id + "_fill", fillDesc);

        vertexCapacity = 1024;
        lineVertices = new float[vertexCapacity * FLOATS_PER_VERTEX];
        fillVertices = new float[vertexCapacity * FLOATS_PER_VERTEX * 2]; // Extra for triangles
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_line");
            resourceManager.disposeBuffer(id + "_fill");
        }
        lineBuffer = null;
        fillBuffer = null;
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

        // Include one point before and after for smooth line entry/exit
        if (firstIdx > 0) firstIdx--;
        if (lastIdx < data.size() - 1) lastIdx++;

        int visibleCount = lastIdx - firstIdx + 1;
        ensureCapacity(visibleCount);

        LineSeriesOptions.DisplayMode mode = options.getDisplayMode();

        if (mode == LineSeriesOptions.DisplayMode.AREA && options.getFillColor() != null) {
            renderArea(ctx, firstIdx, lastIdx);
        }

        if (options.isShowLine() || mode != LineSeriesOptions.DisplayMode.AREA) {
            renderLine(ctx, firstIdx, lastIdx);
        }
    }

    private void renderLine(RenderContext ctx, int firstIdx, int lastIdx) {
        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_SIMPLE);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        Color color = options.getColor();
        float opacity = options.getOpacity();
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                opacity);

        ctx.getDevice().setLineWidth(options.getLineWidth());

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        int segmentStart = -1;
        int floatIndex = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                if (segmentStart >= 0 && floatIndex > 0) {
                    drawLineSegment(floatIndex);
                    floatIndex = 0;
                }
                segmentStart = -1;
            } else {
                float x = (float) coords.xValueToScreenX(timestamps[i]);
                float y = (float) coords.yValueToScreenY(value);

                lineVertices[floatIndex++] = x;
                lineVertices[floatIndex++] = y;

                if (segmentStart < 0) {
                    segmentStart = i;
                }
            }
        }

        if (floatIndex > 0) {
            drawLineSegment(floatIndex);
        }

        shader.unbind();
    }

    private void drawLineSegment(int floatCount) {
        if (floatCount < 4) {
            return;
        }

        lineBuffer.upload(lineVertices, 0, floatCount);
        int vertexCount = floatCount / FLOATS_PER_VERTEX;
        lineBuffer.draw(DrawMode.LINE_STRIP, 0, vertexCount);
    }

    private void renderArea(RenderContext ctx, int firstIdx, int lastIdx) {
        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_SIMPLE);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        Color fillColor = options.getFillColor();
        shader.setUniform("uColor",
                fillColor.getRed() / 255f,
                fillColor.getGreen() / 255f,
                fillColor.getBlue() / 255f,
                fillColor.getAlpha() / 255f * options.getOpacity());

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();
        float baselineY = (float) coords.yValueToScreenY(options.getBaseline());

        int floatIndex = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];
            if (Float.isNaN(value)) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(value);

            // Build triangle strip: alternate between baseline and value
            fillVertices[floatIndex++] = x;
            fillVertices[floatIndex++] = baselineY;
            fillVertices[floatIndex++] = x;
            fillVertices[floatIndex++] = y;
        }

        if (floatIndex > 4) {
            fillBuffer.upload(fillVertices, 0, floatIndex);
            int vertexCount = floatIndex / FLOATS_PER_VERTEX;
            fillBuffer.draw(DrawMode.TRIANGLE_STRIP, 0, vertexCount);
        }

        shader.unbind();
    }

    private void ensureCapacity(int pointCount) {
        int requiredFloats = pointCount * FLOATS_PER_VERTEX;
        if (requiredFloats > lineVertices.length) {
            vertexCapacity = pointCount + pointCount / 2;
            lineVertices = new float[vertexCapacity * FLOATS_PER_VERTEX];
            fillVertices = new float[vertexCapacity * FLOATS_PER_VERTEX * 2];
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
