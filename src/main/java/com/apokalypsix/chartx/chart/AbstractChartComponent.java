package com.apokalypsix.chartx.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.HorizontalAxis;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.axis.YAxis;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.export.ExportOptions;
import com.apokalypsix.chartx.chart.export.ExportService;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorInstance;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorManager;
import com.apokalypsix.chartx.chart.finance.indicator.IndicatorRegistry;
import com.apokalypsix.chartx.chart.finance.indicator.custom.CustomIndicatorRegistry;
import com.apokalypsix.chartx.chart.interaction.ChartModifierGroup;
import com.apokalypsix.chartx.chart.interaction.ModifierSurface;
import com.apokalypsix.chartx.chart.interaction.modifier.MouseWheelZoomModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.RolloverModifier;
import com.apokalypsix.chartx.chart.interaction.modifier.ZoomPanModifier;
import com.apokalypsix.chartx.chart.series.RenderableSeries;
import com.apokalypsix.chartx.chart.series.SeriesType;
import com.apokalypsix.chartx.chart.style.LegendConfig;
import com.apokalypsix.chartx.chart.style.SeriesOptions;
import com.apokalypsix.chartx.chart.style.TooltipConfig;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.apokalypsix.chartx.core.interaction.DrawingInteractionHandler;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierEventPool;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierKeyEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.MouseEventGroup;
import com.apokalypsix.chartx.core.render.model.AxisLabelLayerV2;
import com.apokalypsix.chartx.core.render.model.AxisLayerV2;
import com.apokalypsix.chartx.core.render.model.BackgroundLayerV2;
import com.apokalypsix.chartx.core.render.model.CategoryAxisLayerV2;
import com.apokalypsix.chartx.core.render.model.CrosshairLayerV2;
import com.apokalypsix.chartx.core.render.model.DataLayerV2;
import com.apokalypsix.chartx.core.render.model.DrawingLayerV2;
import com.apokalypsix.chartx.core.render.model.GridLayerV2;
import com.apokalypsix.chartx.core.render.model.InfoOverlayLayerV2;
import com.apokalypsix.chartx.core.render.model.LegendItem;
import com.apokalypsix.chartx.core.render.model.LegendLayerV2;
import com.apokalypsix.chartx.core.render.model.OverlayLayerV2;
import com.apokalypsix.chartx.core.render.model.TextOverlay;
import com.apokalypsix.chartx.core.render.model.TooltipLayerV2;
import com.apokalypsix.chartx.core.render.service.RenderPipeline;
import com.apokalypsix.chartx.core.render.swing.ChartRenderingStrategy;
import com.apokalypsix.chartx.core.render.swing.GLChartRenderingStrategy;
import com.apokalypsix.chartx.core.render.swing.OffscreenChartRenderingStrategy;
import com.apokalypsix.chartx.core.ui.config.ChartUIConfig;
import com.apokalypsix.chartx.core.ui.overlay.IndicatorOverlay;
import com.apokalypsix.chartx.core.ui.overlay.InfoOverlay;
import com.apokalypsix.chartx.core.ui.overlay.OHLCDataOverlay;
import com.apokalypsix.chartx.core.ui.overlay.PriceBarOverlay;
import com.apokalypsix.chartx.core.ui.overlay.SymbolInfoOverlay;
import com.apokalypsix.chartx.core.ui.properties.dialogs.IndicatorPropertyDialog;
import com.apokalypsix.chartx.core.ui.properties.dialogs.SeriesPropertyDialog;
import com.apokalypsix.chartx.core.ui.properties.dialogs.TPOPropertyDialog;
import com.apokalypsix.chartx.core.ui.sidebar.ChartSidebar;
import com.apokalypsix.chartx.core.ui.sidebar.SidebarPosition;
import com.apokalypsix.chartx.core.render.api.RenderBackend;

/**
 * Abstract base class for chart components providing common rendering, viewport
 * management, and interaction handling.
 *
 * <p>
 * This class encapsulates the shared functionality for {@link Chart} including:
 * <ul>
 * <li>GPU-accelerated rendering via multiple backends</li>
 * <li>Viewport and coordinate system management</li>
 * <li>Mouse interaction (pan, zoom)</li>
 * <li>HiDPI display support</li>
 * <li>Crosshair cursor</li>
 * </ul>
 */
public abstract class AbstractChartComponent extends JPanel implements ModifierSurface {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(AbstractChartComponent.class);

	// Rendering (transient - not serializable, must be recreated)
	protected final transient RenderBackend backend;
	protected final transient ChartRenderingStrategy renderStrategy;
	protected final transient JComponent renderComponent;
	protected final transient TextOverlay textOverlay;
	protected final transient RenderPipeline pipeline;

	// Coordinate system (transient - contains rendering state)
	protected final transient Viewport viewport;
	protected final transient YAxisManager axisManager;
	protected transient HorizontalAxis<?> horizontalAxis;

	// Core layers (transient - rendering infrastructure)
	protected final transient BackgroundLayerV2 backgroundLayer;
	protected final transient GridLayerV2 gridLayer;
	protected final transient DataLayerV2 dataLayer;
	protected final transient CrosshairLayerV2 crosshairLayer;
	protected final transient AxisLayerV2 axisLayer;
	protected final transient CategoryAxisLayerV2 categoryAxisLayer;
	protected final transient AxisLabelLayerV2 axisLabelLayer;
	protected final transient LegendLayerV2 legendLayer;
	protected final transient InfoOverlayLayerV2 infoOverlayLayer;
	protected final transient TooltipLayerV2 tooltipLayer;

	// Configuration
	protected long barDuration = 60000; // Default 1 minute

	// Modifier system (transient - interaction handlers)
	protected final transient ChartModifierGroup modifierGroup;
	protected final String surfaceId;
	protected transient MouseEventGroup mouseEventGroup;

	// Indicator system (transient - calculation state)
	protected final transient IndicatorManager indicatorManager;
	protected final transient OverlayLayerV2 indicatorLayer;

	// Interaction state (transient - runtime state)
	protected transient Point lastMousePoint;
	protected transient boolean isPanning = false;

	// UI components (transient - UI state)
	protected transient ChartUIConfig uiConfig;
	protected transient ChartSidebar sidebar;

	// Pluggable overlay registry (transient - runtime overlays)
	protected final transient Map<String, InfoOverlay> overlayRegistry = new LinkedHashMap<>();

	// Well-known overlay IDs for backward compatibility
	public static final String OVERLAY_SYMBOL = "symbol";
	public static final String OVERLAY_OHLC = "ohlc";
	public static final String OVERLAY_INDICATOR = "indicator";
	public static final String OVERLAY_PRICEBAR = "pricebar";

	// Listener for separate-pane indicators
	protected SeparatePaneIndicatorListener separatePaneListener;

	/**
	 * Listener interface for indicators that require a separate pane. Applications
	 * can implement this to handle creating panes for indicators like RSI, MACD,
	 * OBV, CVD that shouldn't overlay on the price chart.
	 */
	public interface SeparatePaneIndicatorListener {
		/**
		 * Called when an indicator that requires a separate pane is added.
		 *
		 * @param instance the indicator instance with calculated data
		 */
		void onSeparatePaneIndicatorAdded(IndicatorInstance<?, ?> instance);

		/**
		 * Called when a separate-pane indicator is removed.
		 *
		 * @param instance the indicator instance
		 */
		void onSeparatePaneIndicatorRemoved(IndicatorInstance<?, ?> instance);
	}

	/**
	 * Creates a chart component with the specified rendering backend.
	 *
	 * @param backend the rendering backend to use
	 */
	protected AbstractChartComponent(RenderBackend backend) {
		this.backend = backend;
		this.surfaceId = "chart-" + UUID.randomUUID().toString().substring(0, 8);
		setLayout(new BorderLayout());

		// Initialize viewport
		viewport = new Viewport();

		// Initialize axis manager
		axisManager = new YAxisManager();

		// Initialize render pipeline
		pipeline = new RenderPipeline(viewport, axisManager);
		pipeline.setUseAbstractedAPI(true);

		// Create core layers
		backgroundLayer = new BackgroundLayerV2();
		gridLayer = new GridLayerV2();
		dataLayer = new DataLayerV2();
		crosshairLayer = new CrosshairLayerV2();
		axisLayer = new AxisLayerV2();
		categoryAxisLayer = new CategoryAxisLayerV2();
		axisLabelLayer = new AxisLabelLayerV2();
		legendLayer = new LegendLayerV2();
		infoOverlayLayer = new InfoOverlayLayerV2();
		tooltipLayer = new TooltipLayerV2();
		indicatorLayer = new OverlayLayerV2();

		// Initialize indicator system
		indicatorManager = new IndicatorManager();
		IndicatorRegistry.registerBuiltInIndicators(indicatorManager);
		setupIndicatorListener();

		// Add core layers to pipeline (subclasses can add more)
		addCoreLayers();

		// Create rendering strategy
		renderStrategy = createRenderingStrategy(backend);
		renderStrategy.initialize(pipeline);
		renderComponent = renderStrategy.getDisplayComponent();

		// Create text overlay
		textOverlay = new TextOverlay();
		textOverlay.setViewport(viewport);
		textOverlay.setCoordinates(pipeline.getCoordinates());
		textOverlay.setAxisManager(axisManager);

		// Enable GPU text rendering (disable Java2D for these features)
		textOverlay.setGPUAxisLabels(true);
		textOverlay.setGPUCrosshairLabels(true);
		textOverlay.setGPULegend(true);
		textOverlay.setGPUInfoOverlays(true);
		textOverlay.setGPUTooltip(true);
		textOverlay.setGPUCategoryAxisLabels(true);

		// Initialize modifier system with default modifiers
		modifierGroup = new ChartModifierGroup();
		setupDefaultModifiers();
		modifierGroup.onAttached(this);

		// Setup UI
		setupLayeredPane();
		setupMouseHandlers();
		setupResizeHandler();
		setupUIComponents();

		log.debug("{} created with {} backend", getClass().getSimpleName(), backend);
	}

	/**
	 * Sets up default modifiers. Subclasses can override to customize.
	 */
	protected void setupDefaultModifiers() {
		modifierGroup.with(new ZoomPanModifier()).with(new MouseWheelZoomModifier()).with(new RolloverModifier());
	}

	/**
	 * Adds core layers to the pipeline. Subclasses should override
	 * {@link #addAdditionalLayers()} to add more layers.
	 */
	protected void addCoreLayers() {
		pipeline.addLayer(backgroundLayer);
		pipeline.addLayer(gridLayer);
		addAdditionalLayers();
		pipeline.addLayer(dataLayer);
		pipeline.addLayer(indicatorLayer);
		pipeline.addLayer(crosshairLayer);
		pipeline.addLayer(axisLayer);
		pipeline.addLayer(categoryAxisLayer);
		pipeline.addLayer(axisLabelLayer);
		pipeline.addLayer(legendLayer);
		pipeline.addLayer(infoOverlayLayer);
		pipeline.addLayer(tooltipLayer);
	}

	/**
	 * Sets up the indicator listener to update the chart when indicators change.
	 */
	private void setupIndicatorListener() {
		indicatorManager.addListener(new IndicatorManager.IndicatorListener() {
			@Override
			public void onIndicatorAdded(IndicatorInstance<?, ?> instance) {
				log.debug("Indicator added: {} (overlayOnPrice={})", instance.getDescriptor().getId(),
						instance.getDescriptor().isOverlayOnPrice());
				if (instance.getDescriptor().isOverlayOnPrice()) {
					updateIndicatorOverlays();
				} else {
					// Separate-pane indicator - notify listener
					log.debug("Separate-pane indicator, listener={}", separatePaneListener);
					if (separatePaneListener != null) {
						separatePaneListener.onSeparatePaneIndicatorAdded(instance);
					}
				}
				repaint();
			}

			@Override
			public void onIndicatorRemoved(IndicatorInstance<?, ?> instance) {
				if (instance.getDescriptor().isOverlayOnPrice()) {
					updateIndicatorOverlays();
				} else {
					// Separate-pane indicator - notify listener
					if (separatePaneListener != null) {
						separatePaneListener.onSeparatePaneIndicatorRemoved(instance);
					}
				}
				repaint();
			}

			@Override
			public void onIndicatorEnabledChanged(IndicatorInstance<?, ?> instance, boolean enabled) {
				updateIndicatorOverlays();
				repaint();
			}

			@Override
			public void onIndicatorRecalculated(IndicatorInstance<?, ?> instance) {
				updateIndicatorOverlays();
				repaint();
			}

			@Override
			public void onIndicatorParametersApplied(IndicatorInstance<?, ?> instance) {
				updateIndicatorOverlays();
				repaint();
			}
		});
	}

	/**
	 * Updates the indicator layer with current indicator results. Subclasses should
	 * override to customize indicator rendering.
	 */
	protected void updateIndicatorOverlays() {
		indicatorLayer.clearOverlays();

		for (IndicatorInstance<?, ?> instance : indicatorManager.getIndicators()) {
			Object result = instance.getOutputData();
			if (result != null && instance.getDescriptor().isOverlayOnPrice()) {
				// Add overlay indicator results to the layer
				// Subclasses can customize this based on result type
				addIndicatorResultToLayer(instance, result);
			}
		}
	}

	/**
	 * Adds an indicator result to the overlay layer. Subclasses can override to
	 * customize rendering for specific result types.
	 *
	 * @param instance the indicator instance
	 * @param result   the calculation result
	 */
	protected void addIndicatorResultToLayer(IndicatorInstance<?, ?> instance, Object result) {
		// Default implementation - subclasses can override for specific
		// rendering
		// The result could be XyData, XyyData, MultiLineResult, etc.
	}

	/**
	 * Override to add additional layers between grid and data layers. Called during
	 * construction.
	 */
	protected void addAdditionalLayers() {
		// Subclasses can override to add layers
	}

	/**
	 * Creates the rendering strategy for the specified backend.
	 */
	protected ChartRenderingStrategy createRenderingStrategy(RenderBackend backend) {
		return switch (backend) {
		case OPENGL -> new GLChartRenderingStrategy();
		case VULKAN, METAL, DX12 -> new OffscreenChartRenderingStrategy(backend);
		case AUTO -> {
			if (OffscreenChartRenderingStrategy.isBackendAvailable(RenderBackend.METAL)) {
				yield new OffscreenChartRenderingStrategy(RenderBackend.METAL);
			} else if (OffscreenChartRenderingStrategy.isBackendAvailable(RenderBackend.VULKAN)) {
				yield new OffscreenChartRenderingStrategy(RenderBackend.VULKAN);
			} else {
				yield new GLChartRenderingStrategy();
			}
		}
		};
	}

	/**
	 * Sets up the layered pane for rendering and text overlay.
	 */
	private void setupLayeredPane() {
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setLayout(new OverlayLayout(layeredPane));

		renderComponent.setAlignmentX(0.0f);
		renderComponent.setAlignmentY(0.0f);
		textOverlay.setAlignmentX(0.0f);
		textOverlay.setAlignmentY(0.0f);

		layeredPane.add(renderComponent, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(textOverlay, JLayeredPane.PALETTE_LAYER);

		add(layeredPane, BorderLayout.CENTER);

		// Store reference for resize handler
		putClientProperty("layeredPane", layeredPane);
	}

	/**
	 * Sets up mouse handlers for pan, zoom, and crosshair. Events are dispatched
	 * through the modifier system.
	 */
	protected void setupMouseHandlers() {
		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dispatchMouseEvent(e, modifierGroup::onMousePressed);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dispatchMouseEvent(e, modifierGroup::onMouseReleased);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				dispatchMouseDragged(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				dispatchMouseEvent(e, modifierGroup::onMouseMoved);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				dispatchMouseEvent(e, modifierGroup::onMouseEntered);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				dispatchMouseEvent(e, modifierGroup::onMouseExited);
			}
		};

		MouseWheelListener wheelListener = this::dispatchMouseWheel;

		KeyAdapter keyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				dispatchKeyEvent(e, modifierGroup::onKeyPressed);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				dispatchKeyEvent(e, modifierGroup::onKeyReleased);
			}
		};

		renderComponent.addMouseListener(mouseAdapter);
		renderComponent.addMouseMotionListener(mouseAdapter);
		renderComponent.addMouseWheelListener(wheelListener);
		renderComponent.addKeyListener(keyAdapter);
		renderComponent.setFocusable(true);
	}

	/**
	 * Dispatches a mouse event through the modifier system.
	 */
	private void dispatchMouseEvent(MouseEvent e,
			java.util.function.Function<ModifierMouseEventArgs, Boolean> handler) {
		ModifierMouseEventArgs args = ModifierEventPool.acquireMouseArgs();
		try {
			args.populate(e, scaleForHiDPI(e.getX()), scaleForHiDPI(e.getY()), this);
			handler.apply(args);
			if (args.isHandled()) {
				repaint();
			}
		} finally {
			ModifierEventPool.release(args);
		}
	}

	/**
	 * Dispatches a mouse dragged event with delta tracking.
	 */
	private void dispatchMouseDragged(MouseEvent e) {
		ModifierMouseEventArgs args = ModifierEventPool.acquireMouseArgs();
		try {
			int scaledX = scaleForHiDPI(e.getX());
			int scaledY = scaleForHiDPI(e.getY());
			args.populate(e, scaledX, scaledY, this);

			// Calculate delta from last position
			if (lastMousePoint != null) {
				int deltaX = scaledX - scaleForHiDPI(lastMousePoint.x);
				int deltaY = scaledY - scaleForHiDPI(lastMousePoint.y);
				args.setDelta(deltaX, deltaY);
			}
			lastMousePoint = e.getPoint();

			modifierGroup.onMouseDragged(args);
			repaint();
		} finally {
			ModifierEventPool.release(args);
		}
	}

	/**
	 * Dispatches a mouse wheel event through the modifier system.
	 */
	private void dispatchMouseWheel(MouseWheelEvent e) {
		ModifierMouseEventArgs args = ModifierEventPool.acquireMouseArgs();
		try {
			args.populateWheel(e, scaleForHiDPI(e.getX()), scaleForHiDPI(e.getY()), this);
			modifierGroup.onMouseWheel(args);
			if (args.isHandled()) {
				onViewportChanged();
				repaint();
			}
		} finally {
			ModifierEventPool.release(args);
		}
	}

	/**
	 * Dispatches a key event through the modifier system.
	 */
	private void dispatchKeyEvent(KeyEvent e, java.util.function.Function<ModifierKeyEventArgs, Boolean> handler) {
		ModifierKeyEventArgs args = ModifierEventPool.acquireKeyArgs();
		try {
			args.populate(e, this);
			handler.apply(args);
			if (args.isHandled()) {
				repaint();
			}
		} finally {
			ModifierEventPool.release(args);
		}
	}

	/**
	 * Sets up the UI components (sidebar and basic overlay infrastructure).
	 * Subclasses should call setupDefaultOverlays() or register their own overlays.
	 */
	private void setupUIComponents() {
		// Sync overlays to GPU layer (uses same list reference for mouse
		// interaction)
		infoOverlayLayer.setOverlays(textOverlay.getInfoOverlays());

		// Setup default overlays - can be overridden by subclasses
		setupDefaultOverlays();
	}

	/**
	 * Sets up default overlays. Override in subclasses to customize. By default, no
	 * overlays are registered - subclasses like FinanceChart should call this or
	 * register their own overlays.
	 */
	protected void setupDefaultOverlays() {
		// Base implementation does nothing - subclasses register their overlays
	}

	// ========== Overlay Registry API ==========

	/**
	 * Registers an overlay with the given ID. Overlays are displayed in the order
	 * they are registered.
	 *
	 * @param id      unique identifier for the overlay
	 * @param overlay the overlay to register
	 */
	public void registerOverlay(String id, InfoOverlay overlay) {
		overlayRegistry.put(id, overlay);
		textOverlay.addInfoOverlay(overlay);
		infoOverlayLayer.setOverlays(textOverlay.getInfoOverlays());
	}

	/**
	 * Unregisters an overlay by ID.
	 *
	 * @param id the overlay ID to unregister
	 * @return the removed overlay, or null if not found
	 */
	public InfoOverlay unregisterOverlay(String id) {
		InfoOverlay removed = overlayRegistry.remove(id);
		if (removed != null) {
			textOverlay.removeInfoOverlay(removed);
			infoOverlayLayer.setOverlays(textOverlay.getInfoOverlays());
		}
		return removed;
	}

	/**
	 * Returns an overlay by ID.
	 *
	 * @param id the overlay ID
	 * @return the overlay, or null if not found
	 */
	public InfoOverlay getOverlay(String id) {
		return overlayRegistry.get(id);
	}

	/**
	 * Returns an overlay by ID, cast to the expected type.
	 *
	 * @param id   the overlay ID
	 * @param type the expected overlay class
	 * @param <T>  the overlay type
	 * @return the overlay cast to the type, or null if not found or wrong type
	 */
	@SuppressWarnings("unchecked")
	public <T extends InfoOverlay> T getOverlay(String id, Class<T> type) {
		InfoOverlay overlay = overlayRegistry.get(id);
		if (overlay != null && type.isInstance(overlay)) {
			return (T) overlay;
		}
		return null;
	}

	/**
	 * Returns all registered overlays.
	 *
	 * @return unmodifiable map of overlay ID to overlay
	 */
	public Map<String, InfoOverlay> getOverlays() {
		return java.util.Collections.unmodifiableMap(overlayRegistry);
	}

	/**
	 * Wires up the settings icon click callbacks for overlays. Called by subclasses
	 * after registering overlays that support settings.
	 */
	protected void wireOverlaySettingsCallbacks() {
		// Wire indicator overlay if present
		IndicatorOverlay indicatorOverlay = getOverlay(OVERLAY_INDICATOR, IndicatorOverlay.class);
		if (indicatorOverlay != null) {
			indicatorOverlay.setOnSettingsClicked(artifact -> {
				if (artifact instanceof IndicatorInstance) {
					showIndicatorSettingsDialog((IndicatorInstance<?, ?>) artifact);
				} else if (artifact instanceof TPOSeries tpoSeries) {
					// Handle TPO series settings
					showTPOSettingsDialog(tpoSeries);
				} else if (artifact instanceof RenderableSeries<?, ?> series) {
					// Handle series added directly to the chart
					showSeriesSettingsDialog(series.getOptions());
				}
			});
		}

		// Wire OHLC overlay if present
		OHLCDataOverlay ohlcOverlay = getOverlay(OVERLAY_OHLC, OHLCDataOverlay.class);
		if (ohlcOverlay != null) {
			ohlcOverlay.setOnSettingsClicked(artifact -> {
				if (artifact instanceof SeriesOptions) {
					showSeriesSettingsDialog((SeriesOptions) artifact);
				}
			});
		}
	}

	/**
	 * Shows the indicator settings dialog.
	 */
	private void showIndicatorSettingsDialog(IndicatorInstance<?, ?> instance) {
		Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
		IndicatorPropertyDialog dialog = new IndicatorPropertyDialog(frame, instance);
		if (dialog.showDialog()) {
			// Changes were applied - recalculate the indicator
			indicatorManager.recalculateIndicator(instance.getId());
			repaint();
		}
	}

	/**
	 * Shows the series settings dialog.
	 */
	private void showSeriesSettingsDialog(SeriesOptions options) {
		Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
		SeriesPropertyDialog dialog = new SeriesPropertyDialog(frame, options);
		if (dialog.showDialog()) {
			// Changes were applied - repaint
			repaint();
		}
	}

	/**
	 * Shows the TPO settings dialog.
	 */
	private void showTPOSettingsDialog(TPOSeries series) {
		Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
		TPOPropertyDialog dialog = new TPOPropertyDialog(frame, series, this::repaint);
		if (dialog.showDialog()) {
			// Repaint is handled dynamically via callback, but ensure final state is
			// rendered
			repaint();
		}
	}

	/**
	 * Sets up the component resize handler.
	 */
	private void setupResizeHandler() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				JLayeredPane layeredPane = (JLayeredPane) getClientProperty("layeredPane");
				if (layeredPane == null)
					return;

				Dimension size = layeredPane.getSize();
				renderComponent.setBounds(0, 0, size.width, size.height);
				textOverlay.setBounds(0, 0, size.width, size.height);

				// Sync viewport dimensions for correct interaction handling
				if (size.width > 0 && size.height > 0) {
					int scaledWidth = scaleForHiDPI(size.width);
					int scaledHeight = scaleForHiDPI(size.height);
					viewport.setSize(scaledWidth, scaledHeight);
					// Scale insets for HiDPI - viewport dimensions are
					// physical, so insets must be too
					viewport.setInsets(scaleForHiDPI(axisManager.getLeftInset()),
							scaleForHiDPI(axisManager.getRightInset()), scaleForHiDPI(10), // topInset default
							scaleForHiDPI(40) // bottomInset default (increased for
												// text descent)
					);
					pipeline.getCoordinates().invalidateCache();

					// Update scale factor for HiDPI text rendering
					pipeline.setScaleFactor((float) getScaleFactor());
				}
			}
		});
	}

	// ========== Legacy Mouse Event Handlers (deprecated, use modifier system)
	// ==========

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMousePressed(MouseEvent e) {
		// Legacy - now handled by ZoomPanModifier
	}

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMouseReleased(MouseEvent e) {
		// Legacy - now handled by ZoomPanModifier
	}

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMouseDragged(MouseEvent e) {
		// Legacy - now handled by ZoomPanModifier
	}

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMouseMoved(MouseEvent e) {
		// Legacy - now handled by RolloverModifier
	}

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMouseEntered(MouseEvent e) {
		// Legacy - now handled by RolloverModifier
	}

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMouseExited(MouseEvent e) {
		// Legacy - now handled by RolloverModifier
	}

	/**
	 * @deprecated Use modifier system instead. Override setupDefaultModifiers() to
	 *             customize.
	 */
	@Deprecated
	protected void handleMouseWheel(MouseWheelEvent e) {
		// Legacy - now handled by MouseWheelZoomModifier
	}

	/**
	 * Called when the viewport changes (pan/zoom). Subclasses can override to
	 * notify listeners or perform additional actions.
	 */
	protected void onViewportChanged() {
		// Subclasses can override
	}

	// ========== Axis Detection and Zooming ==========

	/**
	 * Finds which Y-axis the given x coordinate is over.
	 */
	protected YAxis findAxisAtX(int x, int y, int chartLeft, int chartRight, int chartTop, int chartBottom) {
		if (y < chartTop || y > chartBottom) {
			return null;
		}

		// Check left axes
		List<YAxis> leftAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.LEFT);
		int leftX = 0;
		for (YAxis axis : leftAxes) {
			if (x >= leftX && x < leftX + axis.getWidth()) {
				return axis;
			}
			leftX += axis.getWidth();
		}

		// Check right axes
		List<YAxis> rightAxes = axisManager.getVisibleAxesAtPosition(YAxis.Position.RIGHT);
		int rightX = chartRight;
		for (YAxis axis : rightAxes) {
			if (x >= rightX && x < rightX + axis.getWidth()) {
				return axis;
			}
			rightX += axis.getWidth();
		}

		return null;
	}

	/**
	 * Zooms a specific Y-axis.
	 */
	protected void zoomAxis(YAxis axis, double factor, int anchorY, int chartTop, int chartBottom) {
		if (axis.isAutoScale()) {
			axis.setAutoScale(false);
		}

		int chartHeight = chartBottom - chartTop;
		double normalized = 1.0 - (double) (anchorY - chartTop) / chartHeight;
		double anchorValue = axis.interpolate(normalized);

		double bottomSpan = anchorValue - axis.getMinValue();
		double topSpan = axis.getMaxValue() - anchorValue;

		double newMin = anchorValue - bottomSpan / factor;
		double newMax = anchorValue + topSpan / factor;

		double minRange = 0.01;
		if (newMax - newMin < minRange) {
			double center = (newMin + newMax) / 2;
			newMin = center - minRange / 2;
			newMax = center + minRange / 2;
		}

		axis.setValueRange(newMin, newMax);
		pipeline.getCoordinates().invalidateCache();

		if (YAxis.DEFAULT_AXIS_ID.equals(axis.getId())) {
			viewport.setPriceRange(newMin, newMax);
			viewport.setAutoScaleY(false);
		}
	}

	// ========== HiDPI Support ==========

	/**
	 * Gets the HiDPI scale factor for the render component.
	 */
	@Override
	public double getScaleFactor() {
		if (renderComponent != null) {
			var gc = renderComponent.getGraphicsConfiguration();
			if (gc != null) {
				return gc.getDefaultTransform().getScaleX();
			}
		}
		return 1.0;
	}

	/**
	 * Scales a coordinate for HiDPI displays.
	 */
	@Override
	public int scaleForHiDPI(int coord) {
		return (int) (coord * getScaleFactor());
	}

	/**
	 * Updates the crosshair position, accounting for HiDPI scaling.
	 */
	protected void updateCrosshairPosition(int x, int y) {
		crosshairLayer.setCursorPosition(scaleForHiDPI(x), scaleForHiDPI(y));
		textOverlay.setCursorPosition(scaleForHiDPI(x), scaleForHiDPI(y));
	}

	// ========== Configuration ==========

	/**
	 * Sets the bar duration in milliseconds.
	 */
	public void setBarDuration(long durationMillis) {
		this.barDuration = durationMillis;
		pipeline.setBarDuration(durationMillis);
		repaint();
	}

	/**
	 * Returns the bar duration in milliseconds.
	 */
	public long getBarDuration() {
		return barDuration;
	}

	/**
	 * Enables or disables automatic Y-axis scaling.
	 *
	 * @param autoScale true to enable auto-scaling
	 * @deprecated Use {@code getYAxis().setAutoRange(AutoRangeMode.ALWAYS)} or
	 *             {@code getYAxis().setAutoRange(AutoRangeMode.NEVER)} instead
	 */
	@Deprecated
	public void setAutoScaleY(boolean autoScale) {
		viewport.setAutoScaleY(autoScale);
		YAxis defaultAxis = axisManager.getDefaultAxis();
		if (defaultAxis != null) {
			defaultAxis.setAutoRange(autoScale ? YAxis.AutoRangeMode.ALWAYS : YAxis.AutoRangeMode.NEVER);
		}
		repaint();
	}

	/**
	 * Returns whether automatic Y-axis scaling is enabled.
	 *
	 * @return true if auto-scaling is enabled
	 * @deprecated Use {@code getYAxis().getAutoRange()} instead
	 */
	@Deprecated
	public boolean isAutoScaleY() {
		return viewport.isAutoScaleY();
	}

	/**
	 * Enables or disables the crosshair cursor.
	 */
	public void setCrosshairEnabled(boolean enabled) {
		crosshairLayer.setVisible(enabled);
		repaint();
	}

	/**
	 * Enables or disables the grid.
	 */
	public void setGridEnabled(boolean enabled) {
		gridLayer.setVisible(enabled);
		repaint();
	}

	// ========== Axis Access (New API) ==========

	/**
	 * Sets the horizontal axis for this chart.
	 *
	 * <p>
	 * Call this to configure the chart for time-based or categorical data:
	 * 
	 * <pre>{@code
	 * // Time-based chart
	 * chart.setXAxis(new TimeAxis());
	 *
	 * // Categorical chart
	 * CategoryAxis categoryAxis = new CategoryAxis();
	 * categoryAxis.setCategories("A", "B", "C");
	 * chart.setXAxis(categoryAxis);
	 * }</pre>
	 *
	 * @param axis the horizontal axis (TimeAxis for time-based, CategoryAxis for
	 *             categorical)
	 */
	public HorizontalAxis<?> setXAxis(HorizontalAxis<?> axis) {
		this.horizontalAxis = axis;

		// Update pipeline and text overlay
		if (axis instanceof CategoryAxis catAxis) {
			pipeline.setCategoryAxis(catAxis);
			textOverlay.setCategoryAxis(catAxis);
		} else {
			pipeline.setCategoryAxis(null);
			textOverlay.setCategoryAxis(null);
		}

		repaint();

		return axis;
	}

	/**
	 * Returns the horizontal axis.
	 *
	 * @return the horizontal axis, or null if not set
	 */
	public HorizontalAxis<?> getHorizontalAxis() {
		return horizontalAxis;
	}

	/**
	 * Returns the default Y-axis for this chart.
	 *
	 * <p>
	 * Use the Y-axis to configure price range and auto-ranging behavior:
	 * 
	 * <pre>{@code
	 * chart.getYAxis().setVisibleRange(-20, 20);
	 * chart.getYAxis().setAutoRange(YAxis.AutoRangeMode.ALWAYS);
	 * chart.getYAxis().setGrowBy(0.05); // 5% padding
	 * }</pre>
	 *
	 * @return the default Y-axis
	 */
	public YAxis getYAxis() {
		return axisManager.getDefaultAxis();
	}

	/**
	 * Returns a Y-axis by ID.
	 *
	 * <p>
	 * Use this for multi-axis charts where different series use different Y-axes:
	 * 
	 * <pre>{@code
	 * chart.getYAxis("rsi").setVisibleRange(0, 100);
	 * chart.getYAxis("volume").setAutoRange(YAxis.AutoRangeMode.ALWAYS);
	 * }</pre>
	 *
	 * @param axisId the axis ID
	 * @return the Y-axis with the given ID, or null if not found
	 */
	public YAxis getYAxis(String axisId) {
		return axisManager.getAxis(axisId);
	}

	/**
	 * Returns whether this chart has a category axis configured.
	 *
	 * @return true if the horizontal axis is categorical
	 */
	public boolean hasCategoryAxis() {
		return horizontalAxis != null && !horizontalAxis.isTimeBased();
	}

	// ========== Component Access ==========

	/**
	 * Returns the start time of the visible range.
	 *
	 * @return start time in epoch milliseconds (or category index for categorical
	 *         charts)
	 */
	public long getStartTime() {
		return viewport.getStartTime();
	}

	/**
	 * Returns the end time of the visible range.
	 *
	 * @return end time in epoch milliseconds (or category index for categorical
	 *         charts)
	 */
	public long getEndTime() {
		return viewport.getEndTime();
	}

	/**
	 * Returns the viewport for low-level coordinate access.
	 *
	 * <p>
	 * <b>Prefer using the axis API for range configuration:</b>
	 * <ul>
	 * <li>{@code setVisibleRange(long, long)} or
	 * {@code getXAxis().setVisibleRange()} for X range</li>
	 * <li>{@code getYAxis().setVisibleRange()} for Y range</li>
	 * </ul>
	 *
	 * @return the viewport
	 * @deprecated Use axis methods for range configuration. This method remains for
	 *             internal modifier access and may become package-private in
	 *             future.
	 */
	@Override
	@Deprecated
	public Viewport getViewport() {
		return viewport;
	}

	public RenderPipeline getPipeline() {
		return pipeline;
	}

	public RenderBackend getBackend() {
		return backend;
	}

	public ChartRenderingStrategy getRenderStrategy() {
		return renderStrategy;
	}

	public YAxisManager getAxisManager() {
		return axisManager;
	}

	public BackgroundLayerV2 getBackgroundLayer() {
		return backgroundLayer;
	}

	public GridLayerV2 getGridLayer() {
		return gridLayer;
	}

	public DataLayerV2 getDataLayer() {
		return dataLayer;
	}

	@Override
	public CrosshairLayerV2 getCrosshairLayer() {
		return crosshairLayer;
	}

	public AxisLayerV2 getAxisLayer() {
		return axisLayer;
	}

	@Override
	public TextOverlay getTextOverlay() {
		return textOverlay;
	}

	// ========== Modifier System ==========

	/**
	 * Returns the modifier group for this chart.
	 *
	 * <p>
	 * Use this to add, remove, or configure chart modifiers.
	 *
	 * @return the modifier group
	 */
	public ChartModifierGroup getModifiers() {
		return modifierGroup;
	}

	// ========== Indicator API ==========

	/**
	 * Returns the indicator manager for this chart.
	 *
	 * <p>
	 * Use this to add, remove, or configure indicators.
	 *
	 * @return the indicator manager
	 */
	public IndicatorManager getIndicatorManager() {
		return indicatorManager;
	}

	/**
	 * Returns the indicator overlay layer.
	 *
	 * @return the overlay layer used for indicator rendering
	 */
	public OverlayLayerV2 getIndicatorLayer() {
		return indicatorLayer;
	}

	/**
	 * Sets the listener for separate-pane indicators. When an indicator that
	 * requires its own pane (e.g., RSI, MACD, OBV, CVD) is added, the listener will
	 * be notified so it can create the appropriate pane.
	 *
	 * @param listener the listener to notify, or null to disable
	 */
	public void setSeparatePaneIndicatorListener(SeparatePaneIndicatorListener listener) {
		this.separatePaneListener = listener;
	}

	/**
	 * Returns the listener for separate-pane indicators.
	 *
	 * @return the current listener, or null if none set
	 */
	public SeparatePaneIndicatorListener getSeparatePaneIndicatorListener() {
		return separatePaneListener;
	}

	/**
	 * Adds an indicator to the chart.
	 *
	 * @param indicatorId the registered indicator ID (e.g., "SMA", "RSI", "MACD")
	 * @return the created indicator instance, or null if indicator not found
	 */
	public IndicatorInstance<?, ?> addIndicator(String indicatorId) {
		return indicatorManager.addIndicator(indicatorId);
	}

	/**
	 * Adds an indicator to the chart with custom parameters.
	 *
	 * @param indicatorId the registered indicator ID
	 * @param parameters  initial parameter values
	 * @return the created indicator instance, or null if indicator not found
	 */
	public IndicatorInstance<?, ?> addIndicator(String indicatorId, Map<String, Object> parameters) {
		IndicatorInstance<?, ?> instance = indicatorManager.addIndicator(indicatorId);
		if (instance != null && parameters != null) {
			for (Map.Entry<String, Object> entry : parameters.entrySet()) {
				instance.setParameterValue(entry.getKey(), entry.getValue());
			}
		}
		return instance;
	}

	/**
	 * Removes an indicator from the chart.
	 *
	 * @param instanceId the indicator instance ID
	 */
	public void removeIndicator(String instanceId) {
		indicatorManager.removeIndicator(instanceId);
	}

	/**
	 * Returns all indicator instances on this chart.
	 *
	 * @return list of indicator instances
	 */
	public List<IndicatorInstance<?, ?>> getIndicators() {
		return indicatorManager.getIndicators();
	}

	/**
	 * Calculates all indicators using the current chart data. Call this after data
	 * changes to update indicator values.
	 */
	@SuppressWarnings("unchecked")
	public void calculateIndicators() {
		Object data = dataLayer.getData();
		if (data != null) {
			indicatorManager.calculateAll(data);
		}
	}

	/**
	 * Stages a parameter change for an indicator without recalculating. Use
	 * {@link #applyIndicatorChanges(String)} to apply staged changes.
	 *
	 * @param instanceId the indicator instance ID
	 * @param paramName  the parameter name
	 * @param value      the new value
	 * @return true if parameter was staged successfully
	 */
	public boolean stageIndicatorParameter(String instanceId, String paramName, Object value) {
		return indicatorManager.stageIndicatorParameter(instanceId, paramName, value);
	}

	/**
	 * Applies staged parameter changes and recalculates the indicator.
	 *
	 * @param instanceId the indicator instance ID
	 * @return true if changes were applied
	 */
	public boolean applyIndicatorChanges(String instanceId) {
		return indicatorManager.applyIndicatorChanges(instanceId);
	}

	/**
	 * Applies all pending indicator changes.
	 *
	 * @return list of instance IDs that were updated
	 */
	public List<String> applyAllIndicatorChanges() {
		return indicatorManager.applyAllPendingChanges();
	}

	/**
	 * Discards staged parameter changes for an indicator.
	 *
	 * @param instanceId the indicator instance ID
	 */
	public void discardIndicatorChanges(String instanceId) {
		indicatorManager.discardIndicatorChanges(instanceId);
	}

	/**
	 * Returns whether any indicators have pending (unapplied) changes.
	 *
	 * @return true if there are pending changes
	 */
	public boolean hasIndicatorPendingChanges() {
		return indicatorManager.hasAnyPendingChanges();
	}

	/**
	 * Returns the custom indicator registry for registering user-defined
	 * indicators.
	 *
	 * @return the custom indicator registry
	 */
	public CustomIndicatorRegistry getCustomIndicatorRegistry() {
		return indicatorManager.getCustomRegistry();
	}

	// ========== ModifierSurface Implementation ==========

	@Override
	public CoordinateSystem getCoordinateSystem() {
		return pipeline.getCoordinates();
	}

	@Override
	public Data<?> getPrimaryData() {
		return dataLayer.getData();
	}

	@Override
	public String findAxisAtPosition(int x, int y) {
		int chartLeft = viewport.getLeftInset();
		int chartRight = viewport.getWidth() - viewport.getRightInset();
		int chartTop = viewport.getTopInset();
		int chartBottom = viewport.getHeight() - viewport.getBottomInset();

		YAxis axis = findAxisAtX(x, y, chartLeft, chartRight, chartTop, chartBottom);
		return axis != null ? axis.getId() : null;
	}

	@Override
	public void requestRepaint() {
		repaint();
	}

	@Override
	public void setCursor(Cursor cursor) {
		if (renderComponent != null) {
			renderComponent.setCursor(cursor != null ? cursor : Cursor.getDefaultCursor());
		}
	}

	@Override
	public String getSurfaceId() {
		return surfaceId;
	}

	@Override
	public MouseEventGroup getMouseEventGroup() {
		return mouseEventGroup;
	}

	@Override
	public void setMouseEventGroup(MouseEventGroup group) {
		this.mouseEventGroup = group;
	}

	@Override
	public DrawingLayerV2 getDrawingLayer() {
		return null; // Subclasses (Chart) can override
	}

	@Override
	public DrawingInteractionHandler getDrawingHandler() {
		return null; // Subclasses (Chart) can override
	}

	@Override
	public void dispatchMouseMoved(ModifierMouseEventArgs args) {
		modifierGroup.onMouseMoved(args);
		repaint();
	}

	@Override
	public void dispatchMouseEntered(ModifierMouseEventArgs args) {
		modifierGroup.onMouseEntered(args);
		repaint();
	}

	@Override
	public void dispatchMouseExited(ModifierMouseEventArgs args) {
		modifierGroup.onMouseExited(args);
		repaint();
	}

	// ========== Legend API ==========

	/**
	 * Configures the legend appearance and position.
	 *
	 * @param config the legend configuration
	 */
	public void setLegendConfig(LegendConfig config) {
		textOverlay.setLegendConfig(config);
		legendLayer.setConfig(config);
	}

	/**
	 * Returns the current legend configuration.
	 *
	 * @return the legend config, or null if not set
	 */
	public LegendConfig getLegendConfig() {
		return textOverlay.getLegendConfig();
	}

	/**
	 * Adds a legend item for a series.
	 *
	 * @param seriesId    unique series identifier
	 * @param displayName name to show in legend
	 * @param color       series color
	 * @param type        series rendering type
	 */
	public void addLegendItem(String seriesId, String displayName, Color color, SeriesType type) {
		textOverlay.addLegendItem(new LegendItem(seriesId, displayName, color, type));
		legendLayer.setItems(textOverlay.getLegendItems());
	}

	/**
	 * Adds a legend item.
	 *
	 * @param item the legend item to add
	 */
	public void addLegendItem(LegendItem item) {
		textOverlay.addLegendItem(item);
		legendLayer.setItems(textOverlay.getLegendItems());
	}

	/**
	 * Removes a legend item by series ID.
	 *
	 * @param seriesId the series ID to remove
	 */
	public void removeLegendItem(String seriesId) {
		textOverlay.removeLegendItem(seriesId);
		legendLayer.setItems(textOverlay.getLegendItems());
	}

	/**
	 * Clears all legend items.
	 */
	public void clearLegendItems() {
		textOverlay.clearLegendItems();
		legendLayer.setItems(textOverlay.getLegendItems());
	}

	/**
	 * Sets all legend items at once.
	 *
	 * @param items the legend items
	 */
	public void setLegendItems(List<LegendItem> items) {
		textOverlay.setLegendItems(items);
		legendLayer.setItems(items);
	}

	// ========== Tooltip API ==========

	/**
	 * Configures the tooltip appearance and behavior.
	 *
	 * @param config the tooltip configuration
	 */
	public void setTooltipConfig(TooltipConfig config) {
		textOverlay.setTooltipConfig(config);
		tooltipLayer.setConfig(config);

		// Also configure the RolloverModifier if present
		RolloverModifier rollover = modifierGroup.findModifier(RolloverModifier.class);
		if (rollover != null) {
			rollover.setTooltipConfig(config);
		}
	}

	/**
	 * Returns the current tooltip configuration.
	 *
	 * @return the tooltip config, or null if not set
	 */
	public TooltipConfig getTooltipConfig() {
		return textOverlay.getTooltipConfig();
	}

	/**
	 * Enables or disables tooltips.
	 *
	 * @param enabled true to show tooltips on hover
	 */
	public void setTooltipEnabled(boolean enabled) {
		RolloverModifier rollover = modifierGroup.findModifier(RolloverModifier.class);
		if (rollover != null) {
			rollover.setTooltipEnabled(enabled);
		}
	}

	// ========== Export API ==========

	/**
	 * Exports the chart to a PNG file with default options.
	 *
	 * @param file destination file
	 * @throws IOException if export fails
	 */
	public void exportToPng(File file) throws IOException {
		exportToFile(file, ExportOptions.png().build());
	}

	/**
	 * Exports the chart to a PNG file with custom options.
	 *
	 * @param file    destination file
	 * @param options export options (scale, antialiasing, etc.)
	 * @throws IOException if export fails
	 */
	public void exportToPng(File file, ExportOptions options) throws IOException {
		exportToFile(file, options);
	}

	/**
	 * Exports the chart to a JPEG file with default options.
	 *
	 * @param file destination file
	 * @throws IOException if export fails
	 */
	public void exportToJpeg(File file) throws IOException {
		exportToFile(file, ExportOptions.jpeg().build());
	}

	/**
	 * Exports the chart to a JPEG file with custom options.
	 *
	 * @param file    destination file
	 * @param options export options (quality, background, etc.)
	 * @throws IOException if export fails
	 */
	public void exportToJpeg(File file, ExportOptions options) throws IOException {
		exportToFile(file, options);
	}

	/**
	 * Exports the chart to an SVG file.
	 *
	 * @param file destination file
	 * @throws IOException if export fails
	 */
	public void exportToSvg(File file) throws IOException {
		exportToFile(file, ExportOptions.svg().build());
	}

	/**
	 * Exports the chart to a file with the specified options.
	 *
	 * @param file    destination file
	 * @param options export options
	 * @throws IOException if export fails
	 */
	public void exportToFile(File file, ExportOptions options) throws IOException {
		ExportService exportService = new ExportService(renderComponent, textOverlay);
		exportService.exportToFile(file, options);
	}

	/**
	 * Exports the chart to a BufferedImage.
	 *
	 * @param options export options
	 * @return the exported image
	 */
	public BufferedImage exportToImage(ExportOptions options) {
		ExportService exportService = new ExportService(renderComponent, textOverlay);
		return exportService.exportToImage(options);
	}

	/**
	 * Exports the chart asynchronously. Use for large exports or when UI
	 * responsiveness is critical.
	 *
	 * @param file    destination file
	 * @param options export options
	 * @return CompletableFuture that completes when export is done
	 */
	public CompletableFuture<Void> exportToFileAsync(File file, ExportOptions options) {
		ExportService exportService = new ExportService(renderComponent, textOverlay);
		return exportService.exportToFileAsync(file, options);
	}

	/**
	 * Exports the chart to a BufferedImage asynchronously.
	 *
	 * @param options export options
	 * @return CompletableFuture with the exported image
	 */
	public CompletableFuture<BufferedImage> exportToImageAsync(ExportOptions options) {
		ExportService exportService = new ExportService(renderComponent, textOverlay);
		return exportService.exportToImageAsync(options);
	}

	// ========== UI Configuration API ==========

	/**
	 * Sets the chart UI configuration.
	 *
	 * <p>
	 * This controls the sidebar and info overlays. Use this to customize the
	 * TradingView-style UI elements.
	 *
	 * @param config the UI configuration
	 */
	public void setChartUIConfig(ChartUIConfig config) {
		this.uiConfig = config != null ? config : new ChartUIConfig();
		applyUIConfig();
	}

	/**
	 * Returns the chart UI configuration.
	 *
	 * @return the UI config, or null if not set
	 */
	public ChartUIConfig getChartUIConfig() {
		return uiConfig;
	}

	/**
	 * Applies the current UI configuration.
	 */
	private void applyUIConfig() {
		if (uiConfig == null) {
			return;
		}

		// Configure sidebar
		if (uiConfig.isSidebarEnabled()) {
			if (sidebar == null) {
				sidebar = new ChartSidebar(uiConfig.getSidebarConfig());
				String position = uiConfig.getSidebarConfig().getPosition() == SidebarPosition.LEFT ? BorderLayout.WEST
						: BorderLayout.EAST;
				add(sidebar, position);
				revalidate();
			} else {
				sidebar.setConfig(uiConfig.getSidebarConfig());
			}
			sidebar.setVisible(true);
		} else if (sidebar != null) {
			sidebar.setVisible(false);
		}

		// Configure info overlays (only if registered)
		boolean overlaysEnabled = uiConfig.isInfoOverlaysEnabled();

		// Legacy overlays (SymbolInfoOverlay, OHLCDataOverlay)
		SymbolInfoOverlay symbolOverlay = getSymbolOverlay();
		if (symbolOverlay != null) {
			symbolOverlay.setConfig(uiConfig.getSymbolOverlayConfig());
			symbolOverlay.getConfig().visible(overlaysEnabled && uiConfig.getSymbolOverlayConfig().isVisible());
		}

		OHLCDataOverlay ohlcOverlay = getOhlcOverlay();
		if (ohlcOverlay != null) {
			ohlcOverlay.setConfig(uiConfig.getOhlcOverlayConfig());
			ohlcOverlay.getConfig().visible(overlaysEnabled && uiConfig.getOhlcOverlayConfig().isVisible());
		}

		// New combined PriceBarOverlay
		PriceBarOverlay priceBarOverlay = getPriceBarOverlay();
		if (priceBarOverlay != null) {
			// PriceBarOverlay uses transparent mode by default, just control visibility
			priceBarOverlay.getConfig().visible(overlaysEnabled);
		}

		IndicatorOverlay indicatorOverlay = getIndicatorOverlay();
		if (indicatorOverlay != null) {
			indicatorOverlay.setConfig(uiConfig.getIndicatorOverlayConfig());
			indicatorOverlay.getConfig().visible(overlaysEnabled && uiConfig.getIndicatorOverlayConfig().isVisible());
		}

		repaint();
	}

	/**
	 * Returns the sidebar component.
	 *
	 * @return the sidebar, or null if not created
	 */
	public ChartSidebar getSidebar() {
		return sidebar;
	}

	/**
	 * Sets whether the sidebar is visible.
	 *
	 * @param visible true to show sidebar
	 */
	public void setSidebarVisible(boolean visible) {
		if (sidebar != null) {
			sidebar.setVisible(visible);
		} else if (visible) {
			// Create sidebar on demand
			sidebar = new ChartSidebar(uiConfig != null ? uiConfig.getSidebarConfig() : null);
			add(sidebar, BorderLayout.WEST);
			revalidate();
		}
	}

	/**
	 * Sets whether info overlays are visible.
	 *
	 * @param visible true to show overlays
	 */
	public void setInfoOverlaysVisible(boolean visible) {
		for (InfoOverlay overlay : overlayRegistry.values()) {
			overlay.getConfig().visible(visible);
		}
		repaint();
	}

	/**
	 * Sets the symbol information displayed in the symbol overlay.
	 *
	 * @param symbol    the symbol name (e.g., "AAPL", "EUR/USD")
	 * @param timeframe the timeframe (e.g., "1D", "4H")
	 * @param exchange  the exchange (e.g., "NASDAQ", "NYSE")
	 */
	public void setSymbolInfo(String symbol, String timeframe, String exchange) {
		SymbolInfoOverlay overlay = getSymbolOverlay();
		if (overlay != null) {
			overlay.setSymbolInfo(symbol, timeframe, exchange);
			repaint();
		}
	}

	/**
	 * Returns the symbol info overlay.
	 *
	 * @return the symbol overlay, or null if not registered
	 */
	public SymbolInfoOverlay getSymbolOverlay() {
		return getOverlay(OVERLAY_SYMBOL, SymbolInfoOverlay.class);
	}

	/**
	 * Returns the OHLC data overlay.
	 *
	 * @return the OHLC overlay, or null if not registered
	 */
	public OHLCDataOverlay getOhlcOverlay() {
		return getOverlay(OVERLAY_OHLC, OHLCDataOverlay.class);
	}

	/**
	 * Returns the indicator values overlay.
	 *
	 * @return the indicator overlay, or null if not registered
	 */
	public IndicatorOverlay getIndicatorOverlay() {
		return getOverlay(OVERLAY_INDICATOR, IndicatorOverlay.class);
	}

	/**
	 * Returns the price bar overlay (combined symbol + OHLC).
	 *
	 * @return the price bar overlay, or null if not registered
	 */
	public PriceBarOverlay getPriceBarOverlay() {
		return getOverlay(OVERLAY_PRICEBAR, PriceBarOverlay.class);
	}

	/**
	 * Updates the OHLC overlay to display values at the given bar index. Pass -1 to
	 * show the latest bar.
	 *
	 * @param index the bar index, or -1 for latest
	 */
	public void setOverlayDisplayIndex(int index) {
		// Handle legacy OHLC overlay
		OHLCDataOverlay ohlcOverlay = getOhlcOverlay();
		if (ohlcOverlay != null) {
			ohlcOverlay.setDisplayIndex(index);
		}
		// Handle new PriceBarOverlay
		PriceBarOverlay priceBarOverlay = getPriceBarOverlay();
		if (priceBarOverlay != null) {
			priceBarOverlay.setDisplayIndex(index);
		}
		IndicatorOverlay indicatorOverlay = getIndicatorOverlay();
		if (indicatorOverlay != null) {
			indicatorOverlay.setDisplayIndex(index);
		}
		repaint();
	}

	/**
	 * Binds the OHLC overlay to the chart's OHLC data. Call this after setting data
	 * on the data layer.
	 */
	protected void bindOhlcOverlay() {
		Data<?> data = dataLayer.getData();
		if (data instanceof OhlcData ohlcData) {
			// Handle legacy OHLC overlay
			OHLCDataOverlay ohlcOverlay = getOhlcOverlay();
			if (ohlcOverlay != null) {
				ohlcOverlay.setData(ohlcData);
			}
			// Handle new PriceBarOverlay
			PriceBarOverlay priceBarOverlay = getPriceBarOverlay();
			if (priceBarOverlay != null) {
				priceBarOverlay.setData(ohlcData);
			}
		}
	}

	// ========== Lifecycle ==========

	@Override
	public void repaint() {
		super.repaint();
		if (renderStrategy != null) {
			renderStrategy.requestRepaint();
		}
		if (textOverlay != null) {
			textOverlay.repaint();
		}
	}

	@Override
	public void requestFocus() {
		if (renderComponent != null) {
			renderComponent.requestFocus();
		}
	}

	/**
	 * Disposes of rendering resources.
	 */
	public void dispose() {
		if (renderStrategy != null) {
			renderStrategy.dispose();
		}
	}
}
