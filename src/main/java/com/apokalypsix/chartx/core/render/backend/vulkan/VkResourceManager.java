package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Vulkan implementation of the ResourceManager interface.
 *
 * <p>Manages shaders, buffers, and textures for the Vulkan backend.
 */
public class VkResourceManager implements ResourceManager {

    private static final Logger log = LoggerFactory.getLogger(VkResourceManager.class);

    private VkRenderDevice device;
    private boolean initialized = false;
    private VkTextRendererImpl textRenderer;

    private final Map<String, Shader> shaders = new ConcurrentHashMap<>();
    private final Map<String, Buffer> buffers = new ConcurrentHashMap<>();
    private final Map<String, Texture> textures = new ConcurrentHashMap<>();

    private final Queue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> disposeOperations = new ConcurrentLinkedQueue<>();

    // Pipeline cache for on-demand pipeline creation
    private VkPipelineCache pipelineCache;

    // Vulkan SPIR-V shader sources (push constants layout)
    // These shaders use push constants for projection matrix and color uniform
    private static final String DEFAULT_VERTEX_SHADER_GLSL = """
            #version 450

            layout(location = 0) in vec2 aPosition;
            layout(location = 1) in vec4 aColor;

            layout(push_constant) uniform PushConstants {
                mat4 uProjection;
                vec4 uColorUniform;
            } pc;

            layout(location = 0) out vec4 vColor;

            void main() {
                gl_Position = pc.uProjection * vec4(aPosition, 0.0, 1.0);
                vColor = aColor;
            }
            """;

    private static final String DEFAULT_FRAGMENT_SHADER_GLSL = """
            #version 450

            layout(location = 0) in vec4 vColor;

            layout(location = 0) out vec4 fragColor;

            void main() {
                fragColor = vColor;
            }
            """;

    private static final String SIMPLE_VERTEX_SHADER_GLSL = """
            #version 450

            layout(location = 0) in vec2 aPosition;

            layout(push_constant) uniform PushConstants {
                mat4 uProjection;
                vec4 uColor;
            } pc;

            void main() {
                gl_Position = pc.uProjection * vec4(aPosition, 0.0, 1.0);
            }
            """;

    private static final String SIMPLE_FRAGMENT_SHADER_GLSL = """
            #version 450

            layout(push_constant) uniform PushConstants {
                mat4 uProjection;
                vec4 uColor;
            } pc;

            layout(location = 0) out vec4 fragColor;

            void main() {
                fragColor = pc.uColor;
            }
            """;

    private static final String TEXT_VERTEX_SHADER_GLSL = """
            #version 450

            layout(location = 0) in vec2 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec4 aColor;

            layout(push_constant) uniform PushConstants {
                mat4 uProjection;
                vec4 uColorUniform;
            } pc;

            layout(location = 0) out vec2 vTexCoord;
            layout(location = 1) out vec4 vColor;

            void main() {
                gl_Position = pc.uProjection * vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
            """;

    private static final String TEXT_FRAGMENT_SHADER_GLSL = """
            #version 450

            layout(location = 0) in vec2 vTexCoord;
            layout(location = 1) in vec4 vColor;

            layout(binding = 0) uniform sampler2D uTexture;

            layout(location = 0) out vec4 fragColor;

            void main() {
                float alpha = texture(uTexture, vTexCoord).r;
                if (alpha < 0.01) discard;
                fragColor = vec4(vColor.rgb, vColor.a * alpha);
            }
            """;

    @Override
    public void initialize(RenderDevice device) {
        if (!(device instanceof VkRenderDevice)) {
            throw new IllegalArgumentException("VkResourceManager requires VkRenderDevice");
        }

        this.device = (VkRenderDevice) device;

        log.info("Initializing Vulkan resource manager");

        // Register this resource manager with the device
        this.device.setResourceManager(this);

        // Create pipeline cache
        pipelineCache = new VkPipelineCache(this.device);

        // Load default shaders
        loadDefaultShaders();

        initialized = true;
        log.info("Vulkan resource manager initialized");
    }

    private void loadDefaultShaders() {
        // Default shader with per-vertex color
        Shader defaultShader = createShader(SHADER_DEFAULT,
                ShaderSource.glsl(SHADER_DEFAULT, DEFAULT_VERTEX_SHADER_GLSL, DEFAULT_FRAGMENT_SHADER_GLSL));
        if (defaultShader.isValid()) {
            log.debug("Loaded default shader");
        } else {
            log.error("Failed to compile default shader");
        }

        // Simple shader with uniform color
        Shader simpleShader = createShader(SHADER_SIMPLE,
                ShaderSource.glsl(SHADER_SIMPLE, SIMPLE_VERTEX_SHADER_GLSL, SIMPLE_FRAGMENT_SHADER_GLSL));
        if (simpleShader.isValid()) {
            log.debug("Loaded simple shader");
        } else {
            log.error("Failed to compile simple shader");
        }

        // Text shader for texture atlas text rendering
        Shader textShader = createShader(SHADER_TEXT,
                ShaderSource.glsl(SHADER_TEXT, TEXT_VERTEX_SHADER_GLSL, TEXT_FRAGMENT_SHADER_GLSL));
        if (textShader.isValid()) {
            log.debug("Loaded text shader");
        } else {
            log.error("Failed to compile text shader");
        }
    }

    @Override
    public void dispose() {
        log.info("Disposing Vulkan resource manager");

        // Dispose text renderer
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
        }

        // Dispose pipeline cache first (depends on shaders)
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
            textRenderer = new VkTextRendererImpl(device, this);
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
                log.error("Error executing pending Vulkan operation", e);
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
    // Vulkan-specific methods
    // -------------------------------------------------------------------------

    /**
     * Returns the pipeline cache for on-demand pipeline creation.
     */
    public VkPipelineCache getPipelineCache() {
        return pipelineCache;
    }

    /**
     * Returns the Vulkan render device.
     */
    public VkRenderDevice getVkDevice() {
        return device;
    }
}
