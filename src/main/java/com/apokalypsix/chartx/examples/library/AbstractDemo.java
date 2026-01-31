package com.apokalypsix.chartx.examples.library;

import com.apokalypsix.chartx.ChartX;
import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.ChartLayout;
import com.apokalypsix.chartx.chart.VisualizationPanel;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.core.render.api.RenderBackend;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for backend-agnostic demo applications.
 *
 * <p>AbstractDemo provides a template for creating demos that can run on any
 * supported rendering backend (OpenGL, Vulkan, Metal, DX12, Skija). Subclasses
 * implement the abstract methods to define the demo content, while the base
 * class handles frame creation, backend selection, and lifecycle management.
 *
 * <p>Example subclass:
 * <pre>{@code
 * public class ChartDemo extends AbstractDemo {
 *
 *     @Override
 *     protected String getDemoTitle() {
 *         return "Chart Demo";
 *     }
 *
 *     @Override
 *     protected Component createDemoContent() {
 *         FinanceChart chart = createFinanceChart();
 *         chart.addCandlestickSeries(DemoDataGenerator.generateOHLCData(), new OhlcSeriesOptions());
 *         return chart;
 *     }
 *
 *     @Override
 *     protected JPanel createControlPanel() {
 *         JPanel panel = DemoUIHelper.createControlPanel();
 *         // Add controls...
 *         return panel;
 *     }
 * }
 * }</pre>
 *
 * <p>Demos use the default rendering backend (OpenGL) unless explicitly set.
 */
public abstract class AbstractDemo {

    /** The rendering backend to use */
    protected RenderBackend backend = ChartX.getDefaultBackend();

    /** The main frame */
    protected JFrame frame;

    /** Default window width */
    protected int windowWidth = DemoConfig.DEFAULT_WIDTH;

    /** Default window height */
    protected int windowHeight = DemoConfig.DEFAULT_HEIGHT;

    /**
     * Returns the demo title (displayed in window title bar).
     *
     * @return demo title
     */
    protected abstract String getDemoTitle();

    /**
     * Creates the main demo content component.
     *
     * <p>This is where the chart and related components should be created.
     * Use {@link #createChart()} and {@link #createChartLayout()} to create
     * charts that use the configured backend.
     *
     * @return the main content component
     */
    protected abstract Component createDemoContent();

    /**
     * Creates the control panel (typically placed at the bottom).
     *
     * <p>Use {@link DemoUIHelper} to create styled controls.
     * Return null to skip the control panel.
     *
     * @return control panel or null
     */
    protected abstract JPanel createControlPanel();

    /**
     * Called after the demo is fully set up and visible.
     *
     * <p>Override to perform post-initialization tasks.
     */
    protected void onDemoReady() {
        // Default: do nothing
    }

    /**
     * Called when the demo window is closing.
     *
     * <p>Override to perform cleanup tasks.
     */
    protected void onDemoClosing() {
        // Default: do nothing
    }

    /**
     * Sets the rendering backend to use.
     *
     * <p>Must be called before {@link #run()}.
     *
     * @param backend the rendering backend
     */
    public void setBackend(RenderBackend backend) {
        this.backend = backend;
    }

    /**
     * Returns the current rendering backend.
     *
     * @return rendering backend
     */
    public RenderBackend getBackend() {
        return backend;
    }

    /**
     * Sets the window dimensions.
     *
     * <p>Must be called before {@link #run()}.
     *
     * @param width window width
     * @param height window height
     */
    public void setWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    /**
     * Runs the demo.
     *
     * <p>Creates the UI on the EDT and displays the demo window.
     */
    public void run() {
        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    /**
     * Creates and shows the demo GUI.
     *
     * <p>Called on the EDT by {@link #run()}.
     */
    protected void createAndShowGUI() {
        // Build the title with backend info
        String title = getDemoTitle() + " [" + backend.name() + "]";
        frame = DemoUIHelper.createDemoFrame(title, windowWidth, windowHeight);

        // Add window closing handler
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onDemoClosing();
            }
        });

        // Create and add main content
        Component content = createDemoContent();
        if (content != null) {
            frame.add(content, BorderLayout.CENTER);
        }

        // Create and add control panel
        JPanel controls = createControlPanel();
        if (controls != null) {
            frame.add(controls, BorderLayout.SOUTH);
        }

        // Show the frame
        DemoUIHelper.showFrame(frame);

        // Notify subclass
        onDemoReady();
    }

    /**
     * Creates a Chart using the configured backend.
     *
     * @return new Chart instance
     */
    protected Chart createChart() {
        return new Chart("main", backend);
    }

    /**
     * Creates a Chart with the specified ID using the configured backend.
     *
     * @param id the chart ID
     * @return new Chart instance
     */
    protected Chart createChart(String id) {
        return new Chart(id, backend);
    }

    /**
     * Creates a FinanceChart using the configured backend.
     * Use this for charts that display OHLC data with financial overlays.
     *
     * @return new FinanceChart instance
     */
    protected FinanceChart createFinanceChart() {
        return new FinanceChart("main", backend);
    }

    /**
     * Creates a FinanceChart with the specified ID using the configured backend.
     * Use this for charts that display OHLC data with financial overlays.
     *
     * @param id the chart ID
     * @return new FinanceChart instance
     */
    protected FinanceChart createFinanceChart(String id) {
        return new FinanceChart(id, backend);
    }

    /**
     * Creates a ChartLayout with the specified mode using the configured backend.
     *
     * @param mode the layout mode
     * @return new ChartLayout instance
     */
    protected ChartLayout createChartLayout(ChartLayout.LayoutMode mode) {
        return new ChartLayout(mode, backend);
    }

    /**
     * Creates a ChartLayout with VSTACK mode using the configured backend.
     *
     * @return new ChartLayout instance
     */
    protected ChartLayout createChartLayout() {
        return new ChartLayout(ChartLayout.LayoutMode.VSTACK, backend);
    }

    /**
     * Creates a VisualizationPanel using the configured backend.
     *
     * <p>Use this for standalone visualizations (treemap, sunburst, pie, radar, etc.)
     * that don't need axes or coordinate systems.
     *
     * @return new VisualizationPanel instance
     */
    protected VisualizationPanel createVisualizationPanel() {
        return new VisualizationPanel(backend);
    }

    /**
     * Returns the main frame.
     *
     * @return JFrame or null if not yet created
     */
    protected JFrame getFrame() {
        return frame;
    }

    /**
     * Launches a demo with command line argument support.
     *
     * <p>Usage: java DemoClass [backend]
     * <p>Supported backends: opengl (default), vulkan, metal, skija, dx12, auto
     *
     * @param demo the demo instance to launch
     * @param args command line arguments
     */
    public static void launch(AbstractDemo demo, String[] args) {
        RenderBackend backend = RenderBackend.OPENGL;
        if (args.length > 0) {
            try {
                backend = RenderBackend.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown backend: " + args[0]);
                System.err.println("Available: opengl, vulkan, metal, skija, dx12, auto");
                System.err.println("Using OpenGL.");
            }
        }
        demo.setBackend(backend);
        demo.run();
    }
}
