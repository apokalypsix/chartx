package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.BlendMode;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.VertexAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * DirectX 12 Pipeline State Object (PSO) wrapper.
 *
 * <p>Encapsulates a pre-compiled pipeline state combining shader,
 * vertex layout, primitive topology, and blend mode.
 */
public class DX12Pipeline {

    private static final Logger log = LoggerFactory.getLogger(DX12Pipeline.class);

    // DXGI_FORMAT values
    private static final int DXGI_FORMAT_R32_FLOAT = 41;
    private static final int DXGI_FORMAT_R32G32_FLOAT = 16;
    private static final int DXGI_FORMAT_R32G32B32_FLOAT = 6;
    private static final int DXGI_FORMAT_R32G32B32A32_FLOAT = 2;

    private long pipelineState;
    private final DrawMode drawMode;
    private final BlendMode blendMode;

    /**
     * Creates a pipeline from shader and vertex descriptor.
     *
     * @param device render device
     * @param shader compiled shader
     * @param descriptor vertex buffer descriptor
     * @param drawMode primitive draw mode
     * @param blendMode blend mode
     */
    public DX12Pipeline(DX12RenderDevice device, DX12Shader shader,
                        BufferDescriptor descriptor, DrawMode drawMode, BlendMode blendMode) {
        this.drawMode = drawMode;
        this.blendMode = blendMode;

        // Build vertex attribute arrays
        List<VertexAttribute> attributes = descriptor.getAttributes();
        int[] formats = new int[attributes.size()];
        int[] offsets = new int[attributes.size()];

        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute attr = attributes.get(i);
            formats[i] = toDXGIFormat(attr.getComponents());
            offsets[i] = attr.getOffset();
        }

        int stride = descriptor.getFloatsPerVertex() * Float.BYTES;
        int topology = toD3DTopologyType(drawMode);
        int blendModeInt = toBlendModeInt(blendMode);

        // Create pipeline state
        pipelineState = DX12Native.createPipelineState(
                device.getDevice(),
                device.getRootSignature(),
                shader.getVertexBytecode(),
                shader.getPixelBytecode(),
                topology,
                blendModeInt,
                formats,
                offsets,
                stride
        );

        if (pipelineState == 0) {
            throw new RuntimeException("Failed to create DX12 pipeline state");
        }

        log.debug("Created DX12 pipeline for {} with {}", drawMode, blendMode);
    }

    private int toDXGIFormat(int componentCount) {
        return switch (componentCount) {
            case 1 -> DXGI_FORMAT_R32_FLOAT;
            case 2 -> DXGI_FORMAT_R32G32_FLOAT;
            case 3 -> DXGI_FORMAT_R32G32B32_FLOAT;
            case 4 -> DXGI_FORMAT_R32G32B32A32_FLOAT;
            default -> throw new IllegalArgumentException("Unsupported component count: " + componentCount);
        };
    }

    private int toD3DTopologyType(DrawMode mode) {
        return switch (mode) {
            case POINTS -> 1;        // D3D12_PRIMITIVE_TOPOLOGY_TYPE_POINT
            case LINES, LINE_STRIP, LINE_LOOP -> 2;  // D3D12_PRIMITIVE_TOPOLOGY_TYPE_LINE
            case TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> 3;  // D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE
        };
    }

    private int toBlendModeInt(BlendMode mode) {
        return switch (mode) {
            case NONE -> 0;
            case ALPHA -> 1;
            case ADDITIVE -> 2;
            case MULTIPLY -> 3;
            case PREMULTIPLIED_ALPHA -> 4;
        };
    }

    /**
     * Binds this pipeline for rendering.
     *
     * @param commandList command list handle
     */
    public void bind(long commandList) {
        if (pipelineState != 0) {
            DX12Native.setPipelineState(commandList, pipelineState);
        }
    }

    /**
     * Disposes the pipeline state.
     */
    public void dispose() {
        if (pipelineState != 0) {
            DX12Native.destroyPipelineState(pipelineState);
            pipelineState = 0;
            log.debug("Disposed DX12 pipeline");
        }
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }
}
