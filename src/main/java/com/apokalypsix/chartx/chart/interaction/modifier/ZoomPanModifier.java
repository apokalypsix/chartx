package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.awt.Cursor;

/**
 * Modifier for drag-to-pan functionality.
 *
 * <p>This modifier enables panning the chart by clicking and dragging.
 * By default, left mouse button initiates panning.
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@link #setClipToExtents(boolean)} - Prevent panning beyond data extents</li>
 *   <li>{@link #setExecuteOnButton(int)} - Change which button triggers pan</li>
 *   <li>{@link #setPanXEnabled(boolean)} - Enable/disable horizontal panning</li>
 *   <li>{@link #setPanYEnabled(boolean)} - Enable/disable vertical panning</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * chart.getModifiers().with(new ZoomPanModifier()
 *     .setClipToExtents(true)
 *     .setPanYEnabled(false));
 * }</pre>
 */
public class ZoomPanModifier extends ChartModifierBase {

    /** Whether to clip panning to data extents */
    private boolean clipToExtents = false;

    /** The mouse button that triggers pan (default: left = 1) */
    private int executeOnButton = 1;

    /** Whether horizontal panning is enabled */
    private boolean panXEnabled = true;

    /** Whether vertical panning is enabled */
    private boolean panYEnabled = true;

    /** Whether we are currently panning */
    private boolean isPanning = false;

    /** Last mouse X position during pan */
    private int lastX;

    /** Last mouse Y position during pan */
    private int lastY;

    /** Cursor to show during panning */
    private static final Cursor PAN_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

    /**
     * Creates a new ZoomPanModifier with default settings.
     */
    public ZoomPanModifier() {
    }

    // ========== Configuration ==========

    /**
     * Sets whether panning should be clipped to data extents.
     *
     * <p>When enabled, users cannot pan beyond the actual data boundaries.
     *
     * @param clipToExtents true to clip panning
     * @return this modifier for chaining
     */
    public ZoomPanModifier setClipToExtents(boolean clipToExtents) {
        this.clipToExtents = clipToExtents;
        return this;
    }

    /**
     * Returns whether panning is clipped to data extents.
     *
     * @return true if clipping is enabled
     */
    public boolean isClipToExtents() {
        return clipToExtents;
    }

    /**
     * Sets the mouse button that triggers panning.
     *
     * @param button the button number (1=left, 2=middle, 3=right)
     * @return this modifier for chaining
     */
    public ZoomPanModifier setExecuteOnButton(int button) {
        this.executeOnButton = button;
        return this;
    }

    /**
     * Returns the mouse button that triggers panning.
     *
     * @return the button number
     */
    public int getExecuteOnButton() {
        return executeOnButton;
    }

    /**
     * Enables or disables horizontal panning.
     *
     * @param enabled true to enable horizontal panning
     * @return this modifier for chaining
     */
    public ZoomPanModifier setPanXEnabled(boolean enabled) {
        this.panXEnabled = enabled;
        return this;
    }

    /**
     * Returns whether horizontal panning is enabled.
     *
     * @return true if enabled
     */
    public boolean isPanXEnabled() {
        return panXEnabled;
    }

    /**
     * Enables or disables vertical panning.
     *
     * @param enabled true to enable vertical panning
     * @return this modifier for chaining
     */
    public ZoomPanModifier setPanYEnabled(boolean enabled) {
        this.panYEnabled = enabled;
        return this;
    }

    /**
     * Returns whether vertical panning is enabled.
     *
     * @return true if enabled
     */
    public boolean isPanYEnabled() {
        return panYEnabled;
    }

    /**
     * Returns whether a pan operation is currently in progress.
     *
     * @return true if panning
     */
    public boolean isPanning() {
        return isPanning;
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        if (args.getButton() != executeOnButton) {
            return false;
        }

        // Only start pan if in chart area
        if (!args.isInChartArea()) {
            return false;
        }

        isPanning = true;
        lastX = args.getScreenX();
        lastY = args.getScreenY();
        setCursor(PAN_CURSOR);

        return true;
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        if (!isPanning) {
            return false;
        }

        isPanning = false;
        setCursor(null);

        return true;
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        if (!isPanning) {
            return false;
        }

        int dx = args.getScreenX() - lastX;
        int dy = args.getScreenY() - lastY;

        lastX = args.getScreenX();
        lastY = args.getScreenY();

        // Apply axis constraints
        if (!panXEnabled) {
            dx = 0;
        }
        if (!panYEnabled) {
            dy = 0;
        }

        if (dx != 0 || dy != 0) {
            var viewport = getViewport();
            if (viewport != null) {
                viewport.pan(dx, dy);

                // When Y panning is enabled and auto-scale is disabled,
                // sync the viewport's price range to the Y-axis
                if (panYEnabled && dy != 0 && !viewport.isAutoScaleY()) {
                    syncYAxisWithViewport();
                }

                if (clipToExtents) {
                    clipToDataExtents();
                }

                requestRepaint();
            }
        }

        return true;
    }

    /**
     * Syncs the Y-axis value range with the viewport's price range.
     * Called when panning with auto-scale disabled.
     */
    private void syncYAxisWithViewport() {
        var viewport = getViewport();
        var axisManager = getAxisManager();
        if (viewport == null || axisManager == null) {
            return;
        }
        var defaultAxis = axisManager.getDefaultAxis();
        if (defaultAxis != null && !defaultAxis.isAutoScale()) {
            defaultAxis.setValueRange(viewport.getMinPrice(), viewport.getMaxPrice());
        }
    }

    /**
     * Clips the viewport to data extents if configured.
     */
    private void clipToDataExtents() {
        var viewport = getViewport();
        var data = surface != null ? surface.getPrimaryData() : null;

        if (viewport == null || data == null || data.isEmpty()) {
            return;
        }

        long dataStart = data.getMinX();
        long dataEnd = data.getMaxX();

        long viewStart = viewport.getStartTime();
        long viewEnd = viewport.getEndTime();
        long viewDuration = viewEnd - viewStart;

        // Clip to data bounds
        if (viewStart < dataStart) {
            viewport.setTimeRange(dataStart, dataStart + viewDuration);
        } else if (viewEnd > dataEnd) {
            viewport.setTimeRange(dataEnd - viewDuration, dataEnd);
        }
    }
}
