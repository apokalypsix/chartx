package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.ShaderSource;
import com.apokalypsix.chartx.core.render.api.ShaderStage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan shader implementation using SPIR-V.
 *
 * <p>Supports both pre-compiled SPIR-V and runtime compilation from GLSL
 * using the shaderc library.
 */
public class VkShader implements Shader {

    private static final Logger log = LoggerFactory.getLogger(VkShader.class);

    private final VkRenderDevice device;
    private final String name;

    private long vertexModule = VK_NULL_HANDLE;
    private long fragmentModule = VK_NULL_HANDLE;
    private long pipelineLayout = VK_NULL_HANDLE;
    private long pipeline = VK_NULL_HANDLE;
    private long descriptorSetLayout = VK_NULL_HANDLE;

    // Uniform data (push constants for simplicity)
    private final Map<String, Object> uniforms = new HashMap<>();
    private ByteBuffer pushConstantBuffer;
    private static final int PUSH_CONSTANT_SIZE = 256; // bytes

    private boolean valid = false;
    private boolean bound = false;
    private boolean uniformsDirty = false;
    private boolean hasTextureSampler = false;

    public VkShader(VkRenderDevice device, ShaderSource source) {
        this.device = device;
        this.name = source.getName();

        try {
            compile(source);
        } catch (Exception e) {
            log.error("Failed to compile shader '{}': {}", name, e.getMessage());
        }
    }

    private void compile(ShaderSource source) {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) {
            log.error("Cannot compile shader - device not initialized");
            return;
        }

        // Detect if this shader uses texture samplers (e.g., text shader)
        String fragmentGlsl = source.getGlslSource(ShaderStage.FRAGMENT);
        hasTextureSampler = fragmentGlsl != null && fragmentGlsl.contains("sampler2D");

        try (MemoryStack stack = stackPush()) {
            // Get or compile SPIR-V for vertex shader
            ByteBuffer vertexSpirv = getOrCompileSpirv(source, ShaderStage.VERTEX, stack);
            if (vertexSpirv == null) {
                log.error("Failed to get vertex shader SPIR-V");
                return;
            }

            // Get or compile SPIR-V for fragment shader
            ByteBuffer fragmentSpirv = getOrCompileSpirv(source, ShaderStage.FRAGMENT, stack);
            if (fragmentSpirv == null) {
                log.error("Failed to get fragment shader SPIR-V");
                return;
            }

            // Create shader modules
            vertexModule = createShaderModule(vkDevice, vertexSpirv, stack);
            fragmentModule = createShaderModule(vkDevice, fragmentSpirv, stack);

            if (vertexModule == VK_NULL_HANDLE || fragmentModule == VK_NULL_HANDLE) {
                log.error("Failed to create shader modules");
                return;
            }

            // Create descriptor set layout if shader uses textures
            if (hasTextureSampler) {
                createDescriptorSetLayout(vkDevice, stack);
            }

            // Create pipeline layout with push constants
            createPipelineLayout(vkDevice, stack);

            // Note: Pipeline creation requires render pass info which is set up later
            // For now, we just prepare the shader modules

            valid = true;
            log.debug("Compiled shader: {} (hasTexture={})", name, hasTextureSampler);

        } catch (Exception e) {
            log.error("Shader compilation failed", e);
            dispose();
        }
    }

    private ByteBuffer getOrCompileSpirv(ShaderSource source, ShaderStage stage, MemoryStack stack) {
        // First try to get pre-compiled SPIR-V
        byte[] spirv = source.getSpirvSource(stage);
        if (spirv != null) {
            ByteBuffer buffer = stack.malloc(spirv.length);
            buffer.put(spirv).flip();
            return buffer;
        }

        // Fall back to runtime compilation from GLSL
        String glsl = source.getGlslSource(stage);
        if (glsl == null) {
            log.error("No GLSL or SPIR-V source for {} stage", stage);
            return null;
        }

        return compileGlslToSpirv(glsl, stage, stack);
    }

    private ByteBuffer compileGlslToSpirv(String glsl, ShaderStage stage, MemoryStack stack) {
        long compiler = shaderc_compiler_initialize();
        if (compiler == 0) {
            log.error("Failed to initialize shaderc compiler");
            return null;
        }

        try {
            long options = shaderc_compile_options_initialize();
            shaderc_compile_options_set_target_env(options,
                    shaderc_target_env_vulkan,
                    shaderc_env_version_vulkan_1_1);
            shaderc_compile_options_set_optimization_level(options,
                    shaderc_optimization_level_performance);

            int shaderKind = switch (stage) {
                case VERTEX -> shaderc_vertex_shader;
                case FRAGMENT -> shaderc_fragment_shader;
                case GEOMETRY -> shaderc_geometry_shader;
                case COMPUTE -> shaderc_compute_shader;
            };

            long result = shaderc_compile_into_spv(
                    compiler,
                    glsl,
                    shaderKind,
                    name + "." + stage.name().toLowerCase(),
                    "main",
                    options
            );

            shaderc_compile_options_release(options);

            if (result == 0) {
                log.error("shaderc compilation returned null");
                return null;
            }

            int status = shaderc_result_get_compilation_status(result);
            if (status != shaderc_compilation_status_success) {
                String errorMsg = shaderc_result_get_error_message(result);
                log.error("Shader compilation failed: {}", errorMsg);
                shaderc_result_release(result);
                return null;
            }

            // Get compiled SPIR-V
            ByteBuffer spirvBytes = shaderc_result_get_bytes(result);
            if (spirvBytes == null) {
                log.error("Failed to get compiled SPIR-V bytes");
                shaderc_result_release(result);
                return null;
            }

            // Copy to stack buffer (shaderc buffer may be freed)
            ByteBuffer copy = stack.malloc(spirvBytes.remaining());
            copy.put(spirvBytes).flip();

            shaderc_result_release(result);

            log.debug("Compiled {} shader to SPIR-V ({} bytes)", stage, copy.remaining());
            return copy;

        } finally {
            shaderc_compiler_release(compiler);
        }
    }

    private long createShaderModule(VkDevice device, ByteBuffer spirv, MemoryStack stack) {
        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(spirv);

        LongBuffer pShaderModule = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateShaderModule(device, createInfo, null, pShaderModule);

        if (result != VK_SUCCESS) {
            log.error("Failed to create shader module: {}", result);
            return VK_NULL_HANDLE;
        }

        return pShaderModule.get(0);
    }

    private void createDescriptorSetLayout(VkDevice vkDevice, MemoryStack stack) {
        // Create descriptor set layout for sampler2D at binding 0
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
        bindings.get(0)
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .pImmutableSamplers(null);

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(bindings);

        LongBuffer pDescriptorSetLayout = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, pDescriptorSetLayout);

        if (result != VK_SUCCESS) {
            log.error("Failed to create descriptor set layout: {}", result);
            return;
        }

        descriptorSetLayout = pDescriptorSetLayout.get(0);
        log.debug("Created descriptor set layout for texture sampler");
    }

    private void createPipelineLayout(VkDevice vkDevice, MemoryStack stack) {
        // Push constant range for uniforms (projection matrix, color, etc.)
        VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(PUSH_CONSTANT_SIZE);

        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pPushConstantRanges(pushConstantRanges);

        // Include descriptor set layout if shader uses textures
        if (descriptorSetLayout != VK_NULL_HANDLE) {
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));
        }

        LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
        int result = vkCreatePipelineLayout(vkDevice, pipelineLayoutInfo, null, pPipelineLayout);

        if (result != VK_SUCCESS) {
            log.error("Failed to create pipeline layout: {}", result);
            return;
        }

        pipelineLayout = pPipelineLayout.get(0);

        // Allocate push constant buffer
        pushConstantBuffer = org.lwjgl.system.MemoryUtil.memAlloc(PUSH_CONSTANT_SIZE);
    }

    @Override
    public void bind() {
        if (!valid) return;

        bound = true;

        // Register this shader as the currently bound shader
        device.setCurrentShader(this);

        // If we have a pipeline set externally, bind it
        if (pipeline != VK_NULL_HANDLE) {
            VkCommandBuffer cmd = device.getCommandBuffer();
            if (cmd != null) {
                vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
            }
        }

        // Push constants will be updated when uniforms are set or when flushPushConstants is called
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
     * Flushes any pending push constant updates to the command buffer.
     * Call this before drawing if uniforms have changed.
     */
    public void flushPushConstants() {
        if (!bound || !uniformsDirty) return;
        updatePushConstants();
        uniformsDirty = false;
    }

    /**
     * Returns true if this shader is currently bound.
     */
    public boolean isBound() {
        return bound;
    }

    @Override
    public void dispose() {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(vkDevice, pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }

        if (pipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(vkDevice, pipelineLayout, null);
            pipelineLayout = VK_NULL_HANDLE;
        }

        if (descriptorSetLayout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(vkDevice, descriptorSetLayout, null);
            descriptorSetLayout = VK_NULL_HANDLE;
        }

        if (vertexModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(vkDevice, vertexModule, null);
            vertexModule = VK_NULL_HANDLE;
        }

        if (fragmentModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(vkDevice, fragmentModule, null);
            fragmentModule = VK_NULL_HANDLE;
        }

        if (pushConstantBuffer != null) {
            org.lwjgl.system.MemoryUtil.memFree(pushConstantBuffer);
            pushConstantBuffer = null;
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
            // Transpose the matrix
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

    private void updatePushConstants() {
        if (pushConstantBuffer == null || pipelineLayout == VK_NULL_HANDLE) return;

        VkCommandBuffer cmd = device.getCommandBuffer();
        if (cmd == null) return;

        // Layout: mat4 projection (64 bytes), vec4 color (16 bytes)
        pushConstantBuffer.clear();

        // Write projection matrix
        Object projObj = uniforms.get("uProjection");
        if (projObj instanceof float[] proj && proj.length == 16) {
            for (float v : proj) {
                pushConstantBuffer.putFloat(v);
            }
        } else {
            // Identity matrix
            for (int i = 0; i < 16; i++) {
                pushConstantBuffer.putFloat(i % 5 == 0 ? 1.0f : 0.0f);
            }
        }

        // Write color
        Object colorObj = uniforms.get("uColor");
        if (colorObj instanceof float[] color && color.length == 4) {
            for (float v : color) {
                pushConstantBuffer.putFloat(v);
            }
        } else {
            pushConstantBuffer.putFloat(1.0f); // r
            pushConstantBuffer.putFloat(1.0f); // g
            pushConstantBuffer.putFloat(1.0f); // b
            pushConstantBuffer.putFloat(1.0f); // a
        }

        pushConstantBuffer.flip();

        // Upload push constants
        vkCmdPushConstants(cmd, pipelineLayout,
                VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                0, pushConstantBuffer);
    }

    // -------------------------------------------------------------------------
    // Vulkan-specific getters
    // -------------------------------------------------------------------------

    public long getVertexModule() {
        return vertexModule;
    }

    public long getFragmentModule() {
        return fragmentModule;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public long getPipeline() {
        return pipeline;
    }

    public void setPipeline(long pipeline) {
        this.pipeline = pipeline;
    }

    public String getName() {
        return name;
    }

    public long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public boolean hasTextureSampler() {
        return hasTextureSampler;
    }
}
