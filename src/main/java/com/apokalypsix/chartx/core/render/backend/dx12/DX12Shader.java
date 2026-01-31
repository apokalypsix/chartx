package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.Shader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DirectX 12 implementation of Shader.
 *
 * <p>Compiles HLSL shaders to bytecode and manages uniform data
 * via root constants.
 */
public class DX12Shader implements Shader {

    private static final Logger log = LoggerFactory.getLogger(DX12Shader.class);

    private final String name;
    private byte[] vertexBytecode;
    private byte[] pixelBytecode;
    private boolean valid;

    // Uniform buffer (20 floats: mat4 projection + vec4 color)
    private final float[] uniformBuffer = new float[20];
    private boolean uniformsDirty;

    /**
     * Creates a shader from HLSL source.
     *
     * @param name shader name
     * @param hlslSource combined HLSL source with VSMain and PSMain entry points
     */
    public DX12Shader(String name, String hlslSource) {
        this.name = name;
        compile(hlslSource);
    }

    private void compile(String hlslSource) {
        // Compile vertex shader
        vertexBytecode = DX12Native.compileShader(hlslSource, "VSMain", "vs_5_0");
        if (vertexBytecode == null) {
            log.error("Failed to compile vertex shader: {}", name);
            return;
        }

        // Compile pixel shader
        pixelBytecode = DX12Native.compileShader(hlslSource, "PSMain", "ps_5_0");
        if (pixelBytecode == null) {
            log.error("Failed to compile pixel shader: {}", name);
            return;
        }

        valid = true;
        log.debug("Compiled DX12 shader: {}", name);
    }

    @Override
    public void bind() {
        // Pipeline state handles shader binding in DX12
        // Uniforms are flushed separately via flushUniforms()
    }

    @Override
    public void unbind() {
        // No-op for DX12
    }

    @Override
    public void dispose() {
        vertexBytecode = null;
        pixelBytecode = null;
        valid = false;
        log.debug("Disposed DX12 shader: {}", name);
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void setUniform(String name, int value) {
        // Integer uniforms not directly supported in our simple uniform buffer
        // Could extend the uniform buffer if needed
    }

    @Override
    public void setUniform(String name, float value) {
        if ("uniformColor".equals(name)) {
            uniformBuffer[16] = value;
            uniformBuffer[17] = value;
            uniformBuffer[18] = value;
            uniformBuffer[19] = 1.0f;
            uniformsDirty = true;
        }
    }

    @Override
    public void setUniform(String name, float x, float y) {
        // vec2 uniforms - store in color position if needed
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        if ("uniformColor".equals(name)) {
            uniformBuffer[16] = x;
            uniformBuffer[17] = y;
            uniformBuffer[18] = z;
            uniformBuffer[19] = 1.0f;
            uniformsDirty = true;
        }
    }

    @Override
    public void setUniform(String name, float x, float y, float z, float w) {
        if ("uniformColor".equals(name)) {
            uniformBuffer[16] = x;
            uniformBuffer[17] = y;
            uniformBuffer[18] = z;
            uniformBuffer[19] = w;
            uniformsDirty = true;
        }
    }

    @Override
    public void setUniformMatrix4(String name, float[] matrix) {
        setUniformMatrix4(name, matrix, false);
    }

    @Override
    public void setUniformMatrix4(String name, float[] matrix, boolean transpose) {
        if ("projection".equals(name) && matrix.length >= 16) {
            if (transpose) {
                // Transpose the matrix
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        uniformBuffer[i * 4 + j] = matrix[j * 4 + i];
                    }
                }
            } else {
                System.arraycopy(matrix, 0, uniformBuffer, 0, 16);
            }
            uniformsDirty = true;
        }
    }

    /**
     * Flushes uniform data to the GPU via root constants.
     *
     * @param commandList the command list handle
     */
    void flushUniforms(long commandList) {
        if (uniformsDirty && commandList != 0) {
            DX12Native.setGraphicsRoot32BitConstants(commandList, uniformBuffer, 20, 0);
            uniformsDirty = false;
        }
    }

    /**
     * Gets the shader name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the compiled vertex shader bytecode.
     */
    byte[] getVertexBytecode() {
        return vertexBytecode;
    }

    /**
     * Gets the compiled pixel shader bytecode.
     */
    byte[] getPixelBytecode() {
        return pixelBytecode;
    }
}
