package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for histogram/volume data using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link HistogramLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Histograms are rendered as vertical bars from a baseline to data values.
 * Bars are colored based on whether the value is positive (bullish), negative (bearish),
 * or zero (neutral).
 */
public class HistogramLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(HistogramLayerV2.class);

    /** Z-order for histogram layer (renders after background, before main data) */
    public static final int Z_ORDER = 150;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    // Vertices per bar (2 triangles = 6 vertices)
    private static final int VERTICES_PER_BAR = 6;

    // V2 API resources
    private Buffer barBuffer;
    private Shader defaultShader;
    private boolean v2Initialized = false;

    // Reusable array for vertex data (avoid allocation during render)
    private float[] barVertices;
    private int vertexCapacity;

    // Data
    private HistogramData data;

    // Baseline Y value in data coordinates (typically 0)
    private double baseline = 0.0;

    // Bar styling
    private Color positiveColor = new Color(38, 166, 91);   // Green
    private Color negativeColor = new Color(214, 69, 65);   // Red
    private Color neutralColor = new Color(100, 100, 100);  // Gray
    private float barWidthRatio = 0.8f;
    private float opacity = 1.0f;

    /**
     * Creates a histogram layer using V2 renderers.
     */
    public HistogramLayerV2() {
        super(Z_ORDER);
        // Pre-allocate vertex array
        vertexCapacity = 256;
        barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
    }

    /**
     * Creates a histogram layer with a custom z-order.
     *
     * @param zOrder the z-order for this layer
     */
    public HistogramLayerV2(int zOrder) {
        super(zOrder);
        vertexCapacity = 256;
        barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
    }

    /**
     * Sets the histogram data to render.
     * Repaints automatically.
     */
    public void setData(HistogramData data) {
        this.data = data;
        markDirty();
        requestRepaint();
    }

    /**
     * Returns the current data.
     */
    public HistogramData getData() {
        return data;
    }

    /**
     * Sets the color for positive values.
     */
    public void setPositiveColor(Color color) {
        this.positiveColor = color;
        markDirty();
    }

    /**
     * Sets the color for negative values.
     */
    public void setNegativeColor(Color color) {
        this.negativeColor = color;
        markDirty();
    }

    /**
     * Sets the color for zero values.
     */
    public void setNeutralColor(Color color) {
        this.neutralColor = color;
        markDirty();
    }

    /**
     * Sets the bar width ratio (0.0 to 1.0).
     */
    public void setBarWidthRatio(float ratio) {
        this.barWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio));
        markDirty();
    }

    /**
     * Sets the bar opacity (0.0 to 1.0).
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        markDirty();
    }

    /**
     * Sets the baseline Y value in data coordinates.
     * Bars are drawn from this baseline to their value.
     *
     * @param baseline the baseline value (default 0.0)
     */
    public void setBaseline(double baseline) {
        this.baseline = baseline;
        markDirty();
    }

    /**
     * Returns the current baseline value.
     */
    public double getBaseline() {
        return baseline;
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("HistogramLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for histogram bars (position + color)
        // Initial capacity for 256 bars
        int initialCapacity = 256 * VERTICES_PER_BAR * FLOATS_PER_VERTEX;
        barBuffer = resources.getOrCreateBuffer("histogram.bars",
                BufferDescriptor.positionColor2D(initialCapacity));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("HistogramLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("HistogramLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        if (data == null || data.size() == 0) {
            return;
        }

        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        renderHistogramBars(ctx);
    }

    private void renderHistogramBars(RenderContext ctx) {
        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinatesForData(data);

        // Get visible range
        int firstIdx = findFirstVisibleIndex(viewport);
        int lastIdx = findLastVisibleIndex(viewport);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        int visibleCount = lastIdx - firstIdx + 1;

        // Ensure capacity
        ensureCapacity(visibleCount);

        // Calculate bar width
        double barWidth = ctx.getBarWidth();
        double actualWidth = barWidth * barWidthRatio;
        double halfWidth = actualWidth / 2.0;

        // Build vertex data
        int floatCount = buildBarVertices(coords, firstIdx, lastIdx, halfWidth);

        if (floatCount == 0) {
            return;
        }

        // Upload and draw
        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        barBuffer.upload(barVertices, 0, floatCount);
        barBuffer.draw(DrawMode.TRIANGLES);

        defaultShader.unbind();
    }

    private int buildBarVertices(CoordinateSystem coords, int firstIdx, int lastIdx, double halfWidth) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] values = data.getValuesArray();

        // Calculate baseline screen Y
        float baselineY = (float) coords.yValueToScreenY(baseline);

        for (int i = firstIdx; i <= lastIdx; i++) {
            float value = values[i];
            if (value == 0) {
                continue; // Skip zero values
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float valueY = (float) coords.yValueToScreenY(value);

            Color color = value > 0 ? positiveColor : (value < 0 ? negativeColor : neutralColor);
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = opacity;

            float left = (float) (x - halfWidth);
            float right = (float) (x + halfWidth);

            // Determine top and bottom based on value sign
            float top, bottom;
            if (value > 0) {
                top = valueY;    // In screen coords, smaller Y is higher
                bottom = baselineY;
            } else {
                top = baselineY;
                bottom = valueY;
            }

            // Ensure minimum bar height for visibility
            if (Math.abs(top - bottom) < 1.0f) {
                continue; // Skip bars that are too small to render
            }

            // Triangle 1: top-left, bottom-left, bottom-right
            floatIndex = addVertex(floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(floatIndex, left, bottom, r, g, b, a);
            floatIndex = addVertex(floatIndex, right, bottom, r, g, b, a);

            // Triangle 2: top-left, bottom-right, top-right
            floatIndex = addVertex(floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(floatIndex, right, bottom, r, g, b, a);
            floatIndex = addVertex(floatIndex, right, top, r, g, b, a);
        }

        return floatIndex;
    }

    private int addVertex(int index, float x, float y, float r, float g, float b, float a) {
        barVertices[index++] = x;
        barVertices[index++] = y;
        barVertices[index++] = r;
        barVertices[index++] = g;
        barVertices[index++] = b;
        barVertices[index++] = a;
        return index;
    }

    private int findFirstVisibleIndex(Viewport viewport) {
        long startTime = viewport.getStartTime();
        return data.indexAtOrAfter(startTime);
    }

    private int findLastVisibleIndex(Viewport viewport) {
        long endTime = viewport.getEndTime();
        return data.indexAtOrBefore(endTime);
    }

    private void ensureCapacity(int barCount) {
        int requiredFloats = barCount * VERTICES_PER_BAR * FLOATS_PER_VERTEX;

        if (requiredFloats > barVertices.length) {
            vertexCapacity = barCount + barCount / 2;
            barVertices = new float[vertexCapacity * VERTICES_PER_BAR * FLOATS_PER_VERTEX];
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
                resources.disposeBuffer("histogram.bars");
            }
            barBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }
}
