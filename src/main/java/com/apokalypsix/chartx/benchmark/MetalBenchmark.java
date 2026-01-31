package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Metal backend benchmark with bar counts matching OpenGL/Vulkan benchmarks.
 */
public class MetalBenchmark {

    private static final Logger log = LoggerFactory.getLogger(MetalBenchmark.class);

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP = 100;
    private static final int MEASURE = 500;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Metal Backend Benchmark");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Resolution: " + WIDTH + "x" + HEIGHT);
        System.out.println("  Warmup iterations: " + WARMUP);
        System.out.println("  Measure iterations: " + MEASURE);
        System.out.println();

        if (!RenderBackendFactory.isBackendAvailable(RenderBackend.METAL)) {
            System.out.println("ERROR: Metal is not available!");
            return;
        }

        try {
            RenderDevice device = RenderBackendFactory.createDevice(RenderBackend.METAL, null);
            device.initialize();
            System.out.println("Metal device initialized");

            ResourceManager resources = RenderBackendFactory.createResourceManager(device);
            resources.initialize(device);

            device.setViewport(0, 0, WIDTH, HEIGHT);

            // Bar counts matching existing benchmarks
            int[] barCounts = {1000, 5000, 10000, 50000, 100000, 1000000};

            System.out.println();
            System.out.println("| Data Size | Avg Frame Time | Min | Max | P95 | Throughput |");
            System.out.println("|-----------|----------------|-----|-----|-----|------------|");

            for (int barCount : barCounts) {
                BenchmarkResult result = runBarBenchmark(device, resources, barCount);

                double avgMs = result.getAverageFrameTime();
                double minMs = result.getFrameTimeStats().getMin();
                double maxMs = result.getFrameTimeStats().getMax();
                double p95Ms = result.getP95FrameTime();
                double throughput = barCount / avgMs / 1000.0; // M bars/s

                System.out.printf("| %,d bars | %.2fms | %.2fms | %.2fms | %.2fms | %.1fM bars/s |%n",
                        barCount, avgMs, minMs, maxMs, p95Ms, throughput);
            }

            resources.dispose();
            device.dispose();

            System.out.println();
            System.out.println("Benchmark completed successfully!");

        } catch (Exception e) {
            System.err.println("Metal benchmark FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static BenchmarkResult runBarBenchmark(RenderDevice device, ResourceManager resources, int barCount) {
        BenchmarkResult result = new BenchmarkResult("Bar Render", "Metal", barCount);
        result.setWarmupFrames(WARMUP);
        result.setMeasureFrames(MEASURE);

        // Each bar = 2 triangles for body + 2 triangles for wick = 4 triangles
        // Each triangle = 3 vertices, each vertex = 6 floats (x, y, r, g, b, a)
        int trianglesPerBar = 4;
        int floatsPerVertex = 6;
        int verticesPerTriangle = 3;
        int triangleCount = barCount * trianglesPerBar;
        int floatCount = triangleCount * verticesPerTriangle * floatsPerVertex;

        float[] vertices = generateBarVertices(barCount, WIDTH, HEIGHT);

        // Create buffer
        BufferDescriptor desc = BufferDescriptor.positionColor2D(floatCount);
        Buffer buffer = resources.getOrCreateBuffer("metal_bar_" + barCount, desc);

        buffer.upload(vertices, 0, floatCount);
        buffer.setVertexCount(triangleCount * verticesPerTriangle);

        Shader shader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            device.beginFrame();
            device.clearScreen(0.1f, 0.1f, 0.1f, 1.0f);
            device.setBlendMode(BlendMode.ALPHA);
            if (shader != null) shader.bind();
            buffer.draw(DrawMode.TRIANGLES);
            if (shader != null) shader.unbind();
            device.endFrame();
        }

        // Measure
        for (int i = 0; i < MEASURE; i++) {
            long startNanos = System.nanoTime();

            device.beginFrame();
            device.clearScreen(0.1f, 0.1f, 0.1f, 1.0f);
            device.setBlendMode(BlendMode.ALPHA);
            if (shader != null) shader.bind();
            buffer.draw(DrawMode.TRIANGLES);
            if (shader != null) shader.unbind();
            device.endFrame();

            long endNanos = System.nanoTime();
            result.addFrameTime((endNanos - startNanos) / 1_000_000.0);
        }

        resources.disposeBuffer("metal_bar_" + barCount);

        return result;
    }

    private static float[] generateBarVertices(int barCount, int width, int height) {
        int trianglesPerBar = 4;
        int floatsPerVertex = 6;
        int verticesPerTriangle = 3;
        float[] vertices = new float[barCount * trianglesPerBar * verticesPerTriangle * floatsPerVertex];

        Random random = new Random(42);
        int offset = 0;

        float barWidth = Math.max(1.0f, (float) width / barCount * 0.8f);

        for (int b = 0; b < barCount; b++) {
            float x = (float) b / barCount * width;

            // Random OHLC-like values
            float open = random.nextFloat() * height * 0.8f + height * 0.1f;
            float close = random.nextFloat() * height * 0.8f + height * 0.1f;
            float high = Math.max(open, close) + random.nextFloat() * 20;
            float low = Math.min(open, close) - random.nextFloat() * 20;

            boolean bullish = close > open;
            float r = bullish ? 0.2f : 0.8f;
            float g = bullish ? 0.8f : 0.2f;
            float b_ = 0.2f;
            float a = 1.0f;

            float bodyTop = Math.max(open, close);
            float bodyBottom = Math.min(open, close);

            // Body triangle 1
            vertices[offset++] = x; vertices[offset++] = bodyBottom;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = x + barWidth; vertices[offset++] = bodyBottom;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = x + barWidth; vertices[offset++] = bodyTop;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;

            // Body triangle 2
            vertices[offset++] = x; vertices[offset++] = bodyBottom;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = x + barWidth; vertices[offset++] = bodyTop;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = x; vertices[offset++] = bodyTop;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;

            // Wick (as thin triangles)
            float wickX = x + barWidth / 2;
            float wickWidth = Math.max(1.0f, barWidth * 0.1f);

            // Upper wick triangle 1
            vertices[offset++] = wickX - wickWidth/2; vertices[offset++] = bodyTop;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = wickX + wickWidth/2; vertices[offset++] = bodyTop;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = wickX; vertices[offset++] = high;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;

            // Lower wick triangle 2
            vertices[offset++] = wickX - wickWidth/2; vertices[offset++] = bodyBottom;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = wickX; vertices[offset++] = low;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
            vertices[offset++] = wickX + wickWidth/2; vertices[offset++] = bodyBottom;
            vertices[offset++] = r; vertices[offset++] = g; vertices[offset++] = b_; vertices[offset++] = a;
        }

        return vertices;
    }
}
