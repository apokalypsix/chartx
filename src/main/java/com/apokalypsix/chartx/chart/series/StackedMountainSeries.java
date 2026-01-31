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
 * Renderable stacked mountain (area) series using StackedSeriesGroup.
 *
 * <p>Renders multiple XyData series as stacked filled areas. Each series
 * is stacked on top of the previous one. Supports both standard stacking
 * (cumulative sum) and 100% stacking (normalized to percentages).
 *
 * <p>The fill areas are rendered as triangle strips between stacked levels.
 * Optional border lines can be rendered on top.
 */
public class StackedMountainSeries implements RenderableSeries<XyData, StackedSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 2;

    private final String id;
    private final StackedSeriesGroup group;
    private StackedSeriesOptions options;
    private boolean initialized = false;

    // Stacking computation
    private final StackingCalculator calculator = new StackingCalculator();

    // Rendering resources
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex arrays
    private float[] fillVertices;
    private float[] lineVertices;
    private int vertexCapacity;

    /**
     * Creates a stacked mountain series with default options.
     */
    public StackedMountainSeries(StackedSeriesGroup group) {
        this(group, new StackedSeriesOptions());
    }

    /**
     * Creates a stacked mountain series with the given options.
     */
    public StackedMountainSeries(StackedSeriesGroup group, StackedSeriesOptions options) {
        this.id = group.getId() + "_stacked_mountain";
        this.group = group;
        this.options = options;
    }

    /**
     * Creates a stacked mountain series with a custom ID.
     */
    public StackedMountainSeries(String id, StackedSeriesGroup group, StackedSeriesOptions options) {
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public XyData getData() {
        // Return the first series as the "primary" data for compatibility
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
        return SeriesType.STACKED_MOUNTAIN;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize(ResourceManager resources) {
        if (!initialized) {
            this.resourceManager = resources;

            BufferDescriptor fillDesc = BufferDescriptor.positionOnly2D(16384);
            fillBuffer = resources.getOrCreateBuffer(id + "_fill", fillDesc);

            BufferDescriptor lineDesc = BufferDescriptor.positionOnly2D(8192);
            lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

            vertexCapacity = 2048;
            fillVertices = new float[vertexCapacity * FLOATS_PER_VERTEX * 2];
            lineVertices = new float[vertexCapacity * FLOATS_PER_VERTEX];

            initialized = true;
        }
    }

    @Override
    public void dispose() {
        if (initialized) {
            if (resourceManager != null) {
                resourceManager.disposeBuffer(id + "_fill");
                resourceManager.disposeBuffer(id + "_line");
            }
            fillBuffer = null;
            lineBuffer = null;
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

        // Render fills from bottom to top
        for (int s = 0; s < group.size(); s++) {
            renderSeriesFill(ctx, s, firstIdx, lastIdx);
        }

        // Render lines (top edges) if enabled
        if (options.isShowLines() && options.getLineWidth() > 0) {
            for (int s = 0; s < group.size(); s++) {
                renderSeriesLine(ctx, s, firstIdx, lastIdx);
            }
        }
    }

    private void renderSeriesFill(RenderContext ctx, int seriesIndex,
                                  int firstIdx, int lastIdx) {
        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());
        XyData series = group.getSeries(seriesIndex);

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_SIMPLE);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        Color color = group.getColor(seriesIndex);
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                options.getFillOpacity());

        long[] timestamps = series.getTimestampsArray();
        int floatIndex = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (i >= series.size()) {
                break;
            }

            float baseline = calculator.getStackedBaseline(seriesIndex, i);
            float top = calculator.getStackedTop(seriesIndex, i);

            if (Float.isNaN(baseline) || Float.isNaN(top)) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float yBaseline = (float) coords.yValueToScreenY(baseline);
            float yTop = (float) coords.yValueToScreenY(top);

            // Build triangle strip
            fillVertices[floatIndex++] = x;
            fillVertices[floatIndex++] = yBaseline;
            fillVertices[floatIndex++] = x;
            fillVertices[floatIndex++] = yTop;
        }

        if (floatIndex > 4) {
            fillBuffer.upload(fillVertices, 0, floatIndex);
            int vertexCount = floatIndex / FLOATS_PER_VERTEX;
            fillBuffer.draw(DrawMode.TRIANGLE_STRIP, 0, vertexCount);
        }

        shader.unbind();
    }

    private void renderSeriesLine(RenderContext ctx, int seriesIndex,
                                  int firstIdx, int lastIdx) {
        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());
        XyData series = group.getSeries(seriesIndex);

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_SIMPLE);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Use same color but with line opacity
        Color color = group.getColor(seriesIndex);
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                options.getLineOpacity());

        ctx.getDevice().setLineWidth(options.getLineWidth());

        long[] timestamps = series.getTimestampsArray();
        int floatIndex = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (i >= series.size()) {
                break;
            }

            float top = calculator.getStackedTop(seriesIndex, i);
            if (Float.isNaN(top)) {
                // Draw accumulated segment and start new one
                if (floatIndex >= 4) {
                    lineBuffer.upload(lineVertices, 0, floatIndex);
                    lineBuffer.draw(DrawMode.LINE_STRIP, 0, floatIndex / FLOATS_PER_VERTEX);
                }
                floatIndex = 0;
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(top);

            lineVertices[floatIndex++] = x;
            lineVertices[floatIndex++] = y;
        }

        if (floatIndex >= 4) {
            lineBuffer.upload(lineVertices, 0, floatIndex);
            lineBuffer.draw(DrawMode.LINE_STRIP, 0, floatIndex / FLOATS_PER_VERTEX);
        }

        shader.unbind();
    }

    private void ensureCapacity(int pointCount) {
        if (pointCount > vertexCapacity) {
            vertexCapacity = pointCount + pointCount / 2;
            fillVertices = new float[vertexCapacity * FLOATS_PER_VERTEX * 2];
            lineVertices = new float[vertexCapacity * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        // Recompute if needed
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
        // Recompute if needed
        if (group.isEmpty()) {
            return Double.NaN;
        }
        List<XyData> seriesList = group.getSeriesList();
        calculator.compute(seriesList, startIdx, endIdx, options.getStackMode());

        float max = calculator.findMaxStackedValue();
        // For 100% mode, max should be 100
        if (options.getStackMode() == StackingCalculator.StackMode.PERCENT_100) {
            return 100.0;
        }
        return max == 0 && group.isEmpty() ? Double.NaN : max;
    }
}
