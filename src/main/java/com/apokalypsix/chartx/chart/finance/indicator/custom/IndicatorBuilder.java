package com.apokalypsix.chartx.chart.finance.indicator.custom;

import com.apokalypsix.chartx.chart.finance.indicator.*;
import com.apokalypsix.chartx.chart.data.Data;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Fluent builder for creating custom indicator registrations.
 *
 * <p>Example usage:
 * <pre>
 * registry.builder("my_sma")
 *     .name("My Custom SMA")
 *     .description("Custom implementation of SMA")
 *     .category(IndicatorCategory.TREND)
 *     .overlay(true)
 *     .intParam("period", "Period", 20, 1, 500)
 *     .colorParam("color", "Color", Color.BLUE)
 *     .factory(params -> new MySMAIndicator((Integer) params.get("period")))
 *     .register();
 * </pre>
 *
 * @param <S> the source data type
 * @param <R> the result data type
 */
public class IndicatorBuilder<S extends Data<?>, R extends Data<?>> {

    private final String id;
    private final CustomIndicatorRegistry registry;
    private String displayName;
    private String description = "";
    private IndicatorCategory category = IndicatorCategory.CUSTOM;
    private boolean overlay = false;
    private final Map<String, IndicatorParameter<?>> parameters = new LinkedHashMap<>();
    private Function<Map<String, Object>, Indicator<S, R>> factory;

    /**
     * Creates a new builder for the specified indicator ID.
     *
     * @param id unique indicator identifier
     * @param registry the registry to register with
     */
    IndicatorBuilder(String id, CustomIndicatorRegistry registry) {
        this.id = id;
        this.registry = registry;
        this.displayName = id;
    }

    /**
     * Sets the display name.
     */
    public IndicatorBuilder<S, R> name(String name) {
        this.displayName = name;
        return this;
    }

    /**
     * Sets the description.
     */
    public IndicatorBuilder<S, R> description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the category.
     */
    public IndicatorBuilder<S, R> category(IndicatorCategory category) {
        this.category = category;
        return this;
    }

    /**
     * Sets whether this indicator is overlaid on the price chart.
     */
    public IndicatorBuilder<S, R> overlay(boolean overlay) {
        this.overlay = overlay;
        return this;
    }

    /**
     * Adds an integer parameter.
     */
    public IndicatorBuilder<S, R> intParam(String name, String displayName,
                                            int defaultValue, int min, int max) {
        parameters.put(name, new IndicatorParameter.IntParam(name, displayName, defaultValue, min, max));
        return this;
    }

    /**
     * Adds an integer parameter with default range.
     */
    public IndicatorBuilder<S, R> intParam(String name, String displayName, int defaultValue) {
        return intParam(name, displayName, defaultValue, 1, 1000);
    }

    /**
     * Adds a double parameter.
     */
    public IndicatorBuilder<S, R> doubleParam(String name, String displayName,
                                               double defaultValue, double min, double max) {
        parameters.put(name, new IndicatorParameter.DoubleParam(name, displayName, defaultValue, min, max));
        return this;
    }

    /**
     * Adds a double parameter with default range.
     */
    public IndicatorBuilder<S, R> doubleParam(String name, String displayName, double defaultValue) {
        return doubleParam(name, displayName, defaultValue, 0.0, 100.0);
    }

    /**
     * Adds a color parameter.
     */
    public IndicatorBuilder<S, R> colorParam(String name, String displayName, Color defaultValue) {
        parameters.put(name, new IndicatorParameter.ColorParam(name, displayName, defaultValue));
        return this;
    }

    /**
     * Adds a boolean parameter.
     */
    public IndicatorBuilder<S, R> boolParam(String name, String displayName, boolean defaultValue) {
        parameters.put(name, new IndicatorParameter.BooleanParam(name, displayName, defaultValue));
        return this;
    }

    /**
     * Adds an enum parameter.
     */
    public <E extends Enum<E>> IndicatorBuilder<S, R> enumParam(String name, String displayName,
                                                                 E defaultValue, Class<E> enumClass) {
        parameters.put(name, new IndicatorParameter.EnumParam<>(name, displayName, defaultValue, enumClass));
        return this;
    }

    /**
     * Sets the factory function that creates indicator instances.
     */
    public IndicatorBuilder<S, R> factory(Function<Map<String, Object>, Indicator<S, R>> factory) {
        this.factory = factory;
        return this;
    }

    /**
     * Registers the indicator with the registry.
     *
     * @return the created descriptor
     * @throws IllegalStateException if required fields are missing
     */
    public IndicatorDescriptor register() {
        if (factory == null) {
            throw new IllegalStateException("Factory function is required");
        }

        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                id, displayName, description, category, overlay
        );

        for (IndicatorParameter<?> param : parameters.values()) {
            descriptor.addParameter(param);
        }

        registry.doRegister(descriptor, params -> factory.apply(params));

        return descriptor;
    }

    /**
     * Builds the descriptor without registering.
     * Useful for testing or deferred registration.
     */
    public IndicatorDescriptor build() {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                id, displayName, description, category, overlay
        );

        for (IndicatorParameter<?> param : parameters.values()) {
            descriptor.addParameter(param);
        }

        return descriptor;
    }
}
