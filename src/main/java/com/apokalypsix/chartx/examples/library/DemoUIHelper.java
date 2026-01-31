package com.apokalypsix.chartx.examples.library;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Utility class for creating styled UI components in demo applications.
 *
 * <p>Provides factory methods for creating consistently styled Swing components
 * that match the dark theme used in ChartX demos.
 */
public final class DemoUIHelper {

    private DemoUIHelper() {
        // Utility class
    }

    /**
     * Creates a styled control panel with dark background.
     *
     * @return JPanel configured for controls
     */
    public static JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DemoConfig.PANEL_BACKGROUND);
        return panel;
    }

    /**
     * Creates a styled info label.
     *
     * @param text label text
     * @return styled JLabel
     */
    public static JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DemoConfig.LABEL_FOREGROUND);
        return label;
    }

    /**
     * Creates a styled label for control sections.
     *
     * @param text label text
     * @return styled JLabel
     */
    public static JLabel createControlLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DemoConfig.CONTROL_FOREGROUND);
        return label;
    }

    /**
     * Creates a styled checkbox.
     *
     * @param text checkbox label
     * @param selected initial selection state
     * @return styled JCheckBox
     */
    public static JCheckBox createCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setForeground(DemoConfig.CONTROL_FOREGROUND);
        checkBox.setOpaque(false);
        return checkBox;
    }

    /**
     * Creates a styled checkbox with action listener.
     *
     * @param text checkbox label
     * @param selected initial selection state
     * @param listener action listener
     * @return styled JCheckBox
     */
    public static JCheckBox createCheckBox(String text, boolean selected, ActionListener listener) {
        JCheckBox checkBox = createCheckBox(text, selected);
        checkBox.addActionListener(listener);
        return checkBox;
    }

    /**
     * Creates a styled toggle button.
     *
     * @param text button text
     * @return styled JToggleButton
     */
    public static JToggleButton createToggleButton(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setFocusable(false);
        return button;
    }

    /**
     * Creates a styled toggle button with action listener.
     *
     * @param text button text
     * @param listener action listener
     * @return styled JToggleButton
     */
    public static JToggleButton createToggleButton(String text, ActionListener listener) {
        JToggleButton button = createToggleButton(text);
        button.addActionListener(listener);
        return button;
    }

    /**
     * Creates a styled toggle button and adds it to a button group.
     *
     * @param text button text
     * @param group button group (may be null)
     * @return styled JToggleButton
     */
    public static JToggleButton createToggleButton(String text, ButtonGroup group) {
        JToggleButton button = createToggleButton(text);
        if (group != null) {
            group.add(button);
        }
        return button;
    }

    /**
     * Creates a styled button.
     *
     * @param text button text
     * @return styled JButton
     */
    public static JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        return button;
    }

    /**
     * Creates a styled button with action listener.
     *
     * @param text button text
     * @param listener action listener
     * @return styled JButton
     */
    public static JButton createButton(String text, ActionListener listener) {
        JButton button = createButton(text);
        button.addActionListener(listener);
        return button;
    }

    /**
     * Creates a horizontal spacer for control panels.
     *
     * @param width spacer width in pixels
     * @return Component representing horizontal space
     */
    public static Component createHorizontalSpacer(int width) {
        return Box.createHorizontalStrut(width);
    }

    /**
     * Creates a default horizontal spacer (20 pixels).
     *
     * @return Component representing horizontal space
     */
    public static Component createHorizontalSpacer() {
        return createHorizontalSpacer(20);
    }

    /**
     * Creates a vertical spacer.
     *
     * @param height spacer height in pixels
     * @return Component representing vertical space
     */
    public static Component createVerticalSpacer(int height) {
        return Box.createVerticalStrut(height);
    }

    /**
     * Creates a separator for control panels.
     *
     * @return JSeparator configured for vertical orientation
     */
    public static JSeparator createVerticalSeparator() {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 20));
        return separator;
    }

    /**
     * Creates a combo box with items.
     *
     * @param items items to populate
     * @param <T> type of items
     * @return styled JComboBox
     */
    @SafeVarargs
    public static <T> JComboBox<T> createComboBox(T... items) {
        JComboBox<T> comboBox = new JComboBox<>(items);
        comboBox.setFocusable(false);
        return comboBox;
    }

    /**
     * Creates and configures a demo JFrame.
     *
     * @param title frame title
     * @param width frame width
     * @param height frame height
     * @return configured JFrame
     */
    public static JFrame createDemoFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        return frame;
    }

    /**
     * Creates a demo JFrame with default size.
     *
     * @param title frame title
     * @return configured JFrame
     */
    public static JFrame createDemoFrame(String title) {
        return createDemoFrame(title, DemoConfig.DEFAULT_WIDTH, DemoConfig.DEFAULT_HEIGHT);
    }

    /**
     * Shows a demo frame centered on screen.
     *
     * @param frame frame to display
     */
    public static void showFrame(JFrame frame) {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Shows an error dialog for unavailable backend.
     *
     * @param backendName name of the unavailable backend
     */
    public static void showBackendUnavailable(String backendName) {
        JOptionPane.showMessageDialog(
                null,
                backendName + " backend is not available on this system.",
                "Backend Unavailable",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Shows an error dialog with custom message.
     *
     * @param message error message
     */
    public static void showError(String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
