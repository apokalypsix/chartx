package com.apokalypsix.chartx.core.ui.properties.editors;

import com.apokalypsix.chartx.core.ui.properties.PropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

/**
 * Property editor for boolean values using a styled JCheckBox.
 */
public class BooleanPropertyEditor implements PropertyEditor<Boolean> {

    // Dark theme colors
    private static final Color BACKGROUND = new Color(45, 47, 51);
    private static final Color TEXT_COLOR = new Color(220, 222, 226);
    private static final Color CHECK_COLOR = new Color(38, 166, 154);

    private final JCheckBox checkBox;
    private Consumer<Boolean> onValueChanged;
    private ItemListener itemListener;

    /**
     * Creates a boolean editor with the specified initial value.
     *
     * @param initialValue the initial value
     */
    public BooleanPropertyEditor(boolean initialValue) {
        this(initialValue, null);
    }

    /**
     * Creates a boolean editor with the specified initial value and label.
     *
     * @param initialValue the initial value
     * @param label        optional label text (can be null)
     */
    public BooleanPropertyEditor(boolean initialValue, String label) {
        checkBox = new JCheckBox(label);
        checkBox.setSelected(initialValue);
        checkBox.setBackground(BACKGROUND);
        checkBox.setForeground(TEXT_COLOR);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add item listener
        itemListener = e -> {
            if (onValueChanged != null) {
                onValueChanged.accept(checkBox.isSelected());
            }
        };
        checkBox.addItemListener(itemListener);
    }

    @Override
    public JComponent getComponent() {
        return checkBox;
    }

    @Override
    public Boolean getValue() {
        return checkBox.isSelected();
    }

    @Override
    public void setValue(Boolean value) {
        // Temporarily remove listener to avoid triggering callback
        checkBox.removeItemListener(itemListener);
        checkBox.setSelected(value != null && value);
        checkBox.addItemListener(itemListener);
    }

    @Override
    public void setOnValueChanged(Consumer<Boolean> callback) {
        this.onValueChanged = callback;
    }

    /**
     * Sets the checkbox label text.
     */
    public void setLabel(String label) {
        checkBox.setText(label);
    }

    /**
     * Returns the checkbox label text.
     */
    public String getLabel() {
        return checkBox.getText();
    }
}
