package com.apokalypsix.chartx.core.render.backend.vulkan;

import com.apokalypsix.chartx.core.render.api.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Batched text renderer using texture atlas for Vulkan.
 *
 * <p>This implementation renders text using pre-rendered ASCII glyphs
 * stored in a texture atlas. Each batch builds textured quads that
 * are drawn in a single draw call.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * textRenderer.beginBatch(device, width, height);
 * textRenderer.drawText("100 x 150", x, y, Color.WHITE);
 * textRenderer.drawText("50 x 80", x2, y2, Color.GREEN);
 * textRenderer.endBatch();
 * }</pre>
 */
public class VkTextRenderer {

    private static final Logger log = LoggerFactory.getLogger(VkTextRenderer.class);

    // Vertex format: position(2) + texcoord(2) + color(4) = 8 floats per vertex
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_QUAD = 6;  // 2 triangles
    private static final int INITIAL_CAPACITY = 1000;  // Initial number of quads

    // Font configuration
    private Font font;
    private float fontSize = 10f;
    private String fontFamily = Font.MONOSPACED;

    // Vulkan resources
    private VkRenderDevice device;
    private VkResourceManager resources;
    private VkBuffer vertexBuffer;
    private VkShader textShader;
    private boolean resourcesInitialized = false;

    // Descriptor pool and set for texture binding
    private long descriptorPool = VK_NULL_HANDLE;
    private long descriptorSet = VK_NULL_HANDLE;
    private VkTexture boundTexture;

    // Font atlas cache (by font size)
    private final Map<Integer, VkFontAtlas> atlasCache = new HashMap<>();
    private VkFontAtlas currentAtlas;

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
    public VkTextRenderer() {
        this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
    }

    /**
     * Creates a text renderer with specified font size.
     */
    public VkTextRenderer(float fontSize) {
        this.fontSize = fontSize;
        this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
    }

    /**
     * Sets the font size.
     */
    public void setFontSize(float size) {
        if (size != this.fontSize) {
            this.fontSize = size;
            this.font = new Font(fontFamily, Font.PLAIN, (int) size);
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
            this.font = new Font(fontFamily, Font.PLAIN, (int) fontSize);
        }
    }

    /**
     * Initializes Vulkan resources. Called lazily on first use.
     */
    private void initializeResources(VkRenderDevice device, VkResourceManager resources) {
        if (resourcesInitialized) {
            return;
        }

        this.device = device;
        this.resources = resources;

        // Get text shader
        textShader = (VkShader) resources.getShader(ResourceManager.SHADER_TEXT);
        if (textShader == null || !textShader.isValid()) {
            log.error("Text shader not available");
            return;
        }

        // Create vertex buffer for text quads
        BufferDescriptor bufferDesc = BufferDescriptor.textBuffer(INITIAL_CAPACITY * VERTICES_PER_QUAD);
        vertexBuffer = (VkBuffer) resources.getOrCreateBuffer("vk_text_renderer", bufferDesc);

        // Create descriptor pool
        createDescriptorPool();

        // Initialize batch data array
        batchData = new float[INITIAL_CAPACITY * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];

        resourcesInitialized = true;
        log.debug("VkTextRenderer resources initialized");
    }

    private void createDescriptorPool() {
        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null) return;

        try (MemoryStack stack = stackPush()) {
            // Pool size for combined image samplers
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, stack);
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(10); // Allow up to 10 font atlases

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .maxSets(10)
                    .pPoolSizes(poolSizes);

            LongBuffer pDescriptorPool = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateDescriptorPool(vkDevice, poolInfo, null, pDescriptorPool);
            if (result != VK_SUCCESS) {
                log.error("Failed to create descriptor pool: {}", result);
                return;
            }

            descriptorPool = pDescriptorPool.get(0);
            log.debug("Created descriptor pool for text rendering");
        }
    }

    /**
     * Allocates and updates a descriptor set for the given texture.
     */
    private void bindTexture(VkTexture texture) {
        if (texture == boundTexture && descriptorSet != VK_NULL_HANDLE) {
            return; // Already bound
        }

        VkDevice vkDevice = device.getDevice();
        if (vkDevice == null || descriptorPool == VK_NULL_HANDLE) return;

        try (MemoryStack stack = stackPush()) {
            // Free previous descriptor set if exists
            if (descriptorSet != VK_NULL_HANDLE) {
                vkFreeDescriptorSets(vkDevice, descriptorPool, descriptorSet);
                descriptorSet = VK_NULL_HANDLE;
            }

            // Allocate new descriptor set
            long descriptorSetLayout = textShader.getDescriptorSetLayout();
            if (descriptorSetLayout == VK_NULL_HANDLE) {
                log.error("Text shader has no descriptor set layout");
                return;
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(stack.longs(descriptorSetLayout));

            LongBuffer pDescriptorSet = stack.longs(VK_NULL_HANDLE);
            int result = vkAllocateDescriptorSets(vkDevice, allocInfo, pDescriptorSet);
            if (result != VK_SUCCESS) {
                log.error("Failed to allocate descriptor set: {}", result);
                return;
            }

            descriptorSet = pDescriptorSet.get(0);

            // Update descriptor set with texture
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.get(0)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(texture.getImageView())
                    .sampler(texture.getSampler());

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
            descriptorWrites.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet)
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            vkUpdateDescriptorSets(vkDevice, descriptorWrites, null);
            boundTexture = texture;

            log.debug("Bound texture to descriptor set");
        }
    }

    /**
     * Gets or creates a font atlas for the current font size.
     */
    private VkFontAtlas getOrCreateAtlas() {
        int key = (int) fontSize;
        VkFontAtlas atlas = atlasCache.get(key);
        if (atlas == null) {
            atlas = new VkFontAtlas(font);
            atlas.initialize(device);
            atlasCache.put(key, atlas);
        }
        return atlas;
    }

    /**
     * Returns true if Vulkan text rendering is available.
     */
    public boolean isAvailable() {
        return resourcesInitialized && textShader != null && textShader.isValid();
    }

    /**
     * Begins a text rendering batch.
     *
     * @param device the Vulkan render device
     * @param resources the resource manager
     * @param width screen width
     * @param height screen height
     * @return true if batch was started
     */
    public boolean beginBatch(VkRenderDevice device, VkResourceManager resources, int width, int height) {
        if (inBatch) {
            resetBatchState();
        }

        initializeResources(device, resources);
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
            VkFontAtlas.GlyphInfo glyph = currentAtlas.getGlyph(c);

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
     */
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
        VkCommandBuffer cmd = device.getCommandBuffer();
        if (cmd == null) return;

        // Bind font atlas texture
        VkTexture atlasTexture = currentAtlas.getTexture();
        if (atlasTexture == null || !atlasTexture.isInitialized()) {
            log.warn("Font atlas texture not ready");
            return;
        }
        bindTexture(atlasTexture);

        // Set blend mode for text
        device.setBlendMode(BlendMode.ALPHA);

        // Bind shader
        textShader.bind();

        // Set projection matrix (screen coordinates, Y-down, origin top-left)
        float[] projection = createOrthoMatrix(0, screenWidth, screenHeight, 0);
        textShader.setUniformMatrix4("uProjection", projection);
        textShader.flushPushConstants();

        // Get or create pipeline for text rendering
        VkPipelineCache pipelineCache = resources.getPipelineCache();
        VkPipeline pipeline = pipelineCache.getPipeline(textShader, vertexBuffer.getDescriptor(),
                DrawMode.TRIANGLES, BlendMode.ALPHA);

        // Bind pipeline
        pipeline.bind(cmd);

        // Bind descriptor set
        if (descriptorSet != VK_NULL_HANDLE) {
            try (MemoryStack stack = stackPush()) {
                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        textShader.getPipelineLayout(), 0,
                        stack.longs(descriptorSet), null);
            }
        }

        // Upload and draw vertices
        int vertexCount = batchOffset / FLOATS_PER_VERTEX;
        vertexBuffer.upload(batchData, 0, batchOffset);
        vertexBuffer.setVertexCount(vertexCount);
        vertexBuffer.bind();

        // Draw
        vkCmdDraw(cmd, vertexCount, 1, 0, 0);

        textShader.unbind();
    }

    /**
     * Creates an orthographic projection matrix.
     */
    private float[] createOrthoMatrix(float left, float right, float bottom, float top) {
        float[] matrix = new float[16];
        float zNear = -1.0f;
        float zFar = 1.0f;

        matrix[0] = 2.0f / (right - left);
        matrix[1] = 0;
        matrix[2] = 0;
        matrix[3] = 0;

        matrix[4] = 0;
        matrix[5] = 2.0f / (top - bottom);
        matrix[6] = 0;
        matrix[7] = 0;

        matrix[8] = 0;
        matrix[9] = 0;
        matrix[10] = -2.0f / (zFar - zNear);
        matrix[11] = 0;

        matrix[12] = -(right + left) / (right - left);
        matrix[13] = -(top + bottom) / (top - bottom);
        matrix[14] = -(zFar + zNear) / (zFar - zNear);
        matrix[15] = 1;

        return matrix;
    }

    /**
     * Renders a single text string immediately (not batched).
     */
    public void drawImmediate(VkRenderDevice device, VkResourceManager resources,
                               String text, float x, float y,
                               Color color, int width, int height) {
        beginBatch(device, resources, width, height);
        drawText(text, x, y, color);
        endBatch();
    }

    /**
     * Returns the width of the given text in pixels.
     */
    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // If we have a current atlas (in batch), use it
        if (currentAtlas != null && currentAtlas.isInitialized()) {
            return currentAtlas.getTextWidth(text);
        }

        // Approximate width: assume monospace with ~0.6 character width ratio
        return text.length() * fontSize * 0.6f;
    }

    /**
     * Returns the height of a line of text in pixels.
     */
    public float getTextHeight() {
        return fontSize;
    }

    /**
     * Returns the bounds of the given text.
     */
    public Rectangle2D getTextBounds(String text) {
        float width = getTextWidth(text);
        return new Rectangle2D.Float(0, 0, width, fontSize);
    }

    /**
     * Disposes Vulkan resources.
     */
    public void dispose() {
        VkDevice vkDevice = device != null ? device.getDevice() : null;

        if (vkDevice != null) {
            if (descriptorSet != VK_NULL_HANDLE) {
                vkFreeDescriptorSets(vkDevice, descriptorPool, descriptorSet);
                descriptorSet = VK_NULL_HANDLE;
            }

            if (descriptorPool != VK_NULL_HANDLE) {
                vkDestroyDescriptorPool(vkDevice, descriptorPool, null);
                descriptorPool = VK_NULL_HANDLE;
            }
        }

        for (VkFontAtlas atlas : atlasCache.values()) {
            atlas.dispose();
        }
        atlasCache.clear();

        // Note: vertexBuffer is managed by VkResourceManager

        resourcesInitialized = false;
        currentAtlas = null;
        boundTexture = null;
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
