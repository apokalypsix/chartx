package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.ScatterSeriesOptions;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for series overlays using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link OverlayLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Supports rendering of:
 * <ul>
 *   <li>Line overlays with LINE display mode - connected line segments</li>
 *   <li>Line overlays with AREA display mode - filled area from line to baseline</li>
 *   <li>Line overlays with STEP modes - step lines (before/after/middle)</li>
 *   <li>Scatter overlays - point markers</li>
 * </ul>
 */
public class OverlayLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(OverlayLayerV2.class);

    /** Z-order for the overlay layer (renders after data) */
    public static final int Z_ORDER = 300;

    /**
     * Holds XyData + LineSeriesOptions for line overlays.
     */
    public static class LineOverlay {
        public final XyData data;
        public final LineSeriesOptions options;

        public LineOverlay(XyData data, LineSeriesOptions options) {
            this.data = data;
            this.options = options != null ? options : new LineSeriesOptions();
        }
    }

    /**
     * Holds XyData + ScatterSeriesOptions for scatter overlays.
     */
    public static class ScatterOverlay {
        public final XyData data;
        public final ScatterSeriesOptions options;

        public ScatterOverlay(XyData data, ScatterSeriesOptions options) {
            this.data = data;
            this.options = options != null ? options : new ScatterSeriesOptions();
        }
    }

    private final List<LineOverlay> lineOverlays = new ArrayList<>();
    private final List<ScatterOverlay> scatterOverlays = new ArrayList<>();

    // V2 API resources
    private Buffer lineBuffer;
    private Buffer areaBuffer;
    private Buffer scatterBuffer;
    private Shader defaultShader;
    private boolean v2Initialized = false;

    // Vertex data arrays (reused across frames to minimize allocation)
    private float[] lineVertices;
    private float[] areaVertices;
    private float[] scatterVertices;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;

    // Initial capacities
    private static final int INITIAL_LINE_CAPACITY = 4096;
    private static final int INITIAL_AREA_CAPACITY = 8192;
    private static final int INITIAL_SCATTER_CAPACITY = 2048;

    // Scatter marker geometry (triangles per marker)
    private static final int TRIANGLES_PER_MARKER = 8;
    private static final int VERTICES_PER_MARKER = TRIANGLES_PER_MARKER * 3;

    public OverlayLayerV2() {
        super(Z_ORDER);
        lineVertices = new float[INITIAL_LINE_CAPACITY * FLOATS_PER_VERTEX];
        areaVertices = new float[INITIAL_AREA_CAPACITY * FLOATS_PER_VERTEX];
        scatterVertices = new float[INITIAL_SCATTER_CAPACITY * FLOATS_PER_VERTEX];
    }

    // ========== Overlay Management ==========

    /**
     * Adds a line overlay with data and options.
     */
    public void addLineOverlay(XyData data, LineSeriesOptions options) {
        lineOverlays.add(new LineOverlay(data, options));
        markDirty();
        requestRepaint();
    }

    /**
     * Batch add multiple line overlays (single repaint at end).
     */
    public void addAllLineOverlays(List<LineOverlay> overlays) {
        lineOverlays.addAll(overlays);
        markDirty();
        requestRepaint();
    }

    /**
     * Adds a scatter overlay with data and options.
     */
    public void addScatterOverlay(XyData data, ScatterSeriesOptions options) {
        scatterOverlays.add(new ScatterOverlay(data, options));
        markDirty();
        requestRepaint();
    }

    /**
     * Batch add multiple scatter overlays (single repaint at end).
     */
    public void addAllScatterOverlays(List<ScatterOverlay> overlays) {
        scatterOverlays.addAll(overlays);
        markDirty();
        requestRepaint();
    }

    /**
     * Removes a line overlay by data reference.
     */
    public void removeLineOverlay(XyData data) {
        if (lineOverlays.removeIf(overlay -> overlay.data == data)) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a scatter overlay by data reference.
     */
    public void removeScatterOverlay(XyData data) {
        if (scatterOverlays.removeIf(overlay -> overlay.data == data)) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes all overlays.
     */
    public void clearOverlays() {
        if (!lineOverlays.isEmpty() || !scatterOverlays.isEmpty()) {
            lineOverlays.clear();
            scatterOverlays.clear();
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Returns all line overlays.
     */
    public List<LineOverlay> getLineOverlays() {
        return new ArrayList<>(lineOverlays);
    }

    /**
     * Returns all scatter overlays.
     */
    public List<ScatterOverlay> getScatterOverlays() {
        return new ArrayList<>(scatterOverlays);
    }

    // ========== Initialization ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("OverlayLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffers with position + color (for per-vertex colors)
        lineBuffer = resources.getOrCreateBuffer("overlay.lines",
                BufferDescriptor.positionColor2D(INITIAL_LINE_CAPACITY * FLOATS_PER_VERTEX));

        areaBuffer = resources.getOrCreateBuffer("overlay.areas",
                BufferDescriptor.positionColor2D(INITIAL_AREA_CAPACITY * FLOATS_PER_VERTEX));

        scatterBuffer = resources.getOrCreateBuffer("overlay.scatter",
                BufferDescriptor.positionColor2D(INITIAL_SCATTER_CAPACITY * FLOATS_PER_VERTEX));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("OverlayLayerV2 V2 resources initialized");
    }

    // ========== Rendering ==========

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("OverlayLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        // Render line series overlays
        renderLineOverlays(ctx);

        // Render scatter series overlays
        renderScatterOverlays(ctx);
    }

    /**
     * Renders all visible line overlays.
     */
    private void renderLineOverlays(RenderContext ctx) {
        for (LineOverlay overlay : lineOverlays) {
            if (!overlay.options.isVisible() || overlay.data.size() == 0) {
                continue;
            }

            LineSeriesOptions.DisplayMode mode = overlay.options.getDisplayMode();
            switch (mode) {
                case LINE:
                    renderLineData(ctx, overlay);
                    break;
                case AREA:
                    renderAreaData(ctx, overlay);
                    break;
                case STEP_BEFORE:
                case STEP_AFTER:
                case STEP_MIDDLE:
                    renderStepData(ctx, overlay, mode);
                    break;
            }
        }
    }

    /**
     * Renders a line overlay as connected line segments.
     */
    private void renderLineData(RenderContext ctx, LineOverlay overlay) {
        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinatesForData(overlay.data);

        // Get visible range
        int startIdx = overlay.data.indexAtOrAfter(viewport.getStartTime());
        int endIdx = overlay.data.indexAtOrBefore(viewport.getEndTime());

        if (startIdx < 0 || endIdx < 0 || startIdx > endIdx) {
            return;
        }

        // Include one point before visible range for continuous lines
        if (startIdx > 0) {
            startIdx--;
        }

        long[] timestamps = overlay.data.getTimestampsArray();
        float[] values = overlay.data.getValuesArray();
        Color color = overlay.options.getColor();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = overlay.options.getOpacity();

        // Ensure capacity for line vertices (2 vertices per segment)
        int maxSegments = endIdx - startIdx;
        ensureLineCapacity(maxSegments * 2);

        int floatIndex = 0;
        float prevX = Float.NaN;
        float prevY = Float.NaN;
        boolean hasPrevious = false;

        for (int i = startIdx; i <= endIdx; i++) {
            float value = values[i];

            // Skip NaN values
            if (Float.isNaN(value)) {
                hasPrevious = false;
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(value);

            if (hasPrevious) {
                // Add line segment from previous to current
                floatIndex = addColoredVertex(lineVertices, floatIndex, prevX, prevY, r, g, b, a);
                floatIndex = addColoredVertex(lineVertices, floatIndex, x, y, r, g, b, a);
            }

            prevX = x;
            prevY = y;
            hasPrevious = true;
        }

        if (floatIndex == 0) {
            return;
        }

        // Set line width
        RenderDevice device = ctx.getDevice();
        device.setLineWidth(overlay.options.getLineWidth());

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        lineBuffer.upload(lineVertices, 0, floatIndex);
        lineBuffer.draw(DrawMode.LINES);

        defaultShader.unbind();
    }

    /**
     * Renders a line overlay as filled area from line to baseline.
     */
    private void renderAreaData(RenderContext ctx, LineOverlay overlay) {
        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinatesForData(overlay.data);

        // Get visible range
        int startIdx = overlay.data.indexAtOrAfter(viewport.getStartTime());
        int endIdx = overlay.data.indexAtOrBefore(viewport.getEndTime());

        if (startIdx < 0 || endIdx < 0 || startIdx > endIdx) {
            return;
        }

        // Include one point before visible range for continuous areas
        if (startIdx > 0) {
            startIdx--;
        }

        long[] timestamps = overlay.data.getTimestampsArray();
        float[] values = overlay.data.getValuesArray();

        // Fill color (typically semi-transparent)
        Color fillColor = overlay.options.getFillColor();
        float fr = fillColor.getRed() / 255f;
        float fg = fillColor.getGreen() / 255f;
        float fb = fillColor.getBlue() / 255f;
        float fa = fillColor.getAlpha() / 255f;

        // Line color
        Color lineColor = overlay.options.getColor();
        float lr = lineColor.getRed() / 255f;
        float lg = lineColor.getGreen() / 255f;
        float lb = lineColor.getBlue() / 255f;
        float la = overlay.options.getOpacity();

        // Baseline Y position
        float baselineY = (float) coords.yValueToScreenY(overlay.options.getBaseline());

        // Ensure capacity for area triangles (2 triangles per segment = 6 vertices)
        int maxSegments = endIdx - startIdx;
        ensureAreaCapacity(maxSegments * 6);

        int areaFloatIndex = 0;

        float prevX = Float.NaN;
        float prevY = Float.NaN;
        boolean hasPrevious = false;

        for (int i = startIdx; i <= endIdx; i++) {
            float value = values[i];

            // Skip NaN values
            if (Float.isNaN(value)) {
                hasPrevious = false;
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(value);

            if (hasPrevious) {
                // Create quad as two triangles for filled area
                // Triangle 1: prevX,prevY -> prevX,baseline -> x,baseline
                areaFloatIndex = addColoredVertex(areaVertices, areaFloatIndex, prevX, prevY, fr, fg, fb, fa);
                areaFloatIndex = addColoredVertex(areaVertices, areaFloatIndex, prevX, baselineY, fr, fg, fb, fa);
                areaFloatIndex = addColoredVertex(areaVertices, areaFloatIndex, x, baselineY, fr, fg, fb, fa);

                // Triangle 2: prevX,prevY -> x,baseline -> x,y
                areaFloatIndex = addColoredVertex(areaVertices, areaFloatIndex, prevX, prevY, fr, fg, fb, fa);
                areaFloatIndex = addColoredVertex(areaVertices, areaFloatIndex, x, baselineY, fr, fg, fb, fa);
                areaFloatIndex = addColoredVertex(areaVertices, areaFloatIndex, x, y, fr, fg, fb, fa);
            }

            prevX = x;
            prevY = y;
            hasPrevious = true;
        }

        // Draw filled area
        if (areaFloatIndex > 0) {
            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            areaBuffer.upload(areaVertices, 0, areaFloatIndex);
            areaBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
        }

        // Draw line on top of area if enabled
        if (overlay.options.isShowLine()) {
            renderLineDataInternal(ctx, overlay, coords, timestamps, values,
                    startIdx, endIdx, lr, lg, lb, la);
        }
    }

    /**
     * Internal helper to render line on top of area fill.
     */
    private void renderLineDataInternal(RenderContext ctx, LineOverlay overlay,
                                         CoordinateSystem coords, long[] timestamps, float[] values,
                                         int startIdx, int endIdx,
                                         float r, float g, float b, float a) {
        int maxSegments = endIdx - startIdx;
        ensureLineCapacity(maxSegments * 2);

        int floatIndex = 0;
        float prevX = Float.NaN;
        float prevY = Float.NaN;
        boolean hasPrevious = false;

        for (int i = startIdx; i <= endIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                hasPrevious = false;
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(value);

            if (hasPrevious) {
                floatIndex = addColoredVertex(lineVertices, floatIndex, prevX, prevY, r, g, b, a);
                floatIndex = addColoredVertex(lineVertices, floatIndex, x, y, r, g, b, a);
            }

            prevX = x;
            prevY = y;
            hasPrevious = true;
        }

        if (floatIndex == 0) {
            return;
        }

        RenderDevice device = ctx.getDevice();
        device.setLineWidth(overlay.options.getLineWidth());

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        lineBuffer.upload(lineVertices, 0, floatIndex);
        lineBuffer.draw(DrawMode.LINES);

        defaultShader.unbind();
    }

    /**
     * Renders a line overlay as step lines.
     */
    private void renderStepData(RenderContext ctx, LineOverlay overlay, LineSeriesOptions.DisplayMode mode) {
        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinatesForData(overlay.data);

        // Get visible range
        int startIdx = overlay.data.indexAtOrAfter(viewport.getStartTime());
        int endIdx = overlay.data.indexAtOrBefore(viewport.getEndTime());

        if (startIdx < 0 || endIdx < 0 || startIdx > endIdx) {
            return;
        }

        if (startIdx > 0) {
            startIdx--;
        }

        long[] timestamps = overlay.data.getTimestampsArray();
        float[] values = overlay.data.getValuesArray();
        Color color = overlay.options.getColor();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = overlay.options.getOpacity();

        // Step lines need 2 segments per data point (horizontal + vertical)
        int maxSegments = (endIdx - startIdx) * 2;
        ensureLineCapacity(maxSegments * 2);

        int floatIndex = 0;
        float prevX = Float.NaN;
        float prevY = Float.NaN;
        boolean hasPrevious = false;

        for (int i = startIdx; i <= endIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                hasPrevious = false;
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(value);

            if (hasPrevious) {
                floatIndex = addStepSegments(lineVertices, floatIndex,
                        prevX, prevY, x, y, mode, r, g, b, a);
            }

            prevX = x;
            prevY = y;
            hasPrevious = true;
        }

        if (floatIndex == 0) {
            return;
        }

        RenderDevice device = ctx.getDevice();
        device.setLineWidth(overlay.options.getLineWidth());

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        lineBuffer.upload(lineVertices, 0, floatIndex);
        lineBuffer.draw(DrawMode.LINES);

        defaultShader.unbind();
    }

    /**
     * Adds step line segments between two points.
     */
    private int addStepSegments(float[] vertices, int index,
                                float x1, float y1, float x2, float y2,
                                LineSeriesOptions.DisplayMode mode,
                                float r, float g, float b, float a) {
        float midX;

        switch (mode) {
            case STEP_BEFORE:
                // Vertical first, then horizontal
                index = addColoredVertex(vertices, index, x1, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, x1, y2, r, g, b, a);
                index = addColoredVertex(vertices, index, x1, y2, r, g, b, a);
                index = addColoredVertex(vertices, index, x2, y2, r, g, b, a);
                break;

            case STEP_AFTER:
                // Horizontal first, then vertical
                index = addColoredVertex(vertices, index, x1, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, x2, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, x2, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, x2, y2, r, g, b, a);
                break;

            case STEP_MIDDLE:
                // Meet at midpoint
                midX = (x1 + x2) / 2;
                index = addColoredVertex(vertices, index, x1, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, midX, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, midX, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, midX, y2, r, g, b, a);
                index = addColoredVertex(vertices, index, midX, y2, r, g, b, a);
                index = addColoredVertex(vertices, index, x2, y2, r, g, b, a);
                break;

            default:
                // LINE mode fallback - straight line
                index = addColoredVertex(vertices, index, x1, y1, r, g, b, a);
                index = addColoredVertex(vertices, index, x2, y2, r, g, b, a);
                break;
        }

        return index;
    }

    /**
     * Renders all visible scatter overlays.
     */
    private void renderScatterOverlays(RenderContext ctx) {
        for (ScatterOverlay overlay : scatterOverlays) {
            if (!overlay.options.isVisible() || overlay.data.size() == 0) {
                continue;
            }
            renderScatterData(ctx, overlay);
        }
    }

    /**
     * Renders a scatter overlay as point markers.
     */
    private void renderScatterData(RenderContext ctx, ScatterOverlay overlay) {
        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinatesForData(overlay.data);

        // Get visible range
        int startIdx = overlay.data.indexAtOrAfter(viewport.getStartTime());
        int endIdx = overlay.data.indexAtOrBefore(viewport.getEndTime());

        if (startIdx < 0 || endIdx < 0 || startIdx > endIdx) {
            return;
        }

        long[] timestamps = overlay.data.getTimestampsArray();
        float[] values = overlay.data.getValuesArray();
        Color color = overlay.options.getColor();
        float size = overlay.options.getMarkerSize();
        ScatterSeriesOptions.MarkerShape shape = overlay.options.getMarkerShape();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = overlay.options.getOpacity();

        // Ensure capacity for scatter marker triangles
        int pointCount = endIdx - startIdx + 1;
        ensureScatterCapacity(pointCount * VERTICES_PER_MARKER);

        int floatIndex = 0;

        for (int i = startIdx; i <= endIdx; i++) {
            float value = values[i];

            if (Float.isNaN(value)) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(value);

            floatIndex = addMarkerGeometry(scatterVertices, floatIndex, x, y, size, shape, r, g, b, a);
        }

        if (floatIndex == 0) {
            return;
        }

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        scatterBuffer.upload(scatterVertices, 0, floatIndex);
        scatterBuffer.draw(DrawMode.TRIANGLES);

        defaultShader.unbind();
    }

    /**
     * Adds triangles for a marker at the specified position.
     */
    private int addMarkerGeometry(float[] vertices, int index, float cx, float cy,
                                  float size, ScatterSeriesOptions.MarkerShape shape,
                                  float r, float g, float b, float a) {
        float halfSize = size / 2;

        switch (shape) {
            case CIRCLE:
                // Approximate circle with 8 triangles (fan from center)
                return addCircleMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            case SQUARE:
                // Two triangles for square
                return addSquareMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            case DIAMOND:
                // Two triangles for diamond (rotated square)
                return addDiamondMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            case TRIANGLE_UP:
                return addTriangleUpMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            case TRIANGLE_DOWN:
                return addTriangleDownMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            case CROSS:
                return addCrossMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            case PLUS:
                return addPlusMarker(vertices, index, cx, cy, halfSize, r, g, b, a);

            default:
                return addCircleMarker(vertices, index, cx, cy, halfSize, r, g, b, a);
        }
    }

    private int addCircleMarker(float[] vertices, int index, float cx, float cy,
                                float radius, float r, float g, float b, float a) {
        // 8-segment circle approximation
        int segments = 8;
        double angleStep = 2 * Math.PI / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;

            float x1 = cx + (float) (radius * Math.cos(angle1));
            float y1 = cy + (float) (radius * Math.sin(angle1));
            float x2 = cx + (float) (radius * Math.cos(angle2));
            float y2 = cy + (float) (radius * Math.sin(angle2));

            // Triangle from center to edge
            index = addColoredVertex(vertices, index, cx, cy, r, g, b, a);
            index = addColoredVertex(vertices, index, x1, y1, r, g, b, a);
            index = addColoredVertex(vertices, index, x2, y2, r, g, b, a);
        }

        return index;
    }

    private int addSquareMarker(float[] vertices, int index, float cx, float cy,
                                float halfSize, float r, float g, float b, float a) {
        float left = cx - halfSize;
        float right = cx + halfSize;
        float top = cy - halfSize;
        float bottom = cy + halfSize;

        // Triangle 1
        index = addColoredVertex(vertices, index, left, top, r, g, b, a);
        index = addColoredVertex(vertices, index, left, bottom, r, g, b, a);
        index = addColoredVertex(vertices, index, right, bottom, r, g, b, a);

        // Triangle 2
        index = addColoredVertex(vertices, index, left, top, r, g, b, a);
        index = addColoredVertex(vertices, index, right, bottom, r, g, b, a);
        index = addColoredVertex(vertices, index, right, top, r, g, b, a);

        return index;
    }

    private int addDiamondMarker(float[] vertices, int index, float cx, float cy,
                                 float halfSize, float r, float g, float b, float a) {
        // Diamond points
        float top = cy - halfSize;
        float bottom = cy + halfSize;
        float left = cx - halfSize;
        float right = cx + halfSize;

        // Upper triangle
        index = addColoredVertex(vertices, index, cx, top, r, g, b, a);
        index = addColoredVertex(vertices, index, left, cy, r, g, b, a);
        index = addColoredVertex(vertices, index, right, cy, r, g, b, a);

        // Lower triangle
        index = addColoredVertex(vertices, index, left, cy, r, g, b, a);
        index = addColoredVertex(vertices, index, cx, bottom, r, g, b, a);
        index = addColoredVertex(vertices, index, right, cy, r, g, b, a);

        return index;
    }

    private int addTriangleUpMarker(float[] vertices, int index, float cx, float cy,
                                    float halfSize, float r, float g, float b, float a) {
        // Equilateral triangle pointing up
        float top = cy - halfSize;
        float bottom = cy + halfSize * 0.5f;
        float left = cx - halfSize;
        float right = cx + halfSize;

        index = addColoredVertex(vertices, index, cx, top, r, g, b, a);
        index = addColoredVertex(vertices, index, left, bottom, r, g, b, a);
        index = addColoredVertex(vertices, index, right, bottom, r, g, b, a);

        return index;
    }

    private int addTriangleDownMarker(float[] vertices, int index, float cx, float cy,
                                      float halfSize, float r, float g, float b, float a) {
        // Equilateral triangle pointing down
        float top = cy - halfSize * 0.5f;
        float bottom = cy + halfSize;
        float left = cx - halfSize;
        float right = cx + halfSize;

        index = addColoredVertex(vertices, index, left, top, r, g, b, a);
        index = addColoredVertex(vertices, index, right, top, r, g, b, a);
        index = addColoredVertex(vertices, index, cx, bottom, r, g, b, a);

        return index;
    }

    private int addCrossMarker(float[] vertices, int index, float cx, float cy,
                               float halfSize, float r, float g, float b, float a) {
        // X shape made of 4 thin triangles
        float thickness = halfSize * 0.3f;

        // Diagonal 1 (top-left to bottom-right)
        index = addColoredVertex(vertices, index, cx - halfSize, cy - halfSize + thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx - halfSize + thickness, cy - halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize, cy + halfSize - thickness, r, g, b, a);

        index = addColoredVertex(vertices, index, cx - halfSize, cy - halfSize + thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize, cy + halfSize - thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize - thickness, cy + halfSize, r, g, b, a);

        // Diagonal 2 (top-right to bottom-left)
        index = addColoredVertex(vertices, index, cx + halfSize - thickness, cy - halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize, cy - halfSize + thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx - halfSize, cy + halfSize - thickness, r, g, b, a);

        index = addColoredVertex(vertices, index, cx + halfSize - thickness, cy - halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx - halfSize, cy + halfSize - thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx - halfSize + thickness, cy + halfSize, r, g, b, a);

        return index;
    }

    private int addPlusMarker(float[] vertices, int index, float cx, float cy,
                              float halfSize, float r, float g, float b, float a) {
        // Plus shape made of 2 rectangles (4 triangles each)
        float thickness = halfSize * 0.35f;

        // Horizontal bar
        index = addColoredVertex(vertices, index, cx - halfSize, cy - thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx - halfSize, cy + thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize, cy + thickness, r, g, b, a);

        index = addColoredVertex(vertices, index, cx - halfSize, cy - thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize, cy + thickness, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + halfSize, cy - thickness, r, g, b, a);

        // Vertical bar
        index = addColoredVertex(vertices, index, cx - thickness, cy - halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx - thickness, cy + halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + thickness, cy + halfSize, r, g, b, a);

        index = addColoredVertex(vertices, index, cx - thickness, cy - halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + thickness, cy + halfSize, r, g, b, a);
        index = addColoredVertex(vertices, index, cx + thickness, cy - halfSize, r, g, b, a);

        return index;
    }

    // ========== Vertex Helpers ==========

    /**
     * Adds a colored vertex to the vertex array.
     */
    private int addColoredVertex(float[] vertices, int index,
                                 float x, float y,
                                 float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }

    // ========== Capacity Management ==========

    private void ensureLineCapacity(int vertexCount) {
        int requiredFloats = vertexCount * FLOATS_PER_VERTEX;
        if (requiredFloats > lineVertices.length) {
            lineVertices = new float[requiredFloats + requiredFloats / 2];
        }
    }

    private void ensureAreaCapacity(int vertexCount) {
        int requiredFloats = vertexCount * FLOATS_PER_VERTEX;
        if (requiredFloats > areaVertices.length) {
            areaVertices = new float[requiredFloats + requiredFloats / 2];
        }
    }

    private void ensureScatterCapacity(int vertexCount) {
        int requiredFloats = vertexCount * FLOATS_PER_VERTEX;
        if (requiredFloats > scatterVertices.length) {
            scatterVertices = new float[requiredFloats + requiredFloats / 2];
        }
    }

    // ========== Disposal ==========

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
                resources.disposeBuffer("overlay.lines");
                resources.disposeBuffer("overlay.areas");
                resources.disposeBuffer("overlay.scatter");
            }
            lineBuffer = null;
            areaBuffer = null;
            scatterBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }
}
