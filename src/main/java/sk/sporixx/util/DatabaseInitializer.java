package sk.sporixx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private DatabaseInitializer() {}

    public static void init() {
        if (isAlreadyInitialized()) {
            logger.info("Database already initialized, skipping schema creation.");
            return;
        }
        logger.info("Database not initialized — running schema.sql...");
        runSchema();
        logger.info("Database schema created successfully.");
    }

    private static boolean isAlreadyInitialized() {
        String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='users'";
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.warn("Could not check DB state, will attempt schema creation.", e);
            return false;
        }
    }

    private static void runSchema() {
        String schema = loadSchema();
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
             Statement stmt = conn.createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create database schema.", e);
            throw new RuntimeException("Database initialization failed.", e);
        }
    }

    private static String loadSchema() {
        try (InputStream is = DatabaseInitializer.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql not found in classpath.");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read schema.sql.", e);
        }
    }
}
