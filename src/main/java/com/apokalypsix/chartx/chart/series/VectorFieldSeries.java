package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.VectorFieldSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ColorMap;
import com.apokalypsix.chartx.chart.data.VectorFieldData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable vector field series using VectorFieldData.
 *
 * <p>Renders arrows or lines representing vector directions
 * and magnitudes on a 2D grid.
 */
public class VectorFieldSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final VectorFieldData data;
    private final VectorFieldSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] fillVertices;
    private float[] lineVertices;
    private float[] rgba = new float[4];

    /**
     * Creates a vector field series with default options.
     */
    public VectorFieldSeries(VectorFieldData data) {
        this(data, new VectorFieldSeriesOptions());
    }

    /**
     * Creates a vector field series with the given options.
     */
    public VectorFieldSeries(VectorFieldData data, VectorFieldSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a vector field series with a custom ID.
     */
    public VectorFieldSeries(String id, VectorFieldData data, VectorFieldSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    public String getId() {
        return id;
    }

    public VectorFieldData getData() {
        return data;
    }

    public VectorFieldSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.VECTOR_FIELD;
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        // For triangle arrows
        fillBuffer = resources.getOrCreateBuffer(id + "_fill",
                BufferDescriptor.positionColor2D(32768 * FLOATS_PER_VERTEX));

        // For line arrows
        lineBuffer = resources.getOrCreateBuffer(id + "_line",
                BufferDescriptor.positionColor2D(32768 * FLOATS_PER_VERTEX));

        fillVertices = new float[32768 * FLOATS_PER_VERTEX];
        lineVertices = new float[32768 * FLOATS_PER_VERTEX];

        initialized = true;
    }

    /**
     * Disposes rendering resources.
     */
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_fill");
            resourceManager.disposeBuffer(id + "_line");
        }
        fillBuffer = null;
        lineBuffer = null;
        resourceManager = null;
        initialized = false;
    }

    /**
     * Renders the vector field.
     */
    public void render(RenderContext ctx) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Get visible range from viewport
        Viewport viewport = ctx.getViewport();
        long visibleStart = viewport.getStartTime();
        long visibleEnd = viewport.getEndTime();
        double priceMin = viewport.getMinPrice();
        double priceMax = viewport.getMaxPrice();

        int[] colRange = data.getVisibleColumnRange(visibleStart, visibleEnd);
        int[] rowRange = data.getVisibleRowRange(priceMin, priceMax);

        VectorFieldSeriesOptions.ArrowStyle style = options.getArrowStyle();

        if (style == VectorFieldSeriesOptions.ArrowStyle.TRIANGLE) {
            // Build triangle arrows
            int fillFloatCount = buildTriangleArrows(coords, colRange, rowRange);
            if (fillFloatCount > 0) {
                fillBuffer.upload(fillVertices, 0, fillFloatCount);
                fillBuffer.draw(DrawMode.TRIANGLES);
            }
        } else {
            // Build line arrows
            int lineFloatCount = buildLineArrows(coords, colRange, rowRange, style);
            if (lineFloatCount > 0) {
                ctx.getDevice().setLineWidth(options.getLineWidth());
                lineBuffer.upload(lineVertices, 0, lineFloatCount);
                lineBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();
    }

    private int buildLineArrows(CoordinateSystem coords, int[] colRange, int[] rowRange,
                                 VectorFieldSeriesOptions.ArrowStyle style) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        int skip = options.getSkipFactor();

        ColorMap colorMap = options.getColorMap();
        Color fixedColor = options.getArrowColor();
        float minMag = data.getMinMagnitude();
        float maxMag = data.getMaxMagnitude();
        colorMap.valueRange(minMag, maxMag);

        float lengthScale = options.getLengthScale();
        float minLength = options.getMinLength();
        float maxLength = options.getMaxLength();
        boolean scaleByMag = options.isScaleByMagnitude();
        boolean normalize = options.isNormalizeLength();

        float arrowheadRatio = options.getArrowheadRatio();
        double arrowheadAngle = Math.toRadians(options.getArrowheadAngle());

        for (int row = rowRange[0]; row <= rowRange[1]; row += skip) {
            for (int col = colRange[0]; col <= colRange[1]; col += skip) {
                double x = data.getX(col);
                double y = data.getY(row);
                float dx = data.getDx(row, col);
                float dy = data.getDy(row, col);

                float mag = data.getMagnitude(row, col);
                if (mag < 0.001f) {
                    continue; // Skip zero vectors
                }

                // Get screen position
                float screenX = (float) coords.xValueToScreenX((long) x);
                float screenY = (float) coords.yValueToScreenY(y);

                // Calculate arrow length
                float arrowLength;
                if (normalize) {
                    arrowLength = maxLength * lengthScale;
                } else if (scaleByMag) {
                    float normMag = (mag - minMag) / (maxMag - minMag + 0.001f);
                    arrowLength = minLength + normMag * (maxLength - minLength);
                    arrowLength *= lengthScale;
                } else {
                    arrowLength = mag * lengthScale;
                }
                arrowLength = Math.max(minLength, Math.min(maxLength, arrowLength));

                // Get direction (normalized)
                float dirX = dx / mag;
                float dirY = dy / mag;

                // Arrow endpoint
                float endX = screenX + dirX * arrowLength;
                float endY = screenY - dirY * arrowLength; // Y is inverted in screen coords

                // Get color
                float r, g, b, a;
                if (fixedColor != null) {
                    r = fixedColor.getRed() / 255f;
                    g = fixedColor.getGreen() / 255f;
                    b = fixedColor.getBlue() / 255f;
                    a = (fixedColor.getAlpha() / 255f) * opacity;
                } else {
                    colorMap.getColor(mag, rgba);
                    r = rgba[0];
                    g = rgba[1];
                    b = rgba[2];
                    a = rgba[3] * opacity;
                }

                // Draw arrow line
                floatIndex = addVertex(lineVertices, floatIndex, screenX, screenY, r, g, b, a);
                floatIndex = addVertex(lineVertices, floatIndex, endX, endY, r, g, b, a);

                // Draw arrowhead if style is ARROW
                if (style == VectorFieldSeriesOptions.ArrowStyle.ARROW) {
                    float headLen = arrowLength * arrowheadRatio;

                    // Left barb
                    double angle1 = Math.atan2(-dirY, dirX) + Math.PI - arrowheadAngle;
                    float lx = endX + (float) Math.cos(angle1) * headLen;
                    float ly = endY - (float) Math.sin(angle1) * headLen;

                    floatIndex = addVertex(lineVertices, floatIndex, endX, endY, r, g, b, a);
                    floatIndex = addVertex(lineVertices, floatIndex, lx, ly, r, g, b, a);

                    // Right barb
                    double angle2 = Math.atan2(-dirY, dirX) + Math.PI + arrowheadAngle;
                    float rx = endX + (float) Math.cos(angle2) * headLen;
                    float ry = endY - (float) Math.sin(angle2) * headLen;

                    floatIndex = addVertex(lineVertices, floatIndex, endX, endY, r, g, b, a);
                    floatIndex = addVertex(lineVertices, floatIndex, rx, ry, r, g, b, a);
                }
            }
        }

        return floatIndex;
    }

    private int buildTriangleArrows(CoordinateSystem coords, int[] colRange, int[] rowRange) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        int skip = options.getSkipFactor();

        ColorMap colorMap = options.getColorMap();
        Color fixedColor = options.getArrowColor();
        float minMag = data.getMinMagnitude();
        float maxMag = data.getMaxMagnitude();
        colorMap.valueRange(minMag, maxMag);

        float lengthScale = options.getLengthScale();
        float minLength = options.getMinLength();
        float maxLength = options.getMaxLength();
        boolean scaleByMag = options.isScaleByMagnitude();
        boolean normalize = options.isNormalizeLength();

        for (int row = rowRange[0]; row <= rowRange[1]; row += skip) {
            for (int col = colRange[0]; col <= colRange[1]; col += skip) {
                double x = data.getX(col);
                double y = data.getY(row);
                float dx = data.getDx(row, col);
                float dy = data.getDy(row, col);

                float mag = data.getMagnitude(row, col);
                if (mag < 0.001f) {
                    continue;
                }

                float screenX = (float) coords.xValueToScreenX((long) x);
                float screenY = (float) coords.yValueToScreenY(y);

                // Calculate arrow length
                float arrowLength;
                if (normalize) {
                    arrowLength = maxLength * lengthScale;
                } else if (scaleByMag) {
                    float normMag = (mag - minMag) / (maxMag - minMag + 0.001f);
                    arrowLength = minLength + normMag * (maxLength - minLength);
                    arrowLength *= lengthScale;
                } else {
                    arrowLength = mag * lengthScale;
                }
                arrowLength = Math.max(minLength, Math.min(maxLength, arrowLength));

                // Get direction
                float dirX = dx / mag;
                float dirY = dy / mag;

                // Calculate triangle vertices
                float tipX = screenX + dirX * arrowLength;
                float tipY = screenY - dirY * arrowLength;

                float perpX = dirY;
                float perpY = dirX;
                float baseWidth = arrowLength * 0.3f;

                float base1X = screenX + perpX * baseWidth;
                float base1Y = screenY + perpY * baseWidth;
                float base2X = screenX - perpX * baseWidth;
                float base2Y = screenY - perpY * baseWidth;

                // Get color
                float r, g, b, a;
                if (fixedColor != null) {
                    r = fixedColor.getRed() / 255f;
                    g = fixedColor.getGreen() / 255f;
                    b = fixedColor.getBlue() / 255f;
                    a = (fixedColor.getAlpha() / 255f) * opacity;
                } else {
                    colorMap.getColor(mag, rgba);
                    r = rgba[0];
                    g = rgba[1];
                    b = rgba[2];
                    a = rgba[3] * opacity;
                }

                // Draw triangle
                floatIndex = addVertex(fillVertices, floatIndex, tipX, tipY, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex, base1X, base1Y, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex, base2X, base2Y, r, g, b, a);
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
