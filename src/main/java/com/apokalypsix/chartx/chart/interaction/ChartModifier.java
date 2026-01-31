package com.apokalypsix.chartx.chart.interaction;

import com.apokalypsix.chartx.core.interaction.modifier.ModifierDrawContext;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierKeyEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

/**
 * Core interface for chart interaction modifiers.
 *
 * <p>ChartModifier provides a composable, pluggable system for handling user interactions.
 * Modifiers can handle mouse events, keyboard events, and render visual feedback like
 * crosshairs, selection rectangles, and tooltips.
 *
 * <p>Inspired by SciChart's ChartModifier architecture, this system allows multiple
 * modifiers to be combined for complex interaction patterns while maintaining clean
 * separation of concerns.
 *
 * <h3>Event Handling</h3>
 * <ul>
 *   <li>Event methods return {@code true} if the event was handled (consumed)</li>
 *   <li>Handled events are not passed to subsequent modifiers unless they opt-in via
 *       {@link #getReceiveHandledEvents()}</li>
 *   <li>Mouse events use pooled {@link ModifierMouseEventArgs} to avoid GC pressure</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>{@link #onAttached(ModifierSurface)} - Called when added to a chart</li>
 *   <li>{@link #onDetached()} - Called when removed from a chart</li>
 * </ul>
 *
 * @see ChartModifierBase
 * @see ChartModifierGroup
 */
public interface ChartModifier {

    // ========== Lifecycle ==========

    /**
     * Called when this modifier is attached to a chart surface.
     *
     * @param surface the chart surface providing access to chart components
     */
    void onAttached(ModifierSurface surface);

    /**
     * Called when this modifier is detached from a chart surface.
     */
    void onDetached();

    /**
     * Returns whether this modifier is currently attached to a chart surface.
     *
     * @return true if attached, false otherwise
     */
    boolean isAttached();

    // ========== State ==========

    /**
     * Returns whether this modifier is enabled and will receive events.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Enables or disables this modifier.
     *
     * <p>Disabled modifiers do not receive any events.
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Returns whether this modifier should receive events that have already
     * been marked as handled by a previous modifier.
     *
     * <p>Most modifiers should return false (the default). Set to true for
     * modifiers that need to observe all events, like analytics or logging.
     *
     * @return true to receive handled events, false otherwise
     */
    boolean getReceiveHandledEvents();

    // ========== Mouse Events ==========

    /**
     * Called when a mouse button is pressed.
     *
     * @param args the mouse event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onMousePressed(ModifierMouseEventArgs args);

    /**
     * Called when a mouse button is released.
     *
     * @param args the mouse event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onMouseReleased(ModifierMouseEventArgs args);

    /**
     * Called when the mouse is dragged (moved with button held).
     *
     * @param args the mouse event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onMouseDragged(ModifierMouseEventArgs args);

    /**
     * Called when the mouse is moved (without button held).
     *
     * @param args the mouse event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onMouseMoved(ModifierMouseEventArgs args);

    /**
     * Called when the mouse enters the chart area.
     *
     * @param args the mouse event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onMouseEntered(ModifierMouseEventArgs args);

    /**
     * Called when the mouse exits the chart area.
     *
     * @param args the mouse event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onMouseExited(ModifierMouseEventArgs args);

    /**
     * Called when the mouse wheel is rotated.
     *
     * @param args the mouse event arguments (includes wheel rotation)
     * @return true if the event was handled (consumed)
     */
    boolean onMouseWheel(ModifierMouseEventArgs args);

    // ========== Keyboard Events ==========

    /**
     * Called when a key is pressed.
     *
     * @param args the keyboard event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onKeyPressed(ModifierKeyEventArgs args);

    /**
     * Called when a key is released.
     *
     * @param args the keyboard event arguments
     * @return true if the event was handled (consumed)
     */
    boolean onKeyReleased(ModifierKeyEventArgs args);

    // ========== Rendering ==========

    /**
     * Called to render modifier-specific visuals (crosshairs, selection rectangles, etc.).
     *
     * <p>This is called after the main chart layers have rendered, allowing modifiers
     * to draw overlays on top of the chart content.
     *
     * @param ctx the rendering context
     */
    void onDraw(ModifierDrawContext ctx);
}
