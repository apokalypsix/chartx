package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.Texture;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor.TextureFilter;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor.TextureFormat;
import com.apokalypsix.chartx.core.render.api.TextureDescriptor.TextureWrap;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * OpenGL implementation of the Texture interface.
 *
 * <p>Wraps GL texture creation, upload, and binding using JOGL.
 */
public class GLTexture implements Texture {

    private final GLRenderDevice device;
    private final TextureDescriptor descriptor;

    private int textureId = 0;
    private int width = 0;
    private int height = 0;
    private boolean initialized = false;

    /**
     * Creates a new GLTexture.
     *
     * @param device the render device
     * @param descriptor the texture descriptor
     */
    public GLTexture(GLRenderDevice device, TextureDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;

        initialize();
    }

    private void initialize() {
        GL2ES2 gl = device.getGL();

        int[] texIds = new int[1];
        gl.glGenTextures(1, texIds, 0);
        textureId = texIds[0];

        gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);

        // Set texture parameters
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                toGLFilter(descriptor.getMinFilter()));
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                toGLFilter(descriptor.getMagFilter()));
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
                toGLWrap(descriptor.getWrapS()));
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
                toGLWrap(descriptor.getWrapT()));

        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        initialized = true;
    }

    @Override
    public void upload(BufferedImage image) {
        GL2ES2 gl = device.getGL();

        this.width = image.getWidth();
        this.height = image.getHeight();

        gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);

        ByteBuffer buffer;
        int internalFormat;
        int format;

        // Handle different image types
        int imageType = image.getType();

        if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
            // Single channel grayscale
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            buffer = ByteBuffer.allocateDirect(pixels.length);
            buffer.put(pixels);
            buffer.flip();
            internalFormat = GL2ES2.GL_R8;
            format = GL2ES2.GL_RED;
        } else if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
            // Convert to RGBA
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

            for (int pixel : pixels) {
                int a = (imageType == BufferedImage.TYPE_INT_ARGB) ? (pixel >> 24) & 0xFF : 255;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
            buffer.flip();
            internalFormat = GL.GL_RGBA;
            format = GL.GL_RGBA;
        } else {
            // Convert image to ARGB format and then extract
            BufferedImage converted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            converted.getGraphics().drawImage(image, 0, 0, null);
            int[] pixels = ((DataBufferInt) converted.getRaster().getDataBuffer()).getData();

            buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
            for (int pixel : pixels) {
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
            buffer.flip();
            internalFormat = GL.GL_RGBA;
            format = GL.GL_RGBA;
        }

        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width, height,
                0, format, GL.GL_UNSIGNED_BYTE, buffer);

        if (descriptor.isGenerateMipmaps()) {
            gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
        }

        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    }

    @Override
    public void upload(ByteBuffer data, int width, int height, TextureFormat format) {
        GL2ES2 gl = device.getGL();

        this.width = width;
        this.height = height;

        gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);

        int glInternalFormat = toGLInternalFormat(format);
        int glFormat = toGLFormat(format);
        int glType = toGLType(format);

        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, glInternalFormat, width, height,
                0, glFormat, glType, data);

        if (descriptor.isGenerateMipmaps()) {
            gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
        }

        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    }

    @Override
    public void bind(int unit) {
        GL2ES2 gl = device.getGL();
        gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
    }

    @Override
    public void unbind() {
        device.getGL().glBindTexture(GL.GL_TEXTURE_2D, 0);
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
    public void dispose() {
        if (textureId != 0) {
            device.getGL().glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }
        initialized = false;
        width = 0;
        height = 0;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the OpenGL texture ID.
     */
    public int getTextureId() {
        return textureId;
    }

    private static int toGLFilter(TextureFilter filter) {
        return switch (filter) {
            case NEAREST -> GL.GL_NEAREST;
            case LINEAR -> GL.GL_LINEAR;
            case NEAREST_MIPMAP_NEAREST -> GL.GL_NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_NEAREST -> GL.GL_LINEAR_MIPMAP_NEAREST;
            case NEAREST_MIPMAP_LINEAR -> GL.GL_NEAREST_MIPMAP_LINEAR;
            case LINEAR_MIPMAP_LINEAR -> GL.GL_LINEAR_MIPMAP_LINEAR;
        };
    }

    private static int toGLWrap(TextureWrap wrap) {
        return switch (wrap) {
            case REPEAT -> GL.GL_REPEAT;
            case MIRRORED_REPEAT -> GL.GL_MIRRORED_REPEAT;
            case CLAMP_TO_EDGE -> GL.GL_CLAMP_TO_EDGE;
            case CLAMP_TO_BORDER -> GL2ES2.GL_CLAMP_TO_BORDER;
        };
    }

    private static int toGLInternalFormat(TextureFormat format) {
        return switch (format) {
            case R8 -> GL2ES2.GL_R8;
            case RG8 -> GL2ES2.GL_RG8;
            case RGB8 -> GL.GL_RGB;
            case RGBA8 -> GL.GL_RGBA;
            case R16F -> GL2ES2.GL_R16F;
            case RGBA16F -> GL2ES2.GL_RGBA16F;
            case RGBA32F -> GL2ES2.GL_RGBA32F;
        };
    }

    private static int toGLFormat(TextureFormat format) {
        return switch (format) {
            case R8, R16F -> GL2ES2.GL_RED;
            case RG8 -> GL2ES2.GL_RG;
            case RGB8 -> GL.GL_RGB;
            case RGBA8, RGBA16F, RGBA32F -> GL.GL_RGBA;
        };
    }

    private static int toGLType(TextureFormat format) {
        return switch (format) {
            case R8, RG8, RGB8, RGBA8 -> GL.GL_UNSIGNED_BYTE;
            case R16F, RGBA16F -> GL.GL_HALF_FLOAT;
            case RGBA32F -> GL.GL_FLOAT;
        };
    }
}
