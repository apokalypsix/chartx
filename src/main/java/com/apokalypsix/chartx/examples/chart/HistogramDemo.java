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
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.series.HistogramSeries;
import com.apokalypsix.chartx.chart.style.HistogramSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing histogram charts for distribution visualization.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Distribution histogram with bins</li>
 *   <li>Multiple histogram series overlay</li>
 *   <li>Bar width and color customization</li>
 *   <li>Gaussian distribution visualization</li>
 * </ul>
 */
public class HistogramDemo extends AbstractDemo {

    private Chart chart;

    private HistogramSeries h1Series;
    private HistogramSeries h2Series;

    private static final Color H1_COLOR = new Color(66, 133, 244, 200);   // Blue
    private static final Color H2_COLOR = new Color(234, 67, 53, 150);    // Red (semi-transparent for overlay)

    // Bin configuration for histogram
    private static final int NUM_BINS = 24;
    private static final double BIN_MIN = 0;
    private static final double BIN_MAX = 12;
    private static final double BIN_WIDTH = (BIN_MAX - BIN_MIN) / NUM_BINS;

    public HistogramDemo() {
        setWindowSize(1000, 700);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Histogram";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Set up category axis with bin labels
        chart.setXAxis(new CategoryAxis(CategoryAxis.DEFAULT_AXIS_ID, CategoryAxis.Position.BOTTOM));
        CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
        categoryAxis.setCategories(generateBinLabels());
        categoryAxis.height(40);
        categoryAxis.labelColor(new Color(180, 180, 180));

        // Generate histogram data - simulate two overlapping distributions
        HistogramData h1Data = generateDistributionHistogram("H1", 5.5, 1.5, 1000);
        HistogramData h2Data = generateDistributionHistogram("H2", 7.0, 2.0, 800);

        // Create histogram series
        h1Series = new HistogramSeries(h1Data,
                new HistogramSeriesOptions()
                        .positiveColor(H1_COLOR)
                        .negativeColor(H1_COLOR)
                        .barWidthRatio(0.85f));

        h2Series = new HistogramSeries(h2Data,
                new HistogramSeriesOptions()
                        .positiveColor(H2_COLOR)
                        .negativeColor(H2_COLOR)
                        .barWidthRatio(0.85f));

        // Add series to chart
        chart.addSeries(h1Series);
        chart.addSeries(h2Series);

        // Configure visible range for category count (with padding for first/last bars)
        chart.setVisibleRange(-1, NUM_BINS);
        chart.setBarDuration(1);
        chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.ALWAYS);

        // Add legend
        chart.addLegendItem("h1", "H1", H1_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.HISTOGRAM);
        chart.addLegendItem("h2", "H2", H2_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.HISTOGRAM);

        return chart;
    }

    /**
     * Generates bin labels for the X-axis (e.g., "0-0.5", "0.5-1", etc.)
     */
    private String[] generateBinLabels() {
        String[] labels = new String[NUM_BINS];
        for (int i = 0; i < NUM_BINS; i++) {
            double binStart = BIN_MIN + i * BIN_WIDTH;
            double binEnd = binStart + BIN_WIDTH;
            // Use integer formatting for whole numbers, otherwise one decimal
            if (binStart == (int) binStart && binEnd == (int) binEnd) {
                labels[i] = String.format("%d-%d", (int) binStart, (int) binEnd);
            } else {
                labels[i] = String.format("%.1f", binStart);
            }
        }
        return labels;
    }

    /**
     * Generates histogram data from a Gaussian distribution.
     */
    private HistogramData generateDistributionHistogram(String name, double mean, double stdDev, int samples) {
        // Generate raw samples
        Random rand = new Random(name.hashCode());
        double[] rawSamples = new double[samples];
        for (int i = 0; i < samples; i++) {
            rawSamples[i] = mean + rand.nextGaussian() * stdDev;
        }

        // Count samples into bins
        int[] counts = new int[NUM_BINS];
        for (double v : rawSamples) {
            int bin = (int) ((v - BIN_MIN) / BIN_WIDTH);
            if (bin >= 0 && bin < NUM_BINS) {
                counts[bin]++;
            }
        }

        // Create histogram data using bin index as X position
        HistogramData data = new HistogramData(name.toLowerCase(), name, NUM_BINS);
        for (int i = 0; i < NUM_BINS; i++) {
            data.append(i, counts[i]);
        }

        return data;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Histogram Demo - Distribution of two samples"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Series visibility toggles
        JCheckBox h1Check = new JCheckBox("H1", true);
        h1Check.setForeground(new Color(66, 133, 244));
        h1Check.addActionListener(e -> {
            h1Series.getOptions().visible(h1Check.isSelected());
            chart.repaint();
        });
        panel.add(h1Check);

        JCheckBox h2Check = new JCheckBox("H2", true);
        h2Check.setForeground(new Color(234, 67, 53));
        h2Check.addActionListener(e -> {
            h2Series.getOptions().visible(h2Check.isSelected());
            chart.repaint();
        });
        panel.add(h2Check);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Bar width slider
        panel.add(DemoUIHelper.createControlLabel("Bar Width:"));
        JSlider widthSlider = new JSlider(50, 100, 85);
        widthSlider.addChangeListener(e -> {
            float ratio = widthSlider.getValue() / 100f;
            h1Series.getOptions().barWidthRatio(ratio);
            h2Series.getOptions().barWidthRatio(ratio);
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
        launch(new HistogramDemo(), args);
    }
}
