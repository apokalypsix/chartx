package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.chart.style.LineStyle;

import java.awt.Color;
import java.util.List;

/**
 * Abstract base class for chart drawings.
 *
 * <p>Drawings are interactive chart elements defined by anchor points in data coordinates
 * (time/price). This ensures drawings move correctly during pan/zoom operations.
 *
 * <p>Subclasses define specific drawing types such as trend lines, horizontal lines,
 * rectangles, etc.
 *
 * <p>Drawings support {@link LineStyle} for customizable line rendering including
 * dashed lines, colors, and opacity.
 */
public abstract class Drawing {

    /** Drawing types */
    public enum Type {
        // Line tools
        TREND_LINE,
        HORIZONTAL_LINE,
        VERTICAL_LINE,
        RAY,
        EXTENDED_LINE,
        PARALLEL_CHANNEL,
        REGRESSION_CHANNEL,

        // Shape tools
        RECTANGLE,
        ELLIPSE,
        TRIANGLE,
        POLYLINE,
        POLYGON,
        ARROW,
        ARC,

        // Fibonacci tools
        FIBONACCI_RETRACEMENT,
        FIBONACCI_EXTENSION,
        FIBONACCI_TIME_ZONES,
        FIBONACCI_FAN,
        FIBONACCI_ARC,

        // Gann tools
        GANN_FAN,
        GANN_BOX,

        // Analysis tools
        PITCHFORK,
        MEASURE_TOOL,
        PRICE_RANGE,

        // Annotations
        CALLOUT,
        PRICE_LABEL,
        NOTE
    }

    /** Handle types for editing */
    public enum HandleType {
        NONE,
        ANCHOR_0,
        ANCHOR_1,
        ANCHOR_2,
        ANCHOR_3,
        BODY  // For dragging the entire drawing
    }

    private final String id;
    private Color color = new Color(255, 193, 7);  // Default yellow
    private float lineWidth = 1.5f;
    private float opacity = 1.0f;
    private LineStyle lineStyle;  // Optional LineStyle for advanced styling
    private boolean visible = true;
    private boolean selected = false;

    // Pending values for staged changes (matching indicator staging pattern)
    private Color pendingColor;
    private Float pendingLineWidth;
    private Float pendingOpacity;
    private Boolean pendingVisible;
    private boolean hasPendingChanges = false;

    /**
     * Creates a drawing with the specified ID.
     *
     * @param id unique identifier for this drawing
     */
    protected Drawing(String id) {
        this.id = id;
    }

    // ========== Abstract methods ==========

    /**
     * Returns the type of this drawing.
     */
    public abstract Type getType();

    /**
     * Returns all anchor points for this drawing.
     */
    public abstract List<AnchorPoint> getAnchorPoints();

    /**
     * Returns true if the drawing has all required anchor points.
     */
    public abstract boolean isComplete();

    /**
     * Sets an anchor point at the specified index.
     *
     * @param index anchor index
     * @param point the new anchor point
     */
    public abstract void setAnchorPoint(int index, AnchorPoint point);

    /**
     * Returns the number of anchor points this drawing type requires.
     */
    public abstract int getRequiredAnchorCount();

    /**
     * Returns true if the specified screen coordinates are within hit distance of this drawing.
     *
     * @param screenX screen X coordinate
     * @param screenY screen Y coordinate
     * @param coords coordinate system for transformations
     * @param hitDistance hit detection distance in pixels
     * @return true if point is near the drawing
     */
    public abstract boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance);

    /**
     * Returns the handle type at the specified screen coordinates, or NONE if not on a handle.
     *
     * @param screenX screen X coordinate
     * @param screenY screen Y coordinate
     * @param coords coordinate system for transformations
     * @param handleRadius handle detection radius in pixels
     * @return the handle type at the position
     */
    public abstract HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius);

    // ========== Properties ==========

    /**
     * Returns the unique ID of this drawing.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the drawing color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the drawing color.
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Returns the line width.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the line width.
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Returns the opacity (0.0 - 1.0).
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Sets the opacity (0.0 - 1.0).
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    /**
     * Returns the line style for this drawing.
     * If a LineStyle has been explicitly set, returns that.
     * Otherwise, creates a LineStyle from the basic properties (color, width, opacity).
     */
    public LineStyle getLineStyle() {
        if (lineStyle != null) {
            return lineStyle;
        }
        return LineStyle.solid(color, lineWidth).withOpacity(opacity);
    }

    /**
     * Sets the line style for this drawing.
     * Setting a LineStyle overrides the individual color, width, and opacity properties.
     *
     * @param lineStyle the line style, or null to use basic properties
     */
    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
        if (lineStyle != null) {
            this.color = lineStyle.color();
            this.lineWidth = lineStyle.width();
            this.opacity = lineStyle.opacity();
        }
    }

    /**
     * Returns true if this drawing has a custom LineStyle (with dash pattern etc).
     */
    public boolean hasCustomLineStyle() {
        return lineStyle != null;
    }

    /**
     * Returns true if the drawing is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the visibility of this drawing.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns true if the drawing is selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selection state of this drawing.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    // ========== Staged Changes Support ==========

    /**
     * Stages a color change without applying it immediately.
     *
     * @param color the new color
     */
    public void stageColor(Color color) {
        this.pendingColor = color;
        this.hasPendingChanges = true;
    }

    /**
     * Stages a line width change without applying it immediately.
     *
     * @param lineWidth the new line width
     */
    public void stageLineWidth(float lineWidth) {
        this.pendingLineWidth = lineWidth;
        this.hasPendingChanges = true;
    }

    /**
     * Stages an opacity change without applying it immediately.
     *
     * @param opacity the new opacity (0.0 - 1.0)
     */
    public void stageOpacity(float opacity) {
        this.pendingOpacity = Math.max(0f, Math.min(1f, opacity));
        this.hasPendingChanges = true;
    }

    /**
     * Stages a visibility change without applying it immediately.
     *
     * @param visible the new visibility
     */
    public void stageVisible(boolean visible) {
        this.pendingVisible = visible;
        this.hasPendingChanges = true;
    }

    /**
     * Applies all staged changes.
     *
     * @return true if any changes were applied
     */
    public boolean applyChanges() {
        if (!hasPendingChanges) {
            return false;
        }

        if (pendingColor != null) {
            this.color = pendingColor;
            if (lineStyle != null) {
                // Update line style color
                this.lineStyle = lineStyle.withColor(pendingColor);
            }
        }
        if (pendingLineWidth != null) {
            this.lineWidth = pendingLineWidth;
            if (lineStyle != null) {
                this.lineStyle = lineStyle.withWidth(pendingLineWidth);
            }
        }
        if (pendingOpacity != null) {
            this.opacity = pendingOpacity;
            if (lineStyle != null) {
                this.lineStyle = lineStyle.withOpacity(pendingOpacity);
            }
        }
        if (pendingVisible != null) {
            this.visible = pendingVisible;
        }

        clearPendingChanges();
        return true;
    }

    /**
     * Discards all staged changes without applying them.
     */
    public void discardChanges() {
        clearPendingChanges();
    }

    private void clearPendingChanges() {
        pendingColor = null;
        pendingLineWidth = null;
        pendingOpacity = null;
        pendingVisible = null;
        hasPendingChanges = false;
    }

    /**
     * Returns true if there are pending changes that haven't been applied.
     */
    public boolean hasPendingChanges() {
        return hasPendingChanges;
    }

    /**
     * Returns the effective color (pending if staged, otherwise current).
     */
    public Color getEffectiveColor() {
        return pendingColor != null ? pendingColor : color;
    }

    /**
     * Returns the effective line width (pending if staged, otherwise current).
     */
    public float getEffectiveLineWidth() {
        return pendingLineWidth != null ? pendingLineWidth : lineWidth;
    }

    /**
     * Returns the effective opacity (pending if staged, otherwise current).
     */
    public float getEffectiveOpacity() {
        return pendingOpacity != null ? pendingOpacity : opacity;
    }

    /**
     * Returns the effective visibility (pending if staged, otherwise current).
     */
    public boolean getEffectiveVisible() {
        return pendingVisible != null ? pendingVisible : visible;
    }

    // ========== Utility methods ==========

    /**
     * Calculates the distance from a point to a line segment.
     *
     * @param px point X
     * @param py point Y
     * @param x1 line start X
     * @param y1 line start Y
     * @param x2 line end X
     * @param y2 line end Y
     * @return distance in pixels
     */
    protected static double distanceToLineSegment(double px, double py,
                                                   double x1, double y1,
                                                   double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0) {
            // Line segment is a point
            return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // Project point onto line, clamping to segment
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSq));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }

    /**
     * Calculates the distance from a point to an infinite line.
     *
     * @param px point X
     * @param py point Y
     * @param x1 line point 1 X
     * @param y1 line point 1 Y
     * @param x2 line point 2 X
     * @param y2 line point 2 Y
     * @return distance in pixels
     */
    protected static double distanceToLine(double px, double py,
                                            double x1, double y1,
                                            double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0) {
            return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // Distance using cross product
        return Math.abs((py - y1) * dx - (px - x1) * dy) / Math.sqrt(lengthSq);
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, type=%s, anchors=%d]",
                getClass().getSimpleName(), id, getType(), getAnchorPoints().size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Drawing drawing = (Drawing) obj;
        return id.equals(drawing.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
