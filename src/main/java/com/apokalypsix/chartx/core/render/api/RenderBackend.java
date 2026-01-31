package com.apokalypsix.chartx.core.render.api;

/**
 * Available rendering backends.
 */
public enum RenderBackend {
    /**
     * OpenGL backend using JOGL.
     */
    OPENGL,

    /**
     * Vulkan backend using LWJGL.
     */
    VULKAN,

    /**
     * Metal backend for macOS using JNI.
     * <p>Native GPU-accelerated rendering via Apple's Metal API.
     * Only available on macOS.
     */
    METAL,

    /**
     * DirectX 12 backend for Windows using JNI.
     * <p>Native GPU-accelerated rendering via Microsoft's DirectX 12 API.
     * Only available on Windows.
     */
    DX12,

    /**
     * Automatically select the best available backend.
     */
    AUTO
}
