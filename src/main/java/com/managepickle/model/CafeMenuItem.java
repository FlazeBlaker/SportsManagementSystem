package com.managepickle.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CafeMenuItem {
    private String name;
    private double price;
    private String category; // e.g., "Beverage", "Snack"
}
