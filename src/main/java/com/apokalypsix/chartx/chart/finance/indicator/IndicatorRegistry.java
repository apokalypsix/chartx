package com.apokalypsix.chartx.chart.finance.indicator;

import com.apokalypsix.chartx.chart.finance.indicator.impl.volume.CumulativeDeltaIndicator;
import com.apokalypsix.chartx.chart.finance.indicator.impl.volume.OBVIndicator;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.data.XyyData;

import static com.apokalypsix.chartx.chart.finance.indicator.IndicatorParameter.*;

import java.awt.Color;
import java.util.Map;

/**
 * Registry of built-in indicators with their descriptors and factories.
 *
 * <p>This class provides a centralized location for registering all standard
 * indicators with an IndicatorManager.
 */
public final class IndicatorRegistry {

    // Common parameter names
    public static final String PARAM_PERIOD = "period";
    public static final String PARAM_COLOR = "color";
    public static final String PARAM_FAST_PERIOD = "fastPeriod";
    public static final String PARAM_SLOW_PERIOD = "slowPeriod";
    public static final String PARAM_SIGNAL_PERIOD = "signalPeriod";
    public static final String PARAM_STD_DEV = "stdDev";
    public static final String PARAM_LINE_WIDTH = "lineWidth";

    // Standard colors
    public static final Color COLOR_SMA = new Color(255, 152, 0);      // Orange
    public static final Color COLOR_EMA = new Color(33, 150, 243);     // Blue
    public static final Color COLOR_VWAP = new Color(156, 39, 176);    // Purple
    public static final Color COLOR_RSI = new Color(76, 175, 80);      // Green
    public static final Color COLOR_MACD = new Color(244, 67, 54);     // Red
    public static final Color COLOR_ATR = new Color(255, 193, 7);      // Amber
    public static final Color COLOR_BB_MIDDLE = new Color(33, 150, 243);
    public static final Color COLOR_BB_BANDS = new Color(100, 181, 246);
    public static final Color COLOR_OBV = new Color(0, 188, 212);         // Cyan
    public static final Color COLOR_CVD = new Color(139, 195, 74);        // Light Green

    private IndicatorRegistry() {}

    /**
     * Registers all built-in indicators with the manager.
     *
     * @param manager the indicator manager to register with
     */
    public static void registerBuiltInIndicators(IndicatorManager manager) {
        registerSMA(manager);
        registerEMA(manager);
        registerVWAP(manager);
        registerRSI(manager);
        registerMACD(manager);
        registerATR(manager);
        registerStochastic(manager);
        registerBollingerBands(manager);
        registerOBV(manager);
        registerCVD(manager);
    }

    // ========== SMA ==========

    private static void registerSMA(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "sma",
                "Simple Moving Average",
                "Calculates the arithmetic mean of prices over a specified period",
                IndicatorCategory.TREND,
                true  // overlay on price
        )
        .addParameter(new IntParam(PARAM_PERIOD, "Period", 20, 1, 500))
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_SMA));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int period = (Integer) params.getOrDefault(PARAM_PERIOD, 20);
            return new SMAIndicator(period);
        });
    }

    // ========== EMA ==========

    private static void registerEMA(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "ema",
                "Exponential Moving Average",
                "Calculates exponentially weighted moving average giving more weight to recent prices",
                IndicatorCategory.TREND,
                true
        )
        .addParameter(new IntParam(PARAM_PERIOD, "Period", 20, 1, 500))
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_EMA));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int period = (Integer) params.getOrDefault(PARAM_PERIOD, 20);
            return new EMAIndicator(period);
        });
    }

    // ========== VWAP ==========

    private static void registerVWAP(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "vwap",
                "Volume Weighted Average Price",
                "Calculates the average price weighted by volume",
                IndicatorCategory.VOLUME,
                true
        )
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_VWAP));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> new VWAPIndicator());
    }

    // ========== RSI ==========

    private static void registerRSI(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "rsi",
                "Relative Strength Index",
                "Momentum oscillator measuring speed and magnitude of price changes",
                IndicatorCategory.MOMENTUM,
                false  // separate pane
        )
        .addParameter(new IntParam(PARAM_PERIOD, "Period", 14, 2, 100))
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_RSI));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int period = (Integer) params.getOrDefault(PARAM_PERIOD, 14);
            return new RSIIndicator(period);
        });
    }

    // ========== MACD ==========

    private static void registerMACD(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "macd",
                "MACD",
                "Moving Average Convergence Divergence - trend-following momentum indicator",
                IndicatorCategory.MOMENTUM,
                false
        )
        .addParameter(new IntParam(PARAM_FAST_PERIOD, "Fast Period", 12, 1, 100))
        .addParameter(new IntParam(PARAM_SLOW_PERIOD, "Slow Period", 26, 1, 200))
        .addParameter(new IntParam(PARAM_SIGNAL_PERIOD, "Signal Period", 9, 1, 50))
        .addParameter(new ColorParam(PARAM_COLOR, "MACD Color", COLOR_MACD));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int fastPeriod = (Integer) params.getOrDefault(PARAM_FAST_PERIOD, 12);
            int slowPeriod = (Integer) params.getOrDefault(PARAM_SLOW_PERIOD, 26);
            int signalPeriod = (Integer) params.getOrDefault(PARAM_SIGNAL_PERIOD, 9);
            return new MACDIndicator(fastPeriod, slowPeriod, signalPeriod);
        });
    }

    // ========== ATR ==========

    private static void registerATR(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "atr",
                "Average True Range",
                "Measures market volatility by calculating average of true ranges",
                IndicatorCategory.VOLATILITY,
                false
        )
        .addParameter(new IntParam(PARAM_PERIOD, "Period", 14, 1, 100))
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_ATR));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int period = (Integer) params.getOrDefault(PARAM_PERIOD, 14);
            return new ATRIndicator(period);
        });
    }

    // ========== Stochastic ==========

    private static void registerStochastic(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "stochastic",
                "Stochastic Oscillator",
                "Momentum indicator comparing closing price to price range over a period",
                IndicatorCategory.MOMENTUM,
                false
        )
        .addParameter(new IntParam("kPeriod", "%K Period", 14, 1, 100))
        .addParameter(new IntParam("dPeriod", "%D Period", 3, 1, 50))
        .addParameter(new IntParam("smooth", "Smoothing", 3, 1, 20))
        .addParameter(new ColorParam("kColor", "%K Color", new Color(33, 150, 243)))
        .addParameter(new ColorParam("dColor", "%D Color", new Color(255, 152, 0)));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int kPeriod = (Integer) params.getOrDefault("kPeriod", 14);
            int dPeriod = (Integer) params.getOrDefault("dPeriod", 3);
            int smooth = (Integer) params.getOrDefault("smooth", 3);
            return new StochasticIndicator(kPeriod, dPeriod, smooth);
        });
    }

    // ========== Bollinger Bands ==========

    private static void registerBollingerBands(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "bollinger",
                "Bollinger Bands",
                "Volatility bands placed above and below a moving average",
                IndicatorCategory.VOLATILITY,
                true
        )
        .addParameter(new IntParam(PARAM_PERIOD, "Period", 20, 2, 200))
        .addParameter(new DoubleParam(PARAM_STD_DEV, "Std Dev", 2.0, 0.5, 5.0))
        .addParameter(new ColorParam(PARAM_COLOR, "Band Color", COLOR_BB_BANDS));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> {
            int period = (Integer) params.getOrDefault(PARAM_PERIOD, 20);
            double stdDev = (Double) params.getOrDefault(PARAM_STD_DEV, 2.0);
            return new BollingerBandsIndicator(period, stdDev);
        });
    }

    // ========== OBV ==========

    private static void registerOBV(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "obv",
                "On-Balance Volume",
                "Cumulative indicator relating volume to price change direction",
                IndicatorCategory.VOLUME,
                false  // separate pane
        )
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_OBV));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> new OBVIndicator());
    }

    // ========== CVD (Cumulative Volume Delta) ==========

    private static void registerCVD(IndicatorManager manager) {
        IndicatorDescriptor descriptor = new IndicatorDescriptor(
                "cvd",
                "Cumulative Volume Delta",
                "Running sum of volume delta showing cumulative buying/selling pressure",
                IndicatorCategory.VOLUME,
                false  // separate pane
        )
        .addParameter(new ColorParam(PARAM_COLOR, "Color", COLOR_CVD));

        manager.registerIndicator(descriptor, (Map<String, Object> params) -> new CumulativeDeltaIndicator());
    }

    // ========== Indicator Implementations ==========

    /**
     * SMA indicator implementation conforming to Indicator interface.
     */
    private static class SMAIndicator implements Indicator<OhlcData, XyData> {
        private final int period;

        SMAIndicator(int period) {
            this.period = period;
        }

        @Override
        public String getName() {
            return "SMA(" + period + ")";
        }

        @Override
        public int getMinimumBars() {
            return period;
        }

        @Override
        public XyData calculate(OhlcData source) {
            return SMA.calculate(source, period);
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            SMA.update(result, source, period);
        }
    }

    /**
     * EMA indicator implementation.
     */
    private static class EMAIndicator implements Indicator<OhlcData, XyData> {
        private final int period;

        EMAIndicator(int period) {
            this.period = period;
        }

        @Override
        public String getName() {
            return "EMA(" + period + ")";
        }

        @Override
        public int getMinimumBars() {
            return period;
        }

        @Override
        public XyData calculate(OhlcData source) {
            return EMA.calculate(source, period);
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            EMA.update(result, source, period);
        }
    }

    /**
     * VWAP indicator implementation.
     */
    private static class VWAPIndicator implements Indicator<OhlcData, XyData> {

        @Override
        public String getName() {
            return "VWAP";
        }

        @Override
        public int getMinimumBars() {
            return 1;
        }

        @Override
        public XyData calculate(OhlcData source) {
            return VWAP.calculate(source);
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            // VWAP doesn't have incremental update, recalculate
            XyData recalculated = VWAP.calculate(source);
            for (int i = result.size(); i < recalculated.size(); i++) {
                result.append(recalculated.getXValue(i), recalculated.getValue(i));
            }
        }
    }

    /**
     * RSI indicator implementation.
     */
    private static class RSIIndicator implements Indicator<OhlcData, XyData> {
        private final int period;

        RSIIndicator(int period) {
            this.period = period;
        }

        @Override
        public String getName() {
            return "RSI(" + period + ")";
        }

        @Override
        public int getMinimumBars() {
            return period + 1;
        }

        @Override
        public XyData calculate(OhlcData source) {
            return RSI.calculate(source, period);
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            RSI.update(result, source, period);
        }
    }

    /**
     * MACD indicator implementation.
     */
    private static class MACDIndicator implements Indicator<OhlcData, XyData> {
        private final int fastPeriod;
        private final int slowPeriod;
        private final int signalPeriod;

        MACDIndicator(int fastPeriod, int slowPeriod, int signalPeriod) {
            this.fastPeriod = fastPeriod;
            this.slowPeriod = slowPeriod;
            this.signalPeriod = signalPeriod;
        }

        @Override
        public String getName() {
            return String.format("MACD(%d,%d,%d)", fastPeriod, slowPeriod, signalPeriod);
        }

        @Override
        public int getMinimumBars() {
            return slowPeriod + signalPeriod;
        }

        @Override
        public XyData calculate(OhlcData source) {
            MACD.Result result = MACD.calculate(source, fastPeriod, slowPeriod, signalPeriod);
            return result.macdLine;
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            // MACD update is complex, recalculate for now
            MACD.Result macdResult = MACD.calculate(source, fastPeriod, slowPeriod, signalPeriod);
            XyData recalculated = macdResult.macdLine;
            // Copy new values
            for (int i = result.size(); i < recalculated.size(); i++) {
                result.append(recalculated.getXValue(i), recalculated.getValue(i));
            }
        }
    }

    /**
     * ATR indicator implementation.
     */
    private static class ATRIndicator implements Indicator<OhlcData, XyData> {
        private final int period;

        ATRIndicator(int period) {
            this.period = period;
        }

        @Override
        public String getName() {
            return "ATR(" + period + ")";
        }

        @Override
        public int getMinimumBars() {
            return period + 1;
        }

        @Override
        public XyData calculate(OhlcData source) {
            return ATR.calculate(source, period);
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            ATR.update(result, source, period);
        }
    }

    /**
     * Stochastic indicator implementation.
     */
    private static class StochasticIndicator implements Indicator<OhlcData, XyData> {
        private final int kPeriod;
        private final int dPeriod;
        private final int smooth;

        StochasticIndicator(int kPeriod, int dPeriod, int smooth) {
            this.kPeriod = kPeriod;
            this.dPeriod = dPeriod;
            this.smooth = smooth;
        }

        @Override
        public String getName() {
            return String.format("Stoch(%d,%d,%d)", kPeriod, dPeriod, smooth);
        }

        @Override
        public int getMinimumBars() {
            return kPeriod + dPeriod + smooth;
        }

        @Override
        public XyData calculate(OhlcData source) {
            Stochastic.Result result = Stochastic.calculate(source, kPeriod, dPeriod, smooth);
            return result.kLine;
        }

        @Override
        public void update(XyData result, OhlcData source, int fromIndex) {
            // Stochastic update is complex, recalculate for now
            Stochastic.Result stochResult = Stochastic.calculate(source, kPeriod, dPeriod, smooth);
            XyData recalculated = stochResult.kLine;
            for (int i = result.size(); i < recalculated.size(); i++) {
                result.append(recalculated.getXValue(i), recalculated.getValue(i));
            }
        }
    }

    /**
     * Bollinger Bands indicator implementation.
     */
    private static class BollingerBandsIndicator implements Indicator<OhlcData, XyyData> {
        private final int period;
        private final double stdDev;

        BollingerBandsIndicator(int period, double stdDev) {
            this.period = period;
            this.stdDev = stdDev;
        }

        @Override
        public String getName() {
            return String.format("BB(%d, %.1f)", period, stdDev);
        }

        @Override
        public int getMinimumBars() {
            return period;
        }

        @Override
        public XyyData calculate(OhlcData source) {
            return BollingerBands.calculate(source, period, (float) stdDev);
        }

        @Override
        public void update(XyyData result, OhlcData source, int fromIndex) {
            BollingerBands.update(result, source, period, (float) stdDev);
        }
    }
}
