package com.apokalypsix.chartx.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.ChartX;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorInstance;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.core.interaction.modifier.MouseEventGroup;
import com.apokalypsix.chartx.core.render.api.RenderBackend;

/**
 * Layout manager for arranging multiple Chart instances.
 *
 * <p>ChartLayout supports flat layouts only: VSTACK (vertical), HSTACK (horizontal), and GRID.
 * Charts are sized using ratio-based allocation (0.0-1.0 values) and can be resized
 * via draggable dividers.
 *
 * <p>All charts in a layout can optionally share:
 * <ul>
 *   <li>Time axis (synchronized scrolling and zooming)</li>
 *   <li>Crosshair position (linked crosshairs)</li>
 * </ul>
 *
 * <p>Basic usage:
 * <pre>{@code
 * ChartLayout layout = new ChartLayout(LayoutMode.VSTACK);
 *
 * // Add price chart (70% height)
 * FinanceChart priceChart = layout.addFinanceChart("price", 0.7);
 * priceChart.addCandlestickSeries(ohlcData, new OhlcSeriesOptions());
 *
 * // Add volume chart (30% height)
 * Chart volumeChart = layout.addChart("volume", 0.3);
 * volumeChart.setChartType(ChartType.VOLUME);
 * volumeChart.setHistogramData(volumeData);
 *
 * frame.add(layout);
 * }</pre>
 */
public class ChartLayout extends JPanel implements Chart.RangeChangeListener, AbstractChartComponent.SeparatePaneIndicatorListener {

    private static final Logger log = LoggerFactory.getLogger(ChartLayout.class);

    // ========== Layout Configuration ==========

    /**
     * Layout mode enumeration.
     */
    public enum LayoutMode {
        /** Vertical stack (top to bottom) */
        VSTACK,
        /** Horizontal stack (left to right) */
        HSTACK,
        /** Grid layout (rows x columns) */
        GRID
    }

    private LayoutMode layoutMode = LayoutMode.VSTACK;
    private int gridColumns = 2;

    // ========== Charts ==========

    private final Map<String, ChartSlot> charts = new LinkedHashMap<>();
    private final RenderBackend backend;

    // ========== Synchronization ==========

    private final MouseEventGroup mouseEventGroup = new MouseEventGroup("chartLayout");
    private boolean syncTimeAxis = true;
    private boolean syncCrosshair = true;

    // ========== Dividers ==========

    private static final int DIVIDER_SIZE = 8;
    private static final Color DIVIDER_COLOR = new Color(60, 62, 66);
    private static final Color DIVIDER_HOVER_COLOR = new Color(80, 82, 86);
    private boolean dividersResizable = true;

    // ========== Shared Visible Range ==========

    private long rangeStart = 0;      // X-axis start (timestamp or category index)
    private long rangeEnd = 1;        // X-axis end (timestamp or category index)
    private long barDuration = 0;     // 0 = not applicable (categorical), >0 = time-based

    // ========== Resize State ==========

    private ChartSlot resizingDivider = null;
    private int resizeStartPos;
    private double resizeStartRatio;

    // ========== Constructors ==========

    /**
     * Creates a layout with default VSTACK mode and the default backend.
     *
     * <p>The default backend is determined by {@link ChartX#getDefaultBackend()}.
     *
     * @see ChartX#setDefaultBackend(RenderBackend)
     */
    public ChartLayout() {
        this(LayoutMode.VSTACK, ChartX.getDefaultBackend());
    }

    /**
     * Creates a layout with the specified mode and the default backend.
     *
     * <p>The default backend is determined by {@link ChartX#getDefaultBackend()}.
     *
     * @see ChartX#setDefaultBackend(RenderBackend)
     */
    public ChartLayout(LayoutMode mode) {
        this(mode, ChartX.getDefaultBackend());
    }

    /**
     * Creates a layout with the specified mode and backend.
     */
    public ChartLayout(LayoutMode mode, RenderBackend backend) {
        this.layoutMode = mode;
        this.backend = backend;

        setLayout(null); // Manual layout
        setBackground(new Color(20, 22, 25));

        setupResizeHandlers();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutCharts();
            }
        });
    }

    // ========== Chart Management ==========

    /**
     * Adds a chart with equal ratio (auto-distributed).
     *
     * @param id unique identifier for the chart
     * @return the created chart
     */
    public Chart addChart(String id) {
        return addChart(id, 1.0);
    }

    /**
     * Adds a chart with the specified ratio.
     *
     * @param id unique identifier for the chart
     * @param ratio size ratio (0.0-1.0, relative to total)
     * @return the created chart
     */
    public Chart addChart(String id, double ratio) {
        if (charts.containsKey(id)) {
            throw new IllegalArgumentException("Chart with id '" + id + "' already exists");
        }

        Chart chart = new Chart(id, backend);
        addChart(chart, ratio);
        return chart;
    }

    /**
     * Adds a financial chart with equal ratio (auto-distributed).
     *
     * @param id unique identifier for the chart
     * @return the created finance chart
     */
    public FinanceChart addFinanceChart(String id) {
        return addFinanceChart(id, 1.0);
    }

    /**
     * Adds a financial chart with the specified ratio.
     *
     * @param id unique identifier for the chart
     * @param ratio size ratio (0.0-1.0, relative to total)
     * @return the created finance chart
     */
    public FinanceChart addFinanceChart(String id, double ratio) {
        if (charts.containsKey(id)) {
            throw new IllegalArgumentException("Chart with id '" + id + "' already exists");
        }

        FinanceChart chart = new FinanceChart(id, backend);
        addChart(chart, ratio);
        return chart;
    }

    /**
     * Adds an existing chart with the specified ratio.
     *
     * @param chart the chart to add
     * @param ratio size ratio (0.0-1.0, relative to total)
     */
    public void addChart(Chart chart, double ratio) {
        String id = chart.getId();
        if (charts.containsKey(id)) {
            throw new IllegalArgumentException("Chart with id '" + id + "' already exists");
        }

        // Setup synchronization
        chart.setRangeChangeListener(this);

        // Only apply barDuration to time-based charts
        if (barDuration > 0 && !chart.hasCategoryAxis()) {
            chart.setBarDuration(barDuration);
        }

        // Setup separate-pane indicator listener for automatic indicator pane creation
        chart.setSeparatePaneIndicatorListener(this);

        if (syncCrosshair) {
            mouseEventGroup.add(chart);
        }

        // Sync visible range from existing charts
        syncRangeFromExistingCharts();
        chart.setVisibleRange(rangeStart, rangeEnd);

        // Add to charts map
        ChartSlot slot = new ChartSlot(chart, ratio);
        charts.put(id, slot);

        add(chart);
        layoutCharts();

        log.info("Added chart '{}' with ratio {}", id, ratio);
    }

    /**
     * Returns the chart with the specified ID.
     */
    public Chart getChart(String id) {
        ChartSlot slot = charts.get(id);
        return slot != null ? slot.chart : null;
    }

    /**
     * Removes the chart with the specified ID.
     */
    public void removeChart(String id) {
        ChartSlot slot = charts.remove(id);
        if (slot != null) {
            slot.chart.setRangeChangeListener(null);
            mouseEventGroup.remove(slot.chart);
            remove(slot.chart);
            layoutCharts();
            log.info("Removed chart '{}'", id);
        }
    }

    /**
     * Returns all charts in order.
     */
    public List<Chart> getCharts() {
        List<Chart> result = new ArrayList<>();
        for (ChartSlot slot : charts.values()) {
            result.add(slot.chart);
        }
        return result;
    }

    /**
     * Clears all charts.
     */
    public void clearCharts() {
        List<String> ids = new ArrayList<>(charts.keySet());
        for (String id : ids) {
            removeChart(id);
        }
    }

    // ========== Layout Configuration ==========

    /**
     * Returns the layout mode.
     */
    public LayoutMode getLayoutMode() {
        return layoutMode;
    }

    /**
     * Sets the layout mode.
     */
    public void setLayoutMode(LayoutMode mode) {
        this.layoutMode = mode;
        layoutCharts();
    }

    /**
     * Returns the grid columns (for GRID mode).
     */
    public int getGridColumns() {
        return gridColumns;
    }

    /**
     * Sets the grid columns (for GRID mode).
     */
    public void setGridColumns(int columns) {
        this.gridColumns = Math.max(1, columns);
        if (layoutMode == LayoutMode.GRID) {
            layoutCharts();
        }
    }

    /**
     * Returns the size ratio for a chart.
     */
    public double getChartRatio(String chartId) {
        ChartSlot slot = charts.get(chartId);
        return slot != null ? slot.ratio : 0;
    }

    /**
     * Sets the size ratio for a chart.
     */
    public void setChartRatio(String chartId, double ratio) {
        ChartSlot slot = charts.get(chartId);
        if (slot != null) {
            slot.ratio = ratio;
            layoutCharts();
        }
    }

    /**
     * Returns whether dividers are resizable.
     */
    public boolean isDividersResizable() {
        return dividersResizable;
    }

    /**
     * Sets whether dividers are resizable.
     */
    public void setDividersResizable(boolean resizable) {
        this.dividersResizable = resizable;
    }

    // ========== Synchronization Configuration ==========

    /**
     * Returns whether time axis synchronization is enabled.
     */
    public boolean isSyncTimeAxis() {
        return syncTimeAxis;
    }

    /**
     * Sets whether time axis synchronization is enabled.
     */
    public void setSyncTimeAxis(boolean sync) {
        this.syncTimeAxis = sync;
    }

    /**
     * Returns whether crosshair synchronization is enabled.
     */
    public boolean isSyncCrosshair() {
        return syncCrosshair;
    }

    /**
     * Sets whether crosshair synchronization is enabled.
     */
    public void setSyncCrosshair(boolean sync) {
        this.syncCrosshair = sync;

        // Update mouse event group
        mouseEventGroup.clear();
        if (sync) {
            for (ChartSlot slot : charts.values()) {
                mouseEventGroup.add(slot.chart);
            }
        }
    }

    /**
     * Sets the visible range for all charts.
     *
     * @param start X-axis start value (timestamp for time-based, index for categorical)
     * @param end X-axis end value (timestamp for time-based, index for categorical)
     */
    public void setVisibleRange(long start, long end) {
        this.rangeStart = start;
        this.rangeEnd = end;

        for (ChartSlot slot : charts.values()) {
            slot.chart.setVisibleRange(start, end);
        }
    }

    /**
     * Sets the bar duration for all charts.
     */
    public void setBarDuration(long durationMillis) {
        this.barDuration = durationMillis;

        for (ChartSlot slot : charts.values()) {
            slot.chart.setBarDuration(durationMillis);
        }
    }

    /**
     * Enables viewport following mode for all charts.
     */
    public void setFollowLatestData(boolean follow) {
        for (ChartSlot slot : charts.values()) {
            slot.chart.setFollowLatestData(follow);
        }
    }

    @Override
    public void onRangeChanged(long start, long end) {
        if (!syncTimeAxis) {
            return;
        }

        this.rangeStart = start;
        this.rangeEnd = end;

        // Propagate to all charts
        for (ChartSlot slot : charts.values()) {
            slot.chart.setVisibleRange(start, end);
        }
    }

    private void syncRangeFromExistingCharts() {
        for (ChartSlot slot : charts.values()) {
            long chartEnd = slot.chart.getEndTime();
            long chartStart = slot.chart.getStartTime();
            if (chartEnd - chartStart > 1000) {
                this.rangeStart = chartStart;
                this.rangeEnd = chartEnd;
                return;
            }
        }
    }

    // ========== Layout ==========

    private void layoutCharts() {
        int totalWidth = getWidth();
        int totalHeight = getHeight();

        if (totalWidth <= 0 || totalHeight <= 0 || charts.isEmpty()) {
            return;
        }

        switch (layoutMode) {
            case VSTACK -> layoutVertical(totalWidth, totalHeight);
            case HSTACK -> layoutHorizontal(totalWidth, totalHeight);
            case GRID -> layoutGrid(totalWidth, totalHeight);
        }

        revalidate();
        repaint();
    }

    private void layoutVertical(int totalWidth, int totalHeight) {
        double totalRatio = getTotalRatio();
        if (totalRatio <= 0) return;

        int dividerCount = Math.max(0, charts.size() - 1);
        int availableHeight = totalHeight - (dividerCount * DIVIDER_SIZE);

        int currentY = 0;
        List<ChartSlot> slots = new ArrayList<>(charts.values());

        for (int i = 0; i < slots.size(); i++) {
            ChartSlot slot = slots.get(i);
            int chartHeight = (int) (availableHeight * slot.ratio / totalRatio);

            // Last chart fills remaining space
            if (i == slots.size() - 1) {
                chartHeight = totalHeight - currentY;
            }

            slot.chart.setBounds(0, currentY, totalWidth, chartHeight);
            currentY += chartHeight;

            if (i < slots.size() - 1) {
                currentY += DIVIDER_SIZE;
            }
        }
    }

    private void layoutHorizontal(int totalWidth, int totalHeight) {
        double totalRatio = getTotalRatio();
        if (totalRatio <= 0) return;

        int dividerCount = Math.max(0, charts.size() - 1);
        int availableWidth = totalWidth - (dividerCount * DIVIDER_SIZE);

        int currentX = 0;
        List<ChartSlot> slots = new ArrayList<>(charts.values());

        for (int i = 0; i < slots.size(); i++) {
            ChartSlot slot = slots.get(i);
            int chartWidth = (int) (availableWidth * slot.ratio / totalRatio);

            // Last chart fills remaining space
            if (i == slots.size() - 1) {
                chartWidth = totalWidth - currentX;
            }

            slot.chart.setBounds(currentX, 0, chartWidth, totalHeight);
            currentX += chartWidth;

            if (i < slots.size() - 1) {
                currentX += DIVIDER_SIZE;
            }
        }
    }

    private void layoutGrid(int totalWidth, int totalHeight) {
        int chartCount = charts.size();
        if (chartCount == 0) return;

        int cols = Math.min(gridColumns, chartCount);
        int rows = (int) Math.ceil((double) chartCount / cols);

        int cellWidth = totalWidth / cols;
        int cellHeight = totalHeight / rows;

        List<ChartSlot> slots = new ArrayList<>(charts.values());

        for (int i = 0; i < slots.size(); i++) {
            int row = i / cols;
            int col = i % cols;

            int x = col * cellWidth;
            int y = row * cellHeight;

            // Last column and row fill remaining space
            int width = (col == cols - 1) ? totalWidth - x : cellWidth;
            int height = (row == rows - 1) ? totalHeight - y : cellHeight;

            slots.get(i).chart.setBounds(x, y, width, height);
        }
    }

    private double getTotalRatio() {
        double total = 0;
        for (ChartSlot slot : charts.values()) {
            total += slot.ratio;
        }
        return total;
    }

    // ========== Divider Rendering ==========

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (layoutMode == LayoutMode.GRID) {
            return; // No dividers in grid mode
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            List<ChartSlot> slots = new ArrayList<>(charts.values());

            for (int i = 0; i < slots.size() - 1; i++) {
                ChartSlot slot = slots.get(i);
                Rectangle bounds = slot.chart.getBounds();

                boolean isHovered = (resizingDivider == slot) ||
                        (resizingDivider == null && findDividerAt(getMousePosition()) == slot);

                g2.setColor(isHovered ? DIVIDER_HOVER_COLOR : DIVIDER_COLOR);

                if (layoutMode == LayoutMode.VSTACK) {
                    int dividerY = bounds.y + bounds.height;
                    g2.fillRect(0, dividerY, getWidth(), DIVIDER_SIZE);

                    // Draw grip lines
                    g2.setColor(new Color(100, 102, 106));
                    int centerY = dividerY + DIVIDER_SIZE / 2;
                    int centerX = getWidth() / 2;
                    for (int j = -1; j <= 1; j++) {
                        g2.drawLine(centerX - 15, centerY + j * 2, centerX + 15, centerY + j * 2);
                    }
                } else {
                    int dividerX = bounds.x + bounds.width;
                    g2.fillRect(dividerX, 0, DIVIDER_SIZE, getHeight());

                    // Draw grip lines
                    g2.setColor(new Color(100, 102, 106));
                    int centerX = dividerX + DIVIDER_SIZE / 2;
                    int centerY = getHeight() / 2;
                    for (int j = -1; j <= 1; j++) {
                        g2.drawLine(centerX + j * 2, centerY - 15, centerX + j * 2, centerY + 15);
                    }
                }
            }
        } finally {
            g2.dispose();
        }
    }

    // ========== Resize Handlers ==========

    private void setupResizeHandlers() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!dividersResizable || layoutMode == LayoutMode.GRID) {
                    return;
                }

                resizingDivider = findDividerAt(e.getPoint());
                if (resizingDivider != null) {
                    resizeStartPos = isVertical() ? e.getY() : e.getX();
                    resizeStartRatio = resizingDivider.ratio;
                    setCursor(isVertical() ?
                            Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) :
                            Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                resizingDivider = null;
                updateCursor(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (resizingDivider != null) {
                    int delta = (isVertical() ? e.getY() : e.getX()) - resizeStartPos;
                    adjustChartSizes(resizingDivider, delta);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getPoint());
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void updateCursor(Point point) {
        if (resizingDivider != null || !dividersResizable || layoutMode == LayoutMode.GRID) {
            return;
        }

        ChartSlot divider = findDividerAt(point);
        if (divider != null) {
            setCursor(isVertical() ?
                    Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) :
                    Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private ChartSlot findDividerAt(Point point) {
        if (point == null || layoutMode == LayoutMode.GRID) {
            return null;
        }

        List<ChartSlot> slots = new ArrayList<>(charts.values());

        for (int i = 0; i < slots.size() - 1; i++) {
            ChartSlot slot = slots.get(i);
            Rectangle bounds = slot.chart.getBounds();

            if (isVertical()) {
                int dividerStart = bounds.y + bounds.height;
                int dividerEnd = dividerStart + DIVIDER_SIZE;
                if (point.y >= dividerStart && point.y < dividerEnd) {
                    return slot;
                }
            } else {
                int dividerStart = bounds.x + bounds.width;
                int dividerEnd = dividerStart + DIVIDER_SIZE;
                if (point.x >= dividerStart && point.x < dividerEnd) {
                    return slot;
                }
            }
        }

        return null;
    }

    private void adjustChartSizes(ChartSlot dividerSlot, int delta) {
        List<ChartSlot> slots = new ArrayList<>(charts.values());
        int dividerIndex = slots.indexOf(dividerSlot);
        if (dividerIndex < 0 || dividerIndex >= slots.size() - 1) {
            return;
        }

        ChartSlot slotBefore = slots.get(dividerIndex);
        ChartSlot slotAfter = slots.get(dividerIndex + 1);

        // Calculate available size
        int dividerCount = Math.max(0, charts.size() - 1);
        int availableSize = (isVertical() ? getHeight() : getWidth()) - (dividerCount * DIVIDER_SIZE);
        if (availableSize <= 0) return;

        // Convert pixel delta to ratio delta
        double totalRatio = getTotalRatio();
        double deltaRatio = (double) delta / availableSize * totalRatio;

        // Calculate new ratios
        double minRatio = 0.1 * totalRatio; // Minimum 10%
        double newBeforeRatio = slotBefore.ratio + deltaRatio;
        double newAfterRatio = slotAfter.ratio - deltaRatio;

        // Enforce minimum
        if (newBeforeRatio < minRatio) {
            deltaRatio = minRatio - slotBefore.ratio;
            newBeforeRatio = minRatio;
            newAfterRatio = slotAfter.ratio - deltaRatio;
        }
        if (newAfterRatio < minRatio) {
            deltaRatio = slotAfter.ratio - minRatio;
            newAfterRatio = minRatio;
            newBeforeRatio = slotBefore.ratio + deltaRatio;
        }

        slotBefore.ratio = newBeforeRatio;
        slotAfter.ratio = newAfterRatio;

        resizeStartPos += delta;

        layoutCharts();
    }

    private boolean isVertical() {
        return layoutMode == LayoutMode.VSTACK;
    }

    // ========== Repaint ==========

    @Override
    public void repaint() {
        super.repaint();
        if (charts != null) {
            for (ChartSlot slot : charts.values()) {
                slot.chart.repaint();
            }
        }
    }

    // ========== Disposal ==========

    /**
     * Disposes of rendering resources for all charts.
     */
    public void dispose() {
        for (ChartSlot slot : charts.values()) {
            slot.chart.dispose();
        }
    }

    // ========== Separate Pane Indicator Listener ==========

    /**
     * Default indicator pane ratio (20% of the layout).
     */
    private static final double DEFAULT_INDICATOR_PANE_RATIO = 0.2;

    /**
     * Colors for indicator panes, cycled for multiple indicators.
     */
    private static final Color[] INDICATOR_COLORS = {
            new Color(233, 30, 99),   // Pink
            new Color(156, 39, 176),  // Purple
            new Color(63, 81, 181),   // Indigo
            new Color(0, 150, 136),   // Teal
            new Color(255, 152, 0),   // Orange
    };

    private int indicatorColorIndex = 0;

    @Override
    public void onSeparatePaneIndicatorAdded(IndicatorInstance<?, ?> instance) {
        String indicatorId = instance.getDescriptor().getId();
        String instanceId = instance.getId();
        String chartId = indicatorId + "_" + instanceId;

        // Check if pane already exists
        if (getChart(chartId) != null) {
            log.debug("Indicator pane '{}' already exists", chartId);
            return;
        }

        log.info("Creating separate pane for indicator: {} (instance {})", indicatorId, instanceId);

        // Get output data
        Data<?> outputData = instance.getOutputData();
        if (outputData == null) {
            log.warn("Indicator {} has no output data", indicatorId);
            return;
        }

        // Adjust existing chart ratios to make room
        adjustRatiosForNewPane(DEFAULT_INDICATOR_PANE_RATIO);

        // Create indicator chart
        Chart indicatorChart = addChart(chartId, DEFAULT_INDICATOR_PANE_RATIO);
        indicatorChart.setChartType(ChartType.INDICATOR);

        // Add the indicator data as a line series
        if (outputData instanceof XyData xyData) {
            Color color = INDICATOR_COLORS[indicatorColorIndex % INDICATOR_COLORS.length];
            indicatorColorIndex++;

            indicatorChart.addLineSeries(xyData, new LineSeriesOptions()
                    .color(color)
                    .lineWidth(1.5f));

            // Set symbol info for the indicator
            indicatorChart.setSymbolInfo(
                    instance.getDescriptor().getName(),
                    formatIndicatorParameters(instance),
                    ""
            );
        } else {
            log.warn("Unsupported indicator output type: {}", outputData.getClass().getSimpleName());
        }

        layoutCharts();
        repaint();
    }

    @Override
    public void onSeparatePaneIndicatorRemoved(IndicatorInstance<?, ?> instance) {
        String indicatorId = instance.getDescriptor().getId();
        String instanceId = instance.getId();
        String chartId = indicatorId + "_" + instanceId;

        if (getChart(chartId) != null) {
            log.info("Removing separate pane for indicator: {}", indicatorId);
            removeChart(chartId);
            normalizeRatios();
        }
    }

    /**
     * Adjusts existing chart ratios to make room for a new pane.
     */
    private void adjustRatiosForNewPane(double newPaneRatio) {
        if (charts.isEmpty()) {
            return;
        }

        // Scale down existing ratios proportionally
        double scaleFactor = (1.0 - newPaneRatio);
        for (ChartSlot slot : charts.values()) {
            slot.ratio *= scaleFactor;
        }
    }

    /**
     * Normalizes ratios so they sum to 1.0.
     */
    private void normalizeRatios() {
        double total = getTotalRatio();
        if (total <= 0 || Math.abs(total - 1.0) < 0.001) {
            return;
        }

        for (ChartSlot slot : charts.values()) {
            slot.ratio /= total;
        }
    }

    /**
     * Formats indicator parameters for display.
     */
    private String formatIndicatorParameters(IndicatorInstance<?, ?> instance) {
        Map<String, Object> params = instance.getParameterValues();
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    // ========== Convenience Factory Methods ==========

    /**
     * Creates a typical price + volume layout.
     */
    public static ChartLayout priceAndVolume() {
        return priceAndVolume(RenderBackend.OPENGL);
    }

    /**
     * Creates a typical price + volume layout with the specified backend.
     */
    public static ChartLayout priceAndVolume(RenderBackend backend) {
        ChartLayout layout = new ChartLayout(LayoutMode.VSTACK, backend);

        Chart priceChart = layout.addChart("price", 0.75);
        priceChart.setChartType(ChartType.PRICE);

        Chart volumeChart = layout.addChart("volume", 0.25);
        volumeChart.setChartType(ChartType.VOLUME);

        return layout;
    }

    // ========== Internal Classes ==========

    /**
     * Internal class representing a chart with its layout ratio.
     */
    private static class ChartSlot {
        final Chart chart;
        double ratio;

        ChartSlot(Chart chart, double ratio) {
            this.chart = chart;
            this.ratio = ratio;
        }
    }
}
