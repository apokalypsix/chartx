package com.apokalypsix.chartx.core.render.backend.dx12;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * JNI bridge to native DirectX 12 API.
 *
 * <p>All methods are static and operate on opaque handles (jlong).
 * The native library must be loaded before calling any methods.
 *
 * <p>Handle values represent COM object pointers on the native side.
 * Each createXxx method returns a handle that must be released via
 * the corresponding destroyXxx method.
 */
public final class DX12Native {

    private static final Logger log = LoggerFactory.getLogger(DX12Native.class);
    private static final String LIBRARY_NAME = "chartx-dx12";

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
     * Creates the default DirectX 12 device.
     *
     * @return device handle, or 0 if creation failed
     */
    static native long createDevice();

    /**
     * Destroys a DirectX 12 device.
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
     * Gets the device name (adapter description).
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
     * @return maximum line width (usually 1.0 on DX12)
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
    // Command Allocator and List
    // =========================================================================

    /**
     * Creates a command allocator for a device.
     *
     * @param device device handle
     * @return command allocator handle, or 0 if creation failed
     */
    static native long createCommandAllocator(long device);

    /**
     * Destroys a command allocator.
     *
     * @param allocator command allocator handle
     */
    static native void destroyCommandAllocator(long allocator);

    /**
     * Resets a command allocator.
     *
     * @param allocator command allocator handle
     */
    static native void resetCommandAllocator(long allocator);

    /**
     * Creates a graphics command list.
     *
     * @param device device handle
     * @param allocator command allocator handle
     * @return command list handle, or 0 if creation failed
     */
    static native long createCommandList(long device, long allocator);

    /**
     * Destroys a command list.
     *
     * @param commandList command list handle
     */
    static native void destroyCommandList(long commandList);

    /**
     * Resets a command list with a new allocator.
     *
     * @param commandList command list handle
     * @param allocator command allocator handle
     * @param pipelineState optional pipeline state handle (0 for none)
     */
    static native void resetCommandList(long commandList, long allocator, long pipelineState);

    /**
     * Closes a command list for execution.
     *
     * @param commandList command list handle
     */
    static native void closeCommandList(long commandList);

    /**
     * Executes command lists on a queue.
     *
     * @param queue command queue handle
     * @param commandList command list handle
     */
    static native void executeCommandList(long queue, long commandList);

    // =========================================================================
    // Fence Synchronization
    // =========================================================================

    /**
     * Creates a fence for GPU synchronization.
     *
     * @param device device handle
     * @return fence handle, or 0 if creation failed
     */
    static native long createFence(long device);

    /**
     * Destroys a fence.
     *
     * @param fence fence handle
     */
    static native void destroyFence(long fence);

    /**
     * Signals a fence from a command queue.
     *
     * @param queue command queue handle
     * @param fence fence handle
     * @param value fence value to signal
     */
    static native void signalFence(long queue, long fence, long value);

    /**
     * Waits for a fence to reach a value.
     *
     * @param fence fence handle
     * @param value fence value to wait for
     */
    static native void waitForFence(long fence, long value);

    /**
     * Gets the completed value of a fence.
     *
     * @param fence fence handle
     * @return completed fence value
     */
    static native long getFenceCompletedValue(long fence);

    // =========================================================================
    // Render Target / Offscreen Texture
    // =========================================================================

    /**
     * Creates an offscreen render target texture.
     *
     * @param device device handle
     * @param width texture width
     * @param height texture height
     * @return render target handle, or 0 if creation failed
     */
    static native long createRenderTarget(long device, int width, int height);

    /**
     * Destroys a render target.
     *
     * @param renderTarget render target handle
     */
    static native void destroyRenderTarget(long renderTarget);

    /**
     * Creates a descriptor heap for render target views.
     *
     * @param device device handle
     * @param numDescriptors number of descriptors
     * @return descriptor heap handle, or 0 if creation failed
     */
    static native long createRTVHeap(long device, int numDescriptors);

    /**
     * Destroys a descriptor heap.
     *
     * @param heap descriptor heap handle
     */
    static native void destroyDescriptorHeap(long heap);

    /**
     * Creates a render target view.
     *
     * @param device device handle
     * @param renderTarget render target handle
     * @param rtvHeap RTV descriptor heap handle
     * @param index index in the heap
     */
    static native void createRTV(long device, long renderTarget, long rtvHeap, int index);

    // =========================================================================
    // Render Pass Operations
    // =========================================================================

    /**
     * Begins a render pass (sets render target and clears).
     *
     * @param commandList command list handle
     * @param renderTarget render target handle
     * @param rtvHeap RTV heap handle
     * @param clearR clear color red (0-1)
     * @param clearG clear color green (0-1)
     * @param clearB clear color blue (0-1)
     * @param clearA clear color alpha (0-1)
     */
    static native void beginRenderPass(long commandList, long renderTarget, long rtvHeap,
                                        float clearR, float clearG, float clearB, float clearA);

    /**
     * Ends a render pass.
     *
     * @param commandList command list handle
     * @param renderTarget render target handle
     */
    static native void endRenderPass(long commandList, long renderTarget);

    // =========================================================================
    // Buffer Operations
    // =========================================================================

    /**
     * Creates an upload buffer (CPU-visible).
     *
     * @param device device handle
     * @param sizeInBytes buffer size in bytes
     * @return buffer handle, or 0 if creation failed
     */
    static native long createUploadBuffer(long device, int sizeInBytes);

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
     * @param data float array data
     * @param offset offset in floats
     * @param count number of floats to upload
     */
    static native void uploadBufferData(long buffer, float[] data, int offset, int count);

    /**
     * Uploads data from a direct ByteBuffer to a buffer.
     *
     * @param buffer buffer handle
     * @param data direct ByteBuffer
     * @param offsetBytes offset in bytes
     * @param lengthBytes length in bytes
     */
    static native void uploadBufferDataDirect(long buffer, ByteBuffer data, int offsetBytes, int lengthBytes);

    // =========================================================================
    // Draw Operations
    // =========================================================================

    /**
     * Sets the vertex buffer for drawing.
     *
     * @param commandList command list handle
     * @param buffer buffer handle
     * @param strideBytes vertex stride in bytes
     * @param sizeBytes total buffer size in bytes
     */
    static native void setVertexBuffer(long commandList, long buffer, int strideBytes, int sizeBytes);

    /**
     * Draws non-indexed primitives.
     *
     * @param commandList command list handle
     * @param vertexCount number of vertices
     * @param startVertex first vertex index
     */
    static native void drawInstanced(long commandList, int vertexCount, int startVertex);

    /**
     * Sets the primitive topology.
     *
     * @param commandList command list handle
     * @param topology topology type (1=POINTLIST, 2=LINELIST, 3=LINESTRIP, 4=TRIANGLELIST, 5=TRIANGLESTRIP)
     */
    static native void setPrimitiveTopology(long commandList, int topology);

    // =========================================================================
    // Pipeline State
    // =========================================================================

    /**
     * Creates a root signature.
     *
     * @param device device handle
     * @param numConstants number of 32-bit constants
     * @return root signature handle, or 0 if creation failed
     */
    static native long createRootSignature(long device, int numConstants);

    /**
     * Destroys a root signature.
     *
     * @param rootSignature root signature handle
     */
    static native void destroyRootSignature(long rootSignature);

    /**
     * Creates a pipeline state object.
     *
     * @param device device handle
     * @param rootSignature root signature handle
     * @param vertexShaderBytecode compiled vertex shader bytecode
     * @param pixelShaderBytecode compiled pixel shader bytecode
     * @param topology primitive topology
     * @param blendMode blend mode (0=none, 1=alpha, 2=additive, 3=multiply)
     * @param attributeFormats vertex attribute formats array
     * @param attributeOffsets vertex attribute offsets array
     * @param stride vertex stride in bytes
     * @return pipeline state handle, or 0 if creation failed
     */
    static native long createPipelineState(long device, long rootSignature,
                                            byte[] vertexShaderBytecode, byte[] pixelShaderBytecode,
                                            int topology, int blendMode,
                                            int[] attributeFormats, int[] attributeOffsets, int stride);

    /**
     * Destroys a pipeline state object.
     *
     * @param pipelineState pipeline state handle
     */
    static native void destroyPipelineState(long pipelineState);

    /**
     * Sets the pipeline state for rendering.
     *
     * @param commandList command list handle
     * @param pipelineState pipeline state handle
     */
    static native void setPipelineState(long commandList, long pipelineState);

    /**
     * Sets the root signature.
     *
     * @param commandList command list handle
     * @param rootSignature root signature handle
     */
    static native void setGraphicsRootSignature(long commandList, long rootSignature);

    // =========================================================================
    // Shader Compilation
    // =========================================================================

    /**
     * Compiles HLSL shader source to bytecode.
     *
     * @param source HLSL source code
     * @param entryPoint entry point function name
     * @param target shader target (e.g., "vs_5_0", "ps_5_0")
     * @return compiled bytecode, or null if compilation failed
     */
    static native byte[] compileShader(String source, String entryPoint, String target);

    /**
     * Gets the last shader compilation error message.
     *
     * @return error message, or null if no error
     */
    static native String getShaderCompileError();

    // =========================================================================
    // Uniforms (Root Constants)
    // =========================================================================

    /**
     * Sets 32-bit root constants (uniforms).
     *
     * @param commandList command list handle
     * @param data float array data
     * @param count number of floats
     * @param parameterIndex root parameter index
     */
    static native void setGraphicsRoot32BitConstants(long commandList, float[] data, int count, int parameterIndex);

    // =========================================================================
    // Viewport and Scissor
    // =========================================================================

    /**
     * Sets the viewport.
     *
     * @param commandList command list handle
     * @param x viewport x
     * @param y viewport y
     * @param width viewport width
     * @param height viewport height
     * @param minDepth minimum depth (usually 0)
     * @param maxDepth maximum depth (usually 1)
     */
    static native void setViewport(long commandList, float x, float y, float width, float height,
                                    float minDepth, float maxDepth);

    /**
     * Sets the scissor rectangle.
     *
     * @param commandList command list handle
     * @param left scissor left
     * @param top scissor top
     * @param right scissor right
     * @param bottom scissor bottom
     */
    static native void setScissorRect(long commandList, int left, int top, int right, int bottom);

    // =========================================================================
    // Readback
    // =========================================================================

    /**
     * Creates a readback buffer for CPU access.
     *
     * @param device device handle
     * @param sizeInBytes buffer size in bytes
     * @return readback buffer handle, or 0 if creation failed
     */
    static native long createReadbackBuffer(long device, int sizeInBytes);

    /**
     * Copies render target to readback buffer.
     *
     * @param commandList command list handle
     * @param renderTarget render target handle
     * @param readbackBuffer readback buffer handle
     * @param width image width
     * @param height image height
     */
    static native void copyTextureToBuffer(long commandList, long renderTarget,
                                            long readbackBuffer, int width, int height);

    /**
     * Reads pixels from readback buffer into int array (ARGB format).
     *
     * @param readbackBuffer readback buffer handle
     * @param pixels destination int array
     * @param width image width
     * @param height image height
     */
    static native void readPixels(long readbackBuffer, int[] pixels, int width, int height);

    // =========================================================================
    // Texture Operations
    // =========================================================================

    /**
     * Creates a 2D texture.
     *
     * @param device device handle
     * @param width texture width
     * @param height texture height
     * @param format pixel format
     * @return texture handle, or 0 if creation failed
     */
    static native long createTexture(long device, int width, int height, int format);

    /**
     * Destroys a texture.
     *
     * @param texture texture handle
     */
    static native void destroyTexture(long texture);

    /**
     * Uploads data to a texture.
     *
     * @param commandList command list handle
     * @param texture texture handle
     * @param data pixel data
     * @param width region width
     * @param height region height
     * @param bytesPerRow bytes per row
     */
    static native void uploadTextureData(long commandList, long texture, byte[] data,
                                          int width, int height, int bytesPerRow);

    /**
     * Sets a texture for pixel shader.
     *
     * @param commandList command list handle
     * @param texture texture handle
     * @param index texture slot index
     */
    static native void setTexture(long commandList, long texture, int index);

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Checks if DirectX 12 is supported on this system.
     *
     * @return true if DX12 is available
     */
    static native boolean isDX12Supported();

    // =========================================================================
    // Library Loading
    // =========================================================================

    /**
     * Returns true if the DirectX 12 native library is available.
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
     * @throws UnsupportedOperationException if DirectX 12 is not available
     */
    static void ensureLoaded() {
        if (!available) {
            throw new UnsupportedOperationException(
                    "DirectX 12 is not available: " + (loadError != null ? loadError : "unknown reason"));
        }
    }

    private static void loadNativeLibrary() {
        if (loaded) {
            return;
        }

        // Only attempt on Windows
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("windows")) {
            loadError = "DirectX 12 is only available on Windows";
            log.debug("DX12 native library not available: {}", loadError);
            loaded = true;
            return;
        }

        try {
            // Try loading from java.library.path first
            try {
                System.loadLibrary(LIBRARY_NAME);
                available = isDX12Supported();
                if (!available) {
                    loadError = "DirectX 12 not supported on this hardware";
                }
                log.info("Loaded DX12 native library from library path, available={}", available);
                loaded = true;
                return;
            } catch (UnsatisfiedLinkError e) {
                log.debug("DX12 library not in path, attempting to extract from JAR");
            }

            // Extract from JAR
            extractAndLoadLibrary();
            available = isDX12Supported();
            if (!available) {
                loadError = "DirectX 12 not supported on this hardware";
            }
            log.info("Loaded DX12 native library from JAR, available={}", available);

        } catch (Exception e) {
            loadError = e.getMessage();
            log.warn("Failed to load DX12 native library: {}", loadError);
        }

        loaded = true;
    }

    private static void extractAndLoadLibrary() throws IOException {
        String arch = System.getProperty("os.arch", "");
        String libName = LIBRARY_NAME + ".dll";
        String resourcePath = "/native/windows/" +
                (arch.contains("64") ? "x64" : "x86") +
                "/" + libName;

        try (InputStream in = DX12Native.class.getResourceAsStream(resourcePath)) {
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

    private DX12Native() {
        // Prevent instantiation
    }
}
