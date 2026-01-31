package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.data.BoxWhiskerData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.chart.series.BoxPlotSeries;
import com.apokalypsix.chartx.chart.series.ErrorBarSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.style.BoxPlotSeriesOptions;
import com.apokalypsix.chartx.chart.style.ErrorBarSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Box Plot and Error Bar chart types.
 */
public class StatisticalDemo extends AbstractDemo {

    private Chart chart;

    private BoxWhiskerData boxPlotData;
    private XyData         lineData;
    private XyyData        errorBarData;

    private BoxPlotSeries  boxPlotSeries;
    private LineSeries     lineSeries;
    private ErrorBarSeries errorBarSeries;

    private int currentChart = 0; // 0=box plot, 1=error bars

    // Category labels for box plot (experiments)
    private static final String[] BOX_PLOT_CATEGORIES =
        {
            "Exp 1",
            "Exp 2",
            "Exp 3",
            "Exp 4",
            "Exp 5",
            "Exp 6",
            "Exp 7",
            "Exp 8",
            "Exp 9",
            "Exp 10",
            "Exp 11",
            "Exp 12",
            "Exp 13",
            "Exp 14",
            "Exp 15",
            "Exp 16",
            "Exp 17",
            "Exp 18",
            "Exp 19",
            "Exp 20"
        };

    // Category labels for error bars (measurements)
    private static final String[] ERROR_BAR_CATEGORIES = new String[30];
    static {
        for (int i = 0; i < 30; i++) {
            ERROR_BAR_CATEGORIES[i] = "M" + (i + 1);
        }
    }

    public StatisticalDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Statistical Charts";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Set up category axis for box plot initially
        chart.setXAxis(
            new CategoryAxis(CategoryAxis.DEFAULT_AXIS_ID, CategoryAxis.Position.BOTTOM));
        CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
        categoryAxis.setCategories(BOX_PLOT_CATEGORIES);
        categoryAxis.labelColor(new Color(180, 180, 180));

        // Create box plot data (distribution per category)
        boxPlotData = createBoxPlotData();

        // Create line with error bars data
        createLineWithErrorBars();

        // Create series
        boxPlotSeries =
            new BoxPlotSeries(
                boxPlotData,
                new BoxPlotSeriesOptions()
                    .boxColor(new Color(65, 131, 196, 180))
                    .medianColor(new Color(241, 196, 15))
                    .borderColor(new Color(200, 200, 200))
                    .boxWidthRatio(0.6f)
                    .whiskerCapRatio(0.3f));

        lineSeries =
            new LineSeries(
                lineData,
                new LineSeriesOptions()
                    .color(new Color(46, 204, 113))
                    .lineWidth(2f)
                    .visible(false));

        errorBarSeries =
            new ErrorBarSeries(
                errorBarData,
                new ErrorBarSeriesOptions()
                    .color(new Color(231, 76, 60))
                    .lineWidth(1.5f)
                    .capWidth(8f)
                    .visible(false));

        // Add series to chart
        chart.addSeries(boxPlotSeries);
        chart.addSeries(lineSeries);
        chart.addSeries(errorBarSeries);

        // Configure viewport from actual data ranges
        fitViewportToData();

        return chart;
    }

    private void fitViewportToData() {
        // Use category indices for X range (with padding for half a category on
        // each side)
        int categoryCount =
            currentChart == 0 ? BOX_PLOT_CATEGORIES.length : ERROR_BAR_CATEGORIES.length;
        chart.setVisibleRange(-1, categoryCount);

        float minVal = boxPlotData.findMinValue(0, boxPlotData.size() - 1);
        float maxVal = boxPlotData.findMaxValue(0, boxPlotData.size() - 1);

        // Also consider error bar data range
        for (int i = 0; i < errorBarData.size(); i++) {
            float upper = errorBarData.getUpper(i);
            float lower = errorBarData.getLower(i);
            if (upper > maxVal)
                maxVal = upper;
            if (lower < minVal)
                minVal = lower;
        }

        float valuePadding = (maxVal - minVal) * 0.05f;
        double yMin = minVal - valuePadding;
        double yMax = maxVal + valuePadding;

        // Set Y-axis range
        chart.getYAxis().setVisibleRange(yMin, yMax);
    }

    private BoxWhiskerData createBoxPlotData() {
        BoxWhiskerData data =
            new BoxWhiskerData("experiment", "Experiment Results", BOX_PLOT_CATEGORIES.length);
        Random rand = new Random(42);

        for (int i = 0; i < BOX_PLOT_CATEGORIES.length; i++) {
            // Generate random sample data
            float[] samples = new float[50];
            float mean = 50 + rand.nextFloat() * 40;
            float stdDev = 5 + rand.nextFloat() * 10;

            for (int j = 0; j < samples.length; j++) {
                samples[j] = mean + (float) rand.nextGaussian() * stdDev;
            }

            // Sort and calculate quartiles
            Arrays.sort(samples);
            float min = samples[0];
            float q1 = samples[12]; // 25th percentile
            float median = samples[25]; // 50th percentile
            float q3 = samples[37]; // 75th percentile
            float max = samples[49];

            // Use category label
            data.append(min, q1, median, q3, max, BOX_PLOT_CATEGORIES[i]);
        }

        return data;
    }

    private void createLineWithErrorBars() {
        int points = ERROR_BAR_CATEGORIES.length;
        lineData = new XyData("measurement", "Measurements", points);
        errorBarData = new XyyData("errors", "Error Bars", points);

        Random rand = new Random(123);
        float baseValue = 50;

        for (int i = 0; i < points; i++) {
            // Create measurement with trend
            float value =
                baseValue + (float) Math.sin(i * 0.3) * 15 + (rand.nextFloat() - 0.5f) * 5;
            float error = 3 + rand.nextFloat() * 7; // Variable error

            // Use category index as X value
            lineData.append(i, value);
            errorBarData.append(i, value + error, value, value - error);
        }
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Chart Type:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        ButtonGroup group = new ButtonGroup();

        JRadioButton boxBtn = new JRadioButton("Box Plot", true);
        boxBtn.setForeground(Color.WHITE);
        boxBtn.addActionListener(e -> {
            currentChart = 0;
            updateVisibility();
        });
        group.add(boxBtn);
        panel.add(boxBtn);

        JRadioButton errorBtn = new JRadioButton("Line with Error Bars", false);
        errorBtn.setForeground(Color.WHITE);
        errorBtn.addActionListener(e -> {
            currentChart = 1;
            updateVisibility();
        });
        group.add(errorBtn);
        panel.add(errorBtn);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Show mean toggle for box plot
        JCheckBox meanBox = new JCheckBox("Show Mean");
        meanBox.setForeground(Color.WHITE);
        meanBox.setSelected(false);
        meanBox.addActionListener(e -> {
            boxPlotSeries.getOptions().showMean(meanBox.isSelected());
            chart.repaint();
        });
        panel.add(meanBox);

        return panel;
    }

    private void updateVisibility() {
        boxPlotSeries.getOptions().visible(currentChart == 0);
        lineSeries.getOptions().visible(currentChart == 1);
        errorBarSeries.getOptions().visible(currentChart == 1);

        // Switch category labels based on active chart
        CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
        if (currentChart == 0) {
            categoryAxis.setCategories(BOX_PLOT_CATEGORIES);
        } else {
            categoryAxis.setCategories(ERROR_BAR_CATEGORIES);
        }

        // Refit viewport for new data
        fitViewportToData();
        chart.repaint();
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new StatisticalDemo(), args);
    }
}
