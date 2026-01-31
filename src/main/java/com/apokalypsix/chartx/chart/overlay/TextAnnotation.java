package com.apokalypsix.chartx.chart.overlay;

import java.awt.Color;
import java.awt.Font;

/**
 * Text annotation for displaying labels on the chart.
 *
 * <p>Text annotations can be styled with custom fonts, colors, and backgrounds.
 * They support horizontal and vertical alignment relative to the anchor point.
 *
 * <p>Example usage:
 * <pre>{@code
 * TextAnnotation note = new TextAnnotation("note1", timestamp, price, "Breakout!");
 * note.setColor(Color.YELLOW);
 * note.setBackgroundColor(new Color(0, 0, 0, 180));
 * note.setAlignment(HAlign.CENTER, VAlign.BOTTOM);
 * annotationLayer.addAnnotation(note);
 * }</pre>
 */
public class TextAnnotation extends Annotation {

    private String text;
    private Font font = new Font("SansSerif", Font.PLAIN, 12);

    // Alignment relative to anchor point
    private HAlign hAlign = HAlign.CENTER;
    private VAlign vAlign = VAlign.BOTTOM;

    // Padding inside background box (if backgroundColor is set)
    private int paddingX = 4;
    private int paddingY = 2;

    // Border
    private Color borderColor = null;
    private int borderWidth = 1;

    // Whether to show as a label (with background) or plain text
    private boolean showAsLabel = false;

    /**
     * Creates a text annotation.
     *
     * @param id unique identifier
     * @param timestamp time coordinate
     * @param price price coordinate
     * @param text the text to display
     */
    public TextAnnotation(String id, long timestamp, double price, String text) {
        super(id, timestamp, price);
        this.text = text;
    }

    /**
     * Creates a text annotation with default ID.
     */
    public TextAnnotation(long timestamp, double price, String text) {
        this("text_" + System.nanoTime(), timestamp, price, text);
    }

    @Override
    public AnnotationType getType() {
        return showAsLabel ? AnnotationType.LABEL : AnnotationType.TEXT;
    }

    // ========== Text-specific getters ==========

    public String getText() {
        return text;
    }

    public Font getFont() {
        return font;
    }

    public HAlign getHAlign() {
        return hAlign;
    }

    public VAlign getVAlign() {
        return vAlign;
    }

    public int getPaddingX() {
        return paddingX;
    }

    public int getPaddingY() {
        return paddingY;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public boolean isShowAsLabel() {
        return showAsLabel;
    }

    // ========== Text-specific setters ==========

    public void setText(String text) {
        this.text = text;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setFontSize(int size) {
        this.font = font.deriveFont((float) size);
    }

    public void setFontStyle(int style) {
        this.font = font.deriveFont(style);
    }

    public void setAlignment(HAlign hAlign, VAlign vAlign) {
        this.hAlign = hAlign;
        this.vAlign = vAlign;
    }

    public void setHAlign(HAlign hAlign) {
        this.hAlign = hAlign;
    }

    public void setVAlign(VAlign vAlign) {
        this.vAlign = vAlign;
    }

    public void setPadding(int x, int y) {
        this.paddingX = x;
        this.paddingY = y;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    public void setShowAsLabel(boolean showAsLabel) {
        this.showAsLabel = showAsLabel;
    }

    @Override
    public String toString() {
        return String.format("TextAnnotation[id=%s, text='%s', time=%d, price=%.2f]",
                getId(), text, timestamp, price);
    }

    /**
     * Horizontal alignment options.
     */
    public enum HAlign {
        LEFT,    // Text starts at anchor point
        CENTER,  // Text centered on anchor point
        RIGHT    // Text ends at anchor point
    }

    /**
     * Vertical alignment options.
     */
    public enum VAlign {
        TOP,     // Text below anchor point
        MIDDLE,  // Text vertically centered on anchor point
        BOTTOM   // Text above anchor point
    }
}
