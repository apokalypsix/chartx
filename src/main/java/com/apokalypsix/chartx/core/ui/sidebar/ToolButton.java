package com.apokalypsix.chartx.core.ui.sidebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A tool button for the sidebar with text label.
 *
 * <p>Displays a short text label (e.g., "TL" for trend line, "SMA" for indicator).
 * Supports hover and selected states with configurable colors.
 *
 * <p>Example usage:
 * <pre>{@code
 * ToolButton button = new ToolButton("TL", "Trend Line");
 * button.setOnClick(() -> chart.setDrawingTool(DrawingTool.TREND_LINE));
 * }</pre>
 */
public class ToolButton extends JComponent {

    private final String label;
    private final String tooltip;
    private boolean selected;
    private boolean hovered;
    private Runnable onClick;

    // Styling (can be overridden by SidebarConfig)
    private Color backgroundColor;
    private Color hoverColor;
    private Color selectedColor;
    private Color textColor;
    private Color textSelectedColor;
    private Font font;
    private int cornerRadius = 4;

    /**
     * Creates a tool button with the given label and tooltip.
     *
     * @param label   short text label (e.g., "TL", "SMA")
     * @param tooltip full description for tooltip
     */
    public ToolButton(String label, String tooltip) {
        this.label = label;
        this.tooltip = tooltip;

        // Default styling
        this.backgroundColor = new Color(40, 42, 46);
        this.hoverColor = new Color(55, 57, 61);
        this.selectedColor = new Color(42, 130, 255);
        this.textColor = new Color(180, 182, 186);
        this.textSelectedColor = Color.WHITE;
        this.font = new Font("SansSerif", Font.BOLD, 10);

        setToolTipText(tooltip);
        setPreferredSize(new Dimension(36, 36));
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
            public void mousePressed(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    /**
     * Returns the button label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the tooltip text.
     */
    public String getTooltipText() {
        return tooltip;
    }

    /**
     * Returns whether the button is selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets whether the button is selected.
     */
    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            repaint();
        }
    }

    /**
     * Sets the click handler.
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    /**
     * Applies styling from a SidebarConfig.
     */
    public void applyConfig(SidebarConfig config) {
        this.backgroundColor = config.getButtonColor();
        this.hoverColor = config.getButtonHoverColor();
        this.selectedColor = config.getButtonSelectedColor();
        this.textColor = config.getTextColor();
        this.textSelectedColor = config.getTextSelectedColor();
        this.font = config.getButtonFont();
        this.cornerRadius = config.getCornerRadius();

        int size = config.getButtonSize();
        setPreferredSize(new Dimension(size, size));

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
        if (selected) {
            bg = selectedColor;
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
        g2.setColor(selected ? textSelectedColor : textColor);

        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(label)) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(label, textX, textY);

        g2.dispose();
    }
}
