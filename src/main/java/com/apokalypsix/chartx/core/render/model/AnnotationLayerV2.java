package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.overlay.Annotation;
import com.apokalypsix.chartx.chart.overlay.BoxAnnotation;
import com.apokalypsix.chartx.chart.overlay.MarkerAnnotation;
import com.apokalypsix.chartx.chart.overlay.MarkerAnnotation.MarkerShape;
import com.apokalypsix.chartx.chart.overlay.TextAnnotation;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.TextRenderer;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for chart annotations using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link AnnotationLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>This layer renders marker annotations (shapes like circles, squares, triangles,
 * arrows, etc.) and text annotations at specific chart locations using GPU rendering.
 */
public class AnnotationLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(AnnotationLayerV2.class);

    /** Z-order for annotation layer (renders on top of data) */
    public static final int Z_ORDER = 700;

    private final List<Annotation> annotations = new ArrayList<>();

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    // Maximum vertices per marker (for complex shapes like stars)
    private static final int MAX_VERTICES_PER_MARKER = 24;
    // Vertices per box fill (2 triangles = 6 vertices)
    private static final int BOX_FILL_VERTICES = 6;
    // Vertices per box border (4 lines = 8 vertices)
    private static final int BOX_BORDER_VERTICES = 8;

    // V2 API resources
    private Buffer markerBuffer;
    private Buffer boxFillBuffer;
    private Buffer boxBorderBuffer;
    private Shader defaultShader;
    private boolean v2Initialized = false;

    // Vertex data array (reused to avoid allocations)
    private float[] markerVertices;
    private float[] boxFillVertices;
    private float[] boxBorderVertices;
    private int vertexCapacity;
    private int boxCapacity;

    public AnnotationLayerV2() {
        super(Z_ORDER);
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("AnnotationLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for markers (position + color)
        int initialCapacity = 64 * MAX_VERTICES_PER_MARKER * FLOATS_PER_VERTEX;
        markerBuffer = resources.getOrCreateBuffer("annotation.markers",
                BufferDescriptor.positionColor2D(initialCapacity));

        // Create buffers for box annotations
        boxFillBuffer = resources.getOrCreateBuffer("annotation.boxFill",
                BufferDescriptor.positionColor2D(64 * BOX_FILL_VERTICES * FLOATS_PER_VERTEX));
        boxBorderBuffer = resources.getOrCreateBuffer("annotation.boxBorder",
                BufferDescriptor.positionColor2D(64 * BOX_BORDER_VERTICES * FLOATS_PER_VERTEX));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        // Initialize vertex arrays
        vertexCapacity = 64;
        markerVertices = new float[vertexCapacity * MAX_VERTICES_PER_MARKER * FLOATS_PER_VERTEX];

        boxCapacity = 32;
        boxFillVertices = new float[boxCapacity * BOX_FILL_VERTICES * FLOATS_PER_VERTEX];
        boxBorderVertices = new float[boxCapacity * BOX_BORDER_VERTICES * FLOATS_PER_VERTEX];

        v2Initialized = true;
        log.debug("AnnotationLayerV2 V2 resources initialized");
    }

    // ========== Annotation management ==========

    /**
     * Adds an annotation to this layer.
     */
    public void addAnnotation(Annotation annotation) {
        if (!annotations.contains(annotation)) {
            annotations.add(annotation);
            annotations.sort(Comparator.comparingInt(Annotation::getZOrder));
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Batch add multiple annotations (single repaint at end).
     */
    public void addAllAnnotations(List<? extends Annotation> annotationList) {
        boolean added = false;
        for (Annotation annotation : annotationList) {
            if (!annotations.contains(annotation)) {
                annotations.add(annotation);
                added = true;
            }
        }
        if (added) {
            annotations.sort(Comparator.comparingInt(Annotation::getZOrder));
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes an annotation from this layer.
     */
    public void removeAnnotation(Annotation annotation) {
        if (annotations.remove(annotation)) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes an annotation by ID.
     */
    public void removeAnnotation(String id) {
        if (annotations.removeIf(a -> a.getId().equals(id))) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Clears all annotations.
     */
    public void clearAnnotations() {
        if (!annotations.isEmpty()) {
            annotations.clear();
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Returns all annotations.
     */
    public List<Annotation> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    /**
     * Returns only text annotations (for TextOverlay to render).
     */
    public List<TextAnnotation> getTextAnnotations() {
        List<TextAnnotation> textAnnotations = new ArrayList<>();
        for (Annotation a : annotations) {
            if (a instanceof TextAnnotation && a.isVisible()) {
                textAnnotations.add((TextAnnotation) a);
            }
        }
        return textAnnotations;
    }

    /**
     * Returns the annotation at the specified screen position, if any.
     */
    public Annotation getAnnotationAt(int screenX, int screenY, CoordinateSystem coords) {
        // Search in reverse z-order (top to bottom)
        for (int i = annotations.size() - 1; i >= 0; i--) {
            Annotation a = annotations.get(i);
            if (!a.isVisible()) continue;

            float ax = (float) coords.xValueToScreenX(a.getTimestamp()) + a.getOffsetX();
            float ay = (float) coords.yValueToScreenY(a.getPrice()) + a.getOffsetY();

            int hitRadius = (a instanceof MarkerAnnotation)
                    ? ((MarkerAnnotation) a).getSize() + 2
                    : 10;

            if (Math.abs(screenX - ax) <= hitRadius && Math.abs(screenY - ay) <= hitRadius) {
                return a;
            }
        }
        return null;
    }

    // ========== Rendering ==========

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("AnnotationLayerV2 requires abstracted API - skipping render");
            return;
        }

        if (annotations.isEmpty()) {
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinates();

        // Collect visible annotations by type
        List<MarkerAnnotation> markers = new ArrayList<>();
        List<BoxAnnotation> boxes = new ArrayList<>();
        List<TextAnnotation> texts = new ArrayList<>();

        for (Annotation a : annotations) {
            if (!a.isVisible()) {
                continue;
            }

            if (a instanceof BoxAnnotation box) {
                // Check if box overlaps visible time range
                if (box.getEndTime() >= viewport.getStartTime() && box.getStartTime() <= viewport.getEndTime()) {
                    boxes.add(box);
                }
            } else if (a instanceof MarkerAnnotation marker) {
                // Check if in visible time range
                long time = marker.getTimestamp();
                if (time >= viewport.getStartTime() && time <= viewport.getEndTime()) {
                    markers.add(marker);
                }
            } else if (a instanceof TextAnnotation text) {
                // Check if in visible time range
                long time = text.getTimestamp();
                if (time >= viewport.getStartTime() && time <= viewport.getEndTime()) {
                    texts.add(text);
                }
            }
        }

        if (markers.isEmpty() && boxes.isEmpty() && texts.isEmpty()) {
            return;
        }

        // Get shader
        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        defaultShader.bind();
        defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Render box annotations first (they're usually backgrounds)
        if (!boxes.isEmpty()) {
            renderBoxAnnotations(boxes, coords, viewport);
        }

        // Render marker annotations
        if (!markers.isEmpty()) {
            ensureMarkerCapacity(markers.size());
            int floatCount = buildMarkerVertices(markers, coords);
            if (floatCount > 0) {
                markerBuffer.upload(markerVertices, 0, floatCount);
                markerBuffer.draw(DrawMode.TRIANGLES);
            }
        }

        defaultShader.unbind();

        // Render text annotations using backend-agnostic TextRenderer
        if (!texts.isEmpty()) {
            renderTextAnnotations(texts, coords, viewport, ctx);
        }
    }

    /**
     * Renders text annotations using the GPU text renderer.
     */
    private void renderTextAnnotations(List<TextAnnotation> texts, CoordinateSystem coords,
                                        Viewport viewport, RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();
        TextRenderer textRenderer = resources.getTextRenderer();
        if (textRenderer == null) {
            return;
        }

        // Set scale factor for HiDPI displays
        textRenderer.setScaleFactor(ctx.getScaleFactor());

        if (!textRenderer.beginBatch(viewport.getWidth(), viewport.getHeight())) {
            return;
        }

        try {
            for (TextAnnotation text : texts) {
                String label = text.getText();
                if (label == null || label.isEmpty()) {
                    continue;
                }

                // Set font size if specified
                if (text.getFont() != null) {
                    textRenderer.setFontSize(text.getFont().getSize());
                }

                // Calculate screen position
                float screenX = (float) coords.xValueToScreenX(text.getTimestamp()) + text.getOffsetX();
                float screenY = (float) coords.yValueToScreenY(text.getPrice()) + text.getOffsetY();

                // Apply vertical alignment
                float fontSize = textRenderer.getFontSize();
                switch (text.getVAlign()) {
                    case TOP -> screenY += fontSize;
                    case MIDDLE -> screenY += fontSize / 2;
                    case BOTTOM -> {} // baseline is at y
                }

                Color color = text.getColor() != null ? text.getColor() : Color.WHITE;

                // Draw based on horizontal alignment
                switch (text.getHAlign()) {
                    case LEFT -> textRenderer.drawText(label, screenX, screenY, color);
                    case CENTER -> textRenderer.drawTextCentered(label, screenX, screenY, color);
                    case RIGHT -> textRenderer.drawTextRight(label, screenX, screenY, color);
                }
            }
        } finally {
            textRenderer.endBatch();
        }
    }

    /**
     * Renders box annotations (fills and borders).
     */
    private void renderBoxAnnotations(List<BoxAnnotation> boxes, CoordinateSystem coords, Viewport viewport) {
        ensureBoxCapacity(boxes.size());

        // Build fill vertices
        int fillFloatCount = 0;
        int borderFloatCount = 0;

        for (BoxAnnotation box : boxes) {
            // Calculate screen coordinates
            float left = (float) coords.xValueToScreenX(box.getStartTime());
            float right = (float) coords.xValueToScreenX(box.getEndTime());
            // Note: Y axis is inverted (higher price = lower screen Y)
            float top = (float) coords.yValueToScreenY(box.getHighPrice());
            float bottom = (float) coords.yValueToScreenY(box.getLowPrice());

            // Clamp to visible area
            left = Math.max(left, viewport.getLeftInset());
            right = Math.min(right, viewport.getWidth() - viewport.getRightInset());
            top = Math.max(top, viewport.getTopInset());
            bottom = Math.min(bottom, viewport.getHeight() - viewport.getBottomInset());

            if (left >= right || top >= bottom) {
                continue;
            }

            // Build fill
            Color fillColor = box.getFillColor();
            if (fillColor != null) {
                float r = fillColor.getRed() / 255f;
                float g = fillColor.getGreen() / 255f;
                float b = fillColor.getBlue() / 255f;
                float a = fillColor.getAlpha() / 255f * box.getOpacity();

                // Triangle 1: top-left, bottom-left, bottom-right
                fillFloatCount = addVertex(boxFillVertices, fillFloatCount, left, top, r, g, b, a);
                fillFloatCount = addVertex(boxFillVertices, fillFloatCount, left, bottom, r, g, b, a);
                fillFloatCount = addVertex(boxFillVertices, fillFloatCount, right, bottom, r, g, b, a);

                // Triangle 2: top-left, bottom-right, top-right
                fillFloatCount = addVertex(boxFillVertices, fillFloatCount, left, top, r, g, b, a);
                fillFloatCount = addVertex(boxFillVertices, fillFloatCount, right, bottom, r, g, b, a);
                fillFloatCount = addVertex(boxFillVertices, fillFloatCount, right, top, r, g, b, a);
            }

            // Build border
            Color borderColor = box.getBorderColor();
            if (borderColor != null) {
                float r = borderColor.getRed() / 255f;
                float g = borderColor.getGreen() / 255f;
                float b = borderColor.getBlue() / 255f;
                float a = borderColor.getAlpha() / 255f * box.getOpacity();

                // Top line
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, left, top, r, g, b, a);
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, right, top, r, g, b, a);

                // Bottom line
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, left, bottom, r, g, b, a);
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, right, bottom, r, g, b, a);

                // Left line
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, left, top, r, g, b, a);
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, left, bottom, r, g, b, a);

                // Right line
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, right, top, r, g, b, a);
                borderFloatCount = addVertex(boxBorderVertices, borderFloatCount, right, bottom, r, g, b, a);
            }
        }

        // Draw fills
        if (fillFloatCount > 0) {
            boxFillBuffer.upload(boxFillVertices, 0, fillFloatCount);
            boxFillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders
        if (borderFloatCount > 0) {
            boxBorderBuffer.upload(boxBorderVertices, 0, borderFloatCount);
            boxBorderBuffer.draw(DrawMode.LINES);
        }
    }

    /**
     * Returns all box annotations (for label rendering by TextOverlay).
     */
    public List<BoxAnnotation> getBoxAnnotations() {
        List<BoxAnnotation> boxAnnotations = new ArrayList<>();
        for (Annotation a : annotations) {
            if (a instanceof BoxAnnotation && a.isVisible()) {
                boxAnnotations.add((BoxAnnotation) a);
            }
        }
        return boxAnnotations;
    }

    private int buildMarkerVertices(List<MarkerAnnotation> markers, CoordinateSystem coords) {
        int floatIndex = 0;

        for (MarkerAnnotation marker : markers) {
            float cx = (float) coords.xValueToScreenX(marker.getTimestamp()) + marker.getOffsetX();
            float cy = (float) coords.yValueToScreenY(marker.getPrice()) + marker.getOffsetY();
            int size = marker.getSize();
            float halfSize = size / 2f;

            Color c = marker.getColor();
            float r = c.getRed() / 255f;
            float g = c.getGreen() / 255f;
            float b = c.getBlue() / 255f;
            float a = marker.getOpacity();

            floatIndex = buildShape(markerVertices, floatIndex, marker.getShape(),
                    cx, cy, halfSize, r, g, b, a, marker.isFilled());
        }

        return floatIndex;
    }

    private int buildShape(float[] vertices, int idx, MarkerShape shape,
                           float cx, float cy, float halfSize,
                           float r, float g, float b, float a, boolean filled) {
        switch (shape) {
            case CIRCLE:
                return buildCircle(vertices, idx, cx, cy, halfSize, r, g, b, a, 12);
            case SQUARE:
                return buildSquare(vertices, idx, cx, cy, halfSize, r, g, b, a);
            case DIAMOND:
                return buildDiamond(vertices, idx, cx, cy, halfSize, r, g, b, a);
            case TRIANGLE_UP:
                return buildTriangle(vertices, idx, cx, cy, halfSize, r, g, b, a, true);
            case TRIANGLE_DOWN:
                return buildTriangle(vertices, idx, cx, cy, halfSize, r, g, b, a, false);
            case ARROW_UP:
                return buildArrow(vertices, idx, cx, cy, halfSize, r, g, b, a, true);
            case ARROW_DOWN:
                return buildArrow(vertices, idx, cx, cy, halfSize, r, g, b, a, false);
            case CROSS:
                return buildCross(vertices, idx, cx, cy, halfSize, r, g, b, a);
            case X:
                return buildX(vertices, idx, cx, cy, halfSize, r, g, b, a);
            case STAR:
                return buildStar(vertices, idx, cx, cy, halfSize, r, g, b, a);
            case FLAG:
                return buildFlag(vertices, idx, cx, cy, halfSize, r, g, b, a);
            default:
                return buildCircle(vertices, idx, cx, cy, halfSize, r, g, b, a, 12);
        }
    }

    private int buildCircle(float[] v, int idx, float cx, float cy, float radius,
                            float r, float g, float b, float a, int segments) {
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;

            float x1 = cx + (float) (radius * Math.cos(angle1));
            float y1 = cy + (float) (radius * Math.sin(angle1));
            float x2 = cx + (float) (radius * Math.cos(angle2));
            float y2 = cy + (float) (radius * Math.sin(angle2));

            idx = addVertex(v, idx, cx, cy, r, g, b, a);
            idx = addVertex(v, idx, x1, y1, r, g, b, a);
            idx = addVertex(v, idx, x2, y2, r, g, b, a);
        }
        return idx;
    }

    private int buildSquare(float[] v, int idx, float cx, float cy, float half,
                            float r, float g, float b, float a) {
        // Two triangles
        idx = addVertex(v, idx, cx - half, cy - half, r, g, b, a);
        idx = addVertex(v, idx, cx - half, cy + half, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy + half, r, g, b, a);

        idx = addVertex(v, idx, cx - half, cy - half, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy + half, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy - half, r, g, b, a);
        return idx;
    }

    private int buildDiamond(float[] v, int idx, float cx, float cy, float half,
                             float r, float g, float b, float a) {
        // Four triangles from center
        idx = addVertex(v, idx, cx, cy, r, g, b, a);
        idx = addVertex(v, idx, cx, cy - half, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy, r, g, b, a);

        idx = addVertex(v, idx, cx, cy, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy, r, g, b, a);
        idx = addVertex(v, idx, cx, cy + half, r, g, b, a);

        idx = addVertex(v, idx, cx, cy, r, g, b, a);
        idx = addVertex(v, idx, cx, cy + half, r, g, b, a);
        idx = addVertex(v, idx, cx - half, cy, r, g, b, a);

        idx = addVertex(v, idx, cx, cy, r, g, b, a);
        idx = addVertex(v, idx, cx - half, cy, r, g, b, a);
        idx = addVertex(v, idx, cx, cy - half, r, g, b, a);
        return idx;
    }

    private int buildTriangle(float[] v, int idx, float cx, float cy, float half,
                              float r, float g, float b, float a, boolean up) {
        float tipY = up ? cy - half : cy + half;
        float baseY = up ? cy + half : cy - half;

        idx = addVertex(v, idx, cx, tipY, r, g, b, a);
        idx = addVertex(v, idx, cx - half, baseY, r, g, b, a);
        idx = addVertex(v, idx, cx + half, baseY, r, g, b, a);
        return idx;
    }

    private int buildArrow(float[] v, int idx, float cx, float cy, float half,
                           float r, float g, float b, float a, boolean up) {
        float tipY = up ? cy - half : cy + half;
        float midY = up ? cy - half * 0.3f : cy + half * 0.3f;
        float baseY = up ? cy + half : cy - half;
        float shaftHalf = half * 0.3f;

        // Arrow head (triangle)
        idx = addVertex(v, idx, cx, tipY, r, g, b, a);
        idx = addVertex(v, idx, cx - half, midY, r, g, b, a);
        idx = addVertex(v, idx, cx + half, midY, r, g, b, a);

        // Arrow shaft (rectangle as two triangles)
        idx = addVertex(v, idx, cx - shaftHalf, midY, r, g, b, a);
        idx = addVertex(v, idx, cx - shaftHalf, baseY, r, g, b, a);
        idx = addVertex(v, idx, cx + shaftHalf, baseY, r, g, b, a);

        idx = addVertex(v, idx, cx - shaftHalf, midY, r, g, b, a);
        idx = addVertex(v, idx, cx + shaftHalf, baseY, r, g, b, a);
        idx = addVertex(v, idx, cx + shaftHalf, midY, r, g, b, a);
        return idx;
    }

    private int buildCross(float[] v, int idx, float cx, float cy, float half,
                           float r, float g, float b, float a) {
        float thick = half * 0.3f;
        // Horizontal bar
        idx = addVertex(v, idx, cx - half, cy - thick, r, g, b, a);
        idx = addVertex(v, idx, cx - half, cy + thick, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy + thick, r, g, b, a);
        idx = addVertex(v, idx, cx - half, cy - thick, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy + thick, r, g, b, a);
        idx = addVertex(v, idx, cx + half, cy - thick, r, g, b, a);

        // Vertical bar
        idx = addVertex(v, idx, cx - thick, cy - half, r, g, b, a);
        idx = addVertex(v, idx, cx - thick, cy + half, r, g, b, a);
        idx = addVertex(v, idx, cx + thick, cy + half, r, g, b, a);
        idx = addVertex(v, idx, cx - thick, cy - half, r, g, b, a);
        idx = addVertex(v, idx, cx + thick, cy + half, r, g, b, a);
        idx = addVertex(v, idx, cx + thick, cy - half, r, g, b, a);
        return idx;
    }

    private int buildX(float[] v, int idx, float cx, float cy, float half,
                       float r, float g, float b, float a) {
        float thick = half * 0.25f;
        // Diagonal from top-left to bottom-right (as a rotated rectangle)
        float d = half * 0.707f; // cos(45) and sin(45)
        float t = thick * 0.707f;

        // First diagonal
        idx = addVertex(v, idx, cx - d - t, cy - d + t, r, g, b, a);
        idx = addVertex(v, idx, cx - d + t, cy - d - t, r, g, b, a);
        idx = addVertex(v, idx, cx + d + t, cy + d - t, r, g, b, a);
        idx = addVertex(v, idx, cx - d - t, cy - d + t, r, g, b, a);
        idx = addVertex(v, idx, cx + d + t, cy + d - t, r, g, b, a);
        idx = addVertex(v, idx, cx + d - t, cy + d + t, r, g, b, a);

        // Second diagonal
        idx = addVertex(v, idx, cx + d - t, cy - d - t, r, g, b, a);
        idx = addVertex(v, idx, cx + d + t, cy - d + t, r, g, b, a);
        idx = addVertex(v, idx, cx - d + t, cy + d + t, r, g, b, a);
        idx = addVertex(v, idx, cx + d - t, cy - d - t, r, g, b, a);
        idx = addVertex(v, idx, cx - d + t, cy + d + t, r, g, b, a);
        idx = addVertex(v, idx, cx - d - t, cy + d - t, r, g, b, a);
        return idx;
    }

    private int buildStar(float[] v, int idx, float cx, float cy, float half,
                          float r, float g, float b, float a) {
        int points = 5;
        float innerRadius = half * 0.4f;
        for (int i = 0; i < points; i++) {
            double angle1 = Math.PI / 2 + 2 * Math.PI * i / points;
            double angle2 = angle1 + Math.PI / points;
            double angle3 = angle1 + 2 * Math.PI / points;

            float x1 = cx + (float) (half * Math.cos(angle1));
            float y1 = cy - (float) (half * Math.sin(angle1));
            float x2 = cx + (float) (innerRadius * Math.cos(angle2));
            float y2 = cy - (float) (innerRadius * Math.sin(angle2));
            float x3 = cx + (float) (half * Math.cos(angle3));
            float y3 = cy - (float) (half * Math.sin(angle3));

            // Triangle from center to two outer points through inner point
            idx = addVertex(v, idx, cx, cy, r, g, b, a);
            idx = addVertex(v, idx, x1, y1, r, g, b, a);
            idx = addVertex(v, idx, x2, y2, r, g, b, a);

            idx = addVertex(v, idx, cx, cy, r, g, b, a);
            idx = addVertex(v, idx, x2, y2, r, g, b, a);
            idx = addVertex(v, idx, x3, y3, r, g, b, a);
        }
        return idx;
    }

    private int buildFlag(float[] v, int idx, float cx, float cy, float half,
                          float r, float g, float b, float a) {
        float poleWidth = half * 0.15f;
        float flagHeight = half * 0.7f;
        float flagWidth = half * 0.8f;

        // Pole (vertical line from anchor up)
        idx = addVertex(v, idx, cx - poleWidth, cy, r * 0.5f, g * 0.5f, b * 0.5f, a);
        idx = addVertex(v, idx, cx - poleWidth, cy - half * 1.5f, r * 0.5f, g * 0.5f, b * 0.5f, a);
        idx = addVertex(v, idx, cx + poleWidth, cy - half * 1.5f, r * 0.5f, g * 0.5f, b * 0.5f, a);
        idx = addVertex(v, idx, cx - poleWidth, cy, r * 0.5f, g * 0.5f, b * 0.5f, a);
        idx = addVertex(v, idx, cx + poleWidth, cy - half * 1.5f, r * 0.5f, g * 0.5f, b * 0.5f, a);
        idx = addVertex(v, idx, cx + poleWidth, cy, r * 0.5f, g * 0.5f, b * 0.5f, a);

        // Flag (triangular pennant)
        float flagTop = cy - half * 1.5f;
        idx = addVertex(v, idx, cx + poleWidth, flagTop, r, g, b, a);
        idx = addVertex(v, idx, cx + poleWidth, flagTop + flagHeight, r, g, b, a);
        idx = addVertex(v, idx, cx + poleWidth + flagWidth, flagTop + flagHeight * 0.5f, r, g, b, a);
        return idx;
    }

    private int addVertex(float[] v, int idx, float x, float y,
                          float r, float g, float b, float a) {
        v[idx++] = x;
        v[idx++] = y;
        v[idx++] = r;
        v[idx++] = g;
        v[idx++] = b;
        v[idx++] = a;
        return idx;
    }

    private void ensureMarkerCapacity(int markerCount) {
        int requiredFloats = markerCount * MAX_VERTICES_PER_MARKER * FLOATS_PER_VERTEX;
        if (markerVertices == null || requiredFloats > markerVertices.length) {
            vertexCapacity = markerCount + markerCount / 2;
            markerVertices = new float[vertexCapacity * MAX_VERTICES_PER_MARKER * FLOATS_PER_VERTEX];
        }
    }

    private void ensureBoxCapacity(int boxCount) {
        int requiredFillFloats = boxCount * BOX_FILL_VERTICES * FLOATS_PER_VERTEX;
        int requiredBorderFloats = boxCount * BOX_BORDER_VERTICES * FLOATS_PER_VERTEX;

        if (boxFillVertices == null || requiredFillFloats > boxFillVertices.length) {
            boxCapacity = boxCount + boxCount / 2;
            boxFillVertices = new float[boxCapacity * BOX_FILL_VERTICES * FLOATS_PER_VERTEX];
        }

        if (boxBorderVertices == null || requiredBorderFloats > boxBorderVertices.length) {
            boxCapacity = boxCount + boxCount / 2;
            boxBorderVertices = new float[boxCapacity * BOX_BORDER_VERTICES * FLOATS_PER_VERTEX];
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
                resources.disposeBuffer("annotation.markers");
                resources.disposeBuffer("annotation.boxFill");
                resources.disposeBuffer("annotation.boxBorder");
            }
            markerBuffer = null;
            boxFillBuffer = null;
            boxBorderBuffer = null;
            defaultShader = null;
            markerVertices = null;
            boxFillVertices = null;
            boxBorderVertices = null;
            v2Initialized = false;
        }
    }
}
