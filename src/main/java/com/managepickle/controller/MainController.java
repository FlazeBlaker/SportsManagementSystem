package com.managepickle.controller;

import com.managepickle.model.User;
import com.managepickle.model.AppConfig;
import com.managepickle.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;

public class MainController {

    @FXML
    private Button settingsButton;

    @FXML
    private StackPane contentArea;

    @FXML
    private StackPane modalContainer;

    private static MainController instance;

    @FXML
    private javafx.scene.control.Label brandLabel;

    @FXML
    public void initialize() {
        instance = this;

        // Set Brand Name
        AppConfig config = com.managepickle.utils.ConfigManager.getConfig();

        if (brandLabel != null) {
            brandLabel.setText("ArenaFlow"); // Rebranding app name
            brandLabel.setGraphic(
                    new org.kordamp.ikonli.javafx.FontIcon(org.kordamp.ikonli.material2.Material2MZ.SPORTS_SOCCER)); // Temporary
                                                                                                                     // placeholder
                                                                                                                     // logic
                                                                                                                     // if
                                                                                                                     // no
                                                                                                                     // logo

            if (config != null && config.getVenueLogoPath() != null && !config.getVenueLogoPath().isEmpty()) {
                try {
                    javafx.scene.image.Image logo = new javafx.scene.image.Image("file:" + config.getVenueLogoPath());
                    javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logo);
                    logoView.setFitHeight(40);
                    logoView.setPreserveRatio(true);
                    brandLabel.setGraphic(logoView);
                    brandLabel.setText(""); // Only logo if available
                } catch (Exception e) {
                    System.err.println("Could not load venue logo: " + e.getMessage());
                }
            }
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            // Role Based Access Control
            if (!"ADMIN".equals(currentUser.getRole())) {
                settingsButton.setVisible(false);
                settingsButton.setManaged(false);
            }
        }

        // Load default view (Dashboard)
        showDashboard();
    }

    public static void showModal(Node content) {
        if (instance != null && instance.modalContainer != null) {
            // detailed glassmorphism overlay style
            instance.modalContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");

            instance.modalContainer.getChildren().clear();
            instance.modalContainer.getChildren().add(content);
            instance.modalContainer.setVisible(true);

            // Add Blur to main content
            javafx.scene.effect.BoxBlur blur = new javafx.scene.effect.BoxBlur(10, 10, 3);
            instance.contentArea.setEffect(blur);
        }
    }

    public static void closeModal() {
        if (instance != null && instance.modalContainer != null) {
            instance.modalContainer.setVisible(false);
            instance.modalContainer.getChildren().clear();

            // Remove Blur
            instance.contentArea.setEffect(null);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            System.out.println("Loading view: " + fxmlPath);
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            Node view = fxmlLoader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            System.out.println("Successfully loaded view: " + fxmlPath);
            // Ensure modal is closed when switching views
            closeModal();
        } catch (Exception e) {
            System.err.println("ERROR loading view: " + fxmlPath);
            e.printStackTrace();

            // Show error in contentArea
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                    "Error loading view: " + fxmlPath + "\n" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px; -fx-padding: 20;");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errorLabel);
        }
    }

    @FXML
    private void showDashboard() {
        loadView("/com/managepickle/home.fxml");
    }

    @FXML
    private void showBookings() {
        loadView("/com/managepickle/bookings.fxml");
    }

    @FXML
    private void showMembers() {
        System.out.println("Switching to Members");
    }

    @FXML
    private void showPOS() {
        System.out.println("Switching to POS");
    }

    @FXML
    private void showSettings() {
        loadView("/com/managepickle/settings.fxml");
    }
}
