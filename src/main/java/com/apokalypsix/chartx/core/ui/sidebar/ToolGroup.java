package com.apokalypsix.chartx.core.ui.sidebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A collapsible group of tool buttons.
 *
 * <p>Groups related tools together (e.g., "Lines", "Shapes", "Indicators").
 * Can be expanded/collapsed by clicking the header.
 *
 * <p>Example usage:
 * <pre>{@code
 * ToolGroup group = new ToolGroup("Lines");
 * group.addButton(new ToolButton("TL", "Trend Line"));
 * group.addButton(new ToolButton("HL", "Horizontal Line"));
 * }</pre>
 */
public class ToolGroup extends JPanel {

    private final String name;
    private final List<ToolButton> buttons = new ArrayList<>();
    private final JPanel buttonPanel;
    private final JLabel headerLabel;
    private boolean expanded = true;
    private boolean showHeader = false;

    private SidebarConfig config;
    private int buttonPadding = 6;

    /**
     * Creates a tool group with the given name.
     *
     * @param name the group name
     */
    public ToolGroup(String name) {
        this.name = name;

        setLayout(new BorderLayout());
        setOpaque(false);

        // Header (optional, hidden by default)
        headerLabel = new JLabel(name);
        headerLabel.setForeground(new Color(100, 102, 106));
        headerLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.setVisible(false);

        headerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setExpanded(!expanded);
            }
        });

        add(headerLabel, BorderLayout.NORTH);

        // Button panel
        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        add(buttonPanel, BorderLayout.CENTER);
    }

    /**
     * Returns the group name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the group is expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Sets whether the group is expanded.
     */
    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            this.expanded = expanded;
            buttonPanel.setVisible(expanded);
            revalidate();
        }
    }

    /**
     * Returns whether the header is shown.
     */
    public boolean isShowHeader() {
        return showHeader;
    }

    /**
     * Sets whether to show the header.
     */
    public void setShowHeader(boolean show) {
        this.showHeader = show;
        headerLabel.setVisible(show);
    }

    /**
     * Adds a button to this group.
     */
    public void addButton(ToolButton button) {
        buttons.add(button);

        if (config != null) {
            button.applyConfig(config);
        }

        // Wrap button in a panel for proper centering and padding
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, buttonPadding / 2));
        wrapper.setOpaque(false);
        wrapper.add(button);

        buttonPanel.add(wrapper);
        revalidate();
    }

    /**
     * Removes a button from this group.
     */
    public void removeButton(ToolButton button) {
        buttons.remove(button);
        // Find and remove the wrapper containing this button
        for (Component comp : buttonPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel wrapper = (JPanel) comp;
                for (Component child : wrapper.getComponents()) {
                    if (child == button) {
                        buttonPanel.remove(wrapper);
                        revalidate();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Returns all buttons in this group.
     */
    public List<ToolButton> getButtons() {
        return new ArrayList<>(buttons);
    }

    /**
     * Clears the selection on all buttons in this group.
     */
    public void clearSelection() {
        for (ToolButton button : buttons) {
            button.setSelected(false);
        }
    }

    /**
     * Applies styling from a SidebarConfig.
     */
    public void applyConfig(SidebarConfig config) {
        this.config = config;
        this.buttonPadding = config.getButtonPadding();

        headerLabel.setForeground(config.getTextColor());
        headerLabel.setFont(config.getButtonFont());
        setShowHeader(config.isShowGroupHeaders());

        for (ToolButton button : buttons) {
            button.applyConfig(config);
        }

        // Re-add buttons with updated padding
        buttonPanel.removeAll();
        for (ToolButton button : buttons) {
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, buttonPadding / 2));
            wrapper.setOpaque(false);
            wrapper.add(button);
            buttonPanel.add(wrapper);
        }

        revalidate();
        repaint();
    }
}
