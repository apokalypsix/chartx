package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.BoxPlotSeriesOptions;
import com.apokalypsix.chartx.chart.data.BoxWhiskerData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable box plot series using BoxWhiskerData.
 *
 * <p>Renders box-and-whisker plots showing statistical quartiles:
 * box (Q1-Q3), median line, and whiskers (min-max).
 */
public class BoxPlotSeries extends AbstractRenderableSeries<BoxWhiskerData, BoxPlotSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int VERTICES_PER_BOX = 6;  // 2 triangles for box fill

    // Rendering resources
    private Buffer boxBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;

    // Reusable arrays
    private float[] boxVertices;
    private float[] lineVertices;
    private int vertexCapacity;

    /**
     * Creates a box plot series with default options.
     */
    public BoxPlotSeries(BoxWhiskerData data) {
        this(data, new BoxPlotSeriesOptions());
    }

    /**
     * Creates a box plot series with the given options.
     */
    public BoxPlotSeries(BoxWhiskerData data, BoxPlotSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a box plot series with a custom ID.
     */
    public BoxPlotSeries(String id, BoxWhiskerData data, BoxPlotSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.BOX_PLOT;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor boxDesc = BufferDescriptor.positionColor2D(2048);
        boxBuffer = resources.getOrCreateBuffer(id + "_box", boxDesc);

        BufferDescriptor lineDesc = BufferDescriptor.positionColor2D(4096);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        vertexCapacity = 256;
        boxVertices = new float[vertexCapacity * VERTICES_PER_BOX * FLOATS_PER_VERTEX];
        // Lines: box outline (8), whiskers (4), caps (4), median (2) = 18 vertices per box
        lineVertices = new float[vertexCapacity * 20 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_box");
            resourceManager.disposeBuffer(id + "_line");
        }
        boxBuffer = null;
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

        int visibleCount = lastIdx - firstIdx + 1;
        ensureCapacity(visibleCount);

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        double barSlotWidth = ctx.getBarWidth();
        double boxWidth = barSlotWidth * options.getBoxWidthRatio();
        double halfBoxWidth = boxWidth / 2.0;
        double whiskerCapWidth = boxWidth * options.getWhiskerCapRatio();
        double halfCapWidth = whiskerCapWidth / 2.0;

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw filled boxes
        int boxFloatCount = buildBoxVertices(coords, firstIdx, lastIdx, halfBoxWidth);
        if (boxFloatCount > 0) {
            boxBuffer.upload(boxVertices, 0, boxFloatCount);
            boxBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw lines (outlines, whiskers, median)
        ctx.getDevice().setLineWidth(options.getLineWidth());
        int lineFloatCount = buildLineVertices(coords, firstIdx, lastIdx, halfBoxWidth, halfCapWidth);
        if (lineFloatCount > 0) {
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private int buildBoxVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                  double halfBoxWidth) {
        int floatIndex = 0;

        float[] q1Array = data.getQ1Array();
        float[] q3Array = data.getQ3Array();

        Color boxColor = options.getBoxColor();
        float r = boxColor.getRed() / 255f;
        float g = boxColor.getGreen() / 255f;
        float b = boxColor.getBlue() / 255f;
        float a = (boxColor.getAlpha() / 255f) * options.getOpacity();

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (!data.hasValue(i)) {
                continue;
            }

            // Use index as X value for category-based box plots
            float x = (float) coords.xValueToScreenX(data.getXValue(i));
            float q1Y = (float) coords.yValueToScreenY(q1Array[i]);
            float q3Y = (float) coords.yValueToScreenY(q3Array[i]);

            float left = (float) (x - halfBoxWidth);
            float right = (float) (x + halfBoxWidth);

            // Box rectangle (Q1 to Q3)
            floatIndex = addVertex(boxVertices, floatIndex, left, q3Y, r, g, b, a);
            floatIndex = addVertex(boxVertices, floatIndex, left, q1Y, r, g, b, a);
            floatIndex = addVertex(boxVertices, floatIndex, right, q1Y, r, g, b, a);

            floatIndex = addVertex(boxVertices, floatIndex, left, q3Y, r, g, b, a);
            floatIndex = addVertex(boxVertices, floatIndex, right, q1Y, r, g, b, a);
            floatIndex = addVertex(boxVertices, floatIndex, right, q3Y, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildLineVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                   double halfBoxWidth, double halfCapWidth) {
        int floatIndex = 0;

        float[] minArray = data.getMinArray();
        float[] q1Array = data.getQ1Array();
        float[] medianArray = data.getMedianArray();
        float[] q3Array = data.getQ3Array();
        float[] maxArray = data.getMaxArray();

        Color borderColor = options.getBorderColor();
        float br = borderColor.getRed() / 255f;
        float bg = borderColor.getGreen() / 255f;
        float bb = borderColor.getBlue() / 255f;
        float ba = options.getOpacity();

        Color medianColor = options.getMedianColor();
        float mr = medianColor.getRed() / 255f;
        float mg = medianColor.getGreen() / 255f;
        float mb = medianColor.getBlue() / 255f;

        for (int i = firstIdx; i <= lastIdx; i++) {
            if (!data.hasValue(i)) {
                continue;
            }

            // Use index as X value for category-based box plots
            float x = (float) coords.xValueToScreenX(data.getXValue(i));
            float minY = (float) coords.yValueToScreenY(minArray[i]);
            float q1Y = (float) coords.yValueToScreenY(q1Array[i]);
            float medianY = (float) coords.yValueToScreenY(medianArray[i]);
            float q3Y = (float) coords.yValueToScreenY(q3Array[i]);
            float maxY = (float) coords.yValueToScreenY(maxArray[i]);

            float left = (float) (x - halfBoxWidth);
            float right = (float) (x + halfBoxWidth);
            float capLeft = (float) (x - halfCapWidth);
            float capRight = (float) (x + halfCapWidth);

            // Box outline (4 lines)
            // Left edge
            floatIndex = addVertex(lineVertices, floatIndex, left, q1Y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, left, q3Y, br, bg, bb, ba);
            // Right edge
            floatIndex = addVertex(lineVertices, floatIndex, right, q1Y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, right, q3Y, br, bg, bb, ba);
            // Top edge
            floatIndex = addVertex(lineVertices, floatIndex, left, q3Y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, right, q3Y, br, bg, bb, ba);
            // Bottom edge
            floatIndex = addVertex(lineVertices, floatIndex, left, q1Y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, right, q1Y, br, bg, bb, ba);

            // Lower whisker (min to Q1)
            floatIndex = addVertex(lineVertices, floatIndex, x, minY, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, x, q1Y, br, bg, bb, ba);

            // Upper whisker (Q3 to max)
            floatIndex = addVertex(lineVertices, floatIndex, x, q3Y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, x, maxY, br, bg, bb, ba);

            // Lower cap
            floatIndex = addVertex(lineVertices, floatIndex, capLeft, minY, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, capRight, minY, br, bg, bb, ba);

            // Upper cap
            floatIndex = addVertex(lineVertices, floatIndex, capLeft, maxY, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, capRight, maxY, br, bg, bb, ba);

            // Median line (different color)
            floatIndex = addVertex(lineVertices, floatIndex, left, medianY, mr, mg, mb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, right, medianY, mr, mg, mb, ba);
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

    private void ensureCapacity(int boxCount) {
        if (boxCount > vertexCapacity) {
            vertexCapacity = boxCount + boxCount / 2;
            boxVertices = new float[vertexCapacity * VERTICES_PER_BOX * FLOATS_PER_VERTEX];
            lineVertices = new float[vertexCapacity * 20 * FLOATS_PER_VERTEX];
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
