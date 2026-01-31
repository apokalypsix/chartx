package com.apokalypsix.chartx.core.ui.sidebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A flyout menu that appears to the right of the sidebar when a menu item is clicked.
 *
 * <p>Shows a list of tool options in a popup-style panel.
 */
public class ToolFlyoutMenu extends JPanel {

    private final List<FlyoutMenuItem> items = new ArrayList<>();
    private FlyoutMenuItem hoveredItem = null;
    private Consumer<String> onItemSelected;

    // Styling
    private Color backgroundColor = new Color(35, 37, 41);
    private Color borderColor = new Color(60, 62, 66);
    private Color hoverColor = new Color(50, 52, 56);
    private Color selectedColor = new Color(42, 130, 255);
    private Color textColor = new Color(200, 202, 206);
    private Color textSelectedColor = Color.WHITE;
    private Font itemFont = new Font("SansSerif", Font.PLAIN, 12);
    private int itemHeight = 28;
    private int padding = 8;
    private int cornerRadius = 6;

    public ToolFlyoutMenu() {
        setOpaque(false);
        setLayout(null);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoveredItem(e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredItem = null;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (hoveredItem != null && onItemSelected != null) {
                    onItemSelected.accept(hoveredItem.id);
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    /**
     * Sets the items to display in the flyout.
     */
    public void setItems(List<FlyoutMenuItem> items) {
        this.items.clear();
        this.items.addAll(items);
        updateSize();
        repaint();
    }

    /**
     * Clears all items.
     */
    public void clearItems() {
        this.items.clear();
        updateSize();
        repaint();
    }

    /**
     * Sets the callback for when an item is selected.
     */
    public void setOnItemSelected(Consumer<String> callback) {
        this.onItemSelected = callback;
    }

    /**
     * Applies styling from a SidebarConfig.
     */
    public void applyConfig(SidebarConfig config) {
        this.backgroundColor = darker(config.getBackgroundColor(), 0.9f);
        this.borderColor = config.getBorderColor();
        this.hoverColor = config.getButtonHoverColor();
        this.selectedColor = config.getButtonSelectedColor();
        this.textColor = config.getTextColor();
        this.textSelectedColor = config.getTextSelectedColor();
        this.itemFont = config.getButtonFont().deriveFont(Font.PLAIN, 12f);
        repaint();
    }

    private Color darker(Color c, float factor) {
        return new Color(
                Math.max(0, (int) (c.getRed() * factor)),
                Math.max(0, (int) (c.getGreen() * factor)),
                Math.max(0, (int) (c.getBlue() * factor)));
    }

    private void updateSize() {
        int width = calculateWidth();
        int height = padding * 2 + items.size() * itemHeight;
        setPreferredSize(new Dimension(width, height));
        setSize(width, height);
    }

    private int calculateWidth() {
        FontMetrics fm = getFontMetrics(itemFont);
        int maxWidth = 100;
        for (FlyoutMenuItem item : items) {
            int width = fm.stringWidth(item.label) + padding * 4;
            if (item.shortLabel != null) {
                width += fm.stringWidth(item.shortLabel) + 20;
            }
            maxWidth = Math.max(maxWidth, width);
        }
        return maxWidth;
    }

    private void updateHoveredItem(int y) {
        int index = (y - padding) / itemHeight;
        if (index >= 0 && index < items.size()) {
            FlyoutMenuItem newHovered = items.get(index);
            if (newHovered != hoveredItem) {
                hoveredItem = newHovered;
                repaint();
            }
        } else if (hoveredItem != null) {
            hoveredItem = null;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Draw background
        g2.setColor(backgroundColor);
        RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius);
        g2.fill(bg);

        // Draw border
        g2.setColor(borderColor);
        g2.draw(bg);

        // Draw items
        g2.setFont(itemFont);
        FontMetrics fm = g2.getFontMetrics();

        int y = padding;
        for (FlyoutMenuItem item : items) {
            // Highlight hovered item
            if (item == hoveredItem) {
                g2.setColor(hoverColor);
                g2.fillRect(2, y, w - 4, itemHeight);
            }

            // Draw short label on left
            if (item.shortLabel != null) {
                g2.setColor(item == hoveredItem ? selectedColor : new Color(100, 102, 106));
                g2.drawString(item.shortLabel, padding, y + (itemHeight + fm.getAscent()) / 2 - 2);
            }

            // Draw main label
            g2.setColor(item == hoveredItem ? textSelectedColor : textColor);
            int labelX = item.shortLabel != null ? padding + 40 : padding;
            g2.drawString(item.label, labelX, y + (itemHeight + fm.getAscent()) / 2 - 2);

            y += itemHeight;
        }

        g2.dispose();
    }

    /**
     * Represents an item in the flyout menu.
     */
    public static class FlyoutMenuItem {
        public final String id;
        public final String label;
        public final String shortLabel;

        public FlyoutMenuItem(String id, String label) {
            this(id, label, null);
        }

        public FlyoutMenuItem(String id, String label, String shortLabel) {
            this.id = id;
            this.label = label;
            this.shortLabel = shortLabel;
        }
    }
}
