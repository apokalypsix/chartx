package com.apokalypsix.chartx.benchmark;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple profiler for measuring render operation timings.
 *
 * <p>Usage:
 * <pre>{@code
 * RenderProfiler.begin("vertexBuild");
 * // ... vertex building code ...
 * RenderProfiler.end("vertexBuild");
 *
 * RenderProfiler.printStats();
 * }</pre>
 */
public class RenderProfiler {

    private static final Map<String, TimingStats> stats = new HashMap<>();
    private static final Map<String, Long> activeTimers = new HashMap<>();
    private static boolean enabled = false;

    private RenderProfiler() {}

    /**
     * Enables or disables the profiler.
     * When disabled, begin/end calls have minimal overhead.
     */
    public static void setEnabled(boolean enabled) {
        RenderProfiler.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Begins timing for the specified operation.
     */
    public static void begin(String name) {
        if (!enabled) return;
        activeTimers.put(name, System.nanoTime());
    }

    /**
     * Ends timing for the specified operation.
     */
    public static void end(String name) {
        if (!enabled) return;
        Long startTime = activeTimers.remove(name);
        if (startTime != null) {
            long elapsed = System.nanoTime() - startTime;
            stats.computeIfAbsent(name, TimingStats::new).record(elapsed);
        }
    }

    /**
     * Records a single timing measurement.
     */
    public static void record(String name, long elapsedNanos) {
        if (!enabled) return;
        stats.computeIfAbsent(name, TimingStats::new).record(elapsedNanos);
    }

    /**
     * Resets all statistics.
     */
    public static void reset() {
        stats.clear();
        activeTimers.clear();
    }

    /**
     * Prints statistics for all recorded operations.
     */
    public static void printStats() {
        if (stats.isEmpty()) {
            System.out.println("No profiling data collected (profiler may be disabled)");
            return;
        }

        System.out.println("\n=== Render Profiler Stats ===");
        System.out.printf("%-25s | %10s | %10s | %10s | %10s | %8s%n",
                "Operation", "Avg (ms)", "Min (ms)", "Max (ms)", "Total (ms)", "Calls");
        System.out.println("-".repeat(85));

        stats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getTotalMs(), a.getValue().getTotalMs()))
                .forEach(e -> {
                    TimingStats s = e.getValue();
                    System.out.printf("%-25s | %10.3f | %10.3f | %10.3f | %10.1f | %8d%n",
                            e.getKey(),
                            s.getAverageMs(),
                            s.getMinMs(),
                            s.getMaxMs(),
                            s.getTotalMs(),
                            s.getCount());
                });

        System.out.println();
    }

    /**
     * Returns statistics for a specific operation.
     */
    public static TimingStats getStats(String name) {
        return stats.get(name);
    }

    /**
     * Timing statistics for a single operation.
     */
    public static class TimingStats {
        private final String name;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos = Long.MIN_VALUE;
        private int count;

        public TimingStats(String name) {
            this.name = name;
        }

        public void record(long nanos) {
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
            count++;
        }

        public String getName() { return name; }
        public long getTotalNanos() { return totalNanos; }
        public long getMinNanos() { return count > 0 ? minNanos : 0; }
        public long getMaxNanos() { return count > 0 ? maxNanos : 0; }
        public int getCount() { return count; }

        public double getTotalMs() { return totalNanos / 1_000_000.0; }
        public double getAverageMs() { return count > 0 ? getTotalMs() / count : 0; }
        public double getMinMs() { return getMinNanos() / 1_000_000.0; }
        public double getMaxMs() { return getMaxNanos() / 1_000_000.0; }
    }
}
