package com.apokalypsix.chartx.examples.chart;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.EMA;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Basic chart demo showing core ChartX features:
 * - Candlestick rendering
 * - Grid lines
 * - Time and price axes with labels
 * - Crosshair cursor with time/price readout
 * - EMA indicator overlays
 *
 * <p>This demo can be run with any rendering backend by using the
 * appropriate launcher from the opengl/, vulkan/, or metal/ packages.
 */
public class ChartDemo extends AbstractDemo {

    private FinanceChart chart;

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Basic Chart";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate sample data
        OhlcData data = DemoDataGenerator.generateOHLCData();
        chart.addCandlestickSeries(data, new OhlcSeriesOptions());
        chart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Add EMA indicators
        XyData ema9 = EMA.calculate(data, 9);
        XyData ema20 = EMA.calculate(data, 20);
        XyData ema50 = EMA.calculate(data, 50);

        chart.addLineSeries(ema9, new LineSeriesOptions()
                .color(DemoConfig.EMA_9_COLOR)
                .lineWidth(DemoConfig.LINE_WIDTH_STANDARD));

        chart.addLineSeries(ema20, new LineSeriesOptions()
                .color(DemoConfig.EMA_20_COLOR)
                .lineWidth(DemoConfig.LINE_WIDTH_STANDARD));

        chart.addLineSeries(ema50, new LineSeriesOptions()
                .color(DemoConfig.EMA_50_COLOR)
                .lineWidth(DemoConfig.LINE_WIDTH_THICK));

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Info label
        panel.add(DemoUIHelper.createInfoLabel("Pan: Left-click + drag | Zoom: Mouse wheel"));
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

        // Auto-scale Y toggle
        panel.add(DemoUIHelper.createCheckBox("Auto-scale Y", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            chart.getYAxis().setAutoRange(cb.isSelected() ? YAxis.AutoRangeMode.ALWAYS : YAxis.AutoRangeMode.NEVER);
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Zoom to fit button
        panel.add(DemoUIHelper.createButton("Zoom to Fit", e -> chart.zoomToFit()));

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new ChartDemo(), args);
    }
}
