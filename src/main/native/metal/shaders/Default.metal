/**
 * Default.metal - Position + Color shader
 *
 * Used for rendering with per-vertex colors (e.g., colored triangles, lines).
 * Vertex format: [x, y, r, g, b, a] (6 floats per vertex)
 */

#include <metal_stdlib>
using namespace metal;

// Uniform buffer structure (buffer index 1)
struct Uniforms {
    float4x4 projection;    // 64 bytes: projection matrix
    float4 color;           // 16 bytes: multiplier color (usually white)
};

// Vertex input from vertex buffer (buffer index 0)
struct VertexIn {
    float2 position [[attribute(0)]];
    float4 color    [[attribute(1)]];
};

// Vertex output / Fragment input
struct VertexOut {
    float4 position [[position]];
    float4 color;
};

// Vertex shader
vertex VertexOut vertexMain(VertexIn in [[stage_in]],
                            constant Uniforms& uniforms [[buffer(1)]]) {
    VertexOut out;
    out.position = uniforms.projection * float4(in.position, 0.0, 1.0);
    out.color = in.color * uniforms.color;
    return out;
}

// Fragment shader
fragment float4 fragmentMain(VertexOut in [[stage_in]]) {
    return in.color;
}
