package com.managepickle.database;

import com.managepickle.model.Court;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtDAO {
    private static final Logger logger = LoggerFactory.getLogger(CourtDAO.class);

    public static void saveCourt(Court court) {
        if (court.getId() > 0) {
            updateCourt(court);
        } else {
            int newId = createCourt(court);
            if (newId != -1) {
                court.setId(newId);
            }
        }
    }

    public static int createCourt(Court court) {
        String sql = "INSERT INTO courts(name, type, icon_code, environment_type, hourly_rate, use_global_rates) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, court.getName());
            pstmt.setString(2, court.getType());
            pstmt.setString(3, court.getIconCode());
            pstmt.setString(4, court.getEnvironmentType());
            pstmt.setDouble(5, court.getHourlyRate());
            pstmt.setInt(6, court.isUseGlobalRates() ? 1 : 0);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating court", e);
        }
        return -1;
    }

    public static void updateCourt(Court court) {
        String sql = "UPDATE courts SET name = ?, type = ?, icon_code = ?, environment_type = ?, hourly_rate = ?, use_global_rates = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, court.getName());
            pstmt.setString(2, court.getType());
            pstmt.setString(3, court.getIconCode());
            pstmt.setString(4, court.getEnvironmentType());
            pstmt.setDouble(5, court.getHourlyRate());
            pstmt.setInt(6, court.isUseGlobalRates() ? 1 : 0);
            pstmt.setInt(7, court.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating court", e);
        }
    }

    public static void deleteCourt(int id) {
        String sql = "DELETE FROM courts WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting court", e);
        }
    }

    public static List<Court> getAllCourts() {
        List<Court> courts = new ArrayList<>();
        String sql = "SELECT * FROM courts";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Court c = Court.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .type(rs.getString("type"))
                        .iconCode(rs.getString("icon_code"))
                        .environmentType(rs.getString("environment_type"))
                        .environmentType(rs.getString("environment_type"))
                        .hourlyRate(rs.getDouble("hourly_rate"))
                        .useGlobalRates(rs.getInt("use_global_rates") == 1) // 1 = true
                        .build();
                courts.add(c);
            }

            // Merge with AppConfig for complex rules
            try {
                com.managepickle.model.AppConfig config = com.managepickle.utils.ConfigManager.getConfig();
                if (config != null && config.getCourts() != null) {
                    for (Court dbCourt : courts) {
                        for (Court configCourt : config.getCourts()) {
                            if (configCourt.getId() == dbCourt.getId()) {
                                dbCourt.setUseGlobalRates(configCourt.isUseGlobalRates());
                                dbCourt.setPricingRules(configCourt.getPricingRules());
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to merge court config from JSON", e);
            }

        } catch (SQLException e) {
            logger.error("Error fetching all courts", e);
        }
        return courts;
    }
}
