package com.apokalypsix.chartx.core.overlay;

import com.apokalypsix.chartx.chart.style.LineStyle;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.overlay.Drawing;
import com.apokalypsix.chartx.chart.overlay.Rectangle;
import com.apokalypsix.chartx.chart.overlay.VerticalLine;

import java.awt.Color;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates annotations for trading session boundaries.
 *
 * <p>Creates visual markers for:
 * <ul>
 *   <li>Session start/end lines</li>
 *   <li>Pre-market and after-hours regions</li>
 *   <li>Opening range boxes (first N minutes)</li>
 *   <li>Initial Balance (IB) periods</li>
 * </ul>
 *
 * <p>Supports configurable session times for different markets.
 */
public class SessionBoundaryGenerator implements AnnotationGenerator {

    public static final String ID = "sessionBoundaries";

    /**
     * Defines a trading session.
     */
    public record SessionDefinition(
            String name,
            LocalTime startTime,
            LocalTime endTime,
            Color lineColor,
            Color fillColor,
            boolean showLines,
            boolean showFill
    ) {
        /**
         * Creates a session with just start/end times.
         */
        public SessionDefinition(String name, LocalTime start, LocalTime end) {
            this(name, start, end, new Color(100, 100, 100), null, true, false);
        }

        /**
         * Creates a session with colored fill.
         */
        public static SessionDefinition withFill(String name, LocalTime start, LocalTime end, Color fill) {
            return new SessionDefinition(name, start, end, null, fill, false, true);
        }
    }

    // Pre-defined sessions
    public static final SessionDefinition US_REGULAR_SESSION =
            new SessionDefinition("RTH", LocalTime.of(9, 30), LocalTime.of(16, 0));

    public static final SessionDefinition US_PREMARKET =
            SessionDefinition.withFill("Pre-Market",
                    LocalTime.of(4, 0), LocalTime.of(9, 30),
                    new Color(50, 50, 80, 50));

    public static final SessionDefinition US_AFTERHOURS =
            SessionDefinition.withFill("After-Hours",
                    LocalTime.of(16, 0), LocalTime.of(20, 0),
                    new Color(50, 50, 80, 50));

    public static final SessionDefinition US_OPENING_RANGE =
            SessionDefinition.withFill("Opening Range",
                    LocalTime.of(9, 30), LocalTime.of(9, 35),
                    new Color(100, 150, 100, 40));

    public static final SessionDefinition US_INITIAL_BALANCE =
            SessionDefinition.withFill("Initial Balance",
                    LocalTime.of(9, 30), LocalTime.of(10, 30),
                    new Color(100, 100, 150, 30));

    private boolean enabled = true;
    private ZoneId timezone = ZoneId.of("America/New_York");
    private List<SessionDefinition> sessions = new ArrayList<>();
    private LineStyle lineStyle = LineStyle.dashed(new Color(80, 80, 80), 1.0f);

    // Initial Balance tracking
    private boolean showInitialBalance = false;
    private int initialBalanceMinutes = 60;

    public SessionBoundaryGenerator() {
        // Default: show US regular session boundaries
        sessions.add(US_REGULAR_SESSION);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Session Boundaries";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the timezone for session time calculations.
     */
    public void setTimezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    /**
     * Adds a session definition.
     */
    public void addSession(SessionDefinition session) {
        sessions.add(session);
    }

    /**
     * Removes a session by name.
     */
    public void removeSession(String name) {
        sessions.removeIf(s -> s.name().equals(name));
    }

    /**
     * Clears all session definitions.
     */
    public void clearSessions() {
        sessions.clear();
    }

    /**
     * Sets whether to show Initial Balance boxes.
     */
    public void setShowInitialBalance(boolean show) {
        this.showInitialBalance = show;
    }

    /**
     * Sets the Initial Balance duration in minutes.
     */
    public void setInitialBalanceMinutes(int minutes) {
        this.initialBalanceMinutes = minutes;
    }

    /**
     * Configures for US equity market sessions.
     */
    public void configureUSEquity() {
        sessions.clear();
        sessions.add(US_REGULAR_SESSION);
        sessions.add(US_PREMARKET);
        sessions.add(US_AFTERHOURS);
        timezone = ZoneId.of("America/New_York");
    }

    @Override
    public List<Drawing> generate(OhlcData series) {
        List<Drawing> annotations = new ArrayList<>();

        if (!enabled || series == null || series.isEmpty() || sessions.isEmpty()) {
            return annotations;
        }

        long[] timestamps = series.getTimestampsArray();
        float[] highs = series.getHighArray();
        float[] lows = series.getLowArray();
        int size = series.size();

        // Find all unique days in the data
        List<LocalDate> tradingDays = new ArrayList<>();
        LocalDate lastDate = null;

        for (int i = 0; i < size; i++) {
            LocalDate date = Instant.ofEpochMilli(timestamps[i])
                    .atZone(timezone)
                    .toLocalDate();
            if (!date.equals(lastDate)) {
                tradingDays.add(date);
                lastDate = date;
            }
        }

        // Generate annotations for each day and session
        int annotationCount = 0;
        for (LocalDate day : tradingDays) {
            for (SessionDefinition session : sessions) {
                // Calculate session timestamps for this day
                ZonedDateTime sessionStart = ZonedDateTime.of(day, session.startTime(), timezone);
                ZonedDateTime sessionEnd = ZonedDateTime.of(day, session.endTime(), timezone);

                // Handle overnight sessions
                if (session.endTime().isBefore(session.startTime())) {
                    sessionEnd = sessionEnd.plusDays(1);
                }

                long startMillis = sessionStart.toInstant().toEpochMilli();
                long endMillis = sessionEnd.toInstant().toEpochMilli();

                // Check if session is within data range
                if (startMillis > timestamps[size - 1] || endMillis < timestamps[0]) {
                    continue;
                }

                // Generate session start line
                if (session.showLines() && session.lineColor() != null) {
                    String startId = String.format("session-%s-start-%d", session.name(), annotationCount);
                    VerticalLine startLine = new VerticalLine(startId, startMillis, 0);
                    startLine.setLineStyle(lineStyle.withColor(session.lineColor()));
                    annotations.add(startLine);

                    String endId = String.format("session-%s-end-%d", session.name(), annotationCount);
                    VerticalLine endLine = new VerticalLine(endId, endMillis, 0);
                    endLine.setLineStyle(lineStyle.withColor(session.lineColor()));
                    annotations.add(endLine);
                }

                // Generate session fill (as a rectangle)
                if (session.showFill() && session.fillColor() != null) {
                    // Find price range within session
                    float sessionHigh = Float.MIN_VALUE;
                    float sessionLow = Float.MAX_VALUE;

                    for (int i = 0; i < size; i++) {
                        if (timestamps[i] >= startMillis && timestamps[i] < endMillis) {
                            sessionHigh = Math.max(sessionHigh, highs[i]);
                            sessionLow = Math.min(sessionLow, lows[i]);
                        }
                    }

                    if (sessionHigh > sessionLow) {
                        String rectId = String.format("session-%s-fill-%d", session.name(), annotationCount);
                        Rectangle rect = new Rectangle(rectId, startMillis, sessionHigh);
                        rect.setCorner2(new com.apokalypsix.chartx.chart.overlay.AnchorPoint(
                                endMillis, sessionLow));
                        rect.setColor(session.fillColor());
                        rect.setOpacity(session.fillColor().getAlpha() / 255f);
                        annotations.add(rect);
                    }
                }

                annotationCount++;
            }

            // Generate Initial Balance box if enabled
            if (showInitialBalance) {
                annotations.addAll(generateInitialBalance(series, day));
            }
        }

        return annotations;
    }

    private List<Drawing> generateInitialBalance(OhlcData series, LocalDate day) {
        List<Drawing> annotations = new ArrayList<>();

        ZonedDateTime ibStart = ZonedDateTime.of(day, LocalTime.of(9, 30), timezone);
        ZonedDateTime ibEnd = ibStart.plusMinutes(initialBalanceMinutes);

        long startMillis = ibStart.toInstant().toEpochMilli();
        long endMillis = ibEnd.toInstant().toEpochMilli();

        long[] timestamps = series.getTimestampsArray();
        float[] highs = series.getHighArray();
        float[] lows = series.getLowArray();
        int size = series.size();

        // Find IB high and low
        float ibHigh = Float.MIN_VALUE;
        float ibLow = Float.MAX_VALUE;

        for (int i = 0; i < size; i++) {
            if (timestamps[i] >= startMillis && timestamps[i] < endMillis) {
                ibHigh = Math.max(ibHigh, highs[i]);
                ibLow = Math.min(ibLow, lows[i]);
            }
        }

        if (ibHigh > ibLow) {
            // Create IB rectangle (extends to end of session)
            String rectId = "ib-" + day.toString();
            ZonedDateTime sessionEnd = ZonedDateTime.of(day, LocalTime.of(16, 0), timezone);
            long sessionEndMillis = sessionEnd.toInstant().toEpochMilli();

            Rectangle rect = new Rectangle(rectId, startMillis, ibHigh);
            rect.setCorner2(new com.apokalypsix.chartx.chart.overlay.AnchorPoint(
                    sessionEndMillis, ibLow));
            rect.setColor(new Color(100, 100, 150, 30));
            rect.setOpacity(0.3f);
            annotations.add(rect);
        }

        return annotations;
    }
}
