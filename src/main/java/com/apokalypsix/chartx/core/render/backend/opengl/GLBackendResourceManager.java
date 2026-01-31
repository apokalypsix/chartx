package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * OpenGL implementation of the ResourceManager interface.
 *
 * <p>Manages shaders, buffers, and textures for the OpenGL backend.
 * Provides thread-safe access to resources and handles deferred operations.
 */
public class GLBackendResourceManager implements ResourceManager {

    private static final Logger log = LoggerFactory.getLogger(GLBackendResourceManager.class);

    private GLRenderDevice device;
    private boolean initialized = false;
    private GLTextRenderer textRenderer;

    private final Map<String, Shader> shaders = new ConcurrentHashMap<>();
    private final Map<String, Buffer> buffers = new ConcurrentHashMap<>();
    private final Map<String, Texture> textures = new ConcurrentHashMap<>();

    private final Queue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> disposeOperations = new ConcurrentLinkedQueue<>();

    // Default shader sources
    private static final String DEFAULT_VERTEX_SHADER = """
            #version 150
            in vec2 aPosition;
            in vec4 aColor;

            uniform mat4 uProjection;

            out vec4 vColor;

            void main() {
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
                vColor = aColor;
            }
            """;

    private static final String DEFAULT_FRAGMENT_SHADER = """
            #version 150
            in vec4 vColor;

            out vec4 fragColor;

            void main() {
                fragColor = vColor;
            }
            """;

    private static final String SIMPLE_VERTEX_SHADER = """
            #version 150
            in vec2 aPosition;

            uniform mat4 uProjection;

            void main() {
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
            }
            """;

    private static final String SIMPLE_FRAGMENT_SHADER = """
            #version 150
            uniform vec4 uColor;

            out vec4 fragColor;

            void main() {
                fragColor = uColor;
            }
            """;

    private static final String TEXT_VERTEX_SHADER = """
            #version 150
            in vec2 aPosition;
            in vec2 aTexCoord;
            in vec4 aColor;

            uniform mat4 uProjection;

            out vec2 vTexCoord;
            out vec4 vColor;

            void main() {
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
            """;

    private static final String TEXT_FRAGMENT_SHADER = """
            #version 150
            in vec2 vTexCoord;
            in vec4 vColor;

            uniform sampler2D uTexture;

            out vec4 fragColor;

            void main() {
                float alpha = texture(uTexture, vTexCoord).r;
                if (alpha < 0.01) discard;
                fragColor = vec4(vColor.rgb, vColor.a * alpha);
            }
            """;

    @Override
    public void initialize(RenderDevice device) {
        if (!(device instanceof GLRenderDevice)) {
            throw new IllegalArgumentException("GLBackendResourceManager requires GLRenderDevice");
        }

        this.device = (GLRenderDevice) device;

        log.debug("Initializing GL resource manager");

        // Load default shaders
        loadDefaultShaders();

        initialized = true;
        log.debug("GL resource manager initialized");
    }

    private void loadDefaultShaders() {
        // Default shader with per-vertex color
        Shader defaultShader = createShader(SHADER_DEFAULT,
                ShaderSource.glsl(SHADER_DEFAULT, DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER));
        if (defaultShader.isValid()) {
            log.debug("Loaded default shader");
        } else {
            log.error("Failed to compile default shader");
        }

        // Simple shader with uniform color
        Shader simpleShader = createShader(SHADER_SIMPLE,
                ShaderSource.glsl(SHADER_SIMPLE, SIMPLE_VERTEX_SHADER, SIMPLE_FRAGMENT_SHADER));
        if (simpleShader.isValid()) {
            log.debug("Loaded simple shader");
        } else {
            log.error("Failed to compile simple shader");
        }

        // Text shader for texture atlas text rendering
        Shader textShader = createShader(SHADER_TEXT,
                ShaderSource.glsl(SHADER_TEXT, TEXT_VERTEX_SHADER, TEXT_FRAGMENT_SHADER));
        if (textShader.isValid()) {
            log.debug("Loaded text shader");
        } else {
            log.error("Failed to compile text shader");
        }
    }

    @Override
    public void dispose() {
        log.debug("Disposing GL resource manager");

        // Dispose text renderer
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
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
            textRenderer = new GLTextRenderer(device);
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
                log.error("Error executing pending GL operation", e);
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
    // Utility methods
    // -------------------------------------------------------------------------

    /**
     * Creates an orthographic projection matrix for 2D rendering.
     *
     * @param left left edge of the viewport
     * @param right right edge of the viewport
     * @param bottom bottom edge of the viewport
     * @param top top edge of the viewport
     * @return 4x4 projection matrix in column-major order
     */
    public static float[] createOrthoMatrix(float left, float right, float bottom, float top) {
        float[] matrix = new float[16];

        float width = right - left;
        float height = top - bottom;

        // Column-major order for OpenGL
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
