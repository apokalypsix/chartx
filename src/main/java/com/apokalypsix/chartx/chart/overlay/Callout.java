package com.apokalypsix.chartx.chart.overlay;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A callout annotation with text and a pointer to a chart location.
 *
 * <p>Callouts consist of a text box and a line pointing to a specific
 * price/time location on the chart.
 */
public class Callout extends Drawing {

    private AnchorPoint target;   // The point being annotated
    private AnchorPoint textBox;  // The text box location

    private String text = "";
    private Color backgroundColor = new Color(50, 50, 50, 200);
    private Color textColor = Color.WHITE;
    private Color borderColor = new Color(100, 100, 100);
    private int padding = 8;
    private int fontSize = 12;

    public Callout(String id, long targetTimestamp, double targetPrice) {
        super(id);
        this.target = new AnchorPoint(targetTimestamp, targetPrice);
    }

    public Callout(String id, long targetTimestamp, double targetPrice,
                   long textBoxTimestamp, double textBoxPrice, String text) {
        super(id);
        this.target = new AnchorPoint(targetTimestamp, targetPrice);
        this.textBox = new AnchorPoint(textBoxTimestamp, textBoxPrice);
        this.text = text;
    }

    @Override
    public Type getType() {
        return Type.CALLOUT;
    }

    @Override
    public List<AnchorPoint> getAnchorPoints() {
        List<AnchorPoint> points = new ArrayList<>(2);
        if (target != null) points.add(target);
        if (textBox != null) points.add(textBox);
        return points;
    }

    @Override
    public boolean isComplete() {
        return target != null && textBox != null;
    }

    @Override
    public void setAnchorPoint(int index, AnchorPoint point) {
        switch (index) {
            case 0 -> target = point;
            case 1 -> textBox = point;
            default -> throw new IndexOutOfBoundsException("Callout has only 2 anchor points");
        }
    }

    @Override
    public int getRequiredAnchorCount() {
        return 2;
    }

    @Override
    public boolean containsPoint(int screenX, int screenY, CoordinateSystem coords, int hitDistance) {
        if (!isComplete()) return false;

        double x1 = coords.xValueToScreenX(target.timestamp());
        double y1 = coords.yValueToScreenY(target.price());
        double x2 = coords.xValueToScreenX(textBox.timestamp());
        double y2 = coords.yValueToScreenY(textBox.price());

        // Check near target point
        if (isNearPoint(screenX, screenY, x1, y1, hitDistance)) return true;

        // Check near text box area (simplified rectangle check)
        int boxWidth = Math.max(80, text.length() * 8);
        int boxHeight = 24 + padding * 2;
        if (screenX >= x2 - boxWidth / 2 && screenX <= x2 + boxWidth / 2 &&
            screenY >= y2 - boxHeight / 2 && screenY <= y2 + boxHeight / 2) {
            return true;
        }

        // Check near connector line
        if (distanceToLineSegment(screenX, screenY, x1, y1, x2, y2) <= hitDistance) return true;

        return false;
    }

    @Override
    public HandleType getHandleAt(int screenX, int screenY, CoordinateSystem coords, int handleRadius) {
        if (!isComplete()) return HandleType.NONE;

        double x1 = coords.xValueToScreenX(target.timestamp());
        double y1 = coords.yValueToScreenY(target.price());
        double x2 = coords.xValueToScreenX(textBox.timestamp());
        double y2 = coords.yValueToScreenY(textBox.price());

        if (isNearPoint(screenX, screenY, x1, y1, handleRadius)) return HandleType.ANCHOR_0;
        if (isNearPoint(screenX, screenY, x2, y2, handleRadius)) return HandleType.ANCHOR_1;

        if (containsPoint(screenX, screenY, coords, handleRadius)) return HandleType.BODY;
        return HandleType.NONE;
    }

    private boolean isNearPoint(double px, double py, double x, double y, int radius) {
        return (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius;
    }

    public AnchorPoint getTarget() { return target; }
    public AnchorPoint getTextBox() { return textBox; }
    public void setTextBox(AnchorPoint textBox) { this.textBox = textBox; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

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

    public void move(long deltaTime, double deltaPrice) {
        if (target != null) target = new AnchorPoint(target.timestamp() + deltaTime, target.price() + deltaPrice);
        if (textBox != null) textBox = new AnchorPoint(textBox.timestamp() + deltaTime, textBox.price() + deltaPrice);
    }
}
