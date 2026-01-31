package com.apokalypsix.chartx.core.ui.sidebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A TradingView-style sidebar panel with menu items that show flyout submenus.
 *
 * <p>The sidebar displays category buttons that expand into flyout menus:
 * <ul>
 *   <li>Selection - cursor, crosshair</li>
 *   <li>Lines - trend line, horizontal, vertical, ray, channel</li>
 *   <li>Shapes - rectangle, ellipse, triangle</li>
 *   <li>Fibonacci - retracement, extension, fan</li>
 *   <li>Indicators - SMA, EMA, RSI, MACD, etc.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ChartSidebar sidebar = new ChartSidebar();
 * sidebar.setConfig(new SidebarConfig()
 *     .position(SidebarPosition.LEFT)
 *     .width(48));
 * sidebar.setToolSelectedListener(toolId -> chart.setTool(toolId));
 * }</pre>
 */
public class ChartSidebar extends JPanel {

    private SidebarConfig config;
    private final Map<String, SidebarMenuButton> menuButtons = new LinkedHashMap<>();
    private final ToolFlyoutMenu flyoutMenu;
    private final JPopupMenu popup;

    private SidebarMenuButton expandedMenu = null;
    private String selectedToolId;
    private Consumer<String> toolSelectedListener;

    // Standard tool IDs
    public static final String TOOL_CURSOR = "cursor";
    public static final String TOOL_CROSSHAIR = "crosshair";
    public static final String TOOL_TREND_LINE = "trend_line";
    public static final String TOOL_HORIZONTAL_LINE = "horizontal_line";
    public static final String TOOL_VERTICAL_LINE = "vertical_line";
    public static final String TOOL_RAY = "ray";
    public static final String TOOL_CHANNEL = "channel";
    public static final String TOOL_RECTANGLE = "rectangle";
    public static final String TOOL_ELLIPSE = "ellipse";
    public static final String TOOL_TRIANGLE = "triangle";
    public static final String TOOL_ARROW = "arrow";
    public static final String TOOL_FIB_RETRACEMENT = "fib_retracement";
    public static final String TOOL_FIB_EXTENSION = "fib_extension";
    public static final String TOOL_FIB_FAN = "fib_fan";
    public static final String TOOL_PITCHFORK = "pitchfork";
    public static final String TOOL_MEASURE = "measure";
    public static final String TOOL_TEXT = "text";
    public static final String TOOL_CALLOUT = "callout";

    // Indicator tool IDs
    public static final String IND_SMA = "ind_sma";
    public static final String IND_EMA = "ind_ema";
    public static final String IND_RSI = "ind_rsi";
    public static final String IND_MACD = "ind_macd";
    public static final String IND_BB = "ind_bollinger";
    public static final String IND_ATR = "ind_atr";
    public static final String IND_VWAP = "ind_vwap";
    public static final String IND_VOLUME = "ind_volume";
    public static final String IND_OBV = "ind_obv";
    public static final String IND_CVD = "ind_cvd";

    /**
     * Creates a sidebar with default configuration.
     */
    public ChartSidebar() {
        this(new SidebarConfig());
    }

    /**
     * Creates a sidebar with the given configuration.
     *
     * @param config the sidebar configuration
     */
    public ChartSidebar(SidebarConfig config) {
        this.config = config;

        setLayout(new BorderLayout());

        // Create flyout menu
        flyoutMenu = new ToolFlyoutMenu();
        flyoutMenu.setOnItemSelected(this::onFlyoutItemSelected);

        // Create popup to hold flyout
        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setBackground(new Color(0, 0, 0, 0));
        popup.add(flyoutMenu);

        applyConfig();
        createDefaultMenus();
    }

    /**
     * Returns the current configuration.
     */
    public SidebarConfig getConfig() {
        return config;
    }

    /**
     * Sets the sidebar configuration.
     */
    public void setConfig(SidebarConfig config) {
        this.config = config != null ? config : new SidebarConfig();
        applyConfig();
    }

    /**
     * Sets the listener called when a tool is selected.
     *
     * @param listener receives the selected tool ID
     */
    public void setToolSelectedListener(Consumer<String> listener) {
        this.toolSelectedListener = listener;
    }

    /**
     * Returns the currently selected tool ID.
     */
    public String getSelectedToolId() {
        return selectedToolId;
    }

    /**
     * Selects a tool by ID.
     *
     * @param toolId the tool ID
     */
    public void selectTool(String toolId) {
        if (selectedToolId != null && selectedToolId.equals(toolId)) {
            return;
        }

        selectedToolId = toolId;

        // Update menu button selection states
        for (SidebarMenuButton button : menuButtons.values()) {
            boolean hasSelected = false;
            for (ToolFlyoutMenu.FlyoutMenuItem item : button.getSubItems()) {
                if (item.id.equals(toolId)) {
                    hasSelected = true;
                    break;
                }
            }
            button.setSelectedSubItemId(hasSelected ? toolId : null);
        }

        if (toolSelectedListener != null) {
            toolSelectedListener.accept(toolId);
        }
    }

    /**
     * Adds a menu category to the sidebar.
     */
    public void addMenu(SidebarMenuButton menu) {
        menuButtons.put(menu.getId(), menu);
        menu.applyConfig(config);
        menu.setOnExpand(() -> showFlyout(menu));
        rebuildPanel();
    }

    /**
     * Gets a menu button by ID.
     */
    public SidebarMenuButton getMenu(String id) {
        return menuButtons.get(id);
    }

    /**
     * Returns all menu buttons.
     */
    public List<SidebarMenuButton> getMenus() {
        return new ArrayList<>(menuButtons.values());
    }

    private void applyConfig() {
        setBackground(config.getBackgroundColor());
        setPreferredSize(new Dimension(config.getWidth(), 0));
        setBorder(BorderFactory.createMatteBorder(
                0, 0, 0,
                config.getPosition() == SidebarPosition.LEFT ? 1 : 0,
                config.getBorderColor()));

        flyoutMenu.applyConfig(config);

        for (SidebarMenuButton menu : menuButtons.values()) {
            menu.applyConfig(config);
        }

        revalidate();
        repaint();
    }

    private void createDefaultMenus() {
        // Selection menu
        SidebarMenuButton selectionMenu = new SidebarMenuButton("selection", "Selection", "Selection tools");
        selectionMenu.addSubItem(TOOL_CURSOR, "Cursor", "Sel");
        selectionMenu.addSubItem(TOOL_CROSSHAIR, "Crosshair", "+");
        addMenu(selectionMenu);

        // Lines menu
        SidebarMenuButton linesMenu = new SidebarMenuButton("lines", "Lines", "Line drawing tools");
        linesMenu.addSubItem(TOOL_TREND_LINE, "Trend Line", "TL");
        linesMenu.addSubItem(TOOL_HORIZONTAL_LINE, "Horizontal Line", "HL");
        linesMenu.addSubItem(TOOL_VERTICAL_LINE, "Vertical Line", "VL");
        linesMenu.addSubItem(TOOL_RAY, "Ray", "Ray");
        linesMenu.addSubItem(TOOL_CHANNEL, "Parallel Channel", "Ch");
        addMenu(linesMenu);

        // Shapes menu
        SidebarMenuButton shapesMenu = new SidebarMenuButton("shapes", "Shapes", "Shape drawing tools");
        shapesMenu.addSubItem(TOOL_RECTANGLE, "Rectangle", "Rec");
        shapesMenu.addSubItem(TOOL_ELLIPSE, "Ellipse", "Ell");
        shapesMenu.addSubItem(TOOL_TRIANGLE, "Triangle", "Tri");
        shapesMenu.addSubItem(TOOL_ARROW, "Arrow", "Arr");
        addMenu(shapesMenu);

        // Fibonacci menu
        SidebarMenuButton fibMenu = new SidebarMenuButton("fibonacci", "Fibonacci", "Fibonacci tools");
        fibMenu.addSubItem(TOOL_FIB_RETRACEMENT, "Fib Retracement", "Fib");
        fibMenu.addSubItem(TOOL_FIB_EXTENSION, "Fib Extension", "FEx");
        fibMenu.addSubItem(TOOL_FIB_FAN, "Fib Fan", "FFn");
        addMenu(fibMenu);

        // Analysis menu
        SidebarMenuButton analysisMenu = new SidebarMenuButton("analysis", "Analysis", "Analysis tools");
        analysisMenu.addSubItem(TOOL_PITCHFORK, "Pitchfork", "PF");
        analysisMenu.addSubItem(TOOL_MEASURE, "Measure", "Msr");
        addMenu(analysisMenu);

        // Annotations menu
        SidebarMenuButton annotMenu = new SidebarMenuButton("annotations", "Text", "Text and annotations");
        annotMenu.addSubItem(TOOL_TEXT, "Text", "Txt");
        annotMenu.addSubItem(TOOL_CALLOUT, "Callout", "Cal");
        addMenu(annotMenu);

        // Indicators menu
        SidebarMenuButton indMenu = new SidebarMenuButton("indicators", "Indicators", "Technical indicators");
        indMenu.addSubItem(IND_SMA, "Simple Moving Average", "SMA");
        indMenu.addSubItem(IND_EMA, "Exponential MA", "EMA");
        indMenu.addSubItem(IND_RSI, "Relative Strength Index", "RSI");
        indMenu.addSubItem(IND_MACD, "MACD", "MAC");
        indMenu.addSubItem(IND_BB, "Bollinger Bands", "BB");
        indMenu.addSubItem(IND_ATR, "Average True Range", "ATR");
        indMenu.addSubItem(IND_VWAP, "VWAP", "VWP");
        indMenu.addSubItem(IND_VOLUME, "Volume", "Vol");
        indMenu.addSubItem(IND_OBV, "On-Balance Volume", "OBV");
        indMenu.addSubItem(IND_CVD, "Cumulative Volume Delta", "CVD");
        addMenu(indMenu);

        rebuildPanel();

        // Default selection
        selectTool(TOOL_CURSOR);
    }

    private void showFlyout(SidebarMenuButton menu) {
        // Collapse previously expanded menu
        if (expandedMenu != null && expandedMenu != menu) {
            expandedMenu.setExpanded(false);
        }

        if (menu.isExpanded()) {
            // Close if already open
            menu.setExpanded(false);
            popup.setVisible(false);
            expandedMenu = null;
        } else {
            // Show flyout
            menu.setExpanded(true);
            expandedMenu = menu;

            flyoutMenu.setItems(menu.getSubItems());

            // Position flyout to the right of the button
            Point menuLoc = menu.getLocationOnScreen();
            int x = menuLoc.x + menu.getWidth() + 2;
            int y = menuLoc.y;

            popup.show(menu, menu.getWidth() + 2, 0);
        }
    }

    private void onFlyoutItemSelected(String itemId) {
        popup.setVisible(false);
        if (expandedMenu != null) {
            expandedMenu.setExpanded(false);
            expandedMenu = null;
        }
        selectTool(itemId);
    }

    private void rebuildPanel() {
        removeAll();

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        for (SidebarMenuButton menu : menuButtons.values()) {
            // Center the menu button
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
            wrapper.setOpaque(false);
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, menu.getPreferredSize().height + 4));
            wrapper.add(menu);
            content.add(wrapper);
        }

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Closes any open flyout menu.
     */
    public void closeFlyout() {
        if (expandedMenu != null) {
            expandedMenu.setExpanded(false);
            expandedMenu = null;
            popup.setVisible(false);
        }
    }
}
