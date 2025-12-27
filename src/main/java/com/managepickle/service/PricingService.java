package com.managepickle.service;

import com.managepickle.model.AppConfig;
import com.managepickle.model.Court;
import com.managepickle.model.OperatingRule;
import com.managepickle.model.PricingRule;
import com.managepickle.utils.ConfigManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.managepickle.model.OperatingHours;
import java.util.List;

public class PricingService {

    /**
     * Calculates the total cost for a court booking duration considering all rules.
     */
    public static double calculateCourtCost(Court court, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start))
            return 0.0;

        AppConfig config = ConfigManager.getConfig();
        double totalCost = 0.0;

        // Iterate in 1-minute chunks for perfect granularity (especially for Happy
        // Hours)
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            double hourlyRate = getRateForTime(court, config, current);

            // Add cost for 1 minute (1/60th of an hour)
            totalCost += (hourlyRate / 60.0);

            current = current.plusMinutes(1);
        }

        return totalCost;
    }

    private static double getRateForTime(Court court, AppConfig config, LocalDateTime time) {
        DayOfWeek day = time.getDayOfWeek();
        LocalTime localTime = time.toLocalTime();
        double effectiveRate = -1.0;

        // 1. Check Global Rules (Base defaults)
        if (court.isUseGlobalRates() && config.getGlobalPricingRules() != null) {
            for (PricingRule rule : config.getGlobalPricingRules()) {
                if (matchesRule(rule, day, localTime)) {
                    effectiveRate = rule.getRate();
                }
            }
        }

        // 2. Check Court-Specific Rules (Overrides global)
        if (court.getPricingRules() != null) {
            for (PricingRule rule : court.getPricingRules()) {
                if (matchesRule(rule, day, localTime)) {
                    effectiveRate = rule.getRate();
                }
            }
        }

        // 3. Fallback to Court Base Rate if no rules matched
        if (effectiveRate < 0) {
            effectiveRate = court.getHourlyRate();
        }

        return effectiveRate;
    }

    private static boolean matchesRule(PricingRule rule, DayOfWeek day, LocalTime time) {
        // Check Day
        if (rule.getDays() == null || !rule.getDays().contains(day)) {
            return false;
        }

        // Check Time Range (Inclusive Start, Exclusive End)
        LocalTime start = LocalTime.parse(rule.getStartTime());
        LocalTime end = LocalTime.parse(rule.getEndTime());

        // Handle 00:00 (Midnight) as End of Day if used as an end time
        if (end.equals(LocalTime.MIN)) {
            return !time.isBefore(start); // From start until eternity (24:00)
        }

        return !time.isBefore(start) && time.isBefore(end);
    }

    /**
     * Checks if the venue is open at a specific time.
     */
    public static boolean isVenueOpen(LocalDateTime dateTime) {
        OperatingHours hours = getOperatingHours(dateTime.toLocalDate());
        if (hours.isClosed())
            return false;
        if (hours.is24Hours())
            return true;

        LocalTime time = dateTime.toLocalTime();
        LocalTime open = LocalTime.of(hours.getStartHour(), 0);
        LocalTime close = LocalTime.of(hours.getEndHour(), 0);

        return !time.isBefore(open) && time.isBefore(close);
    }

    public static OperatingHours getOperatingHours(LocalDate date) {
        AppConfig config = ConfigManager.getConfig();
        List<OperatingRule> schedule = config.getOperatingSchedule();

        // Default if no schedule set
        if (schedule == null || schedule.isEmpty()) {
            return OperatingHours.builder()
                    .startHour(6)
                    .endHour(23)
                    .isClosed(false)
                    .is24Hours(false)
                    .build();
        }

        DayOfWeek day = date.getDayOfWeek();
        for (OperatingRule rule : schedule) {
            if (rule.getDays() != null && rule.getDays().contains(day)) {
                if (rule.isClosed()) {
                    return OperatingHours.builder().isClosed(true).build();
                }
                if (rule.is24Hours()) {
                    return OperatingHours.builder().startHour(0).endHour(23).is24Hours(true).build();
                }

                int start = LocalTime.parse(rule.getOpenTime()).getHour();
                LocalTime close = LocalTime.parse(rule.getCloseTime());
                // If close is 00:00, treat as midnight (hour 24), so last slot starts at 23
                int end = close.equals(LocalTime.MIN) ? 23 : close.getHour();
                return OperatingHours.builder()
                        .startHour(start)
                        .endHour(end)
                        .isClosed(false)
                        .is24Hours(false)
                        .build();
            }
        }

        // Fallback: Closed if no rule matches
        return OperatingHours.builder().isClosed(true).build();
    }
}
