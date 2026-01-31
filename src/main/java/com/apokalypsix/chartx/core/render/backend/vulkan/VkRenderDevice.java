package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan implementation of the RenderDevice interface.
 *
 * <p>Provides Vulkan-based rendering for ChartX. This implementation uses
 * LWJGL for Vulkan bindings and supports offscreen rendering for integration
 * with Swing applications.
 *
 * <p>Key features:
 * <ul>
 *   <li>Vulkan 1.1+ device management</li>
 *   <li>VMA (Vulkan Memory Allocator) for efficient memory management</li>
 *   <li>Offscreen rendering to framebuffers</li>
 *   <li>SPIR-V shader support</li>
 * </ul>
 */
public class VkRenderDevice implements RenderDevice {

    private static final Logger log = LoggerFactory.getLogger(VkRenderDevice.class);

    // Enable validation layers in debug mode
    private static final boolean ENABLE_VALIDATION = true;
    private static final Set<String> VALIDATION_LAYERS = Set.of(
            "VK_LAYER_KHRONOS_validation"
    );

    // Vulkan objects
    private VkInstance instance;
    private long debugMessenger = VK_NULL_HANDLE;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private int graphicsQueueFamily = -1;

    // Command pool and buffers
    private long commandPool;
    private VkCommandBuffer commandBuffer;

    // Synchronization
    private long renderFence;
    private long renderSemaphore;

    // Render pass and framebuffer
    private long renderPass = VK_NULL_HANDLE;
    private VkFramebuffer framebuffer;
    private float clearR = 0.1f, clearG = 0.1f, clearB = 0.12f, clearA = 1.0f;

    // Device info
    private String deviceName = "Unknown";
    private int maxTextureSize = 4096;
    private float maxLineWidth = 1.0f;

    private boolean initialized = false;
    private boolean inFrame = false;
    private int frameWidth = 800;
    private int frameHeight = 600;

    // Current render state
    private BlendMode currentBlendMode = BlendMode.ALPHA;
    private float currentLineWidth = 1.0f;

    // Currently bound shader (for pipeline lookup)
    private VkShader currentShader;
    private VkResourceManager resourceManager;

    public VkRenderDevice() {
        // Default constructor
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        log.info("Initializing Vulkan render device");

        try (MemoryStack stack = stackPush()) {
            // Create Vulkan instance
            createInstance(stack);

            // Setup debug messenger if validation is enabled
            if (ENABLE_VALIDATION && instance != null) {
                setupDebugMessenger(stack);
            }

            // Select physical device
            selectPhysicalDevice(stack);

            // Create logical device
            createLogicalDevice(stack);

            // Create command pool
            createCommandPool(stack);

            // Create synchronization objects
            createSyncObjects(stack);

            // Create render pass
            createRenderPass(stack);

            // Create default framebuffer
            framebuffer = new VkFramebuffer(this, frameWidth, frameHeight);
            framebuffer.create(renderPass, false);

            initialized = true;
            log.info("Vulkan render device initialized: {}", deviceName);

        } catch (UnsatisfiedLinkError e) {
            log.error("Vulkan native library not available: {}", e.getMessage());
            cleanup();
            throw new UnsupportedOperationException(
                    "Vulkan native library not found. Install Vulkan SDK/runtime.", e);
        } catch (Exception e) {
            log.error("Failed to initialize Vulkan", e);
            cleanup();
            throw new RuntimeException("Vulkan initialization failed", e);
        }
    }

    private void createInstance(MemoryStack stack) {
        // Check for validation layer support
        boolean validationAvailable = ENABLE_VALIDATION && checkValidationLayerSupport(stack);
        if (ENABLE_VALIDATION && !validationAvailable) {
            log.warn("Validation layers requested but not available - continuing without validation");
        }

        // Application info
        VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("ChartX"))
                .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
                .pEngineName(stack.UTF8("ChartX Engine"))
                .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                .apiVersion(VK_MAKE_API_VERSION(0, 1, 1, 0));

        // Required extensions
        PointerBuffer extensions = getRequiredExtensions(stack);

        // Instance create info
        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(extensions);

        // On macOS with MoltenVK, we need to enable portability enumeration
        // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR = 0x00000001
        if (hasPortabilityEnumeration(stack)) {
            createInfo.flags(0x00000001); // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
        }

        // Enable validation layers only if available
        if (validationAvailable) {
            PointerBuffer layers = stack.mallocPointer(VALIDATION_LAYERS.size());
            for (String layer : VALIDATION_LAYERS) {
                layers.put(stack.UTF8(layer));
            }
            layers.flip();
            createInfo.ppEnabledLayerNames(layers);

            // Enable debug messenger for instance creation
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = createDebugMessengerCreateInfo(stack);
            createInfo.pNext(debugCreateInfo.address());
        }

        // Create instance
        PointerBuffer pInstance = stack.mallocPointer(1);
        int result = vkCreateInstance(createInfo, null, pInstance);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create Vulkan instance: " + result);
        }

        instance = new VkInstance(pInstance.get(0), createInfo);
        log.debug("Vulkan instance created");
    }

    private boolean checkValidationLayerSupport(MemoryStack stack) {
        IntBuffer layerCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(layerCount, null);

        VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

        Set<String> availableLayerNames = new HashSet<>();
        for (VkLayerProperties layer : availableLayers) {
            availableLayerNames.add(layer.layerNameString());
        }

        return availableLayerNames.containsAll(VALIDATION_LAYERS);
    }

    private PointerBuffer getRequiredExtensions(MemoryStack stack) {
        // Query available extensions
        IntBuffer extensionCount = stack.ints(0);
        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);

        VkExtensionProperties.Buffer availableExtensions =
                VkExtensionProperties.malloc(extensionCount.get(0), stack);
        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, availableExtensions);

        Set<String> availableExtensionNames = new HashSet<>();
        for (VkExtensionProperties ext : availableExtensions) {
            availableExtensionNames.add(ext.extensionNameString());
        }

        // Build list of extensions to enable
        java.util.List<String> extensionsToEnable = new java.util.ArrayList<>();

        // Debug utils for validation (if available)
        if (ENABLE_VALIDATION && availableExtensionNames.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
            extensionsToEnable.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        }

        // Portability enumeration (for macOS MoltenVK, if available)
        if (availableExtensionNames.contains("VK_KHR_portability_enumeration")) {
            extensionsToEnable.add("VK_KHR_portability_enumeration");
        }

        log.debug("Enabling Vulkan extensions: {}", extensionsToEnable);

        PointerBuffer extensions = stack.mallocPointer(extensionsToEnable.size());
        for (String ext : extensionsToEnable) {
            extensions.put(stack.UTF8(ext));
        }
        extensions.flip();
        return extensions;
    }

    private boolean hasPortabilityEnumeration(MemoryStack stack) {
        IntBuffer extensionCount = stack.ints(0);
        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);

        VkExtensionProperties.Buffer availableExtensions =
                VkExtensionProperties.malloc(extensionCount.get(0), stack);
        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, availableExtensions);

        for (VkExtensionProperties ext : availableExtensions) {
            if ("VK_KHR_portability_enumeration".equals(ext.extensionNameString())) {
                return true;
            }
        }
        return false;
    }

    private VkDebugUtilsMessengerCreateInfoEXT createDebugMessengerCreateInfo(MemoryStack stack) {
        return VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                .messageType(
                        VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                .pfnUserCallback((messageSeverity, messageType, pCallbackData, pUserData) -> {
                    VkDebugUtilsMessengerCallbackDataEXT callbackData =
                            VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                    String message = callbackData.pMessageString();

                    if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                        log.error("Vulkan validation: {}", message);
                    } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                        log.warn("Vulkan validation: {}", message);
                    } else {
                        log.debug("Vulkan: {}", message);
                    }
                    return VK_FALSE;
                });
    }

    private void setupDebugMessenger(MemoryStack stack) {
        VkDebugUtilsMessengerCreateInfoEXT createInfo = createDebugMessengerCreateInfo(stack);
        LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

        int result = vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger);
        if (result == VK_SUCCESS) {
            debugMessenger = pDebugMessenger.get(0);
            log.debug("Debug messenger created");
        } else {
            log.warn("Failed to create debug messenger: {}", result);
        }
    }

    private void selectPhysicalDevice(MemoryStack stack) {
        IntBuffer deviceCount = stack.ints(0);
        vkEnumeratePhysicalDevices(instance, deviceCount, null);

        if (deviceCount.get(0) == 0) {
            throw new RuntimeException("No Vulkan-capable GPU found");
        }

        PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
        vkEnumeratePhysicalDevices(instance, deviceCount, devices);

        // Select first suitable device (prefer discrete GPU)
        VkPhysicalDevice selectedDevice = null;
        int selectedScore = -1;

        for (int i = 0; i < deviceCount.get(0); i++) {
            VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), instance);
            int score = rateDevice(device, stack);
            if (score > selectedScore) {
                selectedScore = score;
                selectedDevice = device;
            }
        }

        if (selectedDevice == null || selectedScore < 0) {
            throw new RuntimeException("No suitable Vulkan GPU found");
        }

        physicalDevice = selectedDevice;

        // Get device properties
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(physicalDevice, properties);
        deviceName = properties.deviceNameString();
        maxTextureSize = properties.limits().maxImageDimension2D();
        maxLineWidth = properties.limits().lineWidthRange().get(1);

        log.info("Selected GPU: {} (max texture: {}, max line width: {})",
                deviceName, maxTextureSize, maxLineWidth);
    }

    private int rateDevice(VkPhysicalDevice device, MemoryStack stack) {
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(device, properties);

        // Check for required queue family
        int queueFamily = findGraphicsQueueFamily(device, stack);
        if (queueFamily < 0) {
            return -1;
        }

        int score = 0;

        // Prefer discrete GPU
        if (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 1000;
        }

        // Higher score for more texture size
        score += properties.limits().maxImageDimension2D() / 1000;

        return score;
    }

    private int findGraphicsQueueFamily(VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer queueFamilyCount = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

        VkQueueFamilyProperties.Buffer queueFamilies =
                VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

        for (int i = 0; i < queueFamilies.capacity(); i++) {
            if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                return i;
            }
        }

        return -1;
    }

    private void createLogicalDevice(MemoryStack stack) {
        graphicsQueueFamily = findGraphicsQueueFamily(physicalDevice, stack);

        // Queue create info
        VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
        queueCreateInfos.get(0)
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(graphicsQueueFamily)
                .pQueuePriorities(stack.floats(1.0f));

        // Query supported features
        VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceFeatures(physicalDevice, supportedFeatures);

        // Device features - only enable what's supported
        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

        // Enable wide lines if supported (not available on MoltenVK/Metal)
        if (supportedFeatures.wideLines()) {
            deviceFeatures.wideLines(true);
            log.debug("Wide lines feature enabled");
        } else {
            log.debug("Wide lines feature not supported - line width will be limited to 1.0");
            maxLineWidth = 1.0f;
        }

        // Device create info
        VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfos)
                .pEnabledFeatures(deviceFeatures);

        // Enable validation layers for device (deprecated but still used by some drivers)
        if (ENABLE_VALIDATION) {
            PointerBuffer layers = stack.mallocPointer(VALIDATION_LAYERS.size());
            for (String layer : VALIDATION_LAYERS) {
                layers.put(stack.UTF8(layer));
            }
            layers.flip();
            createInfo.ppEnabledLayerNames(layers);
        }

        // Create logical device
        PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);
        int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create logical device: " + result);
        }

        device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

        // Get graphics queue
        PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
        vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
        graphicsQueue = new VkQueue(pQueue.get(0), device);

        log.debug("Logical device and queue created");
    }

    private void createCommandPool(MemoryStack stack) {
        VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(graphicsQueueFamily);

        LongBuffer pCommandPool = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateCommandPool(device, poolInfo, null, pCommandPool);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create command pool: " + result);
        }

        commandPool = pCommandPool.get(0);

        // Allocate command buffer
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);

        PointerBuffer pCommandBuffers = stack.mallocPointer(1);
        result = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffer: " + result);
        }

        commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

        log.debug("Command pool and buffer created");
    }

    private void createSyncObjects(MemoryStack stack) {
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT);

        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

        LongBuffer pFence = stack.longs(VK_NULL_HANDLE);
        LongBuffer pSemaphore = stack.longs(VK_NULL_HANDLE);

        if (vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS ||
            vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create synchronization objects");
        }

        renderFence = pFence.get(0);
        renderSemaphore = pSemaphore.get(0);

        log.debug("Synchronization objects created");
    }

    private void createRenderPass(MemoryStack stack) {
        // Color attachment
        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
        attachments.get(0)
                .format(VK_FORMAT_R8G8B8A8_UNORM)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        // Color attachment reference
        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        // Subpass
        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorAttachmentRef);

        // Subpass dependency
        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
        dependency.get(0)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

        // Create render pass
        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subpass)
                .pDependencies(dependency);

        LongBuffer pRenderPass = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create render pass: " + result);
        }

        renderPass = pRenderPass.get(0);
        log.debug("Render pass created");
    }

    @Override
    public void dispose() {
        if (!initialized) {
            return;
        }

        log.info("Disposing Vulkan render device");

        // Wait for device to be idle
        if (device != null) {
            vkDeviceWaitIdle(device);
        }

        cleanup();
        initialized = false;
    }

    private void cleanup() {
        if (framebuffer != null) {
            framebuffer.dispose();
            framebuffer = null;
        }

        if (device != null) {
            if (renderPass != VK_NULL_HANDLE) {
                vkDestroyRenderPass(device, renderPass, null);
                renderPass = VK_NULL_HANDLE;
            }
            if (renderSemaphore != VK_NULL_HANDLE) {
                vkDestroySemaphore(device, renderSemaphore, null);
            }
            if (renderFence != VK_NULL_HANDLE) {
                vkDestroyFence(device, renderFence, null);
            }
            if (commandPool != VK_NULL_HANDLE) {
                vkDestroyCommandPool(device, commandPool, null);
            }
            vkDestroyDevice(device, null);
            device = null;
        }

        if (debugMessenger != VK_NULL_HANDLE && instance != null) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            debugMessenger = VK_NULL_HANDLE;
        }

        if (instance != null) {
            vkDestroyInstance(instance, null);
            instance = null;
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.VULKAN;
    }

    @Override
    public void beginFrame() {
        if (!initialized) return;

        try (MemoryStack stack = stackPush()) {
            // Wait for previous frame
            vkWaitForFences(device, renderFence, true, Long.MAX_VALUE);
            vkResetFences(device, renderFence);

            // Resize framebuffer if needed
            if (framebuffer.getWidth() != frameWidth || framebuffer.getHeight() != frameHeight) {
                framebuffer.resize(renderPass, frameWidth, frameHeight, false);
            }

            // Reset and begin command buffer
            vkResetCommandBuffer(commandBuffer, 0);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);
            inFrame = true;

            // Begin render pass
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.get(0).color()
                    .float32(0, clearR)
                    .float32(1, clearG)
                    .float32(2, clearB)
                    .float32(3, clearA);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer.getFramebuffer())
                    .pClearValues(clearValues);

            renderPassInfo.renderArea().offset().set(0, 0);
            renderPassInfo.renderArea().extent().set(frameWidth, frameHeight);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            // Set viewport
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0)
                    .y(frameHeight)  // Flip Y for OpenGL-style coordinates
                    .width(frameWidth)
                    .height(-frameHeight)  // Negative height for flip
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            vkCmdSetViewport(commandBuffer, 0, viewport);

            // Set scissor
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(frameWidth, frameHeight);
            vkCmdSetScissor(commandBuffer, 0, scissor);

            // Set line width
            vkCmdSetLineWidth(commandBuffer, currentLineWidth);
        }
    }

    @Override
    public void endFrame() {
        if (!initialized) return;

        try (MemoryStack stack = stackPush()) {
            // End render pass
            vkCmdEndRenderPass(commandBuffer);

            // Transition image for transfer if we need to read it back
            framebuffer.transitionForTransfer(commandBuffer);
            framebuffer.copyToStagingBuffer(commandBuffer);

            vkEndCommandBuffer(commandBuffer);
            inFrame = false;

            // Submit command buffer
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer));

            vkQueueSubmit(graphicsQueue, submitInfo, renderFence);
        }
    }

    /**
     * Reads the rendered frame into a pixel array.
     * Must be called after endFrame() and the fence has been waited on.
     *
     * @param pixels array to fill with ARGB pixels (must be width * height size)
     */
    public void readFramePixels(int[] pixels) {
        if (!initialized || framebuffer == null) return;

        // Wait for GPU to complete
        vkWaitForFences(device, renderFence, true, Long.MAX_VALUE);

        framebuffer.readPixels(pixels);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        frameWidth = width;
        frameHeight = height;
        // Viewport is set dynamically in Vulkan via command buffer
    }

    @Override
    public void setScissorEnabled(boolean enabled) {
        // Scissor is always enabled in Vulkan, controlled via dynamic state
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        if (!initialized || !inFrame) return;

        try (MemoryStack stack = stackPush()) {
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(x, y);
            scissor.extent().set(width, height);
            vkCmdSetScissor(commandBuffer, 0, scissor);
        }
    }

    @Override
    public void setBlendMode(BlendMode mode) {
        currentBlendMode = mode;
        // Blend mode is part of pipeline state in Vulkan
    }

    @Override
    public void setLineWidth(float width) {
        currentLineWidth = Math.min(width, maxLineWidth);
        // Line width is set dynamically via vkCmdSetLineWidth
        if (initialized && inFrame) {
            vkCmdSetLineWidth(commandBuffer, currentLineWidth);
        }
    }

    @Override
    public void setLineSmoothing(boolean enabled) {
        // Line smoothing is controlled via multisampling in Vulkan
    }

    @Override
    public void setDepthTestEnabled(boolean enabled) {
        // Depth test is part of pipeline state
    }

    @Override
    public void clearScreen(float r, float g, float b, float a) {
        // Store clear color for next frame's render pass begin
        clearR = r;
        clearG = g;
        clearB = b;
        clearA = a;
    }

    @Override
    public void clearDepth() {
        // Depth clear is done via render pass begin
    }

    @Override
    public Shader createShader(ShaderSource source) {
        return new VkShader(this, source);
    }

    @Override
    public Buffer createBuffer(BufferDescriptor descriptor) {
        return new VkBuffer(this, descriptor);
    }

    @Override
    public Texture createTexture(TextureDescriptor descriptor) {
        return new VkTexture(this, descriptor);
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
        return "Vulkan - " + deviceName;
    }

    // -------------------------------------------------------------------------
    // Vulkan-specific getters
    // -------------------------------------------------------------------------

    public VkInstance getInstance() {
        return instance;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public int getGraphicsQueueFamily() {
        return graphicsQueueFamily;
    }

    public long getCommandPool() {
        return commandPool;
    }

    public VkCommandBuffer getCommandBuffer() {
        return inFrame ? commandBuffer : null;
    }

    /**
     * Returns true if currently recording a frame (between beginFrame and endFrame).
     */
    public boolean isInFrame() {
        return inFrame;
    }

    public BlendMode getCurrentBlendMode() {
        return currentBlendMode;
    }

    public long getRenderPass() {
        return renderPass;
    }

    public VkFramebuffer getFramebuffer() {
        return framebuffer;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    /**
     * Sets the currently bound shader.
     * Called by VkShader.bind() to register itself.
     */
    public void setCurrentShader(VkShader shader) {
        this.currentShader = shader;
    }

    /**
     * Returns the currently bound shader.
     */
    public VkShader getCurrentShader() {
        return currentShader;
    }

    /**
     * Sets the resource manager for this device.
     * Used to access the pipeline cache.
     */
    public void setResourceManager(VkResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Returns the resource manager.
     */
    public VkResourceManager getResourceManager() {
        return resourceManager;
    }
}
