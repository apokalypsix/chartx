package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import javax.swing.*;
import java.util.List;

/**
 * Modifier for mouse wheel zoom functionality.
 *
 * <p>This modifier handles zoom operations via the mouse wheel. It supports
 * zone-aware zooming:
 * <ul>
 *   <li>Over chart area: zooms both axes (or X only if Y is auto-scaling)</li>
 *   <li>Over X-axis: zooms time axis only</li>
 *   <li>Over Y-axis: zooms that specific axis only</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@link #setZoomFactor(double)} - Change zoom sensitivity</li>
 *   <li>{@link #setXAxisZoomEnabled(boolean)} - Enable/disable X-axis zooming</li>
 *   <li>{@link #setYAxisZoomEnabled(boolean)} - Enable/disable Y-axis zooming</li>
 *   <li>{@link #setGrowFactor(double)} - Factor when zooming in</li>
 *   <li>{@link #setShrinkFactor(double)} - Factor when zooming out</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * chart.getModifiers().with(new MouseWheelZoomModifier()
 *     .setGrowFactor(1.2)
 *     .setShrinkFactor(0.8));
 * }</pre>
 */
public class MouseWheelZoomModifier extends ChartModifierBase {

    /** Default zoom factor for zooming in */
    private static final double DEFAULT_GROW_FACTOR = 1.1;

    /** Default zoom factor for zooming out */
    private static final double DEFAULT_SHRINK_FACTOR = 0.9;

    /** Minimum time between repaints in milliseconds (throttling) */
    private static final long THROTTLE_MS = 16; // ~60 FPS

    /** Zoom factor when scrolling up (zoom in) */
    private double growFactor = DEFAULT_GROW_FACTOR;

    /** Zoom factor when scrolling down (zoom out) */
    private double shrinkFactor = DEFAULT_SHRINK_FACTOR;

    /** Whether X-axis zooming is enabled */
    private boolean xAxisZoomEnabled = true;

    /** Whether Y-axis zooming is enabled */
    private boolean yAxisZoomEnabled = true;

    /** Minimum wheel rotation to trigger zoom */
    private double minWheelDelta = 0.001;

    /** Accumulated zoom factor for throttling */
    private double accumulatedFactor = 1.0;

    /** Last anchor position for throttled zoom */
    private int lastAnchorX = 0;
    private int lastAnchorY = 0;

    /** Last time a repaint was triggered */
    private long lastRepaintTime = 0;

    /** Whether we have pending zoom to apply */
    private boolean hasPendingZoom = false;

    /** Stored zone info for deferred zoom application */
    private String pendingHitAxisId = null;
    private boolean pendingInXAxisArea = false;
    private boolean pendingInChartArea = false;

    /** Timer to flush pending zoom when user stops scrolling */
    private Timer flushTimer;

    /**
     * Creates a new MouseWheelZoomModifier with default settings.
     */
    public MouseWheelZoomModifier() {
        // Timer to flush any pending zoom after scrolling stops
        flushTimer = new Timer((int) THROTTLE_MS * 2, e -> {
            if (hasPendingZoom) {
                applyAccumulatedZoom();
            }
        });
        flushTimer.setRepeats(false);
    }

    // ========== Configuration ==========

    /**
     * Sets the zoom factor for zooming in (scroll up/toward user).
     *
     * @param factor the grow factor (should be > 1.0)
     * @return this modifier for chaining
     */
    public MouseWheelZoomModifier setGrowFactor(double factor) {
        this.growFactor = factor;
        return this;
    }

    /**
     * Returns the grow factor for zooming in.
     *
     * @return the grow factor
     */
    public double getGrowFactor() {
        return growFactor;
    }

    /**
     * Sets the zoom factor for zooming out (scroll down/away from user).
     *
     * @param factor the shrink factor (should be < 1.0)
     * @return this modifier for chaining
     */
    public MouseWheelZoomModifier setShrinkFactor(double factor) {
        this.shrinkFactor = factor;
        return this;
    }

    /**
     * Returns the shrink factor for zooming out.
     *
     * @return the shrink factor
     */
    public double getShrinkFactor() {
        return shrinkFactor;
    }

    /**
     * Sets both grow and shrink factors at once.
     *
     * @param factor the base zoom factor (grow = factor, shrink = 1/factor)
     * @return this modifier for chaining
     */
    public MouseWheelZoomModifier setZoomFactor(double factor) {
        this.growFactor = factor;
        this.shrinkFactor = 1.0 / factor;
        return this;
    }

    /**
     * Enables or disables X-axis zooming.
     *
     * @param enabled true to enable
     * @return this modifier for chaining
     */
    public MouseWheelZoomModifier setXAxisZoomEnabled(boolean enabled) {
        this.xAxisZoomEnabled = enabled;
        return this;
    }

    /**
     * Returns whether X-axis zooming is enabled.
     *
     * @return true if enabled
     */
    public boolean isXAxisZoomEnabled() {
        return xAxisZoomEnabled;
    }

    /**
     * Enables or disables Y-axis zooming.
     *
     * @param enabled true to enable
     * @return this modifier for chaining
     */
    public MouseWheelZoomModifier setYAxisZoomEnabled(boolean enabled) {
        this.yAxisZoomEnabled = enabled;
        return this;
    }

    /**
     * Returns whether Y-axis zooming is enabled.
     *
     * @return true if enabled
     */
    public boolean isYAxisZoomEnabled() {
        return yAxisZoomEnabled;
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMouseWheel(ModifierMouseEventArgs args) {
        double rotation = args.getWheelRotation();
        if (Math.abs(rotation) < minWheelDelta) {
            return false;
        }

        double factor = rotation < 0 ? growFactor : shrinkFactor;
        int x = args.getScreenX();
        int y = args.getScreenY();

        // Determine zoom zone
        String hitAxisId = args.getHitAxisId();
        boolean inXAxisArea = args.isInXAxisArea();
        boolean inChartArea = args.isInChartArea();

        if (hitAxisId == null && !inXAxisArea && !inChartArea) {
            return false;
        }

        // Accumulate the zoom factor and store zone info
        accumulatedFactor *= factor;
        lastAnchorX = x;
        lastAnchorY = y;
        pendingHitAxisId = hitAxisId;
        pendingInXAxisArea = inXAxisArea;
        pendingInChartArea = inChartArea;
        hasPendingZoom = true;

        // Check if we should apply the zoom now or wait (throttling)
        long now = System.currentTimeMillis();
        if (now - lastRepaintTime >= THROTTLE_MS) {
            applyAccumulatedZoom();
            lastRepaintTime = now;
        }

        // Restart the flush timer to catch any pending zoom when scrolling stops
        flushTimer.restart();

        return true;
    }

    /**
     * Applies accumulated zoom and resets the accumulator.
     */
    private void applyAccumulatedZoom() {
        if (!hasPendingZoom || accumulatedFactor == 1.0) {
            return;
        }

        if (pendingHitAxisId != null && yAxisZoomEnabled) {
            // Over a Y-axis - zoom that axis only
            zoomYAxis(pendingHitAxisId, accumulatedFactor, lastAnchorY);
        } else if (pendingInXAxisArea && xAxisZoomEnabled) {
            // Over X-axis area - zoom time only
            zoomTimeAxis(accumulatedFactor, lastAnchorX);
        } else if (pendingInChartArea) {
            // In chart area - combined zoom
            zoomChartArea(accumulatedFactor, lastAnchorX, lastAnchorY);
        }

        // Reset accumulator
        accumulatedFactor = 1.0;
        hasPendingZoom = false;
        pendingHitAxisId = null;

        requestRepaint();
    }

    /**
     * Zooms the time axis around the given anchor point.
     */
    private void zoomTimeAxis(double factor, int anchorX) {
        var viewport = getViewport();
        if (viewport != null && xAxisZoomEnabled) {
            viewport.zoomTime(factor, anchorX);
        }
    }

    /**
     * Zooms a specific Y-axis around the given anchor point.
     */
    private void zoomYAxis(String axisId, double factor, int anchorY) {
        var axisManager = getAxisManager();
        if (axisManager == null) {
            return;
        }

        YAxis axis = axisManager.getAxis(axisId);
        if (axis == null) {
            return;
        }

        if (axis.isAutoScale()) {
            axis.setAutoScale(false);
        }

        var viewport = getViewport();
        int chartTop = surface.getChartTop();
        int chartBottom = surface.getChartBottom();
        int chartHeight = chartBottom - chartTop;

        // Calculate anchor value in axis space
        double normalized = 1.0 - (double) (anchorY - chartTop) / chartHeight;
        double anchorValue = axis.interpolate(normalized);

        double bottomSpan = anchorValue - axis.getMinValue();
        double topSpan = axis.getMaxValue() - anchorValue;

        double newMin = anchorValue - bottomSpan / factor;
        double newMax = anchorValue + topSpan / factor;

        // Enforce minimum range
        double minRange = 0.01;
        if (newMax - newMin < minRange) {
            double center = (newMin + newMax) / 2;
            newMin = center - minRange / 2;
            newMax = center + minRange / 2;
        }

        axis.setValueRange(newMin, newMax);

        // Sync default axis with viewport
        if (YAxis.DEFAULT_AXIS_ID.equals(axis.getId()) && viewport != null) {
            viewport.setPriceRange(newMin, newMax);
            viewport.setAutoScaleY(false);
        }

        // Invalidate coordinate cache
        var coords = getCoordinates();
        if (coords instanceof com.apokalypsix.chartx.core.coordinate.CartesianCoordinateSystem ccs) {
            ccs.invalidateCache();
        }
    }

    /**
     * Zooms in the chart area with appropriate behavior based on axis state.
     */
    private void zoomChartArea(double factor, int anchorX, int anchorY) {
        var viewport = getViewport();
        var axisManager = getAxisManager();

        if (viewport == null) {
            return;
        }

        YAxis defaultAxis = axisManager != null ? axisManager.getDefaultAxis() : null;

        // If default axis is auto-scaling, only zoom time
        if (defaultAxis != null && defaultAxis.isAutoScale()) {
            if (xAxisZoomEnabled) {
                viewport.zoomTime(factor, anchorX);
            }
        } else {
            // Zoom both axes
            if (xAxisZoomEnabled && yAxisZoomEnabled) {
                viewport.zoom(factor, anchorX, anchorY);
            } else if (xAxisZoomEnabled) {
                viewport.zoomTime(factor, anchorX);
            } else if (yAxisZoomEnabled) {
                viewport.zoomPrice(factor, anchorY);
            }

            // Also zoom the default axis if not auto-scaling
            if (defaultAxis != null && !defaultAxis.isAutoScale() && yAxisZoomEnabled) {
                zoomYAxis(defaultAxis.getId(), factor, anchorY);
            }
        }
    }

    /**
     * Finds the Y-axis at the given screen position.
     *
     * @param x screen X coordinate
     * @param y screen Y coordinate
     * @return the axis at that position, or null
     */
    private YAxis findAxisAtPosition(int x, int y) {
        var axisManager = getAxisManager();
        if (axisManager == null || surface == null) {
            return null;
        }

        int chartLeft = surface.getChartLeft();
        int chartRight = surface.getChartRight();
        int chartTop = surface.getChartTop();
        int chartBottom = surface.getChartBottom();

        // Not in Y range of chart
        if (y < chartTop || y > chartBottom) {
            return null;
        }

        // Check left axes
        List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);
        int leftX = 0;
        for (YAxis axis : leftAxes) {
            if (x >= leftX && x < leftX + axis.getWidth()) {
                return axis;
            }
            leftX += axis.getWidth();
        }

        // Check right axes
        List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);
        int rightX = chartRight;
        for (YAxis axis : rightAxes) {
            if (x >= rightX && x < rightX + axis.getWidth()) {
                return axis;
            }
            rightX += axis.getWidth();
        }

        return null;
    }
}
