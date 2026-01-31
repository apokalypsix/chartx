package com.apokalypsix.chartx.core.render.backend.metal;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Batched text renderer using texture atlas for Metal.
 *
 * <p>This implementation renders text using pre-rendered ASCII glyphs
 * stored in a texture atlas. Each batch builds textured quads that
 * are drawn in a single draw call.
 */
public class MetalTextRenderer implements TextRenderer {

    private static final Logger log = LoggerFactory.getLogger(MetalTextRenderer.class);

    // Vertex format: position(2) + texcoord(2) + color(4) = 8 floats per vertex
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_QUAD = 6;  // 2 triangles
    private static final int INITIAL_CAPACITY = 1000;

    // Font configuration
    private Font font;
    private float fontSize = 10f;
    private String fontFamily = Font.MONOSPACED;

    // Metal resources
    private final MetalRenderDevice device;
    private final MetalResourceManager resources;
    private MetalBuffer vertexBuffer;
    private MetalShader textShader;
    private boolean resourcesInitialized = false;

    // Font atlas cache (by font size)
    private final Map<Integer, MetalFontAtlas> atlasCache = new HashMap<>();
    private MetalFontAtlas currentAtlas;

    // Batch state
    private float[] batchData;
    private int batchOffset;
    private boolean inBatch = false;

    // Screen dimensions for current batch
    private int screenWidth;
    private int screenHeight;

    /**
     * Creates a text renderer with default settings.
     */
    public MetalTextRenderer(MetalRenderDevice device, MetalResourceManager resources) {
        this.device = device;
        this.resources = resources;
        this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
    }

    /**
     * Creates a text renderer with specified font size.
     */
    public MetalTextRenderer(MetalRenderDevice device, MetalResourceManager resources, float fontSize) {
        this.device = device;
        this.resources = resources;
        this.fontSize = fontSize;
        this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
    }

    @Override
    public void setFontSize(float size) {
        if (size != this.fontSize) {
            this.fontSize = size;
            this.font = new Font(fontFamily, Font.PLAIN, (int) size);
        }
    }

    @Override
    public float getFontSize() {
        return fontSize;
    }

    @Override
    public void setFontFamily(String family) {
        if (!family.equals(this.fontFamily)) {
            this.fontFamily = family;
            this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
        }
    }

    /**
     * Initializes Metal resources. Called lazily on first use.
     */
    private void initializeResources() {
        if (resourcesInitialized) {
            return;
        }

        // Get text shader
        textShader = (MetalShader) resources.getShader(ResourceManager.SHADER_TEXT);
        if (textShader == null || !textShader.isValid()) {
            log.error("Text shader not available");
            return;
        }

        // Create vertex buffer for text quads
        BufferDescriptor bufferDesc = BufferDescriptor.textBuffer(INITIAL_CAPACITY * VERTICES_PER_QUAD);
        vertexBuffer = (MetalBuffer) resources.getOrCreateBuffer("metal_text_renderer", bufferDesc);

        // Initialize batch data array
        batchData = new float[INITIAL_CAPACITY * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];

        resourcesInitialized = true;
        log.debug("MetalTextRenderer resources initialized");
    }

    /**
     * Gets or creates a font atlas for the current font size.
     */
    private MetalFontAtlas getOrCreateAtlas() {
        int key = (int) fontSize;
        MetalFontAtlas atlas = atlasCache.get(key);
        if (atlas == null) {
            atlas = new MetalFontAtlas(font);
            atlas.initialize(device);
            atlasCache.put(key, atlas);
        }
        return atlas;
    }

    @Override
    public boolean beginBatch(int width, int height) {
        if (inBatch) {
            resetBatchState();
        }

        initializeResources();
        if (!resourcesInitialized) {
            return false;
        }

        this.screenWidth = width;
        this.screenHeight = height;
        this.batchOffset = 0;
        this.inBatch = true;
        this.currentAtlas = getOrCreateAtlas();

        return true;
    }

    /**
     * Resets the batch state.
     */
    private void resetBatchState() {
        inBatch = false;
        batchOffset = 0;
    }

    @Override
    public void drawText(String text, float x, float y, Color color) {
        if (!inBatch || text == null || text.isEmpty()) {
            return;
        }
        buildTextQuads(text, x, y, color);
    }

    @Override
    public void drawTextCentered(String text, float centerX, float y, Color color) {
        if (!inBatch || text == null || text.isEmpty()) {
            return;
        }
        float width = currentAtlas.getTextWidth(text);
        float x = centerX - width / 2;
        buildTextQuads(text, x, y, color);
    }

    @Override
    public void drawTextRight(String text, float rightX, float y, Color color) {
        if (!inBatch || text == null || text.isEmpty()) {
            return;
        }
        float width = currentAtlas.getTextWidth(text);
        float x = rightX - width;
        buildTextQuads(text, x, y, color);
    }

    /**
     * Builds textured quads for the given text string.
     */
    private void buildTextQuads(String text, float x, float y, Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        float cursorX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            MetalFontAtlas.GlyphInfo glyph = currentAtlas.getGlyph(c);

            if (glyph.width() <= 0) {
                cursorX += glyph.advance();
                continue;
            }

            ensureCapacity(VERTICES_PER_QUAD * FLOATS_PER_VERTEX);

            float x0 = cursorX + glyph.xOffset();
            float y0 = y - glyph.yOffset();
            float x1 = x0 + glyph.width();
            float y1 = y0 + glyph.height();

            float u0 = glyph.u0();
            float v0 = glyph.v0();
            float u1 = glyph.u1();
            float v1 = glyph.v1();

            // Build two triangles (6 vertices)
            addVertex(x0, y0, u0, v0, r, g, b, a);
            addVertex(x0, y1, u0, v1, r, g, b, a);
            addVertex(x1, y0, u1, v0, r, g, b, a);

            addVertex(x1, y0, u1, v0, r, g, b, a);
            addVertex(x0, y1, u0, v1, r, g, b, a);
            addVertex(x1, y1, u1, v1, r, g, b, a);

            cursorX += glyph.advance();
        }
    }

    private void addVertex(float x, float y, float u, float v, float r, float g, float b, float a) {
        batchData[batchOffset++] = x;
        batchData[batchOffset++] = y;
        batchData[batchOffset++] = u;
        batchData[batchOffset++] = v;
        batchData[batchOffset++] = r;
        batchData[batchOffset++] = g;
        batchData[batchOffset++] = b;
        batchData[batchOffset++] = a;
    }

    private void ensureCapacity(int additionalFloats) {
        if (batchOffset + additionalFloats > batchData.length) {
            int newCapacity = Math.max(batchData.length * 2, batchOffset + additionalFloats);
            float[] newData = new float[newCapacity];
            System.arraycopy(batchData, 0, newData, 0, batchOffset);
            batchData = newData;
        }
    }

    @Override
    public void endBatch() {
        if (!inBatch) {
            return;
        }

        try {
            if (batchOffset > 0) {
                renderBatch();
            }
        } finally {
            batchOffset = 0;
            inBatch = false;
        }
    }

    private void renderBatch() {
        // Bind font atlas texture
        MetalTexture atlasTexture = currentAtlas.getTexture();
        if (atlasTexture == null || !atlasTexture.isInitialized()) {
            log.warn("Font atlas texture not ready");
            return;
        }

        // Set blend mode for text
        device.setBlendMode(BlendMode.ALPHA);

        // Bind shader
        textShader.bind();

        // Set projection matrix
        float[] projection = MetalResourceManager.createOrthoMatrix(0, screenWidth, screenHeight, 0);
        textShader.setUniformMatrix4("uProjection", projection);

        // Bind texture
        atlasTexture.bind(0);

        // Upload and draw vertices
        int vertexCount = batchOffset / FLOATS_PER_VERTEX;
        vertexBuffer.upload(batchData, 0, batchOffset);
        vertexBuffer.setVertexCount(vertexCount);
        vertexBuffer.bind();
        vertexBuffer.draw(DrawMode.TRIANGLES);

        atlasTexture.unbind();
        textShader.unbind();
    }

    @Override
    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (currentAtlas != null && currentAtlas.isInitialized()) {
            return currentAtlas.getTextWidth(text);
        }
        return text.length() * fontSize * 0.6f;
    }

    @Override
    public float getTextHeight() {
        return fontSize;
    }

    @Override
    public boolean isInBatch() {
        return inBatch;
    }

    @Override
    public void dispose() {
        for (MetalFontAtlas atlas : atlasCache.values()) {
            atlas.dispose();
        }
        atlasCache.clear();
        resourcesInitialized = false;
        currentAtlas = null;
    }
}
