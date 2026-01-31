package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Micro-benchmarks for specific rendering operations.
 *
 * <p>Tests individual components of the rendering pipeline:
 * <ul>
 *   <li>Coordinate transformations</li>
 *   <li>Vertex array building</li>
 *   <li>Buffer uploads</li>
 *   <li>Draw calls</li>
 * </ul>
 */
public class MicroBenchmark {

    private static final Logger log = LoggerFactory.getLogger(MicroBenchmark.class);

    private static final int WARMUP = 100;
    private static final int ITERATIONS = 1000;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("ChartX Micro-Benchmarks");
        System.out.println("========================================");
        System.out.println();

        // Run CPU-only benchmarks (no GPU required)
        benchmarkCoordinateTransforms();
        benchmarkVertexArrayBuilding();
        benchmarkDataAccess();

        // Run GPU benchmarks if Vulkan available
        if (RenderBackendFactory.isBackendAvailable(RenderBackend.VULKAN)) {
            benchmarkBufferUploads();
        } else {
            System.out.println("Vulkan not available - skipping GPU benchmarks");
        }

        System.out.println("========================================");
    }

    /**
     * Benchmarks coordinate system transformations (time->screen, price->screen).
     */
    private static void benchmarkCoordinateTransforms() {
        System.out.println("--- Coordinate Transform Benchmark ---");

        int[] sizes = {1000, 10000, 100000};

        for (int size : sizes) {
            // Setup
            Viewport viewport = new Viewport();
            viewport.setSize(1280, 720);
            viewport.setInsets(50, 60, 10, 30);

            long startTime = System.currentTimeMillis() - size * 60000L;
            long endTime = System.currentTimeMillis();
            viewport.setTimeRange(startTime, endTime);

            YAxisManager axisManager = new YAxisManager();
            YAxis priceAxis = axisManager.getDefaultAxis();
            priceAxis.setValueRange(50.0, 150.0);

            MultiAxisCoordinateSystem coords = new MultiAxisCoordinateSystem(viewport, axisManager);

            // Generate test timestamps and prices
            long[] timestamps = new long[size];
            float[] prices = new float[size];
            Random random = new Random(42);
            for (int i = 0; i < size; i++) {
                timestamps[i] = startTime + i * 60000L;
                prices[i] = 50 + random.nextFloat() * 100;
            }

            // Warmup
            for (int w = 0; w < WARMUP; w++) {
                for (int i = 0; i < size; i++) {
                    coords.xValueToScreenX(timestamps[i]);
                    coords.yValueToScreenY(prices[i], YAxis.DEFAULT_AXIS_ID);
                }
            }

            // Measure
            long start = System.nanoTime();
            for (int iter = 0; iter < ITERATIONS; iter++) {
                for (int i = 0; i < size; i++) {
                    coords.xValueToScreenX(timestamps[i]);
                    coords.yValueToScreenY(prices[i], YAxis.DEFAULT_AXIS_ID);
                }
            }
            long elapsed = System.nanoTime() - start;

            double totalTransforms = (double) size * ITERATIONS * 2; // x + y
            double msPerIteration = elapsed / 1_000_000.0 / ITERATIONS;
            double transformsPerMs = totalTransforms / ITERATIONS / msPerIteration;

            System.out.printf("  %,7d transforms: %.3f ms/iter, %.0f transforms/ms%n",
                    size * 2, msPerIteration, transformsPerMs);
        }
        System.out.println();
    }

    /**
     * Benchmarks vertex array construction for candlestick rendering.
     */
    private static void benchmarkVertexArrayBuilding() {
        System.out.println("--- Vertex Array Building Benchmark ---");

        int[] sizes = {1000, 10000, 100000};

        for (int size : sizes) {
            // Create test data
            OhlcData series = createTestData(size);

            // Pre-allocate arrays (6 floats per vertex, 6 vertices per candle body)
            int floatsPerCandle = 6 * 6; // 6 floats/vertex * 6 vertices
            float[] bodyVertices = new float[size * floatsPerCandle];
            float[] wickVertices = new float[size * 2 * 6]; // 2 vertices per wick

            // Warmup
            for (int w = 0; w < WARMUP; w++) {
                buildVertices(series, bodyVertices, wickVertices);
            }

            // Measure
            long start = System.nanoTime();
            for (int iter = 0; iter < ITERATIONS; iter++) {
                buildVertices(series, bodyVertices, wickVertices);
            }
            long elapsed = System.nanoTime() - start;

            double msPerIteration = elapsed / 1_000_000.0 / ITERATIONS;
            double candlesPerMs = size / msPerIteration;

            System.out.printf("  %,7d candles: %.3f ms/iter, %.0f candles/ms%n",
                    size, msPerIteration, candlesPerMs);
        }
        System.out.println();
    }

    /**
     * Benchmarks raw data access patterns (array iteration vs random access).
     */
    private static void benchmarkDataAccess() {
        System.out.println("--- Data Access Benchmark ---");

        int size = 100000;
        OhlcData series = createTestData(size);

        // Sequential access benchmark
        long start = System.nanoTime();
        float sum = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (int i = 0; i < size; i++) {
                sum += series.getOpen(i) + series.getHigh(i) + series.getLow(i) + series.getClose(i);
            }
        }
        long seqElapsed = System.nanoTime() - start;

        // Random access benchmark
        Random random = new Random(42);
        int[] indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = random.nextInt(size);
        }

        start = System.nanoTime();
        sum = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (int i = 0; i < size; i++) {
                int idx = indices[i];
                sum += series.getOpen(idx) + series.getHigh(idx) + series.getLow(idx) + series.getClose(idx);
            }
        }
        long randElapsed = System.nanoTime() - start;

        double seqMsPerIter = seqElapsed / 1_000_000.0 / ITERATIONS;
        double randMsPerIter = randElapsed / 1_000_000.0 / ITERATIONS;

        System.out.printf("  Sequential access: %.3f ms/iter (%.0f bars/ms)%n",
                seqMsPerIter, size / seqMsPerIter);
        System.out.printf("  Random access:     %.3f ms/iter (%.0f bars/ms)%n",
                randMsPerIter, size / randMsPerIter);
        System.out.printf("  Random/Sequential ratio: %.2fx slower%n", randMsPerIter / seqMsPerIter);
        System.out.println();
    }

    /**
     * Benchmarks Vulkan buffer upload operations.
     */
    private static void benchmarkBufferUploads() {
        System.out.println("--- Buffer Upload Benchmark (Vulkan) ---");

        RenderDevice device = null;
        ResourceManager resources = null;

        try {
            device = RenderBackendFactory.createDevice(RenderBackend.VULKAN, null);
            device.initialize();

            resources = RenderBackendFactory.createResourceManager(device);
            resources.initialize(device);

            int[] sizes = {1000, 10000, 100000, 500000};

            for (int floatCount : sizes) {
                // Create test data
                float[] data = new float[floatCount];
                Random random = new Random(42);
                for (int i = 0; i < floatCount; i++) {
                    data[i] = random.nextFloat();
                }

                // Create buffer
                BufferDescriptor desc = BufferDescriptor.positionColor2D(floatCount);
                Buffer buffer = resources.getOrCreateBuffer("bench_" + floatCount, desc);

                // Warmup
                for (int w = 0; w < WARMUP; w++) {
                    buffer.upload(data, 0, floatCount);
                }

                // Measure
                long start = System.nanoTime();
                for (int iter = 0; iter < ITERATIONS; iter++) {
                    buffer.upload(data, 0, floatCount);
                }
                long elapsed = System.nanoTime() - start;

                double msPerUpload = elapsed / 1_000_000.0 / ITERATIONS;
                double mbPerSec = (floatCount * 4.0 / 1_000_000) / (msPerUpload / 1000);

                System.out.printf("  %,9d floats (%,6d KB): %.3f ms/upload, %.1f MB/s%n",
                        floatCount, floatCount * 4 / 1024, msPerUpload, mbPerSec);

                // Cleanup
                resources.disposeBuffer("bench_" + floatCount);
            }

        } finally {
            if (resources != null) resources.dispose();
            if (device != null) device.dispose();
        }
        System.out.println();
    }

    // Helper methods

    private static OhlcData createTestData(int count) {
        OhlcData series = new OhlcData("BENCH", "Benchmark");

        Random random = new Random(42);
        long baseTime = System.currentTimeMillis() - count * 60000L;
        double price = 100.0;

        for (int i = 0; i < count; i++) {
            long time = baseTime + i * 60000L;
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

    private static void buildVertices(OhlcData series, float[] bodyVertices, float[] wickVertices) {
        int bodyOffset = 0;
        int wickOffset = 0;

        float barWidth = 8.0f;
        float halfWidth = barWidth / 2;

        for (int i = 0; i < series.size(); i++) {
            float open = series.getOpen(i);
            float high = series.getHigh(i);
            float low = series.getLow(i);
            float close = series.getClose(i);

            // Simulate coordinate transform (simplified)
            float x = i * 10.0f;
            float yOpen = 720 - open * 5;
            float yClose = 720 - close * 5;
            float yHigh = 720 - high * 5;
            float yLow = 720 - low * 5;

            // Color based on direction
            float r = close >= open ? 0.2f : 0.8f;
            float g = close >= open ? 0.7f : 0.2f;
            float b = 0.3f;
            float a = 1.0f;

            // Body vertices (2 triangles = 6 vertices)
            float top = Math.min(yOpen, yClose);
            float bottom = Math.max(yOpen, yClose);

            // Triangle 1
            bodyVertices[bodyOffset++] = x - halfWidth; bodyVertices[bodyOffset++] = top;
            bodyVertices[bodyOffset++] = r; bodyVertices[bodyOffset++] = g;
            bodyVertices[bodyOffset++] = b; bodyVertices[bodyOffset++] = a;

            bodyVertices[bodyOffset++] = x + halfWidth; bodyVertices[bodyOffset++] = top;
            bodyVertices[bodyOffset++] = r; bodyVertices[bodyOffset++] = g;
            bodyVertices[bodyOffset++] = b; bodyVertices[bodyOffset++] = a;

            bodyVertices[bodyOffset++] = x - halfWidth; bodyVertices[bodyOffset++] = bottom;
            bodyVertices[bodyOffset++] = r; bodyVertices[bodyOffset++] = g;
            bodyVertices[bodyOffset++] = b; bodyVertices[bodyOffset++] = a;

            // Triangle 2
            bodyVertices[bodyOffset++] = x + halfWidth; bodyVertices[bodyOffset++] = top;
            bodyVertices[bodyOffset++] = r; bodyVertices[bodyOffset++] = g;
            bodyVertices[bodyOffset++] = b; bodyVertices[bodyOffset++] = a;

            bodyVertices[bodyOffset++] = x + halfWidth; bodyVertices[bodyOffset++] = bottom;
            bodyVertices[bodyOffset++] = r; bodyVertices[bodyOffset++] = g;
            bodyVertices[bodyOffset++] = b; bodyVertices[bodyOffset++] = a;

            bodyVertices[bodyOffset++] = x - halfWidth; bodyVertices[bodyOffset++] = bottom;
            bodyVertices[bodyOffset++] = r; bodyVertices[bodyOffset++] = g;
            bodyVertices[bodyOffset++] = b; bodyVertices[bodyOffset++] = a;

            // Wick vertices (1 line = 2 vertices)
            wickVertices[wickOffset++] = x; wickVertices[wickOffset++] = yHigh;
            wickVertices[wickOffset++] = r; wickVertices[wickOffset++] = g;
            wickVertices[wickOffset++] = b; wickVertices[wickOffset++] = a;

            wickVertices[wickOffset++] = x; wickVertices[wickOffset++] = yLow;
            wickVertices[wickOffset++] = r; wickVertices[wickOffset++] = g;
            wickVertices[wickOffset++] = b; wickVertices[wickOffset++] = a;
        }
    }
}
