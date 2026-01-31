package com.apokalypsix.chartx.chart.finance.indicator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes an indicator's metadata and parameters.
 *
 * <p>This class provides information about an indicator type without
 * requiring an instance of the indicator. It's used by the IndicatorManager
 * to list available indicators and create configuration dialogs.
 */
public class IndicatorDescriptor {

    private final String id;
    private final String name;
    private final String description;
    private final IndicatorCategory category;
    private final Map<String, IndicatorParameter<?>> parameters;
    private final boolean overlayOnPrice;  // true = overlay on price chart, false = separate pane

    /**
     * Creates an indicator descriptor.
     *
     * @param id unique identifier (e.g., "sma", "rsi")
     * @param name display name (e.g., "Simple Moving Average")
     * @param description brief description of the indicator
     * @param category indicator category
     * @param overlayOnPrice true if this indicator overlays on the price chart
     */
    public IndicatorDescriptor(String id, String name, String description,
                               IndicatorCategory category, boolean overlayOnPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.overlayOnPrice = overlayOnPrice;
        this.parameters = new LinkedHashMap<>();
    }

    /**
     * Adds a parameter to this descriptor.
     *
     * @param param the parameter to add
     * @return this descriptor for chaining
     */
    public IndicatorDescriptor addParameter(IndicatorParameter<?> param) {
        parameters.put(param.name(), param);
        return this;
    }

    /**
     * Returns the unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the category.
     */
    public IndicatorCategory getCategory() {
        return category;
    }

    /**
     * Returns true if this indicator overlays on the price chart.
     */
    public boolean isOverlayOnPrice() {
        return overlayOnPrice;
    }

    /**
     * Returns all parameters (unmodifiable).
     */
    public Map<String, IndicatorParameter<?>> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Returns a parameter by name.
     */
    public IndicatorParameter<?> getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Returns true if this descriptor has any configurable parameters.
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("IndicatorDescriptor[%s: %s]", id, name);
    }
}
