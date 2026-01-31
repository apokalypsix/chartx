package com.apokalypsix.chartx.examples.chart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.EMA;
import com.apokalypsix.chartx.chart.interaction.ChartModifier;
import com.apokalypsix.chartx.chart.interaction.ChartModifierGroup;
import com.apokalypsix.chartx.chart.interaction.modifier.DataPointSelectionModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.MouseWheelZoomModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.RolloverModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.RubberBandXyZoomModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.XAxisDragModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.YAxisDragModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.ZoomExtentsModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.ZoomPanModifier;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo for the ChartModifier system.
 *
 * <p>This demo showcases the modifier architecture by allowing users to
 * dynamically enable and disable individual modifiers. Each modifier provides
 * different interaction behavior:
 *
 * <ul>
 *   <li><b>ZoomPanModifier</b> - Drag to pan the chart</li>
 *   <li><b>MouseWheelZoomModifier</b> - Scroll wheel to zoom</li>
 *   <li><b>RolloverModifier</b> - Crosshair with data tooltips</li>
 *   <li><b>XAxisDragModifier</b> - Drag on X-axis to pan/zoom time</li>
 *   <li><b>YAxisDragModifier</b> - Drag on Y-axis to pan/zoom price</li>
 *   <li><b>RubberBandXyZoomModifier</b> - Shift+drag to box zoom</li>
 *   <li><b>ZoomExtentsModifier</b> - Double-click to fit all data</li>
 *   <li><b>DataPointSelectionModifier</b> - Click to select bars</li>
 * </ul>
 *
 * <p>This demo can be run with any rendering backend by using the
 * appropriate launcher from the opengl/, vulkan/, or metal/ packages.
 */
public class ModifierDemo extends AbstractDemo {

    private FinanceChart chart;
    private OhlcData data;

    /** Map of modifier name to modifier instance */
    private final Map<String, ChartModifier> availableModifiers = new LinkedHashMap<>();

    /** Map of modifier name to checkbox */
    private final Map<String, JCheckBox> modifierCheckboxes = new LinkedHashMap<>();

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Modifier System";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate sample data
        data = DemoDataGenerator.generateOHLCData();
        chart.addCandlestickSeries(data, new OhlcSeriesOptions());
        chart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Add EMA indicators for visual reference
        XyData ema20 = EMA.calculate(data, 20);
        chart.addLineSeries(ema20, new LineSeriesOptions()
                .color(DemoConfig.EMA_20_COLOR)
                .lineWidth(DemoConfig.LINE_WIDTH_STANDARD));

        // Clear default modifiers and set up our controlled set
        setupModifiers();

        return chart;
    }

    /**
     * Sets up all available modifiers.
     */
    private void setupModifiers() {
        ChartModifierGroup group = chart.getModifiers();
        group.clear();

        // Create all modifiers
        ZoomPanModifier zoomPan = new ZoomPanModifier();
        MouseWheelZoomModifier wheelZoom = new MouseWheelZoomModifier();
        RolloverModifier rollover = new RolloverModifier();
        XAxisDragModifier xAxisDrag = new XAxisDragModifier();
        YAxisDragModifier yAxisDrag = new YAxisDragModifier();
        RubberBandXyZoomModifier rubberBand = new RubberBandXyZoomModifier();
        ZoomExtentsModifier zoomExtents = new ZoomExtentsModifier();
        DataPointSelectionModifier selection = new DataPointSelectionModifier();

        // Configure selection modifier with data
        selection.setData(data);
        selection.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Store in map for checkbox binding
        availableModifiers.put("ZoomPan (drag)", zoomPan);
        availableModifiers.put("MouseWheel Zoom", wheelZoom);
        availableModifiers.put("Rollover (crosshair)", rollover);
        availableModifiers.put("X-Axis Drag", xAxisDrag);
        availableModifiers.put("Y-Axis Drag", yAxisDrag);
        availableModifiers.put("RubberBand Zoom (Shift+drag)", rubberBand);
        availableModifiers.put("Zoom Extents (double-click)", zoomExtents);
        availableModifiers.put("Data Point Selection", selection);

        // Add all modifiers to the group
        for (ChartModifier modifier : availableModifiers.values()) {
            group.add(modifier);
        }

        // Enable default modifiers
        zoomPan.setEnabled(true);
        wheelZoom.setEnabled(true);
        rollover.setEnabled(true);
        xAxisDrag.setEnabled(false);
        yAxisDrag.setEnabled(false);
        rubberBand.setEnabled(false);
        zoomExtents.setEnabled(true);
        selection.setEnabled(false);
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DemoConfig.PANEL_BACKGROUND);

        // Info panel at top
        JPanel infoPanel = DemoUIHelper.createControlPanel();
        infoPanel.add(DemoUIHelper.createInfoLabel(
                "Toggle modifiers below to change chart interactions"));
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // Modifier toggles panel
        JPanel modifiersPanel = new JPanel(new GridLayout(2, 4, 10, 5));
        modifiersPanel.setBackground(DemoConfig.PANEL_BACKGROUND);
        modifiersPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create checkbox for each modifier
        for (Map.Entry<String, ChartModifier> entry : availableModifiers.entrySet()) {
            String name = entry.getKey();
            ChartModifier modifier = entry.getValue();

            JCheckBox checkBox = DemoUIHelper.createCheckBox(name, modifier.isEnabled(), e -> {
                JCheckBox cb = (JCheckBox) e.getSource();
                modifier.setEnabled(cb.isSelected());
                chart.repaint();
            });

            modifierCheckboxes.put(name, checkBox);
            modifiersPanel.add(checkBox);
        }

        mainPanel.add(modifiersPanel, BorderLayout.CENTER);

        // Button panel at bottom
        JPanel buttonPanel = DemoUIHelper.createControlPanel();

        buttonPanel.add(DemoUIHelper.createButton("Enable All", e -> {
            for (Map.Entry<String, ChartModifier> entry : availableModifiers.entrySet()) {
                entry.getValue().setEnabled(true);
                modifierCheckboxes.get(entry.getKey()).setSelected(true);
            }
            chart.repaint();
        }));

        buttonPanel.add(DemoUIHelper.createButton("Disable All", e -> {
            for (Map.Entry<String, ChartModifier> entry : availableModifiers.entrySet()) {
                entry.getValue().setEnabled(false);
                modifierCheckboxes.get(entry.getKey()).setSelected(false);
            }
            chart.repaint();
        }));

        buttonPanel.add(DemoUIHelper.createButton("Reset Defaults", e -> {
            resetToDefaults();
            chart.repaint();
        }));

        buttonPanel.add(DemoUIHelper.createHorizontalSpacer());

        buttonPanel.add(DemoUIHelper.createButton("Zoom to Fit", e -> chart.zoomToFit()));

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Resets modifiers to default state.
     */
    private void resetToDefaults() {
        // Default enabled: ZoomPan, MouseWheel, Rollover, ZoomExtents
        setModifierEnabled("ZoomPan (drag)", true);
        setModifierEnabled("MouseWheel Zoom", true);
        setModifierEnabled("Rollover (crosshair)", true);
        setModifierEnabled("X-Axis Drag", false);
        setModifierEnabled("Y-Axis Drag", false);
        setModifierEnabled("RubberBand Zoom (Shift+drag)", false);
        setModifierEnabled("Zoom Extents (double-click)", true);
        setModifierEnabled("Data Point Selection", false);
    }

    /**
     * Sets a modifier's enabled state and updates the UI.
     */
    private void setModifierEnabled(String name, boolean enabled) {
        ChartModifier modifier = availableModifiers.get(name);
        JCheckBox checkBox = modifierCheckboxes.get(name);
        if (modifier != null && checkBox != null) {
            modifier.setEnabled(enabled);
            checkBox.setSelected(enabled);
        }
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new ModifierDemo(), args);
    }
}
