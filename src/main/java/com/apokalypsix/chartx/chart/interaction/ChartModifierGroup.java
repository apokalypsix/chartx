package com.apokalypsix.chartx.chart.interaction;

import com.apokalypsix.chartx.core.interaction.modifier.ModifierDrawContext;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierKeyEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A composite container for multiple chart modifiers.
 *
 * <p>ChartModifierGroup dispatches events to child modifiers in order,
 * respecting the handled state. This allows combining multiple modifiers
 * for complex interaction patterns.
 *
 * <h3>Event Dispatch Rules</h3>
 * <ul>
 *   <li>Events are dispatched to modifiers in the order they were added</li>
 *   <li>If a modifier returns true (handled), subsequent modifiers only
 *       receive the event if they have opted in via {@link #setReceiveHandledEvents(boolean)}</li>
 *   <li>Disabled modifiers do not receive events</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ChartModifierGroup modifiers = new ChartModifierGroup()
 *     .with(new ZoomPanModifier())
 *     .with(new MouseWheelZoomModifier())
 *     .with(new RolloverModifier());
 *
 * chart.setModifiers(modifiers);
 * }</pre>
 */
public class ChartModifierGroup extends ChartModifierBase {

    /** The list of child modifiers */
    private final List<ChartModifier> modifiers = new ArrayList<>();

    /**
     * Creates an empty modifier group.
     */
    public ChartModifierGroup() {
    }

    /**
     * Creates a modifier group with the given modifiers.
     *
     * @param modifiers the initial modifiers
     */
    public ChartModifierGroup(ChartModifier... modifiers) {
        for (ChartModifier modifier : modifiers) {
            add(modifier);
        }
    }

    // ========== Modifier Management ==========

    /**
     * Adds a modifier to this group.
     *
     * @param modifier the modifier to add
     * @return this group for chaining
     */
    public ChartModifierGroup add(ChartModifier modifier) {
        if (modifier != null && !modifiers.contains(modifier)) {
            modifiers.add(modifier);
            if (isAttached()) {
                modifier.onAttached(surface);
            }
        }
        return this;
    }

    /**
     * Adds a modifier to this group. Alias for {@link #add(ChartModifier)}.
     *
     * @param modifier the modifier to add
     * @return this group for chaining
     */
    public ChartModifierGroup with(ChartModifier modifier) {
        return add(modifier);
    }

    /**
     * Removes a modifier from this group.
     *
     * @param modifier the modifier to remove
     * @return this group for chaining
     */
    public ChartModifierGroup remove(ChartModifier modifier) {
        if (modifier != null && modifiers.remove(modifier)) {
            if (modifier.isAttached()) {
                modifier.onDetached();
            }
        }
        return this;
    }

    /**
     * Removes all modifiers from this group.
     *
     * @return this group for chaining
     */
    public ChartModifierGroup clear() {
        for (ChartModifier modifier : modifiers) {
            if (modifier.isAttached()) {
                modifier.onDetached();
            }
        }
        modifiers.clear();
        return this;
    }

    /**
     * Returns an unmodifiable view of the modifiers in this group.
     *
     * @return the list of modifiers
     */
    public List<ChartModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    /**
     * Returns the number of modifiers in this group.
     *
     * @return the modifier count
     */
    public int size() {
        return modifiers.size();
    }

    /**
     * Returns true if this group contains no modifiers.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return modifiers.isEmpty();
    }

    /**
     * Finds the first modifier of the given type in this group.
     *
     * @param type the modifier class to find
     * @param <T> the modifier type
     * @return the modifier, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends ChartModifier> T findModifier(Class<T> type) {
        for (ChartModifier modifier : modifiers) {
            if (type.isInstance(modifier)) {
                return (T) modifier;
            }
        }
        return null;
    }

    // ========== Lifecycle ==========

    @Override
    public void onAttached(ModifierSurface surface) {
        super.onAttached(surface);
        for (ChartModifier modifier : modifiers) {
            modifier.onAttached(surface);
        }
    }

    @Override
    public void onDetached() {
        for (ChartModifier modifier : modifiers) {
            modifier.onDetached();
        }
        super.onDetached();
    }

    // ========== Event Dispatch ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMousePressed);
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMouseReleased);
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMouseDragged);
    }

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMouseMoved);
    }

    @Override
    public boolean onMouseEntered(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMouseEntered);
    }

    @Override
    public boolean onMouseExited(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMouseExited);
    }

    @Override
    public boolean onMouseWheel(ModifierMouseEventArgs args) {
        return dispatchMouseEvent(args, ChartModifier::onMouseWheel);
    }

    @Override
    public boolean onKeyPressed(ModifierKeyEventArgs args) {
        return dispatchKeyEvent(args, ChartModifier::onKeyPressed);
    }

    @Override
    public boolean onKeyReleased(ModifierKeyEventArgs args) {
        return dispatchKeyEvent(args, ChartModifier::onKeyReleased);
    }

    /**
     * Dispatches a mouse event to all enabled modifiers.
     *
     * @param args the event arguments
     * @param handler the event handler method
     * @return true if any modifier handled the event
     */
    private boolean dispatchMouseEvent(ModifierMouseEventArgs args,
                                        MouseEventHandler handler) {
        for (ChartModifier modifier : modifiers) {
            if (!modifier.isEnabled()) {
                continue;
            }
            if (args.isHandled() && !modifier.getReceiveHandledEvents()) {
                continue;
            }

            if (handler.handle(modifier, args)) {
                args.setHandled(true);
            }
        }
        return args.isHandled();
    }

    /**
     * Dispatches a key event to all enabled modifiers.
     *
     * @param args the event arguments
     * @param handler the event handler method
     * @return true if any modifier handled the event
     */
    private boolean dispatchKeyEvent(ModifierKeyEventArgs args,
                                      KeyEventHandler handler) {
        for (ChartModifier modifier : modifiers) {
            if (!modifier.isEnabled()) {
                continue;
            }
            if (args.isHandled() && !modifier.getReceiveHandledEvents()) {
                continue;
            }

            if (handler.handle(modifier, args)) {
                args.setHandled(true);
            }
        }
        return args.isHandled();
    }

    // ========== Rendering ==========

    @Override
    public void onDraw(ModifierDrawContext ctx) {
        for (ChartModifier modifier : modifiers) {
            if (modifier.isEnabled()) {
                modifier.onDraw(ctx);
            }
        }
    }

    // ========== Functional Interfaces ==========

    @FunctionalInterface
    private interface MouseEventHandler {
        boolean handle(ChartModifier modifier, ModifierMouseEventArgs args);
    }

    @FunctionalInterface
    private interface KeyEventHandler {
        boolean handle(ChartModifier modifier, ModifierKeyEventArgs args);
    }
}
