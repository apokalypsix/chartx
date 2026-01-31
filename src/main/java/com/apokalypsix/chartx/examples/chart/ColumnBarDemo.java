package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.apokalypsix.chartx.chart.Chart;
import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.data.WaterfallData;
import com.apokalypsix.chartx.chart.data.XyData;
import com.apokalypsix.chartx.chart.series.ImpulseSeries;
import com.apokalypsix.chartx.chart.series.WaterfallSeries;
import com.apokalypsix.chartx.chart.style.ImpulseSeriesOptions;
import com.apokalypsix.chartx.chart.style.WaterfallSeriesOptions;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;

/**
 * Demo showcasing Waterfall and Impulse chart types.
 */
public class ColumnBarDemo extends AbstractDemo {

	private Chart chart;

	private WaterfallData waterfallData;
	private XyData impulseData;

	private WaterfallSeries waterfallSeries;
	private ImpulseSeries impulseSeries;

	private int currentChart = 0; // 0=waterfall, 1=impulse

	// Waterfall category labels
	private static final String[] WATERFALL_CATEGORIES = { "Start", "Products", "Services", "COGS", "OpEx", "Marketing",
			"Interest", "Other", "Net" };

	// Impulse category labels (signal positions)
	private static final int IMPULSE_POINTS = 50;
	private static final String[] IMPULSE_CATEGORIES = generateImpulseLabels(IMPULSE_POINTS);

	private static String[] generateImpulseLabels(int count) {
		String[] labels = new String[count];
		for (int i = 0; i < count; i++) {
			labels[i] = "T" + i;
		}
		return labels;
	}

	public ColumnBarDemo() {
		setWindowSize(1200, 800);
	}

	@Override
	protected String getDemoTitle() {
		return "ChartX Demo - Waterfall & Impulse Charts";
	}

	@Override
	protected Component createDemoContent() {
		chart = createChart();

		// Set up category axis (start with waterfall categories)
		chart.setXAxis(new CategoryAxis(CategoryAxis.DEFAULT_AXIS_ID, CategoryAxis.Position.BOTTOM));
		CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
		categoryAxis.setCategories(WATERFALL_CATEGORIES);
		categoryAxis.height(40);
		categoryAxis.labelColor(new Color(180, 180, 180));

		// Create waterfall data (financial analysis)
		waterfallData = createWaterfallData();

		// Create impulse data (signal spikes)
		impulseData = createImpulseData();

		// Configure visible range for waterfall categories
		chart.setVisibleRange(-1, WATERFALL_CATEGORIES.length);
		chart.setBarDuration(1);

		// Create series
		waterfallSeries = new WaterfallSeries(waterfallData,
				new WaterfallSeriesOptions().positiveColor(new Color(46, 204, 113))
						.negativeColor(new Color(231, 76, 60)).totalColor(new Color(52, 152, 219))
						.connectorColor(new Color(150, 150, 150)).barWidthRatio(0.6f).showConnectors(true));

		impulseSeries = new ImpulseSeries(impulseData,
				new ImpulseSeriesOptions().color(new Color(155, 89, 182)).lineWidth(2f)
						.markerShape(ImpulseSeriesOptions.MarkerShape.CIRCLE).markerSize(6f)
						.markerColor(new Color(241, 196, 15)).visible(false));

		// Add series to chart
		chart.addSeries(waterfallSeries);
		chart.addSeries(impulseSeries);

		return chart;
	}

	private WaterfallData createWaterfallData() {
		WaterfallData data = new WaterfallData("revenue", "Revenue Analysis");

		// Financial waterfall using category-based data
		data.append(1000, true, "Starting Revenue");
		data.append(450, "Product Sales");
		data.append(200, "Services");
		data.append(-380, "COGS");
		data.append(-220, "Operating Exp");
		data.append(-150, "Marketing");
		data.append(-30, "Interest");
		data.append(80, "Other Income");
		data.appendTotal("Net Income");

		return data;
	}

	private XyData createImpulseData() {
		XyData data = new XyData("signal", "Signal Impulses", IMPULSE_POINTS);
		Random rand = new Random(42);

		for (int i = 0; i < IMPULSE_POINTS; i++) {
			// Create sparse impulse pattern
			float value;
			if (rand.nextFloat() < 0.15) {
				// 15% chance of impulse
				value = 400 + (rand.nextFloat() - 0.5f) * 200;
			} else {
				value = 500; // Baseline
			}
			data.append(i, value); // Use category index
		}

		return data;
	}

	@Override
	protected JPanel createControlPanel() {
		JPanel panel = DemoUIHelper.createControlPanel();

		panel.add(DemoUIHelper.createInfoLabel("Chart Type:"));
		panel.add(DemoUIHelper.createHorizontalSpacer());

		ButtonGroup group = new ButtonGroup();

		JRadioButton waterfallBtn = new JRadioButton("Waterfall", true);
		waterfallBtn.setForeground(Color.WHITE);
		waterfallBtn.addActionListener(e -> {
			currentChart = 0;
			updateVisibility();
		});
		group.add(waterfallBtn);
		panel.add(waterfallBtn);

		JRadioButton impulseBtn = new JRadioButton("Impulse/Stem", false);
		impulseBtn.setForeground(Color.WHITE);
		impulseBtn.addActionListener(e -> {
			currentChart = 1;
			updateVisibility();
		});
		group.add(impulseBtn);
		panel.add(impulseBtn);

		panel.add(DemoUIHelper.createHorizontalSpacer());

		// Show connectors toggle for waterfall
		JCheckBox connectorsBox = new JCheckBox("Show Connectors");
		connectorsBox.setForeground(Color.WHITE);
		connectorsBox.setSelected(true);
		connectorsBox.addActionListener(e -> {
			waterfallSeries.getOptions().showConnectors(connectorsBox.isSelected());
			chart.repaint();
		});
		panel.add(connectorsBox);

		// Marker shape selector for impulse
		JComboBox<String> markerCombo = new JComboBox<>(new String[] { "Circle", "Square", "Diamond", "None" });
		markerCombo.addActionListener(e -> {
			ImpulseSeriesOptions.MarkerShape shape = switch (markerCombo.getSelectedIndex()) {
			case 0 -> ImpulseSeriesOptions.MarkerShape.CIRCLE;
			case 1 -> ImpulseSeriesOptions.MarkerShape.SQUARE;
			case 2 -> ImpulseSeriesOptions.MarkerShape.DIAMOND;
			default -> ImpulseSeriesOptions.MarkerShape.NONE;
			};
			impulseSeries.getOptions().markerShape(shape);
			chart.repaint();
		});
		panel.add(new JLabel("Marker:"));
		panel.add(markerCombo);

		return panel;
	}

	private void updateVisibility() {
		waterfallSeries.getOptions().visible(currentChart == 0);
		impulseSeries.getOptions().visible(currentChart == 1);

		// Switch category labels based on active chart
		CategoryAxis categoryAxis = (CategoryAxis) chart.getHorizontalAxis();
		if (currentChart == 0) {
			categoryAxis.setCategories(WATERFALL_CATEGORIES);
			chart.setVisibleRange(-1, WATERFALL_CATEGORIES.length);
		} else {
			categoryAxis.setCategories(IMPULSE_CATEGORIES);
			chart.setVisibleRange(-1, IMPULSE_POINTS);
		}

		chart.repaint();
	}

	@Override
	protected void onDemoClosing() {
		if (chart != null) {
			chart.dispose();
		}
	}

	public static void main(String[] args) {
		launch(new ColumnBarDemo(), args);
	}
}
