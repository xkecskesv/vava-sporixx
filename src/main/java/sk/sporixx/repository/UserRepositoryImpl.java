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
 * ukladanie a hladanie usera z db
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

    // Mapuje dáta z databázy na Java objekt
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

    private User mapUserWithFlags(ResultSet result) throws SQLException {
        User user = mapResult(result);
        applyDerivedFlags(result, user);
        return user;
    }

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
}