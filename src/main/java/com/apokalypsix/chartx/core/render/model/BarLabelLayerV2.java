package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.data.OHLCBar;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.gl.TextRenderer;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for displaying text labels on each bar using the abstracted rendering API.
 *
 * <p>This is the V2 version of {@link BarLabelLayer} that works with the abstracted
 * rendering API context. Since text rendering is complex and uses Java2D/textures,
 * this layer continues to use {@link TextRenderer} for actual text rendering.
 *
 * <p>BarLabelLayerV2 provides:
 * <ul>
 *   <li>Per-bar text labels (e.g., delta, volume, custom values)</li>
 *   <li>Configurable label positioning (above, below, center)</li>
 *   <li>LOD-aware visibility (hides labels when zoomed out)</li>
 *   <li>Customizable label formatting</li>
 * </ul>
 *
 * <p>Labels are only rendered when bars are wide enough to be readable,
 * preventing visual clutter when zoomed out.
 */
public class BarLabelLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(BarLabelLayerV2.class);

    /** Z-order for bar labels (above data, below drawings) */
    public static final int Z_ORDER = 550;

    /**
     * Label positioning options.
     */
    public enum LabelPosition {
        /** Above the bar's high */
        ABOVE,
        /** Below the bar's low */
        BELOW,
        /** Centered on the bar */
        CENTER,
        /** Inside the bar body at top */
        INSIDE_TOP,
        /** Inside the bar body at bottom */
        INSIDE_BOTTOM,
        /** At the bar's close price */
        AT_CLOSE,
        /** At the bar's open price */
        AT_OPEN
    }

    /**
     * Provides label text for a bar.
     */
    @FunctionalInterface
    public interface LabelProvider {
        /**
         * Returns the label text for the given bar, or null to skip.
         *
         * @param index the bar index
         * @param bar the bar data
         * @return label text, or null to skip this bar
         */
        String getLabel(int index, OHLCBar bar);
    }

    /**
     * Provides label color for a bar.
     */
    @FunctionalInterface
    public interface ColorProvider {
        /**
         * Returns the color for the given bar's label.
         *
         * @param index the bar index
         * @param bar the bar data
         * @return label color
         */
        Color getColor(int index, OHLCBar bar);
    }

    // Configuration
    private OhlcData data;
    private LabelProvider labelProvider;
    private ColorProvider colorProvider;
    private LabelPosition position = LabelPosition.ABOVE;
    private Color defaultColor = Color.WHITE;
    private float fontSize = 10f;
    private int verticalOffset = 3;  // Pixels from bar
    private double minBarWidth = 10.0;  // Minimum bar width in pixels to show labels

    // State
    private TextRenderer textRenderer;
    private long barDuration;

    // Reusable bar instance
    private final OHLCBar reusableBar = new OHLCBar();

    /**
     * Creates a bar label layer.
     */
    public BarLabelLayerV2() {
        super(Z_ORDER);
    }

    // ========== Configuration ==========

    /**
     * Sets the OHLC data to label.
     */
    public void setData(OhlcData data) {
        this.data = data;
        markDirty();
    }

    /**
     * Sets the bar duration for width calculations.
     */
    public void setBarDuration(long barDuration) {
        this.barDuration = barDuration;
    }

    /**
     * Sets the label provider function.
     *
     * @param provider function that returns label text for each bar
     */
    public void setLabelProvider(LabelProvider provider) {
        this.labelProvider = provider;
        markDirty();
    }

    /**
     * Sets the color provider function.
     *
     * @param provider function that returns color for each bar's label
     */
    public void setColorProvider(ColorProvider provider) {
        this.colorProvider = provider;
    }

    /**
     * Sets the label position.
     */
    public void setPosition(LabelPosition position) {
        this.position = position;
        markDirty();
    }

    /**
     * Sets the default label color.
     */
    public void setDefaultColor(Color color) {
        this.defaultColor = color;
    }

    /**
     * Sets the font size.
     */
    public void setFontSize(float size) {
        this.fontSize = size;
        markDirty();
    }

    /**
     * Sets the vertical offset from the bar.
     */
    public void setVerticalOffset(int offset) {
        this.verticalOffset = offset;
    }

    /**
     * Sets the minimum bar width in pixels to show labels.
     * Labels are hidden when bars are narrower than this.
     */
    public void setMinBarWidth(double width) {
        this.minBarWidth = width;
    }

    // ========== Common Label Providers ==========

    /**
     * Creates a label provider that shows volume.
     */
    public static LabelProvider volumeProvider() {
        return (index, bar) -> formatVolume(bar.getVolume());
    }

    /**
     * Creates a label provider that shows close price.
     */
    public static LabelProvider closePriceProvider() {
        return (index, bar) -> formatPrice(bar.getClose());
    }

    /**
     * Creates a label provider that shows change percentage.
     */
    public static LabelProvider changeProvider() {
        return (index, bar) -> {
            float change = bar.getClose() - bar.getOpen();
            float pct = (change / bar.getOpen()) * 100;
            return String.format("%.2f%%", pct);
        };
    }

    /**
     * Creates a label provider that shows delta (custom value).
     *
     * @param deltaFunction function to compute delta from bar
     */
    public static LabelProvider deltaProvider(BiFunction<Integer, OHLCBar, Float> deltaFunction) {
        return (index, bar) -> {
            Float delta = deltaFunction.apply(index, bar);
            if (delta == null) return null;
            return formatVolume(delta);
        };
    }

    /**
     * Creates a color provider for positive/negative values.
     *
     * @param positiveColor color for positive values
     * @param negativeColor color for negative values
     * @param neutralColor color for zero/neutral values
     */
    public static ColorProvider bullBearColorProvider(Color positiveColor, Color negativeColor, Color neutralColor) {
        return (index, bar) -> {
            if (bar.getClose() > bar.getOpen()) return positiveColor;
            if (bar.getClose() < bar.getOpen()) return negativeColor;
            return neutralColor;
        };
    }

    // ========== Rendering ==========

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        textRenderer = new TextRenderer(fontSize);
        log.debug("BarLabelLayerV2 initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        if (data == null || data.isEmpty() || labelProvider == null) {
            return;
        }

        // Check if abstracted API context is available
        // Note: TextRenderer handles its own GL rendering internally, so we proceed
        // regardless of hasAbstractedAPI(). The check is here for consistency with
        // V2 layer patterns but does not skip rendering.
        if (ctx.hasAbstractedAPI()) {
            log.trace("BarLabelLayerV2 rendering with abstracted API context");
        }

        // Get GL context - TextRenderer requires it
        GL2ES2 gl = ctx.getGL();
        if (gl == null) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinates();

        // Check if bars are wide enough to show labels
        double barWidth = coords.getPixelWidth(barDuration);
        if (barWidth < minBarWidth) {
            return;  // Too zoomed out, skip labels
        }

        // Get visible range
        int firstVisible = ctx.getFirstVisibleIndex();
        int lastVisible = ctx.getLastVisibleIndex();

        if (firstVisible < 0 || lastVisible < 0) {
            return;
        }

        // Begin text batch
        textRenderer.setFontSize(fontSize);
        if (!textRenderer.beginBatch(gl, ctx.getViewport().getWidth(), ctx.getViewport().getHeight())) {
            return;
        }

        try {
            // Render labels for visible bars
            for (int i = firstVisible; i <= lastVisible && i < data.size(); i++) {
                data.getBar(i, reusableBar);

                // Get label text
                String label = labelProvider.getLabel(i, reusableBar);
                if (label == null || label.isEmpty()) {
                    continue;
                }

                // Calculate position
                float x = (float) coords.xValueToScreenX(reusableBar.getTimestamp() + barDuration / 2);
                float y = calculateY(coords, reusableBar);

                // Get color
                Color color = colorProvider != null ?
                        colorProvider.getColor(i, reusableBar) : defaultColor;

                // Render label (centered horizontally)
                textRenderer.drawTextCentered(label, x, y, color);
            }
        } finally {
            textRenderer.endBatch(gl);
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        if (textRenderer != null) {
            textRenderer.dispose(gl);
            textRenderer = null;
        }
        log.debug("BarLabelLayerV2 disposed");
    }

    private float calculateY(CoordinateSystem coords, OHLCBar bar) {
        float y;
        switch (position) {
            case ABOVE:
                y = (float) coords.yValueToScreenY(bar.getHigh()) - verticalOffset - fontSize;
                break;
            case BELOW:
                y = (float) coords.yValueToScreenY(bar.getLow()) + verticalOffset;
                break;
            case CENTER:
                float midPrice = (bar.getHigh() + bar.getLow()) / 2;
                y = (float) coords.yValueToScreenY(midPrice) - fontSize / 2;
                break;
            case INSIDE_TOP:
                float bodyTop = Math.max(bar.getOpen(), bar.getClose());
                y = (float) coords.yValueToScreenY(bodyTop) + verticalOffset;
                break;
            case INSIDE_BOTTOM:
                float bodyBottom = Math.min(bar.getOpen(), bar.getClose());
                y = (float) coords.yValueToScreenY(bodyBottom) - verticalOffset - fontSize;
                break;
            case AT_CLOSE:
                y = (float) coords.yValueToScreenY(bar.getClose()) - fontSize / 2;
                break;
            case AT_OPEN:
                y = (float) coords.yValueToScreenY(bar.getOpen()) - fontSize / 2;
                break;
            default:
                y = (float) coords.yValueToScreenY(bar.getHigh()) - verticalOffset - fontSize;
        }
        return y;
    }

    // ========== Formatting Helpers ==========

    private static String formatVolume(float volume) {
        if (volume >= 1_000_000) {
            return String.format("%.1fM", volume / 1_000_000);
        } else if (volume >= 1_000) {
            return String.format("%.1fK", volume / 1_000);
        } else {
            return String.format("%.0f", volume);
        }
    }

    private static String formatPrice(float price) {
        if (price >= 1000) {
            return String.format("%.0f", price);
        } else if (price >= 100) {
            return String.format("%.1f", price);
        } else {
            return String.format("%.2f", price);
        }
    }
}
