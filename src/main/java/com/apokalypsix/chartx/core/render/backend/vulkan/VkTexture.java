package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.Texture;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan texture implementation.
 *
 * <p>Manages Vulkan images, image views, and samplers for texture data.
 */
public class VkTexture implements Texture {

    private static final Logger log = LoggerFactory.getLogger(VkTexture.class);

    private final VkRenderDevice device;
    private final TextureDescriptor descriptor;

    private long image = VK_NULL_HANDLE;
    private long imageMemory = VK_NULL_HANDLE;
    private long imageView = VK_NULL_HANDLE;
    private long sampler = VK_NULL_HANDLE;

    // Staging buffer for uploads
    private long stagingBuffer = VK_NULL_HANDLE;
    private long stagingMemory = VK_NULL_HANDLE;

    private int width;
    private int height;
    private boolean disposed = false;

    public VkTexture(VkRenderDevice device, TextureDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.width = descriptor.getWidth();
        this.height = descriptor.getHeight();

        if (device.isInitialized()) {
            createTexture();
            createSampler();
        }
    }

    private void createTexture() {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        try (MemoryStack stack = stackPush()) {
            // Determine format based on descriptor
            int format = switch (descriptor.getFormat()) {
                case RGBA8 -> VK_FORMAT_R8G8B8A8_UNORM;
                case RGB8 -> VK_FORMAT_R8G8B8_UNORM;
                case R8 -> VK_FORMAT_R8_UNORM;
                default -> VK_FORMAT_R8G8B8A8_UNORM;
            };

            // Create image
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            imageInfo.extent()
                    .width(width)
                    .height(height)
                    .depth(1);

            LongBuffer pImage = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateImage(vkDevice, imageInfo, null, pImage);
            if (result != VK_SUCCESS) {
                log.error("Failed to create image: {}", result);
                return;
            }
            image = pImage.get(0);

            // Allocate memory
            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(vkDevice, image, memRequirements);

            int memoryTypeIndex = findMemoryType(
                    memRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    stack
            );

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(memoryTypeIndex);

            LongBuffer pMemory = stack.longs(VK_NULL_HANDLE);
            result = vkAllocateMemory(vkDevice, allocInfo, null, pMemory);
            if (result != VK_SUCCESS) {
                log.error("Failed to allocate image memory: {}", result);
                vkDestroyImage(vkDevice, image, null);
                image = VK_NULL_HANDLE;
                return;
            }
            imageMemory = pMemory.get(0);

            vkBindImageMemory(vkDevice, image, imageMemory, 0);

            // Create image view
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format);

            viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);

            LongBuffer pImageView = stack.longs(VK_NULL_HANDLE);
            result = vkCreateImageView(vkDevice, viewInfo, null, pImageView);
            if (result != VK_SUCCESS) {
                log.error("Failed to create image view: {}", result);
                return;
            }
            imageView = pImageView.get(0);

            log.debug("Created Vulkan texture {}x{}", width, height);
        }
    }

    private void createSampler() {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        try (MemoryStack stack = stackPush()) {
            // Check filter mode from descriptor
            boolean isLinear = descriptor.getMinFilter() == TextureDescriptor.TextureFilter.LINEAR ||
                               descriptor.getMagFilter() == TextureDescriptor.TextureFilter.LINEAR;

            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(isLinear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST)
                    .minFilter(isLinear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .anisotropyEnable(false)
                    .maxAnisotropy(1.0f)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .mipLodBias(0.0f)
                    .minLod(0.0f)
                    .maxLod(0.0f);

            LongBuffer pSampler = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateSampler(vkDevice, samplerInfo, null, pSampler);
            if (result != VK_SUCCESS) {
                log.error("Failed to create sampler: {}", result);
                return;
            }
            sampler = pSampler.get(0);
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

        return 0;
    }

    @Override
    public void upload(BufferedImage image) {
        if (disposed || this.image == VK_NULL_HANDLE) return;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        // Resize texture if needed
        if (image.getWidth() != width || image.getHeight() != height) {
            disposeTextureResources();
            disposed = false;
            width = image.getWidth();
            height = image.getHeight();
            createTexture();
            createSampler();
        }

        try (MemoryStack stack = stackPush()) {
            int pixelCount = width * height;
            int bufferSize = pixelCount * 4; // RGBA

            // Create staging buffer
            createStagingBuffer(bufferSize, stack);

            // Map and fill staging buffer
            PointerBuffer pMapped = stack.mallocPointer(1);
            vkMapMemory(vkDevice, stagingMemory, 0, bufferSize, 0, pMapped);
            ByteBuffer mapped = pMapped.getByteBuffer(0, bufferSize);

            // Copy image data to staging buffer
            int[] pixels = new int[pixelCount];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            for (int pixel : pixels) {
                mapped.put((byte) ((pixel >> 16) & 0xFF)); // R
                mapped.put((byte) ((pixel >> 8) & 0xFF));  // G
                mapped.put((byte) (pixel & 0xFF));         // B
                mapped.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
            mapped.flip();

            vkUnmapMemory(vkDevice, stagingMemory);

            // Copy staging buffer to image using command buffer
            copyBufferToImage(bufferSize);

            // Clean up staging buffer
            vkDestroyBuffer(vkDevice, stagingBuffer, null);
            vkFreeMemory(vkDevice, stagingMemory, null);
            stagingBuffer = VK_NULL_HANDLE;
            stagingMemory = VK_NULL_HANDLE;

            log.debug("Uploaded texture data {}x{}", width, height);
        }
    }

    /**
     * Copies data from staging buffer to the image using a command buffer.
     */
    private void copyBufferToImage(int bufferSize) {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        try (MemoryStack stack = stackPush()) {
            // Allocate a temporary command buffer
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(device.getCommandPool())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(vkDevice, allocInfo, pCommandBuffer);
            VkCommandBuffer cmd = new VkCommandBuffer(pCommandBuffer.get(0), vkDevice);

            // Begin command buffer
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(cmd, beginInfo);

            // Transition image to transfer destination layout
            transitionImageLayout(cmd, stack, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            // Copy buffer to image
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.get(0)
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0);
            region.get(0).imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.get(0).imageOffset().set(0, 0, 0);
            region.get(0).imageExtent().set(width, height, 1);

            vkCmdCopyBufferToImage(cmd, stagingBuffer, image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            // Transition image to shader read layout
            transitionImageLayout(cmd, stack, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            vkEndCommandBuffer(cmd);

            // Submit command buffer
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(cmd));

            vkQueueSubmit(device.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(device.getGraphicsQueue());

            // Free command buffer
            vkFreeCommandBuffers(vkDevice, device.getCommandPool(), cmd);
        }
    }

    /**
     * Transitions image layout using a pipeline barrier.
     */
    private void transitionImageLayout(VkCommandBuffer cmd, MemoryStack stack,
                                        int oldLayout, int newLayout) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image);

        barrier.get(0).subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);

        int srcStage, dstStage;
        if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED &&
            newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
            barrier.get(0)
                    .srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
                   newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
            barrier.get(0)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        } else {
            srcStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
            dstStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
        }

        vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0,
                null, null, barrier);
    }

    /**
     * Disposes texture resources without marking as disposed.
     */
    private void disposeTextureResources() {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        if (sampler != VK_NULL_HANDLE) {
            vkDestroySampler(vkDevice, sampler, null);
            sampler = VK_NULL_HANDLE;
        }

        if (imageView != VK_NULL_HANDLE) {
            vkDestroyImageView(vkDevice, imageView, null);
            imageView = VK_NULL_HANDLE;
        }

        if (image != VK_NULL_HANDLE) {
            vkDestroyImage(vkDevice, image, null);
            image = VK_NULL_HANDLE;
        }

        if (imageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, imageMemory, null);
            imageMemory = VK_NULL_HANDLE;
        }
    }

    private void createStagingBuffer(int size, MemoryStack stack) {
        VkDevice vkDevice = device.getDevice();

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer pBuffer = stack.longs(VK_NULL_HANDLE);
        vkCreateBuffer(vkDevice, bufferInfo, null, pBuffer);
        stagingBuffer = pBuffer.get(0);

        VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
        vkGetBufferMemoryRequirements(vkDevice, stagingBuffer, memRequirements);

        int memoryTypeIndex = findMemoryType(
                memRequirements.memoryTypeBits(),
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                stack
        );

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(memoryTypeIndex);

        LongBuffer pMemory = stack.longs(VK_NULL_HANDLE);
        vkAllocateMemory(vkDevice, allocInfo, null, pMemory);
        stagingMemory = pMemory.get(0);

        vkBindBufferMemory(vkDevice, stagingBuffer, stagingMemory, 0);
    }

    @Override
    public void upload(ByteBuffer data, int width, int height, TextureDescriptor.TextureFormat format) {
        if (disposed || this.image == VK_NULL_HANDLE) return;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        // Resize texture if needed
        if (width != this.width || height != this.height) {
            disposeTextureResources();
            disposed = false;
            this.width = width;
            this.height = height;
            createTexture();
            createSampler();
        }

        try (MemoryStack stack = stackPush()) {
            int bufferSize = data.remaining();

            // Create staging buffer
            createStagingBuffer(bufferSize, stack);

            // Map and fill staging buffer
            PointerBuffer pMapped = stack.mallocPointer(1);
            vkMapMemory(vkDevice, stagingMemory, 0, bufferSize, 0, pMapped);
            ByteBuffer mapped = pMapped.getByteBuffer(0, bufferSize);

            // Copy data to staging buffer
            int originalPos = data.position();
            mapped.put(data);
            data.position(originalPos); // Restore position
            mapped.flip();

            vkUnmapMemory(vkDevice, stagingMemory);

            // Copy staging buffer to image using command buffer
            copyBufferToImage(bufferSize);

            // Clean up staging buffer
            vkDestroyBuffer(vkDevice, stagingBuffer, null);
            vkFreeMemory(vkDevice, stagingMemory, null);
            stagingBuffer = VK_NULL_HANDLE;
            stagingMemory = VK_NULL_HANDLE;

            log.debug("Uploaded texture data {}x{} (format: {})", width, height, format);
        }
    }

    /**
     * Uploads grayscale (R8) data directly from a byte array.
     * Used for font atlas uploads.
     *
     * @param data grayscale pixel data
     * @param width image width
     * @param height image height
     */
    public void uploadGrayscale(byte[] data, int width, int height) {
        if (disposed || this.image == VK_NULL_HANDLE) return;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        // Resize texture if needed
        if (width != this.width || height != this.height) {
            disposeTextureResources();
            disposed = false;
            this.width = width;
            this.height = height;
            createTexture();
            createSampler();
        }

        try (MemoryStack stack = stackPush()) {
            int bufferSize = data.length;

            // Create staging buffer
            createStagingBuffer(bufferSize, stack);

            // Map and fill staging buffer
            PointerBuffer pMapped = stack.mallocPointer(1);
            vkMapMemory(vkDevice, stagingMemory, 0, bufferSize, 0, pMapped);
            ByteBuffer mapped = pMapped.getByteBuffer(0, bufferSize);

            // Copy data to staging buffer
            mapped.put(data);
            mapped.flip();

            vkUnmapMemory(vkDevice, stagingMemory);

            // Copy staging buffer to image using command buffer
            copyBufferToImage(bufferSize);

            // Clean up staging buffer
            vkDestroyBuffer(vkDevice, stagingBuffer, null);
            vkFreeMemory(vkDevice, stagingMemory, null);
            stagingBuffer = VK_NULL_HANDLE;
            stagingMemory = VK_NULL_HANDLE;

            log.debug("Uploaded grayscale texture data {}x{}", width, height);
        }
    }

    @Override
    public void bind(int unit) {
        // In Vulkan, textures are bound via descriptor sets
        // This is handled at the pipeline/shader level
    }

    @Override
    public void unbind() {
        // In Vulkan, texture unbinding is handled via descriptor sets
        // No explicit unbind needed
    }

    @Override
    public boolean isInitialized() {
        return image != VK_NULL_HANDLE && !disposed;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        if (sampler != VK_NULL_HANDLE) {
            vkDestroySampler(vkDevice, sampler, null);
            sampler = VK_NULL_HANDLE;
        }

        if (imageView != VK_NULL_HANDLE) {
            vkDestroyImageView(vkDevice, imageView, null);
            imageView = VK_NULL_HANDLE;
        }

        if (image != VK_NULL_HANDLE) {
            vkDestroyImage(vkDevice, image, null);
            image = VK_NULL_HANDLE;
        }

        if (imageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, imageMemory, null);
            imageMemory = VK_NULL_HANDLE;
        }

        log.debug("Disposed Vulkan texture");
    }

    // -------------------------------------------------------------------------
    // Vulkan-specific getters
    // -------------------------------------------------------------------------

    public long getImage() {
        return image;
    }

    public long getImageView() {
        return imageView;
    }

    public long getSampler() {
        return sampler;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
