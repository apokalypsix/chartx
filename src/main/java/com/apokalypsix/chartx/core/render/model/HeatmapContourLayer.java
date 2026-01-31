package com.apokalypsix.chartx.core.render.model;

import com.apokalypsix.chartx.chart.series.ContourSeries;
import com.apokalypsix.chartx.chart.series.HeatmapSeries;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for heatmap and contour series.
 *
 * <p>Wraps HeatmapSeries and ContourSeries to integrate them with the Chart
 * render pipeline, providing proper initialization and rendering.
 */
public class HeatmapContourLayer extends AbstractRenderLayer {

    /** Default z-order (renders before standard series) */
    public static final int DEFAULT_Z_ORDER = 150;

    private HeatmapSeries heatmapSeries;
    private ContourSeries contourSeries;
    private ResourceManager resourceManager;

    public HeatmapContourLayer() {
        super(DEFAULT_Z_ORDER);
    }

    public void setHeatmapSeries(HeatmapSeries series) {
        this.heatmapSeries = series;
        if (resourceManager != null && series != null && !series.isInitialized()) {
            series.initialize(resourceManager);
        }
    }

    public void setContourSeries(ContourSeries series) {
        this.contourSeries = series;
        if (resourceManager != null && series != null && !series.isInitialized()) {
            series.initialize(resourceManager);
        }
    }

    public HeatmapSeries getHeatmapSeries() {
        return heatmapSeries;
    }

    public ContourSeries getContourSeries() {
        return contourSeries;
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 initialization happens on first render
    }

    @Override
    public void render(RenderContext ctx) {
        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        // Initialize resource manager if needed
        if (resourceManager == null) {
            resourceManager = ctx.getResourceManager();
        }

        // Initialize and render heatmap
        if (heatmapSeries != null) {
            if (!heatmapSeries.isInitialized()) {
                heatmapSeries.initialize(resourceManager);
            }
            if (heatmapSeries.getOptions().isVisible()) {
                heatmapSeries.render(ctx);
            }
        }

        // Initialize and render contour
        if (contourSeries != null) {
            if (!contourSeries.isInitialized()) {
                contourSeries.initialize(resourceManager);
            }
            if (contourSeries.getOptions().isVisible()) {
                contourSeries.render(ctx);
            }
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        if (heatmapSeries != null) {
            heatmapSeries.dispose();
        }
        if (contourSeries != null) {
            contourSeries.dispose();
        }
    }
}
