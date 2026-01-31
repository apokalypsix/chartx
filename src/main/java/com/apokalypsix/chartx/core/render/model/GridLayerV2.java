package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for chart grid lines using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link GridLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Draws horizontal lines at price levels and vertical lines at time intervals.
 * Grid spacing adapts automatically based on zoom level.
 */
public class GridLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(GridLayerV2.class);

    /** Z-order for the grid layer (renders after background, before data) */
    public static final int Z_ORDER = 50;

    private Color gridColor = new Color(40, 42, 46);
    private float lineWidth = 1.0f;

    // V2 API resources
    private Buffer lineBuffer;
    private Shader shader;
    private boolean v2Initialized = false;

    // Vertex data
    private float[] lineVertices;
    private int vertexCapacity;

    // Floats per vertex: x, y
    private static final int FLOATS_PER_VERTEX = 2;

    public GridLayerV2() {
        super(Z_ORDER);
        vertexCapacity = 256;
        lineVertices = new float[vertexCapacity * 2 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("GridLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create line buffer for grid lines
        lineBuffer = resources.getOrCreateBuffer("grid.lines",
                BufferDescriptor.positionOnly2D(1024 * FLOATS_PER_VERTEX));

        // Get simple shader (position only, uniform color)
        shader = resources.getShader(ResourceManager.SHADER_SIMPLE);

        v2Initialized = true;
        log.debug("GridLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("GridLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinates();
        YAxisManager axisManager = ctx.getAxisManager();

        // Check if we're in horizontal bar mode (category axis present)
        boolean horizontalBarMode = ctx.hasCategoryAxis();

        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();

        int floatIndex = 0;

        if (horizontalBarMode) {
            // Horizontal bar mode: vertical grid lines at value levels
            YAxis defaultAxis = axisManager != null ? axisManager.getDefaultAxis() : null;
            if (defaultAxis != null) {
                double[] valueLevels = defaultAxis.calculateGridLevels(8);
                ensureCapacity(valueLevels.length);

                for (double value : valueLevels) {
                    // Map Y-axis value to horizontal X position
                    double normalized = defaultAxis.normalize(value);
                    float x = chartLeft + (float)(normalized * (chartRight - chartLeft));
                    if (x >= chartLeft && x <= chartRight) {
                        floatIndex = addLine(lineVertices, floatIndex, x, chartTop, x, chartBottom);
                    }
                }
            }
        } else {
            // Standard mode: horizontal lines at price levels, vertical at time levels
            YAxis defaultAxis = axisManager != null ? axisManager.getDefaultAxis() : null;
            double[] priceGridLevels = calculatePriceGridLevels(viewport, defaultAxis);
            long[] timeGridLevels = calculateTimeGridLevels(viewport, ctx.getBarDuration());

            int totalLines = priceGridLevels.length + timeGridLevels.length;
            ensureCapacity(totalLines);

            // Horizontal grid lines (price levels)
            for (double price : priceGridLevels) {
                float y = (float) coords.yValueToScreenY(price);
                floatIndex = addLine(lineVertices, floatIndex, chartLeft, y, chartRight, y);
            }

            // Vertical grid lines (time levels)
            for (long time : timeGridLevels) {
                float x = (float) coords.xValueToScreenX(time);
                if (x >= chartLeft && x <= chartRight) {
                    floatIndex = addLine(lineVertices, floatIndex, x, chartTop, x, chartBottom);
                }
            }
        }

        if (floatIndex == 0) {
            return;
        }

        if (shader == null || !shader.isValid()) {
            return;
        }

        // Set line width
        RenderDevice device = ctx.getDevice();
        device.setLineWidth(lineWidth);

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());
        shader.setUniform("uColor",
                gridColor.getRed() / 255f,
                gridColor.getGreen() / 255f,
                gridColor.getBlue() / 255f,
                1.0f);

        lineBuffer.upload(lineVertices, 0, floatIndex);
        lineBuffer.draw(DrawMode.LINES);

        shader.unbind();
    }

    /**
     * Calculates price levels for horizontal grid lines.
     *
     * <p>If an axis is provided, delegates to the axis's scale for proper
     * grid level calculation (e.g., log scales use powers instead of linear intervals).
     */
    private double[] calculatePriceGridLevels(Viewport viewport, YAxis axis) {
        // Use axis scale if available
        if (axis != null) {
            return axis.calculateGridLevels(8);
        }

        // Fallback to viewport-based linear calculation
        double minPrice = viewport.getMinPrice();
        double maxPrice = viewport.getMaxPrice();
        double range = maxPrice - minPrice;

        // Calculate nice grid interval
        double interval = calculateNiceInterval(range, 8);

        // Find first grid line at or above minPrice
        double firstLevel = Math.ceil(minPrice / interval) * interval;

        // Count levels
        int count = 0;
        for (double p = firstLevel; p <= maxPrice; p += interval) {
            count++;
        }

        double[] levels = new double[count];
        int i = 0;
        for (double p = firstLevel; p <= maxPrice && i < count; p += interval) {
            levels[i++] = p;
        }

        return levels;
    }

    private static final int MAX_GRID_LEVELS = 100;

    /**
     * Calculates time levels for vertical grid lines.
     */
    private long[] calculateTimeGridLevels(Viewport viewport, long barDuration) {
        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();
        long duration = endTime - startTime;

        // Handle edge cases
        if (duration <= 0 || startTime >= endTime) {
            return new long[0];
        }

        // Calculate grid interval based on visible duration
        long interval = calculateTimeInterval(duration, barDuration);

        // Safety check
        if (interval <= 0) {
            return new long[0];
        }

        // Find first grid line at or after startTime
        long firstLevel = ((startTime / interval) + 1) * interval;

        // Count levels with safety cap
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

    /**
     * Calculates a "nice" interval for grid lines.
     */
    private double calculateNiceInterval(double range, int targetLines) {
        double roughInterval = range / targetLines;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughInterval)));

        double normalized = roughInterval / magnitude;

        double niceNormalized;
        if (normalized <= 1.0) {
            niceNormalized = 1.0;
        } else if (normalized <= 2.0) {
            niceNormalized = 2.0;
        } else if (normalized <= 5.0) {
            niceNormalized = 5.0;
        } else {
            niceNormalized = 10.0;
        }

        return niceNormalized * magnitude;
    }

    /**
     * Calculates time interval for grid lines.
     */
    private long calculateTimeInterval(long duration, long barDuration) {
        // Handle edge case
        if (duration <= 0) {
            return 60000L;
        }

        // Target roughly 6-10 vertical grid lines
        long roughInterval = duration / 8;

        // Snap to nice time intervals (in milliseconds)
        long[] niceIntervals = {
                1000L,           // 1 second
                5000L,           // 5 seconds
                10000L,          // 10 seconds
                30000L,          // 30 seconds
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
                resources.disposeBuffer("grid.lines");
            }
            lineBuffer = null;
            shader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    public void setGridColor(Color color) {
        this.gridColor = color;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setLineWidth(float width) {
        this.lineWidth = width;
    }

    public float getLineWidth() {
        return lineWidth;
    }
}
