// Text shader - texture atlas rendering
// Used for text rendering with alpha from grayscale atlas

cbuffer Uniforms : register(b0) {
    float4x4 projection;
    float4 uniformColor;
};

Texture2D fontAtlas : register(t0);
SamplerState fontSampler : register(s0);

struct VSInput {
    float2 position : POSITION;
    float2 texCoord : TEXCOORD;
};

struct PSInput {
    float4 position : SV_POSITION;
    float2 texCoord : TEXCOORD;
};

PSInput VSMain(VSInput input) {
    PSInput output;
    output.position = mul(projection, float4(input.position, 0.0, 1.0));
    output.texCoord = input.texCoord;
    return output;
}

float4 PSMain(PSInput input) : SV_TARGET {
    float alpha = fontAtlas.Sample(fontSampler, input.texCoord).r;
    return float4(uniformColor.rgb, uniformColor.a * alpha);
}
