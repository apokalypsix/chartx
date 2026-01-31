package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.series.HorizontalBarSeries;
import com.apokalypsix.chartx.chart.style.HistogramSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing a population pyramid chart (back-to-back horizontal bars).
 *
 * <p>Shows demographic data with:
 * <ul>
 *   <li>Age groups on the Y-axis</li>
 *   <li>Male population extending left (negative values)</li>
 *   <li>Female population extending right (positive values)</li>
 *   <li>Mirrored horizontal bar layout</li>
 * </ul>
 */
public class PopulationPyramidDemo extends AbstractDemo {

    private Chart chart;

    private HorizontalBarSeries maleSeries;
    private HorizontalBarSeries femaleSeries;

    private static final Color MALE_COLOR = new Color(52, 152, 219);    // Blue
    private static final Color FEMALE_COLOR = new Color(231, 76, 60);   // Red

    // Age groups and population data (in millions)
    private static final String[] AGE_GROUPS = {
            "0-4", "5-9", "10-14", "15-19", "20-24", "25-29", "30-34", "35-39",
            "40-44", "45-49", "50-54", "55-59", "60-64", "65-69", "70-74", "75+"
    };

    // Approximate US population distribution (simplified)
    private static final float[] MALE_POP = {
            10.0f, 10.5f, 10.8f, 11.0f, 11.5f, 12.0f, 11.0f, 10.5f,
            10.0f, 10.5f, 11.0f, 10.5f, 9.5f, 7.5f, 5.5f, 6.0f
    };

    private static final float[] FEMALE_POP = {
            9.5f, 10.0f, 10.3f, 10.5f, 11.0f, 11.5f, 10.8f, 10.5f,
            10.2f, 10.8f, 11.2f, 11.0f, 10.0f, 8.5f, 6.5f, 8.0f
    };

    public PopulationPyramidDemo() {
        setWindowSize(1000, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Population Pyramid";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();

        // Set up category axis with age groups
        chart.setXAxis(new CategoryAxis(CategoryAxis.DEFAULT_AXIS_ID, CategoryAxis.Position.LEFT));
        CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
        categoryAxis.setCategories(AGE_GROUPS);
        categoryAxis.height(60);  // Width when vertical
        categoryAxis.labelColor(new Color(180, 180, 180));

        // Create histogram data for male (negative values for left extension)
        HistogramData maleData = new HistogramData("male", "Male", AGE_GROUPS.length);
        HistogramData femaleData = new HistogramData("female", "Female", AGE_GROUPS.length);

        // Use category indices as Y positions (synthetic timestamps)
        for (int i = 0; i < AGE_GROUPS.length; i++) {
            // Male population goes negative (extends left)
            maleData.append(i, -MALE_POP[i]);
            // Female population goes positive (extends right)
            femaleData.append(i, FEMALE_POP[i]);
        }

        // Create horizontal bar series
        maleSeries = new HorizontalBarSeries(maleData,
                new HistogramSeriesOptions()
                        .positiveColor(MALE_COLOR)
                        .negativeColor(MALE_COLOR)
                        .barWidthRatio(0.8f));

        femaleSeries = new HorizontalBarSeries(femaleData,
                new HistogramSeriesOptions()
                        .positiveColor(FEMALE_COLOR)
                        .negativeColor(FEMALE_COLOR)
                        .barWidthRatio(0.8f));

        // Add series to chart
        chart.addSeries(maleSeries);
        chart.addSeries(femaleSeries);

        // Configure visible range for category count (with padding for first/last bars)
        chart.setVisibleRange(-1, AGE_GROUPS.length);

        // Set bar duration to 1 (matches our index spacing)
        chart.setBarDuration(1);

        // Set Y-axis range for population values
        // In horizontal bar mode, Y-axis values control horizontal bar extent
        // and are rendered at the bottom as X-axis labels
        chart.getYAxis().setVisibleRange(-15, 15);
        chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.NEVER);
        chart.getYAxis().setVisible(false);  // Hide right-side labels (shown at bottom instead)

        // Disable zoom/pan - categorical charts should show all data
        chart.getModifiers().clear();

        // Add legend
        chart.addLegendItem("male", "Male", MALE_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.HORIZONTAL_BAR);
        chart.addLegendItem("female", "Female", FEMALE_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.HORIZONTAL_BAR);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Population Chart Demo - Age distribution by gender"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Series visibility toggles
        JCheckBox maleCheck = new JCheckBox("Male", true);
        maleCheck.setForeground(MALE_COLOR);
        maleCheck.addActionListener(e -> {
            maleSeries.getOptions().visible(maleCheck.isSelected());
            chart.repaint();
        });
        panel.add(maleCheck);

        JCheckBox femaleCheck = new JCheckBox("Female", true);
        femaleCheck.setForeground(FEMALE_COLOR);
        femaleCheck.addActionListener(e -> {
            femaleSeries.getOptions().visible(femaleCheck.isSelected());
            chart.repaint();
        });
        panel.add(femaleCheck);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Bar width slider
        panel.add(DemoUIHelper.createControlLabel("Bar Width:"));
        JSlider widthSlider = new JSlider(50, 100, 80);
        widthSlider.addChangeListener(e -> {
            float ratio = widthSlider.getValue() / 100f;
            maleSeries.getOptions().barWidthRatio(ratio);
            femaleSeries.getOptions().barWidthRatio(ratio);
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
        launch(new PopulationPyramidDemo(), args);
    }
}
