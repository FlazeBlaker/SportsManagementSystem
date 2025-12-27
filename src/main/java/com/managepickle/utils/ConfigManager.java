package com.managepickle.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.managepickle.model.AppConfig;
import com.managepickle.model.Court;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "managepickle_config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static AppConfig currentConfig;

    public static AppConfig getConfig() {
        if (currentConfig == null) {
            loadConfig();
        }
        return currentConfig;
    }

    public static void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                currentConfig = gson.fromJson(reader, AppConfig.class);
            } catch (Exception e) {
                logger.error("Failed to load config file: " + e.getMessage());
                e.printStackTrace(); // Print to stderr for debugging
                currentConfig = null; // Trigger default rollback
            }
        }

        // Use defaults if loading failed or file didn't exist
        if (currentConfig == null) {
            currentConfig = AppConfig.builder()
                    .openingTime("06:00")
                    .closingTime("23:00")
                    .courts(new ArrayList<>())
                    // Default Vibrant Theme Colors
                    .brandColor("#7C3AED") // Purple
                    .navbarColor("rgba(30, 41, 59, 0.85)") // Translucent Slate
                    .panelColor("#1E293B") // Dark Slate
                    .outlineColor("#10B981") // Green
                    .textColor("#FFFFFF") // White
                    .backgroundColor("#0F172A") // Dark Navy
                    .build();

            // Should we add default courts if list is empty?
            if (currentConfig.getCourts().isEmpty()) {
                List<Court> defaults = new ArrayList<>();
                defaults.add(Court.builder().id(1).name("Court 1").type("Indoor").hourlyRate(40.0).build());
                defaults.add(Court.builder().id(2).name("Court 2").type("Indoor").hourlyRate(40.0).build());
                defaults.add(Court.builder().id(3).name("Court 3").type("Outdoor").hourlyRate(30.0).build());
                currentConfig.setCourts(defaults);
                saveConfig(); // Save these defaults immediately
            }
        }

        // Ensure theme colors exist (for existing configs with old schema)
        boolean needsSave = false;

        if (currentConfig.getBrandColor() == null) {
            currentConfig.setBrandColor("#7C3AED");
            currentConfig.setNavbarColor("rgba(30, 41, 59, 0.85)");
            currentConfig.setPanelColor("#1E293B");
            currentConfig.setOutlineColor("#10B981");
            currentConfig.setTextColor("#FFFFFF");
            currentConfig.setBackgroundColor("#0F172A");
            needsSave = true;
        }

        // Initialize New Dynamic Features if missing
        if (currentConfig.getCurrencySymbol() == null) {
            currentConfig.setCurrencySymbol("₹");
            currentConfig.setHasCafe(true);
            currentConfig.setHasRentals(true);
            currentConfig.setHasProducts(true);

            currentConfig.setResourceName("Court");
            currentConfig.setResourceNamePlural("Courts");
            currentConfig.setMinSlotDuration(60);

            List<com.managepickle.model.CafeMenuItem> defaultMenu = new ArrayList<>();
            defaultMenu.add(new com.managepickle.model.CafeMenuItem("Water Bottle", 20.0, "Beverage"));
            defaultMenu.add(new com.managepickle.model.CafeMenuItem("Energy Drink", 100.0, "Beverage"));
            defaultMenu.add(new com.managepickle.model.CafeMenuItem("Protein Bar", 150.0, "Snack"));
            currentConfig.setCafeMenu(defaultMenu);

            List<com.managepickle.model.RentalOption> defaultRentals = new ArrayList<>();
            defaultRentals.add(new com.managepickle.model.RentalOption("Paddle", 100.0,
                    com.managepickle.model.RentalOption.RentalType.HOURLY));
            defaultRentals.add(new com.managepickle.model.RentalOption("Ball", 50.0,
                    com.managepickle.model.RentalOption.RentalType.FLAT));
            currentConfig.setRentalOptions(defaultRentals);
            needsSave = true;
        }

        if (needsSave) {
            saveConfig();
        }
    }

    public static void saveConfig() {
        if (currentConfig == null)
            return;

        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(currentConfig, writer);
        } catch (IOException e) {
            logger.error("Failed to save config file", e);
        }
    }

    public static void updateConfig(String open, String close, List<Court> courts) {
        if (currentConfig == null)
            loadConfig();
        currentConfig.setOpeningTime(open);
        currentConfig.setClosingTime(close);
        currentConfig.setCourts(courts);
        saveConfig();
    }
}
