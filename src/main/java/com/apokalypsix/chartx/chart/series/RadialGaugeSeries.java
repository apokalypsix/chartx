package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.RadialGaugeSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ArcUtils;
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
 * Renderable radial gauge series using GaugeData.
 *
 * <p>Renders arc-based gauges with needles, color zones,
 * and tick marks. Supports multiple needles for multi-value gauges.
 */
public class RadialGaugeSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final GaugeData data;
    private final RadialGaugeSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] fillVertices;
    private float[] lineVertices;

    /**
     * Creates a radial gauge series with default options.
     */
    public RadialGaugeSeries(GaugeData data) {
        this(data, new RadialGaugeSeriesOptions());
    }

    /**
     * Creates a radial gauge series with the given options.
     */
    public RadialGaugeSeries(GaugeData data, RadialGaugeSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a radial gauge series with a custom ID.
     */
    public RadialGaugeSeries(String id, GaugeData data, RadialGaugeSeriesOptions options) {
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

    public RadialGaugeSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.RADIAL_GAUGE;
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
                BufferDescriptor.positionColor2D(16384 * FLOATS_PER_VERTEX));

        lineBuffer = resources.getOrCreateBuffer(id + "_line",
                BufferDescriptor.positionColor2D(8192 * FLOATS_PER_VERTEX));

        fillVertices = new float[16384 * FLOATS_PER_VERTEX];
        lineVertices = new float[8192 * FLOATS_PER_VERTEX];

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
     * Renders the radial gauge.
     *
     * @param ctx render context
     * @param centerX center X position
     * @param centerY center Y position
     * @param radius outer radius
     */
    public void render(RenderContext ctx, float centerX, float centerY, float radius) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Build and draw fills (track, zones, value arc, needles, center cap)
        int fillFloatCount = buildFillVertices(centerX, centerY, radius);
        if (fillFloatCount > 0) {
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw tick marks
        int lineFloatCount = buildLineVertices(centerX, centerY, radius);
        if (lineFloatCount > 0) {
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private int buildFillVertices(float centerX, float centerY, float radius) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        float innerRadius = radius * options.getInnerRadiusRatio();

        double startAngle = Math.toRadians(options.getStartAngle());
        double endAngle = Math.toRadians(options.getEndAngle());
        int totalSegments = (int) (options.getArcSpan() * options.getSegmentsPerDegree());

        // Draw track background (full arc)
        Color trackColor = options.getTrackColor();
        float tr = trackColor.getRed() / 255f;
        float tg = trackColor.getGreen() / 255f;
        float tb = trackColor.getBlue() / 255f;
        float ta = (trackColor.getAlpha() / 255f) * opacity;

        floatIndex = ArcUtils.tessellateDonutSegment(fillVertices, floatIndex,
                centerX, centerY, innerRadius, radius,
                startAngle, endAngle, totalSegments, tr, tg, tb, ta);

        // Draw zones
        List<GaugeData.Zone> zones = data.getZones();
        if (!zones.isEmpty()) {
            for (GaugeData.Zone zone : zones) {
                float startNorm = (zone.getStart() - data.getMinValue()) / data.getRange();
                float endNorm = (zone.getEnd() - data.getMinValue()) / data.getRange();

                startNorm = Math.max(0, Math.min(1, startNorm));
                endNorm = Math.max(0, Math.min(1, endNorm));

                double zoneStartAngle = options.valueToAngle(startNorm);
                double zoneEndAngle = options.valueToAngle(endNorm);
                int zoneSegments = Math.max(4, (int) ((endNorm - startNorm) * totalSegments));

                Color zoneColor = zone.getColor();
                float zr = zoneColor.getRed() / 255f;
                float zg = zoneColor.getGreen() / 255f;
                float zb = zoneColor.getBlue() / 255f;
                float za = (zoneColor.getAlpha() / 255f) * opacity * 0.7f;

                floatIndex = ArcUtils.tessellateDonutSegment(fillVertices, floatIndex,
                        centerX, centerY, innerRadius, radius,
                        zoneStartAngle, zoneEndAngle, zoneSegments, zr, zg, zb, za);
            }
        }

        // Draw value arc if enabled
        if (options.isShowValueArc()) {
            float normalized = data.getNormalizedValue();
            double valueEndAngle = options.valueToAngle(normalized);
            int valueSegments = Math.max(4, (int) (normalized * totalSegments));

            Color valueColor;
            if (options.isUseZoneColors()) {
                valueColor = data.getColorForValue(data.getValue(), options.getValueColor());
            } else {
                valueColor = options.getValueColor();
            }

            float vr = valueColor.getRed() / 255f;
            float vg = valueColor.getGreen() / 255f;
            float vb = valueColor.getBlue() / 255f;
            float va = (valueColor.getAlpha() / 255f) * opacity;

            floatIndex = ArcUtils.tessellateDonutSegment(fillVertices, floatIndex,
                    centerX, centerY, innerRadius, radius,
                    startAngle, valueEndAngle, valueSegments, vr, vg, vb, va);
        }

        // Draw needle(s) for each value
        for (int i = 0; i < data.getValueCount(); i++) {
            float normalized = data.getNormalizedValue(i);
            double needleAngle = options.valueToAngle(normalized);

            Color needleColor = options.getNeedleColor();
            float nr = needleColor.getRed() / 255f;
            float ng = needleColor.getGreen() / 255f;
            float nb = needleColor.getBlue() / 255f;
            float na = opacity;

            floatIndex = buildNeedle(floatIndex, centerX, centerY, radius, needleAngle,
                    nr, ng, nb, na);
        }

        // Draw center cap
        float capRadius = radius * options.getCenterCapRatio();
        if (capRadius > 0) {
            Color capColor = options.getCenterCapColor();
            float cr = capColor.getRed() / 255f;
            float cg = capColor.getGreen() / 255f;
            float cb = capColor.getBlue() / 255f;
            float ca = opacity;

            floatIndex = ArcUtils.tessellatePieSlice(fillVertices, floatIndex,
                    centerX, centerY, capRadius,
                    0, Math.PI * 2, 24, cr, cg, cb, ca);
        }

        return floatIndex;
    }

    private int buildNeedle(int floatIndex, float centerX, float centerY, float radius,
                            double angle, float r, float g, float b, float a) {
        float needleLength = radius * options.getNeedleLengthRatio();
        float needleWidth = options.getNeedleWidth();
        float capRadius = radius * options.getCenterCapRatio();

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Needle tip position
        float tipX = centerX + cos * needleLength;
        float tipY = centerY + sin * needleLength;

        // Perpendicular direction for needle width
        float perpX = -sin;
        float perpY = cos;

        switch (options.getNeedleStyle()) {
            case LINE:
                // Simple line - render as thin quad
                float halfWidth = needleWidth / 2;
                float baseX = centerX + cos * capRadius;
                float baseY = centerY + sin * capRadius;

                floatIndex = addVertex(fillVertices, floatIndex,
                        baseX + perpX * halfWidth, baseY + perpY * halfWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        baseX - perpX * halfWidth, baseY - perpY * halfWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        tipX - perpX * (halfWidth * 0.3f), tipY - perpY * (halfWidth * 0.3f), r, g, b, a);

                floatIndex = addVertex(fillVertices, floatIndex,
                        baseX + perpX * halfWidth, baseY + perpY * halfWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        tipX - perpX * (halfWidth * 0.3f), tipY - perpY * (halfWidth * 0.3f), r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        tipX + perpX * (halfWidth * 0.3f), tipY + perpY * (halfWidth * 0.3f), r, g, b, a);
                break;

            case ARROW:
                // Arrow shape - wide at base, pointed tip
                float baseRadius = capRadius * 1.2f;
                float arrowBaseX = centerX + cos * baseRadius;
                float arrowBaseY = centerY + sin * baseRadius;

                // Tail end (small triangle behind center)
                float tailLength = capRadius * 0.5f;
                float tailX = centerX - cos * tailLength;
                float tailY = centerY - sin * tailLength;
                float tailWidth = needleWidth * 0.6f;

                // Main needle body (trapezoid)
                floatIndex = addVertex(fillVertices, floatIndex,
                        arrowBaseX + perpX * needleWidth, arrowBaseY + perpY * needleWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        arrowBaseX - perpX * needleWidth, arrowBaseY - perpY * needleWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        tipX, tipY, r, g, b, a);

                // Tail
                floatIndex = addVertex(fillVertices, floatIndex,
                        centerX + perpX * tailWidth, centerY + perpY * tailWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        centerX - perpX * tailWidth, centerY - perpY * tailWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        tailX, tailY, r, g, b, a);
                break;

            case TRIANGLE:
                // Simple triangle needle
                float triBaseX = centerX + cos * capRadius;
                float triBaseY = centerY + sin * capRadius;
                float triWidth = needleWidth * 1.5f;

                floatIndex = addVertex(fillVertices, floatIndex,
                        triBaseX + perpX * triWidth, triBaseY + perpY * triWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        triBaseX - perpX * triWidth, triBaseY - perpY * triWidth, r, g, b, a);
                floatIndex = addVertex(fillVertices, floatIndex,
                        tipX, tipY, r, g, b, a);
                break;
        }

        return floatIndex;
    }

    private int buildLineVertices(float centerX, float centerY, float radius) {
        int floatIndex = 0;
        float opacity = options.getOpacity();

        if (!options.isShowTicks()) {
            return 0;
        }

        Color tickColor = options.getTickColor();
        float tr = tickColor.getRed() / 255f;
        float tg = tickColor.getGreen() / 255f;
        float tb = tickColor.getBlue() / 255f;
        float ta = opacity;

        int majorCount = options.getMajorTickCount();
        int minorPerMajor = options.getMinorTicksPerMajor();
        float majorLen = radius * options.getMajorTickLengthRatio();
        float minorLen = radius * options.getMinorTickLengthRatio();
        float outerRadius = radius;

        // Major ticks
        for (int i = 0; i < majorCount; i++) {
            float t = (float) i / (majorCount - 1);
            double angle = options.valueToAngle(t);

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            float outerX = centerX + cos * outerRadius;
            float outerY = centerY + sin * outerRadius;
            float innerX = centerX + cos * (outerRadius - majorLen);
            float innerY = centerY + sin * (outerRadius - majorLen);

            floatIndex = addVertex(lineVertices, floatIndex, outerX, outerY, tr, tg, tb, ta);
            floatIndex = addVertex(lineVertices, floatIndex, innerX, innerY, tr, tg, tb, ta);

            // Minor ticks between major ticks
            if (i < majorCount - 1 && minorPerMajor > 0) {
                float majorStep = 1.0f / (majorCount - 1);
                float minorStep = majorStep / (minorPerMajor + 1);

                for (int j = 1; j <= minorPerMajor; j++) {
                    float minorT = t + j * minorStep;
                    double minorAngle = options.valueToAngle(minorT);

                    float minorCos = (float) Math.cos(minorAngle);
                    float minorSin = (float) Math.sin(minorAngle);

                    float minorOuterX = centerX + minorCos * outerRadius;
                    float minorOuterY = centerY + minorSin * outerRadius;
                    float minorInnerX = centerX + minorCos * (outerRadius - minorLen);
                    float minorInnerY = centerY + minorSin * (outerRadius - minorLen);

                    floatIndex = addVertex(lineVertices, floatIndex,
                            minorOuterX, minorOuterY, tr, tg, tb, ta * 0.7f);
                    floatIndex = addVertex(lineVertices, floatIndex,
                            minorInnerX, minorInnerY, tr, tg, tb, ta * 0.7f);
                }
            }
        }

        // Draw border if enabled
        if (options.getBorderWidth() > 0) {
            Color borderColor = options.getBorderColor();
            float br = borderColor.getRed() / 255f;
            float bg = borderColor.getGreen() / 255f;
            float bb = borderColor.getBlue() / 255f;
            float ba = (borderColor.getAlpha() / 255f) * opacity;

            int segments = (int) (options.getArcSpan() * options.getSegmentsPerDegree());
            double startAngle = Math.toRadians(options.getStartAngle());
            double angleStep = Math.toRadians(options.getArcSpan()) / segments;

            // Outer arc border
            for (int i = 0; i < segments; i++) {
                double a1 = startAngle + i * angleStep;
                double a2 = startAngle + (i + 1) * angleStep;

                float x1 = centerX + (float) Math.cos(a1) * outerRadius;
                float y1 = centerY + (float) Math.sin(a1) * outerRadius;
                float x2 = centerX + (float) Math.cos(a2) * outerRadius;
                float y2 = centerY + (float) Math.sin(a2) * outerRadius;

                floatIndex = addVertex(lineVertices, floatIndex, x1, y1, br, bg, bb, ba);
                floatIndex = addVertex(lineVertices, floatIndex, x2, y2, br, bg, bb, ba);
            }

            // Inner arc border
            float innerRadius = radius * options.getInnerRadiusRatio();
            for (int i = 0; i < segments; i++) {
                double a1 = startAngle + i * angleStep;
                double a2 = startAngle + (i + 1) * angleStep;

                float x1 = centerX + (float) Math.cos(a1) * innerRadius;
                float y1 = centerY + (float) Math.sin(a1) * innerRadius;
                float x2 = centerX + (float) Math.cos(a2) * innerRadius;
                float y2 = centerY + (float) Math.sin(a2) * innerRadius;

                floatIndex = addVertex(lineVertices, floatIndex, x1, y1, br, bg, bb, ba);
                floatIndex = addVertex(lineVertices, floatIndex, x2, y2, br, bg, bb, ba);
            }

            // End cap lines
            double endAngle = Math.toRadians(options.getEndAngle());

            // Start cap
            float startOuterX = centerX + (float) Math.cos(startAngle) * outerRadius;
            float startOuterY = centerY + (float) Math.sin(startAngle) * outerRadius;
            float startInnerX = centerX + (float) Math.cos(startAngle) * innerRadius;
            float startInnerY = centerY + (float) Math.sin(startAngle) * innerRadius;
            floatIndex = addVertex(lineVertices, floatIndex, startOuterX, startOuterY, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, startInnerX, startInnerY, br, bg, bb, ba);

            // End cap
            float endOuterX = centerX + (float) Math.cos(endAngle) * outerRadius;
            float endOuterY = centerY + (float) Math.sin(endAngle) * outerRadius;
            float endInnerX = centerX + (float) Math.cos(endAngle) * innerRadius;
            float endInnerY = centerY + (float) Math.sin(endAngle) * innerRadius;
            floatIndex = addVertex(lineVertices, floatIndex, endOuterX, endOuterY, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, endInnerX, endInnerY, br, bg, bb, ba);
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
