package com.apokalypsix.chartx.core.render.backend.dx12;

/**
 * Registry of built-in HLSL shader sources for DirectX 12.
 */
public final class DX12ShaderRegistry {

    private DX12ShaderRegistry() {
        // Prevent instantiation
    }

    /**
     * Default shader with per-vertex color.
     * Input: position (float2) + color (float4)
     */
    public static final String DEFAULT_SHADER = """
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
            """;

    /**
     * Simple shader with uniform color only.
     * Input: position (float2) only
     */
    public static final String SIMPLE_SHADER = """
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
            """;

    /**
     * Text shader with texture atlas sampling.
     * Input: position (float2) + texCoord (float2) + color (float4)
     * Samples red channel as alpha for SDF/bitmap fonts.
     */
    public static final String TEXT_SHADER = """
            cbuffer Uniforms : register(b0) {
                float4x4 projection;
                float4 uniformColor;
            };

            Texture2D fontTexture : register(t0);
            SamplerState fontSampler : register(s0);

            struct VSInput {
                float2 position : POSITION;
                float2 texCoord : TEXCOORD;
                float4 color : COLOR;
            };

            struct PSInput {
                float4 position : SV_POSITION;
                float2 texCoord : TEXCOORD;
                float4 color : COLOR;
            };

            PSInput VSMain(VSInput input) {
                PSInput output;
                output.position = mul(projection, float4(input.position, 0.0, 1.0));
                output.texCoord = input.texCoord;
                output.color = input.color;
                return output;
            }

            float4 PSMain(PSInput input) : SV_TARGET {
                float alpha = fontTexture.Sample(fontSampler, input.texCoord).r;
                return float4(input.color.rgb, input.color.a * alpha);
            }
            """;

    /**
     * Gets a shader source by name.
     *
     * @param name shader name ("default", "simple", or "text")
     * @return shader source code, or null if not found
     */
    public static String getShaderSource(String name) {
        return switch (name.toLowerCase()) {
            case "default" -> DEFAULT_SHADER;
            case "simple" -> SIMPLE_SHADER;
            case "text" -> TEXT_SHADER;
            default -> null;
        };
    }
}
