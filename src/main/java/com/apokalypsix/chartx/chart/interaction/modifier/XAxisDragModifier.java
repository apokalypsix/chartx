package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.awt.Cursor;

/**
 * Modifier for dragging on the X-axis to pan/zoom time.
 *
 * <p>This modifier enables interaction specifically on the X-axis area:
 * <ul>
 *   <li>Drag left/right to pan the time axis</li>
 *   <li>Optionally scale by drag distance for zoom effect</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * chart.getModifiers().with(new XAxisDragModifier()
 *     .setDragMode(DragMode.PAN));
 * }</pre>
 */
public class XAxisDragModifier extends ChartModifierBase {

    /** Current drag mode */
    private DragMode dragMode = DragMode.PAN;

    /** Whether currently dragging */
    private boolean isDragging = false;

    /** Last X position during drag */
    private int lastX;

    /** Start X position for zoom calculation */
    private int startX;

    /** Cursor to show when hovering over axis */
    private static final Cursor AXIS_CURSOR = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);

    /** Cursor to show during pan drag */
    private static final Cursor PAN_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

    /**
     * Creates a new XAxisDragModifier with default settings.
     */
    public XAxisDragModifier() {
    }

    // ========== Configuration ==========

    /**
     * Sets the drag mode.
     *
     * @param mode the drag mode
     * @return this modifier for chaining
     */
    public XAxisDragModifier setDragMode(DragMode mode) {
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

        // Only handle if in X-axis area
        if (!args.isInXAxisArea()) {
            return false;
        }

        isDragging = true;
        lastX = args.getScreenX();
        startX = args.getScreenX();
        setCursor(PAN_CURSOR);

        return true;
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        if (!isDragging) {
            return false;
        }

        isDragging = false;
        setCursor(null);

        return true;
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        if (!isDragging) {
            return false;
        }

        int currentX = args.getScreenX();
        int deltaX = currentX - lastX;
        lastX = currentX;

        var viewport = getViewport();
        if (viewport == null || deltaX == 0) {
            return true;
        }

        switch (dragMode) {
            case PAN -> {
                // Pan time axis
                viewport.pan(deltaX, 0);
            }
            case SCALE -> {
                // Scale based on drag distance from start point
                double totalDelta = currentX - startX;
                double factor = 1.0 + (totalDelta / 100.0);
                factor = Math.max(0.1, Math.min(10.0, factor));
                viewport.zoomTime(factor > 1.0 ? 1.01 : 0.99, startX);
            }
        }

        requestRepaint();
        return true;
    }

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        // Show resize cursor when hovering over X-axis
        if (args.isInXAxisArea()) {
            setCursor(AXIS_CURSOR);
            return false; // Don't consume
        }
        return false;
    }

    /**
     * Drag mode options.
     */
    public enum DragMode {
        /** Drag to pan time axis */
        PAN,
        /** Drag to scale/zoom time axis */
        SCALE
    }
}
