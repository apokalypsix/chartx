package com.apokalypsix.chartx.core.ui.properties.editors;

import com.apokalypsix.chartx.core.ui.properties.PropertyEditor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Property editor for enum values using a styled JComboBox.
 *
 * @param <E> the enum type
 */
public class EnumPropertyEditor<E extends Enum<E>> implements PropertyEditor<E> {

    // Dark theme colors
    private static final Color BACKGROUND = new Color(45, 47, 51);
    private static final Color TEXT_COLOR = new Color(220, 222, 226);
    private static final Color BORDER_COLOR = new Color(60, 62, 66);
    private static final Color SELECTION_BG = new Color(38, 166, 154);

    private final JComboBox<E> comboBox;
    private final Class<E> enumClass;
    private Consumer<E> onValueChanged;
    private ActionListener actionListener;

    /**
     * Creates an enum editor for the specified enum class.
     *
     * @param enumClass    the enum class
     * @param initialValue the initial selected value
     */
    public EnumPropertyEditor(Class<E> enumClass, E initialValue) {
        this.enumClass = enumClass;

        comboBox = new JComboBox<>(enumClass.getEnumConstants());
        if (initialValue != null) {
            comboBox.setSelectedItem(initialValue);
        }

        styleComboBox();

        // Add action listener
        actionListener = e -> {
            if (onValueChanged != null) {
                onValueChanged.accept(getValue());
            }
        };
        comboBox.addActionListener(actionListener);
    }

    private void styleComboBox() {
        comboBox.setPreferredSize(new Dimension(150, 24));
        comboBox.setBackground(BACKGROUND);
        comboBox.setForeground(TEXT_COLOR);

        // Custom UI for dark theme
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton();
                button.setText("\u25BC"); // Down arrow
                button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
                button.setBackground(BACKGROUND);
                button.setForeground(TEXT_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setOpaque(true);
                return button;
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane scroller = super.createScroller();
                        scroller.setBackground(BACKGROUND);
                        scroller.getViewport().setBackground(BACKGROUND);
                        scroller.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
                        return scroller;
                    }
                };
                popup.getList().setBackground(BACKGROUND);
                popup.getList().setForeground(TEXT_COLOR);
                popup.getList().setSelectionBackground(SELECTION_BG);
                popup.getList().setSelectionForeground(Color.WHITE);
                popup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
                return popup;
            }
        });

        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // Custom renderer for dark theme
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (isSelected) {
                    label.setBackground(SELECTION_BG);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(BACKGROUND);
                    label.setForeground(TEXT_COLOR);
                }

                // Use toString() for display (allows enums to provide custom display names)
                if (value != null) {
                    label.setText(value.toString());
                }

                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                return label;
            }
        });

        comboBox.setOpaque(true);
    }

    /**
     * Formats an enum constant name for display.
     * Converts SOME_VALUE to "Some Value".
     */
    private String formatEnumName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        StringBuilder sb = new StringBuilder();
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    @Override
    public JComponent getComponent() {
        return comboBox;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getValue() {
        return (E) comboBox.getSelectedItem();
    }

    @Override
    public void setValue(E value) {
        // Temporarily remove listener to avoid triggering callback
        comboBox.removeActionListener(actionListener);
        comboBox.setSelectedItem(value);
        comboBox.addActionListener(actionListener);
    }

    @Override
    public void setOnValueChanged(Consumer<E> callback) {
        this.onValueChanged = callback;
    }

    /**
     * Returns the enum class this editor handles.
     */
    public Class<E> getEnumClass() {
        return enumClass;
    }
}
