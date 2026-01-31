package com.apokalypsix.chartx.examples.chart;

import java.awt.Component;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.ChartLayout;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.Timeframe;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.core.data.TimeframeAggregator;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Multi-timeframe chart demo:
 * - Multiple timeframes displayed side by side
 * - Automatic data aggregation from base timeframe
 * - Horizontal, vertical, and grid layouts
 * - Synchronized crosshair across timeframes
 *
 * <p>This demo can be run with any rendering backend.
 *
 * <p>Uses ChartLayout with HSTACK mode to display multiple timeframes side by side.
 */
public class MultiTimeframeDemo extends AbstractDemo {

    private ChartLayout layout;
    private OhlcData baseData;

    public MultiTimeframeDemo() {
        setWindowSize(1600, 900);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Multi-Timeframe";
    }

    @Override
    protected Component createDemoContent() {
        // Generate 1-minute base data
        baseData = generateSampleOHLCData(2000);

        // Create chart layout with HSTACK mode
        layout = createChartLayout(ChartLayout.LayoutMode.HSTACK);

        // Add timeframe views with aggregated data
        addTimeframeChart("1m", baseData, Timeframe.M1);
        addTimeframeChart("5m", TimeframeAggregator.aggregate(baseData, Timeframe.M5), Timeframe.M5);
        addTimeframeChart("15m", TimeframeAggregator.aggregate(baseData, Timeframe.M15), Timeframe.M15);
        addTimeframeChart("1h", TimeframeAggregator.aggregate(baseData, Timeframe.H1), Timeframe.H1);

        return layout;
    }

    private void addTimeframeChart(String id, OhlcData data, Timeframe timeframe) {
        FinanceChart chart = layout.addFinanceChart(id, 0.25);
        chart.addCandlestickSeries(data, new OhlcSeriesOptions());
        chart.setBarDuration(timeframe.millis);
        chart.setSymbolInfo("ES", timeframe.displayName, "CME");
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Layout selector
        panel.add(DemoUIHelper.createControlLabel("Layout:"));
        JComboBox<String> layoutCombo = DemoUIHelper.createComboBox("Horizontal", "Vertical", "Grid");
        layoutCombo.addActionListener(e -> {
            String selected = (String) layoutCombo.getSelectedItem();
            ChartLayout.LayoutMode mode = switch (selected) {
                case "Vertical" -> ChartLayout.LayoutMode.VSTACK;
                case "Grid" -> ChartLayout.LayoutMode.GRID;
                default -> ChartLayout.LayoutMode.HSTACK;
            };
            layout.setLayoutMode(mode);
        });
        panel.add(layoutCombo);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Timeframe info
        panel.add(DemoUIHelper.createInfoLabel("Timeframes: 1m, 5m, 15m, 1H (auto-aggregated from 1m base)"));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Grid toggle
        panel.add(DemoUIHelper.createCheckBox("Grid", true, e -> {
            boolean enabled = ((JCheckBox) e.getSource()).isSelected();
            for (Chart chart : layout.getCharts()) {
                chart.setGridEnabled(enabled);
            }
            layout.repaint();
        }));

        // Crosshair toggle
        panel.add(DemoUIHelper.createCheckBox("Crosshair", true, e -> {
            boolean enabled = ((JCheckBox) e.getSource()).isSelected();
            for (Chart chart : layout.getCharts()) {
                chart.setCrosshairEnabled(enabled);
            }
            layout.repaint();
        }));

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (layout != null) {
            layout.dispose();
        }
    }

    private OhlcData generateSampleOHLCData(int bars) {
        OhlcData series = new OhlcData("base_m1", "1 Minute Data", bars);

        Random random = new Random(DemoConfig.DEFAULT_RANDOM_SEED);
        long startTime = System.currentTimeMillis() - (bars * DemoConfig.DEFAULT_BAR_DURATION);

        float price = DemoConfig.DEFAULT_START_PRICE;
        float volatility = DemoConfig.DEFAULT_VOLATILITY;

        for (int i = 0; i < bars; i++) {
            long timestamp = startTime + (i * DemoConfig.DEFAULT_BAR_DURATION);

            // Add some trending behavior
            float trend = (float) Math.sin(i / 200.0) * 2f;
            float change = (random.nextFloat() - 0.48f + trend * 0.1f) * volatility;
            float open = price;
            float close = price + change;

            float wickUp = random.nextFloat() * volatility * 0.5f;
            float high = Math.max(open, close) + wickUp;

            float wickDown = random.nextFloat() * volatility * 0.5f;
            float low = Math.min(open, close) - wickDown;

            float volume = 1000 + random.nextFloat() * 9000;

            series.append(timestamp, open, high, low, close, volume);

            price = close;

            if (random.nextFloat() < 0.03f) {
                volatility = 8f + random.nextFloat() * 10f;
            } else {
                volatility = Math.max(3f, volatility * 0.98f);
            }
        }

        return series;
    }

    public static void main(String[] args) {
        launch(new MultiTimeframeDemo(), args);
    }
}
