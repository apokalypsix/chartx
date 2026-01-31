package com.apokalypsix.chartx.core.render.backend.metal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * JNI bridge to native Metal API.
 *
 * <p>All methods are static and operate on opaque handles (jlong).
 * The native library must be loaded before calling any methods.
 *
 * <p>Handle values represent retained Objective-C object references
 * on the native side. Each createXxx method returns a handle that
 * must be released via the corresponding destroyXxx method.
 */
public final class MetalNative {

    private static final Logger log = LoggerFactory.getLogger(MetalNative.class);
    private static final String LIBRARY_NAME = "chartx-metal";

    private static volatile boolean loaded = false;
    private static volatile boolean available = false;
    private static volatile String loadError = null;

    static {
        loadNativeLibrary();
    }

    // =========================================================================
    // Device Management
    // =========================================================================

    /**
     * Creates the default Metal device.
     *
     * @return device handle, or 0 if creation failed
     */
    static native long createDevice();

    /**
     * Destroys a Metal device.
     *
     * @param device device handle
     */
    static native void destroyDevice(long device);

    /**
     * Checks if a device is ready for rendering.
     *
     * @param device device handle
     * @return true if device is ready
     */
    static native boolean isDeviceReady(long device);

    /**
     * Gets the device name.
     *
     * @param device device handle
     * @return device name string
     */
    static native String getDeviceName(long device);

    /**
     * Gets the maximum supported texture size.
     *
     * @param device device handle
     * @return maximum texture dimension in pixels
     */
    static native int getMaxTextureSize(long device);

    /**
     * Gets the maximum supported line width.
     *
     * @param device device handle
     * @return maximum line width (usually 1.0 on Metal)
     */
    static native float getMaxLineWidth(long device);

    // =========================================================================
    // Command Queue
    // =========================================================================

    /**
     * Creates a command queue for a device.
     *
     * @param device device handle
     * @return command queue handle, or 0 if creation failed
     */
    static native long createCommandQueue(long device);

    /**
     * Destroys a command queue.
     *
     * @param queue command queue handle
     */
    static native void destroyCommandQueue(long queue);

    // =========================================================================
    // Command Buffer and Encoding
    // =========================================================================

    /**
     * Creates a command buffer from a command queue.
     *
     * @param queue command queue handle
     * @return command buffer handle (autoreleased, valid for one frame)
     */
    static native long createCommandBuffer(long queue);

    /**
     * Creates a render pass descriptor.
     *
     * @param colorTexture target color texture handle
     * @param clearR       clear color red component (0-1)
     * @param clearG       clear color green component (0-1)
     * @param clearB       clear color blue component (0-1)
     * @param clearA       clear color alpha component (0-1)
     * @return render pass descriptor handle
     */
    static native long createRenderPassDescriptor(long colorTexture,
                                                   float clearR, float clearG, float clearB, float clearA);

    /**
     * Destroys a render pass descriptor.
     *
     * @param descriptor render pass descriptor handle
     */
    static native void destroyRenderPassDescriptor(long descriptor);

    /**
     * Creates a render command encoder.
     *
     * @param commandBuffer command buffer handle
     * @param descriptor    render pass descriptor handle
     * @return render command encoder handle (autoreleased)
     */
    static native long createRenderCommandEncoder(long commandBuffer, long descriptor);

    /**
     * Ends encoding on a render command encoder.
     *
     * @param encoder render command encoder handle
     */
    static native void endEncoding(long encoder);

    /**
     * Commits a command buffer for execution.
     *
     * @param commandBuffer command buffer handle
     */
    static native void commitCommandBuffer(long commandBuffer);

    /**
     * Waits until a command buffer has completed execution.
     *
     * @param commandBuffer command buffer handle
     */
    static native void waitUntilCompleted(long commandBuffer);

    // =========================================================================
    // Buffer Operations
    // =========================================================================

    /**
     * Creates a buffer with the specified size.
     *
     * @param device      device handle
     * @param sizeInBytes buffer size in bytes
     * @param options     resource options (0 for shared storage)
     * @return buffer handle, or 0 if creation failed
     */
    static native long createBuffer(long device, int sizeInBytes, int options);

    /**
     * Destroys a buffer.
     *
     * @param buffer buffer handle
     */
    static native void destroyBuffer(long buffer);

    /**
     * Uploads float array data to a buffer.
     *
     * @param buffer buffer handle
     * @param data   float array data
     * @param offset offset in floats from start of data array
     * @param count  number of floats to upload
     */
    static native void uploadBufferData(long buffer, float[] data, int offset, int count);

    /**
     * Uploads data from a direct ByteBuffer to a buffer.
     *
     * @param buffer       buffer handle
     * @param data         direct ByteBuffer
     * @param offsetBytes  offset in bytes
     * @param lengthBytes  length in bytes
     */
    static native void uploadBufferDataDirect(long buffer, ByteBuffer data, int offsetBytes, int lengthBytes);

    /**
     * Binds a buffer as a vertex buffer.
     *
     * @param encoder encoder handle
     * @param buffer  buffer handle
     * @param offset  offset in bytes
     * @param index   buffer index (0 for vertex data, 1 for uniforms)
     */
    static native void setVertexBuffer(long encoder, long buffer, int offset, int index);

    // =========================================================================
    // Draw Calls
    // =========================================================================

    /**
     * Draws non-indexed primitives.
     *
     * @param encoder       encoder handle
     * @param primitiveType primitive type (0=point, 1=line, 2=lineStrip, 3=triangle, 4=triangleStrip)
     * @param vertexStart   first vertex index
     * @param vertexCount   number of vertices
     */
    static native void drawPrimitives(long encoder, int primitiveType, int vertexStart, int vertexCount);

    /**
     * Draws indexed primitives.
     *
     * @param encoder       encoder handle
     * @param primitiveType primitive type
     * @param indexCount    number of indices
     * @param indexType     index type (0=UInt16, 1=UInt32)
     * @param indexBuffer   index buffer handle
     * @param indexOffset   offset in bytes into index buffer
     */
    static native void drawIndexedPrimitives(long encoder, int primitiveType, int indexCount,
                                              int indexType, long indexBuffer, int indexOffset);

    // =========================================================================
    // Shader/Library Operations
    // =========================================================================

    /**
     * Creates a shader library from MSL source code.
     *
     * @param device    device handle
     * @param mslSource Metal Shading Language source code
     * @return library handle, or 0 if compilation failed
     */
    static native long createLibraryFromSource(long device, String mslSource);

    /**
     * Creates a shader library from precompiled metallib data.
     *
     * @param device       device handle
     * @param metallibData precompiled metallib binary data
     * @return library handle, or 0 if loading failed
     */
    static native long createLibraryFromMetallib(long device, byte[] metallibData);

    /**
     * Destroys a shader library.
     *
     * @param library library handle
     */
    static native void destroyLibrary(long library);

    /**
     * Gets a function from a shader library.
     *
     * @param library      library handle
     * @param functionName function name
     * @return function handle, or 0 if not found
     */
    static native long createFunction(long library, String functionName);

    /**
     * Destroys a shader function.
     *
     * @param function function handle
     */
    static native void destroyFunction(long function);

    // =========================================================================
    // Pipeline State
    // =========================================================================

    /**
     * Creates a render pipeline state.
     *
     * @param device           device handle
     * @param vertexFunction   vertex function handle
     * @param fragmentFunction fragment function handle
     * @param pixelFormat      pixel format (e.g., 80 for BGRA8Unorm)
     * @param blendMode        blend mode (0=none, 1=alpha, 2=additive, 3=multiply)
     * @param attributeFormats vertex attribute formats array
     * @param attributeOffsets vertex attribute offsets array
     * @param stride           vertex stride in bytes
     * @return pipeline state handle, or 0 if creation failed
     */
    static native long createPipelineState(long device, long vertexFunction, long fragmentFunction,
                                            int pixelFormat, int blendMode,
                                            int[] attributeFormats, int[] attributeOffsets, int stride);

    /**
     * Destroys a pipeline state.
     *
     * @param pipelineState pipeline state handle
     */
    static native void destroyPipelineState(long pipelineState);

    /**
     * Binds a pipeline state to a render encoder.
     *
     * @param encoder       encoder handle
     * @param pipelineState pipeline state handle
     */
    static native void setRenderPipelineState(long encoder, long pipelineState);

    // =========================================================================
    // Uniforms (Push Constants Equivalent)
    // =========================================================================

    /**
     * Sets vertex shader bytes (small uniform data).
     *
     * @param encoder encoder handle
     * @param data    float array data
     * @param count   number of floats
     * @param index   buffer index
     */
    static native void setVertexBytes(long encoder, float[] data, int count, int index);

    /**
     * Sets fragment shader bytes (small uniform data).
     *
     * @param encoder encoder handle
     * @param data    float array data
     * @param count   number of floats
     * @param index   buffer index
     */
    static native void setFragmentBytes(long encoder, float[] data, int count, int index);

    // =========================================================================
    // Texture Operations
    // =========================================================================

    /**
     * Creates a 2D texture.
     *
     * @param device device handle
     * @param width  texture width
     * @param height texture height
     * @param format pixel format
     * @param usage  texture usage flags
     * @return texture handle, or 0 if creation failed
     */
    static native long createTexture(long device, int width, int height, int format, int usage);

    /**
     * Destroys a texture.
     *
     * @param texture texture handle
     */
    static native void destroyTexture(long texture);

    /**
     * Uploads data to a texture.
     *
     * @param texture     texture handle
     * @param data        pixel data
     * @param width       region width
     * @param height      region height
     * @param bytesPerRow bytes per row
     */
    static native void uploadTextureData(long texture, byte[] data, int width, int height, int bytesPerRow);

    /**
     * Binds a texture to a fragment shader.
     *
     * @param encoder encoder handle
     * @param texture texture handle
     * @param index   texture index
     */
    static native void setFragmentTexture(long encoder, long texture, int index);

    // =========================================================================
    // Sampler Operations
    // =========================================================================

    /**
     * Creates a texture sampler.
     *
     * @param device      device handle
     * @param minFilter   minification filter (0=nearest, 1=linear)
     * @param magFilter   magnification filter (0=nearest, 1=linear)
     * @param addressMode address mode (0=clampToEdge, 1=repeat, 2=mirrorRepeat)
     * @return sampler handle, or 0 if creation failed
     */
    static native long createSampler(long device, int minFilter, int magFilter, int addressMode);

    /**
     * Destroys a sampler.
     *
     * @param sampler sampler handle
     */
    static native void destroySampler(long sampler);

    /**
     * Binds a sampler to a fragment shader.
     *
     * @param encoder encoder handle
     * @param sampler sampler handle
     * @param index   sampler index
     */
    static native void setFragmentSamplerState(long encoder, long sampler, int index);

    // =========================================================================
    // Framebuffer / Pixel Readback
    // =========================================================================

    /**
     * Creates an offscreen render target texture.
     *
     * @param device device handle
     * @param width  texture width
     * @param height texture height
     * @return texture handle configured for rendering and readback
     */
    static native long createOffscreenTexture(long device, int width, int height);

    /**
     * Reads pixels from a texture into an int array (ARGB format).
     *
     * @param texture texture handle
     * @param pixels  destination int array (TYPE_INT_ARGB format)
     * @param width   texture width
     * @param height  texture height
     */
    static native void readPixels(long texture, int[] pixels, int width, int height);

    /**
     * Synchronizes a texture with CPU for readback (adds blit command).
     *
     * @param commandBuffer command buffer handle
     * @param texture       texture handle
     */
    static native void synchronizeTexture(long commandBuffer, long texture);

    // =========================================================================
    // Render State
    // =========================================================================

    /**
     * Sets the viewport.
     *
     * @param encoder encoder handle
     * @param x       viewport x origin
     * @param y       viewport y origin
     * @param width   viewport width
     * @param height  viewport height
     * @param znear   near depth (usually 0)
     * @param zfar    far depth (usually 1)
     */
    static native void setViewport(long encoder, double x, double y, double width, double height,
                                    double znear, double zfar);

    /**
     * Sets the scissor rectangle.
     *
     * @param encoder encoder handle
     * @param x       scissor x origin
     * @param y       scissor y origin
     * @param width   scissor width
     * @param height  scissor height
     */
    static native void setScissorRect(long encoder, int x, int y, int width, int height);

    /**
     * Sets the cull mode.
     *
     * @param encoder  encoder handle
     * @param cullMode cull mode (0=none, 1=front, 2=back)
     */
    static native void setCullMode(long encoder, int cullMode);

    /**
     * Sets the front-facing winding order.
     *
     * @param encoder encoder handle
     * @param winding winding order (0=clockwise, 1=counterClockwise)
     */
    static native void setFrontFacing(long encoder, int winding);

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Checks if Metal is supported on this system.
     *
     * @return true if Metal is available
     */
    static native boolean isMetalSupported();

    // =========================================================================
    // Library Loading
    // =========================================================================

    /**
     * Returns true if the Metal native library is available.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Returns the error message if library loading failed.
     */
    static String getLoadError() {
        return loadError;
    }

    /**
     * Ensures the native library is loaded.
     *
     * @throws UnsupportedOperationException if Metal is not available
     */
    static void ensureLoaded() {
        if (!available) {
            throw new UnsupportedOperationException(
                    "Metal is not available: " + (loadError != null ? loadError : "unknown reason"));
        }
    }

    private static void loadNativeLibrary() {
        if (loaded) {
            return;
        }

        // Only attempt on macOS
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            loadError = "Metal is only available on macOS";
            log.debug("Metal native library not available: {}", loadError);
            loaded = true;
            return;
        }

        try {
            // Try loading from java.library.path first
            try {
                System.loadLibrary(LIBRARY_NAME);
                available = isMetalSupported();
                if (!available) {
                    loadError = "Metal not supported on this hardware";
                }
                log.info("Loaded Metal native library from library path, available={}", available);
                loaded = true;
                return;
            } catch (UnsatisfiedLinkError e) {
                log.debug("Metal library not in path, attempting to extract from JAR");
            }

            // Extract from JAR
            extractAndLoadLibrary();
            available = isMetalSupported();
            if (!available) {
                loadError = "Metal not supported on this hardware";
            }
            log.info("Loaded Metal native library from JAR, available={}", available);

        } catch (Exception e) {
            loadError = e.getMessage();
            log.warn("Failed to load Metal native library: {}", loadError);
        }

        loaded = true;
    }

    private static void extractAndLoadLibrary() throws IOException {
        String arch = System.getProperty("os.arch", "");
        String libName = "lib" + LIBRARY_NAME + ".dylib";
        String resourcePath = "/native/macos/" +
                (arch.contains("aarch64") || arch.contains("arm") ? "arm64" : "x86_64") +
                "/" + libName;

        try (InputStream in = MetalNative.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Native library not found: " + resourcePath);
            }

            Path tempDir = Files.createTempDirectory("chartx-native");
            Path tempLib = tempDir.resolve(libName);
            Files.copy(in, tempLib, StandardCopyOption.REPLACE_EXISTING);
            tempLib.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();

            System.load(tempLib.toAbsolutePath().toString());
        }
    }

    private MetalNative() {
        // Prevent instantiation
    }
}
