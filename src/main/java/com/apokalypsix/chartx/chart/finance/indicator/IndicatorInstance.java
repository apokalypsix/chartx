package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.data.Data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an active indicator instance on a chart.
 *
 * <p>An indicator instance holds the configured parameters, the output data,
 * and tracks whether it's enabled. Multiple instances of the same indicator type
 * can exist with different parameters (e.g., SMA(20) and SMA(50)).
 *
 * @param <S> the source data type
 * @param <R> the result data type
 */
public class IndicatorInstance<S extends Data<?>, R extends Data<?>> {

    private final String id;
    private final IndicatorDescriptor descriptor;
    private final Indicator<S, R> indicator;
    private final Map<String, Object> parameterValues;
    private final Map<String, Object> pendingParameters;
    private R outputData;
    private boolean enabled = true;
    private boolean needsRecalculation = true;
    private boolean hasPendingChanges = false;

    /**
     * Creates a new indicator instance.
     *
     * @param descriptor the indicator descriptor
     * @param indicator the indicator implementation
     */
    public IndicatorInstance(IndicatorDescriptor descriptor, Indicator<S, R> indicator) {
        this(UUID.randomUUID().toString().substring(0, 8), descriptor, indicator);
    }

    /**
     * Creates a new indicator instance with a specific ID.
     *
     * @param id unique instance ID
     * @param descriptor the indicator descriptor
     * @param indicator the indicator implementation
     */
    public IndicatorInstance(String id, IndicatorDescriptor descriptor, Indicator<S, R> indicator) {
        this.id = Objects.requireNonNull(id);
        this.descriptor = Objects.requireNonNull(descriptor);
        this.indicator = Objects.requireNonNull(indicator);
        this.parameterValues = new LinkedHashMap<>();
        this.pendingParameters = new LinkedHashMap<>();

        // Initialize with default parameter values
        for (IndicatorParameter<?> param : descriptor.getParameters().values()) {
            parameterValues.put(param.name(), param.defaultValue());
        }
    }

    /**
     * Returns the unique instance ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the indicator descriptor.
     */
    public IndicatorDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the indicator implementation.
     */
    public Indicator<S, R> getIndicator() {
        return indicator;
    }

    /**
     * Returns the output data, or null if not yet calculated.
     */
    public R getOutputData() {
        return outputData;
    }

    /**
     * Sets the output data.
     */
    public void setOutputData(R outputData) {
        this.outputData = outputData;
    }

    /**
     * Returns true if this indicator is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this indicator is enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns true if this indicator needs recalculation.
     */
    public boolean needsRecalculation() {
        return needsRecalculation;
    }

    /**
     * Marks this indicator as needing recalculation.
     */
    public void markNeedsRecalculation() {
        this.needsRecalculation = true;
    }

    /**
     * Marks this indicator as having been recalculated.
     */
    public void markRecalculated() {
        this.needsRecalculation = false;
    }

    /**
     * Returns all parameter values (unmodifiable).
     */
    public Map<String, Object> getParameterValues() {
        return Collections.unmodifiableMap(parameterValues);
    }

    /**
     * Returns a parameter value.
     *
     * @param name parameter name
     * @return the current value, or null if not set
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameterValue(String name) {
        return (T) parameterValues.get(name);
    }

    /**
     * Returns a parameter value with type-safe casting.
     *
     * @param param the parameter definition
     * @return the current value
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameterValue(IndicatorParameter<T> param) {
        Object value = parameterValues.get(param.name());
        return value != null ? (T) value : param.defaultValue();
    }

    /**
     * Sets a parameter value.
     *
     * @param name parameter name
     * @param value the new value
     * @return true if the value changed
     * @throws IllegalArgumentException if the value is invalid
     */
    public boolean setParameterValue(String name, Object value) {
        IndicatorParameter<?> param = descriptor.getParameter(name);
        if (param == null) {
            throw new IllegalArgumentException("Unknown parameter: " + name);
        }

        @SuppressWarnings("unchecked")
        IndicatorParameter<Object> typedParam = (IndicatorParameter<Object>) param;
        if (!typedParam.isValid(value)) {
            throw new IllegalArgumentException("Invalid value for " + name + ": " + value);
        }

        Object oldValue = parameterValues.put(name, value);
        if (!Objects.equals(oldValue, value)) {
            needsRecalculation = true;
            return true;
        }
        return false;
    }

    /**
     * Resets all parameters to their default values.
     */
    public void resetToDefaults() {
        for (IndicatorParameter<?> param : descriptor.getParameters().values()) {
            parameterValues.put(param.name(), param.defaultValue());
        }
        pendingParameters.clear();
        hasPendingChanges = false;
        needsRecalculation = true;
    }

    // ==================== Pending Parameter Changes ====================

    /**
     * Stages a parameter change without triggering recalculation.
     * The change will be applied when {@link #applyChanges()} is called.
     *
     * @param name parameter name
     * @param value the new value
     * @return true if the value is valid and was staged
     * @throws IllegalArgumentException if the parameter name is unknown or value is invalid
     */
    public boolean stageParameterValue(String name, Object value) {
        IndicatorParameter<?> param = descriptor.getParameter(name);
        if (param == null) {
            throw new IllegalArgumentException("Unknown parameter: " + name);
        }

        @SuppressWarnings("unchecked")
        IndicatorParameter<Object> typedParam = (IndicatorParameter<Object>) param;
        if (!typedParam.isValid(value)) {
            throw new IllegalArgumentException("Invalid value for " + name + ": " + value);
        }

        // Check if the value differs from the current applied value
        Object currentValue = parameterValues.get(name);
        if (Objects.equals(currentValue, value)) {
            // Remove from pending if it was previously staged
            pendingParameters.remove(name);
            hasPendingChanges = !pendingParameters.isEmpty();
            return false;
        }

        pendingParameters.put(name, value);
        hasPendingChanges = true;
        return true;
    }

    /**
     * Applies all staged parameter changes and marks the indicator for recalculation.
     * After calling this method, {@link #hasPendingChanges()} will return false.
     *
     * @return true if any changes were applied
     */
    public boolean applyChanges() {
        if (pendingParameters.isEmpty()) {
            return false;
        }

        parameterValues.putAll(pendingParameters);
        pendingParameters.clear();
        hasPendingChanges = false;
        needsRecalculation = true;
        return true;
    }

    /**
     * Discards all staged parameter changes without applying them.
     * After calling this method, {@link #hasPendingChanges()} will return false.
     */
    public void discardChanges() {
        pendingParameters.clear();
        hasPendingChanges = false;
    }

    /**
     * Returns true if there are pending parameter changes that haven't been applied.
     */
    public boolean hasPendingChanges() {
        return hasPendingChanges;
    }

    /**
     * Returns the pending value for a parameter, or null if no change is staged.
     *
     * @param name parameter name
     * @return the staged value, or null if not staged
     */
    @SuppressWarnings("unchecked")
    public <T> T getPendingParameterValue(String name) {
        return (T) pendingParameters.get(name);
    }

    /**
     * Returns the effective value for a parameter (pending if staged, otherwise current).
     * Use this method in UI to show the value the user will see after applying.
     *
     * @param name parameter name
     * @return the effective value (pending if exists, otherwise current)
     */
    @SuppressWarnings("unchecked")
    public <T> T getEffectiveParameterValue(String name) {
        if (pendingParameters.containsKey(name)) {
            return (T) pendingParameters.get(name);
        }
        return (T) parameterValues.get(name);
    }

    /**
     * Returns all pending parameter changes (unmodifiable).
     */
    public Map<String, Object> getPendingParameters() {
        return Collections.unmodifiableMap(pendingParameters);
    }

    /**
     * Generates a display name for this instance based on its parameters.
     * For example: "SMA(20)" or "Bollinger(20, 2.0)"
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(descriptor.getName());
        if (!parameterValues.isEmpty()) {
            sb.append("(");
            boolean first = true;
            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                IndicatorParameter<?> param = descriptor.getParameter(entry.getKey());
                // Only include numeric parameters in the short name
                if (param instanceof IndicatorParameter.IntParam ||
                    param instanceof IndicatorParameter.DoubleParam) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(entry.getValue());
                    first = false;
                }
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("IndicatorInstance[%s: %s, enabled=%s]",
                id, getDisplayName(), enabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IndicatorInstance<?, ?> that = (IndicatorInstance<?, ?>) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
