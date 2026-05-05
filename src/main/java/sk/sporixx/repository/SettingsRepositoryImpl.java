package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.UserSettings;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.DatabaseConfig;

import java.sql.*;

public class SettingsRepositoryImpl implements SettingsRepository {

    private static final Logger logger = LoggerFactory.getLogger(SettingsRepositoryImpl.class);

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
    }

    @Override
    public UserSettings load() {
        int userId = SessionManager.getInstance().getCurrentUserId();

        UserSettings.UserSettingsBuilder builder = UserSettings.builder()
                .languageCode("en")
                .currencyCode("EUR")
                .currencySymbol("€")
                .decimalSeparator(",")
                .thousandsSeparator(" ")
                .dateFormat("dd.MM.yyyy")
                .timeFormat("HH:mm");

        String userSql = "SELECT language_code, currency_code FROM users WHERE id = ?";

        String accountSql = """
                SELECT u.language_code, u.currency_code,
                       r.decimal_separator, r.thousands_separator,
                       r.date_format, r.time_format
                FROM accounts a
                JOIN regions r ON a.region_id = r.id
                JOIN users u ON u.id = a.owner_user_id
                WHERE a.owner_user_id = ?
                LIMIT 1
                """;

        try (Connection conn = getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    builder.languageCode(rs.getString("language_code"))
                            .currencyCode(rs.getString("currency_code"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(accountSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    builder.languageCode(rs.getString("language_code"))
                            .currencyCode(rs.getString("currency_code"))
                            .decimalSeparator(rs.getString("decimal_separator"))
                            .thousandsSeparator(rs.getString("thousands_separator"))
                            .dateFormat(rs.getString("date_format"))
                            .timeFormat(rs.getString("time_format"));
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to load settings for userId={}, using defaults", userId, e);
        }

        return builder.build();
    }

    @Override
    public void save(UserSettings settings) {
        int userId = SessionManager.getInstance().getCurrentUserId();

        String userSql = "UPDATE users SET language_code = ?, currency_code = ? WHERE id = ?";

        try (Connection conn = getConnection()) {

            try (PreparedStatement psUser = conn.prepareStatement(userSql)) {
                psUser.setString(1, settings.getLanguageCode());
                psUser.setString(2, settings.getCurrencyCode());
                psUser.setInt(3, userId);
                psUser.executeUpdate();
            }

            logger.info("User settings saved for userId={}", userId);

        } catch (SQLException e) {
            logger.error("Failed to save settings for userId={}", userId, e);
        }
    }
}