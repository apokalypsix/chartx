package com.apokalypsix.chartx.core.render.util;

/**
 * Marching Squares algorithm for contour line extraction from 2D grid data.
 *
 * <p>Generates line segments representing iso-contour lines at specified
 * threshold values. Uses linear interpolation for smooth contour placement.
 *
 * <p>The output is a series of line segment endpoints suitable for GL_LINES rendering.
 */
public final class MarchingSquares {

    private MarchingSquares() {
        // Utility class
    }

    /**
     * Lookup table for edge segments based on cell configuration.
     * Each cell can have 0, 1, or 2 line segments passing through.
     * The configuration is a 4-bit number based on which corners are above threshold.
     * Each entry lists the edges to connect: -1 = no more segments,
     * pairs of edge indices (0=bottom, 1=right, 2=top, 3=left).
     */
    private static final int[][] EDGE_TABLE = {
            {},                  // 0: all below
            {3, 0},              // 1: bottom-left above
            {0, 1},              // 2: bottom-right above
            {3, 1},              // 3: bottom row above
            {1, 2},              // 4: top-right above
            {3, 0, 1, 2},        // 5: bottom-left and top-right above (saddle)
            {0, 2},              // 6: right column above
            {3, 2},              // 7: all but top-left above
            {2, 3},              // 8: top-left above
            {2, 0},              // 9: left column above
            {0, 1, 2, 3},        // 10: bottom-right and top-left above (saddle)
            {2, 1},              // 11: all but top-right above
            {1, 3},              // 12: top row above
            {1, 0},              // 13: all but bottom-right above
            {0, 3},              // 14: all but bottom-left above
            {}                   // 15: all above
    };

    /**
     * Extracts contour lines at the given threshold from grid data.
     *
     * @param values grid values in row-major order
     * @param rows number of rows
     * @param cols number of columns
     * @param xCoords X coordinates for each column
     * @param yCoords Y coordinates for each row
     * @param threshold the iso-value for the contour
     * @param output output array for line segments [x1, y1, x2, y2, ...]
     * @param outputOffset starting offset in output array
     * @return number of floats written (divide by 4 for segment count)
     */
    public static int extractContour(
            float[] values, int rows, int cols,
            double[] xCoords, double[] yCoords,
            float threshold,
            float[] output, int outputOffset) {

        int floatIndex = outputOffset;

        // Process each cell (formed by 2x2 grid points)
        for (int row = 0; row < rows - 1; row++) {
            for (int col = 0; col < cols - 1; col++) {
                // Get the four corner values
                float v00 = values[row * cols + col];           // bottom-left
                float v10 = values[row * cols + col + 1];       // bottom-right
                float v01 = values[(row + 1) * cols + col];     // top-left
                float v11 = values[(row + 1) * cols + col + 1]; // top-right

                // Skip cells with NaN values
                if (Float.isNaN(v00) || Float.isNaN(v10) || Float.isNaN(v01) || Float.isNaN(v11)) {
                    continue;
                }

                // Determine cell configuration (which corners are above threshold)
                int config = 0;
                if (v00 >= threshold) config |= 1;
                if (v10 >= threshold) config |= 2;
                if (v11 >= threshold) config |= 4;
                if (v01 >= threshold) config |= 8;

                // Get the edges to connect for this configuration
                int[] edges = EDGE_TABLE[config];
                if (edges.length == 0) {
                    continue;
                }

                // Cell coordinates
                double x0 = xCoords[col];
                double x1 = xCoords[col + 1];
                double y0 = yCoords[row];
                double y1 = yCoords[row + 1];

                // Process each pair of edges to create line segments
                for (int i = 0; i < edges.length; i += 2) {
                    int edge1 = edges[i];
                    int edge2 = edges[i + 1];

                    // Get interpolated points on each edge
                    float[] p1 = getEdgePoint(edge1, v00, v10, v01, v11, threshold,
                            (float) x0, (float) x1, (float) y0, (float) y1);
                    float[] p2 = getEdgePoint(edge2, v00, v10, v01, v11, threshold,
                            (float) x0, (float) x1, (float) y0, (float) y1);

                    output[floatIndex++] = p1[0];
                    output[floatIndex++] = p1[1];
                    output[floatIndex++] = p2[0];
                    output[floatIndex++] = p2[1];
                }
            }
        }

        return floatIndex - outputOffset;
    }

    /**
     * Extracts contour lines for multiple threshold levels.
     *
     * @param values grid values
     * @param rows number of rows
     * @param cols number of columns
     * @param xCoords X coordinates
     * @param yCoords Y coordinates
     * @param thresholds array of threshold values
     * @param output output array
     * @param outputOffset starting offset
     * @param segmentCounts output: number of floats for each threshold level
     * @return total number of floats written
     */
    public static int extractContours(
            float[] values, int rows, int cols,
            double[] xCoords, double[] yCoords,
            float[] thresholds,
            float[] output, int outputOffset,
            int[] segmentCounts) {

        int totalFloats = 0;
        int currentOffset = outputOffset;

        for (int i = 0; i < thresholds.length; i++) {
            int floatsWritten = extractContour(values, rows, cols, xCoords, yCoords,
                    thresholds[i], output, currentOffset);
            segmentCounts[i] = floatsWritten;
            currentOffset += floatsWritten;
            totalFloats += floatsWritten;
        }

        return totalFloats;
    }

    /**
     * Estimates the maximum output size for contour extraction.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param levelCount number of threshold levels
     * @return maximum number of floats that could be generated
     */
    public static int estimateOutputSize(int rows, int cols, int levelCount) {
        // Each cell can produce at most 2 segments (8 floats)
        // Multiplied by number of cells and levels
        int cells = (rows - 1) * (cols - 1);
        return cells * 8 * levelCount;
    }

    /**
     * Gets the interpolated point on the specified edge.
     *
     * @param edge edge index (0=bottom, 1=right, 2=top, 3=left)
     * @param v00 bottom-left value
     * @param v10 bottom-right value
     * @param v01 top-left value
     * @param v11 top-right value
     * @param threshold threshold value
     * @param x0 left x
     * @param x1 right x
     * @param y0 bottom y
     * @param y1 top y
     * @return float[2] with {x, y} of interpolated point
     */
    private static float[] getEdgePoint(int edge,
                                         float v00, float v10, float v01, float v11,
                                         float threshold,
                                         float x0, float x1, float y0, float y1) {
        float[] point = new float[2];

        switch (edge) {
            case 0: // Bottom edge (v00 to v10)
                float t0 = interpolate(v00, v10, threshold);
                point[0] = lerp(x0, x1, t0);
                point[1] = y0;
                break;
            case 1: // Right edge (v10 to v11)
                float t1 = interpolate(v10, v11, threshold);
                point[0] = x1;
                point[1] = lerp(y0, y1, t1);
                break;
            case 2: // Top edge (v01 to v11)
                float t2 = interpolate(v01, v11, threshold);
                point[0] = lerp(x0, x1, t2);
                point[1] = y1;
                break;
            case 3: // Left edge (v00 to v01)
                float t3 = interpolate(v00, v01, threshold);
                point[0] = x0;
                point[1] = lerp(y0, y1, t3);
                break;
        }

        return point;
    }

    /**
     * Computes the interpolation factor for where the threshold crosses between two values.
     */
    private static float interpolate(float v1, float v2, float threshold) {
        if (Math.abs(v2 - v1) < 1e-10f) {
            return 0.5f;
        }
        return (threshold - v1) / (v2 - v1);
    }

    /**
     * Linear interpolation.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
