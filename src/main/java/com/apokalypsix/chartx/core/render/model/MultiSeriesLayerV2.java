package com.apokalypsix.chartx.core.render.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.apokalypsix.chartx.chart.series.RenderableSeries;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer that manages and renders multiple series.
 *
 * <p>This layer supports the new Data/Series architecture where series
 * combine data with rendering options. Multiple series of different types
 * can be added and will be rendered in z-order.
 *
 * <p>Example usage:
 * <pre>{@code
 * MultiSeriesLayerV2 layer = new MultiSeriesLayerV2();
 * layer.addSeries(new CandlestickSeries(ohlcData, ohlcOptions));
 * layer.addSeries(new LineSeries(emaData, lineOptions));
 * layer.addSeries(new BandSeries(bbData, bandOptions));
 * }</pre>
 */
public class MultiSeriesLayerV2 extends AbstractRenderLayer {

    /** Default z-order for multi-series layer (after grid, before crosshair) */
    public static final int DEFAULT_Z_ORDER = 200;

    // Thread-safe list for series
    private final CopyOnWriteArrayList<RenderableSeries<?, ?>> seriesList = new CopyOnWriteArrayList<>();

    // Sorted view for rendering (rebuilt when series added/removed)
    private volatile List<RenderableSeries<?, ?>> sortedSeries = new ArrayList<>();
    private volatile boolean needsSort = true;

    private ResourceManager resourceManager;

    /**
     * Creates a multi-series layer with default z-order.
     */
    public MultiSeriesLayerV2() {
        super(DEFAULT_Z_ORDER);
    }

    /**
     * Creates a multi-series layer with the specified z-order.
     *
     * @param zOrder the layer z-order
     */
    public MultiSeriesLayerV2(int zOrder) {
        super(zOrder);
    }

    // ========== Series Management ==========

    /**
     * Adds a series to this layer.
     * Repaints automatically.
     *
     * @param series the series to add
     */
    public void addSeries(RenderableSeries<?, ?> series) {
        if (series != null && !seriesList.contains(series)) {
            seriesList.add(series);
            needsSort = true;
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Batch add multiple series (single repaint).
     *
     * @param seriesToAdd the series to add
     */
    public void addAllSeries(List<? extends RenderableSeries<?, ?>> seriesToAdd) {
        boolean added = false;
        for (RenderableSeries<?, ?> series : seriesToAdd) {
            if (series != null && !seriesList.contains(series)) {
                seriesList.add(series);
                added = true;
            }
        }
        if (added) {
            needsSort = true;
            markDirty();
            requestRepaint();
        }
    }

    /**
     * Removes a series from this layer by ID.
     * Repaints automatically.
     *
     * @param seriesId the ID of the series to remove
     * @return true if the series was removed
     */
    public boolean removeSeries(String seriesId) {
        RenderableSeries<?, ?> toRemove = null;
        for (RenderableSeries<?, ?> s : seriesList) {
            if (s.getId().equals(seriesId)) {
                toRemove = s;
                break;
            }
        }
        if (toRemove != null) {
            seriesList.remove(toRemove);
            needsSort = true;
            markDirty();
            requestRepaint();
            return true;
        }
        return false;
    }

    /**
     * Removes a series from this layer.
     * Repaints automatically.
     *
     * @param series the series to remove
     * @return true if the series was removed
     */
    public boolean removeSeries(RenderableSeries<?, ?> series) {
        if (series != null && seriesList.remove(series)) {
            needsSort = true;
            markDirty();
            requestRepaint();
            return true;
        }
        return false;
    }

    /**
     * Returns a series by ID.
     *
     * @param seriesId the series ID
     * @return the series, or null if not found
     */
    public RenderableSeries<?, ?> getSeries(String seriesId) {
        for (RenderableSeries<?, ?> s : seriesList) {
            if (s.getId().equals(seriesId)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns all series in this layer.
     *
     * @return unmodifiable list of series
     */
    public List<RenderableSeries<?, ?>> getAllSeries() {
        return new ArrayList<>(seriesList);
    }

    /**
     * Returns the number of series in this layer.
     */
    public int getSeriesCount() {
        return seriesList.size();
    }

    /**
     * Clears all series from this layer.
     * Repaints automatically.
     */
    public void clearSeries() {
        if (!seriesList.isEmpty()) {
            seriesList.clear();
            sortedSeries = new ArrayList<>();
            needsSort = false;
            markDirty();
            requestRepaint();
        }
    }

    // ========== Auto-scaling Support ==========

    /**
     * Calculates the minimum Y value across all visible series in the given index range.
     *
     * @param startIdx start index
     * @param endIdx end index
     * @return minimum value, or Double.NaN if no valid data
     */
    public double getMinValue(int startIdx, int endIdx) {
        double min = Double.NaN;
        for (RenderableSeries<?, ?> series : seriesList) {
            if (series.isVisible()) {
                double seriesMin = series.getMinValue(startIdx, endIdx);
                if (!Double.isNaN(seriesMin)) {
                    if (Double.isNaN(min) || seriesMin < min) {
                        min = seriesMin;
                    }
                }
            }
        }
        return min;
    }

    /**
     * Calculates the maximum Y value across all visible series in the given index range.
     *
     * @param startIdx start index
     * @param endIdx end index
     * @return maximum value, or Double.NaN if no valid data
     */
    public double getMaxValue(int startIdx, int endIdx) {
        double max = Double.NaN;
        for (RenderableSeries<?, ?> series : seriesList) {
            if (series.isVisible()) {
                double seriesMax = series.getMaxValue(startIdx, endIdx);
                if (!Double.isNaN(seriesMax)) {
                    if (Double.isNaN(max) || seriesMax > max) {
                        max = seriesMax;
                    }
                }
            }
        }
        return max;
    }

    // ========== RenderLayer Implementation ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // Series are initialized lazily when rendered using the ResourceManager from RenderContext
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        for (RenderableSeries<?, ?> series : seriesList) {
            series.dispose();
        }
    }

    @Override
    public void render(RenderContext ctx) {
        if (!isVisible() || seriesList.isEmpty()) {
            return;
        }

        // Sort by z-order if needed
        if (needsSort) {
            rebuildSortedList();
        }

        // Get resource manager from context - required for series initialization
        ResourceManager rm = ctx.getResourceManager();
        if (rm == null) {
            // Fallback: try to get from stored reference if context doesn't have abstracted API
            rm = resourceManager;
        }

        // Render each series in z-order
        for (RenderableSeries<?, ?> series : sortedSeries) {
            if (!series.isVisible()) {
                continue;
            }

            // Initialize if needed
            if (!series.isInitialized() && rm != null) {
                series.initialize(rm);
            }

            // Render
            series.render(ctx);
        }

        markClean();
    }

    private void rebuildSortedList() {
        List<RenderableSeries<?, ?>> newList = new ArrayList<>(seriesList);
        newList.sort(Comparator.comparingInt(RenderableSeries::getZOrder));
        sortedSeries = newList;
        needsSort = false;
    }
}
