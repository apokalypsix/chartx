package com.apokalypsix.chartx.core.render.util;

import java.awt.Color;

/**
 * Maps value ranges to colors for heatmaps and other visualizations.
 *
 * <p>Provides pre-built color maps (viridis, plasma, jet, grayscale) and
 * support for custom gradient definitions. Colors are interpolated linearly
 * between gradient stops.
 *
 * <p>For performance, use the float array methods to avoid Color object allocation.
 */
public class ColorMap {

    /** Color stops for the gradient */
    private final float[][] stops; // Each stop: [position, r, g, b, a]

    /** Number of stops */
    private final int stopCount;

    /** Value range */
    private float minValue;
    private float maxValue;

    /** Pre-computed lookup table for fast color lookup */
    private int[] lut;
    private int lutSize;

    // ========== Pre-built Color Maps ==========

    /**
     * Creates a viridis color map (perceptually uniform, colorblind-friendly).
     */
    public static ColorMap viridis() {
        return new ColorMap(new float[][]{
                {0.0f, 0.267f, 0.004f, 0.329f, 1.0f},  // Dark purple
                {0.25f, 0.282f, 0.140f, 0.458f, 1.0f}, // Purple
                {0.5f, 0.127f, 0.566f, 0.551f, 1.0f},  // Teal
                {0.75f, 0.369f, 0.789f, 0.383f, 1.0f}, // Green
                {1.0f, 0.993f, 0.906f, 0.144f, 1.0f}   // Yellow
        });
    }

    /**
     * Creates a plasma color map (perceptually uniform).
     */
    public static ColorMap plasma() {
        return new ColorMap(new float[][]{
                {0.0f, 0.050f, 0.030f, 0.528f, 1.0f},  // Dark blue
                {0.25f, 0.417f, 0.001f, 0.658f, 1.0f}, // Purple
                {0.5f, 0.798f, 0.280f, 0.470f, 1.0f},  // Pink
                {0.75f, 0.973f, 0.580f, 0.254f, 1.0f}, // Orange
                {1.0f, 0.940f, 0.975f, 0.131f, 1.0f}   // Yellow
        });
    }

    /**
     * Creates a jet color map (classic rainbow, not colorblind-friendly).
     */
    public static ColorMap jet() {
        return new ColorMap(new float[][]{
                {0.0f, 0.0f, 0.0f, 0.5f, 1.0f},    // Dark blue
                {0.11f, 0.0f, 0.0f, 1.0f, 1.0f},   // Blue
                {0.34f, 0.0f, 1.0f, 1.0f, 1.0f},   // Cyan
                {0.5f, 0.0f, 1.0f, 0.0f, 1.0f},    // Green
                {0.66f, 1.0f, 1.0f, 0.0f, 1.0f},   // Yellow
                {0.89f, 1.0f, 0.0f, 0.0f, 1.0f},   // Red
                {1.0f, 0.5f, 0.0f, 0.0f, 1.0f}     // Dark red
        });
    }

    /**
     * Creates a grayscale color map.
     */
    public static ColorMap grayscale() {
        return new ColorMap(new float[][]{
                {0.0f, 0.0f, 0.0f, 0.0f, 1.0f},    // Black
                {1.0f, 1.0f, 1.0f, 1.0f, 1.0f}     // White
        });
    }

    /**
     * Creates a thermal/heat color map (black-red-yellow-white).
     */
    public static ColorMap thermal() {
        return new ColorMap(new float[][]{
                {0.0f, 0.0f, 0.0f, 0.0f, 1.0f},    // Black
                {0.33f, 0.8f, 0.0f, 0.0f, 1.0f},   // Red
                {0.66f, 1.0f, 0.8f, 0.0f, 1.0f},   // Yellow
                {1.0f, 1.0f, 1.0f, 1.0f, 1.0f}     // White
        });
    }

    /**
     * Creates a cool-to-warm diverging color map.
     */
    public static ColorMap coolWarm() {
        return new ColorMap(new float[][]{
                {0.0f, 0.227f, 0.298f, 0.753f, 1.0f},  // Blue
                {0.5f, 0.865f, 0.865f, 0.865f, 1.0f},  // White/gray
                {1.0f, 0.706f, 0.016f, 0.150f, 1.0f}   // Red
        });
    }

    /**
     * Creates a two-color gradient.
     */
    public static ColorMap gradient(Color from, Color to) {
        return new ColorMap(new float[][]{
                {0.0f, from.getRed() / 255f, from.getGreen() / 255f, from.getBlue() / 255f, from.getAlpha() / 255f},
                {1.0f, to.getRed() / 255f, to.getGreen() / 255f, to.getBlue() / 255f, to.getAlpha() / 255f}
        });
    }

    /**
     * Creates a three-color gradient (for diverging data).
     */
    public static ColorMap gradient(Color low, Color mid, Color high) {
        return new ColorMap(new float[][]{
                {0.0f, low.getRed() / 255f, low.getGreen() / 255f, low.getBlue() / 255f, low.getAlpha() / 255f},
                {0.5f, mid.getRed() / 255f, mid.getGreen() / 255f, mid.getBlue() / 255f, mid.getAlpha() / 255f},
                {1.0f, high.getRed() / 255f, high.getGreen() / 255f, high.getBlue() / 255f, high.getAlpha() / 255f}
        });
    }

    // ========== Constructors ==========

    /**
     * Creates a color map from gradient stops.
     *
     * @param stops array of [position, r, g, b, a] where position is 0-1 and colors are 0-1
     */
    public ColorMap(float[][] stops) {
        this.stops = stops;
        this.stopCount = stops.length;
        this.minValue = 0;
        this.maxValue = 1;
        this.lutSize = 0; // No LUT by default
    }

    /**
     * Creates a color map with value range.
     */
    public ColorMap(float[][] stops, float minValue, float maxValue) {
        this(stops);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    // ========== Configuration ==========

    /**
     * Sets the value range for mapping.
     */
    public ColorMap valueRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    /**
     * Pre-computes a lookup table for faster color access.
     *
     * @param size number of entries in the LUT
     */
    public ColorMap buildLUT(int size) {
        this.lutSize = size;
        this.lut = new int[size];

        for (int i = 0; i < size; i++) {
            float t = (float) i / (size - 1);
            float[] rgba = new float[4];
            interpolate(t, rgba);
            lut[i] = ((int) (rgba[3] * 255) << 24) |
                    ((int) (rgba[0] * 255) << 16) |
                    ((int) (rgba[1] * 255) << 8) |
                    ((int) (rgba[2] * 255));
        }

        return this;
    }

    // ========== Color Mapping ==========

    /**
     * Maps a value to a Color object.
     *
     * @param value the value to map
     * @return interpolated Color
     */
    public Color getColor(float value) {
        float[] rgba = new float[4];
        getColor(value, rgba);
        return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    /**
     * Maps a value to RGBA components (0-1 range).
     *
     * @param value the value to map
     * @param out output array [r, g, b, a]
     */
    public void getColor(float value, float[] out) {
        float t = normalize(value);
        interpolate(t, out);
    }

    /**
     * Maps a value to an ARGB int (for BufferedImage).
     *
     * @param value the value to map
     * @return ARGB color value
     */
    public int getARGB(float value) {
        float t = normalize(value);

        // Use LUT if available
        if (lutSize > 0) {
            int index = Math.max(0, Math.min(lutSize - 1, (int) (t * (lutSize - 1))));
            return lut[index];
        }

        float[] rgba = new float[4];
        interpolate(t, rgba);
        return ((int) (rgba[3] * 255) << 24) |
                ((int) (rgba[0] * 255) << 16) |
                ((int) (rgba[1] * 255) << 8) |
                ((int) (rgba[2] * 255));
    }

    /**
     * Batch converts values to RGBA floats for vertex buffers.
     *
     * @param values input values
     * @param offset start offset in values
     * @param count number of values to convert
     * @param outR output red components
     * @param outG output green components
     * @param outB output blue components
     * @param outA output alpha components
     */
    public void getColors(float[] values, int offset, int count,
                          float[] outR, float[] outG, float[] outB, float[] outA) {
        float[] rgba = new float[4];
        for (int i = 0; i < count; i++) {
            float t = normalize(values[offset + i]);
            interpolate(t, rgba);
            outR[i] = rgba[0];
            outG[i] = rgba[1];
            outB[i] = rgba[2];
            outA[i] = rgba[3];
        }
    }

    // ========== Internal ==========

    private float normalize(float value) {
        if (Float.isNaN(value)) {
            return 0;
        }
        float range = maxValue - minValue;
        if (range <= 0) {
            return 0.5f;
        }
        return Math.max(0, Math.min(1, (value - minValue) / range));
    }

    private void interpolate(float t, float[] out) {
        // Handle edge cases
        if (t <= stops[0][0]) {
            out[0] = stops[0][1];
            out[1] = stops[0][2];
            out[2] = stops[0][3];
            out[3] = stops[0][4];
            return;
        }
        if (t >= stops[stopCount - 1][0]) {
            out[0] = stops[stopCount - 1][1];
            out[1] = stops[stopCount - 1][2];
            out[2] = stops[stopCount - 1][3];
            out[3] = stops[stopCount - 1][4];
            return;
        }

        // Find the two stops to interpolate between
        for (int i = 0; i < stopCount - 1; i++) {
            if (t >= stops[i][0] && t <= stops[i + 1][0]) {
                float localT = (t - stops[i][0]) / (stops[i + 1][0] - stops[i][0]);
                out[0] = lerp(stops[i][1], stops[i + 1][1], localT);
                out[1] = lerp(stops[i][2], stops[i + 1][2], localT);
                out[2] = lerp(stops[i][3], stops[i + 1][3], localT);
                out[3] = lerp(stops[i][4], stops[i + 1][4], localT);
                return;
            }
        }

        // Fallback (shouldn't happen)
        out[0] = out[1] = out[2] = 0.5f;
        out[3] = 1.0f;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // ========== Accessors ==========

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public int getStopCount() {
        return stopCount;
    }
}
