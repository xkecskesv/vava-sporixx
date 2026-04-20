package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.util.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementácia repozitára používateľov nad SQLite databázou.
 */
public class UserRepositoryImpl implements UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);
    private static final String SELECT_USER_WITH_FLAGS = "SELECT u.*, "
            + "CASE WHEN EXISTS (SELECT 1 FROM account_access aa WHERE aa.user_id = u.id AND aa.access_level >= 2) THEN 1 ELSE 0 END AS family_manager, "
            + "CASE WHEN EXISTS (SELECT 1 FROM account_access aa WHERE aa.user_id = u.id AND aa.access_level >= 3) THEN 1 ELSE 0 END AS admin_access, "
            + "CASE WHEN EXISTS (SELECT 1 FROM accounts a WHERE a.owner_user_id = u.id AND a.is_active = 1) THEN 1 ELSE 0 END AS user_active "
            + "FROM users u";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
    }

    /**
     * Namapuje riadok databázy na doménový objekt {@link User}.
     *
     * @param result výsledok SQL dotazu
     * @return namapovaný používateľ bez odvodených príznakov
     * @throws SQLException keď nastane chyba pri čítaní stĺpcov
     */
    private User mapResult(ResultSet result) throws SQLException {
        User user = new User();
        user.setId(result.getInt("id"));
        user.setEmail(result.getString("email"));
        user.setPasswordHash(result.getString("password_hash"));
        user.setFirstName(result.getString("first_name"));
        user.setLastName(result.getString("last_name"));
        user.setPhotoPath(result.getString("photo_path"));
        user.setGender(result.getString("gender"));

        String createdAtStr = result.getString("created_at");
        if (createdAtStr != null) {
            user.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        return user;
    }

    /**
     * Aplikuje odvodené príznaky používateľa (rola a aktívny stav).
     *
     * @param result výsledok SQL dotazu s vypočítanými stĺpcami
     * @param user používateľ, do ktorého sa príznaky zapisujú
     * @throws SQLException keď nastane chyba pri čítaní výsledku
     */
    private void applyDerivedFlags(ResultSet result, User user) throws SQLException {
        if (result.getInt("admin_access") == 1) {
            user.setRole(Role.ADMIN);
        } else if (result.getInt("family_manager") == 1) {
            user.setRole(Role.FAMILY_MANAGER);
        } else {
            user.setRole(Role.USER);
        }
        user.setActive(result.getInt("user_active") == 1);
    }

    /**
     * Namapuje používateľa vrátane odvodených príznakov z adminového SELECT-u.
     *
     * @param result výsledok SQL dotazu
     * @return používateľ s nastavenou rolou a aktívnym stavom
     * @throws SQLException keď nastane chyba pri mapovaní
     */
    private User mapUserWithFlags(ResultSet result) throws SQLException {
        User user = mapResult(result);
        applyDerivedFlags(result, user);
        return user;
    }

    //TODO: vo find nastaviť role usera


    @Override
    public Optional<User> findByEmail(String email) {
        String sql = SELECT_USER_WITH_FLAGS + " WHERE u.email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapUserWithFlags(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by email: {}", email, e);
            throw new RuntimeException("Error reading from database (findByEmail)", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by ID: {}", id, e);
            throw new RuntimeException("Error reading from database (findById)", e);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, photo_path, gender, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFirstName());
            pstmt.setString(4, user.getLastName());
            pstmt.setString(5, user.getPhotoPath());

            String gender = user.getGender() != null ? user.getGender() : GenderCode.UNKNOWN;
            pstmt.setString(6, gender);

            LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
            pstmt.setString(7, createdAt.toString().replace("T", " "));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                        user.setCreatedAt(createdAt);
                    }
                }
            }
            logger.info("User successfully saved to DB with ID: {}", user.getId());

        } catch (SQLException e) {
            logger.error("Database error while saving user with email: {}", user.getEmail(), e);
            throw new RuntimeException("Error writing to database (save)", e);
        }
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, password_hash = ?, first_name = ?, last_name = ?, photo_path = ?, gender = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFirstName());
            pstmt.setString(4, user.getLastName());
            pstmt.setString(5, user.getPhotoPath());
            pstmt.setString(6, user.getGender());
            pstmt.setInt(7, user.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                logger.warn("No user row updated for ID: {}", user.getId());
                throw new RuntimeException("User update failed, no rows affected");
            }
            logger.info("User successfully updated in DB with ID: {}", user.getId());
            return user;

        } catch (SQLException e) {
            logger.error("Database error while updating user with ID: {}", user.getId(), e);
            throw new RuntimeException("Error writing to database (update)", e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = SELECT_USER_WITH_FLAGS + " ORDER BY u.created_at DESC";

        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUserWithFlags(rs));
            }
            return users;
        } catch (SQLException e) {
            logger.error("Database error while loading all users for admin panel", e);
            throw new RuntimeException("Error reading from database (findAll)", e);
        }
    }

    @Override
    public void deleteById(int id) {
        // Keep this order to satisfy FK dependencies: account_access -> accounts -> users.
        String deleteAccessSql = "DELETE FROM account_access WHERE user_id = ?";
        String deleteAccountsSql = "DELETE FROM accounts WHERE owner_user_id = ?";
        String deleteUserSql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteAccessStmt = conn.prepareStatement(deleteAccessSql);
                 PreparedStatement deleteAccountsStmt = conn.prepareStatement(deleteAccountsSql);
                 PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSql)) {

                deleteAccessStmt.setInt(1, id);
                deleteAccessStmt.executeUpdate();

                deleteAccountsStmt.setInt(1, id);
                deleteAccountsStmt.executeUpdate();

                deleteUserStmt.setInt(1, id);
                int affectedRows = deleteUserStmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    throw new RuntimeException(String.format("User with ID %d does not exist", id));
                }

                conn.commit();
                logger.info("User successfully deleted from DB with ID: {}", id);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            logger.error("Database error while deleting user with ID: {}", id, e);
            throw new RuntimeException("Error deleting user from database", e);
        }
    }

    /**
     * Uloží stav roly rodinného manažéra úpravou záznamov v {@code account_access}.
     *
     * <p>Pri povýšení zabezpečí, že používateľ má aspoň jedno oprávnenie s úrovňou 2.
     * Pri znížení roly zníži existujúce oprávnenia úrovne 2 na úroveň 1, pričom vyššie
     * úrovne (napr. admin úroveň 3) ponechá bez zmeny.</p>
     *
     * @param userId ID používateľa, ktorému sa má upraviť oprávnenie
     * @param isFamilyManager požadovaný stav roly rodinného manažéra
     */
    @Override
    public void updateFamilyManagerStatus(int userId, boolean isFamilyManager) {
        String demoteSql = "UPDATE account_access SET access_level = 1 WHERE user_id = ? AND access_level = 2";
        String alreadyManagerSql = "SELECT 1 FROM account_access WHERE user_id = ? AND access_level >= 2 LIMIT 1";
        String pickAccountSql = "SELECT id FROM accounts WHERE owner_user_id = ? ORDER BY is_active DESC, id ASC LIMIT 1";
        String grantManagerSql = "INSERT INTO account_access (user_id, account_id, access_level) VALUES (?, ?, 2) "
                + "ON CONFLICT(user_id, account_id) DO UPDATE SET access_level = "
                + "CASE WHEN account_access.access_level < 2 THEN 2 ELSE account_access.access_level END";

        try (Connection conn = getConnection()) {
            if (!isFamilyManager) {
                try (PreparedStatement demoteStmt = conn.prepareStatement(demoteSql)) {
                    demoteStmt.setInt(1, userId);
                    demoteStmt.executeUpdate();
                }
                return;
            }

            try (PreparedStatement managerCheckStmt = conn.prepareStatement(alreadyManagerSql)) {
                managerCheckStmt.setInt(1, userId);
                try (ResultSet managerResult = managerCheckStmt.executeQuery()) {
                    if (managerResult.next()) {
                        return;
                    }
                }
            }

            Integer accountId = null;
            try (PreparedStatement accountStmt = conn.prepareStatement(pickAccountSql)) {
                accountStmt.setInt(1, userId);
                try (ResultSet accountResult = accountStmt.executeQuery()) {
                    if (accountResult.next()) {
                        accountId = accountResult.getInt("id");
                    }
                }
            }

            if (accountId == null) {
                logger.warn("Cannot grant family manager role, user {} has no account", userId);
                return;
            }

            try (PreparedStatement grantStmt = conn.prepareStatement(grantManagerSql)) {
                grantStmt.setInt(1, userId);
                grantStmt.setInt(2, accountId);
                grantStmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Database error while updating family manager status for user {}", userId, e);
            throw new RuntimeException("Error updating family manager status", e);
        }
    }
}