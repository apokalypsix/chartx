package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * DirectX 12 implementation of ResourceManager.
 *
 * <p>Manages shaders, buffers, textures, and pipeline caching.
 */
public class DX12ResourceManager implements ResourceManager {

    private static final Logger log = LoggerFactory.getLogger(DX12ResourceManager.class);

    private DX12RenderDevice device;
    private DX12TextRenderer textRenderer;

    // Resource storage
    private final ConcurrentHashMap<String, DX12Shader> shaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DX12Buffer> buffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DX12Texture> textures = new ConcurrentHashMap<>();

    // Pipeline cache
    private final DX12PipelineCache pipelineCache = new DX12PipelineCache();

    // Deferred operations
    private final ConcurrentLinkedQueue<Runnable> deferredOps = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> disposeOps = new ConcurrentLinkedQueue<>();

    // Current shader for draw calls
    private DX12Shader currentShader;

    private boolean initialized;

    public DX12ResourceManager() {
        // Default constructor
    }

    @Override
    public void initialize(RenderDevice renderDevice) {
        if (initialized) {
            return;
        }

        if (!(renderDevice instanceof DX12RenderDevice)) {
            throw new IllegalArgumentException("Expected DX12RenderDevice");
        }

        this.device = (DX12RenderDevice) renderDevice;
        this.device.setResourceManager(this);

        log.info("Initializing DX12 resource manager");

        // Load default shaders
        loadDefaultShaders();

        initialized = true;
        log.info("DX12 resource manager initialized");
    }

    private void loadDefaultShaders() {
        // Default shader (per-vertex color)
        String defaultSource = DX12ShaderRegistry.getShaderSource("default");
        if (defaultSource != null) {
            try {
                DX12Shader shader = new DX12Shader("default", defaultSource);
                shaders.put(SHADER_DEFAULT, shader);
                shaders.put("default", shader);
                log.debug("Loaded default DX12 shader");
            } catch (Exception e) {
                log.warn("Failed to load default shader: {}", e.getMessage());
            }
        }

        // Simple shader (uniform color)
        String simpleSource = DX12ShaderRegistry.getShaderSource("simple");
        if (simpleSource != null) {
            try {
                DX12Shader shader = new DX12Shader("simple", simpleSource);
                shaders.put(SHADER_SIMPLE, shader);
                shaders.put("simple", shader);
                log.debug("Loaded simple DX12 shader");
            } catch (Exception e) {
                log.warn("Failed to load simple shader: {}", e.getMessage());
            }
        }

        // Text shader
        String textSource = DX12ShaderRegistry.getShaderSource("text");
        if (textSource != null) {
            try {
                DX12Shader shader = new DX12Shader("text", textSource);
                shaders.put(SHADER_TEXT, shader);
                shaders.put("text", shader);
                log.debug("Loaded text DX12 shader");
            } catch (Exception e) {
                log.warn("Failed to load text shader: {}", e.getMessage());
            }
        }
    }

    @Override
    public Shader getShader(String name) {
        return shaders.get(name);
    }

    /**
     * Gets a shader with the correct type.
     */
    DX12Shader getShader(String name, @SuppressWarnings("unused") boolean typed) {
        return shaders.get(name);
    }

    @Override
    public void registerShader(String name, Shader shader) {
        if (shader instanceof DX12Shader dx12Shader) {
            shaders.put(name, dx12Shader);
        } else {
            throw new IllegalArgumentException("Expected DX12Shader");
        }
    }

    @Override
    public Shader createShader(String name, ShaderSource source) {
        // DX12 uses HLSL, not GLSL/SPIR-V from ShaderSource
        // Get HLSL from registry instead
        String hlslSource = DX12ShaderRegistry.getShaderSource(name);
        if (hlslSource != null) {
            DX12Shader shader = new DX12Shader(name, hlslSource);
            shaders.put(name, shader);
            return shader;
        }
        throw new UnsupportedOperationException("Shader not found in DX12ShaderRegistry: " + name);
    }

    @Override
    public Buffer getOrCreateBuffer(String name, BufferDescriptor descriptor) {
        return buffers.computeIfAbsent(name, k -> new DX12Buffer(device, descriptor));
    }

    @Override
    public Buffer getBuffer(String name) {
        return buffers.get(name);
    }

    @Override
    public void disposeBuffer(String name) {
        DX12Buffer buffer = buffers.remove(name);
        if (buffer != null) {
            disposeOps.add(buffer::dispose);
        }
    }

    @Override
    public Texture createTexture(TextureDescriptor descriptor) {
        return new DX12Texture(device, descriptor);
    }

    @Override
    public void registerTexture(String name, Texture texture) {
        if (texture instanceof DX12Texture dx12Texture) {
            textures.put(name, dx12Texture);
        } else {
            throw new IllegalArgumentException("Expected DX12Texture");
        }
    }

    @Override
    public Texture getTexture(String name) {
        return textures.get(name);
    }

    @Override
    public void disposeTexture(String name) {
        DX12Texture texture = textures.remove(name);
        if (texture != null) {
            disposeOps.add(texture::dispose);
        }
    }

    @Override
    public TextRenderer getTextRenderer() {
        if (textRenderer == null && device != null) {
            textRenderer = new DX12TextRenderer(device, this);
        }
        return textRenderer;
    }

    @Override
    public void runOnRenderThread(Runnable action) {
        deferredOps.add(action);
    }

    @Override
    public void processPendingOperations() {
        Runnable op;
        while ((op = deferredOps.poll()) != null) {
            try {
                op.run();
            } catch (Exception e) {
                log.error("Error in deferred operation: {}", e.getMessage(), e);
            }
        }

        while ((op = disposeOps.poll()) != null) {
            try {
                op.run();
            } catch (Exception e) {
                log.error("Error in dispose operation: {}", e.getMessage(), e);
            }
        }
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
    public void dispose() {
        if (!initialized) {
            return;
        }

        log.info("Disposing DX12 resource manager");

        // Dispose text renderer
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
        }

        // Clear pipeline cache
        pipelineCache.clear();

        // Dispose all buffers
        buffers.values().forEach(DX12Buffer::dispose);
        buffers.clear();

        // Dispose all textures
        textures.values().forEach(DX12Texture::dispose);
        textures.clear();

        // Dispose all shaders
        shaders.values().forEach(DX12Shader::dispose);
        shaders.clear();

        initialized = false;
    }

    /**
     * Gets the pipeline cache.
     */
    DX12PipelineCache getPipelineCache() {
        return pipelineCache;
    }

    /**
     * Sets the current shader for draw calls.
     */
    void setCurrentShader(DX12Shader shader) {
        this.currentShader = shader;
    }

    /**
     * Gets the current shader.
     */
    DX12Shader getCurrentShader() {
        return currentShader;
    }
}
