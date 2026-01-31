package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

/**
 * SPI provider for the OpenGL rendering backend.
 *
 * <p>This provider is automatically discovered via ServiceLoader when the
 * chartx module is on the classpath.
 */
public class OpenGLBackendProvider implements RenderBackendProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenGLBackendProvider.class);

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.OPENGL;
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.jogamp.opengl.GL2ES2");
            log.debug("OpenGL backend is available (JOGL found)");
            return true;
        } catch (ClassNotFoundException e) {
            log.debug("OpenGL backend not available: JOGL not found");
            return false;
        }
    }

    @Override
    public int getPriority() {
        // OpenGL is a standard backend with moderate priority
        // Modern/platform-optimized backends (Vulkan, Metal, DX12) have higher priority
        return 50;
    }

    @Override
    public RenderDevice createDevice(Component target) {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("OpenGL backend is not available");
        }
        return new GLRenderDevice(target);
    }

    @Override
    public ResourceManager createResourceManager() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("OpenGL backend is not available");
        }
        return new GLBackendResourceManager();
    }
}
