package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.VertexAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Manages Metal render pipeline state.
 *
 * <p>Each pipeline is configured for a specific combination of:
 * <ul>
 *   <li>Shader program</li>
 *   <li>Vertex format</li>
 *   <li>Primitive topology (triangles, lines, etc.)</li>
 *   <li>Blend mode</li>
 * </ul>
 */
public class MetalPipeline {

    private static final Logger log = LoggerFactory.getLogger(MetalPipeline.class);

    // Metal pixel format for BGRA8
    private static final int MTL_PIXEL_FORMAT_BGRA8_UNORM = 80;

    // Metal vertex format constants
    private static final int MTL_VERTEX_FORMAT_FLOAT = 28;
    private static final int MTL_VERTEX_FORMAT_FLOAT2 = 29;
    private static final int MTL_VERTEX_FORMAT_FLOAT3 = 30;
    private static final int MTL_VERTEX_FORMAT_FLOAT4 = 31;

    private final MetalRenderDevice device;

    private long pipelineState;
    private MetalShader shader;
    private boolean disposed = false;

    public MetalPipeline(MetalRenderDevice device) {
        this.device = device;
    }

    /**
     * Creates a render pipeline state.
     *
     * @param shader           the shader to use
     * @param bufferDescriptor vertex buffer format
     * @param drawMode         primitive topology
     * @param blendMode        blend mode
     */
    public void create(MetalShader shader, BufferDescriptor bufferDescriptor,
                       DrawMode drawMode, BlendMode blendMode) {
        this.shader = shader;

        if (!device.isInitialized()) {
            log.error("Cannot create pipeline - device not initialized");
            return;
        }

        if (!shader.isValid()) {
            log.error("Cannot create pipeline - shader is invalid");
            return;
        }

        List<VertexAttribute> attributes = bufferDescriptor.getAttributes();

        // Build attribute formats and offsets arrays
        int[] formats = new int[attributes.size()];
        int[] offsets = new int[attributes.size()];

        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute attr = attributes.get(i);
            formats[i] = toMetalFormat(attr.getComponents());
            offsets[i] = attr.getOffset();
        }

        int stride = bufferDescriptor.getStrideInBytes();
        int blendModeInt = toMetalBlendMode(blendMode);

        // Create pipeline state
        pipelineState = MetalNative.createPipelineState(
                device.getDevice(),
                shader.getVertexFunction(),
                shader.getFragmentFunction(),
                MTL_PIXEL_FORMAT_BGRA8_UNORM,
                blendModeInt,
                formats,
                offsets,
                stride
        );

        if (pipelineState == 0) {
            log.error("Failed to create Metal pipeline state for {} with {}",
                    drawMode, blendMode);
            return;
        }

        log.debug("Created Metal pipeline for {} with {}", drawMode, blendMode);
    }

    /**
     * Converts vertex component count to Metal format.
     */
    private int toMetalFormat(int components) {
        return switch (components) {
            case 1 -> MTL_VERTEX_FORMAT_FLOAT;
            case 2 -> MTL_VERTEX_FORMAT_FLOAT2;
            case 3 -> MTL_VERTEX_FORMAT_FLOAT3;
            case 4 -> MTL_VERTEX_FORMAT_FLOAT4;
            default -> MTL_VERTEX_FORMAT_FLOAT4;
        };
    }

    /**
     * Converts blend mode to Metal constant.
     */
    private int toMetalBlendMode(BlendMode mode) {
        return switch (mode) {
            case NONE -> 0;
            case ALPHA -> 1;
            case ADDITIVE -> 2;
            case MULTIPLY -> 3;
            case PREMULTIPLIED_ALPHA -> 4;
        };
    }

    /**
     * Binds this pipeline to the render encoder.
     */
    public void bind(long encoder) {
        if (pipelineState == 0 || encoder == 0) return;
        MetalNative.setRenderPipelineState(encoder, pipelineState);
    }

    /**
     * Returns the shader used by this pipeline.
     */
    public MetalShader getShader() {
        return shader;
    }

    /**
     * Returns the pipeline state handle.
     */
    public long getPipelineState() {
        return pipelineState;
    }

    /**
     * Returns true if this pipeline is valid and ready to use.
     */
    public boolean isValid() {
        return pipelineState != 0 && !disposed;
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        if (pipelineState != 0) {
            MetalNative.destroyPipelineState(pipelineState);
            pipelineState = 0;
        }

        log.debug("Disposed Metal pipeline");
    }
}
