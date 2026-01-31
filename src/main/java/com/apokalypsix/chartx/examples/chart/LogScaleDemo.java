package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Random;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.axis.scale.AxisScale;
import com.apokalypsix.chartx.chart.axis.scale.LinearScale;
import com.apokalypsix.chartx.chart.axis.scale.LogarithmicScale;
import com.apokalypsix.chartx.chart.axis.scale.PercentageScale;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing different axis scale types.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Logarithmic scale for exponential data (crypto/long-term stocks)</li>
 *   <li>Percentage scale showing relative changes from baseline</li>
 *   <li>Linear scale (default) for comparison</li>
 *   <li>Switching between scale types at runtime</li>
 * </ul>
 *
 * <p>The demo generates crypto-style data that grows exponentially over time,
 * which looks much better on a logarithmic scale where equal percentage moves
 * appear as equal distances.
 */
public class LogScaleDemo extends AbstractDemo {

    private FinanceChart chart;
    private OhlcData data;

    private JLabel scaleLabel;
    private ButtonGroup scaleGroup;

    public LogScaleDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Axis Scales (Linear, Log, Percentage)";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Generate crypto-style data with exponential growth
        data = generateExponentialData();

        chart.addCandlestickSeries(data, new OhlcSeriesOptions());
        chart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Start with logarithmic scale to show the benefit
        chart.getYAxis().setScale(LogarithmicScale.BASE_10);
        chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.ALWAYS);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Scale type selector
        panel.add(DemoUIHelper.createInfoLabel("Axis Scale Type:"));
        panel.add(Box.createHorizontalStrut(10));

        scaleGroup = new ButtonGroup();

        JRadioButton logButton = createScaleButton("Logarithmic (Recommended)", true);
        logButton.addActionListener(e -> setScale(LogarithmicScale.BASE_10, "Logarithmic"));

        JRadioButton linearButton = createScaleButton("Linear", false);
        linearButton.addActionListener(e -> setScale(LinearScale.INSTANCE, "Linear"));

        JRadioButton pctButton = createScaleButton("Percentage", false);
        pctButton.addActionListener(e -> {
            // Use first close price as reference
            double firstPrice = data.getClose(0);
            setScale(new PercentageScale(firstPrice), "Percentage (ref=" + String.format("%.2f", firstPrice) + ")");
        });

        scaleGroup.add(logButton);
        scaleGroup.add(linearButton);
        scaleGroup.add(pctButton);

        panel.add(logButton);
        panel.add(linearButton);
        panel.add(pctButton);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        scaleLabel = DemoUIHelper.createInfoLabel("Scale: Logarithmic");
        scaleLabel.setForeground(new Color(100, 200, 100));
        panel.add(scaleLabel);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Auto-scale toggle
        JCheckBox autoScaleCheck = new JCheckBox("Auto Scale", true);
        autoScaleCheck.setForeground(Color.WHITE);
        autoScaleCheck.addActionListener(e -> {
            chart.getYAxis().setAutoRange(
                    autoScaleCheck.isSelected() ? YAxis.AutoRangeMode.ALWAYS : YAxis.AutoRangeMode.NEVER);
            chart.repaint();
        });
        panel.add(autoScaleCheck);

        // Grid toggle
        panel.add(DemoUIHelper.createCheckBox("Grid", true, e -> {
            chart.setGridEnabled(((JCheckBox) e.getSource()).isSelected());
        }));

        return panel;
    }

    private JRadioButton createScaleButton(String label, boolean selected) {
        JRadioButton button = new JRadioButton(label, selected);
        button.setForeground(Color.WHITE);
        button.setOpaque(false);
        return button;
    }

    private void setScale(AxisScale scale, String name) {
        chart.getYAxis().setScale(scale);
        // Reset auto-range to recalculate with new scale
        chart.getYAxis().resetAutoRange();
        scaleLabel.setText("Scale: " + name);
        chart.repaint();
    }

    /**
     * Generates exponential growth data similar to crypto price movements.
     *
     * <p>The data grows from ~100 to ~10,000+ over 500 bars, which demonstrates
     * why logarithmic scale is better for visualizing percentage-based changes.
     */
    private OhlcData generateExponentialData() {
        int barCount = 500;
        OhlcData ohlcData = new OhlcData("crypto", "BTC-LIKE", barCount);

        Random random = new Random(42);
        long startTime = System.currentTimeMillis() - (barCount * DemoConfig.DEFAULT_BAR_DURATION);

        // Start at 100, simulate exponential growth to ~10,000+
        double price = 100.0;
        double dailyReturn = 0.005;  // 0.5% average daily return
        double volatility = 0.03;    // 3% daily volatility

        for (int i = 0; i < barCount; i++) {
            long timestamp = startTime + (i * DemoConfig.DEFAULT_BAR_DURATION);

            // Log-normal returns for realistic exponential growth
            double returnPct = dailyReturn + volatility * (random.nextGaussian());
            double multiplier = Math.exp(returnPct);

            double open = price;
            double close = price * multiplier;

            // High and low with wicks
            double wickUp = price * volatility * 0.5 * random.nextDouble();
            double wickDown = price * volatility * 0.5 * random.nextDouble();
            double high = Math.max(open, close) + wickUp;
            double low = Math.min(open, close) - wickDown;

            // Ensure positive values for log scale
            low = Math.max(1.0, low);

            float volume = 1000 + random.nextFloat() * 9000;

            ohlcData.append(timestamp, (float) open, (float) high, (float) low, (float) close, volume);

            price = close;

            // Occasional volatility spikes (crypto-style)
            if (random.nextFloat() < 0.03f) {
                volatility = 0.05 + random.nextDouble() * 0.05;
            } else {
                volatility = Math.max(0.02, volatility * 0.98);
            }
        }

        return ohlcData;
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new LogScaleDemo(), args);
    }
}
