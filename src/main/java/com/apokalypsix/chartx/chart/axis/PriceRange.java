package com.apokalypsix.chartx.chart.axis;

/**
 * Immutable value object representing a price interval.
 */
public final class PriceRange {

    private final double min;
    private final double max;

    /**
     * Creates a price range.
     *
     * @param min minimum price (inclusive)
     * @param max maximum price (inclusive)
     * @throws IllegalArgumentException if max is less than min
     */
    public PriceRange(double min, double max) {
        if (max < min) {
            throw new IllegalArgumentException("Max must be >= min: min=" + min + ", max=" + max);
        }
        this.min = min;
        this.max = max;
    }

    /**
     * Creates a price range centered at the given price with the specified span.
     */
    public static PriceRange centered(double center, double span) {
        double halfSpan = span / 2.0;
        return new PriceRange(center - halfSpan, center + halfSpan);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    /**
     * Returns the span (max - min) of this range.
     */
    public double getSpan() {
        return max - min;
    }

    /**
     * Returns true if the given price falls within this range (inclusive).
     */
    public boolean contains(double price) {
        return price >= min && price <= max;
    }

    /**
     * Returns true if this range overlaps with another range.
     */
    public boolean overlaps(PriceRange other) {
        return this.min <= other.max && this.max >= other.min;
    }

    /**
     * Returns a new range expanded by the given percentage (0.1 = 10% expansion).
     */
    public PriceRange expandByPercent(double percent) {
        double expansion = getSpan() * percent;
        return new PriceRange(min - expansion, max + expansion);
    }

    /**
     * Returns a new range expanded by the given absolute amount on both sides.
     */
    public PriceRange expand(double amount) {
        return new PriceRange(min - amount, max + amount);
    }

    /**
     * Returns the midpoint of this range.
     */
    public double getMidpoint() {
        return (min + max) / 2.0;
    }

    /**
     * Returns a normalized position (0.0 to 1.0) for the given price within this range.
     * 0.0 corresponds to min, 1.0 corresponds to max.
     */
    public double normalize(double price) {
        if (min == max) {
            return 0.5;
        }
        return (price - min) / (max - min);
    }

    /**
     * Interpolates a price from a normalized position (0.0 to 1.0).
     */
    public double interpolate(double normalizedPosition) {
        return min + normalizedPosition * (max - min);
    }

    /**
     * Returns a new range that encompasses both this range and the given price.
     */
    public PriceRange include(double price) {
        if (price < min) {
            return new PriceRange(price, max);
        } else if (price > max) {
            return new PriceRange(min, price);
        }
        return this;
    }

    /**
     * Returns a new range that encompasses both this range and another range.
     */
    public PriceRange union(PriceRange other) {
        return new PriceRange(Math.min(this.min, other.min), Math.max(this.max, other.max));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceRange)) return false;
        PriceRange that = (PriceRange) o;
        return Double.compare(that.min, min) == 0 && Double.compare(that.max, max) == 0;
    }

    @Override
    public int hashCode() {
        long h1 = Double.doubleToLongBits(min);
        long h2 = Double.doubleToLongBits(max);
        return (int) (h1 ^ (h1 >>> 32)) * 31 + (int) (h2 ^ (h2 >>> 32));
    }

    @Override
    public String toString() {
        return String.format("PriceRange[%.4f, %.4f]", min, max);
    }
}
