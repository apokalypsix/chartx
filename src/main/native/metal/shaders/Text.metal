/**
 * Text.metal - Textured text shader with per-vertex color
 *
 * Used for rendering text from a glyph texture atlas.
 * Vertex format: [x, y, u, v, r, g, b, a] (8 floats per vertex)
 * Samples the red channel of the texture as alpha.
 */

#include <metal_stdlib>
using namespace metal;

// Uniform buffer structure (buffer index 1)
struct Uniforms {
    float4x4 projection;    // 64 bytes: projection matrix
    float4 color;           // 16 bytes: color multiplier (usually white)
};

// Vertex input from vertex buffer (buffer index 0)
struct VertexIn {
    float2 position [[attribute(0)]];
    float2 texCoord [[attribute(1)]];
    float4 color    [[attribute(2)]];
};

// Vertex output / Fragment input
struct VertexOut {
    float4 position [[position]];
    float2 texCoord;
    float4 color;
};

// Vertex shader
vertex VertexOut vertexMain(VertexIn in [[stage_in]],
                            constant Uniforms& uniforms [[buffer(1)]]) {
    VertexOut out;
    out.position = uniforms.projection * float4(in.position, 0.0, 1.0);
    out.texCoord = in.texCoord;
    out.color = in.color * uniforms.color;
    return out;
}

// Fragment shader
fragment float4 fragmentMain(VertexOut in [[stage_in]],
                             texture2d<float> glyphTexture [[texture(0)]],
                             sampler glyphSampler [[sampler(0)]]) {
    // Sample the red channel as alpha (grayscale glyph atlas)
    float alpha = glyphTexture.sample(glyphSampler, in.texCoord).r;

    // Discard very transparent fragments
    if (alpha < 0.01) {
        discard_fragment();
    }

    return float4(in.color.rgb, in.color.a * alpha);
}
