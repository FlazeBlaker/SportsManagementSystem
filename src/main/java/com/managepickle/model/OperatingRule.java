package com.managepickle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingRule {
    private String name; // e.g. "Weekdays", "Weekends"
    private List<DayOfWeek> days; // [MONDAY, TUESDAY, ...]
    private String openTime; // "06:00"
    private String closeTime; // "23:00"
    private boolean is24Hours;
    private boolean isClosed;

    // Safety check for logic
    public boolean appliesTo(DayOfWeek day) {
        return days != null && days.contains(day);
    }
}
