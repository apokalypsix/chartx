package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.TimeAxis;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.series.ScatterSeries;
import com.apokalypsix.chartx.chart.style.ScatterSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing scatter plots with multiple data series.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Multiple scatter series with different colors</li>
 *   <li>Various marker shapes (circle, square, triangle, diamond)</li>
 *   <li>Marker size customization</li>
 *   <li>Series legend</li>
 * </ul>
 */
public class ScatterPlotDemo extends AbstractDemo {

    private Chart chart;

    private ScatterSeries sample0Series;
    private ScatterSeries sample1Series;
    private ScatterSeries sample2Series;
    private ScatterSeries sample3Series;

    private static final Color SAMPLE_0_COLOR = new Color(66, 133, 244);   // Blue
    private static final Color SAMPLE_1_COLOR = new Color(234, 67, 53);    // Red
    private static final Color SAMPLE_2_COLOR = new Color(52, 168, 83);    // Green
    private static final Color SAMPLE_3_COLOR = new Color(251, 188, 5);    // Yellow

    public ScatterPlotDemo() {
        setWindowSize(1000, 700);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Scatter Plot";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();
        chart.setXAxis(new TimeAxis());
        chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.ALWAYS);

        // Generate sample data - clusters in different regions
        XyData sample0Data = generateClusterData("Sample 0", -75, 600, 50);
        XyData sample1Data = generateClusterData("Sample 1", -25, 200, 50);
        XyData sample2Data = generateClusterData("Sample 2", 25, -200, 50);
        XyData sample3Data = generateClusterData("Sample 3", 75, -600, 50);

        // Create scatter series with different marker shapes
        sample0Series = new ScatterSeries(sample0Data,
                new ScatterSeriesOptions()
                        .color(SAMPLE_0_COLOR)
                        .markerSize(8f)
                        .markerShape(ScatterSeriesOptions.MarkerShape.SQUARE));

        sample1Series = new ScatterSeries(sample1Data,
                new ScatterSeriesOptions()
                        .color(SAMPLE_1_COLOR)
                        .markerSize(8f)
                        .markerShape(ScatterSeriesOptions.MarkerShape.CIRCLE));

        sample2Series = new ScatterSeries(sample2Data,
                new ScatterSeriesOptions()
                        .color(SAMPLE_2_COLOR)
                        .markerSize(8f)
                        .markerShape(ScatterSeriesOptions.MarkerShape.DIAMOND));

        sample3Series = new ScatterSeries(sample3Data,
                new ScatterSeriesOptions()
                        .color(SAMPLE_3_COLOR)
                        .markerSize(8f)
                        .markerShape(ScatterSeriesOptions.MarkerShape.TRIANGLE_UP));

        // Add series to chart
        chart.addSeries(sample0Series);
        chart.addSeries(sample1Series);
        chart.addSeries(sample2Series);
        chart.addSeries(sample3Series);

        // Fit viewport to data
        chart.zoomToFit();

        // Setup legend
        chart.addLegendItem("sample0", "Sample 0", SAMPLE_0_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.SCATTER);
        chart.addLegendItem("sample1", "Sample 1", SAMPLE_1_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.SCATTER);
        chart.addLegendItem("sample2", "Sample 2", SAMPLE_2_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.SCATTER);
        chart.addLegendItem("sample3", "Sample 3", SAMPLE_3_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.SCATTER);

        return chart;
    }

    private XyData generateClusterData(String name, float centerX, float centerY, int points) {
        Random rand = new Random(name.hashCode());

        // Use X value as "timestamp" (scaled to visible range)
        long baseTime = System.currentTimeMillis() - 100 * 60000L;

        // Generate all points first, then sort by timestamp (required by XyData)
        List<long[]> pointsList = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            // Generate clustered points with Gaussian distribution
            float x = centerX + (float) rand.nextGaussian() * 25;
            float y = centerY + (float) rand.nextGaussian() * 150;
            long timestamp = baseTime + (long) ((x + 100) * 600); // Scale x to time
            pointsList.add(new long[]{timestamp, Float.floatToIntBits(y)});
        }

        // Sort by timestamp (ascending order required)
        pointsList.sort(Comparator.comparingLong(p -> p[0]));

        // Create data with sorted points, ensuring strictly ascending timestamps
        XyData data = new XyData(name.toLowerCase().replace(" ", "_"), name, points);
        long lastTimestamp = Long.MIN_VALUE;
        for (long[] point : pointsList) {
            long timestamp = point[0];
            // Ensure strictly ascending - increment if duplicate
            if (timestamp <= lastTimestamp) {
                timestamp = lastTimestamp + 1;
            }
            lastTimestamp = timestamp;
            data.append(timestamp, Float.intBitsToFloat((int) point[1]));
        }

        return data;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Scatter Plot Demo - Multiple samples with clustering"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Series visibility toggles
        panel.add(createSeriesCheckbox("Sample 0", SAMPLE_0_COLOR, sample0Series));
        panel.add(createSeriesCheckbox("Sample 1", SAMPLE_1_COLOR, sample1Series));
        panel.add(createSeriesCheckbox("Sample 2", SAMPLE_2_COLOR, sample2Series));
        panel.add(createSeriesCheckbox("Sample 3", SAMPLE_3_COLOR, sample3Series));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Marker size slider
        panel.add(DemoUIHelper.createControlLabel("Marker Size:"));
        JSlider sizeSlider = new JSlider(4, 16, 8);
        sizeSlider.addChangeListener(e -> {
            float size = sizeSlider.getValue();
            sample0Series.getOptions().markerSize(size);
            sample1Series.getOptions().markerSize(size);
            sample2Series.getOptions().markerSize(size);
            sample3Series.getOptions().markerSize(size);
            chart.repaint();
        });
        panel.add(sizeSlider);

        return panel;
    }

    private JCheckBox createSeriesCheckbox(String label, Color color, ScatterSeries series) {
        JCheckBox cb = new JCheckBox(label, true);
        cb.setForeground(color);
        cb.addActionListener(e -> {
            series.getOptions().visible(cb.isSelected());
            chart.repaint();
        });
        return cb;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new ScatterPlotDemo(), args);
    }
}
