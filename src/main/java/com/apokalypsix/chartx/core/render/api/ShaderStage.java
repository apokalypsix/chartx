package com.apokalypsix.chartx.core.render.api;

/**
 * Shader pipeline stages.
 */
public enum ShaderStage {
    /**
     * Vertex shader stage.
     */
    VERTEX,

    /**
     * Fragment (pixel) shader stage.
     */
    FRAGMENT,

    /**
     * Geometry shader stage (optional).
     */
    GEOMETRY,

    /**
     * Compute shader stage.
     */
    COMPUTE
}
