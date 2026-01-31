package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.TimeAxis;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.chart.series.BandSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.style.BandSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing projected values with confidence bands.
 *
 * <p>Shows a line chart with confidence intervals, where:
 * <ul>
 *   <li>Main line shows the projected/forecast values</li>
 *   <li>Shaded band shows the confidence interval (e.g., 95% CI)</li>
 *   <li>Historical data transitions to forecast data</li>
 * </ul>
 */
public class ProjectedValuesDemo extends AbstractDemo {

    private Chart chart;

    private LineSeries series1Line;
    private LineSeries series2Line;
    private BandSeries confidenceBand;

    private static final Color SERIES_1_COLOR = new Color(66, 133, 244);    // Blue
    private static final Color SERIES_2_COLOR = new Color(234, 67, 53);     // Red
    private static final Color CONFIDENCE_FILL = new Color(234, 67, 53, 50); // Red with transparency

    public ProjectedValuesDemo() {
        setWindowSize(1200, 600);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Projected Values with Confidence Bands";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();
        chart.setXAxis(new TimeAxis());

        // Generate time series data with projections
        int historicalPoints = 30;
        int projectedPoints = 20;
        int totalPoints = historicalPoints + projectedPoints;

        XyData series1Data = new XyData("series1", "Series 1", totalPoints);
        XyData series2Data = new XyData("series2", "Series 2", totalPoints);
        XyyData confidenceData = new XyyData("confidence", "95% Confidence", projectedPoints);

        Random rand = new Random(42);
        long startTime = System.currentTimeMillis() - totalPoints * 7 * 24 * 60 * 60 * 1000L; // Weekly data
        long weekMs = 7 * 24 * 60 * 60 * 1000L;

        float value1 = 50;
        float value2 = 60;

        // Generate historical data
        for (int i = 0; i < historicalPoints; i++) {
            long timestamp = startTime + i * weekMs;

            // Series 1: Gradual upward trend with noise
            value1 = value1 + 1.5f + (rand.nextFloat() - 0.5f) * 5;
            series1Data.append(timestamp, value1);

            // Series 2: Stronger upward trend with noise
            value2 = value2 + 2.0f + (rand.nextFloat() - 0.5f) * 8;
            series2Data.append(timestamp, value2);
        }

        // Generate projected data with widening confidence interval
        float projectedValue2 = value2;
        for (int i = 0; i < projectedPoints; i++) {
            long timestamp = startTime + (historicalPoints + i) * weekMs;

            // Series 1: Continue trend (no confidence band for this series)
            value1 = value1 + 1.5f + (rand.nextFloat() - 0.5f) * 3;
            series1Data.append(timestamp, value1);

            // Series 2: Projected with widening confidence interval
            projectedValue2 = projectedValue2 + 2.0f;

            // Confidence interval widens as we project further
            float confidenceWidth = 5 + i * 2.5f;
            float upper = projectedValue2 + confidenceWidth;
            float lower = projectedValue2 - confidenceWidth;

            series2Data.append(timestamp, projectedValue2);
            confidenceData.append(timestamp, upper, projectedValue2, lower);
        }

        // Create confidence band (rendered first, behind the lines)
        confidenceBand = new BandSeries(confidenceData,
                new BandSeriesOptions()
                        .fillColor(CONFIDENCE_FILL)
                        .upperColor(null)  // No border lines
                        .lowerColor(null)
                        .middleColor(null)
                        .lineWidth(0f));

        // Create line series
        series1Line = new LineSeries(series1Data,
                new LineSeriesOptions()
                        .color(SERIES_1_COLOR)
                        .lineWidth(2f));

        series2Line = new LineSeries(series2Data,
                new LineSeriesOptions()
                        .color(SERIES_2_COLOR)
                        .lineWidth(2.5f));

        // Add series to chart (order matters for z-ordering)
        chart.addSeries(confidenceBand);
        chart.addSeries(series1Line);
        chart.addSeries(series2Line);

        // Fit viewport to show all data
        chart.zoomToFit();

        // Add legend
        chart.addLegendItem("series1", "Series 1", SERIES_1_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.LINE);
        chart.addLegendItem("series2", "Series 2", SERIES_2_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.LINE);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Projected Values - Test"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Series visibility toggles
        JCheckBox series1Check = new JCheckBox("Series 1", true);
        series1Check.setForeground(SERIES_1_COLOR);
        series1Check.addActionListener(e -> {
            series1Line.getOptions().visible(series1Check.isSelected());
            chart.repaint();
        });
        panel.add(series1Check);

        JCheckBox series2Check = new JCheckBox("Series 2", true);
        series2Check.setForeground(SERIES_2_COLOR);
        series2Check.addActionListener(e -> {
            series2Line.getOptions().visible(series2Check.isSelected());
            chart.repaint();
        });
        panel.add(series2Check);

        // Confidence band toggle
        JCheckBox confidenceCheck = new JCheckBox("95% Confidence", true);
        confidenceCheck.setForeground(Color.WHITE);
        confidenceCheck.addActionListener(e -> {
            confidenceBand.getOptions().visible(confidenceCheck.isSelected());
            chart.repaint();
        });
        panel.add(confidenceCheck);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Grid toggle
        panel.add(DemoUIHelper.createCheckBox("Grid", true, e -> {
            chart.setGridEnabled(((JCheckBox) e.getSource()).isSelected());
        }));

        // Crosshair toggle
        panel.add(DemoUIHelper.createCheckBox("Crosshair", true, e -> {
            chart.setCrosshairEnabled(((JCheckBox) e.getSource()).isSelected());
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
        launch(new ProjectedValuesDemo(), args);
    }
}
