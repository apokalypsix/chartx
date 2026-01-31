package com.apokalypsix.chartx.chart;

import java.awt.Color;

import javax.swing.JComponent;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.series.BoundedRenderable;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.model.VisualizationRenderLayer;
import com.apokalypsix.chartx.core.render.service.RenderPipeline;
import com.apokalypsix.chartx.core.render.swing.ChartRenderingStrategy;
import com.apokalypsix.chartx.core.render.swing.GLChartRenderingStrategy;
import com.apokalypsix.chartx.core.render.swing.OffscreenChartRenderingStrategy;
import com.apokalypsix.chartx.core.render.api.RenderBackend;

/**
 * Backend-agnostic panel for rendering standalone visualizations.
 *
 * <p>Provides a lightweight alternative to {@link Chart} for visualizations that
 * don't need the full charting infrastructure (axes, coordinate systems, data layers).
 * Wraps a {@link BoundedRenderable} (such as a
 * {@link com.apokalypsix.chartx.chart.series.VisualizationGroup VisualizationGroup})
 * and renders it through the standard {@link ChartRenderingStrategy} pipeline, supporting
 * all rendering backends (OpenGL, Vulkan, Metal, DX12).
 *
 * <p>Usage:
 * <pre>{@code
 * VisualizationPanel panel = new VisualizationPanel(RenderBackend.AUTO);
 * panel.setRenderable(vizGroup);
 * panel.setPadding(40);
 * frame.add(panel.getDisplayComponent(), BorderLayout.CENTER);
 * }</pre>
 */
public class VisualizationPanel {

    private final RenderPipeline pipeline;
    private final ChartRenderingStrategy renderStrategy;
    private final JComponent displayComponent;
    private final VisualizationRenderLayer layer;

    /**
     * Creates a visualization panel using the AUTO backend.
     */
    public VisualizationPanel() {
        this(RenderBackend.AUTO);
    }

    /**
     * Creates a visualization panel using the specified backend.
     */
    public VisualizationPanel(RenderBackend backend) {
        Viewport viewport = new Viewport();
        YAxisManager axisManager = new YAxisManager();

        pipeline = new RenderPipeline(viewport, axisManager);
        pipeline.setUseAbstractedAPI(true);

        layer = new VisualizationRenderLayer();
        pipeline.addLayer(layer);

        renderStrategy = createRenderingStrategy(backend);
        renderStrategy.initialize(pipeline);
        displayComponent = renderStrategy.getDisplayComponent();
    }

    /**
     * Sets the renderable to display.
     */
    public void setRenderable(BoundedRenderable renderable) {
        layer.setRenderable(renderable);
    }

    /**
     * Returns the current renderable.
     */
    public BoundedRenderable getRenderable() {
        return layer.getRenderable();
    }

    /**
     * Sets the padding (in pixels) around the renderable.
     */
    public void setPadding(float padding) {
        layer.setPadding(padding);
    }

    /**
     * Sets the background color.
     */
    public void setBackgroundColor(Color color) {
        pipeline.setBackgroundColor(color);
    }

    /**
     * Returns the Swing component for layout. Add this to your container.
     */
    public JComponent getDisplayComponent() {
        return displayComponent;
    }

    /**
     * Requests a repaint of the visualization.
     */
    public void repaint() {
        renderStrategy.requestRepaint();
    }

    /**
     * Releases all rendering resources.
     */
    public void dispose() {
        renderStrategy.dispose();
    }

    private ChartRenderingStrategy createRenderingStrategy(RenderBackend backend) {
        return switch (backend) {
            case OPENGL -> new GLChartRenderingStrategy();
            case VULKAN, METAL, DX12 -> new OffscreenChartRenderingStrategy(backend);
            case AUTO -> {
                if (OffscreenChartRenderingStrategy.isBackendAvailable(RenderBackend.METAL)) {
                    yield new OffscreenChartRenderingStrategy(RenderBackend.METAL);
                } else if (OffscreenChartRenderingStrategy.isBackendAvailable(RenderBackend.VULKAN)) {
                    yield new OffscreenChartRenderingStrategy(RenderBackend.VULKAN);
                } else {
                    yield new GLChartRenderingStrategy();
                }
            }
        };
    }
}
