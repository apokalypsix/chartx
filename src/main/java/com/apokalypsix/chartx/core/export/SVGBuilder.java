package com.apokalypsix.chartx.core.export;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds SVG XML output for vector export.
 *
 * <p>Provides methods to add basic SVG elements like rectangles, lines,
 * paths, text, and circles. Supports grouping elements and applying styles.
 */
public class SVGBuilder {

    private final int width;
    private final int height;
    private final StringBuilder content = new StringBuilder();
    private final List<String> groupStack = new ArrayList<>();

    /**
     * Creates a new SVG builder with the given dimensions.
     *
     * @param width SVG width in pixels
     * @param height SVG height in pixels
     */
    public SVGBuilder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Starts a new group element with optional ID and style.
     *
     * @param id group ID (can be null)
     * @param style CSS style string (can be null)
     */
    public void startGroup(String id, String style) {
        content.append("<g");
        if (id != null && !id.isEmpty()) {
            content.append(" id=\"").append(escapeXml(id)).append("\"");
        }
        if (style != null && !style.isEmpty()) {
            content.append(" style=\"").append(escapeXml(style)).append("\"");
        }
        content.append(">\n");
        groupStack.add(id);
    }

    /**
     * Ends the current group element.
     */
    public void endGroup() {
        if (!groupStack.isEmpty()) {
            groupStack.remove(groupStack.size() - 1);
            content.append("</g>\n");
        }
    }

    /**
     * Adds a rectangle element.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @param fill fill color (can be null for no fill)
     * @param stroke stroke color (can be null for no stroke)
     */
    public void addRect(float x, float y, float width, float height,
                        Color fill, Color stroke) {
        content.append("<rect x=\"").append(format(x))
                .append("\" y=\"").append(format(y))
                .append("\" width=\"").append(format(width))
                .append("\" height=\"").append(format(height)).append("\"");

        if (fill != null) {
            content.append(" fill=\"").append(colorToSvg(fill)).append("\"");
        } else {
            content.append(" fill=\"none\"");
        }

        if (stroke != null) {
            content.append(" stroke=\"").append(colorToSvg(stroke)).append("\"");
        }

        content.append("/>\n");
    }

    /**
     * Adds a rectangle element with rounded corners.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @param rx corner radius X
     * @param ry corner radius Y
     * @param fill fill color (can be null)
     * @param stroke stroke color (can be null)
     */
    public void addRoundedRect(float x, float y, float width, float height,
                               float rx, float ry, Color fill, Color stroke) {
        content.append("<rect x=\"").append(format(x))
                .append("\" y=\"").append(format(y))
                .append("\" width=\"").append(format(width))
                .append("\" height=\"").append(format(height))
                .append("\" rx=\"").append(format(rx))
                .append("\" ry=\"").append(format(ry)).append("\"");

        if (fill != null) {
            content.append(" fill=\"").append(colorToSvg(fill)).append("\"");
        } else {
            content.append(" fill=\"none\"");
        }

        if (stroke != null) {
            content.append(" stroke=\"").append(colorToSvg(stroke)).append("\"");
        }

        content.append("/>\n");
    }

    /**
     * Adds a line element using the group's style.
     *
     * @param x1 start X
     * @param y1 start Y
     * @param x2 end X
     * @param y2 end Y
     */
    public void addLine(float x1, float y1, float x2, float y2) {
        content.append("<line x1=\"").append(format(x1))
                .append("\" y1=\"").append(format(y1))
                .append("\" x2=\"").append(format(x2))
                .append("\" y2=\"").append(format(y2))
                .append("\"/>\n");
    }

    /**
     * Adds a line element with inline style.
     *
     * @param x1 start X
     * @param y1 start Y
     * @param x2 end X
     * @param y2 end Y
     * @param style CSS style string
     */
    public void addLine(float x1, float y1, float x2, float y2, String style) {
        content.append("<line x1=\"").append(format(x1))
                .append("\" y1=\"").append(format(y1))
                .append("\" x2=\"").append(format(x2))
                .append("\" y2=\"").append(format(y2))
                .append("\" style=\"").append(escapeXml(style))
                .append("\"/>\n");
    }

    /**
     * Adds a line element with stroke color and width.
     *
     * @param x1 start X
     * @param y1 start Y
     * @param x2 end X
     * @param y2 end Y
     * @param stroke stroke color
     * @param strokeWidth stroke width
     */
    public void addLine(float x1, float y1, float x2, float y2,
                        Color stroke, float strokeWidth) {
        content.append("<line x1=\"").append(format(x1))
                .append("\" y1=\"").append(format(y1))
                .append("\" x2=\"").append(format(x2))
                .append("\" y2=\"").append(format(y2))
                .append("\" stroke=\"").append(colorToSvg(stroke))
                .append("\" stroke-width=\"").append(format(strokeWidth))
                .append("\"/>\n");
    }

    /**
     * Adds a path element.
     *
     * @param pathData SVG path data (d attribute)
     * @param style CSS style string (can be null)
     */
    public void addPath(String pathData, String style) {
        content.append("<path d=\"").append(escapeXml(pathData)).append("\"");
        if (style != null && !style.isEmpty()) {
            content.append(" style=\"").append(escapeXml(style)).append("\"");
        }
        content.append("/>\n");
    }

    /**
     * Adds a path element with fill and stroke.
     *
     * @param pathData SVG path data
     * @param fill fill color (can be null)
     * @param stroke stroke color (can be null)
     * @param strokeWidth stroke width
     */
    public void addPath(String pathData, Color fill, Color stroke, float strokeWidth) {
        content.append("<path d=\"").append(escapeXml(pathData)).append("\"");

        if (fill != null) {
            content.append(" fill=\"").append(colorToSvg(fill)).append("\"");
        } else {
            content.append(" fill=\"none\"");
        }

        if (stroke != null) {
            content.append(" stroke=\"").append(colorToSvg(stroke))
                    .append("\" stroke-width=\"").append(format(strokeWidth)).append("\"");
        }

        content.append("/>\n");
    }

    /**
     * Adds a text element.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param text the text content
     * @param style CSS style string (can be null)
     */
    public void addText(float x, float y, String text, String style) {
        content.append("<text x=\"").append(format(x))
                .append("\" y=\"").append(format(y)).append("\"");
        if (style != null && !style.isEmpty()) {
            content.append(" style=\"").append(escapeXml(style)).append("\"");
        }
        content.append(">").append(escapeXml(text)).append("</text>\n");
    }

    /**
     * Adds a text element with color and font size.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param text the text content
     * @param fill text color
     * @param fontSize font size in pixels
     * @param fontFamily font family name
     */
    public void addText(float x, float y, String text, Color fill,
                        float fontSize, String fontFamily) {
        content.append("<text x=\"").append(format(x))
                .append("\" y=\"").append(format(y))
                .append("\" fill=\"").append(colorToSvg(fill))
                .append("\" font-size=\"").append(format(fontSize)).append("px\"");

        if (fontFamily != null && !fontFamily.isEmpty()) {
            content.append(" font-family=\"").append(escapeXml(fontFamily)).append("\"");
        }

        content.append(">").append(escapeXml(text)).append("</text>\n");
    }

    /**
     * Adds a circle element.
     *
     * @param cx center X
     * @param cy center Y
     * @param r radius
     * @param fill fill color (can be null)
     * @param stroke stroke color (can be null)
     */
    public void addCircle(float cx, float cy, float r, Color fill, Color stroke) {
        content.append("<circle cx=\"").append(format(cx))
                .append("\" cy=\"").append(format(cy))
                .append("\" r=\"").append(format(r)).append("\"");

        if (fill != null) {
            content.append(" fill=\"").append(colorToSvg(fill)).append("\"");
        } else {
            content.append(" fill=\"none\"");
        }

        if (stroke != null) {
            content.append(" stroke=\"").append(colorToSvg(stroke)).append("\"");
        }

        content.append("/>\n");
    }

    /**
     * Adds an ellipse element.
     *
     * @param cx center X
     * @param cy center Y
     * @param rx radius X
     * @param ry radius Y
     * @param fill fill color (can be null)
     * @param stroke stroke color (can be null)
     */
    public void addEllipse(float cx, float cy, float rx, float ry,
                           Color fill, Color stroke) {
        content.append("<ellipse cx=\"").append(format(cx))
                .append("\" cy=\"").append(format(cy))
                .append("\" rx=\"").append(format(rx))
                .append("\" ry=\"").append(format(ry)).append("\"");

        if (fill != null) {
            content.append(" fill=\"").append(colorToSvg(fill)).append("\"");
        } else {
            content.append(" fill=\"none\"");
        }

        if (stroke != null) {
            content.append(" stroke=\"").append(colorToSvg(stroke)).append("\"");
        }

        content.append("/>\n");
    }

    /**
     * Adds a polyline element (open path).
     *
     * @param points array of [x1,y1,x2,y2,...] coordinates
     * @param stroke stroke color
     * @param strokeWidth stroke width
     */
    public void addPolyline(float[] points, Color stroke, float strokeWidth) {
        if (points == null || points.length < 4) return;

        content.append("<polyline points=\"");
        for (int i = 0; i < points.length; i += 2) {
            if (i > 0) content.append(" ");
            content.append(format(points[i])).append(",").append(format(points[i + 1]));
        }
        content.append("\" fill=\"none\" stroke=\"").append(colorToSvg(stroke))
                .append("\" stroke-width=\"").append(format(strokeWidth))
                .append("\"/>\n");
    }

    /**
     * Adds a polygon element (closed filled path).
     *
     * @param points array of [x1,y1,x2,y2,...] coordinates
     * @param fill fill color
     * @param stroke stroke color (can be null)
     */
    public void addPolygon(float[] points, Color fill, Color stroke) {
        if (points == null || points.length < 6) return;

        content.append("<polygon points=\"");
        for (int i = 0; i < points.length; i += 2) {
            if (i > 0) content.append(" ");
            content.append(format(points[i])).append(",").append(format(points[i + 1]));
        }
        content.append("\"");

        if (fill != null) {
            content.append(" fill=\"").append(colorToSvg(fill)).append("\"");
        } else {
            content.append(" fill=\"none\"");
        }

        if (stroke != null) {
            content.append(" stroke=\"").append(colorToSvg(stroke)).append("\"");
        }

        content.append("/>\n");
    }

    /**
     * Builds and returns the complete SVG document.
     *
     * @return the SVG XML string
     */
    public String build() {
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
        svg.append("width=\"").append(width).append("\" ");
        svg.append("height=\"").append(height).append("\" ");
        svg.append("viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");
        svg.append(content);
        svg.append("</svg>");
        return svg.toString();
    }

    // ========== Utility Methods ==========

    /**
     * Converts a Java Color to SVG color string.
     */
    private String colorToSvg(Color c) {
        if (c.getAlpha() == 255) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        } else {
            return String.format("rgba(%d,%d,%d,%.2f)",
                    c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
        }
    }

    /**
     * Escapes special XML characters.
     */
    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Formats a float value for SVG output.
     */
    private String format(float value) {
        // Remove trailing zeros for cleaner output
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }
}
