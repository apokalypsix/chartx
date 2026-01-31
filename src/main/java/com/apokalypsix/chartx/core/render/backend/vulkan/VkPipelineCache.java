package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches Vulkan graphics pipelines for reuse.
 *
 * <p>Pipelines in Vulkan are immutable and tied to specific combinations of:
 * <ul>
 *   <li>Shader program</li>
 *   <li>Vertex input format</li>
 *   <li>Primitive topology (draw mode)</li>
 *   <li>Blend mode</li>
 * </ul>
 *
 * <p>This cache creates pipelines on-demand and reuses them for subsequent draws
 * with the same configuration.
 */
public class VkPipelineCache {

    private static final Logger log = LoggerFactory.getLogger(VkPipelineCache.class);

    private final VkRenderDevice device;
    private final Map<PipelineKey, VkPipeline> pipelines = new ConcurrentHashMap<>();

    public VkPipelineCache(VkRenderDevice device) {
        this.device = device;
    }

    /**
     * Gets or creates a pipeline for the given configuration.
     *
     * @param shader the shader to use
     * @param descriptor vertex buffer descriptor
     * @param drawMode primitive topology
     * @param blendMode blend mode
     * @return the pipeline
     */
    public VkPipeline getPipeline(VkShader shader, BufferDescriptor descriptor,
                                   DrawMode drawMode, BlendMode blendMode) {
        PipelineKey key = new PipelineKey(shader.getName(), descriptor.hashCode(), drawMode, blendMode);

        return pipelines.computeIfAbsent(key, k -> {
            log.debug("Creating pipeline for shader={}, drawMode={}, blendMode={}",
                    shader.getName(), drawMode, blendMode);

            VkPipeline pipeline = new VkPipeline(device);
            pipeline.create(shader, descriptor, device.getRenderPass(), drawMode, blendMode);
            return pipeline;
        });
    }

    /**
     * Disposes all cached pipelines.
     */
    public void dispose() {
        for (VkPipeline pipeline : pipelines.values()) {
            pipeline.dispose();
        }
        pipelines.clear();
        log.debug("Disposed {} cached pipelines", pipelines.size());
    }

    /**
     * Key for pipeline lookup.
     */
    private record PipelineKey(String shaderName, int descriptorHash, DrawMode drawMode, BlendMode blendMode) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PipelineKey that)) return false;
            return descriptorHash == that.descriptorHash &&
                   Objects.equals(shaderName, that.shaderName) &&
                   drawMode == that.drawMode &&
                   blendMode == that.blendMode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(shaderName, descriptorHash, drawMode, blendMode);
        }
    }
}
