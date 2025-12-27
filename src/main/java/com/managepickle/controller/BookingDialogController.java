package com.managepickle.controller;

import com.managepickle.database.BookingDAO;
import com.managepickle.database.CourtDAO;
import com.managepickle.model.Booking;
import com.managepickle.model.Court;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import com.managepickle.utils.ConfigManager;
// import com.managepickle.service.PricingService;
// import com.managepickle.model.CafeMenuItem;
// import com.managepickle.model.RentalOption;
// import com.managepickle.model.AppConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class BookingDialogController {

    @FXML
    private ComboBox<Court> courtComboBox;
    @FXML
    private TextField customerNameField;
    @FXML
    private TextField customerPhoneField;
    // Logic-only DatePicker (Hidden from UI)
    private DatePicker datePicker = new DatePicker();
    @FXML
    private ComboBox<String> startTimeComboBox;
    @FXML
    private ComboBox<String> endTimeComboBox;
    @FXML
    private Label courtPriceLabel;
    @FXML
    private Label extrasPriceLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label warningLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label courtLabel;
    @FXML
    private Label courtCostLabel;
    @FXML
    private Button deleteButton;
    @FXML
    private Button saveButton;

    private double courtCostTotal = 0.0;
    private BookingsController parentController;

    @FXML
    public void initialize() {
        // Load Resource Name
        com.managepickle.model.AppConfig config = com.managepickle.utils.ConfigManager.getConfig();
        String rSingular = config.getResourceName() != null ? config.getResourceName() : "Resource";

        if (courtLabel != null)
            courtLabel.setText(rSingular + ":");
        if (courtCostLabel != null)
            courtCostLabel.setText(rSingular + " Cost:");

        // Load Courts
        List<Court> courts = CourtDAO.getAllCourts();
        courtComboBox.setItems(FXCollections.observableArrayList(courts));
        courtComboBox.setConverter(new StringConverter<Court>() {
            @Override
            public String toString(Court court) {
                if (court == null)
                    return "";
                String type = court.getEnvironmentType();
                if (type == null)
                    type = court.getType();
                return court.getName() + (type != null && !type.isEmpty() ? " (" + type + ")" : "");
            }

            @Override
            public Court fromString(String string) {
                return null;
            }
        });

        refreshTimeSlots();

        // Add Listeners for Price Calculation
        datePicker.setOnAction(e -> {
            refreshTimeSlots();
            calculatePrice();
        });

        // Add Listeners for Price Calculation
        courtComboBox.setOnAction(e -> calculatePrice());
        startTimeComboBox.setOnAction(e -> calculatePrice());
        endTimeComboBox.setOnAction(e -> calculatePrice());

    }

    private void refreshTimeSlots() {
        startTimeComboBox.getItems().clear();
        endTimeComboBox.getItems().clear();

        LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        com.managepickle.model.OperatingHours hours = com.managepickle.service.PricingService.getOperatingHours(date);

        if (hours.isClosed()) {
            startTimeComboBox.setPromptText("CLOSED");
            endTimeComboBox.setPromptText("CLOSED");
            return;
        }

        startTimeComboBox.setPromptText("Start Time");
        endTimeComboBox.setPromptText("End Time");

        for (int i = hours.getStartHour(); i <= hours.getEndHour(); i++) {
            String time = String.format("%02d:00", i);
            startTimeComboBox.getItems().add(time);
            endTimeComboBox.getItems().add(time);
        }
    }

    // Extras UI setup removed

    // Extras UI removed

    private void calculatePrice() {
        if (priceLabel == null)
            return;

        try {
            Court selectedCourt = courtComboBox.getValue();
            String startStr = startTimeComboBox.getValue();
            String endStr = endTimeComboBox.getValue();
            LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();

            if (selectedCourt != null && startStr != null && endStr != null) {
                LocalTime start = LocalTime.parse(startStr);
                LocalTime end = LocalTime.parse(endStr);
                LocalDateTime startDT = LocalDateTime.of(date, start);
                LocalDateTime endDT = LocalDateTime.of(date, end);

                // Handle Midnight (00:00) as the next day if start is not 00:00
                if (end.equals(LocalTime.MIN) && !start.equals(LocalTime.MIN)) {
                    endDT = endDT.plusDays(1);
                }

                if (endDT.isAfter(startDT)) {
                    courtCostTotal = com.managepickle.service.PricingService.calculateCourtCost(selectedCourt, startDT,
                            endDT);

                    // Calculate Extras
                    double grandTotal = courtCostTotal;
                    com.managepickle.model.AppConfig config = com.managepickle.utils.ConfigManager.getConfig();
                    String symbol = config.getCurrencySymbol() != null ? config.getCurrencySymbol() : "₹";

                    courtPriceLabel.setText(String.format("%s%.2f", symbol, courtCostTotal));
                    extrasPriceLabel.setText("0.00");
                    priceLabel.setText(String.format("%s%.2f", symbol, grandTotal));
                } else {
                    priceLabel.setText("Invalid Time Range");
                }
            } else {
                courtPriceLabel.setText("0.00");
                extrasPriceLabel.setText("0.00");
                priceLabel.setText("0.00");
            }
        } catch (Exception e) {
            priceLabel.setText("Error");
        }
    }

    public void setBookingContext(Court court, LocalTime startTime, LocalDate date) {
        if (date != null) {
            datePicker.setValue(date);
            refreshTimeSlots(); // Ensure slots are for the correct day
        }

        if (court != null) {
            courtComboBox.setValue(court);
            courtComboBox.setDisable(true); // Lock court
        }

        if (startTime != null) {
            // Find the matching string in the combobox
            String timeStr = startTime.toString();
            if (timeStr.length() == 5)
                timeStr += ":00"; // Ensure format matches HH:MM:00 if needed, but our combo uses 06:00
            // Actually our combo assumes HH:MM format from the loop
            String formattedTime = String.format("%02d:00", startTime.getHour());

            startTimeComboBox.setValue(formattedTime);
            startTimeComboBox.setDisable(true); // Lock start time

            // Auto-set end time to +1 hour
            LocalTime endTime = startTime.plusHours(1);
            String formattedEndTime = String.format("%02d:00", endTime.getHour());
            endTimeComboBox.setValue(formattedEndTime);

            // Set date to today (or passed date if we supported that)
            // datePicker.setValue(LocalDate.now()); // REMOVED: Respect the passed date!
        }

        calculatePrice();
    }

    private Booking editingBooking = null;

    // ... (rest of initialize and calculatePrice code remains, but need to ensure
    // imports are correct)

    public void setBookingToEdit(Booking booking) {
        if (booking == null)
            return;

        this.editingBooking = booking;

        titleLabel.setText("Edit Booking");
        saveButton.setText("Update Booking");

        // Show Delete Button
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);

        customerNameField.setText(booking.getCustomerName());
        customerPhoneField.setText(booking.getCustomerPhone());

        datePicker.setValue(booking.getStartTime().toLocalDate());

        String startStr = String.format("%02d:00", booking.getStartTime().getHour());
        String endStr = String.format("%02d:00", booking.getEndTime().getHour());

        startTimeComboBox.setValue(startStr);
        endTimeComboBox.setValue(endStr);

        // Find matching court in combobox
        for (Court c : courtComboBox.getItems()) {
            if (c.getId() == booking.getCourtId()) {
                courtComboBox.setValue(c);
                break;
            }
        }

        calculatePrice();
    }

    @FXML
    private void handleDelete() {
        if (editingBooking != null) {
            // In a real app, show confirmation dialog
            BookingDAO.deleteBooking(editingBooking.getId());
            if (parentController != null) {
                parentController.refresh();
                parentController.clearBookingForm();
            } else {
                MainController.closeModal();
            }
        }
    }

    @FXML
    private void handleSave() {
        hideWarning(); // Clear previous warnings
        try {
            Court selectedCourt = courtComboBox.getValue();
            String name = customerNameField.getText();
            String phone = customerPhoneField.getText();
            LocalDate date = datePicker.getValue();
            String startStr = startTimeComboBox.getValue();
            String endStr = endTimeComboBox.getValue();

            if (selectedCourt == null || name.isEmpty() || date == null || startStr == null || endStr == null) {
                showWarning("⚠️ Please fill in all fields");
                return;
            }

            LocalDateTime start = LocalDateTime.of(date, LocalTime.parse(startStr));
            LocalDateTime end = LocalDateTime.of(date, LocalTime.parse(endStr));

            if (end.isBefore(start) || end.equals(start)) {
                showWarning("⚠️ End time must be after start time");
                return;
            }

            // Check Overlap (Exclude current booking if editing)
            // Note: BookingDAO.isBookingOverlapping logic might effectively block updates
            // if it counts itself.
            // Ideally we'd modify checkOverlap to take an exclusion ID, but for now let's
            // skip check if params haven't changed?
            // Or better, assume DAO is strict. For MVP update, let's proceed.
            // Correct fix: update isBookingOverlapping to ignore 'this' booking ID.
            // For now, let's attempt save. If overlap, it might fail validation but that's
            // better than corruption.

            // Validate Operating Hours
            if (!com.managepickle.service.PricingService.isVenueOpen(start)) {
                showWarning("⚠️ Venue is closed at " + start.toLocalTime());
                return;
            }
            if (!com.managepickle.service.PricingService.isVenueOpen(end.minusMinutes(1))) { // Check end time (minus 1
                                                                                             // min to be inclusive of
                                                                                             // range)
                showWarning("⚠️ Venue is closed by " + end.toLocalTime());
                return;
            }

            double amount = com.managepickle.service.PricingService.calculateCourtCost(selectedCourt, start, end);
            double existingExtrasCost = 0.0;
            String extrasString = null;

            if (editingBooking != null) {
                // Preserve existing items
                extrasString = editingBooking.getCafeItems();
                // Recalculate existing extras cost from JSON
                if (extrasString != null && !extrasString.isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(extrasString);
                        JSONArray items = json.optJSONArray("items");
                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                double p = item.optDouble("price", 0.0);
                                int q = item.optInt("qty", 0);
                                if (item.has("total")) {
                                    existingExtrasCost += item.getDouble("total");
                                } else if (p > 0) {
                                    existingExtrasCost += (p * q); // Fallback
                                } else {
                                    // Try to lookup price? For now assume total or 0 if missing.
                                    // Actually the Cafe Logic saves 'total' field.
                                    // Rentals usually have 'price' and 'qty' but no 'total' in my previous reading?
                                    // Let's rely on 'total' if present, else 0.
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing existing extras: " + e.getMessage());
                    }
                }
            } else {
                // New Booking starts with no extras
                JSONObject empty = new JSONObject();
                empty.put("items", new JSONArray());
                extrasString = empty.toString();
            }

            if (editingBooking != null) {
                // UPDATE EXISTING
                // Check if the new time slot conflicts with OTHER bookings (excluding this one)
                if (BookingDAO.isBookingOverlapping(selectedCourt.getId(), start, end, editingBooking.getId())) {
                    showWarning(
                            "❌ This time slot conflicts with another booking! The ENTIRE duration must be available.");
                    return;
                }

                Booking updated = Booking.builder()
                        .id(editingBooking.getId())
                        .courtId(selectedCourt.getId())
                        .customerName(name)
                        .customerPhone(phone)
                        .startTime(start)
                        .endTime(end)
                        .totalAmount(amount + existingExtrasCost) // Preserve extras cost
                        .cafeItems(extrasString)
                        .status(editingBooking.getStatus())
                        .build();

                BookingDAO.updateBooking(updated);

            } else {
                // CREATE NEW
                if (BookingDAO.isBookingOverlapping(selectedCourt.getId(), start, end)) {
                    showWarning("❌ This time slot is already booked! Please choose a different time.");
                    return;
                }

                Booking newBooking = Booking.builder()
                        .courtId(selectedCourt.getId())
                        .customerName(name)
                        .customerPhone(phone)
                        .startTime(start)
                        .endTime(end)
                        .totalAmount(amount) // New booking starts with 0 extras
                        .cafeItems(extrasString)
                        .status("CONFIRMED")
                        .build();

                BookingDAO.createBooking(newBooking);
            }

            if (parentController != null) {
                parentController.refresh();
            } else {
                MainController.closeModal();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        if (parentController != null) {
            parentController.clearBookingForm();
        } else {
            closeDialog();
        }
    }

    public void setParentController(BookingsController parentController) {
        this.parentController = parentController;
    }

    private void closeDialog() {
        if (parentController != null) {
            parentController.closeBookingForm();
        } else {
            MainController.closeModal();
        }
    }

    private void showWarning(String message) {
        warningLabel.setText(message);
        warningLabel.setVisible(true);
        warningLabel.setManaged(true);
    }

    private void hideWarning() {
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);
    }
}
