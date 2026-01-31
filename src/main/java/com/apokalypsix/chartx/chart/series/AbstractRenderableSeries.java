package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.chart.style.SeriesOptions;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.core.render.api.ResourceManager;

/**
 * Abstract base class for renderable series.
 *
 * <p>Provides common functionality for managing data, options, and
 * initialization state.
 *
 * @param <D> the type of Data this series renders
 * @param <O> the type of SeriesOptions this series uses
 */
public abstract class AbstractRenderableSeries<D extends Data<?>, O extends SeriesOptions>
        implements RenderableSeries<D, O> {

    protected final String id;
    protected final D data;
    protected O options;
    protected boolean initialized = false;

    /**
     * Creates a new series with the given data and options.
     *
     * @param data the data to render
     * @param options the rendering options
     */
    protected AbstractRenderableSeries(D data, O options) {
        this.id = data.getId() + "_" + getType().name().toLowerCase();
        this.data = data;
        this.options = options;
    }

    /**
     * Creates a new series with a custom ID.
     *
     * @param id the series ID
     * @param data the data to render
     * @param options the rendering options
     */
    protected AbstractRenderableSeries(String id, D data, O options) {
        this.id = id;
        this.data = data;
        this.options = options;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public D getData() {
        return data;
    }

    @Override
    public O getOptions() {
        return options;
    }

    @Override
    public void setOptions(O options) {
        this.options = options;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize(ResourceManager resources) {
        if (!initialized) {
            doInitialize(resources);
            initialized = true;
        }
    }

    @Override
    public void dispose() {
        if (initialized) {
            doDispose();
            initialized = false;
        }
    }

    /**
     * Subclasses implement this to initialize rendering resources.
     */
    protected abstract void doInitialize(ResourceManager resources);

    /**
     * Subclasses implement this to release rendering resources.
     */
    protected abstract void doDispose();

    @Override
    public String toString() {
        return String.format("%s[id=%s, data=%s, visible=%s]",
                getClass().getSimpleName(), id, data.getId(), isVisible());
    }
}
