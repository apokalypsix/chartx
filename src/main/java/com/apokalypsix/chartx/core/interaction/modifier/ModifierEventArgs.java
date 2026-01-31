package com.apokalypsix.chartx.core.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ModifierSurface;

/**
 * Base class for modifier event arguments.
 *
 * <p>This class provides common functionality for all modifier events,
 * including handled state tracking and source identification.
 *
 * <p>Event args are designed to be pooled to avoid GC pressure in
 * high-frequency event handling (e.g., mouse move). Subclasses should
 * implement {@link #reset()} to clear state for reuse.
 *
 * @see ModifierMouseEventArgs
 * @see ModifierKeyEventArgs
 */
public abstract class ModifierEventArgs {

    /** The surface that generated this event */
    protected ModifierSurface source;

    /** Whether this event has been handled (consumed) by a modifier */
    protected boolean handled;

    /** Whether this event originated from the master chart (vs. synced slave) */
    protected boolean master = true;

    /**
     * Returns the surface that generated this event.
     *
     * @return the source surface
     */
    public ModifierSurface getSource() {
        return source;
    }

    /**
     * Sets the source surface.
     *
     * @param source the source surface
     */
    public void setSource(ModifierSurface source) {
        this.source = source;
    }

    /**
     * Returns whether this event has been handled (consumed) by a modifier.
     *
     * @return true if handled, false otherwise
     */
    public boolean isHandled() {
        return handled;
    }

    /**
     * Marks this event as handled (consumed).
     *
     * <p>Handled events will not be passed to subsequent modifiers unless
     * they have opted in via {@link ChartModifier#getReceiveHandledEvents()}.
     *
     * @param handled true to mark as handled
     */
    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    /**
     * Returns whether this event originated from the master chart.
     *
     * <p>In multi-chart sync scenarios, the master chart is the one where
     * the user interaction occurred. Slave charts receive propagated events
     * with isMaster=false.
     *
     * @return true if this is the master event, false if propagated from sync
     */
    public boolean isMaster() {
        return master;
    }

    /**
     * Sets whether this is a master or slave event.
     *
     * @param master true for master event, false for slave (synced) event
     */
    public void setMaster(boolean master) {
        this.master = master;
    }

    /**
     * Resets this event args for reuse in a pool.
     *
     * <p>Subclasses should override and call super to reset additional fields.
     */
    public void reset() {
        source = null;
        handled = false;
        master = true;
    }
}
