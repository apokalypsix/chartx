package com.apokalypsix.chartx.core.ui.overlay;

/**
 * Overlay displaying symbol information.
 *
 * <p>Shows:
 * <ul>
 *   <li>Symbol name (e.g., "AAPL", "EUR/USD")</li>
 *   <li>Timeframe (e.g., "1D", "4H", "15m")</li>
 *   <li>Exchange (e.g., "NASDAQ", "NYSE")</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SymbolInfoOverlay overlay = new SymbolInfoOverlay(config);
 * overlay.setSymbol("AAPL");
 * overlay.setTimeframe("1D");
 * overlay.setExchange("NASDAQ");
 * }</pre>
 */
public class SymbolInfoOverlay extends InfoOverlay {

    private String symbol = "";
    private String timeframe = "";
    private String exchange = "";

    /**
     * Creates a symbol info overlay with default configuration.
     */
    public SymbolInfoOverlay() {
        this(new InfoOverlayConfig());
    }

    /**
     * Creates a symbol info overlay with the given configuration.
     *
     * @param config the appearance configuration
     */
    public SymbolInfoOverlay(InfoOverlayConfig config) {
        super("Symbol", config);
    }

    /**
     * Returns the symbol name.
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Sets the symbol name.
     *
     * @param symbol the symbol (e.g., "AAPL", "EUR/USD")
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol != null ? symbol : "";
        updateTitle();
    }

    /**
     * Returns the timeframe.
     */
    public String getTimeframe() {
        return timeframe;
    }

    /**
     * Sets the timeframe.
     *
     * @param timeframe the timeframe (e.g., "1D", "4H", "15m")
     */
    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe != null ? timeframe : "";
        updateTitle();
    }

    /**
     * Returns the exchange.
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Sets the exchange.
     *
     * @param exchange the exchange (e.g., "NASDAQ", "NYSE")
     */
    public void setExchange(String exchange) {
        this.exchange = exchange != null ? exchange : "";
    }

    /**
     * Sets all symbol information at once.
     *
     * @param symbol    the symbol name
     * @param timeframe the timeframe
     * @param exchange  the exchange
     */
    public void setSymbolInfo(String symbol, String timeframe, String exchange) {
        this.symbol = symbol != null ? symbol : "";
        this.timeframe = timeframe != null ? timeframe : "";
        this.exchange = exchange != null ? exchange : "";
        updateTitle();
    }

    /**
     * Updates the title to show symbol and timeframe.
     */
    private void updateTitle() {
        StringBuilder sb = new StringBuilder();
        if (!symbol.isEmpty()) {
            sb.append(symbol);
        }
        if (!timeframe.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(timeframe);
        }
        setTitle(sb.length() > 0 ? sb.toString() : "Symbol");
    }

    @Override
    public void updateContent() {
        clearRows();

        if (!exchange.isEmpty()) {
            addRow("Exchange", exchange);
        }
    }
}
