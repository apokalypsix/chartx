package com.apokalypsix.chartx.core.render.backend.dx12;

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
 * Font texture atlas for efficient text rendering with DirectX 12.
 *
 * <p>Pre-renders ASCII glyphs (32-126) to a texture atlas using Java2D.
 * Each glyph's metrics (UV coords, width, advance) are stored for lookup.
 */
public class DX12FontAtlas {

    private static final Logger log = LoggerFactory.getLogger(DX12FontAtlas.class);

    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;

    private final Font font;
    private final GlyphInfo[] glyphs = new GlyphInfo[CHAR_COUNT];

    private DX12Texture texture;
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
            float u0, float v0,
            float u1, float v1,
            int width,
            int height,
            int xOffset,
            int yOffset,
            int advance
    ) {}

    public DX12FontAtlas(Font font) {
        this.font = font;
    }

    public void initialize(DX12RenderDevice device) {
        if (initialized) {
            dispose();
        }

        BufferedImage atlasImage = buildAtlasImage();
        uploadTexture(device, atlasImage);
        initialized = true;
    }

    private BufferedImage buildAtlasImage() {
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D tempG2d = tempImage.createGraphics();
        tempG2d.setFont(font);
        FontMetrics metrics = tempG2d.getFontMetrics();
        FontRenderContext frc = tempG2d.getFontRenderContext();

        lineHeight = metrics.getHeight();
        ascent = metrics.getAscent();
        maxAdvance = metrics.getMaxAdvance();

        int padding = 2;
        int charsPerRow = 16;
        int rows = (CHAR_COUNT + charsPerRow - 1) / charsPerRow;

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

    private void uploadTexture(DX12RenderDevice device, BufferedImage atlasImage) {
        byte[] pixels = ((DataBufferByte) atlasImage.getRaster().getDataBuffer()).getData();

        TextureDescriptor desc = TextureDescriptor.fontAtlas(atlasWidth, atlasHeight);

        texture = new DX12Texture(device, desc);
        texture.uploadGrayscale(pixels, atlasWidth, atlasHeight);

        log.debug("Created DX12 font atlas: {}x{}, {} glyphs", atlasWidth, atlasHeight, CHAR_COUNT);
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    public GlyphInfo getGlyph(char c) {
        if (c >= FIRST_CHAR && c <= LAST_CHAR) {
            return glyphs[c - FIRST_CHAR];
        }
        return glyphs[0];
    }

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

    public DX12Texture getTexture() {
        return texture;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        initialized = false;
    }
}
