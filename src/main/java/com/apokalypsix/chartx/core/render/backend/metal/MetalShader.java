package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.ShaderSource;
import com.apokalypsix.chartx.core.render.api.ShaderStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Metal shader implementation using MSL (Metal Shading Language).
 *
 * <p>Compiles MSL source code and extracts vertex/fragment functions.
 * Uniforms are managed via setVertexBytes/setFragmentBytes (push constants equivalent).
 */
public class MetalShader implements Shader {

    private static final Logger log = LoggerFactory.getLogger(MetalShader.class);

    // Uniform buffer layout: projection (64 bytes) + color (16 bytes) = 80 bytes = 20 floats
    private static final int UNIFORM_SIZE_FLOATS = 20;

    private final MetalRenderDevice device;
    private final String name;

    private long library;
    private long vertexFunction;
    private long fragmentFunction;

    // Uniform data
    private final Map<String, Object> uniforms = new HashMap<>();
    private float[] uniformBuffer = new float[UNIFORM_SIZE_FLOATS];
    private boolean uniformsDirty = true;

    private boolean valid = false;
    private boolean bound = false;
    private boolean hasTextureSampler = false;

    public MetalShader(MetalRenderDevice device, ShaderSource source) {
        this.device = device;
        this.name = source.getName();

        try {
            compile(source);
        } catch (Exception e) {
            log.error("Failed to compile shader '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Creates a shader from MSL source code directly.
     */
    public MetalShader(MetalRenderDevice device, String name, String mslSource) {
        this.device = device;
        this.name = name;

        try {
            compileFromMsl(mslSource);
        } catch (Exception e) {
            log.error("Failed to compile shader '{}': {}", name, e.getMessage());
        }
    }

    private void compile(ShaderSource source) {
        // For Metal, we need MSL source
        // We'll look for GLSL and convert patterns, or expect MSL directly
        String vertexGlsl = source.getGlslSource(ShaderStage.VERTEX);
        String fragmentGlsl = source.getGlslSource(ShaderStage.FRAGMENT);

        if (vertexGlsl == null || fragmentGlsl == null) {
            log.error("Metal shader requires shader source (GLSL provided, MSL expected)");
            log.info("Using default Metal shader for '{}'", name);
            // Fall back to built-in shaders based on name
            String mslSource = getMslForShaderName(name);
            if (mslSource != null) {
                compileFromMsl(mslSource);
            }
            return;
        }

        // Detect texture usage
        hasTextureSampler = fragmentGlsl.contains("sampler2D");

        // Generate MSL from shader name (we use pre-written MSL shaders)
        String mslSource = getMslForShaderName(name);
        if (mslSource != null) {
            compileFromMsl(mslSource);
        } else {
            log.error("No MSL source available for shader: {}", name);
        }
    }

    private void compileFromMsl(String mslSource) {
        if (!device.isInitialized()) {
            log.error("Cannot compile shader - device not initialized");
            return;
        }

        // Detect texture usage
        hasTextureSampler = mslSource.contains("texture2d<");

        // Compile MSL to library
        library = MetalNative.createLibraryFromSource(device.getDevice(), mslSource);
        if (library == 0) {
            log.error("Failed to compile MSL source for shader: {}", name);
            return;
        }

        // Get vertex function
        vertexFunction = MetalNative.createFunction(library, "vertexMain");
        if (vertexFunction == 0) {
            log.error("Failed to find vertex function 'vertexMain' in shader: {}", name);
            MetalNative.destroyLibrary(library);
            library = 0;
            return;
        }

        // Get fragment function
        fragmentFunction = MetalNative.createFunction(library, "fragmentMain");
        if (fragmentFunction == 0) {
            log.error("Failed to find fragment function 'fragmentMain' in shader: {}", name);
            MetalNative.destroyFunction(vertexFunction);
            MetalNative.destroyLibrary(library);
            library = 0;
            vertexFunction = 0;
            return;
        }

        valid = true;
        log.debug("Compiled Metal shader: {} (hasTexture={})", name, hasTextureSampler);
    }

    /**
     * Returns MSL source code for a known shader name.
     */
    private String getMslForShaderName(String shaderName) {
        return switch (shaderName) {
            case "default" -> MetalShaderRegistry.DEFAULT_SHADER;
            case "simple" -> MetalShaderRegistry.SIMPLE_SHADER;
            case "text" -> MetalShaderRegistry.TEXT_SHADER;
            default -> {
                log.warn("Unknown shader name: {}, using default", shaderName);
                yield MetalShaderRegistry.DEFAULT_SHADER;
            }
        };
    }

    @Override
    public void bind() {
        if (!valid) return;

        bound = true;
        device.setCurrentShader(this);
        uniformsDirty = true;
    }

    @Override
    public void unbind() {
        bound = false;
        if (device.getCurrentShader() == this) {
            device.setCurrentShader(null);
        }
    }

    /**
     * Flushes uniform data to the render encoder.
     * Call this before drawing if uniforms have changed.
     */
    public void flushUniforms(long encoder) {
        if (!bound || encoder == 0) return;

        if (uniformsDirty) {
            updateUniformBuffer();
            uniformsDirty = false;
        }

        // Upload uniforms via setVertexBytes (buffer index 1)
        MetalNative.setVertexBytes(encoder, uniformBuffer, UNIFORM_SIZE_FLOATS, 1);

        // Also set for fragment shader (for color access in Simple shader)
        MetalNative.setFragmentBytes(encoder, uniformBuffer, UNIFORM_SIZE_FLOATS, 1);
    }

    private void updateUniformBuffer() {
        // Layout: mat4 projection (16 floats) + vec4 color (4 floats)

        // Write projection matrix (offset 0)
        Object projObj = uniforms.get("uProjection");
        if (projObj instanceof float[] proj && proj.length == 16) {
            System.arraycopy(proj, 0, uniformBuffer, 0, 16);
        } else {
            // Identity matrix
            for (int i = 0; i < 16; i++) {
                uniformBuffer[i] = (i % 5 == 0) ? 1.0f : 0.0f;
            }
        }

        // Write color (offset 16)
        Object colorObj = uniforms.get("uColor");
        if (colorObj instanceof float[] color && color.length == 4) {
            System.arraycopy(color, 0, uniformBuffer, 16, 4);
        } else {
            uniformBuffer[16] = 1.0f; // r
            uniformBuffer[17] = 1.0f; // g
            uniformBuffer[18] = 1.0f; // b
            uniformBuffer[19] = 1.0f; // a
        }
    }

    @Override
    public void dispose() {
        if (fragmentFunction != 0) {
            MetalNative.destroyFunction(fragmentFunction);
            fragmentFunction = 0;
        }

        if (vertexFunction != 0) {
            MetalNative.destroyFunction(vertexFunction);
            vertexFunction = 0;
        }

        if (library != 0) {
            MetalNative.destroyLibrary(library);
            library = 0;
        }

        uniforms.clear();
        valid = false;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void setUniform(String name, int value) {
        uniforms.put(name, value);
        uniformsDirty = true;
    }

    @Override
    public void setUniform(String name, float value) {
        uniforms.put(name, value);
        uniformsDirty = true;
    }

    @Override
    public void setUniform(String name, float x, float y) {
        uniforms.put(name, new float[]{x, y});
        uniformsDirty = true;
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        uniforms.put(name, new float[]{x, y, z});
        uniformsDirty = true;
    }

    @Override
    public void setUniform(String name, float x, float y, float z, float w) {
        uniforms.put(name, new float[]{x, y, z, w});
        uniformsDirty = true;
    }

    @Override
    public void setUniformMatrix4(String name, float[] matrix) {
        setUniformMatrix4(name, matrix, false);
    }

    @Override
    public void setUniformMatrix4(String name, float[] matrix, boolean transpose) {
        if (transpose) {
            float[] transposed = new float[16];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    transposed[j * 4 + i] = matrix[i * 4 + j];
                }
            }
            uniforms.put(name, transposed);
        } else {
            uniforms.put(name, matrix.clone());
        }
        uniformsDirty = true;
    }

    // -------------------------------------------------------------------------
    // Metal-specific getters
    // -------------------------------------------------------------------------

    /**
     * Returns the Metal library handle.
     */
    public long getLibrary() {
        return library;
    }

    /**
     * Returns the vertex function handle.
     */
    public long getVertexFunction() {
        return vertexFunction;
    }

    /**
     * Returns the fragment function handle.
     */
    public long getFragmentFunction() {
        return fragmentFunction;
    }

    /**
     * Returns the shader name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if this shader uses texture samplers.
     */
    public boolean hasTextureSampler() {
        return hasTextureSampler;
    }

    /**
     * Returns true if this shader is currently bound.
     */
    public boolean isBound() {
        return bound;
    }
}
