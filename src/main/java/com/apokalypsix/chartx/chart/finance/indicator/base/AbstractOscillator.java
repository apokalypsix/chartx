package com.apokalypsix.chartx.chart.finance.indicator.base;

import com.apokalypsix.chartx.chart.data.OhlcData;

/**
 * Base class for oscillator indicators with overbought/oversold levels.
 *
 * <p>Oscillators are momentum indicators that fluctuate around a central value
 * or within a bounded range. Common examples include RSI, Stochastic, CCI, and
 * Williams %R.
 *
 * <p>This class extends {@link AbstractOhlcIndicator} with support for:
 * <ul>
 *   <li>Overbought and oversold threshold levels</li>
 *   <li>Center line value</li>
 *   <li>Bounded vs unbounded range detection</li>
 * </ul>
 */
public abstract class AbstractOscillator extends AbstractOhlcIndicator {

    private final float overboughtLevel;
    private final float oversoldLevel;
    private final float centerLine;
    private final boolean bounded;

    /**
     * Creates a bounded oscillator (e.g., RSI 0-100, Stochastic 0-100).
     *
     * @param id unique identifier
     * @param name display name
     * @param minimumBars minimum bars before valid output
     * @param overboughtLevel threshold for overbought condition
     * @param oversoldLevel threshold for oversold condition
     * @param centerLine center/neutral value
     */
    protected AbstractOscillator(String id, String name, int minimumBars,
                                  float overboughtLevel, float oversoldLevel, float centerLine) {
        super(id, name, minimumBars);
        this.overboughtLevel = overboughtLevel;
        this.oversoldLevel = oversoldLevel;
        this.centerLine = centerLine;
        this.bounded = true;
    }

    /**
     * Creates an unbounded oscillator (e.g., CCI, ROC, Momentum).
     *
     * @param id unique identifier
     * @param name display name
     * @param minimumBars minimum bars before valid output
     * @param centerLine center/zero line value
     */
    protected AbstractOscillator(String id, String name, int minimumBars, float centerLine) {
        super(id, name, minimumBars);
        this.overboughtLevel = Float.NaN;
        this.oversoldLevel = Float.NaN;
        this.centerLine = centerLine;
        this.bounded = false;
    }

    /**
     * Returns the overbought threshold level.
     * Returns Float.NaN for unbounded oscillators.
     */
    public float getOverboughtLevel() {
        return overboughtLevel;
    }

    /**
     * Returns the oversold threshold level.
     * Returns Float.NaN for unbounded oscillators.
     */
    public float getOversoldLevel() {
        return oversoldLevel;
    }

    /**
     * Returns the center/neutral line value.
     */
    public float getCenterLine() {
        return centerLine;
    }

    /**
     * Returns true if this oscillator has a bounded range.
     */
    public boolean isBounded() {
        return bounded;
    }

    /**
     * Checks if a value is in the overbought zone.
     *
     * @param value the oscillator value to check
     * @return true if overbought, false otherwise
     */
    public boolean isOverbought(float value) {
        return bounded && !Float.isNaN(value) && value >= overboughtLevel;
    }

    /**
     * Checks if a value is in the oversold zone.
     *
     * @param value the oscillator value to check
     * @return true if oversold, false otherwise
     */
    public boolean isOversold(float value) {
        return bounded && !Float.isNaN(value) && value <= oversoldLevel;
    }

    /**
     * Returns the suggested minimum Y-axis value for display.
     * Override for bounded oscillators (e.g., return 0 for RSI).
     */
    public float getSuggestedMinY() {
        return bounded ? 0 : Float.NaN;
    }

    /**
     * Returns the suggested maximum Y-axis value for display.
     * Override for bounded oscillators (e.g., return 100 for RSI).
     */
    public float getSuggestedMaxY() {
        return bounded ? 100 : Float.NaN;
    }

    /**
     * Computes the raw oscillator values.
     * Subclasses must implement this to provide the actual calculation.
     */
    @Override
    protected abstract void computeValues(OhlcData source, float[] outValues, long[] timestamps);
}
