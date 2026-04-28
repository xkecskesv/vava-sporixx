package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.AccountAccess;
import sk.sporixx.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountAccessRepositoryImpl implements AccountAccessRepository {

    private static final Logger logger = LoggerFactory.getLogger(AccountAccessRepositoryImpl.class);

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
    }

    @Override
    public List<AccountAccess> findByUserId(int userId) {
        String sql = "SELECT * FROM account_access WHERE user_id = ?";
        List<AccountAccess> accessList = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    accessList.add(mapResultSetToAccountAccess(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding account access for user ID: {}", userId, e);
            throw new RuntimeException("DB error finding user accesses", e);
        }
        return accessList;
    }

    @Override
    public void grantAccess(int userId, int accountId, int accessLevel) {
        String sql = "INSERT INTO account_access (user_id, account_id, access_level) VALUES (?, ?, ?) " +
                "ON CONFLICT(user_id, account_id) DO UPDATE SET access_level = excluded.access_level";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, accountId);
            pstmt.setInt(3, accessLevel);
            pstmt.executeUpdate();

            logger.info("Granted access (level {}) for user {} to account {}", accessLevel, userId, accountId);
        } catch (SQLException e) {
            logger.error("Error granting access for user {} to account {}", userId, accountId, e);
            throw new RuntimeException("DB error granting access", e);
        }
    }

    @Override
    public void revokeAccess(int userId, int accountId) {
        String sql = "DELETE FROM account_access WHERE user_id = ? AND account_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, accountId);
            pstmt.executeUpdate();

            logger.info("Revoked access for user {} to account {}", userId, accountId);
        } catch (SQLException e) {
            logger.error("Error revoking access for user {} to account {}", userId, accountId, e);
            throw new RuntimeException("DB error revoking access", e);
        }
    }

    @Override
    public void revokeAllAccess(int userId) {
        String sql = "DELETE FROM account_access WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int rowsDeleted = pstmt.executeUpdate();

            logger.info("Revoked ALL access for user {} ({} records deleted)", userId, rowsDeleted);
        } catch (SQLException e) {
            logger.error("Error revoking all access for user {}", userId, e);
            throw new RuntimeException("DB error revoking all access for user", e);
        }
    }

    @Override
    public List<AccountAccess> findByAccountId(int accountId) {
        String sql = "SELECT * FROM account_access WHERE account_id = ?";
        List<AccountAccess> accessList = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    accessList.add(mapResultSetToAccountAccess(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding account access for account ID: {}", accountId, e);
            throw new RuntimeException("DB error finding account accesses", e);
        }
        return accessList;
    }

    @Override
    public void revokeAllAccessForAccount(int accountId) {
        String sql = "DELETE FROM account_access WHERE account_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            int rowsDeleted = pstmt.executeUpdate();

            logger.info("Revoked ALL access for account {} ({} records deleted)", accountId, rowsDeleted);
        } catch (SQLException e) {
            logger.error("Error revoking all access for account {}", accountId, e);
            throw new RuntimeException("DB error revoking all access for account", e);
        }
    }

    private AccountAccess mapResultSetToAccountAccess(ResultSet rs) throws SQLException {
        LocalDateTime grantedAt = null;
        if (rs.getString("granted_at") != null) {
            grantedAt = LocalDateTime.parse(rs.getString("granted_at").replace(" ", "T"));
        }

        return AccountAccess.builder()
                .userId(rs.getInt("user_id"))
                .accountId(rs.getInt("account_id"))
                .accessLevel(rs.getInt("access_level"))
                .grantedAt(grantedAt)
                .build();
    }
}