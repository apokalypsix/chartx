package com.apokalypsix.chartx.chart.overlay.region;

import java.awt.Color;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Generator for alternating background regions.
 *
 * <p>Creates time range regions with alternating colors based on:
 * <ul>
 *   <li>Fixed time intervals (e.g., every hour, day, or week)</li>
 *   <li>Trading sessions (RTH/ETH or custom session times)</li>
 *   <li>Custom patterns defined by start/end times</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create alternating daily backgrounds
 * AlternatingBackground.daily(
 *     startTime, endTime,
 *     new Color(30, 32, 36),      // Even day color
 *     new Color(35, 37, 42),      // Odd day color
 *     ZoneId.of("America/New_York")
 * ).forEach(regionLayer::addRegion);
 *
 * // Create session-based alternating backgrounds
 * AlternatingBackground.sessions(
 *     startTime, endTime,
 *     LocalTime.of(9, 30), LocalTime.of(16, 0),  // Session times
 *     new Color(25, 30, 25, 40),  // Session color
 *     new Color(25, 25, 30, 40),  // Off-session color
 *     ZoneId.of("America/New_York")
 * ).forEach(regionLayer::addRegion);
 * }</pre>
 */
public class AlternatingBackground {

    /** Interval types for alternating backgrounds */
    public enum Interval {
        HOURLY,
        DAILY,
        WEEKLY
    }

    private AlternatingBackground() {}

    // ========== Fixed interval alternating backgrounds ==========

    /**
     * Creates alternating backgrounds at hourly intervals.
     */
    public static List<TimeRangeRegion> hourly(long startTime, long endTime,
                                                Color evenColor, Color oddColor,
                                                ZoneId zone) {
        return byInterval(startTime, endTime, Interval.HOURLY, evenColor, oddColor, zone);
    }

    /**
     * Creates alternating backgrounds at daily intervals.
     */
    public static List<TimeRangeRegion> daily(long startTime, long endTime,
                                               Color evenColor, Color oddColor,
                                               ZoneId zone) {
        return byInterval(startTime, endTime, Interval.DAILY, evenColor, oddColor, zone);
    }

    /**
     * Creates alternating backgrounds at weekly intervals.
     */
    public static List<TimeRangeRegion> weekly(long startTime, long endTime,
                                                Color evenColor, Color oddColor,
                                                ZoneId zone) {
        return byInterval(startTime, endTime, Interval.WEEKLY, evenColor, oddColor, zone);
    }

    /**
     * Creates alternating backgrounds at the specified interval.
     *
     * @param startTime range start in milliseconds
     * @param endTime range end in milliseconds
     * @param interval interval type
     * @param evenColor color for even intervals
     * @param oddColor color for odd intervals
     * @param zone timezone for interval calculations
     * @return list of alternating regions
     */
    public static List<TimeRangeRegion> byInterval(long startTime, long endTime,
                                                    Interval interval,
                                                    Color evenColor, Color oddColor,
                                                    ZoneId zone) {
        List<TimeRangeRegion> regions = new ArrayList<>();

        ZonedDateTime start = Instant.ofEpochMilli(startTime).atZone(zone);
        ZonedDateTime end = Instant.ofEpochMilli(endTime).atZone(zone);

        // Align to interval boundary
        ZonedDateTime current = alignToInterval(start, interval);
        int count = 0;

        while (current.toInstant().toEpochMilli() < endTime) {
            ZonedDateTime next = advanceInterval(current, interval);

            long regionStart = Math.max(current.toInstant().toEpochMilli(), startTime);
            long regionEnd = Math.min(next.toInstant().toEpochMilli(), endTime);

            if (regionEnd > regionStart) {
                Color color = (count % 2 == 0) ? evenColor : oddColor;
                regions.add(new TimeRangeRegion(regionStart, regionEnd, color));
            }

            current = next;
            count++;
        }

        return regions;
    }

    // ========== Session-based alternating backgrounds ==========

    /**
     * Creates alternating backgrounds based on trading sessions.
     *
     * @param startTime range start in milliseconds
     * @param endTime range end in milliseconds
     * @param sessionStart session start time (e.g., 9:30 for NYSE open)
     * @param sessionEnd session end time (e.g., 16:00 for NYSE close)
     * @param sessionColor color during session (RTH)
     * @param offSessionColor color outside session (ETH/overnight)
     * @param zone timezone for session calculations
     * @return list of alternating regions
     */
    public static List<TimeRangeRegion> sessions(long startTime, long endTime,
                                                  LocalTime sessionStart, LocalTime sessionEnd,
                                                  Color sessionColor, Color offSessionColor,
                                                  ZoneId zone) {
        List<TimeRangeRegion> regions = new ArrayList<>();

        ZonedDateTime start = Instant.ofEpochMilli(startTime).atZone(zone);
        ZonedDateTime end = Instant.ofEpochMilli(endTime).atZone(zone);

        // Start at the beginning of the first day
        LocalDate currentDate = start.toLocalDate();
        LocalDate lastDate = end.toLocalDate().plusDays(1);

        while (!currentDate.isAfter(lastDate)) {
            // Calculate session times for this day
            ZonedDateTime daySessionStart = ZonedDateTime.of(currentDate, sessionStart, zone);
            ZonedDateTime daySessionEnd = ZonedDateTime.of(currentDate, sessionEnd, zone);

            // Pre-session (overnight / ETH morning)
            if (sessionStart.isAfter(LocalTime.MIDNIGHT)) {
                ZonedDateTime dayStart = currentDate.atStartOfDay(zone);
                long preStart = Math.max(dayStart.toInstant().toEpochMilli(), startTime);
                long preEnd = Math.min(daySessionStart.toInstant().toEpochMilli(), endTime);

                if (preEnd > preStart) {
                    regions.add(new TimeRangeRegion(preStart, preEnd, offSessionColor, "ETH"));
                }
            }

            // Session (RTH)
            long rthStart = Math.max(daySessionStart.toInstant().toEpochMilli(), startTime);
            long rthEnd = Math.min(daySessionEnd.toInstant().toEpochMilli(), endTime);

            if (rthEnd > rthStart) {
                regions.add(new TimeRangeRegion(rthStart, rthEnd, sessionColor, "RTH"));
            }

            // Post-session (ETH evening)
            if (sessionEnd.isBefore(LocalTime.MAX)) {
                ZonedDateTime nextDayStart = currentDate.plusDays(1).atStartOfDay(zone);
                long postStart = Math.max(daySessionEnd.toInstant().toEpochMilli(), startTime);
                long postEnd = Math.min(nextDayStart.toInstant().toEpochMilli(), endTime);

                if (postEnd > postStart) {
                    regions.add(new TimeRangeRegion(postStart, postEnd, offSessionColor, "ETH"));
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return regions;
    }

    /**
     * Creates alternating backgrounds for US stock market sessions (NYSE/NASDAQ).
     * RTH: 9:30 AM - 4:00 PM Eastern
     */
    public static List<TimeRangeRegion> usEquitySessions(long startTime, long endTime,
                                                          Color rthColor, Color ethColor) {
        return sessions(startTime, endTime,
                LocalTime.of(9, 30), LocalTime.of(16, 0),
                rthColor, ethColor,
                ZoneId.of("America/New_York"));
    }

    /**
     * Creates alternating backgrounds for US futures sessions (CME).
     * RTH: 9:30 AM - 4:00 PM Eastern (pit session approximation)
     */
    public static List<TimeRangeRegion> usFuturesSessions(long startTime, long endTime,
                                                           Color rthColor, Color ethColor) {
        return sessions(startTime, endTime,
                LocalTime.of(9, 30), LocalTime.of(16, 0),
                rthColor, ethColor,
                ZoneId.of("America/New_York"));
    }

    /**
     * Creates alternating backgrounds for European sessions (LSE, Euronext).
     * RTH: 8:00 AM - 4:30 PM London time
     */
    public static List<TimeRangeRegion> europeanSessions(long startTime, long endTime,
                                                          Color rthColor, Color ethColor) {
        return sessions(startTime, endTime,
                LocalTime.of(8, 0), LocalTime.of(16, 30),
                rthColor, ethColor,
                ZoneId.of("Europe/London"));
    }

    /**
     * Creates alternating backgrounds for Asian sessions (Tokyo).
     * RTH: 9:00 AM - 3:00 PM Japan time (with lunch break ignored)
     */
    public static List<TimeRangeRegion> asianSessions(long startTime, long endTime,
                                                       Color rthColor, Color ethColor) {
        return sessions(startTime, endTime,
                LocalTime.of(9, 0), LocalTime.of(15, 0),
                rthColor, ethColor,
                ZoneId.of("Asia/Tokyo"));
    }

    // ========== Weekend highlighting ==========

    /**
     * Creates regions highlighting weekends.
     *
     * @param startTime range start in milliseconds
     * @param endTime range end in milliseconds
     * @param weekendColor color for weekend periods
     * @param zone timezone for weekend calculations
     * @return list of weekend regions
     */
    public static List<TimeRangeRegion> weekends(long startTime, long endTime,
                                                  Color weekendColor, ZoneId zone) {
        List<TimeRangeRegion> regions = new ArrayList<>();

        ZonedDateTime start = Instant.ofEpochMilli(startTime).atZone(zone);
        ZonedDateTime end = Instant.ofEpochMilli(endTime).atZone(zone);

        LocalDate currentDate = start.toLocalDate();
        LocalDate lastDate = end.toLocalDate().plusDays(1);

        while (!currentDate.isAfter(lastDate)) {
            DayOfWeek day = currentDate.getDayOfWeek();

            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                ZonedDateTime dayStart = currentDate.atStartOfDay(zone);
                ZonedDateTime dayEnd = currentDate.plusDays(1).atStartOfDay(zone);

                long regionStart = Math.max(dayStart.toInstant().toEpochMilli(), startTime);
                long regionEnd = Math.min(dayEnd.toInstant().toEpochMilli(), endTime);

                if (regionEnd > regionStart) {
                    String label = day == DayOfWeek.SATURDAY ? "Sat" : "Sun";
                    regions.add(new TimeRangeRegion(regionStart, regionEnd, weekendColor, label));
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return regions;
    }

    // ========== Helper methods ==========

    private static ZonedDateTime alignToInterval(ZonedDateTime dt, Interval interval) {
        return switch (interval) {
            case HOURLY -> dt.withMinute(0).withSecond(0).withNano(0);
            case DAILY -> dt.toLocalDate().atStartOfDay(dt.getZone());
            case WEEKLY -> {
                LocalDate monday = dt.toLocalDate().minusDays(dt.getDayOfWeek().getValue() - 1L);
                yield monday.atStartOfDay(dt.getZone());
            }
        };
    }

    private static ZonedDateTime advanceInterval(ZonedDateTime dt, Interval interval) {
        return switch (interval) {
            case HOURLY -> dt.plusHours(1);
            case DAILY -> dt.plusDays(1);
            case WEEKLY -> dt.plusWeeks(1);
        };
    }
}
