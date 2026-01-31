package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.VisualizationPanel;
import com.apokalypsix.chartx.chart.data.PieData;
import com.apokalypsix.chartx.chart.data.RadarData;
import com.apokalypsix.chartx.chart.series.PieSeries;
import com.apokalypsix.chartx.chart.series.RadarSeries;
import com.apokalypsix.chartx.chart.series.VisualizationGroup;
import com.apokalypsix.chartx.chart.style.PieSeriesOptions;
import com.apokalypsix.chartx.chart.style.RadarSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Pie, Donut, and Radar/Spider charts.
 */
public class PiePolarDemo extends AbstractDemo {

    private VisualizationPanel vizPanel;
    private VisualizationGroup vizGroup;

    public PiePolarDemo() {
        setWindowSize(1000, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Pie & Polar Charts";
    }

    @Override
    protected Component createDemoContent() {
        // Create pie data (market share)
        PieData pieData = new PieData("market", "Browser Market Share");
        pieData.append("Chrome", 65.0f);
        pieData.append("Safari", 18.0f);
        pieData.append("Firefox", 8.0f);
        pieData.append("Edge", 5.0f);
        pieData.append("Other", 4.0f);

        // Create radar data (product comparison)
        String[] axes = {"Performance", "Reliability", "Ease of Use", "Features", "Price", "Support"};
        RadarData radarData = new RadarData("comparison", "Product Comparison", axes);
        radarData.addSeries("Product A", new float[]{85, 90, 70, 80, 60, 85});
        radarData.addSeries("Product B", new float[]{70, 75, 90, 85, 80, 70});
        radarData.addSeries("Product C", new float[]{60, 80, 85, 70, 95, 75});

        // Create series with colors
        Color[] pieColors = {
            new Color(66, 133, 244),   // Chrome blue
            new Color(52, 168, 83),    // Safari green
            new Color(251, 188, 5),    // Firefox orange
            new Color(0, 120, 215),    // Edge blue
            new Color(150, 150, 150)   // Other gray
        };

        PieSeries pieSeries = new PieSeries(pieData,
                new PieSeriesOptions()
                        .sliceColors(pieColors)
                        .defaultExplodeOffset(0));

        PieSeries donutSeries = new PieSeries(pieData,
                new PieSeriesOptions()
                        .sliceColors(pieColors)
                        .innerRadiusRatio(0.5f));

        Color[] radarColors = {
            new Color(66, 133, 244, 180),
            new Color(234, 67, 53, 180),
            new Color(52, 168, 83, 180)
        };

        RadarSeries radarSeries = new RadarSeries(radarData,
                new RadarSeriesOptions()
                        .seriesColors(radarColors)
                        .showGrid(true)
                        .gridLevels(5)
                        .showFill(true)
                        .showSpokes(true));

        // Create visualization group
        vizGroup = new VisualizationGroup();
        vizGroup.add("Pie", pieSeries);
        vizGroup.add("Donut", donutSeries);
        vizGroup.add("Radar/Spider", radarSeries);

        // Create backend-agnostic visualization panel
        vizPanel = createVisualizationPanel();
        vizPanel.setRenderable(vizGroup);
        vizPanel.setPadding(60);

        return vizPanel.getDisplayComponent();
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Chart Type:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        ButtonGroup group = new ButtonGroup();
        List<String> names = vizGroup.getNames();

        for (int i = 0; i < names.size(); i++) {
            final int idx = i;
            JRadioButton btn = new JRadioButton(names.get(i), i == 0);
            btn.setForeground(Color.WHITE);
            btn.addActionListener(e -> {
                vizGroup.setActive(idx);
                vizPanel.repaint();
            });
            group.add(btn);
            panel.add(btn);
        }

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Explode control for pie
        panel.add(DemoUIHelper.createControlLabel("Explode:"));
        JSlider explodeSlider = new JSlider(0, 30, 0);
        explodeSlider.addChangeListener(e -> {
            float explode = explodeSlider.getValue();
            ((PieSeries) vizGroup.get("Pie")).getOptions().defaultExplodeOffset(explode);
            ((PieSeries) vizGroup.get("Donut")).getOptions().defaultExplodeOffset(explode);
            vizPanel.repaint();
        });
        panel.add(explodeSlider);

        // Fill toggle for radar
        JCheckBox fillBox = new JCheckBox("Fill Areas", true);
        fillBox.setForeground(Color.WHITE);
        fillBox.addActionListener(e -> {
            ((RadarSeries) vizGroup.get("Radar/Spider")).getOptions().showFill(fillBox.isSelected());
            vizPanel.repaint();
        });
        panel.add(fillBox);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (vizPanel != null) {
            vizPanel.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new PiePolarDemo(), args);
    }
}
