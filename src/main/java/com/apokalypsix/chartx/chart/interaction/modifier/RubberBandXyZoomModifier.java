package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierDrawContext;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.awt.*;

/**
 * Modifier for rubber-band box zoom functionality.
 *
 * <p>This modifier allows users to drag a rectangle to zoom into a specific
 * area of the chart. The zoom is performed when the mouse is released.
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * chart.getModifiers().with(new RubberBandXyZoomModifier()
 *     .setExecuteOn(ExecuteOn.SHIFT_LEFT_CLICK)
 *     .setZoomXEnabled(true)
 *     .setZoomYEnabled(true));
 * }</pre>
 */
public class RubberBandXyZoomModifier extends ChartModifierBase {

    /** When to execute the rubber band zoom */
    private ExecuteOn executeOn = ExecuteOn.SHIFT_LEFT_CLICK;

    /** Whether X-axis zoom is enabled */
    private boolean zoomXEnabled = true;

    /** Whether Y-axis zoom is enabled */
    private boolean zoomYEnabled = true;

    /** Whether currently drawing the rubber band */
    private boolean isRubberBanding = false;

    /** Start X of rubber band */
    private int startX;

    /** Start Y of rubber band */
    private int startY;

    /** Current X of rubber band */
    private int currentX;

    /** Current Y of rubber band */
    private int currentY;

    /** Minimum drag distance to trigger zoom */
    private int minDragDistance = 10;

    /** Rubber band fill color */
    private Color fillColor = new Color(100, 149, 237, 50); // Cornflower blue with alpha

    /** Rubber band border color */
    private Color borderColor = new Color(100, 149, 237, 200);

    /**
     * Creates a new RubberBandXyZoomModifier with default settings.
     */
    public RubberBandXyZoomModifier() {
    }

    // ========== Configuration ==========

    /**
     * Sets when the rubber band zoom should be executed.
     *
     * @param executeOn the execution trigger
     * @return this modifier for chaining
     */
    public RubberBandXyZoomModifier setExecuteOn(ExecuteOn executeOn) {
        this.executeOn = executeOn;
        return this;
    }

    /**
     * Returns the execution trigger.
     *
     * @return the execution trigger
     */
    public ExecuteOn getExecuteOn() {
        return executeOn;
    }

    /**
     * Enables or disables X-axis zoom.
     *
     * @param enabled true to enable
     * @return this modifier for chaining
     */
    public RubberBandXyZoomModifier setZoomXEnabled(boolean enabled) {
        this.zoomXEnabled = enabled;
        return this;
    }

    /**
     * Returns whether X-axis zoom is enabled.
     *
     * @return true if enabled
     */
    public boolean isZoomXEnabled() {
        return zoomXEnabled;
    }

    /**
     * Enables or disables Y-axis zoom.
     *
     * @param enabled true to enable
     * @return this modifier for chaining
     */
    public RubberBandXyZoomModifier setZoomYEnabled(boolean enabled) {
        this.zoomYEnabled = enabled;
        return this;
    }

    /**
     * Returns whether Y-axis zoom is enabled.
     *
     * @return true if enabled
     */
    public boolean isZoomYEnabled() {
        return zoomYEnabled;
    }

    /**
     * Sets the fill color for the rubber band rectangle.
     *
     * @param color the fill color
     * @return this modifier for chaining
     */
    public RubberBandXyZoomModifier setFillColor(Color color) {
        this.fillColor = color;
        return this;
    }

    /**
     * Sets the border color for the rubber band rectangle.
     *
     * @param color the border color
     * @return this modifier for chaining
     */
    public RubberBandXyZoomModifier setBorderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        if (!shouldExecute(args)) {
            return false;
        }

        if (!args.isInChartArea()) {
            return false;
        }

        isRubberBanding = true;
        startX = args.getScreenX();
        startY = args.getScreenY();
        currentX = startX;
        currentY = startY;

        return true;
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        if (!isRubberBanding) {
            return false;
        }

        isRubberBanding = false;

        // Check minimum drag distance
        int width = Math.abs(currentX - startX);
        int height = Math.abs(currentY - startY);

        if (width < minDragDistance && height < minDragDistance) {
            requestRepaint();
            return true;
        }

        // Perform zoom
        performZoom();

        requestRepaint();
        return true;
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        if (!isRubberBanding) {
            return false;
        }

        currentX = args.getScreenX();
        currentY = args.getScreenY();

        // Clip to chart area
        int chartLeft = surface.getChartLeft();
        int chartRight = surface.getChartRight();
        int chartTop = surface.getChartTop();
        int chartBottom = surface.getChartBottom();

        currentX = Math.max(chartLeft, Math.min(chartRight, currentX));
        currentY = Math.max(chartTop, Math.min(chartBottom, currentY));

        requestRepaint();
        return true;
    }

    /**
     * Checks if this modifier should execute based on the event and configuration.
     */
    private boolean shouldExecute(ModifierMouseEventArgs args) {
        if (!args.isLeftButton()) {
            return false;
        }

        return switch (executeOn) {
            case LEFT_CLICK -> true;
            case SHIFT_LEFT_CLICK -> args.isShiftDown();
            case CTRL_LEFT_CLICK -> args.isCtrlDown();
            case ALT_LEFT_CLICK -> args.isAltDown();
        };
    }

    /**
     * Performs the zoom operation based on the rubber band rectangle.
     */
    private void performZoom() {
        var viewport = getViewport();
        var coords = getCoordinates();

        if (viewport == null || coords == null) {
            return;
        }

        // Calculate bounds
        int left = Math.min(startX, currentX);
        int right = Math.max(startX, currentX);
        int top = Math.min(startY, currentY);
        int bottom = Math.max(startY, currentY);

        // Zoom X axis
        if (zoomXEnabled && right > left) {
            long startTime = coords.screenXToXValue(left);
            long endTime = coords.screenXToXValue(right);
            viewport.setTimeRange(startTime, endTime);
        }

        // Zoom Y axis
        if (zoomYEnabled && bottom > top) {
            double maxPrice = coords.screenYToYValue(top);
            double minPrice = coords.screenYToYValue(bottom);
            viewport.setPriceRange(minPrice, maxPrice);
            viewport.setAutoScaleY(false);

            // Also update default Y-axis
            var axisManager = getAxisManager();
            if (axisManager != null) {
                var defaultAxis = axisManager.getDefaultAxis();
                if (defaultAxis != null) {
                    defaultAxis.setValueRange(minPrice, maxPrice);
                    defaultAxis.setAutoScale(false);
                }
            }
        }
    }

    // ========== Rendering ==========

    @Override
    public void onDraw(ModifierDrawContext ctx) {
        if (!isRubberBanding) {
            return;
        }

        Graphics2D g = ctx.getGraphics();

        int left = Math.min(startX, currentX);
        int top = Math.min(startY, currentY);
        int width = Math.abs(currentX - startX);
        int height = Math.abs(currentY - startY);

        // Draw fill
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fillRect(left, top, width, height);
        }

        // Draw border
        if (borderColor != null) {
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{5.0f}, 0.0f)); // Dashed line
            g.drawRect(left, top, width, height);
        }
    }

    /**
     * Execution trigger options.
     */
    public enum ExecuteOn {
        /** Execute on plain left click */
        LEFT_CLICK,
        /** Execute on shift + left click */
        SHIFT_LEFT_CLICK,
        /** Execute on ctrl + left click */
        CTRL_LEFT_CLICK,
        /** Execute on alt + left click */
        ALT_LEFT_CLICK
    }
}
