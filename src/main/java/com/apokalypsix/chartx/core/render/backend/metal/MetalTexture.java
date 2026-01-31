package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.Texture;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Metal texture implementation.
 *
 * <p>Manages Metal textures and samplers for texture data.
 */
public class MetalTexture implements Texture {

    private static final Logger log = LoggerFactory.getLogger(MetalTexture.class);

    // Metal pixel format constants
    private static final int MTL_PIXEL_FORMAT_R8_UNORM = 10;
    private static final int MTL_PIXEL_FORMAT_RG8_UNORM = 30;
    private static final int MTL_PIXEL_FORMAT_RGBA8_UNORM = 70;
    private static final int MTL_PIXEL_FORMAT_BGRA8_UNORM = 80;

    // Metal texture usage flags
    private static final int MTL_TEXTURE_USAGE_SHADER_READ = 0x0001;

    // Metal sampler filter/address mode constants
    private static final int MTL_SAMPLER_MIN_MAG_FILTER_NEAREST = 0;
    private static final int MTL_SAMPLER_MIN_MAG_FILTER_LINEAR = 1;
    private static final int MTL_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE = 0;
    private static final int MTL_SAMPLER_ADDRESS_MODE_REPEAT = 1;
    private static final int MTL_SAMPLER_ADDRESS_MODE_MIRROR_REPEAT = 2;

    private final MetalRenderDevice device;
    private final TextureDescriptor descriptor;

    private long texture;
    private long sampler;

    private int width;
    private int height;
    private boolean disposed = false;

    public MetalTexture(MetalRenderDevice device, TextureDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.width = descriptor.getWidth();
        this.height = descriptor.getHeight();

        if (device.isInitialized()) {
            createTexture();
            createSampler();
        }
    }

    private void createTexture() {
        int format = toMetalFormat(descriptor.getFormat());
        int usage = MTL_TEXTURE_USAGE_SHADER_READ;

        texture = MetalNative.createTexture(device.getDevice(), width, height, format, usage);
        if (texture == 0) {
            log.error("Failed to create Metal texture {}x{}", width, height);
            return;
        }

        log.debug("Created Metal texture {}x{} (format: {})", width, height, descriptor.getFormat());
    }

    private void createSampler() {
        int minFilter = toMetalFilter(descriptor.getMinFilter());
        int magFilter = toMetalFilter(descriptor.getMagFilter());
        int addressMode = toMetalAddressMode(descriptor.getWrapS());

        sampler = MetalNative.createSampler(device.getDevice(), minFilter, magFilter, addressMode);
        if (sampler == 0) {
            log.error("Failed to create Metal sampler");
        }
    }

    private int toMetalFormat(TextureDescriptor.TextureFormat format) {
        return switch (format) {
            case R8 -> MTL_PIXEL_FORMAT_R8_UNORM;
            case RG8 -> MTL_PIXEL_FORMAT_RG8_UNORM;
            case RGB8, RGBA8 -> MTL_PIXEL_FORMAT_RGBA8_UNORM;
            default -> MTL_PIXEL_FORMAT_RGBA8_UNORM;
        };
    }

    private int toMetalFilter(TextureDescriptor.TextureFilter filter) {
        return switch (filter) {
            case NEAREST, NEAREST_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR ->
                    MTL_SAMPLER_MIN_MAG_FILTER_NEAREST;
            default -> MTL_SAMPLER_MIN_MAG_FILTER_LINEAR;
        };
    }

    private int toMetalAddressMode(TextureDescriptor.TextureWrap wrap) {
        return switch (wrap) {
            case REPEAT -> MTL_SAMPLER_ADDRESS_MODE_REPEAT;
            case MIRRORED_REPEAT -> MTL_SAMPLER_ADDRESS_MODE_MIRROR_REPEAT;
            default -> MTL_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        };
    }

    @Override
    public void upload(BufferedImage image) {
        if (disposed || texture == 0) return;

        // Resize texture if needed
        if (image.getWidth() != width || image.getHeight() != height) {
            disposeTextureResources();
            width = image.getWidth();
            height = image.getHeight();
            createTexture();
        }

        // Extract pixel data in RGBA format
        int pixelCount = width * height;
        byte[] data = new byte[pixelCount * 4];

        int[] pixels = new int[pixelCount];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        for (int i = 0; i < pixelCount; i++) {
            int pixel = pixels[i];
            data[i * 4] = (byte) ((pixel >> 16) & 0xFF);     // R
            data[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);  // G
            data[i * 4 + 2] = (byte) (pixel & 0xFF);         // B
            data[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // A
        }

        int bytesPerRow = width * 4;
        MetalNative.uploadTextureData(texture, data, width, height, bytesPerRow);

        log.debug("Uploaded texture data {}x{}", width, height);
    }

    @Override
    public void upload(ByteBuffer data, int width, int height, TextureDescriptor.TextureFormat format) {
        if (disposed || texture == 0) return;

        // Resize texture if needed
        if (width != this.width || height != this.height) {
            disposeTextureResources();
            this.width = width;
            this.height = height;
            createTexture();
        }

        // Copy ByteBuffer to byte array for JNI
        byte[] bytes = new byte[data.remaining()];
        int pos = data.position();
        data.get(bytes);
        data.position(pos); // Restore position

        int bytesPerPixel = switch (format) {
            case R8 -> 1;
            case RG8 -> 2;
            case RGB8 -> 3;
            case RGBA8, RGBA16F, RGBA32F -> 4;
            case R16F -> 2;
        };

        int bytesPerRow = width * bytesPerPixel;
        MetalNative.uploadTextureData(texture, bytes, width, height, bytesPerRow);

        log.debug("Uploaded texture data {}x{} (format: {})", width, height, format);
    }

    /**
     * Uploads grayscale (R8) data directly from a byte array.
     * Used for font atlas uploads.
     *
     * @param data      grayscale pixel data
     * @param width     image width
     * @param height    image height
     */
    public void uploadGrayscale(byte[] data, int width, int height) {
        if (disposed || texture == 0) return;

        // Resize texture if needed
        if (width != this.width || height != this.height) {
            disposeTextureResources();
            this.width = width;
            this.height = height;
            createTexture();
        }

        int bytesPerRow = width; // R8 format
        MetalNative.uploadTextureData(texture, data, width, height, bytesPerRow);

        log.debug("Uploaded grayscale texture data {}x{}", width, height);
    }

    @Override
    public void bind(int unit) {
        if (disposed || texture == 0) return;

        long encoder = device.getRenderEncoder();
        if (encoder == 0) return;

        MetalNative.setFragmentTexture(encoder, texture, unit);
        if (sampler != 0) {
            MetalNative.setFragmentSamplerState(encoder, sampler, unit);
        }
    }

    @Override
    public void unbind() {
        // Metal doesn't require explicit unbinding
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
        return texture != 0 && !disposed;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        disposeTextureResources();
        disposeSampler();

        log.debug("Disposed Metal texture");
    }

    private void disposeTextureResources() {
        if (texture != 0) {
            MetalNative.destroyTexture(texture);
            texture = 0;
        }
    }

    private void disposeSampler() {
        if (sampler != 0) {
            MetalNative.destroySampler(sampler);
            sampler = 0;
        }
    }

    // -------------------------------------------------------------------------
    // Metal-specific getters
    // -------------------------------------------------------------------------

    /**
     * Returns the Metal texture handle.
     */
    public long getTexture() {
        return texture;
    }

    /**
     * Returns the Metal sampler handle.
     */
    public long getSampler() {
        return sampler;
    }
}
