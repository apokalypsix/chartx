package com.apokalypsix.chartx.core.render.api;

/**
 * Backend-agnostic shader program interface.
 *
 * <p>A shader encapsulates a GPU program consisting of vertex and fragment
 * stages (and optionally geometry). Implementations handle the backend-specific
 * details of shader compilation, linking, and uniform management.
 */
public interface Shader {

    /**
     * Binds this shader for use in subsequent draw calls.
     */
    void bind();

    /**
     * Unbinds this shader.
     */
    void unbind();

    /**
     * Releases GPU resources associated with this shader.
     */
    void dispose();

    /**
     * Returns true if this shader was successfully compiled and linked.
     */
    boolean isValid();

    // -------------------------------------------------------------------------
    // Uniform setters
    // -------------------------------------------------------------------------

    /**
     * Sets an integer uniform value.
     *
     * @param name uniform name
     * @param value the value
     */
    void setUniform(String name, int value);

    /**
     * Sets a float uniform value.
     *
     * @param name uniform name
     * @param value the value
     */
    void setUniform(String name, float value);

    /**
     * Sets a vec2 uniform value.
     *
     * @param name uniform name
     * @param x x component
     * @param y y component
     */
    void setUniform(String name, float x, float y);

    /**
     * Sets a vec3 uniform value.
     *
     * @param name uniform name
     * @param x x component
     * @param y y component
     * @param z z component
     */
    void setUniform(String name, float x, float y, float z);

    /**
     * Sets a vec4 uniform value.
     *
     * @param name uniform name
     * @param x x component
     * @param y y component
     * @param z z component
     * @param w w component
     */
    void setUniform(String name, float x, float y, float z, float w);

    /**
     * Sets a 4x4 matrix uniform value.
     *
     * @param name uniform name
     * @param matrix 16-element array in column-major order
     */
    void setUniformMatrix4(String name, float[] matrix);

    /**
     * Sets a 4x4 matrix uniform value.
     *
     * @param name uniform name
     * @param matrix 16-element array in column-major order
     * @param transpose whether to transpose the matrix
     */
    void setUniformMatrix4(String name, float[] matrix, boolean transpose);
}
