package com.managepickle.database;

import com.managepickle.model.Booking;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookingDAO {
    private static final Logger logger = LoggerFactory.getLogger(BookingDAO.class);
    // Using simple ISO-like string format for SQLite, or standard ISO8601
    private static final DateTimeFormatter Dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings ORDER BY start_time DESC";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                bookings.add(Booking.builder()
                        .id(rs.getInt("id"))
                        .courtId(rs.getInt("court_id"))
                        .customerName(rs.getString("customer_name"))
                        .customerPhone(rs.getString("customer_phone"))
                        .startTime(LocalDateTime.parse(rs.getString("start_time"), Dtf))
                        .endTime(LocalDateTime.parse(rs.getString("end_time"), Dtf))
                        .totalAmount(rs.getDouble("total_amount"))
                        .paidAmount(rs.getDouble("paid_amount"))
                        .noShow(rs.getBoolean("no_show"))
                        .cafeItems(rs.getString("cafe_items"))
                        .status(rs.getString("status"))
                        .build());
            }
        } catch (SQLException e) {
            logger.error("Error fetching bookings", e);
        }
        return bookings;
    }

    public static boolean isBookingOverlapping(int courtId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE court_id = ? AND status != 'CANCELLED' " +
                "AND ((start_time < ? AND end_time > ?) OR (start_time >= ? AND start_time < ?))";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courtId);
            pstmt.setString(2, end.format(Dtf)); // Use Dtf for consistency
            pstmt.setString(3, start.format(Dtf)); // Use Dtf for consistency
            pstmt.setString(4, start.format(Dtf)); // Use Dtf for consistency
            pstmt.setString(5, end.format(Dtf)); // Use Dtf for consistency

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking booking overlap", e);
        }
        return false;
    }

    public static void createBooking(Booking booking) {
        String sql = "INSERT INTO bookings(court_id, customer_name, customer_phone, start_time, end_time, total_amount, paid_amount, no_show, cafe_items, status) VALUES(?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, booking.getCourtId());
            pstmt.setString(2, booking.getCustomerName());
            pstmt.setString(3, booking.getCustomerPhone());
            pstmt.setString(4, booking.getStartTime().format(Dtf));
            pstmt.setString(5, booking.getEndTime().format(Dtf));
            pstmt.setDouble(6, booking.getTotalAmount());
            pstmt.setDouble(7, booking.getPaidAmount());
            pstmt.setBoolean(8, booking.isNoShow());
            pstmt.setString(9, booking.getCafeItems());
            pstmt.setString(10, booking.getStatus());

            pstmt.executeUpdate();
            logger.info("Booking created for: " + booking.getCustomerName());

        } catch (SQLException e) {
            logger.error("Error creating booking", e);
        }
    }

    public static void updateBooking(Booking booking) {
        String sql = "UPDATE bookings SET court_id=?, customer_name=?, customer_phone=?, start_time=?, end_time=?, total_amount=?, paid_amount=?, no_show=?, cafe_items=?, status=? WHERE id=?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, booking.getCourtId());
            pstmt.setString(2, booking.getCustomerName());
            pstmt.setString(3, booking.getCustomerPhone());
            pstmt.setString(4, booking.getStartTime().format(Dtf));
            pstmt.setString(5, booking.getEndTime().format(Dtf));
            pstmt.setDouble(6, booking.getTotalAmount());
            pstmt.setDouble(7, booking.getPaidAmount());
            pstmt.setBoolean(8, booking.isNoShow());
            pstmt.setString(9, booking.getCafeItems());
            pstmt.setString(10, booking.getStatus());
            pstmt.setInt(11, booking.getId());

            pstmt.executeUpdate();
            logger.info("Booking updated: " + booking.getId());

        } catch (SQLException e) {
            logger.error("Error updating booking", e);
        }
    }

    public static void deleteBooking(int id) {
        String sql = "DELETE FROM bookings WHERE id=?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.info("Booking deleted: " + id);

        } catch (SQLException e) {
            logger.error("Error deleting booking", e);
        }
    }
}
