package com.apokalypsix.chartx.core.ui.properties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Base class for property dialogs with dark theme styling.
 *
 * <p>Provides a modal dialog with a title, content panel, and action buttons
 * (Apply, Cancel, Reset). Subclasses populate the content panel with
 * property editors.
 *
 * <p>Dialog uses dark theme colors to match the chart UI:
 * <ul>
 *   <li>Background: RGB(30, 32, 36)</li>
 *   <li>Border: RGB(60, 62, 66)</li>
 *   <li>Text: RGB(220, 222, 226)</li>
 *   <li>Buttons: RGB(50, 52, 56) with hover highlight</li>
 * </ul>
 */
public abstract class PropertyDialog extends JDialog {

    // Dark theme colors (matching chart UI)
    protected static final Color BACKGROUND_COLOR = new Color(30, 32, 36);
    protected static final Color PANEL_COLOR = new Color(40, 42, 46);
    protected static final Color BORDER_COLOR = new Color(60, 62, 66);
    protected static final Color TEXT_COLOR = new Color(220, 222, 226);
    protected static final Color LABEL_COLOR = new Color(180, 182, 186);
    protected static final Color BUTTON_COLOR = new Color(50, 52, 56);
    protected static final Color BUTTON_HOVER_COLOR = new Color(70, 72, 76);
    protected static final Color BUTTON_PRESSED_COLOR = new Color(80, 82, 86);
    protected static final Color PRIMARY_BUTTON_COLOR = new Color(38, 166, 154);
    protected static final Color PRIMARY_BUTTON_HOVER = new Color(48, 180, 168);

    protected static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    protected static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    protected static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    private final JPanel contentPanel;
    private final JButton applyButton;
    private final JButton cancelButton;
    private final JButton resetButton;

    private boolean applied = false;

    /**
     * Creates a property dialog with the given title.
     *
     * @param owner the owner frame
     * @param title the dialog title
     */
    public PropertyDialog(Frame owner, String title) {
        super(owner, title, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Content panel (subclasses populate this)
        contentPanel = new JPanel();
        contentPanel.setBackground(PANEL_COLOR);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(PANEL_COLOR);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        resetButton = createButton("Reset", false);
        resetButton.addActionListener(e -> onReset());
        buttonPanel.add(resetButton);

        // Spacer
        buttonPanel.add(Box.createHorizontalStrut(20));

        // Close button (changes are applied dynamically)
        applyButton = createButton("Close", true);
        applyButton.addActionListener(e -> onClose());
        buttonPanel.add(applyButton);

        // Cancel button is hidden by default (live updates mean no cancel needed)
        cancelButton = createButton("Cancel", false);
        cancelButton.addActionListener(e -> onCancel());
        cancelButton.setVisible(false);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Escape key closes dialog
        getRootPane().registerKeyboardAction(
                e -> onClose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter key closes
        getRootPane().setDefaultButton(applyButton);

        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
    }

    /**
     * Returns the content panel where property editors should be added.
     */
    protected JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * Creates a styled button.
     */
    protected JButton createButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(LABEL_FONT);
        button.setForeground(TEXT_COLOR);
        button.setBackground(primary ? PRIMARY_BUTTON_COLOR : BUTTON_COLOR);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(80, 28));

        Color normalColor = primary ? PRIMARY_BUTTON_COLOR : BUTTON_COLOR;
        Color hoverColor = primary ? PRIMARY_BUTTON_HOVER : BUTTON_HOVER_COLOR;

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(normalColor);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(BUTTON_PRESSED_COLOR);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverColor : normalColor);
            }
        });

        return button;
    }

    /**
     * Creates a labeled property row with a label and editor component.
     *
     * @param label     the property label
     * @param component the editor component
     * @return a panel containing the label and component
     */
    protected JPanel createPropertyRow(String label, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(PANEL_COLOR);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(LABEL_FONT);
        labelComponent.setForeground(LABEL_COLOR);
        labelComponent.setPreferredSize(new Dimension(120, 24));
        row.add(labelComponent, BorderLayout.WEST);

        row.add(component, BorderLayout.CENTER);

        return row;
    }

    /**
     * Creates a section header for grouping related properties.
     *
     * @param title the section title
     * @return a label component
     */
    protected JLabel createSectionHeader(String title) {
        JLabel header = new JLabel(title);
        header.setFont(TITLE_FONT);
        header.setForeground(TEXT_COLOR);
        header.setBorder(new EmptyBorder(15, 0, 5, 0));
        return header;
    }

    /**
     * Styles a scrollbar with dark theme colors.
     */
    private void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = BORDER_COLOR;
                this.thumbHighlightColor = BUTTON_HOVER_COLOR;
                this.trackColor = PANEL_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollBar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? BUTTON_HOVER_COLOR : BORDER_COLOR);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(PANEL_COLOR);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
        });
        scrollBar.setPreferredSize(new Dimension(10, 10));
    }

    /**
     * Called when Close button is clicked.
     * Changes are applied dynamically, so this just closes the dialog.
     */
    protected void onClose() {
        applied = true;
        dispose();
    }

    /**
     * Called when Apply button is clicked (legacy, kept for compatibility).
     * @deprecated Use onClose() instead - changes are now applied dynamically.
     */
    @Deprecated
    protected void onApply() {
        applied = true;
    }

    /**
     * Called when Cancel button is clicked.
     * Subclasses can override to revert to original values.
     * Note: Cancel button is hidden by default since changes are applied dynamically.
     */
    protected void onCancel() {
        applied = false;
        dispose();
    }

    /**
     * Called when Reset button is clicked.
     * Subclasses should override to reset values to defaults.
     */
    protected abstract void onReset();

    /**
     * Returns true if the dialog was closed normally (not cancelled).
     */
    public boolean isApplied() {
        return applied;
    }

    /**
     * Shows the dialog centered over the owner.
     *
     * @return true if closed normally, false if cancelled
     */
    public boolean showDialog() {
        pack();
        setMinimumSize(new Dimension(350, 200));

        // Limit height to 85% of owner window height
        Window owner = getOwner();
        if (owner != null) {
            int maxHeight = (int) (owner.getHeight() * 0.85);
            if (getHeight() > maxHeight) {
                setSize(getWidth(), maxHeight);
            }
        }

        setLocationRelativeTo(owner);
        setVisible(true);
        return applied;
    }
}
