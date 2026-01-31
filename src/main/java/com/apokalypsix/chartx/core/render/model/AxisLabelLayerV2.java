package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.TextRenderer;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for axis labels using GPU text rendering.
 *
 * <p>Renders Y-axis price labels and X-axis time labels directly on the GPU
 * using the TextRenderer API, replacing Java2D rendering in TextOverlay.
 */
public class AxisLabelLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(AxisLabelLayerV2.class);

    /** Z-order for axis labels (renders last, on top of everything) */
    public static final int Z_ORDER = 900;

    // Colors
    private Color axisLabelColor = new Color(140, 142, 146);

    // Font size
    private float fontSize = 11f;

    // Time formatters
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
    private final Date reusableDate = new Date();

    // Tick mark size (used for label positioning relative to tick marks drawn by AxisLayerV2)
    private static final int TICK_SIZE = 4;

    public AxisLabelLayerV2() {
        super(Z_ORDER);
        timeFormat.setTimeZone(TimeZone.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        log.debug("AxisLabelLayerV2 initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        ResourceManager resources = ctx.getResourceManager();
        TextRenderer textRenderer = resources.getTextRenderer();
        if (textRenderer == null) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        YAxisManager axisManager = ctx.getAxisManager();
        if (axisManager == null) {
            return;
        }

        int width = viewport.getWidth();
        int height = viewport.getHeight();

        if (!textRenderer.beginBatch(width, height)) {
            return;
        }

        try {
            // Set scale factor for HiDPI displays
            float scaleFactor = ctx.getScaleFactor();
            textRenderer.setScaleFactor(scaleFactor);
            textRenderer.setFontSize(fontSize);

            // Get actual rendered text height for layout
            float textHeight = textRenderer.getTextHeight();

            // Scale spacing values for HiDPI
            float tickSize = TICK_SIZE * scaleFactor;
            float labelSpacing = 2 * scaleFactor;
            float labelGap = 10 * scaleFactor;

            // Check if we have a category axis and determine rendering mode based on its position
            CategoryAxis categoryAxis = ctx.getCategoryAxis();
            boolean hasCategoryAxis = ctx.hasCategoryAxis();

            // Horizontal bar mode: categories on left/right (vertical), values at bottom
            // Vertical bar mode: categories at top/bottom (horizontal), values on left/right
            boolean horizontalBarMode = hasCategoryAxis && !categoryAxis.isHorizontal();

            if (hasCategoryAxis) {
                // Render category labels at the category axis position
                renderCategoryAxisLabels(ctx, textRenderer, textHeight, tickSize, labelSpacing, scaleFactor);

                if (horizontalBarMode) {
                    // Horizontal bars: value labels at bottom
                    renderHorizontalValueAxisLabels(ctx, textRenderer, axisManager, textHeight, tickSize, labelSpacing, labelGap, scaleFactor);
                } else {
                    // Vertical bars: value labels on left/right (standard Y-axis)
                    renderYAxisLabels(ctx, textRenderer, axisManager, textHeight, tickSize, labelSpacing, scaleFactor);
                }
            } else {
                // Standard time-based mode: Y-axis labels on left/right, time labels at bottom
                renderYAxisLabels(ctx, textRenderer, axisManager, textHeight, tickSize, labelSpacing, scaleFactor);
                renderXAxisLabels(ctx, textRenderer, textHeight, tickSize, labelSpacing, labelGap, scaleFactor);
            }

        } finally {
            textRenderer.endBatch();
        }
    }

    private void renderYAxisLabels(RenderContext ctx, TextRenderer textRenderer, YAxisManager axisManager, float textHeight, float tickSize, float labelSpacing, float scaleFactor) {
        Viewport viewport = ctx.getViewport();

        // Insets are already in physical pixels (scaled when set)
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();
        int viewportHeight = viewport.getHeight();

        // Account for font descent and extra safety margin
        // Use 40% of text height as buffer to ensure descenders fit
        float bottomMargin = textHeight * 0.4f;
        // Maximum Y position for text baseline to keep text within viewport
        float maxTextY = viewportHeight - bottomMargin;

        // Get visible axes
        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);

        // Draw left axis labels (from chart area outward)
        int leftX = chartLeft;
        for (int i = leftAxes.size() - 1; i >= 0; i--) {
            YAxis axis = leftAxes.get(i);
            // Scale axis width from logical to physical pixels
            int axisWidth = (int)(axis.getWidth() * scaleFactor);
            leftX -= axisWidth;

            CoordinateSystem coords = ctx.getCoordinatesForAxis(axis.getId());
            double[] priceLevels = calculatePriceGridLevels(axis);

            Color labelColor = axis.getLabelColor();
            for (double price : priceLevels) {
                float y = (float) coords.yValueToScreenY(price);
                if (y >= chartTop && y <= chartBottom) {
                    String label = axis.formatValue(price);
                    float textX = leftX + axisWidth - textRenderer.getTextWidth(label) - tickSize - labelSpacing;
                    float textY = y + textHeight / 3; // Approximate vertical centering
                    // Ensure text doesn't extend past viewport bottom
                    textY = Math.min(textY, maxTextY);
                    textRenderer.drawText(label, textX, textY, labelColor);
                }
            }
        }

        // Draw right axis labels (from chart area outward)
        int rightX = chartRight;
        int viewportWidth = viewport.getWidth();
        for (YAxis axis : rightAxes) {
            // Scale axis width from logical to physical pixels
            int axisWidth = (int)(axis.getWidth() * scaleFactor);
            CoordinateSystem coords = ctx.getCoordinatesForAxis(axis.getId());
            double[] priceLevels = calculatePriceGridLevels(axis);

            // Right edge of this axis area (where text must not exceed)
            float axisRightEdge = Math.min(rightX + axisWidth, viewportWidth);

            Color labelColor = axis.getLabelColor();
            for (double price : priceLevels) {
                float y = (float) coords.yValueToScreenY(price);
                if (y >= chartTop && y <= chartBottom) {
                    String label = axis.formatValue(price);
                    float textWidth = textRenderer.getTextWidth(label);
                    // Position text so it doesn't overflow the axis area or viewport
                    float textX = Math.min(rightX + tickSize + labelSpacing,
                                           axisRightEdge - textWidth - labelSpacing);
                    float textY = y + textHeight / 3;
                    // Ensure text doesn't extend past viewport bottom
                    textY = Math.min(textY, maxTextY);
                    textRenderer.drawText(label, textX, textY, labelColor);
                }
            }

            rightX += axisWidth;
        }
    }

    private void renderXAxisLabels(RenderContext ctx, TextRenderer textRenderer, float textHeight, float tickSize, float labelSpacing, float labelGap, float scaleFactor) {
        Viewport viewport = ctx.getViewport();
        CoordinateSystem coords = ctx.getCoordinates();

        // Insets are already in physical pixels (scaled when set)
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();
        int viewportHeight = viewport.getHeight();

        // Account for font descent and extra safety margin
        // Use 40% of text height as buffer to ensure descenders fit
        float bottomMargin = textHeight * 0.4f;

        long[] timeLevels = calculateTimeGridLevels(viewport, ctx.getBarDuration());
        boolean showDate = needsDateLabels(timeLevels);

        float lastLabelRight = Float.NEGATIVE_INFINITY;

        for (long time : timeLevels) {
            float x = (float) coords.xValueToScreenX(time);
            if (x >= chartLeft && x <= chartRight) {
                reusableDate.setTime(time);
                String label = showDate ? dateFormat.format(reusableDate) : timeFormat.format(reusableDate);
                float labelWidth = textRenderer.getTextWidth(label);
                float textX = x - labelWidth / 2;

                // Avoid overlapping labels
                if (textX > lastLabelRight + labelGap) {
                    // Position baseline so text fits within viewport
                    float textY = Math.min(
                        chartBottom + tickSize + textHeight + labelSpacing,
                        viewportHeight - bottomMargin
                    );
                    textRenderer.drawTextCentered(label, x, textY, axisLabelColor);
                    lastLabelRight = textX + labelWidth;
                }
            }
        }
    }

    /**
     * Renders Y-axis values at the bottom for horizontal bar charts.
     *
     * <p>In horizontal bar mode, the Y-axis values (which control horizontal bar extent)
     * should be displayed at the bottom as an X-axis.
     */
    private void renderHorizontalValueAxisLabels(RenderContext ctx, TextRenderer textRenderer,
                                                  YAxisManager axisManager, float textHeight,
                                                  float tickSize, float labelSpacing, float labelGap, float scaleFactor) {
        Viewport viewport = ctx.getViewport();

        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();
        int viewportHeight = viewport.getHeight();

        float bottomMargin = textHeight * 0.4f;

        // Get the default Y-axis (controls horizontal bar values)
        YAxis axis = axisManager.getDefaultAxis();
        if (axis == null) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinatesForAxis(axis.getId());
        double[] valueLevels = calculatePriceGridLevels(axis);

        float lastLabelRight = Float.NEGATIVE_INFINITY;

        for (double value : valueLevels) {
            // For horizontal bars, Y-axis values map to X screen coordinates
            // priceToScreenY gives vertical position, but we need horizontal
            // The horizontal position is: chartLeft + (value - minValue) / (maxValue - minValue) * chartWidth
            double normalized = axis.normalize(value);
            float x = chartLeft + (float)(normalized * (chartRight - chartLeft));

            if (x >= chartLeft && x <= chartRight) {
                String label = axis.formatValue(value);
                float labelWidth = textRenderer.getTextWidth(label);
                float textX = x - labelWidth / 2;

                // Avoid overlapping labels
                if (textX > lastLabelRight + labelGap) {
                    float textY = Math.min(
                        chartBottom + tickSize + textHeight + labelSpacing,
                        viewportHeight - bottomMargin
                    );
                    textRenderer.drawTextCentered(label, x, textY, axis.getLabelColor());
                    lastLabelRight = textX + labelWidth;
                }
            }
        }
    }

    /**
     * Renders category axis labels using GPU text rendering.
     */
    private void renderCategoryAxisLabels(RenderContext ctx, TextRenderer textRenderer,
                                           float textHeight, float tickSize, float labelSpacing, float scaleFactor) {
        CategoryAxis axis = ctx.getCategoryAxis();
        if (axis == null || !axis.isVisible() || axis.getCategoryCount() == 0) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        int chartTop = viewport.getTopInset();
        int chartBottom = viewport.getHeight() - viewport.getBottomInset();
        int chartLeft = viewport.getLeftInset();
        int chartRight = viewport.getWidth() - viewport.getRightInset();
        int viewportHeight = viewport.getHeight();

        // Scale axis width for HiDPI
        int axisWidth = (int) (axis.getHeight() * scaleFactor);

        Color labelColor = axis.getLabelColor();

        // Account for font descent and extra safety margin
        float bottomMargin = textHeight * 0.4f;
        float maxTextY = viewportHeight - bottomMargin;

        // Use coordinate system for viewport-aware positioning
        CoordinateSystem coords = ctx.getCoordinates();
        boolean isVertical = !axis.isHorizontal();

        // Minimum gap between labels to prevent overlapping
        float labelGap = 10 * scaleFactor;

        // Track last rendered label position to prevent overlap
        float lastHorizontalRight = Float.NEGATIVE_INFINITY;
        // For vertical axes (LEFT/RIGHT), we iterate from top to bottom (low Y to high Y),
        // so track the bottom edge of the last rendered label
        float lastVerticalBottom = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < axis.getCategoryCount(); i++) {
            String label = axis.getLabel(i);
            if (label == null || label.isEmpty()) {
                continue;
            }

            // Calculate position using viewport-aware coordinates
            // For both horizontal and vertical category axes, use xValueToScreenX since
            // categories are indexed via the time/X dimension (setVisibleRange controls this)
            double pos = coords.xValueToScreenX((long) i);
            if (!isVertical) {
                // Horizontal category axis: add half bar width for centering
                pos += ctx.getBarWidth() / 2.0;
            }

            switch (axis.getInternalPosition()) {
                case LEFT:
                    // Labels on the left side of the chart
                    if (pos >= chartTop && pos <= chartBottom) {
                        float textY = (float) pos + textHeight / 3;
                        // Check for vertical overlap - we iterate top to bottom (low Y to high Y),
                        // so ensure this label's top is below the last label's bottom
                        if (textY > lastVerticalBottom + labelGap) {
                            float labelWidth = textRenderer.getTextWidth(label);
                            float textX = axisWidth - labelWidth - tickSize - labelSpacing;
                            textY = Math.min(textY, maxTextY);
                            textRenderer.drawText(label, textX, textY, labelColor);
                            lastVerticalBottom = textY + textHeight;
                        }
                    }
                    break;

                case RIGHT:
                    // Labels on the right side of the chart
                    if (pos >= chartTop && pos <= chartBottom) {
                        float textY = (float) pos + textHeight / 3;
                        // Check for vertical overlap - we iterate top to bottom
                        if (textY > lastVerticalBottom + labelGap) {
                            float textX = chartRight + tickSize + labelSpacing;
                            textY = Math.min(textY, maxTextY);
                            textRenderer.drawText(label, textX, textY, labelColor);
                            lastVerticalBottom = textY + textHeight;
                        }
                    }
                    break;

                case TOP:
                    // Labels on the top of the chart
                    if (pos >= chartLeft && pos <= chartRight) {
                        float labelWidth = textRenderer.getTextWidth(label);
                        float textX = (float) pos - labelWidth / 2;
                        // Check for horizontal overlap
                        if (textX > lastHorizontalRight + labelGap) {
                            float textY = axisWidth - tickSize - labelSpacing;
                            textRenderer.drawTextCentered(label, (float) pos, textY, labelColor);
                            lastHorizontalRight = textX + labelWidth;
                        }
                    }
                    break;

                case BOTTOM:
                    // Labels on the bottom of the chart
                    if (pos >= chartLeft && pos <= chartRight) {
                        float labelWidth = textRenderer.getTextWidth(label);
                        float textX = (float) pos - labelWidth / 2;
                        // Check for horizontal overlap
                        if (textX > lastHorizontalRight + labelGap) {
                            float textY = Math.min(
                                chartBottom + tickSize + textHeight + labelSpacing,
                                viewportHeight - bottomMargin
                            );
                            textRenderer.drawTextCentered(label, (float) pos, textY, labelColor);
                            lastHorizontalRight = textX + labelWidth;
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Calculates price levels for Y-axis labels based on axis value range and scale.
     *
     * <p>Delegates to the axis's scale for proper grid level calculation
     * (e.g., log scales use powers instead of linear intervals).
     */
    private double[] calculatePriceGridLevels(YAxis axis) {
        return axis.calculateGridLevels(8);
    }

    private static final int MAX_GRID_LEVELS = 100;

    /**
     * Calculates time levels for X-axis labels.
     */
    private long[] calculateTimeGridLevels(Viewport viewport, long barDuration) {
        long startTime = viewport.getStartTime();
        long endTime = viewport.getEndTime();
        long duration = endTime - startTime;

        if (duration <= 0 || startTime >= endTime) {
            return new long[0];
        }

        long interval = calculateTimeInterval(duration);
        if (interval <= 0) {
            return new long[0];
        }

        long firstLevel = ((startTime / interval) + 1) * interval;

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
        if (duration <= 0) {
            return 60000L;
        }

        long roughInterval = duration / 8;

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
                2592000000L,     // 30 days
                7776000000L,     // 90 days
                31536000000L     // 365 days
        };

        for (long interval : niceIntervals) {
            if (interval >= roughInterval) {
                return interval;
            }
        }
        return niceIntervals[niceIntervals.length - 1];
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        // No V2 resources to dispose - TextRenderer is managed by ResourceManager
    }

    // ========== Configuration ==========

    public void setAxisLabelColor(Color color) {
        this.axisLabelColor = color;
    }

    public Color getAxisLabelColor() {
        return axisLabelColor;
    }

    public void setFontSize(float size) {
        this.fontSize = size;
    }

    public float getFontSize() {
        return fontSize;
    }
}
