package com.apokalypsix.chartx.core.render.util;

/**
 * Utility class for spline and Bezier curve tessellation.
 *
 * <p>Provides methods for generating smooth curves from data points using
 * Catmull-Rom splines and cubic Bezier curves. All methods output to
 * pre-allocated float arrays to avoid allocation in rendering loops.
 *
 * <p>This class is stateless and all methods are static for performance.
 */
public final class CurveUtils {

    /** Default tension for Catmull-Rom splines (0.5 = standard Catmull-Rom) */
    public static final float DEFAULT_TENSION = 0.5f;

    /** Minimum segments per curve segment for tessellation */
    private static final int MIN_SEGMENTS = 2;

    /** Maximum segments per curve segment for tessellation */
    private static final int MAX_SEGMENTS = 32;

    private CurveUtils() {
        // Utility class
    }

    // ========== Catmull-Rom Spline ==========

    /**
     * Computes the number of floats needed for Catmull-Rom tessellation output.
     *
     * @param pointCount number of input points
     * @param segmentsPerCurve segments to generate between each pair of points
     * @return number of floats needed in output array (x,y pairs)
     */
    public static int catmullRomOutputSize(int pointCount, int segmentsPerCurve) {
        if (pointCount < 2) {
            return pointCount * 2;
        }
        // For N points, we have N-1 segments, each with segmentsPerCurve divisions
        // Plus the final point
        int totalPoints = (pointCount - 1) * segmentsPerCurve + 1;
        return totalPoints * 2;
    }

    /**
     * Tessellates a Catmull-Rom spline from the input points.
     *
     * <p>The spline passes through all input points with smooth interpolation.
     * Uses default tension of 0.5 (standard Catmull-Rom).
     *
     * @param inputX array of x coordinates
     * @param inputY array of y coordinates
     * @param startIdx starting index in input arrays
     * @param count number of points to process
     * @param segmentsPerCurve number of segments between each pair of points
     * @param output output array for tessellated points (x,y,x,y,...)
     * @param outputOffset starting index in output array
     * @return number of floats written to output
     */
    public static int catmullRom(
            float[] inputX, float[] inputY,
            int startIdx, int count,
            int segmentsPerCurve,
            float[] output, int outputOffset) {
        return catmullRom(inputX, inputY, startIdx, count, segmentsPerCurve,
                DEFAULT_TENSION, output, outputOffset);
    }

    /**
     * Tessellates a Catmull-Rom spline with configurable tension.
     *
     * @param inputX array of x coordinates
     * @param inputY array of y coordinates
     * @param startIdx starting index in input arrays
     * @param count number of points to process
     * @param segmentsPerCurve number of segments between each pair of points
     * @param tension spline tension (0.0 = tight, 0.5 = standard, 1.0 = loose)
     * @param output output array for tessellated points
     * @param outputOffset starting index in output array
     * @return number of floats written to output
     */
    public static int catmullRom(
            float[] inputX, float[] inputY,
            int startIdx, int count,
            int segmentsPerCurve,
            float tension,
            float[] output, int outputOffset) {

        if (count < 2) {
            if (count == 1) {
                output[outputOffset] = inputX[startIdx];
                output[outputOffset + 1] = inputY[startIdx];
                return 2;
            }
            return 0;
        }

        segmentsPerCurve = Math.max(MIN_SEGMENTS, Math.min(MAX_SEGMENTS, segmentsPerCurve));
        int outIdx = outputOffset;

        // Process each segment between consecutive points
        for (int i = 0; i < count - 1; i++) {
            int idx = startIdx + i;

            // Get four control points (p0, p1, p2, p3)
            // For edge cases, duplicate the endpoint
            float x0 = (i > 0) ? inputX[idx - 1] : inputX[idx];
            float y0 = (i > 0) ? inputY[idx - 1] : inputY[idx];

            float x1 = inputX[idx];
            float y1 = inputY[idx];

            float x2 = inputX[idx + 1];
            float y2 = inputY[idx + 1];

            float x3 = (i < count - 2) ? inputX[idx + 2] : inputX[idx + 1];
            float y3 = (i < count - 2) ? inputY[idx + 2] : inputY[idx + 1];

            // Generate points along this segment
            int steps = (i == count - 2) ? segmentsPerCurve : segmentsPerCurve;
            for (int s = 0; s < steps; s++) {
                float t = (float) s / segmentsPerCurve;

                // Catmull-Rom basis functions with tension
                float t2 = t * t;
                float t3 = t2 * t;

                // Tension adjustment (standard Catmull-Rom has tension = 0.5)
                float c = tension;

                // Basis coefficients
                float b0 = -c * t + 2 * c * t2 - c * t3;
                float b1 = 1 + (c - 3) * t2 + (2 - c) * t3;
                float b2 = c * t + (3 - 2 * c) * t2 + (c - 2) * t3;
                float b3 = -c * t2 + c * t3;

                output[outIdx++] = x0 * b0 + x1 * b1 + x2 * b2 + x3 * b3;
                output[outIdx++] = y0 * b0 + y1 * b1 + y2 * b2 + y3 * b3;
            }
        }

        // Add final point
        int lastIdx = startIdx + count - 1;
        output[outIdx++] = inputX[lastIdx];
        output[outIdx++] = inputY[lastIdx];

        return outIdx - outputOffset;
    }

    // ========== Cubic Bezier ==========

    /**
     * Computes the number of floats needed for cubic Bezier tessellation output.
     *
     * @param controlPointCount number of control points (must be 4)
     * @param segments number of segments to tessellate
     * @return number of floats needed in output array
     */
    public static int bezierOutputSize(int controlPointCount, int segments) {
        if (controlPointCount != 4) {
            throw new IllegalArgumentException("Cubic Bezier requires exactly 4 control points");
        }
        return (segments + 1) * 2;
    }

    /**
     * Tessellates a cubic Bezier curve.
     *
     * @param p0x x coordinate of first control point (start)
     * @param p0y y coordinate of first control point (start)
     * @param p1x x coordinate of second control point (first handle)
     * @param p1y y coordinate of second control point (first handle)
     * @param p2x x coordinate of third control point (second handle)
     * @param p2y y coordinate of third control point (second handle)
     * @param p3x x coordinate of fourth control point (end)
     * @param p3y y coordinate of fourth control point (end)
     * @param segments number of line segments to generate
     * @param output output array for points
     * @param outputOffset starting index in output array
     * @return number of floats written to output
     */
    public static int cubicBezier(
            float p0x, float p0y,
            float p1x, float p1y,
            float p2x, float p2y,
            float p3x, float p3y,
            int segments,
            float[] output, int outputOffset) {

        segments = Math.max(MIN_SEGMENTS, Math.min(MAX_SEGMENTS, segments));
        int outIdx = outputOffset;

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float u = 1 - t;
            float u2 = u * u;
            float u3 = u2 * u;
            float t2 = t * t;
            float t3 = t2 * t;

            // Cubic Bezier formula: B(t) = (1-t)^3*P0 + 3*(1-t)^2*t*P1 + 3*(1-t)*t^2*P2 + t^3*P3
            output[outIdx++] = u3 * p0x + 3 * u2 * t * p1x + 3 * u * t2 * p2x + t3 * p3x;
            output[outIdx++] = u3 * p0y + 3 * u2 * t * p1y + 3 * u * t2 * p2y + t3 * p3y;
        }

        return outIdx - outputOffset;
    }

    // ========== Adaptive Bezier Tessellation ==========

    /**
     * Tessellates a cubic Bezier curve with adaptive subdivision based on curvature.
     *
     * <p>Adds more points where the curve bends sharply and fewer where it's nearly straight.
     * This produces more efficient tessellation than fixed segment counts.
     *
     * @param p0x x coordinate of first control point
     * @param p0y y coordinate of first control point
     * @param p1x x coordinate of second control point
     * @param p1y y coordinate of second control point
     * @param p2x x coordinate of third control point
     * @param p2y y coordinate of third control point
     * @param p3x x coordinate of fourth control point
     * @param p3y y coordinate of fourth control point
     * @param tolerance maximum deviation from true curve in pixels
     * @param output output array for points
     * @param outputOffset starting index in output array
     * @param maxPoints maximum points to generate (safety limit)
     * @return number of floats written to output
     */
    public static int adaptiveBezier(
            float p0x, float p0y,
            float p1x, float p1y,
            float p2x, float p2y,
            float p3x, float p3y,
            float tolerance,
            float[] output, int outputOffset,
            int maxPoints) {

        // Start with the first point
        output[outputOffset] = p0x;
        output[outputOffset + 1] = p0y;

        int[] outIdx = new int[] { outputOffset + 2 };
        int maxFloats = outputOffset + maxPoints * 2;

        adaptiveBezierRecursive(
                p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y,
                tolerance * tolerance, // Use squared tolerance to avoid sqrt
                output, outIdx, maxFloats, 0);

        return outIdx[0] - outputOffset;
    }

    private static void adaptiveBezierRecursive(
            float p0x, float p0y,
            float p1x, float p1y,
            float p2x, float p2y,
            float p3x, float p3y,
            float toleranceSquared,
            float[] output, int[] outIdx, int maxFloats, int depth) {

        // Safety limit for recursion depth
        if (depth > 10 || outIdx[0] >= maxFloats - 2) {
            output[outIdx[0]++] = p3x;
            output[outIdx[0]++] = p3y;
            return;
        }

        // Calculate midpoints using de Casteljau's algorithm
        float p01x = (p0x + p1x) * 0.5f;
        float p01y = (p0y + p1y) * 0.5f;
        float p12x = (p1x + p2x) * 0.5f;
        float p12y = (p1y + p2y) * 0.5f;
        float p23x = (p2x + p3x) * 0.5f;
        float p23y = (p2y + p3y) * 0.5f;

        float p012x = (p01x + p12x) * 0.5f;
        float p012y = (p01y + p12y) * 0.5f;
        float p123x = (p12x + p23x) * 0.5f;
        float p123y = (p12y + p23y) * 0.5f;

        float p0123x = (p012x + p123x) * 0.5f;
        float p0123y = (p012y + p123y) * 0.5f;

        // Check flatness: distance from control points to line p0-p3
        float dx = p3x - p0x;
        float dy = p3y - p0y;
        float lenSq = dx * dx + dy * dy;

        float d1, d2;
        if (lenSq < 1e-10f) {
            // Degenerate case: start and end are the same point
            d1 = distanceSquared(p1x, p1y, p0x, p0y);
            d2 = distanceSquared(p2x, p2y, p0x, p0y);
        } else {
            // Distance from control points to line
            d1 = crossProductMagnitudeSquared(p1x - p0x, p1y - p0y, dx, dy) / lenSq;
            d2 = crossProductMagnitudeSquared(p2x - p0x, p2y - p0y, dx, dy) / lenSq;
        }

        // If flat enough, just add the endpoint
        if (d1 <= toleranceSquared && d2 <= toleranceSquared) {
            output[outIdx[0]++] = p3x;
            output[outIdx[0]++] = p3y;
            return;
        }

        // Subdivide
        adaptiveBezierRecursive(p0x, p0y, p01x, p01y, p012x, p012y, p0123x, p0123y,
                toleranceSquared, output, outIdx, maxFloats, depth + 1);
        adaptiveBezierRecursive(p0123x, p0123y, p123x, p123y, p23x, p23y, p3x, p3y,
                toleranceSquared, output, outIdx, maxFloats, depth + 1);
    }

    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    private static float crossProductMagnitudeSquared(float ax, float ay, float bx, float by) {
        float cross = ax * by - ay * bx;
        return cross * cross;
    }

    // ========== Utility: Convert XY data to Catmull-Rom control points ==========

    /**
     * Calculates cubic Bezier control points from data points for smooth curve fitting.
     *
     * <p>This converts N data points into N-1 sets of 4 Bezier control points,
     * which can then be tessellated. The resulting curve passes through all
     * original data points with C1 continuity.
     *
     * @param dataX array of x coordinates
     * @param dataY array of y coordinates
     * @param startIdx starting index
     * @param count number of points
     * @param tension tension factor (0.0 = tight, 1.0 = loose, 0.33 = typical)
     * @param controlPoints output array for control points (requires (count-1)*8 floats)
     * @param controlOffset starting index in output
     * @return number of floats written (number of curves * 8)
     */
    public static int calculateBezierControlPoints(
            float[] dataX, float[] dataY,
            int startIdx, int count,
            float tension,
            float[] controlPoints, int controlOffset) {

        if (count < 2) {
            return 0;
        }

        int outIdx = controlOffset;

        for (int i = 0; i < count - 1; i++) {
            int idx = startIdx + i;

            // Data points for this segment
            float x1 = dataX[idx];
            float y1 = dataY[idx];
            float x2 = dataX[idx + 1];
            float y2 = dataY[idx + 1];

            // Calculate tangent vectors using neighboring points
            float dx1, dy1, dx2, dy2;

            if (i > 0) {
                dx1 = (x2 - dataX[idx - 1]) * tension;
                dy1 = (y2 - dataY[idx - 1]) * tension;
            } else {
                dx1 = (x2 - x1) * tension;
                dy1 = (y2 - y1) * tension;
            }

            if (i < count - 2) {
                dx2 = (dataX[idx + 2] - x1) * tension;
                dy2 = (dataY[idx + 2] - y1) * tension;
            } else {
                dx2 = (x2 - x1) * tension;
                dy2 = (y2 - y1) * tension;
            }

            // Control points: P0, P0+tangent/3, P1-tangent/3, P1
            controlPoints[outIdx++] = x1;           // P0.x
            controlPoints[outIdx++] = y1;           // P0.y
            controlPoints[outIdx++] = x1 + dx1 / 3; // C1.x
            controlPoints[outIdx++] = y1 + dy1 / 3; // C1.y
            controlPoints[outIdx++] = x2 - dx2 / 3; // C2.x
            controlPoints[outIdx++] = y2 - dy2 / 3; // C2.y
            controlPoints[outIdx++] = x2;           // P1.x
            controlPoints[outIdx++] = y2;           // P1.y
        }

        return outIdx - controlOffset;
    }
}
