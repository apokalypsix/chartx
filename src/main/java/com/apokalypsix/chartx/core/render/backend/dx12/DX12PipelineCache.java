package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for DirectX 12 pipeline states.
 *
 * <p>Pipeline state objects are expensive to create, so they are cached
 * by a key combining shader, vertex format, topology, and blend mode.
 */
public class DX12PipelineCache {

    private static final Logger log = LoggerFactory.getLogger(DX12PipelineCache.class);

    private final ConcurrentHashMap<PipelineKey, DX12Pipeline> cache = new ConcurrentHashMap<>();

    /**
     * Gets or creates a pipeline for the given configuration.
     *
     * @param device render device
     * @param shader compiled shader
     * @param descriptor vertex buffer descriptor
     * @param drawMode primitive draw mode
     * @param blendMode blend mode
     * @return cached or newly created pipeline
     */
    public DX12Pipeline getOrCreate(DX12RenderDevice device, DX12Shader shader,
                                     BufferDescriptor descriptor, DrawMode drawMode, BlendMode blendMode) {
        if (shader == null) {
            return null;
        }

        PipelineKey key = new PipelineKey(
                shader.getName(),
                descriptor.getFloatsPerVertex(),
                drawMode,
                blendMode
        );

        return cache.computeIfAbsent(key, k -> {
            log.debug("Creating cached pipeline for {}", k);
            return new DX12Pipeline(device, shader, descriptor, drawMode, blendMode);
        });
    }

    /**
     * Clears all cached pipelines and disposes their resources.
     */
    public void clear() {
        cache.values().forEach(DX12Pipeline::dispose);
        cache.clear();
        log.debug("Cleared pipeline cache");
    }

    /**
     * Key for pipeline cache lookup.
     */
    private record PipelineKey(String shaderName, int floatsPerVertex, DrawMode drawMode, BlendMode blendMode) {

        @Override
        public String toString() {
            return String.format("PipelineKey{%s, %d floats, %s, %s}",
                    shaderName, floatsPerVertex, drawMode, blendMode);
        }
    }
}
