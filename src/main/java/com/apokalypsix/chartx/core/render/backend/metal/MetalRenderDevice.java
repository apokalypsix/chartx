package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metal implementation of the RenderDevice interface for macOS.
 *
 * <p>Provides Metal-based rendering for ChartX via JNI. This implementation
 * supports offscreen rendering for integration with Swing applications.
 *
 * <p>Key features:
 * <ul>
 *   <li>Metal device and command queue management</li>
 *   <li>Offscreen rendering to textures for Swing integration</li>
 *   <li>MSL (Metal Shading Language) shader support</li>
 *   <li>Efficient buffer management</li>
 * </ul>
 */
public class MetalRenderDevice implements RenderDevice {

    private static final Logger log = LoggerFactory.getLogger(MetalRenderDevice.class);

    // Metal object handles
    private long device;
    private long commandQueue;
    private long commandBuffer;
    private long renderPassDescriptor;
    private long renderEncoder;

    // Offscreen rendering
    private long offscreenTexture;
    private int textureWidth;
    private int textureHeight;

    // Device info
    private String deviceName = "Unknown";
    private int maxTextureSize = 16384;
    private float maxLineWidth = 1.0f;

    // State
    private boolean initialized = false;
    private boolean inFrame = false;
    private int frameWidth = 800;
    private int frameHeight = 600;

    // Clear color
    private float clearR = 0.1f;
    private float clearG = 0.1f;
    private float clearB = 0.12f;
    private float clearA = 1.0f;

    // Render state
    private BlendMode currentBlendMode = BlendMode.ALPHA;
    private float currentLineWidth = 1.0f;
    private boolean scissorEnabled = false;
    private int scissorX, scissorY, scissorWidth, scissorHeight;

    // Currently bound shader
    private MetalShader currentShader;
    private MetalResourceManager resourceManager;

    public MetalRenderDevice() {
        // Default constructor
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        log.info("Initializing Metal render device");

        // Ensure native library is loaded
        MetalNative.ensureLoaded();

        // Create Metal device
        device = MetalNative.createDevice();
        if (device == 0) {
            throw new RuntimeException("Failed to create Metal device");
        }

        // Get device info
        deviceName = MetalNative.getDeviceName(device);
        maxTextureSize = MetalNative.getMaxTextureSize(device);
        maxLineWidth = MetalNative.getMaxLineWidth(device);

        log.info("Metal device: {} (max texture: {}, max line width: {})",
                deviceName, maxTextureSize, maxLineWidth);

        // Create command queue
        commandQueue = MetalNative.createCommandQueue(device);
        if (commandQueue == 0) {
            MetalNative.destroyDevice(device);
            throw new RuntimeException("Failed to create Metal command queue");
        }

        // Create initial offscreen texture
        createOffscreenTexture(frameWidth, frameHeight);

        initialized = true;
        log.info("Metal render device initialized");
    }

    private void createOffscreenTexture(int width, int height) {
        if (offscreenTexture != 0) {
            MetalNative.destroyTexture(offscreenTexture);
        }

        offscreenTexture = MetalNative.createOffscreenTexture(device, width, height);
        if (offscreenTexture == 0) {
            throw new RuntimeException("Failed to create offscreen texture");
        }

        textureWidth = width;
        textureHeight = height;
    }

    @Override
    public void dispose() {
        if (!initialized) {
            return;
        }

        log.info("Disposing Metal render device");

        // Wait for any in-flight work
        if (commandBuffer != 0) {
            MetalNative.waitUntilCompleted(commandBuffer);
        }

        // Destroy resources
        if (offscreenTexture != 0) {
            MetalNative.destroyTexture(offscreenTexture);
            offscreenTexture = 0;
        }

        if (commandQueue != 0) {
            MetalNative.destroyCommandQueue(commandQueue);
            commandQueue = 0;
        }

        if (device != 0) {
            MetalNative.destroyDevice(device);
            device = 0;
        }

        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.METAL;
    }

    @Override
    public void beginFrame() {
        if (!initialized || inFrame) {
            return;
        }

        // Resize offscreen texture if needed
        if (textureWidth != frameWidth || textureHeight != frameHeight) {
            createOffscreenTexture(frameWidth, frameHeight);
        }

        // Create command buffer
        commandBuffer = MetalNative.createCommandBuffer(commandQueue);
        if (commandBuffer == 0) {
            log.error("Failed to create command buffer");
            return;
        }

        // Create render pass descriptor with clear color
        renderPassDescriptor = MetalNative.createRenderPassDescriptor(
                offscreenTexture, clearR, clearG, clearB, clearA);
        if (renderPassDescriptor == 0) {
            log.error("Failed to create render pass descriptor");
            return;
        }

        // Create render encoder
        renderEncoder = MetalNative.createRenderCommandEncoder(commandBuffer, renderPassDescriptor);
        if (renderEncoder == 0) {
            MetalNative.destroyRenderPassDescriptor(renderPassDescriptor);
            log.error("Failed to create render encoder");
            return;
        }

        inFrame = true;

        // Set default viewport
        MetalNative.setViewport(renderEncoder, 0, 0, frameWidth, frameHeight, 0.0, 1.0);

        // Set default scissor (full viewport)
        MetalNative.setScissorRect(renderEncoder, 0, 0, frameWidth, frameHeight);
    }

    @Override
    public void endFrame() {
        if (!initialized || !inFrame) {
            return;
        }

        // End encoding
        MetalNative.endEncoding(renderEncoder);
        renderEncoder = 0;

        // Synchronize texture for CPU readback
        MetalNative.synchronizeTexture(commandBuffer, offscreenTexture);

        // Commit and wait
        MetalNative.commitCommandBuffer(commandBuffer);
        MetalNative.waitUntilCompleted(commandBuffer);

        // Clean up
        MetalNative.destroyRenderPassDescriptor(renderPassDescriptor);
        renderPassDescriptor = 0;
        commandBuffer = 0;

        inFrame = false;
    }

    /**
     * Reads the rendered frame into a pixel array.
     * Must be called after endFrame().
     *
     * @param pixels array to fill with ARGB pixels (must be width * height size)
     */
    public void readFramePixels(int[] pixels) {
        if (!initialized || offscreenTexture == 0) {
            return;
        }

        MetalNative.readPixels(offscreenTexture, pixels, frameWidth, frameHeight);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        frameWidth = width;
        frameHeight = height;

        if (inFrame && renderEncoder != 0) {
            MetalNative.setViewport(renderEncoder, x, y, width, height, 0.0, 1.0);
        }
    }

    @Override
    public void setScissorEnabled(boolean enabled) {
        scissorEnabled = enabled;
        if (inFrame && renderEncoder != 0) {
            if (enabled) {
                MetalNative.setScissorRect(renderEncoder, scissorX, scissorY, scissorWidth, scissorHeight);
            } else {
                MetalNative.setScissorRect(renderEncoder, 0, 0, frameWidth, frameHeight);
            }
        }
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        scissorX = x;
        scissorY = y;
        scissorWidth = width;
        scissorHeight = height;

        if (inFrame && renderEncoder != 0 && scissorEnabled) {
            MetalNative.setScissorRect(renderEncoder, x, y, width, height);
        }
    }

    @Override
    public void setBlendMode(BlendMode mode) {
        currentBlendMode = mode;
        // Blend mode is part of pipeline state in Metal
    }

    @Override
    public void setLineWidth(float width) {
        currentLineWidth = Math.min(width, maxLineWidth);
        // Line width is fixed at 1.0 in Metal (no wide lines support)
    }

    @Override
    public void setLineSmoothing(boolean enabled) {
        // Line smoothing controlled via MSAA in Metal
    }

    @Override
    public void setDepthTestEnabled(boolean enabled) {
        // Depth test is part of pipeline state
    }

    @Override
    public void clearScreen(float r, float g, float b, float a) {
        // Store clear color for next frame's render pass
        clearR = r;
        clearG = g;
        clearB = b;
        clearA = a;
    }

    @Override
    public void clearDepth() {
        // Depth clear would be done via render pass
    }

    @Override
    public Shader createShader(ShaderSource source) {
        return new MetalShader(this, source);
    }

    @Override
    public Buffer createBuffer(BufferDescriptor descriptor) {
        return new MetalBuffer(this, descriptor);
    }

    @Override
    public Texture createTexture(TextureDescriptor descriptor) {
        return new MetalTexture(this, descriptor);
    }

    @Override
    public float getMaxLineWidth() {
        return maxLineWidth;
    }

    @Override
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    @Override
    public String getRendererInfo() {
        return "Metal - " + deviceName;
    }

    // -------------------------------------------------------------------------
    // Metal-specific getters
    // -------------------------------------------------------------------------

    /**
     * Returns the Metal device handle.
     */
    public long getDevice() {
        return device;
    }

    /**
     * Returns the command queue handle.
     */
    public long getCommandQueue() {
        return commandQueue;
    }

    /**
     * Returns the current render encoder handle.
     * Only valid during frame rendering.
     */
    public long getRenderEncoder() {
        return inFrame ? renderEncoder : 0;
    }

    /**
     * Returns true if currently recording a frame.
     */
    public boolean isInFrame() {
        return inFrame;
    }

    /**
     * Returns the current blend mode.
     */
    public BlendMode getCurrentBlendMode() {
        return currentBlendMode;
    }

    /**
     * Returns the current frame width.
     */
    public int getFrameWidth() {
        return frameWidth;
    }

    /**
     * Returns the current frame height.
     */
    public int getFrameHeight() {
        return frameHeight;
    }

    /**
     * Sets the currently bound shader.
     */
    public void setCurrentShader(MetalShader shader) {
        this.currentShader = shader;
    }

    /**
     * Returns the currently bound shader.
     */
    public MetalShader getCurrentShader() {
        return currentShader;
    }

    /**
     * Sets the resource manager for this device.
     */
    public void setResourceManager(MetalResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Returns the resource manager.
     */
    public MetalResourceManager getResourceManager() {
        return resourceManager;
    }
}
