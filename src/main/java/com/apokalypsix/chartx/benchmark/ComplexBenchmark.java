package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.finance.FinanceChart;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.render.api.RenderBackendFactory;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.DefaultIntervalXYDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Complex benchmark comparing JFreeChart and ChartX across multiple scenarios:
 *
 * 1. Multi-Series: 20 overlaid line series
 * 2. Candlestick: OHLC candlestick chart
 * 3. Financial: Candlesticks + Volume bars + 3 indicator lines (SMA)
 * 4. High Density: 50 line series with 100k points each
 */
public class ComplexBenchmark {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP = 50;
    private static final int MEASURE = 200;
    private static final long BAR_DURATION = 60000; // 1 minute bars

    public static void main(String[] args) {
        // Configure logging to ERROR level
        configureLogging();

        System.out.println("================================================================");
        System.out.println("Complex Benchmark: JFreeChart vs ChartX OpenGL");
        System.out.println("================================================================");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Resolution: " + WIDTH + "x" + HEIGHT);
        System.out.println("  Warmup iterations: " + WARMUP);
        System.out.println("  Measure iterations: " + MEASURE);
        System.out.println();

        // Check OpenGL availability
        if (!RenderBackendFactory.isBackendAvailable(RenderBackend.OPENGL)) {
            System.err.println("ERROR: OpenGL backend is not available!");
            System.exit(1);
        }

        // Run all benchmark scenarios
        runMultiSeriesBenchmark();
        runCandlestickBenchmark();
        runFinancialChartBenchmark();
        runHighDensityBenchmark();

        System.out.println("================================================================");
        System.out.println("All benchmarks completed!");
        System.out.println("================================================================");
        System.exit(0);
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

    // ========== Scenario 1: Multi-Series (20 line series) ==========

    private static void runMultiSeriesBenchmark() {
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("Scenario 1: Multi-Series (20 overlaid line series)");
        System.out.println("────────────────────────────────────────────────────────────────");

        int[] dataSizes = {1000, 5000, 10000, 50000};
        int seriesCount = 20;

        System.out.println();
        System.out.println("| Data Points | JFreeChart | ChartX-GL | Speedup |");
        System.out.println("|-------------|------------|-----------|---------|");

        for (int dataSize : dataSizes) {
            try {
                double jfreeTime = runJFreeMultiSeries(dataSize, seriesCount);
                double chartxTime = runChartXMultiSeries(dataSize, seriesCount);
                double speedup = jfreeTime / chartxTime;

                System.out.printf("| %,d x %d | %.2fms | %.2fms | %.1fx |%n",
                        dataSize, seriesCount, jfreeTime, chartxTime, speedup);
            } catch (Exception e) {
                System.err.printf("| %,d x %d | ERROR: %s |%n", dataSize, seriesCount, e.getMessage());
            }
        }
        System.out.println();
    }

    private static double runJFreeMultiSeries(int dataSize, int seriesCount) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        Random random = new Random(42);

        for (int s = 0; s < seriesCount; s++) {
            XYSeries series = new XYSeries("Series " + s, false, true);
            double value = 100 + s * 10;
            for (int i = 0; i < dataSize; i++) {
                value += (random.nextDouble() - 0.5) * 2;
                series.add(i, value);
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                null, null, null, dataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.setAntiAlias(false);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int s = 0; s < seriesCount; s++) {
            renderer.setSeriesPaint(s, new Color((s * 37) % 256, (s * 73) % 256, (s * 127) % 256));
        }
        plot.setRenderer(renderer);

        return measureJFreeChart(chart);
    }

    private static double runChartXMultiSeries(int dataSize, int seriesCount) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        double[] result = new double[1];

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = null;
            try {
                Chart chart = new Chart("benchmark", RenderBackend.OPENGL);
                Random random = new Random(42);

                long baseTime = System.currentTimeMillis() - dataSize * BAR_DURATION;
                for (int s = 0; s < seriesCount; s++) {
                    XyData data = new XyData("series" + s, "Series " + s, dataSize);
                    double value = 100 + s * 10;
                    for (int i = 0; i < dataSize; i++) {
                        value += (random.nextDouble() - 0.5) * 2;
                        data.append(baseTime + i * BAR_DURATION, (float) value);
                    }
                    chart.addLineSeries(data, new LineSeriesOptions()
                            .color(new Color((s * 37) % 256, (s * 73) % 256, (s * 127) % 256)));
                }

                result[0] = measureChartX(chart, frame = createFrame(chart));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (frame != null) frame.dispose();
                latch.countDown();
            }
        });

        latch.await();
        return result[0];
    }

    // ========== Scenario 2: Candlestick Chart ==========

    private static void runCandlestickBenchmark() {
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("Scenario 2: Candlestick Chart (OHLC)");
        System.out.println("────────────────────────────────────────────────────────────────");

        int[] dataSizes = {1000, 5000, 10000, 50000, 100000};

        System.out.println();
        System.out.println("| Candles | JFreeChart | ChartX-GL | Speedup |");
        System.out.println("|---------|------------|-----------|---------|");

        for (int dataSize : dataSizes) {
            try {
                double jfreeTime = runJFreeCandlestick(dataSize);
                double chartxTime = runChartXCandlestick(dataSize);
                double speedup = jfreeTime / chartxTime;

                System.out.printf("| %,d | %.2fms | %.2fms | %.1fx |%n",
                        dataSize, jfreeTime, chartxTime, speedup);
            } catch (Exception e) {
                System.err.printf("| %,d | ERROR: %s |%n", dataSize, e.getMessage());
            }
        }
        System.out.println();
    }

    private static double runJFreeCandlestick(int dataSize) {
        Date[] dates = new Date[dataSize];
        double[] highs = new double[dataSize];
        double[] lows = new double[dataSize];
        double[] opens = new double[dataSize];
        double[] closes = new double[dataSize];
        double[] volumes = new double[dataSize];

        Random random = new Random(42);
        long baseTime = System.currentTimeMillis() - dataSize * BAR_DURATION;
        double price = 100.0;

        for (int i = 0; i < dataSize; i++) {
            dates[i] = new Date(baseTime + i * BAR_DURATION);
            double open = price;
            double change = (random.nextDouble() - 0.5) * 2;
            double close = price + change;
            double high = Math.max(open, close) + random.nextDouble() * 0.5;
            double low = Math.min(open, close) - random.nextDouble() * 0.5;

            opens[i] = open;
            highs[i] = high;
            lows[i] = low;
            closes[i] = close;
            volumes[i] = random.nextDouble() * 1000000;
            price = close;
        }

        DefaultHighLowDataset dataset = new DefaultHighLowDataset(
                "OHLC", dates, highs, lows, opens, closes, volumes);

        JFreeChart chart = ChartFactory.createCandlestickChart(
                null, null, null, dataset, false);
        chart.setAntiAlias(false);

        XYPlot plot = chart.getXYPlot();
        CandlestickRenderer renderer = new CandlestickRenderer();
        renderer.setUpPaint(new Color(50, 200, 50));
        renderer.setDownPaint(new Color(200, 50, 50));
        plot.setRenderer(renderer);

        return measureJFreeChart(chart);
    }

    private static double runChartXCandlestick(int dataSize) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        double[] result = new double[1];

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = null;
            try {
                FinanceChart chart = new FinanceChart("benchmark", RenderBackend.OPENGL);

                OhlcData data = new OhlcData("ohlc", "OHLC", dataSize);
                Random random = new Random(42);
                long baseTime = System.currentTimeMillis() - dataSize * BAR_DURATION;
                double price = 100.0;

                for (int i = 0; i < dataSize; i++) {
                    double open = price;
                    double change = (random.nextDouble() - 0.5) * 2;
                    double close = price + change;
                    double high = Math.max(open, close) + random.nextDouble() * 0.5;
                    double low = Math.min(open, close) - random.nextDouble() * 0.5;
                    double volume = random.nextDouble() * 1000000;

                    data.append(baseTime + i * BAR_DURATION,
                            (float) open, (float) high, (float) low, (float) close, (float) volume);
                    price = close;
                }

                                chart.addCandlestickSeries(data, new OhlcSeriesOptions());

                result[0] = measureChartX(chart, frame = createFrame(chart));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (frame != null) frame.dispose();
                latch.countDown();
            }
        });

        latch.await();
        return result[0];
    }

    // ========== Scenario 3: Financial Chart (Candles + Volume + Indicators) ==========

    private static void runFinancialChartBenchmark() {
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("Scenario 3: Financial Chart (Candles + Volume + 3 SMAs)");
        System.out.println("────────────────────────────────────────────────────────────────");

        int[] dataSizes = {1000, 5000, 10000, 50000};

        System.out.println();
        System.out.println("| Bars | JFreeChart | ChartX-GL | Speedup |");
        System.out.println("|------|------------|-----------|---------|");

        for (int dataSize : dataSizes) {
            try {
                double jfreeTime = runJFreeFinancial(dataSize);
                double chartxTime = runChartXFinancial(dataSize);
                double speedup = jfreeTime / chartxTime;

                System.out.printf("| %,d | %.2fms | %.2fms | %.1fx |%n",
                        dataSize, jfreeTime, chartxTime, speedup);
            } catch (Exception e) {
                System.err.printf("| %,d | ERROR: %s |%n", dataSize, e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println();
    }

    private static double runJFreeFinancial(int dataSize) {
        // Create OHLC data
        Date[] dates = new Date[dataSize];
        double[] highs = new double[dataSize];
        double[] lows = new double[dataSize];
        double[] opens = new double[dataSize];
        double[] closes = new double[dataSize];
        double[] volumes = new double[dataSize];

        Random random = new Random(42);
        long baseTime = System.currentTimeMillis() - dataSize * BAR_DURATION;
        double price = 100.0;

        for (int i = 0; i < dataSize; i++) {
            dates[i] = new Date(baseTime + i * BAR_DURATION);
            double open = price;
            double change = (random.nextDouble() - 0.5) * 2;
            double close = price + change;
            double high = Math.max(open, close) + random.nextDouble() * 0.5;
            double low = Math.min(open, close) - random.nextDouble() * 0.5;

            opens[i] = open;
            highs[i] = high;
            lows[i] = low;
            closes[i] = close;
            volumes[i] = random.nextDouble() * 1000000;
            price = close;
        }

        // Candlestick plot
        DefaultHighLowDataset ohlcDataset = new DefaultHighLowDataset(
                "OHLC", dates, highs, lows, opens, closes, volumes);

        JFreeChart candleChart = ChartFactory.createCandlestickChart(
                null, null, null, ohlcDataset, false);
        XYPlot pricePlot = candleChart.getXYPlot();

        // Add SMA lines
        XYSeriesCollection smaDataset = new XYSeriesCollection();
        int[] smaPeriods = {10, 20, 50};
        for (int period : smaPeriods) {
            XYSeries sma = new XYSeries("SMA" + period, false, true);
            double sum = 0;
            for (int i = 0; i < dataSize; i++) {
                sum += closes[i];
                if (i >= period) {
                    sum -= closes[i - period];
                    sma.add(dates[i].getTime(), sum / period);
                } else if (i == period - 1) {
                    sma.add(dates[i].getTime(), sum / period);
                }
            }
            smaDataset.addSeries(sma);
        }

        XYLineAndShapeRenderer smaRenderer = new XYLineAndShapeRenderer(true, false);
        smaRenderer.setSeriesPaint(0, Color.BLUE);
        smaRenderer.setSeriesPaint(1, Color.ORANGE);
        smaRenderer.setSeriesPaint(2, Color.MAGENTA);
        pricePlot.setDataset(1, smaDataset);
        pricePlot.setRenderer(1, smaRenderer);

        // Volume plot
        XYSeries volumeSeries = new XYSeries("Volume", false, true);
        for (int i = 0; i < dataSize; i++) {
            volumeSeries.add(dates[i].getTime(), volumes[i]);
        }
        XYSeriesCollection volumeDataset = new XYSeriesCollection(volumeSeries);

        XYPlot volumePlot = new XYPlot(volumeDataset, null,
                new org.jfree.chart.axis.NumberAxis(), new XYBarRenderer());
        volumePlot.setBackgroundPaint(Color.DARK_GRAY);

        // Combined plot
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot();
        combinedPlot.add(pricePlot, 3);
        combinedPlot.add(volumePlot, 1);

        JFreeChart chart = new JFreeChart(combinedPlot);
        chart.setAntiAlias(false);

        return measureJFreeChart(chart);
    }

    private static double runChartXFinancial(int dataSize) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        double[] result = new double[1];

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = null;
            try {
                FinanceChart chart = new FinanceChart("benchmark", RenderBackend.OPENGL);

                OhlcData data = new OhlcData("ohlc", "OHLC", dataSize);
                Random random = new Random(42);
                long baseTime = System.currentTimeMillis() - dataSize * BAR_DURATION;
                double price = 100.0;

                double[] closes = new double[dataSize];
                long[] times = new long[dataSize];

                for (int i = 0; i < dataSize; i++) {
                    times[i] = baseTime + i * BAR_DURATION;
                    double open = price;
                    double change = (random.nextDouble() - 0.5) * 2;
                    double close = price + change;
                    double high = Math.max(open, close) + random.nextDouble() * 0.5;
                    double low = Math.min(open, close) - random.nextDouble() * 0.5;
                    double volume = random.nextDouble() * 1000000;

                    data.append(times[i], (float) open, (float) high, (float) low, (float) close, (float) volume);
                    closes[i] = close;
                    price = close;
                }

                                chart.addCandlestickSeries(data, new OhlcSeriesOptions());

                // Add SMA indicators
                int[] smaPeriods = {10, 20, 50};
                Color[] smaColors = {Color.BLUE, Color.ORANGE, Color.MAGENTA};
                for (int p = 0; p < smaPeriods.length; p++) {
                    int period = smaPeriods[p];
                    XyData sma = new XyData("sma" + period, "SMA" + period, dataSize);
                    double sum = 0;
                    for (int i = 0; i < dataSize; i++) {
                        sum += closes[i];
                        if (i >= period) {
                            sum -= closes[i - period];
                            sma.append(times[i], (float) (sum / period));
                        } else if (i == period - 1) {
                            sma.append(times[i], (float) (sum / period));
                        }
                    }
                    chart.addLineSeries(sma, new LineSeriesOptions().color(smaColors[p]));
                }

                result[0] = measureChartX(chart, frame = createFrame(chart));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (frame != null) frame.dispose();
                latch.countDown();
            }
        });

        latch.await();
        return result[0];
    }

    // ========== Scenario 4: High Density (50 series x 100k points) ==========

    private static void runHighDensityBenchmark() {
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("Scenario 4: High Density (multiple series, large data)");
        System.out.println("────────────────────────────────────────────────────────────────");

        int[][] configs = {
                {10, 50000},   // 10 series x 50k = 500k points
                {20, 50000},   // 20 series x 50k = 1M points
                {50, 20000},   // 50 series x 20k = 1M points
                {10, 100000},  // 10 series x 100k = 1M points
        };

        System.out.println();
        System.out.println("| Config | Total Points | JFreeChart | ChartX-GL | Speedup |");
        System.out.println("|--------|--------------|------------|-----------|---------|");

        for (int[] config : configs) {
            int seriesCount = config[0];
            int pointsPerSeries = config[1];
            int totalPoints = seriesCount * pointsPerSeries;

            try {
                double jfreeTime = runJFreeMultiSeries(pointsPerSeries, seriesCount);
                double chartxTime = runChartXMultiSeries(pointsPerSeries, seriesCount);
                double speedup = jfreeTime / chartxTime;

                System.out.printf("| %dx%dk | %,d | %.2fms | %.2fms | %.1fx |%n",
                        seriesCount, pointsPerSeries / 1000, totalPoints,
                        jfreeTime, chartxTime, speedup);
            } catch (Exception e) {
                System.err.printf("| %dx%dk | ERROR: %s |%n",
                        seriesCount, pointsPerSeries / 1000, e.getMessage());
            }
        }
        System.out.println();
    }

    // ========== Measurement Helpers ==========

    private static double measureJFreeChart(JFreeChart chart) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            chart.draw(g2, new Rectangle(0, 0, WIDTH, HEIGHT));
        }

        // Measure
        long totalNanos = 0;
        for (int i = 0; i < MEASURE; i++) {
            long startNanos = System.nanoTime();
            chart.draw(g2, new Rectangle(0, 0, WIDTH, HEIGHT));
            totalNanos += System.nanoTime() - startNanos;
        }

        g2.dispose();
        return totalNanos / MEASURE / 1_000_000.0;
    }

    private static double measureChartX(JComponent chart, JFrame frame) throws InterruptedException {
        chart.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.pack();
        frame.setVisible(true);

        Thread.sleep(100); // Let GL initialize

        // Initial paint
        chart.paintImmediately(0, 0, WIDTH, HEIGHT);

        // Set visible range for Chart types
        if (chart instanceof Chart c) {
            c.zoomToFit();
            chart.paintImmediately(0, 0, WIDTH, HEIGHT);
        }

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            chart.paintImmediately(0, 0, WIDTH, HEIGHT);
        }

        // Measure
        long totalNanos = 0;
        for (int i = 0; i < MEASURE; i++) {
            long startNanos = System.nanoTime();
            chart.paintImmediately(0, 0, WIDTH, HEIGHT);
            totalNanos += System.nanoTime() - startNanos;
        }

        Toolkit.getDefaultToolkit().sync();
        return totalNanos / MEASURE / 1_000_000.0;
    }

    private static JFrame createFrame(JComponent chart) {
        JFrame frame = new JFrame("Benchmark");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.add(chart);
        frame.setLocation(-10000, -10000);
        return frame;
    }
}
