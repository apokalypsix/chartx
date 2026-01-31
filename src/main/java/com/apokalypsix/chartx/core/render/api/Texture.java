package com.apokalypsix.chartx.core.render.api;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Backend-agnostic texture interface.
 *
 * <p>A texture encapsulates a GPU image that can be sampled in shaders.
 */
public interface Texture {

    /**
     * Uploads image data from a BufferedImage.
     *
     * @param image the image to upload
     */
    void upload(BufferedImage image);

    /**
     * Uploads raw pixel data.
     *
     * @param data the pixel data
     * @param width image width
     * @param height image height
     * @param format the pixel format
     */
    void upload(ByteBuffer data, int width, int height, TextureDescriptor.TextureFormat format);

    /**
     * Binds this texture to a texture unit.
     *
     * @param unit the texture unit (0-15 typically)
     */
    void bind(int unit);

    /**
     * Unbinds this texture.
     */
    void unbind();

    /**
     * Returns the texture width.
     */
    int getWidth();

    /**
     * Returns the texture height.
     */
    int getHeight();

    /**
     * Releases GPU resources associated with this texture.
     */
    void dispose();

    /**
     * Returns true if this texture has been initialized.
     */
    boolean isInitialized();
}
