package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Calendar;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.TimeAxis;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.chart.series.BandSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.style.BandSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing difference/area charts.
 *
 * <p>Shows the filled area between two series, similar to JFreeChart's
 * "Daylight Hours" demo showing sunrise/sunset times.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Band series showing area between upper and lower bounds</li>
 *   <li>Line series overlay for the boundaries</li>
 *   <li>Time-based data across a year</li>
 * </ul>
 */
public class DifferenceChartDemo extends AbstractDemo {

    private Chart chart;

    private BandSeries daylightBand;
    private LineSeries sunriseLine;
    private LineSeries sunsetLine;

    private static final Color DAYLIGHT_FILL = new Color(255, 215, 0, 100);  // Gold with transparency
    private static final Color SUNRISE_COLOR = new Color(255, 140, 0);        // Dark orange
    private static final Color SUNSET_COLOR = new Color(255, 69, 0);          // Red-orange

    public DifferenceChartDemo() {
        setWindowSize(1200, 600);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Difference Chart (Daylight Hours)";
    }

    @Override
    protected Component createDemoContent() {
        chart = createChart();
        chart.setXAxis(new TimeAxis());

        // Generate daylight data for London, UK (approximate)
        // Sunrise and sunset times throughout the year
        XyyData daylightData = new XyyData("daylight", "Daylight Hours", 365);
        XyData sunriseData = new XyData("sunrise", "Sunrise", 365);
        XyData sunsetData = new XyData("sunset", "Sunset", 365);

        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
        long startTime = cal.getTimeInMillis();

        for (int day = 0; day < 365; day++) {
            long timestamp = startTime + day * 24L * 60 * 60 * 1000;

            // Approximate sunrise/sunset times for London (hours from midnight)
            // Using simplified sinusoidal model
            double dayOfYear = day;
            double angle = (dayOfYear - 172) * 2 * Math.PI / 365;  // Peak at summer solstice (June 21)

            // Sunrise: varies from ~8am in winter to ~4:30am in summer
            double sunriseHour = 6.25 - 1.75 * Math.cos(angle);
            // Sunset: varies from ~4pm in winter to ~9:30pm in summer
            double sunsetHour = 18.75 + 2.75 * Math.cos(angle);

            // Convert to decimal hours for display
            float sunrise = (float) sunriseHour;
            float sunset = (float) sunsetHour;

            // Band data: upper is sunset, lower is sunrise
            daylightData.append(timestamp, sunset, (sunrise + sunset) / 2, sunrise);
            sunriseData.append(timestamp, sunrise);
            sunsetData.append(timestamp, sunset);
        }

        // Create band series for the filled area
        daylightBand = new BandSeries(daylightData,
                new BandSeriesOptions()
                        .fillColor(DAYLIGHT_FILL)
                        .upperColor(SUNSET_COLOR)
                        .lowerColor(SUNRISE_COLOR)
                        .middleColor(null)  // No middle line
                        .lineWidth(2f));

        // Create line series for the boundaries
        sunriseLine = new LineSeries(sunriseData,
                new LineSeriesOptions()
                        .color(SUNRISE_COLOR)
                        .lineWidth(2f)
                        .visible(false));  // Hidden by default (band shows the lines)

        sunsetLine = new LineSeries(sunsetData,
                new LineSeriesOptions()
                        .color(SUNSET_COLOR)
                        .lineWidth(2f)
                        .visible(false));

        // Add series to chart
        chart.addSeries(daylightBand);
        chart.addSeries(sunriseLine);
        chart.addSeries(sunsetLine);

        // Fit viewport to show all data
        chart.zoomToFit();

        // Add legend
        chart.addLegendItem("sunrise", "Sunrise", SUNRISE_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.LINE);
        chart.addLegendItem("sunset", "Sunset", SUNSET_COLOR,
                com.apokalypsix.chartx.chart.series.SeriesType.LINE);

        // Set Y-axis to show hours (4-22)
        chart.getYAxis().setVisibleRange(4, 22);
        chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.NEVER);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Daylight Hours - London, UK"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Show separate lines toggle
        JCheckBox showLines = new JCheckBox("Show Boundary Lines", false);
        showLines.setForeground(Color.WHITE);
        showLines.addActionListener(e -> {
            boolean show = showLines.isSelected();
            sunriseLine.getOptions().visible(show);
            sunsetLine.getOptions().visible(show);
            chart.repaint();
        });
        panel.add(showLines);

        // Show fill toggle
        JCheckBox showFill = new JCheckBox("Show Filled Area", true);
        showFill.setForeground(Color.WHITE);
        showFill.addActionListener(e -> {
            daylightBand.getOptions().visible(showFill.isSelected());
            chart.repaint();
        });
        panel.add(showFill);

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
        launch(new DifferenceChartDemo(), args);
    }
}
