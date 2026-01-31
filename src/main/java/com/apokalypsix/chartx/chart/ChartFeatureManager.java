package com.apokalypsix.chartx.chart;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central registry for toggleable chart features.
 *
 * <p>ChartFeatureManager provides:
 * <ul>
 *   <li>Registration of toggleable features with default states</li>
 *   <li>Enable/disable features by ID</li>
 *   <li>Toggle change notifications</li>
 *   <li>Feature grouping and exclusive toggle support</li>
 *   <li>State persistence (save/restore)</li>
 * </ul>
 *
 * <p>Features can be organized into groups where some groups may be
 * mutually exclusive (e.g., only one drawing tool active at a time).
 */
public class ChartFeatureManager {

    /**
     * Feature listener interface for toggle state changes.
     */
    @FunctionalInterface
    public interface FeatureListener {
        /**
         * Called when a feature's enabled state changes.
         *
         * @param featureId the feature ID
         * @param enabled the new state
         */
        void onFeatureToggled(String featureId, boolean enabled);
    }

    /**
     * Describes a registered feature.
     */
    public record FeatureDescriptor(
            String id,
            String name,
            String description,
            String group,
            boolean defaultEnabled,
            boolean exclusive
    ) {
        /**
         * Creates a feature descriptor with basic info.
         */
        public FeatureDescriptor(String id, String name, boolean defaultEnabled) {
            this(id, name, null, null, defaultEnabled, false);
        }

        /**
         * Creates a feature descriptor with group info.
         */
        public FeatureDescriptor(String id, String name, String group, boolean defaultEnabled) {
            this(id, name, null, group, defaultEnabled, false);
        }
    }

    // Feature registry
    private final Map<String, FeatureDescriptor> features = new LinkedHashMap<>();
    private final Map<String, Boolean> featureStates = new HashMap<>();

    // Groups and exclusive toggle handling
    private final Map<String, Set<String>> featureGroups = new HashMap<>();
    private final Set<String> exclusiveGroups = new HashSet<>();

    // Listeners
    private final List<FeatureListener> globalListeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<Boolean>>> featureListeners = new HashMap<>();

    // ========== Feature Registration ==========

    /**
     * Registers a simple toggleable feature.
     *
     * @param id unique feature ID
     * @param name display name
     * @param defaultEnabled initial enabled state
     * @return this manager for chaining
     */
    public ChartFeatureManager registerFeature(String id, String name, boolean defaultEnabled) {
        return registerFeature(new FeatureDescriptor(id, name, defaultEnabled));
    }

    /**
     * Registers a feature with full descriptor.
     *
     * @param descriptor the feature descriptor
     * @return this manager for chaining
     */
    public ChartFeatureManager registerFeature(FeatureDescriptor descriptor) {
        features.put(descriptor.id(), descriptor);
        featureStates.put(descriptor.id(), descriptor.defaultEnabled());

        // Add to group if specified
        if (descriptor.group() != null) {
            featureGroups.computeIfAbsent(descriptor.group(), k -> new LinkedHashSet<>())
                    .add(descriptor.id());
        }

        return this;
    }

    /**
     * Registers a group of mutually exclusive features.
     * Only one feature in the group can be enabled at a time.
     *
     * @param groupName the group name
     * @param featureIds features to add to the group
     * @return this manager for chaining
     */
    public ChartFeatureManager registerExclusiveGroup(String groupName, String... featureIds) {
        exclusiveGroups.add(groupName);
        Set<String> group = featureGroups.computeIfAbsent(groupName, k -> new LinkedHashSet<>());
        group.addAll(Arrays.asList(featureIds));

        // Update feature descriptors to reference the group
        for (String id : featureIds) {
            FeatureDescriptor existing = features.get(id);
            if (existing != null && existing.group() == null) {
                features.put(id, new FeatureDescriptor(
                        existing.id(),
                        existing.name(),
                        existing.description(),
                        groupName,
                        existing.defaultEnabled(),
                        true
                ));
            }
        }

        return this;
    }

    // ========== Feature State Management ==========

    /**
     * Returns true if the feature is enabled.
     *
     * @param featureId the feature ID
     * @return true if enabled, false if disabled or not registered
     */
    public boolean isFeatureEnabled(String featureId) {
        return featureStates.getOrDefault(featureId, false);
    }

    /**
     * Sets whether a feature is enabled.
     *
     * @param featureId the feature ID
     * @param enabled the new state
     */
    public void setFeatureEnabled(String featureId, boolean enabled) {
        FeatureDescriptor descriptor = features.get(featureId);
        if (descriptor == null) {
            return;
        }

        Boolean currentState = featureStates.get(featureId);
        if (currentState != null && currentState == enabled) {
            return;  // No change
        }

        // Handle exclusive groups
        if (enabled && descriptor.group() != null && exclusiveGroups.contains(descriptor.group())) {
            // Disable other features in the same exclusive group
            Set<String> group = featureGroups.get(descriptor.group());
            if (group != null) {
                for (String otherId : group) {
                    if (!otherId.equals(featureId) && Boolean.TRUE.equals(featureStates.get(otherId))) {
                        featureStates.put(otherId, false);
                        notifyListeners(otherId, false);
                    }
                }
            }
        }

        // Update state
        featureStates.put(featureId, enabled);
        notifyListeners(featureId, enabled);
    }

    /**
     * Toggles a feature's enabled state.
     *
     * @param featureId the feature ID
     * @return the new state
     */
    public boolean toggleFeature(String featureId) {
        boolean newState = !isFeatureEnabled(featureId);
        setFeatureEnabled(featureId, newState);
        return newState;
    }

    /**
     * Enables a feature.
     *
     * @param featureId the feature ID
     */
    public void enableFeature(String featureId) {
        setFeatureEnabled(featureId, true);
    }

    /**
     * Disables a feature.
     *
     * @param featureId the feature ID
     */
    public void disableFeature(String featureId) {
        setFeatureEnabled(featureId, false);
    }

    /**
     * Disables all features in a group.
     *
     * @param groupName the group name
     */
    public void disableGroup(String groupName) {
        Set<String> group = featureGroups.get(groupName);
        if (group != null) {
            for (String featureId : group) {
                setFeatureEnabled(featureId, false);
            }
        }
    }

    /**
     * Resets a feature to its default state.
     *
     * @param featureId the feature ID
     */
    public void resetToDefault(String featureId) {
        FeatureDescriptor descriptor = features.get(featureId);
        if (descriptor != null) {
            setFeatureEnabled(featureId, descriptor.defaultEnabled());
        }
    }

    /**
     * Resets all features to their default states.
     */
    public void resetAllToDefaults() {
        for (FeatureDescriptor descriptor : features.values()) {
            setFeatureEnabled(descriptor.id(), descriptor.defaultEnabled());
        }
    }

    // ========== Query Methods ==========

    /**
     * Returns the descriptor for a feature.
     */
    public FeatureDescriptor getFeature(String featureId) {
        return features.get(featureId);
    }

    /**
     * Returns all registered features.
     */
    public Collection<FeatureDescriptor> getAllFeatures() {
        return Collections.unmodifiableCollection(features.values());
    }

    /**
     * Returns all enabled feature IDs.
     */
    public Set<String> getActiveFeatures() {
        Set<String> active = new LinkedHashSet<>();
        for (Map.Entry<String, Boolean> entry : featureStates.entrySet()) {
            if (entry.getValue()) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    /**
     * Returns all features in a group.
     */
    public Set<String> getFeaturesInGroup(String groupName) {
        Set<String> group = featureGroups.get(groupName);
        return group != null ? Collections.unmodifiableSet(group) : Collections.emptySet();
    }

    /**
     * Returns the currently active feature in an exclusive group.
     *
     * @param groupName the group name
     * @return the active feature ID, or null if none active
     */
    public String getActiveInGroup(String groupName) {
        Set<String> group = featureGroups.get(groupName);
        if (group != null) {
            for (String featureId : group) {
                if (Boolean.TRUE.equals(featureStates.get(featureId))) {
                    return featureId;
                }
            }
        }
        return null;
    }

    /**
     * Returns all group names.
     */
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(featureGroups.keySet());
    }

    /**
     * Returns true if a group is exclusive.
     */
    public boolean isExclusiveGroup(String groupName) {
        return exclusiveGroups.contains(groupName);
    }

    // ========== Listeners ==========

    /**
     * Adds a global listener for all feature changes.
     */
    public void addListener(FeatureListener listener) {
        globalListeners.add(listener);
    }

    /**
     * Removes a global listener.
     */
    public void removeListener(FeatureListener listener) {
        globalListeners.remove(listener);
    }

    /**
     * Adds a listener for a specific feature.
     *
     * @param featureId the feature ID
     * @param listener callback when the feature is toggled
     */
    public void addToggleListener(String featureId, Consumer<Boolean> listener) {
        featureListeners.computeIfAbsent(featureId, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    /**
     * Removes a listener for a specific feature.
     */
    public void removeToggleListener(String featureId, Consumer<Boolean> listener) {
        List<Consumer<Boolean>> listeners = featureListeners.get(featureId);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners(String featureId, boolean enabled) {
        // Notify global listeners
        for (FeatureListener listener : globalListeners) {
            listener.onFeatureToggled(featureId, enabled);
        }

        // Notify feature-specific listeners
        List<Consumer<Boolean>> listeners = featureListeners.get(featureId);
        if (listeners != null) {
            for (Consumer<Boolean> listener : listeners) {
                listener.accept(enabled);
            }
        }
    }

    // ========== State Persistence ==========

    /**
     * Exports the current feature states as a map.
     *
     * @return map of feature ID to enabled state
     */
    public Map<String, Boolean> exportState() {
        return new HashMap<>(featureStates);
    }

    /**
     * Imports feature states from a map.
     *
     * @param state map of feature ID to enabled state
     */
    public void importState(Map<String, Boolean> state) {
        for (Map.Entry<String, Boolean> entry : state.entrySet()) {
            if (features.containsKey(entry.getKey())) {
                setFeatureEnabled(entry.getKey(), entry.getValue());
            }
        }
    }

    // ========== Common Feature IDs ==========

    /** Feature ID for crosshair display */
    public static final String FEATURE_CROSSHAIR = "crosshair";

    /** Feature ID for grid display */
    public static final String FEATURE_GRID = "grid";

    /** Feature ID for volume bars */
    public static final String FEATURE_VOLUME = "volume";

    /** Feature ID for alternating session backgrounds */
    public static final String FEATURE_SESSION_BACKGROUNDS = "sessionBackgrounds";

    /** Feature ID for day separator lines */
    public static final String FEATURE_DAY_SEPARATORS = "daySeparators";

    /** Feature ID for price axis */
    public static final String FEATURE_PRICE_AXIS = "priceAxis";

    /** Feature ID for time axis */
    public static final String FEATURE_TIME_AXIS = "timeAxis";

    /** Feature ID for snap to candle */
    public static final String FEATURE_SNAP_TO_CANDLE = "snapToCandle";

    /** Feature ID for auto-scale */
    public static final String FEATURE_AUTO_SCALE = "autoScale";

    /**
     * Registers common chart features with typical defaults.
     *
     * @return this manager for chaining
     */
    public ChartFeatureManager registerCommonFeatures() {
        registerFeature(FEATURE_CROSSHAIR, "Crosshair", true);
        registerFeature(FEATURE_GRID, "Grid", true);
        registerFeature(FEATURE_VOLUME, "Volume", true);
        registerFeature(FEATURE_SESSION_BACKGROUNDS, "Session Backgrounds", true);
        registerFeature(FEATURE_DAY_SEPARATORS, "Day Separators", false);
        registerFeature(FEATURE_PRICE_AXIS, "Price Axis", true);
        registerFeature(FEATURE_TIME_AXIS, "Time Axis", true);
        registerFeature(FEATURE_SNAP_TO_CANDLE, "Snap to Candle", true);
        registerFeature(FEATURE_AUTO_SCALE, "Auto Scale", true);
        return this;
    }
}
