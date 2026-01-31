package com.apokalypsix.chartx.examples.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.apokalypsix.chartx.chart.ChartLayout;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.EMA;
import com.apokalypsix.chartx.chart.finance.indicator.SMA;
import com.apokalypsix.chartx.chart.interaction.DrawingTool;
import com.apokalypsix.chartx.chart.overlay.Callout;
import com.apokalypsix.chartx.chart.overlay.Drawing;
import com.apokalypsix.chartx.chart.overlay.Note;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.core.interaction.DrawingInteractionHandler;
import com.apokalypsix.chartx.core.ui.config.ChartUIConfig;
import com.apokalypsix.chartx.core.ui.sidebar.ChartSidebar;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoConfig;
import com.apokalypsix.chartx.examples.library.DemoDataGenerator;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing TradingView-style UI components:
 * - Left sidebar with drawing tools and indicators
 * - Foldable info overlays showing symbol, OHLC, and indicator values
 * - Dark theme styling
 * - Automatic pane creation for separate-pane indicators (RSI, MACD, OBV, CVD)
 *
 * <p>This demo can be run with any rendering backend by using the
 * appropriate launcher from the opengl/, vulkan/, or metal/ packages.
 */
public class OhlcDemo extends AbstractDemo {

    private ChartLayout layout;
    private FinanceChart priceChart;

    @Override
    protected String getDemoTitle() {
        return "ChartX - TradingView Style UI";
    }

    @Override
    protected Component createDemoContent() {
        // Create chart layout for multi-pane support
        layout = createChartLayout(ChartLayout.LayoutMode.VSTACK);

        // Create main price chart (FinanceChart for OHLC data and indicators)
        priceChart = layout.addFinanceChart("price", 1.0);

        // Generate sample data
        OhlcData data = DemoDataGenerator.generateOHLCData();
        priceChart.addCandlestickSeries(data, new OhlcSeriesOptions());
        priceChart.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);
        layout.setBarDuration(DemoConfig.DEFAULT_BAR_DURATION);

        // Set symbol info
        priceChart.setSymbolInfo("AAPL", "1D", "NASDAQ");

        // Configure TradingView-style UI
        priceChart.setChartUIConfig(ChartUIConfig.tradingViewStyle());

        // Note: OHLC data is automatically bound to PriceBarOverlay via addCandlestickSeries()

        // Add some indicators using the new indicator manager
        priceChart.addIndicator("sma", java.util.Map.of("period", 20));
        priceChart.addIndicator("sma", java.util.Map.of("period", 50));
        priceChart.addIndicator("ema", java.util.Map.of("period", 9));

        // Also add visual line series for the indicators
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

        // Set up sidebar tool selection listener
        ChartSidebar sidebar = priceChart.getSidebar();
        if (sidebar != null) {
            sidebar.setToolSelectedListener(toolId -> {
                System.out.println("Selected tool: " + toolId);

                // Handle indicator tool selection
                if (toolId.startsWith("ind_")) {
                    handleIndicatorToolSelection(toolId);
                } else {
                    // Handle drawing tool selection
                    handleDrawingToolSelection(toolId);
                }
            });
        }

        // Set up drawing listener for text input on Note/Callout creation
        priceChart.addDrawingListener(new DrawingInteractionHandler.DrawingListener() {
            @Override
            public void onDrawingCreated(Drawing drawing) {
                if (drawing instanceof Note note) {
                    promptForNoteText(note);
                } else if (drawing instanceof Callout callout) {
                    promptForCalloutText(callout);
                }
            }

            @Override
            public void onDrawingModified(Drawing drawing) {}

            @Override
            public void onDrawingDeleted(Drawing drawing) {}

            @Override
            public void onSelectionChanged(Drawing drawing) {}
        });

        return layout;
    }

    private void handleIndicatorToolSelection(String toolId) {
        String indicatorId = switch (toolId) {
            case ChartSidebar.IND_SMA -> "sma";
            case ChartSidebar.IND_EMA -> "ema";
            case ChartSidebar.IND_RSI -> "rsi";
            case ChartSidebar.IND_MACD -> "macd";
            case ChartSidebar.IND_BB -> "bollinger";
            case ChartSidebar.IND_ATR -> "atr";
            case ChartSidebar.IND_VWAP -> "vwap";
            case ChartSidebar.IND_OBV -> "obv";
            case ChartSidebar.IND_CVD -> "cvd";
            default -> null;
        };

        if (indicatorId != null) {
            // Add indicator to main price chart
            priceChart.addIndicator(indicatorId);
            layout.repaint();
        }
    }

    private void handleDrawingToolSelection(String toolId) {
        DrawingTool drawingTool = switch (toolId) {
            case ChartSidebar.TOOL_CURSOR -> DrawingTool.SELECT;
            case ChartSidebar.TOOL_CROSSHAIR -> DrawingTool.NONE;  // Crosshair is not a drawing tool
            case ChartSidebar.TOOL_TREND_LINE -> DrawingTool.TREND_LINE;
            case ChartSidebar.TOOL_HORIZONTAL_LINE -> DrawingTool.HORIZONTAL_LINE;
            case ChartSidebar.TOOL_VERTICAL_LINE -> DrawingTool.VERTICAL_LINE;
            case ChartSidebar.TOOL_RAY -> DrawingTool.RAY;
            case ChartSidebar.TOOL_CHANNEL -> DrawingTool.PARALLEL_CHANNEL;
            case ChartSidebar.TOOL_RECTANGLE -> DrawingTool.RECTANGLE;
            case ChartSidebar.TOOL_ELLIPSE -> DrawingTool.ELLIPSE;
            case ChartSidebar.TOOL_TRIANGLE -> DrawingTool.TRIANGLE;
            case ChartSidebar.TOOL_ARROW -> DrawingTool.ARROW;
            case ChartSidebar.TOOL_FIB_RETRACEMENT -> DrawingTool.FIBONACCI_RETRACEMENT;
            case ChartSidebar.TOOL_FIB_EXTENSION -> DrawingTool.FIBONACCI_EXTENSION;
            case ChartSidebar.TOOL_FIB_FAN -> DrawingTool.FIBONACCI_FAN;
            case ChartSidebar.TOOL_PITCHFORK -> DrawingTool.PITCHFORK;
            case ChartSidebar.TOOL_MEASURE -> DrawingTool.MEASURE_TOOL;
            case ChartSidebar.TOOL_TEXT -> DrawingTool.NOTE;
            case ChartSidebar.TOOL_CALLOUT -> DrawingTool.CALLOUT;
            default -> null;
        };

        if (drawingTool != null) {
            priceChart.setDrawingTool(drawingTool);
        }
    }

    private void promptForNoteText(Note note) {
        SwingUtilities.invokeLater(() -> {
            JTextField titleField = new JTextField(20);
            JTextArea contentArea = new JTextArea(4, 20);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);

            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.add(new JLabel("Title:"), BorderLayout.NORTH);
            panel.add(titleField, BorderLayout.CENTER);

            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(new JLabel("Content:"), BorderLayout.NORTH);
            contentPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

            JPanel mainPanel = new JPanel(new BorderLayout(5, 10));
            mainPanel.add(panel, BorderLayout.NORTH);
            mainPanel.add(contentPanel, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(priceChart, mainPanel, "Add Note",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                note.setTitle(titleField.getText());
                note.setContent(contentArea.getText());
                note.setExpanded(true);
                layout.repaint();
            } else {
                // User cancelled - remove the note
                priceChart.removeDrawing(note);
            }
        });
    }

    private void promptForCalloutText(Callout callout) {
        SwingUtilities.invokeLater(() -> {
            String text = JOptionPane.showInputDialog(priceChart, "Enter callout text:", "Add Callout",
                    JOptionPane.PLAIN_MESSAGE);

            if (text != null && !text.isEmpty()) {
                callout.setText(text);
                layout.repaint();
            } else if (text == null) {
                // User cancelled - remove the callout
                priceChart.removeDrawing(callout);
            }
        });
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        // Info label
        panel.add(DemoUIHelper.createInfoLabel("TradingView-style UI with sidebar and overlays"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Sidebar toggle
        panel.add(DemoUIHelper.createCheckBox("Sidebar", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            priceChart.setSidebarVisible(cb.isSelected());
        }));

        // Overlays toggle
        panel.add(DemoUIHelper.createCheckBox("Info Overlays", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            priceChart.setInfoOverlaysVisible(cb.isSelected());
        }));

        // Grid toggle
        panel.add(DemoUIHelper.createCheckBox("Grid", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            priceChart.setGridEnabled(cb.isSelected());
        }));

        // Crosshair toggle
        panel.add(DemoUIHelper.createCheckBox("Crosshair", true, e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            priceChart.setCrosshairEnabled(cb.isSelected());
        }));

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (layout != null) {
            layout.dispose();
        }
    }

    public static void main(String[] args) {
        launch(new OhlcDemo(), args);
    }
}
