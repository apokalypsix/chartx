package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.ContourSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ColorMap;
import com.apokalypsix.chartx.core.render.util.MarchingSquares;
import com.apokalypsix.chartx.chart.data.HeatmapData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable contour series using HeatmapData.
 *
 * <p>Extracts and renders iso-contour lines from 2D grid data using
 * the Marching Squares algorithm. Supports multiple contour levels
 * with color mapping.
 */
public class ContourSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final HeatmapData data;
    private final ContourSeriesOptions options;

    // Rendering resources
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Contour extraction cache
    private float[] cachedContourData;
    private int[] cachedSegmentCounts;
    private float[] cachedLevels;
    private boolean cacheValid;

    // Reusable arrays
    private float[] lineVertices;
    private float[] rgba = new float[4];

    /**
     * Creates a contour series with default options.
     */
    public ContourSeries(HeatmapData data) {
        this(data, new ContourSeriesOptions());
    }

    /**
     * Creates a contour series with the given options.
     */
    public ContourSeries(HeatmapData data, ContourSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a contour series with a custom ID.
     */
    public ContourSeries(String id, HeatmapData data, ContourSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
        this.cacheValid = false;
    }

    public String getId() {
        return id;
    }

    public HeatmapData getData() {
        return data;
    }

    public ContourSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.CONTOUR;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Invalidates the contour cache, forcing recalculation on next render.
     */
    public void invalidateCache() {
        cacheValid = false;
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        BufferDescriptor lineDesc = BufferDescriptor.positionColor2D(65536);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        // Allocate contour extraction buffers
        int maxOutputSize = MarchingSquares.estimateOutputSize(
                data.getRows(), data.getCols(), options.getLevelCount());
        cachedContourData = new float[maxOutputSize];
        cachedSegmentCounts = new int[options.getLevelCount()];

        lineVertices = new float[maxOutputSize * 3]; // More than enough for vertices

        initialized = true;
    }

    /**
     * Disposes rendering resources.
     */
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_line");
        }
        lineBuffer = null;
        initialized = false;
    }

    /**
     * Renders the contour lines.
     */
    public void render(RenderContext ctx) {
        if (!initialized || data.isEmpty() || !options.isVisible()) {
            return;
        }

        // Ensure contours are extracted
        if (!cacheValid) {
            extractContours();
        }

        if (cachedLevels == null || cachedLevels.length == 0) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        ctx.getDevice().setLineWidth(options.getLineWidth());

        // Build and draw contour lines
        int lineFloatCount = buildLineVertices(coords);
        if (lineFloatCount > 0) {
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private void extractContours() {
        // Generate contour levels
        float minValue = data.getMinValue();
        float maxValue = data.getMaxValue();
        cachedLevels = options.generateLevels(minValue, maxValue);

        // Ensure buffers are large enough
        int maxOutput = MarchingSquares.estimateOutputSize(
                data.getRows(), data.getCols(), cachedLevels.length);
        if (cachedContourData.length < maxOutput) {
            cachedContourData = new float[maxOutput];
        }
        if (cachedSegmentCounts.length < cachedLevels.length) {
            cachedSegmentCounts = new int[cachedLevels.length];
        }

        // Extract contours for all levels
        MarchingSquares.extractContours(
                data.getValuesArray(),
                data.getRows(),
                data.getCols(),
                data.getXCoordsArray(),
                data.getYCoordsArray(),
                cachedLevels,
                cachedContourData, 0,
                cachedSegmentCounts);

        cacheValid = true;
    }

    private int buildLineVertices(CoordinateSystem coords) {
        int floatIndex = 0;

        ColorMap colorMap = options.getColorMap();
        float minValue = data.getMinValue();
        float maxValue = data.getMaxValue();
        colorMap.valueRange(minValue, maxValue);

        Color fixedLineColor = options.getLineColor();
        float opacity = options.getOpacity();

        int contourDataOffset = 0;

        for (int level = 0; level < cachedLevels.length; level++) {
            int floatsForLevel = cachedSegmentCounts[level];
            int segmentCount = floatsForLevel / 4; // Each segment is 4 floats (x1,y1,x2,y2)

            // Determine color for this level
            float r, g, b, a;
            if (fixedLineColor != null) {
                r = fixedLineColor.getRed() / 255f;
                g = fixedLineColor.getGreen() / 255f;
                b = fixedLineColor.getBlue() / 255f;
                a = (fixedLineColor.getAlpha() / 255f) * opacity;
            } else {
                colorMap.getColor(cachedLevels[level], rgba);
                r = rgba[0];
                g = rgba[1];
                b = rgba[2];
                a = rgba[3] * opacity;
            }

            // Convert each segment to screen coordinates
            for (int seg = 0; seg < segmentCount; seg++) {
                int idx = contourDataOffset + seg * 4;

                // Data coordinates
                float dataX1 = cachedContourData[idx];
                float dataY1 = cachedContourData[idx + 1];
                float dataX2 = cachedContourData[idx + 2];
                float dataY2 = cachedContourData[idx + 3];

                // Convert to screen coordinates
                float screenX1 = (float) coords.xValueToScreenX((long) dataX1);
                float screenY1 = (float) coords.yValueToScreenY(dataY1);
                float screenX2 = (float) coords.xValueToScreenX((long) dataX2);
                float screenY2 = (float) coords.yValueToScreenY(dataY2);

                // Add line segment vertices
                floatIndex = addVertex(lineVertices, floatIndex, screenX1, screenY1, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, screenX2, screenY2, r, g, b, a);
            }

            contourDataOffset += floatsForLevel;
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

    /**
     * Returns the contour levels currently in use.
     */
    public float[] getContourLevels() {
        if (!cacheValid) {
            extractContours();
        }
        return cachedLevels;
    }
}
