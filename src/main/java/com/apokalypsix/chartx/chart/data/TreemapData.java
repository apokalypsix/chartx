package com.apokalypsix.chartx.chart.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical data for treemap and sunburst charts.
 *
 * <p>Stores a tree of nodes where each node has a value
 * that determines its visual size in the treemap.
 */
public class TreemapData {

    /**
     * A node in the treemap hierarchy.
     */
    public static class Node {
        private final String id;
        private String label;
        private double value;
        private Color color;
        private Node parent;
        private final List<Node> children;

        // Layout data (computed during layout)
        private float x, y, width, height;
        private int depth;

        public Node(String id, String label, double value) {
            this.id = id;
            this.label = label;
            this.value = value;
            this.children = new ArrayList<>();
            this.depth = 0;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public Node getParent() {
            return parent;
        }

        public List<Node> getChildren() {
            return children;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public int getDepth() {
            return depth;
        }

        // Layout accessors
        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public void setLayout(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /**
         * Adds a child node.
         */
        public Node addChild(String childId, String childLabel, double childValue) {
            Node child = new Node(childId, childLabel, childValue);
            addChild(child);
            return child;
        }

        /**
         * Adds an existing node as a child.
         */
        public void addChild(Node child) {
            child.parent = this;
            child.depth = this.depth + 1;
            children.add(child);
        }

        /**
         * Returns the sum of all leaf values under this node.
         */
        public double getAggregateValue() {
            if (isLeaf()) {
                return value;
            }
            double sum = 0;
            for (Node child : children) {
                sum += child.getAggregateValue();
            }
            return sum;
        }

        /**
         * Returns all descendant nodes at a specific depth.
         */
        public List<Node> getNodesAtDepth(int targetDepth) {
            List<Node> result = new ArrayList<>();
            collectNodesAtDepth(targetDepth, result);
            return result;
        }

        private void collectNodesAtDepth(int targetDepth, List<Node> result) {
            if (this.depth == targetDepth) {
                result.add(this);
            } else if (this.depth < targetDepth) {
                for (Node child : children) {
                    child.collectNodesAtDepth(targetDepth, result);
                }
            }
        }

        /**
         * Flattens this subtree into a list (pre-order traversal).
         */
        public List<Node> flatten() {
            List<Node> result = new ArrayList<>();
            flattenInto(result);
            return result;
        }

        private void flattenInto(List<Node> result) {
            result.add(this);
            for (Node child : children) {
                child.flattenInto(result);
            }
        }

        /**
         * Returns all leaf nodes in this subtree.
         */
        public List<Node> getLeaves() {
            List<Node> result = new ArrayList<>();
            collectLeaves(result);
            return result;
        }

        private void collectLeaves(List<Node> result) {
            if (isLeaf()) {
                result.add(this);
            } else {
                for (Node child : children) {
                    child.collectLeaves(result);
                }
            }
        }

        /**
         * Updates depth values for this node and all descendants.
         */
        void updateDepths(int newDepth) {
            this.depth = newDepth;
            for (Node child : children) {
                child.updateDepths(newDepth + 1);
            }
        }
    }

    private final String id;
    private final String name;
    private final Node root;

    // Cached values
    private int maxDepth = -1;
    private double totalValue = -1;

    /**
     * Creates treemap data with a root node.
     */
    public TreemapData(String id, String name) {
        this.id = id;
        this.name = name;
        this.root = new Node("root", name, 0);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Adds a top-level child to the root.
     */
    public Node addNode(String nodeId, String label, double value) {
        invalidateCache();
        return root.addChild(nodeId, label, value);
    }

    /**
     * Returns the maximum depth of the tree.
     */
    public int getMaxDepth() {
        if (maxDepth < 0) {
            maxDepth = computeMaxDepth(root);
        }
        return maxDepth;
    }

    private int computeMaxDepth(Node node) {
        if (node.isLeaf()) {
            return node.getDepth();
        }
        int max = node.getDepth();
        for (Node child : node.getChildren()) {
            max = Math.max(max, computeMaxDepth(child));
        }
        return max;
    }

    /**
     * Returns the total value of all leaves.
     */
    public double getTotalValue() {
        if (totalValue < 0) {
            totalValue = root.getAggregateValue();
        }
        return totalValue;
    }

    /**
     * Returns all nodes at a specific depth level.
     */
    public List<Node> getNodesAtDepth(int depth) {
        return root.getNodesAtDepth(depth);
    }

    /**
     * Returns all leaf nodes.
     */
    public List<Node> getLeaves() {
        return root.getLeaves();
    }

    /**
     * Returns all nodes in pre-order.
     */
    public List<Node> getAllNodes() {
        return root.flatten();
    }

    /**
     * Finds a node by ID.
     */
    public Node findNode(String nodeId) {
        return findNodeRecursive(root, nodeId);
    }

    private Node findNodeRecursive(Node node, String nodeId) {
        if (node.getId().equals(nodeId)) {
            return node;
        }
        for (Node child : node.getChildren()) {
            Node found = findNodeRecursive(child, nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Invalidates cached values after structural changes.
     */
    public void invalidateCache() {
        maxDepth = -1;
        totalValue = -1;
    }

    /**
     * Sorts children at each level by value (descending).
     */
    public void sortByValue() {
        sortNodeByValue(root);
    }

    private void sortNodeByValue(Node node) {
        if (!node.isLeaf()) {
            node.getChildren().sort((a, b) -> Double.compare(b.getAggregateValue(), a.getAggregateValue()));
            for (Node child : node.getChildren()) {
                sortNodeByValue(child);
            }
        }
    }

    /**
     * Assigns colors to nodes based on depth using a gradient.
     */
    public void assignDepthColors(Color startColor, Color endColor) {
        int maxD = getMaxDepth();
        if (maxD == 0) maxD = 1;

        assignDepthColorsRecursive(root, startColor, endColor, maxD);
    }

    private void assignDepthColorsRecursive(Node node, Color start, Color end, int maxD) {
        float t = (float) node.getDepth() / maxD;
        int r = (int) (start.getRed() + t * (end.getRed() - start.getRed()));
        int g = (int) (start.getGreen() + t * (end.getGreen() - start.getGreen()));
        int b = (int) (start.getBlue() + t * (end.getBlue() - start.getBlue()));
        node.setColor(new Color(r, g, b));

        for (Node child : node.getChildren()) {
            assignDepthColorsRecursive(child, start, end, maxD);
        }
    }

    /**
     * Assigns colors based on parent node (children get variations of parent color).
     */
    public void assignHierarchicalColors(Color[] baseColors) {
        List<Node> topLevel = root.getChildren();
        for (int i = 0; i < topLevel.size(); i++) {
            Color baseColor = baseColors[i % baseColors.length];
            assignChildColors(topLevel.get(i), baseColor);
        }
    }

    private void assignChildColors(Node node, Color baseColor) {
        node.setColor(baseColor);

        if (!node.isLeaf()) {
            List<Node> children = node.getChildren();
            for (int i = 0; i < children.size(); i++) {
                // Create color variation
                float hueShift = (float) i / children.size() * 0.1f;
                float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(),
                        baseColor.getBlue(), null);
                float newHue = (hsb[0] + hueShift) % 1.0f;
                float newSat = Math.max(0.2f, hsb[1] - 0.1f);
                float newBri = Math.min(1.0f, hsb[2] + 0.1f);
                Color childColor = Color.getHSBColor(newHue, newSat, newBri);

                assignChildColors(children.get(i), childColor);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("TreemapData[id=%s, nodes=%d, depth=%d, total=%.2f]",
                id, getAllNodes().size(), getMaxDepth(), getTotalValue());
    }
}
