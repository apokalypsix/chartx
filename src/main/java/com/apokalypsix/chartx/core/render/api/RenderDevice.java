package com.apokalypsix.chartx.core.render.api;

/**
 * Backend-agnostic rendering device interface.
 *
 * <p>RenderDevice abstracts the underlying graphics API (OpenGL, Vulkan, etc.)
 * and provides a unified interface for GPU state management, resource creation,
 * and frame rendering.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Managing the graphics context lifecycle</li>
 *   <li>Setting render state (blending, viewport, scissor, etc.)</li>
 *   <li>Creating backend-specific resources (shaders, buffers, textures)</li>
 *   <li>Frame synchronization</li>
 * </ul>
 */
public interface RenderDevice {

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initializes the render device.
     *
     * <p>Must be called before any other methods. This sets up the graphics
     * context and prepares the device for rendering.
     */
    void initialize();

    /**
     * Releases all resources and shuts down the device.
     */
    void dispose();

    /**
     * Returns true if the device has been initialized.
     */
    boolean isInitialized();

    /**
     * Returns the active rendering backend type.
     */
    RenderBackend getBackendType();

    // -------------------------------------------------------------------------
    // Frame Management
    // -------------------------------------------------------------------------

    /**
     * Begins a new frame.
     *
     * <p>Call this at the start of each render cycle. For Vulkan, this acquires
     * the next swapchain image. For OpenGL, this is typically a no-op.
     */
    void beginFrame();

    /**
     * Ends the current frame.
     *
     * <p>Call this after all draw calls for the frame. For Vulkan, this submits
     * command buffers and presents. For OpenGL, this may trigger a buffer swap.
     */
    void endFrame();

    // -------------------------------------------------------------------------
    // Viewport and Scissor
    // -------------------------------------------------------------------------

    /**
     * Sets the viewport rectangle.
     *
     * @param x left edge in pixels
     * @param y bottom edge in pixels (OpenGL convention)
     * @param width viewport width in pixels
     * @param height viewport height in pixels
     */
    void setViewport(int x, int y, int width, int height);

    /**
     * Enables or disables scissor testing.
     *
     * @param enabled true to enable scissor test
     */
    void setScissorEnabled(boolean enabled);

    /**
     * Sets the scissor rectangle.
     *
     * @param x left edge in pixels
     * @param y bottom edge in pixels (OpenGL convention)
     * @param width scissor width in pixels
     * @param height scissor height in pixels
     */
    void setScissor(int x, int y, int width, int height);

    // -------------------------------------------------------------------------
    // Render State
    // -------------------------------------------------------------------------

    /**
     * Sets the blend mode.
     *
     * @param mode the blend mode
     */
    void setBlendMode(BlendMode mode);

    /**
     * Sets the line width for line primitives.
     *
     * @param width line width in pixels
     */
    void setLineWidth(float width);

    /**
     * Enables or disables line smoothing (anti-aliasing).
     *
     * @param enabled true to enable line smoothing
     */
    void setLineSmoothing(boolean enabled);

    /**
     * Enables or disables depth testing.
     *
     * @param enabled true to enable depth test
     */
    void setDepthTestEnabled(boolean enabled);

    // -------------------------------------------------------------------------
    // Clear Operations
    // -------------------------------------------------------------------------

    /**
     * Clears the color buffer with the specified color.
     *
     * @param r red component (0-1)
     * @param g green component (0-1)
     * @param b blue component (0-1)
     * @param a alpha component (0-1)
     */
    void clearScreen(float r, float g, float b, float a);

    /**
     * Clears the depth buffer.
     */
    void clearDepth();

    // -------------------------------------------------------------------------
    // Resource Creation
    // -------------------------------------------------------------------------

    /**
     * Creates a shader from the given source.
     *
     * @param source the shader source (GLSL and/or SPIR-V)
     * @return the compiled shader
     */
    Shader createShader(ShaderSource source);

    /**
     * Creates a vertex buffer with the specified layout.
     *
     * @param descriptor the buffer descriptor
     * @return the created buffer
     */
    Buffer createBuffer(BufferDescriptor descriptor);

    /**
     * Creates a texture with the specified format.
     *
     * @param descriptor the texture descriptor
     * @return the created texture
     */
    Texture createTexture(TextureDescriptor descriptor);

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns the maximum supported line width.
     */
    float getMaxLineWidth();

    /**
     * Returns the maximum texture size supported.
     */
    int getMaxTextureSize();

    /**
     * Returns a human-readable description of the graphics hardware.
     */
    String getRendererInfo();

    // -------------------------------------------------------------------------
    // Pixel Readback (for offscreen rendering)
    // -------------------------------------------------------------------------

    /**
     * Reads the current frame buffer pixels into the provided array.
     *
     * <p>This method is used for offscreen rendering where the rendered
     * content needs to be transferred to a BufferedImage for display in Swing.
     *
     * <p>The pixel array should be sized to match the current viewport
     * (width * height). Pixels are returned in ARGB format suitable for
     * BufferedImage.TYPE_INT_ARGB.
     *
     * @param pixels the array to receive pixel data
     */
    void readFramePixels(int[] pixels);

    /**
     * Returns true if this device supports offscreen rendering with pixel readback.
     *
     * <p>Some devices (like OpenGL via GLJPanel) don't need pixel readback
     * because they render directly to the Swing component.
     *
     * @return true if readFramePixels is supported
     */
    default boolean supportsPixelReadback() {
        return true;
    }
}
