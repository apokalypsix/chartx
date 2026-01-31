package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

/**
 * SPI provider for the Metal rendering backend.
 *
 * <p>This provider is automatically discovered via ServiceLoader when the
 * chartx-backend-metal module is on the classpath. Metal is only available
 * on macOS.
 */
public class MetalBackendProvider implements RenderBackendProvider {

    private static final Logger log = LoggerFactory.getLogger(MetalBackendProvider.class);

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.METAL;
    }

    @Override
    public boolean isAvailable() {
        // Metal is only available on macOS
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            log.debug("Metal backend not available: not running on macOS");
            return false;
        }

        try {
            boolean available = MetalNative.isAvailable();
            log.debug("Metal backend availability: {}", available);
            return available;
        } catch (Exception | UnsatisfiedLinkError e) {
            log.debug("Metal backend not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getPriority() {
        // Metal is a platform-optimized backend with highest priority on macOS
        return 150;
    }

    @Override
    public RenderDevice createDevice(Component target) {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Metal backend is not available");
        }
        return new MetalRenderDevice();
    }

    @Override
    public ResourceManager createResourceManager() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Metal backend is not available");
        }
        return new MetalResourceManager();
    }
}
