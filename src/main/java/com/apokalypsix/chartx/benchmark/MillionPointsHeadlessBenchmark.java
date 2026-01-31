package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.render.api.RenderBackendFactory;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Headless Million Points Benchmark for automated performance testing.
 *
 * <p>Runs performance measurements for rendering 1 million data points
 * across all available backends without requiring user interaction.
 * Suitable for CI/CD pipelines and automated benchmarking.
 *
 * <p>Usage:
 * <pre>
 * java -cp chartx-benchmarks.jar com.apokalypsix.chartx.benchmark.MillionPointsHeadlessBenchmark
 * </pre>
 */
public class MillionPointsHeadlessBenchmark {

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int WARMUP = 100;
    private static final int MEASURE = 500;

    // Point counts to test
    private static final int[] POINT_COUNTS = {
            100_000,
            250_000,
            500_000,
            1_000_000,
            2_000_000,
            5_000_000
    };

    public static void main(String[] args) {
        configureLogging();

        System.out.println("================================================================");
        System.out.println("Million Points Headless Benchmark");
        System.out.println("================================================================");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Resolution: " + WIDTH + "x" + HEIGHT);
        System.out.println("  Warmup iterations: " + WARMUP);
        System.out.println("  Measure iterations: " + MEASURE);
        System.out.println();

        // Run benchmarks for each available backend
        for (RenderBackend backend : new RenderBackend[]{
                RenderBackend.METAL,
                RenderBackend.VULKAN,
                RenderBackend.OPENGL
        }) {
            if (RenderBackendFactory.isBackendAvailable(backend)) {
                runBackendBenchmark(backend);
            } else {
                System.out.println("Skipping " + backend + " (not available)");
                System.out.println();
            }
        }

        System.out.println("================================================================");
        System.out.println("Benchmark completed!");
        System.out.println("================================================================");
        System.exit(0);
    }

    private static void runBackendBenchmark(RenderBackend backend) {
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("Backend: " + backend);
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println();
        System.out.println("| Points | Frame Time | FPS | Throughput | P95 | P99 |");
        System.out.println("|--------|------------|-----|------------|-----|-----|");

        for (int pointCount : POINT_COUNTS) {
            try {
                BenchmarkResult result = runBenchmark(backend, pointCount);
                printResult(pointCount, result);
            } catch (Exception e) {
                System.out.printf("| %s | ERROR: %s |%n",
                        formatNumber(pointCount), e.getMessage());
            }

            // Brief pause between tests to let GPU resources settle
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println();
    }

    private static void printResult(int pointCount, BenchmarkResult result) {
        double avgMs = result.getAverageFrameTime();
        double fps = result.getAverageFPS();
        double throughput = pointCount / avgMs / 1000.0; // M pts/s
        double p95 = result.getP95FrameTime();
        double p99 = result.getP99FrameTime();

        System.out.printf("| %s | %.2f ms | %.1f | %.1f M/s | %.2f ms | %.2f ms |%n",
                formatNumber(pointCount), avgMs, fps, throughput, p95, p99);
    }

    private static BenchmarkResult runBenchmark(RenderBackend backend, int pointCount)
            throws Exception {
        BenchmarkResult result = new BenchmarkResult("MillionPoints", backend.name(), pointCount);
        result.setWarmupFrames(WARMUP);
        result.setMeasureFrames(MEASURE);

        CountDownLatch latch = new CountDownLatch(1);
        Exception[] error = new Exception[1];

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = null;
            try {
                // Generate data
                XyData data = generateData(pointCount);

                // Create chart
                Chart chart = new Chart("benchmark", backend);
                chart.addLineSeries(data, new LineSeriesOptions()
                        .color(new Color(100, 180, 255))
                        .lineWidth(1.0f));
                chart.setPreferredSize(new Dimension(WIDTH, HEIGHT));

                // Create off-screen frame
                frame = new JFrame("Benchmark");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setUndecorated(true);
                frame.add(chart);
                frame.pack();
                frame.setLocation(-10000, -10000);
                frame.setVisible(true);

                // Let GPU initialize
                Thread.sleep(100);

                // Initial paint
                chart.paintImmediately(0, 0, WIDTH, HEIGHT);

                // Set visible range
                chart.setVisibleRange(data.getMinX(), data.getMaxX());
                chart.paintImmediately(0, 0, WIDTH, HEIGHT);

                // Warmup
                for (int i = 0; i < WARMUP; i++) {
                    chart.paintImmediately(0, 0, WIDTH, HEIGHT);
                }

                // Measure
                for (int i = 0; i < MEASURE; i++) {
                    long startNanos = System.nanoTime();
                    chart.paintImmediately(0, 0, WIDTH, HEIGHT);
                    long endNanos = System.nanoTime();
                    result.addFrameTime((endNanos - startNanos) / 1_000_000.0);
                }

                Toolkit.getDefaultToolkit().sync();

            } catch (Exception e) {
                error[0] = e;
            } finally {
                if (frame != null) {
                    frame.dispose();
                }
                latch.countDown();
            }
        });

        latch.await();

        if (error[0] != null) {
            throw error[0];
        }

        return result;
    }

    /**
     * Generates synthetic data matching ChartGPU's million-points example.
     * Uses deterministic xorshift32 PRNG for reproducibility.
     */
    private static XyData generateData(int count) {
        XyData data = new XyData("data", "Signal", count);

        // Match ChartGPU's parameters exactly
        final double freq = 0.012;
        final double lowFreq = 0.0017;
        final double noiseAmp = 0.35;

        // xorshift32 PRNG with ChartGPU's seed
        int state = 0x12345678;

        // Use millisecond timestamps (1ms per point)
        long baseTime = System.currentTimeMillis() - count;

        for (int i = 0; i < count; i++) {
            // Generate next random value [0, 1)
            state ^= state << 13;
            state ^= state >>> 17;
            state ^= state << 5;
            double rand01 = (state >>> 0 & 0xFFFFFFFFL) / 4294967296.0;

            // Match ChartGPU's formula exactly
            double y = Math.sin(i * freq) * 0.95
                    + Math.sin(i * lowFreq + 1.1) * 0.6
                    + (rand01 - 0.5) * noiseAmp;

            data.append(baseTime + i, (float) y);
        }

        return data;
    }

    private static String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    private static void configureLogging() {
        try {
            Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            Object loggerContext = org.slf4j.LoggerFactory.getILoggerFactory();
            Object rootLogger = loggerContextClass.getMethod("getLogger", String.class)
                    .invoke(loggerContext, org.slf4j.Logger.ROOT_LOGGER_NAME);
            Object errorLevel = levelClass.getField("ERROR").get(null);
            rootLogger.getClass().getMethod("setLevel", levelClass).invoke(rootLogger, errorLevel);
        } catch (Exception e) {
            // Ignore
        }
    }
}
