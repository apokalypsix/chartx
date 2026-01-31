package com.apokalypsix.chartx.core.render.swing;

import com.apokalypsix.chartx.core.render.service.RenderPipeline;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.render.api.RenderBackendFactory;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Offscreen rendering strategy for non-OpenGL backends (Vulkan, Metal, DX12).
 *
 * <p>This strategy creates a RenderDevice for the specified backend and renders
 * to an offscreen buffer. The rendered pixels are then transferred to a BufferedImage
 * for display in Swing via OffscreenRenderPanel.
 *
 * <p>This enables chart rendering on backends other than OpenGL while maintaining
 * full Swing compatibility.
 */
public class OffscreenChartRenderingStrategy implements ChartRenderingStrategy {

    private static final Logger log = LoggerFactory.getLogger(OffscreenChartRenderingStrategy.class);

    private final RenderBackend backend;

    private RenderDevice device;
    private ResourceManager resourceManager;
    private OffscreenRenderPanel renderPanel;
    private RenderPipeline pipeline;
    private boolean initialized = false;

    /**
     * Creates an offscreen rendering strategy for the specified backend.
     *
     * @param backend the rendering backend (VULKAN, METAL, or DX12)
     */
    public OffscreenChartRenderingStrategy(RenderBackend backend) {
        this.backend = backend;

        // Validate that this is a non-GL backend
        if (backend == RenderBackend.OPENGL) {
            throw new IllegalArgumentException(
                    "Use GLChartRenderingStrategy for OpenGL backend");
        }
    }

    @Override
    public void initialize(RenderPipeline pipeline) {
        this.pipeline = pipeline;

        try {
            // Create and initialize the render device
            device = RenderBackendFactory.createDevice(backend, null);
            device.initialize();

            if (!device.isInitialized()) {
                throw new RuntimeException("Failed to initialize " + backend + " device");
            }

            // Create resource manager
            resourceManager = RenderBackendFactory.createResourceManager(device);
            resourceManager.initialize(device);

            // Initialize pipeline for this device
            pipeline.initializeForDevice(device, resourceManager);

            // Create the render panel
            renderPanel = new OffscreenRenderPanel(device);
            renderPanel.setRenderCallback(this::renderFrame);

            initialized = true;
            log.info("OffscreenChartRenderingStrategy initialized with {} backend", backend);

        } catch (Exception e) {
            log.error("Failed to initialize {} rendering strategy", backend, e);
            throw new RuntimeException("Failed to initialize " + backend + " rendering", e);
        }
    }

    /**
     * Callback invoked by OffscreenRenderPanel to render a frame.
     */
    private void renderFrame(RenderDevice device, int width, int height) {
        if (pipeline == null || !initialized) {
            return;
        }

        // Use the pipeline's non-GL rendering method
        pipeline.renderWithDevice(device, resourceManager, width, height);
    }

    @Override
    public JComponent getDisplayComponent() {
        return renderPanel;
    }

    @Override
    public void requestRepaint() {
        if (renderPanel != null) {
            renderPanel.requestRender();
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void dispose() {
        initialized = false;

        if (resourceManager != null) {
            resourceManager.dispose();
            resourceManager = null;
        }

        if (device != null) {
            device.dispose();
            device = null;
        }

        renderPanel = null;
        log.info("OffscreenChartRenderingStrategy disposed");
    }

    @Override
    public String getBackendName() {
        return backend.name();
    }

    /**
     * Returns the render device.
     */
    public RenderDevice getDevice() {
        return device;
    }

    /**
     * Returns the resource manager.
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Returns the backend type.
     */
    public RenderBackend getBackend() {
        return backend;
    }

    /**
     * Checks if the specified backend is available on this system.
     *
     * @param backend the backend to check
     * @return true if the backend is available
     */
    public static boolean isBackendAvailable(RenderBackend backend) {
        return RenderBackendFactory.isBackendAvailable(backend);
    }
}
