package com.managepickle.controller;

import com.managepickle.utils.ConfigManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SettingsController {

    // Theme Color Pickers
    @FXML
    private ColorPicker brandColorPicker;
    @FXML
    private ColorPicker outlineColorPicker;
    @FXML
    private ColorPicker navbarColorPicker;
    @FXML
    private ColorPicker backgroundColorPicker;
    @FXML
    private ColorPicker panelColorPicker;
    @FXML
    private ColorPicker textColorPicker;

    // Branding & Resources
    @FXML
    private javafx.scene.image.ImageView logoPreview;
    @FXML
    private javafx.scene.control.TextField resourceNameField;
    @FXML
    private javafx.scene.control.TextField resourceNamePluralField;
    @FXML
    private javafx.scene.control.ComboBox<Integer> minSlotDurationField;

    private String venueLogoPath;

    @FXML
    public void initialize() {
        // --- Load Theme Colors ---
        loadThemeColors();

        // --- Load branding and resources ---
        minSlotDurationField.getItems().addAll(30, 60, 90, 120);
        loadBrandingConfig();
    }

    private void loadBrandingConfig() {
        var config = ConfigManager.getConfig();
        resourceNameField.setText(config.getResourceName());
        resourceNamePluralField.setText(config.getResourceNamePlural());
        minSlotDurationField.setValue(config.getMinSlotDuration());
        this.venueLogoPath = config.getVenueLogoPath();

        if (venueLogoPath != null && !venueLogoPath.isEmpty()) {
            try {
                logoPreview.setImage(new javafx.scene.image.Image("file:" + venueLogoPath));
            } catch (Exception e) {
                System.err.println("Could not load logo preview: " + e.getMessage());
            }
        }
    }

    private void loadThemeColors() {
        var config = ConfigManager.getConfig();
        brandColorPicker.setValue(Color.web(config.getBrandColor()));
        outlineColorPicker.setValue(Color.web(config.getOutlineColor()));
        navbarColorPicker.setValue(Color.web(config.getNavbarColor()));
        backgroundColorPicker.setValue(Color.web(config.getBackgroundColor()));
        panelColorPicker.setValue(Color.web(config.getPanelColor()));
        textColorPicker.setValue(Color.web(config.getTextColor()));
    }

    @FXML
    private void handleSave() {
        // Save Theme Colors
        var config = ConfigManager.getConfig();
        config.setBrandColor(toHexString(brandColorPicker.getValue()));
        config.setOutlineColor(toHexString(outlineColorPicker.getValue()));
        config.setNavbarColor(toHexString(navbarColorPicker.getValue()));
        config.setBackgroundColor(toHexString(backgroundColorPicker.getValue()));
        config.setPanelColor(toHexString(panelColorPicker.getValue()));
        config.setTextColor(toHexString(textColorPicker.getValue()));

        // Save Branding
        config.setResourceName(resourceNameField.getText());
        config.setResourceNamePlural(resourceNamePluralField.getText());
        config.setMinSlotDuration(minSlotDurationField.getValue());
        config.setVenueLogoPath(venueLogoPath);

        System.out.println("Saving Config...");
        ConfigManager.saveConfig();

        // Auto-Restart Application
        com.managepickle.App.restart();
    }

    @FXML
    private void handleChangeLogo() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose Venue Logo");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        java.io.File selectedFile = fileChooser.showOpenDialog(logoPreview.getScene().getWindow());
        if (selectedFile != null) {
            this.venueLogoPath = selectedFile.getAbsolutePath();
            try {
                logoPreview.setImage(new javafx.scene.image.Image("file:" + venueLogoPath));
            } catch (Exception e) {
                System.err.println("Could not update logo preview: " + e.getMessage());
            }
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
