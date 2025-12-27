package com.managepickle.model;

import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    private String openingTime;
    private String closingTime;
    private List<Court> courts;

    // Resource Branding
    private String resourceName; // e.g. "Court", "Pitch", "Lane"
    private String resourceNamePlural; // e.g. "Courts", "Pitches"
    private int minSlotDuration; // 30, 60, 90, 120
    private String venueLogoPath;

    // Theme Colors (Semantic System)
    private String brandColor; // Main brand identity (Buttons, Highlights)
    private String navbarColor; // Top navigation background
    private String panelColor; // Cards, Sidebars, Surface
    private String outlineColor; // Borders, Secondary accents
    private String textColor; // Main text color
    private String backgroundColor; // Global background

    // Dynamic Feature Configuration
    private String currencySymbol;
    private boolean hasCafe;
    private boolean hasRentals;
    private boolean hasProducts;
    private List<CafeMenuItem> cafeMenu;
    private List<RentalOption> rentalOptions;
    private List<ProductItem> productCatalog;

    // Complex Scheduling & Pricing
    private List<OperatingRule> operatingSchedule; // Replaces simple open/close strings
    private List<PricingRule> globalPricingRules; // Base Rate Card (Weekday, Weekend, etc)
}
