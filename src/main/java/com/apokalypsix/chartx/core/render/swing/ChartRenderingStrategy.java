package com.apokalypsix.chartx.core.render.swing;

import javax.swing.*;

import com.apokalypsix.chartx.core.render.service.RenderPipeline;

/**
 * Strategy interface for chart rendering backends.
 *
 * <p>This abstraction allows Chart to work with different rendering
 * backends (OpenGL, Vulkan, Metal, DX12) without being coupled to any
 * specific implementation.
 *
 * <p>Implementations handle:
 * <ul>
 *   <li>Creating the appropriate Swing component for display</li>
 *   <li>Managing the render device lifecycle</li>
 *   <li>Connecting the RenderPipeline to the rendering backend</li>
 *   <li>Handling frame rendering and pixel transfer to Swing</li>
 * </ul>
 */
public interface ChartRenderingStrategy {

    /**
     * Initializes the rendering strategy with the given pipeline.
     *
     * <p>This method sets up the render device, resource manager, and
     * connects them to the pipeline for rendering.
     *
     * @param pipeline the render pipeline to use
     */
    void initialize(RenderPipeline pipeline);

    /**
     * Returns the Swing component used for display.
     *
     * <p>This component should be added to the Chart's layout.
     * For OpenGL, this is typically a GLJPanel. For other backends,
     * this is an offscreen panel that renders to a BufferedImage.
     *
     * @return the display component
     */
    JComponent getDisplayComponent();

    /**
     * Requests a repaint of the rendered content.
     *
     * <p>This triggers the rendering backend to render a new frame
     * and update the display component.
     */
    void requestRepaint();

    /**
     * Returns true if the strategy is initialized and ready to render.
     */
    boolean isInitialized();

    /**
     * Disposes of rendering resources.
     *
     * <p>This should be called when the chart is being removed or
     * the application is shutting down. After disposal, the strategy
     * should not be used again.
     */
    void dispose();

    /**
     * Returns the name of this rendering backend for display purposes.
     */
    String getBackendName();
}
