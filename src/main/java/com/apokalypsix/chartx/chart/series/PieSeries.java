package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.PolarCoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.PieSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ArcUtils;
import com.apokalypsix.chartx.chart.data.PieData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable pie chart series using PieData.
 *
 * <p>Renders pie slices as filled wedges with optional borders and labels.
 * Supports exploded segments for emphasis. When innerRadiusRatio > 0,
 * renders as a donut chart.
 */
public class PieSeries implements BoundedRenderable {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final PieData data;
    private final PieSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer sliceBuffer;
    private Buffer borderBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] sliceVertices;
    private float[] borderVertices;
    private int vertexCapacity;

    // Polar coordinate system (recreated on each render)
    private PolarCoordinateSystem polarCoords;

    /**
     * Creates a pie series with default options.
     */
    public PieSeries(PieData data) {
        this(data, new PieSeriesOptions());
    }

    /**
     * Creates a pie series with the given options.
     */
    public PieSeries(PieData data, PieSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a pie series with a custom ID.
     */
    public PieSeries(String id, PieData data, PieSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    @Override
    public String getId() {
        return id;
    }

    public PieData getData() {
        return data;
    }

    public PieSeriesOptions getOptions() {
        return options;
    }

    @Override
    public SeriesType getType() {
        return SeriesType.PIE;
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
        // No cached layout to invalidate for pie charts
    }

    @Override
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        int segments = options.getSegmentsPerSlice();
        int maxSlices = 32; // Initial capacity

        // For pie: segments * 3 vertices per slice (triangles)
        // For donut: segments * 6 vertices per slice (2 triangles per segment)
        int verticesPerSlice = segments * 6;
        int floatsPerSlice = verticesPerSlice * FLOATS_PER_VERTEX;

        sliceBuffer = resources.getOrCreateBuffer("pie." + id + ".slices",
                BufferDescriptor.positionColor2D(maxSlices * floatsPerSlice));

        // Border: segments * 2 vertices per slice (lines)
        borderBuffer = resources.getOrCreateBuffer("pie." + id + ".border",
                BufferDescriptor.positionColor2D(maxSlices * segments * 2 * FLOATS_PER_VERTEX));

        vertexCapacity = maxSlices;
        sliceVertices = new float[maxSlices * floatsPerSlice];
        borderVertices = new float[maxSlices * segments * 4 * FLOATS_PER_VERTEX];

        initialized = true;
    }

    @Override
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer("pie." + id + ".slices");
            resourceManager.disposeBuffer("pie." + id + ".border");
        }
        sliceBuffer = null;
        borderBuffer = null;
        resourceManager = null;
        initialized = false;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        if (!initialized || data.isEmpty() || !options.isVisible()) {
            return;
        }

        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        int sliceCount = data.size();
        ensureCapacity(sliceCount);

        // Setup polar coordinates
        float padding = Math.min(width, height) * options.getPaddingRatio();
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        float outerRadius = Math.min(width, height) / 2 - padding;
        float innerRadius = outerRadius * options.getInnerRadiusRatio();

        polarCoords = new PolarCoordinateSystem(centerX, centerY, outerRadius, innerRadius);
        polarCoords.startAngle(options.getStartAngleRadians());
        polarCoords.clockwise(options.isClockwise());

        RenderDevice device = ctx.getDevice();
        ResourceManager resources = ctx.getResourceManager();

        Shader shader = resources.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw filled slices
        int sliceFloatCount = buildSliceVertices();
        if (sliceFloatCount > 0) {
            sliceBuffer.upload(sliceVertices, 0, sliceFloatCount);
            sliceBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders
        if (options.getBorderColor() != null && options.getBorderWidth() > 0) {
            device.setLineWidth(options.getBorderWidth());
            int borderFloatCount = buildBorderVertices();
            if (borderFloatCount > 0) {
                borderBuffer.upload(borderVertices, 0, borderFloatCount);
                borderBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();
    }

    private int buildSliceVertices() {
        int floatIndex = 0;
        int sliceCount = data.size();
        int segments = options.getSegmentsPerSlice();
        float innerRadius = polarCoords.getInnerRadius();
        float outerRadius = polarCoords.getOuterRadius();
        float opacity = options.getOpacity();
        float defaultExplode = options.getDefaultExplodeOffset();

        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();

        for (int i = 0; i < sliceCount; i++) {
            double startAngle = data.getStartAngle(i);
            double sweepAngle = data.getSweepAngle(i);

            if (sweepAngle <= 0) {
                continue;
            }

            double endAngle = startAngle + sweepAngle;

            Color sliceColor = options.getSliceColor(i);
            float r = sliceColor.getRed() / 255f;
            float g = sliceColor.getGreen() / 255f;
            float b = sliceColor.getBlue() / 255f;
            float a = (sliceColor.getAlpha() / 255f) * opacity;

            // Handle explode offset
            float explodeOffset = data.getExplodeOffset(i);
            if (explodeOffset <= 0 && defaultExplode > 0) {
                explodeOffset = defaultExplode;
            }

            float sliceCenterX = centerX;
            float sliceCenterY = centerY;

            if (explodeOffset > 0) {
                double midAngle = startAngle + sweepAngle / 2;
                sliceCenterX += explodeOffset * (float) Math.cos(midAngle);
                sliceCenterY += explodeOffset * (float) Math.sin(midAngle);
            }

            if (innerRadius > 0) {
                // Donut segment
                floatIndex = ArcUtils.tessellateDonutSegment(sliceVertices, floatIndex,
                        sliceCenterX, sliceCenterY, innerRadius, outerRadius,
                        startAngle, endAngle, segments, r, g, b, a);
            } else {
                // Pie slice
                floatIndex = ArcUtils.tessellatePieSlice(sliceVertices, floatIndex,
                        sliceCenterX, sliceCenterY, outerRadius,
                        startAngle, endAngle, segments, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int buildBorderVertices() {
        int floatIndex = 0;
        int sliceCount = data.size();
        int segments = options.getSegmentsPerSlice();
        float innerRadius = polarCoords.getInnerRadius();
        float outerRadius = polarCoords.getOuterRadius();
        float defaultExplode = options.getDefaultExplodeOffset();

        Color borderColor = options.getBorderColor();
        float r = borderColor.getRed() / 255f;
        float g = borderColor.getGreen() / 255f;
        float b = borderColor.getBlue() / 255f;
        float a = (borderColor.getAlpha() / 255f) * options.getOpacity();

        float centerX = polarCoords.getCenterX();
        float centerY = polarCoords.getCenterY();

        for (int i = 0; i < sliceCount; i++) {
            double startAngle = data.getStartAngle(i);
            double sweepAngle = data.getSweepAngle(i);

            if (sweepAngle <= 0) {
                continue;
            }

            double endAngle = startAngle + sweepAngle;

            // Handle explode offset
            float explodeOffset = data.getExplodeOffset(i);
            if (explodeOffset <= 0 && defaultExplode > 0) {
                explodeOffset = defaultExplode;
            }

            float sliceCenterX = centerX;
            float sliceCenterY = centerY;

            if (explodeOffset > 0) {
                double midAngle = startAngle + sweepAngle / 2;
                sliceCenterX += explodeOffset * (float) Math.cos(midAngle);
                sliceCenterY += explodeOffset * (float) Math.sin(midAngle);
            }

            if (innerRadius > 0) {
                // Donut segment border
                floatIndex = ArcUtils.tessellateDonutSegmentBorder(borderVertices, floatIndex,
                        sliceCenterX, sliceCenterY, innerRadius, outerRadius,
                        startAngle, endAngle, segments, r, g, b, a);
            } else {
                // Pie slice border
                floatIndex = ArcUtils.tessellatePieSliceBorder(borderVertices, floatIndex,
                        sliceCenterX, sliceCenterY, outerRadius,
                        startAngle, endAngle, segments, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private void ensureCapacity(int sliceCount) {
        if (sliceCount > vertexCapacity) {
            vertexCapacity = sliceCount + sliceCount / 2;
            int segments = options.getSegmentsPerSlice();
            int verticesPerSlice = segments * 6;
            int floatsPerSlice = verticesPerSlice * FLOATS_PER_VERTEX;

            sliceVertices = new float[vertexCapacity * floatsPerSlice];
            borderVertices = new float[vertexCapacity * segments * 4 * FLOATS_PER_VERTEX];
        }
    }

    /**
     * Returns the slice index at the given screen coordinates, or -1 if none.
     */
    public int hitTest(float screenX, float screenY) {
        if (polarCoords == null || data.isEmpty()) {
            return -1;
        }

        float radius = polarCoords.screenToRadius(screenX, screenY);
        float innerRadius = polarCoords.getInnerRadius();
        float outerRadius = polarCoords.getOuterRadius();

        // Check if within the ring/pie area
        if (radius < innerRadius || radius > outerRadius) {
            return -1;
        }

        double angle = polarCoords.screenToAngle(screenX, screenY);
        return data.indexAtAngle(angle);
    }
}
