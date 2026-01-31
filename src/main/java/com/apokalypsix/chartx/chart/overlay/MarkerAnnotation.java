package com.apokalypsix.chartx.chart.overlay;

import java.awt.Color;

/**
 * Marker annotation for displaying icons/symbols on the chart.
 *
 * <p>Markers are simple geometric shapes or arrows used to highlight
 * specific data points or events. They are rendered using OpenGL for
 * performance with many markers.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Buy signal marker
 * MarkerAnnotation buy = new MarkerAnnotation("buy1", timestamp, lowPrice, MarkerShape.ARROW_UP);
 * buy.setColor(new Color(38, 166, 91));
 * buy.setSize(12);
 * annotationLayer.addAnnotation(buy);
 *
 * // Sell signal marker
 * MarkerAnnotation sell = new MarkerAnnotation("sell1", timestamp, highPrice, MarkerShape.ARROW_DOWN);
 * sell.setColor(new Color(214, 69, 65));
 * annotationLayer.addAnnotation(sell);
 * }</pre>
 */
public class MarkerAnnotation extends Annotation {

    private MarkerShape shape = MarkerShape.CIRCLE;
    private int size = 8;  // Size in pixels
    private boolean filled = true;
    private int strokeWidth = 2;

    // Optional label shown next to marker
    private String label = null;

    /**
     * Creates a marker annotation with the specified shape.
     *
     * @param id unique identifier
     * @param timestamp time coordinate
     * @param price price coordinate
     * @param shape the marker shape
     */
    public MarkerAnnotation(String id, long timestamp, double price, MarkerShape shape) {
        super(id, timestamp, price);
        this.shape = shape;
    }

    /**
     * Creates a marker annotation with default ID.
     */
    public MarkerAnnotation(long timestamp, double price, MarkerShape shape) {
        this("marker_" + System.nanoTime(), timestamp, price, shape);
    }

    /**
     * Creates a circle marker.
     */
    public MarkerAnnotation(long timestamp, double price) {
        this(timestamp, price, MarkerShape.CIRCLE);
    }

    @Override
    public AnnotationType getType() {
        return AnnotationType.MARKER;
    }

    // ========== Marker-specific getters ==========

    public MarkerShape getShape() {
        return shape;
    }

    public int getSize() {
        return size;
    }

    public boolean isFilled() {
        return filled;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public String getLabel() {
        return label;
    }

    public boolean hasLabel() {
        return label != null && !label.isEmpty();
    }

    // ========== Marker-specific setters ==========

    public void setShape(MarkerShape shape) {
        this.shape = shape;
    }

    public void setSize(int size) {
        this.size = Math.max(2, size);
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = Math.max(1, strokeWidth);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("MarkerAnnotation[id=%s, shape=%s, time=%d, price=%.2f]",
                getId(), shape, timestamp, price);
    }

    // ========== Factory methods for common markers ==========

    /**
     * Creates a buy signal marker (green up arrow).
     */
    public static MarkerAnnotation buySignal(long timestamp, double price) {
        MarkerAnnotation marker = new MarkerAnnotation(timestamp, price, MarkerShape.ARROW_UP);
        marker.setColor(new Color(38, 166, 91));
        marker.setSize(10);
        return marker;
    }

    /**
     * Creates a sell signal marker (red down arrow).
     */
    public static MarkerAnnotation sellSignal(long timestamp, double price) {
        MarkerAnnotation marker = new MarkerAnnotation(timestamp, price, MarkerShape.ARROW_DOWN);
        marker.setColor(new Color(214, 69, 65));
        marker.setSize(10);
        return marker;
    }

    /**
     * Creates an alert marker (yellow triangle).
     */
    public static MarkerAnnotation alert(long timestamp, double price) {
        MarkerAnnotation marker = new MarkerAnnotation(timestamp, price, MarkerShape.TRIANGLE_UP);
        marker.setColor(new Color(255, 193, 7));
        marker.setSize(10);
        return marker;
    }

    /**
     * Creates an info marker (blue circle).
     */
    public static MarkerAnnotation info(long timestamp, double price) {
        MarkerAnnotation marker = new MarkerAnnotation(timestamp, price, MarkerShape.CIRCLE);
        marker.setColor(new Color(33, 150, 243));
        marker.setSize(8);
        return marker;
    }

    /**
     * Marker shapes available for rendering.
     */
    public enum MarkerShape {
        // Basic shapes
        CIRCLE,
        SQUARE,
        DIAMOND,

        // Triangles
        TRIANGLE_UP,
        TRIANGLE_DOWN,

        // Arrows
        ARROW_UP,
        ARROW_DOWN,

        // Crosses
        CROSS,      // + shape
        X,          // X shape

        // Special
        STAR,
        FLAG
    }
}
