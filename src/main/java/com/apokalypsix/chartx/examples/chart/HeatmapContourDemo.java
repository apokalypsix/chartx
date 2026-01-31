package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.TimeAxis;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.HeatmapData;
import com.apokalypsix.chartx.chart.series.ContourSeries;
import com.apokalypsix.chartx.chart.series.HeatmapSeries;
import com.apokalypsix.chartx.chart.style.ContourSeriesOptions;
import com.apokalypsix.chartx.chart.style.HeatmapSeriesOptions;
import com.apokalypsix.chartx.core.render.model.HeatmapContourLayer;
import com.apokalypsix.chartx.core.render.util.ColorMap;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Heatmap and Contour charts using the Chart infrastructure.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Heatmap visualization with configurable color maps</li>
 *   <li>Contour line overlay with adjustable levels</li>
 *   <li>Proper axis labels and grid</li>
 * </ul>
 */
public class HeatmapContourDemo extends AbstractDemo {

    private Chart chart;

    private HeatmapData heatmapData;
    private HeatmapSeries heatmapSeries;
    private ContourSeries contourSeries;
    private HeatmapContourLayer heatmapContourLayer;

    private static final int ROWS = 50;
    private static final int COLS = 80;

    public HeatmapContourDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Heatmap & Contour";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Configure axes for numeric data (not time-based)
        chart.setXAxis(new TimeAxis());
        chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.NEVER);

        // Create coordinate arrays for heatmap
        double[] xCoords = new double[COLS];
        double[] yCoords = new double[ROWS];

        for (int i = 0; i < COLS; i++) {
            xCoords[i] = i;
        }
        for (int i = 0; i < ROWS; i++) {
            yCoords[i] = i;
        }

        heatmapData = new HeatmapData("heat", "Temperature Map", xCoords, yCoords);

        // Generate sample data (2D Gaussian-like pattern)
        generateHeatmapData();

        // Create heatmap series
        heatmapSeries = new HeatmapSeries(heatmapData,
                new HeatmapSeriesOptions()
                        .colorMap(ColorMap.viridis())
                        .showCellBorders(false)
                        .opacity(0.9f));

        // Create contour series (overlay)
        contourSeries = new ContourSeries(heatmapData,
                new ContourSeriesOptions()
                        .levelCount(10)
                        .lineWidth(1.5f)
                        .lineColor(new Color(255, 255, 255, 180)));

        // Create layer and add series
        heatmapContourLayer = new HeatmapContourLayer();
        heatmapContourLayer.setHeatmapSeries(heatmapSeries);
        heatmapContourLayer.setContourSeries(contourSeries);

        // Add layer to chart
        chart.addRenderLayer(heatmapContourLayer);

        // Set visible range to match data dimensions
        chart.setVisibleRange(0, COLS);
        chart.getYAxis().setVisibleRange(0, ROWS);

        // Set bar duration to 1 (matches cell width)
        chart.setBarDuration(1);

        return chart;
    }

    private void generateHeatmapData() {
        // Create multiple Gaussian peaks
        float[][] centers = {
            {0.3f, 0.4f, 1.0f},   // x, y, amplitude
            {0.7f, 0.6f, 0.8f},
            {0.5f, 0.2f, 0.6f},
            {0.2f, 0.8f, 0.5f}
        };

        for (int row = 0; row < ROWS; row++) {
            float ny = (float) row / ROWS;
            for (int col = 0; col < COLS; col++) {
                float nx = (float) col / COLS;

                float value = 0;
                for (float[] center : centers) {
                    float dx = nx - center[0];
                    float dy = ny - center[1];
                    float dist = dx * dx + dy * dy;
                    value += center[2] * (float) Math.exp(-dist * 15);
                }

                // Add some noise
                value += 0.1f * (float) Math.sin(nx * 20) * (float) Math.cos(ny * 15);

                heatmapData.setValue(row, col, value);
            }
        }
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Visualization:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Heatmap toggle
        JCheckBox heatmapBox = new JCheckBox("Heatmap", true);
        heatmapBox.setForeground(Color.WHITE);
        heatmapBox.addActionListener(e -> {
            heatmapSeries.getOptions().visible(heatmapBox.isSelected());
            chart.repaint();
        });
        panel.add(heatmapBox);

        // Contour toggle
        JCheckBox contourBox = new JCheckBox("Contour Lines", true);
        contourBox.setForeground(Color.WHITE);
        contourBox.addActionListener(e -> {
            contourSeries.getOptions().visible(contourBox.isSelected());
            chart.repaint();
        });
        panel.add(contourBox);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Color map selector
        panel.add(DemoUIHelper.createControlLabel("Color Map:"));
        JComboBox<String> colorMapCombo = DemoUIHelper.createComboBox(
                "Viridis", "Plasma", "Jet", "Grayscale", "Thermal");
        colorMapCombo.addActionListener(e -> {
            ColorMap map = switch (colorMapCombo.getSelectedIndex()) {
                case 1 -> ColorMap.plasma();
                case 2 -> ColorMap.jet();
                case 3 -> ColorMap.grayscale();
                case 4 -> ColorMap.thermal();
                default -> ColorMap.viridis();
            };
            heatmapSeries.getOptions().colorMap(map);
            chart.repaint();
        });
        panel.add(colorMapCombo);

        // Contour levels
        panel.add(DemoUIHelper.createControlLabel("Contour Levels:"));
        JSlider levelsSlider = new JSlider(3, 20, 10);
        levelsSlider.addChangeListener(e -> {
            contourSeries.getOptions().levelCount(levelsSlider.getValue());
            contourSeries.invalidateCache();
            chart.repaint();
        });
        panel.add(levelsSlider);

        // Regenerate button
        JButton regenButton = new JButton("Regenerate");
        regenButton.addActionListener(e -> {
            generateHeatmapData();
            contourSeries.invalidateCache();
            chart.repaint();
        });
        panel.add(regenButton);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new HeatmapContourDemo(), args);
    }
}
