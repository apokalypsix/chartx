package com.apokalypsix.chartx.core.render.service.v2;

import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.chart.style.ChartStyle;
import com.apokalypsix.chartx.core.render.model.ColoredCandleRule;
import com.apokalypsix.chartx.core.render.model.OHLCColorRule;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.render.api.*;

import java.awt.Color;

/**
 * Renders OHLC data as candlesticks using the abstracted rendering API.
 *
 * <p>This renderer uses the backend-agnostic rendering interfaces.
 *
 * <p>Supports multiple chart styles including:
 * <ul>
 *   <li>{@link ChartStyle#CANDLESTICK} - Traditional filled candlesticks</li>
 *   <li>{@link ChartStyle#OHLC_BAR} - Vertical line with horizontal ticks</li>
 *   <li>{@link ChartStyle#HOLLOW_CANDLE} - Outline for bullish, filled for bearish</li>
 *   <li>{@link ChartStyle#COLORED_CANDLE} - Custom coloring rules</li>
 *   <li>{@link ChartStyle#HEIKIN_ASHI} - Smoothed candles</li>
 * </ul>
 */
public class CandlestickRendererV2 {

    // Colors
    private Color bullishColor = new Color(38, 166, 91);   // Green
    private Color bearishColor = new Color(214, 69, 65);   // Red
    private Color wickColor = new Color(150, 150, 150);    // Gray

    // Configuration
    private float bodyWidthRatio = 0.8f;
    private float tickWidthRatio = 0.5f;
    private ChartStyle chartStyle = ChartStyle.CANDLESTICK;
    private OHLCColorRule colorRule = ColoredCandleRule.CLOSE_VS_OPEN;

    // Buffers
    private Buffer bodyBuffer;
    private Buffer wickBuffer;
    private Buffer tickBuffer;
    private Shader shader;

    // Vertex arrays
    private float[] bodyVertices;
    private float[] wickVertices;
    private float[] tickVertices;
    private int bodyVertexCapacity;
    private int wickVertexCapacity;
    private int tickVertexCapacity;
    private int hollowOutlineFloatCount;

    // Initialization tracking
    protected boolean initialized = false;

    // Constants
    private static final int BODY_VERTICES_PER_CANDLE = 6;  // 2 triangles
    private static final int WICK_VERTICES_PER_CANDLE = 2;  // 1 line
    private static final int TICK_VERTICES_PER_BAR = 4;     // 2 lines
    private static final int OUTLINE_VERTICES_PER_CANDLE = 8;  // 4 lines

    /**
     * Floats per vertex for position + color (x, y, r, g, b, a).
     */
    protected static final int FLOATS_PER_VERTEX_COLOR = 6;

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        // Create buffers
        bodyBuffer = resources.getOrCreateBuffer("candlestick.body",
                BufferDescriptor.positionColor2D(1024 * FLOATS_PER_VERTEX_COLOR));
        wickBuffer = resources.getOrCreateBuffer("candlestick.wick",
                BufferDescriptor.positionColor2D(1024 * FLOATS_PER_VERTEX_COLOR));
        tickBuffer = resources.getOrCreateBuffer("candlestick.tick",
                BufferDescriptor.positionColor2D(1024 * FLOATS_PER_VERTEX_COLOR));

        // Get shader
        shader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        // Pre-allocate vertex arrays
        bodyVertexCapacity = 256;
        wickVertexCapacity = 256;
        tickVertexCapacity = 256;
        bodyVertices = new float[bodyVertexCapacity * BODY_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR];
        wickVertices = new float[wickVertexCapacity * WICK_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR];
        tickVertices = new float[tickVertexCapacity * TICK_VERTICES_PER_BAR * FLOATS_PER_VERTEX_COLOR];

        initialized = true;
    }

    public void render(RenderContext ctx, OhlcData data) {
        if (data == null || data.isEmpty() || !ctx.hasVisibleData()) {
            return;
        }

        switch (chartStyle) {
            case CANDLESTICK, HEIKIN_ASHI -> renderCandlesticks(ctx, data);
            case OHLC_BAR -> renderOHLCBars(ctx, data);
            case HOLLOW_CANDLE -> renderHollowCandles(ctx, data);
            case COLORED_CANDLE -> renderColoredCandles(ctx, data);
        }
    }

    private void renderCandlesticks(RenderContext ctx, OhlcData data) {
        RenderDevice device = ctx.getDevice();
        CoordinateSystem coords = ctx.getCoordinatesForData(data);

        int firstIdx = ctx.getFirstVisibleIndex();
        int lastIdx = ctx.getLastVisibleIndex();
        int visibleCount = lastIdx - firstIdx + 1;

        ensureCapacity(visibleCount);

        double barWidth = ctx.getBarWidth();
        double halfBodyWidth = barWidth * bodyWidthRatio / 2.0;

        int bodyFloatCount = buildBodyVertices(data, coords, firstIdx, lastIdx, halfBodyWidth);
        int wickFloatCount = buildWickVertices(data, coords, firstIdx, lastIdx);

        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        if (bodyFloatCount > 0) {
            bodyBuffer.upload(bodyVertices, 0, bodyFloatCount);
            bodyBuffer.draw(DrawMode.TRIANGLES);
        }

        if (wickFloatCount > 0) {
            wickBuffer.upload(wickVertices, 0, wickFloatCount);
            wickBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private void renderOHLCBars(RenderContext ctx, OhlcData data) {
        RenderDevice device = ctx.getDevice();
        CoordinateSystem coords = ctx.getCoordinatesForData(data);

        int firstIdx = ctx.getFirstVisibleIndex();
        int lastIdx = ctx.getLastVisibleIndex();
        int visibleCount = lastIdx - firstIdx + 1;

        ensureCapacity(visibleCount);

        double barWidth = ctx.getBarWidth();
        double bodyWidth = barWidth * bodyWidthRatio;
        double tickWidth = bodyWidth * tickWidthRatio;

        int wickFloatCount = buildOHLCWickVertices(data, coords, firstIdx, lastIdx);
        int tickFloatCount = buildOHLCTickVertices(data, coords, firstIdx, lastIdx, tickWidth);

        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        if (wickFloatCount > 0) {
            wickBuffer.upload(wickVertices, 0, wickFloatCount);
            wickBuffer.draw(DrawMode.LINES);
        }

        if (tickFloatCount > 0) {
            tickBuffer.upload(tickVertices, 0, tickFloatCount);
            tickBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private void renderHollowCandles(RenderContext ctx, OhlcData data) {
        CoordinateSystem coords = ctx.getCoordinatesForData(data);

        int firstIdx = ctx.getFirstVisibleIndex();
        int lastIdx = ctx.getLastVisibleIndex();
        int visibleCount = lastIdx - firstIdx + 1;

        ensureHollowCapacity(visibleCount);

        double barWidth = ctx.getBarWidth();
        double halfBodyWidth = barWidth * bodyWidthRatio / 2.0;

        int bodyFloatCount = buildHollowBodyVertices(data, coords, firstIdx, lastIdx, halfBodyWidth);
        int wickFloatCount = buildWickVertices(data, coords, firstIdx, lastIdx);

        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Draw wicks first
        if (wickFloatCount > 0) {
            wickBuffer.upload(wickVertices, 0, wickFloatCount);
            wickBuffer.draw(DrawMode.LINES);
        }

        // Draw filled bodies (bearish)
        if (bodyFloatCount > 0) {
            bodyBuffer.upload(bodyVertices, 0, bodyFloatCount);
            bodyBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw hollow outlines (bullish)
        if (hollowOutlineFloatCount > 0) {
            tickBuffer.upload(tickVertices, 0, hollowOutlineFloatCount);
            tickBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private void renderColoredCandles(RenderContext ctx, OhlcData data) {
        CoordinateSystem coords = ctx.getCoordinatesForData(data);

        int firstIdx = ctx.getFirstVisibleIndex();
        int lastIdx = ctx.getLastVisibleIndex();
        int visibleCount = lastIdx - firstIdx + 1;

        ensureCapacity(visibleCount);

        double barWidth = ctx.getBarWidth();
        double halfBodyWidth = barWidth * bodyWidthRatio / 2.0;

        int bodyFloatCount = buildColoredBodyVertices(data, coords, firstIdx, lastIdx, halfBodyWidth);
        int wickFloatCount = buildColoredWickVertices(data, coords, firstIdx, lastIdx);

        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        if (bodyFloatCount > 0) {
            bodyBuffer.upload(bodyVertices, 0, bodyFloatCount);
            bodyBuffer.draw(DrawMode.TRIANGLES);
        }

        if (wickFloatCount > 0) {
            wickBuffer.upload(wickVertices, 0, wickFloatCount);
            wickBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    // ========== Vertex Building Methods ==========

    private int buildBodyVertices(OhlcData data, CoordinateSystem coords,
                                   int firstIdx, int lastIdx, double halfBodyWidth) {
        int floatIndex = 0;
        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float open = opens[i];
            float close = closes[i];

            boolean bullish = close >= open;
            Color color = bullish ? bullishColor : bearishColor;

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

            // Two triangles for body
            floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, left, bottom, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);

            floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, top, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildWickVertices(OhlcData data, CoordinateSystem coords,
                                   int firstIdx, int lastIdx) {
        int floatIndex = 0;
        long[] timestamps = data.getTimestampsArray();
        float[] highs = data.getHighArray();
        float[] lows = data.getLowArray();

        float r = wickColor.getRed() / 255f;
        float g = wickColor.getGreen() / 255f;
        float b = wickColor.getBlue() / 255f;
        float a = 1.0f;

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float highY = (float) coords.yValueToScreenY(highs[i]);
            float lowY = (float) coords.yValueToScreenY(lows[i]);

            floatIndex = addVertex(wickVertices, floatIndex, x, highY, r, g, b, a);
            floatIndex = addVertex(wickVertices, floatIndex, x, lowY, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildColoredBodyVertices(OhlcData data, CoordinateSystem coords,
                                          int firstIdx, int lastIdx, double halfBodyWidth) {
        int floatIndex = 0;
        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float open = opens[i];
            float close = closes[i];

            Color color = colorRule.getColor(data, i, bullishColor, bearishColor);

            float top = (float) coords.yValueToScreenY(Math.max(open, close));
            float bottom = (float) coords.yValueToScreenY(Math.min(open, close));
            float left = (float) (x - halfBodyWidth);
            float right = (float) (x + halfBodyWidth);

            if (Math.abs(top - bottom) < 1.0f) {
                float mid = (top + bottom) / 2;
                top = mid - 0.5f;
                bottom = mid + 0.5f;
            }

            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, left, bottom, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);

            floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);
            floatIndex = addVertex(bodyVertices, floatIndex, right, top, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildColoredWickVertices(OhlcData data, CoordinateSystem coords,
                                          int firstIdx, int lastIdx) {
        int floatIndex = 0;
        long[] timestamps = data.getTimestampsArray();
        float[] highs = data.getHighArray();
        float[] lows = data.getLowArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float highY = (float) coords.yValueToScreenY(highs[i]);
            float lowY = (float) coords.yValueToScreenY(lows[i]);

            Color color = colorRule.getColor(data, i, bullishColor, bearishColor);
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            floatIndex = addVertex(wickVertices, floatIndex, x, highY, r, g, b, a);
            floatIndex = addVertex(wickVertices, floatIndex, x, lowY, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildHollowBodyVertices(OhlcData data, CoordinateSystem coords,
                                         int firstIdx, int lastIdx, double halfBodyWidth) {
        int floatIndex = 0;
        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] closes = data.getCloseArray();

        // Build filled bodies for bearish candles only
        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float open = opens[i];
            float close = closes[i];
            boolean bullish = close >= open;

            if (!bullish) {
                Color color = bearishColor;
                float top = (float) coords.yValueToScreenY(open);
                float bottom = (float) coords.yValueToScreenY(close);
                float left = (float) (x - halfBodyWidth);
                float right = (float) (x + halfBodyWidth);

                if (Math.abs(top - bottom) < 1.0f) {
                    float mid = (top + bottom) / 2;
                    top = mid - 0.5f;
                    bottom = mid + 0.5f;
                }

                float r = color.getRed() / 255f;
                float g = color.getGreen() / 255f;
                float b = color.getBlue() / 255f;
                float a = 1.0f;

                floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(bodyVertices, floatIndex, left, bottom, r, g, b, a);
                floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);

                floatIndex = addVertex(bodyVertices, floatIndex, left, top, r, g, b, a);
                floatIndex = addVertex(bodyVertices, floatIndex, right, bottom, r, g, b, a);
                floatIndex = addVertex(bodyVertices, floatIndex, right, top, r, g, b, a);
            }
        }

        // Build bullish outlines
        int tickFloatIndex = 0;
        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float open = opens[i];
            float close = closes[i];
            boolean bullish = close >= open;

            if (bullish) {
                Color color = bullishColor;
                float top = (float) coords.yValueToScreenY(close);
                float bottom = (float) coords.yValueToScreenY(open);
                float left = (float) (x - halfBodyWidth);
                float right = (float) (x + halfBodyWidth);

                if (Math.abs(top - bottom) < 1.0f) {
                    float mid = (top + bottom) / 2;
                    top = mid - 0.5f;
                    bottom = mid + 0.5f;
                }

                float r = color.getRed() / 255f;
                float g = color.getGreen() / 255f;
                float b = color.getBlue() / 255f;
                float a = 1.0f;

                // 4 lines for rectangle outline
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, left, top, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, right, top, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, left, bottom, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, right, bottom, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, left, top, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, left, bottom, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, right, top, r, g, b, a);
                tickFloatIndex = addVertex(tickVertices, tickFloatIndex, right, bottom, r, g, b, a);
            }
        }

        hollowOutlineFloatCount = tickFloatIndex;
        return floatIndex;
    }

    private int buildOHLCWickVertices(OhlcData data, CoordinateSystem coords,
                                       int firstIdx, int lastIdx) {
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
            Color color = bullish ? bullishColor : bearishColor;
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            floatIndex = addVertex(wickVertices, floatIndex, x, highY, r, g, b, a);
            floatIndex = addVertex(wickVertices, floatIndex, x, lowY, r, g, b, a);
        }

        return floatIndex;
    }

    private int buildOHLCTickVertices(OhlcData data, CoordinateSystem coords,
                                       int firstIdx, int lastIdx, double tickWidth) {
        int floatIndex = 0;
        long[] timestamps = data.getTimestampsArray();
        float[] opens = data.getOpenArray();
        float[] closes = data.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            float x = (float) coords.xValueToScreenX(timestamps[i]);
            float openY = (float) coords.yValueToScreenY(opens[i]);
            float closeY = (float) coords.yValueToScreenY(closes[i]);

            boolean bullish = closes[i] >= opens[i];
            Color color = bullish ? bullishColor : bearishColor;
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = 1.0f;

            // Open tick to the left
            floatIndex = addVertex(tickVertices, floatIndex, (float) (x - tickWidth), openY, r, g, b, a);
            floatIndex = addVertex(tickVertices, floatIndex, x, openY, r, g, b, a);

            // Close tick to the right
            floatIndex = addVertex(tickVertices, floatIndex, x, closeY, r, g, b, a);
            floatIndex = addVertex(tickVertices, floatIndex, (float) (x + tickWidth), closeY, r, g, b, a);
        }

        return floatIndex;
    }

    // ========== Capacity Management ==========

    private void ensureCapacity(int candleCount) {
        int requiredBodyFloats = candleCount * BODY_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR;
        int requiredWickFloats = candleCount * WICK_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR;
        int requiredTickFloats = candleCount * TICK_VERTICES_PER_BAR * FLOATS_PER_VERTEX_COLOR;

        if (requiredBodyFloats > bodyVertices.length) {
            bodyVertexCapacity = candleCount + candleCount / 2;
            bodyVertices = new float[bodyVertexCapacity * BODY_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR];
        }

        if (requiredWickFloats > wickVertices.length) {
            wickVertexCapacity = candleCount + candleCount / 2;
            wickVertices = new float[wickVertexCapacity * WICK_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR];
        }

        if (requiredTickFloats > tickVertices.length) {
            tickVertexCapacity = candleCount + candleCount / 2;
            tickVertices = new float[tickVertexCapacity * TICK_VERTICES_PER_BAR * FLOATS_PER_VERTEX_COLOR];
        }
    }

    private void ensureHollowCapacity(int candleCount) {
        ensureCapacity(candleCount);

        int requiredOutlineFloats = candleCount * OUTLINE_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR;
        if (requiredOutlineFloats > tickVertices.length) {
            tickVertexCapacity = candleCount + candleCount / 2;
            tickVertices = new float[tickVertexCapacity * OUTLINE_VERTICES_PER_CANDLE * FLOATS_PER_VERTEX_COLOR];
        }
    }

    public void dispose(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();
        if (resources != null) {
            resources.disposeBuffer("candlestick.body");
            resources.disposeBuffer("candlestick.wick");
            resources.disposeBuffer("candlestick.tick");
        }
        bodyBuffer = null;
        wickBuffer = null;
        tickBuffer = null;
        shader = null;
        initialized = false;
    }

    // ========== Configuration ==========

    public void setBullishColor(Color color) { this.bullishColor = color; }
    public Color getBullishColor() { return bullishColor; }

    public void setBearishColor(Color color) { this.bearishColor = color; }
    public Color getBearishColor() { return bearishColor; }

    public void setWickColor(Color color) { this.wickColor = color; }
    public Color getWickColor() { return wickColor; }

    public void setBodyWidthRatio(float ratio) { this.bodyWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio)); }
    public float getBodyWidthRatio() { return bodyWidthRatio; }

    public void setChartStyle(ChartStyle style) { this.chartStyle = style != null ? style : ChartStyle.CANDLESTICK; }
    public ChartStyle getChartStyle() { return chartStyle; }

    public void setColorRule(OHLCColorRule rule) { this.colorRule = rule != null ? rule : ColoredCandleRule.CLOSE_VS_OPEN; }
    public OHLCColorRule getColorRule() { return colorRule; }

    public void setTickWidthRatio(float ratio) { this.tickWidthRatio = Math.max(0.1f, Math.min(1.0f, ratio)); }
    public float getTickWidthRatio() { return tickWidthRatio; }

    // ========== Helper Methods ==========

    /**
     * Helper to add a vertex with position and color to a float array.
     */
    protected int addVertex(float[] vertices, int index, float x, float y,
                            float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }
}
