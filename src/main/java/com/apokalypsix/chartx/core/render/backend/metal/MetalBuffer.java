package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

/**
 * Metal buffer implementation for vertex data.
 *
 * <p>Uses Metal's shared memory mode for efficient CPU-to-GPU data transfer.
 * Buffers are mapped for direct access, allowing fast uploads without staging.
 */
public class MetalBuffer implements Buffer {

    private static final Logger log = LoggerFactory.getLogger(MetalBuffer.class);

    // Metal resource options for shared storage
    private static final int MTL_RESOURCE_STORAGE_MODE_SHARED = 0;

    private final MetalRenderDevice device;
    private final BufferDescriptor descriptor;

    private long buffer;
    private int capacity;
    private int currentVertexCount;
    private boolean disposed = false;

    // Current shader bound to this buffer (for pipeline lookup)
    private MetalShader currentShader;

    public MetalBuffer(MetalRenderDevice device, BufferDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.capacity = descriptor.getInitialCapacity();

        if (device.isInitialized()) {
            createBuffer();
        }
    }

    private void createBuffer() {
        int sizeInBytes = capacity * Float.BYTES;

        buffer = MetalNative.createBuffer(device.getDevice(), sizeInBytes, MTL_RESOURCE_STORAGE_MODE_SHARED);
        if (buffer == 0) {
            log.error("Failed to create Metal buffer");
            return;
        }

        log.debug("Created Metal buffer ({} bytes, {} floats capacity)", sizeInBytes, capacity);
    }

    @Override
    public void upload(float[] data, int offset, int count) {
        if (disposed || buffer == 0) return;

        // Resize if needed
        if (count > capacity) {
            resize(count + count / 2);
        }

        MetalNative.uploadBufferData(buffer, data, offset, count);
        currentVertexCount = count / descriptor.getFloatsPerVertex();
    }

    @Override
    public void upload(FloatBuffer data, int offset) {
        if (disposed || buffer == 0) return;

        int count = data.remaining();

        // Resize if needed
        if (count > capacity) {
            resize(count + count / 2);
        }

        // Convert FloatBuffer to float array for JNI upload
        float[] tempArray = new float[count];
        data.get(tempArray);

        MetalNative.uploadBufferData(buffer, tempArray, 0, count);
        currentVertexCount = count / descriptor.getFloatsPerVertex();
    }

    private void resize(int newCapacity) {
        log.debug("Resizing buffer from {} to {} floats", capacity, newCapacity);

        // Destroy old buffer
        if (buffer != 0) {
            MetalNative.destroyBuffer(buffer);
            buffer = 0;
        }

        // Create new buffer with larger capacity
        capacity = newCapacity;
        createBuffer();
    }

    @Override
    public void bind() {
        if (disposed || buffer == 0) return;

        long encoder = device.getRenderEncoder();
        if (encoder == 0) return;

        // Bind buffer at index 0 (vertex data)
        MetalNative.setVertexBuffer(encoder, buffer, 0, 0);
    }

    @Override
    public void unbind() {
        // Metal doesn't require explicit unbinding
    }

    @Override
    public void draw(DrawMode mode) {
        draw(mode, 0, currentVertexCount);
    }

    @Override
    public void draw(DrawMode mode, int first, int count) {
        if (disposed || count <= 0) return;

        long encoder = device.getRenderEncoder();
        if (encoder == 0) {
            log.warn("No render encoder available for draw call");
            return;
        }

        // Get shader from device or this buffer
        MetalShader shader = currentShader;
        if (shader == null) {
            shader = device.getCurrentShader();
        }
        if (shader == null || !shader.isValid()) {
            log.warn("No valid shader bound for draw call");
            return;
        }

        // Get or create pipeline for this draw mode
        MetalPipeline pipeline = findOrCreatePipeline(shader, mode);
        if (pipeline == null) {
            log.warn("Could not get pipeline for draw mode: {}", mode);
            return;
        }

        // Bind pipeline
        pipeline.bind(encoder);

        // Bind vertex buffer
        bind();

        // Flush uniforms (push constants equivalent)
        shader.flushUniforms(encoder);

        // Issue draw call
        int primitiveType = convertDrawMode(mode);
        MetalNative.drawPrimitives(encoder, primitiveType, first, count);
    }

    private MetalPipeline findOrCreatePipeline(MetalShader shader, DrawMode mode) {
        MetalResourceManager resourceManager = device.getResourceManager();
        if (resourceManager != null) {
            MetalPipelineCache cache = resourceManager.getPipelineCache();
            if (cache != null) {
                return cache.getPipeline(shader, descriptor, mode, device.getCurrentBlendMode());
            }
        }

        // Fallback: create pipeline directly (not cached)
        log.debug("Creating uncached pipeline for draw mode: {}", mode);
        MetalPipeline pipeline = new MetalPipeline(device);
        pipeline.create(shader, descriptor, mode, device.getCurrentBlendMode());
        return pipeline;
    }

    /**
     * Converts DrawMode to Metal primitive type constant.
     */
    private int convertDrawMode(DrawMode mode) {
        return switch (mode) {
            case POINTS -> 0;           // MTLPrimitiveTypePoint
            case LINES -> 1;            // MTLPrimitiveTypeLine
            case LINE_STRIP -> 2;       // MTLPrimitiveTypeLineStrip
            case TRIANGLES -> 3;        // MTLPrimitiveTypeTriangle
            case TRIANGLE_STRIP -> 4;   // MTLPrimitiveTypeTriangleStrip
            case TRIANGLE_FAN -> 3;     // Metal doesn't have fan - use triangles
            case LINE_LOOP -> 2;        // Use line strip (caller should close loop)
        };
    }

    @Override
    public int getVertexCount() {
        return currentVertexCount;
    }

    @Override
    public void setVertexCount(int count) {
        this.currentVertexCount = count;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        if (buffer != 0) {
            MetalNative.destroyBuffer(buffer);
            buffer = 0;
        }

        log.debug("Disposed Metal buffer");
    }

    @Override
    public boolean isInitialized() {
        return buffer != 0 && !disposed;
    }

    // -------------------------------------------------------------------------
    // Metal-specific methods
    // -------------------------------------------------------------------------

    /**
     * Returns the Metal buffer handle.
     */
    public long getBuffer() {
        return buffer;
    }

    /**
     * Returns the buffer descriptor.
     */
    public BufferDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Sets the current shader for this buffer.
     * Call this before draw() to ensure the correct pipeline is used.
     */
    public void setCurrentShader(MetalShader shader) {
        this.currentShader = shader;
    }

    /**
     * Returns the current shader.
     */
    public MetalShader getCurrentShader() {
        return currentShader;
    }
}
