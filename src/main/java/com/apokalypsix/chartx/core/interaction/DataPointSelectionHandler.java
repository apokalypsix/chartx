package com.apokalypsix.chartx.core.interaction;

import com.apokalypsix.chartx.chart.interaction.ChartSelectionListener;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.chart.data.OHLCBar;
import com.apokalypsix.chartx.chart.data.OhlcData;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles data point (bar/candle) selection and hover interactions.
 *
 * <p>This handler manages:
 * <ul>
 *   <li>Click-to-select on bars</li>
 *   <li>Hover detection and events</li>
 *   <li>Multi-select with Ctrl/Cmd</li>
 *   <li>Range-select with Shift</li>
 *   <li>Selection highlighting state</li>
 * </ul>
 */
public class DataPointSelectionHandler {

    private final DataPointHitTester hitTester;
    private final List<ChartSelectionListener> listeners = new CopyOnWriteArrayList<>();

    // Selection state
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();
    private int lastSelectedIndex = -1;
    private int hoveredIndex = -1;

    // Configuration
    private boolean enabled = true;
    private boolean hoverEnabled = true;
    private boolean multiSelectEnabled = true;
    private boolean rangeSelectEnabled = true;

    /**
     * Creates a data point selection handler.
     */
    public DataPointSelectionHandler() {
        this.hitTester = new DataPointHitTester();
    }

    /**
     * Creates a data point selection handler with a pre-configured hit tester.
     */
    public DataPointSelectionHandler(DataPointHitTester hitTester) {
        this.hitTester = hitTester;
    }

    // ========== Configuration ==========

    /**
     * Sets the OHLC series for selection.
     */
    public void setSeries(OhlcData series) {
        hitTester.setSeries(series);
        clearSelection();
    }

    /**
     * Sets the bar duration for hit testing.
     */
    public void setBarDuration(long barDuration) {
        hitTester.setBarDuration(barDuration);
    }

    /**
     * Sets whether selection handling is enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns true if selection handling is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether hover detection is enabled.
     */
    public void setHoverEnabled(boolean enabled) {
        this.hoverEnabled = enabled;
    }

    /**
     * Sets whether multi-select (Ctrl+click) is enabled.
     */
    public void setMultiSelectEnabled(boolean enabled) {
        this.multiSelectEnabled = enabled;
    }

    /**
     * Sets whether range-select (Shift+click) is enabled.
     */
    public void setRangeSelectEnabled(boolean enabled) {
        this.rangeSelectEnabled = enabled;
    }

    /**
     * Returns the hit tester for additional configuration.
     */
    public DataPointHitTester getHitTester() {
        return hitTester;
    }

    // ========== Selection State ==========

    /**
     * Returns the set of selected bar indices.
     */
    public Set<Integer> getSelectedIndices() {
        return Collections.unmodifiableSet(selectedIndices);
    }

    /**
     * Returns true if the bar at the given index is selected.
     */
    public boolean isSelected(int index) {
        return selectedIndices.contains(index);
    }

    /**
     * Returns the currently hovered bar index, or -1 if none.
     */
    public int getHoveredIndex() {
        return hoveredIndex;
    }

    /**
     * Returns true if any bars are selected.
     */
    public boolean hasSelection() {
        return !selectedIndices.isEmpty();
    }

    /**
     * Returns the number of selected bars.
     */
    public int getSelectionCount() {
        return selectedIndices.size();
    }

    /**
     * Selects a bar by index.
     *
     * @param index the bar index
     * @param addToSelection true to add to existing selection, false to replace
     */
    public void select(int index, boolean addToSelection) {
        if (!addToSelection) {
            selectedIndices.clear();
        }
        if (selectedIndices.add(index)) {
            lastSelectedIndex = index;
            notifySelected(index);
        }
    }

    /**
     * Selects a range of bars.
     *
     * @param startIndex start index (inclusive)
     * @param endIndex end index (inclusive)
     * @param addToSelection true to add to existing selection
     */
    public void selectRange(int startIndex, int endIndex, boolean addToSelection) {
        if (!addToSelection) {
            selectedIndices.clear();
        }

        int min = Math.min(startIndex, endIndex);
        int max = Math.max(startIndex, endIndex);

        for (int i = min; i <= max; i++) {
            if (selectedIndices.add(i)) {
                notifySelected(i);
            }
        }
        lastSelectedIndex = endIndex;
    }

    /**
     * Deselects a bar by index.
     */
    public void deselect(int index) {
        if (selectedIndices.remove(index)) {
            if (selectedIndices.isEmpty()) {
                notifySelectionCleared();
            }
        }
    }

    /**
     * Clears all selection.
     */
    public void clearSelection() {
        if (!selectedIndices.isEmpty()) {
            selectedIndices.clear();
            lastSelectedIndex = -1;
            notifySelectionCleared();
        }
    }

    // ========== Mouse Event Handling ==========

    /**
     * Handles mouse pressed events.
     *
     * @param e the mouse event
     * @param coords the coordinate system
     * @return true if the event was consumed
     */
    public boolean mousePressed(MouseEvent e, CoordinateSystem coords) {
        if (!enabled) {
            return false;
        }

        DataPointHitTester.HitResult hit = hitTester.hitTest(e.getX(), e.getY(), coords);
        if (!hit.isHit()) {
            // Click on empty space - clear selection unless modifier held
            if (!e.isControlDown() && !e.isShiftDown()) {
                clearSelection();
            }
            return false;
        }

        // Create click event
        ChartSelectionListener.DataPointClickEvent event = new ChartSelectionListener.DataPointClickEvent(
                hit.barIndex(),
                hit.toBar(),
                hit.screenX(),
                hit.screenY(),
                hit.region(),
                e.getClickCount(),
                e.isShiftDown(),
                e.isControlDown(),
                e.isAltDown()
        );

        // Notify listeners
        if (e.getClickCount() == 2) {
            notifyDoubleClicked(event);
        } else {
            notifyClicked(event);
        }

        // Handle selection
        if (e.isShiftDown() && rangeSelectEnabled && lastSelectedIndex >= 0) {
            // Range select
            selectRange(lastSelectedIndex, hit.barIndex(), e.isControlDown());
        } else if (e.isControlDown() && multiSelectEnabled) {
            // Toggle selection
            if (isSelected(hit.barIndex())) {
                deselect(hit.barIndex());
            } else {
                select(hit.barIndex(), true);
            }
        } else {
            // Single select
            select(hit.barIndex(), false);
        }

        return true;
    }

    /**
     * Handles mouse moved events for hover detection.
     *
     * @param e the mouse event
     * @param coords the coordinate system
     * @return the cursor to display, or null for default
     */
    public Cursor mouseMoved(MouseEvent e, CoordinateSystem coords) {
        if (!enabled || !hoverEnabled) {
            return null;
        }

        DataPointHitTester.HitResult hit = hitTester.hitTest(e.getX(), e.getY(), coords);

        if (hit.isHit()) {
            if (hoveredIndex != hit.barIndex()) {
                hoveredIndex = hit.barIndex();
                notifyHovered(new ChartSelectionListener.DataPointHoverEvent(
                        hit.barIndex(),
                        hit.toBar(),
                        hit.screenX(),
                        hit.screenY(),
                        hit.region()
                ));
            }
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        } else {
            if (hoveredIndex >= 0) {
                hoveredIndex = -1;
                notifyHoverEnded();
            }
            return null;
        }
    }

    /**
     * Called when mouse exits the chart area.
     */
    public void mouseExited() {
        if (hoveredIndex >= 0) {
            hoveredIndex = -1;
            notifyHoverEnded();
        }
    }

    // ========== Listeners ==========

    /**
     * Adds a selection listener.
     */
    public void addListener(ChartSelectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a selection listener.
     */
    public void removeListener(ChartSelectionListener listener) {
        listeners.remove(listener);
    }

    private void notifyClicked(ChartSelectionListener.DataPointClickEvent event) {
        for (ChartSelectionListener listener : listeners) {
            listener.onDataPointClicked(event);
        }
    }

    private void notifyDoubleClicked(ChartSelectionListener.DataPointClickEvent event) {
        for (ChartSelectionListener listener : listeners) {
            listener.onDataPointDoubleClicked(event);
        }
    }

    private void notifyHovered(ChartSelectionListener.DataPointHoverEvent event) {
        for (ChartSelectionListener listener : listeners) {
            listener.onDataPointHovered(event);
        }
    }

    private void notifyHoverEnded() {
        for (ChartSelectionListener listener : listeners) {
            listener.onHoverEnded();
        }
    }

    private void notifySelected(int index) {
        OHLCBar bar = hitTester.getBar(index);
        if (bar != null) {
            ChartSelectionListener.DataPointSelectionEvent event =
                    new ChartSelectionListener.DataPointSelectionEvent(index, bar, true);
            for (ChartSelectionListener listener : listeners) {
                listener.onDataPointSelected(event);
            }
        }
    }

    private void notifySelectionCleared() {
        for (ChartSelectionListener listener : listeners) {
            listener.onSelectionCleared();
        }
    }
}
