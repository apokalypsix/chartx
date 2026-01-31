package com.apokalypsix.chartx.core.ui.properties.dialogs;

import com.apokalypsix.chartx.chart.overlay.Drawing;
import com.apokalypsix.chartx.core.ui.properties.PropertyDialog;
import com.apokalypsix.chartx.core.ui.properties.editors.*;

import javax.swing.*;
import java.awt.*;

/**
 * Property dialog for editing drawing properties.
 *
 * <p>Allows editing of common drawing properties (color, line width, opacity, visibility)
 * and can be extended for type-specific properties.
 *
 * <p>Uses the drawing's staging API to allow Apply/Cancel behavior.
 *
 * <p>Example usage:
 * <pre>{@code
 * DrawingPropertyDialog dialog = new DrawingPropertyDialog(frame, drawing);
 * if (dialog.showDialog()) {
 *     drawing.applyChanges();
 *     chart.repaint();
 * } else {
 *     drawing.discardChanges();
 * }
 * }</pre>
 */
public class DrawingPropertyDialog extends PropertyDialog {

    private final Drawing drawing;

    private ColorPropertyEditor colorEditor;
    private DoublePropertyEditor lineWidthEditor;
    private DoublePropertyEditor opacityEditor;
    private BooleanPropertyEditor visibleEditor;

    /**
     * Creates a drawing property dialog.
     *
     * @param owner   the owner frame
     * @param drawing the drawing to edit
     */
    public DrawingPropertyDialog(Frame owner, Drawing drawing) {
        super(owner, getDialogTitle(drawing));
        this.drawing = drawing;
        buildContent();
    }

    private static String getDialogTitle(Drawing drawing) {
        String typeName = formatTypeName(drawing.getType().name());
        return typeName + " Settings";
    }

    private static String formatTypeName(String name) {
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

    private void buildContent() {
        JPanel content = getContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Color
        colorEditor = new ColorPropertyEditor(drawing.getEffectiveColor());
        colorEditor.setOnValueChanged(drawing::stageColor);
        addPropertyRow(content, "Color", colorEditor.getComponent());

        // Line Width
        lineWidthEditor = new DoublePropertyEditor(0.5, 10.0, drawing.getEffectiveLineWidth(), 0.5);
        lineWidthEditor.setOnValueChanged(value -> drawing.stageLineWidth(value.floatValue()));
        addPropertyRow(content, "Line Width", lineWidthEditor.getComponent());

        // Opacity
        opacityEditor = new DoublePropertyEditor(0.0, 1.0, drawing.getEffectiveOpacity(), 0.1);
        opacityEditor.setOnValueChanged(value -> drawing.stageOpacity(value.floatValue()));
        addPropertyRow(content, "Opacity", opacityEditor.getComponent());

        // Visible
        visibleEditor = new BooleanPropertyEditor(drawing.getEffectiveVisible());
        visibleEditor.setOnValueChanged(drawing::stageVisible);
        addPropertyRow(content, "Visible", visibleEditor.getComponent());

        // Add type-specific properties
        addTypeSpecificProperties(content);
    }

    private void addPropertyRow(JPanel content, String label, JComponent component) {
        JPanel row = createPropertyRow(label, component);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        content.add(row);
    }

    /**
     * Override this method in subclasses to add type-specific properties.
     *
     * @param content the content panel
     */
    protected void addTypeSpecificProperties(JPanel content) {
        // Subclasses can override to add type-specific properties
        // For example, TrendLine could add "Extend Left" and "Extend Right" checkboxes
        // Rectangle could add "Filled" checkbox and "Fill Color" picker
    }

    @Override
    protected void onApply() {
        drawing.applyChanges();
        super.onApply();
    }

    @Override
    protected void onCancel() {
        drawing.discardChanges();
        super.onCancel();
    }

    @Override
    protected void onReset() {
        // Reset to default values
        Color defaultColor = new Color(255, 193, 7);
        float defaultLineWidth = 1.5f;
        float defaultOpacity = 1.0f;
        boolean defaultVisible = true;

        colorEditor.setValue(defaultColor);
        lineWidthEditor.setValue((double) defaultLineWidth);
        opacityEditor.setValue((double) defaultOpacity);
        visibleEditor.setValue(defaultVisible);

        // Stage the reset values
        drawing.stageColor(defaultColor);
        drawing.stageLineWidth(defaultLineWidth);
        drawing.stageOpacity(defaultOpacity);
        drawing.stageVisible(defaultVisible);
    }

    /**
     * Returns the drawing being edited.
     */
    public Drawing getDrawing() {
        return drawing;
    }
}
