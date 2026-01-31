package com.apokalypsix.chartx.core.overlay;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.overlay.Drawing;

import java.util.List;

/**
 * Interface for automatically generating chart annotations.
 *
 * <p>AnnotationGenerators create drawings, regions, or labels based on
 * data analysis (e.g., day boundaries, session times, patterns).
 *
 * <p>Implementations can generate:
 * <ul>
 *   <li>Vertical lines (day separators, session boundaries)</li>
 *   <li>Horizontal lines (support/resistance levels)</li>
 *   <li>Rectangles (initial balance, value areas)</li>
 *   <li>Text annotations (pattern labels, markers)</li>
 * </ul>
 */
public interface AnnotationGenerator {

    /**
     * Returns the unique ID of this generator.
     */
    String getId();

    /**
     * Returns the display name of this generator.
     */
    String getName();

    /**
     * Generates annotations from the given data.
     *
     * @param data the OHLC data
     * @return list of generated drawings/annotations
     */
    List<Drawing> generate(OhlcData data);

    /**
     * Returns true if this generator is enabled.
     */
    boolean isEnabled();

    /**
     * Sets whether this generator is enabled.
     */
    void setEnabled(boolean enabled);

    /**
     * Called when the data is updated.
     * Generators can implement incremental update logic.
     *
     * @param data the updated data
     * @param fromIndex the index from which data was added/changed
     * @return new annotations to add, or null to regenerate all
     */
    default List<Drawing> onDataUpdated(OhlcData data, int fromIndex) {
        // Default: regenerate all
        return null;
    }
}
