package com.apokalypsix.chartx;

import com.apokalypsix.chartx.chart.ChartFactory;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.render.api.RenderBackendFactory;

/**
 * Main entry point for ChartX library configuration.
 *
 * <p>ChartX provides static methods for configuring library-wide defaults,
 * including the default rendering backend. This allows applications to
 * set their preferred backend once at startup.
 *
 * <p>Example usage:
 * <pre>{@code
 * // At application startup
 * ChartX.setDefaultBackend(RenderBackend.METAL);
 *
 * // All charts created without explicit backend will use METAL
 * Chart chart = new Chart("main");  // Uses METAL
 * }</pre>
 *
 * @see ChartFactory for creating charts with a specific backend
 * @see RenderBackend for available backends
 */
public final class ChartX {

    private static volatile RenderBackend defaultBackend = null;

    private ChartX() {
        // Utility class - no instantiation
    }

    /**
     * Sets the default backend for all charts created without an explicit backend.
     *
     * <p>This setting affects:
     * <ul>
     *   <li>{@link com.apokalypsix.chartx.chart.Chart} default constructors</li>
     *   <li>{@link com.apokalypsix.chartx.chart.ChartLayout} default constructors</li>
     *   <li>{@link com.apokalypsix.chartx.chart.finance.FinanceChart} default constructors</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * ChartX.setDefaultBackend(RenderBackend.VULKAN);
     * Chart chart = new Chart("main");  // Uses VULKAN
     * }</pre>
     *
     * @param backend the default backend to use (null to reset to auto-detection)
     */
    public static void setDefaultBackend(RenderBackend backend) {
        defaultBackend = backend;
    }

    /**
     * Returns the default backend for chart creation.
     *
     * <p>If no default has been set via {@link #setDefaultBackend(RenderBackend)},
     * this method uses {@link RenderBackendFactory#detectBestBackend()} to
     * automatically select the best available backend for the current platform.
     *
     * @return the default backend (never null)
     */
    public static RenderBackend getDefaultBackend() {
        RenderBackend backend = defaultBackend;
        if (backend != null) {
            return backend;
        }
        return RenderBackendFactory.detectBestBackend();
    }

    /**
     * Resets the default backend to auto-detection mode.
     *
     * <p>After calling this method, {@link #getDefaultBackend()} will use
     * automatic backend detection based on platform capabilities.
     */
    public static void resetDefaultBackend() {
        defaultBackend = null;
    }

    /**
     * Returns whether a custom default backend has been set.
     *
     * @return true if a custom default was set, false if using auto-detection
     */
    public static boolean hasCustomDefaultBackend() {
        return defaultBackend != null;
    }
}
