package com.apokalypsix.chartx.core.interaction.modifier;

import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;
import com.apokalypsix.chartx.chart.interaction.ModifierSurface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages synchronization of mouse events across multiple chart surfaces.
 *
 * <p>When charts are members of the same MouseEventGroup, mouse events
 * (particularly crosshair/rollover) are propagated to all members. This
 * enables synchronized crosshairs and tooltips across linked charts.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MouseEventGroup syncGroup = new MouseEventGroup("main-sync");
 * syncGroup.add(chart1);
 * syncGroup.add(chart2);
 *
 * // Now crosshairs will sync between chart1 and chart2
 * }</pre>
 *
 * <p>Events propagated to slave charts have {@link ModifierMouseEventArgs#isMaster()}
 * returning false, allowing modifiers to distinguish between local and
 * synced events.
 */
public class MouseEventGroup {

    /** Unique identifier for this group */
    private final String id;

    /** Member surfaces in this group */
    private final List<ModifierSurface> members = new CopyOnWriteArrayList<>();

    /**
     * Creates a new mouse event group with the given ID.
     *
     * @param id unique identifier for this group
     */
    public MouseEventGroup(String id) {
        this.id = id;
    }

    /**
     * Returns the unique identifier for this group.
     *
     * @return the group ID
     */
    public String getId() {
        return id;
    }

    /**
     * Adds a surface to this group.
     *
     * @param surface the surface to add
     */
    public void add(ModifierSurface surface) {
        if (surface != null && !members.contains(surface)) {
            members.add(surface);
            surface.setMouseEventGroup(this);
        }
    }

    /**
     * Removes a surface from this group.
     *
     * @param surface the surface to remove
     */
    public void remove(ModifierSurface surface) {
        if (surface != null && members.remove(surface)) {
            surface.setMouseEventGroup(null);
        }
    }

    /**
     * Removes all surfaces from this group.
     */
    public void clear() {
        for (ModifierSurface member : members) {
            member.setMouseEventGroup(null);
        }
        members.clear();
    }

    /**
     * Returns the number of surfaces in this group.
     *
     * @return the member count
     */
    public int size() {
        return members.size();
    }

    /**
     * Returns an unmodifiable view of the members.
     *
     * @return the list of member surfaces
     */
    public List<ModifierSurface> getMembers() {
        return List.copyOf(members);
    }

    /**
     * Propagates a mouse event from the source to all other members.
     *
     * <p>The event is cloned with isMaster=false and coordinates converted
     * to each target surface's coordinate system.
     *
     * @param source the surface where the event originated
     * @param args the event arguments
     * @param eventType the type of event to propagate
     */
    public void propagateEvent(ModifierSurface source, ModifierMouseEventArgs args,
                               MouseEventType eventType) {
        if (source == null || args == null || !args.isMaster()) {
            return; // Don't re-propagate slave events
        }

        // Get timestamp from master event for coordinate conversion
        long timestamp = args.getTimestamp();

        for (ModifierSurface member : members) {
            if (member == source) {
                continue;
            }

            // Convert timestamp to this member's X coordinate
            var coords = member.getCoordinateSystem();
            if (coords == null) {
                continue;
            }

            int slaveX = (int) coords.xValueToScreenX(timestamp);
            // Keep Y coordinate relative (could also convert price if needed)
            int slaveY = args.getScreenY();

            // Dispatch to member
            dispatchToMember(member, slaveX, slaveY, args, eventType);
        }
    }

    /**
     * Dispatches a synced event to a member surface.
     */
    private void dispatchToMember(ModifierSurface member, int x, int y,
                                   ModifierMouseEventArgs masterArgs,
                                   MouseEventType eventType) {
        // Create slave args
        ModifierMouseEventArgs slaveArgs = new ModifierMouseEventArgs();
        slaveArgs.setSource(member);
        slaveArgs.setScreenX(x);
        slaveArgs.setScreenY(y);
        slaveArgs.setMaster(false);
        slaveArgs.setButton(masterArgs.getButton());
        slaveArgs.setClickCount(masterArgs.getClickCount());
        slaveArgs.setShiftDown(masterArgs.isShiftDown());
        slaveArgs.setCtrlDown(masterArgs.isCtrlDown());
        slaveArgs.setAltDown(masterArgs.isAltDown());
        slaveArgs.setMetaDown(masterArgs.isMetaDown());

        // Dispatch to member's modifier group
        switch (eventType) {
            case MOVED -> member.dispatchMouseMoved(slaveArgs);
            case ENTERED -> member.dispatchMouseEntered(slaveArgs);
            case EXITED -> member.dispatchMouseExited(slaveArgs);
            // Other event types could be added if needed
        }
    }

    /**
     * Types of mouse events that can be propagated.
     */
    public enum MouseEventType {
        MOVED,
        ENTERED,
        EXITED,
        PRESSED,
        RELEASED,
        DRAGGED,
        WHEEL
    }
}
