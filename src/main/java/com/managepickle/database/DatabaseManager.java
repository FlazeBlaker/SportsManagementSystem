package com.managepickle.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:managepickle.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            logger.info("Connected to SQLite database.");
        } catch (SQLException e) {
            logger.error("Connection to SQLite has failed.", e);
        }
        return conn;
    }

    public static void initializeDatabase() {
        String userTable = "CREATE TABLE IF NOT EXISTS users ("
                + "	id INTEGER PRIMARY KEY,"
                + "	username TEXT NOT NULL UNIQUE,"
                + "	password_hash TEXT NOT NULL,"
                + "	role TEXT NOT NULL DEFAULT 'STAFF'"
                + ");";

        String courtTable = "CREATE TABLE IF NOT EXISTS courts ("
                + " id INTEGER PRIMARY KEY,"
                + " name TEXT NOT NULL,"
                + " type TEXT,"
                + " icon_code TEXT,"
                + " environment_type TEXT,"
                + " hourly_rate REAL"
                + ");";

        String bookingTable = "CREATE TABLE IF NOT EXISTS bookings ("
                + " id INTEGER PRIMARY KEY,"
                + " court_id INTEGER,"
                + " customer_name TEXT,"
                + " customer_phone TEXT,"
                + " start_time TEXT,"
                + " end_time TEXT,"
                + " total_amount REAL,"
                + " paid_amount REAL DEFAULT 0,"
                + " no_show INTEGER DEFAULT 0,"
                + " cafe_items TEXT,"
                + " status TEXT,"
                + " FOREIGN KEY(court_id) REFERENCES courts(id)"
                + ");";

        String configTable = "CREATE TABLE IF NOT EXISTS venue_config ("
                + " key TEXT PRIMARY KEY,"
                + " value TEXT"
                + ");";

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {

            // Create Tables
            stmt.execute(userTable);
            stmt.execute(courtTable);
            stmt.execute(bookingTable);
            stmt.execute(configTable);

            // Migration: Add new columns if they don't exist
            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "bookings", "paid_amount");
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE bookings ADD COLUMN paid_amount REAL DEFAULT 0");
                    logger.info("Added paid_amount column to bookings table");
                }
            } catch (SQLException e) {
                logger.warn("Column paid_amount may already exist", e);
            }

            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "bookings", "no_show");
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE bookings ADD COLUMN no_show INTEGER DEFAULT 0");
                    logger.info("Added no_show column to bookings table");
                }
            } catch (SQLException e) {
                logger.warn("Column no_show may already exist", e);
            }

            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "bookings", "cafe_items");
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE bookings ADD COLUMN cafe_items TEXT");
                    logger.info("Added cafe_items column to bookings table");
                }
            } catch (SQLException e) {
                logger.warn("Column cafe_items may already exist", e);
            }

            // Migration for courts: icon_code and environment_type
            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "courts", "icon_code");
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE courts ADD COLUMN icon_code TEXT");
                    logger.info("Added icon_code column to courts table");
                }
            } catch (SQLException e) {
                logger.warn("Column icon_code may already exist", e);
            }

            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "courts", "environment_type");
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE courts ADD COLUMN environment_type TEXT");
                    logger.info("Added environment_type column to courts table");
                }
            } catch (SQLException e) {
                logger.warn("Column environment_type may already exist", e);
            }

            logger.info("Database initialized (Users, Courts, Bookings, Config).");

            // Default admin will be created via Setup Wizard
            // if (UserDAO.findByUsername("admin") == null) { ... }

            // SYNC CONFIG FROM FILE TO DB
            com.managepickle.utils.ConfigManager.loadConfig();
            com.managepickle.model.AppConfig config = com.managepickle.utils.ConfigManager.getConfig();

            // Sync Hours
            ConfigDAO.setValue("opening_time", config.getOpeningTime());
            ConfigDAO.setValue("closing_time", config.getClosingTime());

            // Sync Courts (Upsert/Delete Logic or just Replace All?)
            // For simplicity and to ensure ID match, we'll iterate.
            // CAUTION: Deleting courts might orphan bookings if we are not careful.
            // For now, let's assume we Update existing IDs and Insert new ones.
            // If the user removed a court from Config, we should probably delete it from
            // DB?
            // Let's implement a simple "Clear and Re-insert" approach for Courts Table?
            // OR safer: Check existing.

            // Safer Sync:
            // 1. Update/Insert from Config
            for (com.managepickle.model.Court c : config.getCourts()) {
                CourtDAO.saveCourt(c);
            }
            // 2. (Optional) Delete courts in DB that are NOT in Config?
            // Skipping delete for now to prevent booking data loss on accidental config
            // edit.

        } catch (SQLException e) {
            logger.error("Database initialization failed.", e);
        }
    }
}
