package com.apokalypsix.chartx.core.overlay;

import com.apokalypsix.chartx.chart.style.LineStyle;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.overlay.Drawing;
import com.apokalypsix.chartx.chart.overlay.VerticalLine;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates vertical separator lines at day boundaries.
 *
 * <p>Creates dashed vertical lines at the start of each trading day,
 * making it easy to visualize day-over-day price action.
 *
 * <p>Supports configurable:
 * <ul>
 *   <li>Timezone for day boundary calculation</li>
 *   <li>Line style (color, dash pattern, opacity)</li>
 *   <li>Weekday-only mode (skip weekends)</li>
 * </ul>
 */
public class DaySeparatorGenerator implements AnnotationGenerator {

    public static final String ID = "daySeparators";

    private boolean enabled = true;
    private ZoneId timezone = ZoneId.of("America/New_York");
    private LineStyle lineStyle = LineStyle.dashed(new Color(100, 100, 100, 180), 1.0f);
    private boolean skipWeekends = false;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Day Separators";
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
     * Sets the timezone for day boundary calculation.
     */
    public void setTimezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    /**
     * Returns the current timezone.
     */
    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * Sets the line style for separator lines.
     */
    public void setLineStyle(LineStyle style) {
        this.lineStyle = style;
    }

    /**
     * Returns the current line style.
     */
    public LineStyle getLineStyle() {
        return lineStyle;
    }

    /**
     * Sets whether to skip weekend day boundaries.
     */
    public void setSkipWeekends(boolean skip) {
        this.skipWeekends = skip;
    }

    @Override
    public List<Drawing> generate(OhlcData data) {
        List<Drawing> lines = new ArrayList<>();

        if (!enabled || data == null || data.isEmpty()) {
            return lines;
        }

        long[] timestamps = data.getTimestampsArray();
        int size = data.size();

        LocalDate previousDate = null;
        int separatorCount = 0;

        for (int i = 0; i < size; i++) {
            ZonedDateTime dateTime = Instant.ofEpochMilli(timestamps[i])
                    .atZone(timezone);
            LocalDate date = dateTime.toLocalDate();

            if (!date.equals(previousDate)) {
                // Check if we should skip weekends
                if (skipWeekends) {
                    int dayOfWeek = date.getDayOfWeek().getValue();
                    if (dayOfWeek == 6 || dayOfWeek == 7) {  // Saturday or Sunday
                        previousDate = date;
                        continue;
                    }
                }

                if (previousDate != null) {
                    // Create separator line at this timestamp
                    String id = "daysep-" + separatorCount++;
                    VerticalLine line = new VerticalLine(id, timestamps[i], 0);
                    line.setLineStyle(lineStyle);
                    lines.add(line);
                }
                previousDate = date;
            }
        }

        return lines;
    }
}
