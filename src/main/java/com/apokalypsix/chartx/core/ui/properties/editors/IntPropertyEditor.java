package com.apokalypsix.chartx.core.ui.properties.editors;

import com.apokalypsix.chartx.core.ui.properties.PropertyEditor;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Property editor for integer values using a JSpinner.
 *
 * <p>Supports min/max constraints and step size configuration.
 */
public class IntPropertyEditor implements PropertyEditor<Integer> {

    // Dark theme colors
    private static final Color BACKGROUND = new Color(45, 47, 51);
    private static final Color TEXT_COLOR = new Color(220, 222, 226);
    private static final Color BORDER_COLOR = new Color(60, 62, 66);

    private final JSpinner spinner;
    private final SpinnerNumberModel model;
    private Consumer<Integer> onValueChanged;
    private ChangeListener changeListener;

    /**
     * Creates an integer editor with specified constraints.
     *
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @param defaultValue initial value
     */
    public IntPropertyEditor(int min, int max, int defaultValue) {
        this(min, max, defaultValue, 1);
    }

    /**
     * Creates an integer editor with specified constraints and step size.
     *
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @param defaultValue initial value
     * @param step         increment/decrement step
     */
    public IntPropertyEditor(int min, int max, int defaultValue, int step) {
        model = new SpinnerNumberModel(defaultValue, min, max, step);
        spinner = new JSpinner(model);

        // Style the spinner
        styleSpinner();

        // Add change listener
        changeListener = e -> {
            if (onValueChanged != null) {
                onValueChanged.accept(getValue());
            }
        };
        spinner.addChangeListener(changeListener);
    }

    private void styleSpinner() {
        spinner.setPreferredSize(new Dimension(100, 24));

        // Style the text field
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.NumberEditor) {
            JFormattedTextField field = ((JSpinner.NumberEditor) editor).getTextField();
            field.setBackground(BACKGROUND);
            field.setForeground(TEXT_COLOR);
            field.setCaretColor(TEXT_COLOR);
            field.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

        spinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }

    @Override
    public JComponent getComponent() {
        return spinner;
    }

    @Override
    public Integer getValue() {
        return model.getNumber().intValue();
    }

    @Override
    public void setValue(Integer value) {
        // Temporarily remove listener to avoid triggering callback
        spinner.removeChangeListener(changeListener);
        model.setValue(value);
        spinner.addChangeListener(changeListener);
    }

    @Override
    public void setOnValueChanged(Consumer<Integer> callback) {
        this.onValueChanged = callback;
    }

    @Override
    public boolean isValid() {
        int value = getValue();
        int min = ((Number) model.getMinimum()).intValue();
        int max = ((Number) model.getMaximum()).intValue();
        return value >= min && value <= max;
    }

    /**
     * Returns the minimum allowed value.
     */
    public int getMin() {
        return ((Number) model.getMinimum()).intValue();
    }

    /**
     * Returns the maximum allowed value.
     */
    public int getMax() {
        return ((Number) model.getMaximum()).intValue();
    }

    /**
     * Sets the step size for increment/decrement.
     */
    public void setStepSize(int step) {
        model.setStepSize(step);
    }
}
