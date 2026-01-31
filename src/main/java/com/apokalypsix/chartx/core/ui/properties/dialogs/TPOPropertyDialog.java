package com.apokalypsix.chartx.core.ui.properties.dialogs;

import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.core.data.model.TPOSeries.LineThickness;
import com.apokalypsix.chartx.core.data.model.TPOSeries.LineType;
import com.apokalypsix.chartx.core.ui.properties.PropertyDialog;
import com.apokalypsix.chartx.core.ui.properties.editors.BooleanPropertyEditor;
import com.apokalypsix.chartx.core.ui.properties.editors.ColorPropertyEditor;
import com.apokalypsix.chartx.core.ui.properties.editors.DoublePropertyEditor;
import com.apokalypsix.chartx.core.ui.properties.editors.EnumPropertyEditor;

import javax.swing.*;
import java.awt.*;

/**
 * Property dialog for editing TPO (Market Profile) display settings.
 *
 * <p>Allows configuration of:
 * <ul>
 *   <li>POC (Point of Control) - enable/disable and color</li>
 *   <li>Value Area (VAH/VAL) - enable/disable and color</li>
 *   <li>Initial Balance - enable/disable and color</li>
 *   <li>Single Prints - enable/disable and color</li>
 *   <li>Overlay mode and opacity</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * TPOPropertyDialog dialog = new TPOPropertyDialog(frame, tpoSeries);
 * if (dialog.showDialog()) {
 *     chart.repaint();
 * }
 * }</pre>
 */
public class TPOPropertyDialog extends PropertyDialog {

    private final TPOSeries series;
    private final Runnable onPropertyChanged;

    // Editors for display settings
    private BooleanPropertyEditor showPOCEditor;
    private ColorPropertyEditor pocColorEditor;
    private BooleanPropertyEditor showValueAreaEditor;
    private ColorPropertyEditor valueAreaColorEditor;
    private BooleanPropertyEditor showVAHEditor;
    private ColorPropertyEditor vahColorEditor;
    private EnumPropertyEditor<LineType> vahLineTypeEditor;
    private EnumPropertyEditor<LineThickness> vahLineThicknessEditor;
    private BooleanPropertyEditor showVALEditor;
    private ColorPropertyEditor valColorEditor;
    private EnumPropertyEditor<LineType> valLineTypeEditor;
    private EnumPropertyEditor<LineThickness> valLineThicknessEditor;
    private BooleanPropertyEditor showIBEditor;
    private ColorPropertyEditor ibColorEditor;
    private BooleanPropertyEditor showSinglePrintsEditor;
    private ColorPropertyEditor singlePrintColorEditor;
    private BooleanPropertyEditor overlayModeEditor;
    private DoublePropertyEditor opacityEditor;

    // Stored original values for reset
    private boolean origShowPOC;
    private Color origPocColor;
    private boolean origShowValueArea;
    private Color origValueAreaColor;
    private boolean origShowVAH;
    private Color origVahColor;
    private LineType origVahLineType;
    private LineThickness origVahLineThickness;
    private boolean origShowVAL;
    private Color origValColor;
    private LineType origValLineType;
    private LineThickness origValLineThickness;
    private boolean origShowIB;
    private Color origIBColor;
    private boolean origShowSinglePrints;
    private Color origSinglePrintColor;
    private boolean origOverlayMode;
    private float origOpacity;

    /**
     * Creates a TPO property dialog.
     *
     * @param owner  the owner frame
     * @param series the TPO series to edit
     */
    public TPOPropertyDialog(Frame owner, TPOSeries series) {
        this(owner, series, null);
    }

    /**
     * Creates a TPO property dialog with a change callback.
     *
     * @param owner             the owner frame
     * @param series            the TPO series to edit
     * @param onPropertyChanged callback invoked when any property changes (for live repaint)
     */
    public TPOPropertyDialog(Frame owner, TPOSeries series, Runnable onPropertyChanged) {
        super(owner, "TPO Settings");
        this.series = series;
        this.onPropertyChanged = onPropertyChanged;
        storeOriginalValues();
        buildContent();
    }

    private void storeOriginalValues() {
        origShowPOC = series.isShowPOC();
        origPocColor = series.getPocColor();
        origShowValueArea = series.isShowValueArea();
        origValueAreaColor = series.getValueAreaColor();
        origShowVAH = series.isShowVAH();
        origVahColor = series.getVahColor();
        origVahLineType = series.getVahLineType();
        origVahLineThickness = series.getVahLineThickness();
        origShowVAL = series.isShowVAL();
        origValColor = series.getValColor();
        origValLineType = series.getValLineType();
        origValLineThickness = series.getValLineThickness();
        origShowIB = series.isShowInitialBalance();
        origIBColor = series.getIBColor();
        origShowSinglePrints = series.isHighlightSinglePrints();
        origSinglePrintColor = series.getSinglePrintColor();
        origOverlayMode = series.isOverlayMode();
        origOpacity = series.getOpacity();
    }

    private void buildContent() {
        JPanel content = getContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // POC Section
        content.add(createSectionHeader("Point of Control (POC)"));
        addPOCEditors(content);

        // Value Area Section
        content.add(createSectionHeader("Value Area"));
        addValueAreaEditors(content);

        // VAH Section
        content.add(createSectionHeader("Value Area High (VAH)"));
        addVAHEditors(content);

        // VAL Section
        content.add(createSectionHeader("Value Area Low (VAL)"));
        addVALEditors(content);

        // Initial Balance Section
        content.add(createSectionHeader("Initial Balance"));
        addIBEditors(content);

        // Single Prints Section
        content.add(createSectionHeader("Single Prints"));
        addSinglePrintsEditors(content);

        // Display Section
        content.add(createSectionHeader("Display"));
        addDisplayEditors(content);
    }

    private void addPOCEditors(JPanel content) {
        // Show POC checkbox
        showPOCEditor = new BooleanPropertyEditor(series.isShowPOC());
        showPOCEditor.setOnValueChanged(value -> {
            series.setShowPOC(value);
            notifyPropertyChanged();
        });
        addRow(content, "Show POC", showPOCEditor.getComponent());

        // POC Color
        pocColorEditor = new ColorPropertyEditor(series.getPocColor());
        pocColorEditor.setOnValueChanged(value -> {
            series.setPocColor(value);
            notifyPropertyChanged();
        });
        addRow(content, "POC Color", pocColorEditor.getComponent());
    }

    private void addValueAreaEditors(JPanel content) {
        // Show Value Area checkbox
        showValueAreaEditor = new BooleanPropertyEditor(series.isShowValueArea());
        showValueAreaEditor.setOnValueChanged(value -> {
            series.setShowValueArea(value);
            notifyPropertyChanged();
        });
        addRow(content, "Show Value Area", showValueAreaEditor.getComponent());

        // Value Area Color
        valueAreaColorEditor = new ColorPropertyEditor(series.getValueAreaColor());
        valueAreaColorEditor.setOnValueChanged(value -> {
            series.setValueAreaColor(value);
            notifyPropertyChanged();
        });
        addRow(content, "Value Area Color", valueAreaColorEditor.getComponent());
    }

    private void addVAHEditors(JPanel content) {
        // Show VAH checkbox
        showVAHEditor = new BooleanPropertyEditor(series.isShowVAH());
        showVAHEditor.setOnValueChanged(value -> {
            series.setShowVAH(value);
            notifyPropertyChanged();
        });
        addRow(content, "Show VAH Line", showVAHEditor.getComponent());

        // VAH Color
        vahColorEditor = new ColorPropertyEditor(series.getVahColor());
        vahColorEditor.setOnValueChanged(value -> {
            series.setVahColor(value);
            notifyPropertyChanged();
        });
        addRow(content, "VAH Color", vahColorEditor.getComponent());

        // VAH Line Type
        vahLineTypeEditor = new EnumPropertyEditor<>(LineType.class, series.getVahLineType());
        vahLineTypeEditor.setOnValueChanged(value -> {
            series.setVahLineType(value);
            notifyPropertyChanged();
        });
        addRow(content, "Line Type", vahLineTypeEditor.getComponent());

        // VAH Line Thickness
        vahLineThicknessEditor = new EnumPropertyEditor<>(LineThickness.class, series.getVahLineThickness());
        vahLineThicknessEditor.setOnValueChanged(value -> {
            series.setVahLineThickness(value);
            notifyPropertyChanged();
        });
        addRow(content, "Line Thickness", vahLineThicknessEditor.getComponent());
    }

    private void addVALEditors(JPanel content) {
        // Show VAL checkbox
        showVALEditor = new BooleanPropertyEditor(series.isShowVAL());
        showVALEditor.setOnValueChanged(value -> {
            series.setShowVAL(value);
            notifyPropertyChanged();
        });
        addRow(content, "Show VAL Line", showVALEditor.getComponent());

        // VAL Color
        valColorEditor = new ColorPropertyEditor(series.getValColor());
        valColorEditor.setOnValueChanged(value -> {
            series.setValColor(value);
            notifyPropertyChanged();
        });
        addRow(content, "VAL Color", valColorEditor.getComponent());

        // VAL Line Type
        valLineTypeEditor = new EnumPropertyEditor<>(LineType.class, series.getValLineType());
        valLineTypeEditor.setOnValueChanged(value -> {
            series.setValLineType(value);
            notifyPropertyChanged();
        });
        addRow(content, "Line Type", valLineTypeEditor.getComponent());

        // VAL Line Thickness
        valLineThicknessEditor = new EnumPropertyEditor<>(LineThickness.class, series.getValLineThickness());
        valLineThicknessEditor.setOnValueChanged(value -> {
            series.setValLineThickness(value);
            notifyPropertyChanged();
        });
        addRow(content, "Line Thickness", valLineThicknessEditor.getComponent());
    }

    private void addIBEditors(JPanel content) {
        // Show Initial Balance checkbox
        showIBEditor = new BooleanPropertyEditor(series.isShowInitialBalance());
        showIBEditor.setOnValueChanged(value -> {
            series.setShowInitialBalance(value);
            notifyPropertyChanged();
        });
        addRow(content, "Show Initial Balance", showIBEditor.getComponent());

        // IB Color
        ibColorEditor = new ColorPropertyEditor(series.getIBColor());
        ibColorEditor.setOnValueChanged(value -> {
            series.setIBColor(value);
            notifyPropertyChanged();
        });
        addRow(content, "IB Color", ibColorEditor.getComponent());
    }

    private void addSinglePrintsEditors(JPanel content) {
        // Show Single Prints checkbox
        showSinglePrintsEditor = new BooleanPropertyEditor(series.isHighlightSinglePrints());
        showSinglePrintsEditor.setOnValueChanged(value -> {
            series.setHighlightSinglePrints(value);
            notifyPropertyChanged();
        });
        addRow(content, "Highlight Singles", showSinglePrintsEditor.getComponent());

        // Single Print Color
        singlePrintColorEditor = new ColorPropertyEditor(series.getSinglePrintColor());
        singlePrintColorEditor.setOnValueChanged(value -> {
            series.setSinglePrintColor(value);
            notifyPropertyChanged();
        });
        addRow(content, "Single Print Color", singlePrintColorEditor.getComponent());
    }

    private void addDisplayEditors(JPanel content) {
        // Overlay Mode checkbox
        overlayModeEditor = new BooleanPropertyEditor(series.isOverlayMode());
        overlayModeEditor.setOnValueChanged(value -> {
            series.setOverlayMode(value);
            notifyPropertyChanged();
        });
        addRow(content, "Overlay Mode", overlayModeEditor.getComponent());

        // Opacity slider (0.1 to 1.0)
        opacityEditor = new DoublePropertyEditor(0.1, 1.0, series.getOpacity(), 0.05);
        opacityEditor.setOnValueChanged(value -> {
            series.setOpacity(value.floatValue());
            notifyPropertyChanged();
        });
        addRow(content, "Opacity", opacityEditor.getComponent());
    }

    private void addRow(JPanel content, String label, JComponent component) {
        JPanel row = createPropertyRow(label, component);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        content.add(row);
    }

    private void notifyPropertyChanged() {
        if (onPropertyChanged != null) {
            onPropertyChanged.run();
        }
    }

    @Override
    protected void onReset() {
        // Reset to defaults
        series.setShowPOC(true);
        series.setPocColor(new Color(255, 193, 7));
        series.setShowValueArea(true);
        series.setValueAreaColor(new Color(100, 149, 237, 40));
        series.setShowVAH(false);
        series.setVahColor(new Color(0, 150, 255));
        series.setVahLineType(LineType.DASHED);
        series.setVahLineThickness(LineThickness.NORMAL);
        series.setShowVAL(false);
        series.setValColor(new Color(255, 100, 100));
        series.setValLineType(LineType.DASHED);
        series.setValLineThickness(LineThickness.NORMAL);
        series.setShowInitialBalance(true);
        series.setIBColor(new Color(255, 255, 255));
        series.setHighlightSinglePrints(true);
        series.setSinglePrintColor(new Color(255, 0, 0, 100));
        series.setOverlayMode(false);
        series.setOpacity(1.0f);

        // Update editor values
        showPOCEditor.setValue(true);
        pocColorEditor.setValue(new Color(255, 193, 7));
        showValueAreaEditor.setValue(true);
        valueAreaColorEditor.setValue(new Color(100, 149, 237, 40));
        showVAHEditor.setValue(false);
        vahColorEditor.setValue(new Color(0, 150, 255));
        vahLineTypeEditor.setValue(LineType.DASHED);
        vahLineThicknessEditor.setValue(LineThickness.NORMAL);
        showVALEditor.setValue(false);
        valColorEditor.setValue(new Color(255, 100, 100));
        valLineTypeEditor.setValue(LineType.DASHED);
        valLineThicknessEditor.setValue(LineThickness.NORMAL);
        showIBEditor.setValue(true);
        ibColorEditor.setValue(new Color(255, 255, 255));
        showSinglePrintsEditor.setValue(true);
        singlePrintColorEditor.setValue(new Color(255, 0, 0, 100));
        overlayModeEditor.setValue(false);
        opacityEditor.setValue(1.0);

        // Trigger repaint after reset
        notifyPropertyChanged();
    }

    /**
     * Returns the TPO series being edited.
     */
    public TPOSeries getTPOSeries() {
        return series;
    }
}
