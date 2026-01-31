package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.HistogramSeriesOptions;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable horizontal bar series using HistogramData.
 *
 * <p>Renders horizontal bars from baseline to value, with positive/negative coloring.
 * This is the horizontal equivalent of HistogramSeries (vertical bars).
 */
public class HorizontalBarSeries extends AbstractRenderableSeries<HistogramData, HistogramSeriesOptions> {

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int VERTICES_PER_BAR = 6; // 2 triangles

    // Rendering resources
    private Buffer barBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex array
    private float[] barVertices;
    private int vertexCapacity;

    /**
     * Creates a horizontal bar series with the given data and default options.
     */
    public HorizontalBarSeries(HistogramData data) {
        this(data, new HistogramSeriesOptions());
    }

    /**
     * Creates a horizontal bar series with the given data and options.
     */
    public HorizontalBarSeries(HistogramData data, HistogramSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a horizontal bar series with a custom ID.
     */
    public HorizontalBarSeries(String id, HistogramData data, HistogramSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.HORIZONTAL_BAR;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor desc = BufferDescriptor.positionColor2D(1024);
        barBuffer = resources.getOrCreateBuffer(id + "_bar", desc);

        vertexCapacity = 256;
        barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_bar");
        }
        barBuffer = null;
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

        // For horizontal bars, bar height is based on time slot height (vertical dimension)
        // Since we use time as vertical position, we need to calculate based on chart height
        Viewport viewport = ctx.getViewport();
        long visibleDuration = viewport.getEndTime() - viewport.getStartTime();
        double chartHeight = viewport.getChartHeight();
        double barSlotHeight = (visibleDuration > 0) ? (chartHeight * ctx.getBarDuration() / visibleDuration) : chartHeight;
        double barHeight = barSlotHeight * options.getBarWidthRatio();
        double halfBarHeight = barHeight / 2.0;

        // Get the Y-axis (controls horizontal bar extent) for proper value-to-screen mapping
        YAxis axis = ctx.getAxisManager().getAxis(options.getYAxisId());
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        double chartWidth = chartRight - chartLeft;

        // Map baseline value to horizontal screen position
        double baselineNormalized = axis.normalize(options.getBaseline());
        float baselineX = (float) (chartLeft + baselineNormalized * chartWidth);
        float opacity = options.getOpacity();

        int floatIndex = buildBarVertices(coords, axis, chartLeft, chartWidth, firstIdx, lastIdx, halfBarHeight, baselineX, opacity);

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        if (floatIndex > 0) {
            barBuffer.upload(barVertices, 0, floatIndex);
            barBuffer.draw(DrawMode.TRIANGLES);
        }

        shader.unbind();
    }

    private int buildBarVertices(CoordinateSystem coords, YAxis axis, int chartLeft, double chartWidth,
                                  int firstIdx, int lastIdx, double halfBarHeight, float baselineX, float opacity) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            // Y position is determined by timestamp (vertical position)
            float y = (float) coords.xValueToScreenX(timestamps[i]);
            float value = values[i];

            // X position (horizontal extent) is determined by value, mapped using axis range
            double valueNormalized = axis.normalize(value);
            float valueX = (float) (chartLeft + valueNormalized * chartWidth);

            Color color = options.getColorForValue(value);
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = opacity;

            float top = (float) (y - halfBarHeight);
            float bottom = (float) (y + halfBarHeight);

            // Bar rectangle from baseline to value (horizontal)
            float left, right;
            if (value >= options.getBaseline()) {
                left = baselineX;
                right = valueX;
            } else {
                left = valueX;
                right = baselineX;
            }

            // Triangle 1: top-left, bottom-left, bottom-right
            floatIndex = addVertex(barVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, left, bottom, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, right, bottom, r, g, b, a);

            // Triangle 2: top-left, bottom-right, top-right
            floatIndex = addVertex(barVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, right, bottom, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, right, top, r, g, b, a);
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

    private void ensureCapacity(int barCount) {
        int requiredFloats = barCount * VERTICES_PER_BAR * FLOATS_PER_VERTEX;
        if (requiredFloats > barVertices.length) {
            vertexCapacity = barCount + barCount / 2;
            barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);

        float minValue = data.findMinValue(startIdx, endIdx);
        return Math.min(minValue, options.getBaseline());
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);

        float maxValue = data.findMaxValue(startIdx, endIdx);
        return Math.max(maxValue, options.getBaseline());
    }
}
