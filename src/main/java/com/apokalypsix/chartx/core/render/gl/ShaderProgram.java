package com.apokalypsix.chartx.core.render.gl;

import com.jogamp.opengl.GL2ES2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper for OpenGL shader program with vertex and fragment shaders.
 *
 * <p>Handles shader compilation, linking, and uniform management.
 */
public class ShaderProgram {

    private static final Logger log = LoggerFactory.getLogger(ShaderProgram.class);

    private int programId = 0;
    private int vertexShaderId = 0;
    private int fragmentShaderId = 0;
    private boolean initialized = false;

    private final Map<String, Integer> uniformLocations = new HashMap<>();

    /**
     * Creates and compiles a shader program from source strings.
     *
     * @param gl the GL context
     * @param vertexSource vertex shader source code
     * @param fragmentSource fragment shader source code
     * @return true if compilation succeeded
     */
    public boolean compile(GL2ES2 gl, String vertexSource, String fragmentSource) {
        if (initialized) {
            dispose(gl);
        }

        // Compile vertex shader
        vertexShaderId = compileShader(gl, GL2ES2.GL_VERTEX_SHADER, vertexSource);
        if (vertexShaderId == 0) {
            return false;
        }

        // Compile fragment shader
        fragmentShaderId = compileShader(gl, GL2ES2.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShaderId == 0) {
            gl.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
            return false;
        }

        // Link program
        programId = gl.glCreateProgram();
        gl.glAttachShader(programId, vertexShaderId);
        gl.glAttachShader(programId, fragmentShaderId);

        // Bind standard attribute locations before linking (for GLSL 120 compatibility)
        gl.glBindAttribLocation(programId, 0, "aPosition");
        gl.glBindAttribLocation(programId, 1, "aColor");

        gl.glLinkProgram(programId);

        // Check link status
        int[] linkStatus = new int[1];
        gl.glGetProgramiv(programId, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            int[] logLength = new int[1];
            gl.glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] logBytes = new byte[logLength[0]];
            gl.glGetProgramInfoLog(programId, logLength[0], null, 0, logBytes, 0);
            log.error("Shader program link failed: {}", new String(logBytes));

            gl.glDeleteProgram(programId);
            gl.glDeleteShader(vertexShaderId);
            gl.glDeleteShader(fragmentShaderId);
            programId = 0;
            vertexShaderId = 0;
            fragmentShaderId = 0;
            return false;
        }

        // Detach shaders after linking (they're no longer needed)
        gl.glDetachShader(programId, vertexShaderId);
        gl.glDetachShader(programId, fragmentShaderId);

        initialized = true;
        uniformLocations.clear();
        return true;
    }

    /**
     * Loads and compiles a shader program from resource files.
     *
     * @param gl the GL context
     * @param vertexResourcePath path to vertex shader resource (e.g., "/shaders/basic.vert")
     * @param fragmentResourcePath path to fragment shader resource
     * @return true if compilation succeeded
     */
    public boolean loadFromResources(GL2ES2 gl, String vertexResourcePath, String fragmentResourcePath) {
        String vertexSource = loadResource(vertexResourcePath);
        String fragmentSource = loadResource(fragmentResourcePath);

        if (vertexSource == null || fragmentSource == null) {
            return false;
        }

        return compile(gl, vertexSource, fragmentSource);
    }

    private String loadResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("Shader resource not found: {}", resourcePath);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("Failed to load shader resource: {}", resourcePath, e);
            return null;
        }
    }

    private int compileShader(GL2ES2 gl, int type, String source) {
        int shaderId = gl.glCreateShader(type);
        gl.glShaderSource(shaderId, 1, new String[]{source}, null);
        gl.glCompileShader(shaderId);

        // Check compilation status
        int[] compileStatus = new int[1];
        gl.glGetShaderiv(shaderId, GL2ES2.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            int[] logLength = new int[1];
            gl.glGetShaderiv(shaderId, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] logBytes = new byte[logLength[0]];
            gl.glGetShaderInfoLog(shaderId, logLength[0], null, 0, logBytes, 0);
            String shaderType = type == GL2ES2.GL_VERTEX_SHADER ? "vertex" : "fragment";
            log.error("{} shader compilation failed: {}", shaderType, new String(logBytes));

            gl.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    /**
     * Activates this shader program for rendering.
     */
    public void use(GL2ES2 gl) {
        gl.glUseProgram(programId);
    }

    /**
     * Deactivates shader programs.
     */
    public void unuse(GL2ES2 gl) {
        gl.glUseProgram(0);
    }

    /**
     * Gets the location of a uniform variable, caching the result.
     */
    public int getUniformLocation(GL2ES2 gl, String name) {
        return uniformLocations.computeIfAbsent(name, n -> gl.glGetUniformLocation(programId, n));
    }

    /**
     * Gets the location of an attribute variable.
     */
    public int getAttributeLocation(GL2ES2 gl, String name) {
        return gl.glGetAttribLocation(programId, name);
    }

    // ========== Uniform setters ==========

    public void setUniform1i(GL2ES2 gl, String name, int value) {
        gl.glUniform1i(getUniformLocation(gl, name), value);
    }

    public void setUniform1f(GL2ES2 gl, String name, float value) {
        gl.glUniform1f(getUniformLocation(gl, name), value);
    }

    public void setUniform2f(GL2ES2 gl, String name, float x, float y) {
        gl.glUniform2f(getUniformLocation(gl, name), x, y);
    }

    public void setUniform3f(GL2ES2 gl, String name, float x, float y, float z) {
        gl.glUniform3f(getUniformLocation(gl, name), x, y, z);
    }

    public void setUniform4f(GL2ES2 gl, String name, float x, float y, float z, float w) {
        gl.glUniform4f(getUniformLocation(gl, name), x, y, z, w);
    }

    public void setUniformMatrix4fv(GL2ES2 gl, String name, boolean transpose, FloatBuffer matrix) {
        gl.glUniformMatrix4fv(getUniformLocation(gl, name), 1, transpose, matrix);
    }

    public void setUniformMatrix4fv(GL2ES2 gl, String name, boolean transpose, float[] matrix) {
        gl.glUniformMatrix4fv(getUniformLocation(gl, name), 1, transpose, matrix, 0);
    }

    /**
     * Returns the program ID.
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * Returns true if the program has been successfully compiled.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Releases GPU resources.
     */
    public void dispose(GL2ES2 gl) {
        if (vertexShaderId != 0) {
            gl.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
        }
        if (fragmentShaderId != 0) {
            gl.glDeleteShader(fragmentShaderId);
            fragmentShaderId = 0;
        }
        if (programId != 0) {
            gl.glDeleteProgram(programId);
            programId = 0;
        }
        initialized = false;
        uniformLocations.clear();
    }
}
