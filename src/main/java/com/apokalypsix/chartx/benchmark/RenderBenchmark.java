package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.core.render.service.v2.CandlestickRendererV2;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.render.api.*;
import com.apokalypsix.chartx.core.render.backend.vulkan.VkRenderDevice;
import com.apokalypsix.chartx.core.render.backend.vulkan.VkSwingPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Benchmark suite for comparing OpenGL and Vulkan rendering performance.
 *
 * <p>Measures frame times, buffer uploads, and draw calls across
 * different data sizes and chart configurations.
 */
public class RenderBenchmark {

    private static final Logger log = LoggerFactory.getLogger(RenderBenchmark.class);

    private static final int WARMUP_FRAMES = 60;
    private static final int MEASURE_FRAMES = 300;
    private static final int CHART_WIDTH = 1280;
    private static final int CHART_HEIGHT = 720;
    private static final long BAR_DURATION = 60000; // 1 minute

    private final List<BenchmarkResult> results = new ArrayList<>();

    /**
     * Runs the complete benchmark suite.
     */
    public void runFullSuite() {
        log.info("========================================");
        log.info("ChartX Render Benchmark Suite");
        log.info("========================================");
        log.info("Configuration:");
        log.info("  Resolution: {}x{}", CHART_WIDTH, CHART_HEIGHT);
        log.info("  Warmup frames: {}", WARMUP_FRAMES);
        log.info("  Measure frames: {}", MEASURE_FRAMES);
        log.info("");

        // Data sizes to test
        int[] dataSizes = {1000, 5000, 10000, 50000, 100000};

        // Run Vulkan benchmarks if available
        if (RenderBackendFactory.isBackendAvailable(RenderBackend.VULKAN)) {
            log.info("Running Vulkan benchmarks...");
            for (int size : dataSizes) {
                try {
                    BenchmarkResult result = runVulkanBenchmark(size);
                    results.add(result);
                    log.info(result.toString());
                } catch (Exception e) {
                    log.error("Vulkan benchmark failed for size {}: {}", size, e.getMessage());
                }
            }
        } else {
            log.warn("Vulkan not available - skipping Vulkan benchmarks");
        }

        // Print summary
        printSummary();
    }

    /**
     * Runs a Vulkan benchmark with the specified data size.
     */
    public BenchmarkResult runVulkanBenchmark(int dataSize) throws Exception {
        log.info("  Testing Vulkan with {} bars...", dataSize);

        BenchmarkResult result = new BenchmarkResult("Candlestick", "Vulkan", dataSize);
        result.setWarmupFrames(WARMUP_FRAMES);
        result.setMeasureFrames(MEASURE_FRAMES);

        // Use a latch to wait for benchmark completion
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        SwingUtilities.invokeLater(() -> {
            RenderDevice device = null;
            ResourceManager resources = null;
            JFrame frame = null;

            try {
                // Initialize Vulkan
                device = RenderBackendFactory.createDevice(RenderBackend.VULKAN, null);
                device.initialize();

                resources = RenderBackendFactory.createResourceManager(device);
                resources.initialize(device);

                // Create chart infrastructure
                OhlcData series = createTestData(dataSize);
                Viewport viewport = new Viewport();
                viewport.setSize(CHART_WIDTH, CHART_HEIGHT);
                viewport.setInsets(50, 60, 10, 30);

                YAxisManager axisManager = new YAxisManager();
                YAxis priceAxis = axisManager.getDefaultAxis();
                updateAxisRange(priceAxis, series);

                if (series.size() > 0) {
                    viewport.setTimeRange(series.getXValue(0), series.getXValue(series.size() - 1));
                }

                MultiAxisCoordinateSystem coordinates = new MultiAxisCoordinateSystem(viewport, axisManager);

                // Create renderer
                CandlestickRendererV2 renderer = new CandlestickRendererV2();
                RenderContext initCtx = createRenderContext(device, resources, viewport, coordinates, axisManager, series);
                renderer.initialize(initCtx);

                // Create invisible frame for rendering
                frame = new JFrame("Benchmark");
                frame.setSize(CHART_WIDTH, CHART_HEIGHT);
                frame.setUndecorated(true);
                frame.setLocation(-10000, -10000); // Off-screen

                VkSwingPanel panel = new VkSwingPanel((VkRenderDevice) device);

                // Benchmark state
                final int[] frameCount = {0};
                final long[] totalVertices = {0};
                final long[] totalDrawCalls = {0};
                final RenderDevice finalDevice = device;
                final ResourceManager finalResources = resources;

                panel.setRenderCallback((dev, width, height) -> {
                    long frameStart = System.nanoTime();

                    // Clear screen
                    dev.clearScreen(0.08f, 0.09f, 0.10f, 1.0f);

                    // Create render context
                    RenderContext ctx = createRenderContext(finalDevice, finalResources, viewport, coordinates, axisManager, series);

                    // Render
                    long renderStart = System.nanoTime();
                    renderer.render(ctx, series);
                    long renderEnd = System.nanoTime();

                    long frameEnd = System.nanoTime();
                    double frameTimeMs = (frameEnd - frameStart) / 1_000_000.0;
                    double renderTimeMs = (renderEnd - renderStart) / 1_000_000.0;

                    frameCount[0]++;

                    // Record after warmup
                    if (frameCount[0] > WARMUP_FRAMES) {
                        result.addFrameTime(frameTimeMs);
                        result.addDrawTime(renderTimeMs);

                        // Estimate vertices (6 per candle body + 2 per wick)
                        int visibleBars = ctx.getLastVisibleIndex() - ctx.getFirstVisibleIndex() + 1;
                        totalVertices[0] += visibleBars * 8; // Approximate
                        totalDrawCalls[0] += 2; // Body + wick
                    }

                    // Stop after enough frames
                    if (frameCount[0] >= WARMUP_FRAMES + MEASURE_FRAMES) {
                        result.setTotalVertices(totalVertices[0]);
                        result.setTotalDrawCalls(totalDrawCalls[0]);
                        latch.countDown();
                    }
                });

                frame.add(panel);
                frame.setVisible(true);

                // Request continuous rendering
                Timer timer = new Timer(1, e -> {
                    if (frameCount[0] < WARMUP_FRAMES + MEASURE_FRAMES) {
                        panel.requestRender();
                    }
                });
                timer.start();

                // Wait for benchmark to complete (with timeout)
                final JFrame finalFrame = frame;
                final RenderDevice finalDevice2 = device;
                final ResourceManager finalResources2 = resources;
                final CandlestickRendererV2 finalRenderer = renderer;
                final Timer finalTimer = timer;

                new Thread(() -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    SwingUtilities.invokeLater(() -> {
                        finalTimer.stop();
                        finalFrame.setVisible(false);
                        finalFrame.dispose();

                        RenderContext disposeCtx = createRenderContext(finalDevice2, finalResources2, viewport, coordinates, axisManager, series);
                        finalRenderer.dispose(disposeCtx);
                        finalResources2.dispose();
                        finalDevice2.dispose();
                    });
                }).start();

            } catch (Exception e) {
                error.set(e);
                latch.countDown();

                if (frame != null) {
                    frame.dispose();
                }
                if (resources != null) {
                    resources.dispose();
                }
                if (device != null) {
                    device.dispose();
                }
            }
        });

        // Wait for completion
        latch.await();

        if (error.get() != null) {
            throw error.get();
        }

        return result;
    }

    /**
     * Creates test OHLC data with the specified number of bars.
     */
    private OhlcData createTestData(int count) {
        OhlcData series = new OhlcData("BENCH", "Benchmark Data");

        Random random = new Random(42); // Fixed seed for reproducibility
        long baseTime = System.currentTimeMillis() - count * BAR_DURATION;
        double price = 100.0;

        for (int i = 0; i < count; i++) {
            long time = baseTime + i * BAR_DURATION;
            double change = (random.nextDouble() - 0.5) * 2.0;
            double open = price;
            double high = Math.max(open + random.nextDouble() * 1.5, open);
            double low = Math.min(open - random.nextDouble() * 1.5, open);
            price = price + change;
            double close = price;

            high = Math.max(high, Math.max(open, close));
            low = Math.min(low, Math.min(open, close));

            series.append(time, (float) open, (float) high, (float) low, (float) close, 1000);
        }

        return series;
    }

    private void updateAxisRange(YAxis axis, OhlcData series) {
        if (axis == null || series.size() == 0) return;

        float minPrice = Float.MAX_VALUE;
        float maxPrice = Float.MIN_VALUE;
        for (int i = 0; i < series.size(); i++) {
            minPrice = Math.min(minPrice, series.getLow(i));
            maxPrice = Math.max(maxPrice, series.getHigh(i));
        }
        float padding = (maxPrice - minPrice) * 0.1f;
        axis.setValueRange(minPrice - padding, maxPrice + padding);
    }

    private RenderContext createRenderContext(RenderDevice device, ResourceManager resources,
                                               Viewport viewport, MultiAxisCoordinateSystem coordinates,
                                               YAxisManager axisManager, OhlcData series) {
        RenderContext ctx = new RenderContext(null, viewport, coordinates, axisManager, null);
        ctx.setDevice(device);
        ctx.setResourceManager(resources);

        if (series != null && series.size() > 0) {
            ctx.setVisibleRange(0, series.size() - 1);
            ctx.setBarDuration(BAR_DURATION);
        }

        return ctx;
    }

    private void printSummary() {
        log.info("");
        log.info("========================================");
        log.info("BENCHMARK SUMMARY");
        log.info("========================================");

        if (results.isEmpty()) {
            log.info("No benchmark results available.");
            return;
        }

        // Group by backend
        List<BenchmarkResult> vulkanResults = results.stream()
                .filter(r -> "Vulkan".equals(r.getBackend()))
                .toList();

        log.info("");
        log.info("--- Vulkan Results ---");
        for (BenchmarkResult r : vulkanResults) {
            log.info("  {:>7} bars: {:>6.2f}ms avg ({:>6.1f} FPS), p95={:>6.2f}ms",
                    r.getDataSize(), r.getAverageFrameTime(), r.getAverageFPS(), r.getP95FrameTime());
        }

        // Scalability analysis
        if (vulkanResults.size() >= 2) {
            log.info("");
            log.info("--- Scalability Analysis ---");
            BenchmarkResult first = vulkanResults.get(0);
            for (int i = 1; i < vulkanResults.size(); i++) {
                BenchmarkResult current = vulkanResults.get(i);
                double dataRatio = (double) current.getDataSize() / first.getDataSize();
                double timeRatio = current.getAverageFrameTime() / first.getAverageFrameTime();
                log.info("  {}x data -> {:.2f}x time (ideal: {:.2f}x)",
                        (int) dataRatio, timeRatio, dataRatio);
            }
        }

        log.info("");
        log.info("========================================");
    }

    /**
     * Main entry point for running benchmarks.
     */
    public static void main(String[] args) {
        // Configure logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

        RenderBenchmark benchmark = new RenderBenchmark();
        benchmark.runFullSuite();

        // Exit after benchmarks complete
        System.exit(0);
    }
}
