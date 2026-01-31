package com.apokalypsix.chartx.chart.data;

/**
 * Data for vector field visualizations.
 *
 * <p>Stores a 2D grid of vectors, where each cell has
 * a position (x, y) and a direction vector (dx, dy).
 */
public class VectorFieldData {

    private final String id;
    private final String name;

    /** Number of rows in the grid */
    private final int rows;

    /** Number of columns in the grid */
    private final int cols;

    /** X coordinates of grid points */
    private final double[] xCoords;

    /** Y coordinates of grid points */
    private final double[] yCoords;

    /** X components of vectors (row-major order) */
    private final float[] dx;

    /** Y components of vectors (row-major order) */
    private final float[] dy;

    /** Magnitude cache (computed on demand) */
    private float[] magnitudes;
    private float minMagnitude = Float.MAX_VALUE;
    private float maxMagnitude = Float.MIN_VALUE;
    private boolean magnitudesValid = false;

    /**
     * Creates a uniform vector field grid.
     *
     * @param id unique identifier
     * @param name display name
     * @param rows number of rows
     * @param cols number of columns
     * @param xMin minimum x coordinate
     * @param xMax maximum x coordinate
     * @param yMin minimum y coordinate
     * @param yMax maximum y coordinate
     */
    public VectorFieldData(String id, String name, int rows, int cols,
                           double xMin, double xMax, double yMin, double yMax) {
        this.id = id;
        this.name = name;
        this.rows = rows;
        this.cols = cols;

        // Create uniform grid
        this.xCoords = new double[cols];
        this.yCoords = new double[rows];

        double xStep = (xMax - xMin) / (cols - 1);
        double yStep = (yMax - yMin) / (rows - 1);

        for (int i = 0; i < cols; i++) {
            xCoords[i] = xMin + i * xStep;
        }
        for (int i = 0; i < rows; i++) {
            yCoords[i] = yMin + i * yStep;
        }

        int size = rows * cols;
        this.dx = new float[size];
        this.dy = new float[size];
    }

    /**
     * Creates a vector field with custom grid coordinates.
     *
     * @param id unique identifier
     * @param name display name
     * @param xCoords x coordinates (length = cols)
     * @param yCoords y coordinates (length = rows)
     */
    public VectorFieldData(String id, String name, double[] xCoords, double[] yCoords) {
        this.id = id;
        this.name = name;
        this.cols = xCoords.length;
        this.rows = yCoords.length;
        this.xCoords = xCoords.clone();
        this.yCoords = yCoords.clone();

        int size = rows * cols;
        this.dx = new float[size];
        this.dy = new float[size];
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public double[] getXCoords() {
        return xCoords;
    }

    public double[] getYCoords() {
        return yCoords;
    }

    public float[] getDxArray() {
        return dx;
    }

    public float[] getDyArray() {
        return dy;
    }

    // ========== Vector Access ==========

    /**
     * Returns the index for a given row and column.
     */
    public int index(int row, int col) {
        return row * cols + col;
    }

    /**
     * Sets the vector at the given grid position.
     */
    public void setVector(int row, int col, float dxValue, float dyValue) {
        int idx = index(row, col);
        dx[idx] = dxValue;
        dy[idx] = dyValue;
        magnitudesValid = false;
    }

    /**
     * Gets the X component of the vector at the given position.
     */
    public float getDx(int row, int col) {
        return dx[index(row, col)];
    }

    /**
     * Gets the Y component of the vector at the given position.
     */
    public float getDy(int row, int col) {
        return dy[index(row, col)];
    }

    /**
     * Gets the vector at the given position as [dx, dy].
     */
    public float[] getVector(int row, int col, float[] out) {
        int idx = index(row, col);
        out[0] = dx[idx];
        out[1] = dy[idx];
        return out;
    }

    /**
     * Gets the X coordinate at the given column.
     */
    public double getX(int col) {
        return xCoords[col];
    }

    /**
     * Gets the Y coordinate at the given row.
     */
    public double getY(int row) {
        return yCoords[row];
    }

    // ========== Magnitude ==========

    private void updateMagnitudes() {
        if (magnitudesValid) {
            return;
        }

        int size = rows * cols;
        if (magnitudes == null || magnitudes.length != size) {
            magnitudes = new float[size];
        }

        minMagnitude = Float.MAX_VALUE;
        maxMagnitude = Float.MIN_VALUE;

        for (int i = 0; i < size; i++) {
            float mag = (float) Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
            magnitudes[i] = mag;

            if (mag < minMagnitude) {
                minMagnitude = mag;
            }
            if (mag > maxMagnitude) {
                maxMagnitude = mag;
            }
        }

        magnitudesValid = true;
    }

    /**
     * Gets the magnitude at the given position.
     */
    public float getMagnitude(int row, int col) {
        updateMagnitudes();
        return magnitudes[index(row, col)];
    }

    /**
     * Gets the minimum magnitude in the field.
     */
    public float getMinMagnitude() {
        updateMagnitudes();
        return minMagnitude;
    }

    /**
     * Gets the maximum magnitude in the field.
     */
    public float getMaxMagnitude() {
        updateMagnitudes();
        return maxMagnitude;
    }

    /**
     * Gets the angle of the vector at the given position (radians).
     */
    public double getAngle(int row, int col) {
        int idx = index(row, col);
        return Math.atan2(dy[idx], dx[idx]);
    }

    // ========== Bulk Operations ==========

    /**
     * Sets all vectors using arrays of dx and dy values.
     */
    public void setVectors(float[] dxValues, float[] dyValues) {
        int size = Math.min(dxValues.length, rows * cols);
        System.arraycopy(dxValues, 0, dx, 0, size);
        System.arraycopy(dyValues, 0, dy, 0, size);
        magnitudesValid = false;
    }

    /**
     * Sets vectors using a function of position.
     */
    public void setVectorsFromFunction(VectorFunction function) {
        for (int row = 0; row < rows; row++) {
            double y = yCoords[row];
            for (int col = 0; col < cols; col++) {
                double x = xCoords[col];
                float[] vector = function.compute(x, y);
                int idx = index(row, col);
                dx[idx] = vector[0];
                dy[idx] = vector[1];
            }
        }
        magnitudesValid = false;
    }

    /**
     * Normalizes all vectors to unit length.
     */
    public void normalize() {
        for (int i = 0; i < dx.length; i++) {
            float mag = (float) Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
            if (mag > 0) {
                dx[i] /= mag;
                dy[i] /= mag;
            }
        }
        magnitudesValid = false;
    }

    /**
     * Scales all vectors by the given factor.
     */
    public void scale(float factor) {
        for (int i = 0; i < dx.length; i++) {
            dx[i] *= factor;
            dy[i] *= factor;
        }
        magnitudesValid = false;
    }

    // ========== Range Queries ==========

    /**
     * Returns the column range visible in the given x range.
     *
     * @param xMin minimum x
     * @param xMax maximum x
     * @return array of [startCol, endCol] (inclusive)
     */
    public int[] getVisibleColumnRange(double xMin, double xMax) {
        int startCol = 0;
        int endCol = cols - 1;

        // Binary search for start
        while (startCol < cols && xCoords[startCol] < xMin) {
            startCol++;
        }
        if (startCol > 0) startCol--;

        // Binary search for end
        while (endCol > 0 && xCoords[endCol] > xMax) {
            endCol--;
        }
        if (endCol < cols - 1) endCol++;

        return new int[]{Math.max(0, startCol), Math.min(cols - 1, endCol)};
    }

    /**
     * Returns the row range visible in the given y range.
     *
     * @param yMin minimum y
     * @param yMax maximum y
     * @return array of [startRow, endRow] (inclusive)
     */
    public int[] getVisibleRowRange(double yMin, double yMax) {
        int startRow = 0;
        int endRow = rows - 1;

        while (startRow < rows && yCoords[startRow] < yMin) {
            startRow++;
        }
        if (startRow > 0) startRow--;

        while (endRow > 0 && yCoords[endRow] > yMax) {
            endRow--;
        }
        if (endRow < rows - 1) endRow++;

        return new int[]{Math.max(0, startRow), Math.min(rows - 1, endRow)};
    }

    /**
     * Functional interface for computing vectors from position.
     */
    @FunctionalInterface
    public interface VectorFunction {
        /**
         * Computes the vector at the given position.
         *
         * @param x x coordinate
         * @param y y coordinate
         * @return array of [dx, dy]
         */
        float[] compute(double x, double y);
    }

    @Override
    public String toString() {
        return String.format("VectorFieldData[id=%s, rows=%d, cols=%d, magRange=%.2f-%.2f]",
                id, rows, cols, getMinMagnitude(), getMaxMagnitude());
    }
}
