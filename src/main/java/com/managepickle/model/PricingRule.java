package com.managepickle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.util.List;
import java.time.DayOfWeek;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {
    private String name; // e.g. "WeekendRate"
    private double rate;
    private List<DayOfWeek> days; // e.g. [SATURDAY, SUNDAY]
    private String startTime; // "06:00"
    private String endTime; // "23:00"

    // Helper accessors
    public LocalTime getStartLocalTime() {
        return LocalTime.parse(startTime);
    }

    public LocalTime getEndLocalTime() {
        return LocalTime.parse(endTime);
    }
}
