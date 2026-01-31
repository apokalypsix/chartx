package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.series.CandlestickSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demonstrates SeriesOptions customization.
 *
 * <p>This demo showcases:
 * <ul>
 *   <li>OhlcSeriesOptions: colors, styles (candlestick, hollow, OHLC bars)</li>
 *   <li>LineSeriesOptions: colors, line width, display modes (line, area, step)</li>
 *   <li>Runtime option changes and their immediate effect</li>
 *   <li>Fluent builder pattern for options configuration</li>
 * </ul>
 */
public class SeriesOptionsDemo extends AbstractDemo {

    private FinanceChart chart;
    private OhlcData ohlcData;
    private XyData emaData;
    private CandlestickSeries candleSeries;
    private LineSeries lineSeries;

    // Color presets
    private static final Color[][] COLOR_PRESETS = {
            {new Color(38, 166, 91), new Color(214, 69, 65)},    // Classic Green/Red
            {new Color(52, 152, 219), new Color(230, 126, 34)},  // Blue/Orange
            {new Color(155, 89, 182), new Color(241, 196, 15)},  // Purple/Yellow
            {new Color(26, 188, 156), new Color(231, 76, 60)},   // Teal/Coral
            {new Color(46, 204, 113), new Color(149, 165, 166)}, // Green/Gray
    };

    private static final String[] COLOR_NAMES = {
            "Classic (Green/Red)",
            "Blue/Orange",
            "Purple/Yellow",
            "Teal/Coral",
            "Green/Gray"
    };

    public SeriesOptionsDemo() {
        setWindowSize(1400, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Series Options";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate sample data
        ohlcData = DemoDataGenerator.generateOHLCData();
        emaData = DemoDataGenerator.calculateEMA(ohlcData, 20);

        // Add candlestick series with default options
        candleSeries = chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions()
                .upColor(COLOR_PRESETS[0][0])
                .downColor(COLOR_PRESETS[0][1])
                .barWidthRatio(0.8f));

        // Add EMA line with area display
        lineSeries = chart.addLineSeries(emaData, new LineSeriesOptions()
                .color(new Color(41, 128, 185))
                .lineWidth(2.0f)
                .displayMode(LineSeriesOptions.DisplayMode.LINE));

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DemoConfig.PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Row 1: Candlestick options
        JPanel candleRow = DemoUIHelper.createControlPanel();
        candleRow.add(DemoUIHelper.createControlLabel("Candlestick Options:"));

        // Color preset selector
        JComboBox<String> colorCombo = new JComboBox<>(COLOR_NAMES);
        colorCombo.addActionListener(e -> {
            int idx = colorCombo.getSelectedIndex();
            candleSeries.getOptions()
                    .upColor(COLOR_PRESETS[idx][0])
                    .downColor(COLOR_PRESETS[idx][1]);
            chart.repaint();
        });
        candleRow.add(colorCombo);

        // Style selector
        candleRow.add(DemoUIHelper.createControlLabel("Style:"));
        JComboBox<String> styleCombo = DemoUIHelper.createComboBox(
                "Candlestick", "Hollow Candle", "OHLC Bar", "Line", "Heikin-Ashi");
        styleCombo.addActionListener(e -> {
            OhlcSeriesOptions.OhlcStyle style = switch (styleCombo.getSelectedIndex()) {
                case 1 -> OhlcSeriesOptions.OhlcStyle.HOLLOW_CANDLE;
                case 2 -> OhlcSeriesOptions.OhlcStyle.OHLC_BAR;
                case 3 -> OhlcSeriesOptions.OhlcStyle.LINE;
                case 4 -> OhlcSeriesOptions.OhlcStyle.HEIKIN_ASHI;
                default -> OhlcSeriesOptions.OhlcStyle.CANDLESTICK;
            };
            candleSeries.getOptions().style(style);
            chart.repaint();
        });
        candleRow.add(styleCombo);

        // Bar width slider
        candleRow.add(DemoUIHelper.createControlLabel("Bar Width:"));
        JSlider widthSlider = new JSlider(20, 100, 80);
        widthSlider.setPreferredSize(new Dimension(100, 25));
        widthSlider.addChangeListener(e -> {
            float ratio = widthSlider.getValue() / 100f;
            candleSeries.getOptions().barWidthRatio(ratio);
            chart.repaint();
        });
        candleRow.add(widthSlider);

        panel.add(candleRow);

        // Row 2: Line options
        JPanel lineRow = DemoUIHelper.createControlPanel();
        lineRow.add(DemoUIHelper.createControlLabel("Line Options:"));

        // Display mode
        lineRow.add(DemoUIHelper.createControlLabel("Mode:"));
        JComboBox<String> modeCombo = DemoUIHelper.createComboBox(
                "Line", "Area", "Step Before", "Step After", "Step Middle");
        modeCombo.addActionListener(e -> {
            LineSeriesOptions.DisplayMode mode = switch (modeCombo.getSelectedIndex()) {
                case 1 -> LineSeriesOptions.DisplayMode.AREA;
                case 2 -> LineSeriesOptions.DisplayMode.STEP_BEFORE;
                case 3 -> LineSeriesOptions.DisplayMode.STEP_AFTER;
                case 4 -> LineSeriesOptions.DisplayMode.STEP_MIDDLE;
                default -> LineSeriesOptions.DisplayMode.LINE;
            };
            lineSeries.getOptions().displayMode(mode);
            chart.repaint();
        });
        lineRow.add(modeCombo);

        // Line width slider
        lineRow.add(DemoUIHelper.createControlLabel("Line Width:"));
        JSlider lineWidthSlider = new JSlider(5, 50, 20);
        lineWidthSlider.setPreferredSize(new Dimension(100, 25));
        lineWidthSlider.addChangeListener(e -> {
            float width = lineWidthSlider.getValue() / 10f;
            lineSeries.getOptions().lineWidth(width);
            chart.repaint();
        });
        lineRow.add(lineWidthSlider);

        // Line color selector
        lineRow.add(DemoUIHelper.createControlLabel("Color:"));
        JComboBox<String> lineColorCombo = DemoUIHelper.createComboBox(
                "Blue", "Orange", "Green", "Red", "Purple");
        lineColorCombo.addActionListener(e -> {
            Color color = switch (lineColorCombo.getSelectedIndex()) {
                case 1 -> new Color(230, 126, 34);
                case 2 -> new Color(46, 204, 113);
                case 3 -> new Color(231, 76, 60);
                case 4 -> new Color(155, 89, 182);
                default -> new Color(41, 128, 185);
            };
            lineSeries.getOptions()
                    .color(color)
                    .fillColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            chart.repaint();
        });
        lineRow.add(lineColorCombo);

        // Opacity slider
        lineRow.add(DemoUIHelper.createControlLabel("Opacity:"));
        JSlider opacitySlider = new JSlider(10, 100, 100);
        opacitySlider.setPreferredSize(new Dimension(80, 25));
        opacitySlider.addChangeListener(e -> {
            float opacity = opacitySlider.getValue() / 100f;
            lineSeries.getOptions().opacity(opacity);
            chart.repaint();
        });
        lineRow.add(opacitySlider);

        panel.add(lineRow);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new SeriesOptionsDemo(), args);
    }
}
