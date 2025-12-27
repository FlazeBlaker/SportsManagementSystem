package com.managepickle.controller;

import com.managepickle.App;
import com.managepickle.database.UserDAO;
import com.managepickle.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        User user = UserDAO.findByUsername(username);

        if (user != null && user.getPasswordHash().equals(password)) {
            // Success
            com.managepickle.utils.SessionManager.setCurrentUser(user);
            loadDashboard();
        } else {
            errorLabel.setText("Invalid credentials.");
        }
    }

    private void loadDashboard() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/managepickle/dashboard.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());

            // Apply Dynamic Theme
            com.managepickle.utils.ThemeManager.applyTheme(scene);

            stage.setScene(scene);

            // Re-apply Kiosk mode properties after scene switch
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load dashboard.");
        }
    }
}
