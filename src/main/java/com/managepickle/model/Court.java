package com.managepickle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Court {
    private int id;
    private String name; // e.g., "Court 1"
    private String type; // e.g., "Indoor", "Outdoor"
    private String iconCode; // Ikonli code (e.g. mdi2f-football)
    private String environmentType; // Indoor, Outdoor, A/C
    private double hourlyRate; // Acts as Base Rate if no rules match
    private boolean useGlobalRates; // If true, look at AppConfig.globalPricingRules first
    private java.util.List<PricingRule> pricingRules; // Specific overrides for this court
}
