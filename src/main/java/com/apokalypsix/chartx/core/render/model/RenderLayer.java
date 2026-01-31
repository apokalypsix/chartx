package com.apokalypsix.chartx.core.render.model;

import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.jogamp.opengl.GL2ES2;

/**
 * Interface for a rendering layer in the chart pipeline.
 *
 * <p>Layers are rendered in order based on their z-order, from back to front.
 * Each layer is responsible for rendering a specific type of content (background,
 * grid, data, overlays, etc.).
 */
public interface RenderLayer {

    /**
     * Returns the z-order for this layer. Lower values render first (back).
     */
    int getZOrder();

    /**
     * Returns true if this layer is visible.
     */
    boolean isVisible();

    /**
     * Sets the visibility of this layer.
     */
    void setVisible(boolean visible);

    /**
     * Initializes GL resources for this layer.
     * Called once when the layer is added to the pipeline.
     *
     * @param gl the GL context
     * @param resources the resource manager
     */
    void initialize(GL2ES2 gl, GLResourceManager resources);

    /**
     * Renders this layer.
     *
     * @param ctx the render context for this frame
     */
    void render(RenderContext ctx);

    /**
     * Returns true if this layer needs to be redrawn.
     */
    boolean isDirty();

    /**
     * Marks this layer as needing a redraw.
     */
    void markDirty();

    /**
     * Marks this layer as clean (just rendered).
     */
    void markClean();

    /**
     * Releases GL resources for this layer.
     * Called when the layer is removed or the context is destroyed.
     *
     * @param gl the GL context
     */
    void dispose(GL2ES2 gl);
}
