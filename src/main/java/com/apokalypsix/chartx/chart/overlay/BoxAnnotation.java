package com.apokalypsix.chartx.chart.overlay;

import java.awt.Color;
import java.util.UUID;

/**
 * A rectangular box annotation for programmatic use.
 *
 * <p>BoxAnnotation is a simple box with time and price bounds, designed for
 * programmatic addition of chart regions like FVGs (Fair Value Gaps),
 * TPO boxes, Initial Balance zones, or volume profile bars.
 *
 * <p>Unlike the interactive {@code Rectangle} drawing, BoxAnnotation has no
 * selection handles or drag behavior - it's purely a visual annotation.
 *
 * <p>Example usage:
 * <pre>{@code
 * BoxAnnotation fvg = new BoxAnnotation(
 *     startTime, endTime,
 *     lowPrice, highPrice,
 *     new Color(10, 147, 150, 100)  // semi-transparent teal
 * );
 * fvg.setLabel("FVG");
 * chart.addAnnotation(fvg);
 * }</pre>
 */
public class BoxAnnotation extends Annotation {

    // End coordinates (base class has start coordinates via timestamp/price)
    private long endTime;
    private double endPrice;

    // Styling
    private Color fillColor;
    private Color borderColor = null;
    private float borderWidth = 1.0f;

    // Optional label
    private String label = null;
    private Color labelColor = Color.WHITE;

    /**
     * Creates a box annotation with the specified bounds and fill color.
     *
     * @param startTime start time (epoch milliseconds)
     * @param endTime   end time (epoch milliseconds)
     * @param lowPrice  lower price bound
     * @param highPrice upper price bound
     * @param fillColor fill color (use alpha for transparency)
     */
    public BoxAnnotation(long startTime, long endTime, double lowPrice, double highPrice, Color fillColor) {
        this(UUID.randomUUID().toString(), startTime, endTime, lowPrice, highPrice, fillColor);
    }

    /**
     * Creates a box annotation with the specified ID, bounds, and fill color.
     *
     * @param id        unique identifier
     * @param startTime start time (epoch milliseconds)
     * @param endTime   end time (epoch milliseconds)
     * @param lowPrice  lower price bound
     * @param highPrice upper price bound
     * @param fillColor fill color (use alpha for transparency)
     */
    public BoxAnnotation(String id, long startTime, long endTime, double lowPrice, double highPrice, Color fillColor) {
        super(id, startTime, lowPrice);
        this.endTime = endTime;
        this.endPrice = highPrice;
        this.fillColor = fillColor;
    }

    @Override
    public AnnotationType getType() {
        return AnnotationType.BOX;
    }

    // ========== Bounds getters ==========

    /**
     * Returns the start time (left edge).
     */
    public long getStartTime() {
        return timestamp;
    }

    /**
     * Returns the end time (right edge).
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Returns the low price (bottom edge).
     */
    public double getLowPrice() {
        return price;
    }

    /**
     * Returns the high price (top edge).
     */
    public double getHighPrice() {
        return endPrice;
    }

    /**
     * Returns the fill color.
     */
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * Returns the border color, or null if no border.
     */
    public Color getBorderColor() {
        return borderColor;
    }

    /**
     * Returns the border width in pixels.
     */
    public float getBorderWidth() {
        return borderWidth;
    }

    /**
     * Returns the label text, or null if none.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the label text color.
     */
    public Color getLabelColor() {
        return labelColor;
    }

    // ========== Bounds setters ==========

    /**
     * Sets the time bounds.
     *
     * @param startTime start time (epoch milliseconds)
     * @param endTime   end time (epoch milliseconds)
     */
    public void setTimeBounds(long startTime, long endTime) {
        this.timestamp = startTime;
        this.endTime = endTime;
    }

    /**
     * Sets the start time (left edge).
     */
    public void setStartTime(long startTime) {
        this.timestamp = startTime;
    }

    /**
     * Sets the end time (right edge).
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the price bounds.
     *
     * @param lowPrice  lower price bound
     * @param highPrice upper price bound
     */
    public void setPriceBounds(double lowPrice, double highPrice) {
        this.price = lowPrice;
        this.endPrice = highPrice;
    }

    /**
     * Sets the low price (bottom edge).
     */
    public void setLowPrice(double lowPrice) {
        this.price = lowPrice;
    }

    /**
     * Sets the high price (top edge).
     */
    public void setHighPrice(double highPrice) {
        this.endPrice = highPrice;
    }

    /**
     * Sets the fill color.
     *
     * @param fillColor fill color (use alpha for transparency)
     */
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    /**
     * Sets the border color. Pass null to remove the border.
     *
     * @param borderColor border color, or null for no border
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * Sets the border width in pixels.
     *
     * @param borderWidth border width
     */
    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    /**
     * Sets the label text. Pass null to remove the label.
     *
     * @param label label text, or null for no label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets the label text color.
     *
     * @param labelColor label text color
     */
    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    /**
     * Checks if a point is inside this box.
     *
     * @param time  timestamp to check
     * @param price price to check
     * @return true if the point is inside the box
     */
    public boolean contains(long time, double price) {
        long minTime = Math.min(timestamp, endTime);
        long maxTime = Math.max(timestamp, endTime);
        double minPrice = Math.min(this.price, endPrice);
        double maxPrice = Math.max(this.price, endPrice);

        return time >= minTime && time <= maxTime && price >= minPrice && price <= maxPrice;
    }
}
