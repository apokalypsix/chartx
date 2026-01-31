package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.SunburstSeriesOptions;
import com.apokalypsix.chartx.core.render.util.ArcUtils;
import com.apokalypsix.chartx.core.render.util.NodeLayoutCache;
import com.apokalypsix.chartx.chart.data.TreemapData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.TextRenderer;

import java.awt.Color;
import java.util.UUID;

/**
 * Renderable sunburst series using TreemapData.
 *
 * <p>Renders hierarchical data as concentric rings where
 * each ring represents a depth level in the hierarchy.
 */
public class SunburstSeries implements BoundedRenderable {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final TreemapData data;
    private final SunburstSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] fillVertices;
    private float[] lineVertices;

    // Layout data (angles per node, stored in cache)
    private NodeLayoutCache layoutCache;
    private float centerX, centerY, radius;
    private boolean layoutValid;

    /**
     * Creates a sunburst series with default options.
     */
    public SunburstSeries(TreemapData data) {
        this(data, new SunburstSeriesOptions());
    }

    /**
     * Creates a sunburst series with the given options.
     */
    public SunburstSeries(TreemapData data, SunburstSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a sunburst series with a custom ID.
     */
    public SunburstSeries(String id, TreemapData data, SunburstSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
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

    public SunburstSeriesOptions getOptions() {
        return options;
    }

    @Override
    public SeriesType getType() {
        return SeriesType.SUNBURST;
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

        fillBuffer = resources.getOrCreateBuffer("sunburst." + id + ".fill",
                BufferDescriptor.positionColor2D(32768 * FLOATS_PER_VERTEX));

        lineBuffer = resources.getOrCreateBuffer("sunburst." + id + ".line",
                BufferDescriptor.positionColor2D(8192 * FLOATS_PER_VERTEX));

        fillVertices = new float[32768 * FLOATS_PER_VERTEX];
        lineVertices = new float[8192 * FLOATS_PER_VERTEX];

        layoutCache = new NodeLayoutCache();

        initialized = true;
    }

    @Override
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer("sunburst." + id + ".fill");
            resourceManager.disposeBuffer("sunburst." + id + ".line");
        }
        fillBuffer = null;
        lineBuffer = null;
        resourceManager = null;
        layoutCache = null;
        initialized = false;
    }

    /**
     * Renders the sunburst chart within a rectangular bounds.
     * Derives center and radius from the bounds.
     */
    @Override
    public void render(RenderContext ctx, float x, float y, float width, float height) {
        float cx = x + width / 2f;
        float cy = y + height / 2f;
        float r = Math.min(width, height) / 2f;
        render(ctx, cx, cy, r);
    }

    /**
     * Renders the sunburst chart.
     *
     * @param ctx render context
     * @param centerX center X position
     * @param centerY center Y position
     * @param radius outer radius
     */
    public void render(RenderContext ctx, float centerX, float centerY, float radius) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        // Update layout if needed
        if (!layoutValid || this.centerX != centerX || this.centerY != centerY || this.radius != radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            computeLayout();
        }

        RenderDevice device = ctx.getDevice();
        ResourceManager resources = ctx.getResourceManager();

        Shader shader = resources.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Build and draw segment fills
        int fillFloatCount = buildFillVertices();
        if (fillFloatCount > 0) {
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw borders
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
            renderLabelsRecursive(textRenderer, data.getRoot(), labelColor, maxDepth);
        } finally {
            textRenderer.endBatch();
        }
    }

    private void renderLabelsRecursive(TextRenderer textRenderer, TreemapData.Node node,
                                        Color labelColor, int maxDepth) {
        int depth = node.getDepth();

        if (maxDepth > 0 && depth > maxDepth) {
            return;
        }

        if (depth > 0) {
            float nodeStartAngle = layoutCache.getStartAngle(node);
            float nodeEndAngle = layoutCache.getEndAngle(node);
            float arcSpan = nodeEndAngle - nodeStartAngle;

            if (arcSpan >= options.getMinLabelAngle()) {
                String label = node.getLabel();
                if (label != null && !label.isEmpty()) {
                    float[] radii = options.getRingRadii(depth, radius);
                    float innerRadius = radii[0];
                    float outerRadius = radii[1];
                    float midRadius = (innerRadius + outerRadius) / 2f;

                    // Arc width at mid-radius
                    float arcWidth = midRadius * (float) Math.toRadians(arcSpan);
                    float textWidth = textRenderer.getTextWidth(label);

                    if (textWidth <= arcWidth) {
                        float midAngle = (float) Math.toRadians((nodeStartAngle + nodeEndAngle) / 2f);
                        float lx = centerX + (float) Math.cos(midAngle) * midRadius;
                        float ly = centerY + (float) Math.sin(midAngle) * midRadius;
                        textRenderer.drawTextCentered(label, lx, ly, labelColor);
                    }
                }
            }
        }

        for (TreemapData.Node child : node.getChildren()) {
            renderLabelsRecursive(textRenderer, child, labelColor, maxDepth);
        }
    }

    private void computeLayout() {
        // Assign angular positions to all nodes, writing to cache
        layoutCache.clear();
        TreemapData.Node root = data.getRoot();
        assignAngles(root, 0, 360);
        layoutValid = true;
    }

    private void assignAngles(TreemapData.Node node, double startAngle, double endAngle) {
        // Store angles in cache (startAngle, endAngle, 0, 0)
        layoutCache.setLayout(node, (float) startAngle, (float) endAngle, 0, 0);

        if (node.isLeaf()) {
            return;
        }

        double totalValue = node.getAggregateValue();
        if (totalValue <= 0) {
            return;
        }

        double currentAngle = startAngle;
        double segmentGap = options.getSegmentGap();

        for (TreemapData.Node child : node.getChildren()) {
            double childValue = child.getAggregateValue();
            double childSpan = ((endAngle - startAngle) - segmentGap * node.getChildren().size())
                    * (childValue / totalValue);

            if (childSpan > 0) {
                assignAngles(child, currentAngle, currentAngle + childSpan);
                currentAngle += childSpan + segmentGap;
            }
        }
    }

    private int buildFillVertices() {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        int maxDepth = options.getMaxVisibleDepth();
        Color defaultColor = options.getDefaultColor();

        TreemapData.Node root = data.getRoot();
        floatIndex = renderNodeRecursive(fillVertices, floatIndex, root, opacity, maxDepth, defaultColor);

        return floatIndex;
    }

    private int renderNodeRecursive(float[] vertices, int floatIndex, TreemapData.Node node,
                                     float opacity, int maxDepth, Color defaultColor) {
        int depth = node.getDepth();

        // Skip if beyond max depth
        if (maxDepth > 0 && depth > maxDepth) {
            return floatIndex;
        }

        // Don't render root (center)
        if (depth > 0) {
            // Get ring radii for this depth
            float[] radii = options.getRingRadii(depth, radius);
            float innerRadius = radii[0];
            float outerRadius = radii[1];

            if (outerRadius > innerRadius) {
                // Get angles from cache
                float nodeStartAngle = layoutCache.getStartAngle(node);
                float nodeEndAngle = layoutCache.getEndAngle(node);
                double startAngle = Math.toRadians(nodeStartAngle);
                double endAngle = Math.toRadians(nodeEndAngle);
                int segments = Math.max(4, (int) ((nodeEndAngle - nodeStartAngle) * options.getSegmentsPerDegree()));

                // Get color
                Color color = node.getColor() != null ? node.getColor() : defaultColor;
                float r = color.getRed() / 255f;
                float g = color.getGreen() / 255f;
                float b = color.getBlue() / 255f;
                float a = (color.getAlpha() / 255f) * opacity;

                // Tessellate donut segment
                floatIndex = ArcUtils.tessellateDonutSegment(vertices, floatIndex,
                        centerX, centerY, innerRadius, outerRadius,
                        startAngle, endAngle, segments, r, g, b, a);
            }
        }

        // Render children
        for (TreemapData.Node child : node.getChildren()) {
            floatIndex = renderNodeRecursive(vertices, floatIndex, child, opacity, maxDepth, defaultColor);
        }

        return floatIndex;
    }

    private int buildLineVertices() {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        int maxDepth = options.getMaxVisibleDepth();

        Color borderColor = options.getBorderColor();
        float br = borderColor.getRed() / 255f;
        float bg = borderColor.getGreen() / 255f;
        float bb = borderColor.getBlue() / 255f;
        float ba = opacity;

        TreemapData.Node root = data.getRoot();
        floatIndex = renderBordersRecursive(lineVertices, floatIndex, root, maxDepth, br, bg, bb, ba);

        return floatIndex;
    }

    private int renderBordersRecursive(float[] vertices, int floatIndex, TreemapData.Node node,
                                        int maxDepth, float r, float g, float b, float a) {
        int depth = node.getDepth();

        // Skip if beyond max depth
        if (maxDepth > 0 && depth > maxDepth) {
            return floatIndex;
        }

        // Don't render root border
        if (depth > 0) {
            float[] radii = options.getRingRadii(depth, radius);
            float innerRadius = radii[0];
            float outerRadius = radii[1];

            if (outerRadius > innerRadius) {
                float nodeStartAngle = layoutCache.getStartAngle(node);
                float nodeEndAngle = layoutCache.getEndAngle(node);
                double startAngle = Math.toRadians(nodeStartAngle);
                double endAngle = Math.toRadians(nodeEndAngle);
                int segments = Math.max(4, (int) ((nodeEndAngle - nodeStartAngle) * options.getSegmentsPerDegree()));

                // Inner arc
                for (int i = 0; i < segments; i++) {
                    double a1 = startAngle + i * (endAngle - startAngle) / segments;
                    double a2 = startAngle + (i + 1) * (endAngle - startAngle) / segments;

                    float x1 = centerX + (float) Math.cos(a1) * innerRadius;
                    float y1 = centerY + (float) Math.sin(a1) * innerRadius;
                    float x2 = centerX + (float) Math.cos(a2) * innerRadius;
                    float y2 = centerY + (float) Math.sin(a2) * innerRadius;

                    floatIndex = addVertex(vertices, floatIndex, x1, y1, r, g, b, a);
                    floatIndex = addVertex(vertices, floatIndex, x2, y2, r, g, b, a);
                }

                // Outer arc
                for (int i = 0; i < segments; i++) {
                    double a1 = startAngle + i * (endAngle - startAngle) / segments;
                    double a2 = startAngle + (i + 1) * (endAngle - startAngle) / segments;

                    float x1 = centerX + (float) Math.cos(a1) * outerRadius;
                    float y1 = centerY + (float) Math.sin(a1) * outerRadius;
                    float x2 = centerX + (float) Math.cos(a2) * outerRadius;
                    float y2 = centerY + (float) Math.sin(a2) * outerRadius;

                    floatIndex = addVertex(vertices, floatIndex, x1, y1, r, g, b, a);
                    floatIndex = addVertex(vertices, floatIndex, x2, y2, r, g, b, a);
                }

                // Radial edges
                float x1 = centerX + (float) Math.cos(startAngle) * innerRadius;
                float y1 = centerY + (float) Math.sin(startAngle) * innerRadius;
                float x2 = centerX + (float) Math.cos(startAngle) * outerRadius;
                float y2 = centerY + (float) Math.sin(startAngle) * outerRadius;
                floatIndex = addVertex(vertices, floatIndex, x1, y1, r, g, b, a);
                floatIndex = addVertex(vertices, floatIndex, x2, y2, r, g, b, a);

                x1 = centerX + (float) Math.cos(endAngle) * innerRadius;
                y1 = centerY + (float) Math.sin(endAngle) * innerRadius;
                x2 = centerX + (float) Math.cos(endAngle) * outerRadius;
                y2 = centerY + (float) Math.sin(endAngle) * outerRadius;
                floatIndex = addVertex(vertices, floatIndex, x1, y1, r, g, b, a);
                floatIndex = addVertex(vertices, floatIndex, x2, y2, r, g, b, a);
            }
        }

        // Render children borders
        for (TreemapData.Node child : node.getChildren()) {
            floatIndex = renderBordersRecursive(vertices, floatIndex, child, maxDepth, r, g, b, a);
        }

        return floatIndex;
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

        // Calculate polar coordinates
        float dx = screenX - centerX;
        float dy = screenY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;

        return findNodeAtRecursive(data.getRoot(), dist, angle, options.getMaxVisibleDepth());
    }

    private TreemapData.Node findNodeAtRecursive(TreemapData.Node node, float dist, float angle, int maxDepth) {
        int depth = node.getDepth();

        if (maxDepth > 0 && depth > maxDepth) {
            return null;
        }

        if (depth > 0) {
            float[] radii = options.getRingRadii(depth, radius);
            float innerRadius = radii[0];
            float outerRadius = radii[1];

            if (dist >= innerRadius && dist <= outerRadius) {
                float startAngle = layoutCache.getStartAngle(node);
                float endAngle = layoutCache.getEndAngle(node);

                if (angle >= startAngle && angle <= endAngle) {
                    // Check children first
                    for (TreemapData.Node child : node.getChildren()) {
                        TreemapData.Node found = findNodeAtRecursive(child, dist, angle, maxDepth);
                        if (found != null) {
                            return found;
                        }
                    }
                    return node;
                }
            }
        }

        // Check children
        for (TreemapData.Node child : node.getChildren()) {
            TreemapData.Node found = findNodeAtRecursive(child, dist, angle, maxDepth);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}
