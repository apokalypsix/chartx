package com.apokalypsix.chartx.core.render.backend.dx12;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Swing panel that renders using DirectX 12.
 *
 * <p>Renders to an offscreen DirectX 12 texture and blits it to Swing
 * via BufferedImage for display.
 */
public class DX12SwingPanel extends JPanel {

    /**
     * Callback interface for custom rendering.
     */
    @FunctionalInterface
    public interface DX12RenderCallback {
        void render(DX12RenderDevice device, DX12ResourceManager resources, int width, int height);
    }

    private final DX12RenderDevice device;
    private final DX12ResourceManager resources;

    private BufferedImage backBuffer;
    private int[] pixelBuffer;
    private DX12RenderCallback renderCallback;

    private int lastWidth;
    private int lastHeight;

    /**
     * Creates a new DX12 Swing panel.
     *
     * @param device render device
     * @param resources resource manager
     */
    public DX12SwingPanel(DX12RenderDevice device, DX12ResourceManager resources) {
        this.device = device;
        this.resources = resources;

        setOpaque(true);
        setDoubleBuffered(true);
    }

    /**
     * Sets the render callback.
     *
     * @param callback callback to invoke during painting
     */
    public void setRenderCallback(DX12RenderCallback callback) {
        this.renderCallback = callback;
    }

    /**
     * Requests a repaint of the panel.
     */
    public void requestRender() {
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

        // Resize buffers if needed
        if (width != lastWidth || height != lastHeight) {
            resizeBuffers(width, height);
            lastWidth = width;
            lastHeight = height;
        }

        // Set viewport
        device.setViewport(0, 0, width, height);

        // Begin frame
        device.beginFrame();

        // Clear
        device.clearScreen(0.1f, 0.1f, 0.1f, 1.0f);

        // Custom rendering
        if (renderCallback != null) {
            renderCallback.render(device, resources, width, height);
        }

        // End frame
        device.endFrame();

        // Read pixels from GPU
        device.readFramePixels(pixelBuffer);

        // Blit to Graphics
        if (backBuffer != null) {
            g.drawImage(backBuffer, 0, 0, null);
        }
    }

    private void resizeBuffers(int width, int height) {
        backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        pixelBuffer = ((DataBufferInt) backBuffer.getRaster().getDataBuffer()).getData();
    }

    /**
     * Gets the render device.
     */
    public DX12RenderDevice getDevice() {
        return device;
    }

    /**
     * Gets the resource manager.
     */
    public DX12ResourceManager getResources() {
        return resources;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }
}
