package com.apokalypsix.chartx.core.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ModifierSurface;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Mouse event arguments for chart modifiers.
 *
 * <p>This class encapsulates all mouse event data needed by modifiers,
 * including screen coordinates, button state, modifier keys, and wheel rotation.
 *
 * <p><b>Pooling:</b> Instances are pooled to avoid GC pressure during
 * high-frequency mouse move events. Always obtain instances via
 * {@link com.apokalypsix.chartx.interaction.modifier.event.ModifierEventPool#acquireMouseArgs()}
 * and release via {@link com.apokalypsix.chartx.interaction.modifier.event.ModifierEventPool#release(ModifierMouseEventArgs)}.
 *
 * <p><b>Coordinate Systems:</b>
 * <ul>
 *   <li>{@code screenX/screenY} - HiDPI-scaled screen coordinates</li>
 *   <li>{@code getTimestamp()/getPrice()} - Data coordinates (lazy computed)</li>
 * </ul>
 */
public final class ModifierMouseEventArgs extends ModifierEventArgs {

    // ========== Screen Coordinates (HiDPI scaled) ==========

    /** X coordinate in scaled pixels */
    private int screenX;

    /** Y coordinate in scaled pixels */
    private int screenY;

    /** X delta from previous event (for drag operations) */
    private int deltaX;

    /** Y delta from previous event (for drag operations) */
    private int deltaY;

    // ========== Wheel ==========

    /** Mouse wheel rotation (positive = scroll down, negative = scroll up) */
    private double wheelRotation;

    // ========== Button/Modifier State ==========

    /** The mouse button that triggered this event (1=left, 2=middle, 3=right) */
    private int button;

    /** Click count for multi-click detection */
    private int clickCount;

    /** Whether shift key is held */
    private boolean shiftDown;

    /** Whether ctrl key is held */
    private boolean ctrlDown;

    /** Whether alt key is held */
    private boolean altDown;

    /** Whether meta key (Command on Mac, Windows key on Windows) is held */
    private boolean metaDown;

    // ========== Hit Detection Cache ==========

    /** The ID of the Y-axis under the mouse, or null if in chart area */
    private String hitAxisId;

    /** Cached flag for chart area hit */
    private boolean inChartArea;

    /** Cached flag for X-axis area hit */
    private boolean inXAxisArea;

    /** Whether hit detection has been computed */
    private boolean hitDetectionComputed;

    // ========== Data Coordinates (lazy) ==========

    /** Cached timestamp at current position */
    private long timestamp = Long.MIN_VALUE;

    /** Cached price at current position */
    private double price = Double.NaN;

    /** Whether data coordinates have been computed */
    private boolean dataCoordsComputed;

    // ========== Constructors ==========

    /**
     * Creates an empty mouse event args for pooling.
     */
    public ModifierMouseEventArgs() {
    }

    // ========== Screen Coordinates ==========

    public int getScreenX() {
        return screenX;
    }

    public void setScreenX(int screenX) {
        this.screenX = screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public void setScreenY(int screenY) {
        this.screenY = screenY;
    }

    public int getDeltaX() {
        return deltaX;
    }

    public void setDeltaX(int deltaX) {
        this.deltaX = deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }

    public void setDeltaY(int deltaY) {
        this.deltaY = deltaY;
    }

    // ========== Wheel ==========

    public double getWheelRotation() {
        return wheelRotation;
    }

    public void setWheelRotation(double wheelRotation) {
        this.wheelRotation = wheelRotation;
    }

    // ========== Button/Modifier State ==========

    public int getButton() {
        return button;
    }

    public void setButton(int button) {
        this.button = button;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public void setShiftDown(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public boolean isCtrlDown() {
        return ctrlDown;
    }

    public void setCtrlDown(boolean ctrlDown) {
        this.ctrlDown = ctrlDown;
    }

    public boolean isAltDown() {
        return altDown;
    }

    public void setAltDown(boolean altDown) {
        this.altDown = altDown;
    }

    public boolean isMetaDown() {
        return metaDown;
    }

    public void setMetaDown(boolean metaDown) {
        this.metaDown = metaDown;
    }

    /**
     * Returns true if left mouse button is involved in this event.
     */
    public boolean isLeftButton() {
        return button == MouseEvent.BUTTON1;
    }

    /**
     * Returns true if right mouse button is involved in this event.
     */
    public boolean isRightButton() {
        return button == MouseEvent.BUTTON3;
    }

    /**
     * Returns true if middle mouse button is involved in this event.
     */
    public boolean isMiddleButton() {
        return button == MouseEvent.BUTTON2;
    }

    // ========== Data Coordinates (lazy computed) ==========

    /**
     * Returns the timestamp at the current mouse position.
     *
     * <p>This is lazily computed from screen coordinates using the
     * coordinate system from the source surface.
     *
     * @return timestamp in epoch milliseconds
     */
    public long getTimestamp() {
        ensureDataCoordsComputed();
        return timestamp;
    }

    /**
     * Returns the price at the current mouse position.
     *
     * <p>This is lazily computed from screen coordinates using the
     * coordinate system from the source surface.
     *
     * @return the price value
     */
    public double getPrice() {
        ensureDataCoordsComputed();
        return price;
    }

    private void ensureDataCoordsComputed() {
        if (!dataCoordsComputed && source != null) {
            var coords = source.getCoordinateSystem();
            if (coords != null) {
                timestamp = coords.screenXToXValue(screenX);
                price = coords.screenYToYValue(screenY);
            }
            dataCoordsComputed = true;
        }
    }

    // ========== Hit Detection ==========

    /**
     * Returns the ID of the Y-axis under the mouse, or null if not over an axis.
     *
     * @return axis ID or null
     */
    public String getHitAxisId() {
        ensureHitDetectionComputed();
        return hitAxisId;
    }

    /**
     * Returns true if the mouse is in the main chart area.
     *
     * @return true if in chart area
     */
    public boolean isInChartArea() {
        ensureHitDetectionComputed();
        return inChartArea;
    }

    /**
     * Returns true if the mouse is in the X-axis area.
     *
     * @return true if in X-axis area
     */
    public boolean isInXAxisArea() {
        ensureHitDetectionComputed();
        return inXAxisArea;
    }

    private void ensureHitDetectionComputed() {
        if (!hitDetectionComputed && source != null) {
            hitAxisId = source.findAxisAtPosition(screenX, screenY);

            var viewport = source.getViewport();
            if (viewport != null) {
                int chartLeft = viewport.getLeftInset();
                int chartRight = viewport.getWidth() - viewport.getRightInset();
                int chartTop = viewport.getTopInset();
                int chartBottom = viewport.getHeight() - viewport.getBottomInset();

                inChartArea = screenX >= chartLeft && screenX <= chartRight
                        && screenY >= chartTop && screenY <= chartBottom;
                inXAxisArea = screenY > chartBottom && screenX >= chartLeft && screenX <= chartRight;
            }
            hitDetectionComputed = true;
        }
    }

    // ========== Pooling Support ==========

    /**
     * Populates this event args from a Swing MouseEvent.
     *
     * @param e the source MouseEvent
     * @param scaledX HiDPI-scaled X coordinate
     * @param scaledY HiDPI-scaled Y coordinate
     * @param surface the source surface
     */
    public void populate(MouseEvent e, int scaledX, int scaledY, ModifierSurface surface) {
        this.source = surface;
        this.screenX = scaledX;
        this.screenY = scaledY;
        this.button = e.getButton();
        this.clickCount = e.getClickCount();
        this.shiftDown = e.isShiftDown();
        this.ctrlDown = e.isControlDown();
        this.altDown = e.isAltDown();
        this.metaDown = e.isMetaDown();
        this.wheelRotation = 0;
        this.deltaX = 0;
        this.deltaY = 0;
        this.handled = false;
        this.master = true;

        // Reset lazy caches
        this.dataCoordsComputed = false;
        this.hitDetectionComputed = false;
        this.timestamp = Long.MIN_VALUE;
        this.price = Double.NaN;
        this.hitAxisId = null;
        this.inChartArea = false;
        this.inXAxisArea = false;
    }

    /**
     * Populates this event args from a Swing MouseWheelEvent.
     *
     * @param e the source MouseWheelEvent
     * @param scaledX HiDPI-scaled X coordinate
     * @param scaledY HiDPI-scaled Y coordinate
     * @param surface the source surface
     */
    public void populateWheel(MouseWheelEvent e, int scaledX, int scaledY, ModifierSurface surface) {
        populate(e, scaledX, scaledY, surface);
        this.wheelRotation = e.getPreciseWheelRotation();
    }

    /**
     * Sets the delta values for drag operations.
     *
     * @param deltaX X delta from previous position
     * @param deltaY Y delta from previous position
     */
    public void setDelta(int deltaX, int deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override
    public void reset() {
        super.reset();
        screenX = 0;
        screenY = 0;
        deltaX = 0;
        deltaY = 0;
        wheelRotation = 0;
        button = 0;
        clickCount = 0;
        shiftDown = false;
        ctrlDown = false;
        altDown = false;
        metaDown = false;
        hitAxisId = null;
        inChartArea = false;
        inXAxisArea = false;
        hitDetectionComputed = false;
        timestamp = Long.MIN_VALUE;
        price = Double.NaN;
        dataCoordsComputed = false;
    }
}
