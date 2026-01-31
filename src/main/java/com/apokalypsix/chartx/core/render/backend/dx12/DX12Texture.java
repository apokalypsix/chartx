package com.apokalypsix.chartx.core.render.backend.dx12;

import com.apokalypsix.chartx.core.render.api.Texture;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor.TextureFormat;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor.TextureFilter;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor.TextureWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

/**
 * DirectX 12 implementation of Texture.
 */
public class DX12Texture implements Texture {

    private static final Logger log = LoggerFactory.getLogger(DX12Texture.class);

    // DXGI_FORMAT values
    private static final int DXGI_FORMAT_R8_UNORM = 61;
    private static final int DXGI_FORMAT_R8G8_UNORM = 49;
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;
    private static final int DXGI_FORMAT_R16G16B16A16_FLOAT = 10;
    private static final int DXGI_FORMAT_R32G32B32A32_FLOAT = 2;

    private final DX12RenderDevice device;
    private final TextureDescriptor descriptor;

    private long textureHandle;
    private int width;
    private int height;
    private boolean initialized;

    public DX12Texture(DX12RenderDevice device, TextureDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.width = descriptor.getWidth();
        this.height = descriptor.getHeight();

        if (width > 0 && height > 0) {
            createTexture();
        }
    }

    private void createTexture() {
        int dxgiFormat = toDXGIFormat(descriptor.getFormat());
        textureHandle = DX12Native.createTexture(device.getDevice(), width, height, dxgiFormat);

        if (textureHandle == 0) {
            throw new RuntimeException("Failed to create DX12 texture");
        }

        initialized = true;
        log.debug("Created DX12 texture {}x{} format={}", width, height, descriptor.getFormat());
    }

    private int toDXGIFormat(TextureFormat format) {
        return switch (format) {
            case R8 -> DXGI_FORMAT_R8_UNORM;
            case RG8 -> DXGI_FORMAT_R8G8_UNORM;
            case RGB8, RGBA8 -> DXGI_FORMAT_R8G8B8A8_UNORM;
            case R16F, RGBA16F -> DXGI_FORMAT_R16G16B16A16_FLOAT;
            case RGBA32F -> DXGI_FORMAT_R32G32B32A32_FLOAT;
        };
    }

    @Override
    public void upload(BufferedImage image) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // Resize texture if needed
        if (imgWidth != width || imgHeight != height) {
            if (textureHandle != 0) {
                DX12Native.destroyTexture(textureHandle);
            }
            width = imgWidth;
            height = imgHeight;
            createTexture();
        }

        // Convert to RGBA bytes
        byte[] data = extractPixelData(image);
        int bytesPerRow = width * 4;

        long commandList = device.getCommandList();
        if (commandList != 0) {
            DX12Native.uploadTextureData(commandList, textureHandle, data, width, height, bytesPerRow);
        }
    }

    private byte[] extractPixelData(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        byte[] data = new byte[w * h * 4];

        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            for (int i = 0; i < pixels.length; i++) {
                int argb = pixels[i];
                data[i * 4] = (byte) ((argb >> 16) & 0xFF);     // R
                data[i * 4 + 1] = (byte) ((argb >> 8) & 0xFF);  // G
                data[i * 4 + 2] = (byte) (argb & 0xFF);         // B
                data[i * 4 + 3] = (byte) ((argb >> 24) & 0xFF); // A
            }
        } else if (type == BufferedImage.TYPE_4BYTE_ABGR) {
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            for (int i = 0; i < w * h; i++) {
                data[i * 4] = pixels[i * 4 + 3];     // R (from ABGR)
                data[i * 4 + 1] = pixels[i * 4 + 2]; // G
                data[i * 4 + 2] = pixels[i * 4 + 1]; // B
                data[i * 4 + 3] = pixels[i * 4];     // A
            }
        } else {
            // Fallback: use getRGB
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = image.getRGB(x, y);
                    int idx = (y * w + x) * 4;
                    data[idx] = (byte) ((argb >> 16) & 0xFF);
                    data[idx + 1] = (byte) ((argb >> 8) & 0xFF);
                    data[idx + 2] = (byte) (argb & 0xFF);
                    data[idx + 3] = (byte) ((argb >> 24) & 0xFF);
                }
            }
        }

        return data;
    }

    @Override
    public void upload(ByteBuffer data, int width, int height, TextureFormat format) {
        if (width != this.width || height != this.height) {
            if (textureHandle != 0) {
                DX12Native.destroyTexture(textureHandle);
            }
            this.width = width;
            this.height = height;
            createTexture();
        }

        byte[] bytes = new byte[data.remaining()];
        int pos = data.position();
        data.get(bytes);
        data.position(pos);

        int bytesPerRow = width * getBytesPerPixel(format);

        long commandList = device.getCommandList();
        if (commandList != 0) {
            DX12Native.uploadTextureData(commandList, textureHandle, bytes, width, height, bytesPerRow);
        }
    }

    private int getBytesPerPixel(TextureFormat format) {
        return switch (format) {
            case R8 -> 1;
            case RG8 -> 2;
            case RGB8 -> 3;
            case RGBA8 -> 4;
            case R16F -> 2;
            case RGBA16F -> 8;
            case RGBA32F -> 16;
        };
    }

    /**
     * Uploads grayscale (R8) data to the texture.
     */
    public void uploadGrayscale(byte[] data, int width, int height) {
        if (width != this.width || height != this.height) {
            if (textureHandle != 0) {
                DX12Native.destroyTexture(textureHandle);
            }
            this.width = width;
            this.height = height;
            createTexture();
        }

        int bytesPerRow = width; // R8 = 1 byte per pixel

        long commandList = device.getCommandList();
        if (commandList != 0) {
            DX12Native.uploadTextureData(commandList, textureHandle, data, width, height, bytesPerRow);
        }
    }

    @Override
    public void bind(int unit) {
        long commandList = device.getCommandList();
        if (commandList != 0 && textureHandle != 0) {
            DX12Native.setTexture(commandList, textureHandle, unit);
        }
    }

    @Override
    public void unbind() {
        // No-op for DX12
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void dispose() {
        if (textureHandle != 0) {
            DX12Native.destroyTexture(textureHandle);
            textureHandle = 0;
            initialized = false;
            log.debug("Disposed DX12 texture");
        }
    }

    long getHandle() {
        return textureHandle;
    }

    TextureDescriptor getDescriptor() {
        return descriptor;
    }
}
