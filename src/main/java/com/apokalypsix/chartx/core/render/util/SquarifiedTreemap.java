package com.apokalypsix.chartx.core.render.util;

import com.apokalypsix.chartx.core.render.util.NodeLayoutCache;
import com.apokalypsix.chartx.chart.data.TreemapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Squarified treemap layout algorithm.
 *
 * <p>Computes rectangular layouts for hierarchical data that
 * minimize aspect ratios for better visual perception.
 *
 * <p>Based on the algorithm described in:
 * "Squarified Treemaps" by Bruls, Huizing, and van Wijk (2000)
 */
public class SquarifiedTreemap {

    /** Padding between cells */
    private float padding = 2f;

    /** Minimum cell size (cells smaller than this won't be rendered) */
    private float minCellSize = 4f;

    /** Header height for groups (0 to disable) */
    private float headerHeight = 0f;

    public SquarifiedTreemap() {
    }

    public SquarifiedTreemap padding(float padding) {
        this.padding = padding;
        return this;
    }

    public SquarifiedTreemap minCellSize(float size) {
        this.minCellSize = size;
        return this;
    }

    public SquarifiedTreemap headerHeight(float height) {
        this.headerHeight = height;
        return this;
    }

    /**
     * Computes layout for the treemap data.
     *
     * @param data the treemap data
     * @param x left edge
     * @param y top edge
     * @param width available width
     * @param height available height
     */
    public void layout(TreemapData data, float x, float y, float width, float height) {
        TreemapData.Node root = data.getRoot();
        root.setLayout(x, y, width, height);
        layoutChildren(root, x, y, width, height);
    }

    /**
     * Computes layout for a single node's children.
     */
    public void layoutChildren(TreemapData.Node parent, float x, float y, float width, float height) {
        List<TreemapData.Node> children = parent.getChildren();
        if (children.isEmpty()) {
            return;
        }

        // Apply padding
        float px = x + padding;
        float py = y + padding + headerHeight;
        float pw = width - 2 * padding;
        float ph = height - 2 * padding - headerHeight;

        if (pw <= 0 || ph <= 0) {
            return;
        }

        // Sort children by value (descending) for better layout
        List<TreemapData.Node> sorted = new ArrayList<>(children);
        sorted.sort((a, b) -> Double.compare(b.getAggregateValue(), a.getAggregateValue()));

        // Normalize values to fit in available area
        double totalValue = parent.getAggregateValue();
        if (totalValue <= 0) {
            return;
        }

        double totalArea = pw * ph;
        double[] areas = new double[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            areas[i] = (sorted.get(i).getAggregateValue() / totalValue) * totalArea;
        }

        // Squarify layout
        squarify(sorted, areas, px, py, pw, ph);

        // Recursively layout children
        for (TreemapData.Node child : children) {
            layoutChildren(child, child.getX(), child.getY(), child.getWidth(), child.getHeight());
        }
    }

    private void squarify(List<TreemapData.Node> nodes, double[] areas,
                          float x, float y, float width, float height) {
        if (nodes.isEmpty()) {
            return;
        }

        if (nodes.size() == 1) {
            // Single node fills the space
            TreemapData.Node node = nodes.get(0);
            node.setLayout(x, y, width, height);
            return;
        }

        // Determine layout direction (split along shorter side for squarer cells)
        boolean horizontal = width >= height;
        float side = horizontal ? height : width;

        // Build rows using squarify algorithm
        List<TreemapData.Node> currentRow = new ArrayList<>();
        List<Double> currentAreas = new ArrayList<>();
        double currentRowArea = 0;
        float currentOffset = 0;

        int startIndex = 0;

        for (int i = 0; i < nodes.size(); i++) {
            TreemapData.Node node = nodes.get(i);
            double nodeArea = areas[i];

            // Check if adding this node improves the aspect ratio
            double newRowArea = currentRowArea + nodeArea;
            float newRowWidth = (float) (newRowArea / side);

            double worstBefore = currentRow.isEmpty() ? Double.MAX_VALUE :
                    worstAspectRatio(currentAreas, side, currentRowArea);

            currentAreas.add(nodeArea);
            double worstAfter = worstAspectRatio(currentAreas, side, newRowArea);
            currentAreas.remove(currentAreas.size() - 1);

            if (currentRow.isEmpty() || worstAfter <= worstBefore) {
                // Add to current row
                currentRow.add(node);
                currentAreas.add(nodeArea);
                currentRowArea = newRowArea;
            } else {
                // Layout current row and start new one
                float rowWidth = (float) (currentRowArea / side);
                layoutRow(currentRow, currentAreas, horizontal,
                        x, y, width, height, currentOffset, rowWidth, side);
                currentOffset += rowWidth;

                // Reset for new row
                currentRow.clear();
                currentAreas.clear();
                currentRow.add(node);
                currentAreas.add(nodeArea);
                currentRowArea = nodeArea;
            }
        }

        // Layout remaining row
        if (!currentRow.isEmpty()) {
            float rowWidth = (float) (currentRowArea / side);
            layoutRow(currentRow, currentAreas, horizontal,
                    x, y, width, height, currentOffset, rowWidth, side);
        }
    }

    private double worstAspectRatio(List<Double> areas, float side, double totalArea) {
        if (areas.isEmpty() || totalArea <= 0) {
            return Double.MAX_VALUE;
        }

        float rowWidth = (float) (totalArea / side);
        if (rowWidth <= 0) {
            return Double.MAX_VALUE;
        }

        double worst = 0;
        for (double area : areas) {
            float cellHeight = (float) (area / rowWidth);
            double ratio = Math.max(rowWidth / cellHeight, cellHeight / rowWidth);
            worst = Math.max(worst, ratio);
        }
        return worst;
    }

    private void layoutRow(List<TreemapData.Node> row, List<Double> areas, boolean horizontal,
                           float x, float y, float width, float height,
                           float offset, float rowWidth, float side) {
        float cellOffset = 0;

        for (int i = 0; i < row.size(); i++) {
            TreemapData.Node node = row.get(i);
            double area = areas.get(i);
            float cellSize = (float) (area / rowWidth);

            float cellX, cellY, cellW, cellH;

            if (horizontal) {
                cellX = x + offset;
                cellY = y + cellOffset;
                cellW = rowWidth;
                cellH = cellSize;
            } else {
                cellX = x + cellOffset;
                cellY = y + offset;
                cellW = cellSize;
                cellH = rowWidth;
            }

            node.setLayout(cellX, cellY, cellW, cellH);
            cellOffset += cellSize;
        }
    }

    /**
     * Computes layout for a flat list of nodes (single level).
     */
    public void layoutFlat(List<TreemapData.Node> nodes, float x, float y, float width, float height) {
        if (nodes.isEmpty()) {
            return;
        }

        // Calculate total value
        double totalValue = 0;
        for (TreemapData.Node node : nodes) {
            totalValue += node.getValue();
        }

        if (totalValue <= 0) {
            return;
        }

        // Sort by value
        List<TreemapData.Node> sorted = new ArrayList<>(nodes);
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Compute areas
        double totalArea = (width - 2 * padding) * (height - 2 * padding);
        double[] areas = new double[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            areas[i] = (sorted.get(i).getValue() / totalValue) * totalArea;
        }

        // Apply padding
        float px = x + padding;
        float py = y + padding;
        float pw = width - 2 * padding;
        float ph = height - 2 * padding;

        squarify(sorted, areas, px, py, pw, ph);
    }

    /**
     * Returns nodes that are large enough to render.
     */
    public List<TreemapData.Node> getVisibleNodes(TreemapData data) {
        List<TreemapData.Node> result = new ArrayList<>();
        collectVisibleNodes(data.getRoot(), result);
        return result;
    }

    private void collectVisibleNodes(TreemapData.Node node, List<TreemapData.Node> result) {
        if (node.getWidth() >= minCellSize && node.getHeight() >= minCellSize) {
            result.add(node);
            for (TreemapData.Node child : node.getChildren()) {
                collectVisibleNodes(child, result);
            }
        }
    }

    // ========== Cache-based overloads ==========

    /**
     * Computes layout writing results to a {@link NodeLayoutCache} instead of node fields.
     */
    public void layout(TreemapData data, float x, float y, float width, float height, NodeLayoutCache cache) {
        TreemapData.Node root = data.getRoot();
        cache.setLayout(root, x, y, width, height);
        layoutChildren(root, x, y, width, height, cache);
    }

    /**
     * Computes layout for a node's children, writing to a cache.
     */
    public void layoutChildren(TreemapData.Node parent, float x, float y, float width, float height,
                                NodeLayoutCache cache) {
        List<TreemapData.Node> children = parent.getChildren();
        if (children.isEmpty()) {
            return;
        }

        float px = x + padding;
        float py = y + padding + headerHeight;
        float pw = width - 2 * padding;
        float ph = height - 2 * padding - headerHeight;

        if (pw <= 0 || ph <= 0) {
            return;
        }

        List<TreemapData.Node> sorted = new ArrayList<>(children);
        sorted.sort((a, b) -> Double.compare(b.getAggregateValue(), a.getAggregateValue()));

        double totalValue = parent.getAggregateValue();
        if (totalValue <= 0) {
            return;
        }

        double totalArea = pw * ph;
        double[] areas = new double[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            areas[i] = (sorted.get(i).getAggregateValue() / totalValue) * totalArea;
        }

        squarify(sorted, areas, px, py, pw, ph, cache);

        for (TreemapData.Node child : children) {
            float cx = cache.getX(child);
            float cy = cache.getY(child);
            float cw = cache.getWidth(child);
            float ch = cache.getHeight(child);
            layoutChildren(child, cx, cy, cw, ch, cache);
        }
    }

    private void squarify(List<TreemapData.Node> nodes, double[] areas,
                          float x, float y, float width, float height, NodeLayoutCache cache) {
        if (nodes.isEmpty()) {
            return;
        }

        if (nodes.size() == 1) {
            cache.setLayout(nodes.get(0), x, y, width, height);
            return;
        }

        boolean horizontal = width >= height;
        float side = horizontal ? height : width;

        List<TreemapData.Node> currentRow = new ArrayList<>();
        List<Double> currentAreas = new ArrayList<>();
        double currentRowArea = 0;
        float currentOffset = 0;

        for (int i = 0; i < nodes.size(); i++) {
            TreemapData.Node node = nodes.get(i);
            double nodeArea = areas[i];

            double newRowArea = currentRowArea + nodeArea;

            double worstBefore = currentRow.isEmpty() ? Double.MAX_VALUE :
                    worstAspectRatio(currentAreas, side, currentRowArea);

            currentAreas.add(nodeArea);
            double worstAfter = worstAspectRatio(currentAreas, side, newRowArea);
            currentAreas.remove(currentAreas.size() - 1);

            if (currentRow.isEmpty() || worstAfter <= worstBefore) {
                currentRow.add(node);
                currentAreas.add(nodeArea);
                currentRowArea = newRowArea;
            } else {
                float rowWidth = (float) (currentRowArea / side);
                layoutRow(currentRow, currentAreas, horizontal,
                        x, y, width, height, currentOffset, rowWidth, side, cache);
                currentOffset += rowWidth;

                currentRow.clear();
                currentAreas.clear();
                currentRow.add(node);
                currentAreas.add(nodeArea);
                currentRowArea = nodeArea;
            }
        }

        if (!currentRow.isEmpty()) {
            float rowWidth = (float) (currentRowArea / side);
            layoutRow(currentRow, currentAreas, horizontal,
                    x, y, width, height, currentOffset, rowWidth, side, cache);
        }
    }

    private void layoutRow(List<TreemapData.Node> row, List<Double> areas, boolean horizontal,
                           float x, float y, float width, float height,
                           float offset, float rowWidth, float side, NodeLayoutCache cache) {
        float cellOffset = 0;

        for (int i = 0; i < row.size(); i++) {
            TreemapData.Node node = row.get(i);
            double area = areas.get(i);
            float cellSize = (float) (area / rowWidth);

            float cellX, cellY, cellW, cellH;

            if (horizontal) {
                cellX = x + offset;
                cellY = y + cellOffset;
                cellW = rowWidth;
                cellH = cellSize;
            } else {
                cellX = x + cellOffset;
                cellY = y + offset;
                cellW = cellSize;
                cellH = rowWidth;
            }

            cache.setLayout(node, cellX, cellY, cellW, cellH);
            cellOffset += cellSize;
        }
    }

    /**
     * Returns nodes that are large enough to render, reading sizes from a cache.
     */
    public List<TreemapData.Node> getVisibleNodes(TreemapData data, NodeLayoutCache cache) {
        List<TreemapData.Node> result = new ArrayList<>();
        collectVisibleNodes(data.getRoot(), result, cache);
        return result;
    }

    private void collectVisibleNodes(TreemapData.Node node, List<TreemapData.Node> result, NodeLayoutCache cache) {
        if (cache.getWidth(node) >= minCellSize && cache.getHeight(node) >= minCellSize) {
            result.add(node);
            for (TreemapData.Node child : node.getChildren()) {
                collectVisibleNodes(child, result, cache);
            }
        }
    }
}
