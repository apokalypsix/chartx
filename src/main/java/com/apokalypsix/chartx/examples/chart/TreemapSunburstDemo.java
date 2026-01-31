package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.apokalypsix.chartx.chart.VisualizationPanel;
import com.apokalypsix.chartx.chart.data.TreemapData;
import com.apokalypsix.chartx.chart.series.SunburstSeries;
import com.apokalypsix.chartx.chart.series.TreemapSeries;
import com.apokalypsix.chartx.chart.series.VisualizationGroup;
import com.apokalypsix.chartx.chart.style.SunburstSeriesOptions;
import com.apokalypsix.chartx.chart.style.TreemapSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Treemap and Sunburst charts for hierarchical data.
 */
public class TreemapSunburstDemo extends AbstractDemo {

    private VisualizationPanel vizPanel;
    private TreemapData treeData;
    private VisualizationGroup vizGroup;

    public TreemapSunburstDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Treemap & Sunburst";
    }

    @Override
    protected Component createDemoContent() {
        // Create hierarchical data (simulating file system or portfolio)
        treeData = createSampleData();

        // Assign colors
        Color[] baseColors = {
            new Color(65, 131, 196),   // Blue
            new Color(46, 204, 113),   // Green
            new Color(231, 76, 60),    // Red
            new Color(155, 89, 182),   // Purple
            new Color(241, 196, 15),   // Yellow
            new Color(230, 126, 34)    // Orange
        };
        treeData.assignHierarchicalColors(baseColors);

        // Create series
        TreemapSeries treemapSeries = new TreemapSeries(treeData,
                new TreemapSeriesOptions()
                        .padding(3f)
                        .borderWidth(1.5f)
                        .showLabels(true)
                        .minLabelCellSize(50));

        SunburstSeries sunburstSeries = new SunburstSeries(treeData,
                new SunburstSeriesOptions()
                        .innerRadiusRatio(0.15f)
                        .ringGap(2f)
                        .segmentGap(0.5f)
                        .maxVisibleDepth(4));

        // Create visualization group
        vizGroup = new VisualizationGroup();
        vizGroup.add("Treemap", treemapSeries);
        vizGroup.add("Sunburst", sunburstSeries);

        // Create backend-agnostic visualization panel
        vizPanel = createVisualizationPanel();
        vizPanel.setRenderable(vizGroup);
        vizPanel.setPadding(40);

        return vizPanel.getDisplayComponent();
    }

    private TreemapData createSampleData() {
        TreemapData data = new TreemapData("portfolio", "Investment Portfolio");

        // Technology sector
        TreemapData.Node tech = data.addNode("tech", "Technology", 0);
        tech.addChild("aapl", "Apple", 2800);
        tech.addChild("msft", "Microsoft", 2500);
        tech.addChild("googl", "Google", 1800);
        TreemapData.Node chips = tech.addChild("chips", "Semiconductors", 0);
        chips.addChild("nvda", "NVIDIA", 1200);
        chips.addChild("amd", "AMD", 400);
        chips.addChild("intc", "Intel", 300);

        // Healthcare sector
        TreemapData.Node health = data.addNode("health", "Healthcare", 0);
        health.addChild("jnj", "Johnson & Johnson", 900);
        health.addChild("unh", "UnitedHealth", 800);
        health.addChild("pfe", "Pfizer", 500);
        health.addChild("abbv", "AbbVie", 450);

        // Finance sector
        TreemapData.Node finance = data.addNode("finance", "Finance", 0);
        finance.addChild("jpm", "JPMorgan", 700);
        finance.addChild("bac", "Bank of America", 500);
        finance.addChild("v", "Visa", 600);
        finance.addChild("ma", "Mastercard", 550);

        // Consumer sector
        TreemapData.Node consumer = data.addNode("consumer", "Consumer", 0);
        consumer.addChild("amzn", "Amazon", 1500);
        consumer.addChild("wmt", "Walmart", 400);
        consumer.addChild("hd", "Home Depot", 350);

        // Energy sector
        TreemapData.Node energy = data.addNode("energy", "Energy", 0);
        energy.addChild("xom", "Exxon", 450);
        energy.addChild("cvx", "Chevron", 400);

        return data;
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
                vizGroup.invalidateLayout();
                vizPanel.repaint();
            });
            group.add(btn);
            panel.add(btn);
        }

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Max depth control
        panel.add(DemoUIHelper.createControlLabel("Max Depth:"));
        JComboBox<String> depthCombo = DemoUIHelper.createComboBox("All", "1", "2", "3", "4");
        depthCombo.addActionListener(e -> {
            int depth = depthCombo.getSelectedIndex() == 0 ? 0 : depthCombo.getSelectedIndex();
            ((TreemapSeries) vizGroup.get("Treemap")).getOptions().maxVisibleDepth(depth);
            ((SunburstSeries) vizGroup.get("Sunburst")).getOptions().maxVisibleDepth(depth);
            vizGroup.invalidateLayout();
            vizPanel.repaint();
        });
        panel.add(depthCombo);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Labels toggle
        JCheckBox labelsBox = new JCheckBox("Labels", true);
        labelsBox.setForeground(Color.WHITE);
        labelsBox.addActionListener(e -> {
            boolean show = labelsBox.isSelected();
            ((TreemapSeries) vizGroup.get("Treemap")).getOptions().showLabels(show);
            ((SunburstSeries) vizGroup.get("Sunburst")).getOptions().showLabels(show);
            vizPanel.repaint();
        });
        panel.add(labelsBox);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (vizPanel != null) {
            vizPanel.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new TreemapSunburstDemo(), args);
    }
}
