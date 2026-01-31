package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.data.BubbleData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.series.BubbleSeries;
import com.apokalypsix.chartx.chart.series.SplineLineSeries;
import com.apokalypsix.chartx.chart.series.SplineMountainSeries;
import com.apokalypsix.chartx.chart.style.BubbleSeriesOptions;
import com.apokalypsix.chartx.chart.style.SplineSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Spline and Bubble chart types.
 */
public class SplineChartsDemo extends AbstractDemo {

    private Chart chart;

    private XyData splineData;
    private XyData mountainData;
    private BubbleData bubbleData;

    private SplineLineSeries splineLineSeries;
    private SplineMountainSeries splineMountainSeries;
    private BubbleSeries bubbleSeries;

    private int currentChart = 0; // 0=spline line, 1=spline mountain, 2=bubble

    public SplineChartsDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Spline & Bubble Charts";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Create spline data (smooth curve)
        splineData = createSmoothData("Temperature", 100);
        mountainData = createSmoothData("Rainfall", 100);
        bubbleData = createBubbleData();

        // Set visible range based on data
        long startTime = splineData.getMinX();
        long endTime = splineData.getMaxX();
        chart.setVisibleRange(startTime - 60000, endTime + 60000);

        // Create and add series
        splineLineSeries = new SplineLineSeries(splineData,
                new SplineSeriesOptions()
                        .color(new Color(65, 131, 196))
                        .lineWidth(2.5f)
                        .tension(0.4f));

        splineMountainSeries = new SplineMountainSeries(mountainData,
                new SplineSeriesOptions()
                        .color(new Color(46, 204, 113))
                        .fillColor(new Color(46, 204, 113, 100))
                        .lineWidth(2.0f)
                        .tension(0.3f)
                        .showLine(true)
                        .visible(false));

        bubbleSeries = new BubbleSeries(bubbleData,
                new BubbleSeriesOptions()
                        .color(new Color(155, 89, 182, 180))
                        .borderColor(new Color(155, 89, 182))
                        .borderWidth(2f)
                        .minRadius(10f)
                        .maxRadius(60f)
                        .visible(false));

        // Add series to chart
        chart.addSeries(splineLineSeries);
        chart.addSeries(splineMountainSeries);
        chart.addSeries(bubbleSeries);

        return chart;
    }

    private XyData createSmoothData(String name, int points) {
        XyData data = new XyData(name.toLowerCase(), name, points);
        Random rand = new Random(42);
        long startTime = System.currentTimeMillis() - points * 60000L;

        float baseValue = 100 + rand.nextFloat() * 10;

        for (int i = 0; i < points; i++) {
            // Create smooth wave pattern
            float wave = (float) Math.sin(i * 0.1) * 10;
            float noise = (rand.nextFloat() - 0.5f) * 3;
            float value = baseValue + wave + noise;
            data.append(startTime + i * 60000L, value);
        }

        return data;
    }

    private BubbleData createBubbleData() {
        BubbleData data = new BubbleData("scatter", "Product Performance", 30);
        Random rand = new Random(123);
        long startTime = System.currentTimeMillis() - 100 * 60000L;

        for (int i = 0; i < 30; i++) {
            // Spread bubbles across time range
            long timestamp = startTime + (i * 3 + rand.nextInt(3)) * 60000L;
            float y = 80 + rand.nextFloat() * 40;
            float size = 5 + rand.nextFloat() * 50;
            data.append(timestamp, y, size);
        }

        return data;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Chart Type:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        ButtonGroup group = new ButtonGroup();

        JRadioButton splineBtn = new JRadioButton("Spline Line", true);
        splineBtn.setForeground(Color.WHITE);
        splineBtn.addActionListener(e -> {
            currentChart = 0;
            updateVisibility();
        });
        group.add(splineBtn);
        panel.add(splineBtn);

        JRadioButton mountainBtn = new JRadioButton("Spline Mountain", false);
        mountainBtn.setForeground(Color.WHITE);
        mountainBtn.addActionListener(e -> {
            currentChart = 1;
            updateVisibility();
        });
        group.add(mountainBtn);
        panel.add(mountainBtn);

        JRadioButton bubbleBtn = new JRadioButton("Bubble", false);
        bubbleBtn.setForeground(Color.WHITE);
        bubbleBtn.addActionListener(e -> {
            currentChart = 2;
            updateVisibility();
        });
        group.add(bubbleBtn);
        panel.add(bubbleBtn);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Tension slider for spline
        panel.add(DemoUIHelper.createControlLabel("Tension:"));
        JSlider tensionSlider = new JSlider(0, 100, 40);
        tensionSlider.addChangeListener(e -> {
            float tension = tensionSlider.getValue() / 100f;
            splineLineSeries.getOptions().tension(tension);
            splineMountainSeries.getOptions().tension(tension);
            chart.repaint();
        });
        panel.add(tensionSlider);

        return panel;
    }

    private void updateVisibility() {
        splineLineSeries.getOptions().visible(currentChart == 0);
        splineMountainSeries.getOptions().visible(currentChart == 1);
        bubbleSeries.getOptions().visible(currentChart == 2);
        chart.repaint();
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new SplineChartsDemo(), args);
    }
}
