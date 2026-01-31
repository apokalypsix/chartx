package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.interaction.ChartSelectionListener;
import com.apokalypsix.chartx.core.interaction.DataPointHitTester;
import com.apokalypsix.chartx.core.interaction.DataPointSelectionHandler;
import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * Modifier for data point (bar/candle) selection.
 *
 * <p>This modifier wraps {@link DataPointSelectionHandler} to integrate
 * bar/candle selection into the modifier system.
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Click-to-select bars/candles</li>
 *   <li>Hover detection with cursor feedback</li>
 *   <li>Multi-select with Ctrl+click</li>
 *   <li>Range-select with Shift+click</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * DataPointSelectionModifier selectionModifier = new DataPointSelectionModifier();
 * selectionModifier.addSelectionListener(new ChartSelectionListener() {
 *     public void onDataPointClicked(int index, OHLCBar bar) {
 *         System.out.println("Clicked bar at index: " + index);
 *     }
 * });
 * chart.getModifiers().with(selectionModifier);
 * }</pre>
 */
public class DataPointSelectionModifier extends ChartModifierBase {

    /** The underlying selection handler */
    private final DataPointSelectionHandler handler;

    /** Cached mouse event adapter */
    private final MouseEventAdapter mouseEventAdapter = new MouseEventAdapter();

    /** The OHLC data to select from */
    private OhlcData data;

    /** Bar duration for hit testing */
    private long barDuration = 60000; // 1 minute default

    /**
     * Creates a new DataPointSelectionModifier.
     */
    public DataPointSelectionModifier() {
        this.handler = new DataPointSelectionHandler();
    }

    /**
     * Creates a new DataPointSelectionModifier with a custom hit tester.
     *
     * @param hitTester the hit tester to use
     */
    public DataPointSelectionModifier(DataPointHitTester hitTester) {
        this.handler = new DataPointSelectionHandler(hitTester);
    }

    // ========== Configuration ==========

    /**
     * Sets the OHLC data for selection.
     *
     * @param data the data series
     * @return this modifier for chaining
     */
    public DataPointSelectionModifier setData(OhlcData data) {
        this.data = data;
        handler.setSeries(data);
        return this;
    }

    /**
     * Sets the bar duration for hit testing.
     *
     * @param duration duration in milliseconds
     * @return this modifier for chaining
     */
    public DataPointSelectionModifier setBarDuration(long duration) {
        this.barDuration = duration;
        handler.setBarDuration(duration);
        return this;
    }

    /**
     * Enables or disables hover detection.
     *
     * @param enabled true to enable hover
     * @return this modifier for chaining
     */
    public DataPointSelectionModifier setHoverEnabled(boolean enabled) {
        handler.setHoverEnabled(enabled);
        return this;
    }

    /**
     * Enables or disables multi-select (Ctrl+click).
     *
     * @param enabled true to enable multi-select
     * @return this modifier for chaining
     */
    public DataPointSelectionModifier setMultiSelectEnabled(boolean enabled) {
        handler.setMultiSelectEnabled(enabled);
        return this;
    }

    /**
     * Enables or disables range-select (Shift+click).
     *
     * @param enabled true to enable range-select
     * @return this modifier for chaining
     */
    public DataPointSelectionModifier setRangeSelectEnabled(boolean enabled) {
        handler.setRangeSelectEnabled(enabled);
        return this;
    }

    // ========== Selection API ==========

    /**
     * Returns the currently selected indices.
     *
     * @return set of selected indices
     */
    public Set<Integer> getSelectedIndices() {
        return handler.getSelectedIndices();
    }

    /**
     * Returns the currently hovered index, or -1 if none.
     *
     * @return hovered index
     */
    public int getHoveredIndex() {
        return handler.getHoveredIndex();
    }

    /**
     * Returns true if the given index is selected.
     *
     * @param index the index to check
     * @return true if selected
     */
    public boolean isSelected(int index) {
        return handler.isSelected(index);
    }

    /**
     * Selects a bar at the given index.
     *
     * @param index the index to select
     * @param addToSelection true to add to existing selection, false to replace
     */
    public void select(int index, boolean addToSelection) {
        handler.select(index, addToSelection);
        requestRepaint();
    }

    /**
     * Selects a range of bars.
     *
     * @param startIndex start index (inclusive)
     * @param endIndex end index (inclusive)
     * @param addToSelection true to add to existing selection, false to replace
     */
    public void selectRange(int startIndex, int endIndex, boolean addToSelection) {
        handler.selectRange(startIndex, endIndex, addToSelection);
        requestRepaint();
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        handler.clearSelection();
        requestRepaint();
    }

    // ========== Listeners ==========

    /**
     * Adds a selection listener.
     *
     * @param listener the listener to add
     */
    public void addSelectionListener(ChartSelectionListener listener) {
        handler.addListener(listener);
    }

    /**
     * Removes a selection listener.
     *
     * @param listener the listener to remove
     */
    public void removeSelectionListener(ChartSelectionListener listener) {
        handler.removeListener(listener);
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMousePressed(ModifierMouseEventArgs args) {
        if (!args.isLeftButton() || data == null) {
            return false;
        }

        mouseEventAdapter.update(args, MouseEvent.MOUSE_PRESSED);
        boolean handled = handler.mousePressed(mouseEventAdapter, getCoordinates());
        if (handled) {
            requestRepaint();
        }
        return handled;
    }

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        if (data == null) {
            return false;
        }

        mouseEventAdapter.update(args, MouseEvent.MOUSE_MOVED);
        Cursor cursor = handler.mouseMoved(mouseEventAdapter, getCoordinates());

        if (cursor != null) {
            setCursor(cursor);
        }

        return false; // Don't consume move events
    }

    /**
     * Adapter to convert ModifierMouseEventArgs to MouseEvent.
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
}
