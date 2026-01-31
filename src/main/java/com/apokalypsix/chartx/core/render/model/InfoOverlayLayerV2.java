package com.apokalypsix.chartx.core.render.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.util.ShapeUtils;
import com.apokalypsix.chartx.core.ui.overlay.InfoOverlay;
import com.apokalypsix.chartx.core.ui.overlay.InfoOverlayConfig;
import com.apokalypsix.chartx.core.ui.overlay.OverlayPosition;
import com.apokalypsix.chartx.core.ui.overlay.PriceBarOverlay;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;
import com.apokalypsix.chartx.core.render.api.TextRenderer;
import com.jogamp.opengl.GL2ES2;

/**
 * Render layer for info overlays using GPU text rendering.
 *
 * <p>Renders collapsible info panels (SymbolInfo, OHLC, Indicator) directly on the GPU
 * using the TextRenderer API, replacing Java2D rendering in TextOverlay.
 *
 * <p>Mouse interaction (click to collapse/expand) is still handled by TextOverlay.
 */
public class InfoOverlayLayerV2 extends AbstractRenderLayer {

    private static final Logger log = LoggerFactory.getLogger(InfoOverlayLayerV2.class);

    /** Z-order for info overlays (after legend, before tooltip) */
    public static final int Z_ORDER = 930;

    // Max overlays and rows
    private static final int MAX_OVERLAYS = 10;
    private static final int MAX_ROWS_PER_OVERLAY = 12;

    // Settings icon appearance
    private static final Color SETTINGS_ICON_COLOR = new Color(150, 152, 156);
    private static final int SETTINGS_ICON_SIZE = 12;

    // V2 API resources
    private Buffer backgroundBuffer;
    private Buffer expandButtonBuffer;
    private Buffer settingsIconBuffer;
    private Shader colorShader;
    private boolean v2Initialized = false;

    // Vertex data for backgrounds (rounded rects)
    private final float[] bgVertices = new float[MAX_OVERLAYS * ShapeUtils.roundedRectFloatCount(4)];
    // Vertex data for expand buttons (triangles)
    private final float[] btnVertices = new float[MAX_OVERLAYS * ShapeUtils.triangleFloatCount()];
    // Vertex data for settings icons (gear shape)
    // Each gear has 8 teeth * 3 triangles * 3 vertices * 6 floats = 432 floats per gear
    private final float[] settingsVertices = new float[MAX_OVERLAYS * MAX_ROWS_PER_OVERLAY * 432];

    // Reference to overlays (shared with TextOverlay)
    private List<InfoOverlay> overlays;

    public InfoOverlayLayerV2() {
        super(Z_ORDER);
    }

    @Override
    protected void doInitialize(GL2ES2 gl, GLResourceManager resources) {
        log.debug("InfoOverlayLayerV2 GL initialized");
    }

    private void initializeV2(RenderContext ctx) {
        ResourceManager resources = ctx.getResourceManager();

        backgroundBuffer = resources.getOrCreateBuffer("infooverlay.background",
                BufferDescriptor.positionColor2D(bgVertices.length));

        expandButtonBuffer = resources.getOrCreateBuffer("infooverlay.expandbtn",
                BufferDescriptor.positionColor2D(btnVertices.length));

        settingsIconBuffer = resources.getOrCreateBuffer("infooverlay.settings",
                BufferDescriptor.positionColor2D(settingsVertices.length));

        colorShader = resources.getShader(ResourceManager.SHADER_DEFAULT);

        v2Initialized = true;
        log.debug("InfoOverlayLayerV2 V2 resources initialized");
    }

    @Override
    public void render(RenderContext ctx) {
        if (!ctx.hasAbstractedAPI()) {
            return;
        }

        if (overlays == null || overlays.isEmpty()) {
            return;
        }

        // Filter visible overlays
        List<InfoOverlay> visibleOverlays = new ArrayList<>();
        for (InfoOverlay overlay : overlays) {
            if (overlay.getConfig().isVisible()) {
                visibleOverlays.add(overlay);
            }
        }

        if (visibleOverlays.isEmpty()) {
            return;
        }

        if (!v2Initialized) {
            initializeV2(ctx);
        }

        ResourceManager resources = ctx.getResourceManager();
        TextRenderer textRenderer = resources.getTextRenderer();
        if (textRenderer == null) {
            return;
        }

        Viewport viewport = ctx.getViewport();
        int width = viewport.getWidth();
        int height = viewport.getHeight();

        // Set scale factor for HiDPI displays
        textRenderer.setScaleFactor(ctx.getScaleFactor());

        // Group overlays by position and calculate stack offsets
        Map<OverlayPosition, List<InfoOverlay>> overlaysByPosition = new EnumMap<>(OverlayPosition.class);
        for (InfoOverlay overlay : visibleOverlays) {
            OverlayPosition pos = overlay.getConfig().getPosition();
            overlaysByPosition.computeIfAbsent(pos, k -> new ArrayList<>()).add(overlay);
        }

        // Update content and calculate layout for each overlay
        float scaleFactor = ctx.getScaleFactor();
        List<OverlayLayout> layouts = new ArrayList<>();
        for (Map.Entry<OverlayPosition, List<InfoOverlay>> entry : overlaysByPosition.entrySet()) {
            List<InfoOverlay> group = entry.getValue();
            int stackOffset = 0;

            for (InfoOverlay overlay : group) {
                overlay.updateContent();
                overlay.setStackOffset(stackOffset);

                OverlayLayout layout = calculateLayout(overlay, textRenderer, width, height, scaleFactor);
                layouts.add(layout);

                // Scale stack spacing for HiDPI
                stackOffset += layout.height + (int)(overlay.getConfig().getStackSpacing() * scaleFactor);
            }
        }

        // Pass 1: Draw all backgrounds
        drawBackgrounds(ctx, layouts);

        // Pass 2: Draw expand buttons
        drawExpandButtons(ctx, layouts);

        // Pass 3: Draw settings icons
        drawSettingsIcons(ctx, layouts);

        // Pass 4: Draw text
        drawText(ctx, textRenderer, layouts, width, height);
    }

    /**
     * Calculates layout for a single overlay.
     */
    private OverlayLayout calculateLayout(InfoOverlay overlay, TextRenderer textRenderer, int viewWidth, int viewHeight, float scaleFactor) {
        InfoOverlayConfig config = overlay.getConfig();

        // Check if this is a horizontal PriceBarOverlay
        boolean isHorizontalLayout = (overlay instanceof PriceBarOverlay) && ((PriceBarOverlay) overlay).isHorizontalLayout();

        // Handle PriceBarOverlay horizontal layout separately
        if (isHorizontalLayout) {
            return calculatePriceBarLayout((PriceBarOverlay) overlay, textRenderer, viewWidth, viewHeight, scaleFactor);
        }

        // Scale config values for HiDPI
        int padding = (int) (config.getPadding() * scaleFactor);
        int rowSpacing = (int) (config.getRowSpacing() * scaleFactor);
        int columnSpacing = (int) (config.getColumnSpacing() * scaleFactor);
        int marginX = (int) (config.getMarginX() * scaleFactor);
        int marginY = (int) (config.getMarginY() * scaleFactor);
        int minWidth = (int) (config.getMinWidth() * scaleFactor);

        // Use actual rendered text heights for layout
        textRenderer.setFontSize(config.getHeaderFont().getSize());
        float headerFontSize = textRenderer.getTextHeight();
        textRenderer.setFontSize(config.getLabelFont().getSize());
        float labelFontSize = textRenderer.getTextHeight();
        textRenderer.setFontSize(config.getValueFont().getSize());
        float valueFontSize = textRenderer.getTextHeight();
        float rowFontSize = Math.max(labelFontSize, valueFontSize);

        // Get rows via reflection or accessor
        List<InfoOverlay.OverlayRow> rows = getOverlayRows(overlay);

        // Check for bottom collapse icon mode
        boolean bottomCollapseIcon = config.getCollapseIconPosition() == InfoOverlayConfig.CollapseIconPosition.BOTTOM_ROW;
        int collapseRowHeight = 0;
        if (bottomCollapseIcon && config.isCollapsible() && !overlay.isCollapsed()) {
            collapseRowHeight = (int) Math.ceil(rowFontSize + rowSpacing);
        }

        // Header height: font height + padding below header
        // Use Math.ceil to avoid losing pixels due to rounding
        int headerHeight = (int) Math.ceil(headerFontSize + padding);

        // Check if header should be hidden (bottom collapse icon mode when expanded with empty title)
        String title = overlay.getTitle();
        boolean hideHeader = bottomCollapseIcon && !overlay.isCollapsed() && (title == null || title.isEmpty());
        int effectiveHeaderHeight = hideHeader ? 0 : headerHeight;

        // Calculate content dimensions
        int contentWidth = 0;
        int contentHeight = 0;

        boolean hasAnySettings = false;
        if (!overlay.isCollapsed() && !rows.isEmpty()) {
            textRenderer.setFontSize(config.getLabelFont().getSize());
            int maxLabelWidth = 0;
            for (InfoOverlay.OverlayRow row : rows) {
                maxLabelWidth = Math.max(maxLabelWidth, (int) Math.ceil(textRenderer.getTextWidth(row.label)));
                if (row.hasSettings) {
                    hasAnySettings = true;
                }
            }

            textRenderer.setFontSize(config.getValueFont().getSize());
            int maxValueWidth = 0;
            for (InfoOverlay.OverlayRow row : rows) {
                maxValueWidth = Math.max(maxValueWidth, (int) Math.ceil(textRenderer.getTextWidth(row.value)));
            }

            contentWidth = maxLabelWidth + columnSpacing + maxValueWidth;
            // Add space for settings icon if any row has settings
            if (hasAnySettings) {
                contentWidth += (int)((SETTINGS_ICON_SIZE + 6) * scaleFactor);
            }
            // Content height: all rows with spacing between them
            // Add padding/2 for space between header and first row
            contentHeight = (int) Math.ceil(rows.size() * (rowFontSize + rowSpacing) - rowSpacing + padding / 2.0);
            // Add collapse row height if needed
            contentHeight += collapseRowHeight;
        }

        // Minimum width includes title
        textRenderer.setFontSize(config.getHeaderFont().getSize());
        int titleTextWidth = (int) Math.ceil(textRenderer.getTextWidth(title != null ? title : ""));
        int titleWidth = titleTextWidth;
        if (config.isCollapsible() && config.isShowExpandButton() && !bottomCollapseIcon) {
            titleWidth += (int) (20 * scaleFactor); // Space for expand button in header
        }
        // Add space for expand button in bottom collapse mode when collapsed
        if (bottomCollapseIcon && config.isCollapsible() && overlay.isCollapsed()) {
            titleWidth += (int) (24 * scaleFactor); // Space for expand button after title
        }

        int overlayWidth = Math.max(minWidth, Math.max(titleWidth, contentWidth) + padding * 2);
        // Total height: header + content + bottom padding
        int overlayHeight = effectiveHeaderHeight + (overlay.isCollapsed() ? 0 : contentHeight + padding);

        // Calculate position using scaled margins
        // Note: stackOffset is already in physical pixels from caller
        int x, y;
        OverlayPosition pos = config.getPosition();

        switch (pos) {
            case TOP_RIGHT:
                x = viewWidth - overlayWidth - marginX;
                y = marginY + overlay.getStackOffset();
                break;
            case BOTTOM_LEFT:
                x = marginX;
                y = viewHeight - overlayHeight - marginY - overlay.getStackOffset();
                break;
            case BOTTOM_RIGHT:
                x = viewWidth - overlayWidth - marginX;
                y = viewHeight - overlayHeight - marginY - overlay.getStackOffset();
                break;
            case TOP_LEFT:
            default:
                x = marginX;
                y = marginY + overlay.getStackOffset();
                break;
        }

        // Update overlay bounds for hit testing (in logical coordinates for mouse events)
        // Convert from physical pixels to logical by dividing by scaleFactor
        int logicalX = (int)(x / scaleFactor);
        int logicalY = (int)(y / scaleFactor);
        int logicalWidth = (int)(overlayWidth / scaleFactor);
        int logicalHeight = (int)(overlayHeight / scaleFactor);
        int logicalHeaderHeight = (int)(effectiveHeaderHeight / scaleFactor);
        overlay.getBounds().setBounds(logicalX, logicalY, logicalWidth, logicalHeight);
        overlay.getHeaderBounds().setBounds(logicalX, logicalY, logicalWidth, logicalHeaderHeight);

        OverlayLayout layout = new OverlayLayout();
        layout.overlay = overlay;
        layout.x = x;
        layout.y = y;
        layout.width = overlayWidth;
        layout.height = overlayHeight;
        layout.headerHeight = effectiveHeaderHeight;
        layout.padding = padding;
        layout.rowSpacing = rowSpacing;
        layout.scaleFactor = scaleFactor;
        layout.rows = rows;
        layout.isHorizontalLayout = false;
        layout.hideHeader = hideHeader;
        layout.bottomCollapseIcon = bottomCollapseIcon;
        layout.collapseRowHeight = collapseRowHeight;
        layout.titleWidth = titleTextWidth;
        return layout;
    }

    /**
     * Calculates layout for a PriceBarOverlay in horizontal mode.
     */
    private OverlayLayout calculatePriceBarLayout(PriceBarOverlay overlay, TextRenderer textRenderer, int viewWidth, int viewHeight, float scaleFactor) {
        InfoOverlayConfig config = overlay.getConfig();

        int padding = (int) (config.getPadding() * scaleFactor);
        int marginX = (int) (config.getMarginX() * scaleFactor);
        int marginY = (int) (config.getMarginY() * scaleFactor);

        // Use header font for the single-line display
        textRenderer.setFontSize(config.getHeaderFont().getSize());
        float lineHeight = textRenderer.getTextHeight();

        // Calculate total width needed for the line
        // Format: EXCHANGE:SYMBOL  O:xxx  H:xxx  L:xxx  C:xxx  V:xxx
        int totalWidth = 0;

        // Symbol text
        String symbolText = buildSymbolText(overlay);
        if (!symbolText.isEmpty()) {
            totalWidth += (int) Math.ceil(textRenderer.getTextWidth(symbolText)) + padding * 2;
        }

        // OHLCV labels and values
        textRenderer.setFontSize(config.getLabelFont().getSize());
        float labelWidth = textRenderer.getTextWidth("O:");
        textRenderer.setFontSize(config.getValueFont().getSize());
        float valueWidth = textRenderer.getTextWidth("00,000.00"); // Approximate max value width

        if (overlay.getData() != null && overlay.getData().size() > 0) {
            // 5 label:value pairs (O, H, L, C, V)
            totalWidth += (int) Math.ceil(5 * (labelWidth + valueWidth + padding));
        } else {
            // Placeholder text
            textRenderer.setFontSize(config.getLabelFont().getSize());
            totalWidth += (int) Math.ceil(textRenderer.getTextWidth("O:—  H:—  L:—  C:—  V:—"));
        }

        int overlayWidth = totalWidth + padding;
        int overlayHeight = (int) Math.ceil(lineHeight) + padding;

        // Calculate position
        int x, y;
        OverlayPosition pos = config.getPosition();

        switch (pos) {
            case TOP_RIGHT:
                x = viewWidth - overlayWidth - marginX;
                y = marginY + overlay.getStackOffset();
                break;
            case BOTTOM_LEFT:
                x = marginX;
                y = viewHeight - overlayHeight - marginY - overlay.getStackOffset();
                break;
            case BOTTOM_RIGHT:
                x = viewWidth - overlayWidth - marginX;
                y = viewHeight - overlayHeight - marginY - overlay.getStackOffset();
                break;
            case TOP_LEFT:
            default:
                x = marginX;
                y = marginY + overlay.getStackOffset();
                break;
        }

        // Update bounds for hit testing
        int logicalX = (int)(x / scaleFactor);
        int logicalY = (int)(y / scaleFactor);
        int logicalWidth = (int)(overlayWidth / scaleFactor);
        int logicalHeight = (int)(overlayHeight / scaleFactor);
        overlay.getBounds().setBounds(logicalX, logicalY, logicalWidth, logicalHeight);
        overlay.getHeaderBounds().setBounds(logicalX, logicalY, logicalWidth, logicalHeight);

        OverlayLayout layout = new OverlayLayout();
        layout.overlay = overlay;
        layout.x = x;
        layout.y = y;
        layout.width = overlayWidth;
        layout.height = overlayHeight;
        layout.headerHeight = overlayHeight;
        layout.padding = padding;
        layout.rowSpacing = 0;
        layout.scaleFactor = scaleFactor;
        layout.rows = Collections.emptyList();
        layout.isHorizontalLayout = true;
        layout.hideHeader = false;
        layout.bottomCollapseIcon = false;
        layout.collapseRowHeight = 0;
        return layout;
    }

    /**
     * Builds the symbol text for a PriceBarOverlay.
     */
    private String buildSymbolText(PriceBarOverlay overlay) {
        String exchange = overlay.getExchange();
        String symbol = overlay.getSymbol();
        if ((exchange == null || exchange.isEmpty()) && (symbol == null || symbol.isEmpty())) {
            return "";
        }
        if (exchange == null || exchange.isEmpty()) {
            return symbol;
        }
        if (symbol == null || symbol.isEmpty()) {
            return exchange;
        }
        return exchange + ":" + symbol;
    }

    /**
     * Gets overlay rows via public accessor.
     */
    private List<InfoOverlay.OverlayRow> getOverlayRows(InfoOverlay overlay) {
        return overlay.getRows();
    }

    private void drawBackgrounds(RenderContext ctx, List<OverlayLayout> layouts) {
        if (colorShader == null || !colorShader.isValid()) {
            return;
        }

        int bgIdx = 0;

        for (OverlayLayout layout : layouts) {
            InfoOverlayConfig config = layout.overlay.getConfig();
            if (!config.isShowBackground()) {
                continue;
            }

            Color bg = config.getBackgroundColor();
            float r = bg.getRed() / 255f;
            float g = bg.getGreen() / 255f;
            float b = bg.getBlue() / 255f;
            float a = bg.getAlpha() / 255f;

            // Scale corner radius for HiDPI
            int cornerRadius = (int)(config.getCornerRadius() * layout.scaleFactor);
            if (bgIdx + ShapeUtils.roundedRectFloatCount(cornerRadius) <= bgVertices.length) {
                bgIdx += ShapeUtils.tessellateRoundedRect(bgVertices, bgIdx,
                        layout.x, layout.y, layout.width, layout.height,
                        cornerRadius, r, g, b, a);
            }
        }

        if (bgIdx > 0) {
            colorShader.bind();
            colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            backgroundBuffer.upload(bgVertices, 0, bgIdx);
            backgroundBuffer.draw(DrawMode.TRIANGLES);

            colorShader.unbind();
        }
    }

    private void drawExpandButtons(RenderContext ctx, List<OverlayLayout> layouts) {
        if (colorShader == null || !colorShader.isValid()) {
            return;
        }

        int btnIdx = 0;

        for (OverlayLayout layout : layouts) {
            // Skip horizontal layouts (PriceBarOverlay)
            if (layout.isHorizontalLayout) {
                continue;
            }

            InfoOverlayConfig config = layout.overlay.getConfig();
            if (!config.isCollapsible() || !config.isShowExpandButton()) {
                continue;
            }

            Color btnColor = config.getExpandButtonColor();
            float r = btnColor.getRed() / 255f;
            float g = btnColor.getGreen() / 255f;
            float b = btnColor.getBlue() / 255f;
            float a = btnColor.getAlpha() / 255f;

            // Use scaled values from layout
            int padding = layout.padding;
            float headerFontSize = config.getHeaderFont().getSize() * layout.scaleFactor;

            float btnX, btnY;
            float x1, y1, x2, y2, x3, y3;
            int size;

            if (layout.bottomCollapseIcon && !layout.overlay.isCollapsed()) {
                // Bottom-row collapse icon: left-aligned, at bottom of content
                size = (int)(10 * layout.scaleFactor);  // Bigger size for bottom mode
                btnX = layout.x + padding + 6 * layout.scaleFactor;
                // Position at the collapse row (after all content rows)
                float contentHeight = layout.height - layout.headerHeight - padding;
                btnY = layout.y + layout.headerHeight + contentHeight - layout.collapseRowHeight / 2f;

                // Update collapse icon bounds for hit testing (in logical coordinates)
                int logicalBtnX = (int)(btnX / layout.scaleFactor);
                int logicalBtnY = (int)(btnY / layout.scaleFactor);
                layout.overlay.getCollapseIconBounds().setBounds(logicalBtnX - 12, logicalBtnY - 12, 24, 24);

                // Up-pointing triangle (collapse)
                x1 = btnX - size / 2f;
                y1 = btnY + size / 2f;
                x2 = btnX + size / 2f;
                y2 = btnY + size / 2f;
                x3 = btnX;
                y3 = btnY - size / 2f;
            } else if (layout.bottomCollapseIcon && layout.overlay.isCollapsed()) {
                // Collapsed bottom mode: to the right of title, in header row, with bigger size
                size = (int)(10 * layout.scaleFactor);
                btnX = layout.x + padding + layout.titleWidth + 12 * layout.scaleFactor; // Right of title
                btnY = layout.y + padding / 2f + headerFontSize / 2;

                // Update collapse icon bounds for hit testing (in logical coordinates)
                int logicalBtnX = (int)(btnX / layout.scaleFactor);
                int logicalBtnY = (int)(btnY / layout.scaleFactor);
                layout.overlay.getCollapseIconBounds().setBounds(logicalBtnX - 12, logicalBtnY - 12, 24, 24);

                // Down-pointing triangle for expand
                x1 = btnX - size / 2f;
                y1 = btnY - size / 2f;
                x2 = btnX + size / 2f;
                y2 = btnY - size / 2f;
                x3 = btnX;
                y3 = btnY + size / 2f;
            } else {
                // Header position (right side of header) - standard header mode
                size = (int)(6 * layout.scaleFactor);
                float btnOffset = 8 * layout.scaleFactor;
                btnX = layout.x + layout.width - padding - btnOffset;
                btnY = layout.y + padding / 2f + headerFontSize / 2;

                if (layout.overlay.isCollapsed()) {
                    // Right-pointing triangle (collapsed, header mode)
                    x1 = btnX;
                    y1 = btnY - size / 2f;
                    x2 = btnX;
                    y2 = btnY + size / 2f;
                    x3 = btnX + size;
                    y3 = btnY;
                } else {
                    // Down-pointing triangle
                    x1 = btnX - size / 2f;
                    y1 = btnY - size / 2f;
                    x2 = btnX + size / 2f;
                    y2 = btnY - size / 2f;
                    x3 = btnX;
                    y3 = btnY + size / 2f;
                }
            }

            if (btnIdx + ShapeUtils.triangleFloatCount() <= btnVertices.length) {
                btnIdx += ShapeUtils.tessellateTriangle(btnVertices, btnIdx,
                        x1, y1, x2, y2, x3, y3, r, g, b, a);
            }
        }

        if (btnIdx > 0) {
            colorShader.bind();
            colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            expandButtonBuffer.upload(btnVertices, 0, btnIdx);
            expandButtonBuffer.draw(DrawMode.TRIANGLES);

            colorShader.unbind();
        }
    }

    private void drawSettingsIcons(RenderContext ctx, List<OverlayLayout> layouts) {
        if (colorShader == null || !colorShader.isValid()) {
            return;
        }

        int idx = 0;
        float r = SETTINGS_ICON_COLOR.getRed() / 255f;
        float g = SETTINGS_ICON_COLOR.getGreen() / 255f;
        float b = SETTINGS_ICON_COLOR.getBlue() / 255f;
        float a = 1.0f;

        for (OverlayLayout layout : layouts) {
            if (layout.overlay.isCollapsed() || layout.rows.isEmpty()) {
                continue;
            }

            InfoOverlayConfig config = layout.overlay.getConfig();
            int padding = layout.padding;
            float scaleFactor = layout.scaleFactor;

            // Calculate row font size for positioning
            float labelFontSize = config.getLabelFont().getSize() * scaleFactor;
            float valueFontSize = config.getValueFont().getSize() * scaleFactor;
            float rowFontSize = Math.max(labelFontSize, valueFontSize);

            float rowY = layout.y + layout.headerHeight + padding / 2f;
            int scaledIconSize = (int)(SETTINGS_ICON_SIZE * scaleFactor);

            for (InfoOverlay.OverlayRow row : layout.rows) {
                if (row.hasSettings) {
                    int iconX = layout.x + layout.width - padding - scaledIconSize;
                    int iconY = (int)(rowY + (rowFontSize - scaledIconSize) / 2);

                    // Update icon bounds for hit testing (in logical coordinates for mouse events)
                    // Convert from physical pixels to logical by dividing by scaleFactor
                    if (row.settingsIconBounds == null) {
                        row.settingsIconBounds = new Rectangle();
                    }
                    int logicalIconX = (int)((iconX - 2) / scaleFactor);
                    int logicalIconY = (int)((iconY - 2) / scaleFactor);
                    int logicalIconWidth = (int)((scaledIconSize + 4) / scaleFactor);
                    int logicalIconHeight = (int)((scaledIconSize + 4) / scaleFactor);
                    row.settingsIconBounds.setBounds(logicalIconX, logicalIconY, logicalIconWidth, logicalIconHeight);

                    // Tessellate gear icon
                    idx = tessellateGearIcon(settingsVertices, idx, iconX, iconY, scaledIconSize, r, g, b, a);
                }

                rowY += rowFontSize + layout.rowSpacing;
            }
        }

        if (idx > 0) {
            colorShader.bind();
            colorShader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

            settingsIconBuffer.upload(settingsVertices, 0, idx);
            settingsIconBuffer.draw(DrawMode.TRIANGLES);

            colorShader.unbind();
        }
    }

    /**
     * Tessellates a proper gear icon into triangles.
     * Returns the new index into the vertex array.
     */
    private int tessellateGearIcon(float[] vertices, int startIdx, int x, int y, int size, float r, float g, float b, float a) {
        int idx = startIdx;
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float outerR = size / 2f;
        float innerR = size * 0.35f;
        int teeth = 8;
        double toothWidth = Math.PI / teeth;

        // Build gear as triangles from center
        // For each tooth: 4 triangles (tooth top, two sides, valley)
        for (int i = 0; i < teeth; i++) {
            double angle = Math.PI * 2 * i / teeth - Math.PI / 2;

            // Tooth corners
            double a1 = angle - toothWidth * 0.35;
            double a2 = angle + toothWidth * 0.35;
            double a3 = angle + toothWidth * 0.65;
            double a4 = angle + Math.PI * 2 / teeth - toothWidth * 0.65;

            float x1 = cx + (float)(Math.cos(a1) * outerR);
            float y1 = cy + (float)(Math.sin(a1) * outerR);
            float x2 = cx + (float)(Math.cos(a2) * outerR);
            float y2 = cy + (float)(Math.sin(a2) * outerR);
            float x3 = cx + (float)(Math.cos(a3) * innerR);
            float y3 = cy + (float)(Math.sin(a3) * innerR);
            float x4 = cx + (float)(Math.cos(a4) * innerR);
            float y4 = cy + (float)(Math.sin(a4) * innerR);

            // Triangle 1: center to tooth outer edge
            idx = addTriangle(vertices, idx, cx, cy, x1, y1, x2, y2, r, g, b, a);
            // Triangle 2: tooth to inner corner
            idx = addTriangle(vertices, idx, cx, cy, x2, y2, x3, y3, r, g, b, a);
            // Triangle 3: valley
            idx = addTriangle(vertices, idx, cx, cy, x3, y3, x4, y4, r, g, b, a);
        }

        return idx;
    }

    /**
     * Adds a single triangle to the vertex array.
     */
    private int addTriangle(float[] vertices, int idx, float x1, float y1, float x2, float y2, float x3, float y3, float r, float g, float b, float a) {
        if (idx + 18 > vertices.length) return idx;

        vertices[idx++] = x1; vertices[idx++] = y1;
        vertices[idx++] = r; vertices[idx++] = g; vertices[idx++] = b; vertices[idx++] = a;

        vertices[idx++] = x2; vertices[idx++] = y2;
        vertices[idx++] = r; vertices[idx++] = g; vertices[idx++] = b; vertices[idx++] = a;

        vertices[idx++] = x3; vertices[idx++] = y3;
        vertices[idx++] = r; vertices[idx++] = g; vertices[idx++] = b; vertices[idx++] = a;

        return idx;
    }

    private void drawText(RenderContext ctx, TextRenderer textRenderer, List<OverlayLayout> layouts, int width, int height) {
        if (!textRenderer.beginBatch(width, height)) {
            return;
        }

        Color shadowColor = new Color(0, 0, 0, 180);

        try {
            for (OverlayLayout layout : layouts) {
                // Handle PriceBarOverlay horizontal layout separately
                if (layout.isHorizontalLayout && layout.overlay instanceof PriceBarOverlay) {
                    drawPriceBarText(textRenderer, layout, (PriceBarOverlay) layout.overlay, shadowColor);
                    continue;
                }

                InfoOverlayConfig config = layout.overlay.getConfig();
                boolean useShadow = config.isTextShadow();

                // Use scaled values from layout
                int padding = layout.padding;
                int rowSpacing = layout.rowSpacing;

                // Use actual rendered text heights for layout
                textRenderer.setFontSize(config.getHeaderFont().getSize());
                float headerFontSize = textRenderer.getTextHeight();
                textRenderer.setFontSize(config.getLabelFont().getSize());
                float labelFontSize = textRenderer.getTextHeight();
                textRenderer.setFontSize(config.getValueFont().getSize());
                float valueFontSize = textRenderer.getTextHeight();
                float rowFontSize = Math.max(labelFontSize, valueFontSize);

                // Draw header (if not hidden)
                if (!layout.hideHeader) {
                    textRenderer.setFontSize(config.getHeaderFont().getSize());
                    float textY = layout.y + padding / 2f + headerFontSize;
                    String title = layout.overlay.getTitle();
                    if (title != null && !title.isEmpty()) {
                        if (useShadow) {
                            textRenderer.drawText(title, layout.x + padding + 1, textY + 1, shadowColor);
                        }
                        textRenderer.drawText(title, layout.x + padding, textY, config.getHeaderColor());
                    }
                }

                // Draw rows if not collapsed
                if (!layout.overlay.isCollapsed() && !layout.rows.isEmpty()) {
                    float rowY = layout.y + layout.headerHeight + padding / 2f;

                    int scaledIconSize = (int)(SETTINGS_ICON_SIZE * layout.scaleFactor);
                    for (InfoOverlay.OverlayRow row : layout.rows) {
                        // Draw label (use config font size, labelFontSize is the rendered height)
                        textRenderer.setFontSize(config.getLabelFont().getSize());
                        if (useShadow) {
                            textRenderer.drawText(row.label, layout.x + padding + 1, rowY + labelFontSize + 1, shadowColor);
                        }
                        textRenderer.drawText(row.label, layout.x + padding, rowY + labelFontSize, config.getLabelColor());

                        // Draw value (right-aligned, offset if settings icon present)
                        textRenderer.setFontSize(config.getValueFont().getSize());
                        float valueWidth = textRenderer.getTextWidth(row.value);
                        float valueRightEdge = layout.x + layout.width - padding;
                        if (row.hasSettings) {
                            valueRightEdge -= scaledIconSize + 6;
                        }
                        float valueX = valueRightEdge - valueWidth;

                        Color valueColor;
                        if (row.colorType == InfoOverlay.ColorType.POSITIVE) {
                            valueColor = config.getPositiveColor();
                        } else if (row.colorType == InfoOverlay.ColorType.NEGATIVE) {
                            valueColor = config.getNegativeColor();
                        } else {
                            valueColor = config.getValueColor();
                        }

                        if (useShadow) {
                            textRenderer.drawText(row.value, valueX + 1, rowY + valueFontSize + 1, shadowColor);
                        }
                        textRenderer.drawText(row.value, valueX, rowY + valueFontSize, valueColor);

                        rowY += rowFontSize + rowSpacing;
                    }
                }
            }
        } finally {
            textRenderer.endBatch();
        }
    }

    /**
     * Draws text for a PriceBarOverlay in horizontal layout mode.
     */
    private void drawPriceBarText(TextRenderer textRenderer, OverlayLayout layout, PriceBarOverlay overlay, Color shadowColor) {
        InfoOverlayConfig config = overlay.getConfig();
        boolean useShadow = config.isTextShadow();
        int padding = layout.padding;

        textRenderer.setFontSize(config.getHeaderFont().getSize());
        float lineHeight = textRenderer.getTextHeight();
        float textY = layout.y + padding / 2f + lineHeight;
        float currentX = layout.x + padding;

        // Draw symbol (EXCHANGE:SYMBOL)
        String symbolText = buildSymbolText(overlay);
        if (!symbolText.isEmpty()) {
            if (useShadow) {
                textRenderer.drawText(symbolText, currentX + 1, textY + 1, shadowColor);
            }
            textRenderer.drawText(symbolText, currentX, textY, config.getHeaderColor());
            currentX += textRenderer.getTextWidth(symbolText) + padding * 2;
        }

        // Draw OHLCV values
        var data = overlay.getData();
        if (data != null && data.size() > 0) {
            int index = overlay.getDisplayIndex();
            if (index < 0 || index >= data.size()) {
                index = data.size() - 1;
            }

            float open = data.getOpen(index);
            float high = data.getHigh(index);
            float low = data.getLow(index);
            float close = data.getClose(index);
            float volume = data.getVolume(index);

            // Determine close color
            Color closeColor = close >= open ? config.getPositiveColor() : config.getNegativeColor();

            java.text.DecimalFormat priceFormat = new java.text.DecimalFormat("#,##0.00");

            // Draw O:xxx
            currentX = drawLabelValueGpu(textRenderer, "O:", priceFormat.format(open), config.getLabelColor(),
                    currentX, textY, config, useShadow, shadowColor, padding);

            // Draw H:xxx
            currentX = drawLabelValueGpu(textRenderer, "H:", priceFormat.format(high), config.getLabelColor(),
                    currentX, textY, config, useShadow, shadowColor, padding);

            // Draw L:xxx
            currentX = drawLabelValueGpu(textRenderer, "L:", priceFormat.format(low), config.getLabelColor(),
                    currentX, textY, config, useShadow, shadowColor, padding);

            // Draw C:xxx (colored)
            currentX = drawLabelValueGpu(textRenderer, "C:", priceFormat.format(close), closeColor,
                    currentX, textY, config, useShadow, shadowColor, padding);

            // Draw V:xxx
            drawLabelValueGpu(textRenderer, "V:", formatVolume(volume), config.getLabelColor(),
                    currentX, textY, config, useShadow, shadowColor, padding);
        } else {
            // No data - show placeholder
            textRenderer.setFontSize(config.getLabelFont().getSize());
            String placeholder = "O:—  H:—  L:—  C:—  V:—";
            if (useShadow) {
                textRenderer.drawText(placeholder, currentX + 1, textY + 1, shadowColor);
            }
            textRenderer.drawText(placeholder, currentX, textY, config.getLabelColor());
        }
    }

    /**
     * Draws a label:value pair for GPU rendering and returns the new X position.
     */
    private float drawLabelValueGpu(TextRenderer textRenderer, String label, String value, Color valueColor,
                                     float x, float y, InfoOverlayConfig config, boolean useShadow,
                                     Color shadowColor, int spacing) {
        // Draw label
        textRenderer.setFontSize(config.getLabelFont().getSize());
        if (useShadow) {
            textRenderer.drawText(label, x + 1, y + 1, shadowColor);
        }
        textRenderer.drawText(label, x, y, config.getLabelColor());
        x += textRenderer.getTextWidth(label);

        // Draw value
        textRenderer.setFontSize(config.getValueFont().getSize());
        if (useShadow) {
            textRenderer.drawText(value, x + 1, y + 1, shadowColor);
        }
        textRenderer.drawText(value, x, y, valueColor);
        x += textRenderer.getTextWidth(value) + spacing;

        return x;
    }

    /**
     * Formats volume with K/M/B suffixes.
     */
    private String formatVolume(float volume) {
        if (volume >= 1_000_000_000) {
            return String.format("%.2fB", volume / 1_000_000_000);
        } else if (volume >= 1_000_000) {
            return String.format("%.2fM", volume / 1_000_000);
        } else if (volume >= 1_000) {
            return String.format("%.2fK", volume / 1_000);
        } else {
            return String.format("%.0f", volume);
        }
    }

    @Override
    protected void doDispose(GL2ES2 gl) {
        v2Initialized = false;
    }

    public void disposeV2(RenderContext ctx) {
        if (v2Initialized && ctx.hasAbstractedAPI()) {
            ResourceManager resources = ctx.getResourceManager();
            if (resources != null) {
                resources.disposeBuffer("infooverlay.background");
                resources.disposeBuffer("infooverlay.expandbtn");
                resources.disposeBuffer("infooverlay.settings");
            }
            backgroundBuffer = null;
            expandButtonBuffer = null;
            settingsIconBuffer = null;
            colorShader = null;
            v2Initialized = false;
        }
    }

    // ========== Configuration ==========

    /**
     * Sets the list of info overlays to render.
     * This should be the same list used by TextOverlay for mouse interaction.
     */
    public void setOverlays(List<InfoOverlay> overlays) {
        this.overlays = overlays;
        markDirty();
    }

    public List<InfoOverlay> getOverlays() {
        return overlays;
    }

    /**
     * Internal layout data for a single overlay.
     */
    private static class OverlayLayout {
        InfoOverlay overlay;
        int x, y, width, height;
        int headerHeight;
        int padding;        // Scaled padding
        int rowSpacing;     // Scaled row spacing
        float scaleFactor;  // HiDPI scale factor
        List<InfoOverlay.OverlayRow> rows;

        // New fields for refactored overlays
        boolean isHorizontalLayout;    // PriceBarOverlay horizontal mode
        boolean hideHeader;            // Empty header (bottom collapse icon mode when expanded)
        boolean bottomCollapseIcon;    // Collapse icon at bottom
        int collapseRowHeight;         // Extra height for collapse icon row
        int titleWidth;                // Width of title text (scaled)
    }
}
