package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Account;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ukladanie a hladanie usera z db
 */
public class AccountRepositoryImpl implements AccountRepository {

    private static final Logger logger = LoggerFactory.getLogger(AccountRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private Account mapResult(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getInt("id"));
        account.setOwnerUserId(rs.getInt("owner_user_id"));
        account.setRegionId(rs.getInt("region_id"));
        account.setDescription(rs.getString("description"));
        account.setAccountTypeId(rs.getInt("account_type_id"));
        account.setDefaultCurrencyCode(rs.getString("default_currency_code"));
        account.setInitialBalance(rs.getDouble("initial_balance"));
        account.setCurrentBalance(rs.getDouble("current_balance"));
        account.setActive(rs.getInt("is_active") == 1);

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            account.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        return account;
    }

    @Override
    public List<Account> findByOwnerUserId(int ownerUserId) {
        String sql = "SELECT * FROM accounts WHERE owner_user_id = ?";
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerUserId);
            ResultSet results = pstmt.executeQuery();

            while (results.next()) {
                accounts.add(mapResult(results));
            }
            logger.debug("Found {} accounts for user ID: {}", accounts.size(), ownerUserId);
        } catch (SQLException e) {
            logger.error("Error finding accounts for user ID: {}", ownerUserId, e);
            throw new RuntimeException("Error reading accounts from database", e);
        }
        return accounts;
    }

    @Override
    public Optional<Account> findById(int accountId) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding account by ID: {}", accountId, e);
            throw new RuntimeException("Error reading account from database", e);
        }
        return Optional.empty();
    }

    @Override
    public Account save(Account account) {
        String sql = "INSERT INTO accounts (owner_user_id, region_id, description, account_type_id, " +
                "default_currency_code, initial_balance, current_balance, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, account.getOwnerUserId());
            pstmt.setInt(2, account.getRegionId());
            pstmt.setString(3, account.getDescription());
            pstmt.setInt(4, account.getAccountTypeId());
            pstmt.setString(5, account.getDefaultCurrencyCode());
            pstmt.setDouble(6, account.getInitialBalance());
            pstmt.setDouble(7, account.getCurrentBalance());
            pstmt.setInt(8, account.isActive() ? 1 : 0);

            LocalDateTime createdAt = account.getCreatedAt() != null ? account.getCreatedAt() : LocalDateTime.now();
            pstmt.setString(9, createdAt.toString().replace("T", " "));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        account.setId(generatedKeys.getInt(1));
                        account.setCreatedAt(createdAt);
                    }
                }
            }
            logger.info("New account created. Type: {}, ID: {}", account.getAccountTypeId(), account.getId());

        } catch (SQLException e) {
            logger.error("Error saving account for owner: {}", account.getOwnerUserId(), e);
            throw new RuntimeException("Error writing account to database", e);
        }
        return account;
    }

    @Override
    public void updateBalance(int accountId, double newBalance) {
        String sql = "UPDATE accounts SET current_balance = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newBalance);
            pstmt.setInt(2, accountId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error updating balance for account ID: {}", accountId, e);
            throw new RuntimeException("Error updating balance in database", e);
        }
    }

    @Override
    public void update(Account account) {
        // TODO: implementovať
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deactivateById(int accountId) {
        // TODO: implementovať
        throw new UnsupportedOperationException("Not implemented yet");
    }
}