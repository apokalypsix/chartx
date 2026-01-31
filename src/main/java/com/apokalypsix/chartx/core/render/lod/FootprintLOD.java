package com.apokalypsix.chartx.core.render.lod;

/**
 * Level-of-Detail controller for footprint chart rendering.
 *
 * <p>Footprint charts require different rendering strategies based on zoom level:
 * <ul>
 *   <li>FULL: Show "bid x ask" text at each price level (requires wide bars)</li>
 *   <li>DETAILED: Show volume bars per level (medium zoom)</li>
 *   <li>SIMPLIFIED: Show single delta bar per level (narrow bars)</li>
 *   <li>MINIMAL: Fall back to candlestick rendering (very zoomed out)</li>
 * </ul>
 *
 * <p>The LOD level is determined by pixels per bar, ensuring the chart
 * remains readable at all zoom levels.
 */
public class FootprintLOD {

    /**
     * Level-of-detail rendering modes.
     */
    public enum Level {
        /**
         * Full detail: show "bid x ask" text at each price level.
         * Requires at least 80 pixels per bar width.
         */
        FULL(80),

        /**
         * Detailed: show volume bars at each price level.
         * Used when bars are 30-80 pixels wide.
         */
        DETAILED(30),

        /**
         * Simplified: show single delta bar per price level.
         * Used when bars are 15-30 pixels wide.
         */
        SIMPLIFIED(15),

        /**
         * Minimal: fall back to standard candlestick rendering.
         * Used when bars are less than 15 pixels wide.
         */
        MINIMAL(0);

        /** Minimum pixels per bar required for this level */
        public final int minPixelsPerBar;

        Level(int minPixelsPerBar) {
            this.minPixelsPerBar = minPixelsPerBar;
        }

        /**
         * Returns true if text rendering is needed at this level.
         */
        public boolean requiresTextRendering() {
            return this == FULL;
        }

        /**
         * Returns true if per-level bars should be rendered.
         */
        public boolean showsLevelBars() {
            return this == DETAILED || this == SIMPLIFIED;
        }

        /**
         * Returns true if this level should show individual price levels.
         */
        public boolean showsPriceLevels() {
            return this != MINIMAL;
        }
    }

    /** Minimum pixels per tick (vertical) required for text rendering */
    private static final double MIN_PIXELS_PER_TICK_FOR_TEXT = 12.0;

    /** Minimum pixels per tick (vertical) required for per-level bars */
    private static final double MIN_PIXELS_PER_TICK_FOR_LEVELS = 4.0;

    // Cached state
    private Level currentLevel = Level.MINIMAL;
    private double lastPixelsPerBar = -1;
    private double lastPixelsPerTick = -1;

    // Configuration
    private boolean enabled = true;
    private Level minimumLevel = Level.MINIMAL;
    private Level maximumLevel = Level.FULL;

    /**
     * Determines the appropriate LOD level for the given zoom.
     *
     * @param pixelsPerBar the current pixels per bar
     * @return the appropriate LOD level
     */
    public Level getLevelForZoom(double pixelsPerBar) {
        if (!enabled) {
            return Level.FULL;
        }

        Level level;
        if (pixelsPerBar >= Level.FULL.minPixelsPerBar) {
            level = Level.FULL;
        } else if (pixelsPerBar >= Level.DETAILED.minPixelsPerBar) {
            level = Level.DETAILED;
        } else if (pixelsPerBar >= Level.SIMPLIFIED.minPixelsPerBar) {
            level = Level.SIMPLIFIED;
        } else {
            level = Level.MINIMAL;
        }

        // Apply constraints
        if (level.ordinal() < minimumLevel.ordinal()) {
            level = minimumLevel;
        }
        if (level.ordinal() > maximumLevel.ordinal()) {
            level = maximumLevel;
        }

        return level;
    }

    /**
     * Updates the current LOD level and returns true if it changed.
     *
     * @param pixelsPerBar the current pixels per bar
     * @return true if the level changed
     */
    public boolean updateLevel(double pixelsPerBar) {
        return updateLevel(pixelsPerBar, -1);
    }

    /**
     * Updates the current LOD level considering both horizontal and vertical space.
     *
     * @param pixelsPerBar the current pixels per bar (horizontal)
     * @param pixelsPerTick the current pixels per tick (vertical), or -1 if unknown
     * @return true if the level changed
     */
    public boolean updateLevel(double pixelsPerBar, double pixelsPerTick) {
        Level newLevel = getLevelForZoom(pixelsPerBar);

        // Apply vertical constraints if pixelsPerTick is known
        if (pixelsPerTick > 0) {
            // Downgrade to DETAILED if not enough vertical space for text
            if (newLevel == Level.FULL && pixelsPerTick < MIN_PIXELS_PER_TICK_FOR_TEXT) {
                newLevel = Level.DETAILED;
            }
            // Downgrade to MINIMAL if not enough vertical space for level bars
            if ((newLevel == Level.DETAILED || newLevel == Level.SIMPLIFIED)
                    && pixelsPerTick < MIN_PIXELS_PER_TICK_FOR_LEVELS) {
                newLevel = Level.MINIMAL;
            }
        }

        boolean changed = (newLevel != currentLevel);
        currentLevel = newLevel;
        lastPixelsPerBar = pixelsPerBar;
        lastPixelsPerTick = pixelsPerTick;
        return changed;
    }

    /**
     * Returns the current LOD level.
     */
    public Level getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Returns the last computed pixels per bar.
     */
    public double getLastPixelsPerBar() {
        return lastPixelsPerBar;
    }

    /**
     * Returns the last computed pixels per tick.
     */
    public double getLastPixelsPerTick() {
        return lastPixelsPerTick;
    }

    /**
     * Enables or disables LOD optimization.
     * When disabled, always uses FULL level.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the minimum LOD level (most zoomed out allowed).
     */
    public void setMinimumLevel(Level level) {
        this.minimumLevel = level;
    }

    public Level getMinimumLevel() {
        return minimumLevel;
    }

    /**
     * Sets the maximum LOD level (most detailed allowed).
     */
    public void setMaximumLevel(Level level) {
        this.maximumLevel = level;
    }

    public Level getMaximumLevel() {
        return maximumLevel;
    }

    /**
     * Calculates the recommended font size for the current zoom level.
     *
     * @param pixelsPerBar current pixels per bar
     * @param pixelsPerTick current pixels per tick (vertical)
     * @return recommended font size in pixels
     */
    public float getRecommendedFontSize(double pixelsPerBar, double pixelsPerTick) {
        // Font should fit within the available space
        float maxWidth = (float) pixelsPerBar * 0.4f; // 40% of bar width per side
        float maxHeight = (float) pixelsPerTick * 0.8f; // 80% of tick height

        // Typical character aspect ratio is about 0.6 (width/height)
        // For "XXX x XXX" format, we need about 9-11 characters
        float widthBasedSize = maxWidth / (10 * 0.6f);
        float heightBasedSize = maxHeight;

        float size = Math.min(widthBasedSize, heightBasedSize);

        // Clamp to reasonable range
        return Math.max(6, Math.min(14, size));
    }

    /**
     * Determines if imbalance highlighting should be shown at current zoom.
     */
    public boolean shouldShowImbalances() {
        return currentLevel != Level.MINIMAL;
    }

    /**
     * Determines if POC (point of control) should be highlighted.
     */
    public boolean shouldShowPOC() {
        return currentLevel != Level.MINIMAL;
    }

    @Override
    public String toString() {
        return String.format("FootprintLOD[level=%s, pxBar=%.1f, pxTick=%.1f, enabled=%b]",
                currentLevel, lastPixelsPerBar, lastPixelsPerTick, enabled);
    }
}
