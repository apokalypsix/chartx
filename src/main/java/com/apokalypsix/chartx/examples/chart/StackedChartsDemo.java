package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.data.StackedSeriesGroup;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.series.StackedColumnSeries;
import com.apokalypsix.chartx.chart.series.StackedMountainSeries;
import com.apokalypsix.chartx.chart.style.StackedSeriesOptions;
import com.apokalypsix.chartx.core.data.StackingCalculator;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Stacked chart types.
 */
public class StackedChartsDemo extends AbstractDemo {

    private Chart chart;

    private StackedSeriesGroup stackedMountainData;
    private StackedSeriesGroup stackedColumnData;

    private StackedMountainSeries stackedMountainSeries;
    private StackedColumnSeries stackedColumnSeries;

    private int currentChart = 0; // 0=stacked mountain, 1=stacked column
    private boolean percent100 = false;

    // Category labels for the data points
    private static final int NUM_POINTS = 50;
    private static final String[] CATEGORIES = generateCategoryLabels(NUM_POINTS);

    private static String[] generateCategoryLabels(int count) {
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
            labels[i] = "Q" + ((i / 4) + 1) + "-W" + ((i % 4) + 1);
        }
        return labels;
    }

    public StackedChartsDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Stacked Charts";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Set up category axis with period labels
        chart.setXAxis(new CategoryAxis(CategoryAxis.DEFAULT_AXIS_ID, CategoryAxis.Position.BOTTOM));
        CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
        categoryAxis.setCategories(CATEGORIES);
        categoryAxis.height(40);
        categoryAxis.labelColor(new Color(180, 180, 180));

        // Create stacked data
        stackedMountainData = createStackedData("mountain", "Revenue by Region", NUM_POINTS);
        stackedColumnData = createStackedData("column", "Sales by Category", NUM_POINTS);

        // Set visible range for category count (with padding)
        chart.setVisibleRange(-1, NUM_POINTS);
        chart.setBarDuration(1);

        // Create series with options
        stackedMountainSeries = new StackedMountainSeries(stackedMountainData,
                new StackedSeriesOptions()
                        .fillOpacity(0.85f)
                        .lineWidth(1.0f)
                        .showLines(true));

        stackedColumnSeries = new StackedColumnSeries(stackedColumnData,
                new StackedSeriesOptions()
                        .fillOpacity(0.9f)
                        .lineWidth(1.0f)
                        .showLines(true)
                        .visible(false));
        stackedColumnSeries.barWidthRatio(0.7f);

        // Add series to chart
        chart.addSeries(stackedMountainSeries);
        chart.addSeries(stackedColumnSeries);

        return chart;
    }

    private StackedSeriesGroup createStackedData(String id, String name, int points) {
        StackedSeriesGroup group = new StackedSeriesGroup(id, name);
        Random rand = new Random(42);

        String[] regions = {"North", "South", "East", "West", "Central"};
        Color[] colors = {
            new Color(65, 131, 196),   // Blue
            new Color(46, 204, 113),   // Green
            new Color(231, 76, 60),    // Red
            new Color(155, 89, 182),   // Purple
            new Color(241, 196, 15)    // Yellow
        };

        for (int r = 0; r < regions.length; r++) {
            XyData series = new XyData(regions[r].toLowerCase(), regions[r], points);
            float baseValue = 50 + rand.nextFloat() * 100;

            for (int i = 0; i < points; i++) {
                float value = baseValue + rand.nextFloat() * 30 - 15;
                value = Math.max(10, value);
                series.append(i, value);  // Use category index as X position
                baseValue = value;
            }

            group.addSeries(series, colors[r % colors.length]);
        }

        return group;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Chart Type:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        ButtonGroup group = new ButtonGroup();

        JRadioButton mountainBtn = new JRadioButton("Stacked Mountain", true);
        mountainBtn.setForeground(Color.WHITE);
        mountainBtn.addActionListener(e -> {
            currentChart = 0;
            updateVisibility();
        });
        group.add(mountainBtn);
        panel.add(mountainBtn);

        JRadioButton columnBtn = new JRadioButton("Stacked Column", false);
        columnBtn.setForeground(Color.WHITE);
        columnBtn.addActionListener(e -> {
            currentChart = 1;
            updateVisibility();
        });
        group.add(columnBtn);
        panel.add(columnBtn);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // 100% stacked toggle
        JCheckBox percent100Box = new JCheckBox("100% Stacked");
        percent100Box.setForeground(Color.WHITE);
        percent100Box.addActionListener(e -> {
            percent100 = percent100Box.isSelected();
            if (percent100) {
                stackedMountainSeries.getOptions().percent100();
                stackedColumnSeries.getOptions().percent100();
            } else {
                stackedMountainSeries.getOptions().stackMode(StackingCalculator.StackMode.NORMAL);
                stackedColumnSeries.getOptions().stackMode(StackingCalculator.StackMode.NORMAL);
            }
            chart.repaint();
        });
        panel.add(percent100Box);

        return panel;
    }

    private void updateVisibility() {
        stackedMountainSeries.getOptions().visible(currentChart == 0);
        stackedColumnSeries.getOptions().visible(currentChart == 1);
        chart.repaint();
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new StackedChartsDemo(), args);
    }
}
