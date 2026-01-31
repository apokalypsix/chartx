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
 * Renderable spline line series using XyData.
 *
 * <p>Renders a smooth Catmull-Rom spline curve through the data points.
 * The curve passes through all data points with configurable tension
 * and tessellation quality.
 *
 * <p>Handles gaps in data (NaN values) by breaking the spline at gaps
 * and starting a new segment.
 */
public class SplineLineSeries extends AbstractRenderableSeries<XyData, SplineSeriesOptions> {

    private static final int FLOATS_PER_VERTEX = 2;

    // Rendering resources
    private Buffer lineBuffer;
    private ResourceManager resourceManager;

    // Reusable arrays for spline computation
    private float[] inputX;
    private float[] inputY;
    private float[] splineOutput;
    private int inputCapacity;
    private int outputCapacity;

    /**
     * Creates a spline line series with the given data and default options.
     */
    public SplineLineSeries(XyData data) {
        this(data, new SplineSeriesOptions());
    }

    /**
     * Creates a spline line series with the given data and options.
     */
    public SplineLineSeries(XyData data, SplineSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a spline line series with a custom ID.
     */
    public SplineLineSeries(String id, XyData data, SplineSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.SPLINE_LINE;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor desc = BufferDescriptor.positionOnly2D(8192);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", desc);

        inputCapacity = 256;
        outputCapacity = inputCapacity * 16; // Account for tessellation expansion
        inputX = new float[inputCapacity];
        inputY = new float[inputCapacity];
        splineOutput = new float[outputCapacity * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_line");
        }
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

        // Include extra points before and after for smooth spline entry/exit
        if (firstIdx > 0) firstIdx--;
        if (lastIdx < data.size() - 1) lastIdx++;

        renderSpline(ctx, firstIdx, lastIdx);
    }

    private void renderSpline(RenderContext ctx, int firstIdx, int lastIdx) {
        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_SIMPLE);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        Color color = options.getColor();
        float opacity = options.getOpacity();
        shader.setUniform("uColor",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                opacity);

        ctx.getDevice().setLineWidth(options.getLineWidth());

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        // Process data in segments separated by NaN gaps
        int segmentStart = -1;
        int segmentCount = 0;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                // End current segment and draw it
                if (segmentCount > 0) {
                    renderSplineSegment(coords, timestamps, segmentStart, segmentCount);
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

        // Draw final segment
        if (segmentCount > 0) {
            renderSplineSegment(coords, timestamps, segmentStart, segmentCount);
        }

        shader.unbind();
    }

    private void renderSplineSegment(CoordinateSystem coords,
                                     long[] timestamps, int startIdx, int count) {
        if (count < 2) {
            return;
        }

        // Ensure input arrays are large enough
        ensureInputCapacity(count);

        // Convert data points to screen coordinates
        float[] values = data.getValuesArray();
        for (int i = 0; i < count; i++) {
            int dataIdx = startIdx + i;
            inputX[i] = (float) coords.xValueToScreenX(timestamps[dataIdx]);
            inputY[i] = (float) coords.yValueToScreenY(values[dataIdx]);
        }

        // Calculate required output size
        int segments = options.getSegmentsPerCurve();
        int requiredOutput = CurveUtils.catmullRomOutputSize(count, segments);
        ensureOutputCapacity(requiredOutput);

        // Tessellate spline
        int floatCount = CurveUtils.catmullRom(
                inputX, inputY, 0, count,
                segments, options.getTension(),
                splineOutput, 0);

        if (floatCount < 4) {
            return;
        }

        // Upload and draw
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
