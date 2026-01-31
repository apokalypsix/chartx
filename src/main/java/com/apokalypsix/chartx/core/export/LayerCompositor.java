package com.apokalypsix.chartx.core.export;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility for compositing multiple image layers (GL content + text overlay).
 */
public final class LayerCompositor {

    private LayerCompositor() {
        // Utility class
    }

    /**
     * Composites an overlay image on top of a base image using alpha blending.
     *
     * @param base the base image (GL-rendered content)
     * @param overlay the overlay image (text, annotations)
     * @return the composited image
     * @throws IllegalArgumentException if image dimensions don't match
     */
    public static BufferedImage composite(BufferedImage base, BufferedImage overlay) {
        if (base.getWidth() != overlay.getWidth() || base.getHeight() != overlay.getHeight()) {
            throw new IllegalArgumentException(
                    "Image dimensions must match: base=" + base.getWidth() + "x" + base.getHeight() +
                            ", overlay=" + overlay.getWidth() + "x" + overlay.getHeight());
        }

        BufferedImage result = new BufferedImage(
                base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = result.createGraphics();
        try {
            // Draw base layer
            g2.drawImage(base, 0, 0, null);

            // Draw overlay with alpha blending (SrcOver is default)
            g2.setComposite(AlphaComposite.SrcOver);
            g2.drawImage(overlay, 0, 0, null);
        } finally {
            g2.dispose();
        }

        return result;
    }

    /**
     * Applies a solid background color behind transparent pixels.
     * Required for JPEG export (no transparency support).
     *
     * @param image the source image with transparency
     * @param backgroundColor the background color to apply
     * @return new image with solid background
     */
    public static BufferedImage applyBackground(BufferedImage image, Color backgroundColor) {
        BufferedImage result = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = result.createGraphics();
        try {
            // Fill background
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());

            // Draw image on top
            g2.drawImage(image, 0, 0, null);
        } finally {
            g2.dispose();
        }

        return result;
    }

    /**
     * Creates a transparent image for text overlay rendering.
     *
     * @param width image width
     * @param height image height
     * @return a new transparent ARGB image
     */
    public static BufferedImage createTransparentImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Scales an image by the given factor using high-quality interpolation.
     *
     * @param image the source image
     * @param scale the scale factor (e.g., 2.0 for 2x)
     * @return the scaled image
     */
    public static BufferedImage scale(BufferedImage image, float scale) {
        if (scale == 1.0f) {
            return image;
        }

        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2 = scaled.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(image, 0, 0, newWidth, newHeight, null);
        } finally {
            g2.dispose();
        }

        return scaled;
    }
}
