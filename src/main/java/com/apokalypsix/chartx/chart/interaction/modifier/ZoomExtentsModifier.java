package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

/**
 * Modifier for zooming to fit all data.
 *
 * <p>This modifier allows users to quickly zoom out to see all data in the chart.
 * By default, it triggers on double-click.
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * chart.getModifiers().with(new ZoomExtentsModifier()
 *     .setExecuteOn(ExecuteOn.DOUBLE_CLICK)
 *     .setAnimated(true));
 * }</pre>
 */
public class ZoomExtentsModifier extends ChartModifierBase {

    /** When to execute zoom extents */
    private ExecuteOn executeOn = ExecuteOn.DOUBLE_CLICK;

    /** Whether to animate the zoom */
    private boolean animated = false;

    /** Padding percentage around data */
    private double paddingPercent = 0.05;

    /**
     * Creates a new ZoomExtentsModifier with default settings.
     */
    public ZoomExtentsModifier() {
    }

    // ========== Configuration ==========

    /**
     * Sets when zoom extents should be executed.
     *
     * @param executeOn the execution trigger
     * @return this modifier for chaining
     */
    public ZoomExtentsModifier setExecuteOn(ExecuteOn executeOn) {
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
     * Sets whether the zoom should be animated.
     *
     * <p>Note: Animation is not yet implemented.
     *
     * @param animated true to animate
     * @return this modifier for chaining
     */
    public ZoomExtentsModifier setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    /**
     * Returns whether zoom animation is enabled.
     *
     * @return true if animated
     */
    public boolean isAnimated() {
        return animated;
    }

    /**
     * Sets the padding percentage around the data.
     *
     * @param percent padding as a decimal (e.g., 0.05 for 5%)
     * @return this modifier for chaining
     */
    public ZoomExtentsModifier setPaddingPercent(double percent) {
        this.paddingPercent = percent;
        return this;
    }

    /**
     * Returns the padding percentage.
     *
     * @return the padding as a decimal
     */
    public double getPaddingPercent() {
        return paddingPercent;
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        if (!shouldExecute(args)) {
            return false;
        }

        zoomToExtents();
        return true;
    }

    /**
     * Checks if this modifier should execute based on the event and configuration.
     */
    private boolean shouldExecute(ModifierMouseEventArgs args) {
        return switch (executeOn) {
            case DOUBLE_CLICK -> args.isLeftButton() && args.getClickCount() >= 2;
            case LEFT_CLICK -> args.isLeftButton();
            case MIDDLE_CLICK -> args.isMiddleButton();
        };
    }

    /**
     * Zooms the viewport to fit all data.
     */
    public void zoomToExtents() {
        var viewport = getViewport();
        if (viewport == null || surface == null) {
            return;
        }

        Data<?> data = surface.getPrimaryData();
        if (data == null || data.isEmpty()) {
            return;
        }

        // Get data time range
        long startTime = data.getMinX();
        long endTime = data.getMaxX();
        long timePadding = (long) ((endTime - startTime) * paddingPercent);

        // Set time range
        viewport.setTimeRange(startTime - timePadding, endTime + timePadding);

        // Auto-scale Y axis
        viewport.setAutoScaleY(true);

        var axisManager = getAxisManager();
        if (axisManager != null) {
            var defaultAxis = axisManager.getDefaultAxis();
            if (defaultAxis != null) {
                defaultAxis.setAutoScale(true);
            }
        }

        requestRepaint();
    }

    /**
     * Execution trigger options.
     */
    public enum ExecuteOn {
        /** Execute on double-click */
        DOUBLE_CLICK,
        /** Execute on single left click */
        LEFT_CLICK,
        /** Execute on middle click */
        MIDDLE_CLICK
    }
}
