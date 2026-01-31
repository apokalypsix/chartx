package com.apokalypsix.chartx.core.render.api;

import java.util.EnumMap;
import java.util.Map;

/**
 * Container for shader source code supporting multiple backends.
 *
 * <p>Allows defining shader code in both GLSL (for OpenGL) and SPIR-V
 * (for Vulkan) formats. The appropriate format is selected based on
 * the active rendering backend.
 */
public class ShaderSource {

    private final Map<ShaderStage, String> glslSources;
    private final Map<ShaderStage, byte[]> spirvSources;
    private final String name;

    private ShaderSource(Builder builder) {
        this.name = builder.name;
        this.glslSources = new EnumMap<>(builder.glslSources);
        this.spirvSources = new EnumMap<>(builder.spirvSources);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Creates a shader source from GLSL vertex and fragment shaders.
     */
    public static ShaderSource glsl(String name, String vertexSource, String fragmentSource) {
        return builder(name)
                .glsl(ShaderStage.VERTEX, vertexSource)
                .glsl(ShaderStage.FRAGMENT, fragmentSource)
                .build();
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the GLSL source for the given stage, or null if not available.
     */
    public String getGlslSource(ShaderStage stage) {
        return glslSources.get(stage);
    }

    /**
     * Returns the SPIR-V bytecode for the given stage, or null if not available.
     */
    public byte[] getSpirvSource(ShaderStage stage) {
        return spirvSources.get(stage);
    }

    /**
     * Returns true if GLSL sources are available.
     */
    public boolean hasGlsl() {
        return !glslSources.isEmpty();
    }

    /**
     * Returns true if SPIR-V sources are available.
     */
    public boolean hasSpirv() {
        return !spirvSources.isEmpty();
    }

    public static class Builder {
        private final String name;
        private final Map<ShaderStage, String> glslSources = new EnumMap<>(ShaderStage.class);
        private final Map<ShaderStage, byte[]> spirvSources = new EnumMap<>(ShaderStage.class);

        public Builder(String name) {
            this.name = name;
        }

        /**
         * Adds GLSL source for a shader stage.
         */
        public Builder glsl(ShaderStage stage, String source) {
            glslSources.put(stage, source);
            return this;
        }

        /**
         * Adds SPIR-V bytecode for a shader stage.
         */
        public Builder spirv(ShaderStage stage, byte[] bytecode) {
            spirvSources.put(stage, bytecode);
            return this;
        }

        public ShaderSource build() {
            if (glslSources.isEmpty() && spirvSources.isEmpty()) {
                throw new IllegalStateException("Shader source must have at least GLSL or SPIR-V sources");
            }
            return new ShaderSource(this);
        }
    }
}
