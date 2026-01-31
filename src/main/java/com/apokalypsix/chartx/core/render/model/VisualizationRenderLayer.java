package com.apokalypsix.chartx.core.render.model;

import com.apokalypsix.chartx.chart.series.BoundedRenderable;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer that delegates to a {@link BoundedRenderable}.
 *
 * <p>Bridges standalone visualization series (treemap, sunburst, pie, radar, etc.)
 * into the layer-based {@link RenderContext} pipeline, so they can be rendered
 * through any {@link com.apokalypsix.chartx.core.render.swing.ChartRenderingStrategy}
 * without GL-specific boilerplate in calling code.
 *
 * <p>Uses lazy initialization: resources are initialized on first render when
 * the abstracted API is available.
 */
public class VisualizationRenderLayer extends AbstractRenderLayer {

    private BoundedRenderable renderable;
    private float padding;
    private boolean v2Initialized = false;

    public VisualizationRenderLayer() {
        super(100);
    }

    public void setRenderable(BoundedRenderable renderable) {
        // Dispose old renderable if switching
        if (this.renderable != null && this.renderable != renderable && v2Initialized) {
            this.renderable.dispose();
        }
        this.renderable = renderable;
        v2Initialized = false;
        markDirty();
    }

    public BoundedRenderable getRenderable() {
        return renderable;
    }

    public void setPadding(float padding) {
        this.padding = padding;
        markDirty();
    }

    public float getPadding() {
        return padding;
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 resources are initialized lazily on first render
    }

    @Override
    public void render(RenderContext ctx) {
        if (renderable == null) {
            return;
        }

        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        // Lazy initialization of V2 resources
        if (!v2Initialized && !renderable.isInitialized()) {
            ResourceManager resources = ctx.getResourceManager();
            renderable.initialize(resources);
            v2Initialized = true;
        }

        int w = ctx.getWidth();
        int h = ctx.getHeight();
        renderable.render(ctx, padding, padding, w - 2 * padding, h - 2 * padding);
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        if (renderable != null && v2Initialized) {
            renderable.dispose();
        }
        v2Initialized = false;
    }
}
