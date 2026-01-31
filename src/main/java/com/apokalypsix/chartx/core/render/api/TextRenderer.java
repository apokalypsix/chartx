package com.apokalypsix.chartx.core.render.api;

import java.awt.Color;

/**
 * Backend-agnostic text renderer interface.
 *
 * <p>TextRenderer provides batched text rendering using texture atlas.
 * All text for a frame should be drawn between beginBatch() and endBatch()
 * for optimal performance.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * textRenderer.beginBatch(width, height);
 * textRenderer.drawText("Label 1", x1, y1, Color.WHITE);
 * textRenderer.drawTextCentered("Label 2", x2, y2, Color.GREEN);
 * textRenderer.endBatch();
 * }</pre>
 *
 * <p>Implementations exist for each rendering backend:
 * <ul>
 *   <li>OpenGL: GLTextRenderer</li>
 *   <li>Vulkan: VkTextRendererImpl</li>
 *   <li>Metal: MetalTextRenderer</li>
 * </ul>
 */
public interface TextRenderer {

    /**
     * Sets the font size for subsequent text.
     *
     * @param size the font size in pixels
     */
    void setFontSize(float size);

    /**
     * Gets the current font size.
     *
     * @return the font size in pixels
     */
    float getFontSize();

    /**
     * Sets the font family for subsequent text.
     *
     * @param family the font family name
     */
    void setFontFamily(String family);

    /**
     * Sets the display scale factor for HiDPI displays.
     *
     * <p>On HiDPI displays (e.g., Retina), the scale factor is typically 2.0.
     * Font sizes will be multiplied by this factor when rendering.
     *
     * @param scale the scale factor (1.0 for standard displays, 2.0 for Retina)
     */
    default void setScaleFactor(float scale) {
        // Default implementation does nothing - backends can override
    }

    /**
     * Gets the current display scale factor.
     *
     * @return the scale factor
     */
    default float getScaleFactor() {
        return 1.0f;
    }

    /**
     * Begins a text rendering batch.
     *
     * <p>Must be called before any draw methods. All text drawn between
     * beginBatch() and endBatch() is batched into a single draw call.
     *
     * @param width screen width in pixels
     * @param height screen height in pixels
     * @return true if batch was started successfully
     */
    boolean beginBatch(int width, int height);

    /**
     * Adds left-aligned text to the batch.
     *
     * @param text the text to render
     * @param x screen x position (left edge)
     * @param y screen y position (baseline)
     * @param color text color
     */
    void drawText(String text, float x, float y, Color color);

    /**
     * Adds centered text to the batch.
     *
     * @param text the text to render
     * @param centerX center x position
     * @param y screen y position (baseline)
     * @param color text color
     */
    void drawTextCentered(String text, float centerX, float y, Color color);

    /**
     * Adds right-aligned text to the batch.
     *
     * @param text the text to render
     * @param rightX right edge x position
     * @param y screen y position (baseline)
     * @param color text color
     */
    void drawTextRight(String text, float rightX, float y, Color color);

    /**
     * Ends the batch and renders all queued text.
     *
     * <p>Must be called after all draw methods for the current batch.
     */
    void endBatch();

    /**
     * Returns the width of the given text in pixels.
     *
     * @param text the text to measure
     * @return text width in pixels
     */
    float getTextWidth(String text);

    /**
     * Returns the height of a line of text.
     *
     * @return line height in pixels
     */
    float getTextHeight();

    /**
     * Returns true if currently in batch mode.
     *
     * @return true if between beginBatch() and endBatch()
     */
    boolean isInBatch();

    /**
     * Disposes GPU resources held by this renderer.
     */
    void dispose();
}
