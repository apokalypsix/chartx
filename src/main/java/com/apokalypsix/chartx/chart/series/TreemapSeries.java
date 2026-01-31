package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.TreemapSeriesOptions;
import com.apokalypsix.chartx.core.render.util.NodeLayoutCache;
import com.apokalypsix.chartx.core.render.util.SquarifiedTreemap;
import com.apokalypsix.chartx.chart.data.TreemapData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.TextRenderer;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

/**
 * Renderable treemap series using TreemapData.
 *
 * <p>Renders hierarchical data as nested rectangles using
 * the squarified treemap algorithm. Supports drill-down
 * and color-coded levels.
 */
public class TreemapSeries implements BoundedRenderable {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final TreemapData data;
    private final TreemapSeriesOptions options;
    private final SquarifiedTreemap layout;

    // Rendering resources (abstracted API)
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] fillVertices;
    private float[] lineVertices;

    // Layout cache (per-series, avoids shared-node layout conflicts)
    private NodeLayoutCache layoutCache;
    private float lastX, lastY, lastWidth, lastHeight;
    private boolean layoutValid;

    /**
     * Creates a treemap series with default options.
     */
    public TreemapSeries(TreemapData data) {
        this(data, new TreemapSeriesOptions());
    }

    /**
     * Creates a treemap series with the given options.
     */
    public TreemapSeries(TreemapData data, TreemapSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a treemap series with a custom ID.
     */
    public TreemapSeries(String id, TreemapData data, TreemapSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.layout = new SquarifiedTreemap();
        this.initialized = false;
        this.layoutValid = false;
    }

    @Override
    public String getId() {
        return id;
    }

    public TreemapData getData() {
        return data;
    }

    public TreemapSeriesOptions getOptions() {
        return options;
    }

    @Override
    public SeriesType getType() {
        return SeriesType.TREEMAP;
    }

    @Override
    public boolean isVisible() {
        return options.isVisible();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void invalidateLayout() {
        layoutValid = false;
    }

    @Override
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        fillBuffer = resources.getOrCreateBuffer("treemap." + id + ".fill",
                BufferDescriptor.positionColor2D(16384 * FLOATS_PER_VERTEX));

        lineBuffer = resources.getOrCreateBuffer("treemap." + id + ".line",
                BufferDescriptor.positionColor2D(8192 * FLOATS_PER_VERTEX));

        fillVertices = new float[16384 * FLOATS_PER_VERTEX];
        lineVertices = new float[8192 * FLOATS_PER_VERTEX];

        layoutCache = new NodeLayoutCache();

        initialized = true;
    }

    @Override
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer("treemap." + id + ".fill");
            resourceManager.disposeBuffer("treemap." + id + ".line");
        }
        fillBuffer = null;
        lineBuffer = null;
        resourceManager = null;
        layoutCache = null;
        initialized = false;
    }

    /**
     * Renders the treemap.
     *
     * @param ctx render context
     * @param x left edge
     * @param y top edge
     * @param width available width
     * @param height available height
     */
    @Override
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        // Recalculate layout if bounds changed
        if (!layoutValid || x != lastX || y != lastY || width != lastWidth || height != lastHeight) {
            updateLayout(x, y, width, height);
        }

        RenderDevice device = ctx.getDevice();
        ResourceManager resources = ctx.getResourceManager();

        Shader shader = resources.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Build and draw fills (cell backgrounds)
        int fillFloatCount = buildFillVertices();
        if (fillFloatCount > 0) {
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw cell borders
        if (options.getBorderWidth() > 0) {
            int lineFloatCount = buildLineVertices();
            if (lineFloatCount > 0) {
                device.setLineWidth(options.getBorderWidth());
                lineBuffer.upload(lineVertices, 0, lineFloatCount);
                lineBuffer.draw(DrawMode.LINES);
            }
        }

        shader.unbind();

        if (options.isShowLabels()) {
            renderLabels(ctx);
        }
    }

    private void renderLabels(RenderContext ctx) {
        ResourceManager rm = ctx.getResourceManager();
        if (rm == null) {
            return;
        }
        TextRenderer textRenderer = rm.getTextRenderer();
        if (textRenderer == null) {
            return;
        }

        textRenderer.setFontSize(options.getLabelFontSize());
        textRenderer.beginBatch(ctx.getWidth(), ctx.getHeight());
        try {
            Color labelColor = options.getLabelColor();
            int maxDepth = options.getMaxVisibleDepth();
            float minSize = options.getMinCellSize();
            float minLabelSize = options.getMinLabelCellSize();

            List<TreemapData.Node> nodes = layout.getVisibleNodes(data, layoutCache);

            for (TreemapData.Node node : nodes) {
                if (maxDepth > 0 && node.getDepth() > maxDepth) {
                    continue;
                }

                float nw = layoutCache.getWidth(node);
                float nh = layoutCache.getHeight(node);

                if (nw < minSize || nh < minSize) {
                    continue;
                }

                // Only label leaf nodes or nodes at max depth
                if (!node.isLeaf() && (maxDepth == 0 || node.getDepth() < maxDepth)) {
                    continue;
                }

                if (nw < minLabelSize || nh < minLabelSize) {
                    continue;
                }

                String label = node.getLabel();
                if (label == null || label.isEmpty()) {
                    continue;
                }

                if (options.isShowValues()) {
                    label = label + " " + String.format(options.getValueFormat(), node.getValue());
                }

                if (textRenderer.getTextWidth(label) > nw) {
                    continue;
                }

                float cx = layoutCache.getX(node) + nw / 2f;
                float cy = layoutCache.getY(node) + nh / 2f;
                textRenderer.drawTextCentered(label, cx, cy, labelColor);
            }
        } finally {
            textRenderer.endBatch();
        }
    }

    private void updateLayout(float x, float y, float width, float height) {
        // Configure layout parameters
        layout.padding(options.getPadding());
        layout.minCellSize(options.getMinCellSize());
        if (options.isShowHeaders()) {
            layout.headerHeight(options.getHeaderHeight());
        } else {
            layout.headerHeight(0);
        }

        // Sort data for better layout
        data.sortByValue();

        // Compute layout into per-series cache
        layoutCache.clear();
        layout.layout(data, x, y, width, height, layoutCache);

        lastX = x;
        lastY = y;
        lastWidth = width;
        lastHeight = height;
        layoutValid = true;
    }

    private int buildFillVertices() {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        Color defaultColor = options.getDefaultColor();
        int maxDepth = options.getMaxVisibleDepth();
        float minSize = options.getMinCellSize();

        // Get all visible nodes
        List<TreemapData.Node> nodes = layout.getVisibleNodes(data, layoutCache);

        for (TreemapData.Node node : nodes) {
            // Skip if beyond max depth
            if (maxDepth > 0 && node.getDepth() > maxDepth) {
                continue;
            }

            float nw = layoutCache.getWidth(node);
            float nh = layoutCache.getHeight(node);

            // Skip if too small
            if (nw < minSize || nh < minSize) {
                continue;
            }

            float nx = layoutCache.getX(node);
            float ny = layoutCache.getY(node);

            // Only render leaf nodes or nodes at max depth
            if (!node.isLeaf() && (maxDepth == 0 || node.getDepth() < maxDepth)) {
                // Draw group header if enabled
                if (options.isShowHeaders() && node.getDepth() > 0) {
                    Color headerColor = options.getHeaderColor();
                    float hr = headerColor.getRed() / 255f;
                    float hg = headerColor.getGreen() / 255f;
                    float hb = headerColor.getBlue() / 255f;
                    float ha = opacity;

                    floatIndex = addQuad(fillVertices, floatIndex,
                            nx, ny, nw, options.getHeaderHeight(),
                            hr, hg, hb, ha);
                }
                continue;
            }

            // Get cell color
            Color cellColor = node.getColor() != null ? node.getColor() : defaultColor;
            float r = cellColor.getRed() / 255f;
            float g = cellColor.getGreen() / 255f;
            float b = cellColor.getBlue() / 255f;
            float a = (cellColor.getAlpha() / 255f) * opacity;

            floatIndex = addQuad(fillVertices, floatIndex, nx, ny, nw, nh, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildLineVertices() {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        Color borderColor = options.getBorderColor();
        float br = borderColor.getRed() / 255f;
        float bg = borderColor.getGreen() / 255f;
        float bb = borderColor.getBlue() / 255f;
        float ba = opacity;

        int maxDepth = options.getMaxVisibleDepth();
        float minSize = options.getMinCellSize();

        // Get all visible nodes
        List<TreemapData.Node> nodes = layout.getVisibleNodes(data, layoutCache);

        for (TreemapData.Node node : nodes) {
            // Skip root
            if (node.getDepth() == 0) {
                continue;
            }

            // Skip if beyond max depth
            if (maxDepth > 0 && node.getDepth() > maxDepth) {
                continue;
            }

            float nw = layoutCache.getWidth(node);
            float nh = layoutCache.getHeight(node);

            // Skip if too small
            if (nw < minSize || nh < minSize) {
                continue;
            }

            float x = layoutCache.getX(node);
            float y = layoutCache.getY(node);
            float w = nw;
            float h = nh;

            // Draw border lines
            // Top
            floatIndex = addVertex(lineVertices, floatIndex, x, y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, x + w, y, br, bg, bb, ba);
            // Right
            floatIndex = addVertex(lineVertices, floatIndex, x + w, y, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, x + w, y + h, br, bg, bb, ba);
            // Bottom
            floatIndex = addVertex(lineVertices, floatIndex, x + w, y + h, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, x, y + h, br, bg, bb, ba);
            // Left
            floatIndex = addVertex(lineVertices, floatIndex, x, y + h, br, bg, bb, ba);
            floatIndex = addVertex(lineVertices, floatIndex, x, y, br, bg, bb, ba);
        }

        return floatIndex;
    }

    private int addQuad(float[] vertices, int index, float x, float y, float width, float height,
                        float r, float g, float b, float a) {
        // Triangle 1
        index = addVertex(vertices, index, x, y, r, g, b, a);
        index = addVertex(vertices, index, x, y + height, r, g, b, a);
        index = addVertex(vertices, index, x + width, y + height, r, g, b, a);
        // Triangle 2
        index = addVertex(vertices, index, x, y, r, g, b, a);
        index = addVertex(vertices, index, x + width, y + height, r, g, b, a);
        index = addVertex(vertices, index, x + width, y, r, g, b, a);
        return index;
    }

    private int addVertex(float[] vertices, int index, float x, float y,
                          float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }

    /**
     * Finds the node at the given screen coordinates.
     */
    public TreemapData.Node findNodeAt(float screenX, float screenY) {
        if (!layoutValid) {
            return null;
        }

        int maxDepth = options.getMaxVisibleDepth();
        return findNodeAtRecursive(data.getRoot(), screenX, screenY, maxDepth);
    }

    private TreemapData.Node findNodeAtRecursive(TreemapData.Node node, float x, float y, int maxDepth) {
        if (!containsPoint(node, x, y)) {
            return null;
        }

        // Check children first (deeper nodes take precedence)
        if (maxDepth == 0 || node.getDepth() < maxDepth) {
            for (TreemapData.Node child : node.getChildren()) {
                TreemapData.Node found = findNodeAtRecursive(child, x, y, maxDepth);
                if (found != null) {
                    return found;
                }
            }
        }

        // Return this node if it's a leaf or at max depth
        if (node.isLeaf() || (maxDepth > 0 && node.getDepth() == maxDepth)) {
            return node;
        }

        return null;
    }

    private boolean containsPoint(TreemapData.Node node, float x, float y) {
        float nx = layoutCache.getX(node);
        float ny = layoutCache.getY(node);
        float nw = layoutCache.getWidth(node);
        float nh = layoutCache.getHeight(node);
        return x >= nx && x < nx + nw && y >= ny && y < ny + nh;
    }
}
