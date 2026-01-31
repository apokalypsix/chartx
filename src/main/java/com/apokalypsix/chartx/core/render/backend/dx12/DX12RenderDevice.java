package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DirectX 12 implementation of RenderDevice.
 *
 * <p>Manages the DX12 device, command queue, command lists, and synchronization.
 * Renders to an offscreen texture for Swing integration.
 */
public class DX12RenderDevice implements RenderDevice {

    private static final Logger log = LoggerFactory.getLogger(DX12RenderDevice.class);

    // DX12 handles
    private long device;
    private long commandQueue;
    private long commandAllocator;
    private long commandList;
    private long fence;
    private long fenceValue;

    // Render target
    private long renderTarget;
    private long rtvHeap;
    private long readbackBuffer;
    private long rootSignature;

    // State
    private int width;
    private int height;
    private boolean initialized;
    private BlendMode currentBlendMode = BlendMode.NONE;
    private boolean scissorEnabled;

    // Clear color
    private float clearR, clearG, clearB, clearA = 1.0f;

    // Resource manager reference
    private DX12ResourceManager resourceManager;

    public DX12RenderDevice() {
        // Default constructor
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        log.info("Initializing DX12 render device");

        // Create device
        device = DX12Native.createDevice();
        if (device == 0) {
            throw new RuntimeException("Failed to create DX12 device");
        }

        // Create command queue
        commandQueue = DX12Native.createCommandQueue(device);
        if (commandQueue == 0) {
            throw new RuntimeException("Failed to create DX12 command queue");
        }

        // Create command allocator
        commandAllocator = DX12Native.createCommandAllocator(device);
        if (commandAllocator == 0) {
            throw new RuntimeException("Failed to create DX12 command allocator");
        }

        // Create command list
        commandList = DX12Native.createCommandList(device, commandAllocator);
        if (commandList == 0) {
            throw new RuntimeException("Failed to create DX12 command list");
        }

        // Create fence for synchronization
        fence = DX12Native.createFence(device);
        if (fence == 0) {
            throw new RuntimeException("Failed to create DX12 fence");
        }
        fenceValue = 1;

        // Create root signature (20 constants: mat4 + vec4)
        rootSignature = DX12Native.createRootSignature(device, 20);
        if (rootSignature == 0) {
            throw new RuntimeException("Failed to create DX12 root signature");
        }

        // Create RTV heap
        rtvHeap = DX12Native.createRTVHeap(device, 1);
        if (rtvHeap == 0) {
            throw new RuntimeException("Failed to create DX12 RTV heap");
        }

        initialized = true;
        log.info("DX12 render device initialized");
    }

    @Override
    public void dispose() {
        if (!initialized) {
            return;
        }

        log.info("Disposing DX12 render device");

        // Wait for GPU to finish
        waitForGpu();

        // Release resources
        if (readbackBuffer != 0) DX12Native.destroyBuffer(readbackBuffer);
        if (renderTarget != 0) DX12Native.destroyRenderTarget(renderTarget);
        if (rtvHeap != 0) DX12Native.destroyDescriptorHeap(rtvHeap);
        if (rootSignature != 0) DX12Native.destroyRootSignature(rootSignature);
        if (fence != 0) DX12Native.destroyFence(fence);
        if (commandList != 0) DX12Native.destroyCommandList(commandList);
        if (commandAllocator != 0) DX12Native.destroyCommandAllocator(commandAllocator);
        if (commandQueue != 0) DX12Native.destroyCommandQueue(commandQueue);
        if (device != 0) DX12Native.destroyDevice(device);

        initialized = false;
        log.info("DX12 render device disposed");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.DX12;
    }

    @Override
    public void beginFrame() {
        if (!initialized) return;

        // Reset command allocator and command list
        DX12Native.resetCommandAllocator(commandAllocator);
        DX12Native.resetCommandList(commandList, commandAllocator, 0);

        // Set root signature
        DX12Native.setGraphicsRootSignature(commandList, rootSignature);

        // Begin render pass (sets render target, transitions state, clears)
        if (renderTarget != 0) {
            DX12Native.beginRenderPass(commandList, renderTarget, rtvHeap, clearR, clearG, clearB, clearA);
        }
    }

    @Override
    public void endFrame() {
        if (!initialized) return;

        // End render pass (transitions state back)
        if (renderTarget != 0) {
            DX12Native.endRenderPass(commandList, renderTarget);
        }

        // Close command list
        DX12Native.closeCommandList(commandList);

        // Execute
        DX12Native.executeCommandList(commandQueue, commandList);

        // Signal and wait
        DX12Native.signalFence(commandQueue, fence, fenceValue);
        DX12Native.waitForFence(fence, fenceValue);
        fenceValue++;
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        if (commandList != 0) {
            DX12Native.setViewport(commandList, x, y, width, height, 0.0f, 1.0f);
            DX12Native.setScissorRect(commandList, x, y, x + width, y + height);
        }
    }

    @Override
    public void setScissorEnabled(boolean enabled) {
        this.scissorEnabled = enabled;
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        if (commandList != 0 && scissorEnabled) {
            DX12Native.setScissorRect(commandList, x, y, x + width, y + height);
        }
    }

    @Override
    public void setBlendMode(BlendMode mode) {
        this.currentBlendMode = mode;
        // Blend mode is set via PSO in DX12, handled during draw
    }

    @Override
    public void setLineWidth(float width) {
        // DX12 doesn't support line width (always 1 pixel)
    }

    @Override
    public void setLineSmoothing(boolean enabled) {
        // Would need MSAA or custom anti-aliasing in DX12
    }

    @Override
    public void setDepthTestEnabled(boolean enabled) {
        // Depth test configured via PSO
    }

    @Override
    public void clearScreen(float r, float g, float b, float a) {
        // Store clear color for beginRenderPass
        this.clearR = r;
        this.clearG = g;
        this.clearB = b;
        this.clearA = a;
        // Actual clear happens in beginRenderPass
    }

    @Override
    public void clearDepth() {
        // Not using depth buffer for 2D rendering
    }

    @Override
    public Shader createShader(ShaderSource source) {
        // DX12 uses HLSL, not GLSL/SPIR-V from ShaderSource
        // Shaders are created via DX12ShaderRegistry
        throw new UnsupportedOperationException("Use DX12ResourceManager for shader creation");
    }

    @Override
    public Buffer createBuffer(BufferDescriptor descriptor) {
        return new DX12Buffer(this, descriptor);
    }

    @Override
    public Texture createTexture(TextureDescriptor descriptor) {
        return new DX12Texture(this, descriptor);
    }

    @Override
    public float getMaxLineWidth() {
        return device != 0 ? DX12Native.getMaxLineWidth(device) : 1.0f;
    }

    @Override
    public int getMaxTextureSize() {
        return device != 0 ? DX12Native.getMaxTextureSize(device) : 16384;
    }

    @Override
    public String getRendererInfo() {
        return device != 0 ? DX12Native.getDeviceName(device) : "DirectX 12";
    }

    // -------------------------------------------------------------------------
    // DX12-specific methods
    // -------------------------------------------------------------------------

    /**
     * Creates or resizes the offscreen render target.
     */
    public void createOffscreenTarget(int width, int height) {
        if (width == this.width && height == this.height && renderTarget != 0) {
            return;
        }

        this.width = width;
        this.height = height;

        // Destroy old resources
        if (renderTarget != 0) {
            DX12Native.destroyRenderTarget(renderTarget);
        }
        if (readbackBuffer != 0) {
            DX12Native.destroyBuffer(readbackBuffer);
        }

        // Create render target
        renderTarget = DX12Native.createRenderTarget(device, width, height);
        if (renderTarget == 0) {
            throw new RuntimeException("Failed to create DX12 render target");
        }

        // Create RTV
        DX12Native.createRTV(device, renderTarget, rtvHeap, 0);

        // Create readback buffer
        int readbackSize = width * height * 4;
        readbackBuffer = DX12Native.createReadbackBuffer(device, readbackSize);

        log.debug("Created DX12 offscreen target {}x{}", width, height);
    }

    /**
     * Reads the frame pixels into the given array.
     */
    public void readFramePixels(int[] pixels) {
        if (renderTarget == 0 || readbackBuffer == 0 || commandList == 0) {
            return;
        }

        // Copy texture to readback buffer
        DX12Native.copyTextureToBuffer(commandList, renderTarget, readbackBuffer, width, height);

        // Read data from readback buffer
        DX12Native.readPixels(readbackBuffer, pixels, width, height);
    }

    /**
     * Waits for the GPU to finish all work.
     */
    public void waitForGpu() {
        if (commandQueue != 0 && fence != 0) {
            DX12Native.signalFence(commandQueue, fence, fenceValue);
            DX12Native.waitForFence(fence, fenceValue);
            fenceValue++;
        }
    }

    /**
     * Gets the device handle.
     */
    public long getDevice() {
        return device;
    }

    /**
     * Gets the command list handle.
     */
    public long getCommandList() {
        return commandList;
    }

    /**
     * Gets the root signature handle.
     */
    public long getRootSignature() {
        return rootSignature;
    }

    /**
     * Gets the current blend mode.
     */
    public BlendMode getCurrentBlendMode() {
        return currentBlendMode;
    }

    /**
     * Sets the resource manager.
     */
    void setResourceManager(DX12ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Gets the resource manager.
     */
    DX12ResourceManager getResourceManager() {
        return resourceManager;
    }
}
