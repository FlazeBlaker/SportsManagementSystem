package com.managepickle.service;

import com.managepickle.model.AppConfig;
import com.managepickle.model.Court;
import com.managepickle.model.OperatingRule;
import com.managepickle.model.PricingRule;
import com.managepickle.utils.ConfigManager;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class PricingService {

    /**
     * Calculates the total cost for a court booking duration considering all rules.
     */
    public static double calculateCourtCost(Court court, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start))
            return 0.0;

        AppConfig config = ConfigManager.getConfig();
        double totalCost = 0.0;

        // We will Iterate in 30-minute chunks for granularity
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            // Find rate for this specific moment
            double hourlyRate = getRateForTime(court, config, current);

            // Add cost for 30 mins (0.5 hours)
            totalCost += (hourlyRate * 0.5);

            current = current.plusMinutes(30);
        }

        return totalCost;
    }

    private static double getRateForTime(Court court, AppConfig config, LocalDateTime time) {
        DayOfWeek day = time.getDayOfWeek();
        LocalTime localTime = time.toLocalTime();

        // 1. Check Court-Specific Rules
        if (court.getPricingRules() != null) {
            for (PricingRule rule : court.getPricingRules()) {
                if (matchesRule(rule, day, localTime)) {
                    return rule.getRate();
                }
            }
        }

        // 2. Check Global Rules (if enabled)
        if (court.isUseGlobalRates() && config.getGlobalPricingRules() != null) {
            for (PricingRule rule : config.getGlobalPricingRules()) {
                if (matchesRule(rule, day, localTime)) {
                    return rule.getRate();
                }
            }
        }

        // 3. Fallback to Base Rate
        return court.getHourlyRate();
    }

    private static boolean matchesRule(PricingRule rule, DayOfWeek day, LocalTime time) {
        // Check Day
        if (rule.getDays() != null && !rule.getDays().contains(day)) {
            return false;
        }

        // Check Time Range (Inclusive Start, Exclusive End)
        // Handle "cross-midnight" logic if needed, but for now assuming simple ranges
        // (06:00 to 23:00)
        LocalTime start = LocalTime.parse(rule.getStartTime());
        LocalTime end = LocalTime.parse(rule.getEndTime());

        return !time.isBefore(start) && time.isBefore(end);
    }

    /**
     * Checks if the venue is open at a specific time.
     */
    public static boolean isVenueOpen(LocalDateTime dateTime) {
        AppConfig config = ConfigManager.getConfig();
        if (config.getOperatingSchedule() == null || config.getOperatingSchedule().isEmpty()) {
            return true; // Default open if no rules
        }

        DayOfWeek day = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();

        for (OperatingRule rule : config.getOperatingSchedule()) {
            if (rule.getDays() != null && rule.getDays().contains(day)) {
                if (rule.is24Hours())
                    return true;

                LocalTime open = LocalTime.parse(rule.getOpenTime());
                LocalTime close = LocalTime.parse(rule.getCloseTime());

                if (!time.isBefore(open) && time.isBefore(close)) {
                    return true;
                }
            }
        }
        return false; // Closed if no matching rule says it's open
    }
}
