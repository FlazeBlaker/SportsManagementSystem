package com.managepickle.controller;

import com.managepickle.database.BookingDAO;
import com.managepickle.database.ConfigDAO;
import com.managepickle.database.CourtDAO;
import com.managepickle.model.Booking;
import com.managepickle.model.Court;
import com.managepickle.model.AppConfig;
import com.managepickle.utils.ConfigManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
            datePicker.setOnAction(e -> {
                currentDate = datePicker.getValue();
                refresh();
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
        courtsHeader.getChildren().clear();
        timeLabels.getChildren().clear();
        bookingGrid.getChildren().clear();

        String startStr = ConfigDAO.getValue("opening_time", "06:00");
        String endStr = ConfigDAO.getValue("closing_time", "23:00");
        LocalTime startTime = LocalTime.parse(startStr);
        LocalTime endTime = LocalTime.parse(endStr);

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
        int startHour = startTime.getHour();
        int endHour = endTime.getHour();
        int row = 0;
        LocalTime currentTime = LocalTime.now();

        for (int h = startHour; h <= endHour; h++) {
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
                StackPane slot = createPremiumSlot(courts.get(col), h);
                bookingGrid.add(slot, col, row);
            }
            row++;
        }

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

            // Click to view details
            final Booking finalBooking = booking;
            slot.setOnMouseClicked(e -> showBookingDetails(finalBooking));

            // Hover effect for booked slots
            final String finalStatusColor = statusColor;
            slot.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), slot);
                st.setToX(1.03);
                st.setToY(1.03);
                st.play();
                slot.setStyle(
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
            });

            final String bookedStyle = slot.getStyle();
            slot.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), slot);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
                slot.setStyle(bookedStyle);
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

            // Court rental breakdown
            Label courtLabel = new Label("Court Rental");
            courtLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

            // Calculate hours safely
            if (booking.getStartTime() == null || booking.getEndTime() == null) {
                pricingContent.getChildren().add(new Label("Error: Invalid booking times"));
                pricingPane.getChildren().add(pricingContent);
                return;
            }

            String currency = ConfigManager.getConfig().getCurrencySymbol();
            if (currency == null)
                currency = "₹"; // Fallback safety

            double hours = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes() / 60.0;
            double hourlyRate = hours > 0 ? booking.getTotalAmount() / hours : 0; // Note: this calculation assumes
                                                                                  // total is just court. Needs
                                                                                  // refinement if total includes other
                                                                                  // things.
            // Better: Court Cost = Total - Paid? No. Court Cost needs to be stored or
            // calculated from Court Rate.
            // For now, let's assume Booking Total is initially just Court Cost.
            // Ideally we separate court cost from add-ons.

            javafx.scene.layout.HBox courtLineItem = createPricingLineItem(
                    String.format("%.1f hour(s) × %s%.2f", hours, currency, hourlyRate),
                    String.format("%s%.2f", currency, booking.getTotalAmount())); // This needs to be just court cost if
                                                                                  // we add rentals!

            // Total section
            javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator();
            separator2.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

            javafx.scene.layout.HBox totalLine = createPricingLineItem("Total Amount",
                    String.format("%s%.2f", currency, booking.getTotalAmount()));
            totalLine.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            javafx.scene.layout.HBox paidLine = createPricingLineItem("Paid",
                    String.format("%s%.2f", currency, booking.getPaidAmount()));
            paidLine.setStyle("-fx-font-size: 14px;");

            double balance = booking.getTotalAmount() - booking.getPaidAmount();
            javafx.scene.layout.HBox balanceLine = createPricingLineItem("Balance Due",
                    String.format("%s%.2f", currency, balance));

            // Color code balance
            String balanceColor = balance > 0.01 ? "#EF4444" : "#22C55E"; // Red if unpaid, Green if paid
            balanceLine.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            if (balanceLine.getChildren().size() > 1) { // Check before casting
                // HBox has Label, spacer (Region), value (Label) -> get(2)
                javafx.scene.Node valueNode = balanceLine.getChildren().get(2);
                if (valueNode instanceof Label) {
                    ((Label) valueNode).setStyle(
                            "-fx-text-fill: " + balanceColor + "; -fx-font-size: 18px; -fx-font-weight: bold;");
                }
            }

            // Payment status badge
            Label statusBadge = new Label(
                    balance > 0.01 ? (booking.getPaidAmount() > 0 ? "PARTIALLY PAID" : "UNPAID") : "PAID IN FULL");
            String badgeColor = balance > 0.01 ? (booking.getPaidAmount() > 0 ? "#F59E0B" : "#EF4444") : "#22C55E";
            statusBadge.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; " +
                            "-fx-padding: 8 16; -fx-background-radius: 8; " +
                            "-fx-font-size: 12px; -fx-font-weight: bold;",
                    badgeColor));

            pricingContent.getChildren().addAll(
                    titleLabel, subtitleLabel, separator1,
                    courtLabel, courtLineItem);

            // RENTALS SECTION
            if (ConfigManager.getConfig().isHasRentals() && ConfigManager.getConfig().getRentalOptions() != null) {
                javafx.scene.control.Separator sepRental = new javafx.scene.control.Separator();
                sepRental.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

                Label rentalLabel = new Label("Equipment Rentals");
                rentalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

                pricingContent.getChildren().addAll(sepRental, rentalLabel);

                for (com.managepickle.model.RentalOption option : ConfigManager.getConfig().getRentalOptions()) {
                    HBox row = new HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label name = new Label(option.getName());
                    name.setStyle("-fx-text-fill: #CBD5E1; -fx-min-width: 100;");

                    // Display Price
                    String rentalCurrency = ConfigManager.getConfig().getCurrencySymbol();
                    if (rentalCurrency == null)
                        rentalCurrency = "₹";
                    String priceStr = String.format("%s%.2f", rentalCurrency, option.getPrice());
                    Label priceDisplay = new Label(priceStr
                            + (option.getType() == com.managepickle.model.RentalOption.RentalType.HOURLY ? "/hr" : ""));
                    priceDisplay.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Logic inputs
                    Button addBtn = new Button("Add");
                    addBtn.setStyle(
                            "-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: #7C3AED; -fx-text-fill: white;");

                    if (option.getType() == com.managepickle.model.RentalOption.RentalType.HOURLY) {
                        javafx.scene.control.Spinner<Integer> qtySpin = new javafx.scene.control.Spinner<>(1, 10, 1);
                        qtySpin.setPrefWidth(60);
                        javafx.scene.control.Spinner<Integer> hrSpin = new javafx.scene.control.Spinner<>(1, 4, 1);
                        hrSpin.setPrefWidth(60);

                        addBtn.setOnAction(e -> {
                            double total = option.getPrice() * qtySpin.getValue() * hrSpin.getValue();
                            String itemName = option.getName() + " (" + qtySpin.getValue() + "x for "
                                    + hrSpin.getValue() + "h)";
                            addCafeItem(booking, itemName, total, 1); // Qty 1 because total deals with the math
                        });

                        row.getChildren().addAll(name, priceDisplay, spacer, new Label("Qty:"), qtySpin,
                                new Label("Hrs:"), hrSpin, addBtn);
                    } else {
                        // Flat
                        javafx.scene.control.Spinner<Integer> qtySpin = new javafx.scene.control.Spinner<>(1, 10, 1);
                        qtySpin.setPrefWidth(60);

                        addBtn.setOnAction(e -> {
                            addCafeItem(booking, option.getName(), option.getPrice(), qtySpin.getValue());
                        });

                        row.getChildren().addAll(name, priceDisplay, spacer, new Label("Qty:"), qtySpin, addBtn);
                    }
                    pricingContent.getChildren().add(row);
                }
            }

            pricingContent.getChildren().addAll(
                    separator2,
                    totalLine, paidLine, balanceLine,
                    new javafx.scene.layout.Region(), // Spacer
                    statusBadge);

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
            if (courtId != -1 && hour != -1) {
                Court selectedCourt = CourtDAO.getAllCourts().stream()
                        .filter(c -> c.getId() == courtId)
                        .findFirst()
                        .orElse(null);

                LocalTime selectedTime = LocalTime.of(hour, 0);

                dialogController.setBookingContext(selectedCourt, selectedTime);
            }

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

        Label title = new Label("Cafe & Gear");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("Add items to this booking");
        subtitle.setStyle("-fx-text-fill: #94A3B8;");

        cafeContent.getChildren().addAll(title, subtitle, new javafx.scene.control.Separator());

        // Cafe Items List
        // Cafe Items List from Config
        var config = ConfigManager.getConfig();
        String currency = config.getCurrencySymbol();
        if (currency == null)
            currency = "₹"; // Fallback safety

        List<com.managepickle.model.CafeMenuItem> menu = config.getCafeMenu();
        if (menu != null) {
            for (com.managepickle.model.CafeMenuItem item : menu) {
                String name = item.getName();
                double price = item.getPrice();

                HBox itemRow = new HBox(15);
                itemRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                itemRow.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 8;");

                VBox info = new VBox(2);
                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Label priceLabel = new Label(String.format("%s%.2f", currency, price));
                priceLabel.setStyle("-fx-text-fill: #10B981;");
                info.getChildren().addAll(nameLabel, priceLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                javafx.scene.control.Spinner<Integer> qtySpinner = new javafx.scene.control.Spinner<>(1, 10, 1);
                qtySpinner.setPrefWidth(70);

                Button addButton = new Button("Add");
                addButton.getStyleClass().add("button-primary");
                addButton.setOnAction(e -> addCafeItem(booking, name, price, qtySpinner.getValue()));

                itemRow.getChildren().addAll(info, spacer, qtySpinner, addButton);
                cafeContent.getChildren().add(itemRow);
            }
        }

        cafePane.getChildren().add(cafeContent);
    }

    private void addCafeItem(Booking booking, String name, double price, int qty) {
        try {
            String currentItems = booking.getCafeItems();
            JSONArray itemsArray;
            if (currentItems == null || currentItems.isEmpty()) {
                itemsArray = new JSONArray();
            } else {
                itemsArray = new JSONArray(currentItems);
            }

            JSONObject newItem = new JSONObject();
            newItem.put("name", name);
            newItem.put("price", price);
            newItem.put("qty", qty);
            newItem.put("total", price * qty);
            itemsArray.put(newItem);

            // Update booking
            booking.setCafeItems(itemsArray.toString());
            // In a real app we should also update totalAmount
            booking.setTotalAmount(booking.getTotalAmount() + (price * qty));

            BookingDAO.updateBooking(booking);

            // Refresh panels
            displayPricingBreakdown(booking);
            displayReceipt(booking);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        HBox courtItem = new HBox();
        courtItem.getChildren().addAll(
                new Label("Court Rental"),
                new Region(),
                new Label(String.format("$%.2f", booking.getTotalAmount() - calculateCafeTotal(booking))));
        ((Region) courtItem.getChildren().get(1)).setPrefWidth(150); // Spacer logic simplified
        HBox.setHgrow(courtItem.getChildren().get(1), Priority.ALWAYS);
        itemsBox.getChildren().add(courtItem);

        // Cafe Items
        try {
            String currentItems = booking.getCafeItems();
            if (currentItems != null && !currentItems.isEmpty()) {
                JSONArray itemsArray = new JSONArray(currentItems);
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
                JSONArray itemsArray = new JSONArray(currentItems);
                for (int i = 0; i < itemsArray.length(); i++) {
                    total += itemsArray.getJSONObject(i).getDouble("total");
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
