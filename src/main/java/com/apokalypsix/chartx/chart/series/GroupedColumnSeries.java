package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.GroupedColumnSeriesOptions;
import com.apokalypsix.chartx.chart.data.GroupedColumnData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable grouped column series using GroupedColumnData.
 *
 * <p>Renders multiple bars side-by-side at each timestamp, with each bar
 * representing a different group/category. Supports configurable colors,
 * spacing, and optional borders.
 */
public class GroupedColumnSeries extends AbstractRenderableSeries<GroupedColumnData, GroupedColumnSeriesOptions> {

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int VERTICES_PER_BAR = 6; // 2 triangles

    // Rendering resources
    private Buffer barBuffer;
    private Buffer borderBuffer;
    private ResourceManager resourceManager;

    // Reusable vertex arrays
    private float[] barVertices;
    private float[] borderVertices;
    private int vertexCapacity;

    /**
     * Creates a grouped column series with default options.
     */
    public GroupedColumnSeries(GroupedColumnData data) {
        this(data, new GroupedColumnSeriesOptions(data.getGroupCount()));
    }

    /**
     * Creates a grouped column series with the given options.
     */
    public GroupedColumnSeries(GroupedColumnData data, GroupedColumnSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a grouped column series with a custom ID.
     */
    public GroupedColumnSeries(String id, GroupedColumnData data, GroupedColumnSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.GROUPED_COLUMN;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor barDesc = BufferDescriptor.positionColor2D(4096);
        barBuffer = resources.getOrCreateBuffer(id + "_bar", barDesc);

        BufferDescriptor borderDesc = BufferDescriptor.positionColor2D(2048);
        borderBuffer = resources.getOrCreateBuffer(id + "_border", borderDesc);

        vertexCapacity = 256;
        int maxGroups = data.getGroupCount();
        barVertices = new float[vertexCapacity * maxGroups * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
        borderVertices = new float[vertexCapacity * maxGroups * 8 * FLOATS_PER_VERTEX]; // 4 lines per bar
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_bar");
            resourceManager.disposeBuffer(id + "_border");
        }
        barBuffer = null;
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

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        // Calculate bar dimensions
        double barSlotWidth = ctx.getBarWidth();
        double groupTotalWidth = barSlotWidth * options.getGroupWidthRatio();
        int groupCount = data.getGroupCount();
        float barSpacing = options.getBarSpacing();
        double singleBarWidth = (groupTotalWidth - barSpacing * (groupCount - 1)) / groupCount;
        double groupStartOffset = -groupTotalWidth / 2.0;

        float baselineY = (float) coords.yValueToScreenY(options.getBaseline());
        float opacity = options.getOpacity();

        // Build bar vertices
        int barFloatIndex = buildBarVertices(coords, firstIdx, lastIdx,
                groupStartOffset, singleBarWidth, barSpacing, baselineY, opacity);

        // Render filled bars
        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        if (barFloatIndex > 0) {
            barBuffer.upload(barVertices, 0, barFloatIndex);
            barBuffer.draw(DrawMode.TRIANGLES);
        }

        // Render borders if enabled
        if (options.isShowBorders() && options.getBorderWidth() > 0) {
            int borderFloatIndex = buildBorderVertices(coords, firstIdx, lastIdx,
                    groupStartOffset, singleBarWidth, barSpacing, baselineY);

            if (borderFloatIndex > 0) {
                ctx.getDevice().setLineWidth(options.getBorderWidth());
                borderBuffer.upload(borderVertices, 0, borderFloatIndex);
                borderBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();
    }

    private int buildBarVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                  double groupStartOffset, double singleBarWidth,
                                  float barSpacing, float baselineY, float opacity) {
        int floatIndex = 0;
        int groupCount = data.getGroupCount();

        for (int i = firstIdx; i <= lastIdx; i++) {
            // Use index as X value for category-based grouped columns
            float centerX = (float) coords.xValueToScreenX(data.getXValue(i));

            for (int g = 0; g < groupCount; g++) {
                float value = data.getValue(i, g);
                if (Float.isNaN(value)) {
                    continue;
                }

                float valueY = (float) coords.yValueToScreenY(value);

                // Calculate bar position within group
                double barOffset = groupStartOffset + g * (singleBarWidth + barSpacing);
                float left = (float) (centerX + barOffset);
                float right = (float) (centerX + barOffset + singleBarWidth);

                // Get color for this group
                Color color = options.getGroupColor(g);
                float r = color.getRed() / 255f;
                float gr = color.getGreen() / 255f;
                float b = color.getBlue() / 255f;
                float a = opacity;

                // Bar rectangle from baseline to value
                float top, bottom;
                if (value >= options.getBaseline()) {
                    top = valueY;
                    bottom = baselineY;
                } else {
                    top = baselineY;
                    bottom = valueY;
                }

                // Triangle 1: top-left, bottom-left, bottom-right
                floatIndex = addVertex(barVertices, floatIndex, left, top, r, gr, b, a);
                floatIndex = addVertex(barVertices, floatIndex, left, bottom, r, gr, b, a);
                floatIndex = addVertex(barVertices, floatIndex, right, bottom, r, gr, b, a);

                // Triangle 2: top-left, bottom-right, top-right
                floatIndex = addVertex(barVertices, floatIndex, left, top, r, gr, b, a);
                floatIndex = addVertex(barVertices, floatIndex, right, bottom, r, gr, b, a);
                floatIndex = addVertex(barVertices, floatIndex, right, top, r, gr, b, a);
            }
        }

        return floatIndex;
    }

    private int buildBorderVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                     double groupStartOffset, double singleBarWidth,
                                     float barSpacing, float baselineY) {
        int floatIndex = 0;
        int groupCount = data.getGroupCount();

        Color borderColor = options.getBorderColor();
        float r = borderColor.getRed() / 255f;
        float g = borderColor.getGreen() / 255f;
        float b = borderColor.getBlue() / 255f;
        float a = 1.0f;

        for (int i = firstIdx; i <= lastIdx; i++) {
            // Use index as X value for category-based grouped columns
            float centerX = (float) coords.xValueToScreenX(data.getXValue(i));

            for (int gr = 0; gr < groupCount; gr++) {
                float value = data.getValue(i, gr);
                if (Float.isNaN(value)) {
                    continue;
                }

                float valueY = (float) coords.yValueToScreenY(value);

                double barOffset = groupStartOffset + gr * (singleBarWidth + barSpacing);
                float left = (float) (centerX + barOffset);
                float right = (float) (centerX + barOffset + singleBarWidth);

                float top, bottom;
                if (value >= options.getBaseline()) {
                    top = valueY;
                    bottom = baselineY;
                } else {
                    top = baselineY;
                    bottom = valueY;
                }

                // Four border lines
                // Left edge
                floatIndex = addVertex(borderVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, left, bottom, r, g, b, a);
                // Right edge
                floatIndex = addVertex(borderVertices, floatIndex, right, top, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, bottom, r, g, b, a);
                // Top edge
                floatIndex = addVertex(borderVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, top, r, g, b, a);
                // Bottom edge
                floatIndex = addVertex(borderVertices, floatIndex, left, bottom, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, bottom, r, g, b, a);
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
        int groupCount = data.getGroupCount();
        int requiredBarFloats = dataCount * groupCount * VERTICES_PER_BAR * FLOATS_PER_VERTEX;
        if (requiredBarFloats > barVertices.length) {
            vertexCapacity = dataCount + dataCount / 2;
            barVertices = new float[vertexCapacity * groupCount * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
            borderVertices = new float[vertexCapacity * groupCount * 8 * FLOATS_PER_VERTEX];
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
