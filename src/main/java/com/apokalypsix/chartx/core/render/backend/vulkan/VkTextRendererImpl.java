package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.TextRenderer;

import java.awt.Color;

/**
 * Vulkan implementation of the TextRenderer interface.
 *
 * <p>Wraps the existing {@link VkTextRenderer} to provide a backend-agnostic
 * API for text rendering.
 */
public class VkTextRendererImpl implements TextRenderer {

    private final VkTextRenderer delegate;
    private final VkRenderDevice device;
    private final VkResourceManager resources;

    /**
     * Creates a VkTextRendererImpl.
     *
     * @param device the Vulkan render device
     * @param resources the Vulkan resource manager
     */
    public VkTextRendererImpl(VkRenderDevice device, VkResourceManager resources) {
        this.device = device;
        this.resources = resources;
        this.delegate = new VkTextRenderer();
    }

    /**
     * Creates a VkTextRendererImpl with initial font size.
     *
     * @param device the Vulkan render device
     * @param resources the Vulkan resource manager
     * @param fontSize initial font size
     */
    public VkTextRendererImpl(VkRenderDevice device, VkResourceManager resources, float fontSize) {
        this.device = device;
        this.resources = resources;
        this.delegate = new VkTextRenderer(fontSize);
    }

    @Override
    public void setFontSize(float size) {
        delegate.setFontSize(size);
    }

    @Override
    public float getFontSize() {
        return delegate.getFontSize();
    }

    @Override
    public void setFontFamily(String family) {
        delegate.setFontFamily(family);
    }

    @Override
    public boolean beginBatch(int width, int height) {
        return delegate.beginBatch(device, resources, width, height);
    }

    @Override
    public void drawText(String text, float x, float y, Color color) {
        delegate.drawText(text, x, y, color);
    }

    @Override
    public void drawTextCentered(String text, float centerX, float y, Color color) {
        delegate.drawTextCentered(text, centerX, y, color);
    }

    @Override
    public void drawTextRight(String text, float rightX, float y, Color color) {
        delegate.drawTextRight(text, rightX, y, color);
    }

    @Override
    public void endBatch() {
        delegate.endBatch();
    }

    @Override
    public float getTextWidth(String text) {
        return delegate.getTextWidth(text);
    }

    @Override
    public float getTextHeight() {
        return delegate.getTextHeight();
    }

    @Override
    public boolean isInBatch() {
        return delegate.isInBatch();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
