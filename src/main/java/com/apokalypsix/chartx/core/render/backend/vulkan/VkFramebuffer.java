package com.apokalypsix.chartx.core.render.backend.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Manages Vulkan framebuffer for offscreen rendering.
 *
 * <p>This class creates and manages:
 * <ul>
 *   <li>Color attachment image and view</li>
 *   <li>Optional depth attachment</li>
 *   <li>Framebuffer object</li>
 *   <li>Staging buffer for CPU readback</li>
 * </ul>
 */
public class VkFramebuffer {

    private static final Logger log = LoggerFactory.getLogger(VkFramebuffer.class);

    private final VkRenderDevice device;

    private int width;
    private int height;

    // Color attachment
    private long colorImage = VK_NULL_HANDLE;
    private long colorImageMemory = VK_NULL_HANDLE;
    private long colorImageView = VK_NULL_HANDLE;

    // Depth attachment (optional)
    private long depthImage = VK_NULL_HANDLE;
    private long depthImageMemory = VK_NULL_HANDLE;
    private long depthImageView = VK_NULL_HANDLE;

    // Framebuffer
    private long framebuffer = VK_NULL_HANDLE;

    // Staging buffer for readback
    private long stagingBuffer = VK_NULL_HANDLE;
    private long stagingMemory = VK_NULL_HANDLE;

    private boolean disposed = false;

    public VkFramebuffer(VkRenderDevice device, int width, int height) {
        this.device = device;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates the framebuffer with the given render pass.
     */
    public void create(long renderPass, boolean withDepth) {
        if (device.getDevice() == null) return;

        try (MemoryStack stack = stackPush()) {
            createColorAttachment(stack);
            if (withDepth) {
                createDepthAttachment(stack);
            }
            createFramebuffer(renderPass, withDepth, stack);
            createStagingBuffer(stack);

            log.debug("Created framebuffer {}x{}", width, height);
        }
    }

    private void createColorAttachment(MemoryStack stack) {
        VkDevice vkDevice = device.getDevice();

        // Create image
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .format(VK_FORMAT_R8G8B8A8_UNORM)
                .mipLevels(1)
                .arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .tiling(VK_IMAGE_TILING_OPTIMAL)
                .usage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

        imageInfo.extent()
                .width(width)
                .height(height)
                .depth(1);

        LongBuffer pImage = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateImage(vkDevice, imageInfo, null, pImage);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create color image: " + result);
        }
        colorImage = pImage.get(0);

        // Allocate memory
        VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(vkDevice, colorImage, memRequirements);

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
            throw new RuntimeException("Failed to allocate color image memory: " + result);
        }
        colorImageMemory = pMemory.get(0);

        vkBindImageMemory(vkDevice, colorImage, colorImageMemory, 0);

        // Create image view
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(colorImage)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(VK_FORMAT_R8G8B8A8_UNORM);

        viewInfo.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);

        LongBuffer pImageView = stack.longs(VK_NULL_HANDLE);
        result = vkCreateImageView(vkDevice, viewInfo, null, pImageView);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create color image view: " + result);
        }
        colorImageView = pImageView.get(0);
    }

    private void createDepthAttachment(MemoryStack stack) {
        VkDevice vkDevice = device.getDevice();
        int depthFormat = VK_FORMAT_D32_SFLOAT;

        // Create image
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .format(depthFormat)
                .mipLevels(1)
                .arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .tiling(VK_IMAGE_TILING_OPTIMAL)
                .usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

        imageInfo.extent()
                .width(width)
                .height(height)
                .depth(1);

        LongBuffer pImage = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateImage(vkDevice, imageInfo, null, pImage);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create depth image: " + result);
        }
        depthImage = pImage.get(0);

        // Allocate memory
        VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(vkDevice, depthImage, memRequirements);

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
            throw new RuntimeException("Failed to allocate depth image memory: " + result);
        }
        depthImageMemory = pMemory.get(0);

        vkBindImageMemory(vkDevice, depthImage, depthImageMemory, 0);

        // Create image view
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(depthImage)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(depthFormat);

        viewInfo.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);

        LongBuffer pImageView = stack.longs(VK_NULL_HANDLE);
        result = vkCreateImageView(vkDevice, viewInfo, null, pImageView);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create depth image view: " + result);
        }
        depthImageView = pImageView.get(0);
    }

    private void createFramebuffer(long renderPass, boolean withDepth, MemoryStack stack) {
        VkDevice vkDevice = device.getDevice();

        LongBuffer attachments = withDepth
                ? stack.longs(colorImageView, depthImageView)
                : stack.longs(colorImageView);

        VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(renderPass)
                .pAttachments(attachments)
                .width(width)
                .height(height)
                .layers(1);

        LongBuffer pFramebuffer = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateFramebuffer(vkDevice, framebufferInfo, null, pFramebuffer);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create framebuffer: " + result);
        }
        framebuffer = pFramebuffer.get(0);
    }

    private void createStagingBuffer(MemoryStack stack) {
        VkDevice vkDevice = device.getDevice();
        long bufferSize = (long) width * height * 4; // RGBA

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(bufferSize)
                .usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT)
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

    /**
     * Resizes the framebuffer.
     */
    public void resize(long renderPass, int newWidth, int newHeight, boolean withDepth) {
        if (newWidth == width && newHeight == height) {
            return;
        }

        dispose();
        this.width = newWidth;
        this.height = newHeight;
        this.disposed = false;
        create(renderPass, withDepth);
    }

    /**
     * Transitions the color image layout for rendering.
     */
    public void transitionForRendering(VkCommandBuffer cmd) {
        transitionImageLayout(cmd, colorImage,
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_IMAGE_ASPECT_COLOR_BIT);
    }

    /**
     * Transitions the color image layout for transfer (readback).
     */
    public void transitionForTransfer(VkCommandBuffer cmd) {
        transitionImageLayout(cmd, colorImage,
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                VK_IMAGE_ASPECT_COLOR_BIT);
    }

    private void transitionImageLayout(VkCommandBuffer cmd, long image,
                                        int oldLayout, int newLayout, int aspectMask) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image);

            barrier.subresourceRange()
                    .aspectMask(aspectMask)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);

            int srcStage;
            int dstStage;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED &&
                newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL &&
                       newLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else {
                srcStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
                dstStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
            }

            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0,
                    null, null, barrier);
        }
    }

    /**
     * Copies the framebuffer content to the staging buffer for CPU readback.
     */
    public void copyToStagingBuffer(VkCommandBuffer cmd) {
        try (MemoryStack stack = stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);
            region.bufferImageHeight(0);

            region.imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);

            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(width, height, 1);

            vkCmdCopyImageToBuffer(cmd, colorImage,
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    stagingBuffer, region);
        }
    }

    /**
     * Reads pixels from the staging buffer into the provided array.
     * Must be called after the command buffer has been submitted and completed.
     */
    public void readPixels(int[] pixels) {
        if (stagingMemory == VK_NULL_HANDLE) return;

        VkDevice vkDevice = device.getDevice();
        long bufferSize = (long) width * height * 4;

        try (MemoryStack stack = stackPush()) {
            var pMapped = stack.mallocPointer(1);
            vkMapMemory(vkDevice, stagingMemory, 0, bufferSize, 0, pMapped);
            var mapped = pMapped.getByteBuffer(0, (int) bufferSize);

            for (int i = 0; i < width * height; i++) {
                int r = mapped.get() & 0xFF;
                int g = mapped.get() & 0xFF;
                int b = mapped.get() & 0xFF;
                int a = mapped.get() & 0xFF;
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }

            vkUnmapMemory(vkDevice, stagingMemory);
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        if (framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(vkDevice, framebuffer, null);
            framebuffer = VK_NULL_HANDLE;
        }

        if (colorImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(vkDevice, colorImageView, null);
            colorImageView = VK_NULL_HANDLE;
        }

        if (colorImage != VK_NULL_HANDLE) {
            vkDestroyImage(vkDevice, colorImage, null);
            colorImage = VK_NULL_HANDLE;
        }

        if (colorImageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, colorImageMemory, null);
            colorImageMemory = VK_NULL_HANDLE;
        }

        if (depthImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(vkDevice, depthImageView, null);
            depthImageView = VK_NULL_HANDLE;
        }

        if (depthImage != VK_NULL_HANDLE) {
            vkDestroyImage(vkDevice, depthImage, null);
            depthImage = VK_NULL_HANDLE;
        }

        if (depthImageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, depthImageMemory, null);
            depthImageMemory = VK_NULL_HANDLE;
        }

        if (stagingBuffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(vkDevice, stagingBuffer, null);
            stagingBuffer = VK_NULL_HANDLE;
        }

        if (stagingMemory != VK_NULL_HANDLE) {
            vkFreeMemory(vkDevice, stagingMemory, null);
            stagingMemory = VK_NULL_HANDLE;
        }

        log.debug("Disposed framebuffer");
    }

    // Getters

    public long getFramebuffer() {
        return framebuffer;
    }

    public long getColorImage() {
        return colorImage;
    }

    public long getColorImageView() {
        return colorImageView;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
