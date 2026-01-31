package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Data for gauge charts (linear and radial).
 *
 * <p>Stores one or more values within a defined range, along with
 * optional color zones for visual thresholds.
 */
public class GaugeData {

    /**
     * Represents a colored zone on the gauge.
     */
    public static class Zone {
        private final float start;
        private final float end;
        private final Color color;
        private final String label;

        public Zone(float start, float end, Color color) {
            this(start, end, color, null);
        }

        public Zone(float start, float end, Color color, String label) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.label = label;
        }

        public float getStart() {
            return start;
        }

        public float getEnd() {
            return end;
        }

        public Color getColor() {
            return color;
        }

        public String getLabel() {
            return label;
        }

        public boolean contains(float value) {
            return value >= start && value <= end;
        }
    }

    private final String id;
    private final String name;

    /** Current values (supports multiple needles) */
    private float[] values;

    /** Value labels (for multiple needles) */
    private String[] valueLabels;

    /** Number of values */
    private int valueCount;

    /** Minimum value of the gauge range */
    private float minValue;

    /** Maximum value of the gauge range */
    private float maxValue;

    /** Color zones */
    private final List<Zone> zones;

    /** Listener support */
    private final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates gauge data with a single value.
     *
     * @param id unique identifier
     * @param name display name
     * @param minValue minimum gauge value
     * @param maxValue maximum gauge value
     */
    public GaugeData(String id, String name, float minValue, float maxValue) {
        this(id, name, minValue, maxValue, 1);
    }

    /**
     * Creates gauge data with multiple values (for multi-needle gauges).
     *
     * @param id unique identifier
     * @param name display name
     * @param minValue minimum gauge value
     * @param maxValue maximum gauge value
     * @param valueCount number of values/needles
     */
    public GaugeData(String id, String name, float minValue, float maxValue, int valueCount) {
        this.id = id;
        this.name = name;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.valueCount = valueCount;
        this.values = new float[valueCount];
        this.valueLabels = new String[valueCount];
        this.zones = new ArrayList<>();

        // Initialize values to minimum
        for (int i = 0; i < valueCount; i++) {
            values[i] = minValue;
        }
    }

    // ========== Basic Accessors ==========

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getRange() {
        return maxValue - minValue;
    }

    public int getValueCount() {
        return valueCount;
    }

    // ========== Value Accessors ==========

    /**
     * Returns the primary (first) value.
     */
    public float getValue() {
        return values[0];
    }

    /**
     * Returns the value at the specified index.
     */
    public float getValue(int index) {
        if (index < 0 || index >= valueCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Count: " + valueCount);
        }
        return values[index];
    }

    /**
     * Returns the label for the value at the specified index.
     */
    public String getValueLabel(int index) {
        if (index < 0 || index >= valueCount) {
            return null;
        }
        return valueLabels[index];
    }

    /**
     * Returns the normalized value (0-1) for the primary value.
     */
    public float getNormalizedValue() {
        return getNormalizedValue(0);
    }

    /**
     * Returns the normalized value (0-1) for the specified index.
     */
    public float getNormalizedValue(int index) {
        float range = getRange();
        if (range <= 0) {
            return 0.5f;
        }
        float value = getValue(index);
        return Math.max(0, Math.min(1, (value - minValue) / range));
    }

    /**
     * Returns all values.
     */
    public float[] getValues() {
        return values.clone();
    }

    // ========== Value Mutation ==========

    /**
     * Sets the primary (first) value.
     */
    public void setValue(float value) {
        setValue(0, value);
    }

    /**
     * Sets the value at the specified index.
     */
    public void setValue(int index, float value) {
        if (index < 0 || index >= valueCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Count: " + valueCount);
        }
        // Clamp to range
        values[index] = Math.max(minValue, Math.min(maxValue, value));
        listenerSupport.fireDataUpdated(null, index);
    }

    /**
     * Sets the label for a value.
     */
    public void setValueLabel(int index, String label) {
        if (index >= 0 && index < valueCount) {
            valueLabels[index] = label;
        }
    }

    /**
     * Sets all values at once.
     */
    public void setValues(float... newValues) {
        int count = Math.min(newValues.length, valueCount);
        for (int i = 0; i < count; i++) {
            values[i] = Math.max(minValue, Math.min(maxValue, newValues[i]));
        }
        listenerSupport.fireDataUpdated(null, 0);
    }

    // ========== Range Mutation ==========

    /**
     * Sets the gauge range.
     */
    public void setRange(float min, float max) {
        if (max <= min) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        this.minValue = min;
        this.maxValue = max;

        // Clamp existing values to new range
        for (int i = 0; i < valueCount; i++) {
            values[i] = Math.max(min, Math.min(max, values[i]));
        }
    }

    // ========== Zone Management ==========

    /**
     * Adds a color zone to the gauge.
     *
     * @param start zone start value
     * @param end zone end value
     * @param color zone color
     */
    public void addZone(float start, float end, Color color) {
        zones.add(new Zone(start, end, color));
    }

    /**
     * Adds a labeled color zone to the gauge.
     */
    public void addZone(float start, float end, Color color, String label) {
        zones.add(new Zone(start, end, color, label));
    }

    /**
     * Adds a zone object.
     */
    public void addZone(Zone zone) {
        zones.add(zone);
    }

    /**
     * Clears all zones.
     */
    public void clearZones() {
        zones.clear();
    }

    /**
     * Returns all zones.
     */
    public List<Zone> getZones() {
        return new ArrayList<>(zones);
    }

    /**
     * Returns the zone containing the specified value, or null if none.
     */
    public Zone getZoneAt(float value) {
        for (Zone zone : zones) {
            if (zone.contains(value)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Returns the color for the specified value based on zones.
     *
     * @param value the value to check
     * @param defaultColor color to return if no zone contains the value
     * @return the zone color or default color
     */
    public Color getColorForValue(float value, Color defaultColor) {
        Zone zone = getZoneAt(value);
        return zone != null ? zone.getColor() : defaultColor;
    }

    // ========== Convenience Methods ==========

    /**
     * Creates standard traffic light zones (green/yellow/red).
     *
     * @param yellowStart start of yellow zone
     * @param redStart start of red zone
     */
    public void setTrafficLightZones(float yellowStart, float redStart) {
        clearZones();
        addZone(minValue, yellowStart, new Color(50, 180, 50), "Normal");
        addZone(yellowStart, redStart, new Color(255, 200, 0), "Warning");
        addZone(redStart, maxValue, new Color(220, 50, 50), "Critical");
    }

    /**
     * Creates gradient zones across the full range.
     *
     * @param zoneCount number of zones
     * @param startColor color at minimum
     * @param endColor color at maximum
     */
    public void setGradientZones(int zoneCount, Color startColor, Color endColor) {
        clearZones();
        float step = getRange() / zoneCount;

        for (int i = 0; i < zoneCount; i++) {
            float t = (float) i / (zoneCount - 1);
            int r = (int) (startColor.getRed() + t * (endColor.getRed() - startColor.getRed()));
            int g = (int) (startColor.getGreen() + t * (endColor.getGreen() - startColor.getGreen()));
            int b = (int) (startColor.getBlue() + t * (endColor.getBlue() - startColor.getBlue()));

            float start = minValue + i * step;
            float end = (i == zoneCount - 1) ? maxValue : minValue + (i + 1) * step;
            addZone(start, end, new Color(r, g, b));
        }
    }

    // ========== Listener Management ==========

    public void addListener(DataListener listener) {
        listenerSupport.addListener(listener);
    }

    public void removeListener(DataListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public String toString() {
        return String.format("GaugeData[id=%s, value=%.2f, range=%.2f-%.2f, zones=%d]",
                id, getValue(), minValue, maxValue, zones.size());
    }
}
