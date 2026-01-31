package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.ErrorBarSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable error bar series using XyyData.
 *
 * <p>Renders error bars showing upper and lower bounds around a central value.
 * Each error bar consists of a vertical (or horizontal) line with caps at the ends.
 * Optionally displays a marker at the center value.
 */
public class ErrorBarSeries extends AbstractRenderableSeries<XyyData, ErrorBarSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    // Rendering resources
    private Buffer lineBuffer;
    private Buffer markerBuffer;
    private ResourceManager resourceManager;

    // Reusable arrays
    private float[] lineVertices;
    private float[] markerVertices;
    private int vertexCapacity;

    /**
     * Creates an error bar series with default options.
     */
    public ErrorBarSeries(XyyData data) {
        this(data, new ErrorBarSeriesOptions());
    }

    /**
     * Creates an error bar series with the given options.
     */
    public ErrorBarSeries(XyyData data, ErrorBarSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates an error bar series with a custom ID.
     */
    public ErrorBarSeries(String id, XyyData data, ErrorBarSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.ERROR_BAR;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        // Lines: 2 vertices per main bar + 4 vertices per cap (2 caps) = 6 vertices per error bar
        BufferDescriptor lineDesc = BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        // Markers: 6 vertices per marker (2 triangles for diamond)
        BufferDescriptor markerDesc = BufferDescriptor.positionColor2D(2048 * FLOATS_PER_VERTEX);
        markerBuffer = resources.getOrCreateBuffer(id + "_marker", markerDesc);

        vertexCapacity = 256;
        lineVertices = new float[vertexCapacity * 6 * FLOATS_PER_VERTEX];
        markerVertices = new float[vertexCapacity * 6 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_line");
            resourceManager.disposeBuffer(id + "_marker");
        }
        lineBuffer = null;
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

        // Draw error bar lines
        ctx.getDevice().setLineWidth(options.getLineWidth());
        int lineFloatCount = buildLineVertices(coords, firstIdx, lastIdx);
        if (lineFloatCount > 0) {
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            int vertexCount = lineFloatCount / FLOATS_PER_VERTEX;
            lineBuffer.draw(DrawMode.LINES, 0, vertexCount);
        }

        // Draw center markers if enabled
        if (options.isShowCenterMarker()) {
            int markerFloatCount = buildMarkerVertices(coords, firstIdx, lastIdx);
            if (markerFloatCount > 0) {
                markerBuffer.upload(markerVertices, 0, markerFloatCount);
                int vertexCount = markerFloatCount / FLOATS_PER_VERTEX;
                markerBuffer.draw(DrawMode.TRIANGLES, 0, vertexCount);
            }
        }

        shader.unbind();
    }

    private int buildLineVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] upperArray = data.getUpperArray();
        float[] middleArray = data.getMiddleArray();
        float[] lowerArray = data.getLowerArray();

        Color color = options.getColor();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = options.getOpacity();

        float halfCapWidth = options.getCapWidth() / 2.0f;
        boolean horizontal = options.isHorizontal();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float middle = middleArray[i];
            if (Float.isNaN(middle)) {
                continue;
            }

            float upper = upperArray[i];
            float lower = lowerArray[i];

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float middleY = (float) coords.yValueToScreenY(middle);
            float upperY = (float) coords.yValueToScreenY(upper);
            float lowerY = (float) coords.yValueToScreenY(lower);

            if (horizontal) {
                // Horizontal error bars (swap X and Y roles)
                // Main bar
                floatIndex = addVertex(lineVertices, floatIndex, lowerY, x, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, upperY, x, r, g, b, a);

                // Lower cap
                floatIndex = addVertex(lineVertices, floatIndex, lowerY, x - halfCapWidth, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, lowerY, x + halfCapWidth, r, g, b, a);

                // Upper cap
                floatIndex = addVertex(lineVertices, floatIndex, upperY, x - halfCapWidth, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, upperY, x + halfCapWidth, r, g, b, a);
            } else {
                // Vertical error bars (default)
                // Main bar
                floatIndex = addVertex(lineVertices, floatIndex, x, lowerY, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, x, upperY, r, g, b, a);

                // Lower cap
                floatIndex = addVertex(lineVertices, floatIndex, x - halfCapWidth, lowerY, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, x + halfCapWidth, lowerY, r, g, b, a);

                // Upper cap
                floatIndex = addVertex(lineVertices, floatIndex, x - halfCapWidth, upperY, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, x + halfCapWidth, upperY, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildMarkerVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] middleArray = data.getMiddleArray();

        Color markerColor = options.getCenterMarkerColor();
        float r = markerColor.getRed() / 255f;
        float g = markerColor.getGreen() / 255f;
        float b = markerColor.getBlue() / 255f;
        float a = options.getOpacity();

        float halfSize = options.getCenterMarkerSize() / 2.0f;
        boolean horizontal = options.isHorizontal();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float middle = middleArray[i];
            if (Float.isNaN(middle)) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(middle);

            if (horizontal) {
                // Swap x and y for horizontal mode
                float temp = x;
                x = y;
                y = temp;
            }

            // Diamond marker (4 triangles would be more accurate, but 2 triangles work for small sizes)
            // Top triangle
            floatIndex = addVertex(markerVertices, floatIndex, x, y - halfSize, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, x - halfSize, y, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, x + halfSize, y, r, g, b, a);

            // Bottom triangle
            floatIndex = addVertex(markerVertices, floatIndex, x, y + halfSize, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, x - halfSize, y, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, x + halfSize, y, r, g, b, a);
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
            lineVertices = new float[vertexCapacity * 6 * FLOATS_PER_VERTEX];
            markerVertices = new float[vertexCapacity * 6 * FLOATS_PER_VERTEX];
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
