package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.GroupedColumnData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.series.GroupedColumnSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.style.GroupedColumnSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing dual axis charts with grouped columns and a line overlay.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Grouped column chart with multiple series</li>
 *   <li>Dual Y-axis (left for bars, right for line)</li>
 *   <li>Multiple data series with different scales</li>
 *   <li>Category-based x-axis</li>
 * </ul>
 */
public class DualAxisDemo extends AbstractDemo {

    private Chart chart;

    private GroupedColumnSeries columnSeries;
    private LineSeries lineSeries;

    private static final Color S1_COLOR = new Color(231, 76, 60);     // Red
    private static final Color S2_COLOR = new Color(52, 152, 219);    // Blue
    private static final Color S3_COLOR = new Color(46, 204, 113);    // Green
    private static final Color S4_COLOR = new Color(241, 196, 15);    // Yellow (line)

    public DualAxisDemo() {
        setWindowSize(1200, 700);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Dual Axis Chart";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Create grouped column data (3 series per category)
        String[] categories = {"Category 1", "Category 2", "Category 3", "Category 4",
                               "Category 5", "Category 6", "Category 7", "Category 8"};
        String[] seriesNames = {"S1", "S2", "S3"};

        // Set up category axis
        chart.setXAxis(new CategoryAxis(CategoryAxis.DEFAULT_AXIS_ID, CategoryAxis.Position.BOTTOM));
        CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
        categoryAxis.setCategories(categories);
        categoryAxis.height(40);
        categoryAxis.labelColor(new Color(180, 180, 180));

        GroupedColumnData columnData = new GroupedColumnData("sales", "Sales by Category", seriesNames);

        Random rand = new Random(42);

        // Generate bar data using category index
        for (int i = 0; i < categories.length; i++) {
            float s1 = 10 + rand.nextFloat() * 50;
            float s2 = 15 + rand.nextFloat() * 40;
            float s3 = 5 + rand.nextFloat() * 35;
            columnData.append(new float[]{s1, s2, s3}, categories[i]);
        }

        // Create line data for S4 (different scale - 0-100 on right axis)
        XyData lineData = new XyData("s4", "S4", categories.length);
        for (int i = 0; i < categories.length; i++) {
            float value = 20 + rand.nextFloat() * 60;
            lineData.append(i, value);
        }

        // Configure visible range for categories
        chart.setVisibleRange(-1, categories.length);
        chart.setBarDuration(1);

        // Create grouped column series
        GroupedColumnSeriesOptions columnOptions = new GroupedColumnSeriesOptions(3);
        columnOptions.groupColor(0, S1_COLOR);
        columnOptions.groupColor(1, S2_COLOR);
        columnOptions.groupColor(2, S3_COLOR);
        columnOptions.groupWidthRatio(0.7f);
        columnOptions.barSpacing(0.1f);

        columnSeries = new GroupedColumnSeries(columnData, columnOptions);

        // Create secondary Y-axis on the right
        YAxis rightAxis = chart.createYAxis("right", YAxis.Position.RIGHT);
        rightAxis.setValueRange(0, 100);
        rightAxis.setAutoScale(false);

        // Create line series on right axis
        lineSeries = new LineSeries(lineData,
                new LineSeriesOptions()
                        .color(S4_COLOR)
                        .lineWidth(3f));

        // Add series to chart
        chart.addSeries(columnSeries);
        chart.addSeries(lineSeries);

        // Associate line data with right axis
        chart.setDataAxis(lineData, "right");

        // Add legend
        chart.addLegendItem("s1", "S1", S1_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.GROUPED_COLUMN);
        chart.addLegendItem("s2", "S2", S2_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.GROUPED_COLUMN);
        chart.addLegendItem("s3", "S3", S3_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.GROUPED_COLUMN);
        chart.addLegendItem("s4", "S4", S4_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.LINE);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Dual Axis Chart - Bars (left axis) + Line (right axis)"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Series visibility toggles
        JCheckBox barsCheck = new JCheckBox("Show Bars", true);
        barsCheck.setForeground(Color.WHITE);
        barsCheck.addActionListener(e -> {
            columnSeries.getOptions().visible(barsCheck.isSelected());
            chart.repaint();
        });
        panel.add(barsCheck);

        JCheckBox lineCheck = new JCheckBox("Show Line (S4)", true);
        lineCheck.setForeground(S4_COLOR);
        lineCheck.addActionListener(e -> {
            lineSeries.getOptions().visible(lineCheck.isSelected());
            chart.repaint();
        });
        panel.add(lineCheck);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Bar width slider
        panel.add(DemoUIHelper.createControlLabel("Bar Width:"));
        JSlider widthSlider = new JSlider(30, 90, 70);
        widthSlider.addChangeListener(e -> {
            float ratio = widthSlider.getValue() / 100f;
            columnSeries.getOptions().groupWidthRatio(ratio);
            chart.repaint();
        });
        panel.add(widthSlider);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Grid toggle
        panel.add(DemoUIHelper.createCheckBox("Grid", true, e -> {
            chart.setGridEnabled(((JCheckBox) e.getSource()).isSelected());
        }));

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new DualAxisDemo(), args);
    }
}
