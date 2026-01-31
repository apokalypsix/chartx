package com.apokalypsix.chartx.core.render.gl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Batched text renderer using texture atlas for GL4 core profile compatibility.
 *
 * <p>This implementation replaces the JOGL awt.TextRenderer with a custom
 * texture atlas-based renderer that works with GL4 core profile on macOS.
 *
 * <p>Uses pre-rendered ASCII glyphs stored in a texture atlas. Each batch
 * builds textured quads that are drawn in a single draw call.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * textRenderer.beginBatch(gl, width, height);
 * textRenderer.drawText("100 x 150", x, y, Color.WHITE);
 * textRenderer.drawText("50 x 80", x2, y2, Color.GREEN);
 * textRenderer.endBatch(gl);
 * }</pre>
 */
public class TextRenderer {

    // Vertex format: position(2) + texcoord(2) + color(4) = 8 floats per vertex
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_QUAD = 6;  // 2 triangles
    private static final int INITIAL_CAPACITY = 1000;  // Initial number of quads

    // Shader source (GLSL 150 for core profile)
    private static final String VERTEX_SHADER = """
            #version 150
            in vec2 aPosition;
            in vec2 aTexCoord;
            in vec4 aColor;
            uniform mat4 uProjection;
            out vec2 vTexCoord;
            out vec4 vColor;

            void main() {
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 150
            in vec2 vTexCoord;
            in vec4 vColor;
            uniform sampler2D uTexture;
            out vec4 fragColor;

            void main() {
                float alpha = texture(uTexture, vTexCoord).r;
                if (alpha < 0.01) discard;
                fragColor = vec4(vColor.rgb, vColor.a * alpha);
            }
            """;

    // Font configuration
    private Font font;
    private float fontSize = 10f;
    private String fontFamily = Font.MONOSPACED;
    private float scaleFactor = 1.0f;

    // GL resources
    private ShaderProgram shader;
    private VertexBuffer vertexBuffer;
    private boolean resourcesInitialized = false;

    // Font atlas cache (by font size)
    private final Map<Integer, FontAtlas> atlasCache = new HashMap<>();
    private FontAtlas currentAtlas;

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
    public TextRenderer() {
        this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
    }

    /**
     * Creates a text renderer with specified font size.
     */
    public TextRenderer(float fontSize) {
        this.fontSize = fontSize;
        this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
    }

    /**
     * Sets the font size (in logical pixels).
     * The actual rendered font will be scaled by the scale factor for HiDPI displays.
     */
    public void setFontSize(float size) {
        if (size != this.fontSize) {
            this.fontSize = size;
            // Create font at scaled size for HiDPI
            int scaledSize = (int) (size * scaleFactor);
            this.font = new Font(fontFamily, Font.PLAIN, Math.max(1, scaledSize));
        }
    }

    public float getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font family.
     */
    public void setFontFamily(String family) {
        if (!family.equals(this.fontFamily)) {
            this.fontFamily = family;
            // Create font at scaled size for HiDPI
            int scaledSize = (int) (fontSize * scaleFactor);
            this.font = new Font(fontFamily, Font.PLAIN, Math.max(1, scaledSize));
        }
    }

    /**
     * Sets the display scale factor for HiDPI displays.
     * This triggers recreation of the font at the scaled size.
     */
    public void setScaleFactor(float scale) {
        float newScale = Math.max(1.0f, scale);
        if (newScale != this.scaleFactor) {
            this.scaleFactor = newScale;
            // Recreate font at new scaled size
            int scaledSize = (int) (fontSize * scaleFactor);
            this.font = new Font(fontFamily, Font.PLAIN, Math.max(1, scaledSize));
        }
    }

    /**
     * Gets the display scale factor.
     */
    public float getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Initializes GL resources. Called lazily on first use.
     */
    private void initializeResources(GL2ES2 gl) {
        if (resourcesInitialized) {
            return;
        }

        // Compile shader
        shader = new ShaderProgram();
        if (!shader.compile(gl, VERTEX_SHADER, FRAGMENT_SHADER)) {
            System.err.println("[ChartX] Failed to compile text shader");
            return;
        }

        // Bind attribute locations after compile but we need to relink
        // Actually, we should bind before linking - let's fix this
        // The ShaderProgram.compile binds aPosition=0, aColor=1 by default
        // We need aPosition=0, aTexCoord=1, aColor=2 for text shader
        // Since ShaderProgram doesn't support custom bindings easily,
        // we'll just query the locations at runtime

        // Create vertex buffer
        vertexBuffer = new VertexBuffer(FLOATS_PER_VERTEX, true);
        vertexBuffer.initialize(gl, INITIAL_CAPACITY * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);

        // Configure attributes based on shader locations
        int posLoc = shader.getAttributeLocation(gl, "aPosition");
        int texLoc = shader.getAttributeLocation(gl, "aTexCoord");
        int colorLoc = shader.getAttributeLocation(gl, "aColor");

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        vertexBuffer.configureAttribute(gl, posLoc, 2, stride, 0);
        vertexBuffer.configureAttribute(gl, texLoc, 2, stride, 2 * Float.BYTES);
        vertexBuffer.configureAttribute(gl, colorLoc, 4, stride, 4 * Float.BYTES);

        // Initialize batch data array
        batchData = new float[INITIAL_CAPACITY * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];

        resourcesInitialized = true;
    }

    /**
     * Gets or creates a font atlas for the current (scaled) font size.
     */
    private FontAtlas getOrCreateAtlas(GL2ES2 gl) {
        // Cache key is the actual (scaled) font size
        int scaledSize = (int) (fontSize * scaleFactor);
        FontAtlas atlas = atlasCache.get(scaledSize);
        if (atlas == null) {
            atlas = new FontAtlas(font);  // font is already at scaled size
            atlas.initialize(gl);
            atlasCache.put(scaledSize, atlas);
        }
        return atlas;
    }

    /**
     * Returns true if GL2 text rendering is available.
     * Always returns true for texture atlas implementation.
     */
    public boolean isGL2Available() {
        return true;  // Texture atlas works with all GL profiles
    }

    /**
     * Begins a text rendering batch.
     *
     * @param gl the GL context
     * @param width screen width
     * @param height screen height
     * @return true if batch was started
     */
    public boolean beginBatch(GL2ES2 gl, int width, int height) {
        if (inBatch) {
            resetBatchState();
        }

        initializeResources(gl);
        if (!resourcesInitialized) {
            return false;
        }

        this.screenWidth = width;
        this.screenHeight = height;
        this.batchOffset = 0;
        this.inBatch = true;
        this.currentAtlas = getOrCreateAtlas(gl);

        return true;
    }

    /**
     * Resets the batch state. Call this if an exception occurs during rendering.
     */
    public void resetBatchState() {
        inBatch = false;
        batchOffset = 0;
    }

    /**
     * Adds text to the batch.
     *
     * @param text the text to render
     * @param x screen x position (left edge)
     * @param y screen y position (baseline)
     * @param color text color
     */
    public void drawText(String text, float x, float y, Color color) {
        if (!inBatch || text == null || text.isEmpty()) {
            return;
        }

        buildTextQuads(text, x, y, color);
    }

    /**
     * Adds centered text to the batch.
     *
     * @param text the text to render
     * @param centerX center x position
     * @param y screen y position (baseline)
     * @param color text color
     */
    public void drawTextCentered(String text, float centerX, float y, Color color) {
        if (!inBatch || text == null || text.isEmpty()) {
            return;
        }

        float width = currentAtlas.getTextWidth(text);
        float x = centerX - width / 2;
        buildTextQuads(text, x, y, color);
    }

    /**
     * Adds right-aligned text to the batch.
     *
     * @param text the text to render
     * @param rightX right edge x position
     * @param y screen y position (baseline)
     * @param color text color
     */
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
            FontAtlas.GlyphInfo glyph = currentAtlas.getGlyph(c);

            // Skip non-printable characters
            if (glyph.width() <= 0) {
                cursorX += glyph.advance();
                continue;
            }

            // Ensure capacity
            ensureCapacity(VERTICES_PER_QUAD * FLOATS_PER_VERTEX);

            // Calculate quad positions (screen coordinates, Y-down)
            // y is the baseline position, glyph.yOffset is distance from baseline to top
            float x0 = cursorX + glyph.xOffset();
            float y0 = y - glyph.yOffset();
            float x1 = x0 + glyph.width();
            float y1 = y0 + glyph.height();

            // UV coordinates
            float u0 = glyph.u0();
            float v0 = glyph.v0();
            float u1 = glyph.u1();
            float v1 = glyph.v1();

            // Build two triangles (6 vertices)
            // Triangle 1: top-left, bottom-left, top-right
            addVertex(x0, y0, u0, v0, r, g, b, a);
            addVertex(x0, y1, u0, v1, r, g, b, a);
            addVertex(x1, y0, u1, v0, r, g, b, a);

            // Triangle 2: top-right, bottom-left, bottom-right
            addVertex(x1, y0, u1, v0, r, g, b, a);
            addVertex(x0, y1, u0, v1, r, g, b, a);
            addVertex(x1, y1, u1, v1, r, g, b, a);

            cursorX += glyph.advance();
        }
    }

    /**
     * Adds a single vertex to the batch.
     */
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

    /**
     * Ensures the batch data array has room for more vertices.
     */
    private void ensureCapacity(int additionalFloats) {
        if (batchOffset + additionalFloats > batchData.length) {
            int newCapacity = Math.max(batchData.length * 2, batchOffset + additionalFloats);
            float[] newData = new float[newCapacity];
            System.arraycopy(batchData, 0, newData, 0, batchOffset);
            batchData = newData;
        }
    }

    /**
     * Ends the batch and renders all text.
     *
     * @param gl the GL context
     */
    public void endBatch(GL2ES2 gl) {
        if (!inBatch) {
            return;
        }

        try {
            if (batchOffset > 0) {
                // Setup GL state
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

                // Use shader
                shader.use(gl);

                // Set projection matrix (screen coordinates, Y-down, origin top-left)
                float[] projection = GLResourceManager.createOrthoMatrix(
                        0, screenWidth, screenHeight, 0);
                shader.setUniformMatrix4fv(gl, "uProjection", false, projection);

                // Bind texture
                currentAtlas.bind(gl, 0);
                shader.setUniform1i(gl, "uTexture", 0);

                // Upload and draw vertices
                vertexBuffer.upload(gl, batchData, 0, batchOffset);
                vertexBuffer.draw(gl, GL.GL_TRIANGLES);

                // Cleanup
                currentAtlas.unbind(gl);
                shader.unuse(gl);
                gl.glDisable(GL.GL_BLEND);
            }
        } finally {
            batchOffset = 0;
            inBatch = false;
        }
    }

    /**
     * Renders a single text string immediately (not batched).
     */
    public void drawImmediate(GL2ES2 gl, String text, float x, float y,
                              Color color, int width, int height) {
        beginBatch(gl, width, height);
        drawText(text, x, y, color);
        endBatch(gl);
    }

    /**
     * Returns the width of the given text in physical pixels.
     */
    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // If we have a current atlas (in batch), use it
        if (currentAtlas != null && currentAtlas.isInitialized()) {
            return currentAtlas.getTextWidth(text);
        }

        // Approximate width using scaled font size
        return text.length() * fontSize * scaleFactor * 0.6f;
    }

    /**
     * Returns the height of a line of text in physical pixels.
     * This returns the scaled font size for proper layout on HiDPI displays.
     */
    public float getTextHeight() {
        return fontSize * scaleFactor;
    }

    /**
     * Returns the bounds of the given text in physical pixels.
     */
    public Rectangle2D getTextBounds(String text) {
        float width = getTextWidth(text);
        return new Rectangle2D.Float(0, 0, width, fontSize * scaleFactor);
    }

    /**
     * Disposes OpenGL resources.
     */
    public void dispose(GL2ES2 gl) {
        if (shader != null) {
            shader.dispose(gl);
            shader = null;
        }

        if (vertexBuffer != null) {
            vertexBuffer.dispose(gl);
            vertexBuffer = null;
        }

        for (FontAtlas atlas : atlasCache.values()) {
            atlas.dispose(gl);
        }
        atlasCache.clear();

        resourcesInitialized = false;
        currentAtlas = null;
    }

    /**
     * Returns true if currently in batch mode.
     */
    public boolean isInBatch() {
        return inBatch;
    }

    /**
     * Returns the number of vertices in the current batch.
     */
    public int getBatchSize() {
        return batchOffset / FLOATS_PER_VERTEX;
    }
}
