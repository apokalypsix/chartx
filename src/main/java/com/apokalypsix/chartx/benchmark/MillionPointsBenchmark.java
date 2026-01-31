package com.apokalypsix.chartx.benchmark;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.core.render.api.RenderBackend;
import com.apokalypsix.chartx.core.render.api.RenderBackendFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Million Points Benchmark - ChartX performance stress test.
 *
 * <p>
 * This benchmark renders 1,000,000 data points to demonstrate ChartX's
 * GPU-accelerated rendering capabilities. It provides interactive controls for
 * zooming, panning, and toggling sampling, along with real-time performance
 * metrics.
 *
 * <p>
 * Inspired by ChartGPU's million-points example:
 * https://github.com/ChartGPU/ChartGPU/tree/main/examples/million-points
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * java -cp chartx-benchmarks.jar com.apokalypsix.chartx.benchmark.MillionPointsBenchmark
 * </pre>
 */
public class MillionPointsBenchmark {

	private static final int TOTAL_POINTS = 1_000_000;
	private static final int DEFAULT_WIDTH = 1280;
	private static final int DEFAULT_HEIGHT = 720;
	private static final int METRICS_UPDATE_INTERVAL_MS = 100;
	private static final int ROLLING_WINDOW_SIZE = 60;

	// UI Components
	private JFrame frame;
	private Chart chart;
	private JLabel frameIdLabel;
	private JLabel fpsLabel;
	private JLabel frameTimeLabel;
	private JLabel pointsRenderedLabel;
	private JLabel totalPointsLabel;
	private JCheckBox samplingCheckbox;
	private JCheckBox benchmarkModeCheckbox;
	private JButton resetZoomButton;
	private JSlider zoomSlider;

	// Performance tracking
	private final RollingStats frameTimeStats = new RollingStats(ROLLING_WINDOW_SIZE);
	private final AtomicLong lastFrameNanos = new AtomicLong(System.nanoTime());
	private final AtomicLong frameCounter = new AtomicLong(0);
	private final AtomicBoolean benchmarkMode = new AtomicBoolean(true);
	private final AtomicBoolean running = new AtomicBoolean(true);
	private volatile int visiblePointCount = TOTAL_POINTS;

	// Data
	private XyData data;
	private long dataMinTime;
	private long dataMaxTime;

	public static void main(String[] args) {
		// Configure logging
		configureLogging();

		// Check backend availability
		RenderBackend backend = selectBestBackend();
		System.out.println("Million Points Benchmark");
		System.out.println("========================");
		System.out.println("Backend: " + backend);
		System.out.println("Points: " + NumberFormat.getInstance().format(TOTAL_POINTS));
		System.out.println();

		SwingUtilities.invokeLater(() -> {
			try {
				new MillionPointsBenchmark().run(backend);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Failed to start benchmark: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}

	private void run(RenderBackend backend) {
		// Generate data
		System.out.print("Generating " + NumberFormat.getInstance().format(TOTAL_POINTS) + " points... ");
		long startGen = System.nanoTime();
		data = generateData(TOTAL_POINTS);
		dataMinTime = data.getMinX();
		dataMaxTime = data.getMaxX();
		System.out.printf("done in %.1fms%n", (System.nanoTime() - startGen) / 1_000_000.0);

		// Create UI
		createUI(backend);

		// Start render loop
		startRenderLoop();

		// Start metrics update timer
		startMetricsUpdater();
	}

	private void createUI(RenderBackend backend) {
		frame = new JFrame("Million Points - ChartX (" + backend + ")");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		// Create chart
		chart = new Chart("million-points", backend);
		chart.addLineSeries(data, new LineSeriesOptions().color(new Color(100, 180, 255)).lineWidth(1.0f));
		chart.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

		// Create control panel
		JPanel controlPanel = createControlPanel();

		// Create metrics panel
		JPanel metricsPanel = createMetricsPanel();

		// Layout
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(controlPanel, BorderLayout.WEST);
		topPanel.add(metricsPanel, BorderLayout.EAST);

		frame.add(topPanel, BorderLayout.NORTH);
		frame.add(chart, BorderLayout.CENTER);

		// Handle resize
		chart.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				scheduleRepaint();
			}
		});

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		// Initialize chart view
		SwingUtilities.invokeLater(() -> {
			chart.setVisibleRange(dataMinTime, dataMaxTime);
			chart.repaint();
		});
	}

	private JPanel createControlPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		panel.setBorder(new EmptyBorder(5, 10, 5, 10));

		// Sampling toggle
		samplingCheckbox = new JCheckBox("Sampling (LTTB)", true);
		samplingCheckbox.setToolTipText("Enable/disable data sampling for performance");
		samplingCheckbox.addActionListener(this::onSamplingToggle);
		panel.add(samplingCheckbox);

		// Benchmark mode toggle
		benchmarkModeCheckbox = new JCheckBox("Benchmark mode", true);
		benchmarkModeCheckbox.setToolTipText("Continuous rendering vs. render-on-demand");
		benchmarkModeCheckbox.addActionListener(e -> benchmarkMode.set(benchmarkModeCheckbox.isSelected()));
		panel.add(benchmarkModeCheckbox);

		panel.add(Box.createHorizontalStrut(20));

		// Reset zoom button
		resetZoomButton = new JButton("Reset Zoom");
		resetZoomButton.addActionListener(e -> {
			chart.setVisibleRange(dataMinTime, dataMaxTime);
			zoomSlider.setValue(0);
			scheduleRepaint();
		});
		panel.add(resetZoomButton);

		// Zoom slider
		panel.add(new JLabel("Zoom:"));
		zoomSlider = new JSlider(0, 100, 0);
		zoomSlider.setPreferredSize(new Dimension(150, 25));
		zoomSlider.addChangeListener(e -> {
			if (!zoomSlider.getValueIsAdjusting()) {
				applyZoom(zoomSlider.getValue());
			}
		});
		panel.add(zoomSlider);

		return panel;
	}

	private JPanel createMetricsPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
		panel.setBorder(new EmptyBorder(5, 10, 5, 10));

		Font monoFont = new Font(Font.MONOSPACED, Font.BOLD, 12);

		// Frame ID
		panel.add(createMetricLabel("Frame:"));
		frameIdLabel = createMetricValue("0", monoFont);
		panel.add(frameIdLabel);

		// FPS
		panel.add(createMetricLabel("FPS:"));
		fpsLabel = createMetricValue("--", monoFont);
		panel.add(fpsLabel);

		// Frame time
		panel.add(createMetricLabel("Time:"));
		frameTimeLabel = createMetricValue("-- ms", monoFont);
		panel.add(frameTimeLabel);

		// Points rendered
		panel.add(createMetricLabel("Rendered:"));
		pointsRenderedLabel = createMetricValue("--", monoFont);
		panel.add(pointsRenderedLabel);

		// Total points
		panel.add(createMetricLabel("Total:"));
		totalPointsLabel = createMetricValue(formatNumber(TOTAL_POINTS), monoFont);
		panel.add(totalPointsLabel);

		return panel;
	}

	private JLabel createMetricLabel(String text) {
		JLabel label = new JLabel(text);
		label.setForeground(Color.GRAY);
		return label;
	}

	private JLabel createMetricValue(String text, Font font) {
		JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(new Color(100, 200, 100));
		return label;
	}

	private void onSamplingToggle(ActionEvent e) {
		// TODO: Toggle LOD/sampling when ChartX supports it via API
		// For now, this is a placeholder for future sampling toggle
		scheduleRepaint();
	}

	private void applyZoom(int zoomLevel) {
		if (zoomLevel == 0) {
			chart.setVisibleRange(dataMinTime, dataMaxTime);
		} else {
			// Zoom into center, exponential scale
			double zoomFactor = Math.pow(0.99, zoomLevel);
			long totalRange = dataMaxTime - dataMinTime;
			long newRange = (long) (totalRange * zoomFactor);
			long center = dataMinTime + totalRange / 2;
			long newStart = center - newRange / 2;
			long newEnd = center + newRange / 2;
			chart.setVisibleRange(newStart, newEnd);
		}
		scheduleRepaint();
	}

	private void startRenderLoop() {
		Thread renderThread = new Thread(() -> {
			while (running.get()) {
				if (benchmarkMode.get()) {
					SwingUtilities.invokeLater(this::renderFrame);
					try {
						Thread.sleep(1); // ~1000 FPS max, GPU-bound in practice
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					try {
						Thread.sleep(16); // ~60 FPS when idle
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "MillionPoints-RenderLoop");
		renderThread.setDaemon(true);
		renderThread.start();
	}

	private void renderFrame() {
		long startNanos = System.nanoTime();

		// Force synchronous repaint
		chart.paintImmediately(0, 0, chart.getWidth(), chart.getHeight());

		long endNanos = System.nanoTime();
		double frameMs = (endNanos - startNanos) / 1_000_000.0;
		frameTimeStats.add(frameMs);
		lastFrameNanos.set(endNanos);
		frameCounter.incrementAndGet();

		// Update visible point count (approximate based on zoom)
		updateVisiblePointCount();
	}

	private void scheduleRepaint() {
		SwingUtilities.invokeLater(this::renderFrame);
	}

	private void updateVisiblePointCount() {
		// Estimate visible points based on current visible range
		// This is an approximation; actual rendered points may differ due to LOD
		Viewport viewport = chart.getViewport();
		if (viewport != null) {
			long visibleStart = viewport.getStartTime();
			long visibleEnd = viewport.getEndTime();
			long totalRange = dataMaxTime - dataMinTime;
			if (totalRange > 0) {
				double fraction = (double) (visibleEnd - visibleStart) / totalRange;
				visiblePointCount = (int) (TOTAL_POINTS * Math.min(1.0, Math.max(0.0, fraction)));
			}
		}
	}

	private void startMetricsUpdater() {
		// Use a background thread for scheduling to avoid EDT congestion
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "MillionPoints-MetricsUpdater");
			t.setDaemon(true);
			return t;
		});
		scheduler.scheduleAtFixedRate(
				() -> SwingUtilities.invokeLater(this::updateMetrics),
				0,
				METRICS_UPDATE_INTERVAL_MS,
				TimeUnit.MILLISECONDS);
	}

	private void updateMetrics() {
		double avgFrameTime = frameTimeStats.getAverage();
		double fps = avgFrameTime > 0 ? 1000.0 / avgFrameTime : 0;

		frameIdLabel.setText(formatNumber((int) frameCounter.get()));
		fpsLabel.setText(String.format("%.1f", fps));
		frameTimeLabel.setText(String.format("%.2f ms", avgFrameTime));
		pointsRenderedLabel.setText(formatNumber(visiblePointCount));
	}

	/**
	 * Generates synthetic data matching ChartGPU's million-points example.
	 * Uses deterministic xorshift32 PRNG for reproducibility.
	 *
	 * @see <a href="https://github.com/ChartGPU/ChartGPU/blob/main/examples/million-points/main.ts">ChartGPU source</a>
	 */
	private static XyData generateData(int count) {
		XyData data = new XyData("data", "Signal", count);

		// Match ChartGPU's parameters exactly
		final double freq = 0.012;
		final double lowFreq = 0.0017;
		final double noiseAmp = 0.35;

		// xorshift32 PRNG with ChartGPU's seed
		int state = 0x12345678;

		// Use millisecond timestamps (1ms per point for 1M points = ~16 minutes of data)
		long baseTime = System.currentTimeMillis() - count;

		for (int i = 0; i < count; i++) {
			// Generate next random value [0, 1)
			state ^= state << 13;
			state ^= state >>> 17;
			state ^= state << 5;
			double rand01 = (state >>> 0 & 0xFFFFFFFFL) / 4294967296.0;

			// Match ChartGPU's formula exactly:
			// y = sin(i * freq) * 0.95 + sin(i * lowFreq + 1.1) * 0.6 + (rand01 - 0.5) * noiseAmp
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

	private static RenderBackend selectBestBackend() {
		// Prefer Metal on macOS, then Vulkan, then OpenGL
		if (RenderBackendFactory.isBackendAvailable(RenderBackend.METAL)) {
			return RenderBackend.METAL;
		}

		if (RenderBackendFactory.isBackendAvailable(RenderBackend.OPENGL)) {
			return RenderBackend.OPENGL;
		}
		if (RenderBackendFactory.isBackendAvailable(RenderBackend.VULKAN)) {
			return RenderBackend.VULKAN;
		}
		throw new RuntimeException("No rendering backend available!");
	}

	private static void configureLogging() {
		try {
			Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
			Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
			Object loggerContext = org.slf4j.LoggerFactory.getILoggerFactory();
			Object rootLogger = loggerContextClass.getMethod("getLogger", String.class).invoke(loggerContext,
					org.slf4j.Logger.ROOT_LOGGER_NAME);
			Object errorLevel = levelClass.getField("ERROR").get(null);
			rootLogger.getClass().getMethod("setLevel", levelClass).invoke(rootLogger, errorLevel);
		} catch (Exception e) {
			// Ignore
		}
	}

	/**
	 * Rolling statistics for smoothed performance metrics.
	 */
	private static class RollingStats {
		private final double[] values;
		private int index;
		private int count;
		private double sum;

		RollingStats(int windowSize) {
			this.values = new double[windowSize];
		}

		synchronized void add(double value) {
			if (count == values.length) {
				sum -= values[index];
			} else {
				count++;
			}
			values[index] = value;
			sum += value;
			index = (index + 1) % values.length;
		}

		synchronized double getAverage() {
			return count > 0 ? sum / count : 0;
		}
	}
}
