package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.BubbleSeriesOptions;
import com.apokalypsix.chartx.chart.data.BubbleData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable bubble series using BubbleData.
 *
 * <p>Renders circles at data points with sizes proportional to the size
 * dimension in the data. Supports scaling by area or radius.
 */
public class BubbleSeries extends AbstractRenderableSeries<BubbleData, BubbleSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int SEGMENTS_PER_CIRCLE = 16;

    // Rendering resources
    private ResourceManager resourceManager;
    private Buffer fillBuffer;
    private Buffer borderBuffer;

    // Reusable arrays
    private float[] fillVertices;
    private float[] borderVertices;
    private int vertexCapacity;

    // Cached size range for scaling
    private float cachedMinSize = Float.NaN;
    private float cachedMaxSize = Float.NaN;
    private int cachedRangeStart = -1;
    private int cachedRangeEnd = -1;

    /**
     * Creates a bubble series with default options.
     */
    public BubbleSeries(BubbleData data) {
        this(data, new BubbleSeriesOptions());
    }

    /**
     * Creates a bubble series with the given options.
     */
    public BubbleSeries(BubbleData data, BubbleSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a bubble series with a custom ID.
     */
    public BubbleSeries(String id, BubbleData data, BubbleSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.BUBBLE;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        int fillCapacity = 8192 * FLOATS_PER_VERTEX;
        int borderCapacity = 4096 * FLOATS_PER_VERTEX;

        fillBuffer = resources.getOrCreateBuffer(id + "_fill",
                BufferDescriptor.positionColor2D(fillCapacity));

        borderBuffer = resources.getOrCreateBuffer(id + "_border",
                BufferDescriptor.positionColor2D(borderCapacity));

        vertexCapacity = 256;
        fillVertices = new float[vertexCapacity * SEGMENTS_PER_CIRCLE * 3 * FLOATS_PER_VERTEX];
        borderVertices = new float[vertexCapacity * SEGMENTS_PER_CIRCLE * 2 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_fill");
            resourceManager.disposeBuffer(id + "_border");
        }
        fillBuffer = null;
        borderBuffer = null;
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

        // Update size range for scaling
        updateSizeRange(firstIdx, lastIdx);

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw filled bubbles
        int fillFloatCount = buildFillVertices(coords, firstIdx, lastIdx);
        if (fillFloatCount > 0) {
            int vertexCount = fillFloatCount / FLOATS_PER_VERTEX;
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.setVertexCount(vertexCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders if enabled
        if (options.getBorderColor() != null && options.getBorderWidth() > 0) {
            ctx.getDevice().setLineWidth(options.getBorderWidth());
            int borderFloatCount = buildBorderVertices(coords, firstIdx, lastIdx);
            if (borderFloatCount > 0) {
                int vertexCount = borderFloatCount / FLOATS_PER_VERTEX;
                borderBuffer.upload(borderVertices, 0, borderFloatCount);
                borderBuffer.setVertexCount(vertexCount);
                borderBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();
    }

    private void updateSizeRange(int startIdx, int endIdx) {
        if (startIdx == cachedRangeStart && endIdx == cachedRangeEnd) {
            return;
        }
        cachedMinSize = data.findMinSize(startIdx, endIdx);
        cachedMaxSize = data.findMaxSize(startIdx, endIdx);
        cachedRangeStart = startIdx;
        cachedRangeEnd = endIdx;
    }

    private float calculateRadius(float sizeValue) {
        float minRadius = options.getMinRadius();
        float maxRadius = options.getMaxRadius();

        if (Float.isNaN(sizeValue) || Float.isNaN(cachedMinSize) || Float.isNaN(cachedMaxSize)) {
            return minRadius;
        }

        float sizeRange = cachedMaxSize - cachedMinSize;
        if (sizeRange <= 0) {
            return (minRadius + maxRadius) / 2;
        }

        float normalized = (sizeValue - cachedMinSize) / sizeRange;

        if (options.isScaleByArea()) {
            // Scale by area: radius proportional to sqrt of normalized value
            // Area = pi * r^2, so r = sqrt(area/pi)
            // For normalized [0,1] -> radius [min, max]
            float minArea = minRadius * minRadius;
            float maxArea = maxRadius * maxRadius;
            float area = minArea + normalized * (maxArea - minArea);
            return (float) Math.sqrt(area);
        } else {
            // Scale by radius directly
            return minRadius + normalized * (maxRadius - minRadius);
        }
    }

    private int buildFillVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();
        float[] sizes = data.getSizesArray();

        Color color = options.getColor();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = (color.getAlpha() / 255f) * options.getOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];
            if (Float.isNaN(value)) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(value);
            float radius = calculateRadius(sizes[i]);

            // Triangle fan for circle
            for (int j = 0; j < SEGMENTS_PER_CIRCLE; j++) {
                double angle1 = 2 * Math.PI * j / SEGMENTS_PER_CIRCLE;
                double angle2 = 2 * Math.PI * (j + 1) / SEGMENTS_PER_CIRCLE;

                floatIndex = addVertex(fillVertices, floatIndex, cx, cy, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        cx + radius * (float) Math.cos(angle1),
                        cy + radius * (float) Math.sin(angle1), r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        cx + radius * (float) Math.cos(angle2),
                        cy + radius * (float) Math.sin(angle2), r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildBorderVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();
        float[] sizes = data.getSizesArray();

        Color borderColor = options.getBorderColor();
        float r = borderColor.getRed() / 255f;
        float g = borderColor.getGreen() / 255f;
        float b = borderColor.getBlue() / 255f;
        float a = options.getOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];
            if (Float.isNaN(value)) {
                continue;
            }

            float cx = (float) coords.xValueToScreenX(timestamps[i]);
            float cy = (float) coords.yValueToScreenY(value);
            float radius = calculateRadius(sizes[i]);

            // Line loop for circle border
            for (int j = 0; j < SEGMENTS_PER_CIRCLE; j++) {
                double angle1 = 2 * Math.PI * j / SEGMENTS_PER_CIRCLE;
                double angle2 = 2 * Math.PI * (j + 1) / SEGMENTS_PER_CIRCLE;

                floatIndex = addVertex(borderVertices, floatIndex,
                        cx + radius * (float) Math.cos(angle1),
                        cy + radius * (float) Math.sin(angle1), r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex,
                        cx + radius * (float) Math.cos(angle2),
                        cy + radius * (float) Math.sin(angle2), r, g, b, a);
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
        if (pointCount > vertexCapacity) {
            vertexCapacity = pointCount + pointCount / 2;
            fillVertices = new float[vertexCapacity * SEGMENTS_PER_CIRCLE * 3 * FLOATS_PER_VERTEX];
            borderVertices = new float[vertexCapacity * SEGMENTS_PER_CIRCLE * 2 * FLOATS_PER_VERTEX];
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
