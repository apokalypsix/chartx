package com.apokalypsix.chartx.core.render.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.series.SeriesType;
import com.apokalypsix.chartx.chart.style.LegendConfig;
import com.apokalypsix.chartx.chart.style.TooltipConfig;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.ui.overlay.InfoOverlay;
import com.apokalypsix.chartx.core.ui.overlay.OverlayPosition;

/**
 * Swing overlay component for rendering text labels using Java2D.
 *
 * <p>This component is layered on top of the GLJPanel to handle text rendering,
 * which avoids GL2 compatibility issues with JOGL's TextRenderer on macOS.
 */
public class TextOverlay extends JPanel {

    // Colors
    private Color axisLabelColor = new Color(140, 142, 146);
    private Color axisBackgroundColor = new Color(20, 22, 25);
    private Color crosshairLabelColor = new Color(220, 222, 226);
    private Color crosshairBackgroundColor = new Color(60, 62, 66);

    // Fonts
    private Font axisFont = new Font("SansSerif", Font.PLAIN, 11);
    private Font crosshairFont = new Font("SansSerif", Font.PLAIN, 11);

    // Formatters
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat timeFormatSeconds = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");

    // References
    private Viewport viewport;
    private MultiAxisCoordinateSystem coordinates;
    private YAxisManager axisManager;
    private AnnotationLayerV2 annotationLayer;
    private CategoryAxis categoryAxis;

    // Crosshair state
    private int cursorX = -1;
    private int cursorY = -1;
    private boolean crosshairVisible = false;

    // Legend support
    private LegendConfig legendConfig;
    private List<LegendItem> legendItems = new ArrayList<>();
    private boolean legendDirty = true;
    private Rectangle legendBounds;

    // Tooltip support
    private TooltipConfig tooltipConfig;
    private TooltipData tooltipData;
    private int tooltipX, tooltipY;
    private boolean tooltipVisible = false;

    // Info overlay support
    private final List<InfoOverlay> infoOverlays = new ArrayList<>();

    // GPU rendering flags - when true, Java2D rendering is skipped (GPU handles it)
    private boolean gpuAxisLabels = false;
    private boolean gpuCrosshairLabels = false;
    private boolean gpuLegend = false;
    private boolean gpuTooltip = false;
    private boolean gpuInfoOverlays = false;
    private boolean gpuCategoryAxisLabels = true;  // GPU layer handles category labels

    // Tick size
    private static final int TICK_SIZE = 4;
    private static final int LABEL_PADDING = 4;

    public TextOverlay() {
        setOpaque(false); // Transparent background
        timeFormat.setTimeZone(TimeZone.getDefault());
        timeFormatSeconds.setTimeZone(TimeZone.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());

        // Add mouse listener for info overlay interactions
        // Only consume clicks that hit an overlay, otherwise pass through
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!handleInfoOverlayClick(e)) {
                    // Pass event to underlying component
                    dispatchToParent(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!isOverInfoOverlay(e)) {
                    dispatchToParent(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isOverInfoOverlay(e)) {
                    dispatchToParent(e);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                dispatchToParent(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                dispatchToParent(e);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                dispatchToParent(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                dispatchToParent(e);
            }
        });

        addMouseWheelListener(e -> dispatchToParent(e));
    }

    /**
     * Dispatches a mouse event to the parent layered pane's other components.
     */
    private void dispatchToParent(MouseEvent e) {
        Container parent = getParent();
        if (parent != null) {
            // Find sibling component (render component) at same location
            Point pt = SwingUtilities.convertPoint(this, e.getPoint(), parent);
            Component target = null;

            for (Component comp : parent.getComponents()) {
                if (comp != this && comp.isVisible() && comp.contains(
                        pt.x - comp.getX(), pt.y - comp.getY())) {
                    target = comp;
                    break;
                }
            }

            if (target != null) {
                Point targetPt = SwingUtilities.convertPoint(this, e.getPoint(), target);
                MouseEvent newEvent = new MouseEvent(
                        target, e.getID(), e.getWhen(), e.getModifiersEx(),
                        targetPt.x, targetPt.y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
                target.dispatchEvent(newEvent);
            }
        }
    }

    /**
     * Dispatches a mouse wheel event to the parent layered pane's other components.
     */
    private void dispatchToParent(java.awt.event.MouseWheelEvent e) {
        Container parent = getParent();
        if (parent != null) {
            Point pt = SwingUtilities.convertPoint(this, e.getPoint(), parent);
            Component target = null;

            for (Component comp : parent.getComponents()) {
                if (comp != this && comp.isVisible() && comp.contains(
                        pt.x - comp.getX(), pt.y - comp.getY())) {
                    target = comp;
                    break;
                }
            }

            if (target != null) {
                Point targetPt = SwingUtilities.convertPoint(this, e.getPoint(), target);
                java.awt.event.MouseWheelEvent newEvent = new java.awt.event.MouseWheelEvent(
                        target, e.getID(), e.getWhen(), e.getModifiersEx(),
                        targetPt.x, targetPt.y, e.getClickCount(), e.isPopupTrigger(),
                        e.getScrollType(), e.getScrollAmount(), e.getWheelRotation());
                target.dispatchEvent(newEvent);
            }
        }
    }

    /**
     * Checks if the mouse event is over any info overlay.
     */
    private boolean isOverInfoOverlay(MouseEvent e) {
        for (InfoOverlay overlay : infoOverlays) {
            if (overlay.getConfig().isVisible() && overlay.getBounds().contains(e.getPoint())) {
                return true;
            }
        }
        return false;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    public void setCoordinates(MultiAxisCoordinateSystem coordinates) {
        this.coordinates = coordinates;
    }

    public void setAxisManager(YAxisManager axisManager) {
        this.axisManager = axisManager;
    }

    public void setCursorPosition(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
    }

    public void setCrosshairVisible(boolean visible) {
        this.crosshairVisible = visible;
    }

    public void setAnnotationLayer(AnnotationLayerV2 annotationLayer) {
        this.annotationLayer = annotationLayer;
    }

    public void setCategoryAxis(CategoryAxis categoryAxis) {
        this.categoryAxis = categoryAxis;
    }

    public CategoryAxis getCategoryAxis() {
        return categoryAxis;
    }

    /**
     * Enables or disables GPU rendering for axis labels.
     * When enabled, Java2D axis label rendering is skipped (AxisLabelLayerV2 handles it).
     */
    public void setGPUAxisLabels(boolean enabled) {
        this.gpuAxisLabels = enabled;
    }

    /**
     * Enables or disables GPU rendering for crosshair labels.
     * When enabled, Java2D crosshair label rendering is skipped (CrosshairLayerV2 handles it).
     */
    public void setGPUCrosshairLabels(boolean enabled) {
        this.gpuCrosshairLabels = enabled;
    }

    /**
     * Enables or disables GPU rendering for legend.
     * When enabled, Java2D legend rendering is skipped (LegendLayerV2 handles it).
     */
    public void setGPULegend(boolean enabled) {
        this.gpuLegend = enabled;
    }

    /**
     * Enables or disables GPU rendering for tooltip.
     * When enabled, Java2D tooltip rendering is skipped (TooltipLayerV2 handles it).
     */
    public void setGPUTooltip(boolean enabled) {
        this.gpuTooltip = enabled;
    }

    /**
     * Enables or disables GPU rendering for info overlays.
     * When enabled, Java2D info overlay rendering is skipped (InfoOverlayLayerV2 handles it).
     */
    public void setGPUInfoOverlays(boolean enabled) {
        this.gpuInfoOverlays = enabled;
    }

    /**
     * Enables or disables GPU rendering for category axis labels.
     * When enabled, Java2D category label rendering is skipped (AxisLabelLayerV2 handles it).
     */
    public void setGPUCategoryAxisLabels(boolean enabled) {
        this.gpuCategoryAxisLabels = enabled;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (viewport == null || coordinates == null || axisManager == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        try {
            drawAxisLabels(g2);
            drawCategoryAxisLabels(g2);
            // Note: Text annotations are now rendered via GPU in AnnotationLayerV2
            if (crosshairVisible && cursorX >= 0 && cursorY >= 0) {
                drawCrosshairLabels(g2);
            }
            drawLegend(g2);
            drawInfoOverlays(g2);
            if (tooltipVisible) {
                drawTooltip(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawAxisLabels(Graphics2D g2) {
        // Skip if GPU rendering is enabled for axis labels
        if (gpuAxisLabels) {
            return;
        }

        // Use component dimensions (logical pixels) not viewport (physical pixels)
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        // Calculate scale factor for HiDPI displays
        double scale = getHiDPIScale();

        // Convert insets from physical to logical pixels
        int chartLeft = (int) (viewport.getLeftInset() / scale);
        int chartRight = width - (int) (viewport.getRightInset() / scale);
        int chartTop = (int) (viewport.getTopInset() / scale);
        int chartBottom = height - (int) (viewport.getBottomInset() / scale);

        g2.setFont(axisFont);
        FontMetrics fm = g2.getFontMetrics();

        // Draw labels for each visible Y-axis
        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);

        // Draw left axis labels (from chart area outward)
        int leftX = chartLeft;
        for (int i = leftAxes.size() - 1; i >= 0; i--) {
            YAxis axis = leftAxes.get(i);
            int axisWidth = (int) (axis.getWidth() / scale);
            leftX -= axisWidth;

            CoordinateSystem axisCoords = coordinates.forAxis(axis.getId());
            double[] priceLevels = calculateAxisGridLevels(axis);

            g2.setColor(axis.getLabelColor());
            for (double price : priceLevels) {
                // Scale Y coordinate from physical to logical pixels
                float y = (float) (axisCoords.yValueToScreenY(price) / scale);
                if (y >= chartTop && y <= chartBottom) {
                    String label = axis.formatValue(price);
                    int labelWidth = fm.stringWidth(label);
                    int textX = leftX + axisWidth - labelWidth - TICK_SIZE - 2;
                    int textY = (int) y + fm.getAscent() / 2 - 2;
                    g2.drawString(label, textX, textY);
                }
            }
        }

        // Draw right axis labels (from chart area outward)
        int rightX = chartRight;
        for (YAxis axis : rightAxes) {
            int axisWidth = (int) (axis.getWidth() / scale);
            CoordinateSystem axisCoords = coordinates.forAxis(axis.getId());
            double[] priceLevels = calculateAxisGridLevels(axis);

            g2.setColor(axis.getLabelColor());
            for (double price : priceLevels) {
                // Scale Y coordinate from physical to logical pixels
                float y = (float) (axisCoords.yValueToScreenY(price) / scale);
                if (y >= chartTop && y <= chartBottom) {
                    String label = axis.formatValue(price);
                    int textX = rightX + TICK_SIZE + 2;
                    int textY = (int) y + fm.getAscent() / 2 - 2;
                    g2.drawString(label, textX, textY);
                }
            }

            rightX += axisWidth;
        }

        // Draw time labels (bottom axis)
        long[] timeLevels = calculateTimeGridLevels();
        Date date = new Date();
        boolean showDate = needsDateLabels(timeLevels);
        int lastLabelRight = Integer.MIN_VALUE;

        g2.setColor(axisLabelColor);
        for (long time : timeLevels) {
            // Scale X coordinate from physical to logical pixels
            float x = (float) (coordinates.xValueToScreenX(time) / scale);
            if (x >= chartLeft && x <= chartRight) {
                date.setTime(time);
                String label = showDate ? dateFormat.format(date) : timeFormat.format(date);
                int labelWidth = fm.stringWidth(label);
                int textX = (int) (x - labelWidth / 2);

                // Avoid overlapping labels
                if (textX > lastLabelRight + 10) {
                    int textY = chartBottom + TICK_SIZE + fm.getAscent() + 2;
                    g2.drawString(label, textX, textY);
                    lastLabelRight = textX + labelWidth;
                }
            }
        }
    }

    /**
     * Calculates grid levels for a specific Y-axis based on its value range and scale.
     *
     * <p>Delegates to the axis's scale for proper grid level calculation
     * (e.g., log scales use powers instead of linear intervals).
     */
    private double[] calculateAxisGridLevels(YAxis axis) {
        return axis.calculateGridLevels(8);
    }

    /**
     * Draws category axis labels using Java2D (fallback when GPU rendering is disabled).
     */
    private void drawCategoryAxisLabels(Graphics2D g2) {
        // Skip if GPU rendering is enabled or no category axis
        if (gpuCategoryAxisLabels || categoryAxis == null ||
            !categoryAxis.isVisible() || categoryAxis.getCategoryCount() == 0) {
            return;
        }

        // Use component dimensions (logical pixels)
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        // Calculate scale factor for HiDPI displays
        double scale = getHiDPIScale();

        // Convert insets from physical to logical pixels
        int chartLeft = (int) (viewport.getLeftInset() / scale);
        int chartRight = width - (int) (viewport.getRightInset() / scale);
        int chartTop = (int) (viewport.getTopInset() / scale);
        int chartBottom = height - (int) (viewport.getBottomInset() / scale);

        g2.setFont(axisFont);
        FontMetrics fm = g2.getFontMetrics();

        // Calculate axis dimensions
        boolean isVertical = !categoryAxis.isHorizontal();
        double axisStart = isVertical ? chartTop : chartLeft;
        double axisLength = isVertical ? (chartBottom - chartTop) : (chartRight - chartLeft);

        // Scale axis width for display
        int axisWidth = categoryAxis.getHeight();

        g2.setColor(categoryAxis.getLabelColor());

        for (int i = 0; i < categoryAxis.getCategoryCount(); i++) {
            String label = categoryAxis.getLabel(i);
            if (label == null || label.isEmpty()) {
                continue;
            }

            // Calculate position at center of category slot (in logical pixels)
            double pos = categoryAxis.indexToCenterScreen(i, axisStart, axisLength);

            switch (categoryAxis.getInternalPosition()) {
                case LEFT:
                    if (pos >= chartTop && pos <= chartBottom) {
                        int labelWidth = fm.stringWidth(label);
                        int textX = axisWidth - labelWidth - TICK_SIZE - 2;
                        int textY = (int) pos + fm.getAscent() / 2 - 2;
                        g2.drawString(label, textX, textY);
                    }
                    break;

                case RIGHT:
                    if (pos >= chartTop && pos <= chartBottom) {
                        int textX = chartRight + TICK_SIZE + 2;
                        int textY = (int) pos + fm.getAscent() / 2 - 2;
                        g2.drawString(label, textX, textY);
                    }
                    break;

                case TOP:
                    if (pos >= chartLeft && pos <= chartRight) {
                        int labelWidth = fm.stringWidth(label);
                        int textX = (int) pos - labelWidth / 2;
                        int textY = axisWidth - TICK_SIZE - 2;
                        g2.drawString(label, textX, textY);
                    }
                    break;

                case BOTTOM:
                    if (pos >= chartLeft && pos <= chartRight) {
                        int labelWidth = fm.stringWidth(label);
                        int textX = (int) pos - labelWidth / 2;
                        int textY = chartBottom + TICK_SIZE + fm.getAscent() + 2;
                        g2.drawString(label, textX, textY);
                    }
                    break;
            }
        }
    }

    private void drawCrosshairLabels(Graphics2D g2) {
        // Skip if GPU rendering is enabled for crosshair labels
        if (gpuCrosshairLabels) {
            return;
        }

        // Use component dimensions (logical pixels) not viewport (physical pixels)
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        // Calculate scale factor for HiDPI displays
        double scale = getHiDPIScale();

        // Convert insets and cursor position from physical to logical pixels
        int chartLeft = (int) (viewport.getLeftInset() / scale);
        int chartRight = width - (int) (viewport.getRightInset() / scale);
        int chartTop = (int) (viewport.getTopInset() / scale);
        int chartBottom = height - (int) (viewport.getBottomInset() / scale);

        // Scale cursor position to logical pixels
        int logicalCursorX = (int) (cursorX / scale);
        int logicalCursorY = (int) (cursorY / scale);

        // Only draw if cursor is in chart area
        if (logicalCursorX < chartLeft || logicalCursorX > chartRight ||
            logicalCursorY < chartTop || logicalCursorY > chartBottom) {
            return;
        }

        g2.setFont(crosshairFont);
        FontMetrics fm = g2.getFontMetrics();

        // Get time at cursor (uses physical pixel coordinates)
        long time = coordinates.screenXToXValue(cursorX);

        // Draw price labels for each visible Y-axis
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);
        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);

        // Draw crosshair price label on right axes
        int rightX = chartRight;
        for (YAxis axis : rightAxes) {
            int axisWidth = (int) (axis.getWidth() / scale);
            CoordinateSystem axisCoords = coordinates.forAxis(axis.getId());
            double price = axisCoords.screenYToYValue(cursorY);

            String priceLabel = axis.formatValue(price);
            Rectangle2D priceBounds = fm.getStringBounds(priceLabel, g2);

            int priceLabelX = rightX + 2;
            int priceLabelY = logicalCursorY - (int) priceBounds.getHeight() / 2;
            int priceLabelW = (int) priceBounds.getWidth() + LABEL_PADDING * 2;
            int priceLabelH = (int) priceBounds.getHeight() + LABEL_PADDING;

            // Draw price label background
            g2.setColor(crosshairBackgroundColor);
            g2.fillRect(priceLabelX, priceLabelY, priceLabelW, priceLabelH);

            // Draw price label text
            g2.setColor(crosshairLabelColor);
            g2.drawString(priceLabel, priceLabelX + LABEL_PADDING,
                    logicalCursorY + fm.getAscent() / 2 - 2);

            rightX += axisWidth;
        }

        // Draw crosshair price label on left axes
        int leftX = chartLeft;
        for (int i = leftAxes.size() - 1; i >= 0; i--) {
            YAxis axis = leftAxes.get(i);
            int axisWidth = (int) (axis.getWidth() / scale);
            leftX -= axisWidth;

            CoordinateSystem axisCoords = coordinates.forAxis(axis.getId());
            double price = axisCoords.screenYToYValue(cursorY);

            String priceLabel = axis.formatValue(price);
            Rectangle2D priceBounds = fm.getStringBounds(priceLabel, g2);

            int priceLabelW = (int) priceBounds.getWidth() + LABEL_PADDING * 2;
            int priceLabelH = (int) priceBounds.getHeight() + LABEL_PADDING;
            int priceLabelX = leftX + axisWidth - priceLabelW - 2;
            int priceLabelY = logicalCursorY - (int) priceBounds.getHeight() / 2;

            // Draw price label background
            g2.setColor(crosshairBackgroundColor);
            g2.fillRect(priceLabelX, priceLabelY, priceLabelW, priceLabelH);

            // Draw price label text
            g2.setColor(crosshairLabelColor);
            g2.drawString(priceLabel, priceLabelX + LABEL_PADDING,
                    logicalCursorY + fm.getAscent() / 2 - 2);
        }

        // Time label (bottom axis)
        String timeLabel = timeFormatSeconds.format(new Date(time));
        Rectangle2D timeBounds = fm.getStringBounds(timeLabel, g2);

        int timeLabelW = (int) timeBounds.getWidth() + LABEL_PADDING * 2;
        int timeLabelH = (int) timeBounds.getHeight() + LABEL_PADDING;
        int timeLabelX = logicalCursorX - timeLabelW / 2;
        int timeLabelY = chartBottom + 2;

        // Draw time label background
        g2.setColor(crosshairBackgroundColor);
        g2.fillRect(timeLabelX, timeLabelY, timeLabelW, timeLabelH);

        // Draw time label text
        g2.setColor(crosshairLabelColor);
        g2.drawString(timeLabel, timeLabelX + LABEL_PADDING,
                timeLabelY + fm.getAscent() + LABEL_PADDING / 2);
    }

    private static final int MAX_GRID_LEVELS = 100;

    private long[] calculateTimeGridLevels() {
        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();
        long duration = endTime - startTime;

        // Handle edge cases
        if (duration <= 0 || startTime >= endTime) {
            return new long[0];
        }

        long interval = calculateTimeInterval(duration);

        // Safety check: ensure interval is positive and reasonable
        if (interval <= 0) {
            return new long[0];
        }

        long firstLevel = ((startTime / interval) + 1) * interval;

        // Count levels with a safety cap
        int count = 0;
        for (long t = firstLevel; t <= endTime && count < MAX_GRID_LEVELS; t += interval) {
            count++;
        }

        long[] levels = new long[count];
        int i = 0;
        for (long t = firstLevel; t <= endTime && i < count; t += interval) {
            levels[i++] = t;
        }

        return levels;
    }

    private boolean needsDateLabels(long[] timeLevels) {
        if (timeLevels.length < 2) return false;
        long span = timeLevels[timeLevels.length - 1] - timeLevels[0];
        return span > 24 * 60 * 60 * 1000; // More than 1 day
    }

    private long calculateTimeInterval(long duration) {
        // Handle edge case of very small or zero duration
        if (duration <= 0) {
            return 60000L; // Default to 1 minute
        }

        long roughInterval = duration / 8;

        // Extended intervals: 1min to 1year
        long[] niceIntervals = {
                60000L,          // 1 minute
                300000L,         // 5 minutes
                600000L,         // 10 minutes
                900000L,         // 15 minutes
                1800000L,        // 30 minutes
                3600000L,        // 1 hour
                7200000L,        // 2 hours
                14400000L,       // 4 hours
                21600000L,       // 6 hours
                43200000L,       // 12 hours
                86400000L,       // 1 day
                604800000L,      // 1 week
                2592000000L,     // 30 days (month)
                7776000000L,     // 90 days (quarter)
                31536000000L     // 365 days (year)
        };

        for (long interval : niceIntervals) {
            if (interval >= roughInterval) {
                return interval;
            }
        }
        return niceIntervals[niceIntervals.length - 1];
    }

    // ========== HiDPI Support ==========

    /**
     * Gets the HiDPI scale factor for this component.
     */
    private double getHiDPIScale() {
        var gc = getGraphicsConfiguration();
        if (gc != null) {
            return gc.getDefaultTransform().getScaleX();
        }
        return 1.0;
    }

    // ========== Configuration ==========

    public void setAxisLabelColor(Color color) {
        this.axisLabelColor = color;
    }

    public void setCrosshairLabelColor(Color color) {
        this.crosshairLabelColor = color;
    }

    public void setCrosshairBackgroundColor(Color color) {
        this.crosshairBackgroundColor = color;
    }

    // ========== Legend Support ==========

    /**
     * Sets the legend configuration.
     *
     * @param config the legend config
     */
    public void setLegendConfig(LegendConfig config) {
        this.legendConfig = config;
        this.legendDirty = true;
        repaint();
    }

    /**
     * Gets the legend configuration.
     *
     * @return the legend config, or null if not set
     */
    public LegendConfig getLegendConfig() {
        return legendConfig;
    }

    /**
     * Sets the legend items to display.
     *
     * @param items the legend items
     */
    public void setLegendItems(List<LegendItem> items) {
        this.legendItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.legendDirty = true;
        repaint();
    }

    /**
     * Adds a legend item.
     *
     * @param item the item to add
     */
    public void addLegendItem(LegendItem item) {
        if (item != null) {
            legendItems.add(item);
            legendDirty = true;
            repaint();
        }
    }

    /**
     * Removes a legend item by series ID.
     *
     * @param seriesId the series ID to remove
     */
    public void removeLegendItem(String seriesId) {
        legendItems.removeIf(item -> item.getSeriesId().equals(seriesId));
        legendDirty = true;
        repaint();
    }

    /**
     * Clears all legend items.
     */
    public void clearLegendItems() {
        legendItems.clear();
        legendDirty = true;
        repaint();
    }

    /**
     * Returns the list of legend items.
     */
    public List<LegendItem> getLegendItems() {
        return legendItems;
    }

    private void drawLegend(Graphics2D g2) {
        // Skip if GPU rendering is enabled for legend
        if (gpuLegend) {
            return;
        }

        if (legendConfig == null || !legendConfig.isVisible() || legendItems.isEmpty()) {
            return;
        }

        // Filter to visible items only
        List<LegendItem> visibleItems = new ArrayList<>();
        for (LegendItem item : legendItems) {
            if (item.isVisible()) {
                visibleItems.add(item);
            }
        }
        if (visibleItems.isEmpty()) {
            return;
        }

        g2.setFont(legendConfig.getFont());
        FontMetrics fm = g2.getFontMetrics();

        // Calculate legend dimensions
        if (legendDirty) {
            calculateLegendBounds(fm, visibleItems);
            legendDirty = false;
        }

        // Calculate position based on config
        Point pos = calculateLegendPosition();

        // Draw background
        if (legendConfig.isShowBackground()) {
            g2.setColor(legendConfig.getBackgroundColor());
            g2.fillRoundRect(pos.x, pos.y, legendBounds.width, legendBounds.height,
                    legendConfig.getCornerRadius(), legendConfig.getCornerRadius());
        }

        // Draw border
        if (legendConfig.isShowBorder()) {
            g2.setColor(legendConfig.getBorderColor());
            g2.drawRoundRect(pos.x, pos.y, legendBounds.width, legendBounds.height,
                    legendConfig.getCornerRadius(), legendConfig.getCornerRadius());
        }

        // Draw items
        drawLegendItems(g2, fm, pos, visibleItems);
    }

    private void calculateLegendBounds(FontMetrics fm, List<LegendItem> items) {
        int swatchSize = legendConfig.getColorSwatchSize();
        int swatchSpacing = legendConfig.getSwatchSpacing();
        int padding = legendConfig.getPadding();
        int itemSpacing = legendConfig.getItemSpacing();

        int totalWidth = padding * 2;
        int totalHeight = padding * 2;

        if (legendConfig.getOrientation() == LegendConfig.Orientation.HORIZONTAL) {
            for (int i = 0; i < items.size(); i++) {
                LegendItem item = items.get(i);
                int itemWidth = swatchSize + swatchSpacing + fm.stringWidth(item.getDisplayName());
                totalWidth += itemWidth;
                if (i > 0) {
                    totalWidth += itemSpacing;
                }
            }
            totalHeight += fm.getHeight();
        } else {
            int maxWidth = 0;
            for (LegendItem item : items) {
                int itemWidth = swatchSize + swatchSpacing + fm.stringWidth(item.getDisplayName());
                maxWidth = Math.max(maxWidth, itemWidth);
            }
            totalWidth += maxWidth;
            totalHeight += items.size() * (fm.getHeight() + 2) - 2;
        }

        legendBounds = new Rectangle(0, 0, totalWidth, totalHeight);
    }

    private Point calculateLegendPosition() {
        int width = getWidth();
        int height = getHeight();
        int marginX = legendConfig.getMarginX();
        int marginY = legendConfig.getMarginY();

        // Account for viewport insets if available
        double scale = getHiDPIScale();
        int chartLeft = viewport != null ? (int) (viewport.getLeftInset() / scale) : 0;
        int chartRight = viewport != null ? width - (int) (viewport.getRightInset() / scale) : width;
        int chartTop = viewport != null ? (int) (viewport.getTopInset() / scale) : 0;
        int chartBottom = viewport != null ? height - (int) (viewport.getBottomInset() / scale) : height;

        int x, y;
        switch (legendConfig.getPosition()) {
            case TOP_LEFT:
                x = chartLeft + marginX;
                y = chartTop + marginY;
                break;
            case TOP_RIGHT:
                x = chartRight - legendBounds.width - marginX;
                y = chartTop + marginY;
                break;
            case BOTTOM_LEFT:
                x = chartLeft + marginX;
                y = chartBottom - legendBounds.height - marginY;
                break;
            case BOTTOM_RIGHT:
                x = chartRight - legendBounds.width - marginX;
                y = chartBottom - legendBounds.height - marginY;
                break;
            case TOP_CENTER:
                x = (chartLeft + chartRight - legendBounds.width) / 2;
                y = chartTop + marginY;
                break;
            case BOTTOM_CENTER:
                x = (chartLeft + chartRight - legendBounds.width) / 2;
                y = chartBottom - legendBounds.height - marginY;
                break;
            default:
                x = chartLeft + marginX;
                y = chartTop + marginY;
        }

        return new Point(x, y);
    }

    private void drawLegendItems(Graphics2D g2, FontMetrics fm, Point origin, List<LegendItem> items) {
        int x = origin.x + legendConfig.getPadding();
        int y = origin.y + legendConfig.getPadding();

        int swatchSize = legendConfig.getColorSwatchSize();
        int swatchSpacing = legendConfig.getSwatchSpacing();
        int itemSpacing = legendConfig.getItemSpacing();

        for (LegendItem item : items) {
            // Draw color swatch
            int swatchY = y + (fm.getAscent() - swatchSize) / 2;
            g2.setColor(item.getColor());
            drawSwatch(g2, x, swatchY, swatchSize, item.getType());

            // Draw text
            g2.setColor(legendConfig.getTextColor());
            g2.drawString(item.getDisplayName(), x + swatchSize + swatchSpacing, y + fm.getAscent());

            // Move to next item
            if (legendConfig.getOrientation() == LegendConfig.Orientation.HORIZONTAL) {
                x += swatchSize + swatchSpacing + fm.stringWidth(item.getDisplayName()) + itemSpacing;
            } else {
                y += fm.getHeight() + 2;
            }
        }
    }

    private void drawSwatch(Graphics2D g2, int x, int y, int size, SeriesType type) {
        switch (type) {
            case LINE:
            case SPLINE_LINE:
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(x, y + size / 2, x + size, y + size / 2);
                break;
            case CANDLESTICK:
                // Draw a small candle shape
                g2.fillRect(x + 2, y + 2, size - 4, size - 4);
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(x + size / 2, y, x + size / 2, y + 2);
                g2.drawLine(x + size / 2, y + size - 2, x + size / 2, y + size);
                break;
            case HISTOGRAM:
            case HORIZONTAL_BAR:
                g2.fillRect(x + 2, y + size / 3, size - 4, size * 2 / 3);
                break;
            case SCATTER:
            case BUBBLE:
                g2.fillOval(x + 2, y + 2, size - 4, size - 4);
                break;
            case BAND:
            case STACKED_MOUNTAIN:
                // Filled area shape
                Color c = g2.getColor();
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
                g2.fillRect(x, y + size / 3, size, size * 2 / 3);
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(x, y + size / 3, x + size, y + size / 3);
                break;
            default:
                // Default square
                g2.fillRect(x, y, size, size);
        }
    }

    // ========== Tooltip Support ==========

    /**
     * Sets the tooltip configuration.
     *
     * @param config the tooltip config
     */
    public void setTooltipConfig(TooltipConfig config) {
        this.tooltipConfig = config;
    }

    /**
     * Gets the tooltip configuration.
     *
     * @return the tooltip config, or null if not set
     */
    public TooltipConfig getTooltipConfig() {
        return tooltipConfig;
    }

    /**
     * Sets the tooltip data to display.
     *
     * @param data the tooltip data
     */
    public void setTooltipData(TooltipData data) {
        this.tooltipData = data;
    }

    /**
     * Sets the tooltip position (screen coordinates).
     *
     * @param x screen X
     * @param y screen Y
     */
    public void setTooltipPosition(int x, int y) {
        this.tooltipX = x;
        this.tooltipY = y;
    }

    /**
     * Sets whether the tooltip is visible.
     *
     * @param visible true to show tooltip
     */
    public void setTooltipVisible(boolean visible) {
        this.tooltipVisible = visible;
        repaint();
    }

    /**
     * Returns whether the tooltip is currently visible.
     *
     * @return true if visible
     */
    public boolean isTooltipVisible() {
        return tooltipVisible;
    }

    private void drawTooltip(Graphics2D g2) {
        // Skip if GPU rendering is enabled for tooltip
        if (gpuTooltip) {
            return;
        }

        if (tooltipData == null || tooltipData.isEmpty()) {
            return;
        }

        TooltipConfig cfg = tooltipConfig != null ? tooltipConfig : new TooltipConfig();

        g2.setFont(cfg.getFont());
        FontMetrics fm = g2.getFontMetrics();

        // Calculate tooltip dimensions
        int swatchSize = 10;
        int swatchSpacing = 4;
        int maxWidth = 0;
        int totalHeight = cfg.getPadding() * 2;

        List<TooltipData.TooltipRow> rows = tooltipData.getRows();
        for (TooltipData.TooltipRow row : rows) {
            String text = buildRowText(row);
            int width = fm.stringWidth(text);
            if (row.getSeriesName() != null && row.getColor() != null) {
                width += swatchSize + swatchSpacing;
            }
            maxWidth = Math.max(maxWidth, width);
            totalHeight += fm.getHeight() + cfg.getRowSpacing();
        }
        totalHeight -= cfg.getRowSpacing(); // Remove extra spacing after last row

        int tooltipWidth = maxWidth + cfg.getPadding() * 2;
        int tooltipHeight = totalHeight;

        // Scale for HiDPI
        double scale = getHiDPIScale();
        int logicalX = (int) (tooltipX / scale);
        int logicalY = (int) (tooltipY / scale);

        // Apply offset
        int x = logicalX + cfg.getOffsetX();
        int y = logicalY + cfg.getOffsetY();

        // Keep tooltip on screen
        int width = getWidth();
        int height = getHeight();
        if (cfg.isAvoidCursor() && x + tooltipWidth > width) {
            x = logicalX - tooltipWidth - cfg.getOffsetX();
        }
        if (y + tooltipHeight > height) {
            y = height - tooltipHeight - 5;
        }
        if (x < 0) x = 5;
        if (y < 0) y = 5;

        // Draw background
        g2.setColor(cfg.getBackgroundColor());
        g2.fillRoundRect(x, y, tooltipWidth, tooltipHeight,
                cfg.getCornerRadius(), cfg.getCornerRadius());

        // Draw border
        g2.setColor(cfg.getBorderColor());
        g2.drawRoundRect(x, y, tooltipWidth, tooltipHeight,
                cfg.getCornerRadius(), cfg.getCornerRadius());

        // Draw rows
        int rowY = y + cfg.getPadding() + fm.getAscent();
        for (TooltipData.TooltipRow row : rows) {
            int rowX = x + cfg.getPadding();

            // Draw color swatch for series
            if (row.getSeriesName() != null && row.getColor() != null && !row.isHeader()) {
                g2.setColor(row.getColor());
                g2.fillRect(rowX, rowY - fm.getAscent() + 3, swatchSize, fm.getAscent() - 2);
                rowX += swatchSize + swatchSpacing;
            }

            // Draw label if present
            if (row.getLabel() != null) {
                g2.setColor(cfg.getLabelColor());
                g2.drawString(row.getLabel(), rowX, rowY);
                rowX += fm.stringWidth(row.getLabel()) + 2;
            }

            // Draw value
            if (row.isHeader()) {
                g2.setFont(cfg.getLabelFont());
                g2.setColor(cfg.getTextColor());
                g2.drawString(row.getValue(), rowX, rowY);
                g2.setFont(cfg.getFont());
            } else {
                g2.setColor(cfg.getTextColor());
                g2.drawString(row.getValue(), rowX, rowY);
            }

            rowY += fm.getHeight() + cfg.getRowSpacing();
        }
    }

    private String buildRowText(TooltipData.TooltipRow row) {
        StringBuilder sb = new StringBuilder();
        if (row.getLabel() != null) {
            sb.append(row.getLabel()).append(" ");
        }
        sb.append(row.getValue());
        return sb.toString();
    }

    // ========== Info Overlay Support ==========

    /**
     * Adds an info overlay to be rendered.
     *
     * @param overlay the overlay to add
     */
    public void addInfoOverlay(InfoOverlay overlay) {
        if (overlay != null && !infoOverlays.contains(overlay)) {
            infoOverlays.add(overlay);
            repaint();
        }
    }

    /**
     * Removes an info overlay.
     *
     * @param overlay the overlay to remove
     */
    public void removeInfoOverlay(InfoOverlay overlay) {
        if (infoOverlays.remove(overlay)) {
            repaint();
        }
    }

    /**
     * Removes all info overlays.
     */
    public void clearInfoOverlays() {
        infoOverlays.clear();
        repaint();
    }

    /**
     * Returns the list of info overlays.
     *
     * @return unmodifiable list of overlays
     */
    public List<InfoOverlay> getInfoOverlays() {
        return Collections.unmodifiableList(infoOverlays);
    }

    /**
     * Draws all info overlays grouped by position.
     */
    private void drawInfoOverlays(Graphics2D g2) {
        // Skip if GPU rendering is enabled for info overlays
        if (gpuInfoOverlays) {
            return;
        }

        if (infoOverlays.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Group overlays by position and calculate stack offsets
        Map<OverlayPosition, List<InfoOverlay>> overlaysByPosition = new EnumMap<>(OverlayPosition.class);
        for (InfoOverlay overlay : infoOverlays) {
            if (overlay.getConfig().isVisible()) {
                OverlayPosition pos = overlay.getConfig().getPosition();
                overlaysByPosition.computeIfAbsent(pos, k -> new ArrayList<>()).add(overlay);
            }
        }

        // Render each position group
        for (Map.Entry<OverlayPosition, List<InfoOverlay>> entry : overlaysByPosition.entrySet()) {
            List<InfoOverlay> group = entry.getValue();
            int stackOffset = 0;

            for (InfoOverlay overlay : group) {
                overlay.setStackOffset(stackOffset);
                overlay.render(g2, width, height);
                stackOffset += overlay.getTotalHeight();
            }
        }
    }

    /**
     * Handles mouse clicks on info overlays for collapse/expand.
     *
     * @return true if the click was consumed by an overlay
     */
    private boolean handleInfoOverlayClick(MouseEvent e) {
        // Check overlays in reverse order (top-most first)
        for (int i = infoOverlays.size() - 1; i >= 0; i--) {
            InfoOverlay overlay = infoOverlays.get(i);
            if (overlay.handleClick(e)) {
                repaint();
                return true; // Click was consumed
            }
        }
        return false;
    }
}
