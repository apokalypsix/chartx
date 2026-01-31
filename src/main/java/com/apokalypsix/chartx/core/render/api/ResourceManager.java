package com.apokalypsix.chartx.core.render.api;

/**
 * Backend-agnostic resource manager interface.
 *
 * <p>ResourceManager handles the lifecycle and caching of rendering resources
 * such as shaders, buffers, and textures. It provides thread-safe access to
 * resources and handles deferred initialization/cleanup on the render thread.
 */
public interface ResourceManager {

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initializes the resource manager with the given render device.
     *
     * <p>Must be called on the render thread before using other methods.
     *
     * @param device the render device
     */
    void initialize(RenderDevice device);

    /**
     * Releases all managed resources.
     *
     * <p>Must be called on the render thread.
     */
    void dispose();

    /**
     * Returns true if the manager has been initialized.
     */
    boolean isInitialized();

    /**
     * Returns the render device this manager is associated with.
     */
    RenderDevice getDevice();

    // -------------------------------------------------------------------------
    // Shader Management
    // -------------------------------------------------------------------------

    /**
     * Returns a shader by name.
     *
     * @param name the shader name
     * @return the shader, or null if not found
     */
    Shader getShader(String name);

    /**
     * Registers a shader with the given name.
     *
     * @param name the shader name
     * @param shader the shader
     */
    void registerShader(String name, Shader shader);

    /**
     * Creates and registers a shader from source.
     *
     * @param name the shader name
     * @param source the shader source
     * @return the created shader
     */
    Shader createShader(String name, ShaderSource source);

    // -------------------------------------------------------------------------
    // Buffer Management
    // -------------------------------------------------------------------------

    /**
     * Returns an existing buffer or creates a new one.
     *
     * @param name the buffer name
     * @param descriptor the buffer descriptor (used if creating)
     * @return the buffer
     */
    Buffer getOrCreateBuffer(String name, BufferDescriptor descriptor);

    /**
     * Returns an existing buffer by name.
     *
     * @param name the buffer name
     * @return the buffer, or null if not found
     */
    Buffer getBuffer(String name);

    /**
     * Disposes a buffer by name.
     *
     * @param name the buffer name
     */
    void disposeBuffer(String name);

    // -------------------------------------------------------------------------
    // Texture Management
    // -------------------------------------------------------------------------

    /**
     * Creates a texture with the given descriptor.
     *
     * @param descriptor the texture descriptor
     * @return the created texture
     */
    Texture createTexture(TextureDescriptor descriptor);

    /**
     * Returns a texture by name.
     *
     * @param name the texture name
     * @return the texture, or null if not found
     */
    Texture getTexture(String name);

    /**
     * Registers a texture with the given name.
     *
     * @param name the texture name
     * @param texture the texture
     */
    void registerTexture(String name, Texture texture);

    /**
     * Disposes a texture by name.
     *
     * @param name the texture name
     */
    void disposeTexture(String name);

    // -------------------------------------------------------------------------
    // Text Rendering
    // -------------------------------------------------------------------------

    /**
     * Returns the text renderer for this backend.
     *
     * <p>The text renderer provides batched text rendering using texture atlas.
     * It is lazily initialized on first access.
     *
     * @return the text renderer
     */
    TextRenderer getTextRenderer();

    // -------------------------------------------------------------------------
    // Thread Safety
    // -------------------------------------------------------------------------

    /**
     * Queues an operation to be executed on the render thread.
     *
     * @param operation the operation to execute
     */
    void runOnRenderThread(Runnable operation);

    /**
     * Processes all pending operations.
     *
     * <p>Must be called on the render thread, typically at the start of each frame.
     */
    void processPendingOperations();

    // -------------------------------------------------------------------------
    // Built-in Shaders
    // -------------------------------------------------------------------------

    /**
     * Name of the default shader (position + color).
     */
    String SHADER_DEFAULT = "default";

    /**
     * Name of the simple shader (position only, uniform color).
     */
    String SHADER_SIMPLE = "simple";

    /**
     * Name of the text shader (position + texcoord + color).
     */
    String SHADER_TEXT = "text";

}
