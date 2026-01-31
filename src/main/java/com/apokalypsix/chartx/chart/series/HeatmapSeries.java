package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.HeatmapSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ColorMap;
import com.apokalypsix.chartx.chart.data.HeatmapData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable heatmap series using HeatmapData.
 *
 * <p>Renders a 2D grid of colored cells based on values mapped through
 * a color map. Supports both uniform and non-uniform grids.
 */
public class HeatmapSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int VERTICES_PER_CELL = 6; // 2 triangles

    private final String id;
    private final HeatmapData data;
    private final HeatmapSeriesOptions options;

    // Rendering resources
    private Buffer cellBuffer;
    private Buffer borderBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] cellVertices;
    private float[] borderVertices;
    private int cellCapacity;

    // Working array for color mapping
    private float[] rgba = new float[4];

    /**
     * Creates a heatmap series with default options.
     */
    public HeatmapSeries(HeatmapData data) {
        this(data, new HeatmapSeriesOptions());
    }

    /**
     * Creates a heatmap series with the given options.
     */
    public HeatmapSeries(HeatmapData data, HeatmapSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a heatmap series with a custom ID.
     */
    public HeatmapSeries(String id, HeatmapData data, HeatmapSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    public String getId() {
        return id;
    }

    public HeatmapData getData() {
        return data;
    }

    public HeatmapSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.HEATMAP;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        BufferDescriptor cellDesc = BufferDescriptor.positionColor2D(65536);
        cellBuffer = resources.getOrCreateBuffer(id + "_cell", cellDesc);

        BufferDescriptor borderDesc = BufferDescriptor.positionColor2D(32768);
        borderBuffer = resources.getOrCreateBuffer(id + "_border", borderDesc);

        cellCapacity = 4096;
        cellVertices = new float[cellCapacity * VERTICES_PER_CELL * FLOATS_PER_VERTEX];
        borderVertices = new float[cellCapacity * 8 * FLOATS_PER_VERTEX];

        initialized = true;
    }

    /**
     * Disposes rendering resources.
     */
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_cell");
            resourceManager.disposeBuffer(id + "_border");
        }
        cellBuffer = null;
        borderBuffer = null;
        initialized = false;
    }

    /**
     * Renders the heatmap.
     */
    public void render(RenderContext ctx) {
        if (!initialized || data.isEmpty() || !options.isVisible()) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        // Get visible range
        double xMin = data.getXMin();
        double xMax = data.getXMax();
        double yMin = data.getYMin();
        double yMax = data.getYMax();

        // Find visible cells
        int[] colRange = data.getVisibleColumnRange(xMin, xMax);
        int[] rowRange = data.getVisibleRowRange(yMin, yMax);

        if (colRange == null || rowRange == null) {
            return;
        }

        int startCol = colRange[0];
        int endCol = colRange[1];
        int startRow = rowRange[0];
        int endRow = rowRange[1];

        int visibleCells = (endCol - startCol + 1) * (endRow - startRow + 1);
        ensureCapacity(visibleCells);

        // Configure color map range
        ColorMap colorMap = options.getColorMap();
        if (options.isAutoScale()) {
            colorMap.valueRange(data.getMinValue(), data.getMaxValue());
        } else {
            colorMap.valueRange(options.getMinValue(), options.getMaxValue());
        }

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Build and draw cells
        int cellFloatCount = buildCellVertices(coords, colorMap, startRow, endRow, startCol, endCol);
        if (cellFloatCount > 0) {
            cellBuffer.upload(cellVertices, 0, cellFloatCount);
            cellBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw cell borders if enabled
        if (options.isShowCellBorders() && options.getCellBorderWidth() > 0) {
            ctx.getDevice().setLineWidth(options.getCellBorderWidth());
            int borderFloatCount = buildBorderVertices(coords, startRow, endRow, startCol, endCol);
            if (borderFloatCount > 0) {
                borderBuffer.upload(borderVertices, 0, borderFloatCount);
                borderBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();
    }

    private int buildCellVertices(CoordinateSystem coords, ColorMap colorMap,
                                   int startRow, int endRow, int startCol, int endCol) {
        int floatIndex = 0;

        float[] values = data.getValuesArray();
        double[] xCoords = data.getXCoordsArray();
        double[] yCoords = data.getYCoordsArray();
        int cols = data.getCols();

        float opacity = options.getOpacity();
        Color nanColor = options.getNanColor();

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                float value = values[row * cols + col];

                // Handle NaN values
                if (Float.isNaN(value)) {
                    if (nanColor == null) {
                        continue; // Skip transparent cells
                    }
                    rgba[0] = nanColor.getRed() / 255f;
                    rgba[1] = nanColor.getGreen() / 255f;
                    rgba[2] = nanColor.getBlue() / 255f;
                    rgba[3] = (nanColor.getAlpha() / 255f) * opacity;
                } else {
                    colorMap.getColor(value, rgba);
                    rgba[3] *= opacity;
                }

                // Calculate cell bounds
                double cellWidth = data.getCellWidth(col);
                double cellHeight = data.getCellHeight(row);

                double x = xCoords[col];
                double y = yCoords[row];

                // Convert to screen coordinates
                float left = (float) coords.xValueToScreenX((long) (x - cellWidth / 2));
                float right = (float) coords.xValueToScreenX((long) (x + cellWidth / 2));
                float top = (float) coords.yValueToScreenY(y + cellHeight / 2);
                float bottom = (float) coords.yValueToScreenY(y - cellHeight / 2);

                // Two triangles for the cell
                floatIndex = addVertex(cellVertices, floatIndex, left, top, rgba[0], rgba[1], rgba[2], rgba[3]);
                floatIndex = addVertex(cellVertices, floatIndex, left, bottom, rgba[0], rgba[1], rgba[2], rgba[3]);
                floatIndex = addVertex(cellVertices, floatIndex, right, bottom, rgba[0], rgba[1], rgba[2], rgba[3]);

                floatIndex = addVertex(cellVertices, floatIndex, left, top, rgba[0], rgba[1], rgba[2], rgba[3]);
                floatIndex = addVertex(cellVertices, floatIndex, right, bottom, rgba[0], rgba[1], rgba[2], rgba[3]);
                floatIndex = addVertex(cellVertices, floatIndex, right, top, rgba[0], rgba[1], rgba[2], rgba[3]);
            }
        }

        return floatIndex;
    }

    private int buildBorderVertices(CoordinateSystem coords,
                                     int startRow, int endRow, int startCol, int endCol) {
        int floatIndex = 0;

        double[] xCoords = data.getXCoordsArray();
        double[] yCoords = data.getYCoordsArray();

        Color borderColor = options.getCellBorderColor();
        float r = borderColor.getRed() / 255f;
        float g = borderColor.getGreen() / 255f;
        float b = borderColor.getBlue() / 255f;
        float a = (borderColor.getAlpha() / 255f) * options.getOpacity();

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                double cellWidth = data.getCellWidth(col);
                double cellHeight = data.getCellHeight(row);

                double x = xCoords[col];
                double y = yCoords[row];

                float left = (float) coords.xValueToScreenX((long) (x - cellWidth / 2));
                float right = (float) coords.xValueToScreenX((long) (x + cellWidth / 2));
                float top = (float) coords.yValueToScreenY(y + cellHeight / 2);
                float bottom = (float) coords.yValueToScreenY(y - cellHeight / 2);

                // Four edges
                floatIndex = addVertex(borderVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, top, r, g, b, a);

                floatIndex = addVertex(borderVertices, floatIndex, right, top, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, right, bottom, r, g, b, a);

                floatIndex = addVertex(borderVertices, floatIndex, right, bottom, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, left, bottom, r, g, b, a);

                floatIndex = addVertex(borderVertices, floatIndex, left, bottom, r, g, b, a);
                floatIndex = addVertex(borderVertices, floatIndex, left, top, r, g, b, a);
            }
        }

        return floatIndex;
    }

    private int addVertex(float[] vertices, int index, float x, float y,
                          float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }

    private void ensureCapacity(int cellCount) {
        if (cellCount > cellCapacity) {
            cellCapacity = cellCount + cellCount / 2;
            cellVertices = new float[cellCapacity * VERTICES_PER_CELL * FLOATS_PER_VERTEX];
            borderVertices = new float[cellCapacity * 8 * FLOATS_PER_VERTEX];
        }
    }

    /**
     * Returns the value at the given screen coordinates.
     *
     * @param coords coordinate system
     * @param screenX screen X
     * @param screenY screen Y
     * @return the value, or NaN if out of bounds
     */
    public float getValueAt(CoordinateSystem coords, float screenX, float screenY) {
        long timestamp = coords.screenXToXValue(screenX);
        double price = coords.screenYToYValue(screenY);

        int col = data.findColumn(timestamp);
        int row = data.findRow(price);

        if (col < 0 || row < 0) {
            return Float.NaN;
        }

        return data.getValue(row, col);
    }
}
