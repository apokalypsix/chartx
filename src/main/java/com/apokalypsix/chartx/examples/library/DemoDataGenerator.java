package com.apokalypsix.chartx.examples.library;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

/**
 * Utility class for generating sample data in demo applications.
 *
 * <p>Provides methods for creating realistic OHLC price data, volume data,
 * and other series types commonly used in financial charting demos.
 */
public final class DemoDataGenerator {

    private DemoDataGenerator() {
        // Utility class
    }

    /**
     * Generates sample OHLC data with default parameters.
     *
     * @return OhlcData with 500 bars of demo data
     */
    public static OhlcData generateOHLCData() {
        return generateOHLCData(
                DemoConfig.DEFAULT_BAR_COUNT,
                DemoConfig.DEFAULT_START_PRICE,
                DemoConfig.DEFAULT_VOLATILITY,
                DemoConfig.DEFAULT_BAR_DURATION,
                DemoConfig.DEFAULT_RANDOM_SEED
        );
    }

    /**
     * Generates sample OHLC data with custom parameters.
     *
     * @param barCount number of bars to generate
     * @param startPrice initial price
     * @param volatility price volatility factor
     * @param barDuration bar duration in milliseconds
     * @param randomSeed seed for random number generator (for reproducibility)
     * @return OhlcData with generated data
     */
    public static OhlcData generateOHLCData(int barCount, float startPrice,
                                             float volatility, long barDuration, int randomSeed) {
        OhlcData data = new OhlcData("demo", "Demo Data", barCount);

        Random random = new Random(randomSeed);
        long startTime = System.currentTimeMillis() - (barCount * barDuration);

        float price = startPrice;
        float currentVolatility = volatility;

        for (int i = 0; i < barCount; i++) {
            long timestamp = startTime + (i * barDuration);

            // Generate realistic OHLC data with slight upward bias
            float change = (random.nextFloat() - 0.48f) * currentVolatility;
            float open = price;
            float close = price + change;

            // High is above both open and close
            float wickUp = random.nextFloat() * currentVolatility * 0.5f;
            float high = Math.max(open, close) + wickUp;

            // Low is below both open and close
            float wickDown = random.nextFloat() * currentVolatility * 0.5f;
            float low = Math.min(open, close) - wickDown;

            // Volume with variation
            float volume = 1000 + random.nextFloat() * 9000;

            data.append(timestamp, open, high, low, close, volume);

            // Update price for next bar
            price = close;

            // Occasionally add volatility spikes
            if (random.nextFloat() < 0.05f) {
                currentVolatility = volatility * 1.5f + random.nextFloat() * volatility * 2f;
            } else {
                currentVolatility = Math.max(volatility * 0.6f, currentVolatility * 0.95f);
            }
        }

        return data;
    }

    /**
     * Generates volume histogram data from OHLC data.
     * Volume bars are colored based on price direction (bullish/bearish).
     *
     * @param ohlcData source OHLC data
     * @return HistogramData with directional volume
     */
    public static HistogramData generateVolumeData(OhlcData ohlcData) {
        HistogramData volume = new HistogramData("volume", "Volume", ohlcData.size());

        long[] timestamps = ohlcData.getTimestampsArray();
        float[] opens = ohlcData.getOpenArray();
        float[] closes = ohlcData.getCloseArray();
        float[] volumes = ohlcData.getVolumeArray();

        for (int i = 0; i < ohlcData.size(); i++) {
            // Use positive volume for bullish candles, negative for bearish
            // This allows the histogram to color bars by direction
            boolean bullish = closes[i] >= opens[i];
            float value = bullish ? volumes[i] : -volumes[i];
            volume.append(timestamps[i], value);
        }

        return volume;
    }

    /**
     * Generates simple volume data (all positive values).
     *
     * @param ohlcData source OHLC data
     * @return HistogramData with volume values
     */
    public static HistogramData generateSimpleVolumeData(OhlcData ohlcData) {
        HistogramData volume = new HistogramData("volume", "Volume", ohlcData.size());

        long[] timestamps = ohlcData.getTimestampsArray();
        float[] volumes = ohlcData.getVolumeArray();

        for (int i = 0; i < ohlcData.size(); i++) {
            volume.append(timestamps[i], volumes[i]);
        }

        return volume;
    }

    /**
     * Converts XyData to HistogramData for display in indicator panes.
     *
     * @param line source XY data
     * @param centerValue value to center around (e.g., 50 for RSI, 0 for MACD)
     * @return HistogramData centered around the specified value
     */
    public static HistogramData xyToHistogram(XyData line, float centerValue) {
        HistogramData hist = new HistogramData(
                line.getId() + "_hist",
                line.getName(),
                line.size()
        );

        long[] timestamps = line.getTimestampsArray();
        float[] values = line.getValuesArray();

        for (int i = 0; i < line.size(); i++) {
            if (!Float.isNaN(values[i])) {
                hist.append(timestamps[i], values[i] - centerValue);
            } else {
                hist.append(timestamps[i], 0);
            }
        }

        return hist;
    }

    /**
     * Converts XyData to HistogramData centered at zero.
     *
     * @param line source XY data
     * @return HistogramData
     */
    public static HistogramData xyToHistogram(XyData line) {
        return xyToHistogram(line, 0f);
    }

    /**
     * Generates random walk price data for simpler demonstrations.
     *
     * @param barCount number of bars
     * @param startPrice initial price
     * @param step maximum price step per bar
     * @return OhlcData with random walk data
     */
    public static OhlcData generateRandomWalk(int barCount, float startPrice, float step) {
        OhlcData data = new OhlcData("random_walk", "Random Walk", barCount);

        Random random = new Random(DemoConfig.DEFAULT_RANDOM_SEED);
        long startTime = System.currentTimeMillis() - (barCount * DemoConfig.DEFAULT_BAR_DURATION);

        float price = startPrice;

        for (int i = 0; i < barCount; i++) {
            long timestamp = startTime + (i * DemoConfig.DEFAULT_BAR_DURATION);

            float change = (random.nextFloat() - 0.5f) * step * 2;
            float open = price;
            float close = price + change;
            float high = Math.max(open, close) + random.nextFloat() * step * 0.5f;
            float low = Math.min(open, close) - random.nextFloat() * step * 0.5f;
            float volume = 1000 + random.nextFloat() * 9000;

            data.append(timestamp, open, high, low, close, volume);
            price = close;
        }

        return data;
    }

    /**
     * Calculates a simple moving average of the close prices.
     *
     * @param ohlcData source OHLC data
     * @param period SMA period
     * @return XyData with SMA values
     */
    public static XyData calculateSMA(OhlcData ohlcData, int period) {
        XyData sma = new XyData("sma_" + period, "SMA " + period, ohlcData.size());

        long[] timestamps = ohlcData.getTimestampsArray();
        float[] closes = ohlcData.getCloseArray();

        float sum = 0;
        for (int i = 0; i < ohlcData.size(); i++) {
            sum += closes[i];

            if (i >= period) {
                sum -= closes[i - period];
            }

            if (i >= period - 1) {
                sma.append(timestamps[i], sum / period);
            } else {
                sma.append(timestamps[i], Float.NaN);
            }
        }

        return sma;
    }

    /**
     * Calculates an exponential moving average of the close prices.
     *
     * @param ohlcData source OHLC data
     * @param period EMA period
     * @return XyData with EMA values
     */
    public static XyData calculateEMA(OhlcData ohlcData, int period) {
        XyData ema = new XyData("ema_" + period, "EMA " + period, ohlcData.size());

        long[] timestamps = ohlcData.getTimestampsArray();
        float[] closes = ohlcData.getCloseArray();

        float multiplier = 2.0f / (period + 1);
        float emaValue = 0;

        for (int i = 0; i < ohlcData.size(); i++) {
            if (i == 0) {
                emaValue = closes[i];
            } else {
                emaValue = (closes[i] - emaValue) * multiplier + emaValue;
            }

            if (i >= period - 1) {
                ema.append(timestamps[i], emaValue);
            } else {
                ema.append(timestamps[i], Float.NaN);
            }
        }

        return ema;
    }

    /**
     * Generates intraday OHLC data for multiple trading sessions.
     * Data is generated only during trading hours (e.g., 9:30 AM - 4:00 PM ET),
     * skipping overnight hours. Each trading day produces data suitable for
     * a single TPO profile.
     *
     * @param tradingDays number of trading days to generate
     * @param barDurationMinutes bar duration in minutes (e.g., 5 or 30)
     * @param startPrice initial price
     * @param volatility price volatility factor
     * @param sessionStartHour session start hour (0-23)
     * @param sessionStartMinute session start minute (0-59)
     * @param sessionEndHour session end hour (0-23)
     * @param sessionEndMinute session end minute (0-59)
     * @param randomSeed seed for reproducibility
     * @return OhlcData with intraday session data
     */
    public static OhlcData generateIntradaySessionData(
            int tradingDays,
            int barDurationMinutes,
            float startPrice,
            float volatility,
            int sessionStartHour,
            int sessionStartMinute,
            int sessionEndHour,
            int sessionEndMinute,
            int randomSeed) {

        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(tz);

        // Calculate session duration in minutes
        int startMinutes = sessionStartHour * 60 + sessionStartMinute;
        int endMinutes = sessionEndHour * 60 + sessionEndMinute;
        int sessionMinutes = endMinutes - startMinutes;
        int barsPerSession = sessionMinutes / barDurationMinutes;

        int totalBars = tradingDays * barsPerSession;
        OhlcData data = new OhlcData("intraday", "Intraday Session Data", totalBars);

        Random random = new Random(randomSeed);
        long barDurationMillis = barDurationMinutes * 60 * 1000L;

        // Start from tradingDays ago, find a weekday
        cal.add(Calendar.DAY_OF_MONTH, -tradingDays - 5); // Extra buffer for weekends

        float price = startPrice;
        float currentVolatility = volatility;
        int daysGenerated = 0;

        while (daysGenerated < tradingDays) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            // Skip weekends
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                continue;
            }

            // Set to session start time
            cal.set(Calendar.HOUR_OF_DAY, sessionStartHour);
            cal.set(Calendar.MINUTE, sessionStartMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long sessionStartTime = cal.getTimeInMillis();

            // Add overnight gap (random but small, simulating opening gap)
            float overnightGap = (random.nextFloat() - 0.5f) * volatility * 2f;
            price += overnightGap;

            // Generate bars for this session
            for (int bar = 0; bar < barsPerSession; bar++) {
                long timestamp = sessionStartTime + (bar * barDurationMillis);

                // Generate realistic OHLC data
                float change = (random.nextFloat() - 0.48f) * currentVolatility;
                float open = price;
                float close = price + change;

                float wickUp = random.nextFloat() * currentVolatility * 0.5f;
                float high = Math.max(open, close) + wickUp;

                float wickDown = random.nextFloat() * currentVolatility * 0.5f;
                float low = Math.min(open, close) - wickDown;

                float volume = 1000 + random.nextFloat() * 9000;

                data.append(timestamp, open, high, low, close, volume);

                price = close;

                // Occasionally add volatility spikes
                if (random.nextFloat() < 0.05f) {
                    currentVolatility = volatility * 1.5f + random.nextFloat() * volatility * 2f;
                } else {
                    currentVolatility = Math.max(volatility * 0.6f, currentVolatility * 0.95f);
                }
            }

            daysGenerated++;
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return data;
    }

    /**
     * Generates intraday OHLC data with default US market session hours (9:30 AM - 4:00 PM ET).
     *
     * @param tradingDays number of trading days to generate
     * @param barDurationMinutes bar duration in minutes
     * @param startPrice initial price
     * @param volatility price volatility factor
     * @param randomSeed seed for reproducibility
     * @return OhlcData with intraday session data
     */
    public static OhlcData generateIntradaySessionData(
            int tradingDays,
            int barDurationMinutes,
            float startPrice,
            float volatility,
            int randomSeed) {
        return generateIntradaySessionData(
                tradingDays,
                barDurationMinutes,
                startPrice,
                volatility,
                9, 30,   // 9:30 AM
                16, 0,   // 4:00 PM
                randomSeed
        );
    }

    /**
     * Generates continuous 24/7 OHLC data suitable for cryptocurrency markets.
     *
     * <p>Unlike {@link #generateIntradaySessionData}, this method generates data
     * for all hours without any gaps for weekends or overnight periods. Each day
     * has 24 hours of data, matching how crypto markets operate.
     *
     * <p>Data is generated in UTC timezone.
     *
     * @param days number of calendar days to generate
     * @param barDurationMinutes bar duration in minutes (e.g., 5, 15, 30)
     * @param startPrice initial price
     * @param volatility price volatility factor
     * @param randomSeed seed for reproducibility
     * @return OhlcData with continuous 24/7 data
     */
    public static OhlcData generate24hCryptoData(
            int days,
            int barDurationMinutes,
            float startPrice,
            float volatility,
            int randomSeed) {

        TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(tz);

        // Calculate bars per day (24 hours)
        int barsPerDay = (24 * 60) / barDurationMinutes;
        int totalBars = days * barsPerDay;

        OhlcData data = new OhlcData("crypto", "Crypto 24h Data", totalBars);

        Random random = new Random(randomSeed);
        long barDurationMillis = barDurationMinutes * 60 * 1000L;

        // Start from 'days' ago at midnight UTC
        cal.add(Calendar.DAY_OF_MONTH, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long startTime = cal.getTimeInMillis();

        float price = startPrice;
        float currentVolatility = volatility;

        for (int i = 0; i < totalBars; i++) {
            long timestamp = startTime + (i * barDurationMillis);

            // Generate realistic OHLC data with slight upward bias (typical for crypto)
            float change = (random.nextFloat() - 0.48f) * currentVolatility;
            float open = price;
            float close = price + change;

            float wickUp = random.nextFloat() * currentVolatility * 0.5f;
            float high = Math.max(open, close) + wickUp;

            float wickDown = random.nextFloat() * currentVolatility * 0.5f;
            float low = Math.min(open, close) - wickDown;

            // Volume varies more in crypto (24/7 with different time zone activity)
            float volume = 500 + random.nextFloat() * 15000;

            data.append(timestamp, open, high, low, close, volume);

            price = close;

            // Crypto markets have more frequent volatility spikes
            if (random.nextFloat() < 0.08f) {
                currentVolatility = volatility * 2f + random.nextFloat() * volatility * 3f;
            } else {
                currentVolatility = Math.max(volatility * 0.5f, currentVolatility * 0.92f);
            }
        }

        return data;
    }

    /**
     * Generates continuous 24/7 OHLC data with default parameters.
     *
     * @param days number of calendar days to generate
     * @return OhlcData with continuous 24/7 data
     */
    public static OhlcData generate24hCryptoData(int days) {
        return generate24hCryptoData(
                days,
                5,                              // 5-minute bars
                DemoConfig.DEFAULT_START_PRICE,
                DemoConfig.DEFAULT_VOLATILITY,
                DemoConfig.DEFAULT_RANDOM_SEED
        );
    }

    /**
     * Generates TPO-optimized 24/7 OHLC data with bell-curve-like price distribution.
     *
     * <p>This generator creates more realistic Market Profile data by:
     * <ul>
     *   <li>Using mean-reverting price behavior within each session</li>
     *   <li>Clustering prices around a daily "fair value" to create bell-curve profiles</li>
     *   <li>Adding more activity in the middle of the price range</li>
     *   <li>Creating realistic Initial Balance ranges in the first 2 hours</li>
     * </ul>
     *
     * @param days number of calendar days to generate
     * @param barDurationMinutes bar duration in minutes (e.g., 5, 15, 30)
     * @param startPrice initial price
     * @param volatility price volatility factor
     * @param randomSeed seed for reproducibility
     * @return OhlcData with bell-curve distributed price data
     */
    public static OhlcData generateTPOOptimizedData(
            int days,
            int barDurationMinutes,
            float startPrice,
            float volatility,
            int randomSeed) {

        TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(tz);

        int barsPerDay = (24 * 60) / barDurationMinutes;
        int totalBars = days * barsPerDay;

        OhlcData data = new OhlcData("tpo_demo", "TPO Demo Data", totalBars);

        Random random = new Random(randomSeed);
        long barDurationMillis = barDurationMinutes * 60 * 1000L;

        // Start from 'days' ago at midnight UTC
        cal.add(Calendar.DAY_OF_MONTH, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long startTime = cal.getTimeInMillis();

        float price = startPrice;

        for (int day = 0; day < days; day++) {
            // Each day has a "fair value" that prices tend to revert to
            // Slight drift from previous day's close
            float dailyFairValue = price + (random.nextFloat() - 0.5f) * volatility * 2;

            // Daily range - prices will oscillate within this
            float dailyRange = volatility * (3 + random.nextFloat() * 2);
            float dailyHigh = dailyFairValue + dailyRange / 2;
            float dailyLow = dailyFairValue - dailyRange / 2;

            // Initial Balance range (first 2 hours) - typically narrower
            float ibRange = dailyRange * (0.4f + random.nextFloat() * 0.2f);
            float ibHigh = dailyFairValue + ibRange / 2;
            float ibLow = dailyFairValue - ibRange / 2;

            int ibBars = (2 * 60) / barDurationMinutes; // First 2 hours

            for (int bar = 0; bar < barsPerDay; bar++) {
                int globalBarIdx = day * barsPerDay + bar;
                long timestamp = startTime + (globalBarIdx * barDurationMillis);

                // Mean reversion strength - stronger pull toward fair value
                float meanReversionStrength = 0.15f;

                // During IB, prices stay within IB range
                // After IB, prices can explore more but still revert to fair value
                float targetPrice;
                float rangeHigh, rangeLow;

                if (bar < ibBars) {
                    // Initial Balance - tighter range around fair value
                    targetPrice = dailyFairValue;
                    rangeHigh = ibHigh;
                    rangeLow = ibLow;
                } else {
                    // Rest of day - can extend beyond IB but reverts to fair value
                    targetPrice = dailyFairValue;
                    rangeHigh = dailyHigh;
                    rangeLow = dailyLow;

                    // Occasionally test the extremes
                    if (random.nextFloat() < 0.1f) {
                        targetPrice = random.nextBoolean() ?
                            dailyFairValue + dailyRange * 0.3f :
                            dailyFairValue - dailyRange * 0.3f;
                    }
                }

                // Mean-reverting random walk
                float pullToMean = (targetPrice - price) * meanReversionStrength;
                float noise = (random.nextFloat() - 0.5f) * volatility;
                float change = pullToMean + noise;

                float open = price;
                float close = price + change;

                // Clamp to daily range (soft boundary)
                if (close > rangeHigh) {
                    close = rangeHigh - random.nextFloat() * volatility * 0.5f;
                } else if (close < rangeLow) {
                    close = rangeLow + random.nextFloat() * volatility * 0.5f;
                }

                // Generate high/low with tendency toward the range center
                float wickUp = random.nextFloat() * volatility * 0.3f;
                float wickDown = random.nextFloat() * volatility * 0.3f;

                float high = Math.max(open, close) + wickUp;
                float low = Math.min(open, close) - wickDown;

                // Clamp high/low to daily range
                high = Math.min(high, dailyHigh);
                low = Math.max(low, dailyLow);

                // Volume - higher near fair value (bell curve)
                float distFromFairValue = Math.abs(price - dailyFairValue) / dailyRange;
                float volumeMultiplier = 1.0f - distFromFairValue * 0.5f; // More volume near center
                float volume = (500 + random.nextFloat() * 10000) * volumeMultiplier;

                data.append(timestamp, open, high, low, close, volume);

                price = close;
            }
        }

        return data;
    }
}
