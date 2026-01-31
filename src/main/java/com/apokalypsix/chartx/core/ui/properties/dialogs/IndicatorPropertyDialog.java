package com.apokalypsix.chartx.core.ui.properties.dialogs;

import com.apokalypsix.chartx.chart.finance.indicator.IndicatorDescriptor;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorInstance;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorParameter;
import com.apokalypsix.chartx.core.ui.properties.PropertyDialog;
import com.apokalypsix.chartx.core.ui.properties.PropertyEditor;
import com.apokalypsix.chartx.core.ui.properties.editors.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Property dialog for editing indicator parameters.
 *
 * <p>Dynamically builds property editors based on the indicator's parameter
 * definitions. Uses the indicator's staging API to allow Apply/Cancel behavior.
 *
 * <p>Example usage:
 * <pre>{@code
 * IndicatorPropertyDialog dialog = new IndicatorPropertyDialog(frame, instance);
 * if (dialog.showDialog()) {
 *     instance.applyChanges();
 *     indicatorManager.recalculate(instance);
 * } else {
 *     instance.discardChanges();
 * }
 * }</pre>
 */
public class IndicatorPropertyDialog extends PropertyDialog {

    private final IndicatorInstance<?, ?> instance;
    private final Map<String, PropertyEditor<?>> editors = new LinkedHashMap<>();

    /**
     * Creates an indicator property dialog.
     *
     * @param owner    the owner frame
     * @param instance the indicator instance to edit
     */
    public IndicatorPropertyDialog(Frame owner, IndicatorInstance<?, ?> instance) {
        super(owner, instance.getDisplayName() + " Settings");
        this.instance = instance;
        buildContent();
    }

    private void buildContent() {
        JPanel content = getContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        IndicatorDescriptor descriptor = instance.getDescriptor();
        Map<String, IndicatorParameter<?>> parameters = descriptor.getParameters();

        if (parameters.isEmpty()) {
            JLabel noParamsLabel = new JLabel("This indicator has no configurable parameters.");
            noParamsLabel.setFont(LABEL_FONT);
            noParamsLabel.setForeground(LABEL_COLOR);
            noParamsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(noParamsLabel);
            return;
        }

        // Group parameters by type for better organization
        // Numeric parameters first, then colors, then booleans
        for (IndicatorParameter<?> param : parameters.values()) {
            if (param instanceof IndicatorParameter.IntParam ||
                param instanceof IndicatorParameter.DoubleParam) {
                addParameterEditor(content, param);
            }
        }

        boolean hasColorSection = false;
        for (IndicatorParameter<?> param : parameters.values()) {
            if (param instanceof IndicatorParameter.ColorParam) {
                if (!hasColorSection) {
                    content.add(createSectionHeader("Colors"));
                    hasColorSection = true;
                }
                addParameterEditor(content, param);
            }
        }

        boolean hasOptionsSection = false;
        for (IndicatorParameter<?> param : parameters.values()) {
            if (param instanceof IndicatorParameter.BooleanParam ||
                param instanceof IndicatorParameter.EnumParam) {
                if (!hasOptionsSection) {
                    content.add(createSectionHeader("Options"));
                    hasOptionsSection = true;
                }
                addParameterEditor(content, param);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addParameterEditor(JPanel content, IndicatorParameter<?> param) {
        PropertyEditor<?> editor = createEditor(param);
        if (editor == null) {
            return;
        }

        editors.put(param.name(), editor);

        JPanel row = createPropertyRow(param.label(), editor.getComponent());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        content.add(row);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PropertyEditor<?> createEditor(IndicatorParameter<?> param) {
        Object currentValue = instance.getEffectiveParameterValue(param.name());

        if (param instanceof IndicatorParameter.IntParam) {
            IndicatorParameter.IntParam intParam = (IndicatorParameter.IntParam) param;
            IntPropertyEditor editor = new IntPropertyEditor(
                    intParam.min(), intParam.max(),
                    currentValue != null ? (Integer) currentValue : intParam.defaultValue());

            editor.setOnValueChanged(value -> stageParameter(param.name(), value));
            return editor;

        } else if (param instanceof IndicatorParameter.DoubleParam) {
            IndicatorParameter.DoubleParam doubleParam = (IndicatorParameter.DoubleParam) param;
            // Determine step size based on range
            double range = doubleParam.max() - doubleParam.min();
            double step = range > 10 ? 0.5 : (range > 1 ? 0.1 : 0.01);

            DoublePropertyEditor editor = new DoublePropertyEditor(
                    doubleParam.min(), doubleParam.max(),
                    currentValue != null ? (Double) currentValue : doubleParam.defaultValue(),
                    step);

            editor.setOnValueChanged(value -> stageParameter(param.name(), value));
            return editor;

        } else if (param instanceof IndicatorParameter.ColorParam) {
            Color color = currentValue != null ? (Color) currentValue : ((IndicatorParameter.ColorParam) param).defaultValue();
            ColorPropertyEditor editor = new ColorPropertyEditor(color);

            editor.setOnValueChanged(value -> stageParameter(param.name(), value));
            return editor;

        } else if (param instanceof IndicatorParameter.BooleanParam) {
            boolean value = currentValue != null ? (Boolean) currentValue : ((IndicatorParameter.BooleanParam) param).defaultValue();
            BooleanPropertyEditor editor = new BooleanPropertyEditor(value);

            editor.setOnValueChanged(v -> stageParameter(param.name(), v));
            return editor;

        } else if (param instanceof IndicatorParameter.EnumParam) {
            return createEnumEditor((IndicatorParameter.EnumParam<?>) param, currentValue);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> PropertyEditor<?> createEnumEditor(IndicatorParameter.EnumParam<E> enumParam, Object currentValue) {
        Class<E> enumClass = (Class<E>) enumParam.possibleValues()[0].getDeclaringClass();
        E enumValue = currentValue != null ? (E) currentValue : enumParam.defaultValue();

        EnumPropertyEditor<E> editor = new EnumPropertyEditor<>(enumClass, enumValue);
        editor.setOnValueChanged(value -> stageParameter(enumParam.name(), value));
        return editor;
    }

    private void stageParameter(String name, Object value) {
        try {
            instance.stageParameterValue(name, value);
        } catch (IllegalArgumentException e) {
            // Value validation failed - the editor should already enforce constraints
            // so this shouldn't normally happen
        }
    }

    @Override
    protected void onApply() {
        // Apply all staged changes
        instance.applyChanges();
        super.onApply();
    }

    @Override
    protected void onCancel() {
        // Discard all staged changes
        instance.discardChanges();
        super.onCancel();
    }

    @Override
    protected void onReset() {
        // Reset all parameters to defaults
        instance.resetToDefaults();

        // Update all editor values
        IndicatorDescriptor descriptor = instance.getDescriptor();
        for (Map.Entry<String, PropertyEditor<?>> entry : editors.entrySet()) {
            String paramName = entry.getKey();
            PropertyEditor<?> editor = entry.getValue();
            IndicatorParameter<?> param = descriptor.getParameter(paramName);
            if (param != null) {
                setEditorValue(editor, param.defaultValue());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setEditorValue(PropertyEditor<?> editor, Object value) {
        if (editor instanceof IntPropertyEditor && value instanceof Integer) {
            ((IntPropertyEditor) editor).setValue((Integer) value);
        } else if (editor instanceof DoublePropertyEditor && value instanceof Double) {
            ((DoublePropertyEditor) editor).setValue((Double) value);
        } else if (editor instanceof ColorPropertyEditor && value instanceof Color) {
            ((ColorPropertyEditor) editor).setValue((Color) value);
        } else if (editor instanceof BooleanPropertyEditor && value instanceof Boolean) {
            ((BooleanPropertyEditor) editor).setValue((Boolean) value);
        } else if (editor instanceof EnumPropertyEditor && value instanceof Enum) {
            ((EnumPropertyEditor) editor).setValue((Enum) value);
        }
    }

    /**
     * Returns the indicator instance being edited.
     */
    public IndicatorInstance<?, ?> getIndicatorInstance() {
        return instance;
    }
}
