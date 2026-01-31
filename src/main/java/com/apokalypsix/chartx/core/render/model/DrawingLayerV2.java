package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.overlay.AnchorPoint;
import com.apokalypsix.chartx.chart.overlay.Arc;
import com.apokalypsix.chartx.chart.overlay.Arrow;
import com.apokalypsix.chartx.chart.overlay.Callout;
import com.apokalypsix.chartx.chart.overlay.Drawing;
import com.apokalypsix.chartx.chart.overlay.Ellipse;
import com.apokalypsix.chartx.chart.overlay.ExtendedLine;
import com.apokalypsix.chartx.chart.overlay.FibonacciArc;
import com.apokalypsix.chartx.chart.overlay.FibonacciExtension;
import com.apokalypsix.chartx.chart.overlay.FibonacciFan;
import com.apokalypsix.chartx.chart.overlay.FibonacciRetracement;
import com.apokalypsix.chartx.chart.overlay.FibonacciTimeZones;
import com.apokalypsix.chartx.chart.overlay.GannBox;
import com.apokalypsix.chartx.chart.overlay.GannFan;
import com.apokalypsix.chartx.chart.overlay.HorizontalLine;
import com.apokalypsix.chartx.chart.overlay.MeasureTool;
import com.apokalypsix.chartx.chart.overlay.Note;
import com.apokalypsix.chartx.chart.overlay.ParallelChannel;
import com.apokalypsix.chartx.chart.overlay.Pitchfork;
import com.apokalypsix.chartx.chart.overlay.Polygon;
import com.apokalypsix.chartx.chart.overlay.Polyline;
import com.apokalypsix.chartx.chart.overlay.PriceLabel;
import com.apokalypsix.chartx.chart.overlay.PriceRange;
import com.apokalypsix.chartx.chart.overlay.Ray;
import com.apokalypsix.chartx.chart.overlay.Rectangle;
import com.apokalypsix.chartx.chart.overlay.RegressionChannel;
import com.apokalypsix.chartx.chart.overlay.TrendLine;
import com.apokalypsix.chartx.chart.overlay.Triangle;
import com.apokalypsix.chartx.chart.overlay.VerticalLine;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for chart drawings using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link DrawingLayer} that uses backend-agnostic
 * rendering interfaces instead of direct GL calls.
 *
 * <p>Supports rendering of various drawing types including trend lines, horizontal/vertical
 * lines, Fibonacci retracements, rectangles, ellipses, and other drawing tools.
 */
public class DrawingLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(DrawingLayerV2.class);

    /** Z-order for drawing layer (above overlays, below annotations) */
    public static final int Z_ORDER = 600;

    private final List<Drawing> drawings = new ArrayList<>();

    // V2 API resources
    private Buffer lineBuffer;      // For lines and outlined shapes (triangles for thick lines)
    private Buffer handleBuffer;    // For selection handles
    private Shader defaultShader;   // Position + per-vertex color
    private boolean v2Initialized = false;

    // Vertex arrays (reused to avoid allocation)
    private float[] lineVertices;
    private float[] handleVertices;

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;

    // Handle rendering constants
    private static final int HANDLE_SIZE = 6;
    private static final int HANDLE_SEGMENTS = 8;
    private static final Color HANDLE_COLOR = new Color(255, 255, 255);
    private static final Color HANDLE_BORDER_COLOR = new Color(33, 150, 243);

    // Hit detection constants
    public static final int HIT_DISTANCE = 5;
    public static final int HANDLE_RADIUS = 8;

    public DrawingLayerV2() {
        super(Z_ORDER);
        // Allocate initial vertex arrays
        lineVertices = new float[1024 * FLOATS_PER_VERTEX];
        handleVertices = new float[256 * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily when first rendered with a valid RenderContext
        log.debug("DrawingLayerV2 GL initialized (v2 resources will init on first render)");
    }

    /**
     * Initializes V2 resources using the abstracted API.
     */
    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffer for drawing geometry (position + color)
        lineBuffer = resources.getOrCreateBuffer("drawing.lines",
                BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX));

        // Create buffer for selection handles
        handleBuffer = resources.getOrCreateBuffer("drawing.handles",
                BufferDescriptor.positionColor2D(512 * FLOATS_PER_VERTEX));

        // Get default shader (position + per-vertex color)
        defaultShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("DrawingLayerV2 V2 resources initialized");
    }

    // ========== Drawing management ==========

    /**
     * Adds a drawing to this layer.
     * Repaints automatically.
     */
    public void addDrawing(Drawing drawing) {
        if (!drawings.contains(drawing)) {
            drawings.add(drawing);
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Batch add multiple drawings (single repaint).
     *
     * @param drawingsToAdd the drawings to add
     */
    public void addAllDrawings(List<Drawing> drawingsToAdd) {
        boolean added = false;
        for (Drawing drawing : drawingsToAdd) {
            if (!drawings.contains(drawing)) {
                drawings.add(drawing);
                added = true;
            }
        }
        if (added) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a drawing from this layer.
     * Repaints automatically.
     */
    public void removeDrawing(Drawing drawing) {
        if (drawings.remove(drawing)) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a drawing by ID.
     * Repaints automatically.
     */
    public void removeDrawing(String id) {
        if (drawings.removeIf(d -> d.getId().equals(id))) {
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Clears all drawings.
     * Repaints automatically.
     */
    public void clearDrawings() {
        if (!drawings.isEmpty()) {
            drawings.clear();
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Returns all drawings.
     */
    public List<Drawing> getDrawings() {
        return new ArrayList<>(drawings);
    }

    /**
     * Returns the drawing at the specified screen position, if any.
     */
    public Drawing getDrawingAt(int screenX, int screenY, CoordinateSystem coords) {
        // Search in reverse order (top to bottom in z-order)
        for (int i = drawings.size() - 1; i >= 0; i--) {
            Drawing d = drawings.get(i);
            if (d.isVisible() && d.containsPoint(screenX, screenY, coords, HIT_DISTANCE)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Returns the handle at the specified screen position for a given drawing.
     */
    public Drawing.HandleType getHandleAt(Drawing drawing, int screenX, int screenY, CoordinateSystem coords) {
        if (drawing == null || !drawing.isVisible()) {
            return Drawing.HandleType.NONE;
        }
        return drawing.getHandleAt(screenX, screenY, coords, HANDLE_RADIUS);
    }

    /**
     * Deselects all drawings.
     */
    public void deselectAll() {
        for (Drawing d : drawings) {
            d.setSelected(false);
        }
        markDirty();
    }

    /**
     * Returns the currently selected drawing, or null if none selected.
     */
    public Drawing getSelectedDrawing() {
        for (Drawing d : drawings) {
            if (d.isSelected()) {
                return d;
            }
        }
        return null;
    }

    // ========== Rendering ==========

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("DrawingLayerV2 requires abstracted API - skipping render");
            return;
        }

        if (drawings.isEmpty()) {
            return;
        }

        // Initialize V2 resources if needed
        if (!v2Initialized) {
            initializeV2(ctx);
        }

        if (defaultShader == null || !defaultShader.isValid()) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinates();

        // Build and render drawing geometry
        int lineFloatCount = buildLineVertices(viewport, coords);
        if (lineFloatCount > 0) {
            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
        }

        // Build and render handles for selected drawings
        int handleFloatCount = buildHandleVertices(coords);
        if (handleFloatCount > 0) {
            defaultShader.bind();
            defaultShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            handleBuffer.upload(handleVertices, 0, handleFloatCount);
            handleBuffer.draw(DrawMode.TRIANGLES);

            defaultShader.unbind();
        }
    }

    private int buildLineVertices(Viewport viewport, CoordinateSystem coords) {
        int idx = 0;

        for (Drawing drawing : drawings) {
            if (!drawing.isVisible() || !drawing.isComplete()) {
                continue;
            }

            Color color = drawing.getColor();
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = drawing.getOpacity();
            float lineWidth = drawing.getLineWidth();

            if (drawing instanceof TrendLine trendLine) {
                idx = buildTrendLineVertices(idx, trendLine, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof HorizontalLine hLine) {
                idx = buildHorizontalLineVertices(idx, hLine, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof VerticalLine vLine) {
                idx = buildVerticalLineVertices(idx, vLine, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Ray ray) {
                idx = buildRayVertices(idx, ray, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof ExtendedLine extLine) {
                idx = buildExtendedLineVertices(idx, extLine, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Rectangle rect) {
                idx = buildRectangleVertices(idx, rect, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof FibonacciRetracement fib) {
                idx = buildFibonacciRetracementVertices(idx, fib, viewport, coords, lineWidth);
            } else if (drawing instanceof Ellipse ellipse) {
                idx = buildEllipseVertices(idx, ellipse, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof ParallelChannel channel) {
                idx = buildParallelChannelVertices(idx, channel, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Pitchfork pitchfork) {
                idx = buildPitchforkVertices(idx, pitchfork, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof FibonacciExtension fibExt) {
                idx = buildFibonacciExtensionVertices(idx, fibExt, viewport, coords, lineWidth);
            } else if (drawing instanceof Triangle triangle) {
                idx = buildTriangleVertices(idx, triangle, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Arrow arrow) {
                idx = buildArrowVertices(idx, arrow, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof MeasureTool measure) {
                idx = buildMeasureToolVertices(idx, measure, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof PriceRange priceRange) {
                idx = buildPriceRangeVertices(idx, priceRange, viewport, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Callout callout) {
                idx = buildCalloutVertices(idx, callout, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof PriceLabel priceLabel) {
                idx = buildPriceLabelVertices(idx, priceLabel, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof RegressionChannel regChannel) {
                idx = buildRegressionChannelVertices(idx, regChannel, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof FibonacciTimeZones fibTZ) {
                idx = buildFibonacciTimeZonesVertices(idx, fibTZ, viewport, coords, lineWidth);
            } else if (drawing instanceof FibonacciFan fibFan) {
                idx = buildFibonacciFanVertices(idx, fibFan, viewport, coords, lineWidth);
            } else if (drawing instanceof FibonacciArc fibArc) {
                idx = buildFibonacciArcVertices(idx, fibArc, coords, lineWidth);
            } else if (drawing instanceof Polyline polyline) {
                idx = buildPolylineVertices(idx, polyline, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Polygon polygon) {
                idx = buildPolygonVertices(idx, polygon, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Arc arc) {
                idx = buildArcVertices(idx, arc, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof GannFan gannFan) {
                idx = buildGannFanVertices(idx, gannFan, viewport, coords, lineWidth);
            } else if (drawing instanceof GannBox gannBox) {
                idx = buildGannBoxVertices(idx, gannBox, coords, r, g, b, a, lineWidth);
            } else if (drawing instanceof Note note) {
                idx = buildNoteVertices(idx, note, coords, r, g, b, a);
            }
        }

        return idx;
    }

    private int buildTrendLineVertices(int idx, TrendLine line, Viewport viewport, CoordinateSystem coords,
                                        float r, float g, float b, float a, float lineWidth) {
        AnchorPoint start = line.getStart();
        AnchorPoint end = line.getEnd();

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        // Calculate line extension if enabled
        if (line.isExtendLeft() || line.isExtendRight()) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length > 0) {
                double ux = dx / length;
                double uy = dy / length;

                double extendDist = Math.max(viewport.getWidth(), viewport.getHeight()) * 2;

                if (line.isExtendLeft()) {
                    x1 = x1 - ux * extendDist;
                    y1 = y1 - uy * extendDist;
                }

                if (line.isExtendRight()) {
                    x2 = x2 + ux * extendDist;
                    y2 = y2 + uy * extendDist;
                }
            }
        }

        idx = buildThickLine(idx, (float) x1, (float) y1, (float) x2, (float) y2, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildHorizontalLineVertices(int idx, HorizontalLine line, Viewport viewport, CoordinateSystem coords,
                                             float r, float g, float b, float a, float lineWidth) {
        double price = line.getPrice();
        double y = coords.yValueToScreenY(price);

        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();

        idx = buildThickLine(idx, chartLeft, (float) y, chartRight, (float) y, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildVerticalLineVertices(int idx, VerticalLine line, Viewport viewport, CoordinateSystem coords,
                                           float r, float g, float b, float a, float lineWidth) {
        double x = coords.xValueToScreenX(line.getTimestamp());

        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();

        idx = buildThickLine(idx, (float) x, chartTop, (float) x, chartBottom, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildRayVertices(int idx, Ray ray, Viewport viewport, CoordinateSystem coords,
                                  float r, float g, float b, float a, float lineWidth) {
        AnchorPoint start = ray.getStart();
        AnchorPoint end = ray.getDirection();

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            double extendDist = Math.max(viewport.getWidth(), viewport.getHeight()) * 2;
            x2 = x1 + (dx / length) * extendDist;
            y2 = y1 + (dy / length) * extendDist;
        }

        idx = buildThickLine(idx, (float) x1, (float) y1, (float) x2, (float) y2, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildExtendedLineVertices(int idx, ExtendedLine line, Viewport viewport, CoordinateSystem coords,
                                           float r, float g, float b, float a, float lineWidth) {
        AnchorPoint start = line.getPoint1();
        AnchorPoint end = line.getPoint2();

        double x1 = coords.xValueToScreenX(start.timestamp());
        double y1 = coords.yValueToScreenY(start.price());
        double x2 = coords.xValueToScreenX(end.timestamp());
        double y2 = coords.yValueToScreenY(end.price());

        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            double extendDist = Math.max(viewport.getWidth(), viewport.getHeight()) * 2;
            double ux = dx / length;
            double uy = dy / length;
            x1 = x1 - ux * extendDist;
            y1 = y1 - uy * extendDist;
            x2 = x2 + ux * extendDist;
            y2 = y2 + uy * extendDist;
        }

        idx = buildThickLine(idx, (float) x1, (float) y1, (float) x2, (float) y2, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildRectangleVertices(int idx, Rectangle rect, CoordinateSystem coords,
                                        float r, float g, float b, float a, float lineWidth) {
        AnchorPoint c1 = rect.getCorner1();
        AnchorPoint c2 = rect.getCorner2();

        float x1 = (float) coords.xValueToScreenX(c1.timestamp());
        float y1 = (float) coords.yValueToScreenY(c1.price());
        float x2 = (float) coords.xValueToScreenX(c2.timestamp());
        float y2 = (float) coords.yValueToScreenY(c2.price());

        float left = Math.min(x1, x2);
        float right = Math.max(x1, x2);
        float top = Math.min(y1, y2);
        float bottom = Math.max(y1, y2);

        // Draw fill if enabled
        if (rect.isFilled()) {
            Color fillColor = rect.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * rect.getOpacity();

            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, left, top, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, left, bottom, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, right, bottom, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, left, top, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, right, bottom, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, right, top, fr, fg, fb, fa);
        }

        // Draw border
        Color borderColor = rect.getBorderColor() != null ? rect.getBorderColor() : rect.getColor();
        float br = borderColor.getRed() / 255f;
        float bg = borderColor.getGreen() / 255f;
        float bb = borderColor.getBlue() / 255f;

        idx = buildThickLine(idx, left, top, right, top, lineWidth, br, bg, bb, a);
        idx = buildThickLine(idx, right, top, right, bottom, lineWidth, br, bg, bb, a);
        idx = buildThickLine(idx, right, bottom, left, bottom, lineWidth, br, bg, bb, a);
        idx = buildThickLine(idx, left, bottom, left, top, lineWidth, br, bg, bb, a);

        return idx;
    }

    private int buildFibonacciRetracementVertices(int idx, FibonacciRetracement fib, Viewport viewport,
                                                   CoordinateSystem coords, float lineWidth) {
        float[] levels = fib.getLevels();

        long leftTime = Math.min(fib.getHigh().timestamp(), fib.getLow().timestamp());
        long rightTime = Math.max(fib.getHigh().timestamp(), fib.getLow().timestamp());

        float x1 = fib.isExtendLines() ? viewport.getLeftInset()
                : (float) coords.xValueToScreenX(leftTime);
        float x2 = fib.isExtendLines() ? viewport.getWidth() - viewport.getRightInset()
                : (float) coords.xValueToScreenX(rightTime);

        for (int i = 0; i < levels.length; i++) {
            Color levelColor = fib.getLevelColor(i);
            float r = levelColor.getRed() / 255f;
            float g = levelColor.getGreen() / 255f;
            float b = levelColor.getBlue() / 255f;
            float a = fib.getOpacity();

            double price = fib.getPriceAtLevel(levels[i]);
            float y = (float) coords.yValueToScreenY(price);

            idx = buildThickLine(idx, x1, y, x2, y, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildEllipseVertices(int idx, Ellipse ellipse, CoordinateSystem coords,
                                      float r, float g, float b, float a, float lineWidth) {
        AnchorPoint c1 = ellipse.getCorner1();
        AnchorPoint c2 = ellipse.getCorner2();

        float x1 = (float) coords.xValueToScreenX(c1.timestamp());
        float y1 = (float) coords.yValueToScreenY(c1.price());
        float x2 = (float) coords.xValueToScreenX(c2.timestamp());
        float y2 = (float) coords.yValueToScreenY(c2.price());

        float cx = (x1 + x2) / 2;
        float cy = (y1 + y2) / 2;
        float rx = Math.abs(x2 - x1) / 2;
        float ry = Math.abs(y2 - y1) / 2;

        int segments = 32;

        // Draw fill if enabled
        if (ellipse.isFilled()) {
            Color fillColor = ellipse.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * ellipse.getOpacity();

            for (int i = 0; i < segments; i++) {
                double angle1 = 2 * Math.PI * i / segments;
                double angle2 = 2 * Math.PI * (i + 1) / segments;

                float px1 = cx + rx * (float) Math.cos(angle1);
                float py1 = cy + ry * (float) Math.sin(angle1);
                float px2 = cx + rx * (float) Math.cos(angle2);
                float py2 = cy + ry * (float) Math.sin(angle2);

                ensureLineCapacity(idx + 3 * FLOATS_PER_VERTEX);
                idx = addVertex(lineVertices, idx, cx, cy, fr, fg, fb, fa);
                idx = addVertex(lineVertices, idx, px1, py1, fr, fg, fb, fa);
                idx = addVertex(lineVertices, idx, px2, py2, fr, fg, fb, fa);
            }
        }

        // Draw outline
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;

            float px1 = cx + rx * (float) Math.cos(angle1);
            float py1 = cy + ry * (float) Math.sin(angle1);
            float px2 = cx + rx * (float) Math.cos(angle2);
            float py2 = cy + ry * (float) Math.sin(angle2);

            idx = buildThickLine(idx, px1, py1, px2, py2, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildParallelChannelVertices(int idx, ParallelChannel channel, Viewport viewport,
                                              CoordinateSystem coords, float r, float g, float b, float a, float lineWidth) {
        AnchorPoint p1 = channel.getPoint1();
        AnchorPoint p2 = channel.getPoint2();
        AnchorPoint p3 = channel.getPoint3();

        float x1 = (float) coords.xValueToScreenX(p1.timestamp());
        float y1 = (float) coords.yValueToScreenY(p1.price());
        float x2 = (float) coords.xValueToScreenX(p2.timestamp());
        float y2 = (float) coords.yValueToScreenY(p2.price());
        float x3 = (float) coords.xValueToScreenX(p3.timestamp());
        float y3 = (float) coords.yValueToScreenY(p3.price());

        // Main line from p1 to p2
        idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);

        // Parallel line through p3
        float dx = x2 - x1;
        float dy = y2 - y1;
        float x4 = x3 + dx;
        float y4 = y3 + dy;
        idx = buildThickLine(idx, x3, y3, x4, y4, lineWidth, r, g, b, a);

        // Draw fill if enabled
        if (channel.isFilled()) {
            Color fillColor = channel.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * channel.getOpacity();

            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, x1, y1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x2, y2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x4, y4, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x1, y1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x4, y4, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x3, y3, fr, fg, fb, fa);
        }

        return idx;
    }

    private int buildPitchforkVertices(int idx, Pitchfork pitchfork, Viewport viewport,
                                        CoordinateSystem coords, float r, float g, float b, float a, float lineWidth) {
        AnchorPoint p1 = pitchfork.getPoint1();
        AnchorPoint p2 = pitchfork.getPoint2();
        AnchorPoint p3 = pitchfork.getPoint3();

        float x1 = (float) coords.xValueToScreenX(p1.timestamp());
        float y1 = (float) coords.yValueToScreenY(p1.price());
        float x2 = (float) coords.xValueToScreenX(p2.timestamp());
        float y2 = (float) coords.yValueToScreenY(p2.price());
        float x3 = (float) coords.xValueToScreenX(p3.timestamp());
        float y3 = (float) coords.yValueToScreenY(p3.price());

        // Midpoint of p2-p3
        float mx = (x2 + x3) / 2;
        float my = (y2 + y3) / 2;

        // Main median line from p1 through midpoint
        float dx = mx - x1;
        float dy = my - y1;
        float extendDist = (float) Math.max(viewport.getWidth(), viewport.getHeight()) * 2;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            float ex = x1 + (dx / length) * extendDist;
            float ey = y1 + (dy / length) * extendDist;
            idx = buildThickLine(idx, x1, y1, ex, ey, lineWidth, r, g, b, a);

            // Upper parallel from p2
            float px2 = x2 + (dx / length) * extendDist;
            float py2 = y2 + (dy / length) * extendDist;
            idx = buildThickLine(idx, x2, y2, px2, py2, lineWidth, r, g, b, a);

            // Lower parallel from p3
            float px3 = x3 + (dx / length) * extendDist;
            float py3 = y3 + (dy / length) * extendDist;
            idx = buildThickLine(idx, x3, y3, px3, py3, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildFibonacciExtensionVertices(int idx, FibonacciExtension fibExt, Viewport viewport,
                                                 CoordinateSystem coords, float lineWidth) {
        float[] levels = fibExt.getLevels();

        float x1 = (float) coords.xValueToScreenX(fibExt.getPoint1().timestamp());
        float x2 = fibExt.isExtendLines() ? viewport.getWidth() - viewport.getRightInset()
                : (float) coords.xValueToScreenX(fibExt.getPoint3().timestamp());

        for (int i = 0; i < levels.length; i++) {
            Color levelColor = fibExt.getLevelColor(i);
            float r = levelColor.getRed() / 255f;
            float g = levelColor.getGreen() / 255f;
            float b = levelColor.getBlue() / 255f;
            float a = fibExt.getOpacity();

            double price = fibExt.getPriceAtLevel(levels[i]);
            float y = (float) coords.yValueToScreenY(price);

            idx = buildThickLine(idx, x1, y, x2, y, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildTriangleVertices(int idx, Triangle triangle, CoordinateSystem coords,
                                       float r, float g, float b, float a, float lineWidth) {
        AnchorPoint p1 = triangle.getPoint1();
        AnchorPoint p2 = triangle.getPoint2();
        AnchorPoint p3 = triangle.getPoint3();

        float x1 = (float) coords.xValueToScreenX(p1.timestamp());
        float y1 = (float) coords.yValueToScreenY(p1.price());
        float x2 = (float) coords.xValueToScreenX(p2.timestamp());
        float y2 = (float) coords.yValueToScreenY(p2.price());
        float x3 = (float) coords.xValueToScreenX(p3.timestamp());
        float y3 = (float) coords.yValueToScreenY(p3.price());

        // Draw fill if enabled
        if (triangle.isFilled()) {
            Color fillColor = triangle.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * triangle.getOpacity();

            ensureLineCapacity(idx + 3 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, x1, y1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x2, y2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x3, y3, fr, fg, fb, fa);
        }

        // Draw edges
        idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);
        idx = buildThickLine(idx, x2, y2, x3, y3, lineWidth, r, g, b, a);
        idx = buildThickLine(idx, x3, y3, x1, y1, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildArrowVertices(int idx, Arrow arrow, CoordinateSystem coords,
                                    float r, float g, float b, float a, float lineWidth) {
        AnchorPoint start = arrow.getStart();
        AnchorPoint end = arrow.getEnd();

        float x1 = (float) coords.xValueToScreenX(start.timestamp());
        float y1 = (float) coords.yValueToScreenY(start.price());
        float x2 = (float) coords.xValueToScreenX(end.timestamp());
        float y2 = (float) coords.yValueToScreenY(end.price());

        // Draw main line
        idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);

        // Draw arrowhead
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            float headSize = arrow.getHeadSize();
            float ux = dx / length;
            float uy = dy / length;

            float ax1 = x2 - headSize * ux + headSize * 0.5f * uy;
            float ay1 = y2 - headSize * uy - headSize * 0.5f * ux;
            float ax2 = x2 - headSize * ux - headSize * 0.5f * uy;
            float ay2 = y2 - headSize * uy + headSize * 0.5f * ux;

            idx = buildThickLine(idx, x2, y2, ax1, ay1, lineWidth, r, g, b, a);
            idx = buildThickLine(idx, x2, y2, ax2, ay2, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildMeasureToolVertices(int idx, MeasureTool measure, CoordinateSystem coords,
                                          float r, float g, float b, float a, float lineWidth) {
        AnchorPoint start = measure.getStart();
        AnchorPoint end = measure.getEnd();

        float x1 = (float) coords.xValueToScreenX(start.timestamp());
        float y1 = (float) coords.yValueToScreenY(start.price());
        float x2 = (float) coords.xValueToScreenX(end.timestamp());
        float y2 = (float) coords.yValueToScreenY(end.price());

        // Draw main diagonal line
        idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);

        // Draw horizontal reference line
        idx = buildThickLine(idx, x1, y1, x2, y1, lineWidth * 0.5f, r, g, b, a * 0.5f);

        // Draw vertical reference line
        idx = buildThickLine(idx, x2, y1, x2, y2, lineWidth * 0.5f, r, g, b, a * 0.5f);

        return idx;
    }

    private int buildPriceRangeVertices(int idx, PriceRange priceRange, Viewport viewport,
                                         CoordinateSystem coords, float r, float g, float b, float a, float lineWidth) {
        float y1 = (float) coords.yValueToScreenY(priceRange.getTopPrice());
        float y2 = (float) coords.yValueToScreenY(priceRange.getBottomPrice());

        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();

        // Draw fill
        if (priceRange.isFilled()) {
            Color fillColor = priceRange.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * priceRange.getOpacity();

            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, chartLeft, y1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, chartLeft, y2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, chartRight, y2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, chartLeft, y1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, chartRight, y2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, chartRight, y1, fr, fg, fb, fa);
        }

        // Draw top and bottom lines
        idx = buildThickLine(idx, chartLeft, y1, chartRight, y1, lineWidth, r, g, b, a);
        idx = buildThickLine(idx, chartLeft, y2, chartRight, y2, lineWidth, r, g, b, a);

        return idx;
    }

    private int buildCalloutVertices(int idx, Callout callout, CoordinateSystem coords,
                                      float r, float g, float b, float a, float lineWidth) {
        AnchorPoint target = callout.getTarget();
        AnchorPoint textBox = callout.getTextBox();

        float x1 = (float) coords.xValueToScreenX(target.timestamp());
        float y1 = (float) coords.yValueToScreenY(target.price());
        float x2 = (float) coords.xValueToScreenX(textBox.timestamp());
        float y2 = (float) coords.yValueToScreenY(textBox.price());

        // Draw connector line
        idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);

        // Draw target marker (small circle)
        idx = buildCircle(lineVertices, idx, x1, y1, 4, r, g, b, a, 8);

        // Draw text box background
        Color bgColor = callout.getBackgroundColor();
        float bgR = bgColor.getRed() / 255f;
        float bgG = bgColor.getGreen() / 255f;
        float bgB = bgColor.getBlue() / 255f;
        float bgA = bgColor.getAlpha() / 255f * callout.getOpacity();

        String text = callout.getText();
        int boxWidth = Math.max(80, text.length() * 8 + callout.getPadding() * 2);
        int boxHeight = callout.getFontSize() + callout.getPadding() * 2;

        float left = x2 - boxWidth / 2f;
        float right = x2 + boxWidth / 2f;
        float top = y2 - boxHeight / 2f;
        float bottom = y2 + boxHeight / 2f;

        ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
        idx = addVertex(lineVertices, idx, left, top, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, left, bottom, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, right, bottom, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, left, top, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, right, bottom, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, right, top, bgR, bgG, bgB, bgA);

        // Draw text box border
        Color borderColor = callout.getBorderColor();
        float brR = borderColor.getRed() / 255f;
        float brG = borderColor.getGreen() / 255f;
        float brB = borderColor.getBlue() / 255f;

        idx = buildThickLine(idx, left, top, right, top, 1, brR, brG, brB, a);
        idx = buildThickLine(idx, right, top, right, bottom, 1, brR, brG, brB, a);
        idx = buildThickLine(idx, right, bottom, left, bottom, 1, brR, brG, brB, a);
        idx = buildThickLine(idx, left, bottom, left, top, 1, brR, brG, brB, a);

        return idx;
    }

    private int buildPriceLabelVertices(int idx, PriceLabel priceLabel, CoordinateSystem coords,
                                         float r, float g, float b, float a, float lineWidth) {
        AnchorPoint anchor = priceLabel.getAnchor();

        float x = (float) coords.xValueToScreenX(anchor.timestamp());
        float y = (float) coords.yValueToScreenY(anchor.price());

        // Draw label background
        Color bgColor = priceLabel.getBackgroundColor();
        float bgR = bgColor.getRed() / 255f;
        float bgG = bgColor.getGreen() / 255f;
        float bgB = bgColor.getBlue() / 255f;
        float bgA = bgColor.getAlpha() / 255f * priceLabel.getOpacity();

        String text = priceLabel.getDisplayText();
        int labelWidth = Math.max(40, text.length() * 7 + priceLabel.getPadding() * 2);
        int labelHeight = priceLabel.getFontSize() + priceLabel.getPadding() * 2;

        float left, right;
        switch (priceLabel.getAlignment()) {
            case LEFT -> {
                left = x;
                right = x + labelWidth;
            }
            case CENTER -> {
                left = x - labelWidth / 2f;
                right = x + labelWidth / 2f;
            }
            default -> {
                left = x - labelWidth;
                right = x;
            }
        }
        float top = y - labelHeight / 2f;
        float bottom = y + labelHeight / 2f;

        ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
        idx = addVertex(lineVertices, idx, left, top, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, left, bottom, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, right, bottom, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, left, top, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, right, bottom, bgR, bgG, bgB, bgA);
        idx = addVertex(lineVertices, idx, right, top, bgR, bgG, bgB, bgA);

        // Draw border
        Color borderColor = priceLabel.getBorderColor();
        float brR = borderColor.getRed() / 255f;
        float brG = borderColor.getGreen() / 255f;
        float brB = borderColor.getBlue() / 255f;

        idx = buildThickLine(idx, left, top, right, top, 1, brR, brG, brB, a);
        idx = buildThickLine(idx, right, top, right, bottom, 1, brR, brG, brB, a);
        idx = buildThickLine(idx, right, bottom, left, bottom, 1, brR, brG, brB, a);
        idx = buildThickLine(idx, left, bottom, left, top, 1, brR, brG, brB, a);

        return idx;
    }

    private int buildRegressionChannelVertices(int idx, RegressionChannel channel, CoordinateSystem coords,
                                                float r, float g, float b, float a, float lineWidth) {
        AnchorPoint start = channel.getStart();
        AnchorPoint end = channel.getEnd();

        float x1 = (float) coords.xValueToScreenX(start.timestamp());
        float y1 = (float) coords.yValueToScreenY(start.price());
        float x2 = (float) coords.xValueToScreenX(end.timestamp());
        float y2 = (float) coords.yValueToScreenY(end.price());

        double channelWidth = channel.getChannelWidth();
        float upperY1 = (float) coords.yValueToScreenY(start.price() + channelWidth);
        float upperY2 = (float) coords.yValueToScreenY(end.price() + channelWidth);
        float lowerY1 = (float) coords.yValueToScreenY(start.price() - channelWidth);
        float lowerY2 = (float) coords.yValueToScreenY(end.price() - channelWidth);

        // Draw fill
        if (channel.isFilled()) {
            Color fillColor = channel.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * channel.getOpacity();

            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, x1, upperY1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x1, lowerY1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x2, lowerY2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x1, upperY1, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x2, lowerY2, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, x2, upperY2, fr, fg, fb, fa);
        }

        // Draw center line
        idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);

        // Draw upper and lower lines
        idx = buildThickLine(idx, x1, upperY1, x2, upperY2, lineWidth * 0.7f, r, g, b, a * 0.7f);
        idx = buildThickLine(idx, x1, lowerY1, x2, lowerY2, lineWidth * 0.7f, r, g, b, a * 0.7f);

        return idx;
    }

    private int buildFibonacciTimeZonesVertices(int idx, FibonacciTimeZones fibTZ, Viewport viewport,
                                                 CoordinateSystem coords, float lineWidth) {
        int[] intervals = fibTZ.getIntervals();
        long unit = fibTZ.getUnitInterval();

        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();

        int cumulative = 0;
        for (int i = 0; i < intervals.length; i++) {
            cumulative += intervals[i];
            long timestamp = fibTZ.getStart().timestamp() + unit * cumulative;

            Color lineColor = fibTZ.getLineColor(i);
            float r = lineColor.getRed() / 255f;
            float g = lineColor.getGreen() / 255f;
            float b = lineColor.getBlue() / 255f;
            float a = fibTZ.getOpacity();

            float x = (float) coords.xValueToScreenX(timestamp);
            idx = buildThickLine(idx, x, chartTop, x, chartBottom, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildFibonacciFanVertices(int idx, FibonacciFan fibFan, Viewport viewport,
                                           CoordinateSystem coords, float lineWidth) {
        float[] levels = fibFan.getLevels();

        float x1 = (float) coords.xValueToScreenX(fibFan.getStart().timestamp());
        float y1 = (float) coords.yValueToScreenY(fibFan.getStart().price());

        float extendDist = (float) Math.max(viewport.getWidth(), viewport.getHeight()) * 2;

        for (int i = 0; i < levels.length; i++) {
            Color levelColor = fibFan.getLevelColor(i);
            float r = levelColor.getRed() / 255f;
            float g = levelColor.getGreen() / 255f;
            float b = levelColor.getBlue() / 255f;
            float a = fibFan.getOpacity();

            double price = fibFan.getPriceAtLevel(levels[i]);
            float x2 = (float) coords.xValueToScreenX(fibFan.getEnd().timestamp());
            float y2 = (float) coords.yValueToScreenY(price);

            // Extend the ray
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                x2 = x1 + (dx / length) * extendDist;
                y2 = y1 + (dy / length) * extendDist;
            }

            idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildFibonacciArcVertices(int idx, FibonacciArc fibArc, CoordinateSystem coords, float lineWidth) {
        float[] levels = fibArc.getLevels();

        float cx = (float) coords.xValueToScreenX(fibArc.getEnd().timestamp());
        float cy = (float) coords.yValueToScreenY(fibArc.getEnd().price());
        double baseRadius = fibArc.getBaseRadius(coords);

        int segments = 24;

        for (int i = 0; i < levels.length; i++) {
            Color levelColor = fibArc.getLevelColor(i);
            float r = levelColor.getRed() / 255f;
            float g = levelColor.getGreen() / 255f;
            float b = levelColor.getBlue() / 255f;
            float a = fibArc.getOpacity();

            float radius = (float) (baseRadius * levels[i]);

            // Draw semicircle
            for (int j = 0; j < segments; j++) {
                double angle1 = Math.PI * j / segments;
                double angle2 = Math.PI * (j + 1) / segments;

                float px1 = cx + radius * (float) Math.cos(angle1);
                float py1 = cy - radius * (float) Math.sin(angle1);
                float px2 = cx + radius * (float) Math.cos(angle2);
                float py2 = cy - radius * (float) Math.sin(angle2);

                idx = buildThickLine(idx, px1, py1, px2, py2, lineWidth, r, g, b, a);
            }
        }

        return idx;
    }

    private int buildPolylineVertices(int idx, Polyline polyline, CoordinateSystem coords,
                                       float r, float g, float b, float a, float lineWidth) {
        List<AnchorPoint> points = polyline.getAnchorPoints();
        if (points.size() < 2) return idx;

        for (int i = 0; i < points.size() - 1; i++) {
            AnchorPoint p1 = points.get(i);
            AnchorPoint p2 = points.get(i + 1);

            float x1 = (float) coords.xValueToScreenX(p1.timestamp());
            float y1 = (float) coords.yValueToScreenY(p1.price());
            float x2 = (float) coords.xValueToScreenX(p2.timestamp());
            float y2 = (float) coords.yValueToScreenY(p2.price());

            idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);
        }

        if (polyline.isClosed() && points.size() > 2) {
            AnchorPoint first = points.get(0);
            AnchorPoint last = points.get(points.size() - 1);

            float x1 = (float) coords.xValueToScreenX(last.timestamp());
            float y1 = (float) coords.yValueToScreenY(last.price());
            float x2 = (float) coords.xValueToScreenX(first.timestamp());
            float y2 = (float) coords.yValueToScreenY(first.price());

            idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildPolygonVertices(int idx, Polygon polygon, CoordinateSystem coords,
                                      float r, float g, float b, float a, float lineWidth) {
        List<AnchorPoint> points = polygon.getAnchorPoints();
        if (points.size() < 3) return idx;

        // Draw fill using triangle fan
        if (polygon.isFilled()) {
            Color fillColor = polygon.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * polygon.getOpacity();

            AnchorPoint center = points.get(0);
            float cx = (float) coords.xValueToScreenX(center.timestamp());
            float cy = (float) coords.yValueToScreenY(center.price());

            for (int i = 1; i < points.size() - 1; i++) {
                AnchorPoint p1 = points.get(i);
                AnchorPoint p2 = points.get(i + 1);

                float x1 = (float) coords.xValueToScreenX(p1.timestamp());
                float y1 = (float) coords.yValueToScreenY(p1.price());
                float x2 = (float) coords.xValueToScreenX(p2.timestamp());
                float y2 = (float) coords.yValueToScreenY(p2.price());

                ensureLineCapacity(idx + 3 * FLOATS_PER_VERTEX);
                idx = addVertex(lineVertices, idx, cx, cy, fr, fg, fb, fa);
                idx = addVertex(lineVertices, idx, x1, y1, fr, fg, fb, fa);
                idx = addVertex(lineVertices, idx, x2, y2, fr, fg, fb, fa);
            }
        }

        // Draw border
        Color borderColor = polygon.getBorderColor() != null ? polygon.getBorderColor() : polygon.getColor();
        float br = borderColor.getRed() / 255f;
        float bg = borderColor.getGreen() / 255f;
        float bb = borderColor.getBlue() / 255f;

        for (int i = 0; i < points.size(); i++) {
            AnchorPoint p1 = points.get(i);
            AnchorPoint p2 = points.get((i + 1) % points.size());

            float x1 = (float) coords.xValueToScreenX(p1.timestamp());
            float y1 = (float) coords.yValueToScreenY(p1.price());
            float x2 = (float) coords.xValueToScreenX(p2.timestamp());
            float y2 = (float) coords.yValueToScreenY(p2.price());

            idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, br, bg, bb, a);
        }

        return idx;
    }

    private int buildArcVertices(int idx, Arc arc, CoordinateSystem coords,
                                  float r, float g, float b, float a, float lineWidth) {
        float cx = (float) coords.xValueToScreenX(arc.getCenter().timestamp());
        float cy = (float) coords.yValueToScreenY(arc.getCenter().price());
        float radius = (float) arc.getRadius(coords);

        double baseAngle = arc.getAngleToRadiusPoint(coords);
        double startAngle = baseAngle + arc.getStartAngle();
        double arcExtent = arc.getArcExtent();

        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double angle1 = startAngle + arcExtent * i / segments;
            double angle2 = startAngle + arcExtent * (i + 1) / segments;

            float x1 = cx + radius * (float) Math.cos(angle1);
            float y1 = cy + radius * (float) Math.sin(angle1);
            float x2 = cx + radius * (float) Math.cos(angle2);
            float y2 = cy + radius * (float) Math.sin(angle2);

            idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildGannFanVertices(int idx, GannFan gannFan, Viewport viewport,
                                      CoordinateSystem coords, float lineWidth) {
        float[][] angles = gannFan.getAngles();

        float x1 = (float) coords.xValueToScreenX(gannFan.getStart().timestamp());
        float y1 = (float) coords.yValueToScreenY(gannFan.getStart().price());

        long timeUnit = gannFan.getTimeUnit();
        double priceUnit = gannFan.getPriceUnit();
        boolean upward = gannFan.isUpward();

        float extendDist = (float) Math.max(viewport.getWidth(), viewport.getHeight()) * 2;

        for (int i = 0; i < angles.length; i++) {
            Color lineColor = gannFan.getLineColor(i);
            float r = lineColor.getRed() / 255f;
            float g = lineColor.getGreen() / 255f;
            float b = lineColor.getBlue() / 255f;
            float a = gannFan.getOpacity();

            float timeRatio = angles[i][0];
            float priceRatio = angles[i][1];

            long deltaTime = (long) (timeUnit * timeRatio);
            double deltaPrice = priceUnit * priceRatio;
            if (!upward) deltaPrice = -deltaPrice;

            float x2 = (float) coords.xValueToScreenX(gannFan.getStart().timestamp() + deltaTime);
            float y2 = (float) coords.yValueToScreenY(gannFan.getStart().price() + deltaPrice);

            // Extend the ray
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                x2 = x1 + (dx / length) * extendDist;
                y2 = y1 + (dy / length) * extendDist;
            }

            idx = buildThickLine(idx, x1, y1, x2, y2, lineWidth, r, g, b, a);
        }

        return idx;
    }

    private int buildGannBoxVertices(int idx, GannBox gannBox, CoordinateSystem coords,
                                      float r, float g, float b, float a, float lineWidth) {
        AnchorPoint c1 = gannBox.getCorner1();
        AnchorPoint c2 = gannBox.getCorner2();

        float x1 = (float) coords.xValueToScreenX(c1.timestamp());
        float y1 = (float) coords.yValueToScreenY(c1.price());
        float x2 = (float) coords.xValueToScreenX(c2.timestamp());
        float y2 = (float) coords.yValueToScreenY(c2.price());

        float left = Math.min(x1, x2);
        float right = Math.max(x1, x2);
        float top = Math.min(y1, y2);
        float bottom = Math.max(y1, y2);

        // Draw fill
        if (gannBox.isFilled()) {
            Color fillColor = gannBox.getFillColor();
            float fr = fillColor.getRed() / 255f;
            float fg = fillColor.getGreen() / 255f;
            float fb = fillColor.getBlue() / 255f;
            float fa = fillColor.getAlpha() / 255f * gannBox.getOpacity();

            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, left, top, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, left, bottom, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, right, bottom, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, left, top, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, right, bottom, fr, fg, fb, fa);
            idx = addVertex(lineVertices, idx, right, top, fr, fg, fb, fa);
        }

        // Draw border
        idx = buildThickLine(idx, left, top, right, top, lineWidth, r, g, b, a);
        idx = buildThickLine(idx, right, top, right, bottom, lineWidth, r, g, b, a);
        idx = buildThickLine(idx, right, bottom, left, bottom, lineWidth, r, g, b, a);
        idx = buildThickLine(idx, left, bottom, left, top, lineWidth, r, g, b, a);

        // Draw grid lines
        Color gridColor = gannBox.getGridColor();
        float gr = gridColor.getRed() / 255f;
        float gg = gridColor.getGreen() / 255f;
        float gb = gridColor.getBlue() / 255f;
        float ga = gridColor.getAlpha() / 255f * gannBox.getOpacity();

        for (float level : gannBox.getPriceLevels()) {
            if (level > 0 && level < 1) {
                float py = top + (bottom - top) * level;
                idx = buildThickLine(idx, left, py, right, py, lineWidth * 0.5f, gr, gg, gb, ga);
            }
        }
        for (float level : gannBox.getTimeLevels()) {
            if (level > 0 && level < 1) {
                float px = left + (right - left) * level;
                idx = buildThickLine(idx, px, top, px, bottom, lineWidth * 0.5f, gr, gg, gb, ga);
            }
        }

        // Draw diagonals
        if (gannBox.isShowDiagonals()) {
            idx = buildThickLine(idx, left, top, right, bottom, lineWidth * 0.5f, gr, gg, gb, ga);
            idx = buildThickLine(idx, left, bottom, right, top, lineWidth * 0.5f, gr, gg, gb, ga);
        }

        return idx;
    }

    private int buildNoteVertices(int idx, Note note, CoordinateSystem coords,
                                   float r, float g, float b, float a) {
        float x = (float) coords.xValueToScreenX(note.getAnchor().timestamp());
        float y = (float) coords.yValueToScreenY(note.getAnchor().price());

        Color markerColor = note.getMarkerColor();
        float mr = markerColor.getRed() / 255f;
        float mg = markerColor.getGreen() / 255f;
        float mb = markerColor.getBlue() / 255f;
        float ma = note.getOpacity();

        int markerSize = note.getMarkerSize();

        if (!note.isExpanded()) {
            // Draw marker icon (diamond shape)
            float halfSize = markerSize / 2f;
            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, x, y - halfSize, mr, mg, mb, ma);
            idx = addVertex(lineVertices, idx, x - halfSize, y, mr, mg, mb, ma);
            idx = addVertex(lineVertices, idx, x, y + halfSize, mr, mg, mb, ma);
            idx = addVertex(lineVertices, idx, x, y - halfSize, mr, mg, mb, ma);
            idx = addVertex(lineVertices, idx, x, y + halfSize, mr, mg, mb, ma);
            idx = addVertex(lineVertices, idx, x + halfSize, y, mr, mg, mb, ma);
        } else {
            // Draw expanded note background
            Color bgColor = note.getBackgroundColor();
            float bgR = bgColor.getRed() / 255f;
            float bgG = bgColor.getGreen() / 255f;
            float bgB = bgColor.getBlue() / 255f;
            float bgA = bgColor.getAlpha() / 255f * note.getOpacity();

            int maxWidth = note.getMaxWidth();
            int padding = note.getPadding();
            int noteHeight = padding * 2 + note.getFontSize() * 3;  // Approximate

            ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);
            idx = addVertex(lineVertices, idx, x, y, bgR, bgG, bgB, bgA);
            idx = addVertex(lineVertices, idx, x, y + noteHeight, bgR, bgG, bgB, bgA);
            idx = addVertex(lineVertices, idx, x + maxWidth, y + noteHeight, bgR, bgG, bgB, bgA);
            idx = addVertex(lineVertices, idx, x, y, bgR, bgG, bgB, bgA);
            idx = addVertex(lineVertices, idx, x + maxWidth, y + noteHeight, bgR, bgG, bgB, bgA);
            idx = addVertex(lineVertices, idx, x + maxWidth, y, bgR, bgG, bgB, bgA);

            // Draw border
            Color borderColor = note.getBorderColor();
            float brR = borderColor.getRed() / 255f;
            float brG = borderColor.getGreen() / 255f;
            float brB = borderColor.getBlue() / 255f;

            idx = buildThickLine(idx, x, y, x + maxWidth, y, 1, brR, brG, brB, a);
            idx = buildThickLine(idx, x + maxWidth, y, x + maxWidth, y + noteHeight, 1, brR, brG, brB, a);
            idx = buildThickLine(idx, x + maxWidth, y + noteHeight, x, y + noteHeight, 1, brR, brG, brB, a);
            idx = buildThickLine(idx, x, y + noteHeight, x, y, 1, brR, brG, brB, a);
        }

        return idx;
    }

    private int buildThickLine(int idx, float x1, float y1, float x2, float y2, float thickness,
                                float r, float g, float b, float a) {
        // Calculate perpendicular vector for line thickness
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.001f) {
            return idx;
        }

        // Normalize and rotate 90 degrees for perpendicular
        float nx = -dy / length * thickness / 2f;
        float ny = dx / length * thickness / 2f;

        // Build quad as two triangles
        ensureLineCapacity(idx + 6 * FLOATS_PER_VERTEX);

        // Triangle 1
        idx = addVertex(lineVertices, idx, x1 - nx, y1 - ny, r, g, b, a);
        idx = addVertex(lineVertices, idx, x1 + nx, y1 + ny, r, g, b, a);
        idx = addVertex(lineVertices, idx, x2 + nx, y2 + ny, r, g, b, a);

        // Triangle 2
        idx = addVertex(lineVertices, idx, x1 - nx, y1 - ny, r, g, b, a);
        idx = addVertex(lineVertices, idx, x2 + nx, y2 + ny, r, g, b, a);
        idx = addVertex(lineVertices, idx, x2 - nx, y2 - ny, r, g, b, a);

        return idx;
    }

    private int buildHandleVertices(CoordinateSystem coords) {
        int idx = 0;

        for (Drawing drawing : drawings) {
            if (!drawing.isVisible() || !drawing.isSelected() || !drawing.isComplete()) {
                continue;
            }

            List<AnchorPoint> anchors = drawing.getAnchorPoints();
            for (AnchorPoint anchor : anchors) {
                float x = (float) coords.xValueToScreenX(anchor.timestamp());
                float y = (float) coords.yValueToScreenY(anchor.price());

                // For horizontal lines, place handle at a consistent position
                if (drawing instanceof HorizontalLine) {
                    // Don't draw handle for horizontal lines - they're dragged by the body
                    continue;
                }

                // Draw handle (filled circle with border)
                idx = buildHandle(idx, x, y);
            }
        }

        return idx;
    }

    private int buildHandle(int idx, float cx, float cy) {
        ensureHandleCapacity(idx + (HANDLE_SEGMENTS * 6 + HANDLE_SEGMENTS * 6) * FLOATS_PER_VERTEX);

        // Draw border circle
        float borderR = HANDLE_BORDER_COLOR.getRed() / 255f;
        float borderG = HANDLE_BORDER_COLOR.getGreen() / 255f;
        float borderB = HANDLE_BORDER_COLOR.getBlue() / 255f;
        idx = buildCircle(handleVertices, idx, cx, cy, HANDLE_SIZE + 2, borderR, borderG, borderB, 1.0f, HANDLE_SEGMENTS);

        // Draw fill circle
        float fillR = HANDLE_COLOR.getRed() / 255f;
        float fillG = HANDLE_COLOR.getGreen() / 255f;
        float fillB = HANDLE_COLOR.getBlue() / 255f;
        idx = buildCircle(handleVertices, idx, cx, cy, HANDLE_SIZE, fillR, fillG, fillB, 1.0f, HANDLE_SEGMENTS);

        return idx;
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

    private int addVertex(float[] v, int idx, float x, float y, float r, float g, float b, float a) {
        v[idx++] = x;
        v[idx++] = y;
        v[idx++] = r;
        v[idx++] = g;
        v[idx++] = b;
        v[idx++] = a;
        return idx;
    }

    private void ensureLineCapacity(int requiredFloats) {
        if (requiredFloats > lineVertices.length) {
            int newCapacity = Math.max(requiredFloats, (int) (lineVertices.length * 1.5));
            lineVertices = new float[newCapacity];
        }
    }

    private void ensureHandleCapacity(int requiredFloats) {
        if (requiredFloats > handleVertices.length) {
            int newCapacity = Math.max(requiredFloats, (int) (handleVertices.length * 1.5));
            handleVertices = new float[newCapacity];
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
                resources.disposeBuffer("drawing.lines");
                resources.disposeBuffer("drawing.handles");
            }
            lineBuffer = null;
            handleBuffer = null;
            defaultShader = null;
            v2Initialized = false;
        }
    }
}
