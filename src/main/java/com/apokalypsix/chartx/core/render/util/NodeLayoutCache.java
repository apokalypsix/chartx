package com.apokalypsix.chartx.core.render.util;

import com.apokalypsix.chartx.chart.data.TreemapData;

import java.util.IdentityHashMap;

/**
 * Per-series layout cache for TreemapData nodes.
 *
 * <p>Stores layout data (x, y, width, height) separately from the shared
 * {@link TreemapData.Node} objects, allowing multiple series to compute
 * independent layouts on the same data without overwriting each other.
 *
 * <p>For treemap layouts, the four floats are (x, y, width, height).
 * For sunburst layouts, they are (startAngle, endAngle, 0, 0).
 */
public class NodeLayoutCache {

    private final IdentityHashMap<TreemapData.Node, float[]> cache = new IdentityHashMap<>();

    /**
     * Sets layout data for a node. Reuses existing float arrays to avoid allocation.
     */
    public void setLayout(TreemapData.Node node, float v0, float v1, float v2, float v3) {
        float[] layout = cache.get(node);
        if (layout == null) {
            layout = new float[4];
            cache.put(node, layout);
        }
        layout[0] = v0;
        layout[1] = v1;
        layout[2] = v2;
        layout[3] = v3;
    }

    /**
     * Returns the first layout value (x for treemap, startAngle for sunburst).
     */
    public float getX(TreemapData.Node node) {
        float[] layout = cache.get(node);
        return layout != null ? layout[0] : 0;
    }

    /**
     * Returns the second layout value (y for treemap, endAngle for sunburst).
     */
    public float getY(TreemapData.Node node) {
        float[] layout = cache.get(node);
        return layout != null ? layout[1] : 0;
    }

    /**
     * Returns the third layout value (width for treemap, unused for sunburst).
     */
    public float getWidth(TreemapData.Node node) {
        float[] layout = cache.get(node);
        return layout != null ? layout[2] : 0;
    }

    /**
     * Returns the fourth layout value (height for treemap, unused for sunburst).
     */
    public float getHeight(TreemapData.Node node) {
        float[] layout = cache.get(node);
        return layout != null ? layout[3] : 0;
    }

    /** Alias for {@link #getX(TreemapData.Node)} — sunburst start angle in degrees. */
    public float getStartAngle(TreemapData.Node node) {
        return getX(node);
    }

    /** Alias for {@link #getY(TreemapData.Node)} — sunburst end angle in degrees. */
    public float getEndAngle(TreemapData.Node node) {
        return getY(node);
    }

    /**
     * Clears all cached layout data, forcing re-layout on next render.
     */
    public void clear() {
        cache.clear();
    }
}
