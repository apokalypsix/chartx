package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;

import java.util.Arrays;

/**
 * 2D grid data for heatmap charts.
 *
 * <p>Stores a 2D array of values with associated row and column coordinates.
 * Supports both uniform grids (regular spacing) and non-uniform grids
 * (variable cell sizes).
 *
 * <p>The data is stored in row-major order for cache-friendly access
 * when iterating by rows.
 */
public class HeatmapData {

    private final String id;
    private final String name;

    /** Number of rows (Y dimension) */
    private int rows;

    /** Number of columns (X dimension) */
    private int cols;

    /** Values in row-major order: values[row * cols + col] */
    private float[] values;

    /** X coordinates for each column (length = cols) */
    private double[] xCoords;

    /** Y coordinates for each row (length = rows) */
    private double[] yCoords;

    /** Whether the grid has uniform spacing */
    private boolean uniform;

    /** Cached min/max values */
    private float cachedMinValue = Float.NaN;
    private float cachedMaxValue = Float.NaN;

    /** Listener support */
    private final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates a uniform heatmap with the given dimensions and coordinate range.
     *
     * @param id unique identifier
     * @param name display name
     * @param rows number of rows
     * @param cols number of columns
     * @param xMin minimum X coordinate
     * @param xMax maximum X coordinate
     * @param yMin minimum Y coordinate
     * @param yMax maximum Y coordinate
     */
    public HeatmapData(String id, String name, int rows, int cols,
                       double xMin, double xMax, double yMin, double yMax) {
        this.id = id;
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.values = new float[rows * cols];
        this.uniform = true;

        // Generate uniform coordinates
        this.xCoords = new double[cols];
        this.yCoords = new double[rows];

        double xStep = cols > 1 ? (xMax - xMin) / (cols - 1) : 0;
        double yStep = rows > 1 ? (yMax - yMin) / (rows - 1) : 0;

        for (int c = 0; c < cols; c++) {
            xCoords[c] = xMin + c * xStep;
        }
        for (int r = 0; r < rows; r++) {
            yCoords[r] = yMin + r * yStep;
        }
    }

    /**
     * Creates a non-uniform heatmap with explicit coordinates.
     *
     * @param id unique identifier
     * @param name display name
     * @param xCoords X coordinates for each column
     * @param yCoords Y coordinates for each row
     */
    public HeatmapData(String id, String name, double[] xCoords, double[] yCoords) {
        this.id = id;
        this.name = name;
        this.cols = xCoords.length;
        this.rows = yCoords.length;
        this.values = new float[rows * cols];
        this.xCoords = Arrays.copyOf(xCoords, cols);
        this.yCoords = Arrays.copyOf(yCoords, rows);
        this.uniform = false;
    }

    // ========== Basic Accessors ==========

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

    public int size() {
        return rows * cols;
    }

    public boolean isEmpty() {
        return rows == 0 || cols == 0;
    }

    public boolean isUniform() {
        return uniform;
    }

    // ========== Coordinate Accessors ==========

    /**
     * Returns the X coordinate for the given column.
     */
    public double getX(int col) {
        if (col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException("Column: " + col + ", Cols: " + cols);
        }
        return xCoords[col];
    }

    /**
     * Returns the Y coordinate for the given row.
     */
    public double getY(int row) {
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Row: " + row + ", Rows: " + rows);
        }
        return yCoords[row];
    }

    /**
     * Returns the X coordinate range.
     */
    public double getXMin() {
        return cols > 0 ? xCoords[0] : 0;
    }

    public double getXMax() {
        return cols > 0 ? xCoords[cols - 1] : 0;
    }

    /**
     * Returns the Y coordinate range.
     */
    public double getYMin() {
        return rows > 0 ? yCoords[0] : 0;
    }

    public double getYMax() {
        return rows > 0 ? yCoords[rows - 1] : 0;
    }

    /**
     * Returns the cell width at the given column.
     * For uniform grids, all cells have the same width.
     */
    public double getCellWidth(int col) {
        if (cols <= 1) {
            return 1;
        }
        if (col == 0) {
            return xCoords[1] - xCoords[0];
        }
        if (col == cols - 1) {
            return xCoords[cols - 1] - xCoords[cols - 2];
        }
        return (xCoords[col + 1] - xCoords[col - 1]) / 2;
    }

    /**
     * Returns the cell height at the given row.
     */
    public double getCellHeight(int row) {
        if (rows <= 1) {
            return 1;
        }
        if (row == 0) {
            return yCoords[1] - yCoords[0];
        }
        if (row == rows - 1) {
            return yCoords[rows - 1] - yCoords[rows - 2];
        }
        return (yCoords[row + 1] - yCoords[row - 1]) / 2;
    }

    /**
     * Returns the raw X coordinates array.
     */
    public double[] getXCoordsArray() {
        return xCoords;
    }

    /**
     * Returns the raw Y coordinates array.
     */
    public double[] getYCoordsArray() {
        return yCoords;
    }

    // ========== Value Accessors ==========

    /**
     * Returns the value at the given row and column.
     */
    public float getValue(int row, int col) {
        checkBounds(row, col);
        return values[row * cols + col];
    }

    /**
     * Returns the raw values array in row-major order.
     */
    public float[] getValuesArray() {
        return values;
    }

    /**
     * Returns true if the value at the given position is valid (not NaN).
     */
    public boolean hasValue(int row, int col) {
        checkBounds(row, col);
        return !Float.isNaN(values[row * cols + col]);
    }

    // ========== Value Range ==========

    /**
     * Returns the minimum value in the grid.
     */
    public float getMinValue() {
        ensureMinMaxCached();
        return cachedMinValue;
    }

    /**
     * Returns the maximum value in the grid.
     */
    public float getMaxValue() {
        ensureMinMaxCached();
        return cachedMaxValue;
    }

    private void ensureMinMaxCached() {
        if (!Float.isNaN(cachedMinValue)) {
            return;
        }

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < values.length; i++) {
            float v = values[i];
            if (!Float.isNaN(v)) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        cachedMinValue = Float.isFinite(min) ? min : 0;
        cachedMaxValue = Float.isFinite(max) ? max : 1;
    }

    private void invalidateCache() {
        cachedMinValue = Float.NaN;
        cachedMaxValue = Float.NaN;
    }

    // ========== Index Lookup ==========

    /**
     * Finds the column index for the given X coordinate.
     * Returns the column containing x, or -1 if out of range.
     */
    public int findColumn(double x) {
        if (cols == 0 || x < xCoords[0] || x > xCoords[cols - 1]) {
            return -1;
        }

        // Binary search for uniform grids is simple
        if (uniform && cols > 1) {
            double step = (xCoords[cols - 1] - xCoords[0]) / (cols - 1);
            int col = (int) ((x - xCoords[0]) / step);
            return Math.max(0, Math.min(cols - 1, col));
        }

        // Binary search for non-uniform
        int low = 0;
        int high = cols - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (xCoords[mid] <= x) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Finds the row index for the given Y coordinate.
     */
    public int findRow(double y) {
        if (rows == 0 || y < yCoords[0] || y > yCoords[rows - 1]) {
            return -1;
        }

        if (uniform && rows > 1) {
            double step = (yCoords[rows - 1] - yCoords[0]) / (rows - 1);
            int row = (int) ((y - yCoords[0]) / step);
            return Math.max(0, Math.min(rows - 1, row));
        }

        int low = 0;
        int high = rows - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (yCoords[mid] <= y) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Returns the column range visible in the given X coordinate range.
     *
     * @param xMin minimum X
     * @param xMax maximum X
     * @return int[2] with {startCol, endCol} inclusive, or null if no overlap
     */
    public int[] getVisibleColumnRange(double xMin, double xMax) {
        if (cols == 0 || xMax < xCoords[0] || xMin > xCoords[cols - 1]) {
            return null;
        }

        int startCol = Math.max(0, findColumn(xMin));
        int endCol = Math.min(cols - 1, findColumn(xMax));

        // Expand slightly to include boundary cells
        if (startCol > 0 && xCoords[startCol] > xMin) {
            startCol--;
        }
        if (endCol < cols - 1 && xCoords[endCol] < xMax) {
            endCol++;
        }

        return new int[]{startCol, endCol};
    }

    /**
     * Returns the row range visible in the given Y coordinate range.
     */
    public int[] getVisibleRowRange(double yMin, double yMax) {
        if (rows == 0 || yMax < yCoords[0] || yMin > yCoords[rows - 1]) {
            return null;
        }

        int startRow = Math.max(0, findRow(yMin));
        int endRow = Math.min(rows - 1, findRow(yMax));

        if (startRow > 0 && yCoords[startRow] > yMin) {
            startRow--;
        }
        if (endRow < rows - 1 && yCoords[endRow] < yMax) {
            endRow++;
        }

        return new int[]{startRow, endRow};
    }

    // ========== Mutation ==========

    /**
     * Sets the value at the given row and column.
     */
    public void setValue(int row, int col, float value) {
        checkBounds(row, col);
        values[row * cols + col] = value;
        invalidateCache();
    }

    /**
     * Sets all values in a row.
     */
    public void setRow(int row, float[] rowValues) {
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Row: " + row + ", Rows: " + rows);
        }
        if (rowValues.length != cols) {
            throw new IllegalArgumentException("Row values length must match cols");
        }
        System.arraycopy(rowValues, 0, values, row * cols, cols);
        invalidateCache();
    }

    /**
     * Sets all values from a 2D array.
     *
     * @param data 2D array [row][col]
     */
    public void setValues(float[][] data) {
        if (data.length != rows) {
            throw new IllegalArgumentException("Data rows must match: " + data.length + " vs " + rows);
        }
        for (int r = 0; r < rows; r++) {
            if (data[r].length != cols) {
                throw new IllegalArgumentException("Data cols must match at row " + r);
            }
            System.arraycopy(data[r], 0, values, r * cols, cols);
        }
        invalidateCache();
        listenerSupport.fireDataUpdated(null, 0);
    }

    /**
     * Sets all values from a flat array in row-major order.
     */
    public void setValues(float[] data) {
        if (data.length != rows * cols) {
            throw new IllegalArgumentException("Data length must match rows * cols");
        }
        System.arraycopy(data, 0, values, 0, data.length);
        invalidateCache();
        listenerSupport.fireDataUpdated(null, 0);
    }

    /**
     * Fills all cells with the given value.
     */
    public void fill(float value) {
        Arrays.fill(values, value);
        invalidateCache();
    }

    /**
     * Clears all values to NaN.
     */
    public void clear() {
        Arrays.fill(values, Float.NaN);
        invalidateCache();
        listenerSupport.fireDataCleared(null);
    }

    // ========== Resizing ==========

    /**
     * Resizes the grid, discarding existing data.
     */
    public void resize(int newRows, int newCols, double xMin, double xMax, double yMin, double yMax) {
        this.rows = newRows;
        this.cols = newCols;
        this.values = new float[newRows * newCols];
        this.uniform = true;

        this.xCoords = new double[newCols];
        this.yCoords = new double[newRows];

        double xStep = newCols > 1 ? (xMax - xMin) / (newCols - 1) : 0;
        double yStep = newRows > 1 ? (yMax - yMin) / (newRows - 1) : 0;

        for (int c = 0; c < newCols; c++) {
            xCoords[c] = xMin + c * xStep;
        }
        for (int r = 0; r < newRows; r++) {
            yCoords[r] = yMin + r * yStep;
        }

        invalidateCache();
    }

    // ========== Validation ==========

    private void checkBounds(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException(
                    "Position [" + row + "," + col + "] out of bounds [" + rows + "," + cols + "]");
        }
    }

    // ========== Listener Management ==========

    public void addListener(DataListener listener) {
        listenerSupport.addListener(listener);
    }

    public void removeListener(DataListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public String toString() {
        return String.format("HeatmapData[id=%s, rows=%d, cols=%d, uniform=%s]",
                id, rows, cols, uniform);
    }
}
