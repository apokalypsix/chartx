package com.apokalypsix.chartx.core.render.api;

import java.nio.FloatBuffer;

/**
 * Backend-agnostic vertex buffer interface.
 *
 * <p>A buffer encapsulates GPU memory for storing vertex data. It handles
 * uploading data from CPU memory and issuing draw commands.
 */
public interface Buffer {

    /**
     * Uploads float data to the buffer.
     *
     * @param data the data to upload
     * @param offset the offset in floats from the start of the buffer
     * @param count the number of floats to upload
     */
    void upload(float[] data, int offset, int count);

    /**
     * Uploads float data to the buffer from a NIO buffer.
     *
     * @param data the data to upload (position to limit)
     * @param offset the offset in floats from the start of the buffer
     */
    void upload(FloatBuffer data, int offset);

    /**
     * Binds this buffer for rendering.
     */
    void bind();

    /**
     * Unbinds this buffer.
     */
    void unbind();

    /**
     * Draws vertices from this buffer.
     *
     * @param mode the primitive drawing mode
     */
    void draw(DrawMode mode);

    /**
     * Draws a range of vertices from this buffer.
     *
     * @param mode the primitive drawing mode
     * @param first the index of the first vertex
     * @param count the number of vertices to draw
     */
    void draw(DrawMode mode, int first, int count);

    /**
     * Returns the number of vertices currently in the buffer.
     */
    int getVertexCount();

    /**
     * Sets the number of vertices in the buffer.
     * <p>Call this after uploading data to indicate how many vertices are valid.
     *
     * @param count the vertex count
     */
    void setVertexCount(int count);

    /**
     * Returns the capacity in floats.
     */
    int getCapacity();

    /**
     * Releases GPU resources associated with this buffer.
     */
    void dispose();

    /**
     * Returns true if this buffer has been initialized.
     */
    boolean isInitialized();
}
