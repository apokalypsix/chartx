package com.apokalypsix.chartx.chart.export;

import java.awt.Color;

/**
 * Configuration options for chart export.
 *
 * <p>Use the static factory methods to create builders for each format:
 * <pre>
 * ExportOptions pngOptions = ExportOptions.png()
 *     .scale(2.0f)
 *     .antialiasing(true, 4)
 *     .build();
 *
 * ExportOptions jpegOptions = ExportOptions.jpeg()
 *     .scale(2.0f)
 *     .quality(0.9f)
 *     .background(Color.WHITE)
 *     .build();
 *
 * ExportOptions svgOptions = ExportOptions.svg()
 *     .withTextOverlay(true)
 *     .build();
 * </pre>
 */
public final class ExportOptions {

    private final ExportFormat format;
    private final float scaleFactor;
    private final float jpegQuality;
    private final Color backgroundColor;
    private final boolean includeTextOverlay;
    private final boolean antialiasing;
    private final int msaaSamples;

    private ExportOptions(Builder builder) {
        this.format = builder.format;
        this.scaleFactor = builder.scaleFactor;
        this.jpegQuality = builder.jpegQuality;
        this.backgroundColor = builder.backgroundColor;
        this.includeTextOverlay = builder.includeTextOverlay;
        this.antialiasing = builder.antialiasing;
        this.msaaSamples = builder.msaaSamples;
    }

    // ========== Static Factory Methods ==========

    /**
     * Creates a builder for PNG export options.
     *
     * @return a new builder
     */
    public static Builder png() {
        return new Builder(ExportFormat.PNG);
    }

    /**
     * Creates a builder for JPEG export options.
     *
     * @return a new builder
     */
    public static Builder jpeg() {
        return new Builder(ExportFormat.JPEG);
    }

    /**
     * Creates a builder for SVG export options.
     *
     * @return a new builder
     */
    public static Builder svg() {
        return new Builder(ExportFormat.SVG);
    }

    // ========== Getters ==========

    public ExportFormat getFormat() {
        return format;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public float getJpegQuality() {
        return jpegQuality;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public boolean isIncludeTextOverlay() {
        return includeTextOverlay;
    }

    public boolean isAntialiasing() {
        return antialiasing;
    }

    public int getMsaaSamples() {
        return msaaSamples;
    }

    /**
     * Returns the export width based on the chart width and scale factor.
     *
     * @param chartWidth the chart's current width
     * @return the export width in pixels
     */
    public int getExportWidth(int chartWidth) {
        return (int) (chartWidth * scaleFactor);
    }

    /**
     * Returns the export height based on the chart height and scale factor.
     *
     * @param chartHeight the chart's current height
     * @return the export height in pixels
     */
    public int getExportHeight(int chartHeight) {
        return (int) (chartHeight * scaleFactor);
    }

    // ========== Builder ==========

    /**
     * Builder for ExportOptions.
     */
    public static class Builder {
        private ExportFormat format;
        private float scaleFactor = 1.0f;
        private float jpegQuality = 0.9f;
        private Color backgroundColor = null;
        private boolean includeTextOverlay = true;
        private boolean antialiasing = true;
        private int msaaSamples = 4;

        private Builder(ExportFormat format) {
            this.format = format;
        }

        /**
         * Sets the scale factor for HiDPI export.
         * <ul>
         * <li>1.0 = standard resolution (1x)</li>
         * <li>2.0 = Retina/HiDPI (2x)</li>
         * <li>3.0 = high resolution print (3x)</li>
         * </ul>
         *
         * @param factor the scale factor
         * @return this builder
         */
        public Builder scale(float factor) {
            this.scaleFactor = Math.max(0.1f, Math.min(10.0f, factor));
            return this;
        }

        /**
         * Sets the JPEG compression quality.
         * Only applicable for JPEG format.
         *
         * @param quality quality from 0.0 (lowest) to 1.0 (highest)
         * @return this builder
         */
        public Builder quality(float quality) {
            this.jpegQuality = Math.max(0.0f, Math.min(1.0f, quality));
            return this;
        }

        /**
         * Sets the background color.
         * Required for JPEG (defaults to black if not set).
         * Optional for PNG (transparent if not set).
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.backgroundColor = color;
            return this;
        }

        /**
         * Sets whether to include the text overlay (axis labels, legend, etc.)
         * in the exported image.
         *
         * @param include true to include text overlay
         * @return this builder
         */
        public Builder withTextOverlay(boolean include) {
            this.includeTextOverlay = include;
            return this;
        }

        /**
         * Sets antialiasing options.
         *
         * @param enabled true to enable antialiasing
         * @param samples MSAA sample count (2, 4, or 8)
         * @return this builder
         */
        public Builder antialiasing(boolean enabled, int samples) {
            this.antialiasing = enabled;
            this.msaaSamples = Math.max(1, Math.min(16, samples));
            return this;
        }

        /**
         * Builds the export options.
         *
         * @return the configured ExportOptions
         */
        public ExportOptions build() {
            // JPEG requires opaque background
            if (format == ExportFormat.JPEG && backgroundColor == null) {
                backgroundColor = Color.BLACK;
            }
            return new ExportOptions(this);
        }
    }
}
