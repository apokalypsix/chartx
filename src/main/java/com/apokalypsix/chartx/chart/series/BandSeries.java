package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.BandSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable band series using XyyData (upper/middle/lower).
 *
 * <p>Renders Bollinger Bands, Keltner Channels, and similar indicators
 * with optional fill between bands and configurable line colors.
 */
public class BandSeries extends AbstractRenderableSeries<XyyData, BandSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 2;

    // Rendering resources
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex arrays
    private float[] fillVertices;
    private float[] lineVertices;
    private int vertexCapacity;

    /**
     * Creates a band series with the given data and default options.
     */
    public BandSeries(XyyData data) {
        this(data, new BandSeriesOptions());
    }

    /**
     * Creates a band series with the given data and options.
     */
    public BandSeries(XyyData data, BandSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a band series with a custom ID.
     */
    public BandSeries(String id, XyyData data, BandSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.BAND;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor fillDesc = BufferDescriptor.positionOnly2D(4096);
        fillBuffer = resources.getOrCreateBuffer(id + "_fill", fillDesc);

        BufferDescriptor lineDesc = BufferDescriptor.positionOnly2D(2048);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        vertexCapacity = 1024;
        fillVertices = new float[vertexCapacity * FLOATS_PER_VERTEX * 2]; // 2 verts per point for strip
        lineVertices = new float[vertexCapacity * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_fill");
            resourceManager.disposeBuffer(id + "_line");
        }
        fillBuffer = null;
        lineBuffer = null;
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

        // Include extra points for smooth transitions
        if (firstIdx > 0) firstIdx--;
        if (lastIdx < data.size() - 1) lastIdx++;

        int visibleCount = lastIdx - firstIdx + 1;
        ensureCapacity(visibleCount);

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_SIMPLE);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Render fill between bands first
        if (options.isShowFill() && options.getFillColor() != null) {
            renderFill(shader, coords, firstIdx, lastIdx);
        }

        // Render lines
        ctx.getDevice().setLineWidth(options.getLineWidth());
        if (options.getUpperColor() != null) {
            renderLine(shader, coords, firstIdx, lastIdx, true, options.getUpperColor());
        }
        if (options.getLowerColor() != null) {
            renderLine(shader, coords, firstIdx, lastIdx, false, options.getLowerColor());
        }

        if (options.isShowMiddle() && options.getMiddleColor() != null) {
            renderMiddleLine(shader, coords, firstIdx, lastIdx, options.getMiddleColor());
        }

        shader.unbind();
    }

    private void renderFill(Shader shader, CoordinateSystem coords,
                            int firstIdx, int lastIdx) {
        Color fillColor = options.getFillColor();
        shader.setUniform("uColor",
                fillColor.getRed() / 255f,
                fillColor.getGreen() / 255f,
                fillColor.getBlue() / 255f,
                fillColor.getAlpha() / 255f);

        long[] timestamps = data.getTimestampsArray();
        float[] upper = data.getUpperArray();
        float[] lower = data.getLowerArray();

        int floatIndex = 0;
        for (int i = firstIdx; i <= lastIdx; i++) {
            if (Float.isNaN(upper[i]) || Float.isNaN(lower[i])) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float upperY = (float) coords.yValueToScreenY(upper[i]);
            float lowerY = (float) coords.yValueToScreenY(lower[i]);

            // Triangle strip: alternate upper/lower
            fillVertices[floatIndex++] = x;
            fillVertices[floatIndex++] = upperY;
            fillVertices[floatIndex++] = x;
            fillVertices[floatIndex++] = lowerY;
        }

        if (floatIndex > 4) {
            fillBuffer.upload(fillVertices, 0, floatIndex);
            int vertexCount = floatIndex / FLOATS_PER_VERTEX;
            fillBuffer.draw(DrawMode.TRIANGLE_STRIP, 0, vertexCount);
        }
    }

    private void renderLine(Shader shader, CoordinateSystem coords,
                            int firstIdx, int lastIdx, boolean isUpper, Color color) {
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                1.0f);

        long[] timestamps = data.getTimestampsArray();
        float[] values = isUpper ? data.getUpperArray() : data.getLowerArray();

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
    }

    private void renderMiddleLine(Shader shader, CoordinateSystem coords,
                                   int firstIdx, int lastIdx, Color color) {
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                1.0f);

        long[] timestamps = data.getTimestampsArray();
        float[] middle = data.getMiddleArray();

        int segmentStart = -1;
        int floatIndex = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = middle[i];

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
    }

    private void drawLineSegment(int floatCount) {
        if (floatCount < 4) {
            return;
        }

        lineBuffer.upload(lineVertices, 0, floatCount);
        int vertexCount = floatCount / FLOATS_PER_VERTEX;
        lineBuffer.draw(DrawMode.LINE_STRIP, 0, vertexCount);
    }

    private void ensureCapacity(int pointCount) {
        int requiredFillFloats = pointCount * FLOATS_PER_VERTEX * 2;
        int requiredLineFloats = pointCount * FLOATS_PER_VERTEX;

        if (requiredFillFloats > fillVertices.length) {
            vertexCapacity = pointCount + pointCount / 2;
            fillVertices = new float[vertexCapacity * FLOATS_PER_VERTEX * 2];
        }

        if (requiredLineFloats > lineVertices.length) {
            lineVertices = new float[vertexCapacity * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        return data.findMinLower(startIdx, endIdx);
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        return data.findMaxUpper(startIdx, endIdx);
    }
}
