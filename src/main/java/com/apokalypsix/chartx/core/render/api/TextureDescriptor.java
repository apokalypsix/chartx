package com.apokalypsix.chartx.core.render.api;

/**
 * Describes the format and usage of a texture.
 */
public class TextureDescriptor {

    private final int width;
    private final int height;
    private final TextureFormat format;
    private final TextureFilter minFilter;
    private final TextureFilter magFilter;
    private final TextureWrap wrapS;
    private final TextureWrap wrapT;
    private final boolean generateMipmaps;

    private TextureDescriptor(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.format = builder.format;
        this.minFilter = builder.minFilter;
        this.magFilter = builder.magFilter;
        this.wrapS = builder.wrapS;
        this.wrapT = builder.wrapT;
        this.generateMipmaps = builder.generateMipmaps;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a descriptor for a font atlas texture (single channel, linear filtering).
     */
    public static TextureDescriptor fontAtlas(int width, int height) {
        return builder()
                .size(width, height)
                .format(TextureFormat.R8)
                .minFilter(TextureFilter.LINEAR)
                .magFilter(TextureFilter.LINEAR)
                .wrap(TextureWrap.CLAMP_TO_EDGE)
                .build();
    }

    /**
     * Creates a descriptor for an RGBA image texture.
     */
    public static TextureDescriptor rgba(int width, int height) {
        return builder()
                .size(width, height)
                .format(TextureFormat.RGBA8)
                .minFilter(TextureFilter.LINEAR)
                .magFilter(TextureFilter.LINEAR)
                .wrap(TextureWrap.CLAMP_TO_EDGE)
                .build();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TextureFormat getFormat() {
        return format;
    }

    public TextureFilter getMinFilter() {
        return minFilter;
    }

    public TextureFilter getMagFilter() {
        return magFilter;
    }

    public TextureWrap getWrapS() {
        return wrapS;
    }

    public TextureWrap getWrapT() {
        return wrapT;
    }

    public boolean isGenerateMipmaps() {
        return generateMipmaps;
    }

    /**
     * Texture pixel formats.
     */
    public enum TextureFormat {
        R8,         // Single channel (8-bit)
        RG8,        // Two channels (8-bit each)
        RGB8,       // Three channels (8-bit each)
        RGBA8,      // Four channels (8-bit each)
        R16F,       // Single channel (16-bit float)
        RGBA16F,    // Four channels (16-bit float)
        RGBA32F     // Four channels (32-bit float)
    }

    /**
     * Texture filtering modes.
     */
    public enum TextureFilter {
        NEAREST,
        LINEAR,
        NEAREST_MIPMAP_NEAREST,
        LINEAR_MIPMAP_NEAREST,
        NEAREST_MIPMAP_LINEAR,
        LINEAR_MIPMAP_LINEAR
    }

    /**
     * Texture wrapping modes.
     */
    public enum TextureWrap {
        REPEAT,
        MIRRORED_REPEAT,
        CLAMP_TO_EDGE,
        CLAMP_TO_BORDER
    }

    public static class Builder {
        private int width = 0;
        private int height = 0;
        private TextureFormat format = TextureFormat.RGBA8;
        private TextureFilter minFilter = TextureFilter.LINEAR;
        private TextureFilter magFilter = TextureFilter.LINEAR;
        private TextureWrap wrapS = TextureWrap.CLAMP_TO_EDGE;
        private TextureWrap wrapT = TextureWrap.CLAMP_TO_EDGE;
        private boolean generateMipmaps = false;

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder format(TextureFormat format) {
            this.format = format;
            return this;
        }

        public Builder minFilter(TextureFilter filter) {
            this.minFilter = filter;
            return this;
        }

        public Builder magFilter(TextureFilter filter) {
            this.magFilter = filter;
            return this;
        }

        public Builder wrap(TextureWrap wrap) {
            this.wrapS = wrap;
            this.wrapT = wrap;
            return this;
        }

        public Builder wrapS(TextureWrap wrap) {
            this.wrapS = wrap;
            return this;
        }

        public Builder wrapT(TextureWrap wrap) {
            this.wrapT = wrap;
            return this;
        }

        public Builder generateMipmaps(boolean generate) {
            this.generateMipmaps = generate;
            return this;
        }

        public TextureDescriptor build() {
            return new TextureDescriptor(this);
        }
    }
}
