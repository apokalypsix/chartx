// Simple shader - uniform color rendering
// Used for grid lines, axes, and single-color primitives

cbuffer Uniforms : register(b0) {
    float4x4 projection;
    float4 uniformColor;
};

struct VSInput {
    float2 position : POSITION;
};

struct PSInput {
    float4 position : SV_POSITION;
};

PSInput VSMain(VSInput input) {
    PSInput output;
    output.position = mul(projection, float4(input.position, 0.0, 1.0));
    return output;
}

float4 PSMain(PSInput input) : SV_TARGET {
    return uniformColor;
}
