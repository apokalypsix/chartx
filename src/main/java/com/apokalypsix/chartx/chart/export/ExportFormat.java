package com.apokalypsix.chartx.chart.export;

/**
 * Supported image export formats.
 */
public enum ExportFormat {

    /** PNG format - lossless compression with transparency support */
    PNG("png", "image/png", true),

    /** JPEG format - lossy compression, no transparency */
    JPEG("jpg", "image/jpeg", false),

    /** SVG format - vector graphics, infinitely scalable */
    SVG("svg", "image/svg+xml", true);

    private final String extension;
    private final String mimeType;
    private final boolean supportsTransparency;

    ExportFormat(String extension, String mimeType, boolean supportsTransparency) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.supportsTransparency = supportsTransparency;
    }

    /**
     * Gets the file extension for this format.
     *
     * @return the extension without dot (e.g., "png", "jpg", "svg")
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets the MIME type for this format.
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns whether this format supports transparency.
     *
     * @return true if transparency is supported
     */
    public boolean supportsTransparency() {
        return supportsTransparency;
    }

    /**
     * Returns whether this is a raster format (PNG, JPEG).
     *
     * @return true if raster format
     */
    public boolean isRaster() {
        return this == PNG || this == JPEG;
    }

    /**
     * Returns whether this is a vector format (SVG).
     *
     * @return true if vector format
     */
    public boolean isVector() {
        return this == SVG;
    }
}
