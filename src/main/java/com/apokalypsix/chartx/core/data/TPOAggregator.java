package com.apokalypsix.chartx.core.data;

import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.core.data.model.TPOProfile;
import com.apokalypsix.chartx.core.data.model.TPOSeries;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Aggregates OHLC data into TPO (Time Price Opportunity) profiles.
 *
 * <p>Creates Market Profile / TPO charts by:
 * <ol>
 *   <li>Detecting session boundaries (daily/custom)</li>
 *   <li>Dividing each session into TPO periods (typically 30 minutes)</li>
 *   <li>Recording which prices were touched in each period</li>
 *   <li>Computing POC, Value Area, Initial Balance</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * TPOAggregator aggregator = new TPOAggregator(0.25f); // 0.25 tick size
 * aggregator.setSessionHours(9, 30, 16, 0); // 9:30 AM - 4:00 PM
 * TPOSeries tpoSeries = aggregator.aggregate(ohlcSeries);
 * }</pre>
 */
public class TPOAggregator {

    private final float tickSize;

    // Session configuration
    private int sessionStartHour = 9;
    private int sessionStartMinute = 30;
    private int sessionEndHour = 16;
    private int sessionEndMinute = 0;

    // TPO period duration (default 30 minutes)
    private long tpoPeriodMillis = 30 * 60 * 1000;

    // Initial balance periods (typically first hour = 2 periods of 30 min)
    private int ibPeriods = 2;

    // Timezone for session detection
    private TimeZone timezone = TimeZone.getDefault();

    /**
     * Creates a TPO aggregator with the specified tick size.
     *
     * @param tickSize the price tick size for bucketing
     */
    public TPOAggregator(float tickSize) {
        this.tickSize = tickSize;
    }

    /**
     * Sets the trading session hours.
     *
     * @param startHour session start hour (0-23)
     * @param startMinute session start minute (0-59)
     * @param endHour session end hour (0-23)
     * @param endMinute session end minute (0-59)
     */
    public void setSessionHours(int startHour, int startMinute, int endHour, int endMinute) {
        this.sessionStartHour = startHour;
        this.sessionStartMinute = startMinute;
        this.sessionEndHour = endHour;
        this.sessionEndMinute = endMinute;
    }

    /**
     * Configures the aggregator for continuous 24-hour sessions.
     * This is suitable for cryptocurrency markets that trade 24/7.
     *
     * <p>Sets the session to run from midnight (00:00) to midnight (24:00),
     * ensuring no data is filtered out. Each calendar day becomes one TPO profile.
     *
     * <p>Recommended to also call {@link #setTimezone(TimeZone)} with UTC:
     * <pre>{@code
     * aggregator.setContinuous24h();
     * aggregator.setTimezone(TimeZone.getTimeZone("UTC"));
     * }</pre>
     */
    public void setContinuous24h() {
        this.sessionStartHour = 0;
        this.sessionStartMinute = 0;
        this.sessionEndHour = 24;
        this.sessionEndMinute = 0;
    }

    /**
     * Sets the TPO period duration.
     *
     * @param millis period duration in milliseconds
     */
    public void setTpoPeriodMillis(long millis) {
        this.tpoPeriodMillis = millis;
    }

    /**
     * Sets the number of periods for initial balance.
     */
    public void setIBPeriods(int periods) {
        this.ibPeriods = periods;
    }

    /**
     * Sets the timezone for session detection.
     */
    public void setTimezone(TimeZone timezone) {
        this.timezone = timezone;
    }

    /**
     * Aggregates an OHLC series into TPO profiles.
     *
     * @param source the source OHLC data
     * @return a TPOSeries containing one profile per session
     */
    public TPOSeries aggregate(OhlcData source) {
        TPOSeries result = new TPOSeries(
                source.getId() + "_tpo",
                source.getName() + " TPO",
                tickSize
        );
        result.setTpoPeriodMillis(tpoPeriodMillis);

        if (source.isEmpty()) {
            return result;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] opens = source.getOpenArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        Calendar cal = Calendar.getInstance(timezone);
        TPOProfile currentProfile = null;
        long currentSessionStart = -1;
        long currentSessionEnd = -1;

        for (int i = 0; i < size; i++) {
            long timestamp = timestamps[i];

            // Check if this is a new session
            cal.setTimeInMillis(timestamp);
            long sessionStart = getSessionStart(cal);
            long sessionEnd = getSessionEnd(cal);

            // Skip bars outside session hours
            if (timestamp < sessionStart || timestamp >= sessionEnd) {
                continue;
            }

            if (currentProfile == null || sessionStart != currentSessionStart) {
                // New session - save previous profile
                if (currentProfile != null) {
                    result.append(currentProfile);
                }

                // Create new profile
                currentSessionStart = sessionStart;
                currentSessionEnd = sessionEnd;
                currentProfile = new TPOProfile(sessionStart, sessionEnd, tickSize, tpoPeriodMillis);
                currentProfile.setIBPeriods(ibPeriods);
            }

            // Calculate period index
            int periodIndex = (int) ((timestamp - sessionStart) / tpoPeriodMillis);

            // Add TPOs for this bar's price range
            currentProfile.addTPORange(highs[i], lows[i], periodIndex);

            // Track open/close
            if (periodIndex == 0 && Float.isNaN(currentProfile.getOpenPrice())) {
                currentProfile.setOpenPrice(opens[i]);
            }
            currentProfile.setClosePrice(closes[i]);

            // Update initial balance
            if (periodIndex < ibPeriods) {
                float ibHigh = currentProfile.getIBHigh();
                float ibLow = currentProfile.getIBLow();

                if (Float.isNaN(ibHigh) || highs[i] > ibHigh) {
                    currentProfile.setInitialBalance(highs[i],
                            Float.isNaN(ibLow) ? lows[i] : Math.min(ibLow, lows[i]));
                }
                if (Float.isNaN(ibLow) || lows[i] < ibLow) {
                    currentProfile.setInitialBalance(
                            Float.isNaN(ibHigh) ? highs[i] : Math.max(ibHigh, highs[i]),
                            lows[i]);
                }
            }
        }

        // Don't forget the last profile
        if (currentProfile != null) {
            result.append(currentProfile);
        }

        return result;
    }

    /**
     * Builds a single TPO profile for a specific time range.
     *
     * @param source the source OHLC data
     * @param startTime profile start timestamp
     * @param endTime profile end timestamp
     * @return the TPO profile
     */
    public TPOProfile buildProfile(OhlcData source, long startTime, long endTime) {
        TPOProfile profile = new TPOProfile(startTime, endTime, tickSize, tpoPeriodMillis);
        profile.setIBPeriods(ibPeriods);

        if (source.isEmpty()) {
            return profile;
        }

        int firstIdx = source.indexAtOrAfter(startTime);
        int lastIdx = source.indexAtOrBefore(endTime);

        if (firstIdx < 0 || lastIdx < 0 || firstIdx > lastIdx) {
            return profile;
        }

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] opens = source.getOpenArray();
        float[] closes = source.getCloseArray();

        for (int i = firstIdx; i <= lastIdx; i++) {
            long timestamp = timestamps[i];

            if (timestamp < startTime || timestamp >= endTime) {
                continue;
            }

            int periodIndex = (int) ((timestamp - startTime) / tpoPeriodMillis);
            profile.addTPORange(highs[i], lows[i], periodIndex);

            // Track open/close
            if (periodIndex == 0 && Float.isNaN(profile.getOpenPrice())) {
                profile.setOpenPrice(opens[i]);
            }
            profile.setClosePrice(closes[i]);

            // Update initial balance
            if (periodIndex < ibPeriods) {
                updateIB(profile, highs[i], lows[i]);
            }
        }

        return profile;
    }

    /**
     * Updates a profile with new data (for real-time updates).
     *
     * @param profile the profile to update
     * @param source the source OHLC data
     * @param fromIndex the index to start from
     */
    public void updateProfile(TPOProfile profile, OhlcData source, int fromIndex) {
        if (source.isEmpty() || fromIndex >= source.size()) {
            return;
        }

        long startTime = profile.getSessionStart();
        long endTime = profile.getSessionEnd();

        long[] timestamps = source.getTimestampsArray();
        float[] highs = source.getHighArray();
        float[] lows = source.getLowArray();
        float[] closes = source.getCloseArray();
        int size = source.size();

        for (int i = fromIndex; i < size; i++) {
            long timestamp = timestamps[i];

            if (timestamp < startTime || timestamp >= endTime) {
                continue;
            }

            int periodIndex = (int) ((timestamp - startTime) / tpoPeriodMillis);
            profile.addTPORange(highs[i], lows[i], periodIndex);
            profile.setClosePrice(closes[i]);

            if (periodIndex < ibPeriods) {
                updateIB(profile, highs[i], lows[i]);
            }
        }
    }

    private void updateIB(TPOProfile profile, float high, float low) {
        float ibHigh = profile.getIBHigh();
        float ibLow = profile.getIBLow();

        float newHigh = Float.isNaN(ibHigh) ? high : Math.max(ibHigh, high);
        float newLow = Float.isNaN(ibLow) ? low : Math.min(ibLow, low);
        profile.setInitialBalance(newHigh, newLow);
    }

    /**
     * Calculates session start time for a given calendar time.
     */
    private long getSessionStart(Calendar cal) {
        Calendar sessionCal = (Calendar) cal.clone();
        sessionCal.set(Calendar.HOUR_OF_DAY, sessionStartHour);
        sessionCal.set(Calendar.MINUTE, sessionStartMinute);
        sessionCal.set(Calendar.SECOND, 0);
        sessionCal.set(Calendar.MILLISECOND, 0);
        return sessionCal.getTimeInMillis();
    }

    /**
     * Calculates session end time for a given calendar time.
     */
    private long getSessionEnd(Calendar cal) {
        Calendar sessionCal = (Calendar) cal.clone();
        sessionCal.set(Calendar.HOUR_OF_DAY, sessionEndHour);
        sessionCal.set(Calendar.MINUTE, sessionEndMinute);
        sessionCal.set(Calendar.SECOND, 0);
        sessionCal.set(Calendar.MILLISECOND, 0);

        // If end hour is less than start hour, it's the next day
        if (sessionEndHour < sessionStartHour ||
                (sessionEndHour == sessionStartHour && sessionEndMinute < sessionStartMinute)) {
            sessionCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return sessionCal.getTimeInMillis();
    }

    /**
     * Returns the configured session duration in milliseconds.
     */
    public long getSessionDuration() {
        int startMinutes = sessionStartHour * 60 + sessionStartMinute;
        int endMinutes = sessionEndHour * 60 + sessionEndMinute;

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60; // Crosses midnight
        }

        return (endMinutes - startMinutes) * 60 * 1000L;
    }

    /**
     * Returns the number of TPO periods per session.
     */
    public int getPeriodsPerSession() {
        return (int) (getSessionDuration() / tpoPeriodMillis);
    }
}
