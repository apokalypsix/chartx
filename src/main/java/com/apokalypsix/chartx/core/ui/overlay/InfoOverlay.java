package com.apokalypsix.chartx.core.ui.overlay;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract base class for foldable info overlays.
 *
 * <p>Info overlays display information in collapsible panels at chart corners.
 * Subclasses implement specific content rendering.
 *
 * <p>Features:
 * <ul>
 *   <li>Collapsible with click on header</li>
 *   <li>Semi-transparent background</li>
 *   <li>Configurable appearance via InfoOverlayConfig</li>
 * </ul>
 */
public abstract class InfoOverlay {

    protected InfoOverlayConfig config;
    protected String title;
    protected boolean collapsed;
    protected Rectangle bounds = new Rectangle();
    protected Rectangle headerBounds = new Rectangle();
    protected int stackOffset = 0;

    // Cached content rows
    protected final List<OverlayRow> rows = new ArrayList<>();

    // Settings click callback
    private Consumer<Object> onSettingsClicked;

    // Collapse icon click bounds for bottom-row mode
    protected Rectangle collapseIconBounds = new Rectangle();

    // Settings icon appearance
    private static final Color SETTINGS_ICON_COLOR = new Color(150, 152, 156);
    private static final Color SETTINGS_ICON_HOVER = new Color(200, 202, 206);
    private static final int SETTINGS_ICON_SIZE = 16;

    // Cached SVG icon
    private static Icon settingsIcon;
    private static Icon settingsIconHover;

    /**
     * Creates an info overlay with the given title and configuration.
     *
     * @param title  the overlay title shown in header
     * @param config the appearance configuration
     */
    public InfoOverlay(String title, InfoOverlayConfig config) {
        this.title = title;
        this.config = config != null ? config : new InfoOverlayConfig();
        this.collapsed = this.config.isCollapsed();
    }

    /**
     * Returns the overlay title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the overlay title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the configuration.
     */
    public InfoOverlayConfig getConfig() {
        return config;
    }

    /**
     * Sets the configuration.
     */
    public void setConfig(InfoOverlayConfig config) {
        this.config = config != null ? config : new InfoOverlayConfig();
    }

    /**
     * Returns whether the overlay is collapsed.
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Sets the collapsed state.
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     * Toggles the collapsed state.
     */
    public void toggleCollapsed() {
        if (config.isCollapsible()) {
            this.collapsed = !this.collapsed;
        }
    }

    /**
     * Returns the current bounds of this overlay.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Returns the header bounds used for click-to-collapse detection.
     */
    public Rectangle getHeaderBounds() {
        return headerBounds;
    }

    /**
     * Returns the collapse icon bounds for hit testing in bottom-row mode.
     */
    public Rectangle getCollapseIconBounds() {
        return collapseIconBounds;
    }

    /**
     * Sets the vertical offset for stacking multiple overlays.
     */
    public void setStackOffset(int offset) {
        this.stackOffset = offset;
    }

    /**
     * Returns the vertical offset.
     */
    public int getStackOffset() {
        return stackOffset;
    }

    /**
     * Sets the callback invoked when a settings icon is clicked.
     *
     * @param callback the callback receiving the artifact object
     */
    public void setOnSettingsClicked(Consumer<Object> callback) {
        this.onSettingsClicked = callback;
    }

    /**
     * Returns the settings click callback.
     */
    public Consumer<Object> getOnSettingsClicked() {
        return onSettingsClicked;
    }

    /**
     * Returns the content rows (read-only).
     */
    public List<OverlayRow> getRows() {
        return rows;
    }

    /**
     * Updates the overlay content. Called before rendering to refresh data.
     * Subclasses should update their rows list here.
     */
    public abstract void updateContent();

    /**
     * Renders the overlay to the graphics context.
     *
     * @param g2     the graphics context
     * @param width  the chart width
     * @param height the chart height
     */
    public void render(Graphics2D g2, int width, int height) {
        if (!config.isVisible()) {
            return;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // Update content before rendering
        updateContent();

        // Calculate dimensions
        int padding = config.getPadding();
        int rowSpacing = config.getRowSpacing();

        FontMetrics headerFm = g2.getFontMetrics(config.getHeaderFont());
        FontMetrics labelFm = g2.getFontMetrics(config.getLabelFont());
        FontMetrics valueFm = g2.getFontMetrics(config.getValueFont());

        int headerHeight = headerFm.getHeight() + padding;

        // Check if we're using bottom-row collapse icon mode
        boolean bottomCollapseIcon = config.getCollapseIconPosition() == InfoOverlayConfig.CollapseIconPosition.BOTTOM_ROW;
        // Extra row for collapse icon when expanded in bottom mode
        int collapseRowHeight = 0;
        if (bottomCollapseIcon && config.isCollapsible() && !collapsed) {
            collapseRowHeight = Math.max(labelFm.getHeight(), valueFm.getHeight()) + rowSpacing;
        }

        // Calculate content dimensions
        int contentWidth = 0;
        int contentHeight = 0;
        boolean hasAnySettings = false;

        if (!collapsed && !rows.isEmpty()) {
            int maxLabelWidth = 0;
            int maxValueWidth = 0;

            for (OverlayRow row : rows) {
                maxLabelWidth = Math.max(maxLabelWidth, labelFm.stringWidth(row.label));
                maxValueWidth = Math.max(maxValueWidth, valueFm.stringWidth(row.value));
                if (row.hasSettings) {
                    hasAnySettings = true;
                }
            }

            contentWidth = maxLabelWidth + config.getColumnSpacing() + maxValueWidth;
            // Add space for settings icon if any row has settings
            if (hasAnySettings) {
                contentWidth += SETTINGS_ICON_SIZE + 6;
            }
            contentHeight = rows.size() * (Math.max(labelFm.getHeight(), valueFm.getHeight()) + rowSpacing) - rowSpacing;
            // Add collapse row height if needed
            contentHeight += collapseRowHeight;
        }

        // Minimum width includes title
        int titleWidth = headerFm.stringWidth(title);
        if (config.isCollapsible() && config.isShowExpandButton() && !bottomCollapseIcon) {
            titleWidth += 20; // Space for expand button in header (not for bottom mode)
        }

        // In bottom collapse icon mode with empty title when expanded, don't add header height
        boolean hideHeader = bottomCollapseIcon && !collapsed && title.isEmpty();
        int effectiveHeaderHeight = hideHeader ? 0 : headerHeight;

        int overlayWidth = Math.max(config.getMinWidth(), Math.max(titleWidth, contentWidth) + padding * 2);
        int overlayHeight = effectiveHeaderHeight + (collapsed ? 0 : contentHeight + padding);

        // Calculate position
        int x, y;
        OverlayPosition pos = config.getPosition();

        switch (pos) {
            case TOP_RIGHT:
                x = width - overlayWidth - config.getMarginX();
                y = config.getMarginY() + stackOffset;
                break;
            case BOTTOM_LEFT:
                x = config.getMarginX();
                y = height - overlayHeight - config.getMarginY() - stackOffset;
                break;
            case BOTTOM_RIGHT:
                x = width - overlayWidth - config.getMarginX();
                y = height - overlayHeight - config.getMarginY() - stackOffset;
                break;
            case TOP_LEFT:
            default:
                x = config.getMarginX();
                y = config.getMarginY() + stackOffset;
                break;
        }

        // Update bounds for hit testing
        bounds.setBounds(x, y, overlayWidth, overlayHeight);
        headerBounds.setBounds(x, y, overlayWidth, effectiveHeaderHeight);

        // Draw background
        if (config.isShowBackground()) {
            g2.setColor(config.getBackgroundColor());
            RoundRectangle2D bg = new RoundRectangle2D.Float(
                    x, y, overlayWidth, overlayHeight,
                    config.getCornerRadius(), config.getCornerRadius());
            g2.fill(bg);
        }

        // Draw border
        if (config.isShowBorder()) {
            g2.setColor(config.getBorderColor());
            RoundRectangle2D border = new RoundRectangle2D.Float(
                    x, y, overlayWidth, overlayHeight,
                    config.getCornerRadius(), config.getCornerRadius());
            g2.draw(border);
        }

        // Text shadow color for transparent mode
        Color shadowColor = new Color(0, 0, 0, 180);
        boolean useShadow = config.isTextShadow();

        // Draw header (if not hidden)
        if (!hideHeader) {
            g2.setFont(config.getHeaderFont());
            int textY = y + padding / 2 + headerFm.getAscent();
            if (useShadow) {
                g2.setColor(shadowColor);
                g2.drawString(title, x + padding + 1, textY + 1);
            }
            g2.setColor(config.getHeaderColor());
            g2.drawString(title, x + padding, textY);

            // Draw expand/collapse button in header (only for header mode)
            if (config.isCollapsible() && config.isShowExpandButton() && !bottomCollapseIcon) {
                g2.setColor(config.getExpandButtonColor());
                int btnX = x + overlayWidth - padding - 8;
                int btnY = y + padding / 2 + headerFm.getAscent() / 2;
                drawExpandButton(g2, btnX, btnY, collapsed);
            }

            // Draw expand button in header when collapsed in bottom mode (to the right of title)
            if (bottomCollapseIcon && config.isCollapsible() && collapsed) {
                g2.setColor(config.getExpandButtonColor());
                int btnX = x + padding + titleWidth + 12; // Right of title with some spacing
                int btnY = y + padding / 2 + headerFm.getAscent() / 2;
                drawCollapseButton(g2, btnX, btnY, false, 10); // false = down arrow (expand), size 10

                // Update collapse icon bounds for hit testing
                collapseIconBounds.setBounds(btnX - 12, btnY - 12, 24, 24);
            }
        }

        // Draw content rows
        if (!collapsed && !rows.isEmpty()) {
            int rowY = y + effectiveHeaderHeight + padding / 2;
            int rowHeight = Math.max(labelFm.getHeight(), valueFm.getHeight()) + rowSpacing;

            for (OverlayRow row : rows) {
                // Label
                g2.setFont(config.getLabelFont());
                if (useShadow) {
                    g2.setColor(shadowColor);
                    g2.drawString(row.label, x + padding + 1, rowY + labelFm.getAscent() + 1);
                }
                g2.setColor(config.getLabelColor());
                g2.drawString(row.label, x + padding, rowY + labelFm.getAscent());

                // Value (offset right if settings icon is present)
                g2.setFont(config.getValueFont());
                Color valueColor;
                if (row.colorType == ColorType.POSITIVE) {
                    valueColor = config.getPositiveColor();
                } else if (row.colorType == ColorType.NEGATIVE) {
                    valueColor = config.getNegativeColor();
                } else {
                    valueColor = config.getValueColor();
                }

                int valueRightEdge = x + overlayWidth - padding;
                if (row.hasSettings) {
                    valueRightEdge -= SETTINGS_ICON_SIZE + 6;
                }
                int valueX = valueRightEdge - valueFm.stringWidth(row.value);
                if (useShadow) {
                    g2.setColor(shadowColor);
                    g2.drawString(row.value, valueX + 1, rowY + valueFm.getAscent() + 1);
                }
                g2.setColor(valueColor);
                g2.drawString(row.value, valueX, rowY + valueFm.getAscent());

                // Draw settings icon if enabled
                if (row.hasSettings) {
                    int iconX = x + overlayWidth - padding - SETTINGS_ICON_SIZE;
                    int iconY = rowY + (labelFm.getHeight() - SETTINGS_ICON_SIZE) / 2;

                    // Update icon bounds for hit testing
                    if (row.settingsIconBounds == null) {
                        row.settingsIconBounds = new Rectangle();
                    }
                    row.settingsIconBounds.setBounds(iconX - 2, iconY - 2, SETTINGS_ICON_SIZE + 4, SETTINGS_ICON_SIZE + 4);

                    drawSettingsIcon(g2, iconX, iconY, SETTINGS_ICON_SIZE, SETTINGS_ICON_COLOR);
                }

                rowY += rowHeight;
            }

            // Draw bottom collapse icon if in that mode
            if (bottomCollapseIcon && config.isCollapsible()) {
                g2.setColor(config.getExpandButtonColor());
                // Left-align the icon, position at bottom of content
                int btnX = x + padding + 6; // Left-aligned with some padding
                int btnY = rowY + rowHeight / 2;
                // Draw up-pointing triangle (collapse) - bigger size
                drawCollapseButton(g2, btnX, btnY, true, 10); // true = up arrow (collapse), size 10

                // Update collapse icon bounds for hit testing
                collapseIconBounds.setBounds(btnX - 12, btnY - 12, 24, 24);
            }
        }
    }

    /**
     * Draws the collapse button (up/down triangle) for bottom-row mode.
     */
    private void drawCollapseButton(Graphics2D g2, int x, int y, boolean pointUp, int size) {
        int[] xPoints, yPoints;

        if (pointUp) {
            // Up-pointing triangle (collapse)
            xPoints = new int[]{x - size / 2, x + size / 2, x};
            yPoints = new int[]{y + size / 2, y + size / 2, y - size / 2};
        } else {
            // Down-pointing triangle (expand)
            xPoints = new int[]{x - size / 2, x + size / 2, x};
            yPoints = new int[]{y - size / 2, y - size / 2, y + size / 2};
        }

        g2.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Gets or creates the settings icon with the given color.
     */
    private static Icon getSettingsIcon(Color color) {
        try {
            // FlatSVGIcon loads from classpath without leading slash
            FlatSVGIcon icon = new FlatSVGIcon("icons/settings.svg", SETTINGS_ICON_SIZE, SETTINGS_ICON_SIZE);
            if (icon.hasFound()) {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
                return icon;
            }
            return null;
        } catch (Exception e) {
            // Fallback: return null and use programmatic drawing
            return null;
        }
    }

    // Flag to track if we've attempted to load SVG icons
    private static boolean svgIconsLoaded = false;

    /**
     * Draws a gear/settings icon using SVG or fallback to programmatic drawing.
     */
    private void drawSettingsIcon(Graphics2D g2, int x, int y, int size, Color color) {
        // Try to use SVG icon first (only attempt once)
        if (!svgIconsLoaded) {
            svgIconsLoaded = true;
            settingsIcon = getSettingsIcon(SETTINGS_ICON_COLOR);
            settingsIconHover = getSettingsIcon(SETTINGS_ICON_HOVER);
        }

        Icon icon = (color == SETTINGS_ICON_HOVER) ? settingsIconHover : settingsIcon;
        if (icon != null) {
            icon.paintIcon(null, g2, x, y);
            return;
        }

        // Fallback to programmatic drawing if SVG loading fails
        drawSettingsIconFallback(g2, x, y, size, color);
    }

    /**
     * Fallback programmatic gear icon drawing.
     */
    private void drawSettingsIconFallback(Graphics2D g2, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float outerR = size / 2f - 0.5f;
        float innerR = size * 0.38f;
        float holeR = size * 0.18f;
        int teeth = 6;

        // Build gear path
        java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
        double toothWidth = Math.PI / teeth;

        for (int i = 0; i < teeth; i++) {
            double angle = Math.PI * 2 * i / teeth - Math.PI / 2;

            double a1 = angle - toothWidth * 0.4;
            double a2 = angle + toothWidth * 0.4;
            double a3 = angle + toothWidth * 0.6;
            double a4 = angle + Math.PI * 2 / teeth - toothWidth * 0.6;

            if (i == 0) {
                path.moveTo(cx + Math.cos(a1) * outerR, cy + Math.sin(a1) * outerR);
            }

            path.lineTo(cx + Math.cos(a1) * outerR, cy + Math.sin(a1) * outerR);
            path.lineTo(cx + Math.cos(a2) * outerR, cy + Math.sin(a2) * outerR);
            path.lineTo(cx + Math.cos(a3) * innerR, cy + Math.sin(a3) * innerR);
            path.lineTo(cx + Math.cos(a4) * innerR, cy + Math.sin(a4) * innerR);
        }
        path.closePath();

        g.setColor(color);
        g.fill(path);

        g.setColor(config.getBackgroundColor());
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - holeR, cy - holeR, holeR * 2, holeR * 2));

        g.dispose();
    }

    /**
     * Draws the expand/collapse button indicator.
     */
    private void drawExpandButton(Graphics2D g2, int x, int y, boolean collapsed) {
        int size = 6;
        int[] xPoints, yPoints;

        if (collapsed) {
            // Right-pointing triangle
            xPoints = new int[]{x, x, x + size};
            yPoints = new int[]{y - size / 2, y + size / 2, y};
        } else {
            // Down-pointing triangle
            xPoints = new int[]{x - size / 2, x + size / 2, x};
            yPoints = new int[]{y - size / 2, y - size / 2, y + size / 2};
        }

        g2.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Handles mouse click events.
     *
     * @param e the mouse event
     * @return true if the click was consumed by this overlay
     */
    public boolean handleClick(MouseEvent e) {
        if (!config.isVisible()) {
            return false;
        }

        int mx = e.getX();
        int my = e.getY();

        // Check settings icon clicks first (higher priority)
        if (!collapsed && onSettingsClicked != null) {
            for (OverlayRow row : rows) {
                if (row.hasSettings && row.settingsIconBounds != null &&
                    row.settingsIconBounds.contains(mx, my)) {
                    onSettingsClicked.accept(row.artifact);
                    return true;
                }
            }
        }

        // Check if click is on bottom collapse icon (for bottom-row mode)
        boolean bottomCollapseIcon = config.getCollapseIconPosition() == InfoOverlayConfig.CollapseIconPosition.BOTTOM_ROW;
        if (bottomCollapseIcon && config.isCollapsible() && collapseIconBounds.contains(mx, my)) {
            toggleCollapsed();
            return true;
        }

        // Check if click is on header (toggle collapse) - for header mode or collapsed state
        if (config.isCollapsible() && headerBounds.contains(mx, my)) {
            toggleCollapsed();
            return true;
        }

        // Check if click is anywhere in overlay (consume but don't act)
        return bounds.contains(mx, my);
    }

    /**
     * Returns the total height of this overlay including stack offset.
     */
    public int getTotalHeight() {
        return bounds.height + config.getStackSpacing();
    }

    /**
     * Clears all content rows.
     */
    protected void clearRows() {
        rows.clear();
    }

    /**
     * Adds a content row.
     */
    protected void addRow(String label, String value) {
        rows.add(new OverlayRow(label, value, ColorType.NEUTRAL));
    }

    /**
     * Adds a content row with color type.
     */
    protected void addRow(String label, String value, ColorType colorType) {
        rows.add(new OverlayRow(label, value, colorType));
    }

    /**
     * Adds a content row with artifact reference and settings icon.
     *
     * @param label       the row label
     * @param value       the row value
     * @param colorType   the value color type
     * @param artifact    the artifact object (indicator, drawing, series)
     * @param hasSettings true to show settings icon
     */
    protected void addRow(String label, String value, ColorType colorType, Object artifact, boolean hasSettings) {
        rows.add(new OverlayRow(label, value, colorType, artifact, hasSettings));
    }

    /**
     * Color type for value display.
     */
    public enum ColorType {
        NEUTRAL,
        POSITIVE,
        NEGATIVE
    }

    /**
     * Row data structure for overlay content.
     */
    public static class OverlayRow {
        public final String label;
        public final String value;
        public final ColorType colorType;
        public final Object artifact;
        public final boolean hasSettings;
        public Rectangle settingsIconBounds;

        public OverlayRow(String label, String value, ColorType colorType) {
            this(label, value, colorType, null, false);
        }

        public OverlayRow(String label, String value, ColorType colorType, Object artifact, boolean hasSettings) {
            this.label = label;
            this.value = value;
            this.colorType = colorType;
            this.artifact = artifact;
            this.hasSettings = hasSettings;
        }
    }
}
