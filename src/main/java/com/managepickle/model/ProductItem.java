package com.managepickle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItem {
    private String name;
    private double price;
    private String category; // e.g., "Equipment", "Accessories"
}
