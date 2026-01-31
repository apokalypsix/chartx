package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions.OhlcStyle;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;

/**
 * Renderable candlestick series using OhlcData.
 *
 * <p>Supports multiple chart styles:
 * <ul>
 *   <li>CANDLESTICK - Standard filled candlesticks</li>
 *   <li>HOLLOW_CANDLE - Hollow body for bullish, filled for bearish</li>
 *   <li>OHLC_BAR - Traditional OHLC bars with horizontal ticks</li>
 *   <li>LINE - Simple line connecting close prices</li>
 * </ul>
 */
public class CandlestickSeries extends AbstractRenderableSeries<OhlcData, OhlcSeriesOptions> {

    // Floats per vertex: x, y, r, g, b, a
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int BODY_VERTICES_PER_CANDLE = 6;
    private static final int WICK_VERTICES_PER_CANDLE = 4;  // 2 segments (upper + lower) for split wicks
    private static final int OUTLINE_VERTICES_PER_CANDLE = 8;  // 4 lines = 8 vertices
    private static final int OHLC_VERTICES_PER_BAR = 6;  // wick + open tick + close tick

    // Rendering resources
    private Buffer bodyBuffer;
    private Buffer wickBuffer;
    private Buffer lineBuffer;  // For LINE style and outlines
    private ResourceManager resourceManager;

    // Reusable vertex arrays
    private float[] bodyVertices;
    private float[] wickVertices;
    private float[] lineVertices;
    private int vertexCapacity;

    /**
     * Creates a candlestick series with the given data and default options.
     */
    public CandlestickSeries(OhlcData data) {
        this(data, new OhlcSeriesOptions());
    }

    /**
     * Creates a candlestick series with the given data and options.
     */
    public CandlestickSeries(OhlcData data, OhlcSeriesOptions options) {
        super(data, options);
    }

    /**
     * Creates a candlestick series with a custom ID.
     */
    public CandlestickSeries(String id, OhlcData data, OhlcSeriesOptions options) {
        super(id, data, options);
    }

    @Override
    public SeriesType getType() {
        return SeriesType.CANDLESTICK;
    }

    @Override
    protected void doInitialize(ResourceManager resources) {
        this.resourceManager = resources;

        BufferDescriptor desc = BufferDescriptor.positionColor2D(1024);
        bodyBuffer = resources.getOrCreateBuffer(id + "_body", desc);
        wickBuffer = resources.getOrCreateBuffer(id + "_wick", desc);

        BufferDescriptor lineDesc = BufferDescriptor.positionColor2D(2048);
        lineBuffer = resources.getOrCreateBuffer(id + "_line", lineDesc);

        vertexCapacity = 256;
        bodyVertices = new float[vertexCapacity * BODY_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX];
        wickVertices = new float[vertexCapacity * WICK_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX];
        lineVertices = new float[vertexCapacity * OUTLINE_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX];
    }

    @Override
    protected void doDispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_body");
            resourceManager.disposeBuffer(id + "_wick");
            resourceManager.disposeBuffer(id + "_line");
        }
        bodyBuffer = null;
        wickBuffer = null;
        lineBuffer = null;
    }

    @Override
    public void render(RenderContext ctx) {
        if (!initialized || data.isEmpty() || !options.isVisible()) {
            return;
        }

        int firstIdx = data.getFirstVisibleIndex(ctx.getViewport().getStartTime());
        int lastIdx = data.getLastVisibleIndex(ctx.getViewport().getEndTime());

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return;
        }

        int visibleCount = lastIdx - firstIdx + 1;
        ensureCapacity(visibleCount);

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        OhlcStyle style = options.getStyle();

        switch (style) {
            case LINE:
                renderLineStyle(ctx, coords, firstIdx, lastIdx);
                break;
            case OHLC_BAR:
                renderOhlcBarStyle(ctx, coords, firstIdx, lastIdx, ctx.getBarWidth());
                break;
            case HOLLOW_CANDLE:
                renderHollowCandleStyle(ctx, coords, firstIdx, lastIdx, ctx.getBarWidth());
                break;
            case CANDLESTICK:
            case HEIKIN_ASHI:
            default:
                renderCandlestickStyle(ctx, coords, firstIdx, lastIdx, ctx.getBarWidth());
                break;
        }

        shader.unbind();
    }

    private void renderCandlestickStyle(RenderContext ctx, CoordinateSystem coords,
                                         int firstIdx, int lastIdx, double barWidth) {
        double bodyWidth = barWidth * options.getBarWidthRatio();
        double halfBodyWidth = bodyWidth / 2.0;

        int bodyFloatCount = buildFilledBodyVertices(coords, firstIdx, lastIdx, halfBodyWidth, false);
        int wickFloatCount = buildWickVertices(coords, firstIdx, lastIdx);

        // Draw wicks first (behind bodies)
        if (wickFloatCount > 0) {
            ctx.getDevice().setLineWidth(options.getWickWidth());
            wickBuffer.upload(wickVertices, 0, wickFloatCount);
            wickBuffer.draw(DrawMode.LINES);
        }

        // Draw bodies
        if (bodyFloatCount > 0) {
            bodyBuffer.upload(bodyVertices, 0, bodyFloatCount);
            bodyBuffer.draw(DrawMode.TRIANGLES);
        }
    }

    private void renderHollowCandleStyle(RenderContext ctx, CoordinateSystem coords,
                                          int firstIdx, int lastIdx, double barWidth) {
        double bodyWidth = barWidth * options.getBarWidthRatio();
        double halfBodyWidth = bodyWidth / 2.0;

        // Build split wicks (upper and lower segments, not through body)
        int wickFloatCount = buildSplitWickVertices(coords, firstIdx, lastIdx);

        // Build filled bodies for bearish candles only
        int bodyFloatCount = buildFilledBodyVertices(coords, firstIdx, lastIdx, halfBodyWidth, true);

        // Build outlines for bullish candles (hollow)
        int outlineFloatCount = buildHollowOutlineVertices(coords, firstIdx, lastIdx, halfBodyWidth);

        // Draw wicks first
        if (wickFloatCount > 0) {
            ctx.getDevice().setLineWidth(options.getWickWidth());
            wickBuffer.upload(wickVertices, 0, wickFloatCount);
            wickBuffer.draw(DrawMode.LINES);
        }

        // Draw filled bodies (bearish)
        if (bodyFloatCount > 0) {
            bodyBuffer.upload(bodyVertices, 0, bodyFloatCount);
            bodyBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw outlines (bullish hollow)
        if (outlineFloatCount > 0) {
            ctx.getDevice().setLineWidth(1.5f);
            lineBuffer.upload(lineVertices, 0, outlineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }
    }

    private void renderOhlcBarStyle(RenderContext ctx, CoordinateSystem coords,
                                     int firstIdx, int lastIdx, double barWidth) {
        double tickWidth = barWidth * options.getBarWidthRatio() * 0.4;

        int lineFloatCount = buildOhlcBarVertices(coords, firstIdx, lastIdx, tickWidth);

        if (lineFloatCount > 0) {
            ctx.getDevice().setLineWidth(options.getWickWidth());
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }
    }

    private void renderLineStyle(RenderContext ctx, CoordinateSystem coords,
                                   int firstIdx, int lastIdx) {
        int lineFloatCount = buildCloseLineVertices(coords, firstIdx, lastIdx);

        if (lineFloatCount > 0) {
            ctx.getDevice().setLineWidth(1.5f);
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINE_STRIP);
        }
    }

    private int buildFilledBodyVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                         double halfBodyWidth, boolean bearishOnly) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float open = opens[i];
            float close = closes[i];
            boolean bullish = close >= open;

            // Skip bullish candles if bearishOnly is true
            if (bearishOnly && bullish) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            Color color = options.getColorForCandle(bullish);

            float top = (float) coords.yValueToScreenY(Math.max(open, close));
            float bottom = (float) coords.yValueToScreenY(Math.min(open, close));
            float left = (float) (x - halfBodyWidth);
            float right = (float) (x + halfBodyWidth);

            // Ensure minimum body height for doji candles
            if (Math.abs(top - bottom) < 1.0f) {
                float mid = (top + bottom) / 2;
                top = mid - 0.5f;
                bottom = mid + 0.5f;
            }

            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            // Triangle 1: top-left, bottom-left, bottom-right
            floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, left, bottom, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);

            // Triangle 2: top-left, bottom-right, top-right
            floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, top, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildHollowOutlineVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                            double halfBodyWidth) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float open = opens[i];
            float close = closes[i];
            boolean bullish = close >= open;

            // Only draw outlines for bullish candles (hollow)
            if (!bullish) {
                continue;
            }

            float x = (float) coords.xValueToScreenX(timestamps[i]);
            Color color = options.getColorForCandle(true);

            float top = (float) coords.yValueToScreenY(close);
            float bottom = (float) coords.yValueToScreenY(open);
            float left = (float) (x - halfBodyWidth);
            float right = (float) (x + halfBodyWidth);

            // Ensure minimum body height
            if (Math.abs(top - bottom) < 1.0f) {
                float mid = (top + bottom) / 2;
                top = mid - 0.5f;
                bottom = mid + 0.5f;
            }

            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            // 4 lines forming the rectangle outline
            // Top line
            floatIndex = addVertex(lineVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, right, top, r, g, b, a);
            // Right line
            floatIndex = addVertex(lineVertices, floatIndex, right, top, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, right, bottom, r, g, b, a);
            // Bottom line
            floatIndex = addVertex(lineVertices, floatIndex, right, bottom, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, left, bottom, r, g, b, a);
            // Left line
            floatIndex = addVertex(lineVertices, floatIndex, left, bottom, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, left, top, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildOhlcBarVertices(CoordinateSystem coords, int firstIdx, int lastIdx,
                                      double tickWidth) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] highs = data.getHighArray();
        float[] lows = data.getLowArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float highY = (float) coords.yValueToScreenY(highs[i]);
            float lowY = (float) coords.yValueToScreenY(lows[i]);
            float openY = (float) coords.yValueToScreenY(opens[i]);
            float closeY = (float) coords.yValueToScreenY(closes[i]);

            boolean bullish = closes[i] >= opens[i];
            Color color = options.getColorForCandle(bullish);
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            // Vertical line from high to low
            floatIndex = addVertex(lineVertices, floatIndex, x, highY, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, x, lowY, r, g, b, a);

            // Open tick (left side)
            floatIndex = addVertex(lineVertices, floatIndex, (float) (x - tickWidth), openY, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, x, openY, r, g, b, a);

            // Close tick (right side)
            floatIndex = addVertex(lineVertices, floatIndex, x, closeY, r, g, b, a);
            floatIndex = addVertex(lineVertices, floatIndex, (float) (x + tickWidth), closeY, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildCloseLineVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] closes = data.getCloseArray();

        // Use upColor for the line
        Color color = options.getUpColor();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = 1.0f;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float y = (float) coords.yValueToScreenY(closes[i]);
            floatIndex = addVertex(lineVertices, floatIndex, x, y, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildWickVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] highs = data.getHighArray();
        float[] lows = data.getLowArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float highY = (float) coords.yValueToScreenY(highs[i]);
            float lowY = (float) coords.yValueToScreenY(lows[i]);

            boolean bullish = closes[i] >= opens[i];
            Color color = options.getWickColorForCandle(bullish);
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            // Wick line from high to low
            floatIndex = addVertex(wickVertices, floatIndex, x, highY, r, g, b, a);
            floatIndex = addVertex(wickVertices, floatIndex, x, lowY, r, g, b, a);
        }

        return floatIndex;
    }

    /**
     * Builds wick vertices with upper and lower segments separated by the body.
     * Used for hollow candle style where wicks shouldn't show through hollow bodies.
     */
    private int buildSplitWickVertices(CoordinateSystem coords, int firstIdx, int lastIdx) {
        int floatIndex = 0;

        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] highs = data.getHighArray();
        float[] lows = data.getLowArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float highY = (float) coords.yValueToScreenY(highs[i]);
            float lowY = (float) coords.yValueToScreenY(lows[i]);

            // Body top and bottom (in screen coordinates)
            float bodyTop = (float) coords.yValueToScreenY(Math.max(opens[i], closes[i]));
            float bodyBottom = (float) coords.yValueToScreenY(Math.min(opens[i], closes[i]));

            boolean bullish = closes[i] >= opens[i];
            Color color = options.getWickColorForCandle(bullish);
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            // Upper wick: from high to top of body
            floatIndex = addVertex(wickVertices, floatIndex, x, highY, r, g, b, a);
            floatIndex = addVertex(wickVertices, floatIndex, x, bodyTop, r, g, b, a);

            // Lower wick: from bottom of body to low
            floatIndex = addVertex(wickVertices, floatIndex, x, bodyBottom, r, g, b, a);
            floatIndex = addVertex(wickVertices, floatIndex, x, lowY, r, g, b, a);
        }

        return floatIndex;
    }

    private int addVertex(float[] vertices, int index, float x, float y,
                          float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }

    private void ensureCapacity(int candleCount) {
        int requiredBodyFloats = candleCount * BODY_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX;
        int requiredWickFloats = candleCount * WICK_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX;
        int requiredLineFloats = candleCount * OUTLINE_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX;

        if (requiredBodyFloats > bodyVertices.length ||
            requiredWickFloats > wickVertices.length ||
            requiredLineFloats > lineVertices.length) {

            vertexCapacity = candleCount + candleCount / 2;
            bodyVertices = new float[vertexCapacity * BODY_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX];
            wickVertices = new float[vertexCapacity * WICK_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX];
            lineVertices = new float[vertexCapacity * OUTLINE_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX];
        }
    }

    @Override
    public double getMinValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        return data.findLowestLow(startIdx, endIdx);
    }

    @Override
    public double getMaxValue(int startIdx, int endIdx) {
        if (data.isEmpty() || startIdx < 0 || endIdx < 0) {
            return Double.NaN;
        }
        startIdx = Math.max(0, startIdx);
        endIdx = Math.min(data.size() - 1, endIdx);
        return data.findHighestHigh(startIdx, endIdx);
    }
}
