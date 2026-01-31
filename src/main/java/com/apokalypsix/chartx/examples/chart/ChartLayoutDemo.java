package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.ChartLayout;
import com.apokalypsix.chartx.chart.ChartType;
import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.EMA;
import com.apokalypsix.chartx.chart.finance.indicator.SMA;
import com.apokalypsix.chartx.chart.style.HistogramSeriesOptions;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing the new unified Chart and ChartLayout classes.
 *
 * <p>This demo demonstrates:
 * <ul>
 *   <li>The unified Chart class combining ChartPanel and ChartPane features</li>
 *   <li>ChartLayout for arranging multiple charts (VSTACK, HSTACK, GRID)</li>
 *   <li>Time axis synchronization across charts</li>
 *   <li>Crosshair synchronization</li>
 *   <li>Resizable dividers</li>
 * </ul>
 */
public class ChartLayoutDemo extends AbstractDemo {

    private ChartLayout layout;

    @Override
    protected String getDemoTitle() {
        return "ChartX - ChartLayout Demo";
    }

    @Override
    protected Component createDemoContent() {
        // Create layout with VSTACK mode (price + volume)
        layout = new ChartLayout(ChartLayout.LayoutMode.VSTACK, getBackend());

        // Generate sample data
        OhlcData data = DemoDataGenerator.generateOHLCData();

        // Create price chart (70% height)
        FinanceChart priceChart = layout.addFinanceChart("price", 0.7);
        priceChart.setChartType(ChartType.PRICE);
        priceChart.addCandlestickSeries(data, new OhlcSeriesOptions());
        priceChart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Add moving averages to price chart
        XyData sma20 = SMA.calculate(data, 20);
        XyData sma50 = SMA.calculate(data, 50);
        XyData ema9 = EMA.calculate(data, 9);

        priceChart.addLineSeries(sma20, new LineSeriesOptions()
                .color(new Color(255, 193, 7))
                .lineWidth(1.5f));

        priceChart.addLineSeries(sma50, new LineSeriesOptions()
                .color(new Color(33, 150, 243))
                .lineWidth(1.5f));

        priceChart.addLineSeries(ema9, new LineSeriesOptions()
                .color(new Color(156, 39, 176))
                .lineWidth(1.5f));

        // Create volume chart (30% height)
        Chart volumeChart = layout.addChart("volume", 0.3);
        volumeChart.setChartType(ChartType.VOLUME);
        HistogramData volumeData = DemoDataGenerator.generateVolumeData(data);
        volumeChart.addHistogramSeries(volumeData, new HistogramSeriesOptions());
        volumeChart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Set bar duration for layout
        layout.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        return layout;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Info label
        panel.add(DemoUIHelper.createInfoLabel("Unified Chart + ChartLayout demo"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Layout mode combo
        String[] modes = {"VSTACK", "HSTACK", "GRID"};
        JComboBox<String> modeCombo = new JComboBox<>(modes);
        modeCombo.addActionListener(e -> {
            String selected = (String) modeCombo.getSelectedItem();
            if (selected != null) {
                layout.setLayoutMode(ChartLayout.LayoutMode.valueOf(selected));
            }
        });
        panel.add(new JLabel("Layout: "));
        panel.add(modeCombo);
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Grid columns spinner (for GRID mode)
        SpinnerNumberModel gridModel = new SpinnerNumberModel(2, 1, 4, 1);
        JSpinner gridSpinner = new JSpinner(gridModel);
        gridSpinner.addChangeListener(e -> {
            layout.setGridColumns((Integer) gridSpinner.getValue());
        });
        panel.add(new JLabel("Grid Cols: "));
        panel.add(gridSpinner);
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Sync time axis toggle
        panel.add(DemoUIHelper.createCheckBox("Sync Time", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            layout.setSyncTimeAxis(cb.isSelected());
        }));

        // Sync crosshair toggle
        panel.add(DemoUIHelper.createCheckBox("Sync Crosshair", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            layout.setSyncCrosshair(cb.isSelected());
        }));

        // Resizable dividers toggle
        panel.add(DemoUIHelper.createCheckBox("Resizable", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            layout.setDividersResizable(cb.isSelected());
        }));

        // Add third chart button
        JButton addChartBtn = new JButton("Add RSI");
        addChartBtn.addActionListener(e -> {
            if (layout.getChart("rsi") == null) {
                OhlcData data = ((FinanceChart) layout.getChart("price")).getPrimaryData();

                // Calculate RSI
                XyData rsiData = calculateRSI(data, 14);

                Chart rsiChart = layout.addChart("rsi", 0.2);
                rsiChart.setChartType(ChartType.INDICATOR);
                rsiChart.addLineSeries(rsiData, new LineSeriesOptions()
                        .color(new Color(233, 30, 99))
                        .lineWidth(1.5f));
                rsiChart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

                // Adjust ratios
                layout.setChartRatio("price", 0.5);
                layout.setChartRatio("volume", 0.3);

                addChartBtn.setEnabled(false);
            }
        });
        panel.add(addChartBtn);

        return panel;
    }

    /**
     * Simple RSI calculation for demo purposes.
     */
    private XyData calculateRSI(OhlcData data, int period) {
        XyData result = new XyData("rsi", "RSI(" + period + ")", data.size());

        float[] closes = data.getCloseArray();
        long[] timestamps = data.getTimestampsArray();

        if (data.size() < period + 1) {
            return result;
        }

        // Calculate gains and losses
        float avgGain = 0;
        float avgLoss = 0;

        for (int i = 1; i <= period; i++) {
            float change = closes[i] - closes[i - 1];
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }

        avgGain /= period;
        avgLoss /= period;

        // First RSI value
        float rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        float rsi = 100 - (100 / (1 + rs));
        result.append(timestamps[period], rsi);

        // Subsequent values using smoothed moving average
        for (int i = period + 1; i < data.size(); i++) {
            float change = closes[i] - closes[i - 1];
            float gain = change > 0 ? change : 0;
            float loss = change < 0 ? Math.abs(change) : 0;

            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;

            rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            rsi = 100 - (100 / (1 + rs));
            result.append(timestamps[i], rsi);
        }

        return result;
    }

    @Override
    protected void onDemoClosing() {
        if (layout != null) {
            layout.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new ChartLayoutDemo(), args);
    }
}
