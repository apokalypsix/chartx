package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.PolarCoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.PolarSeriesOptions;
import com.apokalypsix.chartx.core.render.util.CurveUtils;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable polar line series using XyData.
 *
 * <p>Plots data on polar coordinates where the X value maps to angle
 * and the Y value maps to radius. Supports closed paths and spline smoothing.
 */
public class PolarLineSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final XyData data;
    private final PolarSeriesOptions options;

    // Rendering resources
    private Buffer lineBuffer;
    private Buffer fillBuffer;
    private Buffer markerBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] lineVertices;
    private float[] fillVertices;
    private float[] markerVertices;

    // Working arrays for spline
    private float[] splineX;
    private float[] splineY;

    // Polar coordinate system
    private PolarCoordinateSystem polarCoords;

    /**
     * Creates a polar line series with default options.
     */
    public PolarLineSeries(XyData data) {
        this(data, new PolarSeriesOptions());
    }

    /**
     * Creates a polar line series with the given options.
     */
    public PolarLineSeries(XyData data, PolarSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a polar line series with a custom ID.
     */
    public PolarLineSeries(String id, XyData data, PolarSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    public String getId() {
        return id;
    }

    public XyData getData() {
        return data;
    }

    public PolarSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.POLAR_LINE;
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        BufferDescriptor lineDesc = BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        BufferDescriptor fillDesc = BufferDescriptor.positionColor2D(8192 * FLOATS_PER_VERTEX);
        fillBuffer = resources.getOrCreateBuffer(id + "_fill", fillDesc);

        BufferDescriptor markerDesc = BufferDescriptor.positionColor2D(2048 * FLOATS_PER_VERTEX);
        markerBuffer = resources.getOrCreateBuffer(id + "_marker", markerDesc);

        lineVertices = new float[4096 * FLOATS_PER_VERTEX];
        fillVertices = new float[8192 * FLOATS_PER_VERTEX];
        markerVertices = new float[2048 * FLOATS_PER_VERTEX];

        splineX = new float[1024];
        splineY = new float[1024];

        initialized = true;
    }

    /**
     * Disposes rendering resources.
     */
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_line");
            resourceManager.disposeBuffer(id + "_fill");
            resourceManager.disposeBuffer(id + "_marker");
        }
        lineBuffer = null;
        fillBuffer = null;
        markerBuffer = null;
        initialized = false;
    }

    /**
     * Renders the polar line chart.
     */
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        if (!initialized || data.isEmpty() || !options.isVisible()) {
            return;
        }

        // Setup polar coordinates
        float padding = Math.min(width, height) * options.getPaddingRatio();
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        float radius = Math.min(width, height) / 2 - padding;

        // Find value range for radius mapping
        float minValue = data.findMinValue(0, data.size() - 1);
        float maxValue = data.findMaxValue(0, data.size() - 1);

        polarCoords = new PolarCoordinateSystem(centerX, centerY, radius, 0);
        polarCoords.valueRange(minValue, maxValue);

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Convert data points to screen coordinates
        int pointCount = data.size();
        float[] screenX = new float[pointCount];
        float[] screenY = new float[pointCount];
        convertToScreen(screenX, screenY);

        // Apply spline smoothing if enabled
        if (options.isUseSpline() && pointCount >= 3) {
            // CurveUtils outputs interleaved x,y pairs
            int outputSize = CurveUtils.catmullRomOutputSize(pointCount, options.getSplineSegments());
            if (outputSize * 2 > splineX.length) {
                splineX = new float[outputSize * 2];
            }

            int splinePointCount = CurveUtils.catmullRom(
                    screenX, screenY, 0, pointCount,
                    options.getSplineSegments(), options.getSplineTension(),
                    splineX, 0);

            if (splinePointCount > 0) {
                // Deinterleave the output
                screenX = new float[splinePointCount];
                screenY = new float[splinePointCount];
                for (int i = 0; i < splinePointCount; i++) {
                    screenX[i] = splineX[i * 2];
                    screenY[i] = splineX[i * 2 + 1];
                }
                pointCount = splinePointCount;
            }
        }

        // Draw fill
        if (options.isShowFill()) {
            int fillFloatCount = buildFillVertices(screenX, screenY, pointCount);
            if (fillFloatCount > 0) {
                fillBuffer.upload(fillVertices, 0, fillFloatCount);
                int vertexCount = fillFloatCount / FLOATS_PER_VERTEX;
                fillBuffer.draw(DrawMode.TRIANGLES, 0, vertexCount);
            }
        }

        // Draw line
        if (options.isShowLine()) {
            ctx.getDevice().setLineWidth(options.getLineWidth());
            int lineFloatCount = buildLineVertices(screenX, screenY, pointCount);
            if (lineFloatCount > 0) {
                lineBuffer.upload(lineVertices, 0, lineFloatCount);
                int vertexCount = lineFloatCount / FLOATS_PER_VERTEX;
                lineBuffer.draw(DrawMode.LINES, 0, vertexCount);
            }
        }

        // Draw markers (on original data points only)
        if (options.isShowMarkers()) {
            int markerFloatCount = buildMarkerVertices();
            if (markerFloatCount > 0) {
                markerBuffer.upload(markerVertices, 0, markerFloatCount);
                int vertexCount = markerFloatCount / FLOATS_PER_VERTEX;
                markerBuffer.draw(DrawMode.TRIANGLES, 0, vertexCount);
            }
        }

        shader.unbind();
    }

    private void convertToScreen(float[] screenX, float[] screenY) {
        int pointCount = data.size();
        float[] values = data.getValuesArray();

        // Map X values (assumed to be angles or indices) to angles
        // If timestamps are sequential indices, map them around the circle
        long[] timestamps = data.getTimestampsArray();
        long minTime = timestamps[0];
        long maxTime = timestamps[pointCount - 1];
        long timeRange = maxTime - minTime;

        for (int i = 0; i < pointCount; i++) {
            // Map timestamp to angle (0 to 2*PI)
            double fraction = timeRange > 0 ? (double) (timestamps[i] - minTime) / timeRange : 0;
            double angle = -Math.PI / 2 + fraction * 2 * Math.PI; // Start at top

            // Map value to radius
            float radius = polarCoords.valueToRadius(values[i]);

            screenX[i] = polarCoords.polarToScreenX(angle, radius);
            screenY[i] = polarCoords.polarToScreenY(angle, radius);
        }
    }

    private int buildLineVertices(float[] screenX, float[] screenY, int pointCount) {
        int floatIndex = 0;

        Color lineColor = options.getLineColor();
        float r = lineColor.getRed() / 255f;
        float g = lineColor.getGreen() / 255f;
        float b = lineColor.getBlue() / 255f;
        float a = options.getOpacity();

        int segments = options.isClosePath() ? pointCount : pointCount - 1;

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % pointCount;
            floatIndex = addVertex(lineVertices, floatIndex, screenX[i], screenY[i], r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, screenX[next], screenY[next], r, g, b, a);
        }

        return floatIndex;
    }

    private int buildFillVertices(float[] screenX, float[] screenY, int pointCount) {
        int floatIndex = 0;

        Color fillColor = options.getFillColor();
        float r = fillColor.getRed() / 255f;
        float g = fillColor.getGreen() / 255f;
        float b = fillColor.getBlue() / 255f;
        float a = (fillColor.getAlpha() / 255f) * options.getOpacity();

        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();

        // Create triangles from center to each edge
        int segments = options.isClosePath() ? pointCount : pointCount - 1;

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % pointCount;
            floatIndex = addVertex(fillVertices, floatIndex, centerX, centerY, r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, screenX[i], screenY[i], r, g, b, a);
            floatIndex = addVertex(fillVertices, floatIndex, screenX[next], screenY[next], r, g, b, a);
        }

        return floatIndex;
    }

    private int buildMarkerVertices() {
        int floatIndex = 0;
        int pointCount = data.size();
        float[] values = data.getValuesArray();
        long[] timestamps = data.getTimestampsArray();

        Color markerColor = options.getMarkerColor();
        float r = markerColor.getRed() / 255f;
        float g = markerColor.getGreen() / 255f;
        float b = markerColor.getBlue() / 255f;
        float a = options.getOpacity();

        float half = options.getMarkerSize() / 2;

        long minTime = timestamps[0];
        long maxTime = timestamps[pointCount - 1];
        long timeRange = maxTime - minTime;

        for (int i = 0; i < pointCount; i++) {
            if (Float.isNaN(values[i])) {
                continue;
            }

            double fraction = timeRange > 0 ? (double) (timestamps[i] - minTime) / timeRange : 0;
            double angle = -Math.PI / 2 + fraction * 2 * Math.PI;
            float radius = polarCoords.valueToRadius(values[i]);

            float px = polarCoords.polarToScreenX(angle, radius);
            float py = polarCoords.polarToScreenY(angle, radius);

            // Diamond marker
            floatIndex = addVertex(markerVertices, floatIndex, px, py - half, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, px - half, py, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, px + half, py, r, g, b, a);

            floatIndex = addVertex(markerVertices, floatIndex, px, py + half, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, px - half, py, r, g, b, a);
            floatIndex = addVertex(markerVertices, floatIndex, px + half, py, r, g, b, a);
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
}
