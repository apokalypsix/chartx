package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches Metal render pipeline states for reuse.
 *
 * <p>Pipeline creation is expensive, so this cache stores pipelines
 * keyed by their configuration (shader, vertex format, topology, blend mode).
 */
public class MetalPipelineCache {

    private static final Logger log = LoggerFactory.getLogger(MetalPipelineCache.class);

    private final MetalRenderDevice device;
    private final Map<PipelineKey, MetalPipeline> cache = new ConcurrentHashMap<>();

    public MetalPipelineCache(MetalRenderDevice device) {
        this.device = device;
    }

    /**
     * Gets or creates a pipeline for the given configuration.
     *
     * @param shader     the shader
     * @param descriptor vertex buffer format
     * @param drawMode   primitive topology
     * @param blendMode  blend mode
     * @return the cached or newly created pipeline
     */
    public MetalPipeline getPipeline(MetalShader shader, BufferDescriptor descriptor,
                                     DrawMode drawMode, BlendMode blendMode) {
        PipelineKey key = new PipelineKey(
                shader.getName(),
                descriptor.getFloatsPerVertex(),
                drawMode,
                blendMode
        );

        return cache.computeIfAbsent(key, k -> {
            log.debug("Creating cached pipeline for {}", k);
            MetalPipeline pipeline = new MetalPipeline(device);
            pipeline.create(shader, descriptor, drawMode, blendMode);
            return pipeline;
        });
    }

    /**
     * Clears all cached pipelines.
     */
    public void clear() {
        for (MetalPipeline pipeline : cache.values()) {
            pipeline.dispose();
        }
        cache.clear();
        log.debug("Cleared pipeline cache");
    }

    /**
     * Disposes all cached pipelines.
     */
    public void dispose() {
        clear();
    }

    /**
     * Returns the number of cached pipelines.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Key for pipeline cache lookup.
     */
    private record PipelineKey(
            String shaderName,
            int floatsPerVertex,
            DrawMode drawMode,
            BlendMode blendMode
    ) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PipelineKey that)) return false;
            return floatsPerVertex == that.floatsPerVertex &&
                    Objects.equals(shaderName, that.shaderName) &&
                    drawMode == that.drawMode &&
                    blendMode == that.blendMode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(shaderName, floatsPerVertex, drawMode, blendMode);
        }

        @Override
        public String toString() {
            return "PipelineKey{" + shaderName + ", " + floatsPerVertex +
                    " floats, " + drawMode + ", " + blendMode + "}";
        }
    }
}
