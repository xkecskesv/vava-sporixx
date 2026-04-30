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
                .timeFormat("HH:mm")
                .upcomingPaymentsEnabled(true)
                .budgetLimitAlertsEnabled(true)
                .savingRemindersEnabled(false)
                .savingGoalsUpdatesEnabled(true)
                .achievementsEnabled(true);

        String userSql = "SELECT language_code, currency_code FROM users WHERE id = ?";

        String accountSql = """
                SELECT r.decimal_separator, r.thousands_separator,
                       r.date_format, r.time_format
                FROM accounts a
                JOIN regions r ON a.region_id = r.id
                WHERE a.owner_user_id = ?
                LIMIT 1
                """;

        String notifSql = """
                SELECT notif_upcoming, notif_budget, notif_reminders,
                       notif_goals, notif_achievements
                FROM user_notification_settings
                WHERE user_id = ?
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
                    builder.decimalSeparator(rs.getString("decimal_separator"))
                            .thousandsSeparator(rs.getString("thousands_separator"))
                            .dateFormat(rs.getString("date_format"))
                            .timeFormat(rs.getString("time_format"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(notifSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    builder.upcomingPaymentsEnabled(rs.getInt("notif_upcoming") == 1)
                            .budgetLimitAlertsEnabled(rs.getInt("notif_budget") == 1)
                            .savingRemindersEnabled(rs.getInt("notif_reminders") == 1)
                            .savingGoalsUpdatesEnabled(rs.getInt("notif_goals") == 1)
                            .achievementsEnabled(rs.getInt("notif_achievements") == 1);
                } else {
                    insertDefaultNotifications(conn, userId);
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

        String notifSql = """
                INSERT INTO user_notification_settings
                (user_id, notif_upcoming, notif_budget, notif_reminders, notif_goals, notif_achievements)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    notif_upcoming     = excluded.notif_upcoming,
                    notif_budget       = excluded.notif_budget,
                    notif_reminders    = excluded.notif_reminders,
                    notif_goals        = excluded.notif_goals,
                    notif_achievements = excluded.notif_achievements
                """;

        try (Connection conn = getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setString(1, settings.getLanguageCode());
                ps.setString(2, settings.getCurrencyCode());
                ps.setInt(3, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(notifSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, settings.isUpcomingPaymentsEnabled() ? 1 : 0);
                ps.setInt(3, settings.isBudgetLimitAlertsEnabled() ? 1 : 0);
                ps.setInt(4, settings.isSavingRemindersEnabled() ? 1 : 0);
                ps.setInt(5, settings.isSavingGoalsUpdatesEnabled() ? 1 : 0);
                ps.setInt(6, settings.isAchievementsEnabled() ? 1 : 0);
                ps.executeUpdate();
            }

            logger.info("Settings saved for userId={}", userId);

        } catch (SQLException e) {
            logger.error("Failed to save settings for userId={}", userId, e);
        }
    }

    private void insertDefaultNotifications(Connection conn, int userId) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO user_notification_settings
                (user_id, notif_upcoming, notif_budget, notif_reminders, notif_goals, notif_achievements)
                VALUES (?, 1, 1, 0, 1, 1)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            logger.info("Default notification settings inserted for userId={}", userId);
        }
    }
}