package com.apokalypsix.chartx.chart;

import com.apokalypsix.chartx.ChartX;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.core.render.api.RenderBackend;

/**
 * Factory for creating charts with a specific rendering backend.
 *
 * <p>ChartFactory provides an alternative to setting a global default backend.
 * Use this when you need to create charts with different backends in the same
 * application, or when you want explicit control over the backend for a
 * specific set of charts.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a factory for Vulkan charts
 * ChartFactory vulkanFactory = ChartFactory.withBackend(RenderBackend.VULKAN);
 *
 * // Create charts using the factory
 * Chart chart1 = vulkanFactory.createChart("chart1");
 * FinanceChart chart2 = vulkanFactory.createFinanceChart("chart2");
 * ChartLayout layout = vulkanFactory.createLayout();
 * }</pre>
 *
 * @see ChartX#setDefaultBackend(RenderBackend) for setting a global default
 */
public class ChartFactory {

    private final RenderBackend backend;

    private ChartFactory(RenderBackend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("Backend cannot be null");
        }
        this.backend = backend;
    }

    /**
     * Creates a factory that produces charts with the specified backend.
     *
     * @param backend the rendering backend to use for all charts created by this factory
     * @return a new ChartFactory instance
     * @throws IllegalArgumentException if backend is null
     */
    public static ChartFactory withBackend(RenderBackend backend) {
        return new ChartFactory(backend);
    }

    /**
     * Returns the backend used by this factory.
     *
     * @return the rendering backend
     */
    public RenderBackend getBackend() {
        return backend;
    }

    /**
     * Creates a Chart with the factory's backend.
     *
     * @param id unique identifier for the chart
     * @return a new Chart instance
     */
    public Chart createChart(String id) {
        return new Chart(id, backend);
    }

    /**
     * Creates a FinanceChart with the factory's backend.
     *
     * @param id unique identifier for the chart
     * @return a new FinanceChart instance
     */
    public FinanceChart createFinanceChart(String id) {
        return new FinanceChart(id, backend);
    }

    /**
     * Creates a ChartLayout with VSTACK mode and the factory's backend.
     *
     * @return a new ChartLayout instance
     */
    public ChartLayout createLayout() {
        return new ChartLayout(ChartLayout.LayoutMode.VSTACK, backend);
    }

    /**
     * Creates a ChartLayout with the specified mode and the factory's backend.
     *
     * @param mode the layout mode
     * @return a new ChartLayout instance
     */
    public ChartLayout createLayout(ChartLayout.LayoutMode mode) {
        return new ChartLayout(mode, backend);
    }
}
