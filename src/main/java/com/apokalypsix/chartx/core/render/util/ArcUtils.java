package com.apokalypsix.chartx.core.render.util;

/**
 * Utility class for arc and circle tessellation.
 *
 * <p>Provides methods to generate vertices for arcs, pie slices, donut segments,
 * and rings. All methods use float arrays for zero-allocation rendering.
 *
 * <p>Vertex format is position (x, y) followed by color (r, g, b, a),
 * totaling 6 floats per vertex.
 */
public final class ArcUtils {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private ArcUtils() {
        // Utility class
    }

    // ========== Arc Tessellation ==========

    /**
     * Generates vertices for an arc (line segments along a circle).
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param radius radius in pixels
     * @param startAngle start angle in radians
     * @param endAngle end angle in radians
     * @param segments number of line segments
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateArc(float[] vertices, int startIndex,
                                     float centerX, float centerY, float radius,
                                     double startAngle, double endAngle, int segments,
                                     float r, float g, float b, float a) {
        int index = startIndex;
        double angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = startAngle + i * angleStep;
            double angle2 = startAngle + (i + 1) * angleStep;

            float x1 = centerX + radius * (float) Math.cos(angle1);
            float y1 = centerY + radius * (float) Math.sin(angle1);
            float x2 = centerX + radius * (float) Math.cos(angle2);
            float y2 = centerY + radius * (float) Math.sin(angle2);

            index = addVertex(vertices, index, x1, y1, r, g, b, a);
            index = addVertex(vertices, index, x2, y2, r, g, b, a);
        }

        return index;
    }

    // ========== Pie Slice (Wedge) Generation ==========

    /**
     * Generates triangles for a filled pie slice (wedge from center).
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param radius radius in pixels
     * @param startAngle start angle in radians
     * @param endAngle end angle in radians
     * @param segments number of triangles
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellatePieSlice(float[] vertices, int startIndex,
                                          float centerX, float centerY, float radius,
                                          double startAngle, double endAngle, int segments,
                                          float r, float g, float b, float a) {
        int index = startIndex;
        double angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = startAngle + i * angleStep;
            double angle2 = startAngle + (i + 1) * angleStep;

            float x1 = centerX + radius * (float) Math.cos(angle1);
            float y1 = centerY + radius * (float) Math.sin(angle1);
            float x2 = centerX + radius * (float) Math.cos(angle2);
            float y2 = centerY + radius * (float) Math.sin(angle2);

            // Triangle: center -> edge1 -> edge2
            index = addVertex(vertices, index, centerX, centerY, r, g, b, a);
            index = addVertex(vertices, index, x1, y1, r, g, b, a);
            index = addVertex(vertices, index, x2, y2, r, g, b, a);
        }

        return index;
    }

    /**
     * Generates triangles for a pie slice with an exploded offset.
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param radius radius in pixels
     * @param startAngle start angle in radians
     * @param endAngle end angle in radians
     * @param segments number of triangles
     * @param explodeOffset offset distance from center
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateExplodedPieSlice(float[] vertices, int startIndex,
                                                   float centerX, float centerY, float radius,
                                                   double startAngle, double endAngle, int segments,
                                                   float explodeOffset,
                                                   float r, float g, float b, float a) {
        // Calculate the center angle and offset the slice
        double midAngle = (startAngle + endAngle) / 2;
        float offsetX = explodeOffset * (float) Math.cos(midAngle);
        float offsetY = explodeOffset * (float) Math.sin(midAngle);

        return tessellatePieSlice(vertices, startIndex,
                centerX + offsetX, centerY + offsetY, radius,
                startAngle, endAngle, segments, r, g, b, a);
    }

    // ========== Donut Segment Generation ==========

    /**
     * Generates triangles for a donut segment (annular sector).
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param innerRadius inner radius in pixels
     * @param outerRadius outer radius in pixels
     * @param startAngle start angle in radians
     * @param endAngle end angle in radians
     * @param segments number of segments (each produces 2 triangles)
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateDonutSegment(float[] vertices, int startIndex,
                                               float centerX, float centerY,
                                               float innerRadius, float outerRadius,
                                               double startAngle, double endAngle, int segments,
                                               float r, float g, float b, float a) {
        int index = startIndex;
        double angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = startAngle + i * angleStep;
            double angle2 = startAngle + (i + 1) * angleStep;

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            // Inner edge points
            float innerX1 = centerX + innerRadius * cos1;
            float innerY1 = centerY + innerRadius * sin1;
            float innerX2 = centerX + innerRadius * cos2;
            float innerY2 = centerY + innerRadius * sin2;

            // Outer edge points
            float outerX1 = centerX + outerRadius * cos1;
            float outerY1 = centerY + outerRadius * sin1;
            float outerX2 = centerX + outerRadius * cos2;
            float outerY2 = centerY + outerRadius * sin2;

            // Triangle 1: inner1 -> outer1 -> outer2
            index = addVertex(vertices, index, innerX1, innerY1, r, g, b, a);
            index = addVertex(vertices, index, outerX1, outerY1, r, g, b, a);
            index = addVertex(vertices, index, outerX2, outerY2, r, g, b, a);

            // Triangle 2: inner1 -> outer2 -> inner2
            index = addVertex(vertices, index, innerX1, innerY1, r, g, b, a);
            index = addVertex(vertices, index, outerX2, outerY2, r, g, b, a);
            index = addVertex(vertices, index, innerX2, innerY2, r, g, b, a);
        }

        return index;
    }

    /**
     * Generates triangles for an exploded donut segment.
     */
    public static int tessellateExplodedDonutSegment(float[] vertices, int startIndex,
                                                       float centerX, float centerY,
                                                       float innerRadius, float outerRadius,
                                                       double startAngle, double endAngle, int segments,
                                                       float explodeOffset,
                                                       float r, float g, float b, float a) {
        double midAngle = (startAngle + endAngle) / 2;
        float offsetX = explodeOffset * (float) Math.cos(midAngle);
        float offsetY = explodeOffset * (float) Math.sin(midAngle);

        return tessellateDonutSegment(vertices, startIndex,
                centerX + offsetX, centerY + offsetY,
                innerRadius, outerRadius,
                startAngle, endAngle, segments, r, g, b, a);
    }

    // ========== Ring/Annulus Generation ==========

    /**
     * Generates triangles for a full ring (annulus).
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param innerRadius inner radius in pixels
     * @param outerRadius outer radius in pixels
     * @param segments number of segments around the ring
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateRing(float[] vertices, int startIndex,
                                      float centerX, float centerY,
                                      float innerRadius, float outerRadius, int segments,
                                      float r, float g, float b, float a) {
        return tessellateDonutSegment(vertices, startIndex,
                centerX, centerY, innerRadius, outerRadius,
                0, 2 * Math.PI, segments, r, g, b, a);
    }

    /**
     * Generates triangles for a filled circle.
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param radius radius in pixels
     * @param segments number of triangles
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateCircle(float[] vertices, int startIndex,
                                        float centerX, float centerY, float radius, int segments,
                                        float r, float g, float b, float a) {
        return tessellatePieSlice(vertices, startIndex,
                centerX, centerY, radius, 0, 2 * Math.PI, segments, r, g, b, a);
    }

    // ========== Border/Outline Generation ==========

    /**
     * Generates line vertices for a pie slice border (outline).
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param radius radius in pixels
     * @param startAngle start angle in radians
     * @param endAngle end angle in radians
     * @param arcSegments number of segments for the arc
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellatePieSliceBorder(float[] vertices, int startIndex,
                                                 float centerX, float centerY, float radius,
                                                 double startAngle, double endAngle, int arcSegments,
                                                 float r, float g, float b, float a) {
        int index = startIndex;

        // Calculate edge points
        float x1 = centerX + radius * (float) Math.cos(startAngle);
        float y1 = centerY + radius * (float) Math.sin(startAngle);
        float x2 = centerX + radius * (float) Math.cos(endAngle);
        float y2 = centerY + radius * (float) Math.sin(endAngle);

        // Line from center to start
        index = addVertex(vertices, index, centerX, centerY, r, g, b, a);
        index = addVertex(vertices, index, x1, y1, r, g, b, a);

        // Arc along the edge
        index = tessellateArc(vertices, index, centerX, centerY, radius,
                startAngle, endAngle, arcSegments, r, g, b, a);

        // Line from end back to center
        index = addVertex(vertices, index, x2, y2, r, g, b, a);
        index = addVertex(vertices, index, centerX, centerY, r, g, b, a);

        return index;
    }

    /**
     * Generates line vertices for a donut segment border.
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param innerRadius inner radius in pixels
     * @param outerRadius outer radius in pixels
     * @param startAngle start angle in radians
     * @param endAngle end angle in radians
     * @param arcSegments number of segments for each arc
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateDonutSegmentBorder(float[] vertices, int startIndex,
                                                     float centerX, float centerY,
                                                     float innerRadius, float outerRadius,
                                                     double startAngle, double endAngle, int arcSegments,
                                                     float r, float g, float b, float a) {
        int index = startIndex;

        // Calculate corner points
        float innerX1 = centerX + innerRadius * (float) Math.cos(startAngle);
        float innerY1 = centerY + innerRadius * (float) Math.sin(startAngle);
        float innerX2 = centerX + innerRadius * (float) Math.cos(endAngle);
        float innerY2 = centerY + innerRadius * (float) Math.sin(endAngle);
        float outerX1 = centerX + outerRadius * (float) Math.cos(startAngle);
        float outerY1 = centerY + outerRadius * (float) Math.sin(startAngle);
        float outerX2 = centerX + outerRadius * (float) Math.cos(endAngle);
        float outerY2 = centerY + outerRadius * (float) Math.sin(endAngle);

        // Start edge (inner to outer)
        index = addVertex(vertices, index, innerX1, innerY1, r, g, b, a);
        index = addVertex(vertices, index, outerX1, outerY1, r, g, b, a);

        // Outer arc
        index = tessellateArc(vertices, index, centerX, centerY, outerRadius,
                startAngle, endAngle, arcSegments, r, g, b, a);

        // End edge (outer to inner)
        index = addVertex(vertices, index, outerX2, outerY2, r, g, b, a);
        index = addVertex(vertices, index, innerX2, innerY2, r, g, b, a);

        // Inner arc (reverse direction)
        index = tessellateArc(vertices, index, centerX, centerY, innerRadius,
                endAngle, startAngle, arcSegments, r, g, b, a);

        return index;
    }

    // ========== Gauge Arc Generation ==========

    /**
     * Generates triangles for a gauge arc (thick arc segment).
     *
     * @param vertices output array for vertices
     * @param startIndex starting index in the output array
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param innerRadius inner radius in pixels
     * @param outerRadius outer radius in pixels
     * @param startAngle start angle in radians
     * @param sweepAngle sweep angle in radians (can be negative)
     * @param segments number of segments
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     * @return the new index after adding vertices
     */
    public static int tessellateGaugeArc(float[] vertices, int startIndex,
                                          float centerX, float centerY,
                                          float innerRadius, float outerRadius,
                                          double startAngle, double sweepAngle, int segments,
                                          float r, float g, float b, float a) {
        return tessellateDonutSegment(vertices, startIndex,
                centerX, centerY, innerRadius, outerRadius,
                startAngle, startAngle + sweepAngle, segments, r, g, b, a);
    }

    // ========== Utility Methods ==========

    /**
     * Calculates the number of segments needed for a smooth arc.
     *
     * @param radius radius in pixels
     * @param angleSpan angle span in radians
     * @param maxSegmentLength maximum segment length in pixels
     * @return recommended number of segments
     */
    public static int calculateSegments(float radius, double angleSpan, float maxSegmentLength) {
        double arcLength = radius * Math.abs(angleSpan);
        int segments = (int) Math.ceil(arcLength / maxSegmentLength);
        return Math.max(1, Math.min(segments, 360)); // Clamp to reasonable range
    }

    /**
     * Calculates the number of segments based on angle span (1 segment per ~6 degrees).
     *
     * @param angleSpan angle span in radians
     * @return recommended number of segments
     */
    public static int calculateSegments(double angleSpan) {
        int segments = (int) Math.ceil(Math.abs(angleSpan) / (Math.PI / 30)); // ~6 degrees per segment
        return Math.max(1, Math.min(segments, 60));
    }

    /**
     * Returns the number of floats required for pie slice triangles.
     */
    public static int getPieSliceFloatCount(int segments) {
        return segments * 3 * FLOATS_PER_VERTEX;
    }

    /**
     * Returns the number of floats required for donut segment triangles.
     */
    public static int getDonutSegmentFloatCount(int segments) {
        return segments * 6 * FLOATS_PER_VERTEX; // 2 triangles per segment
    }

    /**
     * Returns the number of floats required for arc lines.
     */
    public static int getArcFloatCount(int segments) {
        return segments * 2 * FLOATS_PER_VERTEX;
    }

    private static int addVertex(float[] vertices, int index, float x, float y,
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
