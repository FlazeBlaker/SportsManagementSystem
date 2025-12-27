package com.managepickle.database;

import com.managepickle.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public static User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return User.builder()
                        .id(rs.getInt("id"))
                        .username(rs.getString("username"))
                        .passwordHash(rs.getString("password_hash"))
                        .role(rs.getString("role"))
                        .build();
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: " + username, e);
        }
        return null;
    }

    public static void createUser(User user) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES(?,?,?)";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole());
            pstmt.executeUpdate();

            logger.info("User created: " + user.getUsername());
        } catch (SQLException e) {
            logger.error("Error creating user: " + user.getUsername(), e);
        }
    }

    public static java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(User.builder()
                        .id(rs.getInt("id"))
                        .username(rs.getString("username"))
                        .passwordHash(rs.getString("password_hash"))
                        .role(rs.getString("role"))
                        .build());
            }
        } catch (SQLException e) {
            logger.error("Error fetching all users", e);
        }
        return users;
    }
}
