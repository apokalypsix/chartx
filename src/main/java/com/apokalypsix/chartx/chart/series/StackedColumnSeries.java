package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.StackedSeriesOptions;
import com.apokalypsix.chartx.chart.data.StackedSeriesGroup;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.data.StackingCalculator;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.List;

/**
 * Renderable stacked column series using StackedSeriesGroup.
 *
 * <p>Renders multiple XyData series as stacked vertical bars. Each series
 * is stacked on top of the previous one. Supports both standard stacking
 * (cumulative sum) and 100% stacking (normalized to percentages).
 */
public class StackedColumnSeries implements RenderableSeries<XyData, StackedSeriesOptions> {

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int VERTICES_PER_BAR = 6; // 2 triangles

    private final String id;
    private final StackedSeriesGroup group;
    private StackedSeriesOptions options;
    private boolean initialized = false;

    // Stacking computation
    private final StackingCalculator calculator = new StackingCalculator();

    // Rendering resources
    private Buffer barBuffer;
    private Buffer borderBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex arrays
    private float[] barVertices;
    private float[] borderVertices;
    private int vertexCapacity;

    /** Ratio of bar width to available slot width */
    private float barWidthRatio = 0.8f;

    /**
     * Creates a stacked column series with default options.
     */
    public StackedColumnSeries(StackedSeriesGroup group) {
        this(group, new StackedSeriesOptions());
    }

    /**
     * Creates a stacked column series with the given options.
     */
    public StackedColumnSeries(StackedSeriesGroup group, StackedSeriesOptions options) {
        this.id = group.getId() + "_stacked_column";
        this.group = group;
        this.options = options;
    }

    /**
     * Creates a stacked column series with a custom ID.
     */
    public StackedColumnSeries(String id, StackedSeriesGroup group, StackedSeriesOptions options) {
        this.id = id;
        this.group = group;
        this.options = options;
    }

    /**
     * Returns the underlying series group.
     */
    public StackedSeriesGroup getGroup() {
        return group;
    }

    /**
     * Returns the bar width ratio.
     */
    public float getBarWidthRatio() {
        return barWidthRatio;
    }

    /**
     * Sets the bar width ratio.
     */
    public StackedColumnSeries barWidthRatio(float ratio) {
        this.barWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public XyData getData() {
        return group.isEmpty() ? null : group.getSeries(0);
    }

    @Override
    public StackedSeriesOptions getOptions() {
        return options;
    }

    @Override
    public void setOptions(StackedSeriesOptions options) {
        this.options = options;
        calculator.invalidateCache();
    }

    @Override
    public SeriesType getType() {
        return SeriesType.STACKED_COLUMN;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize(ResourceManager resources) {
        if (!initialized) {
            this.resourceManager = resources;

            BufferDescriptor barDesc = BufferDescriptor.positionColor2D(8192 * FLOATS_PER_VERTEX);
            barBuffer = resources.getOrCreateBuffer(id + "_bar", barDesc);

            BufferDescriptor borderDesc = BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX);
            borderBuffer = resources.getOrCreateBuffer(id + "_border", borderDesc);

            vertexCapacity = 512;
            int maxSeries = Math.max(8, group.size());
            barVertices = new float[vertexCapacity * maxSeries * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
            borderVertices = new float[vertexCapacity * maxSeries * 8 * FLOATS_PER_VERTEX];

            initialized = true;
        }
    }

    @Override
    public void dispose() {
        if (initialized) {
            if (resourceManager != null) {
                resourceManager.disposeBuffer(id + "_bar");
                resourceManager.disposeBuffer(id + "_border");
            }
            barBuffer = null;
            borderBuffer = null;
            initialized = false;
        }
    }

    @Override
    public void render(RenderContext ctx) {
        if (!initialized || group.isEmpty() || !options.isVisible()) {
            return;
        }

        int firstIdx = group.getFirstVisibleIndex(ctx.getViewport().getStartTime());
        int lastIdx = group.getLastVisibleIndex(ctx.getViewport().getEndTime());

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        // Compute stacked values
        List<XyData> seriesList = group.getSeriesList();
        calculator.compute(seriesList, firstIdx, lastIdx, options.getStackMode());

        int visibleCount = lastIdx - firstIdx + 1;
        ensureCapacity(visibleCount);

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        // Calculate bar dimensions
        double barSlotWidth = ctx.getBarWidth();
        double barWidth = barSlotWidth * barWidthRatio;
        double halfBarWidth = barWidth / 2.0;

        // Build bar vertices for all series
        int barFloatIndex = buildBarVertices(coords, firstIdx, lastIdx, halfBarWidth);

        // Render filled bars
        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        if (barFloatIndex > 0) {
            barBuffer.upload(barVertices, 0, barFloatIndex);
            int vertexCount = barFloatIndex / FLOATS_PER_VERTEX;
            barBuffer.draw(DrawMode.TRIANGLES, 0, vertexCount);
        }

        // Render borders if enabled
        if (options.isShowLines() && options.getLineWidth() > 0) {
            int borderFloatIndex = buildBorderVertices(coords, firstIdx, lastIdx, halfBarWidth);

            if (borderFloatIndex > 0) {
                ctx.getDevice().setLineWidth(options.getLineWidth());
                borderBuffer.upload(borderVertices, 0, borderFloatIndex);
                int vertexCount = borderFloatIndex / FLOATS_PER_VERTEX;
                borderBuffer.draw(DrawMode.LINES, 0, vertexCount);
            }
        }

        shader.unbind();
    }

    private int buildBarVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                  double halfBarWidth) {
        int floatIndex = 0;
        int seriesCount = group.size();
        float opacity = options.getFillOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            XyData firstSeries = group.getSeries(0);
            if (i >= firstSeries.size()) {
                continue;
            }

            float centerX = (float) coords.xValueToScreenX(firstSeries.getTimestampsArray()[i]);
            float left = (float) (centerX - halfBarWidth);
            float right = (float) (centerX + halfBarWidth);

            // Render bars from bottom to top
            for (int s = 0; s < seriesCount; s++) {
                float baseline = calculator.getStackedBaseline(s, i);
                float top = calculator.getStackedTop(s, i);

                if (Float.isNaN(baseline) || Float.isNaN(top)) {
                    continue;
                }

                float topY = (float) coords.yValueToScreenY(top);
                float bottomY = (float) coords.yValueToScreenY(baseline);

                // Get color for this series
                Color color = group.getColor(s);
                float r = color.getRed() / 255f;
                float g = color.getGreen() / 255f;
                float b = color.getBlue() / 255f;
                float a = opacity;

                // Triangle 1: top-left, bottom-left, bottom-right
                floatIndex = addVertex(barVertices, floatIndex, left, topY, r, g, b, a);
                floatIndex = addVertex(barVertices, floatIndex, left, bottomY, r, g, b, a);
                floatIndex = addVertex(barVertices, floatIndex, right, bottomY, r, g, b, a);

                // Triangle 2: top-left, bottom-right, top-right
                floatIndex = addVertex(barVertices, floatIndex, left, topY, r, g, b, a);
                floatIndex = addVertex(barVertices, floatIndex, right, bottomY, r, g, b, a);
                floatIndex = addVertex(barVertices, floatIndex, right, topY, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildBorderVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                     double halfBarWidth) {
        int floatIndex = 0;
        int seriesCount = group.size();
        float lineOpacity = options.getLineOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            XyData firstSeries = group.getSeries(0);
            if (i >= firstSeries.size()) {
                continue;
            }

            float centerX = (float) coords.xValueToScreenX(firstSeries.getTimestampsArray()[i]);
            float left = (float) (centerX - halfBarWidth);
            float right = (float) (centerX + halfBarWidth);

            for (int s = 0; s < seriesCount; s++) {
                float baseline = calculator.getStackedBaseline(s, i);
                float top = calculator.getStackedTop(s, i);

                if (Float.isNaN(baseline) || Float.isNaN(top)) {
                    continue;
                }

                float topY = (float) coords.yValueToScreenY(top);
                float bottomY = (float) coords.yValueToScreenY(baseline);

                // Use a darker version of the series color for borders
                Color color = group.getColor(s);
                float r = color.getRed() / 255f * 0.7f;
                float g = color.getGreen() / 255f * 0.7f;
                float b = color.getBlue() / 255f * 0.7f;
                float a = lineOpacity;

                // Top edge (separating line between stacked segments)
                floatIndex = addVertex(borderVertices, floatIndex, left, topY, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, topY, r, g, b, a);

                // Only draw left/right edges for the bottom series
                if (s == 0) {
                    // Left edge
                    floatIndex = addVertex(borderVertices, floatIndex, left, topY, r, g, b, a);
                    floatIndex = addVertex(borderVertices, floatIndex, left, bottomY, r, g, b, a);
                    // Right edge
                    floatIndex = addVertex(borderVertices, floatIndex, right, topY, r, g, b, a);
                    floatIndex = addVertex(borderVertices, floatIndex, right, bottomY, r, g, b, a);
                    // Bottom edge
                    floatIndex = addVertex(borderVertices, floatIndex, left, bottomY, r, g, b, a);
                    floatIndex = addVertex(borderVertices, floatIndex, right, bottomY, r, g, b, a);
                }
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

    private void ensureCapacity(int dataCount) {
        int seriesCount = group.size();
        int requiredBarFloats = dataCount * seriesCount * VERTICES_PER_BAR * FLOATS_PER_VERTEX;
        if (requiredBarFloats > barVertices.length) {
            vertexCapacity = dataCount + dataCount / 2;
            barVertices = new float[vertexCapacity * seriesCount * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
            borderVertices = new float[vertexCapacity * seriesCount * 8 * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (group.isEmpty()) {
            return Double.NaN;
        }
        List<XyData> seriesList = group.getSeriesList();
        calculator.compute(seriesList, startIdx, endIdx, options.getStackMode());

        float min = calculator.findMinStackedValue();
        return min == 0 && group.isEmpty() ? Double.NaN : min;
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (group.isEmpty()) {
            return Double.NaN;
        }
        List<XyData> seriesList = group.getSeriesList();
        calculator.compute(seriesList, startIdx, endIdx, options.getStackMode());

        float max = calculator.findMaxStackedValue();
        if (options.getStackMode() == StackingCalculator.StackMode.PERCENT_100) {
            return 100.0;
        }
        return max == 0 && group.isEmpty() ? Double.NaN : max;
    }
}
