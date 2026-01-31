package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for chart axes using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link AxisLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Renders axis backgrounds, lines, and tick marks using the abstracted API.
 * Text labels are rendered separately by TextOverlay using Java2D.
 */
public class AxisLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(AxisLayerV2.class);

    /** Z-order for the axis layer (renders after data, before axis labels) */
    public static final int Z_ORDER = 850;

    // Colors
    private Color axisColor = new Color(80, 82, 86);
    private Color backgroundColor = new Color(20, 22, 25);

    // V2 API resources
    private Buffer backgroundBuffer;  // For axis backgrounds (triangles)
    private Buffer lineBuffer;        // For lines and ticks
    private Shader shader;
    private boolean v2Initialized = false;

    // Vertex data
    private float[] lineVertices;
    private int vertexCapacity;

    // Tick mark size
    private static final int TICK_SIZE = 4;

    // Floats per vertex: x, y
    private static final int FLOATS_PER_VERTEX = 2;

    public AxisLayerV2() {
        super(Z_ORDER);
        vertexCapacity = 128;
        lineVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("AxisLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for axis backgrounds (triangles)
        backgroundBuffer = resources.getOrCreateBuffer("axis.background",
                BufferDescriptor.positionOnly2D(72 * FLOATS_PER_VERTEX));

        // Create buffer for lines and ticks
        lineBuffer = resources.getOrCreateBuffer("axis.lines",
                BufferDescriptor.positionOnly2D(256 * FLOATS_PER_VERTEX));

        // Get simple shader (position only, uniform color)
        shader = resources.getShader(ResourceManager.SHADER_SIMPLE);

        v2Initialized = true;
        log.debug("AxisLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("AxisLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();
        MultiAxisCoordinateSystem coords = ctx.getCoordinates();
        YAxisManager axisManager = ctx.getAxisManager();

        // Check if we're in horizontal bar mode (vertical category axis on left/right)
        // A horizontal category axis (bottom/top) is for stacked/column charts - not horizontal bar mode
        boolean horizontalBarMode = ctx.hasCategoryAxis() && !ctx.getCategoryAxis().isHorizontal();

        // Calculate time grid levels (shared across all axes) - only for standard mode
        long[] timeLevels = horizontalBarMode ? new long[0] : calculateTimeGridLevels(viewport);

        // Draw axis backgrounds for all visible axes
        drawAxisBackgrounds(ctx, viewport, axisManager);

        // Draw axis lines and ticks
        drawMultiAxisLines(ctx, viewport, timeLevels, coords, axisManager, horizontalBarMode);
    }

    private void drawAxisBackgrounds(RenderContext ctx, Viewport viewport, YAxisManager axisManager) {
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        int chartLeft = viewport.getLeftInset();
        int chartRight = width - viewport.getRightInset();
        int chartBottom = height - viewport.getBottomInset();

        if (shader == null || !shader.isValid()) {
            return;
        }

        // Count visible axes to allocate enough space
        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);
        int axisCount = leftAxes.size() + rightAxes.size() + 1; // +1 for bottom time axis

        float[] bgVertices = new float[axisCount * 12]; // rectangles * 6 vertices * 2 floats
        int idx = 0;

        // Left axis backgrounds
        int leftX = 0;
        for (YAxis axis : leftAxes) {
            idx = addRect(bgVertices, idx, leftX, 0, leftX + axis.getWidth(), height);
            leftX += axis.getWidth();
        }

        // Right axis backgrounds
        int rightX = chartRight;
        for (YAxis axis : rightAxes) {
            idx = addRect(bgVertices, idx, rightX, 0, rightX + axis.getWidth(), height);
            rightX += axis.getWidth();
        }

        // Bottom axis background (time axis)
        idx = addRect(bgVertices, idx, chartLeft, chartBottom, chartRight, height);

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        shader.setUniform("uColor",
                backgroundColor.getRed() / 255f,
                backgroundColor.getGreen() / 255f,
                backgroundColor.getBlue() / 255f,
                1.0f);

        backgroundBuffer.upload(bgVertices, 0, idx);
        backgroundBuffer.draw(DrawMode.TRIANGLES);

        shader.unbind();
    }

    private void drawMultiAxisLines(RenderContext ctx, Viewport viewport,
                                     long[] timeLevels, MultiAxisCoordinateSystem coords,
                                     YAxisManager axisManager, boolean horizontalBarMode) {
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        int chartLeft = viewport.getLeftInset();
        int chartRight = width - viewport.getRightInset();
        int chartTop = viewport.getTopInset();
        int chartBottom = height - viewport.getBottomInset();

        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);

        // Get default axis for horizontal bar mode
        YAxis defaultAxis = axisManager.getDefaultAxis();

        // Estimate total ticks needed
        int estimatedTicks = timeLevels.length + 10 + (leftAxes.size() + rightAxes.size()) * 12;
        ensureCapacity(estimatedTicks);

        int floatIndex = 0;

        if (!horizontalBarMode) {
            // Standard mode: Draw left axis lines and ticks
            int leftX = 0;
            for (YAxis axis : leftAxes) {
                int axisX = leftX + axis.getWidth();
                CoordinateSystem axisCoords = coords.forAxis(axis.getId());

                // Axis line
                floatIndex = addLine(lineVertices, floatIndex, axisX, chartTop, axisX, chartBottom);

                // Calculate price grid levels for this axis
                double[] priceLevels = calculateAxisGridLevels(axis);

                // Tick marks on the right side of left axis
                for (double price : priceLevels) {
                    float y = (float) axisCoords.yValueToScreenY(price);
                    if (y >= chartTop && y <= chartBottom) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                axisX - TICK_SIZE, y, axisX, y);
                    }
                }

                leftX += axis.getWidth();
            }

            // Draw right axis lines and ticks
            int rightX = chartRight;
            for (YAxis axis : rightAxes) {
                CoordinateSystem axisCoords = coords.forAxis(axis.getId());

                // Axis line
                floatIndex = addLine(lineVertices, floatIndex, rightX, chartTop, rightX, chartBottom);

                // Calculate price grid levels for this axis
                double[] priceLevels = calculateAxisGridLevels(axis);

                // Tick marks on the right side of right axis
                for (double price : priceLevels) {
                    float y = (float) axisCoords.yValueToScreenY(price);
                    if (y >= chartTop && y <= chartBottom) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                rightX, y, rightX + TICK_SIZE, y);
                    }
                }

                rightX += axis.getWidth();
            }
        }

        // Bottom axis line
        floatIndex = addLine(lineVertices, floatIndex, chartLeft, chartBottom, chartRight, chartBottom);

        // Check if there's a horizontal category axis at the bottom
        // If so, CategoryAxisLayerV2 handles the tick marks - skip them here
        boolean hasHorizontalCategoryAxis = ctx.hasCategoryAxis() && ctx.getCategoryAxis().isHorizontal();

        if (horizontalBarMode) {
            // Horizontal bar mode: Draw value-based tick marks at bottom
            if (defaultAxis != null) {
                double[] valueLevels = defaultAxis.calculateGridLevels(8);
                for (double value : valueLevels) {
                    // Map Y-axis value to horizontal X position
                    double normalized = defaultAxis.normalize(value);
                    float x = chartLeft + (float)(normalized * (chartRight - chartLeft));
                    if (x >= chartLeft && x <= chartRight) {
                        floatIndex = addLine(lineVertices, floatIndex,
                                x, chartBottom, x, chartBottom + TICK_SIZE);
                    }
                }
            }
        } else if (!hasHorizontalCategoryAxis) {
            // Standard mode: Time axis tick marks (only when no horizontal category axis)
            for (long time : timeLevels) {
                float x = (float) coords.xValueToScreenX(time);
                if (x >= chartLeft && x <= chartRight) {
                    floatIndex = addLine(lineVertices, floatIndex,
                            x, chartBottom, x, chartBottom + TICK_SIZE);
                }
            }
        }

        if (floatIndex == 0) {
            return;
        }

        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        shader.setUniform("uColor",
                axisColor.getRed() / 255f,
                axisColor.getGreen() / 255f,
                axisColor.getBlue() / 255f,
                1.0f);

        lineBuffer.upload(lineVertices, 0, floatIndex);
        lineBuffer.draw(DrawMode.LINES);

        shader.unbind();
    }

    /**
     * Calculates grid levels for a specific Y-axis based on its value range and scale.
     *
     * <p>Delegates to the axis's scale for proper grid level calculation
     * (e.g., log scales use powers instead of linear intervals).
     */
    private double[] calculateAxisGridLevels(YAxis axis) {
        return axis.calculateGridLevels(8);
    }

    private static final int MAX_GRID_LEVELS = 100;

    // Grid level calculation
    private long[] calculateTimeGridLevels(Viewport viewport) {
        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();
        long duration = endTime - startTime;

        // Handle edge cases
        if (duration <= 0 || startTime >= endTime) {
            return new long[0];
        }

        long interval = calculateTimeInterval(duration);

        // Safety check
        if (interval <= 0) {
            return new long[0];
        }

        long firstLevel = ((startTime / interval) + 1) * interval;

        // Count with safety cap
        int count = 0;
        for (long t = firstLevel; t <= endTime && count < MAX_GRID_LEVELS; t += interval) {
            count++;
        }

        long[] levels = new long[count];
        int i = 0;
        for (long t = firstLevel; t <= endTime && i < count; t += interval) {
            levels[i++] = t;
        }

        return levels;
    }

    private long calculateTimeInterval(long duration) {
        // Handle edge case
        if (duration <= 0) {
            return 60000L;
        }

        long roughInterval = duration / 8;

        long[] niceIntervals = {
                60000L,          // 1 minute
                300000L,         // 5 minutes
                600000L,         // 10 minutes
                900000L,         // 15 minutes
                1800000L,        // 30 minutes
                3600000L,        // 1 hour
                7200000L,        // 2 hours
                14400000L,       // 4 hours
                21600000L,       // 6 hours
                43200000L,       // 12 hours
                86400000L,       // 1 day
                604800000L,      // 1 week
                2592000000L,     // 30 days (month)
                7776000000L,     // 90 days (quarter)
                31536000000L     // 365 days (year)
        };

        for (long interval : niceIntervals) {
            if (interval >= roughInterval) {
                return interval;
            }
        }
        return niceIntervals[niceIntervals.length - 1];
    }

    private int addLine(float[] vertices, int index, float x1, float y1, float x2, float y2) {
        vertices[index++] = x1;
        vertices[index++] = y1;
        vertices[index++] = x2;
        vertices[index++] = y2;
        return index;
    }

    private int addRect(float[] vertices, int index, float x1, float y1, float x2, float y2) {
        // Triangle 1
        vertices[index++] = x1; vertices[index++] = y1;
        vertices[index++] = x1; vertices[index++] = y2;
        vertices[index++] = x2; vertices[index++] = y2;
        // Triangle 2
        vertices[index++] = x1; vertices[index++] = y1;
        vertices[index++] = x2; vertices[index++] = y2;
        vertices[index++] = x2; vertices[index++] = y1;
        return index;
    }

    private void ensureCapacity(int lineCount) {
        int requiredFloats = lineCount * 2 * FLOATS_PER_VERTEX;
        if (requiredFloats > lineVertices.length) {
            vertexCapacity = lineCount + lineCount / 2;
            lineVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        // V2 resources are managed by ResourceManager
        v2Initialized = false;
    }

    /**
     * Disposes V2 resources.
     * Call this when the RenderContext is available during cleanup.
     */
    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            ResourceManager resources = ctx.getResourceManager();
            if (resources != null) {
                resources.disposeBuffer("axis.background");
                resources.disposeBuffer("axis.lines");
            }
            backgroundBuffer = null;
            lineBuffer = null;
            shader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setAxisColor(Color color) {
        this.axisColor = color;
    }

    public Color getAxisColor() {
        return axisColor;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }
}
