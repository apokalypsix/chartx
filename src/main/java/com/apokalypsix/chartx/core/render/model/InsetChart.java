package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.apokalypsix.chartx.chart.axis.PriceRange;
import com.apokalypsix.chartx.chart.axis.TimeRange;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.coordinate.CartesianCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

/**
 * A mini chart rendered as an inset within the main chart area.
 *
 * <p>InsetChart provides "picture-in-picture" functionality, allowing:
 * <ul>
 *   <li>Mini volume profiles</li>
 *   <li>Candle detail views</li>
 *   <li>Indicator close-ups</li>
 *   <li>Comparison charts</li>
 * </ul>
 *
 * <p>Each inset has its own coordinate system and can display different
 * data from the main chart.
 */
public class InsetChart extends AbstractRenderLayer {

    /** Z-order for inset charts (above data, below crosshair) */
    public static final int Z_ORDER = 750;

    /**
     * Positioning modes for inset charts.
     */
    public enum Position {
        /** Fixed position at a corner of the chart */
        CORNER,
        /** Anchored to a specific data point (time, price) */
        ANCHORED_TO_POINT,
        /** User-draggable floating position */
        FLOATING
    }

    /**
     * Corner positions for CORNER mode.
     */
    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    // Configuration
    private String id;
    private String title;
    private Position position = Position.CORNER;
    private Corner corner = Corner.TOP_RIGHT;
    private int margin = 10;  // Margin from chart edge

    // Size (pixels)
    private int width = 200;
    private int height = 150;

    // Anchor point (for ANCHORED_TO_POINT mode)
    private long anchorTime;
    private double anchorPrice;

    // Floating position (for FLOATING mode)
    private int floatingX;
    private int floatingY;

    // Visual styling
    private Color backgroundColor = new Color(30, 30, 30, 230);
    private Color borderColor = new Color(100, 100, 100);
    private float borderWidth = 1.0f;
    private boolean showTitle = true;
    private boolean showBorder = true;
    private boolean showBackground = true;

    // Data
    private OhlcData ohlcSeries;
    private final List<XyData> lineOverlays = new ArrayList<>();
    private TimeRange visibleTimeRange;
    private PriceRange visiblePriceRange;
    private boolean autoScalePriceRange = true;

    // Inset coordinate system
    private Viewport insetViewport;
    private CartesianCoordinateSystem insetCoords;

    // Parent chart info
    private int parentWidth;
    private int parentHeight;

    /**
     * Creates an inset chart with the given ID.
     *
     * @param id unique identifier for this inset
     */
    public InsetChart(String id) {
        super(Z_ORDER);
        this.id = id;
    }

    // ========== Configuration ==========

    /**
     * Returns the inset ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the inset title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the inset title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the position mode.
     */
    public void setPosition(Position position) {
        this.position = position;
        markDirty();
    }

    /**
     * Sets the corner for CORNER position mode.
     */
    public void setCorner(Corner corner) {
        this.corner = corner;
        markDirty();
    }

    /**
     * Sets the margin from chart edges (for CORNER mode).
     */
    public void setMargin(int margin) {
        this.margin = margin;
        markDirty();
    }

    /**
     * Sets the anchor point for ANCHORED_TO_POINT mode.
     */
    public void setAnchorPoint(long time, double price) {
        this.anchorTime = time;
        this.anchorPrice = price;
        markDirty();
    }

    /**
     * Sets the floating position for FLOATING mode.
     */
    public void setFloatingPosition(int x, int y) {
        this.floatingX = x;
        this.floatingY = y;
        markDirty();
    }

    /**
     * Sets the inset size.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        markDirty();
    }

    /**
     * Returns the inset width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the inset height.
     */
    public int getHeight() {
        return height;
    }

    // ========== Visual Styling ==========

    /**
     * Sets the background color (with alpha for transparency).
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    /**
     * Sets the border color.
     */
    public void setBorderColor(Color color) {
        this.borderColor = color;
    }

    /**
     * Sets the border width.
     */
    public void setBorderWidth(float width) {
        this.borderWidth = width;
    }

    /**
     * Sets whether to show the title bar.
     */
    public void setShowTitle(boolean show) {
        this.showTitle = show;
    }

    /**
     * Sets whether to show the border.
     */
    public void setShowBorder(boolean show) {
        this.showBorder = show;
    }

    /**
     * Sets whether to show the background.
     */
    public void setShowBackground(boolean show) {
        this.showBackground = show;
    }

    // ========== Data ==========

    /**
     * Sets the OHLC series to display.
     */
    public void setOhlcData(OhlcData series) {
        this.ohlcSeries = series;
        if (autoScalePriceRange && series != null) {
            updatePriceRangeFromData();
        }
        markDirty();
    }

    /**
     * Returns the OHLC series.
     */
    public OhlcData getOhlcData() {
        return ohlcSeries;
    }

    /**
     * Adds a line series overlay.
     */
    public void addOverlay(XyData series) {
        lineOverlays.add(series);
        markDirty();
    }

    /**
     * Removes a line series overlay.
     */
    public void removeOverlay(XyData series) {
        lineOverlays.remove(series);
        markDirty();
    }

    /**
     * Clears all overlays.
     */
    public void clearOverlays() {
        lineOverlays.clear();
        markDirty();
    }

    /**
     * Sets the visible time range for the inset.
     */
    public void setVisibleTimeRange(TimeRange range) {
        this.visibleTimeRange = range;
        if (autoScalePriceRange) {
            updatePriceRangeFromData();
        }
        markDirty();
    }

    /**
     * Sets the visible time range by start and end time.
     */
    public void setVisibleTimeRange(long startTime, long endTime) {
        setVisibleTimeRange(new TimeRange(startTime, endTime));
    }

    /**
     * Sets the visible price range for the inset.
     */
    public void setVisiblePriceRange(PriceRange range) {
        this.visiblePriceRange = range;
        this.autoScalePriceRange = false;
        markDirty();
    }

    /**
     * Sets the visible price range by min and max price.
     */
    public void setVisiblePriceRange(double minPrice, double maxPrice) {
        setVisiblePriceRange(new PriceRange(minPrice, maxPrice));
    }

    /**
     * Enables auto-scaling of the price range to fit data.
     */
    public void setAutoScalePriceRange(boolean autoScale) {
        this.autoScalePriceRange = autoScale;
        if (autoScale) {
            updatePriceRangeFromData();
        }
    }

    /**
     * Updates the parent chart size (called by render pipeline).
     */
    public void setParentSize(int width, int height) {
        this.parentWidth = width;
        this.parentHeight = height;
    }

    // ========== Rendering ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // Inset uses parent renderers - no additional initialization needed
    }

    @Override
    public void render(RenderContext ctx) {
        if (!isVisible()) {
            return;
        }

        GL2ES2 gl = ctx.getGL();

        // Calculate inset position
        Rectangle bounds = calculateBounds(ctx);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        // Set up scissor for clipping
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(bounds.x, parentHeight - bounds.y - bounds.height, bounds.width, bounds.height);

        // Draw background
        if (showBackground) {
            drawBackground(ctx, bounds);
        }

        // Create inset coordinate system
        updateInsetCoordinates(bounds);

        // Note: Full rendering would require the parent context to support
        // rendering with alternative coordinate systems. For now, this is
        // a placeholder - the actual candle/line rendering would need to be
        // done using the insetCoords.

        // Draw border
        if (showBorder) {
            drawBorder(ctx, bounds);
        }

        // Draw title
        if (showTitle && title != null) {
            drawTitle(ctx, bounds);
        }

        // Restore scissor
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        // No resources to dispose
    }

    // ========== Internal ==========

    private Rectangle calculateBounds(RenderContext ctx) {
        parentWidth = ctx.getViewport().getWidth();
        parentHeight = ctx.getViewport().getHeight();

        int x, y;

        switch (position) {
            case CORNER:
                switch (corner) {
                    case TOP_LEFT:
                        x = margin;
                        y = margin;
                        break;
                    case TOP_RIGHT:
                        x = parentWidth - width - margin;
                        y = margin;
                        break;
                    case BOTTOM_LEFT:
                        x = margin;
                        y = parentHeight - height - margin;
                        break;
                    case BOTTOM_RIGHT:
                    default:
                        x = parentWidth - width - margin;
                        y = parentHeight - height - margin;
                        break;
                }
                break;

            case ANCHORED_TO_POINT:
                CoordinateSystem mainCoords = ctx.getCoordinates();
                x = (int) mainCoords.xValueToScreenX(anchorTime) + 10;  // Offset from anchor
                y = (int) mainCoords.yValueToScreenY(anchorPrice) - height / 2;
                // Keep within bounds
                x = Math.max(0, Math.min(x, parentWidth - width));
                y = Math.max(0, Math.min(y, parentHeight - height));
                break;

            case FLOATING:
            default:
                x = floatingX;
                y = floatingY;
                // Keep within bounds
                x = Math.max(0, Math.min(x, parentWidth - width));
                y = Math.max(0, Math.min(y, parentHeight - height));
                break;
        }

        return new Rectangle(x, y, width, height);
    }

    private void updateInsetCoordinates(Rectangle bounds) {
        if (visibleTimeRange == null && ohlcSeries != null && !ohlcSeries.isEmpty()) {
            // Use full data range
            long start = ohlcSeries.getXValue(0);
            long end = ohlcSeries.getXValue(ohlcSeries.size() - 1);
            visibleTimeRange = new TimeRange(start, end);
        }

        if (visiblePriceRange == null && ohlcSeries != null) {
            updatePriceRangeFromData();
        }

        if (visibleTimeRange != null && visiblePriceRange != null) {
            // Create viewport for the inset bounds
            insetViewport = new Viewport();
            insetViewport.setSize(bounds.width, bounds.height);
            insetViewport.setInsets(0, 0, 0, 0);  // No insets for inset chart
            insetViewport.setTimeRange(visibleTimeRange.getStart(), visibleTimeRange.getEnd());
            insetViewport.setPriceRange(visiblePriceRange.getMin(), visiblePriceRange.getMax());

            // Create coordinate system using the viewport
            insetCoords = new CartesianCoordinateSystem(insetViewport);
            insetCoords.updateCache();
        }
    }

    private void updatePriceRangeFromData() {
        if (ohlcSeries == null || ohlcSeries.isEmpty()) {
            return;
        }

        float minPrice = Float.MAX_VALUE;
        float maxPrice = Float.MIN_VALUE;

        int startIdx = 0;
        int endIdx = ohlcSeries.size() - 1;

        if (visibleTimeRange != null) {
            startIdx = ohlcSeries.indexAtOrAfter(visibleTimeRange.getStart());
            endIdx = ohlcSeries.indexAtOrBefore(visibleTimeRange.getEnd());
            if (startIdx < 0) startIdx = 0;
            if (endIdx < 0) endIdx = ohlcSeries.size() - 1;
        }

        float[] lows = ohlcSeries.getLowArray();
        float[] highs = ohlcSeries.getHighArray();

        for (int i = startIdx; i <= endIdx && i < ohlcSeries.size(); i++) {
            if (lows[i] < minPrice) minPrice = lows[i];
            if (highs[i] > maxPrice) maxPrice = highs[i];
        }

        // Add padding
        double range = maxPrice - minPrice;
        double padding = range * 0.1;
        visiblePriceRange = new PriceRange(minPrice - padding, maxPrice + padding);
    }

    private void drawBackground(RenderContext ctx, Rectangle bounds) {
        // Background drawing would use a simple colored quad shader
        // Placeholder for actual implementation
    }

    private void drawBorder(RenderContext ctx, Rectangle bounds) {
        // Border drawing would use line rendering
        // Placeholder for actual implementation
    }

    private void drawTitle(RenderContext ctx, Rectangle bounds) {
        // Title drawing would use TextRenderer
        // Placeholder for actual implementation
    }

    // ========== Hit Testing ==========

    /**
     * Tests if a screen point is within this inset.
     *
     * @param screenX screen X coordinate
     * @param screenY screen Y coordinate
     * @param ctx render context for bounds calculation
     * @return true if the point is within the inset
     */
    public boolean containsPoint(int screenX, int screenY, RenderContext ctx) {
        Rectangle bounds = calculateBounds(ctx);
        return bounds.contains(screenX, screenY);
    }

    /**
     * Returns the inset's coordinate system for hit testing within.
     */
    public CoordinateSystem getInsetCoordinates() {
        return insetCoords;
    }
}
