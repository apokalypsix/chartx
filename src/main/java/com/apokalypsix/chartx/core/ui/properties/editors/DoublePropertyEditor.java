package com.apokalypsix.chartx.core.ui.properties.editors;

import com.apokalypsix.chartx.core.ui.properties.PropertyEditor;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.function.Consumer;

/**
 * Property editor for double values using a JSpinner.
 *
 * <p>Supports min/max constraints, step size, and decimal precision configuration.
 */
public class DoublePropertyEditor implements PropertyEditor<Double> {

    // Dark theme colors
    private static final Color BACKGROUND = new Color(45, 47, 51);
    private static final Color TEXT_COLOR = new Color(220, 222, 226);
    private static final Color BORDER_COLOR = new Color(60, 62, 66);

    private final JSpinner spinner;
    private final SpinnerNumberModel model;
    private Consumer<Double> onValueChanged;
    private ChangeListener changeListener;

    /**
     * Creates a double editor with specified constraints.
     *
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @param defaultValue initial value
     */
    public DoublePropertyEditor(double min, double max, double defaultValue) {
        this(min, max, defaultValue, 0.1);
    }

    /**
     * Creates a double editor with specified constraints and step size.
     *
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @param defaultValue initial value
     * @param step         increment/decrement step
     */
    public DoublePropertyEditor(double min, double max, double defaultValue, double step) {
        model = new SpinnerNumberModel(defaultValue, min, max, step);
        spinner = new JSpinner(model);

        // Style the spinner
        styleSpinner(step);

        // Add change listener
        changeListener = e -> {
            if (onValueChanged != null) {
                onValueChanged.accept(getValue());
            }
        };
        spinner.addChangeListener(changeListener);
    }

    private void styleSpinner(double step) {
        spinner.setPreferredSize(new Dimension(100, 24));

        // Configure decimal format based on step precision
        int decimals = getDecimalPlaces(step);
        String pattern = decimals == 0 ? "0" : "0." + "0".repeat(decimals);

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, pattern);
        spinner.setEditor(editor);

        JFormattedTextField field = editor.getTextField();
        field.setBackground(BACKGROUND);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        spinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }

    private int getDecimalPlaces(double value) {
        String text = Double.toString(Math.abs(value));
        int integerPlaces = text.indexOf('.');
        return integerPlaces < 0 ? 0 : Math.min(text.length() - integerPlaces - 1, 4);
    }

    @Override
    public JComponent getComponent() {
        return spinner;
    }

    @Override
    public Double getValue() {
        return model.getNumber().doubleValue();
    }

    @Override
    public void setValue(Double value) {
        // Temporarily remove listener to avoid triggering callback
        spinner.removeChangeListener(changeListener);
        model.setValue(value);
        spinner.addChangeListener(changeListener);
    }

    @Override
    public void setOnValueChanged(Consumer<Double> callback) {
        this.onValueChanged = callback;
    }

    @Override
    public boolean isValid() {
        double value = getValue();
        double min = ((Number) model.getMinimum()).doubleValue();
        double max = ((Number) model.getMaximum()).doubleValue();
        return value >= min && value <= max;
    }

    /**
     * Returns the minimum allowed value.
     */
    public double getMin() {
        return ((Number) model.getMinimum()).doubleValue();
    }

    /**
     * Returns the maximum allowed value.
     */
    public double getMax() {
        return ((Number) model.getMaximum()).doubleValue();
    }

    /**
     * Sets the step size for increment/decrement.
     */
    public void setStepSize(double step) {
        model.setStepSize(step);
    }
}
