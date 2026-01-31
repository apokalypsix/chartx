package com.apokalypsix.chartx.chart.interaction;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.model.CrosshairLayerV2;
import com.apokalypsix.chartx.core.render.model.DrawingLayerV2;
import com.apokalypsix.chartx.core.render.model.TextOverlay;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.core.interaction.modifier.MouseEventGroup;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;
import com.apokalypsix.chartx.core.interaction.DrawingInteractionHandler;

import java.awt.Cursor;

/**
 * Interface for accessing chart components from within modifiers.
 *
 * <p>This interface provides modifiers with controlled access to the chart's
 * coordinate system, viewport, data, and rendering capabilities without
 * exposing the full chart implementation.
 *
 * <p>Implemented by chart components (e.g., AbstractChartComponent) to serve
 * as the connection point between modifiers and the chart infrastructure.
 */
public interface ModifierSurface {

    // ========== Coordinate System ==========

    /**
     * Returns the viewport for the chart.
     *
     * <p>The viewport provides access to the visible time/price range
     * and pan/zoom operations.
     *
     * @return the viewport
     */
    Viewport getViewport();

    /**
     * Returns the coordinate system for converting between data and screen coordinates.
     *
     * @return the coordinate system
     */
    CoordinateSystem getCoordinateSystem();

    /**
     * Returns the Y-axis manager for multi-axis support.
     *
     * @return the Y-axis manager
     */
    YAxisManager getAxisManager();

    // ========== Data Access ==========

    /**
     * Returns the primary data series for the chart.
     *
     * <p>This is typically the main OHLC data for price charts.
     *
     * @return the primary data, or null if not set
     */
    Data<?> getPrimaryData();

    // ========== HiDPI Support ==========

    /**
     * Scales a coordinate for HiDPI displays.
     *
     * @param coord the coordinate to scale
     * @return the scaled coordinate
     */
    int scaleForHiDPI(int coord);

    /**
     * Returns the HiDPI scale factor.
     *
     * @return the scale factor (1.0 for standard displays, 2.0 for Retina, etc.)
     */
    double getScaleFactor();

    // ========== Hit Detection ==========

    /**
     * Finds which Y-axis (if any) is at the given position.
     *
     * @param x the X coordinate (HiDPI scaled)
     * @param y the Y coordinate (HiDPI scaled)
     * @return the axis ID, or null if not over an axis
     */
    String findAxisAtPosition(int x, int y);

    // ========== Rendering ==========

    /**
     * Requests a repaint of the chart.
     */
    void requestRepaint();

    /**
     * Sets the cursor for the chart component.
     *
     * @param cursor the cursor to set, or null for default
     */
    void setCursor(Cursor cursor);

    // ========== Multi-Chart Sync ==========

    /**
     * Returns a unique identifier for this surface.
     *
     * <p>Used for multi-chart synchronization to identify the source
     * of events.
     *
     * @return the surface ID
     */
    String getSurfaceId();

    /**
     * Returns the mouse event group for multi-chart synchronization.
     *
     * @return the event group, or null if not part of a group
     */
    MouseEventGroup getMouseEventGroup();

    /**
     * Sets the mouse event group for multi-chart synchronization.
     *
     * @param group the event group, or null to remove from group
     */
    void setMouseEventGroup(MouseEventGroup group);

    // ========== Chart Dimensions ==========

    /**
     * Returns the chart area left boundary (after axis insets).
     *
     * @return the left X coordinate in pixels
     */
    default int getChartLeft() {
        return getViewport().getLeftInset();
    }

    /**
     * Returns the chart area right boundary (before axis insets).
     *
     * @return the right X coordinate in pixels
     */
    default int getChartRight() {
        return getViewport().getWidth() - getViewport().getRightInset();
    }

    /**
     * Returns the chart area top boundary (after top inset).
     *
     * @return the top Y coordinate in pixels
     */
    default int getChartTop() {
        return getViewport().getTopInset();
    }

    /**
     * Returns the chart area bottom boundary (before bottom inset).
     *
     * @return the bottom Y coordinate in pixels
     */
    default int getChartBottom() {
        return getViewport().getHeight() - getViewport().getBottomInset();
    }

    /**
     * Returns the width of the chart area (excluding axis insets).
     *
     * @return the width in pixels
     */
    default int getChartWidth() {
        return getChartRight() - getChartLeft();
    }

    /**
     * Returns the height of the chart area (excluding insets).
     *
     * @return the height in pixels
     */
    default int getChartHeight() {
        return getChartBottom() - getChartTop();
    }

    // ========== Crosshair Access ==========

    /**
     * Returns the crosshair layer for controlling crosshair rendering.
     *
     * @return the crosshair layer, or null if not available
     */
    CrosshairLayerV2 getCrosshairLayer();

    /**
     * Returns the text overlay for controlling axis labels and tooltips.
     *
     * @return the text overlay, or null if not available
     */
    TextOverlay getTextOverlay();

    // ========== Drawing Access ==========

    /**
     * Returns the drawing layer for managing drawings.
     *
     * @return the drawing layer, or null if not available
     */
    DrawingLayerV2 getDrawingLayer();

    /**
     * Returns the drawing interaction handler.
     *
     * @return the handler, or null if not available
     */
    DrawingInteractionHandler getDrawingHandler();

    // ========== Event Dispatch ==========

    /**
     * Dispatches a mouse moved event to the modifier group.
     *
     * <p>Used by MouseEventGroup for multi-chart sync.
     *
     * @param args the event arguments
     */
    void dispatchMouseMoved(ModifierMouseEventArgs args);

    /**
     * Dispatches a mouse entered event to the modifier group.
     *
     * <p>Used by MouseEventGroup for multi-chart sync.
     *
     * @param args the event arguments
     */
    void dispatchMouseEntered(ModifierMouseEventArgs args);

    /**
     * Dispatches a mouse exited event to the modifier group.
     *
     * <p>Used by MouseEventGroup for multi-chart sync.
     *
     * @param args the event arguments
     */
    void dispatchMouseExited(ModifierMouseEventArgs args);

    // ========== Overlay Updates ==========

    /**
     * Updates the info overlays (OHLC, indicators) to display values at the given bar index.
     *
     * <p>Pass -1 to show the latest bar (default behavior when mouse is not over chart).
     *
     * @param index the bar index, or -1 for latest
     */
    void setOverlayDisplayIndex(int index);
}
