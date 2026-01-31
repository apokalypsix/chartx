package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

/**
 * SPI provider for the Vulkan rendering backend.
 *
 * <p>This provider is automatically discovered via ServiceLoader when the
 * chartx-backend-vulkan module is on the classpath.
 */
public class VulkanBackendProvider implements RenderBackendProvider {

    private static final Logger log = LoggerFactory.getLogger(VulkanBackendProvider.class);

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.VULKAN;
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("org.lwjgl.vulkan.VK10");
            log.debug("Vulkan backend is available (LWJGL Vulkan found)");
            return true;
        } catch (ClassNotFoundException e) {
            log.debug("Vulkan backend not available: LWJGL Vulkan not found");
            return false;
        }
    }

    @Override
    public int getPriority() {
        // Vulkan is a modern backend with higher priority than OpenGL
        return 100;
    }

    @Override
    public RenderDevice createDevice(Component target) {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Vulkan backend is not available");
        }
        return new VkRenderDevice();
    }

    @Override
    public ResourceManager createResourceManager() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Vulkan backend is not available");
        }
        return new VkResourceManager();
    }
}
