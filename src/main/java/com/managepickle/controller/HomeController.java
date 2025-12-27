package com.managepickle.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {

    @FXML
    private Label revenueLabel;
    @FXML
    private Label bookingsLabel;
    @FXML
    private Label occupancyLabel;
    @FXML
    private Label occupancyTitleLabel;

    @FXML
    public void initialize() {
        // Load Resource Name
        com.managepickle.model.AppConfig config = com.managepickle.utils.ConfigManager.getConfig();
        String rPlural = config.getResourceNamePlural() != null ? config.getResourceNamePlural() : "Resources";

        if (occupancyTitleLabel != null)
            occupancyTitleLabel.setText(rPlural + " Occupancy");

        // Simple defaults - no database calls to avoid errors
        if (revenueLabel != null)
            revenueLabel.setText("$0.00");
        if (bookingsLabel != null)
            bookingsLabel.setText("0");
        if (occupancyLabel != null)
            occupancyLabel.setText("0%");

        System.out.println("HomeController initialized successfully");
    }
}
