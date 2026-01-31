package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.VertexAttribute;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Manages Vulkan graphics pipelines.
 *
 * <p>Each pipeline is configured for a specific combination of:
 * <ul>
 *   <li>Shader program</li>
 *   <li>Vertex format</li>
 *   <li>Primitive topology (triangles, lines, etc.)</li>
 *   <li>Blend mode</li>
 * </ul>
 */
public class VkPipeline {

    private static final Logger log = LoggerFactory.getLogger(VkPipeline.class);

    private final VkRenderDevice device;

    private long pipeline = VK_NULL_HANDLE;
    private long pipelineLayout = VK_NULL_HANDLE;
    private VkShader shader;

    private boolean disposed = false;

    public VkPipeline(VkRenderDevice device) {
        this.device = device;
    }

    /**
     * Creates a graphics pipeline.
     *
     * @param shader the shader to use
     * @param bufferDescriptor vertex buffer format
     * @param renderPass the render pass
     * @param drawMode primitive topology
     * @param blendMode blend mode
     */
    public void create(VkShader shader, BufferDescriptor bufferDescriptor,
                       long renderPass, DrawMode drawMode, BlendMode blendMode) {
        this.shader = shader;
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        try (MemoryStack stack = stackPush()) {
            // Shader stages
            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.calloc(2, stack);

            shaderStages.get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(shader.getVertexModule())
                    .pName(stack.UTF8("main"));

            shaderStages.get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(shader.getFragmentModule())
                    .pName(stack.UTF8("main"));

            // Vertex input
            VkPipelineVertexInputStateCreateInfo vertexInputInfo =
                    createVertexInputState(bufferDescriptor, stack);

            // Input assembly
            VkPipelineInputAssemblyStateCreateInfo inputAssembly =
                    VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                            .topology(toVkTopology(drawMode))
                            .primitiveRestartEnable(false);

            // Viewport state (dynamic)
            VkPipelineViewportStateCreateInfo viewportState =
                    VkPipelineViewportStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                            .viewportCount(1)
                            .scissorCount(1);

            // Rasterization
            VkPipelineRasterizationStateCreateInfo rasterizer =
                    VkPipelineRasterizationStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                            .depthClampEnable(false)
                            .rasterizerDiscardEnable(false)
                            .polygonMode(VK_POLYGON_MODE_FILL)
                            .lineWidth(1.0f)
                            .cullMode(VK_CULL_MODE_NONE)
                            .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                            .depthBiasEnable(false);

            // Multisampling
            VkPipelineMultisampleStateCreateInfo multisampling =
                    VkPipelineMultisampleStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                            .sampleShadingEnable(false)
                            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // Color blending
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack);
            configureBlending(colorBlendAttachment.get(0), blendMode);

            VkPipelineColorBlendStateCreateInfo colorBlending =
                    VkPipelineColorBlendStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                            .logicOpEnable(false)
                            .pAttachments(colorBlendAttachment);

            // Dynamic state
            var dynamicStates = stack.ints(
                    VK_DYNAMIC_STATE_VIEWPORT,
                    VK_DYNAMIC_STATE_SCISSOR,
                    VK_DYNAMIC_STATE_LINE_WIDTH
            );

            VkPipelineDynamicStateCreateInfo dynamicState =
                    VkPipelineDynamicStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                            .pDynamicStates(dynamicStates);

            // Pipeline layout (use shader's layout)
            pipelineLayout = shader.getPipelineLayout();

            // Depth stencil (disabled for 2D)
            VkPipelineDepthStencilStateCreateInfo depthStencil =
                    VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                            .depthTestEnable(false)
                            .depthWriteEnable(false)
                            .depthCompareOp(VK_COMPARE_OP_LESS)
                            .depthBoundsTestEnable(false)
                            .stencilTestEnable(false);

            // Create pipeline
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                    VkGraphicsPipelineCreateInfo.calloc(1, stack)
                            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                            .pStages(shaderStages)
                            .pVertexInputState(vertexInputInfo)
                            .pInputAssemblyState(inputAssembly)
                            .pViewportState(viewportState)
                            .pRasterizationState(rasterizer)
                            .pMultisampleState(multisampling)
                            .pDepthStencilState(depthStencil)
                            .pColorBlendState(colorBlending)
                            .pDynamicState(dynamicState)
                            .layout(pipelineLayout)
                            .renderPass(renderPass)
                            .subpass(0)
                            .basePipelineHandle(VK_NULL_HANDLE)
                            .basePipelineIndex(-1);

            LongBuffer pPipeline = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE,
                    pipelineInfo, null, pPipeline);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline: " + result);
            }

            pipeline = pPipeline.get(0);
            log.debug("Created graphics pipeline for {} with {}", drawMode, blendMode);
        }
    }

    private VkPipelineVertexInputStateCreateInfo createVertexInputState(
            BufferDescriptor descriptor, MemoryStack stack) {

        List<VertexAttribute> attributes = descriptor.getAttributes();

        // Binding description
        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.calloc(1, stack)
                        .binding(0)
                        .stride(descriptor.getStrideInBytes())
                        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        // Attribute descriptions
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.calloc(attributes.size(), stack);

        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute attr = attributes.get(i);
            attributeDescriptions.get(i)
                    .binding(0)
                    .location(i)
                    .format(toVkFormat(attr.getComponents()))
                    .offset(attr.getOffset());
        }

        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(bindingDescription)
                .pVertexAttributeDescriptions(attributeDescriptions);
    }

    private void configureBlending(VkPipelineColorBlendAttachmentState attachment, BlendMode mode) {
        attachment.colorWriteMask(
                VK_COLOR_COMPONENT_R_BIT |
                VK_COLOR_COMPONENT_G_BIT |
                VK_COLOR_COMPONENT_B_BIT |
                VK_COLOR_COMPONENT_A_BIT
        );

        switch (mode) {
            case NONE -> attachment.blendEnable(false);
            case ALPHA -> {
                attachment.blendEnable(true);
                attachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                attachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                attachment.colorBlendOp(VK_BLEND_OP_ADD);
                attachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                attachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                attachment.alphaBlendOp(VK_BLEND_OP_ADD);
            }
            case ADDITIVE -> {
                attachment.blendEnable(true);
                attachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                attachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE);
                attachment.colorBlendOp(VK_BLEND_OP_ADD);
                attachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                attachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                attachment.alphaBlendOp(VK_BLEND_OP_ADD);
            }
            case MULTIPLY -> {
                attachment.blendEnable(true);
                attachment.srcColorBlendFactor(VK_BLEND_FACTOR_DST_COLOR);
                attachment.dstColorBlendFactor(VK_BLEND_FACTOR_ZERO);
                attachment.colorBlendOp(VK_BLEND_OP_ADD);
                attachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_DST_ALPHA);
                attachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
                attachment.alphaBlendOp(VK_BLEND_OP_ADD);
            }
        }
    }

    private int toVkTopology(DrawMode mode) {
        return switch (mode) {
            case TRIANGLES -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
            case LINES -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case LINE_STRIP, LINE_LOOP -> VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
            case POINTS -> VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
        };
    }

    private int toVkFormat(int components) {
        return switch (components) {
            case 1 -> VK_FORMAT_R32_SFLOAT;
            case 2 -> VK_FORMAT_R32G32_SFLOAT;
            case 3 -> VK_FORMAT_R32G32B32_SFLOAT;
            case 4 -> VK_FORMAT_R32G32B32A32_SFLOAT;
            default -> VK_FORMAT_R32G32B32A32_SFLOAT;
        };
    }

    /**
     * Binds this pipeline to the command buffer.
     */
    public void bind(VkCommandBuffer cmd) {
        if (pipeline == VK_NULL_HANDLE) return;
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
    }

    /**
     * Binds this pipeline and sets push constants for the projection matrix.
     * Uses an identity matrix by default.
     */
    public void bindWithPushConstants(VkCommandBuffer cmd) {
        bind(cmd);
        if (pipelineLayout != VK_NULL_HANDLE) {
            try (MemoryStack stack = stackPush()) {
                // Push constants: mat4 projection (64 bytes) + vec4 color (16 bytes) = 80 bytes
                var pushConstants = stack.malloc(80);

                // Identity matrix for NDC coordinates
                pushConstants.putFloat(1.0f).putFloat(0.0f).putFloat(0.0f).putFloat(0.0f);
                pushConstants.putFloat(0.0f).putFloat(1.0f).putFloat(0.0f).putFloat(0.0f);
                pushConstants.putFloat(0.0f).putFloat(0.0f).putFloat(1.0f).putFloat(0.0f);
                pushConstants.putFloat(0.0f).putFloat(0.0f).putFloat(0.0f).putFloat(1.0f);

                // White color
                pushConstants.putFloat(1.0f).putFloat(1.0f).putFloat(1.0f).putFloat(1.0f);

                pushConstants.flip();
                vkCmdPushConstants(cmd, pipelineLayout,
                        VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                        0, pushConstants);
            }
        }
    }

    /**
     * Sets a custom projection matrix for this pipeline.
     * Call this after bind() to set push constants.
     */
    public void setProjectionMatrix(VkCommandBuffer cmd, float[] matrix) {
        if (pipelineLayout == VK_NULL_HANDLE || matrix == null || matrix.length != 16) return;

        try (MemoryStack stack = stackPush()) {
            var pushConstants = stack.malloc(80);

            // Projection matrix
            for (float v : matrix) {
                pushConstants.putFloat(v);
            }

            // White color
            pushConstants.putFloat(1.0f).putFloat(1.0f).putFloat(1.0f).putFloat(1.0f);

            pushConstants.flip();
            vkCmdPushConstants(cmd, pipelineLayout,
                    VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                    0, pushConstants);
        }
    }

    /**
     * Returns the shader used by this pipeline.
     */
    public VkShader getShader() {
        return shader;
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(vkDevice, pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }

        // Note: pipelineLayout is owned by VkShader, not destroyed here

        log.debug("Disposed pipeline");
    }

    public long getPipeline() {
        return pipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }
}
