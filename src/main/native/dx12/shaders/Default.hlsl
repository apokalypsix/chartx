// Default shader - per-vertex color rendering
// Used for candlesticks, bars, and colored primitives

cbuffer Uniforms : register(b0) {
    float4x4 projection;
    float4 uniformColor;
};

struct VSInput {
    float2 position : POSITION;
    float4 color : COLOR;
};

struct PSInput {
    float4 position : SV_POSITION;
    float4 color : COLOR;
};

PSInput VSMain(VSInput input) {
    PSInput output;
    output.position = mul(projection, float4(input.position, 0.0, 1.0));
    output.color = input.color;
    return output;
}

float4 PSMain(PSInput input) : SV_TARGET {
    return input.color;
}
