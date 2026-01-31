package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.core.interaction.DrawingInteractionHandler;
import com.apokalypsix.chartx.chart.interaction.DrawingTool;
import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierKeyEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;
import com.apokalypsix.chartx.chart.overlay.Drawing;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Modifier for drawing tool functionality.
 *
 * <p>This modifier wraps the {@link DrawingInteractionHandler} to integrate
 * drawing tools into the modifier system. It handles drawing creation,
 * selection, editing, and deletion.
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Drawing creation (click to place anchor points)</li>
 *   <li>Drawing selection (click to select in SELECT mode)</li>
 *   <li>Drawing editing (drag handles to move anchor points)</li>
 *   <li>Drawing deletion (Delete key)</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * DrawingModifier drawingModifier = new DrawingModifier();
 * chart.getModifiers().with(drawingModifier);
 *
 * // Activate drawing tool
 * drawingModifier.setActiveTool(DrawingTool.TREND_LINE);
 * }</pre>
 */
public class DrawingModifier extends ChartModifierBase {

    /** The underlying drawing handler (lazily resolved from surface) */
    private DrawingInteractionHandler handler;

    /** Cached mouse event for adapter pattern */
    private final MouseEventAdapter mouseEventAdapter = new MouseEventAdapter();

    /** Cached key event for adapter pattern */
    private final KeyEventAdapter keyEventAdapter = new KeyEventAdapter();

    /**
     * Creates a new DrawingModifier.
     */
    public DrawingModifier() {
    }

    // ========== Drawing Tool Management ==========

    /**
     * Sets the active drawing tool.
     *
     * @param tool the tool to activate
     * @return this modifier for chaining
     */
    public DrawingModifier setActiveTool(DrawingTool tool) {
        var h = getHandler();
        if (h != null) {
            h.setActiveTool(tool);
        }
        return this;
    }

    /**
     * Returns the active drawing tool.
     *
     * @return the active tool, or NONE if no tool is active
     */
    public DrawingTool getActiveTool() {
        var h = getHandler();
        return h != null ? h.getActiveTool() : DrawingTool.NONE;
    }

    /**
     * Returns true if a drawing tool is active (not NONE or SELECT).
     *
     * @return true if actively drawing
     */
    public boolean isCreationToolActive() {
        var tool = getActiveTool();
        return tool != DrawingTool.NONE && tool != DrawingTool.SELECT && tool.isCreationTool();
    }

    /**
     * Returns the currently selected drawing.
     *
     * @return the selected drawing, or null if none selected
     */
    public Drawing getSelectedDrawing() {
        var h = getHandler();
        return h != null ? h.getSelectedDrawing() : null;
    }

    /**
     * Adds a drawing listener to receive creation/modification/deletion events.
     *
     * @param listener the listener to add
     */
    public void addDrawingListener(DrawingInteractionHandler.DrawingListener listener) {
        var h = getHandler();
        if (h != null) {
            h.addDrawingListener(listener);
        }
    }

    /**
     * Removes a drawing listener.
     *
     * @param listener the listener to remove
     */
    public void removeDrawingListener(DrawingInteractionHandler.DrawingListener listener) {
        var h = getHandler();
        if (h != null) {
            h.removeDrawingListener(listener);
        }
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        var h = getHandler();
        if (h == null || !h.isToolActive()) {
            return false;
        }

        mouseEventAdapter.update(args, MouseEvent.MOUSE_PRESSED);
        return h.mousePressed(mouseEventAdapter, getCoordinates());
    }

    @Override
    public boolean onMouseReleased(ModifierMouseEventArgs args) {
        var h = getHandler();
        if (h == null || !h.isToolActive()) {
            return false;
        }

        mouseEventAdapter.update(args, MouseEvent.MOUSE_RELEASED);
        return h.mouseReleased(mouseEventAdapter, getCoordinates());
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        var h = getHandler();
        if (h == null || !h.isToolActive()) {
            return false;
        }

        mouseEventAdapter.update(args, MouseEvent.MOUSE_DRAGGED);
        boolean handled = h.mouseDragged(mouseEventAdapter, getCoordinates());
        if (handled) {
            requestRepaint();
        }
        return handled;
    }

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        var h = getHandler();
        if (h == null || !h.isToolActive()) {
            return false;
        }

        mouseEventAdapter.update(args, MouseEvent.MOUSE_MOVED);
        Cursor cursor = h.mouseMoved(mouseEventAdapter, getCoordinates());

        if (cursor != null) {
            setCursor(cursor);
        } else if (isCreationToolActive()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(null);
        }

        requestRepaint();
        return false; // Don't consume move events
    }

    // ========== Keyboard Events ==========

    @Override
    public boolean onKeyPressed(ModifierKeyEventArgs args) {
        var h = getHandler();
        if (h == null) {
            return false;
        }

        keyEventAdapter.update(args, KeyEvent.KEY_PRESSED);
        boolean handled = h.keyPressed(keyEventAdapter);
        if (handled) {
            requestRepaint();
        }
        return handled;
    }

    // ========== Handler Resolution ==========

    /**
     * Gets or lazily resolves the drawing handler from the surface.
     */
    private DrawingInteractionHandler getHandler() {
        if (handler == null && surface != null) {
            handler = surface.getDrawingHandler();
        }
        return handler;
    }

    @Override
    public void onDetached() {
        handler = null;
        super.onDetached();
    }

    // ========== Event Adapters ==========

    /**
     * Adapter to convert ModifierMouseEventArgs to MouseEvent.
     *
     * <p>This is a lightweight adapter that creates a minimal MouseEvent
     * compatible with DrawingInteractionHandler without actually creating
     * a full AWT event.
     */
    private static class MouseEventAdapter extends MouseEvent {
        private static final java.awt.Component DUMMY_SOURCE = new java.awt.Canvas();

        private int x, y, button, clickCount, id;
        private boolean shiftDown, ctrlDown, altDown, metaDown;

        MouseEventAdapter() {
            super(DUMMY_SOURCE, 0, 0, 0, 0, 0, 0, false);
        }

        void update(ModifierMouseEventArgs args, int eventId) {
            this.id = eventId;
            this.x = args.getScreenX();
            this.y = args.getScreenY();
            this.button = args.getButton();
            this.clickCount = args.getClickCount();
            this.shiftDown = args.isShiftDown();
            this.ctrlDown = args.isCtrlDown();
            this.altDown = args.isAltDown();
            this.metaDown = args.isMetaDown();
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getButton() {
            return button;
        }

        @Override
        public int getClickCount() {
            return clickCount;
        }

        @Override
        public boolean isShiftDown() {
            return shiftDown;
        }

        @Override
        public boolean isControlDown() {
            return ctrlDown;
        }

        @Override
        public boolean isAltDown() {
            return altDown;
        }

        @Override
        public boolean isMetaDown() {
            return metaDown;
        }
    }

    /**
     * Adapter to convert ModifierKeyEventArgs to KeyEvent.
     */
    private static class KeyEventAdapter extends KeyEvent {
        private static final java.awt.Component DUMMY_SOURCE = new java.awt.Canvas();

        private int keyCode, id;
        private char keyChar;
        private boolean shiftDown, ctrlDown, altDown, metaDown;

        KeyEventAdapter() {
            super(DUMMY_SOURCE, 0, 0, 0, 0, '\0');
        }

        void update(ModifierKeyEventArgs args, int eventId) {
            this.id = eventId;
            this.keyCode = args.getKeyCode();
            this.keyChar = args.getKeyChar();
            this.shiftDown = args.isShiftDown();
            this.ctrlDown = args.isCtrlDown();
            this.altDown = args.isAltDown();
            this.metaDown = args.isMetaDown();
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public int getKeyCode() {
            return keyCode;
        }

        @Override
        public char getKeyChar() {
            return keyChar;
        }

        @Override
        public boolean isShiftDown() {
            return shiftDown;
        }

        @Override
        public boolean isControlDown() {
            return ctrlDown;
        }

        @Override
        public boolean isAltDown() {
            return altDown;
        }

        @Override
        public boolean isMetaDown() {
            return metaDown;
        }
    }
}
