package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.ShaderSource;
import com.apokalypsix.chartx.core.render.api.ShaderStage;
import com.jogamp.opengl.GL2ES2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenGL implementation of the Shader interface.
 *
 * <p>Wraps shader compilation, linking, and uniform management using JOGL.
 */
public class GLShader implements Shader {

    private static final Logger log = LoggerFactory.getLogger(GLShader.class);

    private final GLRenderDevice device;
    private final String name;

    private int programId = 0;
    private int vertexShaderId = 0;
    private int fragmentShaderId = 0;
    private boolean valid = false;

    private final Map<String, Integer> uniformLocations = new HashMap<>();

    /**
     * Creates a new GLShader.
     *
     * @param device the render device
     * @param source the shader source
     */
    public GLShader(GLRenderDevice device, ShaderSource source) {
        this.device = device;
        this.name = source.getName();

        if (!source.hasGlsl()) {
            log.error("GLShader requires GLSL source: {}", name);
            return;
        }

        String vertexSource = source.getGlslSource(ShaderStage.VERTEX);
        String fragmentSource = source.getGlslSource(ShaderStage.FRAGMENT);

        if (vertexSource == null || fragmentSource == null) {
            log.error("GLShader requires both vertex and fragment shaders: {}", name);
            return;
        }

        compile(vertexSource, fragmentSource);
    }

    private void compile(String vertexSource, String fragmentSource) {
        GL2ES2 gl = device.getGL();

        // Compile vertex shader
        vertexShaderId = compileShader(gl, GL2ES2.GL_VERTEX_SHADER, vertexSource);
        if (vertexShaderId == 0) {
            return;
        }

        // Compile fragment shader
        fragmentShaderId = compileShader(gl, GL2ES2.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShaderId == 0) {
            gl.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
            return;
        }

        // Link program
        programId = gl.glCreateProgram();
        gl.glAttachShader(programId, vertexShaderId);
        gl.glAttachShader(programId, fragmentShaderId);

        // Bind standard attribute locations before linking
        gl.glBindAttribLocation(programId, 0, "aPosition");
        gl.glBindAttribLocation(programId, 1, "aColor");
        gl.glBindAttribLocation(programId, 2, "aTexCoord");

        gl.glLinkProgram(programId);

        // Check link status
        int[] linkStatus = new int[1];
        gl.glGetProgramiv(programId, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            int[] logLength = new int[1];
            gl.glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] logBytes = new byte[Math.max(1, logLength[0])];
            gl.glGetProgramInfoLog(programId, logLength[0], null, 0, logBytes, 0);
            log.error("Shader '{}' link failed: {}", name, new String(logBytes).trim());

            gl.glDeleteProgram(programId);
            gl.glDeleteShader(vertexShaderId);
            gl.glDeleteShader(fragmentShaderId);
            programId = 0;
            vertexShaderId = 0;
            fragmentShaderId = 0;
            return;
        }

        // Detach shaders after linking
        gl.glDetachShader(programId, vertexShaderId);
        gl.glDetachShader(programId, fragmentShaderId);

        valid = true;
        log.debug("Compiled shader: {}", name);
    }

    private int compileShader(GL2ES2 gl, int type, String source) {
        int shaderId = gl.glCreateShader(type);
        gl.glShaderSource(shaderId, 1, new String[]{source}, null);
        gl.glCompileShader(shaderId);

        int[] compileStatus = new int[1];
        gl.glGetShaderiv(shaderId, GL2ES2.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            int[] logLength = new int[1];
            gl.glGetShaderiv(shaderId, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] logBytes = new byte[Math.max(1, logLength[0])];
            gl.glGetShaderInfoLog(shaderId, logLength[0], null, 0, logBytes, 0);
            String shaderType = type == GL2ES2.GL_VERTEX_SHADER ? "vertex" : "fragment";
            log.error("Shader '{}' {} compilation failed: {}", name, shaderType, new String(logBytes).trim());

            gl.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    @Override
    public void bind() {
        if (valid) {
            device.getGL().glUseProgram(programId);
        }
    }

    @Override
    public void unbind() {
        device.getGL().glUseProgram(0);
    }

    @Override
    public void dispose() {
        GL2ES2 gl = device.getGL();

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
        valid = false;
        uniformLocations.clear();
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    private int getUniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name,
                n -> device.getGL().glGetUniformLocation(programId, n));
    }

    @Override
    public void setUniform(String name, int value) {
        device.getGL().glUniform1i(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(String name, float value) {
        device.getGL().glUniform1f(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(String name, float x, float y) {
        device.getGL().glUniform2f(getUniformLocation(name), x, y);
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        device.getGL().glUniform3f(getUniformLocation(name), x, y, z);
    }

    @Override
    public void setUniform(String name, float x, float y, float z, float w) {
        device.getGL().glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    @Override
    public void setUniformMatrix4(String name, float[] matrix) {
        setUniformMatrix4(name, matrix, false);
    }

    @Override
    public void setUniformMatrix4(String name, float[] matrix, boolean transpose) {
        device.getGL().glUniformMatrix4fv(getUniformLocation(name), 1, transpose, matrix, 0);
    }

    /**
     * Returns the OpenGL program ID.
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * Returns the attribute location for the given name.
     */
    public int getAttributeLocation(String name) {
        return device.getGL().glGetAttribLocation(programId, name);
    }
}
