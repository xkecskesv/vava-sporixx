package sk.sporixx.util;

/**
 * Central JDBC configuration for SQLite database access.
 */
public final class DatabaseConfig {

    public static final String SQLITE_URL = "jdbc:sqlite:sporixx.sqlite";

    private DatabaseConfig() {
        throw new UnsupportedOperationException("Utility class");
    }
}

