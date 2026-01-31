package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.finance.indicator.custom.CustomIndicatorRegistry;
import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active indicators on a chart.
 *
 * <p>The IndicatorManager handles:
 * <ul>
 *   <li>Adding and removing indicator instances</li>
 *   <li>Calculating indicator values when source data changes</li>
 *   <li>Managing indicator visibility and enabling/disabling</li>
 *   <li>Registering available indicator types</li>
 * </ul>
 *
 * <p>Indicators are calculated lazily when needed and are updated incrementally
 * when new data arrives.
 */
public class IndicatorManager {

    private static final Logger log = LoggerFactory.getLogger(IndicatorManager.class);

    /**
     * Listener interface for indicator manager events.
     */
    public interface IndicatorListener {
        /** Called when an indicator is added */
        void onIndicatorAdded(IndicatorInstance<?, ?> instance);

        /** Called when an indicator is removed */
        void onIndicatorRemoved(IndicatorInstance<?, ?> instance);

        /** Called when an indicator's enabled state changes */
        void onIndicatorEnabledChanged(IndicatorInstance<?, ?> instance, boolean enabled);

        /** Called when an indicator is recalculated */
        void onIndicatorRecalculated(IndicatorInstance<?, ?> instance);

        /** Called when parameter changes are staged (not yet applied) */
        default void onIndicatorParametersStaged(IndicatorInstance<?, ?> instance) {}

        /** Called when staged parameter changes are applied */
        default void onIndicatorParametersApplied(IndicatorInstance<?, ?> instance) {}

        /** Called when staged parameter changes are discarded */
        default void onIndicatorChangesDiscarded(IndicatorInstance<?, ?> instance) {}
    }

    /**
     * Factory interface for creating indicator instances.
     */
    @FunctionalInterface
    public interface IndicatorFactory<S extends Data<?>, R extends Data<?>> {
        Indicator<S, R> create(Map<String, Object> parameters);
    }

    private final Map<String, IndicatorDescriptor> registeredIndicators = new LinkedHashMap<>();
    private final Map<String, IndicatorFactory<?, ?>> indicatorFactories = new HashMap<>();
    private final Map<String, IndicatorInstance<?, ?>> activeIndicators = new LinkedHashMap<>();
    private final List<IndicatorListener> listeners = new CopyOnWriteArrayList<>();
    private final CustomIndicatorRegistry customRegistry;

    private OhlcData sourceData;
    private final DataListener sourceListener;

    /**
     * Creates an indicator manager.
     */
    public IndicatorManager() {
        // Initialize custom indicator registry
        this.customRegistry = new CustomIndicatorRegistry();

        // Create a listener to invalidate indicators when source data changes
        this.sourceListener = new DataListener() {
            @Override
            public void onDataAppended(Data<?> data, int newIndex) {
                updateIndicators(newIndex);
            }

            @Override
            public void onDataUpdated(Data<?> data, int index) {
                // Mark all indicators for recalculation from the updated index
                for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
                    instance.markNeedsRecalculation();
                }
            }

            @Override
            public void onDataCleared(Data<?> data) {
                clearAllIndicatorOutputs();
            }
        };
    }

    // ========== Configuration ==========

    /**
     * Sets the source OHLC data for indicator calculations.
     *
     * @param data the source data
     */
    public void setSourceData(OhlcData data) {
        if (this.sourceData != null) {
            this.sourceData.removeListener(sourceListener);
        }

        this.sourceData = data;

        if (data != null) {
            data.addListener(sourceListener);
            // Mark all indicators for recalculation
            for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
                instance.markNeedsRecalculation();
            }
        } else {
            clearAllIndicatorOutputs();
        }
    }

    /**
     * Returns the source data.
     */
    public OhlcData getSourceData() {
        return sourceData;
    }

    // ========== Indicator Registration ==========

    /**
     * Registers an indicator type.
     *
     * @param descriptor the indicator descriptor
     * @param factory factory for creating indicator instances
     */
    public <S extends Data<?>, R extends Data<?>> void registerIndicator(
            IndicatorDescriptor descriptor,
            IndicatorFactory<S, R> factory) {
        registeredIndicators.put(descriptor.getId(), descriptor);
        indicatorFactories.put(descriptor.getId(), factory);
    }

    /**
     * Returns all registered indicator descriptors.
     */
    public Collection<IndicatorDescriptor> getRegisteredIndicators() {
        return Collections.unmodifiableCollection(registeredIndicators.values());
    }

    /**
     * Returns registered indicators by category.
     */
    public List<IndicatorDescriptor> getIndicatorsByCategory(IndicatorCategory category) {
        List<IndicatorDescriptor> result = new ArrayList<>();
        for (IndicatorDescriptor desc : registeredIndicators.values()) {
            if (desc.getCategory() == category) {
                result.add(desc);
            }
        }
        return result;
    }

    /**
     * Returns a registered indicator descriptor by ID.
     */
    public IndicatorDescriptor getIndicatorDescriptor(String indicatorId) {
        return registeredIndicators.get(indicatorId);
    }

    // ========== Active Indicator Management ==========

    /**
     * Adds an indicator to the chart with default parameters.
     *
     * @param indicatorId the registered indicator ID
     * @return the created instance, or null if the indicator is not registered
     */
    public IndicatorInstance<?, ?> addIndicator(String indicatorId) {
        return addIndicator(indicatorId, Collections.emptyMap());
    }

    /**
     * Adds an indicator to the chart with custom parameters.
     *
     * @param indicatorId the registered indicator ID
     * @param parameters parameter overrides
     * @return the created instance, or null if the indicator is not registered
     */
    @SuppressWarnings("unchecked")
    public IndicatorInstance<?, ?> addIndicator(String indicatorId, Map<String, Object> parameters) {
        IndicatorDescriptor descriptor = registeredIndicators.get(indicatorId);
        IndicatorFactory<?, ?> factory = indicatorFactories.get(indicatorId);

        if (descriptor == null || factory == null) {
            return null;
        }

        // Merge default parameters with provided overrides
        Map<String, Object> mergedParams = new LinkedHashMap<>();
        for (IndicatorParameter<?> param : descriptor.getParameters().values()) {
            mergedParams.put(param.name(), param.defaultValue());
        }
        mergedParams.putAll(parameters);

        // Create the indicator instance
        Indicator<?, ?> indicator = ((IndicatorFactory<Data<?>, Data<?>>) factory).create(mergedParams);
        IndicatorInstance<Data<?>, Data<?>> instance =
                new IndicatorInstance<>(descriptor, (Indicator<Data<?>, Data<?>>) indicator);

        // Apply parameter values
        for (Map.Entry<String, Object> entry : mergedParams.entrySet()) {
            if (descriptor.getParameter(entry.getKey()) != null) {
                instance.setParameterValue(entry.getKey(), entry.getValue());
            }
        }

        // Calculate the indicator
        calculateIndicator(instance);

        // Add to active indicators
        activeIndicators.put(instance.getId(), instance);

        // Notify listeners
        for (IndicatorListener listener : listeners) {
            listener.onIndicatorAdded(instance);
        }

        return instance;
    }

    /**
     * Removes an indicator from the chart.
     *
     * @param instanceId the instance ID
     * @return true if the indicator was removed
     */
    public boolean removeIndicator(String instanceId) {
        IndicatorInstance<?, ?> instance = activeIndicators.remove(instanceId);
        if (instance == null) {
            return false;
        }

        // Notify listeners
        for (IndicatorListener listener : listeners) {
            listener.onIndicatorRemoved(instance);
        }

        return true;
    }

    /**
     * Removes all active indicators.
     */
    public void removeAllIndicators() {
        List<String> instanceIds = new ArrayList<>(activeIndicators.keySet());
        for (String instanceId : instanceIds) {
            removeIndicator(instanceId);
        }
    }

    /**
     * Returns an active indicator instance by ID.
     */
    public IndicatorInstance<?, ?> getIndicator(String instanceId) {
        return activeIndicators.get(instanceId);
    }

    /**
     * Returns all active indicator instances.
     */
    public Collection<IndicatorInstance<?, ?>> getActiveIndicators() {
        return Collections.unmodifiableCollection(activeIndicators.values());
    }

    /**
     * Returns the number of active indicators.
     */
    public int getActiveIndicatorCount() {
        return activeIndicators.size();
    }

    /**
     * Sets whether an indicator is enabled.
     *
     * @param instanceId the instance ID
     * @param enabled true to enable
     */
    public void setIndicatorEnabled(String instanceId, boolean enabled) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance != null && instance.isEnabled() != enabled) {
            instance.setEnabled(enabled);

            // Notify listeners
            for (IndicatorListener listener : listeners) {
                listener.onIndicatorEnabledChanged(instance, enabled);
            }
        }
    }

    /**
     * Updates an indicator's parameters.
     *
     * @param instanceId the instance ID
     * @param parameters new parameter values
     */
    public void updateIndicatorParameters(String instanceId, Map<String, Object> parameters) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance == null) {
            return;
        }

        boolean changed = false;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (instance.setParameterValue(entry.getKey(), entry.getValue())) {
                changed = true;
            }
        }

        if (changed) {
            // Recalculate with new parameters
            recalculateIndicator(instanceId);
        }
    }

    // ========== Staged Parameter Changes ====================

    /**
     * Stages a parameter change without recalculating the indicator.
     * The change will be applied when {@link #applyIndicatorChanges(String)} is called.
     *
     * @param instanceId the instance ID
     * @param paramName parameter name
     * @param value the new value
     * @return true if the value was staged
     */
    public boolean stageIndicatorParameter(String instanceId, String paramName, Object value) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance == null) {
            return false;
        }

        boolean staged = instance.stageParameterValue(paramName, value);
        if (staged) {
            for (IndicatorListener listener : listeners) {
                listener.onIndicatorParametersStaged(instance);
            }
        }
        return staged;
    }

    /**
     * Stages multiple parameter changes without recalculating.
     *
     * @param instanceId the instance ID
     * @param parameters map of parameter names to values
     * @return true if any values were staged
     */
    public boolean stageIndicatorParameters(String instanceId, Map<String, Object> parameters) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance == null) {
            return false;
        }

        boolean anyStaged = false;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (instance.stageParameterValue(entry.getKey(), entry.getValue())) {
                anyStaged = true;
            }
        }

        if (anyStaged) {
            for (IndicatorListener listener : listeners) {
                listener.onIndicatorParametersStaged(instance);
            }
        }
        return anyStaged;
    }

    /**
     * Applies staged parameter changes for an indicator and recalculates it.
     *
     * @param instanceId the instance ID
     * @return true if changes were applied
     */
    public boolean applyIndicatorChanges(String instanceId) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance == null) {
            return false;
        }

        if (instance.applyChanges()) {
            // Notify listeners about applied changes
            for (IndicatorListener listener : listeners) {
                listener.onIndicatorParametersApplied(instance);
            }

            // Recalculate with new parameters
            calculateIndicator(instance);

            // Notify listeners about recalculation
            for (IndicatorListener listener : listeners) {
                listener.onIndicatorRecalculated(instance);
            }
            return true;
        }
        return false;
    }

    /**
     * Applies all pending changes across all indicators.
     * More efficient than calling applyIndicatorChanges for each indicator.
     *
     * @return list of instance IDs that had changes applied
     */
    public List<String> applyAllPendingChanges() {
        List<String> applied = new ArrayList<>();

        for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
            if (instance.hasPendingChanges()) {
                instance.applyChanges();
                applied.add(instance.getId());

                // Notify listeners about applied changes
                for (IndicatorListener listener : listeners) {
                    listener.onIndicatorParametersApplied(instance);
                }
            }
        }

        // Recalculate all affected indicators
        for (String instanceId : applied) {
            IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
            calculateIndicator(instance);

            for (IndicatorListener listener : listeners) {
                listener.onIndicatorRecalculated(instance);
            }
        }

        return applied;
    }

    /**
     * Discards staged parameter changes for an indicator.
     *
     * @param instanceId the instance ID
     */
    public void discardIndicatorChanges(String instanceId) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance != null && instance.hasPendingChanges()) {
            instance.discardChanges();

            for (IndicatorListener listener : listeners) {
                listener.onIndicatorChangesDiscarded(instance);
            }
        }
    }

    /**
     * Discards all pending changes across all indicators.
     */
    public void discardAllPendingChanges() {
        for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
            if (instance.hasPendingChanges()) {
                instance.discardChanges();

                for (IndicatorListener listener : listeners) {
                    listener.onIndicatorChangesDiscarded(instance);
                }
            }
        }
    }

    /**
     * Returns all indicators that have pending (unapplied) changes.
     */
    public List<IndicatorInstance<?, ?>> getIndicatorsWithPendingChanges() {
        List<IndicatorInstance<?, ?>> result = new ArrayList<>();
        for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
            if (instance.hasPendingChanges()) {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * Returns true if any indicator has pending changes.
     */
    public boolean hasAnyPendingChanges() {
        for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
            if (instance.hasPendingChanges()) {
                return true;
            }
        }
        return false;
    }

    // ========== Calculation ==========

    /**
     * Recalculates a specific indicator.
     */
    public void recalculateIndicator(String instanceId) {
        IndicatorInstance<?, ?> instance = activeIndicators.get(instanceId);
        if (instance != null) {
            // Recalculate
            calculateIndicator(instance);

            // Notify listeners
            for (IndicatorListener listener : listeners) {
                listener.onIndicatorRecalculated(instance);
            }
        }
    }

    /**
     * Recalculates all indicators.
     */
    public void recalculateAllIndicators() {
        for (String instanceId : activeIndicators.keySet()) {
            recalculateIndicator(instanceId);
        }
    }

    @SuppressWarnings("unchecked")
    private void calculateIndicator(IndicatorInstance<?, ?> instance) {
        log.debug("calculateIndicator: {} sourceData={}", instance.getDescriptor().getId(),
                sourceData != null ? sourceData.size() : "null");
        if (sourceData == null || sourceData.isEmpty()) {
            log.debug("calculateIndicator: skipping - no source data");
            return;
        }

        Indicator<OhlcData, ?> indicator =
                (Indicator<OhlcData, ?>) instance.getIndicator();

        Data<?> output = indicator.calculate(sourceData);
        log.debug("calculateIndicator: {} output={}", instance.getDescriptor().getId(),
                output != null ? output.size() : "null");
        ((IndicatorInstance<OhlcData, Data<?>>) instance).setOutputData(output);
        instance.markRecalculated();
    }

    @SuppressWarnings("unchecked")
    private void updateIndicators(int fromIndex) {
        if (sourceData == null) {
            return;
        }

        for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
            if (instance.isEnabled() && instance.getOutputData() != null) {
                Indicator<OhlcData, Data<?>> indicator =
                        (Indicator<OhlcData, Data<?>>) instance.getIndicator();
                indicator.update(instance.getOutputData(), sourceData, fromIndex);
            }
        }
    }

    private void clearAllIndicatorOutputs() {
        for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
            instance.setOutputData(null);
            instance.markNeedsRecalculation();
        }
    }

    /**
     * Calculates all indicators using the provided data.
     *
     * @param data the source data (typically OhlcData)
     */
    @SuppressWarnings("unchecked")
    public void calculateAll(Object data) {
        if (data instanceof OhlcData) {
            OhlcData ohlcData = (OhlcData) data;
            for (IndicatorInstance<?, ?> instance : activeIndicators.values()) {
                if (instance.isEnabled()) {
                    Indicator<OhlcData, ?> indicator =
                            (Indicator<OhlcData, ?>) instance.getIndicator();
                    Data<?> output = indicator.calculate(ohlcData);
                    ((IndicatorInstance<OhlcData, Data<?>>) instance).setOutputData(output);
                    instance.markRecalculated();

                    // Notify listeners
                    for (IndicatorListener listener : listeners) {
                        listener.onIndicatorRecalculated(instance);
                    }
                }
            }
        }
    }

    // ========== Accessors ==========

    /**
     * Returns all active indicator instances as a list.
     */
    public List<IndicatorInstance<?, ?>> getIndicators() {
        return new ArrayList<>(activeIndicators.values());
    }

    /**
     * Returns the custom indicator registry.
     * Use this to register user-defined custom indicators.
     *
     * @return the custom indicator registry
     */
    public CustomIndicatorRegistry getCustomRegistry() {
        return customRegistry;
    }

    // ========== Listeners ==========

    /**
     * Adds an indicator listener.
     */
    public void addListener(IndicatorListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes an indicator listener.
     */
    public void removeListener(IndicatorListener listener) {
        listeners.remove(listener);
    }
}
