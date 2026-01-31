package com.apokalypsix.chartx.chart.finance.indicator.custom;

import com.apokalypsix.chartx.chart.finance.indicator.*;
import com.apokalypsix.chartx.chart.data.Data;

import java.util.*;

/**
 * Registry for custom user-defined indicators.
 *
 * <p>Allows users to register their own indicators alongside the built-in ones.
 * Custom indicators can be created using the fluent builder API or by directly
 * implementing the Indicator interface.
 *
 * <p>Example usage:
 * <pre>
 * CustomIndicatorRegistry customRegistry = new CustomIndicatorRegistry();
 *
 * // Using the builder
 * customRegistry.builder("my_indicator")
 *     .name("My Custom Indicator")
 *     .category(IndicatorCategory.CUSTOM)
 *     .overlay(true)
 *     .intParam("period", "Period", 20, 1, 500)
 *     .factory(params -> new MyIndicator((Integer) params.get("period")))
 *     .register();
 *
 * // Register all custom indicators with the main manager
 * customRegistry.registerAllWith(indicatorManager);
 * </pre>
 *
 * <p>Custom indicators are kept in a separate registry to:
 * <ul>
 *   <li>Keep user code isolated from library internals</li>
 *   <li>Allow easy listing of custom vs. built-in indicators</li>
 *   <li>Support saving/loading custom indicator configurations</li>
 * </ul>
 */
public class CustomIndicatorRegistry {

    private final Map<String, IndicatorDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, IndicatorManager.IndicatorFactory<?, ?>> factories = new HashMap<>();

    /**
     * Creates an empty custom registry.
     */
    public CustomIndicatorRegistry() {
    }

    /**
     * Starts building a new custom indicator.
     *
     * @param id unique identifier for the indicator
     * @param <S> source data type
     * @param <R> result data type
     * @return a builder for the indicator
     */
    public <S extends Data<?>, R extends Data<?>> IndicatorBuilder<S, R> builder(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Indicator ID cannot be null or empty");
        }
        return new IndicatorBuilder<>(id, this);
    }

    /**
     * Registers a custom indicator directly.
     *
     * @param descriptor the indicator descriptor
     * @param factory the factory for creating instances
     * @param <S> source data type
     * @param <R> result data type
     */
    public <S extends Data<?>, R extends Data<?>> void register(
            IndicatorDescriptor descriptor,
            IndicatorManager.IndicatorFactory<S, R> factory) {
        doRegister(descriptor, factory);
    }

    /**
     * Internal registration method called by the builder.
     */
    <S extends Data<?>, R extends Data<?>> void doRegister(
            IndicatorDescriptor descriptor,
            IndicatorManager.IndicatorFactory<S, R> factory) {
        String id = descriptor.getId();
        if (descriptors.containsKey(id)) {
            throw new IllegalArgumentException("Indicator already registered: " + id);
        }
        descriptors.put(id, descriptor);
        factories.put(id, factory);
    }

    /**
     * Unregisters a custom indicator.
     *
     * @param id the indicator ID to unregister
     * @return true if the indicator was unregistered
     */
    public boolean unregister(String id) {
        if (descriptors.remove(id) != null) {
            factories.remove(id);
            return true;
        }
        return false;
    }

    /**
     * Clears all registered custom indicators.
     */
    public void clear() {
        descriptors.clear();
        factories.clear();
    }

    /**
     * Returns all registered custom indicator descriptors.
     */
    public Collection<IndicatorDescriptor> getDescriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    /**
     * Returns a specific descriptor by ID.
     */
    public IndicatorDescriptor getDescriptor(String id) {
        return descriptors.get(id);
    }

    /**
     * Returns true if an indicator with the given ID is registered.
     */
    public boolean isRegistered(String id) {
        return descriptors.containsKey(id);
    }

    /**
     * Returns the number of registered custom indicators.
     */
    public int size() {
        return descriptors.size();
    }

    /**
     * Returns true if no custom indicators are registered.
     */
    public boolean isEmpty() {
        return descriptors.isEmpty();
    }

    /**
     * Registers all custom indicators with an IndicatorManager.
     * This makes the custom indicators available for use on charts.
     *
     * @param manager the indicator manager to register with
     */
    public void registerAllWith(IndicatorManager manager) {
        for (String id : descriptors.keySet()) {
            IndicatorDescriptor descriptor = descriptors.get(id);
            @SuppressWarnings("unchecked")
            IndicatorManager.IndicatorFactory<Data<?>, Data<?>> factory =
                    (IndicatorManager.IndicatorFactory<Data<?>, Data<?>>) factories.get(id);
            manager.registerIndicator(descriptor, factory);
        }
    }

    /**
     * Registers a single custom indicator with an IndicatorManager.
     *
     * @param id the indicator ID to register
     * @param manager the indicator manager to register with
     * @return true if the indicator was registered
     */
    public boolean registerWith(String id, IndicatorManager manager) {
        IndicatorDescriptor descriptor = descriptors.get(id);
        if (descriptor == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        IndicatorManager.IndicatorFactory<Data<?>, Data<?>> factory =
                (IndicatorManager.IndicatorFactory<Data<?>, Data<?>>) factories.get(id);
        manager.registerIndicator(descriptor, factory);
        return true;
    }

    /**
     * Returns custom indicators by category.
     */
    public List<IndicatorDescriptor> getByCategory(IndicatorCategory category) {
        List<IndicatorDescriptor> result = new ArrayList<>();
        for (IndicatorDescriptor desc : descriptors.values()) {
            if (desc.getCategory() == category) {
                result.add(desc);
            }
        }
        return result;
    }

    /**
     * Creates a new indicator instance from the registry.
     *
     * @param id the indicator ID
     * @param parameters parameter values
     * @return the created indicator, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <S extends Data<?>, R extends Data<?>> Indicator<S, R> createInstance(
            String id, Map<String, Object> parameters) {
        IndicatorManager.IndicatorFactory<S, R> factory =
                (IndicatorManager.IndicatorFactory<S, R>) factories.get(id);
        if (factory == null) {
            return null;
        }
        return factory.create(parameters);
    }
}
