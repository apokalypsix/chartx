package com.apokalypsix.chartx.core.ui.properties;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Interface for property editors that provide UI components for editing values.
 *
 * <p>Property editors create Swing components for editing specific value types
 * (int, double, color, boolean, enum). They support value change callbacks
 * to enable staged changes without immediate application.
 *
 * @param <T> the type of value this editor handles
 */
public interface PropertyEditor<T> {

    /**
     * Returns the Swing component for this editor.
     *
     * @return the editor component
     */
    JComponent getComponent();

    /**
     * Returns the current value of this editor.
     *
     * @return the current value
     */
    T getValue();

    /**
     * Sets the value of this editor.
     *
     * @param value the value to set
     */
    void setValue(T value);

    /**
     * Sets a callback to be invoked when the value changes.
     *
     * @param callback the callback to invoke with the new value
     */
    void setOnValueChanged(Consumer<T> callback);

    /**
     * Returns true if the current value is valid.
     *
     * @return true if valid
     */
    default boolean isValid() {
        return true;
    }

    /**
     * Sets whether this editor is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    default void setEnabled(boolean enabled) {
        getComponent().setEnabled(enabled);
    }

    /**
     * Returns whether this editor is enabled.
     *
     * @return true if enabled
     */
    default boolean isEnabled() {
        return getComponent().isEnabled();
    }
}
