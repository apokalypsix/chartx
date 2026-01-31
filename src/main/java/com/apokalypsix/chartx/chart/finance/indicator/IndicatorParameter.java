package com.apokalypsix.chartx.chart.finance.indicator;

import java.awt.Color;

/**
 * Represents a configurable parameter for an indicator.
 *
 * <p>Parameters can be of different types: integer (periods, lengths),
 * double (multipliers, thresholds), color (line colors), or boolean (flags).
 *
 * <p>Each parameter has a name, default value, and optional constraints
 * (min/max for numeric types).
 */
public abstract class IndicatorParameter<T> {

    private final String name;
    private final String label;
    private final T defaultValue;

    protected IndicatorParameter(String name, String label, T defaultValue) {
        this.name = name;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the parameter name (used as identifier).
     */
    public String name() {
        return name;
    }

    /**
     * Returns the display label for UI.
     */
    public String label() {
        return label;
    }

    /**
     * Returns the default value.
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Validates a value for this parameter.
     *
     * @param value the value to validate
     * @return true if valid
     */
    public abstract boolean isValid(T value);

    // ========== Concrete parameter types ==========

    /**
     * Integer parameter (e.g., period, length).
     */
    public static class IntParam extends IndicatorParameter<Integer> {
        private final int min;
        private final int max;

        public IntParam(String name, String label, int defaultValue, int min, int max) {
            super(name, label, defaultValue);
            if (min > max) {
                throw new IllegalArgumentException("min must be <= max");
            }
            if (defaultValue < min || defaultValue > max) {
                throw new IllegalArgumentException("defaultValue must be in [min, max]");
            }
            this.min = min;
            this.max = max;
        }

        /**
         * Creates an integer parameter with default constraints.
         */
        public IntParam(String name, String label, int defaultValue) {
            this(name, label, defaultValue, 1, 1000);
        }

        public int min() { return min; }
        public int max() { return max; }

        @Override
        public boolean isValid(Integer value) {
            return value != null && value >= min && value <= max;
        }
    }

    /**
     * Double parameter (e.g., multiplier, threshold).
     */
    public static class DoubleParam extends IndicatorParameter<Double> {
        private final double min;
        private final double max;

        public DoubleParam(String name, String label, double defaultValue, double min, double max) {
            super(name, label, defaultValue);
            if (min > max) {
                throw new IllegalArgumentException("min must be <= max");
            }
            if (defaultValue < min || defaultValue > max) {
                throw new IllegalArgumentException("defaultValue must be in [min, max]");
            }
            this.min = min;
            this.max = max;
        }

        /**
         * Creates a double parameter with default constraints.
         */
        public DoubleParam(String name, String label, double defaultValue) {
            this(name, label, defaultValue, 0.0, 100.0);
        }

        public double min() { return min; }
        public double max() { return max; }

        @Override
        public boolean isValid(Double value) {
            return value != null && value >= min && value <= max;
        }
    }

    /**
     * Color parameter for indicator line/fill colors.
     */
    public static class ColorParam extends IndicatorParameter<Color> {

        public ColorParam(String name, String label, Color defaultValue) {
            super(name, label, defaultValue);
        }

        @Override
        public boolean isValid(Color value) {
            return value != null;
        }
    }

    /**
     * Boolean parameter (e.g., show/hide options).
     */
    public static class BooleanParam extends IndicatorParameter<Boolean> {

        public BooleanParam(String name, String label, boolean defaultValue) {
            super(name, label, defaultValue);
        }

        @Override
        public boolean isValid(Boolean value) {
            return value != null;
        }
    }

    /**
     * Enum parameter for selecting from predefined options.
     */
    public static class EnumParam<E extends Enum<E>> extends IndicatorParameter<E> {
        private final Class<E> enumClass;

        public EnumParam(String name, String label, E defaultValue, Class<E> enumClass) {
            super(name, label, defaultValue);
            this.enumClass = enumClass;
        }

        @Override
        public boolean isValid(E value) {
            return value != null;
        }

        /**
         * Returns all possible values for this parameter.
         */
        public E[] possibleValues() {
            return enumClass.getEnumConstants();
        }
    }
}
