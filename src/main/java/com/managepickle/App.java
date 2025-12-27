package com.managepickle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Apply AtlantaFX Theme
        Application.setUserAgentStylesheet(new atlantafx.base.theme.Dracula().getUserAgentStylesheet());

        // Initialize Database
        com.managepickle.database.DatabaseManager.initializeDatabase();

        boolean isSetup = com.managepickle.database.UserDAO.getAllUsers().isEmpty();

        if (isSetup) {
            // SETUP MODE: Premium Transparent Window
            stage.initStyle(StageStyle.TRANSPARENT);
        } else {
            // KIOSK MODE SETUP
            // 1. Undecorated (no OS title bar)
            stage.initStyle(StageStyle.UNDECORATED);

            // 2. Full Screen
            stage.setFullScreen(true);
            stage.setFullScreenExitHint(""); // Remove "Press ESC to exit" hint

            // 3. Prevent standard close (Alt+F4 etc)
            stage.setOnCloseRequest(event -> {
                event.consume();
            });
        }

        reloadUI();

        stage.show();
    }

    public static void restart() {
        Platform.runLater(() -> {
            try {
                reloadUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void reloadUI() throws IOException {
        String fxmlFile = "/com/managepickle/login.fxml";
        boolean isSetup = com.managepickle.database.UserDAO.getAllUsers().isEmpty();

        if (isSetup) {
            fxmlFile = "/com/managepickle/setup_wizard.fxml";
            primaryStage.setFullScreen(false);
            primaryStage.setWidth(1100);
            primaryStage.setHeight(750);
            primaryStage.centerOnScreen();
            primaryStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        } else {
            primaryStage.setFullScreen(true);
        }

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlFile));
        Scene scene = new Scene(fxmlLoader.load());

        if (isSetup) {
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        }

        // Load CSS
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/styles/main.css")).toExternalForm());

        if (isSetup) {
            scene.getStylesheets()
                    .add(Objects.requireNonNull(App.class.getResource("/styles/setup.css")).toExternalForm());
        }

        // Apply Dynamic Theme
        com.managepickle.utils.ThemeManager.applyTheme(scene);

        primaryStage.setScene(scene);
        if (!isSetup) {
            primaryStage.setFullScreen(true);
        }
    }

    public static void main(String[] args) {
        launch();
    }

}
