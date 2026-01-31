package com.apokalypsix.chartx.core.render.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.util.*;

/**
 * Factory for creating rendering backend implementations.
 *
 * <p>This factory discovers available backends at runtime using Java's
 * ServiceLoader mechanism. Backend modules register their providers via
 * {@code META-INF/services/com.apokalypsix.chartx.core.render.api.RenderBackendProvider}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Auto-select best available backend
 * RenderDevice device = RenderBackendFactory.createDevice(RenderBackend.AUTO, component);
 *
 * // Or explicitly request a specific backend
 * RenderDevice device = RenderBackendFactory.createDevice(RenderBackend.VULKAN, component);
 * }</pre>
 *
 * <h2>Adding Backend Support</h2>
 * <p>To add a backend, include its module as a dependency:
 * <pre>{@code
 * <dependency>
 *     <groupId>com.apokalypsix</groupId>
 *     <artifactId>chartx-backend-vulkan</artifactId>
 * </dependency>
 * }</pre>
 */
public class RenderBackendFactory {

    private static final Logger log = LoggerFactory.getLogger(RenderBackendFactory.class);

    private static volatile Map<RenderBackend, RenderBackendProvider> providers;
    private static final Object LOCK = new Object();

    private RenderBackendFactory() {
        // Utility class
    }

    /**
     * Creates a render device for the specified backend.
     *
     * @param backend the desired backend (use AUTO for best available)
     * @param target the target Swing component (for context creation)
     * @return the render device
     * @throws UnsupportedOperationException if the backend is not available
     */
    public static RenderDevice createDevice(RenderBackend backend, Component target) {
        RenderBackend selected = backend;

        if (backend == RenderBackend.AUTO) {
            selected = detectBestBackend();
        }

        RenderBackendProvider provider = getProvider(selected);
        if (provider == null) {
            throw new UnsupportedOperationException(
                    "Backend " + selected + " is not available. Available backends: " + getAvailableBackends());
        }

        log.debug("Creating render device for backend: {}", selected);
        return provider.createDevice(target);
    }

    /**
     * Creates a resource manager for the specified render device.
     *
     * @param device the render device
     * @return the resource manager
     */
    public static ResourceManager createResourceManager(RenderDevice device) {
        RenderBackend backend = device.getBackendType();
        RenderBackendProvider provider = getProvider(backend);

        if (provider == null) {
            throw new UnsupportedOperationException(
                    "No provider found for backend: " + backend);
        }

        return provider.createResourceManager();
    }

    /**
     * Detects the best available backend for the current platform.
     *
     * <p>Selection is based on provider priority, with higher priority
     * backends preferred. Only backends that report as available are considered.
     *
     * @return the recommended backend
     * @throws UnsupportedOperationException if no backends are available
     */
    public static RenderBackend detectBestBackend() {
        ensureProvidersLoaded();

        RenderBackendProvider best = null;
        for (RenderBackendProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                if (best == null || provider.getPriority() > best.getPriority()) {
                    best = provider;
                }
            }
        }

        if (best == null) {
            throw new UnsupportedOperationException(
                    "No rendering backends available. Ensure at least one backend module is on the classpath.");
        }

        log.debug("Auto-selected backend: {} (priority {})", best.getBackendType(), best.getPriority());
        return best.getBackendType();
    }

    /**
     * Returns all available backends on this platform.
     *
     * @return set of available backend types
     */
    public static Set<RenderBackend> getAvailableBackends() {
        ensureProvidersLoaded();

        Set<RenderBackend> available = EnumSet.noneOf(RenderBackend.class);
        for (RenderBackendProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                available.add(provider.getBackendType());
            }
        }
        return available;
    }

    /**
     * Returns all registered backends (whether available or not).
     *
     * @return set of registered backend types
     */
    public static Set<RenderBackend> getRegisteredBackends() {
        ensureProvidersLoaded();
        return EnumSet.copyOf(providers.keySet());
    }

    /**
     * Checks if a specific backend is available on this platform.
     *
     * @param backend the backend to check
     * @return true if available
     */
    public static boolean isBackendAvailable(RenderBackend backend) {
        if (backend == RenderBackend.AUTO) {
            return !getAvailableBackends().isEmpty();
        }

        RenderBackendProvider provider = getProvider(backend);
        return provider != null && provider.isAvailable();
    }

    /**
     * Returns the provider for a specific backend.
     *
     * @param backend the backend type
     * @return the provider, or null if not registered
     */
    private static RenderBackendProvider getProvider(RenderBackend backend) {
        ensureProvidersLoaded();
        return providers.get(backend);
    }

    /**
     * Ensures providers are loaded from ServiceLoader.
     */
    private static void ensureProvidersLoaded() {
        if (providers == null) {
            synchronized (LOCK) {
                if (providers == null) {
                    loadProviders();
                }
            }
        }
    }

    /**
     * Loads all providers using ServiceLoader.
     */
    private static void loadProviders() {
        Map<RenderBackend, RenderBackendProvider> map = new EnumMap<>(RenderBackend.class);

        ServiceLoader<RenderBackendProvider> loader = ServiceLoader.load(RenderBackendProvider.class);
        for (RenderBackendProvider provider : loader) {
            RenderBackend type = provider.getBackendType();
            if (type == RenderBackend.AUTO) {
                log.warn("Ignoring provider {} - cannot register for AUTO backend", provider.getClass().getName());
                continue;
            }

            RenderBackendProvider existing = map.get(type);
            if (existing != null) {
                // Keep higher priority provider
                if (provider.getPriority() > existing.getPriority()) {
                    log.debug("Replacing provider {} with {} for backend {} (higher priority)",
                            existing.getClass().getName(), provider.getClass().getName(), type);
                    map.put(type, provider);
                } else {
                    log.debug("Ignoring provider {} for backend {} (lower priority than {})",
                            provider.getClass().getName(), type, existing.getClass().getName());
                }
            } else {
                map.put(type, provider);
                log.debug("Registered provider {} for backend {} (priority {})",
                        provider.getClass().getName(), type, provider.getPriority());
            }
        }

        providers = map;
        log.info("Loaded {} backend providers: {}", map.size(), map.keySet());
    }

    /**
     * Clears the provider cache, forcing providers to be reloaded on next use.
     *
     * <p>This is primarily useful for testing.
     */
    public static void clearProviderCache() {
        synchronized (LOCK) {
            providers = null;
        }
    }
}
