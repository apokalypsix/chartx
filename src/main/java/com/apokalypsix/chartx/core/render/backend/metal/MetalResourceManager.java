package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Metal implementation of the ResourceManager interface.
 *
 * <p>Manages shaders, buffers, textures, and pipelines for the Metal backend.
 * Provides thread-safe access to resources and handles deferred operations.
 */
public class MetalResourceManager implements ResourceManager {

    private static final Logger log = LoggerFactory.getLogger(MetalResourceManager.class);

    private MetalRenderDevice device;
    private boolean initialized = false;
    private MetalTextRenderer textRenderer;

    private final Map<String, Shader> shaders = new ConcurrentHashMap<>();
    private final Map<String, Buffer> buffers = new ConcurrentHashMap<>();
    private final Map<String, Texture> textures = new ConcurrentHashMap<>();

    private final Queue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> disposeOperations = new ConcurrentLinkedQueue<>();

    private MetalPipelineCache pipelineCache;

    @Override
    public void initialize(RenderDevice device) {
        if (!(device instanceof MetalRenderDevice)) {
            throw new IllegalArgumentException("MetalResourceManager requires MetalRenderDevice");
        }

        this.device = (MetalRenderDevice) device;
        this.device.setResourceManager(this);

        log.info("Initializing Metal resource manager");

        // Create pipeline cache
        pipelineCache = new MetalPipelineCache(this.device);

        // Load default shaders
        loadDefaultShaders();

        initialized = true;
        log.info("Metal resource manager initialized");
    }

    private void loadDefaultShaders() {
        // Default shader with per-vertex color
        MetalShader defaultShader = new MetalShader(device, SHADER_DEFAULT, MetalShaderRegistry.DEFAULT_SHADER);
        if (defaultShader.isValid()) {
            shaders.put(SHADER_DEFAULT, defaultShader);
            log.debug("Loaded default Metal shader");
        } else {
            log.error("Failed to compile default Metal shader");
        }

        // Simple shader with uniform color
        MetalShader simpleShader = new MetalShader(device, SHADER_SIMPLE, MetalShaderRegistry.SIMPLE_SHADER);
        if (simpleShader.isValid()) {
            shaders.put(SHADER_SIMPLE, simpleShader);
            log.debug("Loaded simple Metal shader");
        } else {
            log.error("Failed to compile simple Metal shader");
        }

        // Text shader for texture atlas text rendering
        MetalShader textShader = new MetalShader(device, SHADER_TEXT, MetalShaderRegistry.TEXT_SHADER);
        if (textShader.isValid()) {
            shaders.put(SHADER_TEXT, textShader);
            log.debug("Loaded text Metal shader");
        } else {
            log.error("Failed to compile text Metal shader");
        }
    }

    @Override
    public void dispose() {
        log.info("Disposing Metal resource manager");

        // Dispose text renderer
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
        }

        // Dispose pipeline cache
        if (pipelineCache != null) {
            pipelineCache.dispose();
            pipelineCache = null;
        }

        // Dispose all shaders
        for (Shader shader : shaders.values()) {
            shader.dispose();
        }
        shaders.clear();

        // Dispose all buffers
        for (Buffer buffer : buffers.values()) {
            buffer.dispose();
        }
        buffers.clear();

        // Dispose all textures
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();

        // Process any remaining dispose operations
        Runnable op;
        while ((op = disposeOperations.poll()) != null) {
            try {
                op.run();
            } catch (Exception e) {
                log.error("Error during dispose", e);
            }
        }

        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public RenderDevice getDevice() {
        return device;
    }

    @Override
    public Shader getShader(String name) {
        return shaders.get(name);
    }

    @Override
    public void registerShader(String name, Shader shader) {
        Shader existing = shaders.put(name, shader);
        if (existing != null) {
            disposeOperations.add(existing::dispose);
        }
    }

    @Override
    public Shader createShader(String name, ShaderSource source) {
        Shader shader = device.createShader(source);
        shaders.put(name, shader);
        return shader;
    }

    @Override
    public Buffer getOrCreateBuffer(String name, BufferDescriptor descriptor) {
        return buffers.computeIfAbsent(name, n -> device.createBuffer(descriptor));
    }

    @Override
    public Buffer getBuffer(String name) {
        return buffers.get(name);
    }

    @Override
    public void disposeBuffer(String name) {
        Buffer buffer = buffers.remove(name);
        if (buffer != null) {
            buffer.dispose();
        }
    }

    @Override
    public Texture createTexture(TextureDescriptor descriptor) {
        return device.createTexture(descriptor);
    }

    @Override
    public Texture getTexture(String name) {
        return textures.get(name);
    }

    @Override
    public void registerTexture(String name, Texture texture) {
        Texture existing = textures.put(name, texture);
        if (existing != null) {
            disposeOperations.add(existing::dispose);
        }
    }

    @Override
    public void disposeTexture(String name) {
        Texture texture = textures.remove(name);
        if (texture != null) {
            texture.dispose();
        }
    }

    @Override
    public TextRenderer getTextRenderer() {
        if (textRenderer == null && device != null) {
            textRenderer = new MetalTextRenderer(device, this);
        }
        return textRenderer;
    }

    @Override
    public void runOnRenderThread(Runnable operation) {
        pendingOperations.add(operation);
    }

    @Override
    public void processPendingOperations() {
        Runnable op;
        while ((op = pendingOperations.poll()) != null) {
            try {
                op.run();
            } catch (Exception e) {
                log.error("Error executing pending Metal operation", e);
            }
        }

        // Also process dispose operations
        while ((op = disposeOperations.poll()) != null) {
            try {
                op.run();
            } catch (Exception e) {
                log.error("Error during deferred dispose", e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Metal-specific methods
    // -------------------------------------------------------------------------

    /**
     * Returns the pipeline cache.
     */
    public MetalPipelineCache getPipelineCache() {
        return pipelineCache;
    }

    /**
     * Creates an orthographic projection matrix for 2D rendering.
     *
     * @param left   left edge of the viewport
     * @param right  right edge of the viewport
     * @param bottom bottom edge of the viewport
     * @param top    top edge of the viewport
     * @return 4x4 projection matrix in column-major order
     */
    public static float[] createOrthoMatrix(float left, float right, float bottom, float top) {
        float[] matrix = new float[16];

        float width = right - left;
        float height = top - bottom;

        // Column-major order
        matrix[0] = 2.0f / width;
        matrix[1] = 0;
        matrix[2] = 0;
        matrix[3] = 0;

        matrix[4] = 0;
        matrix[5] = 2.0f / height;
        matrix[6] = 0;
        matrix[7] = 0;

        matrix[8] = 0;
        matrix[9] = 0;
        matrix[10] = -1;
        matrix[11] = 0;

        matrix[12] = -(right + left) / width;
        matrix[13] = -(top + bottom) / height;
        matrix[14] = 0;
        matrix[15] = 1;

        return matrix;
    }
}
