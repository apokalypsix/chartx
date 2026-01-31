package com.apokalypsix.chartx.core.render.util;

/**
 * Utilities for tessellating UI shapes into vertex data for GPU rendering.
 *
 * <p>All methods use the positionColor2D vertex format (6 floats per vertex):
 * x, y, r, g, b, a
 *
 * <p>Shapes are tessellated into triangles for efficient GPU rendering.
 */
public class ShapeUtils {

    // Vertex format: position(2) + color(4) = 6 floats per vertex
    public static final int FLOATS_PER_VERTEX = 6;

    // Triangles per shape
    public static final int RECT_TRIANGLES = 2;
    public static final int RECT_VERTICES = RECT_TRIANGLES * 3;

    // For rounded rect: 4 corners (quarter circles) + 5 rectangles (center + 4 edges)
    // Each corner is approximated with segments
    private static final int CORNER_SEGMENTS = 4;

    /**
     * Tessellates a filled rectangle into triangle vertices.
     *
     * @param vertices output array
     * @param offset starting index in array
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param r red (0-1)
     * @param g green (0-1)
     * @param b blue (0-1)
     * @param a alpha (0-1)
     * @return number of floats written
     */
    public static int tessellateRect(float[] vertices, int offset,
                                     float x, float y, float w, float h,
                                     float r, float g, float b, float a) {
        int idx = offset;

        // Two triangles for rectangle
        // Triangle 1: top-left, bottom-left, top-right
        idx = addVertex(vertices, idx, x, y, r, g, b, a);
        idx = addVertex(vertices, idx, x, y + h, r, g, b, a);
        idx = addVertex(vertices, idx, x + w, y, r, g, b, a);

        // Triangle 2: top-right, bottom-left, bottom-right
        idx = addVertex(vertices, idx, x + w, y, r, g, b, a);
        idx = addVertex(vertices, idx, x, y + h, r, g, b, a);
        idx = addVertex(vertices, idx, x + w, y + h, r, g, b, a);

        return idx - offset;
    }

    /**
     * Tessellates a filled rounded rectangle into triangle vertices.
     *
     * @param vertices output array
     * @param offset starting index in array
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param radius corner radius
     * @param r red (0-1)
     * @param g green (0-1)
     * @param b blue (0-1)
     * @param a alpha (0-1)
     * @return number of floats written
     */
    public static int tessellateRoundedRect(float[] vertices, int offset,
                                            float x, float y, float w, float h, float radius,
                                            float r, float g, float b, float a) {
        // Clamp radius to half the smaller dimension
        radius = Math.min(radius, Math.min(w, h) / 2);

        if (radius <= 0) {
            return tessellateRect(vertices, offset, x, y, w, h, r, g, b, a);
        }

        int idx = offset;

        // Center rectangle
        idx += tessellateRect(vertices, idx, x + radius, y + radius, w - 2 * radius, h - 2 * radius, r, g, b, a);

        // Top edge rectangle
        idx += tessellateRect(vertices, idx, x + radius, y, w - 2 * radius, radius, r, g, b, a);

        // Bottom edge rectangle
        idx += tessellateRect(vertices, idx, x + radius, y + h - radius, w - 2 * radius, radius, r, g, b, a);

        // Left edge rectangle
        idx += tessellateRect(vertices, idx, x, y + radius, radius, h - 2 * radius, r, g, b, a);

        // Right edge rectangle
        idx += tessellateRect(vertices, idx, x + w - radius, y + radius, radius, h - 2 * radius, r, g, b, a);

        // Four corners as pie slices (quarter circles)
        // Top-left corner
        idx += tessellateCorner(vertices, idx, x + radius, y + radius, radius, (float) Math.PI, r, g, b, a);

        // Top-right corner
        idx += tessellateCorner(vertices, idx, x + w - radius, y + radius, radius, (float) (1.5 * Math.PI), r, g, b, a);

        // Bottom-right corner
        idx += tessellateCorner(vertices, idx, x + w - radius, y + h - radius, radius, 0, r, g, b, a);

        // Bottom-left corner
        idx += tessellateCorner(vertices, idx, x + radius, y + h - radius, radius, (float) (0.5 * Math.PI), r, g, b, a);

        return idx - offset;
    }

    /**
     * Tessellates a quarter circle (90-degree arc) as triangle fan.
     */
    private static int tessellateCorner(float[] vertices, int idx,
                                        float cx, float cy, float radius, float startAngle,
                                        float r, float g, float b, float a) {
        int startIdx = idx;
        float angleStep = (float) (Math.PI / 2 / CORNER_SEGMENTS);

        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float angle1 = startAngle + i * angleStep;
            float angle2 = startAngle + (i + 1) * angleStep;

            float x1 = cx + (float) Math.cos(angle1) * radius;
            float y1 = cy + (float) Math.sin(angle1) * radius;
            float x2 = cx + (float) Math.cos(angle2) * radius;
            float y2 = cy + (float) Math.sin(angle2) * radius;

            // Triangle: center, edge1, edge2
            idx = addVertex(vertices, idx, cx, cy, r, g, b, a);
            idx = addVertex(vertices, idx, x1, y1, r, g, b, a);
            idx = addVertex(vertices, idx, x2, y2, r, g, b, a);
        }

        return idx - startIdx;
    }

    /**
     * Tessellates a rectangle border (outline only) into line vertices.
     * Uses LINE_LOOP or LINE_STRIP drawing mode.
     *
     * @param vertices output array
     * @param offset starting index in array
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param r red (0-1)
     * @param g green (0-1)
     * @param b blue (0-1)
     * @param a alpha (0-1)
     * @return number of floats written (4 vertices for LINE_LOOP)
     */
    public static int tessellateRectBorder(float[] vertices, int offset,
                                           float x, float y, float w, float h,
                                           float r, float g, float b, float a) {
        int idx = offset;

        // 4 corners for LINE_LOOP
        idx = addVertex(vertices, idx, x, y, r, g, b, a);
        idx = addVertex(vertices, idx, x + w, y, r, g, b, a);
        idx = addVertex(vertices, idx, x + w, y + h, r, g, b, a);
        idx = addVertex(vertices, idx, x, y + h, r, g, b, a);

        return idx - offset;
    }

    /**
     * Tessellates a horizontal line.
     *
     * @return number of floats written (2 vertices)
     */
    public static int tessellateHorizontalLine(float[] vertices, int offset,
                                               float x1, float x2, float y,
                                               float r, float g, float b, float a) {
        int idx = offset;
        idx = addVertex(vertices, idx, x1, y, r, g, b, a);
        idx = addVertex(vertices, idx, x2, y, r, g, b, a);
        return idx - offset;
    }

    /**
     * Tessellates a vertical line.
     *
     * @return number of floats written (2 vertices)
     */
    public static int tessellateVerticalLine(float[] vertices, int offset,
                                             float x, float y1, float y2,
                                             float r, float g, float b, float a) {
        int idx = offset;
        idx = addVertex(vertices, idx, x, y1, r, g, b, a);
        idx = addVertex(vertices, idx, x, y2, r, g, b, a);
        return idx - offset;
    }

    /**
     * Tessellates a filled circle.
     *
     * @param segments number of segments (higher = smoother)
     * @return number of floats written
     */
    public static int tessellateCircle(float[] vertices, int offset,
                                       float cx, float cy, float radius, int segments,
                                       float r, float g, float b, float a) {
        int idx = offset;
        float angleStep = (float) (2 * Math.PI / segments);

        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;

            float x1 = cx + (float) Math.cos(angle1) * radius;
            float y1 = cy + (float) Math.sin(angle1) * radius;
            float x2 = cx + (float) Math.cos(angle2) * radius;
            float y2 = cy + (float) Math.sin(angle2) * radius;

            // Triangle: center, edge1, edge2
            idx = addVertex(vertices, idx, cx, cy, r, g, b, a);
            idx = addVertex(vertices, idx, x1, y1, r, g, b, a);
            idx = addVertex(vertices, idx, x2, y2, r, g, b, a);
        }

        return idx - offset;
    }

    /**
     * Adds a single vertex to the array.
     *
     * @return new index after adding vertex
     */
    private static int addVertex(float[] vertices, int idx,
                                 float x, float y,
                                 float r, float g, float b, float a) {
        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = r;
        vertices[idx++] = g;
        vertices[idx++] = b;
        vertices[idx++] = a;
        return idx;
    }

    /**
     * Calculates the number of floats needed for a rounded rectangle.
     */
    public static int roundedRectFloatCount(float radius) {
        if (radius <= 0) {
            return RECT_VERTICES * FLOATS_PER_VERTEX;
        }
        // 5 rectangles + 4 corners with CORNER_SEGMENTS triangles each
        int triangles = 5 * RECT_TRIANGLES + 4 * CORNER_SEGMENTS;
        return triangles * 3 * FLOATS_PER_VERTEX;
    }

    /**
     * Calculates the number of floats needed for a simple rectangle.
     */
    public static int rectFloatCount() {
        return RECT_VERTICES * FLOATS_PER_VERTEX;
    }

    /**
     * Calculates the number of floats needed for a circle.
     */
    public static int circleFloatCount(int segments) {
        return segments * 3 * FLOATS_PER_VERTEX;
    }

    /**
     * Tessellates a filled triangle.
     *
     * @param vertices output array
     * @param offset starting index in array
     * @param x1, y1 first vertex
     * @param x2, y2 second vertex
     * @param x3, y3 third vertex
     * @param r red (0-1)
     * @param g green (0-1)
     * @param b blue (0-1)
     * @param a alpha (0-1)
     * @return number of floats written
     */
    public static int tessellateTriangle(float[] vertices, int offset,
                                         float x1, float y1,
                                         float x2, float y2,
                                         float x3, float y3,
                                         float r, float g, float b, float a) {
        int idx = offset;
        idx = addVertex(vertices, idx, x1, y1, r, g, b, a);
        idx = addVertex(vertices, idx, x2, y2, r, g, b, a);
        idx = addVertex(vertices, idx, x3, y3, r, g, b, a);
        return idx - offset;
    }

    /**
     * Calculates the number of floats needed for a triangle.
     */
    public static int triangleFloatCount() {
        return 3 * FLOATS_PER_VERTEX;
    }
}
