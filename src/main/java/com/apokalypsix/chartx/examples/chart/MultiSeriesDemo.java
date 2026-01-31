package com.apokalypsix.chartx.examples.chart;

import java.awt.Component;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

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
 * Demonstrates multiple series on the same chart.
 *
 * <p>This demo showcases:
 * <ul>
 *   <li>Multiple OHLC series rendered together</li>
 *   <li>Different OhlcSeriesOptions for each series (colors, styles)</li>
 *   <li>Line overlays with various LineSeriesOptions</li>
 *   <li>Runtime modification of series options</li>
 * </ul>
 *
 * <p>Use case: Comparing two correlated instruments on the same chart.
 */
public class MultiSeriesDemo extends AbstractDemo {

    private FinanceChart chart;
    private OhlcData primaryData;
    private OhlcData secondaryData;
    private CandlestickSeries primarySeries;
    private CandlestickSeries secondarySeries;
    private LineSeries sma20Series;
    private LineSeries ema10Series;

    public MultiSeriesDemo() {
        setWindowSize(1400, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Multiple Series";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate primary data (e.g., ES Futures)
        primaryData = generatePrimaryData();

        // Generate secondary data (e.g., correlated instrument with offset)
        secondaryData = generateSecondaryData(primaryData);

        // Add primary candlestick series
        OhlcSeriesOptions primaryOptions = new OhlcSeriesOptions()
                .upColor(38, 166, 91)      // Green
                .downColor(214, 69, 65)    // Red
                .barWidthRatio(0.7f);
        primarySeries = chart.addCandlestickSeries(primaryData, primaryOptions);

        // Add secondary candlestick series with blue/orange colors
        OhlcSeriesOptions secondaryOptions = new OhlcSeriesOptions()
                .upColor(52, 152, 219)     // Blue
                .downColor(230, 126, 34)   // Orange
                .barWidthRatio(0.5f)       // Narrower bars
                .style(OhlcSeriesOptions.OhlcStyle.HOLLOW_CANDLE);
        secondarySeries = chart.addCandlestickSeries(secondaryData, secondaryOptions);

        // Add SMA 20 overlay on primary data
        XyData sma20Data = DemoDataGenerator.calculateSMA(primaryData, 20);
        LineSeriesOptions sma20Options = new LineSeriesOptions()
                .color(255, 193, 7)        // Amber
                .lineWidth(2.0f);
        sma20Series = chart.addLineSeries(sma20Data, sma20Options);

        // Add EMA 10 overlay on primary data
        XyData ema10Data = DemoDataGenerator.calculateEMA(primaryData, 10);
        LineSeriesOptions ema10Options = new LineSeriesOptions()
                .color(156, 39, 176)       // Purple
                .lineWidth(1.5f);
        ema10Series = chart.addLineSeries(ema10Data, ema10Options);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Primary series visibility
        panel.add(DemoUIHelper.createCheckBox("Primary (Green/Red)", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            primarySeries.getOptions().visible(visible);
            chart.repaint();
        }));

        // Secondary series visibility
        panel.add(DemoUIHelper.createCheckBox("Secondary (Blue/Orange)", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            secondarySeries.getOptions().visible(visible);
            chart.repaint();
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // SMA visibility
        panel.add(DemoUIHelper.createCheckBox("SMA 20", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            sma20Series.getOptions().visible(visible);
            chart.repaint();
        }));

        // EMA visibility
        panel.add(DemoUIHelper.createCheckBox("EMA 10", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            ema10Series.getOptions().visible(visible);
            chart.repaint();
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Secondary style selector
        panel.add(DemoUIHelper.createControlLabel("Secondary Style:"));
        JComboBox<String> styleCombo = DemoUIHelper.createComboBox("Hollow", "Candlestick", "OHLC Bar", "Line");
        styleCombo.addActionListener(e -> {
            String selected = (String) styleCombo.getSelectedItem();
            OhlcSeriesOptions.OhlcStyle style = switch (selected) {
                case "Candlestick" -> OhlcSeriesOptions.OhlcStyle.CANDLESTICK;
                case "OHLC Bar" -> OhlcSeriesOptions.OhlcStyle.OHLC_BAR;
                case "Line" -> OhlcSeriesOptions.OhlcStyle.LINE;
                default -> OhlcSeriesOptions.OhlcStyle.HOLLOW_CANDLE;
            };
            secondarySeries.getOptions().style(style);
            chart.repaint();
        });
        panel.add(styleCombo);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    private OhlcData generatePrimaryData() {
        return DemoDataGenerator.generateOHLCData(
                600,                               // bars
                4500f,                             // start price
                15f,                               // volatility
                DemoConfig.DEFAULT_BAR_DURATION,   // 1-minute bars
                42                                 // seed
        );
    }

    private OhlcData generateSecondaryData(OhlcData reference) {
        // Generate correlated data with an offset and slight divergence
        OhlcData secondary = new OhlcData("secondary", "Secondary Instrument", reference.size());

        Random random = new Random(123);
        float offset = 50f;  // Price offset
        float correlation = 0.85f;  // Correlation factor

        long[] timestamps = reference.getTimestampsArray();
        float[] refOpens = reference.getOpenArray();
        float[] refHighs = reference.getHighArray();
        float[] refLows = reference.getLowArray();
        float[] refCloses = reference.getCloseArray();
        float[] refVolumes = reference.getVolumeArray();

        float drift = 0;  // Cumulative drift

        for (int i = 0; i < reference.size(); i++) {
            // Add some divergence
            drift += (random.nextFloat() - 0.5f) * 0.5f;

            float noise = (random.nextFloat() - 0.5f) * 5f;
            float open = refOpens[i] + offset + drift + noise;
            float high = refHighs[i] + offset + drift + noise + random.nextFloat() * 2f;
            float low = refLows[i] + offset + drift + noise - random.nextFloat() * 2f;
            float close = refCloses[i] + offset + drift + noise;
            float volume = refVolumes[i] * (0.8f + random.nextFloat() * 0.4f);

            secondary.append(timestamps[i], open, high, low, close, volume);
        }

        return secondary;
    }

    public static void main(String[] args) {
        launch(new MultiSeriesDemo(), args);
    }
}
