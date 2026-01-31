package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.List;

/**
 * A price label annotation that displays a price value at a specific location.
 *
 * <p>Price labels are used to mark important price levels with customizable
 * text formatting.
 */
public class PriceLabel extends Drawing {

    private AnchorPoint anchor;

    private String label = "";
    private boolean showPrice = true;
    private Color backgroundColor = new Color(50, 50, 50, 200);
    private Color textColor = Color.WHITE;
    private Color borderColor = new Color(100, 100, 100);
    private int padding = 4;
    private int fontSize = 11;

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    private Alignment alignment = Alignment.RIGHT;

    public PriceLabel(String id, long timestamp, double price) {
        super(id);
        this.anchor = new AnchorPoint(timestamp, price);
    }

    public PriceLabel(String id, long timestamp, double price, String label) {
        super(id);
        this.anchor = new AnchorPoint(timestamp, price);
        this.label = label;
    }

    @Override
    public Type getType() {
        return Type.PRICE_LABEL;
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
            throw new IndexOutOfBoundsException("PriceLabel has only 1 anchor point, index: " + index);
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

        // Estimate label bounds
        String displayText = getDisplayText();
        int labelWidth = Math.max(40, displayText.length() * 7 + padding * 2);
        int labelHeight = fontSize + padding * 2;

        double left, right;
        switch (alignment) {
            case LEFT -> {
                left = x;
                right = x + labelWidth;
            }
            case CENTER -> {
                left = x - labelWidth / 2.0;
                right = x + labelWidth / 2.0;
            }
            default -> {
                left = x - labelWidth;
                right = x;
            }
        }

        return screenX >= left - hitDistance && screenX <= right + hitDistance &&
               screenY >= y - labelHeight / 2.0 - hitDistance && screenY <= y + labelHeight / 2.0 + hitDistance;
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

    public String getDisplayText() {
        if (label != null && !label.isEmpty()) {
            if (showPrice) {
                return label + ": " + String.format("%.2f", anchor.price());
            }
            return label;
        }
        if (showPrice) {
            return String.format("%.2f", anchor.price());
        }
        return "";
    }

    public AnchorPoint getAnchor() { return anchor; }

    public double getPrice() { return anchor.price(); }
    public void setPrice(double price) {
        this.anchor = new AnchorPoint(anchor.timestamp(), price);
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isShowPrice() { return showPrice; }
    public void setShowPrice(boolean showPrice) { this.showPrice = showPrice; }

    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color backgroundColor) { this.backgroundColor = backgroundColor; }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color textColor) { this.textColor = textColor; }

    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color borderColor) { this.borderColor = borderColor; }

    public int getPadding() { return padding; }
    public void setPadding(int padding) { this.padding = padding; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public Alignment getAlignment() { return alignment; }
    public void setAlignment(Alignment alignment) { this.alignment = alignment; }

    public void move(long deltaTime, double deltaPrice) {
        if (anchor != null) {
            anchor = new AnchorPoint(anchor.timestamp() + deltaTime, anchor.price() + deltaPrice);
        }
    }
}
