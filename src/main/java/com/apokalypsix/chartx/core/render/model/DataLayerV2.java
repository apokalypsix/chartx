package com.apokalypsix.chartx.core.render.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.style.ChartStyle;
import com.apokalypsix.chartx.core.data.HeikinAshiTransform;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.service.v2.CandlestickRendererV2;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for the main chart data using V2 abstracted renderers.
 *
 * <p>This layer uses {@link CandlestickRendererV2} which works with the
 * backend-agnostic rendering API. It requires the abstracted API to be
 * enabled on the RenderPipeline.
 *
 * <p>Supports multiple chart styles via {@link ChartStyle}:
 * <ul>
 *   <li>{@link ChartStyle#CANDLESTICK} - Traditional filled candlesticks</li>
 *   <li>{@link ChartStyle#OHLC_BAR} - Vertical line with horizontal ticks</li>
 *   <li>{@link ChartStyle#HOLLOW_CANDLE} - Outline for bullish, filled for bearish</li>
 *   <li>{@link ChartStyle#HEIKIN_ASHI} - Smoothed candles using calculated values</li>
 * </ul>
 */
public class DataLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(DataLayerV2.class);

    /** Z-order for the data layer (renders after background and grid) */
    public static final int Z_ORDER = 200;

    private final CandlestickRendererV2 candlestickRenderer;
    private OhlcData data;

    // Heikin-Ashi caching
    private OhlcData heikinAshiData;
    private boolean heikinAshiDirty = true;
    private final DataListener haInvalidationListener;

    // V2 initialization tracking
    private boolean v2Initialized = false;

    /**
     * Creates a data layer using V2 renderers.
     */
    public DataLayerV2() {
        super(Z_ORDER);
        this.candlestickRenderer = new CandlestickRendererV2();

        // Create listener to invalidate HA cache when source changes
        this.haInvalidationListener = new DataListener() {
            @Override
            public void onDataAppended(Data<?> data, int newIndex) {
                heikinAshiDirty = true;
                markDirty();
            }

            @Override
            public void onDataUpdated(Data<?> data, int index) {
                heikinAshiDirty = true;
                markDirty();
            }

            @Override
            public void onDataCleared(Data<?> data) {
                heikinAshiDirty = true;
                heikinAshiData = null;
                markDirty();
            }
        };
    }

    /**
     * Sets the OHLC data to render.
     */
    public void setData(OhlcData data) {
        // Remove listener from old data
        if (this.data != null) {
            this.data.removeListener(haInvalidationListener);
        }

        this.data = data;
        this.heikinAshiDirty = true;
        this.heikinAshiData = null;

        // Add listener to new data
        if (data != null) {
            data.addListener(haInvalidationListener);
        }

        markDirty();
    }

    /**
     * Returns the current data.
     */
    public OhlcData getData() {
        return data;
    }

    /**
     * Returns the candlestick renderer for configuration.
     */
    public CandlestickRendererV2 getCandlestickRenderer() {
        return candlestickRenderer;
    }

    /**
     * Sets the chart style.
     *
     * @param style the chart style to use
     */
    public void setChartStyle(ChartStyle style) {
        candlestickRenderer.setChartStyle(style);
        markDirty();
    }

    /**
     * Returns the current chart style.
     */
    public ChartStyle getChartStyle() {
        return candlestickRenderer.getChartStyle();
    }

    /**
     * Returns the effective data to render.
     * Returns Heikin-Ashi data when in HA mode, otherwise returns the source data.
     */
    public OhlcData getEffectiveData() {
        if (data == null) {
            return null;
        }

        ChartStyle style = candlestickRenderer.getChartStyle();
        if (style == ChartStyle.HEIKIN_ASHI) {
            ensureHeikinAshiData();
            return heikinAshiData;
        }

        return data;
    }

    /**
     * Ensures the Heikin-Ashi data is computed and up-to-date.
     */
    private void ensureHeikinAshiData() {
        if (heikinAshiDirty || heikinAshiData == null) {
            if (data != null && !data.isEmpty()) {
                heikinAshiData = HeikinAshiTransform.transform(data);
            } else {
                heikinAshiData = null;
            }
            heikinAshiDirty = false;
        }
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        // V2 renderers are initialized lazily when first rendered with a valid RenderContext
        log.debug("DataLayerV2 GL initialized (v2 renderer will init on first render)");
    }

    @Override
    public void render(RenderContext ctx) {
        // Check if abstracted API is available
        if (!ctx.hasAbstractedAPI()) {
            log.warn("DataLayerV2 requires abstracted API - skipping render");
            return;
        }

        // Initialize v2 renderer if needed
        if (!v2Initialized) {
            candlestickRenderer.initialize(ctx);
            v2Initialized = true;
            log.debug("CandlestickRendererV2 initialized");
        }

        OhlcData effectiveData = getEffectiveData();
        if (effectiveData != null && !effectiveData.isEmpty()) {
            candlestickRenderer.render(ctx, effectiveData);
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        // Note: v2 renderers need RenderContext for disposal
        // This will be handled when the context is available
        v2Initialized = false;

        // Clean up listener
        if (data != null) {
            data.removeListener(haInvalidationListener);
        }
    }

    /**
     * Disposes v2 renderer resources.
     * Call this when the RenderContext is available during cleanup.
     */
    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            candlestickRenderer.dispose(ctx);
            v2Initialized = false;
        }
    }
}
