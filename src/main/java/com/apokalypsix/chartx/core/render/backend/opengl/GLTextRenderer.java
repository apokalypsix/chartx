package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.TextRenderer;
import com.jogamp.opengl.GL2ES2;

import java.awt.Color;

/**
 * OpenGL implementation of the TextRenderer interface.
 *
 * <p>Wraps the existing {@link com.apokalypsix.chartx.core.render.gl.TextRenderer}
 * to provide a backend-agnostic API for text rendering.
 */
public class GLTextRenderer implements TextRenderer {

    private final com.apokalypsix.chartx.core.render.gl.TextRenderer delegate;
    private final GLRenderDevice device;

    /**
     * Creates a GLTextRenderer.
     *
     * @param device the OpenGL render device
     */
    public GLTextRenderer(GLRenderDevice device) {
        this.device = device;
        this.delegate = new com.apokalypsix.chartx.core.render.gl.TextRenderer();
    }

    /**
     * Creates a GLTextRenderer with initial font size.
     *
     * @param device the OpenGL render device
     * @param fontSize initial font size
     */
    public GLTextRenderer(GLRenderDevice device, float fontSize) {
        this.device = device;
        this.delegate = new com.apokalypsix.chartx.core.render.gl.TextRenderer(fontSize);
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
    public void setScaleFactor(float scale) {
        delegate.setScaleFactor(scale);
    }

    @Override
    public float getScaleFactor() {
        return delegate.getScaleFactor();
    }

    @Override
    public boolean beginBatch(int width, int height) {
        GL2ES2 gl = device.getGL();
        if (gl == null) {
            return false;
        }
        return delegate.beginBatch(gl, width, height);
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
        GL2ES2 gl = device.getGL();
        if (gl != null) {
            delegate.endBatch(gl);
        }
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
        GL2ES2 gl = device.getGL();
        if (gl != null) {
            delegate.dispose(gl);
        }
    }
}
