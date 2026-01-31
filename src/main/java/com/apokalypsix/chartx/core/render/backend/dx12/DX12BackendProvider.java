package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

/**
 * SPI provider for the DirectX 12 rendering backend.
 *
 * <p>This provider is automatically discovered via ServiceLoader when the
 * chartx-backend-dx12 module is on the classpath. DirectX 12 is only available
 * on Windows.
 */
public class DX12BackendProvider implements RenderBackendProvider {

    private static final Logger log = LoggerFactory.getLogger(DX12BackendProvider.class);

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.DX12;
    }

    @Override
    public boolean isAvailable() {
        // DX12 is only available on Windows
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("windows")) {
            log.debug("DX12 backend not available: not running on Windows");
            return false;
        }

        try {
            boolean available = DX12Native.isAvailable();
            log.debug("DX12 backend availability: {}", available);
            return available;
        } catch (Exception | UnsatisfiedLinkError e) {
            log.debug("DX12 backend not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getPriority() {
        // DX12 is a platform-optimized backend with highest priority on Windows
        return 150;
    }

    @Override
    public RenderDevice createDevice(Component target) {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("DX12 backend is not available");
        }
        return new DX12RenderDevice();
    }

    @Override
    public ResourceManager createResourceManager() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("DX12 backend is not available");
        }
        return new DX12ResourceManager();
    }
}
