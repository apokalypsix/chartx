package com.apokalypsix.chartx.examples.chart;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;
import com.apokalypsix.chartx.examples.library.TpoDataLoader;

/**
 * TPO (Market Profile) chart demo showing all TPO features:
 * <ul>
 *   <li>TPO blocks with period colors</li>
 *   <li>Point of Control (POC) highlighting</li>
 *   <li>Value Area (VAH/VAL) shading</li>
 *   <li>Initial Balance highlighting</li>
 *   <li>Single prints highlighting</li>
 *   <li>Overlay mode (TPO over candlesticks)</li>
 * </ul>
 *
 * <p>This demo uses real BTC/USDT market profile data loaded from a JSON resource file.
 *
 * <p>This demo can be run with any rendering backend by using the
 * appropriate launcher from the opengl/, vulkan/, or metal/ packages.
 */
public class TPODemo extends AbstractDemo {

    private static final String TPO_DATA_RESOURCE = "/tpo-demo-data.json";

    private FinanceChart chart;
    private OhlcData ohlcData;
    private TPOSeries tpoSeries;

    // Controls
    private JCheckBox overlayModeCheckbox;
    private JSlider opacitySlider;
    private JLabel opacityLabel;

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - TPO / Market Profile (BTC/USDT)";
    }

    @Override
    protected Component createDemoContent() {
        chart = createFinanceChart();

        // Load real TPO data from JSON resource
        try {
            TpoDataLoader.TpoData data = TpoDataLoader.loadFromResource(TPO_DATA_RESOURCE);
            ohlcData = data.ohlcData;
            tpoSeries = data.tpoSeries;

            System.out.println("Loaded TPO data: " + data.symbol + " " + data.timeframe);
            System.out.println("Candles: " + ohlcData.size() + ", Profiles: " + tpoSeries.size());
        } catch (IOException e) {
            System.err.println("Failed to load TPO data: " + e.getMessage());
            e.printStackTrace();
            // Return empty chart on error
            return chart;
        }

        // Add candlestick series
        chart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions());

        // Configure TPO display settings on the series
        tpoSeries.setOverlayMode(true);
        tpoSeries.setOpacity(0.6f);  // Match default slider value
        tpoSeries.setShowPOC(true);
        tpoSeries.setShowValueArea(true);

        // Set TPO data on chart
        chart.setTPOData(tpoSeries);

        // Set bar duration for proper time axis (30 minutes for tf30m data)
        chart.setBarDuration(30 * 60 * 1000);

        // Disable auto-scale Y to enable free panning on both axes
        chart.setAutoScaleY(false);

        return chart;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Info label
        panel.add(DemoUIHelper.createInfoLabel("Pan: Left-click + drag | Zoom: Mouse wheel"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // POC toggle
        panel.add(DemoUIHelper.createCheckBox("POC", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            tpoSeries.setShowPOC(cb.isSelected());
            chart.repaint();
        }));

        // Value Area toggle
        panel.add(DemoUIHelper.createCheckBox("Value Area", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            tpoSeries.setShowValueArea(cb.isSelected());
            chart.repaint();
        }));

        // Initial Balance toggle
        panel.add(DemoUIHelper.createCheckBox("Initial Balance", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            tpoSeries.setShowInitialBalance(cb.isSelected());
            chart.repaint();
        }));

        // Single Prints toggle
        panel.add(DemoUIHelper.createCheckBox("Single Prints", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            tpoSeries.setHighlightSinglePrints(cb.isSelected());
            chart.repaint();
        }));

        panel.add(DemoUIHelper.createHorizontalSpacer());
        panel.add(DemoUIHelper.createVerticalSeparator());
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Overlay mode toggle (on by default since candles are always shown)
        overlayModeCheckbox = DemoUIHelper.createCheckBox("Overlay Mode", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            setOverlayMode(cb.isSelected());
        });
        panel.add(overlayModeCheckbox);

        // Opacity slider (visible by default since overlay mode is on)
        opacityLabel = DemoUIHelper.createControlLabel("Opacity: 60%");
        panel.add(opacityLabel);

        opacitySlider = new JSlider(10, 100, 60);
        opacitySlider.setPreferredSize(new Dimension(100, 20));
        opacitySlider.setOpaque(false);
        opacitySlider.addChangeListener(e -> {
            int value = opacitySlider.getValue();
            opacityLabel.setText("Opacity: " + value + "%");
            tpoSeries.setOpacity(value / 100f);
            chart.repaint();
        });
        panel.add(opacitySlider);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Zoom to fit button
        panel.add(DemoUIHelper.createButton("Zoom to Fit", e -> chart.zoomToFit()));

        return panel;
    }

    /**
     * Enables or disables overlay mode.
     * In overlay mode, TPO is rendered semi-transparent over candlesticks.
     * When disabled, TPO is rendered opaque (candles still visible behind).
     */
    private void setOverlayMode(boolean overlay) {
        tpoSeries.setOverlayMode(overlay);
        chart.repaint();
        // Show/hide opacity controls based on overlay mode
        opacityLabel.setVisible(overlay);
        opacitySlider.setVisible(overlay);
    }

    @Override
    protected void onDemoClosing() {
        if (chart != null) {
            chart.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new TPODemo(), args);
    }
}
