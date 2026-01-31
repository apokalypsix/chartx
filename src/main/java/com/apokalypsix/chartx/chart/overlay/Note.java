package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.List;

/**
 * An expandable note annotation for adding detailed comments to the chart.
 *
 * <p>Notes display a small icon/marker that can be expanded to show
 * full text content. Useful for adding analysis notes, reminders, or
 * detailed comments without cluttering the chart.
 */
public class Note extends Drawing {

    private AnchorPoint anchor;

    private String title = "";
    private String content = "";
    private boolean expanded = false;

    private Color backgroundColor = new Color(50, 50, 50, 220);
    private Color textColor = Color.WHITE;
    private Color titleColor = new Color(255, 193, 7);
    private Color borderColor = new Color(100, 100, 100);
    private Color markerColor = new Color(255, 193, 7);

    private int maxWidth = 250;  // Maximum width when expanded
    private int padding = 8;
    private int fontSize = 11;
    private int markerSize = 16;

    public Note(String id, long timestamp, double price) {
        super(id);
        this.anchor = new AnchorPoint(timestamp, price);
    }

    public Note(String id, long timestamp, double price, String title, String content) {
        super(id);
        this.anchor = new AnchorPoint(timestamp, price);
        this.title = title;
        this.content = content;
    }

    @Override
    public Type getType() {
        return Type.NOTE;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        return anchor != null ? List.of(anchor) : List.of();
    }

    @Override
    public boolean isComplete() {
        return anchor != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Note has only 1 anchor point, index: " + index);
        }
        this.anchor = point;
    }

    @Override
    public int getRequiredAnchorCount() {
        return 1;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x = coords.xValueToScreenX(anchor.timestamp());
        double y = coords.yValueToScreenY(anchor.price());

        if (!expanded) {
            // Check marker area
            double halfSize = markerSize / 2.0;
            return screenX >= x - halfSize && screenX <= x + halfSize &&
                   screenY >= y - halfSize && screenY <= y + halfSize;
        } else {
            // Check expanded note area
            int noteHeight = estimateExpandedHeight();
            return screenX >= x && screenX <= x + maxWidth &&
                   screenY >= y && screenY <= y + noteHeight;
        }
    }

    private int estimateExpandedHeight() {
        int lines = 1;  // Title
        if (content != null && !content.isEmpty()) {
            lines += Math.max(1, content.length() / 35);  // Rough estimate
        }
        return padding * 2 + lines * (fontSize + 4);
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x = coords.xValueToScreenX(anchor.timestamp());
        double y = coords.yValueToScreenY(anchor.price());

        if (isNearPoint(screenX, screenY, x, y, handleRadius)) return HandleType.ANCHOR_0;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getAnchor() { return anchor; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public void toggleExpanded() { this.expanded = !this.expanded; }

    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color backgroundColor) { this.backgroundColor = backgroundColor; }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color textColor) { this.textColor = textColor; }

    public Color getTitleColor() { return titleColor; }
    public void setTitleColor(Color titleColor) { this.titleColor = titleColor; }

    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color borderColor) { this.borderColor = borderColor; }

    public Color getMarkerColor() { return markerColor; }
    public void setMarkerColor(Color markerColor) { this.markerColor = markerColor; }

    public int getMaxWidth() { return maxWidth; }
    public void setMaxWidth(int maxWidth) { this.maxWidth = maxWidth; }

    public int getPadding() { return padding; }
    public void setPadding(int padding) { this.padding = padding; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public int getMarkerSize() { return markerSize; }
    public void setMarkerSize(int markerSize) { this.markerSize = markerSize; }

    public void move(long deltaTime, double deltaPrice) {
        if (anchor != null) {
            anchor = new AnchorPoint(anchor.timestamp() + deltaTime, anchor.price() + deltaPrice);
        }
    }
}
