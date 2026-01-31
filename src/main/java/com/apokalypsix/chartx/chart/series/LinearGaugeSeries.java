package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.LinearGaugeSeriesOptions;
import com.apokalypsix.chartx.chart.data.GaugeData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

/**
 * Renderable linear gauge series using GaugeData.
 *
 * <p>Renders horizontal or vertical gauge bars with optional tick marks,
 * labels, and color zones.
 */
public class LinearGaugeSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final GaugeData data;
    private final LinearGaugeSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] fillVertices;
    private float[] lineVertices;

    /**
     * Creates a linear gauge series with default options.
     */
    public LinearGaugeSeries(GaugeData data) {
        this(data, new LinearGaugeSeriesOptions());
    }

    /**
     * Creates a linear gauge series with the given options.
     */
    public LinearGaugeSeries(GaugeData data, LinearGaugeSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a linear gauge series with a custom ID.
     */
    public LinearGaugeSeries(String id, GaugeData data, LinearGaugeSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    public String getId() {
        return id;
    }

    public GaugeData getData() {
        return data;
    }

    public LinearGaugeSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.LINEAR_GAUGE;
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        fillBuffer = resources.getOrCreateBuffer(id + "_fill",
                BufferDescriptor.positionColor2D(2048 * FLOATS_PER_VERTEX));

        lineBuffer = resources.getOrCreateBuffer(id + "_line",
                BufferDescriptor.positionColor2D(1024 * FLOATS_PER_VERTEX));

        fillVertices = new float[2048 * FLOATS_PER_VERTEX];
        lineVertices = new float[1024 * FLOATS_PER_VERTEX];

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
     * Renders the linear gauge.
     *
     * @param ctx render context
     * @param x left edge of rendering area
     * @param y top edge of rendering area
     * @param width available width
     * @param height available height
     */
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        float padding = options.getPadding();

        // Calculate track bounds
        float trackX, trackY, trackWidth, trackHeight;

        if (options.isHorizontal()) {
            trackHeight = (height - 2 * padding) * options.getTrackThickness();
            trackY = y + (height - trackHeight) / 2;
            trackX = x + padding;
            trackWidth = width - 2 * padding;
        } else {
            trackWidth = (width - 2 * padding) * options.getTrackThickness();
            trackX = x + (width - trackWidth) / 2;
            trackY = y + padding;
            trackHeight = height - 2 * padding;
        }

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Build and draw fills (track + value)
        int fillFloatCount = buildFillVertices(trackX, trackY, trackWidth, trackHeight);
        if (fillFloatCount > 0) {
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders and ticks
        int lineFloatCount = buildLineVertices(trackX, trackY, trackWidth, trackHeight);
        if (lineFloatCount > 0) {
            ctx.getDevice().setLineWidth(options.getBorderWidth());
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private int buildFillVertices(float trackX, float trackY, float trackWidth, float trackHeight) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        boolean horizontal = options.isHorizontal();

        // Draw track background
        Color trackColor = options.getTrackColor();
        float tr = trackColor.getRed() / 255f;
        float tg = trackColor.getGreen() / 255f;
        float tb = trackColor.getBlue() / 255f;
        float ta = (trackColor.getAlpha() / 255f) * opacity;

        floatIndex = addQuad(fillVertices, floatIndex, trackX, trackY, trackWidth, trackHeight,
                tr, tg, tb, ta);

        // Draw zones (behind value fill)
        List<GaugeData.Zone> zones = data.getZones();
        if (!zones.isEmpty()) {
            for (GaugeData.Zone zone : zones) {
                float startNorm = (zone.getStart() - data.getMinValue()) / data.getRange();
                float endNorm = (zone.getEnd() - data.getMinValue()) / data.getRange();

                startNorm = Math.max(0, Math.min(1, startNorm));
                endNorm = Math.max(0, Math.min(1, endNorm));

                Color zoneColor = zone.getColor();
                float zr = zoneColor.getRed() / 255f;
                float zg = zoneColor.getGreen() / 255f;
                float zb = zoneColor.getBlue() / 255f;
                float za = (zoneColor.getAlpha() / 255f) * opacity * 0.5f; // Semi-transparent zones

                if (horizontal) {
                    float zoneX = trackX + startNorm * trackWidth;
                    float zoneWidth = (endNorm - startNorm) * trackWidth;
                    floatIndex = addQuad(fillVertices, floatIndex, zoneX, trackY, zoneWidth, trackHeight,
                            zr, zg, zb, za);
                } else {
                    float zoneY = trackY + (1 - endNorm) * trackHeight;
                    float zoneHeight = (endNorm - startNorm) * trackHeight;
                    floatIndex = addQuad(fillVertices, floatIndex, trackX, zoneY, trackWidth, zoneHeight,
                            zr, zg, zb, za);
                }
            }
        }

        // Draw value fill
        if (options.getIndicatorStyle() != LinearGaugeSeriesOptions.IndicatorStyle.MARKER) {
            float normalized = data.getNormalizedValue();

            Color fillColor;
            if (options.isUseZoneColors()) {
                fillColor = data.getColorForValue(data.getValue(), options.getFillColor());
            } else {
                fillColor = options.getFillColor();
            }

            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = (fillColor.getAlpha() / 255f) * opacity;

            if (horizontal) {
                float fillWidth = normalized * trackWidth;
                floatIndex = addQuad(fillVertices, floatIndex, trackX, trackY, fillWidth, trackHeight,
                        fr, fg, fb, fa);
            } else {
                float fillHeight = normalized * trackHeight;
                float fillY = trackY + trackHeight - fillHeight;
                floatIndex = addQuad(fillVertices, floatIndex, trackX, fillY, trackWidth, fillHeight,
                        fr, fg, fb, fa);
            }
        }

        // Draw marker if needed
        if (options.getIndicatorStyle() != LinearGaugeSeriesOptions.IndicatorStyle.FILL) {
            float normalized = data.getNormalizedValue();

            Color markerColor = options.getMarkerColor();
            float mr = markerColor.getRed() / 255f;
            float mg = markerColor.getGreen() / 255f;
            float mb = markerColor.getBlue() / 255f;
            float ma = opacity;

            float markerWidth = options.getMarkerWidth();

            if (horizontal) {
                float markerX = trackX + normalized * trackWidth - markerWidth / 2;
                floatIndex = addQuad(fillVertices, floatIndex, markerX, trackY, markerWidth, trackHeight,
                        mr, mg, mb, ma);
            } else {
                float markerY = trackY + (1 - normalized) * trackHeight - markerWidth / 2;
                floatIndex = addQuad(fillVertices, floatIndex, trackX, markerY, trackWidth, markerWidth,
                        mr, mg, mb, ma);
            }
        }

        return floatIndex;
    }

    private int buildLineVertices(float trackX, float trackY, float trackWidth, float trackHeight) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        boolean horizontal = options.isHorizontal();

        // Draw border
        if (options.getBorderWidth() > 0) {
            Color borderColor = options.getBorderColor();
            float br = borderColor.getRed() / 255f;
            float bg = borderColor.getGreen() / 255f;
            float bb = borderColor.getBlue() / 255f;
            float ba = (borderColor.getAlpha() / 255f) * opacity;

            // Four edges
            floatIndex = addLine(lineVertices, floatIndex, trackX, trackY, trackX + trackWidth, trackY,
                    br, bg, bb, ba);
            floatIndex = addLine(lineVertices, floatIndex, trackX + trackWidth, trackY,
                    trackX + trackWidth, trackY + trackHeight, br, bg, bb, ba);
            floatIndex = addLine(lineVertices, floatIndex, trackX + trackWidth, trackY + trackHeight,
                    trackX, trackY + trackHeight, br, bg, bb, ba);
            floatIndex = addLine(lineVertices, floatIndex, trackX, trackY + trackHeight, trackX, trackY,
                    br, bg, bb, ba);
        }

        // Draw ticks
        if (options.isShowTicks()) {
            Color tickColor = options.getTickColor();
            float tr = tickColor.getRed() / 255f;
            float tg = tickColor.getGreen() / 255f;
            float tb = tickColor.getBlue() / 255f;
            float ta = opacity;

            int majorCount = options.getMajorTickCount();
            int minorPerMajor = options.getMinorTicksPerMajor();
            float majorLen = options.getMajorTickLength();
            float minorLen = options.getMinorTickLength();

            // Major ticks
            for (int i = 0; i < majorCount; i++) {
                float t = (float) i / (majorCount - 1);

                if (horizontal) {
                    float tickX = trackX + t * trackWidth;
                    floatIndex = addLine(lineVertices, floatIndex, tickX, trackY + trackHeight,
                            tickX, trackY + trackHeight + majorLen, tr, tg, tb, ta);
                } else {
                    float tickY = trackY + (1 - t) * trackHeight;
                    floatIndex = addLine(lineVertices, floatIndex, trackX + trackWidth, tickY,
                            trackX + trackWidth + majorLen, tickY, tr, tg, tb, ta);
                }

                // Minor ticks between major ticks
                if (i < majorCount - 1 && minorPerMajor > 0) {
                    float majorStep = 1.0f / (majorCount - 1);
                    float minorStep = majorStep / (minorPerMajor + 1);

                    for (int j = 1; j <= minorPerMajor; j++) {
                        float minorT = t + j * minorStep;

                        if (horizontal) {
                            float tickX = trackX + minorT * trackWidth;
                            floatIndex = addLine(lineVertices, floatIndex, tickX, trackY + trackHeight,
                                    tickX, trackY + trackHeight + minorLen, tr, tg, tb, ta);
                        } else {
                            float tickY = trackY + (1 - minorT) * trackHeight;
                            floatIndex = addLine(lineVertices, floatIndex, trackX + trackWidth, tickY,
                                    trackX + trackWidth + minorLen, tickY, tr, tg, tb, ta);
                        }
                    }
                }
            }
        }

        return floatIndex;
    }

    private int addQuad(float[] vertices, int index, float x, float y, float width, float height,
                        float r, float g, float b, float a) {
        // Triangle 1
        index = addVertex(vertices, index, x, y, r, g, b, a);
        index = addVertex(vertices, index, x, y + height, r, g, b, a);
        index = addVertex(vertices, index, x + width, y + height, r, g, b, a);
        // Triangle 2
        index = addVertex(vertices, index, x, y, r, g, b, a);
        index = addVertex(vertices, index, x + width, y + height, r, g, b, a);
        index = addVertex(vertices, index, x + width, y, r, g, b, a);
        return index;
    }

    private int addLine(float[] vertices, int index, float x1, float y1, float x2, float y2,
                        float r, float g, float b, float a) {
        index = addVertex(vertices, index, x1, y1, r, g, b, a);
        index = addVertex(vertices, index, x2, y2, r, g, b, a);
        return index;
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
