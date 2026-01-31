package com.apokalypsix.chartx.chart.overlay;

import java.awt.Color;

/**
 * Base class for chart annotations.
 *
 * <p>Annotations are visual elements positioned at specific time/price coordinates
 * on the chart. They can be used to mark significant events, add notes, or
 * highlight specific data points.
 *
 * <p>Annotations support two positioning modes:
 * <ul>
 *   <li><b>Absolute</b> - Fixed at a specific time and price</li>
 *   <li><b>Relative</b> - Offset from a data point (follows the data)</li>
 * </ul>
 */
public abstract class Annotation {

    private final String id;

    // Position (in data coordinates)
    protected long timestamp;
    protected double price;

    // Offset from anchor point (in pixels)
    protected int offsetX = 0;
    protected int offsetY = 0;

    // Visibility
    protected boolean visible = true;

    // Z-order for layering (higher = on top)
    protected int zOrder = 0;

    // Common styling
    protected Color color = new Color(220, 222, 226);
    protected Color backgroundColor = null;
    protected float opacity = 1.0f;

    /**
     * Creates an annotation at the specified position.
     *
     * @param id unique identifier
     * @param timestamp time coordinate (epoch milliseconds)
     * @param price price/value coordinate
     */
    protected Annotation(String id, long timestamp, double price) {
        this.id = id;
        this.timestamp = timestamp;
        this.price = price;
    }

    /**
     * Returns the annotation type for rendering dispatch.
     */
    public abstract AnnotationType getType();

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getPrice() {
        return price;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getZOrder() {
        return zOrder;
    }

    public Color getColor() {
        return color;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public float getOpacity() {
        return opacity;
    }

    // ========== Setters ==========

    public void setPosition(long timestamp, double price) {
        this.timestamp = timestamp;
        this.price = price;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setZOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Annotation other = (Annotation) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Annotation types for rendering dispatch.
     */
    public enum AnnotationType {
        TEXT,
        MARKER,
        LABEL,  // Text with background box
        BOX     // Rectangular region with time+price bounds
    }
}
