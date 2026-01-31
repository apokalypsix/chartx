package com.apokalypsix.chartx.core.ui.sidebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A menu button in the sidebar that shows a flyout menu when clicked.
 *
 * <p>Displays a short label and expands to show sub-items in a flyout menu.
 */
public class SidebarMenuButton extends JComponent {

    private final String id;
    private final String label;
    private final String tooltip;
    private final List<ToolFlyoutMenu.FlyoutMenuItem> subItems = new ArrayList<>();

    private boolean hovered;
    private boolean expanded;
    private String selectedSubItemId;

    // Callback
    private Runnable onExpand;

    // Styling
    private Color backgroundColor;
    private Color hoverColor;
    private Color expandedColor;
    private Color textColor;
    private Color textExpandedColor;
    private Color indicatorColor;
    private Font font;
    private int cornerRadius = 4;

    /**
     * Creates a sidebar menu button.
     *
     * @param id      unique identifier
     * @param label   short text label (e.g., "Lines", "Shapes")
     * @param tooltip full description for tooltip
     */
    public SidebarMenuButton(String id, String label, String tooltip) {
        this.id = id;
        this.label = label;
        this.tooltip = tooltip;

        // Default styling
        this.backgroundColor = new Color(40, 42, 46);
        this.hoverColor = new Color(55, 57, 61);
        this.expandedColor = new Color(50, 52, 56);
        this.textColor = new Color(180, 182, 186);
        this.textExpandedColor = new Color(220, 222, 226);
        this.indicatorColor = new Color(100, 102, 106);
        this.font = new Font("SansSerif", Font.PLAIN, 11);

        setToolTipText(tooltip);
        setPreferredSize(new Dimension(48, 36));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onExpand != null) {
                    onExpand.run();
                }
            }
        });
    }

    /**
     * Returns the button ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the button label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Adds a sub-item to this menu.
     */
    public void addSubItem(String itemId, String itemLabel, String shortLabel) {
        subItems.add(new ToolFlyoutMenu.FlyoutMenuItem(itemId, itemLabel, shortLabel));
    }

    /**
     * Adds a sub-item to this menu.
     */
    public void addSubItem(String itemId, String itemLabel) {
        subItems.add(new ToolFlyoutMenu.FlyoutMenuItem(itemId, itemLabel));
    }

    /**
     * Returns the sub-items.
     */
    public List<ToolFlyoutMenu.FlyoutMenuItem> getSubItems() {
        return subItems;
    }

    /**
     * Returns whether this menu is expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Sets whether this menu is expanded.
     */
    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            this.expanded = expanded;
            repaint();
        }
    }

    /**
     * Returns the selected sub-item ID.
     */
    public String getSelectedSubItemId() {
        return selectedSubItemId;
    }

    /**
     * Sets the selected sub-item ID.
     */
    public void setSelectedSubItemId(String id) {
        this.selectedSubItemId = id;
        repaint();
    }

    /**
     * Sets the expand callback.
     */
    public void setOnExpand(Runnable onExpand) {
        this.onExpand = onExpand;
    }

    /**
     * Applies styling from a SidebarConfig.
     */
    public void applyConfig(SidebarConfig config) {
        this.backgroundColor = config.getButtonColor();
        this.hoverColor = config.getButtonHoverColor();
        this.expandedColor = new Color(50, 52, 56);
        this.textColor = config.getTextColor();
        this.textExpandedColor = config.getTextSelectedColor();
        this.font = config.getButtonFont().deriveFont(Font.PLAIN, 11f);
        this.cornerRadius = config.getCornerRadius();

        int size = config.getButtonSize();
        setPreferredSize(new Dimension(config.getWidth() - 12, size));

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Background
        Color bg;
        if (expanded) {
            bg = expandedColor;
        } else if (hovered) {
            bg = hoverColor;
        } else {
            bg = backgroundColor;
        }

        g2.setColor(bg);
        RoundRectangle2D rect = new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius);
        g2.fill(rect);

        // Text
        g2.setFont(font);
        g2.setColor(expanded || hovered ? textExpandedColor : textColor);

        FontMetrics fm = g2.getFontMetrics();
        int textX = 8;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(label, textX, textY);

        // Arrow indicator (right side)
        g2.setColor(indicatorColor);
        int arrowX = w - 14;
        int arrowY = h / 2;
        int arrowSize = 4;

        int[] xPoints = {arrowX, arrowX, arrowX + arrowSize};
        int[] yPoints = {arrowY - arrowSize, arrowY + arrowSize, arrowY};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
    }
}
