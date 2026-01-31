package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.BollingerBands;
import com.apokalypsix.chartx.chart.finance.indicator.EMA;
import com.apokalypsix.chartx.chart.finance.indicator.SMA;
import com.apokalypsix.chartx.chart.finance.indicator.dsl.ExpressionIndicator;
import com.apokalypsix.chartx.chart.finance.indicator.impl.trend.DonchianChannels;
import com.apokalypsix.chartx.chart.finance.indicator.impl.trend.HMAIndicator;
import com.apokalypsix.chartx.chart.finance.indicator.impl.trend.KeltnerChannels;
import com.apokalypsix.chartx.chart.finance.indicator.impl.trend.WMAIndicator;
import com.apokalypsix.chartx.chart.style.BandSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Comprehensive indicators demo showcasing ChartX indicators:
 * <ul>
 *   <li>Trend indicators (SMA, EMA, WMA, HMA, Bollinger, Keltner, etc.)</li>
 *   <li>Momentum oscillators (RSI, MACD, Stochastic, CCI, etc.)</li>
 *   <li>Expression DSL (custom indicator expressions)</li>
 * </ul>
 *
 * <p>This demo can be run with any rendering backend by using the
 * appropriate launcher from the opengl/, vulkan/, or metal/ packages.
 */
public class IndicatorsDemo extends AbstractDemo {

    private FinanceChart chart;
    private OhlcData data;

    // Current indicator selection
    private String selectedIndicator = "EMA";

    // Indicator colors
    private static final Color COLOR_LINE_1 = new Color(33, 150, 243);    // Blue
    private static final Color COLOR_LINE_2 = new Color(255, 152, 0);     // Orange
    private static final Color COLOR_LINE_3 = new Color(156, 39, 176);    // Purple
    private static final Color COLOR_BAND_FILL = new Color(33, 150, 243, 50);

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Indicators";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate sample data
        data = DemoDataGenerator.generateOHLCData();
        chart.addCandlestickSeries(data, new OhlcSeriesOptions());
        chart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Apply initial indicator
        applyIndicator();

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Info label
        panel.add(DemoUIHelper.createInfoLabel("Select indicator:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Indicator selector
        String[] indicatorOptions = {
            "EMA", "SMA", "WMA", "HMA",
            "Bollinger", "Keltner", "Donchian",
            "Expression DSL"
        };

        JComboBox<String> indicatorCombo = new JComboBox<>(indicatorOptions);
        indicatorCombo.setSelectedItem(selectedIndicator);
        indicatorCombo.addActionListener(e -> {
            selectedIndicator = (String) indicatorCombo.getSelectedItem();
            applyIndicator();
        });
        panel.add(indicatorCombo);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Grid toggle
        panel.add(DemoUIHelper.createCheckBox("Grid", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            chart.setGridEnabled(cb.isSelected());
        }));

        // Crosshair toggle
        panel.add(DemoUIHelper.createCheckBox("Crosshair", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            chart.setCrosshairEnabled(cb.isSelected());
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Zoom to fit button
        panel.add(DemoUIHelper.createButton("Zoom to Fit", e -> chart.zoomToFit()));

        return panel;
    }

    /**
     * Applies the selected indicator to the chart.
     */
    private void applyIndicator() {
        // Clear all series and re-add OHLC data
        chart.clearSeries();
        chart.addCandlestickSeries(data, new OhlcSeriesOptions());

        switch (selectedIndicator) {
            case "EMA":
                applyEMA();
                break;
            case "SMA":
                applySMA();
                break;
            case "WMA":
                applyWMA();
                break;
            case "HMA":
                applyHMA();
                break;
            case "Bollinger":
                applyBollingerBands();
                break;
            case "Keltner":
                applyKeltnerChannels();
                break;
            case "Donchian":
                applyDonchianChannels();
                break;
            case "Expression DSL":
                applyExpressionIndicator();
                break;
        }

        chart.repaint();
    }

    private void applyEMA() {
        XyData ema9 = EMA.calculate(data, 9);
        XyData ema20 = EMA.calculate(data, 20);
        XyData ema50 = EMA.calculate(data, 50);

        chart.addLineSeries(ema9, new LineSeriesOptions()
                .color(COLOR_LINE_1).lineWidth(1.5f));
        chart.addLineSeries(ema20, new LineSeriesOptions()
                .color(COLOR_LINE_2).lineWidth(1.5f));
        chart.addLineSeries(ema50, new LineSeriesOptions()
                .color(COLOR_LINE_3).lineWidth(2.0f));
    }

    private void applySMA() {
        XyData sma20 = SMA.calculate(data, 20);
        XyData sma50 = SMA.calculate(data, 50);
        XyData sma200 = SMA.calculate(data, 200);

        chart.addLineSeries(sma20, new LineSeriesOptions()
                .color(COLOR_LINE_1).lineWidth(1.5f));
        chart.addLineSeries(sma50, new LineSeriesOptions()
                .color(COLOR_LINE_2).lineWidth(1.5f));
        chart.addLineSeries(sma200, new LineSeriesOptions()
                .color(COLOR_LINE_3).lineWidth(2.0f));
    }

    private void applyWMA() {
        WMAIndicator wma9 = new WMAIndicator(9);
        WMAIndicator wma20 = new WMAIndicator(20);

        XyData wma9Data = wma9.calculate(data);
        XyData wma20Data = wma20.calculate(data);

        chart.addLineSeries(wma9Data, new LineSeriesOptions()
                .color(COLOR_LINE_1).lineWidth(1.5f));
        chart.addLineSeries(wma20Data, new LineSeriesOptions()
                .color(COLOR_LINE_2).lineWidth(1.5f));
    }

    private void applyHMA() {
        HMAIndicator hma9 = new HMAIndicator(9);
        HMAIndicator hma20 = new HMAIndicator(20);

        XyData hma9Data = hma9.calculate(data);
        XyData hma20Data = hma20.calculate(data);

        chart.addLineSeries(hma9Data, new LineSeriesOptions()
                .color(COLOR_LINE_1).lineWidth(1.5f));
        chart.addLineSeries(hma20Data, new LineSeriesOptions()
                .color(COLOR_LINE_2).lineWidth(1.5f));
    }

    private void applyBollingerBands() {
        XyyData bands = BollingerBands.calculate(data, 20, 2.0f);

        chart.addBandSeries(bands, new BandSeriesOptions()
                .fillColor(COLOR_BAND_FILL)
                .upperColor(COLOR_LINE_1)
                .lowerColor(COLOR_LINE_1)
                .middleColor(COLOR_LINE_2)
                .lineWidth(1.0f));
    }

    private void applyKeltnerChannels() {
        KeltnerChannels keltner = new KeltnerChannels(20, 10, 2.0f);
        XyyData bands = keltner.calculate(data);

        chart.addBandSeries(bands, new BandSeriesOptions()
                .fillColor(new Color(156, 39, 176, 50))
                .upperColor(COLOR_LINE_3)
                .lowerColor(COLOR_LINE_3)
                .middleColor(COLOR_LINE_2)
                .lineWidth(1.0f));
    }

    private void applyDonchianChannels() {
        DonchianChannels donchian = new DonchianChannels(20);
        XyyData bands = donchian.calculate(data);

        chart.addBandSeries(bands, new BandSeriesOptions()
                .fillColor(new Color(0, 150, 136, 50))
                .upperColor(new Color(0, 150, 136))
                .lowerColor(new Color(0, 150, 136))
                .middleColor(COLOR_LINE_2)
                .lineWidth(1.0f));
    }

    private void applyExpressionIndicator() {
        // Custom expression: SMA + 2*ATR bands
        ExpressionIndicator upper = new ExpressionIndicator("Upper Band", "SMA(close, 20) + ATR(14) * 2");
        ExpressionIndicator middle = new ExpressionIndicator("Middle", "SMA(close, 20)");
        ExpressionIndicator lower = new ExpressionIndicator("Lower Band", "SMA(close, 20) - ATR(14) * 2");

        XyData upperData = upper.calculate(data);
        XyData middleData = middle.calculate(data);
        XyData lowerData = lower.calculate(data);

        chart.addLineSeries(upperData, new LineSeriesOptions()
                .color(COLOR_LINE_1).lineWidth(1.0f));
        chart.addLineSeries(middleData, new LineSeriesOptions()
                .color(COLOR_LINE_2).lineWidth(1.5f));
        chart.addLineSeries(lowerData, new LineSeriesOptions()
                .color(COLOR_LINE_1).lineWidth(1.0f));
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new IndicatorsDemo(), args);
    }
}
