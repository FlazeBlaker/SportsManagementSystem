package com.managepickle.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOption {
    private String name;
    private double price;
    private RentalType type;

    public enum RentalType {
        HOURLY,
        FLAT
    }
}
