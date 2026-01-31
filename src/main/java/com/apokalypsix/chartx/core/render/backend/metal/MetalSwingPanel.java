package com.apokalypsix.chartx.core.render.backend.metal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Swing panel that renders using Metal and displays the result.
 *
 * <p>This panel bridges Metal's offscreen rendering with Swing's
 * paint system by:
 * <ol>
 *   <li>Rendering to a Metal texture</li>
 *   <li>Reading the pixels back to CPU memory</li>
 *   <li>Drawing the pixels to a BufferedImage</li>
 *   <li>Blitting the image to the Swing component</li>
 * </ol>
 *
 * <p>While this approach has overhead from the GPU-to-CPU transfer,
 * it provides full compatibility with Java Swing without requiring
 * native window integration.
 */
public class MetalSwingPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(MetalSwingPanel.class);

    private final MetalRenderDevice device;
    private BufferedImage backBuffer;
    private int[] pixelBuffer;

    private final AtomicBoolean renderRequested = new AtomicBoolean(false);
    private MetalRenderCallback renderCallback;

    private int lastWidth = -1;
    private int lastHeight = -1;

    /**
     * Callback interface for custom rendering.
     */
    @FunctionalInterface
    public interface MetalRenderCallback {
        /**
         * Called to render the frame content.
         * The render pass is already active when this is called.
         *
         * @param device the Metal render device
         * @param width frame width
         * @param height frame height
         */
        void render(MetalRenderDevice device, int width, int height);
    }

    public MetalSwingPanel(MetalRenderDevice device) {
        this.device = device;
        setOpaque(true);
        setDoubleBuffered(false);  // We manage our own double buffering
        setBackground(Color.BLACK);
    }

    /**
     * Sets the render callback for custom rendering.
     */
    public void setRenderCallback(MetalRenderCallback callback) {
        this.renderCallback = callback;
    }

    /**
     * Requests a repaint of the Metal content.
     */
    public void requestRender() {
        renderRequested.set(true);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        // Check if device is ready
        if (!device.isInitialized()) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.WHITE);
            g.drawString("Metal not initialized", 10, 20);
            return;
        }

        // Resize buffers if needed
        if (width != lastWidth || height != lastHeight) {
            resizeBuffers(width, height);
            lastWidth = width;
            lastHeight = height;
        }

        // Render the frame
        try {
            renderFrame(width, height);

            // Draw the back buffer
            if (backBuffer != null) {
                g.drawImage(backBuffer, 0, 0, null);
            }
        } catch (Exception e) {
            log.error("Error during Metal render", e);
            g.setColor(Color.RED);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.WHITE);
            g.drawString("Render error: " + e.getMessage(), 10, 20);
        }

        renderRequested.set(false);
    }

    private void resizeBuffers(int width, int height) {
        backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        pixelBuffer = ((DataBufferInt) backBuffer.getRaster().getDataBuffer()).getData();

        // Update device viewport
        device.setViewport(0, 0, width, height);

        log.debug("Resized Metal buffers to {}x{}", width, height);
    }

    private void renderFrame(int width, int height) {
        // Begin frame
        device.beginFrame();

        // Call render callback if set
        if (renderCallback != null) {
            renderCallback.render(device, width, height);
        }

        // End frame
        device.endFrame();

        // Read pixels back
        device.readFramePixels(pixelBuffer);
    }

    /**
     * Returns the render device.
     */
    public MetalRenderDevice getDevice() {
        return device;
    }

    /**
     * Returns true if Metal is available and initialized.
     */
    public boolean isMetalReady() {
        return device != null && device.isInitialized();
    }
}
