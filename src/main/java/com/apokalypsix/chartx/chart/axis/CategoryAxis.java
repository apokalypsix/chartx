package com.apokalypsix.chartx.chart.axis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a categorical axis that maps indices to discrete labels.
 *
 * <p>Unlike numeric axes, category axes display discrete labels at fixed intervals.
 * Each category has an index (0-based) and a display label. This is useful for
 * bar charts, pie charts, and other visualizations with discrete categories.
 *
 * <p>The axis can be used horizontally (as an X-axis replacement) or as labels
 * for polar charts and other categorical visualizations.
 */
public class CategoryAxis implements HorizontalAxis<Integer> {

    /** Default axis ID. */
    public static final String DEFAULT_AXIS_ID = "category-default";

    /** Position of the axis. */
    public enum Position {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    /** How labels should be aligned relative to their category position. */
    public enum LabelAlignment {
        START,    // Align to start of category slot
        CENTER,   // Align to center of category slot (default)
        END       // Align to end of category slot
    }

    private final String id;
    private Position position;
    private final List<String> categories;
    private boolean visible;
    private int height; // Height for horizontal axis, width for vertical
    private Color labelColor;
    private Color lineColor;
    private float lineWidth;
    private LabelAlignment labelAlignment;
    private boolean showGridLines;
    private Color gridLineColor;
    private int maxLabelWidth; // 0 = auto
    private boolean rotateLabels;
    private double labelRotationAngle; // In degrees
    private int labelPadding;

    /**
     * Creates a category axis with the given ID and position.
     *
     * @param id unique identifier for this axis
     * @param position where to display the axis
     */
    public CategoryAxis(String id, Position position) {
        this.id = id;
        this.position = position;
        this.categories = new ArrayList<>();
        this.visible = true;
        this.height = 30;
        this.labelColor = new Color(180, 180, 180);
        this.lineColor = new Color(100, 100, 100);
        this.lineWidth = 1.0f;
        this.labelAlignment = LabelAlignment.CENTER;
        this.showGridLines = false;
        this.gridLineColor = new Color(60, 60, 60);
        this.maxLabelWidth = 0;
        this.rotateLabels = false;
        this.labelRotationAngle = -45;
        this.labelPadding = 4;
    }

    /**
     * Creates a horizontal category axis at the bottom.
     */
    public static CategoryAxis bottomAxis(String id) {
        return new CategoryAxis(id, Position.BOTTOM);
    }

    /**
     * Creates a vertical category axis on the left.
     */
    public static CategoryAxis leftAxis(String id) {
        return new CategoryAxis(id, Position.LEFT);
    }

    // ========== Category Management ==========

    /**
     * Adds a category to the end of the list.
     *
     * @param label the category label
     * @return the index of the added category
     */
    public int addCategory(String label) {
        categories.add(label);
        return categories.size() - 1;
    }

    /**
     * Adds multiple categories.
     *
     * @param labels the category labels
     */
    public void addCategories(String... labels) {
        Collections.addAll(categories, labels);
    }

    /**
     * Adds multiple categories from a list.
     *
     * @param labels the category labels
     */
    public void addCategories(List<String> labels) {
        categories.addAll(labels);
    }

    /**
     * Sets all categories, replacing any existing ones.
     *
     * @param labels the category labels
     */
    public void setCategories(String... labels) {
        categories.clear();
        Collections.addAll(categories, labels);
    }

    /**
     * Sets all categories from a list, replacing any existing ones.
     *
     * @param labels the category labels
     */
    public void setCategories(List<String> labels) {
        categories.clear();
        categories.addAll(labels);
    }

    /**
     * Removes all categories.
     */
    public void clearCategories() {
        categories.clear();
    }

    /**
     * Returns the number of categories.
     */
    public int getCategoryCount() {
        return categories.size();
    }

    /**
     * Returns the label for the given category index.
     *
     * @param index category index (0-based)
     * @return the label, or empty string if index is out of range
     */
    public String getLabel(int index) {
        if (index < 0 || index >= categories.size()) {
            return "";
        }
        return categories.get(index);
    }

    /**
     * Returns an unmodifiable view of all categories.
     */
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    /**
     * Returns the index of the given category label.
     *
     * @param label the label to find
     * @return the index, or -1 if not found
     */
    public int indexOf(String label) {
        return categories.indexOf(label);
    }

    // ========== Coordinate Transformations ==========

    /**
     * Converts a category index to a normalized position (0-1).
     *
     * @param index category index
     * @return normalized position where 0 = first category, 1 = after last category
     */
    public double indexToNormalized(int index) {
        if (categories.isEmpty()) {
            return 0.5;
        }
        return (double) index / categories.size();
    }

    /**
     * Converts a category index to a centered normalized position.
     *
     * @param index category index
     * @return normalized position at the center of the category slot
     */
    public double indexToCenterNormalized(int index) {
        if (categories.isEmpty()) {
            return 0.5;
        }
        return (index + 0.5) / categories.size();
    }

    /**
     * Converts a category index to a screen coordinate.
     *
     * @param index category index
     * @param axisStart start pixel position of the axis
     * @param axisLength length of the axis in pixels
     * @return screen coordinate at the start of the category slot
     */
    public double indexToScreen(int index, double axisStart, double axisLength) {
        return axisStart + indexToNormalized(index) * axisLength;
    }

    /**
     * Converts a category index to a centered screen coordinate.
     *
     * @param index category index
     * @param axisStart start pixel position of the axis
     * @param axisLength length of the axis in pixels
     * @return screen coordinate at the center of the category slot
     */
    public double indexToCenterScreen(int index, double axisStart, double axisLength) {
        return axisStart + indexToCenterNormalized(index) * axisLength;
    }

    /**
     * Converts a screen coordinate to a category index.
     *
     * @param screen screen coordinate
     * @param axisStart start pixel position of the axis
     * @param axisLength length of the axis in pixels
     * @return category index (may be out of bounds if screen is outside axis)
     */
    public int screenToIndex(double screen, double axisStart, double axisLength) {
        if (categories.isEmpty() || axisLength <= 0) {
            return 0;
        }
        double normalized = (screen - axisStart) / axisLength;
        return (int) Math.floor(normalized * categories.size());
    }

    /**
     * Returns the width of each category slot in pixels.
     *
     * @param axisLength total axis length in pixels
     * @return slot width, or axisLength if no categories
     */
    public double getCategorySlotWidth(double axisLength) {
        if (categories.isEmpty()) {
            return axisLength;
        }
        return axisLength / categories.size();
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    /**
     * Returns the internal axis position enum.
     *
     * @return the position as CategoryAxis.Position (includes LEFT/RIGHT)
     */
    public Position getInternalPosition() {
        return position;
    }

    // ========== HorizontalAxis Implementation ==========

    /**
     * Returns the axis position as a HorizontalAxis.Position.
     * Note: CategoryAxis supports LEFT/RIGHT which map to BOTTOM for this interface.
     *
     * @return the position (TOP or BOTTOM)
     */
    @Override
    public HorizontalAxis.Position getPosition() {
        return position == Position.TOP ? HorizontalAxis.Position.TOP : HorizontalAxis.Position.BOTTOM;
    }

    /**
     * Converts a category index to a normalized position in [0, 1) range.
     *
     * @param index the category index
     * @return normalized position
     */
    @Override
    public double toNormalized(Integer index) {
        return indexToNormalized(index);
    }

    /**
     * Returns false since this is a categorical axis.
     *
     * @return false
     */
    @Override
    public boolean isTimeBased() {
        return false;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets axis visibility.
     *
     * @param visible true to show the axis
     */
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getHeight() {
        return height;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public LabelAlignment getLabelAlignment() {
        return labelAlignment;
    }

    public boolean isShowGridLines() {
        return showGridLines;
    }

    public Color getGridLineColor() {
        return gridLineColor;
    }

    public int getMaxLabelWidth() {
        return maxLabelWidth;
    }

    public boolean isRotateLabels() {
        return rotateLabels;
    }

    public double getLabelRotationAngle() {
        return labelRotationAngle;
    }

    public int getLabelPadding() {
        return labelPadding;
    }

    public boolean isHorizontal() {
        return position == Position.TOP || position == Position.BOTTOM;
    }

    // ========== Fluent Setters ==========

    public CategoryAxis position(Position position) {
        this.position = position;
        return this;
    }

    public CategoryAxis visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public CategoryAxis height(int height) {
        this.height = Math.max(0, height);
        return this;
    }

    public CategoryAxis labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public CategoryAxis lineColor(Color color) {
        this.lineColor = color;
        return this;
    }

    public CategoryAxis lineWidth(float width) {
        this.lineWidth = Math.max(0, width);
        return this;
    }

    public CategoryAxis labelAlignment(LabelAlignment alignment) {
        this.labelAlignment = alignment;
        return this;
    }

    public CategoryAxis showGridLines(boolean show) {
        this.showGridLines = show;
        return this;
    }

    public CategoryAxis gridLineColor(Color color) {
        this.gridLineColor = color;
        return this;
    }

    public CategoryAxis maxLabelWidth(int width) {
        this.maxLabelWidth = Math.max(0, width);
        return this;
    }

    public CategoryAxis rotateLabels(boolean rotate) {
        this.rotateLabels = rotate;
        return this;
    }

    public CategoryAxis labelRotationAngle(double degrees) {
        this.labelRotationAngle = degrees;
        return this;
    }

    public CategoryAxis labelPadding(int padding) {
        this.labelPadding = Math.max(0, padding);
        return this;
    }

    // ========== Utility Methods ==========

    /**
     * Creates a copy of this axis with a new ID.
     */
    public CategoryAxis copy(String newId) {
        CategoryAxis copy = new CategoryAxis(newId, this.position);
        copy.categories.addAll(this.categories);
        copy.visible = this.visible;
        copy.height = this.height;
        copy.labelColor = this.labelColor;
        copy.lineColor = this.lineColor;
        copy.lineWidth = this.lineWidth;
        copy.labelAlignment = this.labelAlignment;
        copy.showGridLines = this.showGridLines;
        copy.gridLineColor = this.gridLineColor;
        copy.maxLabelWidth = this.maxLabelWidth;
        copy.rotateLabels = this.rotateLabels;
        copy.labelRotationAngle = this.labelRotationAngle;
        copy.labelPadding = this.labelPadding;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("CategoryAxis[id=%s, pos=%s, categories=%d, visible=%s]",
                id, position, categories.size(), visible);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CategoryAxis other = (CategoryAxis) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
