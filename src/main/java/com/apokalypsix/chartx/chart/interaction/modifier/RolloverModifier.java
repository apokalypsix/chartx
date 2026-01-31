package com.apokalypsix.chartx.chart.interaction.modifier;

import com.apokalypsix.chartx.core.render.model.CrosshairLayerV2;
import com.apokalypsix.chartx.core.render.model.TextOverlay;
import com.apokalypsix.chartx.chart.style.TooltipConfig;
import com.apokalypsix.chartx.core.render.model.TooltipData;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.interaction.ChartModifierBase;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;
import com.apokalypsix.chartx.chart.interaction.ModifierSurface;
import com.apokalypsix.chartx.core.interaction.modifier.MouseEventGroup;

import java.awt.Color;

/**
 * Modifier for crosshair cursor and rollover tooltips.
 *
 * <p>This modifier displays crosshair lines at the cursor position along with
 * axis labels showing the current time and price values. It can optionally
 * snap to data points and propagate crosshair position to synchronized charts.
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Vertical and horizontal crosshair lines</li>
 *   <li>Time label on X-axis</li>
 *   <li>Price labels on Y-axes</li>
 *   <li>Optional snap-to-data-point</li>
 *   <li>Multi-chart crosshair sync</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * chart.getModifiers().with(new RolloverModifier()
 *     .setSnapToDataPoint(true)
 *     .setShowAxisLabels(true));
 * }</pre>
 */
public class RolloverModifier extends ChartModifierBase {

    /** Whether to snap crosshair to nearest data point */
    private boolean snapToDataPoint = false;

    /** Whether to show axis labels at crosshair position */
    private boolean showAxisLabels = true;

    /** Whether to propagate crosshair to synced charts */
    private boolean syncEnabled = true;

    /** Whether crosshair is currently visible */
    private boolean crosshairVisible = false;

    /** Current crosshair X position (screen coords, HiDPI scaled) */
    private int cursorX = -1;

    /** Current crosshair Y position (screen coords, HiDPI scaled) */
    private int cursorY = -1;

    /** Whether to show tooltips with data values */
    private boolean tooltipEnabled = true;

    /** Tooltip configuration */
    private TooltipConfig tooltipConfig;

    /** Reusable tooltip data container (avoid allocations) */
    private final TooltipData tooltipData = new TooltipData();

    /** Default series color for tooltips */
    private Color defaultSeriesColor = new Color(100, 149, 237); // Cornflower blue

    /**
     * Creates a new RolloverModifier with default settings.
     */
    public RolloverModifier() {
        // Receive all move events, even if other modifiers handled them
        setReceiveHandledEvents(true);
    }

    // ========== Configuration ==========

    /**
     * Sets whether to snap the crosshair to the nearest data point.
     *
     * <p>When enabled, the crosshair X position will snap to the nearest
     * bar/candle timestamp, and the Y position will show the corresponding
     * price value.
     *
     * @param snap true to enable snapping
     * @return this modifier for chaining
     */
    public RolloverModifier setSnapToDataPoint(boolean snap) {
        this.snapToDataPoint = snap;
        return this;
    }

    /**
     * Returns whether snap-to-data-point is enabled.
     *
     * @return true if snapping is enabled
     */
    public boolean isSnapToDataPoint() {
        return snapToDataPoint;
    }

    /**
     * Sets whether to show axis labels at the crosshair position.
     *
     * @param show true to show labels
     * @return this modifier for chaining
     */
    public RolloverModifier setShowAxisLabels(boolean show) {
        this.showAxisLabels = show;
        return this;
    }

    /**
     * Returns whether axis labels are shown.
     *
     * @return true if labels are shown
     */
    public boolean isShowAxisLabels() {
        return showAxisLabels;
    }

    /**
     * Sets whether to synchronize crosshair with other charts in the same
     * mouse event group.
     *
     * @param enabled true to enable sync
     * @return this modifier for chaining
     */
    public RolloverModifier setSyncEnabled(boolean enabled) {
        this.syncEnabled = enabled;
        return this;
    }

    /**
     * Returns whether multi-chart sync is enabled.
     *
     * @return true if sync is enabled
     */
    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    /**
     * Sets whether to show tooltips with data values at the cursor position.
     *
     * @param enabled true to show tooltips
     * @return this modifier for chaining
     */
    public RolloverModifier setTooltipEnabled(boolean enabled) {
        this.tooltipEnabled = enabled;
        return this;
    }

    /**
     * Returns whether tooltips are enabled.
     *
     * @return true if tooltips are enabled
     */
    public boolean isTooltipEnabled() {
        return tooltipEnabled;
    }

    /**
     * Sets the tooltip configuration.
     *
     * @param config the tooltip config
     * @return this modifier for chaining
     */
    public RolloverModifier setTooltipConfig(TooltipConfig config) {
        this.tooltipConfig = config;
        return this;
    }

    /**
     * Returns the tooltip configuration.
     *
     * @return the tooltip config, or null if using defaults
     */
    public TooltipConfig getTooltipConfig() {
        return tooltipConfig;
    }

    /**
     * Sets the default color used for series without explicit color.
     *
     * @param color the default color
     * @return this modifier for chaining
     */
    public RolloverModifier setDefaultSeriesColor(Color color) {
        this.defaultSeriesColor = color;
        return this;
    }

    // ========== Mouse Events ==========

    @Override
    public boolean onMouseMoved(ModifierMouseEventArgs args) {
        updateCrosshairPosition(args);
        return false; // Don't consume - allow other modifiers to see move events
    }

    @Override
    public boolean onMouseDragged(ModifierMouseEventArgs args) {
        updateCrosshairPosition(args);
        return false; // Don't consume
    }

    @Override
    public boolean onMouseEntered(ModifierMouseEventArgs args) {
        setCrosshairVisible(true);
        updateCrosshairPosition(args);
        return false;
    }

    @Override
    public boolean onMouseExited(ModifierMouseEventArgs args) {
        setCrosshairVisible(false);
        return false;
    }

    // ========== Crosshair Logic ==========

    /**
     * Updates the crosshair position based on mouse event.
     */
    private void updateCrosshairPosition(ModifierMouseEventArgs args) {
        if (surface == null) {
            return;
        }

        int x = args.getScreenX();
        int y = args.getScreenY();

        // Optionally snap to nearest data point
        if (snapToDataPoint && args.isMaster()) {
            x = snapXToDataPoint(x);
        }

        // Update position
        cursorX = x;
        cursorY = y;

        // Calculate bar index at cursor position
        int barIndex = calculateBarIndex(x);

        // Update crosshair layer
        CrosshairLayerV2 crosshairLayer = surface.getCrosshairLayer();
        if (crosshairLayer != null) {
            crosshairLayer.setCursorPosition(x, y);
        }

        // Update text overlay for axis labels and tooltips
        TextOverlay textOverlay = surface.getTextOverlay();
        if (textOverlay != null) {
            if (showAxisLabels) {
                textOverlay.setCursorPosition(x, y);
            }

            // Update tooltip data
            if (tooltipEnabled) {
                updateTooltipData(barIndex);
                textOverlay.setTooltipPosition(x, y);
                textOverlay.setTooltipData(tooltipData);
                textOverlay.setTooltipVisible(!tooltipData.isEmpty());
            }
        }

        // Update info overlays (OHLC, indicators) with bar at cursor
        surface.setOverlayDisplayIndex(barIndex);

        // Propagate to synced charts
        if (syncEnabled && args.isMaster()) {
            propagateToSyncGroup(args);
        }

        requestRepaint();
    }

    /**
     * Calculates the bar index at the given screen X coordinate.
     *
     * @param screenX the screen X coordinate
     * @return the bar index, or -1 if not available
     */
    private int calculateBarIndex(int screenX) {
        Data<?> data = surface.getPrimaryData();
        var coords = getCoordinates();

        if (data == null || data.isEmpty() || coords == null) {
            return -1;
        }

        // Convert screen X to timestamp
        long timestamp = coords.screenXToXValue(screenX);

        // Find the nearest data point
        int idx = data.indexAtOrBefore(timestamp);
        if (idx < 0) {
            idx = 0;
        }

        // If we have a next point, check which is closer
        if (idx < data.size() - 1) {
            long timeBefore = data.getXValue(idx);
            long timeAfter = data.getXValue(idx + 1);
            if (Math.abs(timestamp - timeAfter) < Math.abs(timestamp - timeBefore)) {
                idx = idx + 1;
            }
        }

        if (idx < 0 || idx >= data.size()) {
            return -1;
        }

        return idx;
    }

    /**
     * Collects tooltip data from the primary data at the given bar index.
     *
     * @param barIndex the bar index, or -1 if no valid bar
     */
    private void updateTooltipData(int barIndex) {
        tooltipData.clear();

        if (barIndex < 0) {
            return;
        }

        Data<?> data = surface.getPrimaryData();
        if (data == null || barIndex >= data.size()) {
            return;
        }

        tooltipData.setTimestamp(data.getXValue(barIndex));

        // Collect data based on data type
        if (data instanceof OhlcData ohlcData) {
            collectOhlcTooltipData(ohlcData, barIndex);
        } else if (data instanceof XyData xyData) {
            collectXyTooltipData(xyData, barIndex);
        }
    }

    /**
     * Collects OHLCV tooltip data from candlestick data.
     */
    private void collectOhlcTooltipData(OhlcData data, int index) {
        float open = data.getOpen(index);
        float high = data.getHigh(index);
        float low = data.getLow(index);
        float close = data.getClose(index);
        float volume = data.getVolume(index);

        tooltipData.addOHLCRow(
                data.getName(),
                defaultSeriesColor,
                open, high, low, close, volume
        );
    }

    /**
     * Collects value tooltip data from XY data.
     */
    private void collectXyTooltipData(XyData data, int index) {
        float value = data.getValue(index);
        tooltipData.addValueRow(data.getName(), defaultSeriesColor, value);
    }

    /**
     * Snaps the X coordinate to the nearest data point timestamp.
     */
    private int snapXToDataPoint(int screenX) {
        if (surface == null) {
            return screenX;
        }

        Data<?> data = surface.getPrimaryData();
        var coords = getCoordinates();

        if (data == null || data.isEmpty() || coords == null) {
            return screenX;
        }

        // Convert screen X to timestamp
        long timestamp = coords.screenXToXValue(screenX);

        // Find nearest data point
        int idx = data.indexAtOrBefore(timestamp);
        if (idx < 0) {
            idx = 0;
        } else if (idx < data.size() - 1) {
            // Check if next point is closer
            long timeBefore = data.getXValue(idx);
            long timeAfter = data.getXValue(idx + 1);
            if (Math.abs(timestamp - timeAfter) < Math.abs(timestamp - timeBefore)) {
                idx = idx + 1;
            }
        }

        // Convert back to screen coordinate
        if (idx >= 0 && idx < data.size()) {
            long snappedTime = data.getXValue(idx);
            return (int) coords.xValueToScreenX(snappedTime);
        }

        return screenX;
    }

    /**
     * Sets crosshair visibility.
     */
    private void setCrosshairVisible(boolean visible) {
        crosshairVisible = visible;

        if (surface == null) {
            return;
        }

        CrosshairLayerV2 crosshairLayer = surface.getCrosshairLayer();
        if (crosshairLayer != null) {
            crosshairLayer.setCursorVisible(visible);
        }

        TextOverlay textOverlay = surface.getTextOverlay();
        if (textOverlay != null) {
            if (showAxisLabels) {
                textOverlay.setCrosshairVisible(visible);
            }

            // Hide tooltip when crosshair is hidden
            if (!visible && tooltipEnabled) {
                textOverlay.setTooltipVisible(false);
            }
        }

        // Reset overlays to show latest bar when mouse exits
        if (!visible) {
            surface.setOverlayDisplayIndex(-1);
        }

        requestRepaint();
    }

    /**
     * Propagates crosshair position to synced charts.
     */
    private void propagateToSyncGroup(ModifierMouseEventArgs args) {
        MouseEventGroup group = surface.getMouseEventGroup();
        if (group != null) {
            group.propagateEvent(surface, args, MouseEventGroup.MouseEventType.MOVED);
        }
    }

    // ========== State Access ==========

    /**
     * Returns whether the crosshair is currently visible.
     *
     * @return true if visible
     */
    public boolean isCrosshairVisible() {
        return crosshairVisible;
    }

    /**
     * Returns the current crosshair X position (HiDPI scaled).
     *
     * @return X coordinate or -1 if not set
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Returns the current crosshair Y position (HiDPI scaled).
     *
     * @return Y coordinate or -1 if not set
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Returns the timestamp at the current crosshair position.
     *
     * @return timestamp in epoch milliseconds, or -1 if not available
     */
    public long getCursorTimestamp() {
        if (cursorX < 0 || getCoordinates() == null) {
            return -1;
        }
        return getCoordinates().screenXToXValue(cursorX);
    }

    /**
     * Returns the price at the current crosshair position.
     *
     * @return price value, or NaN if not available
     */
    public double getCursorPrice() {
        if (cursorY < 0 || getCoordinates() == null) {
            return Double.NaN;
        }
        return getCoordinates().screenYToYValue(cursorY);
    }

    @Override
    public void onDetached() {
        // Hide crosshair when detached
        setCrosshairVisible(false);
        super.onDetached();
    }
}
