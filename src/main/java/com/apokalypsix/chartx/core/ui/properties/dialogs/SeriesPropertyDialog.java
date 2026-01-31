package com.apokalypsix.chartx.core.ui.properties.dialogs;

import com.apokalypsix.chartx.chart.style.*;
import com.apokalypsix.chartx.chart.style.LineSeriesOptions.DisplayMode;
import com.apokalypsix.chartx.chart.style.OhlcSeriesOptions.OhlcStyle;
import com.apokalypsix.chartx.core.ui.properties.PropertyDialog;
import com.apokalypsix.chartx.core.ui.properties.editors.*;

import javax.swing.*;
import java.awt.*;

/**
 * Property dialog for editing series rendering options.
 *
 * <p>Supports different series types with type-specific property editors:
 * <ul>
 *   <li>OhlcSeriesOptions: up/down colors, wick color, style, bar width</li>
 *   <li>LineSeriesOptions: color, line width, fill options</li>
 *   <li>HistogramSeriesOptions: positive/negative colors, bar width</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SeriesPropertyDialog dialog = new SeriesPropertyDialog(frame, ohlcOptions);
 * if (dialog.showDialog()) {
 *     // Options are already modified
 *     chart.repaint();
 * }
 * }</pre>
 */
public class SeriesPropertyDialog extends PropertyDialog {

    private final SeriesOptions options;

    // For OhlcSeriesOptions
    private ColorPropertyEditor upColorEditor;
    private ColorPropertyEditor downColorEditor;
    private ColorPropertyEditor wickColorEditor;
    private EnumPropertyEditor<OhlcStyle> styleEditor;
    private DoublePropertyEditor barWidthEditor;
    private DoublePropertyEditor wickWidthEditor;

    // For LineSeriesOptions
    private ColorPropertyEditor lineColorEditor;
    private DoublePropertyEditor lineWidthEditor;
    private EnumPropertyEditor<DisplayMode> displayModeEditor;

    // For BandSeriesOptions
    private ColorPropertyEditor upperColorEditor;
    private ColorPropertyEditor middleColorEditor;
    private ColorPropertyEditor lowerColorEditor;
    private ColorPropertyEditor fillColorEditor;
    private DoublePropertyEditor bandLineWidthEditor;

    // For ScatterSeriesOptions
    private ColorPropertyEditor scatterColorEditor;
    private DoublePropertyEditor markerSizeEditor;

    // Original values for cancel restoration - OHLC
    private Color originalUpColor;
    private Color originalDownColor;
    private Color originalWickColor;
    private OhlcStyle originalStyle;
    private float originalBarWidth;
    private float originalWickWidth;

    // Original values - Line
    private Color originalLineColor;
    private float originalLineWidth;
    private DisplayMode originalDisplayMode;

    // Original values - Band
    private Color originalUpperColor;
    private Color originalMiddleColor;
    private Color originalLowerColor;
    private Color originalFillColor;
    private float originalBandLineWidth;

    // Original values - Scatter
    private Color originalScatterColor;
    private float originalMarkerSize;

    /**
     * Creates a series property dialog.
     *
     * @param owner   the owner frame
     * @param options the series options to edit
     */
    public SeriesPropertyDialog(Frame owner, SeriesOptions options) {
        super(owner, getDialogTitle(options));
        this.options = options;
        storeOriginalValues();
        buildContent();
    }

    private static String getDialogTitle(SeriesOptions options) {
        if (options instanceof OhlcSeriesOptions) {
            return "Candlestick Settings";
        } else if (options instanceof LineSeriesOptions) {
            return "Line Series Settings";
        } else if (options instanceof BandSeriesOptions) {
            return "Band Series Settings";
        } else if (options instanceof ScatterSeriesOptions) {
            return "Scatter Series Settings";
        } else if (options instanceof HistogramSeriesOptions) {
            return "Histogram Settings";
        }
        return "Series Settings";
    }

    private void storeOriginalValues() {
        if (options instanceof OhlcSeriesOptions ohlc) {
            originalUpColor = ohlc.getUpColor();
            originalDownColor = ohlc.getDownColor();
            originalWickColor = ohlc.getWickColor();
            originalStyle = ohlc.getStyle();
            originalBarWidth = ohlc.getBarWidthRatio();
            originalWickWidth = ohlc.getWickWidth();
        } else if (options instanceof LineSeriesOptions line) {
            originalLineColor = line.getColor();
            originalLineWidth = line.getLineWidth();
            originalDisplayMode = line.getDisplayMode();
        } else if (options instanceof BandSeriesOptions band) {
            originalUpperColor = band.getUpperColor();
            originalMiddleColor = band.getMiddleColor();
            originalLowerColor = band.getLowerColor();
            originalFillColor = band.getFillColor();
            originalBandLineWidth = band.getLineWidth();
        } else if (options instanceof ScatterSeriesOptions scatter) {
            originalScatterColor = scatter.getColor();
            originalMarkerSize = scatter.getMarkerSize();
        }
    }

    private void buildContent() {
        JPanel content = getContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        if (options instanceof OhlcSeriesOptions ohlc) {
            buildOhlcContent(content, ohlc);
        } else if (options instanceof LineSeriesOptions line) {
            buildLineContent(content, line);
        } else if (options instanceof BandSeriesOptions band) {
            buildBandContent(content, band);
        } else if (options instanceof ScatterSeriesOptions scatter) {
            buildScatterContent(content, scatter);
        } else if (options instanceof HistogramSeriesOptions histogram) {
            buildHistogramContent(content, histogram);
        } else {
            JLabel label = new JLabel("No configurable properties for this series type.");
            label.setFont(LABEL_FONT);
            label.setForeground(LABEL_COLOR);
            content.add(label);
        }
    }

    private void buildOhlcContent(JPanel content, OhlcSeriesOptions ohlc) {
        // Style
        styleEditor = new EnumPropertyEditor<>(OhlcStyle.class, ohlc.getStyle());
        styleEditor.setOnValueChanged(value -> ohlc.style(value));
        addPropertyRow(content, "Style", styleEditor.getComponent());

        // Colors section
        content.add(createSectionHeader("Colors"));

        // Up Color
        upColorEditor = new ColorPropertyEditor(ohlc.getUpColor());
        upColorEditor.setOnValueChanged(value -> ohlc.upColor(value));
        addPropertyRow(content, "Up Color", upColorEditor.getComponent());

        // Down Color
        downColorEditor = new ColorPropertyEditor(ohlc.getDownColor());
        downColorEditor.setOnValueChanged(value -> ohlc.downColor(value));
        addPropertyRow(content, "Down Color", downColorEditor.getComponent());

        // Wick Color
        wickColorEditor = new ColorPropertyEditor(
                ohlc.getWickColor() != null ? ohlc.getWickColor() : ohlc.getUpColor());
        wickColorEditor.setOnValueChanged(value -> ohlc.wickColor(value));
        addPropertyRow(content, "Wick Color", wickColorEditor.getComponent());

        // Sizing section
        content.add(createSectionHeader("Sizing"));

        // Bar Width
        barWidthEditor = new DoublePropertyEditor(0.1, 1.0, ohlc.getBarWidthRatio(), 0.1);
        barWidthEditor.setOnValueChanged(value -> ohlc.barWidthRatio(value.floatValue()));
        addPropertyRow(content, "Bar Width", barWidthEditor.getComponent());

        // Wick Width
        wickWidthEditor = new DoublePropertyEditor(0.5, 5.0, ohlc.getWickWidth(), 0.5);
        wickWidthEditor.setOnValueChanged(value -> ohlc.wickWidth(value.floatValue()));
        addPropertyRow(content, "Wick Width", wickWidthEditor.getComponent());
    }

    private void buildLineContent(JPanel content, LineSeriesOptions line) {
        // Display Mode
        displayModeEditor = new EnumPropertyEditor<>(DisplayMode.class, line.getDisplayMode());
        displayModeEditor.setOnValueChanged(value -> line.displayMode(value));
        addPropertyRow(content, "Display Mode", displayModeEditor.getComponent());

        // Colors section
        content.add(createSectionHeader("Appearance"));

        // Color
        lineColorEditor = new ColorPropertyEditor(line.getColor());
        lineColorEditor.setOnValueChanged(value -> line.color(value));
        addPropertyRow(content, "Color", lineColorEditor.getComponent());

        // Line Width
        lineWidthEditor = new DoublePropertyEditor(0.5, 5.0, line.getLineWidth(), 0.5);
        lineWidthEditor.setOnValueChanged(value -> line.lineWidth(value.floatValue()));
        addPropertyRow(content, "Line Width", lineWidthEditor.getComponent());
    }

    private void buildBandContent(JPanel content, BandSeriesOptions band) {
        // Colors section
        content.add(createSectionHeader("Colors"));

        // Upper Color
        upperColorEditor = new ColorPropertyEditor(band.getUpperColor());
        upperColorEditor.setOnValueChanged(value -> band.upperColor(value));
        addPropertyRow(content, "Upper Color", upperColorEditor.getComponent());

        // Middle Color
        middleColorEditor = new ColorPropertyEditor(band.getMiddleColor());
        middleColorEditor.setOnValueChanged(value -> band.middleColor(value));
        addPropertyRow(content, "Middle Color", middleColorEditor.getComponent());

        // Lower Color
        lowerColorEditor = new ColorPropertyEditor(band.getLowerColor());
        lowerColorEditor.setOnValueChanged(value -> band.lowerColor(value));
        addPropertyRow(content, "Lower Color", lowerColorEditor.getComponent());

        // Fill Color
        fillColorEditor = new ColorPropertyEditor(band.getFillColor());
        fillColorEditor.setOnValueChanged(value -> band.fillColor(value));
        addPropertyRow(content, "Fill Color", fillColorEditor.getComponent());

        // Sizing section
        content.add(createSectionHeader("Sizing"));

        // Line Width
        bandLineWidthEditor = new DoublePropertyEditor(0.5, 5.0, band.getLineWidth(), 0.5);
        bandLineWidthEditor.setOnValueChanged(value -> band.lineWidth(value.floatValue()));
        addPropertyRow(content, "Line Width", bandLineWidthEditor.getComponent());
    }

    private void buildScatterContent(JPanel content, ScatterSeriesOptions scatter) {
        // Appearance section
        content.add(createSectionHeader("Appearance"));

        // Color
        scatterColorEditor = new ColorPropertyEditor(scatter.getColor());
        scatterColorEditor.setOnValueChanged(value -> scatter.color(value));
        addPropertyRow(content, "Color", scatterColorEditor.getComponent());

        // Marker Size
        markerSizeEditor = new DoublePropertyEditor(2.0, 20.0, scatter.getMarkerSize(), 1.0);
        markerSizeEditor.setOnValueChanged(value -> scatter.markerSize(value.floatValue()));
        addPropertyRow(content, "Marker Size", markerSizeEditor.getComponent());
    }

    private void buildHistogramContent(JPanel content, HistogramSeriesOptions histogram) {
        // Colors section
        content.add(createSectionHeader("Colors"));

        // Positive Color
        ColorPropertyEditor posColorEditor = new ColorPropertyEditor(histogram.getPositiveColor());
        posColorEditor.setOnValueChanged(value -> histogram.positiveColor(value));
        addPropertyRow(content, "Positive Color", posColorEditor.getComponent());

        // Negative Color
        ColorPropertyEditor negColorEditor = new ColorPropertyEditor(histogram.getNegativeColor());
        negColorEditor.setOnValueChanged(value -> histogram.negativeColor(value));
        addPropertyRow(content, "Negative Color", negColorEditor.getComponent());

        // Sizing section
        content.add(createSectionHeader("Sizing"));

        // Bar Width
        DoublePropertyEditor histBarWidthEditor = new DoublePropertyEditor(0.1, 1.0, histogram.getBarWidthRatio(), 0.1);
        histBarWidthEditor.setOnValueChanged(value -> histogram.barWidthRatio(value.floatValue()));
        addPropertyRow(content, "Bar Width", histBarWidthEditor.getComponent());
    }

    private void addPropertyRow(JPanel content, String label, JComponent component) {
        JPanel row = createPropertyRow(label, component);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        content.add(row);
    }

    @Override
    protected void onApply() {
        // Options are already modified - nothing special to do
        super.onApply();
    }

    @Override
    protected void onCancel() {
        // Restore original values
        if (options instanceof OhlcSeriesOptions ohlc) {
            ohlc.upColor(originalUpColor);
            ohlc.downColor(originalDownColor);
            ohlc.wickColor(originalWickColor);
            ohlc.style(originalStyle);
            ohlc.barWidthRatio(originalBarWidth);
            ohlc.wickWidth(originalWickWidth);
        } else if (options instanceof LineSeriesOptions line) {
            line.color(originalLineColor);
            line.lineWidth(originalLineWidth);
            line.displayMode(originalDisplayMode);
        } else if (options instanceof BandSeriesOptions band) {
            band.upperColor(originalUpperColor);
            band.middleColor(originalMiddleColor);
            band.lowerColor(originalLowerColor);
            band.fillColor(originalFillColor);
            band.lineWidth(originalBandLineWidth);
        } else if (options instanceof ScatterSeriesOptions scatter) {
            scatter.color(originalScatterColor);
            scatter.markerSize(originalMarkerSize);
        }
        super.onCancel();
    }

    @Override
    protected void onReset() {
        // Reset to TradingView-like defaults
        if (options instanceof OhlcSeriesOptions) {
            OhlcSeriesOptions ohlc = (OhlcSeriesOptions) options;

            Color defaultUpColor = new Color(38, 166, 91);
            Color defaultDownColor = new Color(214, 69, 65);
            OhlcStyle defaultStyle = OhlcStyle.CANDLESTICK;
            float defaultBarWidth = 0.8f;
            float defaultWickWidth = 1.0f;

            ohlc.upColor(defaultUpColor);
            ohlc.downColor(defaultDownColor);
            ohlc.wickColor(null);
            ohlc.style(defaultStyle);
            ohlc.barWidthRatio(defaultBarWidth);
            ohlc.wickWidth(defaultWickWidth);

            // Update editors
            upColorEditor.setValue(defaultUpColor);
            downColorEditor.setValue(defaultDownColor);
            wickColorEditor.setValue(defaultUpColor);  // Follows up color when null
            styleEditor.setValue(defaultStyle);
            barWidthEditor.setValue((double) defaultBarWidth);
            wickWidthEditor.setValue((double) defaultWickWidth);
        }
    }

    /**
     * Returns the series options being edited.
     */
    public SeriesOptions getSeriesOptions() {
        return options;
    }
}
