package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorInstance;
import com.apokalypsix.chartx.chart.series.CandlestickSeries;
import com.apokalypsix.chartx.chart.series.ScatterSeries;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.chart.style.ScatterSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Comprehensive demo showing all series types in ChartX.
 *
 * <p>This demo showcases:
 * <ul>
 *   <li>CandlestickSeries - OHLC candles</li>
 *   <li>LineSeries - EMA overlays with different display modes</li>
 *   <li>BandSeries - Bollinger Bands</li>
 *   <li>ScatterSeries - Signal markers</li>
 * </ul>
 *
 * <p>All series types rendered together in a single chart,
 * demonstrating the flexibility of the new architecture.
 */
public class AllSeriesTypesDemo extends AbstractDemo {

    private FinanceChart chart;

    private OhlcData ohlcData;
    private XyData signalData;

    private CandlestickSeries candleSeries;
    private ScatterSeries signalSeries;

    // Indicators (using the indicator system)
    private IndicatorInstance<?, ?> ema20Indicator;
    private IndicatorInstance<?, ?> ema50Indicator;
    private IndicatorInstance<?, ?> bollingerIndicator;

    public AllSeriesTypesDemo() {
        setWindowSize(1500, 900);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - All Series Types";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate sample data
        ohlcData = DemoDataGenerator.generateOHLCData(500, 100f, 3f,
                DemoConfig.DEFAULT_BAR_DURATION, 42);

        // Add primary candlestick series
        candleSeries = chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions()
                .upColor(38, 166, 91)
                .downColor(214, 69, 65)
                .barWidthRatio(0.75f));

        // 1. Add Bollinger Bands using indicator system
        bollingerIndicator = chart.addIndicator("bollinger", Map.of(
                "period", 20,
                "stdDev", 2.0,
                "color", new Color(52, 152, 219)
        ));

        // 2. Add EMAs using indicator system
        ema20Indicator = chart.addIndicator("ema", Map.of(
                "period", 20,
                "color", new Color(255, 193, 7)
        ));

        ema50Indicator = chart.addIndicator("ema", Map.of(
                "period", 50,
                "color", new Color(156, 39, 176)
        ));

        // 3. Add signals as scatter points (still using direct series API)
        signalData = generateSignals(ohlcData);
        signalSeries = chart.addScatterSeries(signalData, new ScatterSeriesOptions()
                .color(new Color(46, 204, 113))
                .markerSize(10)
                .markerShape(ScatterSeriesOptions.MarkerShape.DIAMOND)
                .zOrder(20));

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Series Types:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Toggle candles
        panel.add(DemoUIHelper.createCheckBox("Candlesticks", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            candleSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        // Toggle Bollinger
        panel.add(DemoUIHelper.createCheckBox("Bollinger Bands", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            bollingerIndicator.setEnabled(visible);
            chart.repaint();
        }));

        // Toggle EMA 20
        panel.add(DemoUIHelper.createCheckBox("EMA 20 (Yellow)", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            ema20Indicator.setEnabled(visible);
            chart.repaint();
        }));

        // Toggle EMA 50
        panel.add(DemoUIHelper.createCheckBox("EMA 50 (Purple)", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            ema50Indicator.setEnabled(visible);
            chart.repaint();
        }));

        // Toggle signals
        panel.add(DemoUIHelper.createCheckBox("Signals (Green)", true, e -> {
            boolean visible = ((JCheckBox) e.getSource()).isSelected();
            signalSeries.getOptions().visible(visible);
            chart.repaint();
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Marker shape selector
        panel.add(DemoUIHelper.createControlLabel("Signal Shape:"));
        JComboBox<String> shapeCombo = DemoUIHelper.createComboBox(
                "Diamond", "Circle", "Square", "Triangle");
        shapeCombo.addActionListener(e -> {
            ScatterSeriesOptions.MarkerShape shape = switch (shapeCombo.getSelectedIndex()) {
                case 1 -> ScatterSeriesOptions.MarkerShape.CIRCLE;
                case 2 -> ScatterSeriesOptions.MarkerShape.SQUARE;
                case 3 -> ScatterSeriesOptions.MarkerShape.TRIANGLE_UP;
                default -> ScatterSeriesOptions.MarkerShape.DIAMOND;
            };
            signalSeries.getOptions().markerShape(shape);
            chart.repaint();
        });
        panel.add(shapeCombo);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    /**
     * Generates sample trading signals (when EMA 20 crosses above EMA 50).
     */
    private XyData generateSignals(OhlcData ohlc) {
        XyData signals = new XyData("signals", "Signals", 50);

        long[] timestamps = ohlc.getTimestampsArray();
        float[] closes = ohlc.getCloseArray();

        // Simple crossover detection
        float prevEma20 = 0, prevEma50 = 0;
        float ema20 = 0, ema50 = 0;
        float mult20 = 2f / 21f;
        float mult50 = 2f / 51f;

        for (int i = 0; i < ohlc.size(); i++) {
            if (i == 0) {
                ema20 = closes[i];
                ema50 = closes[i];
            } else {
                prevEma20 = ema20;
                prevEma50 = ema50;
                ema20 = (closes[i] - ema20) * mult20 + ema20;
                ema50 = (closes[i] - ema50) * mult50 + ema50;

                // Detect bullish crossover
                if (i > 50 && prevEma20 <= prevEma50 && ema20 > ema50) {
                    signals.append(timestamps[i], closes[i]);
                }
            }
        }

        return signals;
    }

    public static void main(String[] args) {
        launch(new AllSeriesTypesDemo(), args);
    }
}
