package com.apokalypsix.chartx.chart.overlay.region;

import java.awt.Color;

/**
 * Represents a price range region with background coloring.
 *
 * <p>Regions are used to highlight specific price zones on the chart,
 * such as support/resistance levels, value areas, or trading ranges.
 * Price range regions span the full chart width.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Highlight a support zone
 * PriceRangeRegion support = new PriceRangeRegion(
 *     95.00, 97.50,
 *     new Color(50, 150, 50, 60),  // Semi-transparent green
 *     "Support Zone"
 * );
 * regionLayer.addPriceRegion(support);
 * }</pre>
 */
public class PriceRangeRegion {

    private final String id;
    private double minPrice;
    private double maxPrice;
    private Color fillColor;
    private Color borderColor;
    private String label;
    private boolean visible = true;

    /**
     * Creates a region with the specified price range and fill color.
     *
     * @param minPrice lower price boundary
     * @param maxPrice upper price boundary
     * @param fillColor background fill color (use alpha for transparency)
     */
    public PriceRangeRegion(double minPrice, double maxPrice, Color fillColor) {
        this(generateId(), minPrice, maxPrice, fillColor, null, null);
    }

    /**
     * Creates a region with the specified price range, fill color, and label.
     *
     * @param minPrice lower price boundary
     * @param maxPrice upper price boundary
     * @param fillColor background fill color
     * @param label optional label to display
     */
    public PriceRangeRegion(double minPrice, double maxPrice, Color fillColor, String label) {
        this(generateId(), minPrice, maxPrice, fillColor, null, label);
    }

    /**
     * Creates a fully configured region.
     *
     * @param id unique identifier
     * @param minPrice lower price boundary
     * @param maxPrice upper price boundary
     * @param fillColor background fill color
     * @param borderColor optional border color
     * @param label optional label
     */
    public PriceRangeRegion(String id, double minPrice, double maxPrice,
                            Color fillColor, Color borderColor, String label) {
        if (maxPrice < minPrice) {
            throw new IllegalArgumentException("Max price must be >= min price");
        }
        this.id = id;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.label = label;
    }

    private static String generateId() {
        return "price_region_" + System.nanoTime();
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getHeight() {
        return maxPrice - minPrice;
    }

    public double getMidPrice() {
        return (minPrice + maxPrice) / 2.0;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public String getLabel() {
        return label;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean hasBorder() {
        return borderColor != null;
    }

    public boolean hasLabel() {
        return label != null && !label.isEmpty();
    }

    // ========== Setters ==========

    public void setMinPrice(double minPrice) {
        if (minPrice > this.maxPrice) {
            throw new IllegalArgumentException("Min price must be <= max price");
        }
        this.minPrice = minPrice;
    }

    public void setMaxPrice(double maxPrice) {
        if (maxPrice < this.minPrice) {
            throw new IllegalArgumentException("Max price must be >= min price");
        }
        this.maxPrice = maxPrice;
    }

    public void setPriceRange(double minPrice, double maxPrice) {
        if (maxPrice < minPrice) {
            throw new IllegalArgumentException("Max price must be >= min price");
        }
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // ========== Utility ==========

    /**
     * Returns true if this region overlaps with the given price range.
     */
    public boolean overlaps(double rangeMin, double rangeMax) {
        return minPrice < rangeMax && maxPrice > rangeMin;
    }

    /**
     * Returns true if the given price is within this region.
     */
    public boolean contains(double price) {
        return price >= minPrice && price <= maxPrice;
    }

    @Override
    public String toString() {
        return String.format("PriceRangeRegion[id=%s, price=%.2f-%.2f, label=%s]",
                id, minPrice, maxPrice, label);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PriceRangeRegion other = (PriceRangeRegion) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
