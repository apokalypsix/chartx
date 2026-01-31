package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.core.render.api.ResourceManager;

/**
 * Common interface for standalone series that render within a rectangular bounds.
 *
 * <p>Allows different visualization types (treemap, sunburst, pie, radar, etc.)
 * to be managed interchangeably by {@link VisualizationGroup}.
 *
 * <p>Uses the abstracted rendering API ({@link ResourceManager}) for backend independence.
 */
public interface BoundedRenderable {

    /** Returns the unique identifier for this renderable. */
    String getId();

    /** Returns the series type. */
    SeriesType getType();

    /** Returns whether this renderable is visible. */
    boolean isVisible();

    /** Initializes rendering resources using the abstracted API. */
    void initialize(ResourceManager resources);

    /** Returns true if resources have been initialized. */
    boolean isInitialized();

    /** Renders into the given rectangular bounds. */
    void render(RenderContext ctx, float x, float y, float width, float height);

    /** Releases rendering resources. */
    void dispose();

    /** Invalidates any cached layout, forcing recalculation on next render. */
    void invalidateLayout();
}
