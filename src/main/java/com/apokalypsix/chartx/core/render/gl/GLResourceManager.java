package com.apokalypsix.chartx.core.render.gl;

import com.jogamp.opengl.GL2ES2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages OpenGL resources (shaders, buffers, textures) lifecycle.
 *
 * <p>Provides thread-safe access to resources and handles deferred initialization
 * and cleanup of GL objects.
 */
public class GLResourceManager {

    private static final Logger log = LoggerFactory.getLogger(GLResourceManager.class);

    private final Map<String, ShaderProgram> shaders = new ConcurrentHashMap<>();
    private final Map<String, VertexBuffer> buffers = new ConcurrentHashMap<>();

    private final Queue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> disposeOperations = new ConcurrentLinkedQueue<>();

    private boolean initialized = false;

    // Default shader source (simple 2D rendering with color) - GLSL 150 for Core profile
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

    // Simple shader for single-color rendering
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

    // Text shader for texture atlas text rendering
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

    /**
     * Initializes the resource manager. Must be called on the GL thread.
     */
    public void initialize(GL2ES2 gl) {
        log.debug("Initializing GL resource manager");

        // Load default shaders
        loadDefaultShaders(gl);

        initialized = true;
        log.debug("GL resource manager initialized");
    }

    private void loadDefaultShaders(GL2ES2 gl) {
        // Default shader with per-vertex color
        ShaderProgram defaultShader = new ShaderProgram();
        if (defaultShader.compile(gl, DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER)) {
            shaders.put("default", defaultShader);
            log.debug("Loaded default shader");
        } else {
            log.error("Failed to compile default shader");
        }

        // Simple shader with uniform color
        ShaderProgram simpleShader = new ShaderProgram();
        if (simpleShader.compile(gl, SIMPLE_VERTEX_SHADER, SIMPLE_FRAGMENT_SHADER)) {
            shaders.put("simple", simpleShader);
            log.debug("Loaded simple shader");
        } else {
            log.error("Failed to compile simple shader");
        }

        // Text shader for texture atlas text rendering
        ShaderProgram textShader = new ShaderProgram();
        if (textShader.compile(gl, TEXT_VERTEX_SHADER, TEXT_FRAGMENT_SHADER)) {
            shaders.put("text", textShader);
            log.debug("Loaded text shader");
        } else {
            log.error("Failed to compile text shader");
        }
    }

    /**
     * Returns a shader by name.
     */
    public ShaderProgram getShader(String name) {
        return shaders.get(name);
    }

    /**
     * Registers a custom shader.
     */
    public void registerShader(String name, ShaderProgram shader) {
        ShaderProgram existing = shaders.put(name, shader);
        if (existing != null) {
            // Queue old shader for disposal
            disposeOperations.add(() -> existing.dispose(null)); // Will be handled on GL thread
        }
    }

    /**
     * Creates or retrieves a vertex buffer by name.
     */
    public VertexBuffer getOrCreateBuffer(String name, int floatsPerVertex, boolean dynamic) {
        return buffers.computeIfAbsent(name, n -> new VertexBuffer(floatsPerVertex, dynamic));
    }

    /**
     * Returns an existing buffer or null if not found.
     */
    public VertexBuffer getBuffer(String name) {
        return buffers.get(name);
    }

    /**
     * Removes and disposes a buffer.
     */
    public void disposeBuffer(GL2ES2 gl, String name) {
        VertexBuffer buffer = buffers.remove(name);
        if (buffer != null) {
            buffer.dispose(gl);
        }
    }

    /**
     * Queues an operation to be executed on the GL thread.
     */
    public void runOnGLThread(Runnable operation) {
        pendingOperations.add(operation);
    }

    /**
     * Processes all pending operations. Must be called on the GL thread.
     */
    public void processPendingOperations(GL2ES2 gl) {
        Runnable op;
        while ((op = pendingOperations.poll()) != null) {
            try {
                op.run();
            } catch (Exception e) {
                log.error("Error executing pending GL operation", e);
            }
        }
    }

    /**
     * Returns true if the manager has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Releases all GL resources. Must be called on the GL thread.
     */
    public void dispose(GL2ES2 gl) {
        log.debug("Disposing GL resource manager");

        // Dispose all shaders
        for (ShaderProgram shader : shaders.values()) {
            shader.dispose(gl);
        }
        shaders.clear();

        // Dispose all buffers
        for (VertexBuffer buffer : buffers.values()) {
            buffer.dispose(gl);
        }
        buffers.clear();

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
