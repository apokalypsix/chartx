package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.render.api.RenderBackendFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * ChartX OpenGL benchmark for comparison with JFreeChart.
 *
 * <p>Renders 4 line series (Open, High, Low, Close) using the OpenGL backend
 * to measure frame rendering performance. This provides a direct comparison
 * with the JFreeChartBenchmark which renders the same 4 line series.
 *
 * <p>Usage:
 * <pre>
 * java -cp chartx.jar com.apokalypsix.chartx.benchmark.ChartXBenchmark
 * </pre>
 */
public class ChartXBenchmark {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP = 50;
    private static final int MEASURE = 100;
    private static final long BAR_DURATION = 60000; // 1 minute bars

    /**
     * Configures logging to ERROR level for clean benchmark output.
     */
    private static void configureLogging() {
        try {
            // Use reflection to avoid compile-time dependency on logback
            Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");

            Object loggerContext = org.slf4j.LoggerFactory.getILoggerFactory();
            Object rootLogger = loggerContextClass.getMethod("getLogger", String.class)
                    .invoke(loggerContext, org.slf4j.Logger.ROOT_LOGGER_NAME);

            Object errorLevel = levelClass.getField("ERROR").get(null);
            rootLogger.getClass().getMethod("setLevel", levelClass).invoke(rootLogger, errorLevel);
        } catch (Exception e) {
            // Logging configuration failed, continue anyway
        }
    }

    public static void main(String[] args) {
        // Set logging to ERROR level for clean benchmark output
        configureLogging();

        System.out.println("========================================");
        System.out.println("ChartX OpenGL Benchmark");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Resolution: " + WIDTH + "x" + HEIGHT);
        System.out.println("  Backend: OpenGL");
        System.out.println("  Rendering: 4 line series (OHLC)");
        System.out.println("  Warmup iterations: " + WARMUP);
        System.out.println("  Measure iterations: " + MEASURE);
        System.out.println();

        // Check OpenGL availability
        if (!RenderBackendFactory.isBackendAvailable(RenderBackend.OPENGL)) {
            System.err.println("ERROR: OpenGL backend is not available!");
            System.exit(1);
        }

        // Bar counts matching JFreeChartBenchmark (each bar = 1 point per series, 4 series total)
        int[] barCounts = {10000, 50000, 100000};

        System.out.println("| Data Size | Avg Frame Time | Min | Max | P95 | Throughput |");
        System.out.println("|-----------|----------------|-----|-----|-----|------------|");

        for (int barCount : barCounts) {
            try {
                BenchmarkResult result = runBenchmark(barCount);

                double avgMs = result.getAverageFrameTime();
                double minMs = result.getFrameTimeStats().getMin();
                double maxMs = result.getFrameTimeStats().getMax();
                double p95Ms = result.getP95FrameTime();
                double throughput = (barCount * 4) / avgMs / 1000.0; // M points/s (4 series)

                System.out.printf("| %,d pts | %.2fms | %.2fms | %.2fms | %.2fms | %.1fM pts/s |%n",
                        barCount * 4, avgMs, minMs, maxMs, p95Ms, throughput);
            } catch (Exception e) {
                System.err.printf("| %,d bars | ERROR: %s |%n", barCount, e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println();
        System.out.println("Benchmark completed successfully!");
        System.exit(0);
    }

    private static BenchmarkResult runBenchmark(int barCount) throws Exception {
        BenchmarkResult result = new BenchmarkResult("Line Series", "ChartX-OpenGL", barCount);
        result.setWarmupFrames(WARMUP);
        result.setMeasureFrames(MEASURE);

        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = null;
            try {
                // Create test data (4 line series like JFreeChart)
                XyData[] series = createTestData(barCount);

                // Create chart with OpenGL backend
                Chart chart = new Chart("benchmark", RenderBackend.OPENGL);

                // Add 4 line series matching JFreeChart colors
                chart.addLineSeries(series[0], new LineSeriesOptions()
                        .color(new Color(50, 200, 50)));   // Open - green
                chart.addLineSeries(series[1], new LineSeriesOptions()
                        .color(new Color(200, 50, 50)));   // High - red
                chart.addLineSeries(series[2], new LineSeriesOptions()
                        .color(new Color(50, 50, 200)));   // Low - blue
                chart.addLineSeries(series[3], new LineSeriesOptions()
                        .color(new Color(200, 200, 50)));  // Close - yellow

                chart.setPreferredSize(new Dimension(WIDTH, HEIGHT));

                // Create frame (off-screen for benchmarking)
                frame = new JFrame("ChartX Benchmark");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setUndecorated(true);
                frame.add(chart);
                frame.pack();
                frame.setLocation(-10000, -10000); // Off-screen
                frame.setVisible(true);

                // Let OpenGL initialize
                Thread.sleep(100);

                // Force initial paint to trigger GL initialization
                chart.paintImmediately(0, 0, WIDTH, HEIGHT);

                // Now set visible range AFTER GL is initialized
                // Use explicit time range to ensure all data is rendered
                long minTime = series[0].getMinX();
                long maxTime = series[0].getMaxX();
                chart.setVisibleRange(minTime, maxTime);

                // Verify visible range covers all data
                int firstIdx = series[0].getFirstVisibleIndex(minTime);
                int lastIdx = series[0].getLastVisibleIndex(maxTime);
                int visibleCount = lastIdx - firstIdx + 1;
                if (visibleCount != barCount) {
                    System.err.printf("WARNING: Expected %d visible points, got %d (first=%d, last=%d)%n",
                            barCount, visibleCount, firstIdx, lastIdx);
                }

                // Force another paint after setting range
                chart.paintImmediately(0, 0, WIDTH, HEIGHT);

                // Warmup phase
                for (int i = 0; i < WARMUP; i++) {
                    chart.paintImmediately(0, 0, WIDTH, HEIGHT);
                }

                // Measurement phase
                for (int i = 0; i < MEASURE; i++) {
                    long startNanos = System.nanoTime();

                    // Force synchronous paint
                    chart.paintImmediately(0, 0, WIDTH, HEIGHT);

                    long endNanos = System.nanoTime();
                    result.addFrameTime((endNanos - startNanos) / 1_000_000.0);
                }

                // Final sync to ensure all GPU work is complete before disposing
                Toolkit.getDefaultToolkit().sync();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (frame != null) {
                    frame.dispose();
                }
                latch.countDown();
            }
        });

        latch.await();
        return result;
    }

    /**
     * Creates test line series data matching JFreeChartBenchmark.
     * Returns array of [Open, High, Low, Close] XyData series.
     */
    private static XyData[] createTestData(int barCount) {
        XyData openSeries = new XyData("open", "Open", barCount);
        XyData highSeries = new XyData("high", "High", barCount);
        XyData lowSeries = new XyData("low", "Low", barCount);
        XyData closeSeries = new XyData("close", "Close", barCount);

        Random random = new Random(42); // Fixed seed for reproducibility
        long baseTime = System.currentTimeMillis() - barCount * BAR_DURATION;
        double price = 100.0;

        for (int i = 0; i < barCount; i++) {
            long time = baseTime + i * BAR_DURATION;

            double open = price;
            double change = (random.nextDouble() - 0.5) * 2;
            double close = price + change;
            double high = Math.max(open, close) + random.nextDouble() * 0.5;
            double low = Math.min(open, close) - random.nextDouble() * 0.5;

            openSeries.append(time, (float) open);
            highSeries.append(time, (float) high);
            lowSeries.append(time, (float) low);
            closeSeries.append(time, (float) close);

            price = close;
        }

        return new XyData[] { openSeries, highSeries, lowSeries, closeSeries };
    }

    /**
     * Runs a comparison between JFreeChart and ChartX.
     * Call this method to run both benchmarks and compare results.
     */
    public static void runComparison() {
        System.out.println("========================================");
        System.out.println("JFreeChart vs ChartX Comparison");
        System.out.println("========================================");
        System.out.println();

        int[] barCounts = {1000, 5000, 10000, 50000, 100000};

        System.out.println("| Data Size | JFreeChart | ChartX-GL | Speedup |");
        System.out.println("|-----------|------------|-----------|---------|");

        for (int barCount : barCounts) {
            try {
                // Run JFreeChart benchmark
                BenchmarkResult jfreeResult = runJFreeChartBenchmark(barCount);

                // Run ChartX benchmark
                BenchmarkResult chartxResult = runBenchmark(barCount);

                double jfreeMs = jfreeResult.getAverageFrameTime();
                double chartxMs = chartxResult.getAverageFrameTime();
                double speedup = jfreeMs / chartxMs;

                System.out.printf("| %,d bars | %.2fms | %.2fms | %.1fx |%n",
                        barCount, jfreeMs, chartxMs, speedup);

            } catch (Exception e) {
                System.err.printf("| %,d bars | ERROR |%n", barCount);
            }
        }

        System.out.println();
    }

    /**
     * Runs JFreeChart benchmark for comparison (same as JFreeChartBenchmark).
     */
    private static BenchmarkResult runJFreeChartBenchmark(int barCount) {
        BenchmarkResult result = new BenchmarkResult("OHLC Render", "JFreeChart", barCount);
        result.setWarmupFrames(WARMUP);
        result.setMeasureFrames(MEASURE);

        // Create dataset with OHLC-like data
        org.jfree.data.xy.XYSeriesCollection dataset = createJFreeDataset(barCount);

        // Create chart
        org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createXYLineChart(
                null, null, null, dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                false, false, false);

        chart.setAntiAlias(false);
        chart.setTextAntiAlias(false);

        org.jfree.chart.plot.XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.DARK_GRAY);

        org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer =
                new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(50, 200, 50));
        renderer.setSeriesPaint(1, new Color(200, 50, 50));
        renderer.setSeriesPaint(2, new Color(50, 50, 200));
        renderer.setSeriesPaint(3, new Color(200, 200, 50));
        plot.setRenderer(renderer);

        // Create offscreen buffer
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            chart.draw(g2, new Rectangle(0, 0, WIDTH, HEIGHT));
        }

        // Measure
        for (int i = 0; i < MEASURE; i++) {
            long startNanos = System.nanoTime();
            chart.draw(g2, new Rectangle(0, 0, WIDTH, HEIGHT));
            long endNanos = System.nanoTime();
            result.addFrameTime((endNanos - startNanos) / 1_000_000.0);
        }

        g2.dispose();
        return result;
    }

    private static org.jfree.data.xy.XYSeriesCollection createJFreeDataset(int barCount) {
        org.jfree.data.xy.XYSeries openSeries = new org.jfree.data.xy.XYSeries("Open", false, true);
        org.jfree.data.xy.XYSeries highSeries = new org.jfree.data.xy.XYSeries("High", false, true);
        org.jfree.data.xy.XYSeries lowSeries = new org.jfree.data.xy.XYSeries("Low", false, true);
        org.jfree.data.xy.XYSeries closeSeries = new org.jfree.data.xy.XYSeries("Close", false, true);

        Random random = new Random(42);
        double price = 100.0;

        for (int i = 0; i < barCount; i++) {
            double open = price;
            double change = (random.nextDouble() - 0.5) * 2;
            double close = price + change;
            double high = Math.max(open, close) + random.nextDouble() * 0.5;
            double low = Math.min(open, close) - random.nextDouble() * 0.5;

            openSeries.add(i, open);
            highSeries.add(i, high);
            lowSeries.add(i, low);
            closeSeries.add(i, close);

            price = close;
        }

        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        dataset.addSeries(openSeries);
        dataset.addSeries(highSeries);
        dataset.addSeries(lowSeries);
        dataset.addSeries(closeSeries);

        return dataset;
    }
}
