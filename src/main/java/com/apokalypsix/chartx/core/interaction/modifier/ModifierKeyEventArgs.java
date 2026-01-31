package com.apokalypsix.chartx.core.interaction.modifier;

import com.apokalypsix.chartx.chart.interaction.ModifierSurface;

import java.awt.event.KeyEvent;

/**
 * Keyboard event arguments for chart modifiers.
 *
 * <p>Encapsulates keyboard event data including key code, character,
 * and modifier key states.
 */
public final class ModifierKeyEventArgs extends ModifierEventArgs {

    /** The virtual key code (e.g., KeyEvent.VK_DELETE) */
    private int keyCode;

    /** The character for this key (if applicable) */
    private char keyChar;

    /** Whether shift key is held */
    private boolean shiftDown;

    /** Whether ctrl key is held */
    private boolean ctrlDown;

    /** Whether alt key is held */
    private boolean altDown;

    /** Whether meta key (Command on Mac, Windows key on Windows) is held */
    private boolean metaDown;

    /**
     * Creates an empty keyboard event args for pooling.
     */
    public ModifierKeyEventArgs() {
    }

    // ========== Accessors ==========

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
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

    // ========== Convenience Methods ==========

    /**
     * Returns true if this is the Delete key.
     */
    public boolean isDelete() {
        return keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE;
    }

    /**
     * Returns true if this is the Escape key.
     */
    public boolean isEscape() {
        return keyCode == KeyEvent.VK_ESCAPE;
    }

    /**
     * Returns true if this is an Enter key.
     */
    public boolean isEnter() {
        return keyCode == KeyEvent.VK_ENTER;
    }

    /**
     * Returns true if this is an arrow key.
     */
    public boolean isArrowKey() {
        return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT
                || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
    }

    // ========== Population ==========

    /**
     * Populates this event args from a Swing KeyEvent.
     *
     * @param e the source KeyEvent
     * @param surface the source surface
     */
    public void populate(KeyEvent e, ModifierSurface surface) {
        this.source = surface;
        this.keyCode = e.getKeyCode();
        this.keyChar = e.getKeyChar();
        this.shiftDown = e.isShiftDown();
        this.ctrlDown = e.isControlDown();
        this.altDown = e.isAltDown();
        this.metaDown = e.isMetaDown();
        this.handled = false;
        this.master = true;
    }

    @Override
    public void reset() {
        super.reset();
        keyCode = 0;
        keyChar = '\0';
        shiftDown = false;
        ctrlDown = false;
        altDown = false;
        metaDown = false;
    }
}
