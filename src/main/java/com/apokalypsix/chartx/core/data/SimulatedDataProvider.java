package com.apokalypsix.chartx.core.data;

import com.apokalypsix.chartx.chart.data.HistogramData;
import com.apokalypsix.chartx.chart.data.OhlcData;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simulated real-time data provider for demonstration and testing.
 *
 * <p>Generates random price movements and updates the series using
 * a scheduled executor. Supports both appending new bars and updating
 * the current forming bar.
 */
public class SimulatedDataProvider {

    private static final long DEFAULT_UPDATE_INTERVAL_MS = 500;

    private final OhlcData ohlcSeries;
    private final HistogramData volumeSeries;
    private final long barDuration;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> updateTask;
    private final Random random = new Random();

    // Current bar state
    private long currentBarTime;
    private float currentOpen;
    private float currentHigh;
    private float currentLow;
    private float currentClose;
    private float currentVolume;
    private float lastPrice;
    private float volatility = 5f;

    private long updateIntervalMs = DEFAULT_UPDATE_INTERVAL_MS;
    private boolean running = false;

    /**
     * Creates a simulated data provider.
     *
     * @param ohlcSeries the OHLC series to update
     * @param volumeSeries the volume series to update (can be null)
     * @param barDuration the duration of each bar in milliseconds
     */
    public SimulatedDataProvider(OhlcData ohlcSeries, HistogramData volumeSeries, long barDuration) {
        this.ohlcSeries = ohlcSeries;
        this.volumeSeries = volumeSeries;
        this.barDuration = barDuration;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SimulatedDataProvider");
            t.setDaemon(true);
            return t;
        });

        // Initialize last price from series or default
        if (ohlcSeries != null && !ohlcSeries.isEmpty()) {
            lastPrice = ohlcSeries.getClose(ohlcSeries.size() - 1);
            currentBarTime = ohlcSeries.getXValue(ohlcSeries.size() - 1) + barDuration;
        } else {
            lastPrice = 100f;
            currentBarTime = System.currentTimeMillis();
        }

        initializeCurrentBar();
    }

    /**
     * Sets the update interval.
     *
     * @param intervalMs update interval in milliseconds
     */
    public void setUpdateInterval(long intervalMs) {
        this.updateIntervalMs = Math.max(50, intervalMs);
        if (running) {
            stop();
            start();
        }
    }

    /**
     * Returns the current update interval.
     */
    public long getUpdateInterval() {
        return updateIntervalMs;
    }

    /**
     * Starts generating simulated data updates.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        updateTask = executor.scheduleAtFixedRate(
                this::generateUpdate,
                0,
                updateIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops generating data updates.
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
    }

    /**
     * Returns true if the provider is currently generating updates.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Shuts down the executor. Call this when done with the provider.
     */
    public void shutdown() {
        stop();
        executor.shutdown();
    }

    private void initializeCurrentBar() {
        currentOpen = lastPrice;
        currentHigh = lastPrice;
        currentLow = lastPrice;
        currentClose = lastPrice;
        currentVolume = 1000 + random.nextFloat() * 5000;
    }

    private void generateUpdate() {
        try {
            long now = System.currentTimeMillis();

            // Check if we need to start a new bar
            if (now >= currentBarTime + barDuration) {
                // Finalize current bar and start a new one
                appendCurrentBar();
                currentBarTime = currentBarTime + barDuration;
                initializeCurrentBar();
            } else {
                // Update the current forming bar
                updateCurrentBar();
            }
        } catch (Exception e) {
            // Don't let exceptions kill the scheduled task
            e.printStackTrace();
        }
    }

    private void updateCurrentBar() {
        // Generate random price movement
        float change = (random.nextFloat() - 0.5f) * volatility * 0.3f;
        currentClose = Math.max(0.01f, currentClose + change);
        lastPrice = currentClose;

        // Update high/low
        currentHigh = Math.max(currentHigh, currentClose);
        currentLow = Math.min(currentLow, currentClose);

        // Accumulate volume
        currentVolume += random.nextFloat() * 500;

        // Occasionally adjust volatility
        if (random.nextFloat() < 0.02f) {
            volatility = 3f + random.nextFloat() * 10f;
        }

        // Update the last bar in the series
        if (ohlcSeries != null && !ohlcSeries.isEmpty()) {
            // Check if we need to append first or update
            long lastTimestamp = ohlcSeries.getXValue(ohlcSeries.size() - 1);
            if (lastTimestamp == currentBarTime) {
                ohlcSeries.updateLast(currentOpen, currentHigh, currentLow, currentClose, currentVolume);
            } else {
                // Bar doesn't exist yet, append it
                ohlcSeries.append(currentBarTime, currentOpen, currentHigh, currentLow, currentClose, currentVolume);
            }
        }

        // Update volume series
        if (volumeSeries != null && !volumeSeries.isEmpty()) {
            long lastTimestamp = volumeSeries.getXValue(volumeSeries.size() - 1);
            // Use positive volume for bullish, negative for bearish
            float volumeValue = currentClose >= currentOpen ? currentVolume : -currentVolume;
            if (lastTimestamp == currentBarTime) {
                volumeSeries.updateLast(volumeValue);
            } else {
                volumeSeries.append(currentBarTime, volumeValue);
            }
        }
    }

    private void appendCurrentBar() {
        // Append to OHLC series
        if (ohlcSeries != null) {
            long lastTimestamp = ohlcSeries.isEmpty() ? -1 : ohlcSeries.getXValue(ohlcSeries.size() - 1);
            if (lastTimestamp != currentBarTime) {
                ohlcSeries.append(currentBarTime, currentOpen, currentHigh, currentLow, currentClose, currentVolume);
            }
        }

        // Append to volume series
        if (volumeSeries != null) {
            long lastTimestamp = volumeSeries.isEmpty() ? -1 : volumeSeries.getXValue(volumeSeries.size() - 1);
            if (lastTimestamp != currentBarTime) {
                float volumeValue = currentClose >= currentOpen ? currentVolume : -currentVolume;
                volumeSeries.append(currentBarTime, volumeValue);
            }
        }
    }
}
