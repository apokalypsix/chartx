package com.apokalypsix.chartx.examples.library;

import java.awt.*;

/**
 * Configuration constants for demo applications.
 *
 * <p>Centralizes common colors, sizes, and other constants used across demos
 * for consistent look and feel.
 */
public final class DemoConfig {

    private DemoConfig() {
        // Constants class
    }

    // ========== Colors ==========

    /** Background color for control panels */
    public static final Color PANEL_BACKGROUND = new Color(30, 32, 36);

    /** Foreground color for labels */
    public static final Color LABEL_FOREGROUND = Color.GRAY;

    /** Foreground color for controls (checkboxes, etc.) */
    public static final Color CONTROL_FOREGROUND = Color.LIGHT_GRAY;

    /** Default bullish (positive) color */
    public static final Color BULLISH_COLOR = new Color(38, 166, 91);

    /** Default bearish (negative) color */
    public static final Color BEARISH_COLOR = new Color(214, 69, 65);

    /** Bullish color with transparency for overlays */
    public static final Color BULLISH_COLOR_ALPHA = new Color(38, 166, 91, 180);

    /** Bearish color with transparency for overlays */
    public static final Color BEARISH_COLOR_ALPHA = new Color(214, 69, 65, 180);

    /** EMA 9 indicator color (Yellow) */
    public static final Color EMA_9_COLOR = new Color(255, 193, 7);

    /** EMA 20 indicator color (Blue) */
    public static final Color EMA_20_COLOR = new Color(33, 150, 243);

    /** EMA 50 indicator color (Purple) */
    public static final Color EMA_50_COLOR = new Color(156, 39, 176);

    /** VWAP indicator color (Orange) */
    public static final Color VWAP_COLOR = new Color(255, 152, 0);

    /** Bollinger Bands color */
    public static final Color BOLLINGER_COLOR = new Color(100, 149, 237);

    // ========== Window Sizes ==========

    /** Default demo window width */
    public static final int DEFAULT_WIDTH = 1200;

    /** Default demo window height */
    public static final int DEFAULT_HEIGHT = 800;

    /** Wide demo window width */
    public static final int WIDE_WIDTH = 1400;

    /** Tall demo window height */
    public static final int TALL_HEIGHT = 1000;

    // ========== Data Generation ==========

    /** Default number of bars for demo data */
    public static final int DEFAULT_BAR_COUNT = 500;

    /** Default bar duration in milliseconds (1 minute) */
    public static final long DEFAULT_BAR_DURATION = 60000;

    /** Default starting price for generated data */
    public static final float DEFAULT_START_PRICE = 4500f;

    /** Default volatility for generated data */
    public static final float DEFAULT_VOLATILITY = 5f;

    /** Random seed for reproducible data generation */
    public static final int DEFAULT_RANDOM_SEED = 42;

    // ========== Line Widths ==========

    /** Standard line width */
    public static final float LINE_WIDTH_STANDARD = 1.5f;

    /** Thick line width */
    public static final float LINE_WIDTH_THICK = 2.0f;

    /** Thin line width */
    public static final float LINE_WIDTH_THIN = 1.0f;

    // ========== Pane Heights ==========

    /** Standard price pane height ratio */
    public static final double PRICE_PANE_RATIO = 0.7;

    /** Standard volume pane height ratio */
    public static final double VOLUME_PANE_RATIO = 0.3;

    /** Standard indicator pane height ratio */
    public static final double INDICATOR_PANE_RATIO = 0.15;

    // ========== Histogram Settings ==========

    /** Default histogram bar width ratio */
    public static final float HISTOGRAM_BAR_WIDTH = 0.7f;

    /** Default histogram opacity */
    public static final float HISTOGRAM_OPACITY = 0.5f;
}
