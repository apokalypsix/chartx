package com.apokalypsix.chartx.examples.library;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.data.model.TPOProfile;
import com.apokalypsix.chartx.core.data.model.TPOSeries;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads TPO demo data from JSON resource files.
 *
 * <p>The JSON format expected:
 * <pre>{@code
 * {
 *   "symbol": "...",
 *   "timeframe": "...",
 *   "days": [{
 *     "date": "yyyyMMdd",
 *     "candles": [{"timestamp": x, "o": x, "h": x, "l": x, "c": x, "v": x}, ...],
 *     "tpo": {
 *       "rowSize": x,
 *       "rows": [{"rowId": x, "lowPrice": x, "highPrice": x, "letters": x}, ...],
 *       "poc": x, "vah": x, "val": x, ...
 *     }
 *   }, ...]
 * }
 * }</pre>
 */
public class TpoDataLoader {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Result of loading TPO data.
     */
    public static class TpoData {
        public final OhlcData ohlcData;
        public final TPOSeries tpoSeries;
        public final String symbol;
        public final String timeframe;

        public TpoData(OhlcData ohlcData, TPOSeries tpoSeries, String symbol, String timeframe) {
            this.ohlcData = ohlcData;
            this.tpoSeries = tpoSeries;
            this.symbol = symbol;
            this.timeframe = timeframe;
        }
    }

    /**
     * Loads TPO data from a resource file.
     *
     * @param resourcePath path to the JSON resource (e.g., "/tpo-demo-data.json")
     * @return the loaded TPO data
     * @throws IOException if the resource cannot be read
     */
    public static TpoData loadFromResource(String resourcePath) throws IOException {
        try (InputStream is = TpoDataLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return loadFromStream(is);
        }
    }

    /**
     * Loads TPO data from an input stream.
     */
    public static TpoData loadFromStream(InputStream is) throws IOException {
        JsonNode root = MAPPER.readTree(is);

        String symbol = root.path("symbol").asText("DEMO");
        String timeframe = root.path("timeframe").asText("tf30m");

        JsonNode daysNode = root.path("days");
        if (!daysNode.isArray() || daysNode.isEmpty()) {
            throw new IOException("No days data found in JSON");
        }

        // Count total candles for capacity
        int totalCandles = 0;
        for (JsonNode dayNode : daysNode) {
            totalCandles += dayNode.path("candles").size();
        }

        OhlcData ohlcData = new OhlcData("tpo_demo", symbol, totalCandles);

        // Determine tick size from first day's TPO data
        float tickSize = (float) daysNode.get(0).path("tpo").path("rowSize").asDouble(1.0);
        TPOSeries tpoSeries = new TPOSeries("tpo_demo", symbol + " TPO", tickSize);

        // Determine TPO period from timeframe (assume 30 min for tf30m)
        long tpoPeriodMillis = 30 * 60 * 1000L;
        tpoSeries.setTpoPeriodMillis(tpoPeriodMillis);

        // Process each day
        for (JsonNode dayNode : daysNode) {
            String dateStr = dayNode.path("date").asText();
            LocalDate date = LocalDate.parse(dateStr, DATE_FMT);

            // Load candles
            JsonNode candlesNode = dayNode.path("candles");
            for (JsonNode candleNode : candlesNode) {
                long timestamp = candleNode.path("timestamp").asLong();
                float open = (float) candleNode.path("o").asDouble();
                float high = (float) candleNode.path("h").asDouble();
                float low = (float) candleNode.path("l").asDouble();
                float close = (float) candleNode.path("c").asDouble();
                float volume = (float) candleNode.path("v").asDouble();

                ohlcData.append(timestamp, open, high, low, close, volume);
            }

            // Load TPO profile
            JsonNode tpoNode = dayNode.path("tpo");
            if (!tpoNode.isMissingNode()) {
                TPOProfile profile = loadTpoProfile(tpoNode, date, tpoPeriodMillis);
                tpoSeries.append(profile);
            }
        }

        return new TpoData(ohlcData, tpoSeries, symbol, timeframe);
    }

    /**
     * Loads a single TPO profile from JSON.
     */
    private static TPOProfile loadTpoProfile(JsonNode tpoNode, LocalDate date, long tpoPeriodMillis) {
        float rowSize = (float) tpoNode.path("rowSize").asDouble(1.0);

        // Session times: midnight to midnight UTC
        LocalDateTime sessionStart = date.atStartOfDay();
        LocalDateTime sessionEnd = date.plusDays(1).atStartOfDay();

        long startMillis = sessionStart.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endMillis = sessionEnd.toInstant(ZoneOffset.UTC).toEpochMilli();

        TPOProfile profile = new TPOProfile(startMillis, endMillis, rowSize, tpoPeriodMillis);

        // Load rows - each row has letters count which we distribute as TPO periods
        JsonNode rowsNode = tpoNode.path("rows");
        if (rowsNode.isArray()) {
            int maxPeriod = 0;

            for (JsonNode rowNode : rowsNode) {
                float lowPrice = (float) rowNode.path("lowPrice").asDouble();
                int letters = rowNode.path("letters").asInt(1);

                // Distribute letters across periods (0, 1, 2, ..., letters-1)
                // This creates a realistic distribution
                for (int period = 0; period < letters; period++) {
                    profile.addTPO(lowPrice, period);
                    if (period > maxPeriod) {
                        maxPeriod = period;
                    }
                }
            }
        }

        // Set key levels from JSON
        if (tpoNode.has("poc") && !tpoNode.path("poc").isNull()) {
            // POC is already computed by the profile based on TPO counts
        }

        // Set Initial Balance (first 2 hours = 4 periods of 30 min)
        profile.setIBPeriods(4);

        // Compute IB high/low from the TPO data
        JsonNode rowsForIB = tpoNode.path("rows");
        if (rowsForIB.isArray() && rowsForIB.size() > 0) {
            float ibHigh = Float.NEGATIVE_INFINITY;
            float ibLow = Float.POSITIVE_INFINITY;

            for (JsonNode rowNode : rowsForIB) {
                float lowPrice = (float) rowNode.path("lowPrice").asDouble();
                float highPrice = (float) rowNode.path("highPrice").asDouble();
                int letters = rowNode.path("letters").asInt(0);

                // Check if this row was touched in IB periods (first 4 periods)
                if (letters > 0) {
                    // For simplicity, use prices from rows with high letter counts
                    // as they're more likely to be in the IB
                    if (letters >= 4) {
                        if (highPrice > ibHigh) ibHigh = highPrice;
                        if (lowPrice < ibLow) ibLow = lowPrice;
                    }
                }
            }

            // Fallback: use POC area if no IB detected
            if (Float.isInfinite(ibHigh) || Float.isInfinite(ibLow)) {
                Float poc = tpoNode.has("poc") ? (float) tpoNode.path("poc").asDouble() : null;
                if (poc != null) {
                    ibHigh = poc + rowSize * 5;
                    ibLow = poc - rowSize * 5;
                }
            }

            if (!Float.isInfinite(ibHigh) && !Float.isInfinite(ibLow)) {
                profile.setInitialBalance(ibHigh, ibLow);
            }
        }

        return profile;
    }
}
