package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.VertexAttribute;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan buffer implementation for vertex and index data.
 *
 * <p>Uses host-visible memory for dynamic buffers (frequently updated data)
 * and device-local memory with staging buffers for static data.
 */
public class VkBuffer implements Buffer {

    private static final Logger log = LoggerFactory.getLogger(VkBuffer.class);

    private final VkRenderDevice device;
    private final BufferDescriptor descriptor;

    private long buffer = VK_NULL_HANDLE;
    private long memory = VK_NULL_HANDLE;
    private ByteBuffer mappedMemory;

    private int capacity;
    private int currentVertexCount;
    private boolean disposed = false;

    // Current shader bound to this buffer (for pipeline lookup)
    private VkShader currentShader;

    public VkBuffer(VkRenderDevice device, BufferDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.capacity = descriptor.getInitialCapacity();

        if (device.isInitialized()) {
            createBuffer();
        }
    }

    private void createBuffer() {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        try (MemoryStack stack = stackPush()) {
            int bufferSize = capacity * Float.BYTES;

            // Create buffer
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateBuffer(vkDevice, bufferInfo, null, pBuffer);
            if (result != VK_SUCCESS) {
                log.error("Failed to create buffer: {}", result);
                return;
            }
            buffer = pBuffer.get(0);

            // Get memory requirements
            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(vkDevice, buffer, memRequirements);

            // Find suitable memory type (host visible for dynamic buffers)
            int memoryTypeIndex = findMemoryType(
                    memRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    stack
            );

            if (memoryTypeIndex < 0) {
                log.error("Failed to find suitable memory type");
                vkDestroyBuffer(vkDevice, buffer, null);
                buffer = VK_NULL_HANDLE;
                return;
            }

            // Allocate memory
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(memoryTypeIndex);

            LongBuffer pMemory = stack.longs(VK_NULL_HANDLE);
            result = vkAllocateMemory(vkDevice, allocInfo, null, pMemory);
            if (result != VK_SUCCESS) {
                log.error("Failed to allocate buffer memory: {}", result);
                vkDestroyBuffer(vkDevice, buffer, null);
                buffer = VK_NULL_HANDLE;
                return;
            }
            memory = pMemory.get(0);

            // Bind memory to buffer
            vkBindBufferMemory(vkDevice, buffer, memory, 0);

            // Map memory for writing
            PointerBuffer pMapped = stack.mallocPointer(1);
            vkMapMemory(vkDevice, memory, 0, bufferSize, 0, pMapped);
            mappedMemory = pMapped.getByteBuffer(0, bufferSize);

            log.debug("Created Vulkan buffer ({} bytes)", bufferSize);
        }
    }

    private int findMemoryType(int typeFilter, int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void upload(float[] data, int offset, int count) {
        if (disposed || mappedMemory == null) return;

        // Resize if needed
        if (count > capacity) {
            resize(count + count / 2);
        }

        // Copy data to mapped memory
        mappedMemory.clear();
        for (int i = 0; i < count; i++) {
            mappedMemory.putFloat(data[offset + i]);
        }
        mappedMemory.flip();

        currentVertexCount = count / descriptor.getFloatsPerVertex();
    }

    private void resize(int newCapacity) {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        log.debug("Resizing buffer from {} to {} floats", capacity, newCapacity);

        // Clean up old resources
        if (mappedMemory != null) {
            vkUnmapMemory(vkDevice, memory);
            mappedMemory = null;
        }
        if (buffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(vkDevice, buffer, null);
        }
        if (memory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, memory, null);
        }

        // Create new buffer with larger capacity
        capacity = newCapacity;
        buffer = VK_NULL_HANDLE;
        memory = VK_NULL_HANDLE;
        createBuffer();
    }

    @Override
    public void bind() {
        if (disposed || buffer == VK_NULL_HANDLE) return;

        VkCommandBuffer cmd = device.getCommandBuffer();
        if (cmd == null) return;

        try (MemoryStack stack = stackPush()) {
            LongBuffer buffers = stack.longs(buffer);
            LongBuffer offsets = stack.longs(0);
            vkCmdBindVertexBuffers(cmd, 0, buffers, offsets);
        }
    }

    @Override
    public void draw(DrawMode mode) {
        draw(mode, 0, currentVertexCount);
    }

    @Override
    public void draw(DrawMode mode, int first, int count) {
        if (disposed || count <= 0) return;

        VkCommandBuffer cmd = device.getCommandBuffer();
        if (cmd == null) return;

        // Get shader from device (set by shader.bind())
        VkShader shader = currentShader;
        if (shader == null) {
            shader = device.getCurrentShader();
        }
        if (shader == null || !shader.isValid()) {
            log.warn("No valid shader bound for draw call");
            return;
        }

        // Get or create pipeline for this draw mode from the cache
        VkPipeline pipeline = findOrCreatePipeline(shader, mode);
        if (pipeline == null) {
            log.warn("Could not get pipeline for draw mode: {}", mode);
            return;
        }

        // Bind pipeline
        pipeline.bind(cmd);

        // Flush push constants
        shader.flushPushConstants();

        // Issue draw call
        vkCmdDraw(cmd, count, 1, first, 0);
    }

    /**
     * Finds or creates a pipeline for the given shader and draw mode.
     */
    private VkPipeline findOrCreatePipeline(VkShader shader, DrawMode mode) {
        VkResourceManager resourceManager = device.getResourceManager();
        if (resourceManager != null) {
            VkPipelineCache cache = resourceManager.getPipelineCache();
            if (cache != null) {
                return cache.getPipeline(shader, descriptor, mode, device.getCurrentBlendMode());
            }
        }

        // Fallback: create pipeline directly (not cached)
        log.debug("Creating uncached pipeline for draw mode: {}", mode);
        VkPipeline pipeline = new VkPipeline(device);
        pipeline.create(shader, descriptor, device.getRenderPass(), mode, device.getCurrentBlendMode());
        return pipeline;
    }

    /**
     * Sets the current shader for this buffer.
     * Call this before draw() to ensure the correct pipeline is used.
     */
    public void setCurrentShader(VkShader shader) {
        this.currentShader = shader;
    }

    /**
     * Returns the current shader.
     */
    public VkShader getCurrentShader() {
        return currentShader;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        if (mappedMemory != null) {
            vkUnmapMemory(vkDevice, memory);
            mappedMemory = null;
        }

        if (buffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(vkDevice, buffer, null);
            buffer = VK_NULL_HANDLE;
        }

        if (memory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, memory, null);
            memory = VK_NULL_HANDLE;
        }

        log.debug("Disposed Vulkan buffer");
    }

    // -------------------------------------------------------------------------
    // Vulkan-specific getters
    // -------------------------------------------------------------------------

    public long getBuffer() {
        return buffer;
    }

    public BufferDescriptor getDescriptor() {
        return descriptor;
    }

    public int getCurrentVertexCount() {
        return currentVertexCount;
    }

    /**
     * Creates a VkVertexInputBindingDescription for this buffer.
     */
    public VkVertexInputBindingDescription.Buffer createBindingDescription(MemoryStack stack) {
        return VkVertexInputBindingDescription.calloc(1, stack)
                .binding(0)
                .stride(descriptor.getFloatsPerVertex() * Float.BYTES)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
    }

    /**
     * Creates VkVertexInputAttributeDescriptions for this buffer.
     */
    public VkVertexInputAttributeDescription.Buffer createAttributeDescriptions(MemoryStack stack) {
        List<VertexAttribute> attributes = descriptor.getAttributes();
        VkVertexInputAttributeDescription.Buffer descriptions =
                VkVertexInputAttributeDescription.calloc(attributes.size(), stack);

        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute attr = attributes.get(i);

            int format = switch (attr.getComponents()) {
                case 1 -> VK_FORMAT_R32_SFLOAT;
                case 2 -> VK_FORMAT_R32G32_SFLOAT;
                case 3 -> VK_FORMAT_R32G32B32_SFLOAT;
                case 4 -> VK_FORMAT_R32G32B32A32_SFLOAT;
                default -> VK_FORMAT_R32G32B32A32_SFLOAT;
            };

            descriptions.get(i)
                    .binding(0)
                    .location(i)
                    .format(format)
                    .offset(attr.getOffset());
        }

        return descriptions;
    }

    // -------------------------------------------------------------------------
    // Additional Buffer interface methods
    // -------------------------------------------------------------------------

    @Override
    public void upload(FloatBuffer data, int offset) {
        if (disposed || mappedMemory == null) return;

        int count = data.remaining();

        // Resize if needed
        if (count > capacity) {
            resize(count + count / 2);
        }

        // Copy data to mapped memory
        mappedMemory.clear();
        mappedMemory.position(offset * Float.BYTES);
        for (int i = 0; i < count; i++) {
            mappedMemory.putFloat(data.get());
        }
        mappedMemory.flip();

        currentVertexCount = count / descriptor.getFloatsPerVertex();
    }

    @Override
    public void unbind() {
        // In Vulkan, buffer unbinding is handled differently
        // No explicit unbind needed for vertex buffers
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
    public boolean isInitialized() {
        return buffer != VK_NULL_HANDLE && !disposed;
    }
}
