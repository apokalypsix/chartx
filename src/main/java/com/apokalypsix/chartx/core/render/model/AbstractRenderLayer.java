package com.apokalypsix.chartx.core.render.model;

import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.jogamp.opengl.GL2ES2;

/**
 * Abstract base class for render layers providing common functionality.
 */
public abstract class AbstractRenderLayer implements RenderLayer {

    /**
     * Callback interface for requesting chart repaints when layer content changes.
     */
    @FunctionalInterface
    public interface RepaintCallback {
        void requestRepaint();
    }

    private int zOrder;
    private boolean visible = true;
    private boolean dirty = true;
    protected boolean initialized = false;
    private RepaintCallback repaintCallback;

    /**
     * Creates a layer with the specified z-order.
     *
     * @param zOrder layer z-order (lower values render first)
     */
    protected AbstractRenderLayer(int zOrder) {
        this.zOrder = zOrder;
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    /**
     * Sets the z-order for this layer.
     * Call RenderPipeline.sortLayers() after changing z-order.
     *
     * @param zOrder new z-order value
     */
    protected void setZOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            markDirty();
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markDirty() {
        dirty = true;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

    /**
     * Sets the callback to be invoked when the layer needs a repaint.
     * This is called by Chart to wire up automatic repainting.
     *
     * @param callback the callback, or null to disable auto-repaint
     */
    public void setRepaintCallback(RepaintCallback callback) {
        this.repaintCallback = callback;
    }

    /**
     * Requests a repaint from the parent chart component.
     * Subclasses should call this after modifying layer content.
     */
    protected void requestRepaint() {
        if (repaintCallback != null) {
            repaintCallback.requestRepaint();
        }
    }

    @Override
    public void initialize(GL2ES2 gl, GLResourceManager resources) {
        doInitialize(gl, resources);
        initialized = true;
    }

    @Override
    public void dispose(GL2ES2 gl) {
        if (initialized) {
            doDispose(gl);
            initialized = false;
        }
    }

    /**
     * Subclass hook for initialization. Called by {@link #initialize}.
     */
    protected abstract void doInitialize(GL2ES2 gl, GLResourceManager resources);

    /**
     * Subclass hook for disposal. Called by {@link #dispose}.
     */
    protected abstract void doDispose(GL2ES2 gl);
}
