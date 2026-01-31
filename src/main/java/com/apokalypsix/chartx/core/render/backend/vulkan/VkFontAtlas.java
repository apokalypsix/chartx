package com.apokalypsix.chartx.core.render.backend.vulkan;

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
 * Font texture atlas for efficient text rendering with Vulkan.
 *
 * <p>Pre-renders ASCII glyphs (32-126) to a texture atlas using Java2D.
 * Each glyph's metrics (UV coords, width, advance) are stored for lookup.
 *
 * <p>The atlas is uploaded to Vulkan as a single-channel (R8) texture
 * for memory efficiency.
 */
public class VkFontAtlas {

    private static final Logger log = LoggerFactory.getLogger(VkFontAtlas.class);

    private static final int FIRST_CHAR = 32;  // Space
    private static final int LAST_CHAR = 126;  // Tilde
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;

    private final Font font;
    private final GlyphInfo[] glyphs = new GlyphInfo[CHAR_COUNT];

    private VkTexture texture;
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
    public VkFontAtlas(Font font) {
        this.font = font;
    }

    /**
     * Builds the atlas image and uploads it to Vulkan.
     *
     * @param device the Vulkan render device
     */
    public void initialize(VkRenderDevice device) {
        if (initialized) {
            dispose();
        }

        BufferedImage atlasImage = buildAtlasImage();
        uploadTexture(device, atlasImage);
        initialized = true;
    }

    /**
     * Builds the atlas image using Java2D.
     * No GPU context needed - can be called off the render thread.
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

        // Calculate atlas dimensions - arrange glyphs in a grid
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

        // Ensure minimum cell size
        int cellWidth = Math.max(maxGlyphWidth, maxAdvance + padding * 2);
        int cellHeight = Math.max(maxGlyphHeight, lineHeight + padding * 2);

        atlasWidth = nextPowerOfTwo(cellWidth * charsPerRow);
        atlasHeight = nextPowerOfTwo(cellHeight * rows);

        tempG2d.dispose();

        // Create the atlas image
        BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = atlas.createGraphics();

        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Clear to black (transparent in single-channel)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, atlasWidth, atlasHeight);

        // Draw glyphs
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);

        for (int i = 0; i < CHAR_COUNT; i++) {
            char c = (char) (FIRST_CHAR + i);

            int col = i % charsPerRow;
            int row = i / charsPerRow;

            int cellX = col * cellWidth;
            int cellY = row * cellHeight;

            // Get glyph metrics
            GlyphVector gv = font.createGlyphVector(frc, String.valueOf(c));
            Rectangle2D logicalBounds = gv.getLogicalBounds();
            Rectangle2D visualBounds = gv.getPixelBounds(frc, 0, 0);

            int advance = (int) Math.ceil(logicalBounds.getWidth());
            int glyphWidth = (int) Math.ceil(visualBounds.getWidth());
            int glyphHeight = (int) Math.ceil(visualBounds.getHeight());

            // Calculate offsets
            int xOffset = (int) visualBounds.getX();
            int yOffset = (int) -visualBounds.getY();

            // Draw position - baseline is at cellY + ascent + padding
            int drawX = cellX + padding - xOffset;
            int drawY = cellY + padding + ascent;

            // Draw the character
            g2d.drawString(String.valueOf(c), drawX, drawY);

            // Calculate UV coordinates
            float u0 = (cellX + padding) / (float) atlasWidth;
            float v0 = (cellY + padding) / (float) atlasHeight;
            float u1 = (cellX + padding + glyphWidth + 1) / (float) atlasWidth;
            float v1 = (cellY + padding + glyphHeight + 1) / (float) atlasHeight;

            // Store glyph info
            glyphs[i] = new GlyphInfo(
                    u0, v0, u1, v1,
                    glyphWidth + 1,
                    glyphHeight + 1,
                    xOffset,
                    yOffset,
                    advance
            );
        }

        g2d.dispose();
        log.debug("Built font atlas {}x{} for font {}", atlasWidth, atlasHeight, font.getName());
        return atlas;
    }

    /**
     * Uploads the atlas image to Vulkan as a single-channel texture.
     */
    private void uploadTexture(VkRenderDevice device, BufferedImage atlas) {
        // Create texture descriptor for R8 format
        TextureDescriptor descriptor = TextureDescriptor.fontAtlas(atlasWidth, atlasHeight);

        texture = (VkTexture) device.createTexture(descriptor);

        // Extract pixel data and upload
        byte[] pixels = ((DataBufferByte) atlas.getRaster().getDataBuffer()).getData();
        texture.uploadGrayscale(pixels, atlasWidth, atlasHeight);

        log.debug("Uploaded font atlas to Vulkan texture");
    }

    /**
     * Returns the glyph info for the specified character.
     * Returns info for space character if the character is out of range.
     */
    public GlyphInfo getGlyph(char c) {
        int index = c - FIRST_CHAR;
        if (index < 0 || index >= CHAR_COUNT) {
            return glyphs[0];  // Return space for out-of-range characters
        }
        return glyphs[index];
    }

    /**
     * Calculates the width of the given text in pixels.
     */
    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            GlyphInfo glyph = getGlyph(text.charAt(i));
            width += glyph.advance();
        }
        return width;
    }

    /**
     * Returns the texture.
     */
    public VkTexture getTexture() {
        return texture;
    }

    /**
     * Returns the line height in pixels.
     */
    public int getLineHeight() {
        return lineHeight;
    }

    /**
     * Returns the font ascent (distance from baseline to top).
     */
    public int getAscent() {
        return ascent;
    }

    /**
     * Returns true if the atlas has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Releases GPU resources.
     */
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        initialized = false;
    }

    /**
     * Returns the next power of two >= n.
     */
    private static int nextPowerOfTwo(int n) {
        int value = 1;
        while (value < n) {
            value <<= 1;
        }
        return value;
    }
}
