package com.managepickle.controller;

import com.managepickle.database.ConfigDAO;
import com.managepickle.database.CourtDAO;
import com.managepickle.database.UserDAO;
import com.managepickle.model.AppConfig;
import com.managepickle.model.Court;
import com.managepickle.model.User;
import com.managepickle.utils.ConfigManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import atlantafx.base.theme.Styles;
import com.managepickle.utils.AppStyles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.managepickle.model.OperatingRule;
import com.managepickle.model.PricingRule;
import com.managepickle.model.CafeMenuItem;
import com.managepickle.model.RentalOption;
import com.managepickle.model.ProductItem;

public class SetupWizardController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label notificationLabel;
    @FXML
    private StackPane contentArea;

    private boolean isUpdatingFromTemplate = false;
    @FXML
    private Button backButton;
    @FXML
    private Button nextButton;
    @FXML
    private HBox stepDots;
    @FXML
    private VBox stepsContainer; // New: Sidebar steps list

    // Inputs
    private TextField venueNameField;
    private TextField adminUserField;
    private PasswordField adminPassField;

    private ComboBox<String> currencyField;
    private String logoPath;
    private TextField singularResourceField;
    private TextField pluralResourceField;
    private ComboBox<Integer> minSlotDurationField;

    // Step 2: Operating Hours
    private ObservableList<OperatingRule> hoursList = FXCollections.observableArrayList();
    // private TableView<OperatingRule> hoursTable; // Removed in favor of Custom UI

    // Step 3: Global Rates
    private ObservableList<PricingRule> ratesList = FXCollections.observableArrayList();
    // private TableView<PricingRule> ratesTable; // Removed in favor of Custom UI

    // Step 4: Courts
    private ObservableList<Court> courtsList = FXCollections.observableArrayList();

    private CheckBox enableCafeBox;
    private CheckBox enableRentalsBox;

    // Step 6: Pricing Dynamic Lists
    private ObservableList<RentalOption> rentalList = FXCollections.observableArrayList();

    private ObservableList<CafeMenuItem> cafeList = FXCollections.observableArrayList();
    private CheckBox enableProductsBox;
    private ObservableList<ProductItem> productsList = FXCollections.observableArrayList();

    private int currentStep = 0;
    private int totalSteps = 8; // Account, Timings, Rates, Courts, Features, Extras, Products, Finish
    private final List<Node> steps = new ArrayList<>();

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private Pane glassPane;

    @FXML
    public void initialize() {
        // Apply Glass Effect - Gaussian Blur on Background Only
        if (glassPane != null) {
            glassPane.setEffect(new javafx.scene.effect.BoxBlur(30, 30, 3));
        }

        // CSS will be loaded via App.java and FXML
        enableWindowDragging();
        buildSteps();
        refreshSidebar();
        refreshUI();
    }

    private void enableWindowDragging() {
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Load additional CSS after scene is available
                // setup.css is now loaded in App.java

                newScene.setOnMousePressed(event -> {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                });

                newScene.setOnMouseDragged(event -> {
                    javafx.stage.Stage stage = (javafx.stage.Stage) newScene.getWindow();
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                });
            }
        });
    }

    private void buildSteps() {
        // Step 1: Account (0) - KEEP
        VBox step1 = createStepContainer("Account Details", "Set up your venue and admin account.");
        venueNameField = new TextField();
        venueNameField.setPromptText("Venue Name");

        currencyField = new ComboBox<>();
        currencyField.getItems().addAll(
                "₹ (INR)", "$ (USD)", "€ (EUR)", "£ (GBP)",
                "¥ (JPY)", "$ (AUD)", "$ (CAD)", "Fr (CHF)",
                "¥ (CNY)", "kr (SEK)", "kr (NOK)", "₽ (RUB)");
        currencyField.setValue("₹ (INR)"); // Default
        currencyField.setPromptText("Currency");
        currencyField.setPrefWidth(200);

        Button uploadLogoBtn = new Button("Upload Venue Logo");
        uploadLogoBtn.getStyleClass().add(Styles.BUTTON_OUTLINED);
        uploadLogoBtn.setGraphic(new FontIcon(Material2MZ.PHOTO_CAMERA));
        uploadLogoBtn.setPrefWidth(200);
        uploadLogoBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Logo Image");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            java.io.File file = fileChooser.showOpenDialog(contentArea.getScene().getWindow());
            if (file != null) {
                logoPath = file.getAbsolutePath();
                uploadLogoBtn.setText("Logo: " + file.getName());
            }
        });

        adminUserField = new TextField("admin");
        adminPassField = new PasswordField();

        step1.getChildren().addAll(
                new Label("Venue Name"), venueNameField,
                new Label("Currency Symbol"), currencyField,
                new Label("Branding"), uploadLogoBtn,
                new Label("Username"), adminUserField,
                new Label("Password"), adminPassField);
        steps.add(createCard(step1));

        // Step 2: Timings (1) - NEW
        steps.add(createCard(createTimingsStep()));

        // Step 3: Global Rates (2) - NEW
        steps.add(createCard(createGlobalRatesStep()));

        // Step 4: Features & Labeling (3)
        steps.add(createCard(createFeaturesStep()));

        // Step 5: Courts/Resources (4)
        steps.add(createCard(createCourtsStep()));

        // Step 6: Pricing Dynamic Lists (5)
        steps.add(createCard(createPricingStep()));

        // Step 7: Product Inventory (6) - NEW
        steps.add(createCard(createProductsStep()));

        // Step 8: Finish (7)
        VBox stepFinish = createStepContainer("All Set!", "Ready to launch.");
        Label summary = new Label("Click 'Start' to save settings and launch.");
        summary.setStyle("-fx-text-fill: #94A3B8;");
        stepFinish.getChildren().add(summary);
        steps.add(createCard(stepFinish));
    }

    private void refreshUI() {
        if (steps.isEmpty())
            return;

        String rName = pluralResourceField != null ? pluralResourceField.getText() : "Resources";

        String[] titles = {
                "Account & Branding",
                "Timing & Slots",
                "Global Pricing",
                "Features & Labeling",
                rName + " Setup",
                "Extras Pricing",
                "Product Inventory",
                "Review & Finish"
        };
        String[] subtitles = {
                "Set up your venue name and logo.",
                "Define when your venue is open and slot duration.",
                "Set your standard rate types.",
                "Enable services and name your resources.",
                "Add and configure your " + rName.toLowerCase() + ".",
                "Set prices for cafe and gear.",
                "Manage your physical inventory.",
                "Complete your ArenaFlow setup."
        };

        titleLabel.setText(titles[currentStep]);
        subtitleLabel.setText(subtitles[currentStep]);

        contentArea.getChildren().clear();
        contentArea.getChildren().add(steps.get(currentStep));

        backButton.setDisable(currentStep == 0);
        nextButton.setText(currentStep == totalSteps - 1 ? "Finish" : "Continue");

        refreshSidebar();
    }

    private void refreshSidebar() {
        if (stepsContainer == null)
            return;

        stepsContainer.getChildren().clear();

        String rName = pluralResourceField != null ? pluralResourceField.getText() : "Resources";

        String[] stepNames = {
                "Account Setup",
                "Operating Hours",
                "Global Pricing",
                "Features & Labeling",
                rName + " Setup",
                "Extras Pricing",
                "Product Inventory",
                "Review & Finish"
        };

        // Icons mapping
        org.kordamp.ikonli.Ikon[] stepIcons = {
                Material2AL.BUSINESS, // Account
                Material2AL.ACCESS_TIME, // Hours
                Material2AL.ATTACH_MONEY, // Global Pricing
                Material2MZ.MISCELLANEOUS_SERVICES, // Features
                Material2AL.LAYERS, // Resources
                Material2AL.LOCAL_CAFE, // Extras
                Material2MZ.STORE, // Products
                Material2AL.CHECK_CIRCLE // Finish
        };

        for (int i = 0; i < totalSteps; i++) {
            HBox stepItem = new HBox(AppStyles.SPACING_16);
            stepItem.setAlignment(Pos.CENTER_LEFT);
            stepItem.setPadding(new javafx.geometry.Insets(8, 16, 8, 16));

            // Icon
            FontIcon icon = new FontIcon(stepIcons[i]);
            icon.setIconSize(20);

            // Name
            Label nameLabel = new Label(stepNames[i]);
            nameLabel.setStyle("-fx-font-size: 14px;");

            stepItem.getChildren().addAll(icon, nameLabel);

            // Styling based on state
            if (i == currentStep) {
                stepItem.setStyle("-fx-background-color: -color-accent-subtle; -fx-background-radius: "
                        + AppStyles.RADIUS_MEDIUM + ";");
                icon.setIconColor(javafx.scene.paint.Color.web("#A78BFA")); // Accent
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
            } else if (i < currentStep) {
                icon.setIconColor(javafx.scene.paint.Color.web("#34D399")); // Success
                nameLabel.setStyle("-fx-text-fill: -color-fg-muted;");
            } else {
                stepItem.setOpacity(0.5);
                nameLabel.setStyle("-fx-text-fill: -color-fg-muted;");
            }

            stepsContainer.getChildren().add(stepItem);
        }
    }

    private Node createCard(Node content) {
        StackPane card = new StackPane(content);
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setPadding(new javafx.geometry.Insets(AppStyles.SPACING_24));
        card.setStyle(
                "-fx-background-radius: " + AppStyles.RADIUS_MEDIUM + "; -fx-background-color: -color-bg-default;");
        return card;
    }

    private VBox createStepContainer(String titleStr, String descStr) {
        VBox box = new VBox(AppStyles.SPACING_16);
        box.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleStr);
        title.getStyleClass().add(Styles.TITLE_3);

        Label desc = new Label(descStr);
        desc.getStyleClass().add(Styles.TEXT_MUTED);

        box.getChildren().addAll(title, desc);
        return box;
    }

    @FXML
    private void onNext() {
        hideNotification();
        if (!validateCurrentStep())
            return;

        int nextStep = currentStep + 1;

        // Skip logic for Extras (Step 5)
        if (nextStep == 5 && !enableCafeBox.isSelected() && !enableRentalsBox.isSelected()) {
            nextStep = 6;
        }

        // Skip logic for Products (Step 6)
        if (nextStep == 6 && !enableProductsBox.isSelected()) {
            nextStep = 7;
        }

        if (nextStep < totalSteps) {
            currentStep = nextStep;
            refreshUI();
        } else {
            handleFinish();
        }
    }

    @FXML
    private void onBack() {
        hideNotification();
        if (currentStep > 0) {
            int prevStep = currentStep - 1;

            // Skip logic for Products (Step 6)
            if (prevStep == 6 && !enableProductsBox.isSelected()) {
                prevStep = 5;
            }

            // Skip logic for Extras (Step 5)
            if (prevStep == 5 && !enableCafeBox.isSelected() && !enableRentalsBox.isSelected()) {
                prevStep = 4;
            }

            currentStep = prevStep;
            refreshUI();
        }
    }

    private boolean validateCurrentStep() {
        if (currentStep == 0) { // Account
            if (venueNameField.getText().isEmpty()) {
                showAlert("Venue Name is required");
                return false;
            }
            if (adminUserField.getText().trim().isEmpty() || adminPassField.getText().isEmpty()) {
                showAlert("Admin info required");
                return false;
            }
        } else if (currentStep == 1) { // Operating Hours
            if (hoursList.isEmpty()) {
                showAlert("Please add at least one operating rule/time.");
                return false;
            }
        } else if (currentStep == 2) { // Global Pricing
            if (ratesList.isEmpty()) {
                showAlert("Please add at least one pricing rule.");
                return false;
            }
            if (!checkPricingCoverage()) {
                return false;
            }
        } else if (currentStep == 4) { // Courts/Resources
            String rSingular = singularResourceField != null ? singularResourceField.getText().trim() : "Resource";
            if (courtsList.isEmpty()) {
                showAlert("Please add at least one " + rSingular.toLowerCase() + ".");
                return false;
            }
        } else if (currentStep == 5) { // Extras Pricing
            boolean cafeEnabled = enableCafeBox.isSelected();
            boolean rentalsEnabled = enableRentalsBox.isSelected();

            if (cafeEnabled && cafeList.isEmpty()) {
                showAlert("Cafe is enabled. Please add at least one menu item or disable Cafe in the previous step.");
                return false;
            }
            if (rentalsEnabled && rentalList.isEmpty()) {
                showAlert(
                        "Rentals are enabled. Please add at least one gear option or disable Rentals in the previous step.");
                return false;
            }
        } else if (currentStep == 6) { // Products
            if (enableProductsBox.isSelected() && productsList.isEmpty()) {
                showAlert(
                        "Product sales are enabled. Please add at least one product or disable it in the Features step.");
                return false;
            }
        }
        return true;
    }

    private void handleFinish() {
        try {
            // 1. Save Config
            ConfigDAO.setValue("brand_name", venueNameField.getText().trim());

            AppConfig config = ConfigManager.getConfig();

            // Dynamic Settings
            String selectedCurrency = currencyField.getValue();
            // Extract symbol from "₹ (INR)" format -> take first part
            String symbol = selectedCurrency != null ? selectedCurrency.split("\\s")[0] : "₹";
            config.setCurrencySymbol(symbol);

            // Save Complex Schedules & Rates
            config.setOperatingSchedule(new ArrayList<>(hoursList));
            config.setGlobalPricingRules(new ArrayList<>(ratesList));

            config.setHasCafe(enableCafeBox.isSelected());
            config.setHasRentals(enableRentalsBox.isSelected());
            config.setHasProducts(enableProductsBox.isSelected());

            // Save Pricing
            if (config.isHasRentals()) {
                config.setRentalOptions(new ArrayList<>(rentalList));
            } else {
                config.setRentalOptions(new ArrayList<>()); // Clear if disabled
            }

            if (config.isHasCafe()) {
                config.setCafeMenu(new ArrayList<>(cafeList));
            } else {
                config.setCafeMenu(new ArrayList<>()); // Clear if disabled
            }

            if (config.isHasProducts()) {
                config.setProductCatalog(new ArrayList<>(productsList));
            } else {
                config.setProductCatalog(new ArrayList<>()); // Clear if disabled
            }

            // ArenaFlow Branding & Timing
            config.setVenueLogoPath(logoPath);
            config.setMinSlotDuration(minSlotDurationField.getValue() != null ? minSlotDurationField.getValue() : 60);
            config.setResourceName(singularResourceField.getText().trim());
            config.setResourceNamePlural(pluralResourceField.getText().trim());

            // 2. Create Courts
            // If courtsList is empty, maybe create default?
            if (courtsList.isEmpty()) {
                // Fallback default
                Court c = Court.builder().name("Court 1").type("Indoor").hourlyRate(100.0).build();
                courtsList.add(c);
            }

            List<Court> savedCourts = new ArrayList<>();
            for (Court c : courtsList) {
                int id = CourtDAO.createCourt(c);
                c.setId(id);
                savedCourts.add(c);
            }
            config.setCourts(savedCourts);
            ConfigManager.saveConfig();

            // 3. Create Admin
            User admin = User.builder().username(adminUserField.getText().trim()).passwordHash(adminPassField.getText())
                    .role("ADMIN").build();
            UserDAO.createUser(admin);

            // 4. Go to Login
            goToLogin();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error: " + e.getMessage());
        }
    }

    // --- Dynamic Step Helpers ---

    private VBox createTimingsStep() {
        VBox box = createStepContainer("Timing & Slots", "Define when your venue is open and slot duration.");

        Label slotLabel = new Label("Minimum Booking Slot (Mins):");
        slotLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");
        minSlotDurationField = new ComboBox<>();
        minSlotDurationField.getItems().addAll(30, 45, 60, 90, 120);
        minSlotDurationField.setValue(60);
        minSlotDurationField.setPrefWidth(120);
        minSlotDurationField.setStyle("-fx-font-size: 10px;");

        // Mode Selector - Very Compact
        Label modeLabel = new Label("Schedule Type:");
        modeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");

        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Same Everyday", "Weekdays & Weekends", "Custom (Per Day)");
        modeCombo.setValue("Same Everyday"); // Default
        modeCombo.setPrefWidth(160);
        modeCombo.setStyle("-fx-font-size: 10px;");

        VBox dynamicContent = new VBox(8);
        dynamicContent
                .setStyle("-fx-padding: 6; -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8;");

        modeCombo.setOnAction(e -> updateTimingsUI(modeCombo.getValue(), dynamicContent));

        // Initialize UI based on current list state or default
        if (hoursList.isEmpty()) {
            // Default to "Same Everyday"
            updateTimingsUI("Same Everyday", dynamicContent);
        } else {
            // Try to detect mode from data (Simplified: just reset to current selection for
            // now)
            updateTimingsUI(modeCombo.getValue(), dynamicContent);
        }

        box.getChildren().addAll(slotLabel, minSlotDurationField, modeLabel, modeCombo, dynamicContent);
        return box;
    }

    private void updateTimingsUI(String mode, VBox container) {
        container.getChildren().clear();
        hoursList.clear(); // Reset data on mode switch for simplicity this time

        if ("Same Everyday".equals(mode)) {
            // One Rule for All Days
            OperatingRule rule = OperatingRule.builder()
                    .name("Everyday")
                    .days(java.util.Arrays.asList(DayOfWeek.values()))
                    .openTime("06:00").closeTime("23:00").is24Hours(false)
                    .build();
            hoursList.add(rule);
            container.getChildren().add(createTimingCard(rule, "Every Day"));

        } else if ("Weekdays & Weekends".equals(mode)) {
            // Rule 1: Weekdays
            OperatingRule weekdays = OperatingRule.builder()
                    .name("Weekdays")
                    .days(java.util.Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                    .openTime("06:00").closeTime("23:00").is24Hours(false)
                    .build();

            // Rule 2: Weekends
            OperatingRule weekends = OperatingRule.builder()
                    .name("Weekends")
                    .days(java.util.Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                    .openTime("06:00").closeTime("23:00").is24Hours(false)
                    .build();

            hoursList.add(weekdays);
            hoursList.add(weekends);

            container.getChildren().addAll(
                    createTimingCard(weekdays, "Weekdays (Mon-Fri)"),
                    createTimingCard(weekends, "Weekends (Sat-Sun)"));

        } else {
            // Custom: 7 Rules
            for (DayOfWeek d : DayOfWeek.values()) {
                OperatingRule dayRule = OperatingRule.builder()
                        .name(d.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH))
                        .days(java.util.Arrays.asList(d))
                        .openTime("06:00").closeTime("23:00").is24Hours(false)
                        .build();
                hoursList.add(dayRule);
                container.getChildren().add(createTimingCard(dayRule, dayRule.getName()));
            }
        }
    }

    private javafx.scene.layout.GridPane createTimingCard(OperatingRule rule, String title) {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(6);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-min-width: 80; -fx-font-size: 10px;");

        CheckBox is24h = new CheckBox("24h");
        is24h.setSelected(rule.is24Hours());
        is24h.setStyle("-fx-text-fill: white; -fx-font-size: 9px;");

        ComboBox<String> openBox = generateTimeCombo();
        openBox.setValue(rule.getOpenTime());
        openBox.setStyle("-fx-background-color: #334155; -fx-font-size: 10px;");
        openBox.setPrefWidth(100);
        openBox.setOnAction(e -> {
            String v = openBox.getValue();
            rule.setOpenTime("24:00".equals(v) ? "00:00" : v);
        });

        ComboBox<String> closeBox = generateTimeCombo();
        closeBox.setValue(rule.getCloseTime());
        closeBox.setStyle("-fx-background-color: #334155; -fx-font-size: 10px;");
        closeBox.setPrefWidth(100);
        closeBox.setOnAction(e -> {
            String v = closeBox.getValue();
            rule.setCloseTime("24:00".equals(v) ? "00:00" : v);
        });

        CheckBox isClosed = new CheckBox("Closed");
        isClosed.setSelected(rule.isClosed());
        isClosed.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 9px; -fx-font-weight: bold;");

        is24h.selectedProperty().addListener((obs, old, val) -> {
            rule.set24Hours(val);
            openBox.setDisable(val || rule.isClosed());
            closeBox.setDisable(val || rule.isClosed());
        });

        isClosed.selectedProperty().addListener((obs, old, val) -> {
            rule.setClosed(val);
            openBox.setDisable(val || rule.is24Hours());
            closeBox.setDisable(val || rule.is24Hours());
            is24h.setDisable(val);
        });

        // Initial state
        openBox.setDisable(rule.isClosed() || rule.is24Hours());
        closeBox.setDisable(rule.isClosed() || rule.is24Hours());
        is24h.setDisable(rule.isClosed());

        grid.add(lbl, 0, 0);

        Label openLbl = new Label("Open:");
        openLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 9px;");
        grid.add(openLbl, 1, 0);
        grid.add(openBox, 2, 0);

        Label closeLbl = new Label("Close:");
        closeLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 9px;");
        grid.add(closeLbl, 3, 0);
        grid.add(closeBox, 4, 0);

        grid.add(is24h, 5, 0);
        grid.add(isClosed, 6, 0);

        return grid;
    }

    private ComboBox<String> generateTimeCombo() {
        ComboBox<String> box = new ComboBox<>();
        box.setPrefWidth(100); // Increased for text visibility
        for (int i = 0; i < 24; i++) {
            box.getItems().add(String.format("%02d:00", i));
            box.getItems().add(String.format("%02d:30", i));
        }
        box.getItems().add("24:00");
        return box;
    }

    private VBox ratesListContainer; // Container for cards
    private VBox rateFormContainer; // Container for inline form

    // Courts UI Containers
    private VBox courtsListContainer;
    private VBox courtFormContainer;

    // Cafe/Rentals Containers
    private VBox cafeListContainer;
    private VBox cafeFormContainer;
    private VBox rentalListContainer;
    private VBox rentalFormContainer;

    private VBox productListContainer;
    private VBox productFormContainer;

    private VBox createGlobalRatesStep() {
        VBox box = createStepContainer("Global Rates", "Set standard pricing rules (e.g. Weekends, Happy Hours).");

        // 1. Add Button at Top
        Button addBtn = new Button("+ Add New Rule");
        addBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10; -fx-font-size: 14px;");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            if (rateFormContainer.isVisible()) {
                closeRateForm();
            } else {
                openRateForm(null);
            }
        });

        // 2. Inline Form Container (Initially Hidden)
        rateFormContainer = new VBox(20);
        rateFormContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        rateFormContainer.setManaged(false);
        rateFormContainer.setVisible(false);

        // 3. List Container
        ratesListContainer = new VBox(10);
        ratesListContainer.setStyle("-fx-padding: 0 5 0 0;");

        ScrollPane scroll = new ScrollPane(ratesListContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(200);

        box.getChildren().addAll(addBtn, rateFormContainer, scroll);

        // Initial Render
        renderRatesList();

        return box;
    }

    private void renderRatesList() {
        ratesListContainer.getChildren().clear();
        if (ratesList.isEmpty()) {
            Label empty = new Label("No rates defined. Add one to get started.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            ratesListContainer.getChildren().add(empty);
            return;
        }

        for (PricingRule rule : ratesList) {
            ratesListContainer.getChildren().add(createRateCard(rule));
        }
    }

    private HBox createRateCard(PricingRule rule) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        VBox info = new VBox(5);
        Label name = new Label(rule.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        String dayText = (rule.getDays() == null || rule.getDays().size() == 7) ? "Everyday"
                : rule.getDays().size() + " Days";
        Label details = new Label(dayText + " • " + rule.getStartTime() + " - " + rule.getEndTime());
        details.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(name, details);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label rate = new Label(getCurrencySymbol() + rule.getRate());
        rate.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34D399;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        editBtn.setOnAction(e -> openRateForm(rule));

        Button delBtn = new Button("×");
        delBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 0 5;");
        delBtn.setOnAction(e -> {
            ratesList.remove(rule);
            renderRatesList();
        });

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().addAll(info, spacer, rate, actions);
        return card;
    }

    private void closeRateForm() {
        rateFormContainer.getChildren().clear();
        rateFormContainer.setManaged(false);
        rateFormContainer.setVisible(false);
    }

    private void openRateForm(PricingRule editRule) {
        rateFormContainer.getChildren().clear();
        rateFormContainer.setManaged(true);
        rateFormContainer.setVisible(true);

        Label title = new Label(editRule == null ? "Add Pricing Rule" : "Edit Pricing Rule");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Template Selector
        ComboBox<String> tmplBox = new ComboBox<>();
        tmplBox.getItems().addAll("Weekdays", "Weekends", "Happy Hour", "Custom");
        tmplBox.setPromptText("Select Template...");
        tmplBox.setMaxWidth(Double.MAX_VALUE);
        tmplBox.setStyle("-fx-font-size: 14px;");

        // Define Inputs
        TextField ruleNameField = new TextField(editRule != null ? editRule.getName() : "");
        ruleNameField.setPromptText("Rule Name");
        ruleNameField.setStyle("-fx-font-size: 14px;");

        TextField ruleRateField = new TextField(editRule != null ? String.valueOf(editRule.getRate()) : "");
        ruleRateField.setPromptText("Hourly Rate");
        ruleRateField.setStyle("-fx-font-size: 14px;");

        TextField ruleStartField = new TextField(editRule != null ? editRule.getStartTime() : "06:00");
        ruleStartField.setPromptText("Start");
        ruleStartField.setPrefWidth(120);
        ruleStartField.setStyle("-fx-font-size: 14px;");

        TextField ruleEndField = new TextField(editRule != null ? editRule.getEndTime() : "23:00");
        ruleEndField.setPromptText("End");
        ruleEndField.setPrefWidth(120);
        ruleEndField.setStyle("-fx-font-size: 14px;");

        // Auto-Custom Logic for text fields
        Runnable switchToCustom = () -> {
            if (!isUpdatingFromTemplate && !"Custom".equals(tmplBox.getValue())) {
                tmplBox.setValue("Custom");
            }
        };

        ruleNameField.textProperty().addListener((obs, oldV, newV) -> switchToCustom.run());
        ruleStartField.textProperty().addListener((obs, oldV, newV) -> switchToCustom.run());
        ruleEndField.textProperty().addListener((obs, oldV, newV) -> switchToCustom.run());

        // Days Selection
        Label lblDays = new Label("Applies To");
        lblDays.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

        javafx.scene.layout.FlowPane daysPane = new javafx.scene.layout.FlowPane(10, 10);
        List<CheckBox> dayChecks = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            CheckBox cb = new CheckBox(d.name().substring(0, 3)); // Mon, Tue...
            cb.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            cb.setUserData(d);
            cb.setSelected(true); // Default all
            dayChecks.add(cb);
            daysPane.getChildren().add(cb);

            cb.setOnAction(e -> {
                if (!isUpdatingFromTemplate && !"Custom".equals(tmplBox.getValue())) {
                    tmplBox.setValue("Custom");
                }
            });
        }

        // Initialize checkboxes if editing
        if (editRule != null && editRule.getDays() != null && !editRule.getDays().isEmpty()) {
            for (CheckBox cb : dayChecks) {
                cb.setSelected(editRule.getDays().contains((DayOfWeek) cb.getUserData()));
            }
        }

        // Template Action
        tmplBox.setOnAction(e -> {
            String t = tmplBox.getValue();
            if (t != null && !t.equals("Custom")) {
                isUpdatingFromTemplate = true;

                // Calculate dynamic bounds from hoursList
                LocalTime minOpen = LocalTime.of(9, 0);
                LocalTime maxClose = LocalTime.of(22, 0);
                if (!hoursList.isEmpty()) {
                    minOpen = null;
                    maxClose = null;
                    for (OperatingRule op : hoursList) {
                        if (op.isClosed())
                            continue; // Ignore closed days for range defaults
                        LocalTime start = op.is24Hours() ? LocalTime.MIN : LocalTime.parse(op.getOpenTime());
                        LocalTime end = op.is24Hours() ? LocalTime.of(23, 59) : LocalTime.parse(op.getCloseTime());

                        if (minOpen == null || start.isBefore(minOpen))
                            minOpen = start;
                        if (maxClose == null || end.isAfter(maxClose))
                            maxClose = end;
                    }
                }
                String startStr = minOpen.toString();
                String endStr = (maxClose.equals(LocalTime.MAX)
                        || (maxClose.getHour() == 23 && maxClose.getMinute() == 59)) ? "00:00" : maxClose.toString();

                if (t.contains("Weekdays")) {
                    ruleNameField.setText("Weekdays");
                    ruleStartField.setText(startStr);
                    ruleEndField.setText(endStr);
                    for (CheckBox cb : dayChecks) {
                        DayOfWeek d = (DayOfWeek) cb.getUserData();
                        cb.setSelected(d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY);
                    }
                } else if (t.contains("Weekends")) {
                    ruleNameField.setText("Weekends");
                    ruleStartField.setText(startStr);
                    ruleEndField.setText(endStr);
                    for (CheckBox cb : dayChecks) {
                        DayOfWeek d = (DayOfWeek) cb.getUserData();
                        cb.setSelected(d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY);
                    }
                } else if (t.contains("Happy")) {
                    ruleNameField.setText("Happy Hour");
                    ruleStartField.setText(startStr);
                    ruleEndField.setText(endStr);
                    for (CheckBox cb : dayChecks)
                        cb.setSelected(true);
                }
                isUpdatingFromTemplate = false;
            }
        });

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #475569; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 20;");
        cancelBtn.setOnAction(e -> closeRateForm());

        Button saveBtn = new Button("Save Rule");
        saveBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-effect: dropshadow(three-pass-box, rgba(167, 139, 250, 0.4), 10, 0, 0, 5);");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        saveBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            ruleRateField.setStyle("-fx-font-size: 14px;");
            ruleNameField.setStyle("-fx-font-size: 14px;");

            try {
                String rateText = ruleRateField.getText().replaceAll("[^0-9.]", "");
                if (rateText.isEmpty())
                    throw new NumberFormatException("Empty Rate");
                double r = Double.parseDouble(rateText);

                String nameText = ruleNameField.getText();
                if (nameText == null || nameText.trim().isEmpty()) {
                    ruleNameField.setStyle("-fx-border-color: #EF4444; -fx-font-size: 14px;");
                    throw new IllegalArgumentException("Name required");
                }

                // Gather days from checkboxes
                List<DayOfWeek> days = new ArrayList<>();
                for (CheckBox cb : dayChecks) {
                    if (cb.isSelected()) {
                        days.add((DayOfWeek) cb.getUserData());
                    }
                }

                if (days.isEmpty()) {
                    throw new IllegalArgumentException("Select at least one day");
                }

                // Check for overlaps
                String sTxt = ruleStartField.getText();
                String eTxt = ruleEndField.getText();
                LocalTime newStart = LocalTime.parse("24:00".equals(sTxt) ? "00:00" : sTxt);
                LocalTime newEnd = LocalTime.parse("24:00".equals(eTxt) ? "00:00" : eTxt);
                if (newEnd.equals(LocalTime.MIN) || newEnd.isBefore(newStart)) {
                    newEnd = LocalTime.MAX;
                }

                for (PricingRule existing : ratesList) {
                    if (existing == editRule)
                        continue;

                    // Check if they share any days
                    boolean sharesDay = false;
                    for (DayOfWeek d : days) {
                        if (existing.getDays().contains(d)) {
                            sharesDay = true;
                            break;
                        }
                    }

                    if (sharesDay) {
                        LocalTime exStart = LocalTime.parse(existing.getStartTime());
                        LocalTime exEnd = LocalTime.parse(existing.getEndTime());
                        if (exEnd.equals(LocalTime.MIN) || exEnd.isBefore(exStart)) {
                            exEnd = LocalTime.MAX;
                        }

                        // Overlap condition: (StartA < EndB) and (EndA > StartB)
                        if (newStart.isBefore(exEnd) && newEnd.isAfter(exStart)) {
                            throw new IllegalArgumentException("Overlap with '" + existing.getName() + "' ("
                                    + existing.getStartTime() + "-" + existing.getEndTime() + ")");
                        }
                    }
                }

                if (editRule != null) {
                    editRule.setName(nameText.trim());
                    editRule.setRate(r);
                    editRule.setStartTime(ruleStartField.getText());
                    editRule.setEndTime(ruleEndField.getText());
                    editRule.setDays(days);
                } else {
                    PricingRule newRule = PricingRule.builder()
                            .name(nameText.trim())
                            .rate(r)
                            .days(days)
                            .startTime(ruleStartField.getText())
                            .endTime(ruleEndField.getText())
                            .build();
                    ratesList.add(newRule);
                }

                renderRatesList();
                closeRateForm();
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
                errorLabel.setManaged(true);
                errorLabel.setVisible(true);
            }
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);

        Label lblTemplate = new Label("Template");
        lblTemplate.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

        Label lblName = new Label("Name");
        lblName.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

        Label lblTime = new Label("Time Range (24h)");
        lblTime.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

        Label lblRate = new Label("Rate (" + getCurrencySymbol() + ")");
        lblRate.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

        rateFormContainer.getChildren().addAll(
                title,
                lblTemplate, tmplBox,
                lblName, ruleNameField,
                lblDays, daysPane,
                lblTime,
                new HBox(15, ruleStartField, new Label("-"), ruleEndField),
                lblRate, ruleRateField,
                errorLabel,
                new javafx.scene.control.Separator(),
                buttons);
    }

    private VBox createCourtsStep() {
        VBox box = createStepContainer("Courts", "Add your courts & configure overrides.");

        // 1. Add Button
        Button addBtn = new Button("+ Add New Court");
        addBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10; -fx-font-size: 14px;");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            if (courtFormContainer.isVisible()) {
                closeCourtForm();
            } else {
                openCourtForm(null);
            }
        });

        // 2. Inline Form Container (Initially Hidden)
        courtFormContainer = new VBox(20);
        courtFormContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        courtFormContainer.setManaged(false);
        courtFormContainer.setVisible(false);

        // 3. List Container
        courtsListContainer = new VBox(10);
        courtsListContainer.setStyle("-fx-padding: 0 5 0 0;");

        ScrollPane scroll = new ScrollPane(courtsListContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(200);

        box.getChildren().addAll(addBtn, courtFormContainer, scroll);

        // Initial Render
        renderCourtList();

        return box;
    }

    private void renderCourtList() {
        courtsListContainer.getChildren().clear();
        if (courtsList.isEmpty()) {
            Label empty = new Label("No courts added. Add one to get started.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            courtsListContainer.getChildren().add(empty);
            return;
        }

        for (Court court : courtsList) {
            courtsListContainer.getChildren().add(createCourtCard(court));
        }
    }

    private HBox createCourtCard(Court court) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        // Icon
        String iconCode = court.getIconCode();
        if (iconCode == null || iconCode.isEmpty())
            iconCode = "mzl-layers";
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(javafx.scene.paint.Color.web("#A78BFA"));

        VBox info = new VBox(5);
        Label name = new Label(court.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        String env = court.getEnvironmentType();
        if (env == null)
            env = court.getType(); // Fallback to old type if new env is null

        String rateType = court.isUseGlobalRates() ? "Global Rules"
                : "Flat Rate (" + getCurrencySymbol() + court.getHourlyRate() + ")";
        Label details = new Label(env + " • " + rateType);
        details.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(name, details);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(10);

        Button editBtn = new Button("Edit");
        editBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        editBtn.setOnAction(e -> openCourtForm(court));

        Button delBtn = new Button("×");
        delBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 0 5;");
        delBtn.setOnAction(e -> {
            courtsList.remove(court);
            renderCourtList();
        });

        actions.getChildren().addAll(editBtn, delBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(icon, info, spacer, actions);
        return card;
    }

    private void openCourtForm(Court editCourt) {
        courtFormContainer.getChildren().clear();
        courtFormContainer.setManaged(true);
        courtFormContainer.setVisible(true);

        String rSingular = singularResourceField != null ? singularResourceField.getText().trim() : "Resource";
        if (rSingular.isEmpty())
            rSingular = "Resource";

        Label title = new Label(editCourt == null ? "Add New " + rSingular : "Edit " + rSingular);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Inputs
        TextField nameField = new TextField(editCourt != null ? editCourt.getName() : "");
        nameField.setPromptText(rSingular + " Name (e.g. Center " + rSingular + ")");
        nameField.setStyle("-fx-font-size: 14px;");

        // Icon Picker
        Label lblIcon = new Label("Select Icon");
        lblIcon.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        HBox iconPicker = new HBox(10);
        iconPicker.setAlignment(Pos.CENTER_LEFT);
        iconPicker.setStyle("-fx-padding: 5;");

        org.kordamp.ikonli.Ikon[] icons = {
                Material2AL.LAYERS, Material2AL.BUSINESS, Material2MZ.SPORTS_SOCCER,
                Material2MZ.SPORTS_BASKETBALL, Material2MZ.SPORTS_TENNIS, Material2MZ.SPORTS_VOLLEYBALL,
                Material2MZ.STORE, Material2MZ.MISCELLANEOUS_SERVICES, Material2AL.LOCAL_CAFE,
                Material2AL.ATTACH_MONEY, Material2AL.CHECK_CIRCLE, Material2AL.ACCESS_TIME
        };

        String currentIcon = editCourt != null && editCourt.getIconCode() != null ? editCourt.getIconCode()
                : Material2AL.LAYERS.getDescription();
        final String[] selectedIcon = { currentIcon };

        for (org.kordamp.ikonli.Ikon iconObj : icons) {
            String iconCode = iconObj.getDescription();
            Button iconBtn = new Button();
            iconBtn.setUserData(iconCode);
            iconBtn.setGraphic(new FontIcon(iconObj));
            iconBtn.setStyle(
                    "-fx-background-color: " + (iconCode.equals(selectedIcon[0]) ? "#4C1D95" : "transparent") + "; " +
                            "-fx-border-color: "
                            + (iconCode.equals(selectedIcon[0]) ? "#A78BFA" : "rgba(255,255,255,0.1)") + "; " +
                            "-fx-border-radius: 4; -fx-cursor: hand; -fx-padding: 8;");

            iconBtn.setOnAction(e -> {
                selectedIcon[0] = iconCode;
                // Refresh styles
                iconPicker.getChildren().forEach(n -> {
                    Button b = (Button) n;
                    String ic = (String) b.getUserData();
                    b.setStyle(
                            "-fx-background-color: " + (ic.equals(selectedIcon[0]) ? "#4C1D95" : "transparent") + "; " +
                                    "-fx-border-color: "
                                    + (ic.equals(selectedIcon[0]) ? "#A78BFA" : "rgba(255,255,255,0.1)") + "; " +
                                    "-fx-border-radius: 4; -fx-cursor: hand; -fx-padding: 8;");
                });
            });
            iconPicker.getChildren().add(iconBtn);
        }

        ScrollPane iconScroll = new ScrollPane(iconPicker);
        iconScroll.setFitToHeight(true);
        iconScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        iconScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Label lblEnv = new Label("Environment Type");
        lblEnv.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Indoor", "Outdoor", "A/C (Indoor)");
        typeBox.setValue(editCourt != null ? editCourt.getEnvironmentType() : "Indoor");
        typeBox.setStyle("-fx-font-size: 14px;");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        // rateField removed as per user request

        CheckBox useGlobal = new CheckBox("Use Global Pricing Rules");
        useGlobal.setSelected(editCourt != null ? editCourt.isUseGlobalRates() : true);
        useGlobal.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // Manual Rate field (only visible if global rules are unchecked)
        VBox manualRateBox = new VBox(5);
        Label lblManualRate = new Label("Custom Manual Rate (" + getCurrencySymbol() + ")");
        lblManualRate.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
        TextField manualRateField = new TextField(editCourt != null ? String.valueOf(editCourt.getHourlyRate()) : "0");
        manualRateField.setPromptText("Enter flat rate per hour");
        manualRateField.setStyle("-fx-font-size: 14px;");
        manualRateBox.getChildren().addAll(lblManualRate, manualRateField);

        manualRateBox.setManaged(!useGlobal.isSelected());
        manualRateBox.setVisible(!useGlobal.isSelected());

        useGlobal.selectedProperty().addListener((obs, oldV, newV) -> {
            manualRateBox.setManaged(!newV);
            manualRateBox.setVisible(!newV);
        });

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #475569; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 20;");
        cancelBtn.setOnAction(e -> closeCourtForm());

        Button saveBtn = new Button("Save Court");
        saveBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-effect: dropshadow(three-pass-box, rgba(167, 139, 250, 0.4), 10, 0, 0, 5);");

        saveBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            try {
                String name = nameField.getText();
                if (name == null || name.strip().isEmpty())
                    throw new IllegalArgumentException("Name required");

                double r = 0.0;
                if (!useGlobal.isSelected()) {
                    String rText = manualRateField.getText().replaceAll("[^0-9.]", "");
                    if (rText.isEmpty())
                        throw new IllegalArgumentException("Rate required");
                    r = Double.parseDouble(rText);
                }

                if (editCourt != null) {
                    editCourt.setName(name);
                    editCourt.setEnvironmentType(typeBox.getValue());
                    editCourt.setIconCode(selectedIcon[0]);
                    editCourt.setHourlyRate(r);
                    editCourt.setUseGlobalRates(useGlobal.isSelected());
                } else {
                    Court c = Court.builder()
                            .name(name)
                            .environmentType(typeBox.getValue())
                            .iconCode(selectedIcon[0])
                            .hourlyRate(r)
                            .useGlobalRates(useGlobal.isSelected())
                            .build();
                    courtsList.add(c);
                }
                renderCourtList();
                closeCourtForm();

            } catch (Exception ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setManaged(true);
                errorLabel.setVisible(true);
            }
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);

        courtFormContainer.getChildren().addAll(
                title,
                new Label("Icon"), iconScroll,
                new Label("Name"), nameField,
                new Label("Environment"), typeBox,
                useGlobal,
                manualRateBox,
                errorLabel,
                new Separator(),
                buttons);

        // Helper to label style
        courtFormContainer.getChildren().forEach(n -> {
            if (n instanceof Label) {
                Label l = (Label) n;
                l.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
            }
        });
    }

    private void closeCourtForm() {
        courtFormContainer.getChildren().clear();
        courtFormContainer.setManaged(false);
        courtFormContainer.setVisible(false);
    }

    private VBox createFeaturesStep() {
        VBox step3 = createStepContainer("Features & Labeling",
                "What services do you offer and what do you call your resources?");
        enableCafeBox = new CheckBox("Cafe (Food & Drinks)");
        enableCafeBox.setSelected(true);
        enableCafeBox.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        enableRentalsBox = new CheckBox("Equipment Rentals");
        enableRentalsBox.setSelected(true);
        enableRentalsBox.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        enableProductsBox = new CheckBox("Selling Products/Equipment");
        enableProductsBox.setSelected(true);
        enableProductsBox.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        singularResourceField = new TextField("Court");
        singularResourceField.setPromptText("Singular (e.g. Pitch)");
        singularResourceField.textProperty().addListener((obs, oldV, newV) -> {
            refreshUI();
            refreshSidebar();
        });

        pluralResourceField = new TextField("Courts");
        pluralResourceField.setPromptText("Plural (e.g. Pitches)");
        pluralResourceField.textProperty().addListener((obs, oldV, newV) -> {
            refreshUI();
            refreshSidebar();
        });

        step3.getChildren().addAll(
                new Label("Available Services"),
                enableCafeBox, enableRentalsBox, enableProductsBox,
                new Separator(),
                new Label("What do you call your bookable resources?"),
                new HBox(10, new VBox(5, new Label("Singular"), singularResourceField),
                        new VBox(5, new Label("Plural"), pluralResourceField)));

        // Style the new labels
        step3.getChildren().stream()
                .filter(n -> n instanceof Label)
                .forEach(n -> n.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;"));

        return step3;
    }

    private VBox createProductsStep() {
        VBox box = createStepContainer("Product Inventory", "Manage your paddles, balls, and equipment.");

        Button addProductBtn = new Button("+ Add Product");
        addProductBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-size: 13px;");
        addProductBtn.setMaxWidth(Double.MAX_VALUE);
        addProductBtn.setOnAction(e -> {
            if (productFormContainer.isVisible())
                closeProductForm();
            else
                openProductForm();
        });

        productFormContainer = new VBox(10);
        productFormContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        productFormContainer.setManaged(false);
        productFormContainer.setVisible(false);

        productListContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(productListContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(300);

        box.getChildren().addAll(addProductBtn, productFormContainer, scroll);
        renderProductList();
        return box;
    }

    private void renderProductList() {
        if (productListContainer == null)
            return;
        productListContainer.getChildren().clear();
        if (productsList.isEmpty()) {
            Label placeholder = new Label("No products added. Sell paddles, balls, or gear.");
            placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            productListContainer.getChildren().add(placeholder);
            return;
        }
        for (ProductItem item : productsList) {
            productListContainer.getChildren().add(createProductCard(item));
        }
    }

    private HBox createProductCard(ProductItem item) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        FontIcon icon = new FontIcon(Material2MZ.SHOPPING_BAG);
        icon.setIconSize(20);
        icon.setIconColor(javafx.scene.paint.Color.web("#94A3B8"));

        VBox info = new VBox(2);
        Label name = new Label(item.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label cat = new Label(item.getCategory());
        cat.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        info.getChildren().addAll(name, cat);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label price = new Label(getCurrencySymbol() + item.getPrice());
        price.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");

        Button delBtn = new Button("×");
        delBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 16px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            productsList.remove(item);
            renderProductList();
        });

        card.getChildren().addAll(icon, info, spacer, price, delBtn);
        return card;
    }

    private void openProductForm() {
        productFormContainer.getChildren().clear();
        productFormContainer.setManaged(true);
        productFormContainer.setVisible(true);

        TextField name = new TextField();
        name.setPromptText("Product Name (e.g. Pro Paddle)");
        TextField cat = new TextField();
        cat.setPromptText("Category (e.g. Equipment)");
        TextField price = new TextField();
        price.setPromptText("Price");

        Button save = new Button("Add Product");
        save.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        save.setOnAction(e -> {
            try {
                if (name.getText().isEmpty())
                    return;
                double p = Double.parseDouble(price.getText());
                productsList.add(new ProductItem(name.getText(), p, cat.getText()));
                renderProductList();
                closeProductForm();
            } catch (Exception ex) {
            }
        });

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        cancel.setOnAction(e -> closeProductForm());

        HBox actions = new HBox(10, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        productFormContainer.getChildren().addAll(
                new Label("Name"), name,
                new Label("Category"), cat,
                new Label("Price"), price,
                new Separator(), actions);

        productFormContainer.getChildren().forEach(n -> {
            if (n instanceof Label)
                n.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        });
    }

    private void closeProductForm() {
        productFormContainer.setManaged(false);
        productFormContainer.setVisible(false);
    }

    private VBox createPricingStep() {
        VBox box = createStepContainer("Extras Pricing", "Configure menu items and rental gear.");

        TabPane extrasTabs = new TabPane();
        extrasTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        extrasTabs.setStyle("-fx-background-color: transparent;");

        // --- CAFE TAB ---
        Tab cafeTab = new Tab("Cafe Menu");
        VBox cafeContent = new VBox(15);
        cafeContent.setStyle("-fx-padding: 15; -fx-background-color: transparent;");

        // Cafe Add Button
        Button addCafeBtn = new Button("+ Add Cafe Item");
        addCafeBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-size: 13px;");
        addCafeBtn.setMaxWidth(Double.MAX_VALUE);
        addCafeBtn.setOnAction(e -> {
            if (cafeFormContainer.isVisible())
                closeCafeForm();
            else
                openCafeForm(null);
        });

        // Cafe Form Container
        cafeFormContainer = new VBox(10);
        cafeFormContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        cafeFormContainer.setManaged(false);
        cafeFormContainer.setVisible(false);

        // Cafe List Container
        cafeListContainer = new VBox(10);
        ScrollPane cafeScroll = new ScrollPane(cafeListContainer);
        cafeScroll.setFitToWidth(true);
        cafeScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        cafeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cafeScroll.setMinHeight(200);

        cafeContent.getChildren().addAll(addCafeBtn, cafeFormContainer, cafeScroll);
        cafeTab.setContent(cafeContent);
        renderCafeList();

        // --- RENTALS TAB ---
        Tab rentalTab = new Tab("Equipment Rentals");
        VBox rentalContent = new VBox(15);
        rentalContent.setStyle("-fx-padding: 15; -fx-background-color: transparent;");

        // Rental Add Button
        Button addRentalBtn = new Button("+ Add Rental Gear");
        addRentalBtn.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-size: 13px;");
        addRentalBtn.setMaxWidth(Double.MAX_VALUE);
        addRentalBtn.setOnAction(e -> {
            if (rentalFormContainer.isVisible())
                closeRentalForm();
            else
                openRentalForm(null);
        });

        // Rental Form Container
        rentalFormContainer = new VBox(10);
        rentalFormContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        rentalFormContainer.setManaged(false);
        rentalFormContainer.setVisible(false);

        // Rental List Container
        rentalListContainer = new VBox(10);
        ScrollPane rentalScroll = new ScrollPane(rentalListContainer);
        rentalScroll.setFitToWidth(true);
        rentalScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        rentalScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rentalScroll.setMinHeight(200);

        rentalContent.getChildren().addAll(addRentalBtn, rentalFormContainer, rentalScroll);
        rentalTab.setContent(rentalContent);
        renderRentalList();

        extrasTabs.getTabs().addAll(cafeTab, rentalTab);
        box.getChildren().add(extrasTabs);
        return box;
    }

    // --- CAFE HELPER METHODS ---

    private void renderCafeList() {
        cafeListContainer.getChildren().clear();
        if (cafeList.isEmpty()) {
            Label placeholder = new Label("No cafe items. Add menu items like Water, Snacks.");
            placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            cafeListContainer.getChildren().add(placeholder);
            return;
        }
        for (CafeMenuItem item : cafeList) {
            cafeListContainer.getChildren().add(createCafeCard(item));
        }
    }

    private HBox createCafeCard(CafeMenuItem item) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        FontIcon icon = new FontIcon(Material2AL.LOCAL_CAFE);
        icon.setIconSize(20);
        icon.setIconColor(javafx.scene.paint.Color.web("#94A3B8"));

        VBox info = new VBox(2);
        Label name = new Label(item.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label cat = new Label(item.getCategory());
        cat.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        info.getChildren().addAll(name, cat);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label price = new Label(getCurrencySymbol() + item.getPrice());
        price.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");

        Button delBtn = new Button("×");
        delBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 16px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            cafeList.remove(item);
            renderCafeList();
        });

        card.getChildren().addAll(icon, info, spacer, price, delBtn);
        return card;
    }

    private void openCafeForm(CafeMenuItem editItem) { // Edit not supported yet, simple add
        cafeFormContainer.getChildren().clear();
        cafeFormContainer.setManaged(true);
        cafeFormContainer.setVisible(true);

        TextField n = new TextField();
        n.setPromptText("Item Name (e.g. Water Bottle)");
        n.setStyle("-fx-font-size: 13px;");

        TextField cat = new TextField();
        cat.setPromptText("Category (e.g. Drinks)");
        cat.setStyle("-fx-font-size: 13px;");

        TextField p = new TextField();
        p.setPromptText("Price");
        p.setStyle("-fx-font-size: 13px;");

        Button save = new Button("Add Item");
        save.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        save.setOnAction(e -> {
            try {
                if (n.getText().isEmpty())
                    return;
                double pr = Double.parseDouble(p.getText());
                cafeList.add(new CafeMenuItem(n.getText(), pr, cat.getText()));
                renderCafeList();
                closeCafeForm();
            } catch (Exception ex) {
            }
        });

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        cancel.setOnAction(e -> closeCafeForm());

        HBox actions = new HBox(10, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        cafeFormContainer.getChildren().addAll(
                new Label("Name"), n,
                new Label("Category"), cat,
                new Label("Price"), p,
                new Separator(), actions);

        // Style labels
        cafeFormContainer.getChildren().forEach(node -> {
            if (node instanceof Label)
                node.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        });
    }

    private void closeCafeForm() {
        cafeFormContainer.getChildren().clear();
        cafeFormContainer.setManaged(false);
        cafeFormContainer.setVisible(false);
    }

    // --- RENTAL HELPER METHODS ---

    private void renderRentalList() {
        rentalListContainer.getChildren().clear();
        if (rentalList.isEmpty()) {
            Label placeholder = new Label("No rentals. Add gear like Rackets, Balls.");
            placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            rentalListContainer.getChildren().add(placeholder);
            return;
        }
        for (RentalOption item : rentalList) {
            rentalListContainer.getChildren().add(createRentalCard(item));
        }
    }

    private HBox createRentalCard(RentalOption item) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        FontIcon icon = new FontIcon(Material2MZ.SPORTS_TENNIS);
        icon.setIconSize(20);
        icon.setIconColor(javafx.scene.paint.Color.web("#94A3B8"));

        VBox info = new VBox(2);
        Label name = new Label(item.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label type = new Label(item.getType().toString());
        type.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        info.getChildren().addAll(name, type);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label price = new Label(getCurrencySymbol() + item.getPrice());
        price.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");

        Button delBtn = new Button("×");
        delBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 16px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            rentalList.remove(item);
            renderRentalList();
        });

        card.getChildren().addAll(icon, info, spacer, price, delBtn);
        return card;
    }

    private void openRentalForm(RentalOption editItem) { // Edit not supported yet
        rentalFormContainer.getChildren().clear();
        rentalFormContainer.setManaged(true);
        rentalFormContainer.setVisible(true);

        TextField n = new TextField();
        n.setPromptText("Gear Name (e.g. Pro Racket)");
        n.setStyle("-fx-font-size: 13px;");

        ComboBox<com.managepickle.model.RentalOption.RentalType> typeBox = new ComboBox<>();
        typeBox.getItems().setAll(com.managepickle.model.RentalOption.RentalType.values());
        typeBox.getSelectionModel().selectFirst();
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField p = new TextField();
        p.setPromptText("Price");
        p.setStyle("-fx-font-size: 13px;");

        Button save = new Button("Add Gear");
        save.setStyle(
                "-fx-background-color: #A78BFA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        save.setOnAction(e -> {
            try {
                if (n.getText().isEmpty())
                    return;
                double pr = Double.parseDouble(p.getText());
                rentalList.add(new RentalOption(n.getText(), pr, typeBox.getValue()));
                renderRentalList();
                closeRentalForm();
            } catch (Exception ex) {
            }
        });

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        cancel.setOnAction(e -> closeRentalForm());

        HBox actions = new HBox(10, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        rentalFormContainer.getChildren().addAll(
                new Label("Name"), n,
                new Label("Charge Type"), typeBox,
                new Label("Price"), p,
                new Separator(), actions);

        // Style labels
        rentalFormContainer.getChildren().forEach(node -> {
            if (node instanceof Label)
                node.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        });
    }

    private void closeRentalForm() {
        rentalFormContainer.getChildren().clear();
        rentalFormContainer.setManaged(false);
        rentalFormContainer.setVisible(false);
    }

    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/managepickle/login.fxml"));
            Stage stage = (Stage) nextButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            com.managepickle.utils.ThemeManager.applyTheme(stage.getScene());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCurrencySymbol() {
        if (currencyField != null && currencyField.getValue() != null) {
            return currencyField.getValue().split("\\s")[0];
        }
        return "₹";
    }

    private void showAlert(String msg) {
        if (notificationLabel != null) {
            notificationLabel.setText(msg);

            // Set Graphic (Icon)
            FontIcon warnIcon = new FontIcon(Material2AL.INFO);
            warnIcon.setIconSize(16);
            warnIcon.setIconColor(javafx.scene.paint.Color.web("#EF4444"));
            notificationLabel.setGraphic(warnIcon);
            notificationLabel.setGraphicTextGap(10);

            notificationLabel.setManaged(true);
            notificationLabel.setVisible(true);

            // Pulse effect to draw attention
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                    notificationLabel);
            ft.setFromValue(0.4);
            ft.setToValue(1.0);
            ft.setCycleCount(2);
            ft.setAutoReverse(true);
            ft.play();
        } else {
            // Fallback
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(msg);
            alert.show();
        }
    }

    private void hideNotification() {
        if (notificationLabel != null) {
            notificationLabel.setManaged(false);
            notificationLabel.setVisible(false);
        }
    }

    private boolean checkPricingCoverage() {
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Interval> opIntervals = new ArrayList<>();
            for (OperatingRule op : hoursList) {
                if (op.appliesTo(day)) {
                    if (op.isClosed())
                        continue;

                    LocalTime start = op.is24Hours() ? LocalTime.MIN
                            : LocalTime.parse("24:00".equals(op.getOpenTime()) ? "00:00" : op.getOpenTime());

                    // Handle "00:00" as end of day if it occurs as closeTime
                    String closeStr = op.getCloseTime();
                    LocalTime end = op.is24Hours() ? LocalTime.of(23, 59)
                            : LocalTime.parse("24:00".equals(closeStr) ? "00:00" : closeStr);
                    if (!op.is24Hours() && (end.equals(LocalTime.MIN) || end.isBefore(start))) {
                        end = LocalTime.MAX;
                    }

                    opIntervals.add(new Interval(start, end));
                }
            }

            if (opIntervals.isEmpty())
                continue;

            List<Interval> rateIntervals = new ArrayList<>();
            for (PricingRule pr : ratesList) {
                if (pr.getDays().contains(day)) {
                    LocalTime start = LocalTime.parse("24:00".equals(pr.getStartTime()) ? "00:00" : pr.getStartTime());
                    LocalTime end = LocalTime.parse("24:00".equals(pr.getEndTime()) ? "00:00" : pr.getEndTime());
                    // Normalize end time
                    if (end.equals(LocalTime.MIN) || end.isBefore(start)) {
                        end = LocalTime.MAX;
                    }
                    rateIntervals.add(new Interval(start, end));
                }
            }

            for (Interval opInt : opIntervals) {
                List<Interval> gaps = findGaps(opInt, rateIntervals);
                if (!gaps.isEmpty()) {
                    String dayName = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    StringBuilder sb = new StringBuilder("Pricing missing for " + dayName + ": ");
                    for (int i = 0; i < gaps.size(); i++) {
                        sb.append(gaps.get(i).start).append(" to ")
                                .append(gaps.get(i).end.equals(LocalTime.MAX) ? "00:00" : gaps.get(i).end);
                        if (i < gaps.size() - 1)
                            sb.append(", ");
                    }
                    showAlert(sb.toString());
                    return false;
                }
            }
        }
        return true;
    }

    private static class Interval {
        LocalTime start, end;

        Interval(LocalTime s, LocalTime e) {
            this.start = s;
            this.end = e;
        }
    }

    private List<Interval> findGaps(Interval target, List<Interval> coverages) {
        List<Interval> gaps = new ArrayList<>();
        gaps.add(target);

        for (Interval cover : coverages) {
            List<Interval> nextGaps = new ArrayList<>();
            for (Interval gap : gaps) {
                // If no overlap
                if (!cover.start.isBefore(gap.end) || !cover.end.isAfter(gap.start)) {
                    nextGaps.add(gap);
                } else {
                    // Overlap exists
                    if (cover.start.isAfter(gap.start)) {
                        nextGaps.add(new Interval(gap.start, cover.start));
                    }
                    if (cover.end.isBefore(gap.end)) {
                        nextGaps.add(new Interval(cover.end, gap.end));
                    }
                }
            }
            gaps = nextGaps;
        }

        // Filter out zero-length gaps
        gaps.removeIf(g -> !g.start.isBefore(g.end));

        return gaps;
    }
}
