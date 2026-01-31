package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.awt.Cursor;

/**
 * Modifier for dragging on the Y-axis to pan/zoom price.
 *
 * <p>This modifier enables interaction specifically on Y-axis areas:
 * <ul>
 *   <li>Drag up/down to pan the price axis</li>
 *   <li>Supports multiple Y-axes (each axis can be dragged independently)</li>
 *   <li>Optionally disables auto-scale when manually adjusted</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * chart.getModifiers().with(new YAxisDragModifier()
 *     .setDragMode(DragMode.PAN));
 * }</pre>
 */
public class YAxisDragModifier extends ChartModifierBase {

    /** Current drag mode */
    private DragMode dragMode = DragMode.PAN;

    /** Whether currently dragging */
    private boolean isDragging = false;

    /** The axis being dragged */
    private String activeAxisId;

    /** Last Y position during drag */
    private int lastY;

    /** Start Y position for zoom calculation */
    private int startY;

    /** Cursor to show when hovering over axis */
    private static final Cursor AXIS_CURSOR = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);

    /** Cursor to show during pan drag */
    private static final Cursor PAN_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

    /**
     * Creates a new YAxisDragModifier with default settings.
     */
    public YAxisDragModifier() {
    }

    // ========== Configuration ==========

    /**
     * Sets the drag mode.
     *
     * @param mode the drag mode
     * @return this modifier for chaining
     */
    public YAxisDragModifier setDragMode(DragMode mode) {
        this.dragMode = mode;
        return this;
    }

    /**
     * Returns the current drag mode.
     *
     * @return the drag mode
     */
    public DragMode getDragMode() {
        return dragMode;
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        if (!args.isLeftButton()) {
            return false;
        }

        // Check if over a Y-axis
        String axisId = args.getHitAxisId();
        if (axisId == null) {
            return false;
        }

        isDragging = true;
        activeAxisId = axisId;
        lastY = args.getScreenY();
        startY = args.getScreenY();
        setCursor(PAN_CURSOR);

        // Disable auto-scale for this axis
        var axisManager = getAxisManager();
        if (axisManager != null) {
            YAxis axis = axisManager.getAxis(axisId);
            if (axis != null && axis.isAutoScale()) {
                axis.setAutoScale(false);
            }
        }

        return true;
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        if (!isDragging) {
            return false;
        }

        isDragging = false;
        activeAxisId = null;
        setCursor(null);

        return true;
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        if (!isDragging || activeAxisId == null) {
            return false;
        }

        int currentY = args.getScreenY();
        int deltaY = currentY - lastY;
        lastY = currentY;

        var axisManager = getAxisManager();
        if (axisManager == null || deltaY == 0) {
            return true;
        }

        YAxis axis = axisManager.getAxis(activeAxisId);
        if (axis == null) {
            return true;
        }

        switch (dragMode) {
            case PAN -> panAxis(axis, deltaY);
            case SCALE -> scaleAxis(axis, currentY);
        }

        requestRepaint();
        return true;
    }

    /**
     * Pans the axis by the given delta.
     */
    private void panAxis(YAxis axis, int deltaY) {
        // Convert pixel delta to value delta
        int chartTop = surface.getChartTop();
        int chartBottom = surface.getChartBottom();
        int chartHeight = chartBottom - chartTop;

        if (chartHeight <= 0) return;

        double valueRange = axis.getMaxValue() - axis.getMinValue();
        double deltaValue = (deltaY / (double) chartHeight) * valueRange;

        // Y is inverted, so positive deltaY should decrease values
        axis.setValueRange(
                axis.getMinValue() + deltaValue,
                axis.getMaxValue() + deltaValue
        );

        // Sync with viewport if default axis
        if (YAxis.DEFAULT_AXIS_ID.equals(axis.getId())) {
            var viewport = getViewport();
            if (viewport != null) {
                viewport.setPriceRange(axis.getMinValue(), axis.getMaxValue());
            }
        }

        // Invalidate coordinate cache
        var coords = getCoordinates();
        if (coords instanceof com.apokalypsix.chartx.core.coordinate.CartesianCoordinateSystem ccs) {
            ccs.invalidateCache();
        }
    }

    /**
     * Scales the axis based on drag position.
     */
    private void scaleAxis(YAxis axis, int currentY) {
        int totalDelta = currentY - startY;
        double factor = 1.0 + (totalDelta / 200.0);
        factor = Math.max(0.1, Math.min(10.0, factor));

        // Zoom from center
        double center = (axis.getMinValue() + axis.getMaxValue()) / 2.0;
        double halfRange = (axis.getMaxValue() - axis.getMinValue()) / 2.0;
        double newHalfRange = halfRange * (factor > 1.0 ? 1.01 : 0.99);

        axis.setValueRange(center - newHalfRange, center + newHalfRange);

        // Sync with viewport if default axis
        if (YAxis.DEFAULT_AXIS_ID.equals(axis.getId())) {
            var viewport = getViewport();
            if (viewport != null) {
                viewport.setPriceRange(axis.getMinValue(), axis.getMaxValue());
            }
        }

        // Invalidate coordinate cache
        var coords = getCoordinates();
        if (coords instanceof com.apokalypsix.chartx.core.coordinate.CartesianCoordinateSystem ccs) {
            ccs.invalidateCache();
        }
    }

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        // Show resize cursor when hovering over Y-axis
        String axisId = args.getHitAxisId();
        if (axisId != null) {
            setCursor(AXIS_CURSOR);
            return false; // Don't consume
        }
        return false;
    }

    /**
     * Drag mode options.
     */
    public enum DragMode {
        /** Drag to pan price axis */
        PAN,
        /** Drag to scale/zoom price axis */
        SCALE
    }
}
