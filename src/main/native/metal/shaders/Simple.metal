/**
 * Simple.metal - Position-only shader with uniform color
 *
 * Used for simple geometry rendering where all vertices share the same color.
 * Vertex format: [x, y] (2 floats per vertex)
 */

#include <metal_stdlib>
using namespace metal;

// Uniform buffer structure (buffer index 1)
struct Uniforms {
    float4x4 projection;    // 64 bytes: projection matrix
    float4 color;           // 16 bytes: uniform color for all vertices
};

// Vertex input from vertex buffer (buffer index 0)
struct VertexIn {
    float2 position [[attribute(0)]];
};

// Vertex output / Fragment input
struct VertexOut {
    float4 position [[position]];
};

// Vertex shader
vertex VertexOut vertexMain(VertexIn in [[stage_in]],
                            constant Uniforms& uniforms [[buffer(1)]]) {
    VertexOut out;
    out.position = uniforms.projection * float4(in.position, 0.0, 1.0);
    return out;
}

// Fragment shader
fragment float4 fragmentMain(VertexOut in [[stage_in]],
                             constant Uniforms& uniforms [[buffer(1)]]) {
    return uniforms.color;
}
