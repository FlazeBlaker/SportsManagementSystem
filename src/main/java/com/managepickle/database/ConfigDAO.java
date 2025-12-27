package com.managepickle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigDAO {
    private static final Logger logger = LoggerFactory.getLogger(ConfigDAO.class);

    public static String getValue(String key, String defaultValue) {
        String sql = "SELECT value FROM venue_config WHERE key = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            logger.error("Error fetching config: " + key, e);
        }
        return defaultValue;
    }

    public static void setValue(String key, String value) {
        String sql = "INSERT OR REPLACE INTO venue_config (key, value) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error setting config: " + key, e);
        }
    }
}
