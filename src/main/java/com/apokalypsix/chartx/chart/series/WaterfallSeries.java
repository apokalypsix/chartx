package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.WaterfallSeriesOptions;
import com.apokalypsix.chartx.chart.data.WaterfallData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable waterfall chart series using WaterfallData.
 *
 * <p>Renders waterfall charts showing running totals with color-coded bars:
 * green for increases, red for decreases, and blue for totals.
 * Optional connector lines link consecutive bar tops.
 */
public class WaterfallSeries extends AbstractRenderableSeries<WaterfallData, WaterfallSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int VERTICES_PER_BAR = 6;  // 2 triangles

    // Rendering resources
    private Buffer barBuffer;
    private Buffer borderBuffer;
    private Buffer connectorBuffer;
    private ResourceManager resourceManager;

    // Reusable arrays
    private float[] barVertices;
    private float[] borderVertices;
    private float[] connectorVertices;
    private int vertexCapacity;

    /**
     * Creates a waterfall series with default options.
     */
    public WaterfallSeries(WaterfallData data) {
        this(data, new WaterfallSeriesOptions());
    }

    /**
     * Creates a waterfall series with the given options.
     */
    public WaterfallSeries(WaterfallData data, WaterfallSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a waterfall series with a custom ID.
     */
    public WaterfallSeries(String id, WaterfallData data, WaterfallSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.WATERFALL;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor barDesc = BufferDescriptor.positionColor2D(4096);
        barBuffer = resources.getOrCreateBuffer(id + "_bar", barDesc);

        BufferDescriptor borderDesc = BufferDescriptor.positionColor2D(4096);
        borderBuffer = resources.getOrCreateBuffer(id + "_border", borderDesc);

        BufferDescriptor connectorDesc = BufferDescriptor.positionColor2D(2048);
        connectorBuffer = resources.getOrCreateBuffer(id + "_connector", connectorDesc);

        vertexCapacity = 256;
        barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
        borderVertices = new float[vertexCapacity * 8 * FLOATS_PER_VERTEX]; // 4 edges
        connectorVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX]; // 1 line per bar
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_bar");
            resourceManager.disposeBuffer(id + "_border");
            resourceManager.disposeBuffer(id + "_connector");
        }
        barBuffer = null;
        borderBuffer = null;
        connectorBuffer = null;
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

        double barSlotWidth = ctx.getBarWidth();
        double barWidth = barSlotWidth * options.getBarWidthRatio();
        double halfBarWidth = barWidth / 2.0;

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw connector lines first (behind bars)
        if (options.isShowConnectors()) {
            ctx.getDevice().setLineWidth(options.getConnectorWidth());
            int connectorFloatCount = buildConnectorVertices(coords, firstIdx, lastIdx, halfBarWidth);
            if (connectorFloatCount > 0) {
                connectorBuffer.upload(connectorVertices, 0, connectorFloatCount);
                connectorBuffer.draw(DrawMode.LINES);
            }
        }

        // Draw filled bars
        int barFloatCount = buildBarVertices(coords, firstIdx, lastIdx, halfBarWidth);
        if (barFloatCount > 0) {
            barBuffer.upload(barVertices, 0, barFloatCount);
            barBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders
        if (options.getBorderColor() != null && options.getBorderWidth() > 0) {
            ctx.getDevice().setLineWidth(options.getBorderWidth());
            int borderFloatCount = buildBorderVertices(coords, firstIdx, lastIdx, halfBarWidth);
            if (borderFloatCount > 0) {
                borderBuffer.upload(borderVertices, 0, borderFloatCount);
                borderBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();
    }

    private int buildBarVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                  double halfBarWidth) {
        int floatIndex = 0;

        float[] runningTotals = data.getRunningTotalsArray();
        boolean[] isTotalBar = data.getIsTotalBarArray();
        float[] values = data.getValuesArray();

        float opacity = options.getOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (!data.hasValue(i)) {
                continue;
            }

            // Use index as X value for category-based waterfall charts
            float x = (float) coords.xValueToScreenX(data.getXValue(i));
            float baseline = (float) coords.yValueToScreenY(data.getBaseline(i));
            float top = (float) coords.yValueToScreenY(runningTotals[i]);

            float left = (float) (x - halfBarWidth);
            float right = (float) (x + halfBarWidth);

            // Determine color based on bar type
            Color color;
            if (isTotalBar[i]) {
                color = options.getTotalColor();
            } else if (values[i] >= 0) {
                color = options.getPositiveColor();
            } else {
                color = options.getNegativeColor();
            }

            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = opacity;

            // Bar rectangle
            floatIndex = addVertex(barVertices, floatIndex, left, baseline, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, right, top, r, g, b, a);

            floatIndex = addVertex(barVertices, floatIndex, left, baseline, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, right, top, r, g, b, a);
            floatIndex = addVertex(barVertices, floatIndex, right, baseline, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildBorderVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                     double halfBarWidth) {
        int floatIndex = 0;

        float[] runningTotals = data.getRunningTotalsArray();

        Color borderColor = options.getBorderColor();
        float r = borderColor.getRed() / 255f;
        float g = borderColor.getGreen() / 255f;
        float b = borderColor.getBlue() / 255f;
        float a = options.getOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (!data.hasValue(i)) {
                continue;
            }

            // Use index as X value for category-based waterfall charts
            float x = (float) coords.xValueToScreenX(data.getXValue(i));
            float baseline = (float) coords.yValueToScreenY(data.getBaseline(i));
            float top = (float) coords.yValueToScreenY(runningTotals[i]);

            float left = (float) (x - halfBarWidth);
            float right = (float) (x + halfBarWidth);

            // Left edge
            floatIndex = addVertex(borderVertices, floatIndex, left, baseline, r, g, b, a);
            floatIndex = addVertex(borderVertices, floatIndex, left, top, r, g, b, a);

            // Right edge
            floatIndex = addVertex(borderVertices, floatIndex, right, baseline, r, g, b, a);
            floatIndex = addVertex(borderVertices, floatIndex, right, top, r, g, b, a);

            // Top edge
            floatIndex = addVertex(borderVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(borderVertices, floatIndex, right, top, r, g, b, a);

            // Bottom edge
            floatIndex = addVertex(borderVertices, floatIndex, left, baseline, r, g, b, a);
            floatIndex = addVertex(borderVertices, floatIndex, right, baseline, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildConnectorVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                        double halfBarWidth) {
        int floatIndex = 0;

        float[] runningTotals = data.getRunningTotalsArray();
        boolean[] isTotalBar = data.getIsTotalBarArray();

        Color connectorColor = options.getConnectorColor();
        float r = connectorColor.getRed() / 255f;
        float g = connectorColor.getGreen() / 255f;
        float b = connectorColor.getBlue() / 255f;
        float a = options.getOpacity() * 0.7f; // Slightly transparent

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (!data.hasValue(i)) {
                continue;
            }

            // Skip connector for the first visible bar
            if (i == 0) {
                continue;
            }

            // Skip connector to total bars (they start from 0)
            if (isTotalBar[i]) {
                continue;
            }

            // Check if previous bar exists
            if (i > 0 && !data.hasValue(i - 1)) {
                continue;
            }

            // Use index as X value for category-based waterfall charts
            float prevX = (float) coords.xValueToScreenX(data.getXValue(i - 1));
            float currX = (float) coords.xValueToScreenX(data.getXValue(i));
            float prevTop = (float) coords.yValueToScreenY(runningTotals[i - 1]);

            // Connector line from previous bar's right edge to current bar's left edge
            // at the previous running total level
            float prevRight = (float) (prevX + halfBarWidth);
            float currLeft = (float) (currX - halfBarWidth);

            floatIndex = addVertex(connectorVertices, floatIndex, prevRight, prevTop, r, g, b, a);
            floatIndex = addVertex(connectorVertices, floatIndex, currLeft, prevTop, r, g, b, a);
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
        if (barCount > vertexCapacity) {
            vertexCapacity = barCount + barCount / 2;
            barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
            borderVertices = new float[vertexCapacity * 8 * FLOATS_PER_VERTEX];
            connectorVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
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
