package com.managepickle.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperatingHours {
    private int startHour;
    private int endHour;
    private boolean isClosed;
    private boolean is24Hours;
}
