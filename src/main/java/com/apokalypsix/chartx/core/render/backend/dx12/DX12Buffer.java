package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.BlendMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

/**
 * DirectX 12 implementation of Buffer.
 *
 * <p>Uses upload heap buffers for CPU-visible vertex data.
 */
public class DX12Buffer implements Buffer {

    private static final Logger log = LoggerFactory.getLogger(DX12Buffer.class);

    private final DX12RenderDevice device;
    private final BufferDescriptor descriptor;

    private long bufferHandle;
    private int capacity;
    private int vertexCount;
    private boolean initialized;

    public DX12Buffer(DX12RenderDevice device, BufferDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.capacity = descriptor.getInitialCapacity();

        createBuffer();
    }

    private void createBuffer() {
        int sizeInBytes = capacity * Float.BYTES;
        bufferHandle = DX12Native.createUploadBuffer(device.getDevice(), sizeInBytes);

        if (bufferHandle == 0) {
            throw new RuntimeException("Failed to create DX12 buffer");
        }

        initialized = true;
        log.debug("Created DX12 buffer with capacity {} floats", capacity);
    }

    @Override
    public void upload(float[] data, int offset, int count) {
        // Resize if needed
        if (offset + count > capacity) {
            resize(Math.max(capacity * 2, offset + count));
        }

        DX12Native.uploadBufferData(bufferHandle, data, offset, count);
    }

    @Override
    public void upload(FloatBuffer data, int offset) {
        int count = data.remaining();

        // Resize if needed
        if (offset + count > capacity) {
            resize(Math.max(capacity * 2, offset + count));
        }

        // Convert FloatBuffer to float[]
        float[] array = new float[count];
        int pos = data.position();
        data.get(array);
        data.position(pos);

        DX12Native.uploadBufferData(bufferHandle, array, offset, count);
    }

    private void resize(int newCapacity) {
        if (bufferHandle != 0) {
            DX12Native.destroyBuffer(bufferHandle);
        }
        capacity = newCapacity;
        createBuffer();
        log.debug("Resized DX12 buffer to {} floats", capacity);
    }

    @Override
    public void bind() {
        long commandList = device.getCommandList();
        if (commandList != 0 && bufferHandle != 0) {
            int stride = descriptor.getFloatsPerVertex() * Float.BYTES;
            int size = vertexCount * stride;
            DX12Native.setVertexBuffer(commandList, bufferHandle, stride, size);
        }
    }

    @Override
    public void unbind() {
        // No-op for DX12
    }

    @Override
    public void draw(DrawMode mode) {
        draw(mode, 0, vertexCount);
    }

    @Override
    public void draw(DrawMode mode, int first, int count) {
        long commandList = device.getCommandList();
        if (commandList == 0 || count == 0) {
            return;
        }

        // Get or create pipeline for this draw configuration
        DX12ResourceManager resources = device.getResourceManager();
        if (resources == null) {
            return;
        }

        DX12Shader shader = resources.getCurrentShader();
        if (shader == null) {
            shader = resources.getShader(DX12ResourceManager.SHADER_DEFAULT, true);
        }

        BlendMode blendMode = device.getCurrentBlendMode();
        DX12PipelineCache pipelineCache = resources.getPipelineCache();
        DX12Pipeline pipeline = pipelineCache.getOrCreate(device, shader, descriptor, mode, blendMode);

        if (pipeline != null) {
            pipeline.bind(commandList);
        }

        // Flush shader uniforms
        if (shader != null) {
            shader.flushUniforms(commandList);
        }

        // Set primitive topology
        int topology = toD3DTopology(mode);
        DX12Native.setPrimitiveTopology(commandList, topology);

        // Draw
        DX12Native.drawInstanced(commandList, count, first);
    }

    private int toD3DTopology(DrawMode mode) {
        return switch (mode) {
            case POINTS -> 1;           // D3D_PRIMITIVE_TOPOLOGY_POINTLIST
            case LINES -> 2;            // D3D_PRIMITIVE_TOPOLOGY_LINELIST
            case LINE_STRIP -> 3;       // D3D_PRIMITIVE_TOPOLOGY_LINESTRIP
            case LINE_LOOP -> 2;        // No direct equivalent, use LINELIST
            case TRIANGLES -> 4;        // D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST
            case TRIANGLE_STRIP -> 5;   // D3D_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP
            case TRIANGLE_FAN -> 4;     // No direct equivalent, use TRIANGLELIST
        };
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public void setVertexCount(int count) {
        this.vertexCount = count;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void dispose() {
        if (bufferHandle != 0) {
            DX12Native.destroyBuffer(bufferHandle);
            bufferHandle = 0;
            initialized = false;
            log.debug("Disposed DX12 buffer");
        }
    }

    long getHandle() {
        return bufferHandle;
    }

    BufferDescriptor getDescriptor() {
        return descriptor;
    }
}
