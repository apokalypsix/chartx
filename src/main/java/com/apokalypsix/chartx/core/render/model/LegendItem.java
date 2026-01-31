package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;

import com.apokalypsix.chartx.chart.series.SeriesType;

/**
 * Represents a single item in the chart legend.
 *
 * <p>Contains the series identifier, display name, color, and series type
 * for rendering the appropriate swatch style.
 */
public class LegendItem {

    private final String seriesId;
    private final String displayName;
    private final Color color;
    private final SeriesType type;
    private boolean visible = true;

    /**
     * Creates a legend item.
     *
     * @param seriesId unique identifier for the series
     * @param displayName name to display in the legend
     * @param color color for the swatch
     * @param type series type (affects swatch rendering)
     */
    public LegendItem(String seriesId, String displayName, Color color, SeriesType type) {
        this.seriesId = seriesId;
        this.displayName = displayName != null ? displayName : seriesId;
        this.color = color != null ? color : Color.WHITE;
        this.type = type != null ? type : SeriesType.LINE;
    }

    /**
     * Creates a legend item with LINE type.
     *
     * @param seriesId unique identifier for the series
     * @param displayName name to display in the legend
     * @param color color for the swatch
     */
    public LegendItem(String seriesId, String displayName, Color color) {
        this(seriesId, displayName, color, SeriesType.LINE);
    }

    // ========== Getters ==========

    public String getSeriesId() {
        return seriesId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    public SeriesType getType() {
        return type;
    }

    public boolean isVisible() {
        return visible;
    }

    // ========== Setters ==========

    /**
     * Sets whether this item is visible in the legend.
     *
     * @param visible true to show the item
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LegendItem other = (LegendItem) obj;
        return seriesId.equals(other.seriesId);
    }

    @Override
    public int hashCode() {
        return seriesId.hashCode();
    }
}
