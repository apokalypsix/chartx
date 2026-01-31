package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.core.render.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Quick test to verify Metal backend functionality.
 */
public class MetalQuickTest {

    private static final Logger log = LoggerFactory.getLogger(MetalQuickTest.class);

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP = 10;
    private static final int MEASURE = 50;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Metal Backend Quick Test");
        System.out.println("========================================");
        System.out.println();

        if (!RenderBackendFactory.isBackendAvailable(RenderBackend.METAL)) {
            System.out.println("ERROR: Metal is not available!");
            return;
        }

        System.out.println("Metal is available, creating device...");

        try {
            RenderDevice device = RenderBackendFactory.createDevice(RenderBackend.METAL, null);
            System.out.println("Created Metal device");

            device.initialize();
            System.out.println("Initialized Metal device");

            ResourceManager resources = RenderBackendFactory.createResourceManager(device);
            resources.initialize(device);
            System.out.println("Created and initialized Metal resource manager");

            device.setViewport(0, 0, WIDTH, HEIGHT);

            int[] triangleCounts = {1000, 10000, 50000, 100000};

            for (int count : triangleCounts) {
                double avgMs = runTriangleBenchmark(device, resources, count);
                System.out.printf("  %,d triangles: %.3f ms avg%n", count, avgMs);
            }

            resources.dispose();
            device.dispose();

            System.out.println();
            System.out.println("Metal test completed successfully!");

        } catch (Exception e) {
            System.err.println("Metal test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static double runTriangleBenchmark(RenderDevice device, ResourceManager resources, int triangleCount) {
        // Create vertex data (6 floats per vertex: x, y, r, g, b, a; 3 vertices per triangle)
        int floatsPerVertex = 6;
        int verticesPerTriangle = 3;
        int floatCount = triangleCount * verticesPerTriangle * floatsPerVertex;

        float[] vertices = generateTriangleVertices(triangleCount, WIDTH, HEIGHT);

        // Create buffer
        BufferDescriptor desc = BufferDescriptor.positionColor2D(floatCount);
        Buffer buffer = resources.getOrCreateBuffer("metal_test_" + triangleCount, desc);

        // Upload vertices
        buffer.upload(vertices, 0, floatCount);
        buffer.setVertexCount(triangleCount * verticesPerTriangle);

        // Get default shader
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
        double totalMs = 0;
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
            totalMs += (endNanos - startNanos) / 1_000_000.0;
        }

        // Cleanup
        resources.disposeBuffer("metal_test_" + triangleCount);

        return totalMs / MEASURE;
    }

    private static float[] generateTriangleVertices(int triangleCount, int width, int height) {
        int floatsPerVertex = 6;
        int verticesPerTriangle = 3;
        float[] vertices = new float[triangleCount * verticesPerTriangle * floatsPerVertex];

        Random random = new Random(42);
        int offset = 0;

        for (int t = 0; t < triangleCount; t++) {
            float cx = random.nextFloat() * width;
            float cy = random.nextFloat() * height;
            float size = 5 + random.nextFloat() * 15;

            float r = random.nextFloat();
            float g = random.nextFloat();
            float b = random.nextFloat();
            float a = 0.5f + random.nextFloat() * 0.5f;

            // Vertex 1
            vertices[offset++] = cx;
            vertices[offset++] = cy - size;
            vertices[offset++] = r;
            vertices[offset++] = g;
            vertices[offset++] = b;
            vertices[offset++] = a;

            // Vertex 2
            vertices[offset++] = cx - size;
            vertices[offset++] = cy + size;
            vertices[offset++] = r;
            vertices[offset++] = g;
            vertices[offset++] = b;
            vertices[offset++] = a;

            // Vertex 3
            vertices[offset++] = cx + size;
            vertices[offset++] = cy + size;
            vertices[offset++] = r;
            vertices[offset++] = g;
            vertices[offset++] = b;
            vertices[offset++] = a;
        }

        return vertices;
    }
}
