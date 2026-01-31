package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.SeriesOptions;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.core.render.api.ResourceManager;

/**
 * Interface for renderable series that combine data, options, and rendering.
 *
 * <p>A RenderableSeries wraps a Data object (pure storage) with SeriesOptions
 * (styling configuration) and provides rendering capability. This separation
 * allows the same Data to be visualized by multiple series with different styles.
 *
 * @param <D> the type of Data this series renders
 * @param <O> the type of SeriesOptions this series uses
 */
public interface RenderableSeries<D extends Data<?>, O extends SeriesOptions> {

    /**
     * Returns the unique identifier for this series.
     */
    String getId();

    /**
     * Returns the underlying data.
     */
    D getData();

    /**
     * Returns the rendering options.
     */
    O getOptions();

    /**
     * Sets the rendering options.
     *
     * @param options the new options
     */
    void setOptions(O options);

    /**
     * Returns the series type for identification.
     */
    SeriesType getType();

    /**
     * Returns whether this series is visible.
     */
    default boolean isVisible() {
        return getOptions().isVisible();
    }

    /**
     * Sets visibility.
     */
    default void setVisible(boolean visible) {
        getOptions().setVisible(visible);
    }

    /**
     * Returns the Y-axis ID this series is bound to.
     */
    default String getYAxisId() {
        return getOptions().getYAxisId();
    }

    /**
     * Returns the z-order for rendering.
     */
    default int getZOrder() {
        return getOptions().getZOrder();
    }

    // ========== Rendering ==========

    /**
     * Initializes rendering resources for this series.
     *
     * @param resources the resource manager
     */
    void initialize(ResourceManager resources);

    /**
     * Returns true if this series has been initialized.
     */
    boolean isInitialized();

    /**
     * Renders this series.
     *
     * @param ctx the render context
     */
    void render(RenderContext ctx);

    /**
     * Releases rendering resources.
     */
    void dispose();

    // ========== Auto-scaling support ==========

    /**
     * Returns the minimum Y value in the given index range.
     * Used for auto-scaling the Y axis.
     *
     * @param startIdx the start index (inclusive)
     * @param endIdx the end index (inclusive)
     * @return the minimum value, or Double.NaN if no valid data
     */
    double getMinValue(int startIdx, int endIdx);

    /**
     * Returns the maximum Y value in the given index range.
     * Used for auto-scaling the Y axis.
     *
     * @param startIdx the start index (inclusive)
     * @param endIdx the end index (inclusive)
     * @return the maximum value, or Double.NaN if no valid data
     */
    double getMaxValue(int startIdx, int endIdx);
}
