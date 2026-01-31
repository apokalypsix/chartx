package com.apokalypsix.chartx.core.render.backend.metal;

/**
 * Registry of Metal Shading Language (MSL) shader sources.
 *
 * <p>Contains the MSL source code for standard shaders used by the
 * Metal rendering backend. These correspond to the shader types
 * defined in ResourceManager.
 */
final class MetalShaderRegistry {

    private MetalShaderRegistry() {}

    /**
     * Default shader with per-vertex color.
     * Vertex format: [x, y, r, g, b, a] (6 floats)
     */
    static final String DEFAULT_SHADER = """
            #include <metal_stdlib>
            using namespace metal;

            struct Uniforms {
                float4x4 projection;
                float4 color;
            };

            struct VertexIn {
                float2 position [[attribute(0)]];
                float4 color    [[attribute(1)]];
            };

            struct VertexOut {
                float4 position [[position]];
                float4 color;
            };

            vertex VertexOut vertexMain(VertexIn in [[stage_in]],
                                        constant Uniforms& uniforms [[buffer(1)]]) {
                VertexOut out;
                out.position = uniforms.projection * float4(in.position, 0.0, 1.0);
                out.color = in.color * uniforms.color;
                return out;
            }

            fragment float4 fragmentMain(VertexOut in [[stage_in]]) {
                return in.color;
            }
            """;

    /**
     * Simple shader with uniform color only.
     * Vertex format: [x, y] (2 floats)
     */
    static final String SIMPLE_SHADER = """
            #include <metal_stdlib>
            using namespace metal;

            struct Uniforms {
                float4x4 projection;
                float4 color;
            };

            struct VertexIn {
                float2 position [[attribute(0)]];
            };

            struct VertexOut {
                float4 position [[position]];
            };

            vertex VertexOut vertexMain(VertexIn in [[stage_in]],
                                        constant Uniforms& uniforms [[buffer(1)]]) {
                VertexOut out;
                out.position = uniforms.projection * float4(in.position, 0.0, 1.0);
                return out;
            }

            fragment float4 fragmentMain(VertexOut in [[stage_in]],
                                         constant Uniforms& uniforms [[buffer(1)]]) {
                return uniforms.color;
            }
            """;

    /**
     * Text shader for texture atlas rendering.
     * Vertex format: [x, y, u, v, r, g, b, a] (8 floats)
     * Samples the red channel of the glyph texture as alpha.
     */
    static final String TEXT_SHADER = """
            #include <metal_stdlib>
            using namespace metal;

            struct Uniforms {
                float4x4 projection;
                float4 color;
            };

            struct VertexIn {
                float2 position [[attribute(0)]];
                float2 texCoord [[attribute(1)]];
                float4 color    [[attribute(2)]];
            };

            struct VertexOut {
                float4 position [[position]];
                float2 texCoord;
                float4 color;
            };

            vertex VertexOut vertexMain(VertexIn in [[stage_in]],
                                        constant Uniforms& uniforms [[buffer(1)]]) {
                VertexOut out;
                out.position = uniforms.projection * float4(in.position, 0.0, 1.0);
                out.texCoord = in.texCoord;
                out.color = in.color * uniforms.color;
                return out;
            }

            fragment float4 fragmentMain(VertexOut in [[stage_in]],
                                         texture2d<float> glyphTexture [[texture(0)]],
                                         sampler glyphSampler [[sampler(0)]]) {
                float alpha = glyphTexture.sample(glyphSampler, in.texCoord).r;
                if (alpha < 0.01) {
                    discard_fragment();
                }
                return float4(in.color.rgb, in.color.a * alpha);
            }
            """;
}
