package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.PolarCoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.RadarSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ArcUtils;
import com.apokalypsix.chartx.chart.data.RadarData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable radar/spider chart series using RadarData.
 *
 * <p>Renders multiple overlapping polygons on a polar coordinate system,
 * with a grid of concentric circles and radial spokes for axes.
 */
public class RadarSeries implements BoundedRenderable {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int MAX_AXES = 32;

    private final String id;
    private final RadarData data;
    private final RadarSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer gridBuffer;
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private Buffer pointBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] gridVertices;
    private float[] fillVertices;
    private float[] lineVertices;
    private float[] pointVertices;

    // Polar coordinate system
    private PolarCoordinateSystem polarCoords;

    /**
     * Creates a radar series with default options.
     */
    public RadarSeries(RadarData data) {
        this(data, new RadarSeriesOptions());
    }

    /**
     * Creates a radar series with the given options.
     */
    public RadarSeries(RadarData data, RadarSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a radar series with a custom ID.
     */
    public RadarSeries(String id, RadarData data, RadarSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    @Override
    public String getId() {
        return id;
    }

    public RadarData getData() {
        return data;
    }

    public RadarSeriesOptions getOptions() {
        return options;
    }

    @Override
    public SeriesType getType() {
        return SeriesType.RADAR;
    }

    @Override
    public boolean isVisible() {
        return options.isVisible();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void invalidateLayout() {
        // No cached layout to invalidate for radar charts
    }

    @Override
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        // Grid buffer (concentric circles + spokes)
        gridBuffer = resources.getOrCreateBuffer("radar." + id + ".grid",
                BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX));

        // Fill buffer for polygon fills
        fillBuffer = resources.getOrCreateBuffer("radar." + id + ".fill",
                BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX));

        // Line buffer for polygon outlines
        lineBuffer = resources.getOrCreateBuffer("radar." + id + ".line",
                BufferDescriptor.positionColor2D(2048 * FLOATS_PER_VERTEX));

        // Point buffer for data points
        pointBuffer = resources.getOrCreateBuffer("radar." + id + ".point",
                BufferDescriptor.positionColor2D(1024 * FLOATS_PER_VERTEX));

        gridVertices = new float[4096 * FLOATS_PER_VERTEX];
        fillVertices = new float[4096 * FLOATS_PER_VERTEX];
        lineVertices = new float[2048 * FLOATS_PER_VERTEX];
        pointVertices = new float[1024 * FLOATS_PER_VERTEX];

        initialized = true;
    }

    @Override
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer("radar." + id + ".grid");
            resourceManager.disposeBuffer("radar." + id + ".fill");
            resourceManager.disposeBuffer("radar." + id + ".line");
            resourceManager.disposeBuffer("radar." + id + ".point");
        }
        gridBuffer = null;
        fillBuffer = null;
        lineBuffer = null;
        pointBuffer = null;
        resourceManager = null;
        initialized = false;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        // Setup polar coordinates
        float padding = Math.min(width, height) * options.getPaddingRatio();
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        float radius = Math.min(width, height) / 2 - padding;

        polarCoords = new PolarCoordinateSystem(centerX, centerY, radius, 0);

        RenderDevice device = ctx.getDevice();
        ResourceManager resources = ctx.getResourceManager();

        Shader shader = resources.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw grid (circles and spokes)
        if (options.isShowGrid() || options.isShowSpokes()) {
            int gridFloatCount = buildGridVertices();
            if (gridFloatCount > 0) {
                device.setLineWidth(options.getGridWidth());
                gridBuffer.upload(gridVertices, 0, gridFloatCount);
                gridBuffer.draw(DrawMode.LINES);
            }
        }

        // Draw filled polygons
        if (options.isShowFill() && data.getSeriesCount() > 0) {
            int fillFloatCount = buildFillVertices();
            if (fillFloatCount > 0) {
                fillBuffer.upload(fillVertices, 0, fillFloatCount);
                fillBuffer.draw(DrawMode.TRIANGLES);
            }
        }

        // Draw polygon outlines
        if (data.getSeriesCount() > 0) {
            device.setLineWidth(options.getLineWidth());
            int lineFloatCount = buildLineVertices();
            if (lineFloatCount > 0) {
                lineBuffer.upload(lineVertices, 0, lineFloatCount);
                lineBuffer.draw(DrawMode.LINES);
            }
        }

        // Draw data points
        if (options.isShowPoints() && data.getSeriesCount() > 0) {
            int pointFloatCount = buildPointVertices();
            if (pointFloatCount > 0) {
                pointBuffer.upload(pointVertices, 0, pointFloatCount);
                pointBuffer.draw(DrawMode.TRIANGLES);
            }
        }

        shader.unbind();
    }

    private int buildGridVertices() {
        int floatIndex = 0;
        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();
        float radius = polarCoords.getOuterRadius();
        int axisCount = data.getAxisCount();

        // Grid circles
        if (options.isShowGrid()) {
            Color gridColor = options.getGridColor();
            float r = gridColor.getRed() / 255f;
            float g = gridColor.getGreen() / 255f;
            float b = gridColor.getBlue() / 255f;
            float a = gridColor.getAlpha() / 255f;

            int levels = options.getGridLevels();
            int segments = axisCount * 4; // More segments for smoother circles

            for (int level = 1; level <= levels; level++) {
                float levelRadius = radius * level / levels;
                floatIndex = ArcUtils.tessellateArc(gridVertices, floatIndex,
                        centerX, centerY, levelRadius,
                        0, 2 * Math.PI, segments, r, g, b, a);
            }
        }

        // Spokes (axis lines)
        if (options.isShowSpokes()) {
            Color spokeColor = options.getSpokeColor();
            float r = spokeColor.getRed() / 255f;
            float g = spokeColor.getGreen() / 255f;
            float b = spokeColor.getBlue() / 255f;
            float a = spokeColor.getAlpha() / 255f;

            for (int i = 0; i < axisCount; i++) {
                double angle = data.getAxisAngle(i);
                float endX = centerX + radius * (float) Math.cos(angle);
                float endY = centerY + radius * (float) Math.sin(angle);

                floatIndex = addVertex(gridVertices, floatIndex, centerX, centerY, r, g, b, a);
                floatIndex = addVertex(gridVertices, floatIndex, endX, endY, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildFillVertices() {
        int floatIndex = 0;
        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();
        float radius = polarCoords.getOuterRadius();
        int axisCount = data.getAxisCount();
        int seriesCount = data.getSeriesCount();

        for (int s = 0; s < seriesCount; s++) {
            Color fillColor = options.getSeriesFillColor(s);
            float r = fillColor.getRed() / 255f;
            float g = fillColor.getGreen() / 255f;
            float b = fillColor.getBlue() / 255f;
            float a = fillColor.getAlpha() / 255f;

            // Build polygon as triangle fan from center
            float[] points = new float[axisCount * 2];
            for (int i = 0; i < axisCount; i++) {
                float normalized = data.getNormalizedValue(s, i);
                double angle = data.getAxisAngle(i);
                float pointRadius = radius * normalized;
                points[i * 2] = centerX + pointRadius * (float) Math.cos(angle);
                points[i * 2 + 1] = centerY + pointRadius * (float) Math.sin(angle);
            }

            // Create triangles from center to each edge
            for (int i = 0; i < axisCount; i++) {
                int next = (i + 1) % axisCount;
                floatIndex = addVertex(fillVertices, floatIndex, centerX, centerY, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex, points[i * 2], points[i * 2 + 1], r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex, points[next * 2], points[next * 2 + 1], r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildLineVertices() {
        int floatIndex = 0;
        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();
        float radius = polarCoords.getOuterRadius();
        int axisCount = data.getAxisCount();
        int seriesCount = data.getSeriesCount();

        for (int s = 0; s < seriesCount; s++) {
            Color lineColor = options.getSeriesColor(s);
            float r = lineColor.getRed() / 255f;
            float g = lineColor.getGreen() / 255f;
            float b = lineColor.getBlue() / 255f;
            float a = 1.0f;

            // Build polygon outline
            float[] points = new float[axisCount * 2];
            for (int i = 0; i < axisCount; i++) {
                float normalized = data.getNormalizedValue(s, i);
                double angle = data.getAxisAngle(i);
                float pointRadius = radius * normalized;
                points[i * 2] = centerX + pointRadius * (float) Math.cos(angle);
                points[i * 2 + 1] = centerY + pointRadius * (float) Math.sin(angle);
            }

            // Create line segments around the polygon
            for (int i = 0; i < axisCount; i++) {
                int next = (i + 1) % axisCount;
                floatIndex = addVertex(lineVertices, floatIndex, points[i * 2], points[i * 2 + 1], r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, points[next * 2], points[next * 2 + 1], r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildPointVertices() {
        int floatIndex = 0;
        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();
        float radius = polarCoords.getOuterRadius();
        int axisCount = data.getAxisCount();
        int seriesCount = data.getSeriesCount();
        float pointSize = options.getPointSize();

        for (int s = 0; s < seriesCount; s++) {
            Color pointColor = options.getSeriesColor(s);
            float r = pointColor.getRed() / 255f;
            float g = pointColor.getGreen() / 255f;
            float b = pointColor.getBlue() / 255f;
            float a = 1.0f;

            for (int i = 0; i < axisCount; i++) {
                float normalized = data.getNormalizedValue(s, i);
                double angle = data.getAxisAngle(i);
                float pointRadius = radius * normalized;
                float px = centerX + pointRadius * (float) Math.cos(angle);
                float py = centerY + pointRadius * (float) Math.sin(angle);

                // Draw diamond/square marker as 2 triangles
                float half = pointSize / 2;
                floatIndex = addVertex(pointVertices, floatIndex, px, py - half, r, g, b, a);
                floatIndex = addVertex(pointVertices, floatIndex, px - half, py, r, g, b, a);
                floatIndex = addVertex(pointVertices, floatIndex, px + half, py, r, g, b, a);

                floatIndex = addVertex(pointVertices, floatIndex, px, py + half, r, g, b, a);
                floatIndex = addVertex(pointVertices, floatIndex, px - half, py, r, g, b, a);
                floatIndex = addVertex(pointVertices, floatIndex, px + half, py, r, g, b, a);
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
}
