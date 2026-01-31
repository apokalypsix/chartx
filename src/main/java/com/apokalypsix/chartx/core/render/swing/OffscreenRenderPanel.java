package com.apokalypsix.chartx.core.render.swing;

import com.apokalypsix.chartx.core.render.api.RenderDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Universal Swing panel for offscreen rendering with any RenderDevice.
 *
 * <p>This panel bridges offscreen GPU rendering with Swing's paint system by:
 * <ol>
 *   <li>Rendering to an offscreen buffer via RenderDevice</li>
 *   <li>Reading the pixels back to CPU memory</li>
 *   <li>Drawing the pixels to a BufferedImage</li>
 *   <li>Blitting the image to the Swing component</li>
 * </ol>
 *
 * <p>While this approach has overhead from the GPU-to-CPU transfer,
 * it provides full compatibility with Java Swing and works with any
 * backend that implements the RenderDevice interface.
 *
 * <p>This class consolidates the common pattern from VkSwingPanel,
 * MetalSwingPanel, and DX12SwingPanel into a single reusable component.
 */
public class OffscreenRenderPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(OffscreenRenderPanel.class);

    private final RenderDevice device;
    private BufferedImage backBuffer;
    private int[] pixelBuffer;

    private final AtomicBoolean renderRequested = new AtomicBoolean(false);
    private RenderCallback renderCallback;

    private int lastWidth = -1;
    private int lastHeight = -1;

    /**
     * Callback interface for custom rendering.
     */
    @FunctionalInterface
    public interface RenderCallback {
        /**
         * Called to render the frame content.
         *
         * @param device the render device
         * @param width frame width
         * @param height frame height
         */
        void render(RenderDevice device, int width, int height);
    }

    /**
     * Creates a new OffscreenRenderPanel with the specified device.
     *
     * @param device the render device to use (must be initialized)
     */
    public OffscreenRenderPanel(RenderDevice device) {
        this.device = device;
        setOpaque(true);
        setDoubleBuffered(false);  // We manage our own double buffering
        setBackground(Color.BLACK);
    }

    /**
     * Sets the render callback for custom rendering.
     *
     * @param callback the callback to invoke during rendering
     */
    public void setRenderCallback(RenderCallback callback) {
        this.renderCallback = callback;
    }

    /**
     * Requests a repaint of the rendered content.
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
            drawNotInitializedMessage(g, width, height);
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
            log.error("Error during render", e);
            drawErrorMessage(g, width, height, e.getMessage());
        }

        renderRequested.set(false);
    }

    private void drawNotInitializedMessage(Graphics g, int width, int height) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString(device.getBackendType() + " not initialized", 10, 20);
    }

    private void drawErrorMessage(Graphics g, int width, int height, String message) {
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("Render error: " + message, 10, 20);
    }

    private void resizeBuffers(int width, int height) {
        backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        pixelBuffer = ((DataBufferInt) backBuffer.getRaster().getDataBuffer()).getData();

        // Update device viewport
        device.setViewport(0, 0, width, height);

        log.debug("Resized buffers to {}x{} for {}", width, height, device.getBackendType());
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
    public RenderDevice getDevice() {
        return device;
    }

    /**
     * Returns true if the device is available and initialized.
     */
    public boolean isDeviceReady() {
        return device != null && device.isInitialized();
    }

    /**
     * Returns the current width of the panel.
     */
    public int getPanelWidth() {
        return lastWidth > 0 ? lastWidth : getWidth();
    }

    /**
     * Returns the current height of the panel.
     */
    public int getPanelHeight() {
        return lastHeight > 0 ? lastHeight : getHeight();
    }
}
