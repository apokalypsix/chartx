package com.apokalypsix.chartx.chart.style;

import java.awt.Color;
import java.util.Arrays;

/**
 * Defines the visual style for rendering lines.
 *
 * <p>LineStyle encapsulates all properties needed to render a line:
 * <ul>
 *   <li>Color</li>
 *   <li>Width in pixels</li>
 *   <li>Dash pattern (null for solid lines)</li>
 *   <li>Dash phase offset</li>
 *   <li>Cap style (how line ends are drawn)</li>
 *   <li>Opacity</li>
 * </ul>
 *
 * <p>Immutable value object with builder pattern for easy construction.
 */
public record LineStyle(
        Color color,
        float width,
        float[] dashPattern,
        float dashPhase,
        CapStyle capStyle,
        float opacity
) {

    /**
     * Line end cap styles.
     */
    public enum CapStyle {
        /** Flat end at exactly the endpoint */
        BUTT,
        /** Rounded end extending past the endpoint */
        ROUND,
        /** Square end extending past the endpoint */
        SQUARE
    }

    // ========== Predefined dash patterns ==========

    /** Dashed line pattern: 10 on, 5 off */
    public static final float[] DASH_PATTERN = new float[]{10f, 5f};

    /** Dotted line pattern: 2 on, 4 off */
    public static final float[] DOT_PATTERN = new float[]{2f, 4f};

    /** Dash-dot pattern: 10 on, 3 off, 2 on, 3 off */
    public static final float[] DASH_DOT_PATTERN = new float[]{10f, 3f, 2f, 3f};

    /** Long dash pattern: 20 on, 10 off */
    public static final float[] LONG_DASH_PATTERN = new float[]{20f, 10f};

    /** Short dash pattern: 5 on, 3 off */
    public static final float[] SHORT_DASH_PATTERN = new float[]{5f, 3f};

    // ========== Predefined styles ==========

    /** Default solid line style */
    public static final LineStyle DEFAULT = solid(Color.WHITE, 1.0f);

    /** Thin solid line */
    public static final LineStyle THIN = solid(Color.WHITE, 0.5f);

    /** Thick solid line */
    public static final LineStyle THICK = solid(Color.WHITE, 2.0f);

    /**
     * Compact constructor with validation.
     */
    public LineStyle {
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be positive");
        }
        if (opacity < 0 || opacity > 1) {
            throw new IllegalArgumentException("Opacity must be between 0 and 1");
        }
        if (dashPattern != null) {
            for (float dash : dashPattern) {
                if (dash <= 0) {
                    throw new IllegalArgumentException("Dash pattern values must be positive");
                }
            }
            // Defensive copy
            dashPattern = dashPattern.clone();
        }
        if (capStyle == null) {
            capStyle = CapStyle.BUTT;
        }
    }

    /**
     * Returns true if this is a solid (non-dashed) line.
     */
    public boolean isSolid() {
        return dashPattern == null || dashPattern.length == 0;
    }

    /**
     * Returns true if this is a dashed line.
     */
    public boolean isDashed() {
        return dashPattern != null && dashPattern.length > 0;
    }

    /**
     * Returns the total length of one dash pattern cycle.
     */
    public float getDashPatternLength() {
        if (dashPattern == null) {
            return 0;
        }
        float total = 0;
        for (float dash : dashPattern) {
            total += dash;
        }
        return total;
    }

    /**
     * Returns a copy of the dash pattern, or null if solid.
     */
    @Override
    public float[] dashPattern() {
        return dashPattern != null ? dashPattern.clone() : null;
    }

    /**
     * Returns the effective color with opacity applied.
     */
    public Color getEffectiveColor() {
        if (opacity >= 1.0f) {
            return color;
        }
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) (color.getAlpha() * opacity)
        );
    }

    // ========== Factory methods ==========

    /**
     * Creates a solid line style.
     *
     * @param color line color
     * @param width line width in pixels
     * @return a solid line style
     */
    public static LineStyle solid(Color color, float width) {
        return new LineStyle(color, width, null, 0, CapStyle.BUTT, 1.0f);
    }

    /**
     * Creates a dashed line style.
     *
     * @param color line color
     * @param width line width in pixels
     * @return a dashed line style
     */
    public static LineStyle dashed(Color color, float width) {
        return new LineStyle(color, width, DASH_PATTERN, 0, CapStyle.BUTT, 1.0f);
    }

    /**
     * Creates a dotted line style.
     *
     * @param color line color
     * @param width line width in pixels
     * @return a dotted line style
     */
    public static LineStyle dotted(Color color, float width) {
        return new LineStyle(color, width, DOT_PATTERN, 0, CapStyle.ROUND, 1.0f);
    }

    /**
     * Creates a dash-dot line style.
     *
     * @param color line color
     * @param width line width in pixels
     * @return a dash-dot line style
     */
    public static LineStyle dashDot(Color color, float width) {
        return new LineStyle(color, width, DASH_DOT_PATTERN, 0, CapStyle.BUTT, 1.0f);
    }

    /**
     * Creates a line style with a custom dash pattern.
     *
     * @param color line color
     * @param width line width in pixels
     * @param dashPattern the dash pattern (alternating on/off lengths)
     * @return a custom dashed line style
     */
    public static LineStyle custom(Color color, float width, float[] dashPattern) {
        return new LineStyle(color, width, dashPattern, 0, CapStyle.BUTT, 1.0f);
    }

    // ========== Builder methods (return new instances) ==========

    /**
     * Returns a new LineStyle with a different color.
     */
    public LineStyle withColor(Color newColor) {
        return new LineStyle(newColor, width, dashPattern, dashPhase, capStyle, opacity);
    }

    /**
     * Returns a new LineStyle with a different width.
     */
    public LineStyle withWidth(float newWidth) {
        return new LineStyle(color, newWidth, dashPattern, dashPhase, capStyle, opacity);
    }

    /**
     * Returns a new LineStyle with a different dash pattern.
     */
    public LineStyle withDashPattern(float[] newDashPattern) {
        return new LineStyle(color, width, newDashPattern, dashPhase, capStyle, opacity);
    }

    /**
     * Returns a new LineStyle with a different dash phase.
     */
    public LineStyle withDashPhase(float newDashPhase) {
        return new LineStyle(color, width, dashPattern, newDashPhase, capStyle, opacity);
    }

    /**
     * Returns a new LineStyle with a different cap style.
     */
    public LineStyle withCapStyle(CapStyle newCapStyle) {
        return new LineStyle(color, width, dashPattern, dashPhase, newCapStyle, opacity);
    }

    /**
     * Returns a new LineStyle with a different opacity.
     */
    public LineStyle withOpacity(float newOpacity) {
        return new LineStyle(color, width, dashPattern, dashPhase, capStyle, newOpacity);
    }

    /**
     * Returns a solid version of this style (removes dash pattern).
     */
    public LineStyle asSolid() {
        return new LineStyle(color, width, null, 0, capStyle, opacity);
    }

    /**
     * Returns a dashed version of this style.
     */
    public LineStyle asDashed() {
        return new LineStyle(color, width, DASH_PATTERN, dashPhase, capStyle, opacity);
    }

    @Override
    public String toString() {
        return String.format("LineStyle[color=%s, width=%.1f, dashed=%s, opacity=%.1f]",
                colorToHex(color), width, isDashed(), opacity);
    }

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LineStyle that = (LineStyle) obj;
        return Float.compare(that.width, width) == 0 &&
               Float.compare(that.dashPhase, dashPhase) == 0 &&
               Float.compare(that.opacity, opacity) == 0 &&
               color.equals(that.color) &&
               Arrays.equals(dashPattern, that.dashPattern) &&
               capStyle == that.capStyle;
    }

    @Override
    public int hashCode() {
        int result = color.hashCode();
        result = 31 * result + Float.hashCode(width);
        result = 31 * result + Arrays.hashCode(dashPattern);
        result = 31 * result + Float.hashCode(dashPhase);
        result = 31 * result + capStyle.hashCode();
        result = 31 * result + Float.hashCode(opacity);
        return result;
    }
}
