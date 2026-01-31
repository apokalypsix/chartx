package com.apokalypsix.chartx.chart.interaction;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierDrawContext;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierKeyEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.awt.Cursor;

/**
 * Abstract base class for chart modifiers providing common functionality.
 *
 * <p>Extends this class to create custom modifiers with boilerplate handling
 * for lifecycle, state management, and surface access.
 *
 * <p>All event methods return false by default (not handled). Override
 * specific methods to implement modifier behavior.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class MyModifier extends ChartModifierBase {
 *     @Override
 *     public boolean onMousePressed(ModifierMouseEventArgs args) {
 *         if (args.isLeftButton()) {
 *             // Handle left click
 *             return true; // Event consumed
 *         }
 *         return false;
 *     }
 * }
 * }</pre>
 */
public abstract class ChartModifierBase implements ChartModifier {

    /** The surface this modifier is attached to */
    protected ModifierSurface surface;

    /** Whether this modifier is enabled */
    protected boolean enabled = true;

    /** Whether to receive events even if already handled */
    protected boolean receiveHandledEvents = false;

    // ========== Lifecycle ==========

    @Override
    public void onAttached(ModifierSurface surface) {
        this.surface = surface;
    }

    @Override
    public void onDetached() {
        this.surface = null;
    }

    @Override
    public boolean isAttached() {
        return surface != null;
    }

    // ========== State ==========

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean getReceiveHandledEvents() {
        return receiveHandledEvents;
    }

    /**
     * Sets whether this modifier should receive events that have already
     * been marked as handled.
     *
     * @param receiveHandledEvents true to receive handled events
     * @return this modifier for chaining
     */
    public ChartModifierBase setReceiveHandledEvents(boolean receiveHandledEvents) {
        this.receiveHandledEvents = receiveHandledEvents;
        return this;
    }

    // ========== Mouse Events (default no-op implementations) ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        return false;
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        return false;
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        return false;
    }

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        return false;
    }

    @Override
    public boolean onMouseEntered(ModifierMouseEventArgs args) {
        return false;
    }

    @Override
    public boolean onMouseExited(ModifierMouseEventArgs args) {
        return false;
    }

    @Override
    public boolean onMouseWheel(ModifierMouseEventArgs args) {
        return false;
    }

    // ========== Keyboard Events (default no-op implementations) ==========

    @Override
    public boolean onKeyPressed(ModifierKeyEventArgs args) {
        return false;
    }

    @Override
    public boolean onKeyReleased(ModifierKeyEventArgs args) {
        return false;
    }

    // ========== Rendering ==========

    @Override
    public void onDraw(ModifierDrawContext ctx) {
        // Default: no rendering
    }

    // ========== Protected Accessors ==========

    /**
     * Returns the surface this modifier is attached to.
     *
     * @return the surface, or null if not attached
     */
    protected ModifierSurface getSurface() {
        return surface;
    }

    /**
     * Returns the viewport from the attached surface.
     *
     * @return the viewport, or null if not attached
     */
    protected Viewport getViewport() {
        return surface != null ? surface.getViewport() : null;
    }

    /**
     * Returns the coordinate system from the attached surface.
     *
     * @return the coordinate system, or null if not attached
     */
    protected CoordinateSystem getCoordinates() {
        return surface != null ? surface.getCoordinateSystem() : null;
    }

    /**
     * Returns the Y-axis manager from the attached surface.
     *
     * @return the axis manager, or null if not attached
     */
    protected YAxisManager getAxisManager() {
        return surface != null ? surface.getAxisManager() : null;
    }

    /**
     * Requests a repaint of the chart.
     */
    protected void requestRepaint() {
        if (surface != null) {
            surface.requestRepaint();
        }
    }

    /**
     * Sets the cursor for the chart component.
     *
     * @param cursor the cursor to set, or null for default
     */
    protected void setCursor(Cursor cursor) {
        if (surface != null) {
            surface.setCursor(cursor);
        }
    }

    /**
     * Scales a coordinate for HiDPI displays.
     *
     * @param coord the coordinate to scale
     * @return the scaled coordinate
     */
    protected int scaleForHiDPI(int coord) {
        return surface != null ? surface.scaleForHiDPI(coord) : coord;
    }
}
