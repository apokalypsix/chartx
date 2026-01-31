package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.SplineSeriesOptions;
import com.apokalypsix.chartx.core.render.util.CurveUtils;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable spline mountain (area) series using XyData.
 *
 * <p>Renders a smooth Catmull-Rom spline curve with a filled area
 * from the curve to the baseline. Supports optional line rendering
 * on top of the fill.
 *
 * <p>Handles gaps in data (NaN values) by breaking the spline at gaps.
 */
public class SplineMountainSeries extends AbstractRenderableSeries<XyData, SplineSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 2;

    // Rendering resources
    private Buffer lineBuffer;
    private Buffer fillBuffer;
    private ResourceManager resourceManager;

    // Reusable arrays for spline computation
    private float[] inputX;
    private float[] inputY;
    private float[] splineOutput;
    private float[] fillVertices;
    private int inputCapacity;
    private int outputCapacity;

    /**
     * Creates a spline mountain series with the given data and default options.
     */
    public SplineMountainSeries(XyData data) {
        this(data, createDefaultMountainOptions());
    }

    /**
     * Creates a spline mountain series with the given data and options.
     */
    public SplineMountainSeries(XyData data, SplineSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a spline mountain series with a custom ID.
     */
    public SplineMountainSeries(String id, XyData data, SplineSeriesOptions options) {
        super(id, data, options);
    }

    private static SplineSeriesOptions createDefaultMountainOptions() {
        return new SplineSeriesOptions()
                .color(new Color(65, 131, 196))
                .fillColor(new Color(65, 131, 196, 77))
                .showLine(true);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.SPLINE_MOUNTAIN;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor lineDesc = BufferDescriptor.positionOnly2D(8192);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        BufferDescriptor fillDesc = BufferDescriptor.positionOnly2D(16384);
        fillBuffer = resources.getOrCreateBuffer(id + "_fill", fillDesc);

        inputCapacity = 256;
        outputCapacity = inputCapacity * 16;
        inputX = new float[inputCapacity];
        inputY = new float[inputCapacity];
        splineOutput = new float[outputCapacity * FLOATS_PER_VERTEX];
        fillVertices = new float[outputCapacity * FLOATS_PER_VERTEX * 2]; // Double for triangle strip
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

        // Include extra points for smooth spline
        if (firstIdx > 0) firstIdx--;
        if (lastIdx < data.size() - 1) lastIdx++;

        // Render fill first, then line on top
        if (options.getFillColor() != null) {
            renderFill(ctx, firstIdx, lastIdx);
        }

        if (options.isShowLine()) {
            renderLine(ctx, firstIdx, lastIdx);
        }
    }

    private void renderFill(RenderContext ctx, int firstIdx, int lastIdx) {
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

        // Process data in segments
        int segmentStart = -1;
        int segmentCount = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                if (segmentCount > 0) {
                    renderFillSegment(coords, timestamps, segmentStart, segmentCount);
                    segmentCount = 0;
                }
                segmentStart = -1;
            } else {
                if (segmentStart < 0) {
                    segmentStart = i;
                }
                segmentCount++;
            }
        }

        if (segmentCount > 0) {
            renderFillSegment(coords, timestamps, segmentStart, segmentCount);
        }

        shader.unbind();
    }

    private void renderFillSegment(CoordinateSystem coords,
                                   long[] timestamps, int startIdx, int count) {
        if (count < 2) {
            return;
        }

        ensureInputCapacity(count);

        float[] values = data.getValuesArray();
        for (int i = 0; i < count; i++) {
            int dataIdx = startIdx + i;
            inputX[i] = (float) coords.xValueToScreenX(timestamps[dataIdx]);
            inputY[i] = (float) coords.yValueToScreenY(values[dataIdx]);
        }

        int segments = options.getSegmentsPerCurve();
        int requiredOutput = CurveUtils.catmullRomOutputSize(count, segments);
        ensureOutputCapacity(requiredOutput);

        int splineFloatCount = CurveUtils.catmullRom(
                inputX, inputY, 0, count,
                segments, options.getTension(),
                splineOutput, 0);

        if (splineFloatCount < 4) {
            return;
        }

        // Build triangle strip: alternate baseline and spline points
        float baselineY = (float) coords.yValueToScreenY(options.getBaseline());
        int splinePointCount = splineFloatCount / 2;
        int fillFloatCount = 0;

        for (int i = 0; i < splinePointCount; i++) {
            float x = splineOutput[i * 2];
            float y = splineOutput[i * 2 + 1];

            // Baseline point
            fillVertices[fillFloatCount++] = x;
            fillVertices[fillFloatCount++] = baselineY;

            // Spline point
            fillVertices[fillFloatCount++] = x;
            fillVertices[fillFloatCount++] = y;
        }

        fillBuffer.upload(fillVertices, 0, fillFloatCount);
        int vertexCount = fillFloatCount / FLOATS_PER_VERTEX;
        fillBuffer.draw(DrawMode.TRIANGLE_STRIP, 0, vertexCount);
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
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                options.getOpacity());

        ctx.getDevice().setLineWidth(options.getLineWidth());

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        int segmentStart = -1;
        int segmentCount = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                if (segmentCount > 0) {
                    renderLineSegment(coords, timestamps, segmentStart, segmentCount);
                    segmentCount = 0;
                }
                segmentStart = -1;
            } else {
                if (segmentStart < 0) {
                    segmentStart = i;
                }
                segmentCount++;
            }
        }

        if (segmentCount > 0) {
            renderLineSegment(coords, timestamps, segmentStart, segmentCount);
        }

        shader.unbind();
    }

    private void renderLineSegment(CoordinateSystem coords,
                                   long[] timestamps, int startIdx, int count) {
        if (count < 2) {
            return;
        }

        ensureInputCapacity(count);

        float[] values = data.getValuesArray();
        for (int i = 0; i < count; i++) {
            int dataIdx = startIdx + i;
            inputX[i] = (float) coords.xValueToScreenX(timestamps[dataIdx]);
            inputY[i] = (float) coords.yValueToScreenY(values[dataIdx]);
        }

        int segments = options.getSegmentsPerCurve();
        int requiredOutput = CurveUtils.catmullRomOutputSize(count, segments);
        ensureOutputCapacity(requiredOutput);

        int floatCount = CurveUtils.catmullRom(
                inputX, inputY, 0, count,
                segments, options.getTension(),
                splineOutput, 0);

        if (floatCount < 4) {
            return;
        }

        lineBuffer.upload(splineOutput, 0, floatCount);
        int vertexCount = floatCount / FLOATS_PER_VERTEX;
        lineBuffer.draw(DrawMode.LINE_STRIP, 0, vertexCount);
    }

    private void ensureInputCapacity(int count) {
        if (count > inputCapacity) {
            inputCapacity = count + count / 2;
            inputX = new float[inputCapacity];
            inputY = new float[inputCapacity];
        }
    }

    private void ensureOutputCapacity(int floatCount) {
        if (floatCount > splineOutput.length) {
            outputCapacity = floatCount + floatCount / 2;
            splineOutput = new float[outputCapacity];
            fillVertices = new float[outputCapacity * 2];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        double dataMin = data.findMinValue(startIdx, endIdx);
        // Include baseline in min/max for proper auto-scaling
        return Math.min(dataMin, options.getBaseline());
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        double dataMax = data.findMaxValue(startIdx, endIdx);
        return Math.max(dataMax, options.getBaseline());
    }
}
