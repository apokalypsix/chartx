package com.apokalypsix.chartx.core.interaction;

import com.apokalypsix.chartx.chart.interaction.ChartSelectionListener;
import com.apokalypsix.chartx.chart.interaction.SelectionTool;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.chart.data.OhlcData;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles interactive region selection on the chart.
 *
 * <p>Allows users to drag-select regions for:
 * <ul>
 *   <li>Time range selection (horizontal)</li>
 *   <li>Price range selection (vertical)</li>
 *   <li>Rectangular area selection</li>
 *   <li>Zoom to selection</li>
 * </ul>
 *
 * <p>The handler manages the visual feedback rectangle during dragging
 * and fires events when selection is complete.
 */
public class RegionSelectionHandler {

    /**
     * Listener for region selection events.
     */
    public interface RegionSelectedListener {
        /**
         * Called when a region is selected.
         *
         * @param selection the selected region
         */
        void onRegionSelected(SelectedRegion selection);

        /**
         * Called while dragging, with the current selection preview.
         *
         * @param selection the current selection (in progress)
         */
        default void onSelectionInProgress(SelectedRegion selection) {}

        /**
         * Called when selection is cancelled.
         */
        default void onSelectionCancelled() {}
    }

    /**
     * Represents a selected region on the chart.
     */
    public record SelectedRegion(
            // Data coordinates
            long startTime,
            long endTime,
            double topPrice,
            double bottomPrice,

            // Bar indices
            int startIndex,
            int endIndex,

            // Screen coordinates (for rendering)
            Rectangle screenBounds,

            // Metadata
            SelectionTool tool,
            boolean complete
    ) {
        /**
         * Returns the time span in milliseconds.
         */
        public long getTimeSpan() {
            return Math.abs(endTime - startTime);
        }

        /**
         * Returns the price range.
         */
        public double getPriceRange() {
            return Math.abs(topPrice - bottomPrice);
        }

        /**
         * Returns the number of bars in the selection.
         */
        public int getBarCount() {
            return Math.abs(endIndex - startIndex) + 1;
        }

        /**
         * Returns the normalized start time (always the earlier time).
         */
        public long getNormalizedStartTime() {
            return Math.min(startTime, endTime);
        }

        /**
         * Returns the normalized end time (always the later time).
         */
        public long getNormalizedEndTime() {
            return Math.max(startTime, endTime);
        }

        /**
         * Returns the normalized top price (always the higher price).
         */
        public double getNormalizedTopPrice() {
            return Math.max(topPrice, bottomPrice);
        }

        /**
         * Returns the normalized bottom price (always the lower price).
         */
        public double getNormalizedBottomPrice() {
            return Math.min(topPrice, bottomPrice);
        }

        /**
         * Creates a ChartSelectionListener event from this selection.
         */
        public ChartSelectionListener.RegionSelectionEvent toEvent() {
            return new ChartSelectionListener.RegionSelectionEvent(
                    getNormalizedStartTime(),
                    getNormalizedEndTime(),
                    getNormalizedTopPrice(),
                    getNormalizedBottomPrice(),
                    Math.min(startIndex, endIndex),
                    Math.max(startIndex, endIndex),
                    getBarCount()
            );
        }
    }

    // Configuration
    private SelectionTool activeTool = SelectionTool.NONE;
    private OhlcData series;
    private int viewportWidth;
    private int viewportHeight;

    // Drag state
    private boolean isDragging = false;
    private double dragStartX;
    private double dragStartY;
    private double dragEndX;
    private double dragEndY;
    private long dragStartTime;
    private double dragStartPrice;
    private long dragEndTime;
    private double dragEndPrice;

    // Listeners
    private final List<RegionSelectedListener> listeners = new CopyOnWriteArrayList<>();

    // Selection persistence
    private SelectedRegion lastSelection;
    private boolean persistSelection = false;

    /**
     * Creates a region selection handler.
     */
    public RegionSelectionHandler() {
    }

    // ========== Configuration ==========

    /**
     * Sets the active selection tool.
     *
     * @param tool the selection tool
     */
    public void setActiveTool(SelectionTool tool) {
        if (this.activeTool != tool) {
            // Cancel any in-progress selection
            if (isDragging) {
                cancelSelection();
            }
            this.activeTool = tool;
        }
    }

    /**
     * Returns the active selection tool.
     */
    public SelectionTool getActiveTool() {
        return activeTool;
    }

    /**
     * Returns true if a selection tool is active.
     */
    public boolean isToolActive() {
        return activeTool != SelectionTool.NONE;
    }

    /**
     * Sets the OHLC series for bar index calculations.
     */
    public void setSeries(OhlcData series) {
        this.series = series;
    }

    /**
     * Sets the viewport dimensions for screen coordinate calculations.
     */
    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    /**
     * Sets whether the selection should persist after completion.
     * If false, the selection rectangle disappears after the callback.
     */
    public void setPersistSelection(boolean persist) {
        this.persistSelection = persist;
    }

    // ========== Mouse Handling ==========

    /**
     * Handles mouse pressed events.
     *
     * @param e the mouse event
     * @param coords the coordinate system
     * @return true if the event was consumed
     */
    public boolean mousePressed(MouseEvent e, CoordinateSystem coords) {
        if (activeTool == SelectionTool.NONE) {
            return false;
        }

        if (activeTool.isClickSelection()) {
            // Handle click selection (crosshair)
            handleClickSelection(e.getX(), e.getY(), coords);
            return true;
        }

        if (activeTool.isDragSelection()) {
            // Start drag selection
            isDragging = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
            dragEndX = e.getX();
            dragEndY = e.getY();
            dragStartTime = coords.screenXToXValue(dragStartX);
            dragStartPrice = coords.screenYToYValue(dragStartY);
            dragEndTime = dragStartTime;
            dragEndPrice = dragStartPrice;
            return true;
        }

        return false;
    }

    /**
     * Handles mouse dragged events.
     *
     * @param e the mouse event
     * @param coords the coordinate system
     * @return true if the event was consumed
     */
    public boolean mouseDragged(MouseEvent e, CoordinateSystem coords) {
        if (!isDragging || activeTool == SelectionTool.NONE) {
            return false;
        }

        // Update drag end position
        dragEndX = e.getX();
        dragEndY = e.getY();
        dragEndTime = coords.screenXToXValue(dragEndX);
        dragEndPrice = coords.screenYToYValue(dragEndY);

        // Apply tool constraints
        applyToolConstraints();

        // Notify listeners of in-progress selection
        SelectedRegion preview = createSelection(coords, false);
        for (RegionSelectedListener listener : listeners) {
            listener.onSelectionInProgress(preview);
        }

        return true;
    }

    /**
     * Handles mouse released events.
     *
     * @param e the mouse event
     * @param coords the coordinate system
     * @return true if the event was consumed
     */
    public boolean mouseReleased(MouseEvent e, CoordinateSystem coords) {
        if (!isDragging || activeTool == SelectionTool.NONE) {
            return false;
        }

        isDragging = false;

        // Update final position
        dragEndX = e.getX();
        dragEndY = e.getY();
        dragEndTime = coords.screenXToXValue(dragEndX);
        dragEndPrice = coords.screenYToYValue(dragEndY);

        // Apply tool constraints
        applyToolConstraints();

        // Check if selection is large enough
        double dx = Math.abs(dragEndX - dragStartX);
        double dy = Math.abs(dragEndY - dragStartY);

        if (dx < 5 && dy < 5) {
            // Too small, treat as cancelled
            cancelSelection();
            return true;
        }

        // Create final selection
        SelectedRegion selection = createSelection(coords, true);
        lastSelection = selection;

        // Notify listeners
        for (RegionSelectedListener listener : listeners) {
            listener.onRegionSelected(selection);
        }

        // Clear selection unless persisting
        if (!persistSelection) {
            lastSelection = null;
        }

        return true;
    }

    /**
     * Returns the cursor for the current state.
     *
     * @param e the mouse event
     * @return the cursor, or null for default
     */
    public Cursor mouseMoved(MouseEvent e) {
        if (activeTool == SelectionTool.NONE) {
            return null;
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Handles key pressed events.
     *
     * @param keyCode the key code
     * @return true if the event was consumed
     */
    public boolean keyPressed(int keyCode) {
        // Escape cancels selection
        if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
            if (isDragging) {
                cancelSelection();
                return true;
            }
            if (lastSelection != null && persistSelection) {
                clearSelection();
                return true;
            }
        }
        return false;
    }

    // ========== Selection State ==========

    /**
     * Returns true if currently dragging a selection.
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Returns the current selection (may be in-progress).
     */
    public SelectedRegion getCurrentSelection(CoordinateSystem coords) {
        if (isDragging) {
            return createSelection(coords, false);
        }
        return lastSelection;
    }

    /**
     * Returns the last completed selection.
     */
    public SelectedRegion getLastSelection() {
        return lastSelection;
    }

    /**
     * Clears the last selection.
     */
    public void clearSelection() {
        lastSelection = null;
        for (RegionSelectedListener listener : listeners) {
            listener.onSelectionCancelled();
        }
    }

    /**
     * Cancels the current in-progress selection.
     */
    public void cancelSelection() {
        isDragging = false;
        for (RegionSelectedListener listener : listeners) {
            listener.onSelectionCancelled();
        }
    }

    // ========== Listeners ==========

    /**
     * Adds a region selection listener.
     */
    public void addListener(RegionSelectedListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a region selection listener.
     */
    public void removeListener(RegionSelectedListener listener) {
        listeners.remove(listener);
    }

    // ========== Internal ==========

    private void applyToolConstraints() {
        switch (activeTool) {
            case TIME_RANGE:
                // Constrain to full vertical range
                dragStartY = 0;
                dragEndY = viewportHeight;
                break;

            case PRICE_RANGE:
                // Constrain to full horizontal range
                dragStartX = 0;
                dragEndX = viewportWidth;
                break;

            default:
                // No constraints for RECTANGLE and ZOOM_TO_SELECTION
                break;
        }
    }

    private void handleClickSelection(double x, double y, CoordinateSystem coords) {
        long time = coords.screenXToXValue(x);
        double price = coords.screenYToYValue(y);

        // For click selection, create a point selection (minimal bounds)
        int index = series != null ? series.indexAtOrBefore(time) : -1;

        SelectedRegion selection = new SelectedRegion(
                time, time,
                price, price,
                index, index,
                new Rectangle((int) x - 1, (int) y - 1, 3, 3),
                activeTool,
                true
        );

        for (RegionSelectedListener listener : listeners) {
            listener.onRegionSelected(selection);
        }
    }

    private SelectedRegion createSelection(CoordinateSystem coords, boolean complete) {
        // Calculate bar indices
        int startIndex = -1;
        int endIndex = -1;
        if (series != null && !series.isEmpty()) {
            startIndex = series.indexAtOrAfter(Math.min(dragStartTime, dragEndTime));
            endIndex = series.indexAtOrBefore(Math.max(dragStartTime, dragEndTime));
            if (startIndex < 0) startIndex = 0;
            if (endIndex < 0) endIndex = series.size() - 1;
        }

        // Calculate screen bounds
        Rectangle bounds = new Rectangle(
                (int) Math.min(dragStartX, dragEndX),
                (int) Math.min(dragStartY, dragEndY),
                (int) Math.abs(dragEndX - dragStartX),
                (int) Math.abs(dragEndY - dragStartY)
        );

        return new SelectedRegion(
                dragStartTime,
                dragEndTime,
                dragStartPrice,
                dragEndPrice,
                startIndex,
                endIndex,
                bounds,
                activeTool,
                complete
        );
    }
}
