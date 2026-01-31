package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.TextureDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Font texture atlas for efficient text rendering with Metal.
 *
 * <p>Pre-renders ASCII glyphs (32-126) to a texture atlas using Java2D.
 * Each glyph's metrics (UV coords, width, advance) are stored for lookup.
 *
 * <p>The atlas is uploaded to Metal as a single-channel (R8) texture.
 */
public class MetalFontAtlas {

    private static final Logger log = LoggerFactory.getLogger(MetalFontAtlas.class);

    private static final int FIRST_CHAR = 32;  // Space
    private static final int LAST_CHAR = 126;  // Tilde
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;

    private final Font font;
    private final GlyphInfo[] glyphs = new GlyphInfo[CHAR_COUNT];

    private MetalTexture texture;
    private int atlasWidth;
    private int atlasHeight;
    private int lineHeight;
    private int ascent;
    private int maxAdvance;

    private boolean initialized = false;

    /**
     * Glyph metrics and UV coordinates.
     */
    public record GlyphInfo(
            float u0, float v0,       // Top-left UV
            float u1, float v1,       // Bottom-right UV
            int width,                // Glyph width in pixels
            int height,               // Glyph height in pixels
            int xOffset,              // X offset from cursor to glyph origin
            int yOffset,              // Y offset from baseline to glyph top
            int advance               // Horizontal advance to next glyph
    ) {}

    /**
     * Creates a font atlas for the specified font.
     *
     * @param font the font to render
     */
    public MetalFontAtlas(Font font) {
        this.font = font;
    }

    /**
     * Builds the atlas image and uploads it to Metal.
     *
     * @param device the Metal render device
     */
    public void initialize(MetalRenderDevice device) {
        if (initialized) {
            dispose();
        }

        BufferedImage atlasImage = buildAtlasImage();
        uploadTexture(device, atlasImage);
        initialized = true;
    }

    /**
     * Builds the atlas image using Java2D.
     */
    private BufferedImage buildAtlasImage() {
        // Create a temporary image to measure glyphs
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D tempG2d = tempImage.createGraphics();
        tempG2d.setFont(font);
        FontMetrics metrics = tempG2d.getFontMetrics();
        FontRenderContext frc = tempG2d.getFontRenderContext();

        lineHeight = metrics.getHeight();
        ascent = metrics.getAscent();
        maxAdvance = metrics.getMaxAdvance();

        // Calculate atlas dimensions
        int padding = 2;
        int charsPerRow = 16;
        int rows = (CHAR_COUNT + charsPerRow - 1) / charsPerRow;

        // Measure max glyph dimensions
        int maxGlyphWidth = 0;
        int maxGlyphHeight = 0;

        for (int i = 0; i < CHAR_COUNT; i++) {
            char c = (char) (FIRST_CHAR + i);
            GlyphVector gv = font.createGlyphVector(frc, String.valueOf(c));
            Rectangle2D bounds = gv.getPixelBounds(frc, 0, 0);
            maxGlyphWidth = Math.max(maxGlyphWidth, (int) Math.ceil(bounds.getWidth()) + padding * 2);
            maxGlyphHeight = Math.max(maxGlyphHeight, (int) Math.ceil(bounds.getHeight()) + padding * 2);
        }

        int cellWidth = Math.max(maxGlyphWidth, maxAdvance + padding * 2);
        int cellHeight = Math.max(maxGlyphHeight, lineHeight + padding * 2);

        atlasWidth = nextPowerOfTwo(cellWidth * charsPerRow);
        atlasHeight = nextPowerOfTwo(cellHeight * rows);

        tempG2d.dispose();

        // Create the atlas image
        BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = atlas.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, atlasWidth, atlasHeight);

        g2d.setFont(font);
        g2d.setColor(Color.WHITE);

        for (int i = 0; i < CHAR_COUNT; i++) {
            char c = (char) (FIRST_CHAR + i);

            int col = i % charsPerRow;
            int row = i / charsPerRow;

            int cellX = col * cellWidth;
            int cellY = row * cellHeight;

            GlyphVector gv = font.createGlyphVector(frc, String.valueOf(c));
            Rectangle2D logicalBounds = gv.getLogicalBounds();
            Rectangle2D visualBounds = gv.getPixelBounds(frc, 0, 0);

            int advance = (int) Math.ceil(logicalBounds.getWidth());
            int glyphWidth = (int) Math.ceil(visualBounds.getWidth());
            int glyphHeight = (int) Math.ceil(visualBounds.getHeight());

            int xOffset = (int) Math.floor(visualBounds.getX());
            int yOffset = (int) Math.floor(-visualBounds.getY());

            int drawX = cellX + padding - xOffset;
            int drawY = cellY + padding + yOffset;

            g2d.drawString(String.valueOf(c), drawX, drawY);

            float u0 = (cellX + padding) / (float) atlasWidth;
            float v0 = (cellY + padding) / (float) atlasHeight;
            float u1 = (cellX + padding + glyphWidth) / (float) atlasWidth;
            float v1 = (cellY + padding + glyphHeight) / (float) atlasHeight;

            glyphs[i] = new GlyphInfo(u0, v0, u1, v1, glyphWidth, glyphHeight, xOffset, yOffset, advance);
        }

        g2d.dispose();
        return atlas;
    }

    private void uploadTexture(MetalRenderDevice device, BufferedImage atlasImage) {
        byte[] pixels = ((DataBufferByte) atlasImage.getRaster().getDataBuffer()).getData();

        TextureDescriptor desc = TextureDescriptor.fontAtlas(atlasWidth, atlasHeight);

        texture = new MetalTexture(device, desc);
        texture.uploadGrayscale(pixels, atlasWidth, atlasHeight);

        log.debug("Created Metal font atlas: {}x{}, {} glyphs", atlasWidth, atlasHeight, CHAR_COUNT);
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    /**
     * Returns the glyph info for a character.
     */
    public GlyphInfo getGlyph(char c) {
        if (c >= FIRST_CHAR && c <= LAST_CHAR) {
            return glyphs[c - FIRST_CHAR];
        }
        return glyphs[0]; // Return space for unknown characters
    }

    /**
     * Returns the width of a string in pixels.
     */
    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += getGlyph(text.charAt(i)).advance();
        }
        return width;
    }

    /**
     * Returns the texture containing the atlas.
     */
    public MetalTexture getTexture() {
        return texture;
    }

    /**
     * Returns true if the atlas has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Disposes Metal resources.
     */
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        initialized = false;
    }
}
