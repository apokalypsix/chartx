package com.apokalypsix.chartx.core.data.model;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;
import com.apokalypsix.chartx.chart.data.Data;

import java.awt.Color;
import java.util.Arrays;

/**
 * Series of TPO (Time Price Opportunity) profiles for Market Profile analysis.
 *
 * <p>Each entry in the series is a complete trading session profile showing:
 * <ul>
 *   <li>TPO distribution at each price level</li>
 *   <li>Point of Control (POC)</li>
 *   <li>Value Area (High/Low)</li>
 *   <li>Initial Balance (IB)</li>
 *   <li>Single prints</li>
 * </ul>
 *
 * <h2>Display Modes</h2>
 * <ul>
 *   <li>LETTERS: Show TPO letters (A, B, C, ...)</li>
 *   <li>BLOCKS: Show colored blocks</li>
 *   <li>VOLUME: Show block width by volume</li>
 * </ul>
 */
public class TPOSeries implements Data<TPOProfile> {

    /**
     * Line type for VAH/VAL lines.
     */
    public enum LineType {
        SOLID("Solid", null),
        DASHED("Dashed", new float[]{10f, 5f}),
        DOTTED("Dotted", new float[]{2f, 4f});

        private final String displayName;
        private final float[] dashPattern;

        LineType(String displayName, float[] dashPattern) {
            this.displayName = displayName;
            this.dashPattern = dashPattern;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float[] getDashPattern() {
            return dashPattern;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Line thickness presets for VAH/VAL lines.
     */
    public enum LineThickness {
        THIN("Thin", 1.0f),
        NORMAL("Normal", 1.5f),
        MEDIUM("Medium", 2.0f),
        THICK("Thick", 3.0f);

        private final String displayName;
        private final float width;

        LineThickness(String displayName, float width) {
            this.displayName = displayName;
            this.width = width;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getWidth() {
            return width;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final int DEFAULT_INITIAL_CAPACITY = 64;
    private static final float GROWTH_FACTOR = 1.5f;

    private final String id;
    private final String name;
    private final float tickSize;

    // Session timestamps
    private long[] sessionStarts;

    // Profiles array
    private TPOProfile[] profiles;

    private int size;

    // Configuration
    private long tpoPeriodMillis = 30 * 60 * 1000; // 30 minutes default

    // Display settings
    private boolean showPOC = true;
    private boolean showValueArea = true;
    private boolean showInitialBalance = true;
    private boolean highlightSinglePrints = true;
    private boolean overlayMode = false;
    private float opacity = 1.0f;

    // Colors
    private Color pocColor = new Color(255, 193, 7);              // Yellow
    private Color valueAreaColor = new Color(100, 149, 237, 40);  // Light blue translucent
    private Color ibColor = new Color(255, 255, 255);             // White for IB period blocks
    private Color singlePrintColor = new Color(255, 0, 0, 100);   // Red translucent

    // VAH (Value Area High) line settings
    private boolean showVAH = false;
    private Color vahColor = new Color(0, 150, 255);              // Blue
    private LineType vahLineType = LineType.DASHED;
    private LineThickness vahLineThickness = LineThickness.NORMAL;

    // VAL (Value Area Low) line settings
    private boolean showVAL = false;
    private Color valColor = new Color(255, 100, 100);            // Red
    private LineType valLineType = LineType.DASHED;
    private LineThickness valLineThickness = LineThickness.NORMAL;

    // Listener support
    private final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates a new TPO series.
     *
     * @param id unique identifier
     * @param name display name
     * @param tickSize price tick size
     */
    public TPOSeries(String id, String name, float tickSize) {
        this(id, name, tickSize, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new TPO series with specified capacity.
     */
    public TPOSeries(String id, String name, float tickSize, int initialCapacity) {
        this.id = id;
        this.name = name;
        this.tickSize = tickSize;
        this.sessionStarts = new long[initialCapacity];
        this.profiles = new TPOProfile[initialCapacity];
        this.size = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getXValue(int index) {
        checkIndex(index);
        return sessionStarts[index];
    }

    /**
     * @deprecated Use {@link #getXValue(int)} instead
     */
    @Deprecated
    public long getTimestamp(int index) {
        return getXValue(index);
    }

    @Override
    public long getMinX() {
        return size > 0 ? sessionStarts[0] : -1;
    }

    /**
     * @deprecated Use {@link #getMinX()} instead
     */
    @Deprecated
    public long getStartTime() {
        return getMinX();
    }

    @Override
    public long getMaxX() {
        return size > 0 ? profiles[size - 1].getSessionEnd() : -1;
    }

    /**
     * @deprecated Use {@link #getMaxX()} instead
     */
    @Deprecated
    public long getEndTime() {
        return getMaxX();
    }

    public float getTickSize() {
        return tickSize;
    }

    public long getTpoPeriodMillis() {
        return tpoPeriodMillis;
    }

    public void setTpoPeriodMillis(long millis) {
        this.tpoPeriodMillis = millis;
    }

    // ========== Display settings ==========

    /**
     * Returns whether to show the Point of Control line.
     */
    public boolean isShowPOC() {
        return showPOC;
    }

    /**
     * Sets whether to show the Point of Control line.
     */
    public void setShowPOC(boolean showPOC) {
        this.showPOC = showPOC;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns whether to show the value area shading.
     */
    public boolean isShowValueArea() {
        return showValueArea;
    }

    /**
     * Sets whether to show the value area shading.
     */
    public void setShowValueArea(boolean showValueArea) {
        this.showValueArea = showValueArea;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns whether to show the initial balance range.
     */
    public boolean isShowInitialBalance() {
        return showInitialBalance;
    }

    /**
     * Sets whether to show the initial balance range.
     */
    public void setShowInitialBalance(boolean showInitialBalance) {
        this.showInitialBalance = showInitialBalance;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns whether to highlight single prints.
     */
    public boolean isHighlightSinglePrints() {
        return highlightSinglePrints;
    }

    /**
     * Sets whether to highlight single prints.
     */
    public void setHighlightSinglePrints(boolean highlightSinglePrints) {
        this.highlightSinglePrints = highlightSinglePrints;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns whether overlay mode is enabled.
     * In overlay mode, TPO renders on top of candlesticks with semi-transparency.
     */
    public boolean isOverlayMode() {
        return overlayMode;
    }

    /**
     * Sets whether to render in overlay mode.
     * In overlay mode, TPO renders on top of candlesticks with semi-transparency.
     */
    public void setOverlayMode(boolean overlayMode) {
        this.overlayMode = overlayMode;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the opacity for TPO rendering.
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Sets the opacity for TPO rendering.
     *
     * @param opacity opacity value (0.0 = transparent, 1.0 = opaque)
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
        listenerSupport.fireDataUpdated(this, -1);
    }

    // ========== Color settings ==========

    /**
     * Returns the Point of Control highlight color.
     */
    public Color getPocColor() {
        return pocColor;
    }

    /**
     * Sets the Point of Control highlight color.
     */
    public void setPocColor(Color pocColor) {
        this.pocColor = pocColor;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the value area background color.
     */
    public Color getValueAreaColor() {
        return valueAreaColor;
    }

    /**
     * Sets the value area background color.
     *
     * @param valueAreaColor the color (should have alpha for translucency)
     */
    public void setValueAreaColor(Color valueAreaColor) {
        this.valueAreaColor = valueAreaColor;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the initial balance block color.
     */
    public Color getIBColor() {
        return ibColor;
    }

    /**
     * Sets the initial balance block color.
     */
    public void setIBColor(Color ibColor) {
        this.ibColor = ibColor;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the single print highlight color.
     */
    public Color getSinglePrintColor() {
        return singlePrintColor;
    }

    /**
     * Sets the single print highlight color.
     *
     * @param singlePrintColor the color (should have alpha for translucency)
     */
    public void setSinglePrintColor(Color singlePrintColor) {
        this.singlePrintColor = singlePrintColor;
        listenerSupport.fireDataUpdated(this, -1);
    }

    // ========== VAH (Value Area High) settings ==========

    /**
     * Returns whether to show the VAH line.
     */
    public boolean isShowVAH() {
        return showVAH;
    }

    /**
     * Sets whether to show the VAH line.
     */
    public void setShowVAH(boolean showVAH) {
        this.showVAH = showVAH;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the VAH line color.
     */
    public Color getVahColor() {
        return vahColor;
    }

    /**
     * Sets the VAH line color.
     */
    public void setVahColor(Color vahColor) {
        this.vahColor = vahColor;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the VAH line type.
     */
    public LineType getVahLineType() {
        return vahLineType;
    }

    /**
     * Sets the VAH line type.
     */
    public void setVahLineType(LineType vahLineType) {
        this.vahLineType = vahLineType;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the VAH line thickness.
     */
    public LineThickness getVahLineThickness() {
        return vahLineThickness;
    }

    /**
     * Sets the VAH line thickness.
     */
    public void setVahLineThickness(LineThickness vahLineThickness) {
        this.vahLineThickness = vahLineThickness;
        listenerSupport.fireDataUpdated(this, -1);
    }

    // ========== VAL (Value Area Low) settings ==========

    /**
     * Returns whether to show the VAL line.
     */
    public boolean isShowVAL() {
        return showVAL;
    }

    /**
     * Sets whether to show the VAL line.
     */
    public void setShowVAL(boolean showVAL) {
        this.showVAL = showVAL;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the VAL line color.
     */
    public Color getValColor() {
        return valColor;
    }

    /**
     * Sets the VAL line color.
     */
    public void setValColor(Color valColor) {
        this.valColor = valColor;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the VAL line type.
     */
    public LineType getValLineType() {
        return valLineType;
    }

    /**
     * Sets the VAL line type.
     */
    public void setValLineType(LineType valLineType) {
        this.valLineType = valLineType;
        listenerSupport.fireDataUpdated(this, -1);
    }

    /**
     * Returns the VAL line thickness.
     */
    public LineThickness getValLineThickness() {
        return valLineThickness;
    }

    /**
     * Sets the VAL line thickness.
     */
    public void setValLineThickness(LineThickness valLineThickness) {
        this.valLineThickness = valLineThickness;
        listenerSupport.fireDataUpdated(this, -1);
    }

    // ========== Data access ==========

    /**
     * Returns the profile at the given index.
     */
    public TPOProfile getProfile(int index) {
        checkIndex(index);
        return profiles[index];
    }

    /**
     * Returns the profile for a specific timestamp (finds containing session).
     */
    public TPOProfile getProfileAt(long timestamp) {
        int idx = indexAtOrBefore(timestamp);
        if (idx >= 0) {
            TPOProfile profile = profiles[idx];
            if (timestamp >= profile.getSessionStart() && timestamp <= profile.getSessionEnd()) {
                return profile;
            }
        }
        return null;
    }

    // ========== Range queries ==========

    @Override
    public int indexAtOrBefore(long timestamp) {
        if (size == 0 || timestamp < sessionStarts[0]) {
            return -1;
        }
        if (timestamp >= sessionStarts[size - 1]) {
            return size - 1;
        }
        return binarySearchAtOrBefore(timestamp);
    }

    @Override
    public int indexAtOrAfter(long timestamp) {
        if (size == 0 || timestamp > profiles[size - 1].getSessionEnd()) {
            return -1;
        }
        if (timestamp <= sessionStarts[0]) {
            return 0;
        }
        return binarySearchAtOrAfter(timestamp);
    }

    private int binarySearchAtOrBefore(long timestamp) {
        int low = 0;
        int high = size - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = sessionStarts[mid];

            if (midVal <= timestamp) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    private int binarySearchAtOrAfter(long timestamp) {
        int low = 0;
        int high = size - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = sessionStarts[mid];

            if (midVal >= timestamp) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    /**
     * Finds the highest price across all profiles in range.
     */
    public float findHighestPrice(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float highest = Float.NEGATIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            float high = profiles[i].getHighOfDay();
            if (!Float.isNaN(high) && high > highest) {
                highest = high;
            }
        }
        return highest;
    }

    /**
     * Finds the lowest price across all profiles in range.
     */
    public float findLowestPrice(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        float lowest = Float.POSITIVE_INFINITY;
        for (int i = fromIndex; i <= toIndex; i++) {
            float low = profiles[i].getLowOfDay();
            if (!Float.isNaN(low) && low < lowest) {
                lowest = low;
            }
        }
        return lowest;
    }

    // ========== Mutation ==========

    /**
     * Appends a profile to the series.
     */
    public void append(TPOProfile profile) {
        if (size > 0 && profile.getSessionStart() <= sessionStarts[size - 1]) {
            throw new IllegalArgumentException(
                    "Session start must be ascending. Last: " + sessionStarts[size - 1] +
                            ", given: " + profile.getSessionStart());
        }

        ensureCapacity(size + 1);
        sessionStarts[size] = profile.getSessionStart();
        profiles[size] = profile;
        size++;

        listenerSupport.fireDataAppended(this, size - 1);
    }

    /**
     * Updates the last profile in the series.
     */
    public void updateLast(TPOProfile profile) {
        if (size == 0) {
            throw new IllegalStateException("Cannot update: series is empty");
        }
        profiles[size - 1] = profile;
        listenerSupport.fireDataUpdated(this, size - 1);
    }

    /**
     * Clears all profiles.
     */
    public void clear() {
        Arrays.fill(profiles, 0, size, null);
        size = 0;
        listenerSupport.fireDataCleared(this);
    }

    // ========== Raw array access ==========

    public long[] getSessionStartsArray() {
        return sessionStarts;
    }

    public TPOProfile[] getProfilesArray() {
        return profiles;
    }

    // ========== Internal ==========

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > sessionStarts.length) {
            int newCapacity = Math.max(minCapacity, (int) (sessionStarts.length * GROWTH_FACTOR));
            sessionStarts = Arrays.copyOf(sessionStarts, newCapacity);
            profiles = Arrays.copyOf(profiles, newCapacity);
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex >= size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                    "Invalid range [" + fromIndex + ", " + toIndex + "], Size: " + size);
        }
    }

    // ========== Listeners ==========

    @Override
    public void addListener(DataListener listener) {
        listenerSupport.addListener(listener);
    }

    @Override
    public void removeListener(DataListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public String toString() {
        return String.format("TPOSeries[id=%s, name=%s, size=%d, tickSize=%.4f]",
                id, name, size, tickSize);
    }
}
