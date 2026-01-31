package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.series.CandlestickSeries;
import com.apokalypsix.chartx.chart.series.LineSeries;
import com.apokalypsix.chartx.chart.series.ScatterSeries;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.chart.style.ScatterSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demonstrates rendering the same data with multiple series types.
 *
 * <p>This is the key feature of the Data/Series split architecture:
 * a single Data instance can be rendered by multiple Series with
 * different visual representations.
 *
 * <p>This demo showcases:
 * <ul>
 *   <li>OHLC data displayed as candlesticks AND as a close-price line</li>
 *   <li>Scatter points on significant price levels (highs/lows)</li>
 *   <li>Area fill from close price to a baseline</li>
 *   <li>Zero data copying - all series reference the same underlying data</li>
 * </ul>
 *
 * <p>Use case: Overlay analysis showing price structure in multiple ways.
 */
public class SameDataMultiViewDemo extends AbstractDemo {

    private FinanceChart chart;
    private OhlcData ohlcData;
    private XyData closeData;
    private XyData highData;
    private XyData lowData;

    private CandlestickSeries candleSeries;
    private LineSeries closeLineSeries;
    private LineSeries closeAreaSeries;
    private ScatterSeries highScatterSeries;
    private ScatterSeries lowScatterSeries;

    public SameDataMultiViewDemo() {
        setWindowSize(1400, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Same Data, Multiple Views";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate sample OHLC data
        ohlcData = DemoDataGenerator.generateOHLCData(400, 100f, 3f,
                DemoConfig.DEFAULT_BAR_DURATION, 42);

        // Create derived XY data views from the OHLC data
        // These are lightweight views, not copies
        closeData = extractClosePrices(ohlcData);
        highData = extractHighPrices(ohlcData);
        lowData = extractLowPrices(ohlcData);

        // Add primary candlestick series
        candleSeries = chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions()
                .upColor(38, 166, 91)
                .downColor(214, 69, 65)
                .barWidthRatio(0.7f));

        // 2. Close price line - shows price trend
        closeLineSeries = chart.addLineSeries(closeData, new LineSeriesOptions()
                .color(new Color(41, 128, 185))
                .lineWidth(2.0f)
                .displayMode(LineSeriesOptions.DisplayMode.LINE)
                .visible(false));  // Start hidden

        // 3. Close price area - shows price relative to baseline
        closeAreaSeries = chart.addLineSeries(closeData, new LineSeriesOptions()
                .color(new Color(52, 152, 219))
                .fillColor(new Color(52, 152, 219, 40))
                .lineWidth(1.5f)
                .displayMode(LineSeriesOptions.DisplayMode.AREA)
                .baseline(ohlcData.getClose(0))  // Baseline at first close
                .visible(false));  // Start hidden

        // 4. Scatter on highs - marks local maxima
        highScatterSeries = chart.addScatterSeries(highData, new ScatterSeriesOptions()
                .color(new Color(46, 204, 113))
                .markerSize(6)
                .markerShape(ScatterSeriesOptions.MarkerShape.TRIANGLE_UP)
                .visible(false));  // Start hidden

        // 5. Scatter on lows - marks local minima
        lowScatterSeries = chart.addScatterSeries(lowData, new ScatterSeriesOptions()
                .color(new Color(231, 76, 60))
                .markerSize(6)
                .markerShape(ScatterSeriesOptions.MarkerShape.TRIANGLE_DOWN)
                .visible(false));  // Start hidden

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Same data, multiple visualizations:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Toggle candlesticks
        panel.add(DemoUIHelper.createCheckBox("Candlesticks", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            candleSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        // Toggle close line
        panel.add(DemoUIHelper.createCheckBox("Close Line", false, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            closeLineSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        // Toggle close area
        panel.add(DemoUIHelper.createCheckBox("Close Area", false, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            closeAreaSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        // Toggle high markers
        panel.add(DemoUIHelper.createCheckBox("High Markers", false, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            highScatterSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        // Toggle low markers
        panel.add(DemoUIHelper.createCheckBox("Low Markers", false, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            lowScatterSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Quick presets
        panel.add(DemoUIHelper.createControlLabel("Presets:"));

        JButton candleOnlyBtn = new JButton("Candles Only");
        candleOnlyBtn.addActionListener(e -> {
            candleSeries.getOptions().visible(true);
            closeLineSeries.getOptions().visible(false);
            closeAreaSeries.getOptions().visible(false);
            highScatterSeries.getOptions().visible(false);
            lowScatterSeries.getOptions().visible(false);
            chart.repaint();
        });
        panel.add(candleOnlyBtn);

        JButton lineOnlyBtn = new JButton("Line Only");
        lineOnlyBtn.addActionListener(e -> {
            candleSeries.getOptions().visible(false);
            closeLineSeries.getOptions().visible(true);
            closeAreaSeries.getOptions().visible(false);
            highScatterSeries.getOptions().visible(false);
            lowScatterSeries.getOptions().visible(false);
            chart.repaint();
        });
        panel.add(lineOnlyBtn);

        JButton overlayBtn = new JButton("Full Overlay");
        overlayBtn.addActionListener(e -> {
            candleSeries.getOptions().visible(true);
            closeLineSeries.getOptions().visible(true);
            closeAreaSeries.getOptions().visible(false);
            highScatterSeries.getOptions().visible(true);
            lowScatterSeries.getOptions().visible(true);
            chart.repaint();
        });
        panel.add(overlayBtn);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    /**
     * Extracts close prices from OHLC data as XyData.
     */
    private XyData extractClosePrices(OhlcData ohlc) {
        XyData closes = new XyData("close", "Close Price", ohlc.size());
        long[] timestamps = ohlc.getTimestampsArray();
        float[] closeArray = ohlc.getCloseArray();

        for (int i = 0; i < ohlc.size(); i++) {
            closes.append(timestamps[i], closeArray[i]);
        }

        return closes;
    }

    /**
     * Extracts high prices from OHLC data as XyData.
     */
    private XyData extractHighPrices(OhlcData ohlc) {
        XyData highs = new XyData("high", "High Price", ohlc.size());
        long[] timestamps = ohlc.getTimestampsArray();
        float[] highArray = ohlc.getHighArray();

        for (int i = 0; i < ohlc.size(); i++) {
            highs.append(timestamps[i], highArray[i]);
        }

        return highs;
    }

    /**
     * Extracts low prices from OHLC data as XyData.
     */
    private XyData extractLowPrices(OhlcData ohlc) {
        XyData lows = new XyData("low", "Low Price", ohlc.size());
        long[] timestamps = ohlc.getTimestampsArray();
        float[] lowArray = ohlc.getLowArray();

        for (int i = 0; i < ohlc.size(); i++) {
            lows.append(timestamps[i], lowArray[i]);
        }

        return lows;
    }

    public static void main(String[] args) {
        launch(new SameDataMultiViewDemo(), args);
    }
}
