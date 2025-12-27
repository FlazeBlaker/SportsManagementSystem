package com.managepickle.controller;

import com.managepickle.database.BookingDAO;
import com.managepickle.database.CourtDAO;
import com.managepickle.model.Booking;
import com.managepickle.model.Court;
import com.managepickle.model.AppConfig;
import com.managepickle.utils.ConfigManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class BookingsController {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private GridPane bookingGrid;
    @FXML
    private HBox courtsHeader;
    @FXML
    private VBox timeLabels;
    @FXML
    private javafx.scene.control.DatePicker datePicker;
    @FXML
    private javafx.scene.layout.VBox sidePanel;
    @FXML
    private javafx.scene.layout.StackPane bookingInfoPane;
    @FXML
    private javafx.scene.layout.StackPane pricingPane;
    @FXML
    private javafx.scene.layout.StackPane cafePane;
    @FXML
    private javafx.scene.layout.StackPane receiptPane;
    @FXML
    private javafx.scene.control.TabPane mainTabPane;
    @FXML
    private javafx.scene.control.Tab bookingTab;
    @FXML
    private javafx.scene.control.Tab pricingTab;
    @FXML
    private javafx.scene.control.Tab cafeTab;
    @FXML
    private javafx.scene.control.Tab receiptTab;

    private LocalDate currentDate = LocalDate.now();

    // Track slots by booking ID for multi-slot highlighting
    private java.util.Map<Integer, java.util.List<StackPane>> bookingSlots = new java.util.HashMap<>();
    // Track original styles for slots to restore on mouse exit
    private java.util.Map<StackPane, String> slotOriginalStyles = new java.util.HashMap<>();

    // Default theme colors
    private String panelColor = "#1E293B";
    private String outlineColor = "#10B981";

    @FXML
    public void initialize() {
        try {
            // Load theme colors and config
            var config = ConfigManager.getConfig();
            if (config != null) {
                if (config.getPanelColor() != null)
                    panelColor = config.getPanelColor();
                if (config.getOutlineColor() != null)
                    outlineColor = config.getOutlineColor();

                // Hide tabs based on config
                boolean hasCafe = config.isHasCafe();
                boolean hasRentals = config.isHasRentals();

                if (!hasCafe && !hasRentals) {
                    mainTabPane.getTabs().remove(cafeTab);
                } else if (!hasCafe && hasRentals) {
                    cafeTab.setText("Rentals");
                } else if (hasCafe && !hasRentals) {
                    cafeTab.setText("Cafe");
                } else {
                    cafeTab.setText("Cafe & Gear");
                }
                // Note: Pricing tab is always shown for court fees, but rental section is
                // optional inside it
            }

            // Set up DatePicker
            datePicker.setValue(currentDate);
            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    currentDate = newVal;
                    System.out.println("DEBUG: Date Changed to: " + currentDate);
                    refresh();
                }
            });

            refresh();
        } catch (Exception e) {
            System.err.println("Error initializing BookingsController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refresh() {
        try {
            buildPremiumGrid();
        } catch (Exception e) {
            System.err.println("Error refreshing bookings grid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildPremiumGrid() {
        com.managepickle.model.OperatingHours hours = com.managepickle.service.PricingService
                .getOperatingHours(currentDate);

        courtsHeader.getChildren().clear();
        timeLabels.getChildren().clear();
        bookingGrid.getChildren().clear();

        if (hours.isClosed()) {
            Label closedLabel = new Label("VENUE CLOSED ON " + currentDate.getDayOfWeek());
            closedLabel.setStyle(
                    "-fx-text-fill: #EF4444; -fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 100 0 0 0;");
            bookingGrid.add(closedLabel, 0, 0);
            return;
        }

        int startHour = hours.getStartHour();
        int endHour = hours.getEndHour();

        List<Court> courts = CourtDAO.getAllCourts();

        if (courts.isEmpty()) {
            AppConfig config = ConfigManager.getConfig();
            String rName = config != null && config.getResourceNamePlural() != null ? config.getResourceNamePlural()
                    : "resources";
            Label emptyLabel = new Label(
                    "No " + rName.toLowerCase() + " configured. Please add " + rName.toLowerCase() + " in Settings.");
            emptyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 50;");
            bookingGrid.add(emptyLabel, 0, 0);
            return;
        }

        // Build Premium Court Headers
        for (Court c : courts) {
            VBox headerCard = new VBox(8);
            headerCard.setAlignment(Pos.CENTER);
            headerCard.setPrefWidth(200);
            headerCard.setStyle(String.format(
                    "-fx-background-color: %s;" +
                            "-fx-background-radius: 20 20 0 0; -fx-padding: 20 15; -fx-border-width: 0 0 3 0; " +
                            "-fx-border-color: %s; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 5);",
                    panelColor, outlineColor));
            Label courtName = new Label(c.getName());
            courtName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

            String envType = c.getEnvironmentType();
            if (envType == null)
                envType = c.getType(); // Fallback

            Label courtType = new Label(envType.toUpperCase());
            courtType.setStyle(String.format(
                    "-fx-font-size: 11px; -fx-text-fill: %s; -fx-padding: 4 12; " +
                            "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-background-radius: 12;",
                    outlineColor));

            org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(
                    c.getIconCode() != null && !c.getIconCode().isEmpty() ? c.getIconCode() : "mzl-layers");
            icon.setIconSize(24);
            icon.setIconColor(javafx.scene.paint.Color.web(outlineColor));

            Label status = new Label("● AVAILABLE");
            status.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981;");

            headerCard.getChildren().addAll(icon, courtName, courtType, status);
            courtsHeader.getChildren().add(headerCard);
        }

        // Build Time Grid
        int row = 0;
        LocalTime currentTime = LocalTime.now();

        for (int h = startHour; h <= endHour; h++) {
            try {
                VBox timeBlock = new VBox(5);
                timeBlock.setPrefHeight(70);
                timeBlock.setPrefWidth(70);
                timeBlock.setAlignment(Pos.TOP_RIGHT);

                Label hourLabel = new Label(String.format("%02d:00", h));
                hourLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

                Label periodLabel = new Label(h < 12 ? "AM" : "PM");
                periodLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");

                timeBlock.getChildren().addAll(hourLabel, periodLabel);

                if (h == currentTime.getHour()) {
                    timeBlock.setStyle(
                            "-fx-background-color: rgba(124, 58, 237, 0.2); -fx-background-radius: 12; -fx-padding: 10;");
                }

                timeLabels.getChildren().add(timeBlock);

                for (int col = 0; col < courts.size(); col++) {
                    try {
                        StackPane slot = createPremiumSlot(courts.get(col), h);
                        bookingGrid.add(slot, col, row);
                    } catch (Exception slotEx) {
                        System.err.println(
                                "Error creating slot for court " + col + " hour " + h + ": " + slotEx.getMessage());
                        StackPane errorSlot = new StackPane(new Label("!"));
                        bookingGrid.add(errorSlot, col, row);
                    }
                }
                row++;
            } catch (Exception rowEx) {
                System.err.println("Error creating row for hour " + h + ": " + rowEx.getMessage());
            }
        }

        // Clear tracking maps before rendering
        bookingSlots.clear();
        slotOriginalStyles.clear();
        System.out.println("[DEBUG] Cleared booking slots map");
        renderPremiumBookings(courts, startHour, endHour);
    }

    private StackPane createPremiumSlot(Court court, int hour) {
        StackPane slot = new StackPane();
        slot.setPrefSize(200, 70);

        // Ultra-transparent glass effect
        String glassStyle = "-fx-background-color: rgba(255, 255, 255, 0.03);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 8, 0, 0, 2)," +
                "            innershadow(gaussian, rgba(255, 255, 255, 0.1), 5, 0, 0, 1);" +
                "-fx-cursor: hand;";

        // Check if there's a booking for this slot on the selected date
        LocalDateTime slotStart = LocalDateTime.of(currentDate, LocalTime.of(hour, 0));
        LocalDateTime slotEnd = slotStart.plusHours(1);

        Booking booking = BookingDAO.getAllBookings().stream()
                .filter(b -> b.getCourtId() == court.getId())
                .filter(b -> b.getStartTime().toLocalDate().equals(currentDate))
                .filter(b -> {
                    LocalDateTime bStart = b.getStartTime();
                    LocalDateTime bEnd = b.getEndTime();
                    return (bStart.isBefore(slotEnd) && bEnd.isAfter(slotStart));
                })
                .findFirst()
                .orElse(null);

        if (booking != null) {
            // Determine color based on payment status
            String statusColor, statusColorDark, statusLabel;

            if (booking.isNoShow()) {
                // BLUE for no-show
                statusColor = "59, 130, 246"; // Blue
                statusColorDark = "37, 99, 235";
                statusLabel = "No Show";
            } else if (booking.getPaidAmount() >= booking.getTotalAmount()) {
                // GREEN for fully paid
                statusColor = "34, 197, 94"; // Green
                statusColorDark = "22, 163, 74";
                statusLabel = "Paid";
            } else {
                // RED for unpaid/partial
                statusColor = "239, 68, 68"; // Red
                statusColorDark = "220, 38, 38";
                statusLabel = booking.getPaidAmount() > 0 ? "Partial" : "Unpaid";
            }

            // Booked slot with status color
            slot.setStyle(
                    String.format(
                            "-fx-background-color: linear-gradient(to bottom, rgba(%s, 0.3), rgba(%s, 0.15));",
                            statusColor, statusColor) +
                            "-fx-background-radius: 12;" +
                            String.format("-fx-border-color: rgba(%s, 0.5);", statusColor) +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 12;" +
                            String.format("-fx-effect: dropshadow(gaussian, rgba(%s, 0.4), 12, 0, 0, 3),", statusColor)
                            +
                            String.format("            innershadow(gaussian, rgba(%s, 0.2), 8, 0, 0, 1);", statusColor)
                            +
                            "-fx-cursor: hand;");

            VBox content = new VBox(4);
            content.setAlignment(javafx.geometry.Pos.CENTER);

            Label customerLabel = new Label(booking.getCustomerName());
            customerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 2, 0, 0, 1);");

            Label statusBadge = new Label(statusLabel);
            statusBadge.setStyle(String.format(
                    "-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold;" +
                            "-fx-background-color: rgba(%s, 0.8);" +
                            "-fx-padding: 2 6; -fx-background-radius: 8;",
                    statusColorDark));

            Label timeLabel = new Label(
                    booking.getStartTime().toLocalTime().toString() + " - "
                            + booking.getEndTime().toLocalTime().toString());
            timeLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 11px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 1, 0, 0, 1);");

            content.getChildren().addAll(customerLabel, statusBadge, timeLabel);
            slot.getChildren().add(content);

            // Track this slot for multi-slot highlighting
            bookingSlots.computeIfAbsent(booking.getId(), k -> new java.util.ArrayList<>()).add(slot);
            // Store original style for this slot
            slotOriginalStyles.put(slot, slot.getStyle());
            System.out.println("[DEBUG] Added slot for booking ID: " + booking.getId()
                    + ", total slots for this booking: " + bookingSlots.get(booking.getId()).size());

            // Click to view details
            final Booking finalBooking = booking;
            slot.setOnMouseClicked(e -> showBookingDetails(finalBooking));

            // Hover effect for booked slots - highlights ALL related slots
            final String finalStatusColor = statusColor;
            final int bookingId = booking.getId();
            slot.setOnMouseEntered(e -> {
                System.out.println("[DEBUG] Mouse entered booking ID: " + bookingId);
                // Highlight all slots for this booking
                java.util.List<StackPane> relatedSlots = bookingSlots.get(bookingId);
                System.out.println(
                        "[DEBUG] Related slots count: " + (relatedSlots != null ? relatedSlots.size() : "null"));
                if (relatedSlots != null) {
                    for (StackPane relatedSlot : relatedSlots) {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), relatedSlot);
                        st.setToX(1.03);
                        st.setToY(1.03);
                        st.play();
                        relatedSlot.setStyle(
                                String.format(
                                        "-fx-background-color: linear-gradient(to bottom, rgba(%s, 0.4), rgba(%s, 0.2));",
                                        finalStatusColor, finalStatusColor) +
                                        "-fx-background-radius: 12;" +
                                        String.format("-fx-border-color: rgba(%s, 0.7);", finalStatusColor) +
                                        "-fx-border-width: 2;" +
                                        "-fx-border-radius: 12;" +
                                        String.format("-fx-effect: dropshadow(gaussian, rgba(%s, 0.6), 16, 0, 0, 4),",
                                                finalStatusColor)
                                        +
                                        "            innershadow(gaussian, rgba(255, 255, 255, 0.3), 10, 0, 0, 2);" +
                                        "-fx-cursor: hand;");
                    }
                }
            });

            slot.setOnMouseExited(e -> {
                System.out.println("[DEBUG] Mouse exited booking ID: " + bookingId);
                // Reset all slots for this booking to their original styles
                java.util.List<StackPane> relatedSlots = bookingSlots.get(bookingId);
                if (relatedSlots != null) {
                    for (StackPane relatedSlot : relatedSlots) {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), relatedSlot);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        st.play();
                        String originalStyle = slotOriginalStyles.get(relatedSlot);
                        if (originalStyle != null) {
                            relatedSlot.setStyle(originalStyle);
                        }
                    }
                }
            });
        } else {
            // Available slot
            slot.setStyle(glassStyle);

            Label availableLabel = new Label("Available");
            availableLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 2, 0, 0, 1);");
            slot.getChildren().add(availableLabel);

            slot.setOnMouseClicked(e -> handleSlotClick(court.getId(), hour));

            slot.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), slot);
                st.setToX(1.03);
                st.setToY(1.03);
                st.play();
                slot.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.08);" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                                "-fx-border-width: 1.5;" +
                                "-fx-border-radius: 12;" +
                                "-fx-effect: dropshadow(gaussian, rgba(124, 58, 237, 0.3), 12, 0, 0, 3)," +
                                "            innershadow(gaussian, rgba(255, 255, 255, 0.2), 8, 0, 0, 1);" +
                                "-fx-cursor: hand;");
            });

            slot.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), slot);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
                slot.setStyle(glassStyle);
            });
        }

        return slot;
    }

    private void renderPremiumBookings(List<Court> courts, int startHour, int endHour) {
        // Bookings are now rendered directly in createPremiumSlot
        // This method is kept for compatibility but does nothing
    }

    private void showBookingDetails(Booking booking) {
        // Load booking details into the booking info panel
        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/managepickle/booking_dialog.fxml"));
            javafx.scene.Parent formView = fxmlLoader.load();

            BookingDialogController dialogController = fxmlLoader.getController();
            dialogController.setParentController(this);
            dialogController.setBookingToEdit(booking);

            // Load into the booking info pane
            bookingInfoPane.getChildren().clear();
            bookingInfoPane.getChildren().add(formView);

            // Load pricing breakdown
            displayPricingBreakdown(booking);

            // Load cafe panel and receipt
            displayCafePanel(booking);
            displayReceipt(booking);
        } catch (Exception e) {
            System.err.println("Error loading booking details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearBookingForm() {
        // Clear form but keep panel open with empty state
        bookingInfoPane.getChildren().clear();
        Label placeholderLabel = new Label("📋 Select or create a booking to view details");
        placeholderLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-padding: 40;");
        bookingInfoPane.getChildren().add(placeholderLabel);
    }

    // Dynamic Labels for Live Updates
    private Label lblTotalAmount;
    private Label lblBalance;
    private Label lblStatusBadge;

    private void displayPricingBreakdown(Booking booking) {
        pricingPane.getChildren().clear();

        javafx.scene.layout.VBox pricingContent = new javafx.scene.layout.VBox(15);
        pricingContent.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        pricingContent.setStyle("-fx-padding: 20;");

        try {
            // Title
            Label titleLabel = new Label("Pricing Breakdown");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label subtitleLabel = new Label("Booking #" + booking.getId() + " - " + booking.getCustomerName());
            subtitleLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

            // Divider
            javafx.scene.control.Separator separator1 = new javafx.scene.control.Separator();
            separator1.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

            pricingContent.getChildren().addAll(
                    titleLabel, subtitleLabel, separator1);

            // Get currency and calculate extras for later use
            if (booking.getStartTime() == null || booking.getEndTime() == null) {
                pricingContent.getChildren().add(new Label("Error: Invalid booking times"));
                pricingPane.getChildren().add(pricingContent);
                return;
            }

            String currency = ConfigManager.getConfig().getCurrencySymbol();
            if (currency == null)
                currency = "₹";

            double durationMinutes = java.time.Duration.between(booking.getStartTime(), booking.getEndTime())
                    .toMinutes();
            double durationHours = durationMinutes / 60.0;

            // Calculate Base Court Cost (Total - Extras)
            double currentTotal = booking.getTotalAmount();
            double extrasTotal = calculateCafeTotal(booking);
            double courtCost = currentTotal - extrasTotal;

            // Court rental breakdown
            Label courtLabel = new Label("Court Rental");
            courtLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

            javafx.scene.layout.HBox courtLineItem = createPricingLineItem(
                    String.format("%.1f hour(s)", durationHours),
                    String.format("%s%.2f", currency, courtCost));

            pricingContent.getChildren().addAll(courtLabel, courtLineItem);

            // RENTALS SECTION
            if (ConfigManager.getConfig().isHasRentals() && ConfigManager.getConfig().getRentalOptions() != null) {
                javafx.scene.control.Separator sepRental = new javafx.scene.control.Separator();
                sepRental.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

                Label rentalLabel = new Label("Equipment Rentals");
                rentalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

                pricingContent.getChildren().addAll(sepRental, rentalLabel);

                // Parse existing items for pre-population
                JSONObject existingItems = new JSONObject();
                String itemsJson = booking.getCafeItems();
                if (itemsJson != null && !itemsJson.isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(itemsJson);
                        JSONArray arr = json.optJSONArray("items");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                existingItems.put(obj.getString("name"), obj);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                for (com.managepickle.model.RentalOption option : ConfigManager.getConfig().getRentalOptions()) {
                    HBox row = new HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label name = new Label(option.getName());
                    name.setStyle("-fx-text-fill: #CBD5E1; -fx-min-width: 120;");
                    // REMOVED: Price Text Label as requested

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Logic inputs
                    // Determine existing values
                    int startQty = 0;
                    int startHrs = (int) Math.ceil(durationHours);

                    if (existingItems.has(option.getName())) {
                        JSONObject existing = existingItems.getJSONObject(option.getName());
                        startQty = existing.optInt("qty", 0);
                        // If it's hourly, try to get hrs, else default to duration
                        // Note: Previous logic didn't explicitly store 'hours' separately in JSON other
                        // than inside name string?
                        // Let's look at `addCafeItem`: "name + (qty x hrs)".
                        // Ah, we need a better storage structure if we want to reverse it perfectly.
                        // BUT for this refactor, let's just use the 'qty' and default 'hrs' to duration
                        // unless we can parse it. Since we are refactoring, let's stick to:
                        // Default Hrs = Duration. Qty = 0 (or existing value).
                    }

                    if (option.getType() == com.managepickle.model.RentalOption.RentalType.HOURLY) {
                        // Default Hrs should match booking duration
                        final int durationHrsInt = (startHrs < 1) ? 1 : startHrs;

                        javafx.scene.control.Spinner<Integer> qtySpin = new javafx.scene.control.Spinner<>(0, 50,
                                startQty);
                        qtySpin.setPrefWidth(90);

                        javafx.scene.control.Spinner<Integer> hrSpin = new javafx.scene.control.Spinner<>(1, 24,
                                durationHrsInt);
                        hrSpin.setPrefWidth(90);

                        // Listeners for Live Update
                        qtySpin.valueProperty().addListener(
                                (o, oldVal, newVal) -> handleRentalChange(booking, option, newVal, hrSpin.getValue()));

                        hrSpin.valueProperty().addListener(
                                (o, oldVal, newVal) -> handleRentalChange(booking, option, qtySpin.getValue(), newVal));

                        row.getChildren().addAll(name, spacer, new Label("Qty:"), qtySpin, new Label("Hrs:"), hrSpin);
                    } else {
                        // Flat Rate
                        javafx.scene.control.Spinner<Integer> qtySpin = new javafx.scene.control.Spinner<>(0, 50,
                                startQty);
                        qtySpin.setPrefWidth(90);

                        qtySpin.valueProperty()
                                .addListener((o, oldVal, newVal) -> handleRentalChange(booking, option, newVal, 1));

                        row.getChildren().addAll(name, spacer, new Label("Qty:"), qtySpin);
                    }
                    pricingContent.getChildren().add(row);
                }
            }

            // EXTRA ITEMS BREAKDOWN (Itemized list of rentals and cafe items)
            String itemsJson = booking.getCafeItems();
            if (itemsJson != null && !itemsJson.isEmpty()) {
                try {
                    JSONArray itemsArray;
                    if (itemsJson.trim().startsWith("[")) {
                        itemsArray = new JSONArray(itemsJson);
                    } else {
                        JSONObject root = new JSONObject(itemsJson);
                        itemsArray = root.optJSONArray("items");
                    }

                    if (itemsArray != null && itemsArray.length() > 0) {
                        javafx.scene.control.Separator sepExtras = new javafx.scene.control.Separator();
                        sepExtras.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

                        Label extrasLabel = new Label("Extra Items");
                        extrasLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

                        pricingContent.getChildren().addAll(sepExtras, extrasLabel);

                        // List each item with its cost
                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject item = itemsArray.getJSONObject(i);
                            String itemName = item.getString("name");
                            int qty = item.optInt("qty", 1);
                            double itemTotal = item.optDouble("total", 0);

                            String displayName = itemName + " × " + qty;

                            javafx.scene.layout.HBox itemLine = createPricingLineItem(
                                    displayName,
                                    String.format("%s%.2f", currency, itemTotal));
                            itemLine.setStyle("-fx-font-size: 13px; -fx-padding: 2 0;");
                            pricingContent.getChildren().add(itemLine);
                        }

                        // Show extras subtotal
                        javafx.scene.layout.HBox extrasSubtotalLine = createPricingLineItem(
                                "Extras Subtotal",
                                String.format("%s%.2f", currency, extrasTotal));
                        extrasSubtotalLine
                                .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");
                        pricingContent.getChildren().add(extrasSubtotalLine);
                    }
                } catch (Exception ex) {
                    System.err.println("Error parsing extras for display: " + ex.getMessage());
                }
            }

            // Total section
            javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator();
            separator2.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

            lblTotalAmount = new Label(String.format("%s%.2f", currency, booking.getTotalAmount()));
            lblTotalAmount.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            javafx.scene.layout.HBox totalLine = createPricingLineItem("Total Amount", "");
            totalLine.getChildren().set(2, lblTotalAmount); // Replace value node
            totalLine.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            javafx.scene.layout.HBox paidLine = createPricingLineItem("Paid",
                    String.format("%s%.2f", currency, booking.getPaidAmount()));
            paidLine.setStyle("-fx-font-size: 14px;");

            double balance = booking.getTotalAmount() - booking.getPaidAmount();

            lblBalance = new Label(String.format("%s%.2f", currency, balance));
            String balanceColor = balance > 0.01 ? "#EF4444" : "#22C55E";
            lblBalance.setStyle("-fx-text-fill: " + balanceColor + "; -fx-font-size: 18px; -fx-font-weight: bold;");

            javafx.scene.layout.HBox balanceLine = createPricingLineItem("Balance Due", "");
            balanceLine.getChildren().set(2, lblBalance);

            // Payment status badge
            String statusText = balance > 0.01 ? (booking.getPaidAmount() > 0 ? "PARTIALLY PAID" : "UNPAID")
                    : "PAID IN FULL";
            String badgeColor = balance > 0.01 ? (booking.getPaidAmount() > 0 ? "#F59E0B" : "#EF4444") : "#22C55E";

            lblStatusBadge = new Label(statusText);
            lblStatusBadge.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; " +
                            "-fx-padding: 8 16; -fx-background-radius: 8; " +
                            "-fx-font-size: 12px; -fx-font-weight: bold;",
                    badgeColor));

            pricingContent.getChildren().addAll(
                    separator2,
                    totalLine, paidLine, balanceLine,
                    new javafx.scene.layout.Region(), // Spacer
                    lblStatusBadge);

            pricingPane.getChildren().add(pricingContent);

        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error details: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #EF4444;");
            pricingPane.getChildren().add(errorLabel);
        }
    }

    private javafx.scene.layout.HBox createPricingLineItem(String label, String value) {
        javafx.scene.layout.HBox line = new javafx.scene.layout.HBox();
        line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 14px;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        line.getChildren().addAll(labelNode, spacer, valueNode);
        return line;
    }

    private void handleSlotClick(int courtId, int hour) {
        handleNewBooking(courtId, hour);
    }

    @FXML
    private void handleNewBookingButton() {
        handleNewBooking(-1, -1);
    }

    private void handleNewBooking(int courtId, int hour) {
        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/managepickle/booking_dialog.fxml"));
            javafx.scene.Parent formView = fxmlLoader.load();

            BookingDialogController dialogController = fxmlLoader.getController();
            dialogController.setParentController(this);

            // Smart Pre-selection Logic
            Court selectedCourt = null;
            LocalTime selectedTime = null;

            if (courtId != -1 && hour != -1) {
                selectedCourt = CourtDAO.getAllCourts().stream()
                        .filter(c -> c.getId() == courtId)
                        .findFirst()
                        .orElse(null);

                selectedTime = LocalTime.of(hour, 0);
            }

            dialogController.setBookingContext(selectedCourt, selectedTime, currentDate);

            // Load into Booking Info Tab in Side Panel
            bookingInfoPane.getChildren().setAll(formView);

            // Animation for visual feedback
            FadeTransition ft = new FadeTransition(Duration.millis(300), formView);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            System.err.println("Error opening booking form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayCafePanel(Booking booking) {
        cafePane.getChildren().clear();

        VBox cafeContent = new VBox(15);
        cafeContent.setStyle("-fx-padding: 20;");
        cafeContent.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label title = new Label("Cafe & Refreshments");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("Select items to add");
        subtitle.setStyle("-fx-text-fill: #94A3B8;");

        cafeContent.getChildren().addAll(title, subtitle, new javafx.scene.control.Separator());

        // Cafe Items List from Config
        var config = ConfigManager.getConfig();
        String currency = config.getCurrencySymbol();
        if (currency == null)
            currency = "₹";

        // Parse existing items for pre-population
        JSONObject existingItems = new JSONObject();
        String itemsJson = booking.getCafeItems();
        if (itemsJson != null && !itemsJson.isEmpty()) {
            try {
                JSONObject root;
                if (itemsJson.trim().startsWith("[")) {
                    // Backward compatibility for array root
                    JSONArray arr = new JSONArray(itemsJson);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        existingItems.put(obj.getString("name"), obj);
                    }
                } else {
                    root = new JSONObject(itemsJson);
                    JSONArray arr = root.optJSONArray("items");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            existingItems.put(obj.getString("name"), obj);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        List<com.managepickle.model.CafeMenuItem> menu = config.getCafeMenu();
        if (menu != null) {
            for (com.managepickle.model.CafeMenuItem item : menu) {
                String name = item.getName();

                HBox itemRow = new HBox(15);
                itemRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                itemRow.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 8;");

                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120;");

                // Determine existing quantity
                int startQty = 0;
                if (existingItems.has(name)) {
                    startQty = existingItems.getJSONObject(name).optInt("qty", 0);
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                javafx.scene.control.Spinner<Integer> qtySpinner = new javafx.scene.control.Spinner<>(0, 50, startQty);
                qtySpinner.setPrefWidth(90);

                // Live Listener
                qtySpinner.valueProperty().addListener((o, oldVal, newVal) -> handleCafeChange(booking, item, newVal));

                itemRow.getChildren().addAll(nameLabel, spacer, new Label("Qty:"), qtySpinner);
                cafeContent.getChildren().add(itemRow);
            }
        }

        cafePane.getChildren().add(cafeContent);
    }

    private void handleCafeChange(Booking booking, com.managepickle.model.CafeMenuItem item, int newQty) {
        try {
            double oldExtras = calculateCafeTotal(booking);
            double oldTotal = booking.getTotalAmount();
            double courtCost = oldTotal - oldExtras;
            if (courtCost < 0)
                courtCost = 0;

            JSONObject root;
            JSONArray itemsArray;
            String currentItems = booking.getCafeItems();

            // Robust parsing
            if (currentItems == null || currentItems.isEmpty()) {
                root = new JSONObject();
                itemsArray = new JSONArray();
                root.put("items", itemsArray);
            } else if (currentItems.trim().startsWith("[")) {
                itemsArray = new JSONArray(currentItems);
                root = new JSONObject();
                root.put("items", itemsArray);
            } else {
                try {
                    root = new JSONObject(currentItems);
                    itemsArray = root.optJSONArray("items");
                    if (itemsArray == null) {
                        itemsArray = new JSONArray();
                        root.put("items", itemsArray);
                    }
                } catch (Exception e) {
                    root = new JSONObject();
                    itemsArray = new JSONArray();
                    root.put("items", itemsArray);
                }
            }

            // Update Logic
            boolean found = false;
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject obj = itemsArray.getJSONObject(i);
                if (obj.getString("name").equals(item.getName())) {
                    found = true;
                    if (newQty > 0) {
                        obj.put("qty", newQty);
                        obj.put("total", item.getPrice() * newQty);
                        newArray.put(obj);
                    }
                    // if 0, remove
                } else {
                    newArray.put(obj);
                }
            }

            if (!found && newQty > 0) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.getName());
                obj.put("price", item.getPrice());
                obj.put("qty", newQty);
                obj.put("total", item.getPrice() * newQty);
                newArray.put(obj);
            }

            root.put("items", newArray);
            booking.setCafeItems(root.toString());

            double newExtras = calculateCafeTotal(booking);
            double newTotal = courtCost + newExtras;
            booking.setTotalAmount(newTotal);

            BookingDAO.updateBooking(booking);

            // Live UI Updates
            String currency = ConfigManager.getConfig().getCurrencySymbol();
            if (currency == null)
                currency = "₹";

            if (lblTotalAmount != null)
                lblTotalAmount.setText(String.format("%s%.2f", currency, newTotal));

            double paid = booking.getPaidAmount();
            double balance = newTotal - paid;

            if (lblBalance != null) {
                lblBalance.setText(String.format("%s%.2f", currency, balance));
                String balanceColor = balance > 0.01 ? "#EF4444" : "#22C55E";
                lblBalance.setStyle("-fx-text-fill: " + balanceColor + "; -fx-font-size: 18px; -fx-font-weight: bold;");
            }

            if (lblStatusBadge != null) {
                String statusText = balance > 0.01 ? (paid > 0 ? "PARTIALLY PAID" : "UNPAID") : "PAID IN FULL";
                String badgeColor = balance > 0.01 ? (paid > 0 ? "#F59E0B" : "#EF4444") : "#22C55E";
                lblStatusBadge.setText(statusText);
                lblStatusBadge.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: white; " +
                                "-fx-padding: 8 16; -fx-background-radius: 8; " +
                                "-fx-font-size: 12px; -fx-font-weight: bold;",
                        badgeColor));
            }

            displayReceipt(booking);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRentalChange(Booking booking, com.managepickle.model.RentalOption option, int newQty,
            int newHrs) {
        try {
            double oldExtras = calculateCafeTotal(booking);
            double oldTotal = booking.getTotalAmount();
            double courtCost = oldTotal - oldExtras;
            if (courtCost < 0)
                courtCost = 0;

            JSONObject root;
            JSONArray itemsArray;
            String currentItems = booking.getCafeItems();

            // Robust parsing
            if (currentItems == null || currentItems.isEmpty()) {
                root = new JSONObject();
                itemsArray = new JSONArray();
                root.put("items", itemsArray);
            } else if (currentItems.trim().startsWith("[")) {
                itemsArray = new JSONArray(currentItems);
                root = new JSONObject();
                root.put("items", itemsArray);
            } else {
                try {
                    root = new JSONObject(currentItems);
                    itemsArray = root.optJSONArray("items");
                    if (itemsArray == null) {
                        itemsArray = new JSONArray();
                        root.put("items", itemsArray);
                    }
                } catch (Exception e) {
                    // Fallback
                    root = new JSONObject();
                    itemsArray = new JSONArray();
                    root.put("items", itemsArray);
                }
            }

            // Update Logic
            boolean found = false;
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject obj = itemsArray.getJSONObject(i);
                if (obj.getString("name").equals(option.getName())) {
                    found = true;
                    if (newQty > 0) {
                        // Update
                        obj.put("qty", newQty);
                        obj.put("hrs", newHrs); // Store hrs
                        double total = 0;
                        if (option.getType() == com.managepickle.model.RentalOption.RentalType.HOURLY) {
                            total = option.getPrice() * newQty * newHrs;
                        } else {
                            total = option.getPrice() * newQty;
                        }
                        obj.put("total", total);
                        newArray.put(obj);
                    }
                    // If newQty == 0, exclude (delete)
                } else {
                    newArray.put(obj);
                }
            }

            if (!found && newQty > 0) {
                JSONObject obj = new JSONObject();
                obj.put("name", option.getName());
                obj.put("price", option.getPrice());
                obj.put("qty", newQty);
                obj.put("hrs", newHrs);
                double total = 0;
                if (option.getType() == com.managepickle.model.RentalOption.RentalType.HOURLY) {
                    total = option.getPrice() * newQty * newHrs;
                } else {
                    total = option.getPrice() * newQty;
                }
                obj.put("total", total);
                newArray.put(obj);
            }

            root.put("items", newArray);
            booking.setCafeItems(root.toString());

            double newExtras = calculateCafeTotal(booking);
            double newTotal = courtCost + newExtras;
            booking.setTotalAmount(newTotal);

            BookingDAO.updateBooking(booking);

            // Live UI Updates
            String currency = ConfigManager.getConfig().getCurrencySymbol();
            if (currency == null)
                currency = "₹";

            if (lblTotalAmount != null)
                lblTotalAmount.setText(String.format("%s%.2f", currency, newTotal));

            double paid = booking.getPaidAmount();
            double balance = newTotal - paid;

            if (lblBalance != null) {
                lblBalance.setText(String.format("%s%.2f", currency, balance));
                String balanceColor = balance > 0.01 ? "#EF4444" : "#22C55E"; // Red/Green
                lblBalance.setStyle("-fx-text-fill: " + balanceColor + "; -fx-font-size: 18px; -fx-font-weight: bold;");
            }

            if (lblStatusBadge != null) {
                String statusText = balance > 0.01 ? (paid > 0 ? "PARTIALLY PAID" : "UNPAID") : "PAID IN FULL";
                String badgeColor = balance > 0.01 ? (paid > 0 ? "#F59E0B" : "#EF4444") : "#22C55E";
                lblStatusBadge.setText(statusText);
                lblStatusBadge.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: white; " +
                                "-fx-padding: 8 16; -fx-background-radius: 8; " +
                                "-fx-font-size: 12px; -fx-font-weight: bold;",
                        badgeColor));
            }

            displayReceipt(booking);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // addCafeItem removed as it is replaced by handleCafeChange

    private void displayReceipt(Booking booking) {
        receiptPane.getChildren().clear();

        VBox receipt = new VBox(10);
        receipt.setStyle(
                "-fx-background-color: white; -fx-padding: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");
        receipt.setMaxWidth(350);
        receipt.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        Label header = new Label("ARENAFLOW");
        header.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: black;");

        Label subHeader = new Label("RECEIPT");
        subHeader.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-text-fill: black;");

        Label date = new Label(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        date.setStyle("-fx-font-family: 'Courier New'; -fx-text-fill: #555;");

        receipt.getChildren().addAll(header, subHeader, date, new javafx.scene.control.Separator());

        VBox itemsBox = new VBox(5);
        itemsBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Court Item
        HBox courtItem = new HBox(); // Fix HBox usage if needed, but imports are generic
        courtItem.getChildren().addAll(
                new Label("Court Rental"),
                new Region(),
                new Label(String.format("$%.2f", booking.getTotalAmount() - calculateCafeTotal(booking))));
        Region spacer1 = (Region) courtItem.getChildren().get(1);
        spacer1.setPrefWidth(100);
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        itemsBox.getChildren().add(courtItem);

        // Cafe Items
        try {
            String currentItems = booking.getCafeItems();
            if (currentItems != null && !currentItems.isEmpty()) {
                JSONArray itemsArray;
                if (currentItems.trim().startsWith("[")) {
                    itemsArray = new JSONArray(currentItems);
                } else {
                    JSONObject r = new JSONObject(currentItems);
                    itemsArray = r.optJSONArray("items");
                }

                if (itemsArray != null) {
                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject item = itemsArray.getJSONObject(i);
                        HBox row = new HBox();
                        Label name = new Label(item.getString("name") + " x" + item.getInt("qty"));
                        Region space = new Region();
                        HBox.setHgrow(space, Priority.ALWAYS);
                        Label price = new Label(String.format("$%.2f", item.getDouble("total")));
                        row.getChildren().addAll(name, space, price);
                        itemsBox.getChildren().add(row);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        receipt.getChildren().addAll(itemsBox, new javafx.scene.control.Separator());

        // Totals
        HBox totalRow = new HBox();
        Label totalLabel = new Label("TOTAL");
        totalLabel.setStyle("-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalValue = new Label(String.format("$%.2f", booking.getTotalAmount()));
        totalValue.setStyle("-fx-font-weight: bold;");
        totalRow.getChildren().addAll(totalLabel, spacer, totalValue);

        receipt.getChildren().add(totalRow);

        receiptPane.getChildren().add(receipt);
    }

    private double calculateCafeTotal(Booking booking) {
        double total = 0;
        try {
            String currentItems = booking.getCafeItems();
            if (currentItems != null && !currentItems.isEmpty()) {
                JSONArray itemsArray;
                if (currentItems.trim().startsWith("[")) {
                    itemsArray = new JSONArray(currentItems);
                } else {
                    JSONObject r = new JSONObject(currentItems);
                    itemsArray = r.optJSONArray("items");
                }

                if (itemsArray != null) {
                    for (int i = 0; i < itemsArray.length(); i++) {
                        total += itemsArray.getJSONObject(i).getDouble("total");
                    }
                }
            }
        } catch (Exception e) {
        }
        return total;
    }

    public void closeBookingForm() {
        // Reset Booking Info Tab
        bookingInfoPane.getChildren().clear();
        Label placeholder = new Label("📋 Select a slot to view details");
        placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
        bookingInfoPane.getChildren().add(placeholder);

        // Reset Pricing Tab
        pricingPane.getChildren().clear();
        VBox pricingPlaceholder = new VBox(15);
        pricingPlaceholder.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        pricingPlaceholder.setStyle("-fx-padding: 20;");
        Label pricingTitle = new Label("Pricing Breakdown");
        pricingTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label pricingSub = new Label("No booking selected");
        pricingSub.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-padding: 20 0 0 0;");
        pricingPlaceholder.getChildren().addAll(pricingTitle, pricingSub);
        pricingPane.getChildren().add(pricingPlaceholder);

        // Reset Cafe Tab
        cafePane.getChildren().clear();
        VBox cafePlaceholder = new VBox(15);
        cafePlaceholder.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        cafePlaceholder.setStyle("-fx-padding: 20;");
        Label cafeTitle = new Label("Cafe & Gear");
        cafeTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label cafeSub = new Label("Select a booking first");
        cafeSub.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-padding: 20 0 0 0;");
        cafePlaceholder.getChildren().addAll(cafeTitle, cafeSub);
        cafePane.getChildren().add(cafePlaceholder);

        // Reset Receipt Tab
        receiptPane.getChildren().clear();
        VBox receiptPlaceholder = new VBox(15);
        receiptPlaceholder.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        receiptPlaceholder.setStyle("-fx-padding: 20;");
        Label receiptTitle = new Label("Receipt");
        receiptTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label receiptSub = new Label("No active booking selected");
        receiptSub.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-padding: 20 0 0 0;");
        receiptPlaceholder.getChildren().addAll(receiptTitle, receiptSub);
        receiptPane.getChildren().add(receiptPlaceholder);
    }
}
