package com.apokalypsix.chartx.benchmark;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * JFreeChart benchmark for comparison with ChartX backends.
 *
 * Renders OHLC-style data as line series to a BufferedImage (offscreen).
 */
public class JFreeChartBenchmark {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP = 50;
    private static final int MEASURE = 100;

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
        System.out.println("JFreeChart Benchmark");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Resolution: " + WIDTH + "x" + HEIGHT);
        System.out.println("  Warmup iterations: " + WARMUP);
        System.out.println("  Measure iterations: " + MEASURE);
        System.out.println();

        // Bar counts matching other benchmarks
        int[] barCounts = {10000, 50000, 100000};

        System.out.println("| Data Size | Avg Frame Time | Min | Max | P95 | Throughput |");
        System.out.println("|-----------|----------------|-----|-----|-----|------------|");

        for (int barCount : barCounts) {
            BenchmarkResult result = runBenchmark(barCount);

            double avgMs = result.getAverageFrameTime();
            double minMs = result.getFrameTimeStats().getMin();
            double maxMs = result.getFrameTimeStats().getMax();
            double p95Ms = result.getP95FrameTime();
            double throughput = barCount / avgMs / 1000.0; // M bars/s

            System.out.printf("| %,d bars | %.2fms | %.2fms | %.2fms | %.2fms | %.1fM bars/s |%n",
                    barCount, avgMs, minMs, maxMs, p95Ms, throughput);
        }

        System.out.println();
        System.out.println("Benchmark completed successfully!");
    }

    private static BenchmarkResult runBenchmark(int barCount) {
        BenchmarkResult result = new BenchmarkResult("OHLC Render", "JFreeChart", barCount);
        result.setWarmupFrames(WARMUP);
        result.setMeasureFrames(MEASURE);

        // Create dataset with OHLC-like data (4 series: open, high, low, close)
        XYSeriesCollection dataset = createDataset(barCount);

        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,           // title
                null,           // x-axis label
                null,           // y-axis label
                dataset,
                PlotOrientation.VERTICAL,
                false,          // legend
                false,          // tooltips
                false           // urls
        );

        // Configure for performance
        chart.setAntiAlias(false);
        chart.setTextAntiAlias(false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.DARK_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(50, 200, 50));   // Open - green
        renderer.setSeriesPaint(1, new Color(200, 50, 50));   // High - red
        renderer.setSeriesPaint(2, new Color(50, 50, 200));   // Low - blue
        renderer.setSeriesPaint(3, new Color(200, 200, 50));  // Close - yellow
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

    private static XYSeriesCollection createDataset(int barCount) {
        XYSeries openSeries = new XYSeries("Open", false, true);
        XYSeries highSeries = new XYSeries("High", false, true);
        XYSeries lowSeries = new XYSeries("Low", false, true);
        XYSeries closeSeries = new XYSeries("Close", false, true);

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

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(openSeries);
        dataset.addSeries(highSeries);
        dataset.addSeries(lowSeries);
        dataset.addSeries(closeSeries);

        return dataset;
    }
}
