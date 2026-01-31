package com.apokalypsix.chartx.benchmark;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Stores benchmark results for a single test run.
 */
public class BenchmarkResult {

    private final String name;
    private final String backend;
    private final int dataSize;
    private final List<Double> frameTimes = new ArrayList<>();
    private final List<Double> uploadTimes = new ArrayList<>();
    private final List<Double> drawTimes = new ArrayList<>();
    private final List<Double> transformTimes = new ArrayList<>();

    private long totalVertices;
    private long totalDrawCalls;
    private int warmupFrames;
    private int measureFrames;

    public BenchmarkResult(String name, String backend, int dataSize) {
        this.name = name;
        this.backend = backend;
        this.dataSize = dataSize;
    }

    public void addFrameTime(double ms) {
        frameTimes.add(ms);
    }

    public void addUploadTime(double ms) {
        uploadTimes.add(ms);
    }

    public void addDrawTime(double ms) {
        drawTimes.add(ms);
    }

    public void addTransformTime(double ms) {
        transformTimes.add(ms);
    }

    public void setTotalVertices(long vertices) {
        this.totalVertices = vertices;
    }

    public void setTotalDrawCalls(long drawCalls) {
        this.totalDrawCalls = drawCalls;
    }

    public void setWarmupFrames(int frames) {
        this.warmupFrames = frames;
    }

    public void setMeasureFrames(int frames) {
        this.measureFrames = frames;
    }

    // Statistics helpers
    public DoubleSummaryStatistics getFrameTimeStats() {
        return frameTimes.stream().mapToDouble(d -> d).summaryStatistics();
    }

    public DoubleSummaryStatistics getUploadTimeStats() {
        return uploadTimes.stream().mapToDouble(d -> d).summaryStatistics();
    }

    public DoubleSummaryStatistics getDrawTimeStats() {
        return drawTimes.stream().mapToDouble(d -> d).summaryStatistics();
    }

    public DoubleSummaryStatistics getTransformTimeStats() {
        return transformTimes.stream().mapToDouble(d -> d).summaryStatistics();
    }

    public double getAverageFrameTime() {
        return getFrameTimeStats().getAverage();
    }

    public double getAverageFPS() {
        double avgMs = getAverageFrameTime();
        return avgMs > 0 ? 1000.0 / avgMs : 0;
    }

    public double getP95FrameTime() {
        if (frameTimes.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(frameTimes);
        sorted.sort(Double::compareTo);
        int index = (int) (sorted.size() * 0.95);
        return sorted.get(Math.min(index, sorted.size() - 1));
    }

    public double getP99FrameTime() {
        if (frameTimes.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(frameTimes);
        sorted.sort(Double::compareTo);
        int index = (int) (sorted.size() * 0.99);
        return sorted.get(Math.min(index, sorted.size() - 1));
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getBackend() {
        return backend;
    }

    public int getDataSize() {
        return dataSize;
    }

    public long getTotalVertices() {
        return totalVertices;
    }

    public long getTotalDrawCalls() {
        return totalDrawCalls;
    }

    public int getWarmupFrames() {
        return warmupFrames;
    }

    public int getMeasureFrames() {
        return measureFrames;
    }

    public List<Double> getFrameTimes() {
        return frameTimes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %s [%s] - %,d bars ===\n", name, backend, dataSize));
        sb.append(String.format("  Frames: %d warmup + %d measured\n", warmupFrames, measureFrames));
        sb.append(String.format("  Frame Time: avg=%.2fms, min=%.2fms, max=%.2fms\n",
                getFrameTimeStats().getAverage(),
                getFrameTimeStats().getMin(),
                getFrameTimeStats().getMax()));
        sb.append(String.format("  Frame Time: p95=%.2fms, p99=%.2fms\n", getP95FrameTime(), getP99FrameTime()));
        sb.append(String.format("  FPS: %.1f avg\n", getAverageFPS()));

        if (!uploadTimes.isEmpty()) {
            sb.append(String.format("  Upload Time: avg=%.3fms\n", getUploadTimeStats().getAverage()));
        }
        if (!drawTimes.isEmpty()) {
            sb.append(String.format("  Draw Time: avg=%.3fms\n", getDrawTimeStats().getAverage()));
        }
        if (totalVertices > 0) {
            sb.append(String.format("  Vertices: %,d total, %,d per frame\n",
                    totalVertices, totalVertices / Math.max(1, measureFrames)));
        }
        if (totalDrawCalls > 0) {
            sb.append(String.format("  Draw Calls: %,d total, %d per frame\n",
                    totalDrawCalls, totalDrawCalls / Math.max(1, measureFrames)));
        }

        return sb.toString();
    }

    /**
     * Creates a comparison report between two results.
     */
    public static String compare(BenchmarkResult a, BenchmarkResult b) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== Comparison: %s vs %s (%,d bars) ===\n",
                a.getBackend(), b.getBackend(), a.getDataSize()));

        double aAvg = a.getAverageFrameTime();
        double bAvg = b.getAverageFrameTime();
        double speedup = aAvg / bAvg;

        sb.append(String.format("  %s: %.2fms avg (%.1f FPS)\n", a.getBackend(), aAvg, a.getAverageFPS()));
        sb.append(String.format("  %s: %.2fms avg (%.1f FPS)\n", b.getBackend(), bAvg, b.getAverageFPS()));

        if (speedup > 1.0) {
            sb.append(String.format("  Winner: %s (%.2fx faster)\n", b.getBackend(), speedup));
        } else if (speedup < 1.0) {
            sb.append(String.format("  Winner: %s (%.2fx faster)\n", a.getBackend(), 1.0 / speedup));
        } else {
            sb.append("  Result: Equivalent performance\n");
        }

        // P95 comparison
        double aP95 = a.getP95FrameTime();
        double bP95 = b.getP95FrameTime();
        sb.append(String.format("  P95: %s=%.2fms, %s=%.2fms\n",
                a.getBackend(), aP95, b.getBackend(), bP95));

        return sb.toString();
    }
}
