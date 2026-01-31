package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.core.render.api.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages a set of interchangeable {@link BoundedRenderable} visualizations.
 *
 * <p>Only one visualization is active (rendered) at a time, but all members
 * are initialized eagerly so that switching is instant with no GL init mid-render.
 *
 * <p>Itself implements {@link BoundedRenderable} for composability.
 */
public class VisualizationGroup implements BoundedRenderable {

    private final String id;
    private final List<String> names = new ArrayList<>();
    private final List<BoundedRenderable> renderables = new ArrayList<>();
    private int activeIndex;
    private boolean initialized;

    public VisualizationGroup() {
        this(UUID.randomUUID().toString());
    }

    public VisualizationGroup(String id) {
        this.id = id;
        this.activeIndex = 0;
        this.initialized = false;
    }

    /**
     * Registers a visualization with a display name.
     */
    public VisualizationGroup add(String name, BoundedRenderable renderable) {
        names.add(name);
        renderables.add(renderable);
        return this;
    }

    /**
     * Sets the active visualization by index.
     */
    public void setActive(int index) {
        if (index >= 0 && index < renderables.size()) {
            this.activeIndex = index;
        }
    }

    /**
     * Sets the active visualization by name.
     */
    public void setActive(String name) {
        int idx = names.indexOf(name);
        if (idx >= 0) {
            this.activeIndex = idx;
        }
    }

    /** Returns the index of the currently active visualization. */
    public int getActiveIndex() {
        return activeIndex;
    }

    /** Returns the name of the currently active visualization. */
    public String getActiveName() {
        return names.isEmpty() ? null : names.get(activeIndex);
    }

    /** Returns the display names of all registered visualizations. */
    public List<String> getNames() {
        return names;
    }

    /** Returns the number of registered visualizations. */
    public int size() {
        return renderables.size();
    }

    /** Returns the renderable at the given index. */
    public BoundedRenderable get(int index) {
        return renderables.get(index);
    }

    /** Returns the renderable with the given name, or null. */
    public BoundedRenderable get(String name) {
        int idx = names.indexOf(name);
        return idx >= 0 ? renderables.get(idx) : null;
    }

    // ========== BoundedRenderable implementation ==========

    @Override
    public String getId() {
        return id;
    }

    @Override
    public SeriesType getType() {
        if (renderables.isEmpty()) {
            return null;
        }
        return renderables.get(activeIndex).getType();
    }

    @Override
    public boolean isVisible() {
        if (renderables.isEmpty()) {
            return false;
        }
        return renderables.get(activeIndex).isVisible();
    }

    @Override
    public void initialize(ResourceManager resources) {
        for (BoundedRenderable r : renderables) {
            r.initialize(resources);
        }
        initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        if (renderables.isEmpty()) {
            return;
        }
        renderables.get(activeIndex).render(ctx, x, y, width, height);
    }

    @Override
    public void dispose() {
        for (BoundedRenderable r : renderables) {
            r.dispose();
        }
        initialized = false;
    }

    @Override
    public void invalidateLayout() {
        for (BoundedRenderable r : renderables) {
            r.invalidateLayout();
        }
    }
}
